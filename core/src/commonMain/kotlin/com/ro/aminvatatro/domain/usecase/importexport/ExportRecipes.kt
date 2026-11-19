package com.ro.aminvatatro.domain.usecase.importexport

import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.data.importexport.RECIPE_EXPORT_SCHEMA_VERSION
import com.ro.aminvatatro.data.importexport.RecipeExportPayloadDto
import com.ro.aminvatatro.data.importexport.recipeExportJson
import com.ro.aminvatatro.data.importexport.toExportDto
import com.ro.aminvatatro.domain.repository.RecipeRepository
import kotlinx.serialization.json.Json

/**
 * Result of exporting recipes into a versioned JSON payload.
 */
data class ExportRecipesResult(
    val json: String,
    val recipeCount: Int,
    val schemaVersion: Int = RECIPE_EXPORT_SCHEMA_VERSION,
)

/**
 * Exports all stored recipes (or a selected subset) into a versioned JSON document
 * (`schemaVersion = 1`) stamped with `exportedAt` from the injected [Clock].
 */
class ExportRecipes(
    private val recipeRepository: RecipeRepository,
    private val clock: Clock,
    private val json: Json = recipeExportJson,
) {
    /**
     * Serializes all recipes when [recipeIds] is `null`, or only recipes whose `id` is in
     * [recipeIds] when non-null.
     */
    suspend operator fun invoke(recipeIds: Set<String>? = null): ExportRecipesResult {
        val allRecipes = recipeRepository.getAllRecipes()
        val selected = if (recipeIds == null) {
            allRecipes
        } else {
            allRecipes.filter { it.id in recipeIds }
        }

        val payload = RecipeExportPayloadDto(
            schemaVersion = RECIPE_EXPORT_SCHEMA_VERSION,
            exportedAt = clock.nowEpochMillis(),
            recipes = selected.map { it.toExportDto() },
        )

        val serialized = json.encodeToString(RecipeExportPayloadDto.serializer(), payload)
        return ExportRecipesResult(
            json = serialized,
            recipeCount = selected.size,
            schemaVersion = RECIPE_EXPORT_SCHEMA_VERSION,
        )
    }
}
