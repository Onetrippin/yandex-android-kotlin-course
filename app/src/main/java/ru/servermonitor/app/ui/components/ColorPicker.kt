package ru.servermonitor.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.servermonitor.app.R
import ru.servermonitor.app.ui.theme.ServerMonitorTheme

data class HsvColor(val hue: Float, val saturation: Float, val value: Float) {
    val color: Color get() = Color.hsv(hue, saturation, value)
}

@Composable
fun ColorPicker(
    color: HsvColor,
    onColorChange: (HsvColor) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentColor by rememberUpdatedState(color)
    val currentOnChange by rememberUpdatedState(onColorChange)
    val paletteDescription = stringResource(R.string.color_palette_description)
    val brightnessDescription = stringResource(R.string.brightness)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(color.color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .semantics { contentDescription = colorDescription(color) },
            )
            Text(stringResource(R.string.current_color), style = MaterialTheme.typography.titleMedium)
        }
        Canvas(
            Modifier.fillMaxWidth().aspectRatio(1.5f).padding(14.dp)
                .semantics { contentDescription = paletteDescription }
                .pointerInput(Unit) {
                    fun update(position: Offset) {
                        if (size.width == 0 || size.height == 0) return
                        currentOnChange(
                            currentColor.copy(
                                hue = (position.x / size.width).coerceIn(0f, 1f) * 360f,
                                saturation = 1f - (position.y / size.height).coerceIn(0f, 1f),
                            ),
                        )
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        update(down.position)
                        down.consume()
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.pressed) update(change.position)
                            change.consume()
                        } while (change.pressed)
                    }
                },
        ) {
            drawRect(Brush.horizontalGradient(HUE_COLORS))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.White)))
            drawRect(Color.Black.copy(alpha = 1f - color.value))
            val center = Offset(color.hue / 360f * size.width, (1f - color.saturation) * size.height)
            drawCircle(Color.White, 13.dp.toPx(), center)
            drawCircle(Color.Black, 12.dp.toPx(), center, style = Stroke(2.dp.toPx()))
            drawCircle(color.color, 10.dp.toPx(), center)
        }
        Text(stringResource(R.string.brightness), style = MaterialTheme.typography.labelLarge)
        Slider(
            value = color.value,
            onValueChange = { onColorChange(color.copy(value = it)) },
            modifier = Modifier.semantics { contentDescription = brightnessDescription },
        )
    }
}

private val HUE_COLORS = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)

private fun colorDescription(color: HsvColor): String =
    "HSV ${color.hue.toInt()}, ${(color.saturation * 100).toInt()}%, ${(color.value * 100).toInt()}%"

@Preview(showBackground = true)
@Composable
private fun ColorPickerPreview() {
    ServerMonitorTheme {
        ColorPicker(HsvColor(210f, .7f, .8f), onColorChange = {}, modifier = Modifier.padding(16.dp))
    }
}
