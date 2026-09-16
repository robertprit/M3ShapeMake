package com.compose.canvas.geometry

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

sealed interface Geometry {
    fun boundingRect(): RectF
}

data class RectGeometry(
    val width: Float,
    val height: Float,
    val cornerRadius: Float = 0f
) : Geometry {
    override fun boundingRect() = RectF(0f, 0f, width, height)
}

data class EllipseGeometry(
    val radiusX: Float,
    val radiusY: Float
) : Geometry {
    override fun boundingRect() = RectF(-radiusX, -radiusY, radiusX, radiusY)
}

data class LineGeometry(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float
) : Geometry {
    override fun boundingRect(): RectF = RectF(
        minOf(startX, endX),
        minOf(startY, endY),
        maxOf(startX, endX),
        maxOf(startY, endY)
    )
}

data class TriangleGeometry(
    val width: Float,
    val height: Float
) : Geometry {
    override fun boundingRect() = RectF(0f, 0f, width, height)
}

data class PolygonGeometry(
    val sides: Int,
    val radius: Float
) : Geometry {
    override fun boundingRect() = RectF(-radius, -radius, radius, radius)
}

data class StarGeometry(
    val points: Int,
    val outerRadius: Float,
    val innerRadius: Float
) : Geometry {
    override fun boundingRect() = RectF(-outerRadius, -outerRadius, outerRadius, outerRadius)
}

data class ArcGeometry(
    val radiusX: Float,
    val radiusY: Float,
    val startDegrees: Float,
    val sweepDegrees: Float
) : Geometry {
    override fun boundingRect() = RectF(-radiusX, -radiusY, radiusX, radiusY)
}

data class TextGeometry(
    val text: String,
    val fontFamily: String = "sans-serif",
    val fontSize: Float = 16f,
    val fontWeight: Int = 400,
    val alignment: TextAlign = TextAlign.Left,
    val letterSpacing: Float = 0f,
    val lineHeight: Float = 1.2f
) : Geometry {
    override fun boundingRect(): RectF {
        val lines = text.split('\n').ifEmpty { listOf("") }
        val w = (lines.maxOf { it.length }.coerceAtLeast(1)) * fontSize * 0.55f
        val h = lines.size * fontSize * lineHeight
        return RectF(0f, 0f, w, h)
    }
}

enum class TextAlign { Left, Center, Right }

data class PresetGeometry(
    val presetId: String,
    val width: Float,
    val height: Float,
    val parameters: Map<String, Float> = emptyMap(),
    /**
     * Canonical geometry supplied by the preset catalogue. The preset identity and
     * parameters remain authoritative; this path makes third-party catalogues
     * renderable and portable without coupling the editor core to their runtime.
     */
    val definition: PathGeometry? = null
) : Geometry {
    override fun boundingRect() = RectF(0f, 0f, width, height)
}

data class PathGeometry(
    val segments: List<PathSegment>,
    val closed: Boolean = false
) : Geometry {
    override fun boundingRect(): RectF {
        if (segments.isEmpty()) return RectF.Empty
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        fun include(x: Float, y: Float) {
            minX = minOf(minX, x)
            minY = minOf(minY, y)
            maxX = maxOf(maxX, x)
            maxY = maxOf(maxY, y)
        }
        for (seg in segments) {
            when (seg) {
                is PathSegment.MoveTo -> include(seg.x, seg.y)
                is PathSegment.LineTo -> include(seg.x, seg.y)
                is PathSegment.QuadraticBezierTo -> {
                    include(seg.cx, seg.cy); include(seg.x, seg.y)
                }
                is PathSegment.CubicBezierTo -> {
                    include(seg.cx1, seg.cy1); include(seg.cx2, seg.cy2); include(seg.x, seg.y)
                }
                is PathSegment.Close -> Unit
            }
        }
        if (minX == Float.POSITIVE_INFINITY) return RectF.Empty
        return RectF(minX, minY, maxX, maxY)
    }
}

fun regularPolygonPoints(sides: Int, radius: Float, startDeg: Float = -90f): List<Pair<Float, Float>> {
    val n = sides.coerceAtLeast(3)
    val start = Math.toRadians(startDeg.toDouble())
    return (0 until n).map { i ->
        val a = start + i * 2.0 * Math.PI / n
        (cos(a).toFloat() * radius) to (sin(a).toFloat() * radius)
    }
}

fun starPoints(points: Int, outer: Float, inner: Float): List<Pair<Float, Float>> {
    val n = points.coerceAtLeast(3)
    val start = Math.toRadians(-90.0)
    val out = mutableListOf<Pair<Float, Float>>()
    for (i in 0 until n) {
        val a = start + i * 2.0 * Math.PI / n
        out += cos(a).toFloat() * outer to sin(a).toFloat() * outer
        val b = a + Math.PI / n
        out += cos(b).toFloat() * inner to sin(b).toFloat() * inner
    }
    return out
}

fun Geometry.paramOr(map: Map<String, Float>, key: String, fallback: Float): Float =
    map[key] ?: fallback
