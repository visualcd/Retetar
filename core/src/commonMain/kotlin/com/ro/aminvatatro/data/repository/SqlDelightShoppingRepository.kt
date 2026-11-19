package com.ro.aminvatatro.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.ro.aminvatatro.data.local.db.RetetarDatabase
import com.ro.aminvatatro.domain.model.ShoppingItem
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * SQLDelight-backed implementation of [ShoppingRepository] used on Android, iOS, and Desktop (JVM).
 */
class SqlDelightShoppingRepository(
    private val database: RetetarDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ShoppingRepository {

    private val queries get() = database.retetarQueries

    override fun observeItemsForWeek(weekStart: LocalDate): Flow<List<ShoppingItem>> =
        queries.selectShoppingItemsByWeek(weekStart.toString())
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun getItemsForWeek(weekStart: LocalDate): List<ShoppingItem> = withContext(dispatcher) {
        queries.selectShoppingItemsByWeek(weekStart.toString())
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun upsertItem(item: ShoppingItem) {
        withContext(dispatcher) {
            queries.upsertShoppingItem(
                id = item.id,
                weekStart = item.weekStart.toString(),
                itemKey = item.itemKey,
                name = item.name.trim(),
                normalizedName = item.normalizedName,
                quantity = item.quantity,
                unit = item.unit.name,
                aisle = item.aisle.name,
                source = item.source.name,
                checked = if (item.checked) 1L else 0L,
            )
        }
    }

    override suspend fun replaceItemsForWeek(
        weekStart: LocalDate,
        items: List<ShoppingItem>,
    ) = withContext(dispatcher) {
        database.transaction {
            queries.deleteShoppingItemsByWeek(weekStart.toString())
            items.forEach { item ->
                queries.upsertShoppingItem(
                    id = item.id,
                    weekStart = weekStart.toString(),
                    itemKey = item.itemKey,
                    name = item.name.trim(),
                    normalizedName = item.normalizedName,
                    quantity = item.quantity,
                    unit = item.unit.name,
                    aisle = item.aisle.name,
                    source = item.source.name,
                    checked = if (item.checked) 1L else 0L,
                )
            }
        }
    }

    override suspend fun setChecked(itemId: String, checked: Boolean) {
        withContext(dispatcher) {
            queries.updateShoppingItemChecked(
                checked = if (checked) 1L else 0L,
                id = itemId,
            )
        }
    }

    override suspend fun deleteItem(itemId: String) {
        withContext(dispatcher) {
            queries.deleteShoppingItemById(itemId)
        }
    }

    override suspend fun clearCheckedForWeek(weekStart: LocalDate) {
        withContext(dispatcher) {
            queries.deleteCheckedShoppingItemsByWeek(weekStart.toString())
        }
    }

    override suspend fun deleteAllItems() {
        withContext(dispatcher) {
            queries.deleteAllShoppingItems()
        }
    }
}
