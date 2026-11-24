package com.ro.aminvatatro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.core.units.QuantityFormatter
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.ui.resources.labelRes
import org.jetbrains.compose.resources.stringResource

/**
 * Displays a single ingredient with formatted quantity, unit label, ingredient name,
 * and optional aisle tag.
 */
@Composable
fun IngredientRow(
    name: String,
    quantity: Double?,
    unit: MeasurementUnit,
    aisle: Aisle,
    language: AppLanguage,
    modifier: Modifier = Modifier,
    showAisle: Boolean = true,
    calories: Double? = null,
) {
    val unitLabel = stringResource(unit.labelRes())
    val formattedQty = QuantityFormatter.format(quantity = quantity, language = language)
    val quantityText = when {
        unit == MeasurementUnit.TO_TASTE -> unitLabel
        formattedQty.isNotEmpty() -> "$formattedQty $unitLabel"
        else -> ""
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (calories != null && calories > 0.0) {
            Text(
                text = "${calories.toInt()} kcal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (quantityText.isNotEmpty()) {
            Text(
                text = quantityText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (showAisle) {
            Text(
                text = stringResource(aisle.labelRes()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
