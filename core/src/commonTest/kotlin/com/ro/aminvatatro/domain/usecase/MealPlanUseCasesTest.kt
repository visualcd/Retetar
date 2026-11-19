package com.ro.aminvatatro.domain.usecase

import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.usecase.plan.ChangeServings
import com.ro.aminvatatro.domain.usecase.plan.ChangeServingsResult
import com.ro.aminvatatro.domain.usecase.plan.CopyDay
import com.ro.aminvatatro.domain.usecase.plan.CopyDayResult
import com.ro.aminvatatro.domain.usecase.plan.RemovePlanEntry
import com.ro.aminvatatro.domain.usecase.plan.RepeatPreviousWeek
import com.ro.aminvatatro.domain.usecase.plan.RepeatPreviousWeekResult
import com.ro.aminvatatro.domain.usecase.plan.SetPlanEntry
import com.ro.aminvatatro.domain.usecase.plan.SetPlanEntryResult
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MealPlanUseCasesTest {

    @Test
    fun setRemoveAndChangeServingsSupportMultipleEntriesPerSlotAndValidateInputs() = runTest {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val ids = SequenceIdGenerator("plan")
        recipeRepo.saveRecipes(listOf(sampleRecipe(id = "r1"), sampleRecipe(id = "r2")))

        val setPlanEntry = SetPlanEntry(planRepo, recipeRepo, ids)
        val changeServings = ChangeServings(planRepo)
        val removePlanEntry = RemovePlanEntry(planRepo)

        val monday = LocalDate(2026, 3, 2)
        val first = assertIs<SetPlanEntryResult.Success>(
            setPlanEntry(monday, MealSlot.LUNCH, "r1", servings = 2),
        ).entry
        val second = assertIs<SetPlanEntryResult.Success>(
            setPlanEntry(monday, MealSlot.LUNCH, "r2", servings = 4),
        ).entry

        assertNotEquals(first.id, second.id)
        assertEquals(2, planRepo.getEntriesForDate(monday).size)

        assertIs<SetPlanEntryResult.InvalidServings>(setPlanEntry(monday, MealSlot.DINNER, "r1", servings = 0))
        assertIs<SetPlanEntryResult.InvalidServings>(setPlanEntry(monday, MealSlot.DINNER, "r1", servings = 101))
        assertIs<SetPlanEntryResult.RecipeNotFound>(setPlanEntry(monday, MealSlot.DINNER, "missing", servings = 2))

        val updated = assertIs<ChangeServingsResult.Updated>(changeServings(first.id, servings = 6))
        assertEquals(6, updated.servings)
        assertEquals(6, planRepo.getEntriesForDate(monday).first { it.id == first.id }.servings)

        assertIs<ChangeServingsResult.InvalidServings>(changeServings(first.id, servings = 0))
        assertIs<ChangeServingsResult.InvalidEntryId>(changeServings("  ", servings = 2))

        removePlanEntry(first.id)
        assertEquals(listOf(second), planRepo.getEntriesForDate(monday))
    }

    @Test
    fun copyDayCopiesAllEntriesWithFreshIdsAndHandlesEdgeCases() = runTest {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val ids = SequenceIdGenerator("copy")
        recipeRepo.saveRecipes(listOf(sampleRecipe(id = "r1"), sampleRecipe(id = "r2")))

        val setPlanEntry = SetPlanEntry(planRepo, recipeRepo, ids)
        val copyDay = CopyDay(planRepo, ids)

        val monday = LocalDate(2026, 3, 2)
        val wednesday = LocalDate(2026, 3, 4)
        val friday = LocalDate(2026, 3, 6)

        val e1 = assertIs<SetPlanEntryResult.Success>(setPlanEntry(monday, MealSlot.BREAKFAST, "r1", 2)).entry
        val e2 = assertIs<SetPlanEntryResult.Success>(setPlanEntry(monday, MealSlot.DINNER, "r2", 4)).entry
        assertIs<SetPlanEntryResult.Success>(setPlanEntry(wednesday, MealSlot.SNACK, "r1", 1))

        assertIs<CopyDayResult.SameDate>(copyDay(monday, monday))
        assertIs<CopyDayResult.SourceEmpty>(copyDay(friday, wednesday))
        // Empty source must not wipe existing target entries
        assertEquals(1, planRepo.getEntriesForDate(wednesday).size)

        val copied = assertIs<CopyDayResult.Copied>(copyDay(monday, wednesday))
        assertEquals(wednesday, copied.targetDate)
        assertEquals(2, copied.copiedEntries.size)

        val targetEntries = planRepo.getEntriesForDate(wednesday)
        assertEquals(2, targetEntries.size)
        assertEquals(listOf(MealSlot.BREAKFAST, MealSlot.DINNER), targetEntries.map { it.slot })
        assertEquals(listOf("r1", "r2"), targetEntries.map { it.recipeId })
        assertEquals(listOf(2, 4), targetEntries.map { it.servings })
        assertTrue(targetEntries.none { it.id == e1.id || it.id == e2.id })
    }

    @Test
    fun repeatPreviousWeekShiftsAllEntriesBySevenDaysAcrossYearBoundary() = runTest {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val ids = SequenceIdGenerator("wk")
        recipeRepo.saveRecipes(listOf(sampleRecipe(id = "r1"), sampleRecipe(id = "r2")))

        val setPlanEntry = SetPlanEntry(planRepo, recipeRepo, ids)
        val repeatPreviousWeek = RepeatPreviousWeek(planRepo, ids)

        // Week 2025-12-29 (Mon) .. 2026-01-04 (Sun) -> target week 2026-01-05 (Mon) .. 2026-01-11 (Sun)
        val prevMon = LocalDate(2025, 12, 29)
        val prevThu = LocalDate(2026, 1, 1)
        val prevSun = LocalDate(2026, 1, 4)

        val src1 = assertIs<SetPlanEntryResult.Success>(setPlanEntry(prevMon, MealSlot.BREAKFAST, "r1", 2)).entry
        val src2 = assertIs<SetPlanEntryResult.Success>(setPlanEntry(prevThu, MealSlot.LUNCH, "r2", 3)).entry
        val src3 = assertIs<SetPlanEntryResult.Success>(setPlanEntry(prevSun, MealSlot.DINNER, "r1", 5)).entry

        // Pass a mid-week date (Wednesday 2026-01-07) to verify weekStart normalization
        val repeated = assertIs<RepeatPreviousWeekResult.Repeated>(
            repeatPreviousWeek(LocalDate(2026, 1, 7)),
        )
        assertEquals(LocalDate(2026, 1, 5), repeated.targetWeekStart)
        assertEquals(3, repeated.copiedEntries.size)

        val targetWeekEntries = planRepo.getEntriesForRange(LocalDate(2026, 1, 5), LocalDate(2026, 1, 11))
        assertEquals(
            listOf(LocalDate(2026, 1, 5), LocalDate(2026, 1, 8), LocalDate(2026, 1, 11)),
            targetWeekEntries.map { it.date },
        )
        assertEquals(listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER), targetWeekEntries.map { it.slot })
        assertEquals(listOf(2, 3, 5), targetWeekEntries.map { it.servings })
        val originalIds = setOf(src1.id, src2.id, src3.id)
        assertTrue(targetWeekEntries.none { it.id in originalIds })

        // Repeating from an empty week leaves target week untouched
        assertIs<RepeatPreviousWeekResult.PreviousWeekEmpty>(
            repeatPreviousWeek(LocalDate(2026, 2, 16)),
        )
    }
}
