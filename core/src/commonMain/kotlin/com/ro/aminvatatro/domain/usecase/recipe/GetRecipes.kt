package com.ro.aminvatatro.domain.usecase.recipe

import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.repository.RecipeFilter
import com.ro.aminvatatro.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes stored recipes matching the given [RecipeFilter].
 */
class GetRecipes(
    private val recipeRepository: RecipeRepository,
) {
    /**
     * Returns a reactive stream of recipes filtered and searched according to [filter].
     */
    operator fun invoke(filter: RecipeFilter = RecipeFilter()): Flow<List<Recipe>> =
        recipeRepository.observeRecipes(filter)
}
