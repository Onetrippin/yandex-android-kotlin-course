package ru.servermonitor.app.ui.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.servermonitor.app.R
import ru.servermonitor.app.data.model.ServerEnvironment
import ru.servermonitor.app.ui.theme.ServerMonitorTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal data class ServerEditorDraft(
    val name: String = "Учебный сервер",
    val host: String = "127.0.0.1",
    val port: String = "8080",
    val environment: ServerEnvironment = ServerEnvironment.DEVELOPMENT,
    val maintenanceEpochDay: Long? = null,
    val monitoringEnabled: Boolean = true,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ServerEditorScreen(
    draft: ServerEditorDraft,
    onDraftChange: (ServerEditorDraft) -> Unit,
    presetColor: Int,
    customColor: Int?,
    customSelected: Boolean,
    onPresetSelect: (Int) -> Unit,
    onCustomSelect: () -> Unit,
    onCustomEdit: () -> Unit,
    onDateClick: () -> Unit,
    scrollState: ScrollState,
    isEditing: Boolean = true,
    showValidationError: Boolean = false,
    onCancel: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    Column(
        Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(scrollState).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(if (isEditing) R.string.editor_title else R.string.create_server_title), style = MaterialTheme.typography.headlineSmall)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (draft.monitoringEnabled) MaterialTheme.colorScheme.surfaceContainerLow
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ServerNameField(draft.name) { onDraftChange(draft.copy(name = it)) }
                ServerConnectionFields(
                    host = draft.host,
                    port = draft.port,
                    showRequiredHostError = showValidationError,
                    onHostChange = { onDraftChange(draft.copy(host = it)) },
                    onPortChange = { onDraftChange(draft.copy(port = it)) },
                )
                ServerEnvironmentSelector(draft.environment) { onDraftChange(draft.copy(environment = it)) }
                ServerColorSelector(presetColor, customColor, customSelected, onPresetSelect, onCustomSelect, onCustomEdit)
                MaintenanceDateSection(
                    draft.maintenanceEpochDay, onDateClick,
                    onClear = { onDraftChange(draft.copy(maintenanceEpochDay = null)) },
                )
                MonitoringToggle(draft.monitoringEnabled) { onDraftChange(draft.copy(monitoringEnabled = it)) }
            }
        }
        if (showValidationError) {
            Text(stringResource(R.string.server_form_invalid), color = MaterialTheme.colorScheme.error)
        }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            androidx.compose.material3.Button(onClick = onSave) { Text(stringResource(R.string.save)) }
        }
    }
}

@Composable
private fun ServerNameField(name: String, onNameChange: (String) -> Unit) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.server_name)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
    )
}

