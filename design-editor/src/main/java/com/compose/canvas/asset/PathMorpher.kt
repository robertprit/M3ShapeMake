package com.compose.canvas.asset

import androidx.compose.ui.graphics.Color
import com.compose.canvas.core.ObjectTransform
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.geometry.GeometryToPath
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import kotlin.math.cos
import kotlin.math.sin

data class PreparedPathMorph(
    val start: PathGeometry,
    val end: PathGeometry,
    val mid: PathGeometry? = null
) {
    fun pathAt(progress: Float, stagger: Float = 0f): PathGeometry {
        val t = progress.coerceIn(0f, 1f)
        val amount = stagger.coerceIn(0f, 0.95f)
        val midPath = mid
        return if (midPath == null) {
            interpolatePaths(start, end, t, amount)
        } else if (t <= 0.5f) {
            interpolatePaths(start, midPath, t * 2f, amount)
        } else {
            interpolatePaths(midPath, end, (t - 0.5f) * 2f, amount)
        }
    }
}

data class PathMorphPreparation(
    val morph: PreparedPathMorph?,
    val diagnostic: String?
) {
    val compatible: Boolean get() = morph != null
}

object PathMorpher {
    fun prepare(startDocument: VectorDocument?, endDocument: VectorDocument?): PathMorphPreparation =
        prepare(startDocument, null, endDocument)

    fun prepare(
        startDocument: VectorDocument?,
        midDocument: VectorDocument?,
        endDocument: VectorDocument?
    ): PathMorphPreparation {
        if (startDocument == null || endDocument == null) {
            return PathMorphPreparation(null, "Start- und Endzustand müssen gespeichert sein.")
        }
        val start = singlePath(startDocument)
            ?: return PathMorphPreparation(null, "Der Startzustand benötigt genau ein sichtbares Pfadobjekt.")
        val end = singlePath(endDocument)
            ?: return PathMorphPreparation(null, "Der Endzustand benötigt genau ein sichtbares Pfadobjekt.")
        val mid = midDocument?.let { singlePath(it) }
        if (midDocument != null && mid == null && midDocument.allObjects().isNotEmpty()) {
            return PathMorphPreparation(null, "Der Mittelzustand benötigt genau ein sichtbares Pfadobjekt.")
        }
        val normalizedStart = normalize(start)
        val normalizedEnd = normalize(end)
        val normalizedMid = mid?.let(::normalize)
        if (normalizedMid == null) {
            if (sameTopology(normalizedStart, normalizedEnd)) {
                return PathMorphPreparation(PreparedPathMorph(normalizedStart, normalizedEnd), null)
            }
            val (alignedStart, alignedEnd) = resampleToCommonTopology(normalizedStart, normalizedEnd)
            return PathMorphPreparation(PreparedPathMorph(alignedStart, alignedEnd), null)
        }
        if (sameTopology(normalizedStart, normalizedMid) && sameTopology(normalizedMid, normalizedEnd)) {
            return PathMorphPreparation(PreparedPathMorph(normalizedStart, normalizedEnd, normalizedMid), null)
        }
        val (alignedStart, alignedMid, alignedEnd) = resampleToCommonTopology(
            normalizedStart,
            normalizedMid,
            normalizedEnd
        )
        return PathMorphPreparation(PreparedPathMorph(alignedStart, alignedEnd, alignedMid), null)
    }

    fun previewPath(document: VectorDocument?): PathGeometry? {
        val path = singlePath(document ?: return null) ?: return null
        return normalize(path)
    }

    fun previewFill(document: VectorDocument?): Color {
        val obj = document?.allObjects()?.firstOrNull() ?: return Color(0xFF6750A4)
        val color = obj.fill.color
        return if (color == Color.Unspecified) Color(0xFF6750A4) else color.copy(alpha = obj.fill.alpha)
    }

