package com.ro.aminvatatro.presentation.settings

import aminvatatro.app.shared.generated.resources.Res
import aminvatatro.app.shared.generated.resources.action_cancel
import aminvatatro.app.shared.generated.resources.action_delete
import aminvatatro.app.shared.generated.resources.error_generic
import aminvatatro.app.shared.generated.resources.settings_demo_clear
import aminvatatro.app.shared.generated.resources.settings_demo_clear_confirm_message
import aminvatatro.app.shared.generated.resources.settings_demo_clear_confirm_title
import aminvatatro.app.shared.generated.resources.settings_demo_cleared_msg
import aminvatatro.app.shared.generated.resources.settings_demo_description
import aminvatatro.app.shared.generated.resources.settings_demo_load
import aminvatatro.app.shared.generated.resources.settings_demo_loaded_msg
import aminvatatro.app.shared.generated.resources.settings_export_all
import aminvatatro.app.shared.generated.resources.settings_export_copied
import aminvatatro.app.shared.generated.resources.settings_export_copy
import aminvatatro.app.shared.generated.resources.settings_export_ready
import aminvatatro.app.shared.generated.resources.settings_export_select_toggle
import aminvatatro.app.shared.generated.resources.settings_export_selected
import aminvatatro.app.shared.generated.resources.settings_import_action
import aminvatatro.app.shared.generated.resources.settings_import_error_json
import aminvatatro.app.shared.generated.resources.settings_import_error_validation
import aminvatatro.app.shared.generated.resources.settings_import_error_version
import aminvatatro.app.shared.generated.resources.settings_import_label
import aminvatatro.app.shared.generated.resources.settings_import_placeholder
import aminvatatro.app.shared.generated.resources.settings_import_policy
import aminvatatro.app.shared.generated.resources.settings_import_policy_keep_both
import aminvatatro.app.shared.generated.resources.settings_import_policy_skip
import aminvatatro.app.shared.generated.resources.settings_import_success
import aminvatatro.app.shared.generated.resources.settings_language
import aminvatatro.app.shared.generated.resources.settings_section_appearance
import aminvatatro.app.shared.generated.resources.settings_section_demo
import aminvatatro.app.shared.generated.resources.settings_section_import_export
import aminvatatro.app.shared.generated.resources.settings_theme
import aminvatatro.app.shared.generated.resources.settings_title
import aminvatatro.app.shared.generated.resources.settings_units
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.domain.model.AppTheme
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.usecase.importexport.DuplicatePolicy
import com.ro.aminvatatro.ui.components.ConfirmDialog
import com.ro.aminvatatro.ui.components.ErrorBanner
import com.ro.aminvatatro.ui.icons.AppIcons
import com.ro.aminvatatro.ui.resources.labelRes
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Settings screen allowing the user to configure theme, language, unit system,
 * load/clear demo recipes, and export/import recipes as JSON.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    var showClearDemoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is SettingsUiEvent.DemoLoaded -> getString(Res.string.settings_demo_loaded_msg, event.count)
                SettingsUiEvent.DemoCleared -> getString(Res.string.settings_demo_cleared_msg)
                is SettingsUiEvent.ExportReady -> getString(
                    Res.string.settings_export_ready,
                    event.count,
                    event.schemaVersion,
                )

                is SettingsUiEvent.ImportSucceeded -> getString(
                    Res.string.settings_import_success,
                    event.imported,
                    event.skipped,
                    event.duplicated,
                )
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showClearDemoDialog) {
        ConfirmDialog(
            title = stringResource(Res.string.settings_demo_clear_confirm_title),
            message = stringResource(Res.string.settings_demo_clear_confirm_message),
            confirmLabel = stringResource(Res.string.action_delete),
            cancelLabel = stringResource(Res.string.action_cancel),
            isDestructive = true,
            onConfirm = {
                showClearDemoDialog = false
                viewModel.clearDemoData()
            },
            onDismiss = { showClearDemoDialog = false },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.settings_title)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isBusy) {
                item(key = "progress") {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            state.error?.let { error ->
                item(key = "error") {
                    ErrorBanner(
                        message = when (error) {
                            SettingsError.Generic -> stringResource(Res.string.error_generic)
                            is SettingsError.InvalidJson -> stringResource(Res.string.settings_import_error_json, error.reason)
                            is SettingsError.UnsupportedSchemaVersion -> stringResource(
                                Res.string.settings_import_error_version,
                                error.found,
                                error.supported,
                            )

                            is SettingsError.ImportValidationFailed -> stringResource(
                                Res.string.settings_import_error_validation,
                                error.errorCount,
                            )
                        },
                        onDismiss = viewModel::dismissError,
                    )
                }
            }

            item(key = "appearance") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_section_appearance),
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Text(stringResource(Res.string.settings_theme), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTheme.entries.forEach { theme ->
                                FilterChip(
                                    selected = state.settings.theme == theme,
                                    onClick = { viewModel.setTheme(theme) },
                                    label = { Text(stringResource(theme.labelRes())) },
                                )
                            }
                        }

                        Text(stringResource(Res.string.settings_language), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.forEach { language ->
                                FilterChip(
                                    selected = state.settings.language == language,
                                    onClick = { viewModel.setLanguage(language) },
                                    label = { Text(stringResource(language.labelRes())) },
                                )
                            }
                        }

                        Text(stringResource(Res.string.settings_units), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnitSystem.entries.forEach { unitSystem ->
                                FilterChip(
                                    selected = state.settings.unitSystem == unitSystem,
                                    onClick = { viewModel.setUnitSystem(unitSystem) },
                                    label = { Text(stringResource(unitSystem.labelRes())) },
                                )
                            }
                        }
                    }
                }
            }

            item(key = "demo_data") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_section_demo),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(Res.string.settings_demo_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = viewModel::loadDemoData,
                                enabled = !state.isBusy,
                            ) {
                                Text(stringResource(Res.string.settings_demo_load))
                            }
                            OutlinedButton(
                                onClick = { showClearDemoDialog = true },
                                enabled = !state.isBusy,
                            ) {
                                Text(stringResource(Res.string.settings_demo_clear))
                            }
                        }
                    }
                }
            }

            item(key = "import_export") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_section_import_export),
                            style = MaterialTheme.typography.titleMedium,
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = viewModel::exportAll,
                                enabled = !state.isBusy,
                            ) {
                                Text(stringResource(Res.string.settings_export_all))
                            }
                            if (state.recipes.isNotEmpty()) {
                                OutlinedButton(onClick = viewModel::toggleRecipeSelectionPanel) {
                                    Text(stringResource(Res.string.settings_export_select_toggle))
                                }
                            }
                            if (state.selectedRecipeIdsForExport.isNotEmpty()) {
                                Button(
                                    onClick = viewModel::exportSelected,
                                    enabled = !state.isBusy,
                                ) {
                                    Text(
                                        stringResource(
                                            Res.string.settings_export_selected,
                                            state.selectedRecipeIdsForExport.size,
                                        ),
                                    )
                                }
                            }
                        }

                        if (state.showRecipeSelection && state.recipes.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                state.recipes.forEach { recipe ->
                                    val selected = recipe.id in state.selectedRecipeIdsForExport
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp)
                                            .toggleable(
                                                value = selected,
                                                role = Role.Checkbox,
                                                onValueChange = { viewModel.toggleRecipeForExport(recipe.id) },
                                            ),
                                    ) {
                                        Checkbox(
                                            checked = selected,
                                            onCheckedChange = null,
                                        )
                                        Text(
                                            text = recipe.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }

                        if (state.exportedJson.isNotEmpty()) {
                            OutlinedTextField(
                                value = state.exportedJson,
                                onValueChange = {},
                                readOnly = true,
                                maxLines = 6,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(state.exportedJson))
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(getString(Res.string.settings_export_copied))
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = AppIcons.Copy,
                                    contentDescription = stringResource(Res.string.settings_export_copy),
                                )
                                Spacer(Modifier.padding(horizontal = 4.dp))
                                Text(stringResource(Res.string.settings_export_copy))
                            }
                        }

                        HorizontalDivider()

                        OutlinedTextField(
                            value = state.importJsonInput,
                            onValueChange = viewModel::onImportJsonChanged,
                            label = { Text(stringResource(Res.string.settings_import_label)) },
                            placeholder = { Text(stringResource(Res.string.settings_import_placeholder)) },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            text = stringResource(Res.string.settings_import_policy),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.duplicatePolicy == DuplicatePolicy.SKIP,
                                onClick = { viewModel.setDuplicatePolicy(DuplicatePolicy.SKIP) },
                                label = { Text(stringResource(Res.string.settings_import_policy_skip)) },
                            )
                            FilterChip(
                                selected = state.duplicatePolicy == DuplicatePolicy.KEEP_BOTH,
                                onClick = { viewModel.setDuplicatePolicy(DuplicatePolicy.KEEP_BOTH) },
                                label = { Text(stringResource(Res.string.settings_import_policy_keep_both)) },
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = viewModel::importFromJson,
                            enabled = !state.isBusy && state.importJsonInput.isNotBlank(),
                        ) {
                            Text(stringResource(Res.string.settings_import_action))
                        }
                    }
                }
            }
        }
    }
}
