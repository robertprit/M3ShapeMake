package com.compose.canvas.hit

import androidx.compose.ui.geometry.Offset
import com.compose.canvas.core.SceneNode
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorObject
import com.compose.canvas.geometry.EllipseGeometry
import com.compose.canvas.geometry.LineGeometry
import com.compose.canvas.geometry.ObjectId
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import com.compose.canvas.geometry.RectGeometry
import com.compose.canvas.math.TransformUtils
import com.compose.canvas.viewport.ViewportState
import kotlin.math.abs
import kotlin.math.sqrt

sealed interface HitTarget {
    data class Shape(val id: ObjectId) : HitTarget
    data class Stroke(val id: ObjectId) : HitTarget
    data class Port(val objectId: ObjectId, val portId: ObjectId) : HitTarget
    data class Anchor(val objectId: ObjectId, val anchorId: ObjectId) : HitTarget
    data object None : HitTarget
}

data class HitResult(
    val target: HitTarget,
    val objectId: ObjectId? = (target as? HitTarget.Shape)?.id
        ?: (target as? HitTarget.Stroke)?.id
)

/**
 * Geometric hit testing in document space. [screenTolerance] is converted by
 * dividing by zoom so the finger/stylus radius stays constant on screen.
 */
object HitTesting {
    fun hitTest(
        screenX: Float,
        screenY: Float,
        document: VectorDocument,
        viewport: ViewportState,
        screenTolerance: Float
    ): HitResult {
        val doc = viewport.screenToDocument(screenX, screenY)
        val tolerance = screenTolerance / viewport.zoom
        val topLevel = document.layers.filter { it.visible }.flatMap { it.objects }
        for (node in topLevel.asReversed()) {
            val hit = hitNode(doc, node, tolerance, com.compose.canvas.math.TransformMatrix()) ?: continue
            return HitResult(hit)
        }
        return HitResult(HitTarget.None)
    }

    private fun hitNode(
        doc: Offset,
        node: SceneNode,
        tolerance: Float,
        parent: com.compose.canvas.math.TransformMatrix
    ): HitTarget? {
        if (!node.visible || node.locked) return null
        val world = TransformUtils.multiply(parent, TransformUtils.fromObjectTransform(node.transform))
        return when (node) {
            is VectorObject -> hitObject(doc, node, tolerance, world)
            is com.compose.canvas.core.GroupObject ->
                node.children.asReversed().firstNotNullOfOrNull { hitNode(doc, it, tolerance, world) }
        }
    }

    private fun hitObject(
        doc: Offset,
        obj: VectorObject,
        tolerance: Float,
        world: com.compose.canvas.math.TransformMatrix
    ): HitTarget? {
        val inverse = world.invert() ?: return null
        val local = inverse.transformPoint(doc)
        for (port in obj.semantics.ports) {
            val d = sqrt((local.x - port.x) * (local.x - port.x) + (local.y - port.y) * (local.y - port.y))
            if (d <= port.snapRadius.coerceAtLeast(tolerance)) return HitTarget.Port(obj.id, port.id)
        }
        for (anchor in obj.semantics.anchors) {
            val d = sqrt((local.x - anchor.x) * (local.x - anchor.x) + (local.y - anchor.y) * (local.y - anchor.y))
            if (d <= tolerance) return HitTarget.Anchor(obj.id, anchor.id)
        }
        val hasFill = obj.fill.isVisible
        return when (val geom = obj.geometry) {
            is RectGeometry -> hitRect(local, geom, tolerance, obj.id, hasFill)
            is EllipseGeometry -> hitEllipse(local, geom, tolerance, obj.id)
            is LineGeometry -> hitLine(local, geom, tolerance, obj.id)
            is PathGeometry -> hitPath(local, geom, tolerance, obj.id)
            else -> {
                val path = com.compose.canvas.geometry.GeometryToPath.toPath(geom, obj.semantics.parameters)
                hitPath(local, path, tolerance, obj.id)
                    ?: if (geom.boundingRect().contains(local.x, local.y) && hasFill) HitTarget.Shape(obj.id) else null
            }
        }
    }

    private fun hitRect(
        p: Offset,
        geom: RectGeometry,
        tolerance: Float,
        id: ObjectId,
        hasFill: Boolean
    ): HitTarget? {
        val w = geom.width
        val h = geom.height
        val inFill = p.x in 0f..w && p.y in 0f..h
        if (inFill && hasFill) return HitTarget.Shape(id)
        val edge = distanceToRectEdge(p.x, p.y, w, h)
        if (inFill || (p.x in -tolerance..w + tolerance && p.y in -tolerance..h + tolerance)) {
            if (edge <= tolerance) return HitTarget.Stroke(id)
            if (inFill) return HitTarget.Shape(id)
        }
        return null
    }