    private fun singlePath(document: VectorDocument): PathGeometry? {
        val objects = document.allObjects()
        if (objects.size != 1) return null
        val objectNode = objects.single()
        val path = GeometryToPath.toPath(objectNode.geometry, objectNode.semantics.parameters)
        return transform(path, objectNode.transform)
    }

    private fun transform(path: PathGeometry, transform: ObjectTransform): PathGeometry {
        val radians = Math.toRadians(transform.rotation.toDouble())
        val c = cos(radians).toFloat()
        val s = sin(radians).toFloat()
        fun point(x: Float, y: Float): Pair<Float, Float> {
            val sx = x * transform.scaleX
            val sy = y * transform.scaleY
            return (sx * c - sy * s + transform.x) to (sx * s + sy * c + transform.y)
        }
        return PathGeometry(path.segments.map { segment -> segment.mapPoints(::point) }, path.closed)
    }

    private fun normalize(path: PathGeometry): PathGeometry {
        val bounds = path.boundingRect()
        val maxSize = maxOf(bounds.width, bounds.height).coerceAtLeast(0.001f)
        val scale = 220f / maxSize
        val dx = 128f - bounds.centerX * scale
        val dy = 128f - bounds.centerY * scale
        fun point(x: Float, y: Float) = (x * scale + dx) to (y * scale + dy)
        return PathGeometry(path.segments.map { it.mapPoints(::point) }, path.closed)
    }
}

private const val MORPH_SAMPLE_COUNT = 72

private fun sameTopology(start: PathGeometry, end: PathGeometry): Boolean =
    start.closed == end.closed &&
        start.segments.size == end.segments.size &&
        start.segments.zip(end.segments).all { (a, b) -> a::class == b::class }

private data class MorphPoint(val x: Float, val y: Float)

private fun resampleToCommonTopology(
    start: PathGeometry,
    end: PathGeometry
): Pair<PathGeometry, PathGeometry> {
    val closed = start.closed || end.closed
    val startPts = resamplePolyline(flattenPath(start), MORPH_SAMPLE_COUNT, closed)
    val endPts = alignPolylines(
        startPts,
        resamplePolyline(flattenPath(end), MORPH_SAMPLE_COUNT, closed),
        closed
    )
    return pointsToPath(startPts, closed) to pointsToPath(endPts, closed)
}

private fun resampleToCommonTopology(
    start: PathGeometry,
    mid: PathGeometry,
    end: PathGeometry
): Triple<PathGeometry, PathGeometry, PathGeometry> {
    val closed = start.closed || mid.closed || end.closed
    val startPts = resamplePolyline(flattenPath(start), MORPH_SAMPLE_COUNT, closed)
    val midPts = alignPolylines(
        startPts,
        resamplePolyline(flattenPath(mid), MORPH_SAMPLE_COUNT, closed),
        closed
    )
    val endPts = alignPolylines(
        midPts,
        resamplePolyline(flattenPath(end), MORPH_SAMPLE_COUNT, closed),
        closed
    )
    return Triple(pointsToPath(startPts, closed), pointsToPath(midPts, closed), pointsToPath(endPts, closed))
}

private fun interpolatePaths(
    start: PathGeometry,
    end: PathGeometry,
    t: Float,
    stagger: Float = 0f
): PathGeometry {
    val pairs = start.segments.zip(end.segments)
    if (pairs.isEmpty()) return start
    val lastIndex = (pairs.size - 1).coerceAtLeast(1)
    val window = (1f - stagger).coerceAtLeast(0.05f)
    return PathGeometry(
        segments = pairs.mapIndexed { index, (a, b) ->
            val delay = (index / lastIndex.toFloat()) * stagger
            val local = ((t - delay) / window).coerceIn(0f, 1f)
            interpolate(a, b, local)
        },
        closed = start.closed
    )
}

