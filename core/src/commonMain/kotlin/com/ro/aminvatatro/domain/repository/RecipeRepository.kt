package com.ro.aminvatatro.domain.repository

import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Persistence contract for [Recipe] aggregates, including ingredients, steps, and tags.
 */
interface RecipeRepository {
    /** Observes all recipes matching [filter], emitting updates whenever underlying data changes. */
    fun observeRecipes(filter: RecipeFilter = RecipeFilter()): Flow<List<Recipe>>

    /** Observes a single recipe by [id], emitting `null` if it does not exist. */
    fun observeRecipe(id: String): Flow<Recipe?>

    /** Observes all distinct tags currently associated with stored recipes. */
    fun observeAllTags(): Flow<List<Tag>>

    /** Loads a single recipe by [id], or `null` if absent. */
    suspend fun getRecipeById(id: String): Recipe?

    /** Loads all stored recipes. */
    suspend fun getAllRecipes(): List<Recipe>

    /** Saves (inserts or updates) [recipe] together with its ingredients, steps, and tags atomically. */
    suspend fun saveRecipe(recipe: Recipe)

    /** Saves all [recipes] atomically inside a single transaction. */
    suspend fun saveRecipes(recipes: List<Recipe>)

    /** Deletes the recipe with [id] and cascades removal of dependent records (ingredients, steps, tags, plan entries). */
    suspend fun deleteRecipe(id: String)

    /** Toggles the favorite flag of recipe [id] and records [updatedAt]. */
    suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long)

    /** Returns how many meal plan entries currently reference [recipeId]. */
    suspend fun countMealPlanEntriesForRecipe(recipeId: String): Int

    /** Deletes all recipes and cascades removal of dependent records. */
    suspend fun deleteAllRecipes()
}
