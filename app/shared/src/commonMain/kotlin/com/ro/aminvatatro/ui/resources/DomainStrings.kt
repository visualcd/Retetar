package com.ro.aminvatatro.ui.resources

import aminvatatro.app.shared.generated.resources.Res
import aminvatatro.app.shared.generated.resources.aisle_bakery
import aminvatatro.app.shared.generated.resources.aisle_dairy
import aminvatatro.app.shared.generated.resources.aisle_frozen
import aminvatatro.app.shared.generated.resources.aisle_meat_fish
import aminvatatro.app.shared.generated.resources.aisle_other
import aminvatatro.app.shared.generated.resources.aisle_pantry
import aminvatatro.app.shared.generated.resources.aisle_produce
import aminvatatro.app.shared.generated.resources.aisle_spices
import aminvatatro.app.shared.generated.resources.day_fri
import aminvatatro.app.shared.generated.resources.day_friday_full
import aminvatatro.app.shared.generated.resources.day_mon
import aminvatatro.app.shared.generated.resources.day_monday_full
import aminvatatro.app.shared.generated.resources.day_sat
import aminvatatro.app.shared.generated.resources.day_saturday_full
import aminvatatro.app.shared.generated.resources.day_sun
import aminvatatro.app.shared.generated.resources.day_sunday_full
import aminvatatro.app.shared.generated.resources.day_thu
import aminvatatro.app.shared.generated.resources.day_thursday_full
import aminvatatro.app.shared.generated.resources.day_tue
import aminvatatro.app.shared.generated.resources.day_tuesday_full
import aminvatatro.app.shared.generated.resources.day_wed
import aminvatatro.app.shared.generated.resources.day_wednesday_full
import aminvatatro.app.shared.generated.resources.settings_language_en
import aminvatatro.app.shared.generated.resources.settings_language_ro
import aminvatatro.app.shared.generated.resources.settings_theme_dark
import aminvatatro.app.shared.generated.resources.settings_theme_light
import aminvatatro.app.shared.generated.resources.settings_theme_system
import aminvatatro.app.shared.generated.resources.settings_units_imperial
import aminvatatro.app.shared.generated.resources.settings_units_metric
import aminvatatro.app.shared.generated.resources.slot_breakfast
import aminvatatro.app.shared.generated.resources.slot_dinner
import aminvatatro.app.shared.generated.resources.slot_lunch
import aminvatatro.app.shared.generated.resources.slot_snack
import aminvatatro.app.shared.generated.resources.unit_cup
import aminvatatro.app.shared.generated.resources.unit_g
import aminvatatro.app.shared.generated.resources.unit_kg
import aminvatatro.app.shared.generated.resources.unit_l
import aminvatatro.app.shared.generated.resources.unit_lb
import aminvatatro.app.shared.generated.resources.unit_ml
import aminvatatro.app.shared.generated.resources.unit_oz
import aminvatatro.app.shared.generated.resources.unit_piece
import aminvatatro.app.shared.generated.resources.unit_pinch
import aminvatatro.app.shared.generated.resources.unit_tbsp
import aminvatatro.app.shared.generated.resources.unit_to_taste
import aminvatatro.app.shared.generated.resources.unit_tsp
import aminvatatro.app.shared.generated.resources.validation_cook_invalid
import aminvatatro.app.shared.generated.resources.validation_empty_ingredients
import aminvatatro.app.shared.generated.resources.validation_empty_steps
import aminvatatro.app.shared.generated.resources.validation_ingredient_name_blank
import aminvatatro.app.shared.generated.resources.validation_ingredient_qty_invalid
import aminvatatro.app.shared.generated.resources.validation_ingredient_to_taste_qty
import aminvatatro.app.shared.generated.resources.validation_prep_invalid
import aminvatatro.app.shared.generated.resources.validation_servings_invalid
import aminvatatro.app.shared.generated.resources.validation_step_text_blank
import aminvatatro.app.shared.generated.resources.validation_title_blank
import aminvatatro.app.shared.generated.resources.validation_title_too_long
import androidx.compose.runtime.Composable
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.domain.model.AppTheme
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.validation.RecipeValidationError
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Returns the localized [StringResource] for a [MeasurementUnit]. */
fun MeasurementUnit.labelRes(): StringResource = when (this) {
    MeasurementUnit.G -> Res.string.unit_g
    MeasurementUnit.KG -> Res.string.unit_kg
    MeasurementUnit.OZ -> Res.string.unit_oz
    MeasurementUnit.LB -> Res.string.unit_lb
    MeasurementUnit.ML -> Res.string.unit_ml
    MeasurementUnit.L -> Res.string.unit_l
    MeasurementUnit.TSP -> Res.string.unit_tsp
    MeasurementUnit.TBSP -> Res.string.unit_tbsp
    MeasurementUnit.CUP -> Res.string.unit_cup
    MeasurementUnit.PIECE -> Res.string.unit_piece
    MeasurementUnit.PINCH -> Res.string.unit_pinch
    MeasurementUnit.TO_TASTE -> Res.string.unit_to_taste
}

