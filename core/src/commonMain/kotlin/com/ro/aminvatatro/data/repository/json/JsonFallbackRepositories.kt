package com.ro.aminvatatro.data.repository.json

import com.ro.aminvatatro.core.text.TextNormalizer
import com.ro.aminvatatro.data.local.JsonKeyValueStore
import com.ro.aminvatatro.data.repository.parseAisle
import com.ro.aminvatatro.data.repository.parseMealSlot
import com.ro.aminvatatro.data.repository.parseMeasurementUnit
import com.ro.aminvatatro.data.repository.parseShoppingItemSource
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.ShoppingItem
import com.ro.aminvatatro.domain.model.Step
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import com.ro.aminvatatro.domain.repository.RecipeFilter
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredIngredientDto(
    val id: String,
    val position: Int,
    val name: String,
    val quantity: Double?,
    val unit: String,
    val aisle: String,
)

@Serializable
internal data class StoredStepDto(
    val id: String,
    val position: Int,
    val text: String,
)

@Serializable
internal data class StoredTagDto(
    val id: String,
    val name: String,
)

@Serializable
internal data class StoredRecipeDto(
    val id: String,
    val title: String,
    val description: String = "",
    val baseServings: Int,
    val prepMinutes: Int = 0,
    val cookMinutes: Int = 0,
    val ingredients: List<StoredIngredientDto>,
    val steps: List<StoredStepDto>,
    val tags: List<StoredTagDto> = emptyList(),
    val notes: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

@Serializable
internal data class StoredMealPlanEntryDto(
    val id: String,
    val date: String,
    val slot: String,
    val recipeId: String,
    val servings: Int,
)

@Serializable
internal data class StoredShoppingItemDto(
    val id: String,
    val weekStart: String,
    val itemKey: String,
    val name: String,
    val normalizedName: String,
    val quantity: Double?,
    val unit: String,
    val aisle: String,
    val source: String,
    val checked: Boolean = false,
)

@Serializable
internal data class JsonDatabaseSnapshot(
    val schemaVersion: Int = 1,
    val recipes: List<StoredRecipeDto> = emptyList(),
    val mealPlanEntries: List<StoredMealPlanEntryDto> = emptyList(),
    val shoppingItems: List<StoredShoppingItemDto> = emptyList(),
)

/**
 * Shared atomic JSON document store backing all Web (`localStorage`) fallback repositories.
 *
 * Holding recipes, meal plan entries, and shopping items in a single versioned snapshot ensures
 * atomic updates and consistent cascade deletion of dependent meal plan entries when recipes are deleted.
 */
class JsonDatabaseStorage(
    private val keyValueStore: JsonKeyValueStore,
    private val storageKey: String = DEFAULT_STORAGE_KEY,
) {
    internal val snapshotFlow: MutableStateFlow<JsonDatabaseSnapshot> = MutableStateFlow(loadInitial())

    internal fun mutate(transform: (JsonDatabaseSnapshot) -> JsonDatabaseSnapshot) {
        snapshotFlow.update { current ->
            val updated = transform(current)
            keyValueStore.save(storageKey, updated, JsonDatabaseSnapshot.serializer())
            updated
        }
    }

    private fun loadInitial(): JsonDatabaseSnapshot =
        keyValueStore.load(storageKey, JsonDatabaseSnapshot.serializer())
            .getOrNull()
            ?: JsonDatabaseSnapshot()

    companion object {
        const val DEFAULT_STORAGE_KEY: String = "retetar.db.v1"
    }
}

/**
 * JSON-backed [RecipeRepository] used as the Web (`localStorage`) persistence fallback.
 */
class JsonRecipeRepository(
    private val storage: JsonDatabaseStorage,
) : RecipeRepository {

    override fun observeRecipes(filter: RecipeFilter): Flow<List<Recipe>> {
        val normalizedQuery = TextNormalizer.normalize(filter.query)
        val requiredTags = filter.tagNames
            .map { TextNormalizer.normalize(it) }
            .filter { it.isNotEmpty() }
            .toSet()

        return storage.snapshotFlow.map { snapshot ->
            snapshot.recipes
                .map { it.toDomain() }
                .filter { recipe ->
                    val queryMatches = normalizedQuery.isEmpty() ||
                        recipe.normalizedTitle.contains(normalizedQuery) ||
                        recipe.ingredients.any { it.normalizedName.contains(normalizedQuery) }
                    val favMatches = !filter.favoritesOnly || recipe.isFavorite
                    val timeMatches = filter.maxTotalMinutes == null || recipe.totalMinutes <= filter.maxTotalMinutes
                    val tagsMatch = requiredTags.isEmpty() || run {
                        val recipeTags = recipe.tags.map { TextNormalizer.normalize(it.name) }.toSet()
                        requiredTags.all { it in recipeTags }
                    }
                    queryMatches && favMatches && timeMatches && tagsMatch
                }
                .sortedWith(compareByDescending<Recipe> { it.updatedAt }.thenBy { it.title.lowercase() })
        }
    }

    override fun observeRecipe(id: String): Flow<Recipe?> =
        storage.snapshotFlow.map { snapshot ->
            snapshot.recipes.firstOrNull { it.id == id }?.toDomain()
        }

    override fun observeAllTags(): Flow<List<Tag>> =
        storage.snapshotFlow.map { snapshot ->
            snapshot.recipes
                .flatMap { it.tags }
                .distinctBy { it.name }
                .map { Tag(id = it.id, name = it.name) }
                .sortedBy { it.name.lowercase() }
        }

    override suspend fun getRecipeById(id: String): Recipe? =
        storage.snapshotFlow.value.recipes.firstOrNull { it.id == id }?.toDomain()

    override suspend fun getAllRecipes(): List<Recipe> =
        storage.snapshotFlow.value.recipes
            .map { it.toDomain() }
            .sortedWith(compareByDescending<Recipe> { it.updatedAt }.thenBy { it.title.lowercase() })

    override suspend fun saveRecipe(recipe: Recipe) {
        saveRecipes(listOf(recipe))
    }

    override suspend fun saveRecipes(recipes: List<Recipe>) {
        if (recipes.isEmpty()) return
        storage.mutate { snapshot ->
            val byId = snapshot.recipes.associateBy { it.id }.toMutableMap()
            recipes.forEach { recipe ->
                val existing = byId[recipe.id]
                byId[recipe.id] = recipe.toDto(preservedCreatedAt = existing?.createdAt ?: recipe.createdAt)
            }
            snapshot.copy(recipes = byId.values.toList())
        }
    }

    override suspend fun deleteRecipe(id: String) {
        storage.mutate { snapshot ->
            snapshot.copy(
                recipes = snapshot.recipes.filterNot { it.id == id },
                mealPlanEntries = snapshot.mealPlanEntries.filterNot { it.recipeId == id },
            )
        }
    }

    override suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long) {
        storage.mutate { snapshot ->
            snapshot.copy(
                recipes = snapshot.recipes.map { dto ->
                    if (dto.id == id) dto.copy(isFavorite = isFavorite, updatedAt = updatedAt) else dto
                },
            )
        }
    }

    override suspend fun countMealPlanEntriesForRecipe(recipeId: String): Int =
        storage.snapshotFlow.value.mealPlanEntries.count { it.recipeId == recipeId }

    override suspend fun deleteAllRecipes() {
        storage.mutate { snapshot ->
            snapshot.copy(
                recipes = emptyList(),
                mealPlanEntries = emptyList(),
            )
        }
    }
}

