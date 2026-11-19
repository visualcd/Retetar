package com.ro.aminvatatro.domain.usecase.shopping

import com.ro.aminvatatro.domain.repository.ShoppingRepository

/**
 * Updates the checked state of a shopping list item.
 */
class ToggleItem(
    private val shoppingRepository: ShoppingRepository,
) {
    /**
     * Sets the checked state of [itemId] to [checked]. Blank identifiers are safely ignored.
     */
    suspend operator fun invoke(itemId: String, checked: Boolean) {
        if (itemId.isBlank()) return
        shoppingRepository.setChecked(itemId = itemId, checked = checked)
    }
}
