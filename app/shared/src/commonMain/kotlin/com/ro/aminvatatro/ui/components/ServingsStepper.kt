package com.ro.aminvatatro.ui.components

import aminvatatro.app.shared.generated.resources.Res
import aminvatatro.app.shared.generated.resources.action_decrease_servings
import aminvatatro.app.shared.generated.resources.action_increase_servings
import aminvatatro.app.shared.generated.resources.servings_count
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ro.aminvatatro.domain.validation.RecipeValidator
import com.ro.aminvatatro.ui.icons.AppIcons
import org.jetbrains.compose.resources.stringResource

/**
 * Accessible servings stepper with `>= 48.dp` touch targets and `1..100` bounds.
 */
@Composable
fun ServingsStepper(
    servings: Int,
    onServingsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minServings: Int = RecipeValidator.MIN_SERVINGS,
    maxServings: Int = RecipeValidator.MAX_SERVINGS,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalIconButton(
            onClick = { if (servings > minServings) onServingsChange(servings - 1) },
            enabled = servings > minServings,
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(
                imageVector = AppIcons.Remove,
                contentDescription = stringResource(Res.string.action_decrease_servings),
            )
        }
        Text(
            text = stringResource(Res.string.servings_count, servings),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        FilledTonalIconButton(
            onClick = { if (servings < maxServings) onServingsChange(servings + 1) },
            enabled = servings < maxServings,
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(
                imageVector = AppIcons.Add,
                contentDescription = stringResource(Res.string.action_increase_servings),
            )
        }
    }
}