/** Returns the localized [StringResource] for an [Aisle]. */
fun Aisle.labelRes(): StringResource = when (this) {
    Aisle.PRODUCE -> Res.string.aisle_produce
    Aisle.MEAT_FISH -> Res.string.aisle_meat_fish
    Aisle.DAIRY -> Res.string.aisle_dairy
    Aisle.BAKERY -> Res.string.aisle_bakery
    Aisle.PANTRY -> Res.string.aisle_pantry
    Aisle.SPICES -> Res.string.aisle_spices
    Aisle.FROZEN -> Res.string.aisle_frozen
    Aisle.OTHER -> Res.string.aisle_other
}

/** Returns the localized [StringResource] for a [MealSlot]. */
fun MealSlot.labelRes(): StringResource = when (this) {
    MealSlot.BREAKFAST -> Res.string.slot_breakfast
    MealSlot.LUNCH -> Res.string.slot_lunch
    MealSlot.DINNER -> Res.string.slot_dinner
    MealSlot.SNACK -> Res.string.slot_snack
}

/** Returns the short weekday [StringResource] for a [DayOfWeek]. */
fun DayOfWeek.shortLabelRes(): StringResource = when (this) {
    DayOfWeek.MONDAY -> Res.string.day_mon
    DayOfWeek.TUESDAY -> Res.string.day_tue
    DayOfWeek.WEDNESDAY -> Res.string.day_wed
    DayOfWeek.THURSDAY -> Res.string.day_thu
    DayOfWeek.FRIDAY -> Res.string.day_fri
    DayOfWeek.SATURDAY -> Res.string.day_sat
    DayOfWeek.SUNDAY -> Res.string.day_sun
}

/** Returns the full weekday [StringResource] for a [DayOfWeek]. */
fun DayOfWeek.fullLabelRes(): StringResource = when (this) {
    DayOfWeek.MONDAY -> Res.string.day_monday_full
    DayOfWeek.TUESDAY -> Res.string.day_tuesday_full
    DayOfWeek.WEDNESDAY -> Res.string.day_wednesday_full
    DayOfWeek.THURSDAY -> Res.string.day_thursday_full
    DayOfWeek.FRIDAY -> Res.string.day_friday_full
    DayOfWeek.SATURDAY -> Res.string.day_saturday_full
    DayOfWeek.SUNDAY -> Res.string.day_sunday_full
}

/** Returns the localized [StringResource] for an [AppTheme]. */
fun AppTheme.labelRes(): StringResource = when (this) {
    AppTheme.SYSTEM -> Res.string.settings_theme_system
    AppTheme.LIGHT -> Res.string.settings_theme_light
    AppTheme.DARK -> Res.string.settings_theme_dark
}

/** Returns the localized [StringResource] for an [AppLanguage]. */
fun AppLanguage.labelRes(): StringResource = when (this) {
    AppLanguage.RO -> Res.string.settings_language_ro
    AppLanguage.EN -> Res.string.settings_language_en
}

/** Returns the localized [StringResource] for a [UnitSystem]. */
fun UnitSystem.labelRes(): StringResource = when (this) {
    UnitSystem.METRIC -> Res.string.settings_units_metric
    UnitSystem.IMPERIAL -> Res.string.settings_units_imperial
}

/** Formats a [LocalDate] concisely as `dd.MM`. */
fun LocalDate.formatShortDayMonth(): String {
    val d = day.toString().padStart(2, '0')
    val m = month.number.toString().padStart(2, '0')
    return "$d.$m"
}

/** Formats a [RecipeValidationError] into a localized message. */
@Composable
fun RecipeValidationError.toMessage(): String = when (this) {
    RecipeValidationError.TitleBlank -> stringResource(Res.string.validation_title_blank)
    is RecipeValidationError.TitleTooLong -> stringResource(Res.string.validation_title_too_long, maxLength)
    is RecipeValidationError.InvalidServings -> stringResource(Res.string.validation_servings_invalid, min, max)
    is RecipeValidationError.InvalidPrepMinutes -> stringResource(Res.string.validation_prep_invalid, min, max)
    is RecipeValidationError.InvalidCookMinutes -> stringResource(Res.string.validation_cook_invalid, min, max)
    RecipeValidationError.EmptyIngredients -> stringResource(Res.string.validation_empty_ingredients)
    is RecipeValidationError.IngredientNameBlank -> stringResource(Res.string.validation_ingredient_name_blank, index + 1)
    is RecipeValidationError.InvalidIngredientQuantity -> stringResource(Res.string.validation_ingredient_qty_invalid, index + 1)
    is RecipeValidationError.QuantityNotAllowedForToTaste -> stringResource(Res.string.validation_ingredient_to_taste_qty, index + 1)
    RecipeValidationError.EmptySteps -> stringResource(Res.string.validation_empty_steps)
    is RecipeValidationError.StepTextBlank -> stringResource(Res.string.validation_step_text_blank, index + 1)
}