/**
 * JSON-backed [MealPlanRepository] used as the Web (`localStorage`) persistence fallback.
 */
class JsonMealPlanRepository(
    private val storage: JsonDatabaseStorage,
) : MealPlanRepository {

    override fun observeEntriesForRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<MealPlanEntry>> = storage.snapshotFlow.map { snapshot ->
        snapshot.mealPlanEntries
            .mapNotNull { it.toDomainOrNull() }
            .filter { it.date in startDate..endDate }
            .sortedWith(compareBy<MealPlanEntry> { it.date }.thenBy { it.slot.name }.thenBy { it.id })
    }

    override suspend fun getEntriesForRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MealPlanEntry> = storage.snapshotFlow.value.mealPlanEntries
        .mapNotNull { it.toDomainOrNull() }
        .filter { it.date in startDate..endDate }
        .sortedWith(compareBy<MealPlanEntry> { it.date }.thenBy { it.slot.name }.thenBy { it.id })

    override suspend fun getEntriesForDate(date: LocalDate): List<MealPlanEntry> =
        getEntriesForRange(date, date)

    override suspend fun upsertEntry(entry: MealPlanEntry) {
        storage.mutate { snapshot ->
            // Respect foreign key parity: ignore entry if referenced recipe does not exist
            if (snapshot.recipes.none { it.id == entry.recipeId }) {
                throw IllegalStateException("Foreign key violation: recipe '${entry.recipeId}' does not exist")
            }
            val updated = snapshot.mealPlanEntries.filterNot { it.id == entry.id } + entry.toDto()
            snapshot.copy(mealPlanEntries = updated)
        }
    }

    override suspend fun replaceEntriesForDate(date: LocalDate, entries: List<MealPlanEntry>) {
        replaceEntriesForRange(date, date, entries)
    }

    override suspend fun replaceEntriesForRange(
        startDate: LocalDate,
        endDate: LocalDate,
        entries: List<MealPlanEntry>,
    ) {
        storage.mutate { snapshot ->
            val validRecipeIds = snapshot.recipes.map { it.id }.toSet()
            entries.forEach { entry ->
                if (entry.recipeId !in validRecipeIds) {
                    throw IllegalStateException("Foreign key violation: recipe '${entry.recipeId}' does not exist")
                }
            }
            val remaining = snapshot.mealPlanEntries.filterNot { dto ->
                val d = dto.toDomainOrNull()?.date
                d != null && d in startDate..endDate
            }
            snapshot.copy(mealPlanEntries = remaining + entries.map { it.toDto() })
        }
    }

    override suspend fun updateServings(entryId: String, servings: Int) {
        storage.mutate { snapshot ->
            snapshot.copy(
                mealPlanEntries = snapshot.mealPlanEntries.map { dto ->
                    if (dto.id == entryId) dto.copy(servings = servings) else dto
                },
            )
        }
    }

    override suspend fun deleteEntry(entryId: String) {
        storage.mutate { snapshot ->
            snapshot.copy(mealPlanEntries = snapshot.mealPlanEntries.filterNot { it.id == entryId })
        }
    }

    override suspend fun deleteAllEntries() {
        storage.mutate { snapshot -> snapshot.copy(mealPlanEntries = emptyList()) }
    }
}

