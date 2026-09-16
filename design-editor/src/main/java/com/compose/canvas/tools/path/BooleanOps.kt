package com.compose.canvas.tools.path

import androidx.compose.ui.geometry.Offset
import com.compose.canvas.geometry.GeometryToPath
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import com.compose.canvas.core.VectorObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class BooleanOp { Union, Difference, Intersection, Xor }

object BooleanOps {
    fun objectToWorldPolygon(obj: VectorObject, world: (Offset) -> Offset): List<Offset> {
        val path = GeometryToPath.toPath(obj.geometry, obj.semantics.parameters)
        return flatten(path).map { world(it) }
    }

    fun flatten(path: PathGeometry, steps: Int = 12): List<Offset> {
        val pts = mutableListOf<Offset>()
        var last = Offset.Zero
        var start = Offset.Zero
        for (seg in path.segments) {
            when (seg) {
                is PathSegment.MoveTo -> {
                    last = Offset(seg.x, seg.y)
                    start = last
                    pts += last
                }
                is PathSegment.LineTo -> {
                    last = Offset(seg.x, seg.y)
                    pts += last
                }
                is PathSegment.QuadraticBezierTo -> {
                    for (i in 1..steps) {
                        val t = i / steps.toFloat()
                        pts += quad(last, Offset(seg.cx, seg.cy), Offset(seg.x, seg.y), t)
                    }
                    last = Offset(seg.x, seg.y)
                }
                is PathSegment.CubicBezierTo -> {
                    for (i in 1..steps) {
                        val t = i / steps.toFloat()
                        pts += cubic(
                            last,
                            Offset(seg.cx1, seg.cy1),
                            Offset(seg.cx2, seg.cy2),
                            Offset(seg.x, seg.y),
                            t
                        )
                    }
                    last = Offset(seg.x, seg.y)
                }
                PathSegment.Close -> {
                    if (pts.isEmpty() || pts.last() != start) pts += start
                    last = start
                }
            }
        }
        return pts.distinctBy { "${it.x.toInt()}:${it.y.toInt()}" }.let {
            if (it.size >= 2 && hypot(it.first().x - it.last().x, it.first().y - it.last().y) < 0.5f) {
                it.dropLast(1)
            } else it
        }
    }

    fun apply(a: List<Offset>, b: List<Offset>, op: BooleanOp): List<Offset> {
        if (a.size < 3) return b
        if (b.size < 3) return a
        return when (op) {
            BooleanOp.Intersection -> clipConvex(a, b)
            BooleanOp.Difference -> differenceConvex(a, b)
            BooleanOp.Union -> unionConvex(a, b)
            BooleanOp.Xor -> {
                val ab = differenceConvex(a, b)
                val ba = differenceConvex(b, a)
                if (ba.isEmpty()) ab else unionConvex(ab.ifEmpty { a }, ba)
            }
        }
    }

    fun loopsToPath(loops: List<List<Offset>>): PathGeometry {
        val segs = mutableListOf<PathSegment>()
        for (loop in loops) {
            if (loop.size < 2) continue
            segs += PathSegment.MoveTo(loop.first().x, loop.first().y)
            loop.drop(1).forEach { segs += PathSegment.LineTo(it.x, it.y) }
            segs += PathSegment.Close
        }
        return PathGeometry(segs, closed = true)
    }

    fun pointInPolygon(p: Offset, poly: List<Offset>): Boolean {
        var inside = false
        var j = poly.lastIndex
        for (i in poly.indices) {
            val pi = poly[i]
            val pj = poly[j]
            if ((pi.y > p.y) != (pj.y > p.y)) {
                val xint = (pj.x - pi.x) * (p.y - pi.y) / ((pj.y - pi.y).let { if (it == 0f) 1e-6f else it }) + pi.x
                if (p.x < xint) inside = !inside
            }
            j = i
        }
        return inside
    }

    private fun unionConvex(a: List<Offset>, b: List<Offset>): List<Offset> {
        val pts = mutableListOf<Offset>()
        pts += a.filter { !pointInPolygon(it, b) }
        pts += b.filter { !pointInPolygon(it, a) }
        pts += intersections(a, b)
        if (pts.size < 3) return a
        val c = Offset(pts.map { it.x }.average().toFloat(), pts.map { it.y }.average().toFloat())
        return pts.distinctBy { "${"%.2f".format(it.x)}:${"%.2f".format(it.y)}" }
            .sortedBy { atan2((it.y - c.y).toDouble(), (it.x - c.x).toDouble()) }
    }

