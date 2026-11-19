package com.ro.aminvatatro.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.Step
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipe
import com.ro.aminvatatro.domain.usecase.recipe.SaveRecipe
import com.ro.aminvatatro.domain.usecase.recipe.SaveRecipeResult
import com.ro.aminvatatro.domain.validation.RecipeValidationError
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Editable ingredient row draft inside the recipe editor.
 */
data class IngredientDraft(
    val localKey: Int,
    val id: String = "",
    val name: String = "",
    val quantityText: String = "",
    val unit: MeasurementUnit = MeasurementUnit.G,
    val aisle: Aisle = Aisle.OTHER,
)

/**
 * Editable preparation step draft inside the recipe editor.
 */
data class StepDraft(
    val localKey: Int,
    val id: String = "",
    val text: String = "",
)

/**
 * UI state for creating or editing a [Recipe] with inline validation errors.
 */
data class RecipeEditorUiState(
    val recipeId: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val title: String = "",
    val description: String = "",
    val baseServingsText: String = "4",
    val prepMinutesText: String = "10",
    val cookMinutesText: String = "20",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = 0L,
    val ingredients: List<IngredientDraft> = listOf(IngredientDraft(localKey = 1)),
    val steps: List<StepDraft> = listOf(StepDraft(localKey = 1)),
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val validationErrors: List<RecipeValidationError> = emptyList(),
    val loadError: Boolean = false,
) {
    val isEditing: Boolean get() = recipeId != null
}

/**
 * One-shot navigation events emitted by [RecipeEditorViewModel].
 */
sealed interface RecipeEditorUiEvent {
    data class Saved(val recipeId: String) : RecipeEditorUiEvent
}

/**
 * ViewModel managing recipe creation, editing, ingredient/step reordering, and inline validation.
 */
