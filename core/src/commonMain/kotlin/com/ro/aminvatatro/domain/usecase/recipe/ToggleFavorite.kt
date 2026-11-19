package com.ro.aminvatatro.domain.usecase.recipe

import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.domain.repository.RecipeRepository

/**
 * Outcome of toggling a recipe's favorite flag.
 */
sealed interface ToggleFavoriteResult {
    /** Favorite status was updated to [isFavorite]. */
    data class Updated(val recipeId: String, val isFavorite: Boolean) : ToggleFavoriteResult

    /** No recipe with [recipeId] was found. */
    data class NotFound(val recipeId: String) : ToggleFavoriteResult
}

/**
 * Toggles the favorite flag of a stored recipe and updates its `updatedAt` timestamp.
 */
class ToggleFavorite(
    private val recipeRepository: RecipeRepository,
    private val clock: Clock,
) {
    /**
     * Flips [com.ro.aminvatatro.domain.model.Recipe.isFavorite] for [recipeId].
     */
    suspend operator fun invoke(recipeId: String): ToggleFavoriteResult {
        if (recipeId.isBlank()) return ToggleFavoriteResult.NotFound(recipeId)
        val recipe = recipeRepository.getRecipeById(recipeId)
            ?: return ToggleFavoriteResult.NotFound(recipeId)
        val newFavorite = !recipe.isFavorite
        recipeRepository.setFavorite(
            id = recipe.id,
            isFavorite = newFavorite,
            updatedAt = clock.nowEpochMillis(),
        )
        return ToggleFavoriteResult.Updated(recipeId = recipe.id, isFavorite = newFavorite)
    }
}
