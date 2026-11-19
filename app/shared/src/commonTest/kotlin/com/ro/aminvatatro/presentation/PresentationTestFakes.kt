package com.ro.aminvatatro.presentation

import com.ro.aminvatatro.core.text.TextNormalizer
import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.core.time.IdGenerator
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.domain.model.AppTheme
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.ShoppingItem
import com.ro.aminvatatro.domain.model.Step
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.repository.AppSettings
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import com.ro.aminvatatro.domain.repository.RecipeFilter
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.repository.SettingsRepository
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

internal class FakeClock(
    var currentMillis: Long = 1_700_000_000_000L,
    var currentDate: LocalDate = LocalDate(2026, 3, 4), // Wednesday in ISO week 10 (Mon 2026-03-02)
) : Clock {
    override fun nowEpochMillis(): Long = currentMillis
    override fun today(): LocalDate = currentDate
}

internal class SequenceIdGenerator(
    private val prefix: String = "id",
) : IdGenerator {
    private var counter = 0
    override fun nextId(): String = "$prefix-${++counter}"
}

internal class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsRepository {
    private val _settings = MutableStateFlow(initial)
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    override suspend fun setTheme(theme: AppTheme) {
        _settings.update { it.copy(theme = theme) }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        _settings.update { it.copy(language = language) }
    }

    override suspend fun setUnitSystem(unitSystem: UnitSystem) {
        _settings.update { it.copy(unitSystem = unitSystem) }
    }

    override suspend fun setDemoDataLoaded(loaded: Boolean) {
        _settings.update { it.copy(demoDataLoaded = loaded) }
    }
}

internal class FakeMealPlanRepository : MealPlanRepository {
    val entriesFlow = MutableStateFlow<List<MealPlanEntry>>(emptyList())

    override fun observeEntriesForRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<MealPlanEntry>> = entriesFlow.map { list ->
        list.filter { it.date in startDate..endDate }
            .sortedWith(compareBy<MealPlanEntry>({ it.date }, { it.slot.ordinal }, { it.id }))
    }

    override suspend fun getEntriesForRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MealPlanEntry> = entriesFlow.value
        .filter { it.date in startDate..endDate }
        .sortedWith(compareBy<MealPlanEntry>({ it.date }, { it.slot.ordinal }, { it.id }))

    override suspend fun getEntriesForDate(date: LocalDate): List<MealPlanEntry> =
        entriesFlow.value
            .filter { it.date == date }
            .sortedWith(compareBy<MealPlanEntry>({ it.slot.ordinal }, { it.id }))

    override suspend fun upsertEntry(entry: MealPlanEntry) {
        entriesFlow.update { current ->
            current.filterNot { it.id == entry.id } + entry
        }
    }

    override suspend fun replaceEntriesForDate(date: LocalDate, entries: List<MealPlanEntry>) {
        entriesFlow.update { current ->
            current.filterNot { it.date == date } + entries
        }
    }

    override suspend fun replaceEntriesForRange(
        startDate: LocalDate,
        endDate: LocalDate,
        entries: List<MealPlanEntry>,
    ) {
        entriesFlow.update { current ->
            current.filterNot { it.date in startDate..endDate } + entries
        }
    }

    override suspend fun updateServings(entryId: String, servings: Int) {
        entriesFlow.update { current ->
            current.map { if (it.id == entryId) it.copy(servings = servings) else it }
        }
    }

    override suspend fun deleteEntry(entryId: String) {
        entriesFlow.update { current -> current.filterNot { it.id == entryId } }
    }

    override suspend fun deleteAllEntries() {
        entriesFlow.value = emptyList()
    }

    fun deleteEntriesForRecipe(recipeId: String) {
        entriesFlow.update { current -> current.filterNot { it.recipeId == recipeId } }
    }

    fun countEntriesForRecipe(recipeId: String): Int =
        entriesFlow.value.count { it.recipeId == recipeId }
}

