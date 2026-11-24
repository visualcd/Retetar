package com.ro.aminvatatro.domain.usecase.nutrition

import com.ro.aminvatatro.core.nutrition.NutritionDatabase
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.data.remote.NutritionSearchResult
import com.ro.aminvatatro.data.remote.OpenFoodFactsClient
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.Step
import com.ro.aminvatatro.domain.repository.RecipeFilter
import com.ro.aminvatatro.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NutritionUseCasesTest {

    private val fakeRepo = object : RecipeRepository {
        private val recipes = mutableListOf<Recipe>()

        override fun observeRecipes(filter: RecipeFilter): Flow<List<Recipe>> = flowOf(recipes)
        override fun observeRecipe(id: String): Flow<Recipe?> = flowOf(recipes.firstOrNull { it.id == id })
        override fun observeAllTags() = flowOf(emptyList<com.ro.aminvatatro.domain.model.Tag>())
        override suspend fun getRecipeById(id: String) = recipes.firstOrNull { it.id == id }
        override suspend fun getAllRecipes(): List<Recipe> = recipes.toList()
        override suspend fun saveRecipe(recipe: Recipe) {
            recipes.removeAll { it.id == recipe.id }
            recipes.add(recipe)
        }
        override suspend fun saveRecipes(recipes: List<Recipe>) {
            recipes.forEach { saveRecipe(it) }
        }
        override suspend fun deleteRecipe(id: String) { recipes.removeAll { it.id == id } }
        override suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long) {}
        override suspend fun countMealPlanEntriesForRecipe(recipeId: String): Int = 0
        override suspend fun deleteAllRecipes() { recipes.clear() }
    }

    @Test
    fun nutritionDatabaseCalculatesMassVolumeAndCountCalories() {
        // 500g faina (364 kcal / 100g) = 1820 kcal
        val fainaCals = NutritionDatabase.calculateCalories("Făină de grâu", 500.0, MeasurementUnit.G)
        assertNotNull(fainaCals)
        assertEquals(1820.0, fainaCals, 0.1)

        // 200g (0.2 kg) piept de pui (165 kcal / 100g) = 330 kcal
        val puiCals = NutritionDatabase.calculateCalories("piept de pui", 0.2, MeasurementUnit.KG)
        assertNotNull(puiCals)
        assertEquals(330.0, puiCals, 0.1)

        // 4 oua (55g/ou, 143 kcal / 100g) = 4 * 55 * 1.43 = 314.6 kcal
        val ouaCals = NutritionDatabase.calculateCalories("ouă", 4.0, MeasurementUnit.PIECE)
        assertNotNull(ouaCals)
        assertEquals(314.6, ouaCals, 0.5)

        // 50 ml ulei (0.92 density -> 46g, 884 kcal / 100g) = 406.64 kcal
        val uleiCals = NutritionDatabase.calculateCalories("ulei de măsline", 50.0, MeasurementUnit.ML)
        assertNotNull(uleiCals)
        assertEquals(406.64, uleiCals, 0.5)

        // Sare dupa gust = 0.0 kcal
        val sareCals = NutritionDatabase.calculateCalories("sare", null, MeasurementUnit.TO_TASTE)
        assertEquals(0.0, sareCals)
    }

    @Test
    fun generateRecipesForCaloriesTargetReturnsAtLeastFiveRecipesUnderTarget() = runTest {
        val generator = GenerateRecipesForCaloriesTarget(fakeRepo)

        // Request max 850 kcal per serving
        val suggestions = generator(maxKcalPerServing = 850.0, limit = 5)

        assertEquals(5, suggestions.size)
        suggestions.forEach { suggestion ->
            assertTrue(
                suggestion.caloriesPerServing <= 850.0,
                "Recipe '${suggestion.recipe.title}' exceeded target: ${suggestion.caloriesPerServing} > 850",
            )
            assertTrue(suggestion.recipe.ingredients.isNotEmpty())
            assertTrue(suggestion.recipe.steps.isNotEmpty())
        }
    }

    @Test
    fun generateRecipesPrioritizesMatchingSavedRecipes() = runTest {
        val customRecipe = Recipe(
            id = "custom-1",
            title = "Omletă simplă",
            baseServings = 1,
            ingredients = listOf(
                Ingredient("i-1", 0, "Ouă", 2.0, MeasurementUnit.PIECE, Aisle.DAIRY, 157.3),
            ),
            steps = listOf(Step("s-1", 0, "Bate ouăle și gătește-le în tigaie.")),
            caloriesTotal = 157.3,
        )
        fakeRepo.saveRecipe(customRecipe)

        val generator = GenerateRecipesForCaloriesTarget(fakeRepo)
        val suggestions = generator(maxKcalPerServing = 500.0, limit = 5)

        assertTrue(suggestions.any { it.recipe.id == "custom-1" && !it.isGenerated })
    }
}