class RecipeEditorViewModel(
    private val recipeId: String?,
    private val getRecipe: GetRecipe,
    private val saveRecipe: SaveRecipe,
) : ViewModel() {

    private var nextKeyCounter = 10

    private val _uiState = MutableStateFlow(
        RecipeEditorUiState(
            recipeId = recipeId,
            isLoading = recipeId != null,
        ),
    )
    val uiState: StateFlow<RecipeEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<RecipeEditorUiEvent>(Channel.BUFFERED)
    val events: Flow<RecipeEditorUiEvent> = _events.receiveAsFlow()

    init {
        if (recipeId != null) {
            loadExistingRecipe(recipeId)
        }
    }

    private fun loadExistingRecipe(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = false) }
            try {
                val recipe = getRecipe.getOnce(id)
                if (recipe == null) {
                    _uiState.update { it.copy(isLoading = false, loadError = true) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            recipeId = recipe.id,
                            title = recipe.title,
                            description = recipe.description,
                            baseServingsText = recipe.baseServings.toString(),
                            prepMinutesText = recipe.prepMinutes.toString(),
                            cookMinutesText = recipe.cookMinutes.toString(),
                            notes = recipe.notes,
                            isFavorite = recipe.isFavorite,
                            createdAt = recipe.createdAt,
                            ingredients = recipe.ingredients.map { ing ->
                                IngredientDraft(
                                    localKey = ++nextKeyCounter,
                                    id = ing.id,
                                    name = ing.name,
                                    quantityText = ing.quantity?.let(::formatDraftQuantity) ?: "",
                                    unit = ing.unit,
                                    aisle = ing.aisle,
                                )
                            },
                            steps = recipe.steps.map { step ->
                                StepDraft(
                                    localKey = ++nextKeyCounter,
                                    id = step.id,
                                    text = step.text,
                                )
                            },
                            tags = recipe.tags.map { tag -> tag.name },
                            validationErrors = emptyList(),
                        )
                    }
                }
            } catch (_: Throwable) {
                _uiState.update { it.copy(isLoading = false, loadError = true) }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onBaseServingsChange(value: String) = _uiState.update { it.copy(baseServingsText = value) }
    fun onPrepMinutesChange(value: String) = _uiState.update { it.copy(prepMinutesText = value) }
    fun onCookMinutesChange(value: String) = _uiState.update { it.copy(cookMinutesText = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }
    fun onTagInputChange(value: String) = _uiState.update { it.copy(tagInput = value) }

    fun addIngredient() {
        _uiState.update { state ->
            state.copy(ingredients = state.ingredients + IngredientDraft(localKey = ++nextKeyCounter))
        }
    }

    fun updateIngredient(
        index: Int,
        name: String? = null,
        quantityText: String? = null,
        unit: MeasurementUnit? = null,
        aisle: Aisle? = null,
    ) {
        _uiState.update { state ->
            if (index !in state.ingredients.indices) return@update state
            val updated = state.ingredients.toMutableList()
            val current = updated[index]
            val resolvedUnit = unit ?: current.unit
            val resolvedQty = when {
                resolvedUnit == MeasurementUnit.TO_TASTE -> ""
                quantityText != null -> quantityText
                else -> current.quantityText
            }
            updated[index] = current.copy(
                name = name ?: current.name,
                quantityText = resolvedQty,
                unit = resolvedUnit,
                aisle = aisle ?: current.aisle,
            )
            state.copy(ingredients = updated)
        }
    }

    fun removeIngredient(index: Int) {
        _uiState.update { state ->
            if (index !in state.ingredients.indices) return@update state
            state.copy(ingredients = state.ingredients.filterIndexed { i, _ -> i != index })
        }
    }

    fun moveIngredientUp(index: Int) {
        _uiState.update { state ->
            if (index <= 0 || index !in state.ingredients.indices) return@update state
            val list = state.ingredients.toMutableList()
            val item = list.removeAt(index)
            list.add(index - 1, item)
            state.copy(ingredients = list)
        }
    }

    fun moveIngredientDown(index: Int) {
        _uiState.update { state ->
            if (index !in state.ingredients.indices || index >= state.ingredients.lastIndex) return@update state
            val list = state.ingredients.toMutableList()
            val item = list.removeAt(index)
            list.add(index + 1, item)
            state.copy(ingredients = list)
        }
    }

    fun addStep() {
        _uiState.update { state ->
            state.copy(steps = state.steps + StepDraft(localKey = ++nextKeyCounter))
        }
    }

    fun updateStep(index: Int, text: String) {
        _uiState.update { state ->
            if (index !in state.steps.indices) return@update state
            val list = state.steps.toMutableList()
            list[index] = list[index].copy(text = text)
            state.copy(steps = list)
        }
    }

    fun removeStep(index: Int) {
        _uiState.update { state ->
            if (index !in state.steps.indices) return@update state
            state.copy(steps = state.steps.filterIndexed { i, _ -> i != index })
        }
    }

    fun moveStepUp(index: Int) {
        _uiState.update { state ->
            if (index <= 0 || index !in state.steps.indices) return@update state
            val list = state.steps.toMutableList()
            val item = list.removeAt(index)
            list.add(index - 1, item)
            state.copy(steps = list)
        }
    }

    fun moveStepDown(index: Int) {
        _uiState.update { state ->
            if (index !in state.steps.indices || index >= state.steps.lastIndex) return@update state
            val list = state.steps.toMutableList()
            val item = list.removeAt(index)
            list.add(index + 1, item)
            state.copy(steps = list)
        }
    }

    fun addTag() {
        _uiState.update { state ->
            val trimmed = state.tagInput.trim()
            if (trimmed.isEmpty() || state.tags.any { it.equals(trimmed, ignoreCase = true) }) {
                state.copy(tagInput = "")
            } else {
                state.copy(tags = state.tags + trimmed, tagInput = "")
            }
        }
    }

    fun removeTag(tag: String) {
        _uiState.update { state ->
            state.copy(tags = state.tags.filterNot { it == tag })
        }
    }

    fun save() {
        val current = _uiState.value
        if (current.isSaving) return

        val candidate = buildCandidateRecipe(current)
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = saveRecipe(candidate)) {
                is SaveRecipeResult.Success -> {
                    _uiState.update {
                        it.copy(isSaving = false, validationErrors = emptyList())
                    }
                    _events.send(RecipeEditorUiEvent.Saved(result.recipe.id))
                }

                is SaveRecipeResult.ValidationFailed -> {
                    _uiState.update {
                        it.copy(isSaving = false, validationErrors = result.errors)
                    }
                }
            }
        }
    }

    private fun buildCandidateRecipe(state: RecipeEditorUiState): Recipe {
        val servings = state.baseServingsText.trim().toIntOrNull() ?: 0
        val prep = state.prepMinutesText.trim().ifEmpty { "0" }.toIntOrNull() ?: -1
        val cook = state.cookMinutesText.trim().ifEmpty { "0" }.toIntOrNull() ?: -1

        val ingredients = state.ingredients.mapIndexed { index, draft ->
            val parsedQuantity = if (draft.unit == MeasurementUnit.TO_TASTE) {
                parseNullableDouble(draft.quantityText)
            } else {
                parseNullableDouble(draft.quantityText)
            }
            Ingredient(
                id = draft.id,
                position = index,
                name = draft.name,
                quantity = parsedQuantity,
                unit = draft.unit,
                aisle = draft.aisle,
            )
        }

        val steps = state.steps.mapIndexed { index, draft ->
            Step(
                id = draft.id,
                position = index,
                text = draft.text,
            )
        }

        val tags = state.tags.map { Tag(id = "", name = it) }

        return Recipe(
            id = state.recipeId.orEmpty(),
            title = state.title,
            description = state.description,
            baseServings = servings,
            prepMinutes = prep,
            cookMinutes = cook,
            ingredients = ingredients,
            steps = steps,
            tags = tags,
            notes = state.notes,
            isFavorite = state.isFavorite,
            createdAt = state.createdAt,
        )
    }

    private fun parseNullableDouble(raw: String): Double? {
        val cleaned = raw.trim().replace(',', '.')
        if (cleaned.isEmpty()) return null
        return cleaned.toDoubleOrNull() ?: Double.NaN
    }

    private fun formatDraftQuantity(value: Double): String {
        val asLong = value.toLong()
        return if (asLong.toDouble() == value) asLong.toString() else value.toString()
    }
}