private fun flattenPath(path: PathGeometry): List<MorphPoint> {
    val points = mutableListOf<MorphPoint>()
    var cx = 0f
    var cy = 0f
    var subX = 0f
    var subY = 0f
    fun add(point: MorphPoint) {
        val last = points.lastOrNull()
        if (last == null || last.x != point.x || last.y != point.y) points += point
    }
    for (segment in path.segments) {
        when (segment) {
            is PathSegment.MoveTo -> {
                cx = segment.x
                cy = segment.y
                subX = cx
                subY = cy
                add(MorphPoint(cx, cy))
            }
            is PathSegment.LineTo -> {
                cx = segment.x
                cy = segment.y
                add(MorphPoint(cx, cy))
            }
            is PathSegment.QuadraticBezierTo -> {
                sampleQuadratic(cx, cy, segment.cx, segment.cy, segment.x, segment.y)
                    .drop(1)
                    .forEach(::add)
                cx = segment.x
                cy = segment.y
            }
            is PathSegment.CubicBezierTo -> {
                sampleCubic(cx, cy, segment.cx1, segment.cy1, segment.cx2, segment.cy2, segment.x, segment.y)
                    .drop(1)
                    .forEach(::add)
                cx = segment.x
                cy = segment.y
            }
            PathSegment.Close -> {
                add(MorphPoint(subX, subY))
                cx = subX
                cy = subY
            }
        }
    }
    return points
}

private fun sampleQuadratic(
    x0: Float, y0: Float, cx: Float, cy: Float, x1: Float, y1: Float, steps: Int = 10
): List<MorphPoint> = List(steps + 1) { i ->
    val t = i / steps.toFloat()
    val u = 1f - t
    MorphPoint(
        u * u * x0 + 2f * u * t * cx + t * t * x1,
        u * u * y0 + 2f * u * t * cy + t * t * y1
    )
}

private fun sampleCubic(
    x0: Float, y0: Float,
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    x3: Float, y3: Float,
    steps: Int = 12
): List<MorphPoint> = List(steps + 1) { i ->
    val t = i / steps.toFloat()
    val u = 1f - t
    val uu = u * u
    val tt = t * t
    MorphPoint(
        uu * u * x0 + 3f * uu * t * x1 + 3f * u * tt * x2 + tt * t * x3,
        uu * u * y0 + 3f * uu * t * y1 + 3f * u * tt * y2 + tt * t * y3
    )
}

private fun resamplePolyline(points: List<MorphPoint>, count: Int, closed: Boolean): List<MorphPoint> {
    if (count <= 0) return emptyList()
    if (points.isEmpty()) return List(count) { MorphPoint(128f, 128f) }
    val ring = if (closed && points.size > 1 && near(points.first(), points.last())) {
        points.dropLast(1)
    } else {
        points
    }
    if (ring.size == 1) return List(count) { ring.first() }
    val chain = if (closed) ring + ring.first() else ring
    val distances = FloatArray(chain.size)
    for (i in 1 until chain.size) {
        distances[i] = distances[i - 1] + distance(chain[i - 1], chain[i])
    }
    val total = distances.last().coerceAtLeast(0.0001f)
    return List(count) { i ->
        val target = if (closed) {
            (i.toFloat() / count) * total
        } else {
            (i.toFloat() / (count - 1).coerceAtLeast(1)) * total
        }
        pointAtLength(chain, distances, target)
    }
}

private fun alignPolylines(start: List<MorphPoint>, end: List<MorphPoint>, closed: Boolean): List<MorphPoint> {
    if (start.size != end.size || start.isEmpty()) return end
    fun score(candidate: List<MorphPoint>): Float =
        start.zip(candidate).sumOf { (a, b) -> distanceSquared(a, b).toDouble() }.toFloat()

    var best = end
    var bestScore = score(end)
    val orientations = listOf(end, end.asReversed())
    for (orientation in orientations) {
        if (!closed) {
            val current = score(orientation)
            if (current < bestScore) {
                best = orientation
                bestScore = current
            }
        } else {
            for (offset in orientation.indices) {
                val rotated = List(orientation.size) { orientation[(it + offset) % orientation.size] }
                val current = score(rotated)
                if (current < bestScore) {
                    best = rotated
                    bestScore = current
                }
            }
        }
    }
    return best
}

