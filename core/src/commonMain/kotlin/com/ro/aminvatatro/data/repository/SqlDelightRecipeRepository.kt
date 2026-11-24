package com.ro.aminvatatro.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.ro.aminvatatro.core.text.TextNormalizer
import com.ro.aminvatatro.data.local.db.RetetarDatabase
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.repository.RecipeFilter
import com.ro.aminvatatro.domain.repository.RecipeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * SQLDelight-backed implementation of [RecipeRepository] used on Android, iOS, and Desktop (JVM).
 */
class SqlDelightRecipeRepository(
    private val database: RetetarDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RecipeRepository {

    private val queries get() = database.retetarQueries

    override fun observeRecipes(filter: RecipeFilter): Flow<List<Recipe>> {
        val normalizedQuery = TextNormalizer.normalize(filter.query)
        val queryPattern = if (normalizedQuery.isEmpty()) "" else "%$normalizedQuery%"
        val favoritesFlag = if (filter.favoritesOnly) 1L else 0L
        val maxMinutes = filter.maxTotalMinutes?.toLong()
        val requiredTags = filter.tagNames
            .map { TextNormalizer.normalize(it) }
            .filter { it.isNotEmpty() }
            .toSet()

        val recipesFlow = queries.searchAndFilterRecipes(
            queryPattern = queryPattern,
            favoritesOnly = favoritesFlag,
            maxTotalMinutes = maxMinutes,
        ).asFlow().mapToList(dispatcher)

        val ingredientsFlow = queries.selectAllIngredients().asFlow().mapToList(dispatcher)
        val stepsFlow = queries.selectAllSteps().asFlow().mapToList(dispatcher)
        val tagsFlow = queries.selectAllRecipeTagRows().asFlow().mapToList(dispatcher)

        return combine(recipesFlow, ingredientsFlow, stepsFlow, tagsFlow) { recipes, ingredients, steps, tagRows ->
            val ingredientsByRecipe = ingredients.groupBy { it.recipeId }
            val stepsByRecipe = steps.groupBy { it.recipeId }
            val tagsByRecipe = tagRows.groupBy(
                keySelector = { it.recipeId },
                valueTransform = { Tag(id = it.tagId, name = it.tagName) },
            )

            recipes.map { dbRecipe ->
                dbRecipe.toDomain(
                    ingredients = ingredientsByRecipe[dbRecipe.id].orEmpty().map { it.toDomain() },
                    steps = stepsByRecipe[dbRecipe.id].orEmpty().map { it.toDomain() },
                    tags = tagsByRecipe[dbRecipe.id].orEmpty(),
                )
            }.let { list ->
                if (requiredTags.isEmpty()) {
                    list
                } else {
                    list.filter { recipe ->
                        val recipeTagNorms = recipe.tags.map { TextNormalizer.normalize(it.name) }.toSet()
                        requiredTags.all { it in recipeTagNorms }
                    }
                }
            }
        }
    }

    override fun observeRecipe(id: String): Flow<Recipe?> {
        val recipeFlow = queries.selectRecipeById(id).asFlow().mapToOneOrNull(dispatcher)
        val ingredientsFlow = queries.selectIngredientsByRecipeId(id).asFlow().mapToList(dispatcher)
        val stepsFlow = queries.selectStepsByRecipeId(id).asFlow().mapToList(dispatcher)
        val tagsFlow = queries.selectTagsByRecipeId(id).asFlow().mapToList(dispatcher)

        return combine(recipeFlow, ingredientsFlow, stepsFlow, tagsFlow) { recipe, ingredients, steps, tags ->
            recipe?.toDomain(
                ingredients = ingredients.map { it.toDomain() },
                steps = steps.map { it.toDomain() },
                tags = tags.map { Tag(id = it.id, name = it.name) },
            )
        }
    }

    override fun observeAllTags(): Flow<List<Tag>> =
        queries.selectAllTags().asFlow().mapToList(dispatcher).map { rows ->
            rows.map { it.toDomain() }
        }

    override suspend fun getRecipeById(id: String): Recipe? = withContext(dispatcher) {
        val row = queries.selectRecipeById(id).executeAsOneOrNull() ?: return@withContext null
        val ingredients = queries.selectIngredientsByRecipeId(id).executeAsList().map { it.toDomain() }
        val steps = queries.selectStepsByRecipeId(id).executeAsList().map { it.toDomain() }
        val tags = queries.selectTagsByRecipeId(id).executeAsList().map { Tag(id = it.id, name = it.name) }
        row.toDomain(ingredients = ingredients, steps = steps, tags = tags)
    }

    override suspend fun getAllRecipes(): List<Recipe> = withContext(dispatcher) {
        val recipes = queries.selectAllRecipes().executeAsList()
        val ingredientsByRecipe = queries.selectAllIngredients().executeAsList().groupBy { it.recipeId }
        val stepsByRecipe = queries.selectAllSteps().executeAsList().groupBy { it.recipeId }
        val tagsByRecipe = queries.selectAllRecipeTagRows().executeAsList().groupBy(
            keySelector = { it.recipeId },
            valueTransform = { Tag(id = it.tagId, name = it.tagName) },
        )
        recipes.map { dbRecipe ->
            dbRecipe.toDomain(
                ingredients = ingredientsByRecipe[dbRecipe.id].orEmpty().map { it.toDomain() },
                steps = stepsByRecipe[dbRecipe.id].orEmpty().map { it.toDomain() },
                tags = tagsByRecipe[dbRecipe.id].orEmpty(),
            )
        }
    }

    override suspend fun saveRecipe(recipe: Recipe) = withContext(dispatcher) {
        database.transaction {
            saveRecipeInTransaction(recipe)
            queries.deleteUnusedTags()
        }
    }

    override suspend fun saveRecipes(recipes: List<Recipe>) = withContext(dispatcher) {
        database.transaction {
            recipes.forEach { saveRecipeInTransaction(it) }
            queries.deleteUnusedTags()
        }
    }

    override suspend fun deleteRecipe(id: String) = withContext(dispatcher) {
        database.transaction {
            queries.deleteRecipeById(id)
            queries.deleteUnusedTags()
        }
    }

    override suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long) {
        withContext(dispatcher) {
            queries.updateRecipeFavorite(
                isFavorite = if (isFavorite) 1L else 0L,
                updatedAt = updatedAt,
                id = id,
            )
        }
    }

    override suspend fun countMealPlanEntriesForRecipe(recipeId: String): Int = withContext(dispatcher) {
        queries.countMealPlanByRecipeId(recipeId).executeAsOne().toInt()
    }

    override suspend fun deleteAllRecipes() = withContext(dispatcher) {
        database.transaction {
            queries.deleteAllRecipes()
            queries.deleteUnusedTags()
        }
    }

    private fun saveRecipeInTransaction(recipe: Recipe) {
        val trimmedTitle = recipe.title.trim()
        val normalizedTitle = TextNormalizer.normalize(trimmedTitle)
        val trimmedDescription = recipe.description.trim()
        val trimmedNotes = recipe.notes.trim()
        val favoriteFlag = if (recipe.isFavorite) 1L else 0L

        queries.insertRecipeIfAbsent(
            id = recipe.id,
            title = trimmedTitle,
            normalizedTitle = normalizedTitle,
            description = trimmedDescription,
            baseServings = recipe.baseServings.toLong(),
            prepMinutes = recipe.prepMinutes.toLong(),
            cookMinutes = recipe.cookMinutes.toLong(),
            notes = trimmedNotes,
            isFavorite = favoriteFlag,
            createdAt = recipe.createdAt,
            updatedAt = recipe.updatedAt,
            caloriesTotal = recipe.effectiveCaloriesTotal,
        )
        queries.updateRecipe(
            title = trimmedTitle,
            normalizedTitle = normalizedTitle,
            description = trimmedDescription,
            baseServings = recipe.baseServings.toLong(),
            prepMinutes = recipe.prepMinutes.toLong(),
            cookMinutes = recipe.cookMinutes.toLong(),
            notes = trimmedNotes,
            isFavorite = favoriteFlag,
            updatedAt = recipe.updatedAt,
            caloriesTotal = recipe.effectiveCaloriesTotal,
            id = recipe.id,
        )

        queries.deleteIngredientsByRecipeId(recipe.id)
        recipe.ingredients.forEachIndexed { index, ingredient ->
            val trimmedName = ingredient.name.trim()
            queries.insertIngredient(
                id = ingredient.id,
                recipeId = recipe.id,
                position = index.toLong(),
                name = trimmedName,
                normalizedName = TextNormalizer.normalize(trimmedName),
                quantity = ingredient.quantity,
                unit = ingredient.unit.name,
                aisle = ingredient.aisle.name,
                calories = ingredient.calories,
            )
        }

        queries.deleteStepsByRecipeId(recipe.id)
        recipe.steps.forEachIndexed { index, step ->
            queries.insertStep(
                id = step.id,
                recipeId = recipe.id,
                position = index.toLong(),
                text = step.text.trim(),
            )
        }

        queries.deleteRecipeTagsByRecipeId(recipe.id)
        val seenTagNames = mutableSetOf<String>()
        recipe.tags.forEach { tag ->
            val trimmedTagName = tag.name.trim()
            if (trimmedTagName.isNotEmpty() && seenTagNames.add(trimmedTagName)) {
                val existing = queries.selectTagByName(trimmedTagName).executeAsOneOrNull()
                val resolvedTagId = if (existing != null) {
                    existing.id
                } else {
                    queries.insertOrIgnoreTag(id = tag.id, name = trimmedTagName)
                    queries.selectTagByName(trimmedTagName).executeAsOne().id
                }
                queries.insertOrIgnoreRecipeTag(recipeId = recipe.id, tagId = resolvedTagId)
            }
        }
    }
}
