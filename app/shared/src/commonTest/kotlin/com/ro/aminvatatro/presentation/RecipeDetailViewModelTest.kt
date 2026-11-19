package com.ro.aminvatatro.presentation

import app.cash.turbine.test
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.usecase.plan.SetPlanEntry
import com.ro.aminvatatro.domain.usecase.recipe.DeleteRecipe
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipe
import com.ro.aminvatatro.domain.usecase.recipe.ScaleRecipe
import com.ro.aminvatatro.domain.usecase.recipe.ToggleFavorite
import com.ro.aminvatatro.presentation.detail.RecipeDetailUiEvent
import com.ro.aminvatatro.presentation.detail.RecipeDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun scalingAndImperialConversionUpdateScaledIngredients() = runTest(testDispatcher) {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val settingsRepo = FakeSettingsRepository()
        val clock = FakeClock()
        recipeRepo.recipesFlow.value = listOf(sampleRecipe(id = "r1", baseServings = 4))

        val vm = RecipeDetailViewModel(
            recipeId = "r1",
            getRecipe = GetRecipe(recipeRepo),
            scaleRecipe = ScaleRecipe(),
            toggleFavorite = ToggleFavorite(recipeRepo, clock),
            deleteRecipe = DeleteRecipe(recipeRepo),
            setPlanEntry = SetPlanEntry(planRepo, recipeRepo, SequenceIdGenerator("mp")),
            settingsRepository = settingsRepo,
            clock = clock,
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(4, vm.uiState.value.selectedServings)
        assertEquals(250.0, vm.uiState.value.scaledIngredients.first().quantity)
        assertEquals(MeasurementUnit.G, vm.uiState.value.scaledIngredients.first().unit)

        // Double servings from 4 -> 8
        vm.onServingsChange(8)
        advanceUntilIdle()
        assertEquals(8, vm.uiState.value.selectedServings)
        assertEquals(500.0, vm.uiState.value.scaledIngredients.first().quantity)

        // Switch unit system to IMPERIAL
        settingsRepo.setUnitSystem(UnitSystem.IMPERIAL)
        advanceUntilIdle()
        assertEquals(UnitSystem.IMPERIAL, vm.uiState.value.unitSystem)
        assertEquals(MeasurementUnit.LB, vm.uiState.value.scaledIngredients.first().unit)
    }

    @Test
    fun deleteShowsAffectedEntriesAndEmitsDeletedEvent() = runTest(testDispatcher) {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val settingsRepo = FakeSettingsRepository()
        val clock = FakeClock()
        recipeRepo.recipesFlow.value = listOf(sampleRecipe(id = "r1"))
        planRepo.entriesFlow.value = listOf(
            MealPlanEntry("e1", LocalDate(2026, 3, 2), MealSlot.LUNCH, "r1", 2),
            MealPlanEntry("e2", LocalDate(2026, 3, 3), MealSlot.DINNER, "r1", 4),
        )

        val vm = RecipeDetailViewModel(
            recipeId = "r1",
            getRecipe = GetRecipe(recipeRepo),
            scaleRecipe = ScaleRecipe(),
            toggleFavorite = ToggleFavorite(recipeRepo, clock),
            deleteRecipe = DeleteRecipe(recipeRepo),
            setPlanEntry = SetPlanEntry(planRepo, recipeRepo, SequenceIdGenerator("mp")),
            settingsRepository = settingsRepo,
            clock = clock,
        )
        advanceUntilIdle()

        vm.requestDelete()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showDeleteDialog)
        assertEquals(2, vm.uiState.value.affectedPlanEntriesCount)

        vm.events.test {
            vm.confirmDelete()
            assertEquals(RecipeDetailUiEvent.Deleted, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(0, recipeRepo.getAllRecipes().size)
        assertEquals(0, planRepo.entriesFlow.value.size)
    }

    @Test
    fun addToPlanPersistsEntryAndEmitsEvent() = runTest(testDispatcher) {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val settingsRepo = FakeSettingsRepository()
        val clock = FakeClock()
        recipeRepo.recipesFlow.value = listOf(sampleRecipe(id = "r1", baseServings = 3))

        val vm = RecipeDetailViewModel(
            recipeId = "r1",
            getRecipe = GetRecipe(recipeRepo),
            scaleRecipe = ScaleRecipe(),
            toggleFavorite = ToggleFavorite(recipeRepo, clock),
            deleteRecipe = DeleteRecipe(recipeRepo),
            setPlanEntry = SetPlanEntry(planRepo, recipeRepo, SequenceIdGenerator("mp")),
            settingsRepository = settingsRepo,
            clock = clock,
        )
        advanceUntilIdle()

        vm.openAddToPlanDialog()
        val targetDate = LocalDate(2026, 3, 5)
        vm.selectPlanDate(targetDate)
        vm.selectPlanSlot(MealSlot.DINNER)
        assertTrue(vm.uiState.value.showAddToPlanDialog)

        vm.events.test {
            vm.confirmAddToPlan()
            assertEquals(RecipeDetailUiEvent.AddedToPlan, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val saved = planRepo.entriesFlow.value.singleOrNull()
        assertNotNull(saved)
        assertEquals(targetDate, saved.date)
        assertEquals(MealSlot.DINNER, saved.slot)
        assertEquals("r1", saved.recipeId)
        assertEquals(3, saved.servings)
    }
}
