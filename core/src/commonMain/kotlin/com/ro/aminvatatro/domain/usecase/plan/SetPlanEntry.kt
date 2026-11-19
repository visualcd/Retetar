package com.ro.aminvatatro.domain.usecase.plan

import com.ro.aminvatatro.core.time.IdGenerator
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.validation.RecipeValidator
import kotlinx.datetime.LocalDate

/**
 * Outcome of adding or updating a [MealPlanEntry].
 */
sealed interface SetPlanEntryResult {
    /** The entry was validated and persisted. */
    data class Success(val entry: MealPlanEntry) : SetPlanEntryResult

    /** Serving count fell outside the inclusive `[min, max]` range. */
    data class InvalidServings(val min: Int, val max: Int, val actual: Int) : SetPlanEntryResult

    /** Referenced recipe [recipeId] does not exist. */
    data class RecipeNotFound(val recipeId: String) : SetPlanEntryResult
}

/**
 * Adds a new [MealPlanEntry] (or updates an existing one when `entryId` is provided)
 * after validating serving bounds and verifying that the target recipe exists.
 */
class SetPlanEntry(
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository,
    private val idGenerator: IdGenerator,
) {
    /**
     * Validates and persists a meal plan entry for [date], [slot], [recipeId], and [servings].
     * Multiple entries can coexist within the same slot.
     */
    suspend operator fun invoke(
        date: LocalDate,
        slot: MealSlot,
        recipeId: String,
        servings: Int,
        entryId: String? = null,
    ): SetPlanEntryResult {
        if (servings !in RecipeValidator.MIN_SERVINGS..RecipeValidator.MAX_SERVINGS) {
            return SetPlanEntryResult.InvalidServings(
                min = RecipeValidator.MIN_SERVINGS,
                max = RecipeValidator.MAX_SERVINGS,
                actual = servings,
            )
        }
        if (recipeId.isBlank() || recipeRepository.getRecipeById(recipeId) == null) {
            return SetPlanEntryResult.RecipeNotFound(recipeId)
        }

        val entry = MealPlanEntry(
            id = entryId?.takeIf { it.isNotBlank() } ?: idGenerator.nextId(),
            date = date,
            slot = slot,
            recipeId = recipeId,
            servings = servings,
        )
        mealPlanRepository.upsertEntry(entry)
        return SetPlanEntryResult.Success(entry)
    }
}
