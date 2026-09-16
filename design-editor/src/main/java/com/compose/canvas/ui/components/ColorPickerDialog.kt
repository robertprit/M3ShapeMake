package com.compose.canvas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.compose.canvas.ui.theme.ChromeBorder
import com.compose.canvas.ui.theme.ChromeOnSurface
import com.compose.canvas.ui.theme.ChromeSurface

@Composable
fun ColorPickerDialog(
    initial: Color,
    initialAlpha: Float,
    recents: List<Color>,
    onDismiss: () -> Unit,
    onConfirm: (Color, Float) -> Unit
) {
    val start = remember(initial, initialAlpha) { colorToHsv(initial, initialAlpha) }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var saturation by remember { mutableFloatStateOf(start[1]) }
    var value by remember { mutableFloatStateOf(start[2]) }
    var alpha by remember { mutableFloatStateOf(start[3]) }
    val current = hsvToColor(hue, saturation, value, 1f)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ChromeSurface,
            contentColor = ChromeOnSurface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Farbe wählen", style = MaterialTheme.typography.titleMedium, color = ChromeOnSurface)
                SaturationValueBox(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onChange = { s, v ->
                        saturation = s
                        value = v
                    }
                )
                Text("Farbton", style = MaterialTheme.typography.labelLarge, color = ChromeOnSurface)
                HueSlider(hue = hue, onHue = { hue = it })
                Text("Deckkraft", style = MaterialTheme.typography.labelLarge, color = ChromeOnSurface)
                AlphaSlider(color = current, alpha = alpha, onAlpha = { alpha = it })
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(current.copy(alpha = alpha), RoundedCornerShape(8.dp))
                            .border(1.dp, ChromeBorder, RoundedCornerShape(8.dp))
                    )
                    HexField(
                        color = current,
                        onColor = { parsed ->
                            val hsv = colorToHsv(parsed, alpha)
                            hue = hsv[0]
                            saturation = hsv[1]
                            value = hsv[2]
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (recents.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        recents.take(8).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(c, RoundedCornerShape(6.dp))
                                    .border(1.dp, ChromeBorder, RoundedCornerShape(6.dp))
                                    .clickable {
                                        val hsv = colorToHsv(c, alpha)
                                        hue = hsv[0]
                                        saturation = hsv[1]
                                        value = hsv[2]
                                    }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Abbrechen") }
                    TextButton(onClick = { onConfirm(current, alpha) }) { Text("Übernehmen") }
                }
            }
        }
    }
}

@Composable
private fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (Float, Float) -> Unit
) {
    val hueColor = hsvToColor(hue, 1f, 1f, 1f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .pointerInput(hue) {
                detectTapGestures { pos ->
                    onChange(
                        (pos.x / size.width).coerceIn(0f, 1f),
                        (1f - pos.y / size.height).coerceIn(0f, 1f)
                    )
                }
            }
            .pointerInput(hue) {
                detectDragGestures { change, _ ->
                    val pos = change.position
                    onChange(
                        (pos.x / size.width).coerceIn(0f, 1f),
                        (1f - pos.y / size.height).coerceIn(0f, 1f)
                    )
                }
            }
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, hueColor)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val cx = saturation * size.width
        val cy = (1f - value) * size.height
        drawCircle(Color.White, radius = 10.dp.toPx(), center = Offset(cx, cy), style = Stroke(3.dp.toPx()))
        drawCircle(Color.Black, radius = 10.dp.toPx(), center = Offset(cx, cy), style = Stroke(1.5.dp.toPx()))
    }
}

@Composable
private fun HueSlider(hue: Float, onHue: (Float) -> Unit) {
    val hues = listOf(
        Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { pos -> onHue((pos.x / size.width).coerceIn(0f, 1f) * 360f) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    onHue((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                }
            }
    ) {
        drawRect(Brush.horizontalGradient(hues), size = Size(size.width, size.height))
        val x = (hue / 360f) * size.width
        drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), strokeWidth = 4.dp.toPx())
        drawLine(Color.Black, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
    }
}

@Composable
private fun AlphaSlider(color: Color, alpha: Float, onAlpha: (Float) -> Unit) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { pos -> onAlpha((pos.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    onAlpha((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        val cell = 8.dp.toPx()
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = 0f
            var col = 0
            while (x < size.width) {
                drawRect(
                    color = if ((row + col) % 2 == 0) Color(0xFFD0D0D0) else Color.White,
                    topLeft = Offset(x, y),
                    size = Size(cell, cell)
                )
                x += cell
                col++
            }
            y += cell
            row++
        }
        drawRect(
            Brush.horizontalGradient(listOf(color.copy(alpha = 0f), color.copy(alpha = 1f))),
            size = Size(size.width, size.height)
        )
        val x = alpha * size.width
        drawLine(Color.White, Offset(x, 0f), Offset(x, size.height), strokeWidth = 4.dp.toPx())
        drawLine(Color.Black, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
    }
}

@Composable
private fun HexField(
    color: Color,
    onColor: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val hex = if (color == Color.Unspecified) "" else String.format("#%06X", 0xFFFFFF and color.toArgb())
    var text by remember(hex) { mutableStateOf(hex) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            parseHexColor(raw)?.let(onColor)
        },
        label = { Text("Hex", color = ChromeOnSurface, fontSize = 13.sp) },
        modifier = modifier,
        singleLine = true,
        textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = ChromeOnSurface),
        colors = inspectorFieldColors()
    )
}

@Composable
internal fun inspectorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = ChromeOnSurface,
    unfocusedTextColor = ChromeOnSurface,
    disabledTextColor = ChromeOnSurface,
    focusedLabelColor = ChromeOnSurface,
    unfocusedLabelColor = ChromeOnSurface.copy(alpha = 0.75f),
    cursorColor = ChromeOnSurface,
    focusedBorderColor = ChromeOnSurface,
    unfocusedBorderColor = ChromeBorder
)

internal fun parseHexColor(raw: String): Color? {
    val hex = raw.trim().removePrefix("#")
    val normalized = when (hex.length) {
        3 -> hex.map { "$it$it" }.joinToString("")
        6 -> hex
        else -> return null
    }
    return runCatching { Color(android.graphics.Color.parseColor("#$normalized")) }.getOrNull()
}

internal fun hsvToColor(hue: Float, saturation: Float, value: Float, alpha: Float): Color {
    val argb = android.graphics.Color.HSVToColor(
        (alpha.coerceIn(0f, 1f) * 255f).toInt(),
        floatArrayOf(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    )
    return Color(argb)
}

internal fun colorToHsv(color: Color, alpha: Float): FloatArray {
    if (color == Color.Unspecified) return floatArrayOf(0f, 0f, 1f, alpha.coerceIn(0f, 1f))
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return floatArrayOf(hsv[0], hsv[1], hsv[2], alpha.coerceIn(0f, 1f))
}
