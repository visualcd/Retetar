package com.ro.aminvatatro.data.repository

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.core.time.IdGenerator
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.core.units.UnitConverter
import com.ro.aminvatatro.data.demo.DemoRecipes
import com.ro.aminvatatro.data.local.DriverFactory
import com.ro.aminvatatro.data.local.db.RetetarDatabase
import com.ro.aminvatatro.di.coreModules
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.ShoppingItem
import com.ro.aminvatatro.domain.model.ShoppingItemSource
import com.ro.aminvatatro.domain.model.Step
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import com.ro.aminvatatro.domain.repository.RecipeFilter
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.repository.SettingsRepository
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlDelightPersistenceIntegrationTest {

    private fun createInMemoryDatabase(): Pair<JdbcSqliteDriver, RetetarDatabase> {
        val driver = requireNotNull(DriverFactory.inMemory().createDriver()) as JdbcSqliteDriver
        return driver to RetetarDatabase(driver)
    }

    private fun sampleRecipe(
        id: String = "rec-1",
        title: String = "Clătite cu brânză",
        isFavorite: Boolean = false,
        tags: List<Tag> = listOf(Tag("tag-1", "desert"), Tag("tag-2", "rapid")),
    ): Recipe = Recipe(
        id = id,
        title = title,
        description = "Rețetă clasică",
        baseServings = 4,
        prepMinutes = 10,
        cookMinutes = 20,
        ingredients = listOf(
            Ingredient("ing-1-$id", 0, "Făină", 250.0, MeasurementUnit.G, Aisle.PANTRY),
            Ingredient("ing-2-$id", 1, "Ouă", 3.0, MeasurementUnit.PIECE, Aisle.DAIRY),
            Ingredient("ing-3-$id", 2, "Sare", null, MeasurementUnit.TO_TASTE, Aisle.SPICES),
        ),
        steps = listOf(
            Step("step-1-$id", 0, "Amestecă ingredientele."),
            Step("step-2-$id", 1, "Coace clătitele în tigaie."),
        ),
        tags = tags,
        notes = "Servește cald.",
        isFavorite = isFavorite,
        createdAt = 1000L,
        updatedAt = 2000L,
    )

    @Test
    fun foreignKeysPragmaIsEnabledAndEnforced() = runTest {
        val (driver, db) = createInMemoryDatabase()
        driver.use {
            val fkEnabled = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA foreign_keys;",
                mapper = { cursor ->
                    QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null)
                },
                parameters = 0,
            ).value
            assertEquals(1L, fkEnabled, "PRAGMA foreign_keys must be ON (1)")

            val mealPlanRepo = SqlDelightMealPlanRepository(db, Dispatchers.Unconfined)
            assertFails {
                mealPlanRepo.upsertEntry(
                    MealPlanEntry(
                        id = "orphan-entry",
                        date = LocalDate(2026, 5, 4),
                        slot = MealSlot.LUNCH,
                        recipeId = "non-existent-recipe",
                        servings = 2,
                    ),
                )
            }
        }
    }

    @Test
    fun crudRecipeWithIngredientsStepsAndTagsInSingleTransactionPreservesMealPlanOnUpdate() = runTest {
        val (driver, db) = createInMemoryDatabase()
        driver.use {
            val recipeRepo = SqlDelightRecipeRepository(db, Dispatchers.Unconfined)
            val planRepo = SqlDelightMealPlanRepository(db, Dispatchers.Unconfined)

            val initial = sampleRecipe()
            recipeRepo.saveRecipe(initial)

            val loaded = recipeRepo.getRecipeById(initial.id)
            assertNotNull(loaded)
            assertEquals(initial.title, loaded.title)
            assertEquals(3, loaded.ingredients.size)
            assertEquals(2, loaded.steps.size)
            assertEquals(setOf("desert", "rapid"), loaded.tags.map { it.name }.toSet())

            // Attach a meal plan entry before updating the recipe
            planRepo.upsertEntry(
                MealPlanEntry(
                    id = "plan-1",
                    date = LocalDate(2026, 5, 4),
                    slot = MealSlot.BREAKFAST,
                    recipeId = initial.id,
                    servings = 4,
                ),
            )
            assertEquals(1, recipeRepo.countMealPlanEntriesForRecipe(initial.id))

            // Update recipe in-place (must NOT cascade-delete the MealPlanEntry)
            val updated = initial.copy(
                title = "Clătite pufoase",
                baseServings = 6,
                ingredients = listOf(
                    Ingredient("ing-new-1", 0, "Făină integrală", 300.0, MeasurementUnit.G, Aisle.PANTRY),
                ),
                steps = listOf(Step("step-new-1", 0, "Prepară aluatul și coace.")),
                tags = listOf(Tag("tag-3", "mic dejun")),
                updatedAt = 3000L,
            )
            recipeRepo.saveRecipe(updated)

            recipeRepo.observeRecipe(initial.id).test {
                val observed = awaitItem()
                assertNotNull(observed)
                assertEquals("Clătite pufoase", observed.title)
                assertEquals(6, observed.baseServings)
                assertEquals(1, observed.ingredients.size)
                assertEquals("Făină integrală", observed.ingredients.single().name)
                assertEquals(1, observed.steps.size)
                assertEquals(listOf("mic dejun"), observed.tags.map { it.name })
                cancelAndIgnoreRemainingEvents()
            }

            // MealPlanEntry must still be intact after updating the recipe
            assertEquals(1, recipeRepo.countMealPlanEntriesForRecipe(initial.id))
            // Unused tags ('desert', 'rapid') should have been cleaned up
            val allTags = recipeRepo.observeAllTags().first()
            assertEquals(listOf("mic dejun"), allTags.map { it.name })
        }
    }

    @Test
    fun deletingRecipeCascadesToIngredientsStepsTagsAndMealPlanEntries() = runTest {
        val (driver, db) = createInMemoryDatabase()
        driver.use {
            val recipeRepo = SqlDelightRecipeRepository(db, Dispatchers.Unconfined)
            val planRepo = SqlDelightMealPlanRepository(db, Dispatchers.Unconfined)

            val recipe = sampleRecipe(id = "rec-cascade")
            recipeRepo.saveRecipe(recipe)
            planRepo.upsertEntry(MealPlanEntry("p1", LocalDate(2026, 5, 4), MealSlot.LUNCH, recipe.id, 2))
            planRepo.upsertEntry(MealPlanEntry("p2", LocalDate(2026, 5, 5), MealSlot.DINNER, recipe.id, 4))

            assertEquals(2, recipeRepo.countMealPlanEntriesForRecipe(recipe.id))

            recipeRepo.deleteRecipe(recipe.id)

            assertNull(recipeRepo.getRecipeById(recipe.id))
            assertEquals(0, db.retetarQueries.selectIngredientsByRecipeId(recipe.id).executeAsList().size)
            assertEquals(0, db.retetarQueries.selectStepsByRecipeId(recipe.id).executeAsList().size)
            assertEquals(0, db.retetarQueries.selectTagsByRecipeId(recipe.id).executeAsList().size)
            assertEquals(0, recipeRepo.countMealPlanEntriesForRecipe(recipe.id))
            assertTrue(planRepo.getEntriesForRange(LocalDate(2026, 5, 1), LocalDate(2026, 5, 31)).isEmpty())
        }
    }

    @Test
    fun diacriticInsensitiveSearchAndFiltersWorkOnDemoRecipes() = runTest {
        val (driver, db) = createInMemoryDatabase()
        driver.use {
            val recipeRepo = SqlDelightRecipeRepository(db, Dispatchers.Unconfined)
            recipeRepo.saveRecipes(DemoRecipes.create())

            // Title search without diacritics and with legacy cedilla
            val ciorba = recipeRepo.observeRecipes(RecipeFilter(query = "ciorba")).first()
            assertEquals(listOf("Ciorbă de perișoare"), ciorba.map { it.title })

            val cedilla = recipeRepo.observeRecipes(RecipeFilter(query = "perişoare")).first()
            assertEquals(listOf("Ciorbă de perișoare"), cedilla.map { it.title })

            // Ingredient search without diacritics ('FAINA' -> Făină)
            val withFlour = recipeRepo.observeRecipes(RecipeFilter(query = "FAINA")).first()
            assertEquals(
                setOf("Clătite", "Ciorbă de perișoare", "Tocăniță de pui"),
                withFlour.map { it.title }.toSet(),
            )

            // Favorites filter
            val favorites = recipeRepo.observeRecipes(RecipeFilter(favoritesOnly = true)).first()
            assertEquals(4, favorites.size)
            assertTrue(favorites.all { it.isFavorite })

            // Max total minutes filter (<= 15 min: Salată grecească 10, Omletă 15, Ovăz 15)
            val quick = recipeRepo.observeRecipes(RecipeFilter(maxTotalMinutes = 15)).first()
            assertEquals(
                setOf("Salată grecească", "Omletă cu legume", "Ovăz cu fructe"),
                quick.map { it.title }.toSet(),
            )

            // Tag + time combined filter
            val quickVeg = recipeRepo.observeRecipes(
                RecipeFilter(tagNames = setOf("vegetarian"), maxTotalMinutes = 15),
            ).first()
            assertEquals(
                setOf("Salată grecească", "Ovăz cu fructe"),
                quickVeg.map { it.title }.toSet(),
            )

            // Toggle favorite
            val saladId = quickVeg.first { it.title == "Salată grecească" }.id
            recipeRepo.setFavorite(saladId, isFavorite = true, updatedAt = 9_999L)
            assertEquals(5, recipeRepo.observeRecipes(RecipeFilter(favoritesOnly = true)).first().size)
        }
    }

    @Test
    fun mealPlanRepositoryQueriesByRangeAndReplacesEntries() = runTest {
        val (driver, db) = createInMemoryDatabase()
        driver.use {
            val recipeRepo = SqlDelightRecipeRepository(db, Dispatchers.Unconfined)
            val planRepo = SqlDelightMealPlanRepository(db, Dispatchers.Unconfined)
            val r1 = sampleRecipe("r1", "Paste")
            val r2 = sampleRecipe("r2", "Salată")
            recipeRepo.saveRecipes(listOf(r1, r2))

            val week1Mon = LocalDate(2026, 5, 4)
            val week1Sun = LocalDate(2026, 5, 10)
            val week2Mon = LocalDate(2026, 5, 11)

            planRepo.upsertEntry(MealPlanEntry("e1", week1Mon, MealSlot.LUNCH, r1.id, 2))
            planRepo.upsertEntry(MealPlanEntry("e2", week1Sun, MealSlot.DINNER, r2.id, 3))
            planRepo.upsertEntry(MealPlanEntry("e3", week2Mon, MealSlot.BREAKFAST, r1.id, 1))

            planRepo.observeEntriesForRange(week1Mon, week1Sun).test {
                val items = awaitItem()
                assertEquals(listOf("e1", "e2"), items.map { it.id })
                cancelAndIgnoreRemainingEvents()
            }

            planRepo.updateServings("e1", 5)
            assertEquals(5, planRepo.getEntriesForDate(week1Mon).single().servings)

            planRepo.replaceEntriesForDate(
                date = week1Mon,
                entries = listOf(MealPlanEntry("e4", week1Mon, MealSlot.SNACK, r2.id, 2)),
            )
            assertEquals(listOf("e4"), planRepo.getEntriesForDate(week1Mon).map { it.id })

            planRepo.deleteEntry("e4")
            assertTrue(planRepo.getEntriesForDate(week1Mon).isEmpty())
        }
    }

    @Test
    fun shoppingRepositorySupportsWeekQueriesToggleClearAndReplace() = runTest {
        val (driver, db) = createInMemoryDatabase()
        driver.use {
            val shoppingRepo = SqlDelightShoppingRepository(db, Dispatchers.Unconfined)
            val week1 = LocalDate(2026, 5, 4)
            val week2 = LocalDate(2026, 5, 11)

            val flour = ShoppingItem(
                id = "s1",
                weekStart = week1,
                itemKey = UnitConverter.shoppingItemKey("faina", MeasurementUnit.G),
                name = "Făină",
                normalizedName = "faina",
                quantity = 500.0,
                unit = MeasurementUnit.G,
                aisle = Aisle.PANTRY,
                source = ShoppingItemSource.AUTO,
                checked = false,
            )
            val bread = ShoppingItem(
                id = "s2",
                weekStart = week1,
                itemKey = UnitConverter.shoppingItemKey("paine", MeasurementUnit.PIECE),
                name = "Pâine",
                normalizedName = "paine",
                quantity = 1.0,
                unit = MeasurementUnit.PIECE,
                aisle = Aisle.BAKERY,
                source = ShoppingItemSource.MANUAL,
                checked = true,
            )
            val otherWeekItem = flour.copy(id = "s3", weekStart = week2)

            shoppingRepo.replaceItemsForWeek(week1, listOf(flour, bread))
            shoppingRepo.upsertItem(otherWeekItem)

            shoppingRepo.observeItemsForWeek(week1).test {
                assertEquals(2, awaitItem().size)
                cancelAndIgnoreRemainingEvents()
            }

            shoppingRepo.setChecked("s1", true)
            assertTrue(shoppingRepo.getItemsForWeek(week1).first { it.id == "s1" }.checked)

            shoppingRepo.setChecked("s1", false)
            shoppingRepo.clearCheckedForWeek(week1)
            assertEquals(listOf("s1"), shoppingRepo.getItemsForWeek(week1).map { it.id })
            assertEquals(listOf("s3"), shoppingRepo.getItemsForWeek(week2).map { it.id })
        }
    }

    @Test
    fun migrationFromVersion1ToVersion2PreservesDataAndCreatesSearchIndexes() {
        val properties = Properties().apply { put("foreign_keys", "true") }
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, properties).use { driver ->
            // Create v1 schema without v2 search indexes
            val v1Statements = listOf(
                """
                CREATE TABLE Recipe (
                  id TEXT NOT NULL PRIMARY KEY,
                  title TEXT NOT NULL,
                  normalizedTitle TEXT NOT NULL,
                  description TEXT NOT NULL DEFAULT '',
                  baseServings INTEGER NOT NULL,
                  prepMinutes INTEGER NOT NULL DEFAULT 0,
                  cookMinutes INTEGER NOT NULL DEFAULT 0,
                  notes TEXT NOT NULL DEFAULT '',
                  isFavorite INTEGER NOT NULL DEFAULT 0,
                  createdAt INTEGER NOT NULL,
                  updatedAt INTEGER NOT NULL
                )
                """.trimIndent(),
                """
                CREATE TABLE RecipeIngredient (
                  id TEXT NOT NULL PRIMARY KEY,
                  recipeId TEXT NOT NULL REFERENCES Recipe(id) ON DELETE CASCADE,
                  position INTEGER NOT NULL,
                  name TEXT NOT NULL,
                  normalizedName TEXT NOT NULL,
                  quantity REAL,
                  unit TEXT NOT NULL,
                  aisle TEXT NOT NULL DEFAULT 'OTHER'
                )
                """.trimIndent(),
                """
                CREATE TABLE RecipeStep (
                  id TEXT NOT NULL PRIMARY KEY,
                  recipeId TEXT NOT NULL REFERENCES Recipe(id) ON DELETE CASCADE,
                  position INTEGER NOT NULL,
                  text TEXT NOT NULL
                )
                """.trimIndent(),
                "CREATE TABLE Tag (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL UNIQUE)",
                """
                CREATE TABLE RecipeTag (
                  recipeId TEXT NOT NULL REFERENCES Recipe(id) ON DELETE CASCADE,
                  tagId TEXT NOT NULL REFERENCES Tag(id) ON DELETE CASCADE,
                  PRIMARY KEY (recipeId, tagId)
                )
                """.trimIndent(),
                """
                CREATE TABLE MealPlanEntry (
                  id TEXT NOT NULL PRIMARY KEY,
                  date TEXT NOT NULL,
                  slot TEXT NOT NULL,
                  recipeId TEXT NOT NULL REFERENCES Recipe(id) ON DELETE CASCADE,
                  servings INTEGER NOT NULL
                )
                """.trimIndent(),
                "CREATE INDEX idx_plan_date ON MealPlanEntry(date)",
                """
                CREATE TABLE ShoppingItem (
                  id TEXT NOT NULL PRIMARY KEY,
                  weekStart TEXT NOT NULL,
                  itemKey TEXT NOT NULL,
                  name TEXT NOT NULL,
                  normalizedName TEXT NOT NULL,
                  quantity REAL,
                  unit TEXT NOT NULL,
                  aisle TEXT NOT NULL DEFAULT 'OTHER',
                  source TEXT NOT NULL,
                  checked INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
                "CREATE INDEX idx_shopping_week ON ShoppingItem(weekStart)",
            )
            v1Statements.forEach { sql -> driver.execute(null, sql, 0) }
            driver.execute(
                null,
                """
                INSERT INTO Recipe(id, title, normalizedTitle, baseServings, createdAt, updatedAt)
                VALUES ('v1-rec', 'Supă', 'supa', 4, 100, 100)
                """.trimIndent(),
                0,
            )

            // Apply migration 1 -> 2
            RetetarDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = RetetarDatabase.Schema.version)

            val db = RetetarDatabase(driver)
            val migrated = db.retetarQueries.selectRecipeById("v1-rec").executeAsOneOrNull()
            assertNotNull(migrated)
            assertEquals("Supă", migrated.title)

            val indexNames = driver.executeQuery(
                identifier = null,
                sql = "SELECT name FROM sqlite_master WHERE type = 'index'",
                mapper = { cursor ->
                    val names = mutableSetOf<String>()
                    while (cursor.next().value) {
                        cursor.getString(0)?.let(names::add)
                    }
                    QueryResult.Value(names)
                },
                parameters = 0,
            ).value

            assertTrue("idx_recipe_normalized_title" in indexNames)
            assertTrue("idx_ingredient_normalized_name" in indexNames)
        }
    }

    @Test
    fun koinCoreModulesStartAndResolveAllDependenciesOnJvm() = runTest {
        val testOverrides = module {
            single { DriverFactory.inMemory() }
            single<Settings> { MapSettings() }
        }
        val app = koinApplication {
            allowOverride(true)
            modules(coreModules(listOf(testOverrides)))
        }
        try {
            val koin = app.koin
            val recipeRepo = koin.get<RecipeRepository>()
            val planRepo = koin.get<MealPlanRepository>()
            val shoppingRepo = koin.get<ShoppingRepository>()
            val settingsRepo = koin.get<SettingsRepository>()
            assertNotNull(koin.get<Clock>())
            assertNotNull(koin.get<IdGenerator>())

            val recipe = sampleRecipe("koin-1")
            recipeRepo.saveRecipe(recipe)
            assertEquals("Clătite cu brânză", recipeRepo.getRecipeById("koin-1")?.title)
            assertTrue(planRepo.getEntriesForRange(LocalDate(2026, 5, 4), LocalDate(2026, 5, 10)).isEmpty())
            assertTrue(shoppingRepo.getItemsForWeek(LocalDate(2026, 5, 4)).isEmpty())
            assertNotNull(settingsRepo.settings.value)
        } finally {
            app.close()
        }
    }
}
