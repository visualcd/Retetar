package com.ro.aminvatatro.core.nutrition

import com.ro.aminvatatro.core.text.TextNormalizer
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.core.units.UnitCategory
import com.ro.aminvatatro.core.units.UnitConverter

/**
 * Nutritional reference for a base food item.
 *
 * @property kcalPer100g Energy in kcal per 100 grams of edible portion.
 * @property pieceWeightGrams Typical weight in grams for a single piece/unit (e.g. 1 egg ~ 55g).
 * @property mlDensityGramsPerMl Density in grams per milliliter for liquids (e.g. oil ~ 0.92, milk ~ 1.03, water ~ 1.0).
 */
data class NutritionReference(
    val canonicalName: String,
    val kcalPer100g: Double,
    val pieceWeightGrams: Double = 100.0,
    val mlDensityGramsPerMl: Double = 1.0,
)

/**
 * Built-in reference database containing average caloric values for standard ingredients
 * (cereals, flours, dairy, meats, vegetables, fruits, condiments, fats/oils).
 *
 * Serves as the primary reliable offline fallback and offline-first nutrition provider.
 */
object NutritionDatabase {

    private val items = listOf(
        // Cămară / Cereale & Făinuri & Paste
        NutritionReference("faina", 364.0, pieceWeightGrams = 100.0),
        NutritionReference("faina de grau", 364.0, pieceWeightGrams = 100.0),
        NutritionReference("malai", 365.0, pieceWeightGrams = 100.0),
        NutritionReference("orez", 360.0, pieceWeightGrams = 100.0),
        NutritionReference("spaghete", 371.0, pieceWeightGrams = 100.0),
        NutritionReference("paste", 371.0, pieceWeightGrams = 100.0),
        NutritionReference("taitei", 380.0, pieceWeightGrams = 100.0),
        NutritionReference("ovaz", 389.0, pieceWeightGrams = 100.0),
        NutritionReference("fulgi de ovaz", 389.0, pieceWeightGrams = 100.0),
        NutritionReference("zahar", 387.0, pieceWeightGrams = 100.0),
        NutritionReference("zahar vanilat", 390.0, pieceWeightGrams = 10.0),
        NutritionReference("cacao", 228.0, pieceWeightGrams = 20.0),
        NutritionReference("pesmet", 395.0, pieceWeightGrams = 100.0),
        NutritionReference("bicarbonat", 0.0, pieceWeightGrams = 5.0),
        NutritionReference("drojdie", 105.0, pieceWeightGrams = 25.0),
        NutritionReference("praf de copt", 53.0, pieceWeightGrams = 10.0),

        // Grăsimi & Uleiuri
        NutritionReference("ulei", 884.0, mlDensityGramsPerMl = 0.92),
        NutritionReference("ulei de floarea soarelui", 884.0, mlDensityGramsPerMl = 0.92),
        NutritionReference("ulei de masline", 884.0, mlDensityGramsPerMl = 0.92),
        NutritionReference("unt", 717.0, pieceWeightGrams = 200.0),
        NutritionReference("untura", 902.0, pieceWeightGrams = 100.0),

        // Lactate & Ouă
        NutritionReference("oua", 143.0, pieceWeightGrams = 55.0),
        NutritionReference("ou", 143.0, pieceWeightGrams = 55.0),
        NutritionReference("lapte", 64.0, mlDensityGramsPerMl = 1.03),
        NutritionReference("smantana", 206.0, mlDensityGramsPerMl = 1.02),
        NutritionReference("iaurt", 61.0, mlDensityGramsPerMl = 1.03),
        NutritionReference("iaurt grecesc", 97.0, mlDensityGramsPerMl = 1.03),
        NutritionReference("branza telemea", 264.0, pieceWeightGrams = 150.0),
        NutritionReference("telemea", 264.0, pieceWeightGrams = 150.0),
        NutritionReference("branza de vaci", 98.0, pieceWeightGrams = 200.0),
        NutritionReference("cascaval", 356.0, pieceWeightGrams = 200.0),
        NutritionReference("mozzarella", 280.0, pieceWeightGrams = 125.0),
        NutritionReference("parmezan", 431.0, pieceWeightGrams = 100.0),
        NutritionReference("parmezan ras", 431.0, pieceWeightGrams = 100.0),
        NutritionReference("feta", 264.0, pieceWeightGrams = 150.0),

        // Carne & Pește
        NutritionReference("piept de pui", 165.0, pieceWeightGrams = 200.0),
        NutritionReference("pui", 180.0, pieceWeightGrams = 200.0),
        NutritionReference("carne de pui", 180.0, pieceWeightGrams = 200.0),
        NutritionReference("pulpe de pui", 215.0, pieceWeightGrams = 180.0),
        NutritionReference("carne tocata de porc", 263.0, pieceWeightGrams = 100.0),
        NutritionReference("carne de porc", 242.0, pieceWeightGrams = 150.0),
        NutritionReference("porc", 242.0, pieceWeightGrams = 150.0),
        NutritionReference("bacon", 541.0, pieceWeightGrams = 30.0),
        NutritionReference("carne de vita", 250.0, pieceWeightGrams = 200.0),
        NutritionReference("vita", 250.0, pieceWeightGrams = 200.0),
        NutritionReference("somon", 208.0, pieceWeightGrams = 150.0),
        NutritionReference("ton", 132.0, pieceWeightGrams = 160.0),
        NutritionReference("peste", 100.0, pieceWeightGrams = 150.0),
        NutritionReference("sunca", 145.0, pieceWeightGrams = 30.0),

        // Legume
        NutritionReference("morcov", 41.0, pieceWeightGrams = 80.0),
        NutritionReference("morcovi", 41.0, pieceWeightGrams = 80.0),
        NutritionReference("cartof", 77.0, pieceWeightGrams = 150.0),
        NutritionReference("cartofi", 77.0, pieceWeightGrams = 150.0),
        NutritionReference("ceapa", 40.0, pieceWeightGrams = 100.0),
        NutritionReference("ceapa rosie", 40.0, pieceWeightGrams = 100.0),
        NutritionReference("ceapa verde", 32.0, pieceWeightGrams = 20.0),
        NutritionReference("usturoi", 149.0, pieceWeightGrams = 5.0),
        NutritionReference("ardei", 31.0, pieceWeightGrams = 120.0),
        NutritionReference("ardei gras", 31.0, pieceWeightGrams = 120.0),
        NutritionReference("ardei rosu", 31.0, pieceWeightGrams = 120.0),
        NutritionReference("ardei kapia", 35.0, pieceWeightGrams = 120.0),
        NutritionReference("rosie", 18.0, pieceWeightGrams = 120.0),
        NutritionReference("rosii", 18.0, pieceWeightGrams = 120.0),
        NutritionReference("rosii pasate", 24.0, mlDensityGramsPerMl = 1.05),
        NutritionReference("suc de rosii", 20.0, mlDensityGramsPerMl = 1.03),
        NutritionReference("pasta de tomate", 82.0, pieceWeightGrams = 70.0),
        NutritionReference("castravete", 15.0, pieceWeightGrams = 120.0),
        NutritionReference("castraveti", 15.0, pieceWeightGrams = 120.0),
        NutritionReference("dovlecel", 17.0, pieceWeightGrams = 200.0),
        NutritionReference("vinete", 25.0, pieceWeightGrams = 300.0),
        NutritionReference("varza", 25.0, pieceWeightGrams = 500.0),
        NutritionReference("mazare", 81.0, pieceWeightGrams = 100.0),
        NutritionReference("fasole", 130.0, pieceWeightGrams = 100.0),
        NutritionReference("fasole boabe", 347.0, pieceWeightGrams = 100.0),
        NutritionReference("spanac", 23.0, pieceWeightGrams = 100.0),
        NutritionReference("salata", 15.0, pieceWeightGrams = 150.0),
        NutritionReference("salata verde", 15.0, pieceWeightGrams = 150.0),
        NutritionReference("ciuperci", 22.0, pieceWeightGrams = 30.0),
        NutritionReference("patrunjel", 36.0, pieceWeightGrams = 20.0),
        NutritionReference("marar", 43.0, pieceWeightGrams = 20.0),
        NutritionReference("leustean", 35.0, pieceWeightGrams = 15.0),
        NutritionReference("telina", 16.0, pieceWeightGrams = 100.0),
        NutritionReference("masline", 115.0, pieceWeightGrams = 5.0),

        // Fructe & Desert
        NutritionReference("mar", 52.0, pieceWeightGrams = 150.0),
        NutritionReference("mere", 52.0, pieceWeightGrams = 150.0),
        NutritionReference("banana", 89.0, pieceWeightGrams = 120.0),
        NutritionReference("banane", 89.0, pieceWeightGrams = 120.0),
        NutritionReference("lamaie", 29.0, pieceWeightGrams = 100.0),
        NutritionReference("suc de lamaie", 22.0, mlDensityGramsPerMl = 1.02),
        NutritionReference("capsuni", 32.0, pieceWeightGrams = 20.0),
        NutritionReference("afine", 57.0, pieceWeightGrams = 100.0),
        NutritionReference("zmeura", 52.0, pieceWeightGrams = 100.0),
        NutritionReference("miere", 304.0, mlDensityGramsPerMl = 1.4),
        NutritionReference("nuci", 654.0, pieceWeightGrams = 5.0),
        NutritionReference("migdale", 579.0, pieceWeightGrams = 2.0),

        // Condimente / Diverse
        NutritionReference("sare", 0.0, pieceWeightGrams = 5.0),
        NutritionReference("piper", 251.0, pieceWeightGrams = 3.0),
        NutritionReference("piper negru", 251.0, pieceWeightGrams = 3.0),
        NutritionReference("boia", 282.0, pieceWeightGrams = 5.0),
        NutritionReference("boia dulce", 282.0, pieceWeightGrams = 5.0),
        NutritionReference("cimbru", 276.0, pieceWeightGrams = 5.0),
        NutritionReference("oregano", 265.0, pieceWeightGrams = 5.0),
        NutritionReference("otet", 18.0, mlDensityGramsPerMl = 1.01),
        NutritionReference("mustar", 66.0, pieceWeightGrams = 20.0),
        NutritionReference("apa", 0.0, mlDensityGramsPerMl = 1.0),
        NutritionReference("apa minerala", 0.0, mlDensityGramsPerMl = 1.0),
        NutritionReference("bors", 12.0, mlDensityGramsPerMl = 1.0),
    )

