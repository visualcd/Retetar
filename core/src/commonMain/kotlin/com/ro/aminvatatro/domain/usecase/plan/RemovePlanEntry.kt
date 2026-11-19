package com.ro.aminvatatro.domain.usecase.plan

import com.ro.aminvatatro.domain.repository.MealPlanRepository

/**
 * Removes a single meal plan entry by its identifier.
 */
class RemovePlanEntry(
    private val mealPlanRepository: MealPlanRepository,
) {
    /**
     * Deletes the entry identified by [entryId]. Blank identifiers are safely ignored.
     */
    suspend operator fun invoke(entryId: String) {
        if (entryId.isBlank()) return
        mealPlanRepository.deleteEntry(entryId)
    }
}
