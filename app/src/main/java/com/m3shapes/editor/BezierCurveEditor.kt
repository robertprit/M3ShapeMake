package com.m3shapes.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private enum class BezierHandle { P1, P2 }
private const val BEZIER_EDITOR_PADDING_PX = 20f

/**
 * Editable cubic-Bézier easing curve. The box maps time (x) and progress (y) to 0..1;
 * control handles P1/P2 can be dragged, y-values may overshoot for bounce/anticipation.
 */
@Composable
fun BezierCurveEditor(
    profile: BezierProfile,
    onProfileChange: (BezierProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    var activeHandle by remember { mutableStateOf<BezierHandle?>(null) }
    val currentProfile by rememberUpdatedState(profile)

    Canvas(
        modifier = modifier
            .background(surface, RoundedCornerShape(10.dp))
            .border(1.dp, onSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos ->
                        val nx = toNormalizedX(pos.x, size.width.toFloat())
                        val ny = toNormalizedY(pos.y, size.height.toFloat())
                        val d1 = kotlin.math.abs(nx - currentProfile.x1) + kotlin.math.abs(ny - currentProfile.y1)
                        val d2 = kotlin.math.abs(nx - currentProfile.x2) + kotlin.math.abs(ny - currentProfile.y2)
                        activeHandle = if (d1 <= d2) BezierHandle.P1 else BezierHandle.P2
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val nx = toNormalizedX(change.position.x, size.width.toFloat())
                        val ny = toNormalizedY(change.position.y, size.height.toFloat())
                        val updated = when (activeHandle) {
                            BezierHandle.P1 -> currentProfile.copy(x1 = nx, y1 = ny)
                            BezierHandle.P2 -> currentProfile.copy(x2 = nx, y2 = ny)
                            null -> return@detectDragGestures
                        }
                        onProfileChange(updated)
                    },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val left = BEZIER_EDITOR_PADDING_PX
        val top = BEZIER_EDITOR_PADDING_PX
        val right = w - BEZIER_EDITOR_PADDING_PX
        val bottom = h - BEZIER_EDITOR_PADDING_PX
        val plotW = (right - left).coerceAtLeast(1f)
        val plotH = (bottom - top).coerceAtLeast(1f)
        val gridColor = onSurfaceVariant.copy(alpha = 0.15f)
        val axisColor = onSurfaceVariant.copy(alpha = 0.4f)

        // Bounding box and grid
        for (f in 1..3) {
            val x = left + plotW * f / 4f
            val y = top + plotH * f / 4f
            drawLine(gridColor, Offset(x, top), Offset(x, bottom), strokeWidth = 1f)
            drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1f)
        }
        drawRect(
            color = onSurfaceVariant.copy(alpha = 0.22f),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(plotW, plotH),
            style = Stroke(width = 1f)
        )

        // Linear reference
        drawLine(
            axisColor,
            Offset(left, bottom),
            Offset(right, top),
            strokeWidth = 1.5f
        )

        // Control polygon
        val p1 = Offset(
            x = left + profile.x1.coerceIn(0f, 1f) * plotW,
            y = (bottom - profile.y1 * plotH).coerceIn(top, bottom)
        )
        val p2 = Offset(
            x = left + profile.x2.coerceIn(0f, 1f) * plotW,
            y = (bottom - profile.y2 * plotH).coerceIn(top, bottom)
        )
        drawLine(
            primary.copy(alpha = 0.5f),
            Offset(left, bottom),
            p1,
            strokeWidth = 1.5f
        )
        drawLine(
            primary.copy(alpha = 0.5f),
            Offset(right, top),
            p2,
            strokeWidth = 1.5f
        )

        // Curve
        val path = Path()
        val samples = 64
        path.moveTo(left, bottom)
        for (i in 1..samples) {
            val t = i.toFloat() / samples
            val x = left + profile.curveX(t).coerceIn(0f, 1f) * plotW
            val y = (bottom - profile.curveY(t) * plotH).coerceIn(top, bottom)
            path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = primary,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Endpoints
        drawCircle(primary.copy(alpha = 0.85f), radius = 6f, center = Offset(left, bottom))
        drawCircle(primary.copy(alpha = 0.85f), radius = 6f, center = Offset(right, top))

        // Handles
        drawCircle(Color.White, radius = 10f, center = p1)
        drawCircle(primary, radius = 7f, center = p1)
        drawCircle(Color.White, radius = 10f, center = p2)
        drawCircle(primary, radius = 7f, center = p2)
    }
}

private fun toNormalizedX(px: Float, width: Float): Float {
    val left = BEZIER_EDITOR_PADDING_PX
    val right = width - BEZIER_EDITOR_PADDING_PX
    val plotW = (right - left).coerceAtLeast(1f)
    return ((px - left) / plotW).coerceIn(0f, 1f)
}

private fun toNormalizedY(py: Float, height: Float): Float {
    val top = BEZIER_EDITOR_PADDING_PX
    val bottom = height - BEZIER_EDITOR_PADDING_PX
    val plotH = (bottom - top).coerceAtLeast(1f)
    val yNorm = (1f - ((py - top) / plotH)).coerceIn(0f, 1f)
    return yNorm
}

/** Tiny static curve preview used in the sequencer step list. */
@Composable
fun BezierMiniPreview(
    profile: BezierProfile,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLine(
            onSurfaceVariant.copy(alpha = 0.3f),
            Offset(0f, h),
            Offset(w, 0f),
            strokeWidth = 1f
        )
        val path = Path()
        val samples = 32
        path.moveTo(0f, h)
        for (i in 1..samples) {
            val t = i.toFloat() / samples
            path.lineTo(profile.curveX(t) * w, (1f - profile.curveY(t)) * h)
        }
        drawPath(path, primary, style = Stroke(width = 2f))
    }
}

/** Numeric control-point field; commits on focus loss or keyboard done. */
@Composable
fun BezierCoordField(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(formatBezierValue(value)) }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(value, focused) {
        if (!focused) text = formatBezierValue(value)
    }

    fun commit() {
        val parsed = text.replace(',', '.').toFloatOrNull()
        if (parsed != null && parsed.isFinite()) onChange(parsed)
    }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
        modifier = modifier
            .onFocusChanged { state ->
                focused = state.isFocused
                if (!state.isFocused) commit()
            }
    )
}

private fun formatBezierValue(v: Float): String = "%.2f".format(java.util.Locale.US, v)
