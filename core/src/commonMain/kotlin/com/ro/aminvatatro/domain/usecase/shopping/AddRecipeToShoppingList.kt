package com.ro.aminvatatro.domain.usecase.shopping

import com.ro.aminvatatro.core.text.TextNormalizer
import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.core.time.IdGenerator
import com.ro.aminvatatro.core.time.WeekUtils
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.core.units.UnitCategory
import com.ro.aminvatatro.core.units.UnitConverter
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.ShoppingItem
import com.ro.aminvatatro.domain.model.ShoppingItemSource
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import kotlinx.datetime.LocalDate

/**
 * Adds all ingredients from a [Recipe] directly into the shopping list for the week containing [targetDate].
 *
 * Scales ingredient quantities by `servings / recipe.baseServings` and aggregates them with any
 * existing items in the week:
 * - If an item with matching `itemKey` already exists, its quantity is increased (converting to base units
 *   and promoting/preserving units as appropriate) while keeping its `id`, `source`, and `checked` state.
 * - If no item with matching `itemKey` exists, a new [ShoppingItemSource.AUTO] item is appended with `checked = false`.
 * - To-taste ingredients appear once with `null` quantity.
 * - Existing manual or other unrelated items remain untouched.
 */
class AddRecipeToShoppingList(
    private val shoppingRepository: ShoppingRepository,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        recipe: Recipe,
        servings: Int = recipe.baseServings,
        targetDate: LocalDate = clock.today(),
    ): List<ShoppingItem> {
        if (recipe.baseServings <= 0 || servings <= 0 || recipe.ingredients.isEmpty()) {
            val weekStart = WeekUtils.weekStartOf(targetDate)
            return shoppingRepository.getItemsForWeek(weekStart)
        }

        val weekStart = WeekUtils.weekStartOf(targetDate)
        val groups = linkedMapOf<String, AggregatedGroup>()

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
                targetServings = servings,
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

        val existingItems = shoppingRepository.getItemsForWeek(weekStart).toMutableList()
        val existingIndexByKey = existingItems.mapIndexed { idx, item -> item.itemKey to idx }.toMap().toMutableMap()

        for (group in groups.values) {
            val existingIdx = existingIndexByKey[group.itemKey]
            if (existingIdx != null) {
                val existing = existingItems[existingIdx]
                if (group.isToTaste) {
                    if (existing.aisle == Aisle.OTHER && group.aisle != Aisle.OTHER) {
                        existingItems[existingIdx] = existing.copy(aisle = group.aisle)
                    }
                } else if (existing.unit == MeasurementUnit.TO_TASTE || existing.quantity == null) {
                    val (finalQty, finalUnit) = resolveQuantityAndUnit(group)
                    existingItems[existingIdx] = existing.copy(
                        quantity = finalQty,
                        unit = finalUnit,
                        aisle = if (existing.aisle == Aisle.OTHER) group.aisle else existing.aisle,
                    )
                } else {
                    val existingBase = UnitConverter.toBaseUnit(existing.quantity, existing.unit) ?: existing.quantity
                    val combinedBase = existingBase + group.totalBaseQuantity
                    val (finalQty, finalUnit) = if (existing.unit in group.sourceUnits && group.sourceUnits.size == 1) {
                        if (existing.unit == MeasurementUnit.G || existing.unit == MeasurementUnit.ML) {
                            UnitConverter.promoteMetricBaseUnit(combinedBase, group.category)
                        } else {
                            val factor = existing.unit.factorToBase
                            val qty = if (factor != null) combinedBase / factor else combinedBase
                            qty to existing.unit
                        }
                    } else {
                        UnitConverter.promoteMetricBaseUnit(combinedBase, group.category)
                    }
                    existingItems[existingIdx] = existing.copy(
                        quantity = finalQty,
                        unit = finalUnit,
                        aisle = if (existing.aisle == Aisle.OTHER) group.aisle else existing.aisle,
                    )
                }
            } else {
                val (finalQty, finalUnit) = resolveQuantityAndUnit(group)
                val newItem = ShoppingItem(
                    id = idGenerator.nextId(),
                    weekStart = weekStart,
                    itemKey = group.itemKey,
                    name = group.displayName,
                    normalizedName = group.normalizedName,
                    quantity = finalQty,
                    unit = finalUnit,
                    aisle = group.aisle,
                    source = ShoppingItemSource.AUTO,
                    checked = false,
                )
                existingItems.add(newItem)
                existingIndexByKey[group.itemKey] = existingItems.lastIndex
            }
        }

        val merged = existingItems.sortedWith(
            compareBy(
                { it.aisle.ordinal },
                { it.normalizedName },
                { it.itemKey },
                { it.source.ordinal },
            ),
        )

        shoppingRepository.replaceItemsForWeek(weekStart = weekStart, items = merged)
        return merged
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
