package com.ro.aminvatatro.domain.usecase.plan

import com.ro.aminvatatro.core.time.IdGenerator
import com.ro.aminvatatro.core.time.WeekUtils
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Outcome of copying the previous week's meal plan onto a target week.
 */
sealed interface RepeatPreviousWeekResult {
    /** [copiedEntries] were copied into the week starting on [targetWeekStart]. */
    data class Repeated(
        val targetWeekStart: LocalDate,
        val copiedEntries: List<MealPlanEntry>,
    ) : RepeatPreviousWeekResult

    /** The immediately preceding week had no scheduled entries; target week was left unchanged. */
    data object PreviousWeekEmpty : RepeatPreviousWeekResult
}

/**
 * Copies every meal plan entry from the week preceding `targetWeekDate` into the target week,
 * shifting each entry forward by 7 days with a freshly generated ID.
 */
class RepeatPreviousWeek(
    private val mealPlanRepository: MealPlanRepository,
    private val idGenerator: IdGenerator,
) {
    /**
     * Repeats the previous week's schedule into the week containing [targetWeekDate].
     *
     * When [replaceExisting] is `true` (default), existing entries in the target week are replaced
     * atomically; when `false`, copied entries are merged alongside existing ones.
     */
    suspend operator fun invoke(
        targetWeekDate: LocalDate,
        replaceExisting: Boolean = true,
    ): RepeatPreviousWeekResult {
        val targetWeekStart = WeekUtils.weekStartOf(targetWeekDate)
        val targetWeekEnd = WeekUtils.weekEndOf(targetWeekStart)
        val previousWeekStart = WeekUtils.previousWeek(targetWeekStart)
        val previousWeekEnd = WeekUtils.weekEndOf(previousWeekStart)

        val previousEntries = mealPlanRepository.getEntriesForRange(previousWeekStart, previousWeekEnd)
        if (previousEntries.isEmpty()) return RepeatPreviousWeekResult.PreviousWeekEmpty

        val copiedEntries = previousEntries.map { entry ->
            entry.copy(
                id = idGenerator.nextId(),
                date = entry.date.plus(WeekUtils.DAYS_IN_WEEK, DateTimeUnit.DAY),
            )
        }

        val finalEntries = if (replaceExisting) {
            copiedEntries
        } else {
            mealPlanRepository.getEntriesForRange(targetWeekStart, targetWeekEnd) + copiedEntries
        }

        mealPlanRepository.replaceEntriesForRange(
            startDate = targetWeekStart,
            endDate = targetWeekEnd,
            entries = finalEntries,
        )
        return RepeatPreviousWeekResult.Repeated(
            targetWeekStart = targetWeekStart,
            copiedEntries = copiedEntries,
        )
    }
}
