package com.ro.aminvatatro.domain.usecase

import com.ro.aminvatatro.di.coreModule
import com.ro.aminvatatro.di.useCaseModule
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import com.ro.aminvatatro.domain.usecase.importexport.ExportRecipes
import com.ro.aminvatatro.domain.usecase.importexport.ImportRecipes
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
import com.ro.aminvatatro.domain.usecase.shopping.ClearChecked
import com.ro.aminvatatro.domain.usecase.shopping.GenerateShoppingList
import com.ro.aminvatatro.domain.usecase.shopping.RemoveItem
import com.ro.aminvatatro.domain.usecase.shopping.ToggleItem
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull

class UseCaseKoinModuleTest {

    @Test
    fun useCaseModuleResolvesAllUseCasesWithoutDatabaseDriver() {
        val fakeRepos = module {
            single<MealPlanRepository> { FakeMealPlanRepository() }
            single<RecipeRepository> { FakeRecipeRepository(get<MealPlanRepository>() as FakeMealPlanRepository) }
            single<ShoppingRepository> { FakeShoppingRepository() }
        }
        val app = koinApplication {
            modules(coreModule, fakeRepos, useCaseModule)
        }
        val koin = app.koin
        try {
            assertNotNull(koin.get<GetRecipes>())
            assertNotNull(koin.get<GetRecipe>())
            assertNotNull(koin.get<SaveRecipe>())
            assertNotNull(koin.get<DeleteRecipe>())
            assertNotNull(koin.get<ToggleFavorite>())
            assertNotNull(koin.get<ScaleRecipe>())

            assertNotNull(koin.get<SetPlanEntry>())
            assertNotNull(koin.get<RemovePlanEntry>())
            assertNotNull(koin.get<ChangeServings>())
            assertNotNull(koin.get<CopyDay>())
            assertNotNull(koin.get<RepeatPreviousWeek>())

            assertNotNull(koin.get<GenerateShoppingList>())
            assertNotNull(koin.get<ToggleItem>())
            assertNotNull(koin.get<AddManualItem>())
            assertNotNull(koin.get<RemoveItem>())
            assertNotNull(koin.get<ClearChecked>())

            assertNotNull(koin.get<ExportRecipes>())
            assertNotNull(koin.get<ImportRecipes>())
        } finally {
            app.close()
        }
    }
}
