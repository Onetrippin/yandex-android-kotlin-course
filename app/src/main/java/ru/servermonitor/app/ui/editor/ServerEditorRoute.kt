package ru.servermonitor.app.ui.editor

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.navigation.NavBackStackEntry
import ru.servermonitor.app.data.model.Server
import ru.servermonitor.app.data.model.ServerEnvironment
import ru.servermonitor.app.ui.components.HsvColor
import ru.servermonitor.app.ui.navigation.ServerDestinations
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Composable
fun ServerEditorRoute(
    server: Server?,
    onSave: (Server) -> Unit,
    onCancel: () -> Unit,
    onOpenColorPicker: (HsvColor) -> Unit,
    backStackEntry: NavBackStackEntry,
) {
    val generatedId = rememberSaveable { UUID.randomUUID().toString() }
    val serverId = server?.id ?: generatedId
    val initialIsCustom = server?.labelColor?.let { color -> color !in serverLabelColors.map { it.toArgb() } } ?: false
    val initialHsv = remember(serverId) {
        val values = FloatArray(3)
        AndroidColor.colorToHSV(server?.labelColor ?: AndroidColor.WHITE, values)
        HsvColor(values[0], values[1], values[2])
    }
    var name by rememberSaveable(serverId) { mutableStateOf(server?.name.orEmpty()) }
    var host by rememberSaveable(serverId) { mutableStateOf(server?.host.orEmpty()) }
    var port by rememberSaveable(serverId) { mutableStateOf(server?.port?.toString() ?: "8080") }
    var environment by rememberSaveable(serverId) { mutableStateOf(server?.environment ?: ServerEnvironment.DEVELOPMENT) }
    var maintenanceEpochDay by rememberSaveable(serverId) {
        mutableStateOf(server?.maintenanceUntil?.toLocalDate()?.toEpochDay())
    }
    var monitoringEnabled by rememberSaveable(serverId) { mutableStateOf(server?.isMonitoringEnabled ?: true) }
    var presetColor by rememberSaveable(serverId) { mutableIntStateOf(server?.labelColor?.takeIf { !initialIsCustom } ?: AndroidColor.WHITE) }
    var customSelected by rememberSaveable(serverId) { mutableStateOf(initialIsCustom) }
    var hasCustomColor by rememberSaveable(serverId) { mutableStateOf(initialIsCustom) }
    var customHue by rememberSaveable(serverId) { mutableFloatStateOf(initialHsv.hue) }
    var customSaturation by rememberSaveable(serverId) { mutableFloatStateOf(initialHsv.saturation) }
    var customValue by rememberSaveable(serverId) { mutableFloatStateOf(initialHsv.value) }
    var dateDialogOpen by rememberSaveable(serverId) { mutableStateOf(false) }
    var validationError by rememberSaveable(serverId) { mutableStateOf(false) }
    val editorScrollState = rememberScrollState()
    val keyboard = LocalSoftwareKeyboardController.current
    val customHsv = HsvColor(customHue, customSaturation, customValue)
    val customColor = if (hasCustomColor) customHsv.color.toArgb() else null

    val colorResult by backStackEntry.savedStateHandle
        .getStateFlow<Bundle?>(ServerDestinations.COLOR_RESULT, null)
        .collectAsState()
    LaunchedEffect(colorResult) {
        colorResult?.let { result ->
            customHue = result.getFloat(ServerDestinations.HUE)
            customSaturation = result.getFloat(ServerDestinations.SATURATION)
            customValue = result.getFloat(ServerDestinations.VALUE)
            hasCustomColor = true
            customSelected = true
            backStackEntry.savedStateHandle[ServerDestinations.COLOR_RESULT] = null
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        ServerEditorScreen(
            draft = ServerEditorDraft(name, host, port, environment, maintenanceEpochDay, monitoringEnabled),
            onDraftChange = {
                name = it.name
                host = it.host
                port = it.port.filter { char -> char in '0'..'9' }.take(5)
                environment = it.environment
                maintenanceEpochDay = it.maintenanceEpochDay
                monitoringEnabled = it.monitoringEnabled
                validationError = false
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
                onOpenColorPicker(hsv)
            },
            onDateClick = { keyboard?.hide(); dateDialogOpen = true },
            scrollState = editorScrollState,
            isEditing = server != null,
            showValidationError = validationError,
            onCancel = { keyboard?.hide(); onCancel() },
            onSave = {
                val validPort = port.toIntOrNull()?.takeIf { it in 1..65_535 }
                if (name.isBlank() || !isValidServerHost(host) || validPort == null) {
                    validationError = true
                } else {
                    val selectedDate = maintenanceEpochDay?.let {
                        LocalDateTime.of(LocalDate.ofEpochDay(it), LocalTime.of(23, 59, 59))
                    }
                    val preservedDate = server?.maintenanceUntil
                        ?.takeIf { it.toLocalDate().toEpochDay() == maintenanceEpochDay }
                    val result = Server(
                        id = serverId,
                        name = name.trim(),
                        host = host.trim(),
                        port = validPort,
                        environment = environment,
                        labelColor = if (customSelected && hasCustomColor) customHsv.color.toArgb() else presetColor,
                        maintenanceUntil = preservedDate ?: selectedDate,
                        decommissionAt = server?.decommissionAt,
                        isMonitoringEnabled = monitoringEnabled,
                    )
                    keyboard?.hide()
                    onSave(result)
                }
            },
        )
        if (dateDialogOpen) {
            MaintenanceDateDialog(
                selectedEpochDay = maintenanceEpochDay,
                onDismiss = { dateDialogOpen = false },
                onConfirm = { maintenanceEpochDay = it; dateDialogOpen = false },
            )
        }
    }
}
