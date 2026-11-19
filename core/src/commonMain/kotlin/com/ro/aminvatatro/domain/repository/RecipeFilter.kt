package com.ro.aminvatatro.domain.repository

/**
 * Filter and search criteria for querying recipes.
 *
 * @property query Free-text search matched against normalized recipe titles and ingredient names.
 * @property tagNames Optional set of required tag names (all specified tags must match, case-insensitive).
 * @property maxTotalMinutes Optional upper bound (inclusive) on `prepMinutes + cookMinutes`.
 * @property favoritesOnly When `true`, only favorite recipes are returned.
 */
data class RecipeFilter(
    val query: String = "",
    val tagNames: Set<String> = emptySet(),
    val maxTotalMinutes: Int? = null,
    val favoritesOnly: Boolean = false,
)
