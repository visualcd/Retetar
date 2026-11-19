package com.ro.aminvatatro.domain.usecase.shopping

import com.ro.aminvatatro.core.text.TextNormalizer
import com.ro.aminvatatro.core.time.IdGenerator
import com.ro.aminvatatro.core.time.WeekUtils
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.core.units.UnitCategory
import com.ro.aminvatatro.core.units.UnitConverter
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.ShoppingItem
import com.ro.aminvatatro.domain.model.ShoppingItemSource
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import kotlinx.datetime.LocalDate

/**
 * Generates (or regenerates) the weekly shopping list from scheduled meal plan entries
 * following the aggregation and merge rules in section 6 of the specification:
 *
 * 1. Loads all plan entries in `[weekStart, weekStart + 6]`.
 * 2. Scales every ingredient by `entry.servings / recipe.baseServings` and converts to base unit.
 * 3. Groups by canonical `itemKey` (`normalizedName|CATEGORY` for MASS/VOLUME/COUNT;
 *    `normalizedName|UNIT` for OTHER) and sums base quantities.
 * 4. Keeps the original unit when all sources share the exact same unit; otherwise expresses
 *    the total in the category's base unit, promoted to `KG` / `L` at `>= 1000`.
 * 5. Emits `TO_TASTE` ingredients once with a `null` quantity.
 * 6. Merges with existing week items: existing `AUTO` items keep their `id` and `checked` state,
 *    orphaned `AUTO` items are removed, and all `MANUAL` items remain untouched.
 */
class GenerateShoppingList(
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository,
    private val shoppingRepository: ShoppingRepository,
    private val idGenerator: IdGenerator,
) {
    /**
     * Generates and atomically persists the shopping list for the week containing [weekDate].
     *
     * @return The full persisted list of [ShoppingItem]s (both `AUTO` and preserved `MANUAL`).
     */
    suspend operator fun invoke(weekDate: LocalDate): List<ShoppingItem> {
        val weekStart = WeekUtils.weekStartOf(weekDate)
        val weekEnd = WeekUtils.weekEndOf(weekStart)

        val planEntries = mealPlanRepository.getEntriesForRange(weekStart, weekEnd)
        val recipesById = loadReferencedRecipes(planEntries.map { it.recipeId }.distinct())

        val groups = linkedMapOf<String, AggregatedGroup>()

        for (entry in planEntries) {
            val recipe = recipesById[entry.recipeId] ?: continue
            if (recipe.baseServings <= 0 || entry.servings <= 0) continue

            for (ingredient in recipe.ingredients) {
                val trimmedName = ingredient.name.trim()
                val normalizedName = TextNormalizer.normalize(trimmedName)
                if (normalizedName.isEmpty()) continue

                val itemKey = UnitConverter.shoppingItemKey(normalizedName, ingredient.unit)

                if (ingredient.unit == MeasurementUnit.TO_TASTE) {
                    val existingGroup = groups[itemKey]
                    if (existingGroup == null) {
                        groups[itemKey] = AggregatedGroup(
                            itemKey = itemKey,
                            displayName = trimmedName,
                            normalizedName = normalizedName,
                            category = ingredient.unit.category,
                            aisle = ingredient.aisle,
                            isToTaste = true,
                        )
                    } else if (existingGroup.aisle == Aisle.OTHER && ingredient.aisle != Aisle.OTHER) {
                        existingGroup.aisle = ingredient.aisle
                    }
                    continue
                }

                val scaled = UnitConverter.scaleQuantity(
                    quantity = ingredient.quantity,
                    unit = ingredient.unit,
                    baseServings = recipe.baseServings,
                    targetServings = entry.servings,
                ) ?: continue
                if (scaled <= 0.0) continue

                val baseQty = UnitConverter.toBaseUnit(scaled, ingredient.unit) ?: continue

                val group = groups.getOrPut(itemKey) {
                    AggregatedGroup(
                        itemKey = itemKey,
                        displayName = trimmedName,
                        normalizedName = normalizedName,
                        category = ingredient.unit.category,
                        aisle = ingredient.aisle,
                        isToTaste = false,
                    )
                }
                group.totalBaseQuantity += baseQty
                group.sourceUnits += ingredient.unit
                if (group.aisle == Aisle.OTHER && ingredient.aisle != Aisle.OTHER) {
                    group.aisle = ingredient.aisle
                }
            }
        }

        val existingItems = shoppingRepository.getItemsForWeek(weekStart)
        val existingAutoByKey = existingItems
            .filter { it.source == ShoppingItemSource.AUTO }
            .associateBy { it.itemKey }
        val existingManual = existingItems.filter { it.source == ShoppingItemSource.MANUAL }

        val generatedAutoItems = groups.values.map { group ->
            val (finalQuantity, finalUnit) = resolveQuantityAndUnit(group)
            val previous = existingAutoByKey[group.itemKey]
            ShoppingItem(
                id = previous?.id ?: idGenerator.nextId(),
                weekStart = weekStart,
                itemKey = group.itemKey,
                name = group.displayName,
                normalizedName = group.normalizedName,
                quantity = finalQuantity,
                unit = finalUnit,
                aisle = group.aisle,
                source = ShoppingItemSource.AUTO,
                checked = previous?.checked ?: false,
            )
        }

        val merged = (generatedAutoItems + existingManual)
            .sortedWith(
                compareBy<ShoppingItem>(
                    { it.aisle.ordinal },
                    { it.normalizedName },
                    { it.itemKey },
                    { it.source.ordinal },
                ),
            )

        shoppingRepository.replaceItemsForWeek(weekStart = weekStart, items = merged)
        return merged
    }

    private suspend fun loadReferencedRecipes(recipeIds: List<String>): Map<String, Recipe> {
        val result = mutableMapOf<String, Recipe>()
        for (recipeId in recipeIds) {
            recipeRepository.getRecipeById(recipeId)?.let { recipe ->
                result[recipeId] = recipe
            }
        }
        return result
    }

    private fun resolveQuantityAndUnit(group: AggregatedGroup): Pair<Double?, MeasurementUnit> {
        if (group.isToTaste) {
            return null to MeasurementUnit.TO_TASTE
        }

        if (group.sourceUnits.size == 1) {
            val singleUnit = group.sourceUnits.single()
            val factor = singleUnit.factorToBase
            val quantityInSingleUnit = if (factor != null) {
                group.totalBaseQuantity / factor
            } else {
                group.totalBaseQuantity
            }
            return quantityInSingleUnit to singleUnit
        }

        return UnitConverter.promoteMetricBaseUnit(
            baseQuantity = group.totalBaseQuantity,
            category = group.category,
        )
    }

    private class AggregatedGroup(
        val itemKey: String,
        val displayName: String,
        val normalizedName: String,
        val category: UnitCategory,
        var aisle: Aisle,
        val isToTaste: Boolean,
        var totalBaseQuantity: Double = 0.0,
        val sourceUnits: MutableSet<MeasurementUnit> = linkedSetOf(),
    )
}
