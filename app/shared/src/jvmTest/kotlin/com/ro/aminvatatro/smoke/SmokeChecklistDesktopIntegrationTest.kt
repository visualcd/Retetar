package com.ro.aminvatatro.smoke

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import app.cash.sqldelight.db.SqlDriver
import com.ro.aminvatatro.RetetarApp
import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.core.time.WeekUtils
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.data.demo.DemoRecipes
import com.ro.aminvatatro.data.local.DriverFactory
import com.ro.aminvatatro.di.coreModules
import com.ro.aminvatatro.di.presentationModule
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.domain.model.AppTheme
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.ShoppingItemSource
import com.ro.aminvatatro.domain.model.Step
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.repository.AppSettings
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.repository.SettingsRepository
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import com.ro.aminvatatro.domain.usecase.importexport.DuplicatePolicy
import com.ro.aminvatatro.domain.usecase.importexport.ExportRecipes
import com.ro.aminvatatro.domain.usecase.importexport.ImportRecipes
import com.ro.aminvatatro.domain.usecase.importexport.ImportRecipesResult
import com.ro.aminvatatro.domain.usecase.plan.SetPlanEntry
import com.ro.aminvatatro.domain.usecase.plan.SetPlanEntryResult
import com.ro.aminvatatro.domain.usecase.recipe.DeleteRecipe
import com.ro.aminvatatro.domain.usecase.recipe.DeleteRecipeResult
import com.ro.aminvatatro.domain.usecase.recipe.SaveRecipe
import com.ro.aminvatatro.domain.usecase.recipe.SaveRecipeResult
import com.ro.aminvatatro.domain.usecase.recipe.ScaleRecipe
import com.ro.aminvatatro.domain.usecase.recipe.ScaleRecipeResult
import com.ro.aminvatatro.domain.usecase.shopping.AddManualItem
import com.ro.aminvatatro.domain.usecase.shopping.AddManualItemResult
import com.ro.aminvatatro.domain.usecase.shopping.GenerateShoppingList
import com.ro.aminvatatro.domain.usecase.shopping.ToggleItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.nio.file.Files
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end Desktop (JVM) verification covering all 10 items of PLAN.md Section 11
 * against a real file-backed SQLite database (`JdbcSqliteDriver`), real Koin DI modules,
 * simulated process restart (close driver + `stopKoin` + reopen same DB file), and
 * live `RetetarApp` window resizing (`360.dp` compact <-> `1000.dp` expanded).
 */
@OptIn(ExperimentalTestApi::class)
class SmokeChecklistDesktopIntegrationTest {

    private lateinit var tempDir: File
    private lateinit var dbFile: File
    private lateinit var prefsFile: File

    @BeforeTest
    fun setUp() {
        stopKoin()
        tempDir = Files.createTempDirectory("retetar-smoke-").toFile()
        dbFile = File(tempDir, "retetar.db")
        prefsFile = File(tempDir, "settings.properties")
    }

    @AfterTest
    fun tearDown() {
        runCatching {
            org.koin.mp.KoinPlatformTools.defaultContext().getOrNull()?.get<SqlDriver>()?.close()
        }
        stopKoin()
        tempDir.deleteRecursively()
    }

    private fun startFreshDesktopKoin() = startKoin {
        allowOverride(true)
        modules(
            coreModules(
                extraModules = listOf(
                    presentationModule,
                    module {
                        single { DriverFactory(databaseUrl = "jdbc:sqlite:${dbFile.absolutePath}") }
                        single<SettingsRepository> { FileBackedSettingsRepository(prefsFile) }
                    },
                ),
            ),
        )
    }.koin

