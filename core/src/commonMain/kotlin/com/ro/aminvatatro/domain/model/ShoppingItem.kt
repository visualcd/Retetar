package com.ro.aminvatatro.domain.model

import com.ro.aminvatatro.core.units.MeasurementUnit
import kotlinx.datetime.LocalDate

/**
 * Origin of a [ShoppingItem]: generated automatically from the weekly plan or added manually.
 */
enum class ShoppingItemSource {
    AUTO,
    MANUAL,
}

/**
 * Single item on a weekly shopping list anchored to [weekStart] (Monday).
 */
data class ShoppingItem(
    val id: String,
    val weekStart: LocalDate,
    val itemKey: String,
    val name: String,
    val normalizedName: String,
    val quantity: Double?,
    val unit: MeasurementUnit,
    val aisle: Aisle = Aisle.OTHER,
    val source: ShoppingItemSource,
    val checked: Boolean = false,
)
