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
private const val BEZIER_EDITOR_PADDING_PX = 30f
private const val CURVE_Y_MIN = -1.2f
private const val CURVE_Y_MAX = 2.2f

/**
 * Editable cubic-Bézier easing curve. The box maps time (x) and progress (y) to 0..1;
 * control handles P1/P2 can be dragged, y-values may overshoot for bounce/anticipation.
 */
@Composable
fun BezierCurveEditor(
    profile: BezierProfile,
    onProfileChange: (BezierProfile) -> Unit,
    motionPathType: MotionPathType = MotionPathType.Tween,
    springStiffness: Float = 400f,
    springDamping: Float = 0.75f,
    bounceGravity: Float = 1f,
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
                        if (motionPathType != MotionPathType.Tween) return@detectDragGestures
                        val nx = toNormalizedX(pos.x, size.width.toFloat())
                        val ny = toNormalizedY(pos.y, size.height.toFloat())
                        val d1 = kotlin.math.abs(nx - currentProfile.x1) + kotlin.math.abs(ny - currentProfile.y1)
                        val d2 = kotlin.math.abs(nx - currentProfile.x2) + kotlin.math.abs(ny - currentProfile.y2)
                        activeHandle = if (d1 <= d2) BezierHandle.P1 else BezierHandle.P2
                    },
                    onDrag = { change, _ ->
                        if (motionPathType != MotionPathType.Tween) return@detectDragGestures
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
        val yZero = mapY(0f, top, plotH)
        val yOne = mapY(1f, top, plotH)

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
            Offset(left, yZero),
            Offset(right, yOne),
            strokeWidth = 1.5f
        )

        // Control polygon (Tween only)
        val p1 = Offset(
            x = left + profile.x1.coerceIn(0f, 1f) * plotW,
            y = mapY(profile.y1, top, plotH)
        )
        val p2 = Offset(
            x = left + profile.x2.coerceIn(0f, 1f) * plotW,
            y = mapY(profile.y2, top, plotH)
        )
        if (motionPathType == MotionPathType.Tween) {
            drawLine(
                primary.copy(alpha = 0.5f),
                Offset(left, yZero),
                p1,
                strokeWidth = 1.5f
            )
            drawLine(
                primary.copy(alpha = 0.5f),
                Offset(right, yOne),
                p2,
                strokeWidth = 1.5f
            )
        }

        // Curve
        val path = Path()
        val samples = 64
        path.moveTo(left, mapY(0f, top, plotH))
        for (i in 1..samples) {
            val t = i.toFloat() / samples
            val yValue = when (motionPathType) {
                MotionPathType.Tween -> profile.curveY(t)
                MotionPathType.Spring -> {
                    val damping = springDamping.coerceIn(0.05f, 2f)
                    val stiff = springStiffness.coerceIn(50f, 2_000f)
                    val frequency = (stiff / 450f).coerceIn(0.25f, 5f)
                    val decay = (damping * 3.2f).coerceIn(0.2f, 6f)
                    1f - (kotlin.math.exp((-decay * t).toDouble()).toFloat() *
                        kotlin.math.cos(t * frequency * Math.PI.toFloat() * 2f))
                }
                MotionPathType.Bounce -> gravityBounceEasing(bounceGravity).transform(t)
            }
            val x = left + t * plotW
            val y = mapY(yValue, top, plotH)
            path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = primary,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        // Endpoints
        drawCircle(primary.copy(alpha = 0.85f), radius = 6f, center = Offset(left, yZero))
        drawCircle(primary.copy(alpha = 0.85f), radius = 6f, center = Offset(right, yOne))

        // Handles (Tween only)
        if (motionPathType == MotionPathType.Tween) {
            drawCircle(Color.White, radius = 10f, center = p1)
            drawCircle(primary, radius = 7f, center = p1)
            drawCircle(Color.White, radius = 10f, center = p2)
            drawCircle(primary, radius = 7f, center = p2)
        }
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
    return (CURVE_Y_MAX - (((py - top) / plotH) * (CURVE_Y_MAX - CURVE_Y_MIN)))
        .coerceIn(CURVE_Y_MIN, CURVE_Y_MAX)
}

/** Tiny static curve preview used in the sequencer step list. */
@Composable
fun BezierMiniPreview(
    profile: BezierProfile,
    motionPathType: MotionPathType = MotionPathType.Tween,
    springStiffness: Float = 400f,
    springDamping: Float = 0.75f,
    bounceGravity: Float = 1f,
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
        path.moveTo(0f, miniMapY(0f, h))
        for (i in 1..samples) {
            val t = i.toFloat() / samples
            val yValue = when (motionPathType) {
                MotionPathType.Tween -> profile.curveY(t)
                MotionPathType.Spring -> {
                    val damping = springDamping.coerceIn(0.05f, 2f)
                    val stiff = springStiffness.coerceIn(50f, 2_000f)
                    val frequency = (stiff / 450f).coerceIn(0.25f, 5f)
                    val decay = (damping * 3.2f).coerceIn(0.2f, 6f)
                    1f - (kotlin.math.exp((-decay * t).toDouble()).toFloat() *
                        kotlin.math.cos(t * frequency * Math.PI.toFloat() * 2f))
                }
                MotionPathType.Bounce -> gravityBounceEasing(bounceGravity).transform(t)
            }
            path.lineTo(t * w, miniMapY(yValue, h))
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

private fun mapY(value: Float, top: Float, plotH: Float): Float {
    val norm = (CURVE_Y_MAX - value) / (CURVE_Y_MAX - CURVE_Y_MIN)
    return top + norm * plotH
}

private fun miniMapY(value: Float, height: Float): Float {
    val norm = (CURVE_Y_MAX - value) / (CURVE_Y_MAX - CURVE_Y_MIN)
    return norm * height
}
