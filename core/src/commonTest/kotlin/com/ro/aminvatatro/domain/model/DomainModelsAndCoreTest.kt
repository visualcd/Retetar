package com.ro.aminvatatro.domain.model

import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.core.time.IdGenerator
import com.ro.aminvatatro.core.time.SystemClock
import com.ro.aminvatatro.core.time.UuidIdGenerator
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.core.units.UnitConverter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DomainModelsAndCoreTest {

    @Test
    fun recipeAndIngredientComputedPropertiesWork() {
        val ingredient = Ingredient(
            id = "i-1",
            position = 0,
            name = "  Făină   albă ",
            quantity = 250.0,
            unit = MeasurementUnit.G,
            aisle = Aisle.PANTRY,
        )
        val recipe = Recipe(
            id = "r-1",
            title = "  Mămăligă cu brânză  ",
            description = "Rețetă tradițională",
            baseServings = 4,
            prepMinutes = 10,
            cookMinutes = 25,
            ingredients = listOf(ingredient),
            steps = listOf(Step("s-1", 0, "Fierbe apa.")),
            tags = listOf(Tag("t-1", "rapid")),
            notes = "Servește caldă",
            isFavorite = true,
            createdAt = 1000L,
            updatedAt = 2000L,
        )

        assertEquals("faina alba", ingredient.normalizedName)
        assertEquals("mamaliga cu branza", recipe.normalizedTitle)
        assertEquals(35, recipe.totalMinutes)
    }

    @Test
    fun mealPlanEntryAndShoppingItemHoldExpectedDomainData() {
        val date = LocalDate(2026, 4, 15)
        val entry = MealPlanEntry(
            id = "mp-1",
            date = date,
            slot = MealSlot.DINNER,
            recipeId = "r-1",
            servings = 3,
        )
        assertEquals(MealSlot.DINNER, entry.slot)
        assertEquals(3, entry.servings)

        val item = ShoppingItem(
            id = "sh-1",
            weekStart = LocalDate(2026, 4, 13),
            itemKey = UnitConverter.shoppingItemKey("faina", MeasurementUnit.G),
            name = "Făină",
            normalizedName = "faina",
            quantity = 500.0,
            unit = MeasurementUnit.G,
            aisle = Aisle.PANTRY,
            source = ShoppingItemSource.AUTO,
            checked = false,
        )
        assertEquals("faina|MASS", item.itemKey)
        assertEquals(ShoppingItemSource.AUTO, item.source)
        assertEquals(8, Aisle.entries.size)
        assertEquals(4, MealSlot.entries.size)
        assertEquals(2, ShoppingItemSource.entries.size)
        assertEquals(3, AppTheme.entries.size)
    }

    @Test
    fun systemClockAndUuidIdGeneratorProduceValidValues() {
        val clock: Clock = SystemClock { TimeZone.UTC }
        assertTrue(clock.nowEpochMillis() > 0L)
        assertTrue(clock.today().year >= 2024)

        val idGenerator: IdGenerator = UuidIdGenerator()
        val id1 = idGenerator.nextId()
        val id2 = idGenerator.nextId()
        assertEquals(36, id1.length)
        assertEquals(36, id2.length)
        assertNotEquals(id1, id2)
    }
}
