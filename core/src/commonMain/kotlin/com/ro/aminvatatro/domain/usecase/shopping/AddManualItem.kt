package com.ro.aminvatatro.domain.usecase.shopping

import com.ro.aminvatatro.core.text.TextNormalizer
import com.ro.aminvatatro.core.time.IdGenerator
import com.ro.aminvatatro.core.time.WeekUtils
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.core.units.UnitConverter
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.ShoppingItem
import com.ro.aminvatatro.domain.model.ShoppingItemSource
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import kotlinx.datetime.LocalDate

/**
 * Outcome of adding a manual [ShoppingItem] to a weekly shopping list.
 */
sealed interface AddManualItemResult {
    /** The manual item passed validation and was saved. */
    data class Success(val item: ShoppingItem) : AddManualItemResult

    /** Item name was empty or whitespace-only. */
    data object NameBlank : AddManualItemResult

    /** Item name exceeded [maxLength] characters. */
    data class NameTooLong(val maxLength: Int, val actualLength: Int) : AddManualItemResult

    /** Quantity was non-positive, non-finite, or provided together with [MeasurementUnit.TO_TASTE]. */
    data object InvalidQuantity : AddManualItemResult
}

/**
 * Validates and persists a user-entered manual [ShoppingItem] for the week containing a date.
 */
class AddManualItem(
    private val shoppingRepository: ShoppingRepository,
    private val idGenerator: IdGenerator,
) {
    /**
     * Validates inputs and inserts a `MANUAL` [ShoppingItem] anchored to the Monday of [weekDate].
     *
     * A `null` [quantity] is allowed for quick manual notes (e.g. "Pâine"), while any non-null
     * quantity must be finite and strictly positive (and disallowed for [MeasurementUnit.TO_TASTE]).
     */
    suspend operator fun invoke(
        weekDate: LocalDate,
        name: String,
        quantity: Double? = null,
        unit: MeasurementUnit = MeasurementUnit.PIECE,
        aisle: Aisle = Aisle.OTHER,
    ): AddManualItemResult {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return AddManualItemResult.NameBlank
        if (trimmedName.length > MAX_NAME_LENGTH) {
            return AddManualItemResult.NameTooLong(
                maxLength = MAX_NAME_LENGTH,
                actualLength = trimmedName.length,
            )
        }

        if (unit == MeasurementUnit.TO_TASTE) {
            if (quantity != null) return AddManualItemResult.InvalidQuantity
        } else if (quantity != null && (!quantity.isFinite() || quantity <= 0.0)) {
            return AddManualItemResult.InvalidQuantity
        }

        val weekStart = WeekUtils.weekStartOf(weekDate)
        val normalizedName = TextNormalizer.normalize(trimmedName)
        val itemKey = UnitConverter.shoppingItemKey(normalizedName, unit)

        val item = ShoppingItem(
            id = idGenerator.nextId(),
            weekStart = weekStart,
            itemKey = itemKey,
            name = trimmedName,
            normalizedName = normalizedName,
            quantity = quantity,
            unit = unit,
            aisle = aisle,
            source = ShoppingItemSource.MANUAL,
            checked = false,
        )
        shoppingRepository.upsertItem(item)
        return AddManualItemResult.Success(item)
    }

    companion object {
        const val MAX_NAME_LENGTH: Int = 100
    }
}
