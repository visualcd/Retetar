package com.ro.aminvatatro.domain.usecase.shopping

import com.ro.aminvatatro.domain.repository.ShoppingRepository

/**
 * Removes a single shopping list item by its identifier.
 */
class RemoveItem(
    private val shoppingRepository: ShoppingRepository,
) {
    /**
     * Deletes the shopping item identified by [itemId]. Blank identifiers are safely ignored.
     */
    suspend operator fun invoke(itemId: String) {
        if (itemId.isBlank()) return
        shoppingRepository.deleteItem(itemId)
    }
}
