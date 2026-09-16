package com.compose.canvas.geometry

import kotlin.math.cos
import kotlin.math.sin

object GeometryToPath {
    fun toPath(geometry: Geometry, parameters: Map<String, Float> = emptyMap()): PathGeometry {
        if (geometry is PathGeometry) return geometry
        val merged = if (geometry is PresetGeometry) geometry.parameters + parameters else parameters
        val segments = when (geometry) {
            is PathGeometry -> geometry.segments
            is RectGeometry -> rect(geometry, merged)
            is EllipseGeometry -> ellipse(geometry)
            is LineGeometry -> listOf(
                PathSegment.MoveTo(geometry.startX, geometry.startY),
                PathSegment.LineTo(geometry.endX, geometry.endY)
            )
            is TriangleGeometry -> {
                val w = merged["width"] ?: geometry.width
                val h = merged["height"] ?: geometry.height
                listOf(
                    PathSegment.MoveTo(0f, h),
                    PathSegment.LineTo(w / 2f, 0f),
                    PathSegment.LineTo(w, h),
                    PathSegment.Close
                )
            }
            is PolygonGeometry -> polygon(geometry.sides, merged["radius"] ?: geometry.radius)
            is StarGeometry -> star(
                geometry.points,
                merged["outerRadius"] ?: geometry.outerRadius,
                merged["innerRadius"] ?: geometry.innerRadius
            )
            is ArcGeometry -> arc(geometry)
            is TextGeometry -> rect(
                RectGeometry(geometry.boundingRect().width, geometry.boundingRect().height),
                emptyMap()
            )
            is PresetGeometry -> geometry.definition?.segments ?: preset(geometry.copy(parameters = merged))
        }
        val closed = geometry !is LineGeometry && geometry !is ArcGeometry && geometry !is TextGeometry
        return PathGeometry(segments, closed = closed)
    }

    private fun rect(g: RectGeometry, params: Map<String, Float>): List<PathSegment> {
        val w = params["width"] ?: g.width
        val h = params["height"] ?: g.height
        val r = (params["cornerRadius"] ?: g.cornerRadius).coerceAtMost(minOf(w, h) / 2f)
        if (r <= 0f) {
            return listOf(
                PathSegment.MoveTo(0f, 0f),
                PathSegment.LineTo(w, 0f),
                PathSegment.LineTo(w, h),
                PathSegment.LineTo(0f, h),
                PathSegment.Close
            )
        }
        val k = 0.5522847498f * r
        return listOf(
            PathSegment.MoveTo(r, 0f),
            PathSegment.LineTo(w - r, 0f),
            PathSegment.CubicBezierTo(w - r + k, 0f, w, r - k, w, r),
            PathSegment.LineTo(w, h - r),
            PathSegment.CubicBezierTo(w, h - r + k, w - r + k, h, w - r, h),
            PathSegment.LineTo(r, h),
            PathSegment.CubicBezierTo(r - k, h, 0f, h - r + k, 0f, h - r),
            PathSegment.LineTo(0f, r),
            PathSegment.CubicBezierTo(0f, r - k, r - k, 0f, r, 0f),
            PathSegment.Close
        )
    }

    private fun ellipse(g: EllipseGeometry): List<PathSegment> {
        val rx = g.radiusX
        val ry = g.radiusY
        val kx = 0.5522847498f * rx
        val ky = 0.5522847498f * ry
        return listOf(
            PathSegment.MoveTo(rx, 0f),
            PathSegment.CubicBezierTo(rx, ky, kx, ry, 0f, ry),
            PathSegment.CubicBezierTo(-kx, ry, -rx, ky, -rx, 0f),
            PathSegment.CubicBezierTo(-rx, -ky, -kx, -ry, 0f, -ry),
            PathSegment.CubicBezierTo(kx, -ry, rx, -ky, rx, 0f),
            PathSegment.Close
        )
    }

    private fun polygon(sides: Int, radius: Float): List<PathSegment> {
        val pts = regularPolygonPoints(sides, radius)
        if (pts.isEmpty()) return emptyList()
        return buildList {
            add(PathSegment.MoveTo(pts.first().first, pts.first().second))
            pts.drop(1).forEach { add(PathSegment.LineTo(it.first, it.second)) }
            add(PathSegment.Close)
        }
    }

    private fun star(points: Int, outer: Float, inner: Float): List<PathSegment> {
        val pts = starPoints(points, outer, inner)
        if (pts.isEmpty()) return emptyList()
        return buildList {
            add(PathSegment.MoveTo(pts.first().first, pts.first().second))
            pts.drop(1).forEach { add(PathSegment.LineTo(it.first, it.second)) }
            add(PathSegment.Close)
        }
    }

