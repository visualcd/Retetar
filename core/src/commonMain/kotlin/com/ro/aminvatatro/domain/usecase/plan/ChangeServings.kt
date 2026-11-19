package com.ro.aminvatatro.domain.usecase.plan

import com.ro.aminvatatro.domain.repository.MealPlanRepository
import com.ro.aminvatatro.domain.validation.RecipeValidator

/**
 * Outcome of updating the serving count on an existing meal plan entry.
 */
sealed interface ChangeServingsResult {
    /** Servings were updated successfully. */
    data class Updated(val entryId: String, val servings: Int) : ChangeServingsResult

    /** Requested [actual] servings were outside `[min, max]`. */
    data class InvalidServings(val min: Int, val max: Int, val actual: Int) : ChangeServingsResult

    /** Entry identifier was blank. */
    data object InvalidEntryId : ChangeServingsResult
}

/**
 * Validates and updates the serving count of a scheduled meal plan entry.
 */
class ChangeServings(
    private val mealPlanRepository: MealPlanRepository,
) {
    /**
     * Updates [entryId] to use [servings] portions when within `1..100`.
     */
    suspend operator fun invoke(entryId: String, servings: Int): ChangeServingsResult {
        if (entryId.isBlank()) return ChangeServingsResult.InvalidEntryId
        if (servings !in RecipeValidator.MIN_SERVINGS..RecipeValidator.MAX_SERVINGS) {
            return ChangeServingsResult.InvalidServings(
                min = RecipeValidator.MIN_SERVINGS,
                max = RecipeValidator.MAX_SERVINGS,
                actual = servings,
            )
        }
        mealPlanRepository.updateServings(entryId = entryId, servings = servings)
        return ChangeServingsResult.Updated(entryId = entryId, servings = servings)
    }
}
