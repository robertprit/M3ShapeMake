package com.m3shapes.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.graphics.Color

internal val studioColors = listOf(
    Color(0xFF6750A4), Color(0xFF1E88E5), Color(0xFF00897B),
    Color(0xFF43A047), Color(0xFFFFB300), Color(0xFFE53935),
    Color(0xFFD81B60), Color(0xFF212121)
)

/** Higher stiffness / lower mass → shorter estimated motion time. */
internal fun estimateDurationMs(stiffness: Float, mass: Float = 1f): Int {
    val effective = (stiffness.coerceIn(50f, 10_000f) / mass.coerceIn(0.1f, 10f))
        .coerceIn(5f, 10_000f)
    return (120_000f / effective).toInt().coerceIn(200, 4_000)
}

/** Compose spring has no mass; map mass into effective stiffness (k/m). */
internal fun springStiffnessForMass(stiffness: Float, mass: Float): Float =
    (stiffness.coerceIn(50f, 10_000f) / mass.coerceIn(0.1f, 10f)).coerceIn(5f, 10_000f)

private val AnticipateEasing: Easing = Easing { t ->
    val tension = 2f
    t * t * ((tension + 1f) * t - tension)
}

private val OvershootEasing: Easing = Easing { t ->
    val tension = 2f
    val t1 = t - 1f
    t1 * t1 * ((tension + 1f) * t1 + tension) + 1f
}

internal fun StudioEasing.toEasing(): Easing = when (this) {
    StudioEasing.Linear -> LinearEasing
    StudioEasing.EaseIn -> FastOutLinearInEasing
    StudioEasing.EaseOut -> LinearOutSlowInEasing
    StudioEasing.EaseInOut -> FastOutSlowInEasing
    StudioEasing.Anticipate -> AnticipateEasing
    StudioEasing.Overshoot -> OvershootEasing
}

internal fun copyToClipboard(context: Context, label: String, text: String, toast: String) {
    val mgr = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    mgr.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
}

internal fun sourceForMode(
    mode: ShapeMode,
    material: ShapeSource,
    custom: FormConfig,
    drawn: DrawnPolygonConfig
): ShapeSource = when (mode) {
    ShapeMode.Material -> material
    ShapeMode.Parametric -> ShapeSource.Custom(custom)
    ShapeMode.Editor -> ShapeSource.Drawn(drawn)
}
