package com.ro.aminvatatro.di

import com.ro.aminvatatro.data.demo.DemoRecipes
import com.ro.aminvatatro.domain.model.AppTheme
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.repository.SettingsRepository
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WebKoinAndPersistenceTest {

    @Test
    fun koinCoreModulesStartAndPersistInBrowserLocalStorage() = runTest {
        val app = koinApplication {
            modules(coreModules())
        }
        val koin = app.koin
        val recipeRepo = koin.get<RecipeRepository>()
        val planRepo = koin.get<MealPlanRepository>()
        val shoppingRepo = koin.get<ShoppingRepository>()
        val settingsRepo = koin.get<SettingsRepository>()

        try {
            val recipe = DemoRecipes.create().first()
            recipeRepo.saveRecipe(recipe)
            assertNotNull(recipeRepo.getRecipeById(recipe.id))

            val day = LocalDate(2026, 6, 1)
            planRepo.upsertEntry(MealPlanEntry("web-mp-1", day, MealSlot.LUNCH, recipe.id, 2))
            assertEquals(1, recipeRepo.countMealPlanEntriesForRecipe(recipe.id))

            // Cascade delete removes dependent meal plan entry
            recipeRepo.deleteRecipe(recipe.id)
            assertTrue(planRepo.getEntriesForDate(day).isEmpty())

            settingsRepo.setTheme(AppTheme.LIGHT)
            assertEquals(AppTheme.LIGHT, settingsRepo.settings.value.theme)
            assertTrue(shoppingRepo.getItemsForWeek(day).isEmpty())
        } finally {
            recipeRepo.deleteAllRecipes()
            planRepo.deleteAllEntries()
            shoppingRepo.deleteAllItems()
            settingsRepo.setTheme(AppTheme.SYSTEM)
            app.close()
        }
    }
}
