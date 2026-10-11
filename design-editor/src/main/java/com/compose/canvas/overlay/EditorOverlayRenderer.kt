package com.compose.canvas.overlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.compose.canvas.core.VectorObject
import com.compose.canvas.core.WorldTransform
import com.compose.canvas.editor.Axis
import com.compose.canvas.editor.CreateShapeState
import com.compose.canvas.editor.EditorState
import com.compose.canvas.editor.GeometryType
import com.compose.canvas.editor.MarqueeState
import com.compose.canvas.editor.PathDrawingState
import com.compose.canvas.editor.PathEditState
import com.compose.canvas.editor.PencilDrawingState
import com.compose.canvas.editor.SnapResult
import com.compose.canvas.tools.path.HandleRef
import com.compose.canvas.tools.path.NodeRef
import com.compose.canvas.viewport.ViewportState
import kotlin.math.hypot

object EditorOverlayRenderer {
    private val preview = Color(0x806750A4)
    private val pathColor = Color(0xFF006D3C)
    private val snapColor = Color(0xFFFF6D00)
    private val portColor = Color(0xFF1565C0)
    private val anchorColor = Color(0xFF6A1B9A)
    private val areaColor = Color(0x804CAF50)
    private val nodeColor = Color(0xFF0D47A1)
    private val handleColor = Color(0xFFE65100)

    fun drawCreatePreview(
        scope: DrawScope,
        state: CreateShapeState,
        viewport: ViewportState
    ) {
        if (!state.active) return
        val start = viewport.documentToScreen(state.startDocPoint.x, state.startDocPoint.y)
        val current = viewport.documentToScreen(state.currentDocPoint.x, state.currentDocPoint.y)
        val left = minOf(start.x, current.x)
        val top = minOf(start.y, current.y)
        val width = kotlin.math.abs(current.x - start.x)
        val height = kotlin.math.abs(current.y - start.y)
        when (state.geometryType) {
            GeometryType.Rectangle, GeometryType.Triangle -> scope.drawRect(
                color = preview,
                topLeft = Offset(left, top),
                size = Size(width, height)
            )
            GeometryType.Ellipse, GeometryType.Polygon, GeometryType.Star -> scope.drawOval(
                color = preview,
                topLeft = Offset(left, top),
                size = Size(width, height)
            )
            GeometryType.Line -> scope.drawLine(
                color = preview.copy(alpha = 1f),
                start = start,
                end = current,
                strokeWidth = 2f
            )
        }
    }

    fun drawPathPreview(
        scope: DrawScope,
        state: PathDrawingState,
        viewport: ViewportState
    ) {
        if (!state.active || state.points.isEmpty()) return
        val pts = state.points.map { viewport.documentToScreen(it.x, it.y) }
        for (i in 1 until pts.size) {
            scope.drawLine(color = pathColor, start = pts[i - 1], end = pts[i], strokeWidth = 2f)
        }
        pts.forEach {
            scope.drawCircle(color = Color.White, radius = 10f, center = it)
            scope.drawCircle(color = pathColor, radius = 8f, center = it)
        }
        if (pts.size >= 2) {
            scope.drawCircle(color = pathColor.copy(alpha = 0.3f), radius = 15f, center = pts.first())
        }
    }

    fun drawPencilPreview(scope: DrawScope, state: PencilDrawingState, viewport: ViewportState) {
        if (!state.active || state.points.size < 2) return
        val pts = state.points.map { viewport.documentToScreen(it.x, it.y) }
        for (i in 1 until pts.size) {
            scope.drawLine(color = pathColor, start = pts[i - 1], end = pts[i], strokeWidth = 2.5f)
        }
    }

    fun drawMarquee(scope: DrawScope, state: MarqueeState, viewport: ViewportState) {
        if (!state.active) return
        val a = viewport.documentToScreen(state.start.x, state.start.y)
        val b = viewport.documentToScreen(state.current.x, state.current.y)
        val left = minOf(a.x, b.x)
        val top = minOf(a.y, b.y)
        scope.drawRect(
            color = Color(0x331565C0),
            topLeft = Offset(left, top),
            size = Size(kotlin.math.abs(b.x - a.x), kotlin.math.abs(b.y - a.y))
        )
        scope.drawRect(
            color = Color(0xFF1565C0),
            topLeft = Offset(left, top),
            size = Size(kotlin.math.abs(b.x - a.x), kotlin.math.abs(b.y - a.y)),
            style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        )
    }

    fun drawSnapGuides(scope: DrawScope, guides: List<SnapResult>, viewport: ViewportState) {
        for (g in guides) {
            val p = viewport.documentToScreen(g.position.x, g.position.y)
            when (g.axis) {
                Axis.X, Axis.Both -> scope.drawLine(
                    color = snapColor,
                    start = Offset(p.x, 0f),
                    end = Offset(p.x, viewport.screenHeight),
                    strokeWidth = 1f
                )
                Axis.Y -> Unit
            }
            when (g.axis) {
                Axis.Y, Axis.Both -> scope.drawLine(
                    color = snapColor,
                    start = Offset(0f, p.y),
                    end = Offset(viewport.screenWidth, p.y),
                    strokeWidth = 1f
                )
                Axis.X -> Unit
            }
        }
    }

