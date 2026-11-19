package com.ro.aminvatatro.domain.repository

import com.ro.aminvatatro.domain.model.MealPlanEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Persistence contract for weekly [MealPlanEntry] records.
 */
interface MealPlanRepository {
    /** Observes all meal plan entries within the inclusive `[startDate, endDate]` interval. */
    fun observeEntriesForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MealPlanEntry>>

    /** Loads all meal plan entries within the inclusive `[startDate, endDate]` interval. */
    suspend fun getEntriesForRange(startDate: LocalDate, endDate: LocalDate): List<MealPlanEntry>

    /** Loads all meal plan entries scheduled on [date]. */
    suspend fun getEntriesForDate(date: LocalDate): List<MealPlanEntry>

    /** Inserts or replaces a single meal plan [entry]. */
    suspend fun upsertEntry(entry: MealPlanEntry)

    /** Replaces all meal plan entries on [date] with [entries] inside a single transaction. */
    suspend fun replaceEntriesForDate(date: LocalDate, entries: List<MealPlanEntry>)

    /** Replaces all meal plan entries in `[startDate, endDate]` with [entries] inside a single transaction. */
    suspend fun replaceEntriesForRange(
        startDate: LocalDate,
        endDate: LocalDate,
        entries: List<MealPlanEntry>,
    )

    /** Updates the serving count of [entryId] to [servings]. */
    suspend fun updateServings(entryId: String, servings: Int)

    /** Deletes the meal plan entry identified by [entryId]. */
    suspend fun deleteEntry(entryId: String)

    /** Deletes all meal plan entries. */
    suspend fun deleteAllEntries()
}
