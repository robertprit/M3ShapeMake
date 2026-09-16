package com.compose.canvas.tools.path

import androidx.compose.ui.geometry.Offset
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import kotlin.math.hypot

object PathSimplifier {
    fun simplify(points: List<Offset>, tolerance: Float): List<Offset> {
        if (points.size < 3) return points
        return douglasPeucker(points, tolerance.coerceAtLeast(0.01f))
    }

    private fun douglasPeucker(points: List<Offset>, epsilon: Float): List<Offset> {
        var maxDist = 0f
        var index = 0
        val end = points.lastIndex
        for (i in 1 until end) {
            val d = perpendicular(points[i], points.first(), points.last())
            if (d > maxDist) {
                index = i
                maxDist = d
            }
        }
        return if (maxDist > epsilon) {
            val left = douglasPeucker(points.subList(0, index + 1), epsilon)
            val right = douglasPeucker(points.subList(index, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(points.first(), points.last())
        }
    }

    private fun perpendicular(p: Offset, a: Offset, b: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = hypot(dx, dy)
        if (len < 1e-6f) return hypot(p.x - a.x, p.y - a.y)
        return kotlin.math.abs(dy * p.x - dx * p.y + b.x * a.y - b.y * a.x) / len
    }
}

fun pointsToPath(points: List<Offset>, closed: Boolean): PathGeometry {
    if (points.isEmpty()) return PathGeometry(emptyList())
    val segs = mutableListOf<PathSegment>(PathSegment.MoveTo(points.first().x, points.first().y))
    for (i in 1 until points.size) segs += PathSegment.LineTo(points[i].x, points[i].y)
    if (closed) segs += PathSegment.Close
    return PathGeometry(segs, closed)
}
