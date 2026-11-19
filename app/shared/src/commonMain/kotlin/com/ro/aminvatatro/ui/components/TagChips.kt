package com.ro.aminvatatro.ui.components

import aminvatatro.app.shared.generated.resources.Res
import aminvatatro.app.shared.generated.resources.action_remove
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ro.aminvatatro.ui.icons.AppIcons
import org.jetbrains.compose.resources.stringResource

/**
 * Displays recipe tags as read-only chips, toggleable filter chips, or removable input chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChips(
    tags: List<String>,
    modifier: Modifier = Modifier,
    selectedTags: Set<String> = emptySet(),
    onTagClick: ((String) -> Unit)? = null,
    onRemoveTag: ((String) -> Unit)? = null,
) {
    if (tags.isEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.forEach { tag ->
            when {
                onRemoveTag != null -> InputChip(
                    selected = false,
                    onClick = { onRemoveTag(tag) },
                    label = { Text(tag) },
                    trailingIcon = {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = stringResource(Res.string.action_remove),
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )

                onTagClick != null -> FilterChip(
                    selected = tag in selectedTags,
                    onClick = { onTagClick(tag) },
                    label = { Text(tag) },
                )

                else -> AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(tag) },
                )
            }
        }
    }
}
