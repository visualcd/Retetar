package com.ro.aminvatatro.presentation.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.repository.RecipeFilter
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.usecase.nutrition.CalorieRecipeSuggestion
import com.ro.aminvatatro.domain.usecase.nutrition.GenerateRecipesForCaloriesTarget
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipes
import com.ro.aminvatatro.domain.usecase.recipe.SaveRecipe
import com.ro.aminvatatro.domain.usecase.recipe.ToggleFavorite
import com.ro.aminvatatro.domain.usecase.shopping.AddRecipeToShoppingList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Recipes list screen.
 */
data class RecipesUiState(
    val isLoading: Boolean = true,
    val recipes: List<Recipe> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val query: String = "",
    val selectedTags: Set<String> = emptySet(),
    val maxTotalMinutes: Int? = null,
    val favoritesOnly: Boolean = false,
    val hasError: Boolean = false,
    val showCalorieDialog: Boolean = false,
    val calorieTargetInput: String = "850",
    val isGeneratingSuggestions: Boolean = false,
    val calorieSuggestions: List<CalorieRecipeSuggestion> = emptyList(),
    val calorieGenerationMessage: String? = null,
) {
    /** `true` when any search query or filter is currently active. */
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || selectedTags.isNotEmpty() || maxTotalMinutes != null || favoritesOnly
}

/**
 * One-shot navigation or feedback events emitted by [RecipesViewModel].
 */
sealed interface RecipesUiEvent {
    data class OpenRecipe(val recipeId: String) : RecipesUiEvent
    data object CreateRecipe : RecipesUiEvent
    data class RecipeSaved(val recipeId: String) : RecipesUiEvent
    data object RecipeAddedToShopping : RecipesUiEvent
}

/**
 * ViewModel driving the Recipes list screen with live search, filtering, favorite toggling,
 * and calorie-targeted recipe generation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipesViewModel(
    private val getRecipes: GetRecipes,
    private val toggleFavorite: ToggleFavorite,
    private val recipeRepository: RecipeRepository,
    private val generateRecipesForCaloriesTarget: GenerateRecipesForCaloriesTarget? = null,
    private val saveRecipe: SaveRecipe? = null,
    private val addRecipeToShoppingList: AddRecipeToShoppingList? = null,
) : ViewModel() {

    private val filterFlow = MutableStateFlow(RecipeFilter())

    private val _uiState = MutableStateFlow(RecipesUiState())
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    private val _events = Channel<RecipesUiEvent>(Channel.BUFFERED)
    val events: Flow<RecipesUiEvent> = _events.receiveAsFlow()

    init {
        observeRecipesStream()
        observeTagsStream()
    }

    private fun observeRecipesStream() {
        viewModelScope.launch {
            filterFlow
                .flatMapLatest { filter -> getRecipes(filter) }
                .catch {
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
                }
                .collect { recipes ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            recipes = recipes,
                            hasError = false,
                        )
                    }
                }
        }
    }

    private fun observeTagsStream() {
        viewModelScope.launch {
            recipeRepository.observeAllTags()
                .catch { /* keep existing tags on transient error */ }
                .collect { tags ->
                    _uiState.update { it.copy(availableTags = tags) }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        syncFilter()
    }

    fun toggleFavoritesFilter() {
        _uiState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
        syncFilter()
    }

    fun setMaxTotalMinutes(minutes: Int?) {
        _uiState.update { it.copy(maxTotalMinutes = minutes) }
        syncFilter()
    }

    fun toggleTagFilter(tagName: String) {
        _uiState.update { state ->
            val updated = if (tagName in state.selectedTags) {
                state.selectedTags - tagName
            } else {
                state.selectedTags + tagName
            }
            state.copy(selectedTags = updated)
        }
        syncFilter()
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                query = "",
                selectedTags = emptySet(),
                maxTotalMinutes = null,
                favoritesOnly = false,
            )
        }
        syncFilter()
    }

    fun onToggleFavorite(recipeId: String) {
        viewModelScope.launch {
            toggleFavorite(recipeId)
        }
    }

    fun onAddRecipeToShoppingList(recipeId: String) {
        val adder = addRecipeToShoppingList ?: return
        viewModelScope.launch {
            try {
                val recipe = recipeRepository.getRecipeById(recipeId) ?: return@launch
                adder(recipe = recipe, servings = recipe.baseServings)
                _events.send(RecipesUiEvent.RecipeAddedToShopping)
            } catch (_: Throwable) {
                _uiState.update { it.copy(hasError = true) }
            }
        }
    }

    fun onRecipeClicked(recipeId: String) {
        viewModelScope.launch {
            _events.send(RecipesUiEvent.OpenRecipe(recipeId))
        }
    }

    fun onCreateRecipeClicked() {
        viewModelScope.launch {
            _events.send(RecipesUiEvent.CreateRecipe)
        }
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, hasError = false) }
        syncFilter()
    }

    fun openCalorieTargetDialog() {
        _uiState.update { it.copy(showCalorieDialog = true) }
        generateSuggestionsForCurrentTarget()
    }

    fun dismissCalorieTargetDialog() {
        _uiState.update { it.copy(showCalorieDialog = false, calorieGenerationMessage = null) }
    }

    fun onCalorieTargetInputChange(input: String) {
        _uiState.update { it.copy(calorieTargetInput = input) }
    }

    fun generateSuggestionsForCurrentTarget() {
        val generator = generateRecipesForCaloriesTarget ?: return
        val target = _uiState.value.calorieTargetInput.trim().toDoubleOrNull()
        if (target == null || target <= 0.0) {
            _uiState.update { it.copy(calorieGenerationMessage = "Introdu un număr valid de kcal.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingSuggestions = true,
                    calorieGenerationMessage = null,
                )
            }
            val suggestions = generator(maxKcalPerServing = target, limit = 5)
            _uiState.update {
                it.copy(
                    isGeneratingSuggestions = false,
                    calorieSuggestions = suggestions,
                    calorieGenerationMessage = if (suggestions.isEmpty()) {
                        "Nu am găsit rețete sub $target kcal."
                    } else null,
                )
            }
        }
    }

    fun acceptSuggestion(suggestion: CalorieRecipeSuggestion) {
        val saver = saveRecipe ?: return
        viewModelScope.launch {
            val result = saver(suggestion.recipe)
            if (result is com.ro.aminvatatro.domain.usecase.recipe.SaveRecipeResult.Success) {
                _uiState.update { it.copy(showCalorieDialog = false) }
                _events.send(RecipesUiEvent.OpenRecipe(result.recipe.id))
            }
        }
    }

    private fun syncFilter() {
        val current = _uiState.value
        filterFlow.value = RecipeFilter(
            query = current.query,
            tagNames = current.selectedTags,
            maxTotalMinutes = current.maxTotalMinutes,
            favoritesOnly = current.favoritesOnly,
        )
    }
}