    fun drawMetadata(scope: DrawScope, objects: List<VectorObject>, viewport: ViewportState, document: com.compose.canvas.core.VectorDocument) {
        for (obj in objects) {
            val world = WorldTransform.matrixOf(document, obj.id) ?: continue
            for (port in obj.semantics.ports) {
                val p = world.transformPoint(port.x, port.y)
                val s = viewport.documentToScreen(p.x, p.y)
                scope.drawCircle(color = Color.White, radius = 12f, center = s)
                scope.drawCircle(color = portColor, radius = 9f, center = s)
                scope.drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.White.toArgb()
                        textSize = 12f * scope.density
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        setShadowLayer(3f, 0f, 1f, Color.Black.toArgb())
                    }
                    canvas.nativeCanvas.drawText(
                        port.name.ifBlank { port.portType.name.lowercase() },
                        s.x + 15f,
                        s.y + paint.textSize * 0.35f,
                        paint,
                    )
                }
            }
            for (anchor in obj.semantics.anchors) {
                val p = world.transformPoint(anchor.x, anchor.y)
                val s = viewport.documentToScreen(p.x, p.y)
                scope.drawCircle(color = Color.White, radius = 11f, center = s)
                scope.drawCircle(color = anchorColor, radius = 9f, center = s, style = Stroke(3f))
            }
            for (area in obj.semantics.contentAreas) {
                val tl = world.transformPoint(area.bounds.left, area.bounds.top)
                val br = world.transformPoint(area.bounds.right, area.bounds.bottom)
                val a = viewport.documentToScreen(tl.x, tl.y)
                val b = viewport.documentToScreen(br.x, br.y)
                scope.drawRect(
                    color = areaColor,
                    topLeft = Offset(minOf(a.x, b.x), minOf(a.y, b.y)),
                    size = Size(kotlin.math.abs(b.x - a.x), kotlin.math.abs(b.y - a.y)),
                    style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
                )
            }
        }
    }

    fun drawPathEdit(scope: DrawScope, state: PathEditState, viewport: ViewportState, world: com.compose.canvas.math.TransformMatrix) {
        if (!state.active) return
        for ((si, sub) in state.data.subpaths.withIndex()) {
            for ((ni, node) in sub.nodes.withIndex()) {
                val p = screen(node.point, world, viewport)
                node.handleIn?.let { h ->
                    val hs = screen(h, world, viewport)
                    scope.drawLine(handleColor, p, hs, 1.5f)
                    scope.drawCircle(Color.White, 12f, hs)
                    scope.drawCircle(handleColor, 9f, hs)
                }
                node.handleOut?.let { h ->
                    val hs = screen(h, world, viewport)
                    scope.drawLine(handleColor, p, hs, 1.5f)
                    scope.drawCircle(Color.White, 12f, hs)
                    scope.drawCircle(handleColor, 9f, hs)
                }
                val selected = state.selectedNode == NodeRef(si, ni)
                scope.drawCircle(if (selected) Color(0xFFFFC107) else Color.White, 14f, p)
                scope.drawCircle(nodeColor, 14f, p, style = Stroke(3.5f))
            }
        }
    }

    fun hitPathEdit(
        screenX: Float,
        screenY: Float,
        state: PathEditState,
        viewport: ViewportState,
        world: com.compose.canvas.math.TransformMatrix,
        tolerance: Float
    ): Pair<NodeRef?, HandleRef?> {
        if (!state.active) return null to null
        for ((si, sub) in state.data.subpaths.withIndex()) {
            for ((ni, node) in sub.nodes.withIndex()) {
                val ref = NodeRef(si, ni)
                node.handleOut?.let { h ->
                    val s = screen(h, world, viewport)
                    if (hypot(s.x - screenX, s.y - screenY) <= tolerance.coerceAtLeast(20f)) return ref to HandleRef(ref, true)
                }
                node.handleIn?.let { h ->
                    val s = screen(h, world, viewport)
                    if (hypot(s.x - screenX, s.y - screenY) <= tolerance.coerceAtLeast(20f)) return ref to HandleRef(ref, false)
                }
            }
        }
        for ((si, sub) in state.data.subpaths.withIndex()) {
            for ((ni, node) in sub.nodes.withIndex()) {
                val s = screen(node.point, world, viewport)
                if (hypot(s.x - screenX, s.y - screenY) <= tolerance.coerceAtLeast(22f)) return NodeRef(si, ni) to null
            }
        }
        return null to null
    }

    private fun screen(local: Offset, world: com.compose.canvas.math.TransformMatrix, viewport: ViewportState): Offset {
        val w = world.transformPoint(local)
        return viewport.documentToScreen(w.x, w.y)
    }
}

fun EditorState.overlayObjects(): List<VectorObject> =
    selectedObjects() + document.allObjects().filter { it.semantics.ports.isNotEmpty() || it.semantics.anchors.isNotEmpty() }
        .filter { it.id !in selection.selectedIds }