internal class FakeRecipeRepository(
    private val mealPlanRepository: FakeMealPlanRepository? = null,
) : RecipeRepository {
    val recipesFlow = MutableStateFlow<List<Recipe>>(emptyList())

    override fun observeRecipes(filter: RecipeFilter): Flow<List<Recipe>> {
        val normalizedQuery = TextNormalizer.normalize(filter.query)
        val requiredTags = filter.tagNames
            .map { TextNormalizer.normalize(it) }
            .filter { it.isNotEmpty() }
            .toSet()

        return recipesFlow.map { recipes ->
            recipes.filter { recipe ->
                val queryMatches = normalizedQuery.isEmpty() ||
                    recipe.normalizedTitle.contains(normalizedQuery) ||
                    recipe.ingredients.any { it.normalizedName.contains(normalizedQuery) }
                val favMatches = !filter.favoritesOnly || recipe.isFavorite
                val maxMinutes = filter.maxTotalMinutes
                val timeMatches = maxMinutes == null || recipe.totalMinutes <= maxMinutes
                val tagsMatch = requiredTags.isEmpty() || run {
                    val recipeTags = recipe.tags.map { TextNormalizer.normalize(it.name) }.toSet()
                    requiredTags.all { it in recipeTags }
                }
                queryMatches && favMatches && timeMatches && tagsMatch
            }.sortedWith(compareByDescending<Recipe> { it.updatedAt }.thenBy { it.title.lowercase() })
        }
    }

    override fun observeRecipe(id: String): Flow<Recipe?> =
        recipesFlow.map { list -> list.firstOrNull { it.id == id } }

    override fun observeAllTags(): Flow<List<Tag>> =
        recipesFlow.map { list ->
            list.flatMap { it.tags }.distinctBy { it.name }.sortedBy { it.name.lowercase() }
        }

    override suspend fun getRecipeById(id: String): Recipe? =
        recipesFlow.value.firstOrNull { it.id == id }

    override suspend fun getAllRecipes(): List<Recipe> =
        recipesFlow.value.sortedWith(compareByDescending<Recipe> { it.updatedAt }.thenBy { it.title.lowercase() })

    override suspend fun saveRecipe(recipe: Recipe) {
        recipesFlow.update { current ->
            current.filterNot { it.id == recipe.id } + recipe
        }
    }

    override suspend fun saveRecipes(recipes: List<Recipe>) {
        val incomingIds = recipes.map { it.id }.toSet()
        recipesFlow.update { current ->
            current.filterNot { it.id in incomingIds } + recipes
        }
    }

    override suspend fun deleteRecipe(id: String) {
        recipesFlow.update { current -> current.filterNot { it.id == id } }
        mealPlanRepository?.deleteEntriesForRecipe(id)
    }

    override suspend fun setFavorite(id: String, isFavorite: Boolean, updatedAt: Long) {
        recipesFlow.update { current ->
            current.map {
                if (it.id == id) it.copy(isFavorite = isFavorite, updatedAt = updatedAt) else it
            }
        }
    }

    override suspend fun countMealPlanEntriesForRecipe(recipeId: String): Int =
        mealPlanRepository?.countEntriesForRecipe(recipeId) ?: 0

    override suspend fun deleteAllRecipes() {
        recipesFlow.value = emptyList()
        mealPlanRepository?.deleteAllEntries()
    }
}

internal class FakeShoppingRepository : ShoppingRepository {
    val itemsFlow = MutableStateFlow<List<ShoppingItem>>(emptyList())

    override fun observeItemsForWeek(weekStart: LocalDate): Flow<List<ShoppingItem>> =
        itemsFlow.map { list -> list.filter { it.weekStart == weekStart } }

    override suspend fun getItemsForWeek(weekStart: LocalDate): List<ShoppingItem> =
        itemsFlow.value.filter { it.weekStart == weekStart }

    override suspend fun upsertItem(item: ShoppingItem) {
        itemsFlow.update { current ->
            current.filterNot { it.id == item.id } + item
        }
    }

    override suspend fun replaceItemsForWeek(weekStart: LocalDate, items: List<ShoppingItem>) {
        itemsFlow.update { current ->
            current.filterNot { it.weekStart == weekStart } + items
        }
    }

    override suspend fun setChecked(itemId: String, checked: Boolean) {
        itemsFlow.update { current ->
            current.map { if (it.id == itemId) it.copy(checked = checked) else it }
        }
    }

    override suspend fun deleteItem(itemId: String) {
        itemsFlow.update { current -> current.filterNot { it.id == itemId } }
    }

    override suspend fun clearCheckedForWeek(weekStart: LocalDate) {
        itemsFlow.update { current ->
            current.filterNot { it.weekStart == weekStart && it.checked }
        }
    }

    override suspend fun deleteAllItems() {
        itemsFlow.value = emptyList()
    }
}

internal fun sampleRecipe(
    id: String = "rec-1",
    title: String = "Clătite cu brânză",
    baseServings: Int = 4,
    prepMinutes: Int = 10,
    cookMinutes: Int = 20,
    ingredients: List<Ingredient> = listOf(
        Ingredient("ing-1", 0, "Făină", 250.0, MeasurementUnit.G, Aisle.PANTRY),
        Ingredient("ing-2", 1, "Ouă", 2.0, MeasurementUnit.PIECE, Aisle.DAIRY),
        Ingredient("ing-3", 2, "Sare", null, MeasurementUnit.TO_TASTE, Aisle.SPICES),
    ),
    steps: List<Step> = listOf(
        Step("step-1", 0, "Amestecă ingredientele."),
        Step("step-2", 1, "Coace pe tigaie."),
    ),
    tags: List<Tag> = listOf(Tag("tag-1", "Desert")),
    isFavorite: Boolean = false,
    createdAt: Long = 1_000L,
    updatedAt: Long = 1_000L,
): Recipe = Recipe(
    id = id,
    title = title,
    description = "Descriere $title",
    baseServings = baseServings,
    prepMinutes = prepMinutes,
    cookMinutes = cookMinutes,
    ingredients = ingredients,
    steps = steps,
    tags = tags,
    notes = "Note $title",
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
