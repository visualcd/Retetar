package com.ro.aminvatatro.presentation.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.repository.RecipeFilter
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipes
import com.ro.aminvatatro.domain.usecase.recipe.ToggleFavorite
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
}

/**
 * ViewModel driving the Recipes list screen with live search, filtering, and favorite toggling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipesViewModel(
    private val getRecipes: GetRecipes,
    private val toggleFavorite: ToggleFavorite,
    private val recipeRepository: RecipeRepository,
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
