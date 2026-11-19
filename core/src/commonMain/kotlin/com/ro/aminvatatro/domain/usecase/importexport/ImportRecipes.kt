package com.ro.aminvatatro.domain.usecase.importexport

import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.core.time.IdGenerator
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.data.importexport.RECIPE_EXPORT_SCHEMA_VERSION
import com.ro.aminvatatro.data.importexport.RecipeExportDto
import com.ro.aminvatatro.data.importexport.RecipeExportPayloadDto
import com.ro.aminvatatro.data.importexport.recipeExportJson
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.Step
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.validation.RecipeValidationError
import com.ro.aminvatatro.domain.validation.RecipeValidator
import com.ro.aminvatatro.domain.validation.ValidationResult
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Policy governing how imported recipes colliding with existing ones (by `id`, then by
 * normalized title) are handled.
 */
enum class DuplicatePolicy {
    /** Skip the incoming duplicate recipe and keep the existing one untouched. */
    SKIP,

    /** Import the incoming duplicate as a separate copy with freshly generated identifiers. */
    KEEP_BOTH,
}

/**
 * Typed validation error encountered while inspecting an incoming JSON import payload.
 */
sealed interface ImportValidationError {
    /** Ingredient at [ingredientIndex] in recipe [recipeIndex] used an unknown unit string. */
    data class UnknownUnit(
        val recipeIndex: Int,
        val ingredientIndex: Int,
        val rawValue: String,
    ) : ImportValidationError

    /** Ingredient at [ingredientIndex] in recipe [recipeIndex] used an unknown aisle string. */
    data class UnknownAisle(
        val recipeIndex: Int,
        val ingredientIndex: Int,
        val rawValue: String,
    ) : ImportValidationError

    /** Recipe at [recipeIndex] violated one or more [RecipeValidator] rules. */
    data class RecipeInvalid(
        val recipeIndex: Int,
        val recipeTitle: String,
        val errors: List<RecipeValidationError>,
    ) : ImportValidationError
}

/**
 * Outcome of importing recipes from a JSON payload.
 */
sealed interface ImportRecipesResult {
    /**
     * All recipes in the payload passed validation and accepted entries were persisted in a
     * single atomic transaction.
     */
    data class Success(
        val importedCount: Int,
        val skippedCount: Int,
        val duplicatedCount: Int,
    ) : ImportRecipesResult

    /** Input text was blank, syntactically invalid JSON, or structurally incompatible. */
    data class InvalidJson(val reason: String) : ImportRecipesResult

    /** Payload declared a `schemaVersion` not supported by this version of the application. */
    data class UnsupportedSchemaVersion(
        val found: Int,
        val supported: Int = RECIPE_EXPORT_SCHEMA_VERSION,
    ) : ImportRecipesResult

    /** One or more recipes failed validation; zero recipes were written. */
    data class ValidationFailed(val errors: List<ImportValidationError>) : ImportRecipesResult
}

/**
 * Parses, validates, deduplicates, and atomically imports recipes from a versioned JSON payload.
 *
 * Guarantees:
 * - Malformed JSON or unknown `schemaVersion` yields a typed error (never throws).
 * - Full validation of every recipe runs before any persistence call.
 * - Duplicate matching checks `id` first, then `normalizedTitle`.
 * - Accepted recipes are written in a single repository transaction.
 */
