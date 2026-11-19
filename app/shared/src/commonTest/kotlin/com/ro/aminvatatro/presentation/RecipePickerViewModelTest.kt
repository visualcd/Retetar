package com.ro.aminvatatro.presentation

import app.cash.turbine.test
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.usecase.plan.SetPlanEntry
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipes
import com.ro.aminvatatro.presentation.picker.RecipePickerUiEvent
import com.ro.aminvatatro.presentation.picker.RecipePickerViewModel
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
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class RecipePickerViewModelTest {

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
    fun searchSelectAdjustServingsAndConfirmCreatesMealPlanEntry() = runTest(testDispatcher) {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val r1 = sampleRecipe(id = "r1", title = "Ciorbă de legume", baseServings = 6)
        val r2 = sampleRecipe(id = "r2", title = "Clătite", baseServings = 4)
        recipeRepo.recipesFlow.value = listOf(r1, r2)

        val targetDate = LocalDate(2026, 3, 5)
        val vm = RecipePickerViewModel(
            date = targetDate,
            slot = MealSlot.LUNCH,
            getRecipes = GetRecipes(recipeRepo),
            setPlanEntry = SetPlanEntry(planRepo, recipeRepo, SequenceIdGenerator("mp")),
        )
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.recipes.size)

        vm.onQueryChange("ciorba")
        advanceUntilIdle()
        assertEquals(listOf("r1"), vm.uiState.value.recipes.map { it.id })

        vm.selectRecipe(r1)
        assertEquals(6, vm.uiState.value.servings)
        vm.onServingsChange(3)
        assertEquals(3, vm.uiState.value.servings)

        vm.events.test {
            vm.confirmSelection()
            assertEquals(RecipePickerUiEvent.EntryAdded, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        val entry = planRepo.entriesFlow.value.singleOrNull()
        assertNotNull(entry)
        assertEquals(targetDate, entry.date)
        assertEquals(MealSlot.LUNCH, entry.slot)
        assertEquals("r1", entry.recipeId)
        assertEquals(3, entry.servings)
    }
}
