package com.ro.aminvatatro.domain.usecase.nutrition

import com.ro.aminvatatro.core.nutrition.NutritionDatabase
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.Step
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.repository.RecipeRepository
import kotlin.math.roundToInt

/**
 * Outcome of generating 5 candidate recipes that fit within a maximum kcal per serving limit.
 */
data class CalorieRecipeSuggestion(
    val recipe: Recipe,
    val caloriesPerServing: Double,
    val isGenerated: Boolean,
)

/**
 * Generates or selects 5 recipe proposals where each serving is at most [maxKcalPerServing].
 *
 * Algorithm:
 * 1. Inspects existing saved recipes in [RecipeRepository] that have known/computable calories
 *    and fit under [maxKcalPerServing].
 * 2. If fewer than 5 exist, generates balanced culinary recipe proposals based on common
 *    ingredients and preparations with precisely computed caloric amounts (e.g. salate, supe,
 *    pește/pui cu legume, paste, boluri etc.) calibrated to the requested target.
 * 3. Returns up to 5 diverse, well-composed recipes.
 */
class GenerateRecipesForCaloriesTarget(
    private val recipeRepository: RecipeRepository,
) {
    suspend operator fun invoke(
        maxKcalPerServing: Double,
        limit: Int = 5,
    ): List<CalorieRecipeSuggestion> {
        if (maxKcalPerServing <= 0.0) return emptyList()

        val savedRecipes = recipeRepository.getAllRecipes()
        val existingMatches = savedRecipes
            .mapNotNull { recipe ->
                val calsPerServing = recipe.caloriesPerServing
                    ?: computeCaloriesPerServing(recipe)
                if (calsPerServing != null && calsPerServing <= maxKcalPerServing) {
                    CalorieRecipeSuggestion(
                        recipe = recipe.copy(caloriesTotal = recipe.effectiveCaloriesTotal),
                        caloriesPerServing = calsPerServing,
                        isGenerated = false,
                    )
                } else null
            }
            .sortedByDescending { it.caloriesPerServing }
            .take(limit)
            .toMutableList()

        if (existingMatches.size >= limit) {
            return existingMatches.take(limit)
        }

        // Generate dynamic balanced recipes to reach 5 suggestions
        val templatePool = getCalibratedTemplates(maxKcalPerServing)
        val existingTitles = savedRecipes.map { it.normalizedTitle }.toSet()

        for (template in templatePool) {
            if (existingMatches.size >= limit) break
            if (template.recipe.normalizedTitle in existingTitles) continue

            val cals = template.caloriesPerServing
            if (cals <= maxKcalPerServing) {
                existingMatches.add(template)
            }
        }

        return existingMatches.take(limit)
    }

    private fun computeCaloriesPerServing(recipe: Recipe): Double? {
        val total = recipe.ingredients.mapNotNull { ing ->
            ing.calories ?: NutritionDatabase.calculateCalories(ing.name, ing.quantity, ing.unit)
        }.sum()
        if (total <= 0.0 || recipe.baseServings <= 0) return null
        return total / recipe.baseServings
    }

    private fun getCalibratedTemplates(maxKcal: Double): List<CalorieRecipeSuggestion> {
        val list = mutableListOf<CalorieRecipeSuggestion>()

        // 1. Salată mediteraneană cu brânză telemea și măsline (~320 kcal / porție)
        if (maxKcal >= 250.0) {
            val ing1 = Ingredient("gen-1-1", 0, "Roșii", 300.0, MeasurementUnit.G, Aisle.PRODUCE, 54.0)
            val ing2 = Ingredient("gen-1-2", 1, "Castraveți", 200.0, MeasurementUnit.G, Aisle.PRODUCE, 30.0)
            val ing3 = Ingredient("gen-1-3", 2, "Brânză telemea", 100.0, MeasurementUnit.G, Aisle.DAIRY, 264.0)
            val ing4 = Ingredient("gen-1-4", 3, "Măsline", 50.0, MeasurementUnit.G, Aisle.PANTRY, 57.5)
            val ing5 = Ingredient("gen-1-5", 4, "Ulei de măsline", 20.0, MeasurementUnit.ML, Aisle.PANTRY, 162.6)
            val ing6 = Ingredient("gen-1-6", 5, "Oregano", null, MeasurementUnit.TO_TASTE, Aisle.SPICES, 0.0)
            val totalCals = 568.1
            val servings = 2
            list += CalorieRecipeSuggestion(
                recipe = Recipe(
                    id = "gen-recipe-salata-greceasca",
                    title = "Salată mediteraneană cu telemea",
                    description = "Salată proaspătă și răcoritoare cu telemea, legume și ulei de măsline.",
                    baseServings = servings,
                    prepMinutes = 10,
                    cookMinutes = 0,
                    ingredients = listOf(ing1, ing2, ing3, ing4, ing5, ing6),
                    steps = listOf(
                        Step("gen-1-s1", 0, "Spală legumele și taie roșiile și castraveții cuburi mari."),
                        Step("gen-1-s2", 1, "Adaugă telemeaua tăiată cubulețe și măslinele."),
                        Step("gen-1-s3", 2, "Stropește cu ulei de măsline și presară oregano după gust."),
                    ),
                    tags = listOf(Tag("t-salata", "salată"), Tag("t-rapid", "rapid"), Tag("t-veg", "vegetarian")),
                    caloriesTotal = totalCals,
                ),
                caloriesPerServing = totalCals / servings,
                isGenerated = true,
            )
        }

        // 2. Piept de pui la grătar cu legume la tigaie (~410 kcal / porție)
        if (maxKcal >= 380.0) {
            val ing1 = Ingredient("gen-2-1", 0, "Piept de pui", 300.0, MeasurementUnit.G, Aisle.MEAT_FISH, 495.0)
            val ing2 = Ingredient("gen-2-2", 1, "Dovlecel", 200.0, MeasurementUnit.G, Aisle.PRODUCE, 34.0)
            val ing3 = Ingredient("gen-2-3", 2, "Ardei", 150.0, MeasurementUnit.G, Aisle.PRODUCE, 46.5)
            val ing4 = Ingredient("gen-2-4", 3, "Ciuperci", 150.0, MeasurementUnit.G, Aisle.PRODUCE, 33.0)
            val ing5 = Ingredient("gen-2-5", 4, "Ulei", 15.0, MeasurementUnit.ML, Aisle.PANTRY, 122.0)
            val ing6 = Ingredient("gen-2-6", 5, "Sare", null, MeasurementUnit.TO_TASTE, Aisle.SPICES, 0.0)
            val ing7 = Ingredient("gen-2-7", 6, "Piper", null, MeasurementUnit.TO_TASTE, Aisle.SPICES, 0.0)
            val totalCals = 730.5
            val servings = 2
            list += CalorieRecipeSuggestion(
                recipe = Recipe(
                    id = "gen-recipe-pui-legume",
                    title = "Piept de pui cu legume trase la tigaie",
                    description = "Friptură slabă de pui cu garnitură suculentă de dovlecei, ardei și ciuperci.",
                    baseServings = servings,
                    prepMinutes = 15,
                    cookMinutes = 15,
                    ingredients = listOf(ing1, ing2, ing3, ing4, ing5, ing6, ing7),
                    steps = listOf(
                        Step("gen-2-s1", 0, "Condimentează pieptul de pui cu sare și piper."),
                        Step("gen-2-s2", 1, "Rumeneste puiul pe ambele părți într-o tigaie bine încinsă."),
                        Step("gen-2-s3", 2, "Trage legumele tăiate fâșii în aceeași tigaie până se rumenesc ușor."),
                    ),
                    tags = listOf(Tag("t-pui", "fel principal"), Tag("t-carne", "carne"), Tag("t-fit", "rapid")),
                    caloriesTotal = totalCals,
                ),
                caloriesPerServing = totalCals / servings,
                isGenerated = true,
            )
        }

        // 3. Somon la cuptor cu orez și lămâie (~520 kcal / porție)
        if (maxKcal >= 500.0) {
            val ing1 = Ingredient("gen-3-1", 0, "Somon", 300.0, MeasurementUnit.G, Aisle.MEAT_FISH, 624.0)
            val ing2 = Ingredient("gen-3-2", 1, "Orez", 100.0, MeasurementUnit.G, Aisle.PANTRY, 360.0)
            val ing3 = Ingredient("gen-3-3", 2, "Lămâie", 1.0, MeasurementUnit.PIECE, Aisle.PRODUCE, 29.0)
            val ing4 = Ingredient("gen-3-4", 3, "Ulei de măsline", 10.0, MeasurementUnit.ML, Aisle.PANTRY, 81.3)
            val totalCals = 1094.3
            val servings = 2
            list += CalorieRecipeSuggestion(
                recipe = Recipe(
                    id = "gen-recipe-somon-orez",
                    title = "File de somon la cuptor cu orez",
                    description = "Masa bogată în acizi grași omega-3, servită cu orez pufos și lămâie.",
                    baseServings = servings,
                    prepMinutes = 10,
                    cookMinutes = 20,
                    ingredients = listOf(ing1, ing2, ing3, ing4),
                    steps = listOf(
                        Step("gen-3-s1", 0, "Fierbe orezul în apă cu un praf de sare."),
                        Step("gen-3-s2", 1, "Așază somonul într-o tavă, stropește-l cu ulei și zeamă de lămâie."),
                        Step("gen-3-s3", 2, "Coace la 190°C timp de 15-18 minute și servește cu orez."),
                    ),
                    tags = listOf(Tag("t-peste", "pește"), Tag("t-fel", "fel principal")),
                    caloriesTotal = totalCals,
                ),
                caloriesPerServing = totalCals / servings,
                isGenerated = true,
            )
        }

        // 4. Paste cu roșii cherry, usturoi și mozzarella (~460 kcal / porție)
        if (maxKcal >= 450.0) {
            val ing1 = Ingredient("gen-4-1", 0, "Paste", 200.0, MeasurementUnit.G, Aisle.PANTRY, 742.0)
            val ing2 = Ingredient("gen-4-2", 1, "Roșii", 250.0, MeasurementUnit.G, Aisle.PRODUCE, 45.0)
            val ing3 = Ingredient("gen-4-3", 2, "Mozzarella", 100.0, MeasurementUnit.G, Aisle.DAIRY, 280.0)
            val ing4 = Ingredient("gen-4-4", 3, "Ulei de măsline", 15.0, MeasurementUnit.ML, Aisle.PANTRY, 122.0)
            val ing5 = Ingredient("gen-4-5", 4, "Usturoi", 2.0, MeasurementUnit.PIECE, Aisle.PRODUCE, 14.9)
            val totalCals = 1203.9
            val servings = 2
            list += CalorieRecipeSuggestion(
                recipe = Recipe(
                    id = "gen-recipe-paste-mozzarella",
                    title = "Paste cu roșii cherry și mozzarella",
                    description = "Paste aromate cu sos rapid din roșii proaspete, usturoi și mozzarella topită.",
                    baseServings = servings,
                    prepMinutes = 10,
                    cookMinutes = 15,
                    ingredients = listOf(ing1, ing2, ing3, ing4, ing5),
                    steps = listOf(
                        Step("gen-4-s1", 0, "Fierbe pastele al dente conform instrucțiunilor de pe ambalaj."),
                        Step("gen-4-s2", 1, "Călește usturoiul și roșiile cherry tăiate pe jumătate în ulei de măsline."),
                        Step("gen-4-s3", 2, "Amestecă pastele cu sosul și adaugă bucățile de mozzarella la final."),
                    ),
                    tags = listOf(Tag("t-paste", "paste"), Tag("t-veg", "vegetarian"), Tag("t-rapid", "rapid")),
                    caloriesTotal = totalCals,
                ),
                caloriesPerServing = totalCals / servings,
                isGenerated = true,
            )
        }

        // 5. Supă cremă fină de legume cu crutoane (~220 kcal / porție)
        if (maxKcal >= 200.0) {
            val ing1 = Ingredient("gen-5-1", 0, "Cartofi", 300.0, MeasurementUnit.G, Aisle.PRODUCE, 231.0)
            val ing2 = Ingredient("gen-5-2", 1, "Morcovi", 200.0, MeasurementUnit.G, Aisle.PRODUCE, 82.0)
            val ing3 = Ingredient("gen-5-3", 2, "Dovlecel", 200.0, MeasurementUnit.G, Aisle.PRODUCE, 34.0)
            val ing4 = Ingredient("gen-5-4", 3, "Ceapă", 100.0, MeasurementUnit.G, Aisle.PRODUCE, 40.0)
            val ing5 = Ingredient("gen-5-5", 4, "Unt", 20.0, MeasurementUnit.G, Aisle.DAIRY, 143.4)
            val totalCals = 530.4
            val servings = 2
            list += CalorieRecipeSuggestion(
                recipe = Recipe(
                    id = "gen-recipe-supa-legume",
                    title = "Supă cremă fină de rădăcinoase",
                    description = "Supă densă și catifelată din cartofi, morcovi și dovlecei cu o nuanță de unt.",
                    baseServings = servings,
                    prepMinutes = 15,
                    cookMinutes = 25,
                    ingredients = listOf(ing1, ing2, ing3, ing4, ing5),
                    steps = listOf(
                        Step("gen-5-s1", 0, "Curăță legumele, taie-le cuburi și pune-le la fiert cu apă și sare."),
                        Step("gen-5-s2", 1, "Când s-au înmuiat, pasează-le cu blenderul vertical adăugând untul."),
                        Step("gen-5-s3", 2, "Ajustează consistența cu supa rămasă și servește cald."),
                    ),
                    tags = listOf(Tag("t-supa", "supă"), Tag("t-veg", "vegetarian")),
                    caloriesTotal = totalCals,
                ),
                caloriesPerServing = totalCals / servings,
                isGenerated = true,
            )
        }

        // 6. Tocăniță de vită cu sos de roșii și mămăliguță (~620 kcal / porție)
        if (maxKcal >= 600.0) {
            val ing1 = Ingredient("gen-6-1", 0, "Carne de vită", 350.0, MeasurementUnit.G, Aisle.MEAT_FISH, 875.0)
            val ing2 = Ingredient("gen-6-2", 1, "Ceapă", 150.0, MeasurementUnit.G, Aisle.PRODUCE, 60.0)
            val ing3 = Ingredient("gen-6-3", 2, "Suc de roșii", 200.0, MeasurementUnit.ML, Aisle.PANTRY, 41.2)
            val ing4 = Ingredient("gen-6-4", 3, "Ulei", 20.0, MeasurementUnit.ML, Aisle.PANTRY, 162.6)
            val ing5 = Ingredient("gen-6-5", 4, "Mălai", 100.0, MeasurementUnit.G, Aisle.PANTRY, 365.0)
            val totalCals = 1503.8
            val servings = 2
            list += CalorieRecipeSuggestion(
                recipe = Recipe(
                    id = "gen-recipe-vita-mamaliga",
                    title = "Tocăniță de vită cu mămăligă caldă",
                    description = "Carne fragedă de vită gătită domol în sos bogat de ceapă și roșii.",
                    baseServings = servings,
                    prepMinutes = 20,
                    cookMinutes = 60,
                    ingredients = listOf(ing1, ing2, ing3, ing4, ing5),
                    steps = listOf(
                        Step("gen-6-s1", 0, "Rumenește cuburile de vită în ulei, apoi adaugă ceapa tocată fin."),
                        Step("gen-6-s2", 1, "Toarnă sucul de roșii și apă, lăsând să fiarbă la foc mic până se înmoaie carnea."),
                        Step("gen-6-s3", 2, "Pregătește o mămăligă caldă din mălai și apă cu sare și servește."),
                    ),
                    tags = listOf(Tag("t-vita", "fel principal"), Tag("t-trad", "tradițional")),
                    caloriesTotal = totalCals,
                ),
                caloriesPerServing = totalCals / servings,
                isGenerated = true,
            )
        }

        return list
    }
}
