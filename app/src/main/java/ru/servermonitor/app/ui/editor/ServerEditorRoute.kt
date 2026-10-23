package ru.servermonitor.app.ui.editor

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.servermonitor.app.R
import ru.servermonitor.app.data.model.Server
import ru.servermonitor.app.data.model.ServerEnvironment
import ru.servermonitor.app.ui.components.ColorPicker
import ru.servermonitor.app.ui.components.HsvColor

@Composable
fun ServerEditorRoute() {
    val initial = remember {
        Server(name = "Учебный сервер", host = "127.0.0.1", port = 8080,
            environment = ServerEnvironment.DEVELOPMENT, id = "demo-server")
    }
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var host by rememberSaveable { mutableStateOf(initial.host) }
    var port by rememberSaveable { mutableStateOf(initial.port.toString()) }
    var environment by rememberSaveable { mutableStateOf(initial.environment) }
    var maintenanceEpochDay by rememberSaveable { mutableStateOf(initial.maintenanceUntil?.toLocalDate()?.toEpochDay()) }
    var monitoringEnabled by rememberSaveable { mutableStateOf(initial.isMonitoringEnabled) }
    var presetColor by rememberSaveable { mutableIntStateOf(initial.labelColor) }
    var customSelected by rememberSaveable { mutableStateOf(false) }
    var hasCustomColor by rememberSaveable { mutableStateOf(false) }
    var customHue by rememberSaveable { mutableFloatStateOf(0f) }
    var customSaturation by rememberSaveable { mutableFloatStateOf(1f) }
    var customValue by rememberSaveable { mutableFloatStateOf(1f) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var pickerHue by rememberSaveable { mutableFloatStateOf(0f) }
    var pickerSaturation by rememberSaveable { mutableFloatStateOf(1f) }
    var pickerValue by rememberSaveable { mutableFloatStateOf(1f) }
    var dateDialogOpen by rememberSaveable { mutableStateOf(false) }
    val editorScrollState = rememberScrollState()
    val keyboard = LocalSoftwareKeyboardController.current
    val customHsv = HsvColor(customHue, customSaturation, customValue)
    val customColor = if (hasCustomColor) customHsv.color.toArgb() else null

    BackHandler(enabled = pickerOpen) { pickerOpen = false }
    Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
        if (pickerOpen) {
            ColorPickerScreen(
                color = HsvColor(pickerHue, pickerSaturation, pickerValue),
                onColorChange = {
                    pickerHue = it.hue
                    pickerSaturation = it.saturation
                    pickerValue = it.value
                },
                onCancel = { pickerOpen = false },
                onConfirm = {
                    customHue = pickerHue
                    customSaturation = pickerSaturation
                    customValue = pickerValue
                    hasCustomColor = true
                    customSelected = true
                    pickerOpen = false
                },
            )
        } else {
            ServerEditorScreen(
                draft = ServerEditorDraft(name, host, port, environment, maintenanceEpochDay, monitoringEnabled),
                onDraftChange = {
                    name = it.name
                    host = it.host
                    port = it.port.filter { char -> char in '0'..'9' }.take(5)
                    environment = it.environment
                    maintenanceEpochDay = it.maintenanceEpochDay
                    monitoringEnabled = it.monitoringEnabled
                },
                presetColor = presetColor,
                customColor = customColor,
                customSelected = customSelected,
                onPresetSelect = { presetColor = it; customSelected = false },
                onCustomSelect = { if (hasCustomColor) customSelected = true },
                onCustomEdit = {
                    keyboard?.hide()
                    val hsv = if (hasCustomColor) customHsv else {
                        val values = FloatArray(3)
                        AndroidColor.colorToHSV(presetColor, values)
                        HsvColor(values[0], values[1], values[2])
                    }
                    pickerHue = hsv.hue
                    pickerSaturation = hsv.saturation
                    pickerValue = hsv.value
                    pickerOpen = true
                },
                onDateClick = { keyboard?.hide(); dateDialogOpen = true },
                scrollState = editorScrollState,
            )
        }
        if (dateDialogOpen) {
            MaintenanceDateDialog(
                selectedEpochDay = maintenanceEpochDay,
                onDismiss = { dateDialogOpen = false },
                onConfirm = { maintenanceEpochDay = it; dateDialogOpen = false },
            )
        }
    }
}

@Composable
private fun ColorPickerScreen(
    color: HsvColor,
    onColorChange: (HsvColor) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.color_picker_title), style = MaterialTheme.typography.headlineSmall)
        ColorPicker(color, onColorChange)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.done))
            }
        }
    }
}
