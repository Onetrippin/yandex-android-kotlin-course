package ru.servermonitor.app.ui.colorpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.servermonitor.app.R
import ru.servermonitor.app.ui.components.ColorPicker
import ru.servermonitor.app.ui.components.HsvColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColorPickerRoute(color: HsvColor, onCancel: () -> Unit, onConfirm: (HsvColor) -> Unit) {
    var hue by rememberSaveable { mutableFloatStateOf(color.hue) }
    var saturation by rememberSaveable { mutableFloatStateOf(color.saturation) }
    var value by rememberSaveable { mutableFloatStateOf(color.value) }
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.color_picker_title), style = MaterialTheme.typography.headlineSmall)
            ColorPicker(
                color = HsvColor(hue, saturation, value),
                onColorChange = {
                    hue = it.hue
                    saturation = it.saturation
                    value = it.value
                },
            )
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                Button(onClick = { onConfirm(HsvColor(hue, saturation, value)) }) {
                    Text(stringResource(R.string.done))
                }
            }
        }
    }
}