private fun pointsToPath(points: List<MorphPoint>, closed: Boolean): PathGeometry {
    if (points.isEmpty()) return PathGeometry(emptyList(), closed)
    val segments = buildList {
        add(PathSegment.MoveTo(points.first().x, points.first().y))
        for (i in 1 until points.size) {
            add(PathSegment.LineTo(points[i].x, points[i].y))
        }
        if (closed) add(PathSegment.Close)
    }
    return PathGeometry(segments, closed)
}

private fun pointAtLength(chain: List<MorphPoint>, distances: FloatArray, target: Float): MorphPoint {
    val goal = target.coerceIn(0f, distances.last())
    var index = 1
    while (index < distances.lastIndex && distances[index] < goal) index++
    val from = chain[index - 1]
    val to = chain[index]
    val span = (distances[index] - distances[index - 1]).coerceAtLeast(0.0001f)
    val t = ((goal - distances[index - 1]) / span).coerceIn(0f, 1f)
    return MorphPoint(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)
}

private fun distance(a: MorphPoint, b: MorphPoint): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}

private fun distanceSquared(a: MorphPoint, b: MorphPoint): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return dx * dx + dy * dy
}

private fun near(a: MorphPoint, b: MorphPoint, epsilon: Float = 0.01f): Boolean =
    distanceSquared(a, b) <= epsilon * epsilon

private fun PathSegment.mapPoints(map: (Float, Float) -> Pair<Float, Float>): PathSegment = when (this) {
    is PathSegment.MoveTo -> map(x, y).let { PathSegment.MoveTo(it.first, it.second) }
    is PathSegment.LineTo -> map(x, y).let { PathSegment.LineTo(it.first, it.second) }
    is PathSegment.QuadraticBezierTo -> {
        val control = map(cx, cy)
        val end = map(x, y)
        PathSegment.QuadraticBezierTo(control.first, control.second, end.first, end.second)
    }
    is PathSegment.CubicBezierTo -> {
        val firstControl = map(cx1, cy1)
        val secondControl = map(cx2, cy2)
        val end = map(x, y)
        PathSegment.CubicBezierTo(
            firstControl.first,
            firstControl.second,
            secondControl.first,
            secondControl.second,
            end.first,
            end.second
        )
    }
    PathSegment.Close -> PathSegment.Close
}

private fun interpolate(a: PathSegment, b: PathSegment, t: Float): PathSegment {
    fun value(start: Float, end: Float) = start + (end - start) * t
    return when {
        a is PathSegment.MoveTo && b is PathSegment.MoveTo ->
            PathSegment.MoveTo(value(a.x, b.x), value(a.y, b.y))
        a is PathSegment.LineTo && b is PathSegment.LineTo ->
            PathSegment.LineTo(value(a.x, b.x), value(a.y, b.y))
        a is PathSegment.QuadraticBezierTo && b is PathSegment.QuadraticBezierTo ->
            PathSegment.QuadraticBezierTo(
                value(a.cx, b.cx), value(a.cy, b.cy), value(a.x, b.x), value(a.y, b.y)
            )
        a is PathSegment.CubicBezierTo && b is PathSegment.CubicBezierTo ->
            PathSegment.CubicBezierTo(
                value(a.cx1, b.cx1), value(a.cy1, b.cy1),
                value(a.cx2, b.cx2), value(a.cy2, b.cy2),
                value(a.x, b.x), value(a.y, b.y)
            )
        a is PathSegment.Close && b is PathSegment.Close -> PathSegment.Close
        else -> error("Path topology changed after validation")
    }
}
