package com.ro.aminvatatro.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ro.aminvatatro.data.local.db.RetetarDatabase
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * SQLDelight-backed implementation of [MealPlanRepository] used on Android, iOS, and Desktop (JVM).
 */
class SqlDelightMealPlanRepository(
    private val database: RetetarDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MealPlanRepository {

    private val queries get() = database.retetarQueries

    override fun observeEntriesForRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<MealPlanEntry>> = queries.selectMealPlanByRange(
        startDate = startDate.toString(),
        endDate = endDate.toString(),
    ).asFlow().mapToList(dispatcher).map { rows ->
        rows.map { it.toDomain() }
    }

    override suspend fun getEntriesForRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MealPlanEntry> = withContext(dispatcher) {
        queries.selectMealPlanByRange(
            startDate = startDate.toString(),
            endDate = endDate.toString(),
        ).executeAsList().map { it.toDomain() }
    }

    override suspend fun getEntriesForDate(date: LocalDate): List<MealPlanEntry> = withContext(dispatcher) {
        queries.selectMealPlanByDate(date.toString()).executeAsList().map { it.toDomain() }
    }

    override suspend fun upsertEntry(entry: MealPlanEntry) {
        withContext(dispatcher) {
            queries.upsertMealPlanEntry(
                id = entry.id,
                date = entry.date.toString(),
                slot = entry.slot.name,
                recipeId = entry.recipeId,
                servings = entry.servings.toLong(),
            )
        }
    }

    override suspend fun replaceEntriesForDate(
        date: LocalDate,
        entries: List<MealPlanEntry>,
    ) = withContext(dispatcher) {
        database.transaction {
            queries.deleteMealPlanByDate(date.toString())
            entries.forEach { entry ->
                queries.upsertMealPlanEntry(
                    id = entry.id,
                    date = entry.date.toString(),
                    slot = entry.slot.name,
                    recipeId = entry.recipeId,
                    servings = entry.servings.toLong(),
                )
            }
        }
    }

    override suspend fun replaceEntriesForRange(
        startDate: LocalDate,
        endDate: LocalDate,
        entries: List<MealPlanEntry>,
    ) = withContext(dispatcher) {
        database.transaction {
            queries.deleteMealPlanByRange(
                startDate = startDate.toString(),
                endDate = endDate.toString(),
            )
            entries.forEach { entry ->
                queries.upsertMealPlanEntry(
                    id = entry.id,
                    date = entry.date.toString(),
                    slot = entry.slot.name,
                    recipeId = entry.recipeId,
                    servings = entry.servings.toLong(),
                )
            }
        }
    }

    override suspend fun updateServings(entryId: String, servings: Int) {
        withContext(dispatcher) {
            queries.updateMealPlanServings(servings = servings.toLong(), id = entryId)
        }
    }

    override suspend fun deleteEntry(entryId: String) {
        withContext(dispatcher) {
            queries.deleteMealPlanById(entryId)
        }
    }

    override suspend fun deleteAllEntries() {
        withContext(dispatcher) {
            queries.deleteAllMealPlanEntries()
        }
    }
}
