package com.ro.aminvatatro.domain.usecase

import app.cash.turbine.test
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.model.Step
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.repository.RecipeFilter
import com.ro.aminvatatro.domain.usecase.recipe.DeleteRecipe
import com.ro.aminvatatro.domain.usecase.recipe.DeleteRecipeResult
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipe
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipes
import com.ro.aminvatatro.domain.usecase.recipe.SaveRecipe
import com.ro.aminvatatro.domain.usecase.recipe.SaveRecipeResult
import com.ro.aminvatatro.domain.usecase.recipe.ScaleRecipe
import com.ro.aminvatatro.domain.usecase.recipe.ScaleRecipeResult
import com.ro.aminvatatro.domain.usecase.recipe.ToggleFavorite
import com.ro.aminvatatro.domain.usecase.recipe.ToggleFavoriteResult
import com.ro.aminvatatro.domain.validation.RecipeValidationError
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecipeUseCasesTest {

    @Test
    fun saveRecipeAssignsIdsPositionsTimestampsAndPreservesCreatedAtOnUpdate() = runTest {
        val repo = FakeRecipeRepository()
        val clock = FakeClock(currentMillis = 5_000L)
        val ids = SequenceIdGenerator("gen")
        val saveRecipe = SaveRecipe(repo, clock, ids)

        val draft = sampleRecipe(
            id = "",
            title = "  Omletă cu legume  ",
            ingredients = listOf(
                Ingredient("", 99, "  Ouă  ", 3.0, MeasurementUnit.PIECE, Aisle.DAIRY),
                Ingredient("", 42, "Sare", null, MeasurementUnit.TO_TASTE, Aisle.SPICES),
            ),
            steps = listOf(
                Step("", 10, "  Bate ouăle.  "),
            ),
            tags = listOf(Tag("", "Mic dejun"), Tag("", "mic dejun"), Tag("", "   ")),
            createdAt = 0L,
            updatedAt = 0L,
        )

        val created = assertIs<SaveRecipeResult.Success>(saveRecipe(draft)).recipe
        assertEquals("gen-1", created.id)
        assertEquals("Omletă cu legume", created.title)
        assertEquals(listOf(0, 1), created.ingredients.map { it.position })
        assertEquals("Ouă", created.ingredients[0].name)
        assertEquals(listOf(0), created.steps.map { it.position })
        assertEquals(listOf("Mic dejun"), created.tags.map { it.name })
        assertEquals(5_000L, created.createdAt)
        assertEquals(5_000L, created.updatedAt)

        clock.currentMillis = 9_000L
        val updated = assertIs<SaveRecipeResult.Success>(
            saveRecipe(created.copy(title = "Omletă simplă")),
        ).recipe
        assertEquals(created.id, updated.id)
        assertEquals(5_000L, updated.createdAt)
        assertEquals(9_000L, updated.updatedAt)
        assertEquals("Omletă simplă", repo.getRecipeById(created.id)?.title)
    }

    @Test
    fun saveRecipeRejectsInvalidRecipeWithoutPersisting() = runTest {
        val repo = FakeRecipeRepository()
        val saveRecipe = SaveRecipe(repo, FakeClock(), SequenceIdGenerator())

        val invalid = sampleRecipe(title = "   ", baseServings = 0, ingredients = emptyList(), steps = emptyList())
        val result = assertIs<SaveRecipeResult.ValidationFailed>(saveRecipe(invalid))

        assertTrue(RecipeValidationError.TitleBlank in result.errors)
        assertTrue(RecipeValidationError.EmptyIngredients in result.errors)
        assertTrue(RecipeValidationError.EmptySteps in result.errors)
        assertTrue(repo.getAllRecipes().isEmpty())
    }

    @Test
    fun getRecipesAndGetRecipeObserveChangesReactively() = runTest {
        val repo = FakeRecipeRepository()
        val getRecipes = GetRecipes(repo)
        val getRecipe = GetRecipe(repo)

        val r1 = sampleRecipe(id = "r1", title = "Clătite", prepMinutes = 10, cookMinutes = 15, isFavorite = true)
        val r2 = sampleRecipe(id = "r2", title = "Supă cremă", prepMinutes = 20, cookMinutes = 40, isFavorite = false)

        getRecipes(RecipeFilter(query = "clatite", favoritesOnly = true, maxTotalMinutes = 30)).test {
            assertEquals(emptyList(), awaitItem())
            repo.saveRecipes(listOf(r1, r2))
            assertEquals(listOf(r1), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        getRecipe("r1").test {
            assertEquals(r1, awaitItem())
            repo.deleteRecipe("r1")
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(r2, getRecipe.getOnce("r2"))
        assertNull(getRecipe.getOnce("   "))
    }

    @Test
    fun deleteRecipeReportsAffectedPlanEntriesAndCascades() = runTest {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val deleteRecipe = DeleteRecipe(recipeRepo)

        val recipe = sampleRecipe(id = "r-del")
        recipeRepo.saveRecipe(recipe)
        planRepo.upsertEntry(MealPlanEntry("p1", LocalDate(2026, 3, 2), MealSlot.LUNCH, "r-del", 2))
        planRepo.upsertEntry(MealPlanEntry("p2", LocalDate(2026, 3, 4), MealSlot.DINNER, "r-del", 4))

        assertEquals(2, deleteRecipe.countAffectedPlanEntries("r-del"))

        val result = assertIs<DeleteRecipeResult.Deleted>(deleteRecipe("r-del"))
        assertEquals("r-del", result.recipeId)
        assertEquals(2, result.affectedPlanEntriesCount)
        assertNull(recipeRepo.getRecipeById("r-del"))
        assertTrue(planRepo.getEntriesForRange(LocalDate(2026, 3, 1), LocalDate(2026, 3, 10)).isEmpty())

        assertIs<DeleteRecipeResult.NotFound>(deleteRecipe("r-del"))
        assertIs<DeleteRecipeResult.NotFound>(deleteRecipe(" "))
    }

    @Test
    fun toggleFavoriteFlipsFlagAndUpdatesTimestamp() = runTest {
        val repo = FakeRecipeRepository()
        val clock = FakeClock(currentMillis = 2_000L)
        val toggleFavorite = ToggleFavorite(repo, clock)

        repo.saveRecipe(sampleRecipe(id = "r-fav", isFavorite = false, updatedAt = 1_000L))

        val first = assertIs<ToggleFavoriteResult.Updated>(toggleFavorite("r-fav"))
        assertTrue(first.isFavorite)
        assertEquals(2_000L, repo.getRecipeById("r-fav")?.updatedAt)

        clock.currentMillis = 3_000L
        val second = assertIs<ToggleFavoriteResult.Updated>(toggleFavorite("r-fav"))
        assertFalse(second.isFavorite)
        assertEquals(3_000L, repo.getRecipeById("r-fav")?.updatedAt)

        assertIs<ToggleFavoriteResult.NotFound>(toggleFavorite("missing"))
    }

    @Test
    fun scaleRecipeScalesProportionallyHandlesToTasteAndValidatesBounds() {
        val scaleRecipe = ScaleRecipe()
        val recipe = sampleRecipe(
            baseServings = 4,
            ingredients = listOf(
                Ingredient("i1", 0, "Făină", 400.0, MeasurementUnit.G, Aisle.PANTRY),
                Ingredient("i2", 1, "Ouă", 1.0, MeasurementUnit.PIECE, Aisle.DAIRY),
                Ingredient("i3", 2, "Piper", 2.0, MeasurementUnit.PINCH, Aisle.SPICES),
                Ingredient("i4", 3, "Sare", null, MeasurementUnit.TO_TASTE, Aisle.SPICES),
            ),
        )

        val scaled = assertIs<ScaleRecipeResult.Success>(scaleRecipe(recipe, targetServings = 6))
        assertEquals(6, scaled.targetServings)
        assertEquals(600.0, scaled.ingredients[0].quantity!!, 1e-6)
        assertEquals(MeasurementUnit.G, scaled.ingredients[0].unit)
        assertEquals(1.5, scaled.ingredients[1].quantity!!, 1e-6)
        assertEquals(3.0, scaled.ingredients[2].quantity!!, 1e-6)
        assertNull(scaled.ingredients[3].quantity)
        assertEquals(MeasurementUnit.TO_TASTE, scaled.ingredients[3].unit)

        val metricPromoted = assertIs<ScaleRecipeResult.Success>(
            scaleRecipe(recipe, targetServings = 12, displayUnitSystem = UnitSystem.METRIC),
        )
        assertEquals(1.2, metricPromoted.ingredients[0].quantity!!, 1e-6)
        assertEquals(MeasurementUnit.KG, metricPromoted.ingredients[0].unit)

        val imperial = assertIs<ScaleRecipeResult.Success>(
            scaleRecipe(recipe, targetServings = 4, displayUnitSystem = UnitSystem.IMPERIAL),
        )
        assertEquals(MeasurementUnit.OZ, imperial.ingredients[0].unit)

        assertIs<ScaleRecipeResult.InvalidServings>(scaleRecipe(recipe, targetServings = 0))
        assertIs<ScaleRecipeResult.InvalidServings>(scaleRecipe(recipe, targetServings = 101))
    }
}
