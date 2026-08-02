package com.m3shapes.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Color

internal val studioColors = listOf(
    Color(0xFF6750A4), Color(0xFF1E88E5), Color(0xFF00897B),
    Color(0xFF43A047), Color(0xFFFFB300), Color(0xFFE53935),
    Color(0xFFD81B60), Color(0xFF212121)
)

/** Cubic-Bézier easing from the animation profile. x is time, clamped to 0..1. */
internal fun BezierProfile.toEasing(): Easing = CubicBezierEasing(
    a = x1.coerceIn(0f, 1f),
    b = y1,
    c = x2.coerceIn(0f, 1f),
    d = y2
)

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
