package com.ro.aminvatatro.data.importexport

import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.Step
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Current JSON export/import schema version supported by the application.
 */
const val RECIPE_EXPORT_SCHEMA_VERSION: Int = 1

/**
 * Default [Json] configuration for recipe import/export.
 *
 * `encodeDefaults = true` guarantees that `schemaVersion` and default fields are always written
 * explicitly into exported payloads.
 */
val recipeExportJson: Json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = false
}

/**
 * Root JSON document for exporting and importing recipes.
 */
@Serializable
data class RecipeExportPayloadDto(
    val schemaVersion: Int = RECIPE_EXPORT_SCHEMA_VERSION,
    val exportedAt: Long,
    val recipes: List<RecipeExportDto>,
)

/**
 * Serializable representation of a [Recipe] inside an import/export payload.
 */
@Serializable
data class RecipeExportDto(
    val id: String = "",
    val title: String,
    val description: String = "",
    val baseServings: Int,
    val prepMinutes: Int = 0,
    val cookMinutes: Int = 0,
    val ingredients: List<IngredientExportDto>,
    val steps: List<StepExportDto>,
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

/**
 * Serializable representation of an [Ingredient] inside an import/export payload.
 */
@Serializable
data class IngredientExportDto(
    val id: String = "",
    val position: Int = 0,
    val name: String,
    val quantity: Double? = null,
    val unit: String,
    val aisle: String = Aisle.OTHER.name,
)

/**
 * Serializable representation of a [Step] inside an import/export payload.
 */
@Serializable
data class StepExportDto(
    val id: String = "",
    val position: Int = 0,
    val text: String,
)

/**
 * Converts a domain [Recipe] into a portable [RecipeExportDto].
 */
fun Recipe.toExportDto(): RecipeExportDto = RecipeExportDto(
    id = id,
    title = title,
    description = description,
    baseServings = baseServings,
    prepMinutes = prepMinutes,
    cookMinutes = cookMinutes,
    ingredients = ingredients.mapIndexed { index, ingredient ->
        IngredientExportDto(
            id = ingredient.id,
            position = index,
            name = ingredient.name,
            quantity = ingredient.quantity,
            unit = ingredient.unit.name,
            aisle = ingredient.aisle.name,
        )
    },
    steps = steps.mapIndexed { index, step ->
        StepExportDto(
            id = step.id,
            position = index,
            text = step.text,
        )
    },
    tags = tags.map { it.name },
    notes = notes,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