/**
 * JSON-backed [ShoppingRepository] used as the Web (`localStorage`) persistence fallback.
 */
class JsonShoppingRepository(
    private val storage: JsonDatabaseStorage,
) : ShoppingRepository {

    override fun observeItemsForWeek(weekStart: LocalDate): Flow<List<ShoppingItem>> {
        val weekStr = weekStart.toString()
        return storage.snapshotFlow.map { snapshot ->
            snapshot.shoppingItems
                .filter { it.weekStart == weekStr }
                .mapNotNull { it.toDomainOrNull() }
                .sortedWith(compareBy<ShoppingItem> { it.aisle.name }.thenBy { it.name.lowercase() })
        }
    }

    override suspend fun getItemsForWeek(weekStart: LocalDate): List<ShoppingItem> {
        val weekStr = weekStart.toString()
        return storage.snapshotFlow.value.shoppingItems
            .filter { it.weekStart == weekStr }
            .mapNotNull { it.toDomainOrNull() }
            .sortedWith(compareBy<ShoppingItem> { it.aisle.name }.thenBy { it.name.lowercase() })
    }

    override suspend fun upsertItem(item: ShoppingItem) {
        storage.mutate { snapshot ->
            val updated = snapshot.shoppingItems.filterNot { it.id == item.id } + item.toDto()
            snapshot.copy(shoppingItems = updated)
        }
    }

    override suspend fun replaceItemsForWeek(weekStart: LocalDate, items: List<ShoppingItem>) {
        val weekStr = weekStart.toString()
        storage.mutate { snapshot ->
            val remaining = snapshot.shoppingItems.filterNot { it.weekStart == weekStr }
            snapshot.copy(shoppingItems = remaining + items.map { it.copy(weekStart = weekStart).toDto() })
        }
    }

    override suspend fun setChecked(itemId: String, checked: Boolean) {
        storage.mutate { snapshot ->
            snapshot.copy(
                shoppingItems = snapshot.shoppingItems.map { dto ->
                    if (dto.id == itemId) dto.copy(checked = checked) else dto
                },
            )
        }
    }

    override suspend fun deleteItem(itemId: String) {
        storage.mutate { snapshot ->
            snapshot.copy(shoppingItems = snapshot.shoppingItems.filterNot { it.id == itemId })
        }
    }

    override suspend fun clearCheckedForWeek(weekStart: LocalDate) {
        val weekStr = weekStart.toString()
        storage.mutate { snapshot ->
            snapshot.copy(
                shoppingItems = snapshot.shoppingItems.filterNot { it.weekStart == weekStr && it.checked },
            )
        }
    }

    override suspend fun deleteAllItems() {
        storage.mutate { snapshot -> snapshot.copy(shoppingItems = emptyList()) }
    }
}

