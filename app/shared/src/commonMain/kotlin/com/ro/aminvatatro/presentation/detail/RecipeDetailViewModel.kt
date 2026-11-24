package com.ro.aminvatatro.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.core.time.WeekUtils
import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.repository.SettingsRepository
import com.ro.aminvatatro.domain.usecase.plan.SetPlanEntry
import com.ro.aminvatatro.domain.usecase.plan.SetPlanEntryResult
import com.ro.aminvatatro.domain.usecase.recipe.DeleteRecipe
import com.ro.aminvatatro.domain.usecase.recipe.DeleteRecipeResult
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipe
import com.ro.aminvatatro.domain.usecase.recipe.ScaleRecipe
import com.ro.aminvatatro.domain.usecase.recipe.ScaleRecipeResult
import com.ro.aminvatatro.domain.usecase.recipe.ScaledIngredient
import com.ro.aminvatatro.domain.usecase.recipe.ToggleFavorite
import com.ro.aminvatatro.domain.usecase.shopping.AddRecipeToShoppingList
import com.ro.aminvatatro.domain.validation.RecipeValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * UI state for the Recipe Detail screen with live serving scaling and delete confirmation.
 */
data class RecipeDetailUiState(
    val isLoading: Boolean = true,
    val recipe: Recipe? = null,
    val selectedServings: Int = 1,
    val scaledIngredients: List<ScaledIngredient> = emptyList(),
    val language: AppLanguage = AppLanguage.RO,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val showDeleteDialog: Boolean = false,
    val affectedPlanEntriesCount: Int = 0,
    val showAddToPlanDialog: Boolean = false,
    val currentWeekDays: List<LocalDate> = emptyList(),
    val selectedPlanDate: LocalDate? = null,
    val selectedPlanSlot: MealSlot = MealSlot.LUNCH,
    val hasError: Boolean = false,
)

/**
 * One-shot events emitted by [RecipeDetailViewModel].
 */
sealed interface RecipeDetailUiEvent {
    data object Deleted : RecipeDetailUiEvent
    data object AddedToPlan : RecipeDetailUiEvent
    data object AddedToShoppingList : RecipeDetailUiEvent
}

/**
 * ViewModel for viewing a single recipe, scaling portions, toggling favorite, adding to the
 * weekly meal plan, and deleting with affected meal-plan confirmation.
 */
class RecipeDetailViewModel(
    private val recipeId: String,
    private val getRecipe: GetRecipe,
    private val scaleRecipe: ScaleRecipe,
    private val toggleFavorite: ToggleFavorite,
    private val deleteRecipe: DeleteRecipe,
    private val setPlanEntry: SetPlanEntry,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val addRecipeToShoppingList: AddRecipeToShoppingList? = null,
) : ViewModel() {

    private val selectedServingsFlow = MutableStateFlow<Int?>(null)

    private val _uiState = MutableStateFlow(
        RecipeDetailUiState(
            currentWeekDays = WeekUtils.weekDaysOf(clock.today()),
            selectedPlanDate = clock.today(),
        ),
    )
    val uiState: StateFlow<RecipeDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<RecipeDetailUiEvent>(Channel.BUFFERED)
    val events: Flow<RecipeDetailUiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                getRecipe(recipeId),
                settingsRepository.settings,
                selectedServingsFlow,
            ) { recipe, settings, overrideServings ->
                Triple(recipe, settings, overrideServings)
            }
                .catch {
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
                }
                .collect { (recipe, settings, overrideServings) ->
                    if (recipe == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                recipe = null,
                                scaledIngredients = emptyList(),
                                language = settings.language,
                                unitSystem = settings.unitSystem,
                                hasError = false,
                            )
                        }
                    } else {
                        val effectiveServings = (overrideServings ?: recipe.baseServings)
                            .coerceIn(RecipeValidator.MIN_SERVINGS, RecipeValidator.MAX_SERVINGS)
                        val scaled = when (
                            val res = scaleRecipe(
                                recipe = recipe,
                                targetServings = effectiveServings,
                                displayUnitSystem = settings.unitSystem,
                            )
                        ) {
                            is ScaleRecipeResult.Success -> res.ingredients
                            is ScaleRecipeResult.InvalidServings -> emptyList()
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                recipe = recipe,
                                selectedServings = effectiveServings,
                                scaledIngredients = scaled,
                                language = settings.language,
                                unitSystem = settings.unitSystem,
                                hasError = false,
                            )
                        }
                    }
                }
        }
    }

    fun onServingsChange(servings: Int) {
        if (servings in RecipeValidator.MIN_SERVINGS..RecipeValidator.MAX_SERVINGS) {
            selectedServingsFlow.value = servings
        }
    }

    fun onToggleFavorite() {
        viewModelScope.launch {
            toggleFavorite(recipeId)
        }
    }

    fun requestDelete() {
        viewModelScope.launch {
            val count = deleteRecipe.countAffectedPlanEntries(recipeId)
            _uiState.update {
                it.copy(
                    showDeleteDialog = true,
                    affectedPlanEntriesCount = count,
                )
            }
        }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun confirmDelete() {
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteDialog = false) }
            when (deleteRecipe(recipeId)) {
                is DeleteRecipeResult.Deleted -> _events.send(RecipeDetailUiEvent.Deleted)
                is DeleteRecipeResult.NotFound -> _uiState.update { it.copy(hasError = true) }
            }
        }
    }

    fun openAddToPlanDialog() {
        val today = clock.today()
        _uiState.update {
            it.copy(
                showAddToPlanDialog = true,
                currentWeekDays = WeekUtils.weekDaysOf(today),
                selectedPlanDate = it.selectedPlanDate ?: today,
            )
        }
    }

    fun dismissAddToPlanDialog() {
        _uiState.update { it.copy(showAddToPlanDialog = false) }
    }

    fun selectPlanDate(date: LocalDate) {
        _uiState.update { it.copy(selectedPlanDate = date) }
    }

    fun selectPlanSlot(slot: MealSlot) {
        _uiState.update { it.copy(selectedPlanSlot = slot) }
    }

    fun confirmAddToPlan() {
        val state = _uiState.value
        val targetDate = state.selectedPlanDate ?: clock.today()
        viewModelScope.launch {
            val result = setPlanEntry(
                date = targetDate,
                slot = state.selectedPlanSlot,
                recipeId = recipeId,
                servings = state.selectedServings,
            )
            _uiState.update { it.copy(showAddToPlanDialog = false) }
            if (result is SetPlanEntryResult.Success) {
                _events.send(RecipeDetailUiEvent.AddedToPlan)
            } else {
                _uiState.update { it.copy(hasError = true) }
            }
        }
    }

    fun addIngredientsToShoppingList() {
        val currentRecipe = _uiState.value.recipe ?: return
        val servings = _uiState.value.selectedServings
        val adder = addRecipeToShoppingList ?: return
        viewModelScope.launch {
            try {
                adder(
                    recipe = currentRecipe,
                    servings = servings,
                    targetDate = clock.today(),
                )
                _events.send(RecipeDetailUiEvent.AddedToShoppingList)
            } catch (_: Throwable) {
                _uiState.update { it.copy(hasError = true) }
            }
        }
    }
}
