package com.ro.aminvatatro.domain.usecase.shopping

import com.ro.aminvatatro.core.time.WeekUtils
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import kotlinx.datetime.LocalDate

/**
 * Deletes all checked shopping list items for the selected week.
 */
class ClearChecked(
    private val shoppingRepository: ShoppingRepository,
) {
    /**
     * Clears every checked item belonging to the week containing [weekDate].
     */
    suspend operator fun invoke(weekDate: LocalDate) {
        val weekStart = WeekUtils.weekStartOf(weekDate)
        shoppingRepository.clearCheckedForWeek(weekStart)
    }
}
