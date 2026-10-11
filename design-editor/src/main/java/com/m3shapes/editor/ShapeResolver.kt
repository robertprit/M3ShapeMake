package com.m3shapes.editor

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.graphics.shapes.transformed
import kotlin.math.max

private const val VIEWPORT = 256f
private const val VIEWPORT_MARGIN = 18f

fun RoundedPolygon.fitToViewport(
    viewport: Float = VIEWPORT,
    margin: Float = VIEWPORT_MARGIN
): RoundedPolygon {
    val bounds = computeBounds()
    val maxDimension = max(bounds.width(), bounds.height()).coerceAtLeast(0.001f)
    val scale = (viewport - margin * 2f) / maxDimension
    val translateX = viewport / 2f - (bounds.left + bounds.width() / 2f) * scale
    val translateY = viewport / 2f - (bounds.top + bounds.height() / 2f) * scale
    val matrix = Matrix().apply {
        setScale(scale, scale)
        postTranslate(translateX, translateY)
    }
    return transformed(matrix)
}

private fun RoundedPolygon.computeBounds(): RectF {
    val path = Path()
    toPath(path)
    return RectF().also { path.computeBounds(it, true) }
}
