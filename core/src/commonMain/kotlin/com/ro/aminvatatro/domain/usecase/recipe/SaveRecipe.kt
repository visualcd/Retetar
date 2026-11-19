package com.ro.aminvatatro.domain.usecase.recipe

import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.core.time.IdGenerator
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.validation.RecipeValidationError
import com.ro.aminvatatro.domain.validation.RecipeValidator
import com.ro.aminvatatro.domain.validation.ValidationResult

/**
 * Result of attempting to validate and persist a [Recipe].
 */
sealed interface SaveRecipeResult {
    /** The recipe passed validation and was saved. */
    data class Success(val recipe: Recipe) : SaveRecipeResult

    /** One or more domain validation rules failed; nothing was written. */
    data class ValidationFailed(val errors: List<RecipeValidationError>) : SaveRecipeResult
}

/**
 * Validates and persists a new or edited [Recipe], assigning missing IDs, canonical
 * ingredient/step positions, and timestamps from the injected [Clock].
 */
class SaveRecipe(
    private val recipeRepository: RecipeRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) {
    /**
     * Validates [recipe] with [RecipeValidator] and persists it atomically on success.
     */
    suspend operator fun invoke(recipe: Recipe): SaveRecipeResult {
        val validation = RecipeValidator.validate(recipe)
        if (validation is ValidationResult.Invalid) {
            return SaveRecipeResult.ValidationFailed(validation.errors)
        }

        val now = clock.nowEpochMillis()
        val existing = if (recipe.id.isNotBlank()) recipeRepository.getRecipeById(recipe.id) else null
        val resolvedId = recipe.id.ifBlank { idGenerator.nextId() }
        val resolvedCreatedAt = when {
            existing != null && existing.createdAt > 0L -> existing.createdAt
            recipe.createdAt > 0L -> recipe.createdAt
            else -> now
        }

        val normalizedIngredients = recipe.ingredients.mapIndexed { index, ingredient ->
            ingredient.copy(
                id = ingredient.id.ifBlank { idGenerator.nextId() },
                position = index,
                name = ingredient.name.trim(),
            )
        }

        val normalizedSteps = recipe.steps.mapIndexed { index, step ->
            step.copy(
                id = step.id.ifBlank { idGenerator.nextId() },
                position = index,
                text = step.text.trim(),
            )
        }

        val seenTagNames = mutableSetOf<String>()
        val normalizedTags = recipe.tags.mapNotNull { tag ->
            val trimmedName = tag.name.trim()
            if (trimmedName.isEmpty() || !seenTagNames.add(trimmedName.lowercase())) {
                null
            } else {
                Tag(
                    id = tag.id.ifBlank { idGenerator.nextId() },
                    name = trimmedName,
                )
            }
        }

        val prepared = recipe.copy(
            id = resolvedId,
            title = recipe.title.trim(),
            description = recipe.description.trim(),
            notes = recipe.notes.trim(),
            ingredients = normalizedIngredients,
            steps = normalizedSteps,
            tags = normalizedTags,
            createdAt = resolvedCreatedAt,
            updatedAt = now,
        )

        recipeRepository.saveRecipe(prepared)
        return SaveRecipeResult.Success(prepared)
    }
}
