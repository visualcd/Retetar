package com.ro.aminvatatro.domain.usecase.plan

import com.ro.aminvatatro.core.time.IdGenerator
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import kotlinx.datetime.LocalDate

/**
 * Outcome of copying a day's meal plan entries onto another calendar date.
 */
sealed interface CopyDayResult {
    /** [copiedEntries] were written to [targetDate]. */
    data class Copied(
        val targetDate: LocalDate,
        val copiedEntries: List<MealPlanEntry>,
    ) : CopyDayResult

    /** Source and target dates were identical; nothing was modified. */
    data object SameDate : CopyDayResult

    /** Source date had no scheduled entries; target date was left unchanged. */
    data object SourceEmpty : CopyDayResult
}

/**
 * Copies all meal plan entries from one calendar date onto another, assigning fresh entry IDs.
 */
class CopyDay(
    private val mealPlanRepository: MealPlanRepository,
    private val idGenerator: IdGenerator,
) {
    /**
     * Copies all entries scheduled on [sourceDate] onto [targetDate].
     *
     * When [replaceExisting] is `true` (default), existing entries on [targetDate] are replaced
     * atomically; when `false`, copied entries are appended to [targetDate].
     */
    suspend operator fun invoke(
        sourceDate: LocalDate,
        targetDate: LocalDate,
        replaceExisting: Boolean = true,
    ): CopyDayResult {
        if (sourceDate == targetDate) return CopyDayResult.SameDate

        val sourceEntries = mealPlanRepository.getEntriesForDate(sourceDate)
        if (sourceEntries.isEmpty()) return CopyDayResult.SourceEmpty

        val copied = sourceEntries.map { entry ->
            entry.copy(
                id = idGenerator.nextId(),
                date = targetDate,
            )
        }

        val finalEntries = if (replaceExisting) {
            copied
        } else {
            mealPlanRepository.getEntriesForDate(targetDate) + copied
        }

        mealPlanRepository.replaceEntriesForDate(date = targetDate, entries = finalEntries)
        return CopyDayResult.Copied(targetDate = targetDate, copiedEntries = copied)
    }
}
