package com.ro.aminvatatro.domain.usecase.recipe

import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow

/**
 * Retrieves or observes a single [Recipe] by identifier.
 */
class GetRecipe(
    private val recipeRepository: RecipeRepository,
) {
    /**
     * Observes the recipe identified by [id], emitting `null` when absent.
     */
    operator fun invoke(id: String): Flow<Recipe?> = recipeRepository.observeRecipe(id)

    /**
     * Loads a single snapshot of the recipe identified by [id], or `null` if not found.
     */
    suspend fun getOnce(id: String): Recipe? =
        if (id.isBlank()) null else recipeRepository.getRecipeById(id)
}
