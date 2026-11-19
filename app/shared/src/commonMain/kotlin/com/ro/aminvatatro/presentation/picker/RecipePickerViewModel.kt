package com.ro.aminvatatro.presentation.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.repository.RecipeFilter
import com.ro.aminvatatro.domain.usecase.plan.SetPlanEntry
import com.ro.aminvatatro.domain.usecase.plan.SetPlanEntryResult
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipes
import com.ro.aminvatatro.domain.validation.RecipeValidator
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
import kotlinx.datetime.LocalDate

/**
 * UI state for selecting a recipe and portion count for a target date and [MealSlot].
 */
data class RecipePickerUiState(
    val date: LocalDate,
    val slot: MealSlot,
    val query: String = "",
    val isLoading: Boolean = true,
    val recipes: List<Recipe> = emptyList(),
    val selectedRecipe: Recipe? = null,
    val servings: Int = 2,
    val isSaving: Boolean = false,
    val hasError: Boolean = false,
)

/**
 * One-shot navigation events emitted by [RecipePickerViewModel].
 */
sealed interface RecipePickerUiEvent {
    data object EntryAdded : RecipePickerUiEvent
}

/**
 * ViewModel for searching recipes, picking one, configuring servings, and scheduling it
 * into a specific meal slot.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipePickerViewModel(
    private val date: LocalDate,
    private val slot: MealSlot,
    private val getRecipes: GetRecipes,
    private val setPlanEntry: SetPlanEntry,
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    private val _uiState = MutableStateFlow(RecipePickerUiState(date = date, slot = slot))
    val uiState: StateFlow<RecipePickerUiState> = _uiState.asStateFlow()

    private val _events = Channel<RecipePickerUiEvent>(Channel.BUFFERED)
    val events: Flow<RecipePickerUiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            queryFlow
                .flatMapLatest { q -> getRecipes(RecipeFilter(query = q)) }
                .catch {
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
                }
                .collect { recipes ->
                    _uiState.update { state ->
                        val updatedSelection = state.selectedRecipe?.let { sel ->
                            recipes.firstOrNull { it.id == sel.id }
                        }
                        state.copy(
                            isLoading = false,
                            recipes = recipes,
                            selectedRecipe = updatedSelection,
                            hasError = false,
                        )
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
    }

    fun selectRecipe(recipe: Recipe) {
        _uiState.update {
            it.copy(
                selectedRecipe = recipe,
                servings = recipe.baseServings.coerceIn(
                    RecipeValidator.MIN_SERVINGS,
                    RecipeValidator.MAX_SERVINGS,
                ),
                hasError = false,
            )
        }
    }

    fun onServingsChange(servings: Int) {
        if (servings in RecipeValidator.MIN_SERVINGS..RecipeValidator.MAX_SERVINGS) {
            _uiState.update { it.copy(servings = servings) }
        }
    }

    fun confirmSelection() {
        val current = _uiState.value
        val recipe = current.selectedRecipe ?: return
        if (current.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, hasError = false) }
            when (setPlanEntry(date = date, slot = slot, recipeId = recipe.id, servings = current.servings)) {
                is SetPlanEntryResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(RecipePickerUiEvent.EntryAdded)
                }

                else -> {
                    _uiState.update { it.copy(isSaving = false, hasError = true) }
                }
            }
        }
    }
}