    private fun differenceConvex(a: List<Offset>, b: List<Offset>): List<Offset> {
        val clipped = clipConvex(a, b)
        if (clipped.isEmpty()) return a.filter { !pointInPolygon(it, b) }.ifEmpty { a }
        val outer = a.filter { !pointInPolygon(it, b) } + intersections(a, b)
        if (outer.size < 3) return emptyList()
        val c = Offset(outer.map { it.x }.average().toFloat(), outer.map { it.y }.average().toFloat())
        return outer.distinctBy { "${"%.2f".format(it.x)}:${"%.2f".format(it.y)}" }
            .sortedBy { atan2((it.y - c.y).toDouble(), (it.x - c.x).toDouble()) }
    }

    /** Sutherland–Hodgman, treating [clip] as convex. */
    fun clipConvex(subject: List<Offset>, clip: List<Offset>): List<Offset> {
        if (subject.size < 3 || clip.size < 3) return emptyList()
        var output = subject
        for (i in clip.indices) {
            val a = clip[i]
            val b = clip[(i + 1) % clip.size]
            val input = output
            output = mutableListOf()
            if (input.isEmpty()) break
            var prev = input.last()
            for (cur in input) {
                val curIn = inside(cur, a, b)
                val prevIn = inside(prev, a, b)
                if (curIn) {
                    if (!prevIn) intersect(prev, cur, a, b)?.let { output.add(it) }
                    output.add(cur)
                } else if (prevIn) {
                    intersect(prev, cur, a, b)?.let { output.add(it) }
                }
                prev = cur
            }
        }
        return output
    }

    private fun inside(p: Offset, a: Offset, b: Offset): Boolean =
        (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x) >= 0f

    private fun intersect(p1: Offset, p2: Offset, a: Offset, b: Offset): Offset? {
        val d1x = p2.x - p1.x
        val d1y = p2.y - p1.y
        val d2x = b.x - a.x
        val d2y = b.y - a.y
        val den = d1x * d2y - d1y * d2x
        if (kotlin.math.abs(den) < 1e-8f) return null
        val t = ((a.x - p1.x) * d2y - (a.y - p1.y) * d2x) / den
        return Offset(p1.x + t * d1x, p1.y + t * d1y)
    }

    private fun intersections(a: List<Offset>, b: List<Offset>): List<Offset> {
        val out = mutableListOf<Offset>()
        for (i in a.indices) {
            val a1 = a[i]
            val a2 = a[(i + 1) % a.size]
            for (j in b.indices) {
                val b1 = b[j]
                val b2 = b[(j + 1) % b.size]
                segmentIntersect(a1, a2, b1, b2)?.let { out += it }
            }
        }
        return out
    }

    private fun segmentIntersect(p1: Offset, p2: Offset, q1: Offset, q2: Offset): Offset? {
        val den = (p1.x - p2.x) * (q1.y - q2.y) - (p1.y - p2.y) * (q1.x - q2.x)
        if (kotlin.math.abs(den) < 1e-8f) return null
        val t = ((p1.x - q1.x) * (q1.y - q2.y) - (p1.y - q1.y) * (q1.x - q2.x)) / den
        val u = -((p1.x - p2.x) * (p1.y - q1.y) - (p1.y - p2.y) * (p1.x - q1.x)) / den
        if (t in 0f..1f && u in 0f..1f) {
            return Offset(p1.x + t * (p2.x - p1.x), p1.y + t * (p2.y - p1.y))
        }
        return null
    }

    private fun quad(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
        val u = 1f - t
        return Offset(
            u * u * p0.x + 2 * u * t * p1.x + t * t * p2.x,
            u * u * p0.y + 2 * u * t * p1.y + t * t * p2.y
        )
    }

    private fun cubic(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
        val u = 1f - t
        return Offset(
            u * u * u * p0.x + 3 * u * u * t * p1.x + 3 * u * t * t * p2.x + t * t * t * p3.x,
            u * u * u * p0.y + 3 * u * u * t * p1.y + 3 * u * t * t * p2.y + t * t * t * p3.y
        )
    }
}
