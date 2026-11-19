package com.ro.aminvatatro.presentation

import androidx.compose.ui.unit.dp
import app.cash.turbine.test
import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.domain.model.AppTheme
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.usecase.importexport.DuplicatePolicy
import com.ro.aminvatatro.domain.usecase.importexport.ExportRecipes
import com.ro.aminvatatro.domain.usecase.importexport.ImportRecipes
import com.ro.aminvatatro.presentation.settings.SettingsError
import com.ro.aminvatatro.presentation.settings.SettingsUiEvent
import com.ro.aminvatatro.presentation.settings.SettingsViewModel
import com.ro.aminvatatro.ui.navigation.WindowWidthClass
import com.ro.aminvatatro.ui.navigation.computeWindowWidthClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun preferencesDemoDataExportAndImportRoundTripWork() = runTest(testDispatcher) {
        val settingsRepo = FakeSettingsRepository()
        val recipeRepo = FakeRecipeRepository()
        val clock = FakeClock()
        val idGen = SequenceIdGenerator("imp")

        val vm = SettingsViewModel(
            settingsRepository = settingsRepo,
            recipeRepository = recipeRepo,
            exportRecipes = ExportRecipes(recipeRepo, clock),
            importRecipes = ImportRecipes(recipeRepo, clock, idGen),
            clock = clock,
        )
        advanceUntilIdle()

        // Preferences
        vm.setTheme(AppTheme.DARK)
        vm.setLanguage(AppLanguage.EN)
        vm.setUnitSystem(UnitSystem.IMPERIAL)
        advanceUntilIdle()
        assertEquals(AppTheme.DARK, vm.uiState.value.settings.theme)
        assertEquals(AppLanguage.EN, vm.uiState.value.settings.language)
        assertEquals(UnitSystem.IMPERIAL, vm.uiState.value.settings.unitSystem)

        // Load demo recipes (10)
        vm.events.test {
            vm.loadDemoData()
            assertEquals(SettingsUiEvent.DemoLoaded(10), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(vm.uiState.value.settings.demoDataLoaded)
        assertEquals(10, vm.uiState.value.recipes.size)

        // Export selected (1 recipe)
        val firstDemoId = vm.uiState.value.recipes.first().id
        vm.toggleRecipeForExport(firstDemoId)
        vm.events.test {
            vm.exportSelected()
            assertEquals(SettingsUiEvent.ExportReady(1, 1), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val exportedJson = vm.uiState.value.exportedJson
        assertTrue(exportedJson.contains("\"schemaVersion\": 1") || exportedJson.contains("\"schemaVersion\":1"))

        // Clear demo data -> 0 recipes
        vm.events.test {
            vm.clearDemoData()
            assertEquals(SettingsUiEvent.DemoCleared, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(vm.uiState.value.settings.demoDataLoaded)
        assertEquals(0, vm.uiState.value.recipes.size)

        // Import exported JSON back
        vm.onImportJsonChanged(exportedJson)
        vm.setDuplicatePolicy(DuplicatePolicy.SKIP)
        vm.events.test {
            vm.importFromJson()
            assertEquals(SettingsUiEvent.ImportSucceeded(imported = 1, skipped = 0, duplicated = 0), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, vm.uiState.value.recipes.size)

        // Invalid JSON surfaces typed error without crash
        vm.onImportJsonChanged("{corrupt-json")
        vm.importFromJson()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.error is SettingsError.InvalidJson)

        // Unsupported schemaVersion surfaces typed error
        vm.onImportJsonChanged("""{"schemaVersion":99,"exportedAt":0,"recipes":[]}""")
        vm.importFromJson()
        advanceUntilIdle()
        assertEquals(
            SettingsError.UnsupportedSchemaVersion(found = 99, supported = 1),
            vm.uiState.value.error,
        )
    }

    @Test
    fun windowWidthClassBreakpointsMatchMaterial3() {
        assertEquals(WindowWidthClass.COMPACT, computeWindowWidthClass(360.dp))
        assertEquals(WindowWidthClass.COMPACT, computeWindowWidthClass(599.dp))
        assertEquals(WindowWidthClass.MEDIUM, computeWindowWidthClass(600.dp))
        assertEquals(WindowWidthClass.MEDIUM, computeWindowWidthClass(839.dp))
        assertEquals(WindowWidthClass.EXPANDED, computeWindowWidthClass(840.dp))
        assertEquals(WindowWidthClass.EXPANDED, computeWindowWidthClass(1280.dp))
    }
}