    private fun distanceToRectEdge(px: Float, py: Float, w: Float, h: Float): Float {
        val dx = if (px < 0f) -px else if (px > w) px - w else 0f
        val dy = if (py < 0f) -py else if (py > h) py - h else 0f
        if (dx > 0f && dy > 0f) return sqrt(dx * dx + dy * dy)
        if (px in 0f..w && py in 0f..h) {
            return minOf(px, py, w - px, h - py)
        }
        return maxOf(dx, dy)
    }

    private fun hitEllipse(
        p: Offset,
        geom: EllipseGeometry,
        tolerance: Float,
        id: ObjectId
    ): HitTarget? {
        val rx = geom.radiusX
        val ry = geom.radiusY
        if (rx <= 0f || ry <= 0f) return null
        val nx = p.x / rx
        val ny = p.y / ry
        val r = sqrt(nx * nx + ny * ny)
        val dist = abs(r - 1f) * minOf(rx, ry)
        if (dist <= tolerance) return HitTarget.Stroke(id)
        if (r < 1f) return HitTarget.Shape(id)
        return null
    }

    private fun hitLine(
        p: Offset,
        geom: LineGeometry,
        tolerance: Float,
        id: ObjectId
    ): HitTarget? {
        val d = distancePointToSegment(p.x, p.y, geom.startX, geom.startY, geom.endX, geom.endY)
        return if (d <= tolerance) HitTarget.Stroke(id) else null
    }

    private fun hitPath(
        p: Offset,
        geom: PathGeometry,
        tolerance: Float,
        id: ObjectId
    ): HitTarget? {
        if (geom.segments.isEmpty()) return null
        var closest = Float.MAX_VALUE
        var lastX = 0f
        var lastY = 0f
        var subX = 0f
        var subY = 0f
        for (seg in geom.segments) {
            when (seg) {
                is PathSegment.MoveTo -> {
                    lastX = seg.x; lastY = seg.y
                    subX = seg.x; subY = seg.y
                }
                is PathSegment.LineTo -> {
                    closest = minOf(closest, distancePointToSegment(p.x, p.y, lastX, lastY, seg.x, seg.y))
                    lastX = seg.x; lastY = seg.y
                }
                is PathSegment.QuadraticBezierTo -> {
                    closest = minOf(closest, distanceQuad(p.x, p.y, lastX, lastY, seg.cx, seg.cy, seg.x, seg.y))
                    lastX = seg.x; lastY = seg.y
                }
                is PathSegment.CubicBezierTo -> {
                    closest = minOf(
                        closest,
                        distanceCubic(p.x, p.y, lastX, lastY, seg.cx1, seg.cy1, seg.cx2, seg.cy2, seg.x, seg.y)
                    )
                    lastX = seg.x; lastY = seg.y
                }
                is PathSegment.Close -> {
                    closest = minOf(closest, distancePointToSegment(p.x, p.y, lastX, lastY, subX, subY))
                    lastX = subX; lastY = subY
                }
            }
        }
        return if (closest <= tolerance) HitTarget.Stroke(id) else null
    }

    private fun distancePointToSegment(
        px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-6f) return hypot(px - x1, py - y1)
        val t = (((px - x1) * dx + (py - y1) * dy) / lenSq).coerceIn(0f, 1f)
        return hypot(px - (x1 + t * dx), py - (y1 + t * dy))
    }

    private fun distanceQuad(
        px: Float, py: Float,
        x0: Float, y0: Float, cx: Float, cy: Float, x1: Float, y1: Float,
        steps: Int = 20
    ): Float {
        var closest = Float.MAX_VALUE
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val u = 1f - t
            val bx = u * u * x0 + 2f * u * t * cx + t * t * x1
            val by = u * u * y0 + 2f * u * t * cy + t * t * y1
            closest = minOf(closest, hypot(px - bx, py - by))
        }
        return closest
    }

    private fun distanceCubic(
        px: Float, py: Float,
        x0: Float, y0: Float,
        cx1: Float, cy1: Float, cx2: Float, cy2: Float,
        x1: Float, y1: Float,
        steps: Int = 20
    ): Float {
        var closest = Float.MAX_VALUE
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val u = 1f - t
            val bx = u * u * u * x0 + 3f * u * u * t * cx1 + 3f * u * t * t * cx2 + t * t * t * x1
            val by = u * u * u * y0 + 3f * u * u * t * cy1 + 3f * u * t * t * cy2 + t * t * t * y1
            closest = minOf(closest, hypot(px - bx, py - by))
        }
        return closest
    }

    private fun hypot(dx: Float, dy: Float): Float = sqrt(dx * dx + dy * dy)
}
