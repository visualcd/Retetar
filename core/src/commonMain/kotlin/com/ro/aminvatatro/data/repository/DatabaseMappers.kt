package com.ro.aminvatatro.data.repository

import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.data.local.db.MealPlanEntry as DbMealPlanEntry
import com.ro.aminvatatro.data.local.db.Recipe as DbRecipe
import com.ro.aminvatatro.data.local.db.RecipeIngredient as DbRecipeIngredient
import com.ro.aminvatatro.data.local.db.RecipeStep as DbRecipeStep
import com.ro.aminvatatro.data.local.db.ShoppingItem as DbShoppingItem
import com.ro.aminvatatro.data.local.db.Tag as DbTag
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.ShoppingItem
import com.ro.aminvatatro.domain.model.ShoppingItemSource
import com.ro.aminvatatro.domain.model.Step
import com.ro.aminvatatro.domain.model.Tag
import kotlinx.datetime.LocalDate

internal fun DbRecipe.toDomain(
    ingredients: List<Ingredient>,
    steps: List<Step>,
    tags: List<Tag>,
): Recipe = Recipe(
    id = id,
    title = title,
    description = description,
    baseServings = baseServings.toInt(),
    prepMinutes = prepMinutes.toInt(),
    cookMinutes = cookMinutes.toInt(),
    ingredients = ingredients,
    steps = steps,
    tags = tags,
    notes = notes,
    isFavorite = isFavorite != 0L,
    createdAt = createdAt,
    updatedAt = updatedAt,
    caloriesTotal = caloriesTotal,
)

internal fun DbRecipeIngredient.toDomain(): Ingredient = Ingredient(
    id = id,
    position = position.toInt(),
    name = name,
    quantity = quantity,
    unit = parseMeasurementUnit(unit),
    aisle = parseAisle(aisle),
    calories = calories,
)

internal fun DbRecipeStep.toDomain(): Step = Step(
    id = id,
    position = position.toInt(),
    text = text,
)

internal fun DbTag.toDomain(): Tag = Tag(
    id = id,
    name = name,
)

internal fun DbMealPlanEntry.toDomain(): MealPlanEntry = MealPlanEntry(
    id = id,
    date = LocalDate.parse(date),
    slot = parseMealSlot(slot),
    recipeId = recipeId,
    servings = servings.toInt(),
)

internal fun DbShoppingItem.toDomain(): ShoppingItem = ShoppingItem(
    id = id,
    weekStart = LocalDate.parse(weekStart),
    itemKey = itemKey,
    name = name,
    normalizedName = normalizedName,
    quantity = quantity,
    unit = parseMeasurementUnit(unit),
    aisle = parseAisle(aisle),
    source = parseShoppingItemSource(source),
    checked = checked != 0L,
)

internal fun parseMeasurementUnit(raw: String): MeasurementUnit =
    MeasurementUnit.entries.firstOrNull { it.name == raw } ?: MeasurementUnit.PIECE

internal fun parseAisle(raw: String): Aisle =
    Aisle.entries.firstOrNull { it.name == raw } ?: Aisle.OTHER

internal fun parseMealSlot(raw: String): MealSlot =
    MealSlot.entries.firstOrNull { it.name == raw } ?: MealSlot.DINNER

internal fun parseShoppingItemSource(raw: String): ShoppingItemSource =
    ShoppingItemSource.entries.firstOrNull { it.name == raw } ?: ShoppingItemSource.AUTO