class ImportRecipes(
    private val recipeRepository: RecipeRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    private val json: Json = recipeExportJson,
) {
    /**
     * Imports recipes from [rawJson] applying [duplicatePolicy].
     */
    suspend operator fun invoke(
        rawJson: String,
        duplicatePolicy: DuplicatePolicy = DuplicatePolicy.SKIP,
    ): ImportRecipesResult {
        if (rawJson.isBlank()) {
            return ImportRecipesResult.InvalidJson("JSON input is blank.")
        }

        val rootElement = try {
            json.parseToJsonElement(rawJson)
        } catch (e: SerializationException) {
            return ImportRecipesResult.InvalidJson(e.message ?: "Malformed JSON.")
        } catch (e: IllegalArgumentException) {
            return ImportRecipesResult.InvalidJson(e.message ?: "Malformed JSON.")
        }

        val rootObject = rootElement as? JsonObject
            ?: return ImportRecipesResult.InvalidJson("JSON root must be an object.")

        val versionElement = rootObject["schemaVersion"] as? JsonPrimitive
            ?: return ImportRecipesResult.InvalidJson("Missing schemaVersion.")
        val schemaVersion = (if (versionElement.isString) null else versionElement.intOrNull)
            ?: return ImportRecipesResult.InvalidJson("Invalid schemaVersion.")

        if (schemaVersion != RECIPE_EXPORT_SCHEMA_VERSION) {
            return ImportRecipesResult.UnsupportedSchemaVersion(
                found = schemaVersion,
                supported = RECIPE_EXPORT_SCHEMA_VERSION,
            )
        }

        val payload = try {
            json.decodeFromJsonElement(RecipeExportPayloadDto.serializer(), rootObject)
        } catch (e: SerializationException) {
            return ImportRecipesResult.InvalidJson(e.message ?: "Invalid import payload structure.")
        } catch (e: IllegalArgumentException) {
            return ImportRecipesResult.InvalidJson(e.message ?: "Invalid import payload structure.")
        }

        val validationErrors = mutableListOf<ImportValidationError>()
        val validatedCandidates = mutableListOf<Recipe>()

        payload.recipes.forEachIndexed { recipeIndex, dto ->
            val recipe = parseAndValidateRecipe(dto, recipeIndex, validationErrors)
            if (recipe != null) {
                validatedCandidates += recipe
            }
        }

        if (validationErrors.isNotEmpty()) {
            return ImportRecipesResult.ValidationFailed(validationErrors)
        }

        val existingRecipes = recipeRepository.getAllRecipes()
        val knownRecipeIds = existingRecipes.mapTo(mutableSetOf()) { it.id }
        val knownNormalizedTitles = existingRecipes.mapTo(mutableSetOf()) { it.normalizedTitle }
        val knownIngredientIds = existingRecipes.flatMapTo(mutableSetOf()) { r -> r.ingredients.map { it.id } }
        val knownStepIds = existingRecipes.flatMapTo(mutableSetOf()) { r -> r.steps.map { it.id } }

        val now = clock.nowEpochMillis()
        val toSave = mutableListOf<Recipe>()
        var skippedCount = 0
        var duplicatedCount = 0

        for (candidate in validatedCandidates) {
            val isDuplicateById = candidate.id.isNotBlank() && candidate.id in knownRecipeIds
            val isDuplicateByTitle = candidate.normalizedTitle in knownNormalizedTitles
            val isDuplicate = isDuplicateById || isDuplicateByTitle

            if (isDuplicate) {
                when (duplicatePolicy) {
                    DuplicatePolicy.SKIP -> {
                        skippedCount++
                        continue
                    }

                    DuplicatePolicy.KEEP_BOTH -> {
                        duplicatedCount++
                        val copied = prepareRecipeForInsert(
                            candidate = candidate,
                            forceNewIds = true,
                            knownRecipeIds = knownRecipeIds,
                            knownIngredientIds = knownIngredientIds,
                            knownStepIds = knownStepIds,
                            now = now,
                        )
                        knownRecipeIds += copied.id
                        knownNormalizedTitles += copied.normalizedTitle
                        toSave += copied
                    }
                }
            } else {
                val prepared = prepareRecipeForInsert(
                    candidate = candidate,
                    forceNewIds = false,
                    knownRecipeIds = knownRecipeIds,
                    knownIngredientIds = knownIngredientIds,
                    knownStepIds = knownStepIds,
                    now = now,
                )
                knownRecipeIds += prepared.id
                knownNormalizedTitles += prepared.normalizedTitle
                toSave += prepared
            }
        }

        if (toSave.isNotEmpty()) {
            recipeRepository.saveRecipes(toSave)
        }

        return ImportRecipesResult.Success(
            importedCount = toSave.size,
            skippedCount = skippedCount,
            duplicatedCount = duplicatedCount,
        )
    }

    private fun parseAndValidateRecipe(
        dto: RecipeExportDto,
        recipeIndex: Int,
        errors: MutableList<ImportValidationError>,
    ): Recipe? {
        var hasEnumErrors = false
        val parsedIngredients = mutableListOf<Ingredient>()

        dto.ingredients.forEachIndexed { ingredientIndex, ingredientDto ->
            val unit = MeasurementUnit.entries.firstOrNull { it.name == ingredientDto.unit }
            val aisle = Aisle.entries.firstOrNull { it.name == ingredientDto.aisle }

            if (unit == null) {
                hasEnumErrors = true
                errors += ImportValidationError.UnknownUnit(
                    recipeIndex = recipeIndex,
                    ingredientIndex = ingredientIndex,
                    rawValue = ingredientDto.unit,
                )
            }
            if (aisle == null) {
                hasEnumErrors = true
                errors += ImportValidationError.UnknownAisle(
                    recipeIndex = recipeIndex,
                    ingredientIndex = ingredientIndex,
                    rawValue = ingredientDto.aisle,
                )
            }

            if (unit != null && aisle != null) {
                parsedIngredients += Ingredient(
                    id = ingredientDto.id,
                    position = ingredientIndex,
                    name = ingredientDto.name,
                    quantity = ingredientDto.quantity,
                    unit = unit,
                    aisle = aisle,
                )
            }
        }

        if (hasEnumErrors) return null

        val parsedSteps = dto.steps.mapIndexed { stepIndex, stepDto ->
            Step(
                id = stepDto.id,
                position = stepIndex,
                text = stepDto.text,
            )
        }

        val seenTagNames = mutableSetOf<String>()
        val parsedTags = dto.tags.mapNotNull { rawTag ->
            val trimmed = rawTag.trim()
            if (trimmed.isEmpty() || !seenTagNames.add(trimmed.lowercase())) {
                null
            } else {
                Tag(id = "", name = trimmed)
            }
        }

        val candidate = Recipe(
            id = dto.id.trim(),
            title = dto.title,
            description = dto.description,
            baseServings = dto.baseServings,
            prepMinutes = dto.prepMinutes,
            cookMinutes = dto.cookMinutes,
            ingredients = parsedIngredients,
            steps = parsedSteps,
            tags = parsedTags,
            notes = dto.notes,
            isFavorite = dto.isFavorite,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
        )

        val validation = RecipeValidator.validate(candidate)
        if (validation is ValidationResult.Invalid) {
            errors += ImportValidationError.RecipeInvalid(
                recipeIndex = recipeIndex,
                recipeTitle = dto.title,
                errors = validation.errors,
            )
            return null
        }

        return candidate
    }

    private fun prepareRecipeForInsert(
        candidate: Recipe,
        forceNewIds: Boolean,
        knownRecipeIds: MutableSet<String>,
        knownIngredientIds: MutableSet<String>,
        knownStepIds: MutableSet<String>,
        now: Long,
    ): Recipe {
        val recipeId = if (forceNewIds || candidate.id.isBlank() || candidate.id in knownRecipeIds) {
            allocateUniqueId(knownRecipeIds)
        } else {
            candidate.id
        }

        val preparedIngredients = candidate.ingredients.mapIndexed { index, ingredient ->
            val ingId = if (forceNewIds || ingredient.id.isBlank() || ingredient.id in knownIngredientIds) {
                allocateUniqueId(knownIngredientIds)
            } else {
                knownIngredientIds += ingredient.id
                ingredient.id
            }
            ingredient.copy(
                id = ingId,
                position = index,
                name = ingredient.name.trim(),
            )
        }

        val preparedSteps = candidate.steps.mapIndexed { index, step ->
            val stepId = if (forceNewIds || step.id.isBlank() || step.id in knownStepIds) {
                allocateUniqueId(knownStepIds)
            } else {
                knownStepIds += step.id
                step.id
            }
            step.copy(
                id = stepId,
                position = index,
                text = step.text.trim(),
            )
        }

        val preparedTags = candidate.tags.map { tag ->
            Tag(id = idGenerator.nextId(), name = tag.name.trim())
        }

        val createdAt = if (candidate.createdAt > 0L) candidate.createdAt else now
        val updatedAt = if (forceNewIds || candidate.updatedAt <= 0L) now else candidate.updatedAt

        return candidate.copy(
            id = recipeId,
            title = candidate.title.trim(),
            description = candidate.description.trim(),
            notes = candidate.notes.trim(),
            ingredients = preparedIngredients,
            steps = preparedSteps,
            tags = preparedTags,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun allocateUniqueId(taken: MutableSet<String>): String {
        var id = idGenerator.nextId()
        while (!taken.add(id)) {
            id = idGenerator.nextId()
        }
        return id
    }
}
