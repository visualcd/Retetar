package com.ro.aminvatatro.domain.usecase.recipe

import com.ro.aminvatatro.domain.repository.RecipeRepository

/**
 * Outcome of deleting a [com.ro.aminvatatro.domain.model.Recipe].
 */
sealed interface DeleteRecipeResult {
    /**
     * The recipe was deleted along with [affectedPlanEntriesCount] cascading meal plan entries.
     */
    data class Deleted(
        val recipeId: String,
        val affectedPlanEntriesCount: Int,
    ) : DeleteRecipeResult

    /** No recipe with [recipeId] existed. */
    data class NotFound(val recipeId: String) : DeleteRecipeResult
}

/**
 * Deletes a recipe and reports how many meal plan entries are affected by the cascading delete.
 */
class DeleteRecipe(
    private val recipeRepository: RecipeRepository,
) {
    /**
     * Returns the number of meal plan entries currently referencing [recipeId],
     * useful for displaying an informed confirmation dialog prior to deletion.
     */
    suspend fun countAffectedPlanEntries(recipeId: String): Int =
        if (recipeId.isBlank()) 0 else recipeRepository.countMealPlanEntriesForRecipe(recipeId)

    /**
     * Deletes the recipe identified by [recipeId] (cascading to ingredients, steps, tags,
     * and dependent meal plan entries) and returns how many meal plan entries were removed.
     */
    suspend operator fun invoke(recipeId: String): DeleteRecipeResult {
        if (recipeId.isBlank()) return DeleteRecipeResult.NotFound(recipeId)
        val existing = recipeRepository.getRecipeById(recipeId)
            ?: return DeleteRecipeResult.NotFound(recipeId)
        val affectedCount = recipeRepository.countMealPlanEntriesForRecipe(existing.id)
        recipeRepository.deleteRecipe(existing.id)
        return DeleteRecipeResult.Deleted(
            recipeId = existing.id,
            affectedPlanEntriesCount = affectedCount,
        )
    }
}
