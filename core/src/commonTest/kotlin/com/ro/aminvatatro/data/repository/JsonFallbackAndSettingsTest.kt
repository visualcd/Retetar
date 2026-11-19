package com.ro.aminvatatro.data.repository

import app.cash.turbine.test
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.core.units.UnitConverter
import com.ro.aminvatatro.data.demo.DemoRecipes
import com.ro.aminvatatro.data.local.SettingsJsonKeyValueStore
import com.ro.aminvatatro.data.repository.json.JsonDatabaseStorage
import com.ro.aminvatatro.data.repository.json.JsonMealPlanRepository
import com.ro.aminvatatro.data.repository.json.JsonRecipeRepository
import com.ro.aminvatatro.data.repository.json.JsonShoppingRepository
import com.ro.aminvatatro.data.settings.SettingsRepositoryImpl
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.domain.model.AppTheme
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.model.ShoppingItem
import com.ro.aminvatatro.domain.model.ShoppingItemSource
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.repository.RecipeFilter
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonFallbackAndSettingsTest {

    @Test
    fun settingsRepositoryPersistsAndEmitsUpdates() = runTest {
        val backing = MapSettings()
        val repo = SettingsRepositoryImpl(backing)

        repo.settings.test {
            val initial = awaitItem()
            assertEquals(AppTheme.SYSTEM, initial.theme)
            assertEquals(AppLanguage.RO, initial.language)
            assertEquals(UnitSystem.METRIC, initial.unitSystem)
            assertEquals(false, initial.demoDataLoaded)

            repo.setTheme(AppTheme.DARK)
            assertEquals(AppTheme.DARK, awaitItem().theme)

            repo.setLanguage(AppLanguage.EN)
            assertEquals(AppLanguage.EN, awaitItem().language)

            repo.setUnitSystem(UnitSystem.IMPERIAL)
            assertEquals(UnitSystem.IMPERIAL, awaitItem().unitSystem)

            repo.setDemoDataLoaded(true)
            assertEquals(true, awaitItem().demoDataLoaded)

            cancelAndIgnoreRemainingEvents()
        }

        // Fresh repository instance reads persisted values
        val reloaded = SettingsRepositoryImpl(backing).settings.value
        assertEquals(AppTheme.DARK, reloaded.theme)
        assertEquals(AppLanguage.EN, reloaded.language)
        assertEquals(UnitSystem.IMPERIAL, reloaded.unitSystem)
        assertEquals(true, reloaded.demoDataLoaded)
    }

    @Test
    fun jsonFallbackRepositoriesSupportCrudSearchCascadeAndReloadPersistence() = runTest {
        val backing = MapSettings()
        val store = SettingsJsonKeyValueStore(backing)
        val storage = JsonDatabaseStorage(store)
        val recipeRepo = JsonRecipeRepository(storage)
        val planRepo = JsonMealPlanRepository(storage)
        val shoppingRepo = JsonShoppingRepository(storage)

        val demos = DemoRecipes.create()
        recipeRepo.saveRecipes(demos)

        // Diacritic-insensitive search + filter
        val found = recipeRepo.observeRecipes(RecipeFilter(query = "mamaliga")).first()
        assertEquals(listOf("Mămăligă cu brânză"), found.map { it.title })

        val targetRecipe = demos.first()
        val weekStart = LocalDate(2026, 5, 4)
        planRepo.upsertEntry(MealPlanEntry("mp-1", weekStart, MealSlot.DINNER, targetRecipe.id, 4))
        assertEquals(1, recipeRepo.countMealPlanEntriesForRecipe(targetRecipe.id))

        // Foreign key parity check
        assertFailsWith<IllegalStateException> {
            planRepo.upsertEntry(MealPlanEntry("mp-bad", weekStart, MealSlot.LUNCH, "missing-id", 2))
        }

        // Shopping items persistence + clear checked
        val item = ShoppingItem(
            id = "sh-1",
            weekStart = weekStart,
            itemKey = UnitConverter.shoppingItemKey("faina", MeasurementUnit.G),
            name = "Făină",
            normalizedName = "faina",
            quantity = 250.0,
            unit = MeasurementUnit.G,
            aisle = Aisle.PANTRY,
            source = ShoppingItemSource.AUTO,
            checked = false,
        )
        shoppingRepo.upsertItem(item)
        shoppingRepo.setChecked("sh-1", true)

        // Reload from backing store to verify JSON serialization round-trip
        val reloadedStorage = JsonDatabaseStorage(SettingsJsonKeyValueStore(backing))
        val reloadedRecipeRepo = JsonRecipeRepository(reloadedStorage)
        val reloadedPlanRepo = JsonMealPlanRepository(reloadedStorage)
        val reloadedShoppingRepo = JsonShoppingRepository(reloadedStorage)

        assertEquals(demos.size, reloadedRecipeRepo.getAllRecipes().size)
        assertEquals(1, reloadedPlanRepo.getEntriesForDate(weekStart).size)
        assertTrue(reloadedShoppingRepo.getItemsForWeek(weekStart).single().checked)

        // Cascade delete on Web fallback
        reloadedRecipeRepo.deleteRecipe(targetRecipe.id)
        assertNull(reloadedRecipeRepo.getRecipeById(targetRecipe.id))
        assertTrue(reloadedPlanRepo.getEntriesForDate(weekStart).isEmpty())

        reloadedShoppingRepo.clearCheckedForWeek(weekStart)
        assertTrue(reloadedShoppingRepo.getItemsForWeek(weekStart).isEmpty())
    }
}