    private fun arc(g: ArcGeometry): List<PathSegment> {
        val steps = 16
        val start = Math.toRadians(g.startDegrees.toDouble())
        val sweep = Math.toRadians(g.sweepDegrees.toDouble())
        return buildList {
            for (i in 0..steps) {
                val t = i / steps.toFloat()
                val a = start + sweep * t
                val x = cos(a).toFloat() * g.radiusX
                val y = sin(a).toFloat() * g.radiusY
                add(if (i == 0) PathSegment.MoveTo(x, y) else PathSegment.LineTo(x, y))
            }
        }
    }

    private fun preset(g: PresetGeometry): List<PathSegment> {
        val w = g.width
        val h = g.height
        val p = g.parameters
        return when (g.presetId) {
            "pill" -> rect(RectGeometry(w, h, h / 2f), p)
            "arch" -> {
                val r = w / 2f
                listOf(
                    PathSegment.MoveTo(0f, h),
                    PathSegment.LineTo(0f, r),
                    PathSegment.CubicBezierTo(0f, r * 0.45f, r * 0.45f, 0f, r, 0f),
                    PathSegment.CubicBezierTo(r + r * 0.55f, 0f, w, r * 0.45f, w, r),
                    PathSegment.LineTo(w, h),
                    PathSegment.Close
                )
            }
            "cookie" -> {
                val sides = (p["sides"] ?: 8f).toInt().coerceAtLeast(4)
                val radius = minOf(w, h) / 2f
                val inner = radius * (p["innerRatio"] ?: 0.82f)
                star(sides, radius, inner).map { seg -> offset(seg, w / 2f, h / 2f) }
            }
            "logicBlock", "notchBlock" -> logicBlock(w, h, p)
            "fan" -> {
                val r = minOf(w, h)
                listOf(
                    PathSegment.MoveTo(0f, h),
                    PathSegment.LineTo(0f, 0f),
                    PathSegment.CubicBezierTo(r * 0.55f, 0f, r, h - r * 0.55f, r, h),
                    PathSegment.Close
                )
            }
            "slanted" -> {
                val s = p["skew"] ?: (w * 0.15f)
                listOf(
                    PathSegment.MoveTo(s, 0f),
                    PathSegment.LineTo(w, 0f),
                    PathSegment.LineTo(w - s, h),
                    PathSegment.LineTo(0f, h),
                    PathSegment.Close
                )
            }
            else -> rect(RectGeometry(w, h, p["cornerRadius"] ?: 12f), p)
        }
    }

    private fun logicBlock(w: Float, h: Float, p: Map<String, Float>): List<PathSegment> {
        val r = p["cornerRadius"] ?: 12f
        val notchW = p["notchWidth"] ?: 18f
        val notchD = p["notchDepth"] ?: 6f
        val connW = p["connectorWidth"] ?: 16f
        val connD = p["connectorDepth"] ?: 8f
        val midX = w / 2f
        return listOf(
            PathSegment.MoveTo(r, 0f),
            PathSegment.LineTo(midX - notchW / 2f, 0f),
            PathSegment.LineTo(midX - notchW / 4f, notchD),
            PathSegment.LineTo(midX + notchW / 4f, notchD),
            PathSegment.LineTo(midX + notchW / 2f, 0f),
            PathSegment.LineTo(w - r, 0f),
            PathSegment.CubicBezierTo(w, 0f, w, 0f, w, r),
            PathSegment.LineTo(w, h - r),
            PathSegment.CubicBezierTo(w, h, w, h, w - r, h),
            PathSegment.LineTo(midX + connW / 2f, h),
            PathSegment.LineTo(midX, h + connD),
            PathSegment.LineTo(midX - connW / 2f, h),
            PathSegment.LineTo(r, h),
            PathSegment.CubicBezierTo(0f, h, 0f, h, 0f, h - r),
            PathSegment.LineTo(0f, r),
            PathSegment.CubicBezierTo(0f, 0f, 0f, 0f, r, 0f),
            PathSegment.Close
        )
    }

    private fun offset(seg: PathSegment, dx: Float, dy: Float): PathSegment = when (seg) {
        is PathSegment.MoveTo -> PathSegment.MoveTo(seg.x + dx, seg.y + dy)
        is PathSegment.LineTo -> PathSegment.LineTo(seg.x + dx, seg.y + dy)
        is PathSegment.QuadraticBezierTo ->
            PathSegment.QuadraticBezierTo(seg.cx + dx, seg.cy + dy, seg.x + dx, seg.y + dy)
        is PathSegment.CubicBezierTo -> PathSegment.CubicBezierTo(
            seg.cx1 + dx, seg.cy1 + dy, seg.cx2 + dx, seg.cy2 + dy, seg.x + dx, seg.y + dy
        )
        PathSegment.Close -> PathSegment.Close
    }
}