@Composable
private fun ServerConnectionFields(
    host: String,
    port: String,
    showRequiredHostError: Boolean,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
) {
    val invalidHost = (host.isNotEmpty() || showRequiredHostError) && !isValidServerHost(host)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = host, onValueChange = onHostChange,
            label = { Text(stringResource(R.string.server_host)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = invalidHost,
            supportingText = {
                Text(stringResource(when {
                    invalidHost && host.isBlank() -> R.string.server_host_required
                    invalidHost -> R.string.invalid_server_host
                    else -> R.string.server_host_hint
                }))
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        val invalidPort = port.isNotEmpty() && port.toIntOrNull()?.let { it in 1..65_535 } != true
        OutlinedTextField(
            value = port, onValueChange = onPortChange,
            label = { Text(stringResource(R.string.server_port)) },
            modifier = Modifier.widthIn(max = 180.dp).fillMaxWidth(),
            singleLine = true, isError = invalidPort,
            supportingText = if (invalidPort) ({ Text(stringResource(R.string.invalid_port)) }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServerEnvironmentSelector(environment: ServerEnvironment, onSelect: (ServerEnvironment) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.environment), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ServerEnvironment.entries.forEach { option ->
                val isSelected = environment == option
                OutlinedButton(
                    onClick = { onSelect(option) },
                    modifier = Modifier.semantics { selected = isSelected },
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                ) {
                    Text(stringResource(option.labelRes))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ServerColorSelector(
    presetColor: Int,
    customColor: Int?,
    customSelected: Boolean,
    onPresetSelect: (Int) -> Unit,
    onCustomSelect: () -> Unit,
    onCustomEdit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.label_color), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            serverLabelColors.forEachIndexed { index, color ->
                ColorSwatch(
                    color = color,
                    isSelected = !customSelected && color.toArgb() == presetColor,
                    description = stringResource(R.string.preset_color_description, index + 1),
                    modifier = Modifier.clickable(role = Role.RadioButton) { onPresetSelect(color.toArgb()) },
                )
            }
            ColorSwatch(
                color = customColor?.let { Color(it) },
                isSelected = customSelected,
                description = stringResource(R.string.custom_color_description),
                modifier = Modifier.combinedClickable(
                    role = Role.RadioButton,
                    onClick = if (customColor == null) onCustomEdit else onCustomSelect,
                    onLongClickLabel = stringResource(R.string.edit_custom_color),
                    onLongClick = onCustomEdit,
                ),
            )
        }
        Text(stringResource(R.string.custom_color_hint), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ColorSwatch(color: Color?, isSelected: Boolean, description: String, modifier: Modifier = Modifier) {
    val selectedDescription = stringResource(if (isSelected) R.string.color_selected else R.string.color_not_selected)
    Box(
        modifier.size(48.dp).semantics {
            selected = isSelected
            contentDescription = "$description, $selectedDescription"
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(4.dp))
                .background(color ?: Color.White).border(1.dp, Color.Black, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (color == null) {
                Canvas(Modifier.size(34.dp)) {
                    drawRect(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                }
            }
            if (isSelected) Icon(
                Icons.Default.Check, contentDescription = null,
                tint = if ((color ?: Color.White).luminance() > .4f) Color.Black else Color.White,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaintenanceDateSection(epochDay: Long?, onDateClick: () -> Unit, onClear: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.maintenance_until), style = MaterialTheme.typography.titleSmall)
        Text(
            epochDay?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }
                ?: stringResource(R.string.date_not_selected),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDateClick) {
                Text(stringResource(R.string.choose_date))
            }
            if (epochDay != null) TextButton(onClick = onClear) {
                Text(stringResource(R.string.clear))
            }
        }
    }
}

@Composable
private fun MonitoringToggle(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.monitoring))
            Text(stringResource(if (enabled) R.string.monitoring_enabled else R.string.monitoring_disabled),
                style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = enabled, onCheckedChange = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaintenanceDateDialog(selectedEpochDay: Long?, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selectedEpochDay?.let { LocalDate.ofEpochDay(it).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let {
                        onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay())
                    }
                },
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    ) { DatePicker(state = state) }
}

private val ServerEnvironment.labelRes: Int
    @StringRes get() = when (this) {
        ServerEnvironment.DEVELOPMENT -> R.string.environment_development
        ServerEnvironment.STAGING -> R.string.environment_staging
        ServerEnvironment.PRODUCTION -> R.string.environment_production
    }

internal val serverLabelColors = listOf(
    Color(0xFFEF5350), Color(0xFFFF9800), Color(0xFFFFC107), Color(0xFF66BB6A),
    Color(0xFF26A69A), Color(0xFF42A5F5), Color(0xFF5C6BC0), Color(0xFFAB47BC),
    Color(0xFFEC407A), Color(0xFF795548), Color(0xFF607D8B), Color.White,
)

@Composable
private fun EditorPreview(draft: ServerEditorDraft) {
    ServerMonitorTheme {
        ServerEditorScreen(draft, {}, Color.White.toArgb(), null, false, {}, {}, {}, {}, rememberScrollState())
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ServerEditorPreview() = EditorPreview(ServerEditorDraft())

@Preview(showBackground = true, widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun EmptyServerPreview() = EditorPreview(ServerEditorDraft(name = "", host = "", port = ""))

@Preview(showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun LandscapeServerPreview() = EditorPreview(ServerEditorDraft(name = "Основной сервер базы данных и резервного копирования", monitoringEnabled = false))

@Preview(showBackground = true, widthDp = 840, heightDp = 1000, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TabletServerPreview() = EditorPreview(ServerEditorDraft(monitoringEnabled = false))