    /**
     * Finds the closest matching reference for [rawName] using normalized tokens.
     */
    fun findReference(rawName: String): NutritionReference? {
        val normalized = TextNormalizer.normalize(rawName).trim()
        if (normalized.isEmpty()) return null

        // 1. Exact match
        items.firstOrNull { it.canonicalName == normalized }?.let { return it }

        // 2. Contains match (e.g. "piept de pui dezosat" -> "piept de pui")
        items.firstOrNull { normalized.contains(it.canonicalName) }?.let { return it }

        // 3. Reverse contains match (e.g. "morcovi" -> "morcov")
        items.firstOrNull { it.canonicalName.contains(normalized) }?.let { return it }

        // 4. Token match: check if any multi-word or single word matches
        val tokens = normalized.split(' ').filter { it.length > 2 }
        for (token in tokens) {
            items.firstOrNull { it.canonicalName == token }?.let { return it }
        }

        return null
    }

    /**
     * Calculates the estimated calories for a given ingredient [name], [quantity], and [unit].
     *
     * Returns `null` if [quantity] is null, invalid, or no nutrition data could be estimated.
     */
    fun calculateCalories(
        name: String,
        quantity: Double?,
        unit: MeasurementUnit,
        kcalPer100gOverride: Double? = null,
    ): Double? {
        if (unit == MeasurementUnit.TO_TASTE) return 0.0
        if (quantity == null || !quantity.isFinite() || quantity <= 0.0) return null

        val reference = findReference(name)
        val kcal100g = kcalPer100gOverride ?: reference?.kcalPer100g ?: return null

        val grams: Double = when (unit.category) {
            UnitCategory.MASS -> {
                UnitConverter.toBaseUnit(quantity, unit) ?: return null
            }
            UnitCategory.VOLUME -> {
                val ml = UnitConverter.toBaseUnit(quantity, unit) ?: return null
                val density = reference?.mlDensityGramsPerMl ?: 1.0
                ml * density
            }
            UnitCategory.COUNT -> {
                val pieceGrams = reference?.pieceWeightGrams ?: 100.0
                quantity * pieceGrams
            }
            UnitCategory.OTHER -> {
                // PINCH is about 0.36g
                if (unit == MeasurementUnit.PINCH) 0.5 else 0.0
            }
        }

        return (grams * kcal100g) / 100.0
    }
}
