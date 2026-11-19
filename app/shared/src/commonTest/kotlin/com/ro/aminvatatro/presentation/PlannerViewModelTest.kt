package com.ro.aminvatatro.presentation

import app.cash.turbine.test
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.usecase.plan.ChangeServings
import com.ro.aminvatatro.domain.usecase.plan.CopyDay
import com.ro.aminvatatro.domain.usecase.plan.RemovePlanEntry
import com.ro.aminvatatro.domain.usecase.plan.RepeatPreviousWeek
import com.ro.aminvatatro.presentation.planner.PlannerUiEvent
import com.ro.aminvatatro.presentation.planner.PlannerViewModel
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModelTest {

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
    fun weekNavigationServingsEditRemoveAndCopyDayWork() = runTest(testDispatcher) {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val clock = FakeClock(currentDate = LocalDate(2026, 3, 4)) // Week of Mon 2026-03-02
        val idGen = SequenceIdGenerator("mp")

        recipeRepo.recipesFlow.value = listOf(sampleRecipe(id = "r1", title = "Supă"))
        planRepo.entriesFlow.value = listOf(
            MealPlanEntry("e1", LocalDate(2026, 3, 2), MealSlot.LUNCH, "r1", 2),
        )

        val vm = PlannerViewModel(
            mealPlanRepository = planRepo,
            recipeRepository = recipeRepo,
            changeServings = ChangeServings(planRepo),
            removePlanEntry = RemovePlanEntry(planRepo),
            copyDay = CopyDay(planRepo, idGen),
            repeatPreviousWeek = RepeatPreviousWeek(planRepo, idGen),
            clock = clock,
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(LocalDate(2026, 3, 2), vm.uiState.value.weekStart)
        val mondayLunch = vm.uiState.value.entriesByDateAndSlot[LocalDate(2026, 3, 2)]
            ?.get(MealSlot.LUNCH)
            ?.singleOrNull()
        assertNotNull(mondayLunch)
        assertEquals("Supă", mondayLunch.recipeTitle)

        // Edit servings 2 -> 5
        vm.onEditEntryServings(mondayLunch)
        vm.onEditingServingsChange(5)
        vm.confirmEditServings()
        advanceUntilIdle()
        assertNull(vm.uiState.value.editingEntry)
        assertEquals(5, planRepo.entriesFlow.value.first { it.id == "e1" }.servings)

        // Copy day Monday -> Tuesday
        vm.events.test {
            vm.openCopyDayDialog()
            vm.setCopySourceDate(LocalDate(2026, 3, 2))
            vm.setCopyTargetDate(LocalDate(2026, 3, 2))
            vm.confirmCopyDay()
            assertEquals(PlannerUiEvent.CopyDaySameDate, awaitItem())

            vm.openCopyDayDialog()
            vm.setCopySourceDate(LocalDate(2026, 3, 3))
            vm.setCopyTargetDate(LocalDate(2026, 3, 4))
            vm.confirmCopyDay()
            assertEquals(PlannerUiEvent.CopyDaySourceEmpty, awaitItem())

            vm.openCopyDayDialog()
            vm.setCopySourceDate(LocalDate(2026, 3, 2))
            vm.setCopyTargetDate(LocalDate(2026, 3, 3))
            vm.confirmCopyDay()
            assertEquals(PlannerUiEvent.CopyDayDone(1), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        // Navigate next week and repeat previous week
        vm.nextWeek()
        advanceUntilIdle()
        assertEquals(LocalDate(2026, 3, 9), vm.uiState.value.weekStart)

        vm.events.test {
            vm.onRepeatPreviousWeek()
            assertEquals(PlannerUiEvent.RepeatPreviousWeekDone(2), awaitItem())

            vm.onOpenShoppingList()
            assertEquals(PlannerUiEvent.OpenShoppingForWeek(LocalDate(2026, 3, 9)), awaitItem())

            vm.onAddEntryClicked(LocalDate(2026, 3, 10), MealSlot.DINNER)
            assertEquals(
                PlannerUiEvent.OpenRecipePicker(LocalDate(2026, 3, 10), MealSlot.DINNER),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        // Go back to current week and remove entry
        vm.goToCurrentWeek()
        advanceUntilIdle()
        assertEquals(LocalDate(2026, 3, 2), vm.uiState.value.weekStart)
        vm.onRemoveEntry("e1")
        advanceUntilIdle()
        assertEquals(0, planRepo.getEntriesForDate(LocalDate(2026, 3, 2)).size)
    }

    @Test
    fun repeatPreviousWeekWhenEmptyEmitsEmptyEvent() = runTest(testDispatcher) {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val idGen = SequenceIdGenerator("mp")
        val vm = PlannerViewModel(
            mealPlanRepository = planRepo,
            recipeRepository = recipeRepo,
            changeServings = ChangeServings(planRepo),
            removePlanEntry = RemovePlanEntry(planRepo),
            copyDay = CopyDay(planRepo, idGen),
            repeatPreviousWeek = RepeatPreviousWeek(planRepo, idGen),
            clock = FakeClock(),
        )
        advanceUntilIdle()

        vm.events.test {
            vm.onRepeatPreviousWeek()
            assertEquals(PlannerUiEvent.RepeatPreviousWeekEmpty, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
