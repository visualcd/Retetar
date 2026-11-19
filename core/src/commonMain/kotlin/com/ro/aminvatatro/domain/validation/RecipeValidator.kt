package com.ro.aminvatatro.domain.validation

import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Recipe

/**
 * Typed domain validation errors produced by [RecipeValidator].
 */
sealed interface RecipeValidationError {
    /** Recipe title is empty or blank. */
    data object TitleBlank : RecipeValidationError

    /** Recipe title exceeds [maxLength] characters. */
    data class TitleTooLong(val maxLength: Int, val actualLength: Int) : RecipeValidationError

    /** Recipe base servings are outside the inclusive `[min, max]` range. */
    data class InvalidServings(val min: Int, val max: Int, val actual: Int) : RecipeValidationError

    /** Preparation time is outside `[min, max]` minutes. */
    data class InvalidPrepMinutes(val min: Int, val max: Int, val actual: Int) : RecipeValidationError

    /** Cooking time is outside `[min, max]` minutes. */
    data class InvalidCookMinutes(val min: Int, val max: Int, val actual: Int) : RecipeValidationError

    /** Recipe has no ingredients. */
    data object EmptyIngredients : RecipeValidationError

    /** Ingredient at [index] has a blank name. */
    data class IngredientNameBlank(val index: Int) : RecipeValidationError

    /** Ingredient at [index] has a missing, non-positive, or non-finite quantity for a unit requiring one. */
    data class InvalidIngredientQuantity(val index: Int) : RecipeValidationError

    /** Ingredient at [index] uses [MeasurementUnit.TO_TASTE] but provides a non-null quantity. */
    data class QuantityNotAllowedForToTaste(val index: Int) : RecipeValidationError

    /** Recipe has no preparation steps. */
    data object EmptySteps : RecipeValidationError

    /** Step at [index] has blank text. */
    data class StepTextBlank(val index: Int) : RecipeValidationError
}

/**
 * Outcome of validating a [Recipe].
 */
sealed interface ValidationResult {
    /** Validation passed with no errors. */
    data object Valid : ValidationResult

    /** Validation failed with one or more typed [errors]. */
    data class Invalid(val errors: List<RecipeValidationError>) : ValidationResult

    /** `true` if validation succeeded. */
    val isValid: Boolean
        get() = this is Valid

    /** Returns the list of errors, or an empty list when [Valid]. */
    fun errorsOrEmpty(): List<RecipeValidationError> = when (this) {
        Valid -> emptyList()
        is Invalid -> errors
    }
}

/**
 * Validates a [Recipe] against all domain invariants defined in the specification:
 * - Title length: 1..100 characters (trimmed)
 * - Base servings: 1..100
 * - Prep & cook time: 0..1440 minutes each
 * - At least 1 ingredient (non-blank name; quantity > 0 and finite, `null` only for [MeasurementUnit.TO_TASTE])
 * - At least 1 step (non-blank text)
 */
object RecipeValidator {

    const val MIN_TITLE_LENGTH: Int = 1
    const val MAX_TITLE_LENGTH: Int = 100
    const val MIN_SERVINGS: Int = 1
    const val MAX_SERVINGS: Int = 100
    const val MIN_MINUTES: Int = 0
    const val MAX_MINUTES: Int = 1440

    /**
     * Validates [recipe] and collects every violated rule into [ValidationResult.Invalid],
     * or returns [ValidationResult.Valid] if all rules pass.
     */
    fun validate(recipe: Recipe): ValidationResult {
        val errors = mutableListOf<RecipeValidationError>()

        val trimmedTitle = recipe.title.trim()
        when {
            trimmedTitle.length < MIN_TITLE_LENGTH -> errors += RecipeValidationError.TitleBlank
            trimmedTitle.length > MAX_TITLE_LENGTH -> errors += RecipeValidationError.TitleTooLong(
                maxLength = MAX_TITLE_LENGTH,
                actualLength = trimmedTitle.length,
            )
        }

        if (recipe.baseServings !in MIN_SERVINGS..MAX_SERVINGS) {
            errors += RecipeValidationError.InvalidServings(
                min = MIN_SERVINGS,
                max = MAX_SERVINGS,
                actual = recipe.baseServings,
            )
        }

        if (recipe.prepMinutes !in MIN_MINUTES..MAX_MINUTES) {
            errors += RecipeValidationError.InvalidPrepMinutes(
                min = MIN_MINUTES,
                max = MAX_MINUTES,
                actual = recipe.prepMinutes,
            )
        }

        if (recipe.cookMinutes !in MIN_MINUTES..MAX_MINUTES) {
            errors += RecipeValidationError.InvalidCookMinutes(
                min = MIN_MINUTES,
                max = MAX_MINUTES,
                actual = recipe.cookMinutes,
            )
        }

        if (recipe.ingredients.isEmpty()) {
            errors += RecipeValidationError.EmptyIngredients
        } else {
            recipe.ingredients.forEachIndexed { index, ingredient ->
                if (ingredient.name.isBlank()) {
                    errors += RecipeValidationError.IngredientNameBlank(index)
                }
                if (ingredient.unit == MeasurementUnit.TO_TASTE) {
                    if (ingredient.quantity != null) {
                        errors += RecipeValidationError.QuantityNotAllowedForToTaste(index)
                    }
                } else {
                    val qty = ingredient.quantity
                    if (qty == null || !qty.isFinite() || qty <= 0.0) {
                        errors += RecipeValidationError.InvalidIngredientQuantity(index)
                    }
                }
            }
        }

        if (recipe.steps.isEmpty()) {
            errors += RecipeValidationError.EmptySteps
        } else {
            recipe.steps.forEachIndexed { index, step ->
                if (step.text.isBlank()) {
                    errors += RecipeValidationError.StepTextBlank(index)
                }
            }
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }
}