private fun Recipe.toDto(preservedCreatedAt: Long): StoredRecipeDto = StoredRecipeDto(
    id = id,
    title = title.trim(),
    description = description.trim(),
    baseServings = baseServings,
    prepMinutes = prepMinutes,
    cookMinutes = cookMinutes,
    ingredients = ingredients.mapIndexed { idx, ing ->
        StoredIngredientDto(
            id = ing.id,
            position = idx,
            name = ing.name.trim(),
            quantity = ing.quantity,
            unit = ing.unit.name,
            aisle = ing.aisle.name,
        )
    },
    steps = steps.mapIndexed { idx, step ->
        StoredStepDto(
            id = step.id,
            position = idx,
            text = step.text.trim(),
        )
    },
    tags = tags
        .map { it.copy(name = it.name.trim()) }
        .filter { it.name.isNotEmpty() }
        .distinctBy { it.name }
        .map { StoredTagDto(id = it.id, name = it.name) },
    notes = notes.trim(),
    isFavorite = isFavorite,
    createdAt = preservedCreatedAt,
    updatedAt = updatedAt,
)

private fun StoredRecipeDto.toDomain(): Recipe = Recipe(
    id = id,
    title = title,
    description = description,
    baseServings = baseServings,
    prepMinutes = prepMinutes,
    cookMinutes = cookMinutes,
    ingredients = ingredients.sortedBy { it.position }.map {
        Ingredient(
            id = it.id,
            position = it.position,
            name = it.name,
            quantity = it.quantity,
            unit = parseMeasurementUnit(it.unit),
            aisle = parseAisle(it.aisle),
        )
    },
    steps = steps.sortedBy { it.position }.map {
        Step(id = it.id, position = it.position, text = it.text)
    },
    tags = tags.map { Tag(id = it.id, name = it.name) },
    notes = notes,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun MealPlanEntry.toDto(): StoredMealPlanEntryDto = StoredMealPlanEntryDto(
    id = id,
    date = date.toString(),
    slot = slot.name,
    recipeId = recipeId,
    servings = servings,
)

private fun StoredMealPlanEntryDto.toDomainOrNull(): MealPlanEntry? = runCatching {
    MealPlanEntry(
        id = id,
        date = LocalDate.parse(date),
        slot = parseMealSlot(slot),
        recipeId = recipeId,
        servings = servings,
    )
}.getOrNull()

private fun ShoppingItem.toDto(): StoredShoppingItemDto = StoredShoppingItemDto(
    id = id,
    weekStart = weekStart.toString(),
    itemKey = itemKey,
    name = name.trim(),
    normalizedName = normalizedName,
    quantity = quantity,
    unit = unit.name,
    aisle = aisle.name,
    source = source.name,
    checked = checked,
)

private fun StoredShoppingItemDto.toDomainOrNull(): ShoppingItem? = runCatching {
    ShoppingItem(
        id = id,
        weekStart = LocalDate.parse(weekStart),
        itemKey = itemKey,
        name = name,
        normalizedName = normalizedName,
        quantity = quantity,
        unit = parseMeasurementUnit(unit),
        aisle = parseAisle(aisle),
        source = parseShoppingItemSource(source),
        checked = checked,
    )
}.getOrNull()
