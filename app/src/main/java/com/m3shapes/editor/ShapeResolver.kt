package com.m3shapes.editor

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.graphics.shapes.transformed
import java.util.Locale
import kotlin.math.max

private const val VIEWPORT = 256f
private const val VIEWPORT_MARGIN = 18f

fun compilePolygon(c: FormConfig): RoundedPolygon {
    val rounding = CornerRounding(radius = c.rounding * 55f, smoothing = c.smoothing)
    return RoundedPolygon(
        numVertices = c.numVertices,
        radius = 110f,
        centerX = 128f,
        centerY = 128f,
        rounding = rounding
    )
}

fun compileDrawnPolygon(c: DrawnPolygonConfig): RoundedPolygon {
    require(c.vertices.size >= 3) {
        "Drawn polygon needs at least 3 vertices"
    }
    val centerX = c.vertices.map { it.x }.average().toFloat()
    val centerY = c.vertices.map { it.y }.average().toFloat()
    val perVertex = c.vertices.mapIndexed { index, _ ->
        val corner = when {
            c.perVertexRounding.size == c.vertices.size -> c.perVertexRounding[index]
            else -> c.perVertexRounding.getOrNull(index)
        }
        CornerRounding(
            radius = (corner?.rounding ?: c.rounding).coerceIn(0f, 1f) * 55f,
            smoothing = (corner?.smoothing ?: c.smoothing).coerceIn(0f, 1f)
        )
    }
    return RoundedPolygon(
        vertices = FloatArray(c.vertices.size * 2) { index ->
            val vertex = c.vertices[index / 2]
            if (index % 2 == 0) vertex.x else vertex.y
        },
        perVertexRounding = perVertex,
        centerX = centerX,
        centerY = centerY
    )
}

fun resolveShape(source: ShapeSource): RoundedPolygon = when (source) {
    is ShapeSource.Material -> source.shapeId.toRoundedPolygon().fitToViewport()
    is ShapeSource.Custom -> compilePolygon(source.config)
    is ShapeSource.Drawn -> compileDrawnPolygon(source.config).fitToViewport()
}

fun RoundedPolygon.fitToViewport(
    viewport: Float = VIEWPORT,
    margin: Float = VIEWPORT_MARGIN
): RoundedPolygon {
    val bounds = computeBounds()
    val maxDim = max(bounds.width(), bounds.height()).coerceAtLeast(0.001f)
    val scale = (viewport - margin * 2f) / maxDim
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
    val rect = RectF()
    path.computeBounds(rect, true)
    return rect
}

fun RoundedPolygon.toVpPathData(): String = buildString {
    var first = true
    for (cubic in cubics) {
        if (first) {
            append("M ${cubic.anchor0X.f2()} ${cubic.anchor0Y.f2()} ")
            first = false
        }
        append(
            "C ${cubic.control0X.f2()} ${cubic.control0Y.f2()} " +
                "${cubic.control1X.f2()} ${cubic.control1Y.f2()} " +
                "${cubic.anchor1X.f2()} ${cubic.anchor1Y.f2()} "
        )
    }
    append("Z")
}

fun shapeComposeReference(source: ShapeSource): String = when (source) {
    is ShapeSource.Material -> source.shapeId.composeRef
    is ShapeSource.Custom -> {
        val c = source.config
        "RoundedPolygon(numVertices = ${c.numVertices}, radius = 110f, centerX = 128f, centerY = 128f, " +
            "rounding = CornerRounding(radius = ${"%.2f".format(Locale.US, c.rounding * 55f)}f, " +
            "smoothing = ${"%.2f".format(Locale.US, c.smoothing)}f))"
    }
    is ShapeSource.Drawn -> drawnPolygonComposeReference(source.config)
}

fun drawnPolygonComposeReference(config: DrawnPolygonConfig): String {
    val vertArray = config.vertices.joinToString(", ") { v ->
        "${"%.1f".format(Locale.US, v.x)}f, ${"%.1f".format(Locale.US, v.y)}f"
    }
    val cx = config.vertices.map { it.x }.average()
    val cy = config.vertices.map { it.y }.average()
    val corners = config.vertices.mapIndexed { index, _ ->
        val corner = when {
            config.perVertexRounding.size == config.vertices.size -> config.perVertexRounding[index]
            else -> config.perVertexRounding.getOrNull(index)
        }
        val radius = (corner?.rounding ?: config.rounding) * 55f
        val smoothing = corner?.smoothing ?: config.smoothing
        "CornerRounding(radius = ${"%.2f".format(Locale.US, radius)}f, smoothing = ${"%.2f".format(Locale.US, smoothing)}f)"
    }
    // TODO: Include per-vertex locked flags in Compose RoundedPolygon export when importers need them.
    return buildString {
        append("RoundedPolygon(\n")
        append("        vertices = floatArrayOf($vertArray),\n")
        append("        perVertexRounding = listOf(\n")
        corners.forEachIndexed { i, c ->
            append("            $c")
            if (i < corners.lastIndex) append(",")
            append("\n")
        }
        append("        ),\n")
        append("        centerX = ${"%.1f".format(Locale.US, cx)}f,\n")
        append("        centerY = ${"%.1f".format(Locale.US, cy)}f\n")
        append("    )")
    }
}

fun shapeLabel(source: ShapeSource): String = when (source) {
    is ShapeSource.Material -> source.shapeId.label
    is ShapeSource.Custom -> "Custom ${source.config.numVertices}-Eck"
    is ShapeSource.Drawn -> "Gezeichnet ${source.config.vertices.size}-Eck"
}

private fun Float.f2(): String = String.format(Locale.US, "%.2f", this)
