package com.ro.aminvatatro.di

import app.cash.sqldelight.db.SqlDriver
import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.core.time.IdGenerator
import com.ro.aminvatatro.core.time.SystemClock
import com.ro.aminvatatro.core.time.UuidIdGenerator
import com.ro.aminvatatro.data.local.DriverFactory
import com.ro.aminvatatro.data.local.JsonKeyValueStore
import com.ro.aminvatatro.data.local.defaultLocalStoreJson
import com.ro.aminvatatro.data.local.db.RetetarDatabase
import com.ro.aminvatatro.data.remote.OpenFoodFactsClient
import com.ro.aminvatatro.data.repository.SqlDelightMealPlanRepository
import com.ro.aminvatatro.data.repository.SqlDelightRecipeRepository
import com.ro.aminvatatro.data.repository.SqlDelightShoppingRepository
import com.ro.aminvatatro.data.repository.json.JsonDatabaseStorage
import com.ro.aminvatatro.data.repository.json.JsonMealPlanRepository
import com.ro.aminvatatro.data.repository.json.JsonRecipeRepository
import com.ro.aminvatatro.data.repository.json.JsonShoppingRepository
import com.ro.aminvatatro.data.settings.SettingsRepositoryImpl
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.repository.SettingsRepository
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import com.ro.aminvatatro.domain.usecase.importexport.ExportRecipes
import com.ro.aminvatatro.domain.usecase.importexport.ImportRecipes
import com.ro.aminvatatro.domain.usecase.nutrition.CalculateIngredientCalories
import com.ro.aminvatatro.domain.usecase.nutrition.GenerateRecipesForCaloriesTarget
import com.ro.aminvatatro.domain.usecase.plan.ChangeServings
import com.ro.aminvatatro.domain.usecase.plan.CopyDay
import com.ro.aminvatatro.domain.usecase.plan.RemovePlanEntry
import com.ro.aminvatatro.domain.usecase.plan.RepeatPreviousWeek
import com.ro.aminvatatro.domain.usecase.plan.SetPlanEntry
import com.ro.aminvatatro.domain.usecase.recipe.DeleteRecipe
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipe
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipes
import com.ro.aminvatatro.domain.usecase.recipe.SaveRecipe
import com.ro.aminvatatro.domain.usecase.recipe.ScaleRecipe
import com.ro.aminvatatro.domain.usecase.recipe.ToggleFavorite
import com.ro.aminvatatro.domain.usecase.shopping.AddManualItem
import com.ro.aminvatatro.domain.usecase.shopping.AddRecipeToShoppingList
import com.ro.aminvatatro.domain.usecase.shopping.ClearChecked
import com.ro.aminvatatro.domain.usecase.shopping.GenerateShoppingList
import com.ro.aminvatatro.domain.usecase.shopping.RemoveItem
import com.ro.aminvatatro.domain.usecase.shopping.ToggleItem
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Common infrastructure Koin module shared across all targets.
 */
val coreModule: Module = module {
    single<Clock> { SystemClock() }
    single<IdGenerator> { UuidIdGenerator() }
    single<CoroutineDispatcher> { Dispatchers.Default }
    single<Json> { defaultLocalStoreJson }
    single<Settings> { Settings() }
    single<SettingsRepository> { SettingsRepositoryImpl(backingSettings = get()) }
    single { OpenFoodFactsClient() }
}

/**
 * SQLDelight repository bindings used on Android, iOS, and Desktop (JVM).
 *
 * Requires a [DriverFactory] definition in the active platform module.
 */
val sqlPersistenceModule: Module = module {
    single<SqlDriver> {
        requireNotNull(get<DriverFactory>().createDriver()) {
            "DriverFactory.createDriver() returned null on a SQLDelight target."
        }
    }
    single { RetetarDatabase(driver = get()) }
    single<RecipeRepository> { SqlDelightRecipeRepository(database = get(), dispatcher = get()) }
    single<MealPlanRepository> { SqlDelightMealPlanRepository(database = get(), dispatcher = get()) }
    single<ShoppingRepository> { SqlDelightShoppingRepository(database = get(), dispatcher = get()) }
}

/**
 * JSON `localStorage` repository bindings used on Web (`js` and `wasmJs`).
 *
 * Requires a [JsonKeyValueStore] definition in the active platform module.
 */
val jsonPersistenceModule: Module = module {
    single { JsonDatabaseStorage(keyValueStore = get()) }
    single<RecipeRepository> { JsonRecipeRepository(storage = get()) }
    single<MealPlanRepository> { JsonMealPlanRepository(storage = get()) }
    single<ShoppingRepository> { JsonShoppingRepository(storage = get()) }
}

/**
 * Domain use case Koin module shared across all targets.
 */
val useCaseModule: Module = module {
    factory { GetRecipes(recipeRepository = get()) }
    factory { GetRecipe(recipeRepository = get()) }
    factory { SaveRecipe(recipeRepository = get(), clock = get(), idGenerator = get()) }
    factory { DeleteRecipe(recipeRepository = get()) }
    factory { ToggleFavorite(recipeRepository = get(), clock = get()) }
    factory { ScaleRecipe() }

    factory { SetPlanEntry(mealPlanRepository = get(), recipeRepository = get(), idGenerator = get()) }
    factory { RemovePlanEntry(mealPlanRepository = get()) }
    factory { ChangeServings(mealPlanRepository = get()) }
    factory { CopyDay(mealPlanRepository = get(), idGenerator = get()) }
    factory { RepeatPreviousWeek(mealPlanRepository = get(), idGenerator = get()) }

    factory {
        GenerateShoppingList(
            mealPlanRepository = get(),
            recipeRepository = get(),
            shoppingRepository = get(),
            idGenerator = get(),
        )
    }
    factory { AddRecipeToShoppingList(shoppingRepository = get(), idGenerator = get(), clock = get()) }
    factory { ToggleItem(shoppingRepository = get()) }
    factory { AddManualItem(shoppingRepository = get(), idGenerator = get()) }
    factory { RemoveItem(shoppingRepository = get()) }
    factory { ClearChecked(shoppingRepository = get()) }

    factory { ExportRecipes(recipeRepository = get(), clock = get()) }
    factory { ImportRecipes(recipeRepository = get(), clock = get(), idGenerator = get()) }
    factory { CalculateIngredientCalories(openFoodFactsClient = get()) }
    factory { GenerateRecipesForCaloriesTarget(recipeRepository = get()) }
}

/**
 * Platform-specific persistence module binding either SQLDelight or the Web JSON fallback.
 */
expect fun platformPersistenceModule(): Module

/**
 * Full list of `:core` Koin modules ready to register on any target.
 */
fun coreModules(extraModules: List<Module> = emptyList()): List<Module> =
    listOf(coreModule, platformPersistenceModule(), useCaseModule) + extraModules
