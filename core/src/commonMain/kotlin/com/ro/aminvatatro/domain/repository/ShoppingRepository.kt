package com.ro.aminvatatro.domain.repository

import com.ro.aminvatatro.domain.model.ShoppingItem
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Persistence contract for weekly [ShoppingItem] entries anchored to a week's Monday (`weekStart`).
 */
interface ShoppingRepository {
    /** Observes all shopping items for the week starting on [weekStart]. */
    fun observeItemsForWeek(weekStart: LocalDate): Flow<List<ShoppingItem>>

    /** Loads all shopping items for the week starting on [weekStart]. */
    suspend fun getItemsForWeek(weekStart: LocalDate): List<ShoppingItem>

    /** Inserts or updates a single shopping [item]. */
    suspend fun upsertItem(item: ShoppingItem)

    /** Atomically replaces all shopping items for [weekStart] with [items]. */
    suspend fun replaceItemsForWeek(weekStart: LocalDate, items: List<ShoppingItem>)

    /** Updates the checked status of [itemId]. */
    suspend fun setChecked(itemId: String, checked: Boolean)

    /** Removes a single shopping item by [itemId]. */
    suspend fun deleteItem(itemId: String)

    /** Removes all checked shopping items for [weekStart]. */
    suspend fun clearCheckedForWeek(weekStart: LocalDate)

    /** Removes all shopping items across all weeks. */
    suspend fun deleteAllItems()
}
