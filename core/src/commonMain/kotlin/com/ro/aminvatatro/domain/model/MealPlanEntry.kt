package com.ro.aminvatatro.domain.model

import kotlinx.datetime.LocalDate

/**
 * Scheduled meal entry placing a [recipeId] in a specific [date] and [slot] for [servings] people.
 */
data class MealPlanEntry(
    val id: String,
    val date: LocalDate,
    val slot: MealSlot,
    val recipeId: String,
    val servings: Int,
)