    @Test
    fun fullSection11ChecklistPassesAcrossRestartWithRealSqlitePersistence() = runTest {
        // ---- SESSION 1 ----
        var koin = startFreshDesktopKoin()

        val recipeRepo = koin.get<RecipeRepository>()
        val mealPlanRepo = koin.get<MealPlanRepository>()
        val shoppingRepo = koin.get<ShoppingRepository>()
        val settingsRepo = koin.get<SettingsRepository>()
        val clock = koin.get<Clock>()
        val saveRecipe = koin.get<SaveRecipe>()
        val scaleRecipe = koin.get<ScaleRecipe>()
        val setPlanEntry = koin.get<SetPlanEntry>()
        val generateShoppingList = koin.get<GenerateShoppingList>()
        val toggleItem = koin.get<ToggleItem>()
        val addManualItem = koin.get<AddManualItem>()
        val exportRecipes = koin.get<ExportRecipes>()
        val deleteRecipe = koin.get<DeleteRecipe>()
        val importRecipes = koin.get<ImportRecipes>()

        // 1. Load demo data (as triggered from Settings)
        val demo = DemoRecipes.create(timestampMillis = clock.nowEpochMillis())
        demo.forEach { recipeRepo.saveRecipe(it) }
        settingsRepo.setDemoDataLoaded(true)
        assertEquals(10, recipeRepo.observeRecipes().first().size)

        // 2. Reject invalid recipe, then create valid recipe with 3 ingredients & 2 steps
        val nowMillis = clock.nowEpochMillis()
        val invalidResult = saveRecipe(
            Recipe(
                id = "",
                title = "   ",
                baseServings = 0,
                prepMinutes = -1,
                cookMinutes = 0,
                ingredients = emptyList(),
                steps = emptyList(),
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
        )
        assertIs<SaveRecipeResult.ValidationFailed>(invalidResult)
        assertTrue(invalidResult.errors.isNotEmpty())

        val createdResult = saveRecipe(
            Recipe(
                id = "",
                title = "Brioșe cu afine",
                description = "Pufoase pentru mic dejun",
                baseServings = 4,
                prepMinutes = 15,
                cookMinutes = 25,
                ingredients = listOf(
                    Ingredient("tmp-1", 0, "Făină", 300.0, MeasurementUnit.G, Aisle.PANTRY),
                    Ingredient("tmp-2", 1, "Ouă", 2.0, MeasurementUnit.PIECE, Aisle.DAIRY),
                    Ingredient("tmp-3", 2, "Afine", 150.0, MeasurementUnit.G, Aisle.PRODUCE),
                ),
                steps = listOf(
                    Step("tmp-s1", 0, "Amestecă ingredientele uscate cu ouăle."),
                    Step("tmp-s2", 1, "Adaugă afinele și coace 25 minute la 180°C."),
                ),
                tags = listOf(Tag("t-smoke", "desert")),
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
        )
        val createdRecipe = assertIs<SaveRecipeResult.Success>(createdResult).recipe
        assertEquals(3, createdRecipe.ingredients.size)
        assertEquals(2, createdRecipe.steps.size)

        // 3. Open recipe & scale servings (4 -> 6 servings = 1.5x)
        val reloaded = assertNotNull(recipeRepo.getRecipeById(createdRecipe.id))
        val scaled = assertIs<ScaleRecipeResult.Success>(scaleRecipe(reloaded, targetServings = 6))
        assertEquals(450.0, scaled.ingredients.first { it.ingredient.name == "Făină" }.quantity ?: 0.0, 0.001)
        assertEquals(3.0, scaled.ingredients.first { it.ingredient.name == "Ouă" }.quantity ?: 0.0, 0.001)
        assertEquals(225.0, scaled.ingredients.first { it.ingredient.name == "Afine" }.quantity ?: 0.0, 0.001)

        // 4. Plan two recipes on different days and slots (both share "Făină" and "Ouă")
        val clatite = assertNotNull(recipeRepo.observeRecipes().first().firstOrNull { it.title == "Clătite" })
        val weekStart = WeekUtils.weekStartOf(LocalDate(2026, 11, 19)) // Mon 2026-11-16
        assertIs<SetPlanEntryResult.Success>(setPlanEntry(weekStart, MealSlot.BREAKFAST, createdRecipe.id, servings = 4))
        assertIs<SetPlanEntryResult.Success>(
            setPlanEntry(weekStart.plus(DatePeriod(days = 2)), MealSlot.DINNER, clatite.id, servings = 4),
        )
        assertEquals(2, mealPlanRepo.getEntriesForRange(weekStart, WeekUtils.weekEndOf(weekStart)).size)

        // 5. Generate shopping list, verify aggregation, check an item, add manual item
        val generated = generateShoppingList(weekStart)
        val aggregatedFlour = assertNotNull(generated.firstOrNull { it.normalizedName == "faina" })
        // Brioșe (300 g) + Clătite (250 g) = 550 g aggregated into one row
        assertEquals(550.0, aggregatedFlour.quantity ?: 0.0, 0.001)
        assertEquals(MeasurementUnit.G, aggregatedFlour.unit)

        toggleItem(aggregatedFlour.id, checked = true)
        val manual = assertIs<AddManualItemResult.Success>(
            addManualItem(
                weekDate = weekStart,
                name = "Șervețele",
                quantity = 1.0,
                unit = MeasurementUnit.PIECE,
                aisle = Aisle.OTHER,
            ),
        ).item

        // 6. Regenerate shopping list -> checked state and manual item must survive
        val regenerated = generateShoppingList(weekStart)
        assertTrue(regenerated.first { it.normalizedName == "faina" }.checked)
        assertTrue(regenerated.any { it.id == manual.id && it.source == ShoppingItemSource.MANUAL })

        // 9. Switch language and theme (persisted for restart check)
        settingsRepo.setLanguage(AppLanguage.EN)
        settingsRepo.setTheme(AppTheme.DARK)
        assertEquals(AppLanguage.EN, settingsRepo.settings.value.language)
        assertEquals(AppTheme.DARK, settingsRepo.settings.value.theme)

        // 7. Close app (close SQLite driver & stop Koin) and reopen against the SAME files
        koin.get<SqlDriver>().close()
        stopKoin()

        // ---- SESSION 2 (COLD RESTART) ----
        koin = startFreshDesktopKoin()
        val recipeRepo2 = koin.get<RecipeRepository>()
        val mealPlanRepo2 = koin.get<MealPlanRepository>()
        val shoppingRepo2 = koin.get<ShoppingRepository>()
        val settingsRepo2 = koin.get<SettingsRepository>()
        val exportRecipes2 = koin.get<ExportRecipes>()
        val deleteRecipe2 = koin.get<DeleteRecipe>()
        val importRecipes2 = koin.get<ImportRecipes>()

        assertEquals(11, recipeRepo2.observeRecipes().first().size)
        assertNotNull(recipeRepo2.getRecipeById(createdRecipe.id))
        assertEquals(2, mealPlanRepo2.getEntriesForRange(weekStart, WeekUtils.weekEndOf(weekStart)).size)
        val persistedShopping = shoppingRepo2.getItemsForWeek(weekStart)
        assertTrue(persistedShopping.first { it.normalizedName == "faina" }.checked)
        assertTrue(persistedShopping.any { it.name == "Șervețele" && it.source == ShoppingItemSource.MANUAL })
        assertEquals(AppLanguage.EN, settingsRepo2.settings.value.language)
        assertEquals(AppTheme.DARK, settingsRepo2.settings.value.theme)

        // 8. Export JSON, delete one recipe, import JSON again -> deleted recipe restored
        val exported = exportRecipes2()
        assertEquals(11, exported.recipeCount)
        assertIs<DeleteRecipeResult.Deleted>(deleteRecipe2(createdRecipe.id))
        assertEquals(10, recipeRepo2.observeRecipes().first().size)

        val importOutcome = importRecipes2(exported.json, DuplicatePolicy.SKIP)
        val imported = assertIs<ImportRecipesResult.Success>(importOutcome)
        assertEquals(1, imported.importedCount)
        assertEquals(10, imported.skippedCount)
        assertEquals(11, recipeRepo2.observeRecipes().first().size)
    }

    @Test
    fun desktopAppLiveWindowResizeSwitchesNavigationBetweenCompactAndExpanded() = runComposeUiTest {
        startFreshDesktopKoin()
        var viewportWidth by mutableStateOf(360.dp)

        setContent {
            Box(modifier = Modifier.width(viewportWidth).height(760.dp)) {
                RetetarApp()
            }
        }

        // 10. Narrow Desktop window (360dp) -> bottom navigation bar
        onNodeWithTag("retetar_bottom_bar").assertIsDisplayed()

        // Resize live to wide Desktop window (1000dp) -> navigation rail
        viewportWidth = 1000.dp
        waitForIdle()
        onNodeWithTag("retetar_nav_rail").assertIsDisplayed()

        // Resize back to narrow (400dp) -> bottom navigation bar again
        viewportWidth = 400.dp
        waitForIdle()
        onNodeWithTag("retetar_bottom_bar").assertIsDisplayed()
    }
}

private class FileBackedSettingsRepository(
    private val file: File,
) : SettingsRepository {
    private val state = MutableStateFlow(load())
    override val settings: StateFlow<AppSettings> = state.asStateFlow()

    override suspend fun setTheme(theme: AppTheme) {
        update { it.copy(theme = theme) }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        update { it.copy(language = language) }
    }

    override suspend fun setUnitSystem(unitSystem: UnitSystem) {
        update { it.copy(unitSystem = unitSystem) }
    }

    override suspend fun setDemoDataLoaded(loaded: Boolean) {
        update { it.copy(demoDataLoaded = loaded) }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(state.value)
        state.value = next
        save(next)
    }

    private fun load(): AppSettings {
        if (!file.exists()) return AppSettings()
        val props = Properties().apply { file.inputStream().use(::load) }
        return AppSettings(
            theme = props.getProperty("theme")?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM,
            language = props.getProperty("language")?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.RO,
            unitSystem = props.getProperty("unitSystem")?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: UnitSystem.METRIC,
            demoDataLoaded = props.getProperty("demoDataLoaded")?.toBooleanStrictOrNull() ?: false,
        )
    }

    private fun save(settings: AppSettings) {
        val props = Properties().apply {
            setProperty("theme", settings.theme.name)
            setProperty("language", settings.language.name)
            setProperty("unitSystem", settings.unitSystem.name)
            setProperty("demoDataLoaded", settings.demoDataLoaded.toString())
        }
        file.outputStream().use { props.store(it, "Retetar smoke test settings") }
    }
}
