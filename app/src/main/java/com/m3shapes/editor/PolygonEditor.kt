package com.m3shapes.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal const val EDITOR_VIEWPORT = 256f
private const val HIT_RADIUS_VP = 14f
private const val FIT_MARGIN_VP = 18f

enum class EditorOverlay {
    Off,
    Grid4,
    Grid8,
    Dots8
}

fun defaultDrawnPolygon(numVertices: Int = 4): DrawnPolygonConfig {
    val cx = EDITOR_VIEWPORT / 2f
    val cy = EDITOR_VIEWPORT / 2f
    val r = 90f
    val verts = (0 until numVertices).map { i ->
        val angle = (i.toFloat() / numVertices) * 360f - 90f
        val rad = Math.toRadians(angle.toDouble())
        VertexPoint(
            x = cx + r * kotlin.math.cos(rad).toFloat(),
            y = cy + r * kotlin.math.sin(rad).toFloat()
        )
    }
    return DrawnPolygonConfig(
        vertices = verts,
        perVertexRounding = verts.mapIndexed { i, _ ->
            VertexCornerConfig(vertexId = "v$i")
        }
    )
}

fun drawnFromRoundedPolygon(
    polygon: RoundedPolygon,
    rounding: Float = 0f,
    smoothing: Float = 0f
): DrawnPolygonConfig {
    val verts = polygon.cubics.map { cubic ->
        VertexPoint(x = cubic.anchor0X, y = cubic.anchor0Y)
    }.distinctBy { "${it.x.roundToInt()},${it.y.roundToInt()}" }
        .let { if (it.size >= 3) it else polygon.cubics.map { c -> VertexPoint(c.anchor0X, c.anchor0Y) } }
    val r = rounding.coerceIn(0f, 1f)
    val s = smoothing.coerceIn(0f, 1f)
    return DrawnPolygonConfig(
        vertices = verts,
        rounding = r,
        smoothing = s,
        perVertexRounding = verts.mapIndexed { i, _ ->
            VertexCornerConfig(
                vertexId = "v$i",
                rounding = r,
                smoothing = s
            )
        }
    )
}

fun drawnFromFormConfig(config: FormConfig): DrawnPolygonConfig =
    drawnFromRoundedPolygon(
        polygon = compilePolygon(config),
        rounding = config.rounding,
        smoothing = config.smoothing
    )

fun simplifyStroke(points: List<Offset>, targetVertices: Int = 12): List<Offset> {
    if (points.size < 3) return points
    val closed = if ((points.first() - points.last()).getDistance() > 8f) {
        points + points.first()
    } else {
        points
    }
    val totalLength = closed.zipWithNext().sumOf { (a, b) -> (a - b).getDistance().toDouble() }.toFloat()
    if (totalLength < 1f) return emptyList()
    val step = totalLength / targetVertices
    val result = mutableListOf<Offset>()
    var accumulated = 0f
    var target = 0f
    result.add(closed.first())
    for (i in 0 until closed.lastIndex) {
        val segLen = (closed[i + 1] - closed[i]).getDistance()
        while (target + step <= accumulated + segLen && result.size < targetVertices) {
            target += step
            val t = ((target - accumulated) / segLen).coerceIn(0f, 1f)
            result.add(
                Offset(
                    closed[i].x + t * (closed[i + 1].x - closed[i].x),
                    closed[i].y + t * (closed[i + 1].y - closed[i].y)
                )
            )
        }
        accumulated += segLen
    }
    return result.distinctBy { "${it.x.roundToInt()},${it.y.roundToInt()}" }
        .take(targetVertices.coerceAtLeast(3))
}

private fun Offset.getDistance(): Float = hypot(x, y)

private data class CanvasTransform(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float
)

private fun canvasTransform(
    canvasWidth: Float,
    canvasHeight: Float,
    zoom: Float,
    panX: Float,
    panY: Float
): CanvasTransform {
    val base = min(canvasWidth, canvasHeight) / EDITOR_VIEWPORT
    val s = base * zoom.coerceIn(0.25f, 4f)
    val contentW = EDITOR_VIEWPORT * s
    val contentH = EDITOR_VIEWPORT * s
    return CanvasTransform(
        scale = s,
        offsetX = (canvasWidth - contentW) / 2f + panX,
        offsetY = (canvasHeight - contentH) / 2f + panY
    )
}

private fun Offset.toViewport(t: CanvasTransform): Offset = Offset(
    x = (x - t.offsetX) / t.scale,
    y = (y - t.offsetY) / t.scale
)

private fun Offset.toCanvas(t: CanvasTransform): Offset = Offset(
    x = x * t.scale + t.offsetX,
    y = y * t.scale + t.offsetY
)

private fun findVertexAt(pos: Offset, vertices: List<EditableVertex>, radius: Float): EditableVertex? {
    vertices.forEach { vertex ->
        if (hypot(pos.x - vertex.x, pos.y - vertex.y) <= radius) return vertex
    }
    return null
}

fun fitVerticesToViewport(
    vertices: List<VertexPoint>,
    viewport: Float = EDITOR_VIEWPORT,
    margin: Float = FIT_MARGIN_VP
): List<VertexPoint> {
    if (vertices.size < 2) return vertices

    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    for (v in vertices) {
        minX = min(minX, v.x)
        minY = min(minY, v.y)
        maxX = max(maxX, v.x)
        maxY = max(maxY, v.y)
    }

    val w = (maxX - minX).coerceAtLeast(0.001f)
    val h = (maxY - minY).coerceAtLeast(0.001f)
    val scale = (viewport - margin * 2f) / max(w, h)

    val cx = (minX + maxX) / 2f
    val cy = (minY + maxY) / 2f
    val targetCx = viewport / 2f
    val targetCy = viewport / 2f

    return vertices.map { v ->
        val nx = (v.x - cx) * scale + targetCx
        val ny = (v.y - cy) * scale + targetCy
        VertexPoint(
            x = nx.coerceIn(margin, viewport - margin),
            y = ny.coerceIn(margin, viewport - margin)
        )
    }
}

fun fitDrawnPolygonConfig(config: DrawnPolygonConfig): DrawnPolygonConfig {
    val maxCornerRounding = config.perVertexRounding.maxOfOrNull { it.rounding } ?: config.rounding
    val roundingRadius = (maxOf(config.rounding, maxCornerRounding).coerceIn(0f, 1f) * 55f)
    val effectiveMargin = (FIT_MARGIN_VP + roundingRadius + 6f).coerceAtMost(72f)
    val fitted = fitVerticesToViewport(config.vertices, margin = effectiveMargin)
    return config.copy(
        vertices = fitted,
        perVertexRounding = ensureDrawnCornerIds(config).perVertexRounding
    )
}

@Composable
fun PolygonEditorCanvas(
    config: DrawnPolygonConfig,
    onConfigChange: (DrawnPolygonConfig) -> Unit,
    fillColor: Color,
    freehandMode: Boolean,
    overlay: EditorOverlay = EditorOverlay.Grid4,
    editorState: ShapeEditorState,
    onEditorStateChange: (ShapeEditorState) -> Unit,
    modifier: Modifier = Modifier
) {
    var strokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var dragVertexIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var lastDragVp by remember { mutableStateOf<Offset?>(null) }
    var isPanning by remember { mutableStateOf(false) }

    val currentConfig by rememberUpdatedState(ensureDrawnCornerIds(config))
    val currentFreehandMode by rememberUpdatedState(freehandMode)
    val currentEditorState by rememberUpdatedState(editorState)
    val editableVertices = remember(currentConfig) { currentConfig.toEditableVertices() }
    val polygon = remember(currentConfig) { compileDrawnPolygon(currentConfig) }

    fun pushVertices(updated: List<EditableVertex>) {
        onConfigChange(
            updated.toDrawnPolygonConfig(
                fallbackRounding = currentConfig.rounding,
                fallbackSmoothing = currentConfig.smoothing
            )
        )
    }

    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentFreehandMode, currentEditorState.multiSelectEnabled) {
                    detectTapGestures { offset ->
                        if (currentFreehandMode) return@detectTapGestures
                        val t = canvasTransform(
                            size.width.toFloat(),
                            size.height.toFloat(),
                            currentEditorState.viewport.zoom,
                            currentEditorState.viewport.panX,
                            currentEditorState.viewport.panY
                        )
                        val vp = offset.toViewport(t)
                        val hit = findVertexAt(vp, currentConfig.toEditableVertices(), HIT_RADIUS_VP / currentEditorState.viewport.zoom)
                        if (hit != null) {
                            onEditorStateChange(currentEditorState.selectVertex(hit.id))
                        }
                    }
                }
                .pointerInput(currentFreehandMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val t = canvasTransform(
                                size.width.toFloat(),
                                size.height.toFloat(),
                                currentEditorState.viewport.zoom,
                                currentEditorState.viewport.panX,
                                currentEditorState.viewport.panY
                            )
                            val vp = offset.toViewport(t)
                            if (currentFreehandMode) {
                                strokePoints = listOf(vp)
                                dragVertexIds = emptySet()
                                isPanning = false
                                lastDragVp = null
                                return@detectDragGestures
                            }
                            val verts = currentConfig.toEditableVertices()
                            val hit = findVertexAt(vp, verts, HIT_RADIUS_VP / currentEditorState.viewport.zoom)
                            if (hit != null) {
                                val selection = if (hit.id in currentEditorState.selectedVertexIds) {
                                    currentEditorState
                                } else {
                                    currentEditorState.replaceSelection(hit.id)
                                }
                                onEditorStateChange(selection)
                                dragVertexIds = selection.selectedVertexIds
                                lastDragVp = vp
                                isPanning = false
                            } else {
                                dragVertexIds = emptySet()
                                lastDragVp = offset
                                isPanning = true
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (currentFreehandMode) {
                                val t = canvasTransform(
                                    size.width.toFloat(),
                                    size.height.toFloat(),
                                    currentEditorState.viewport.zoom,
                                    currentEditorState.viewport.panX,
                                    currentEditorState.viewport.panY
                                )
                                val vp = change.position.toViewport(t)
                                val last = strokePoints.lastOrNull()
                                if (last == null || (last - vp).getDistance() > 3f) {
                                    strokePoints = strokePoints + vp
                                }
                                return@detectDragGestures
                            }
                            if (isPanning) {
                                val vpState = currentEditorState.viewport
                                onEditorStateChange(
                                    currentEditorState.copy(
                                        viewport = vpState.copy(
                                            panX = vpState.panX + dragAmount.x,
                                            panY = vpState.panY + dragAmount.y
                                        )
                                    )
                                )
                                return@detectDragGestures
                            }
                            if (dragVertexIds.isEmpty()) return@detectDragGestures
                            val t = canvasTransform(
                                size.width.toFloat(),
                                size.height.toFloat(),
                                currentEditorState.viewport.zoom,
                                currentEditorState.viewport.panX,
                                currentEditorState.viewport.panY
                            )
                            val vp = change.position.toViewport(t)
                            val prev = lastDragVp ?: vp
                            val dx = vp.x - prev.x
                            val dy = vp.y - prev.y
                            lastDragVp = vp
                            val moved = moveSelectedVertices(
                                vertices = currentConfig.toEditableVertices(),
                                selectedIds = dragVertexIds,
                                dx = dx,
                                dy = dy,
                                snapToGrid = false,
                                gridSize = currentEditorState.gridSize
                            )
                            pushVertices(moved)
                        },
                        onDragEnd = {
                            if (currentFreehandMode && strokePoints.size >= 3) {
                                val simplified = simplifyStroke(strokePoints, targetVertices = 12)
                                if (simplified.size >= 3) {
                                    val editable = simplified.mapIndexed { i, pt ->
                                        EditableVertex(id = "v$i", x = pt.x, y = pt.y)
                                    }
                                    onConfigChange(
                                        fitDrawnPolygonConfig(
                                            editable.toDrawnPolygonConfig(
                                                fallbackRounding = currentConfig.rounding,
                                                fallbackSmoothing = currentConfig.smoothing
                                            )
                                        )
                                    )
                                    onEditorStateChange(
                                        currentEditorState.copy(
                                            selectedVertexIds = emptySet(),
                                            lastSelectedVertexId = null
                                        )
                                    )
                                }
                            } else if (dragVertexIds.isNotEmpty() && currentEditorState.snapToGrid) {
                                val snappedVerts = currentConfig.toEditableVertices().map { v ->
                                    if (v.id !in dragVertexIds || v.locked) v
                                    else {
                                        val gx = (v.x / currentEditorState.gridSize).roundToInt() * currentEditorState.gridSize
                                        val gy = (v.y / currentEditorState.gridSize).roundToInt() * currentEditorState.gridSize
                                        v.copy(
                                            x = gx.coerceIn(0f, 256f),
                                            y = gy.coerceIn(0f, 256f)
                                        )
                                    }
                                }
                                pushVertices(snappedVerts)
                            }
                            strokePoints = emptyList()
                            dragVertexIds = emptySet()
                            lastDragVp = null
                            isPanning = false
                        },
                        onDragCancel = {
                            strokePoints = emptyList()
                            dragVertexIds = emptySet()
                            lastDragVp = null
                            isPanning = false
                        }
                    )
                }
        ) {
            val cw = size.width
            val ch = size.height
            val t = canvasTransform(
                cw,
                ch,
                editorState.viewport.zoom,
                editorState.viewport.panX,
                editorState.viewport.panY
            )
            val gridColor = Color(0xFFE8E8E8)
            val contentSize = EDITOR_VIEWPORT * t.scale

            when (overlay) {
                EditorOverlay.Off -> Unit
                EditorOverlay.Grid4 -> {
                    val gridStep = contentSize / 4f
                    for (i in 1..3) {
                        drawLine(
                            gridColor,
                            Offset(t.offsetX + gridStep * i, t.offsetY),
                            Offset(t.offsetX + gridStep * i, t.offsetY + contentSize),
                            strokeWidth = 0.5f
                        )
                        drawLine(
                            gridColor,
                            Offset(t.offsetX, t.offsetY + gridStep * i),
                            Offset(t.offsetX + contentSize, t.offsetY + gridStep * i),
                            strokeWidth = 0.5f
                        )
                    }
                }
                EditorOverlay.Grid8 -> {
                    val gridStep = contentSize / 8f
                    for (i in 1..7) {
                        drawLine(
                            gridColor,
                            Offset(t.offsetX + gridStep * i, t.offsetY),
                            Offset(t.offsetX + gridStep * i, t.offsetY + contentSize),
                            strokeWidth = 0.5f
                        )
                        drawLine(
                            gridColor,
                            Offset(t.offsetX, t.offsetY + gridStep * i),
                            Offset(t.offsetX + contentSize, t.offsetY + gridStep * i),
                            strokeWidth = 0.5f
                        )
                    }
                }
                EditorOverlay.Dots8 -> {
                    val step = contentSize / 8f
                    for (y in 0..8) {
                        for (x in 0..8) {
                            drawCircle(
                                color = gridColor,
                                radius = 1.2f,
                                center = Offset(t.offsetX + step * x, t.offsetY + step * y)
                            )
                        }
                    }
                }
            }

            if (editableVertices.size >= 3) {
                val path = polygon.toPath().asComposePath()
                val strokeWidth = 2f / t.scale
                translate(t.offsetX, t.offsetY) {
                    scale(t.scale, t.scale, pivot = Offset.Zero) {
                        drawPath(path, fillColor.copy(alpha = 0.35f), style = Fill)
                        drawPath(path, fillColor, style = Stroke(width = strokeWidth))
                    }
                }

                editableVertices.forEachIndexed { index, vertex ->
                    val center = Offset(vertex.x, vertex.y).toCanvas(t)
                    val selected = vertex.id in editorState.selectedVertexIds
                    val last = vertex.id == editorState.lastSelectedVertexId
                    val radius = when {
                        last -> 11f
                        selected -> 10f
                        else -> 7f
                    }
                    val color = when {
                        vertex.locked -> Color(0xFF757575)
                        last -> Color(0xFFE53935)
                        selected -> Color(0xFF1E88E5)
                        else -> fillColor
                    }
                    drawCircle(color = color, radius = radius, center = center)
                    drawCircle(
                        color = Color.White,
                        radius = radius * 0.45f,
                        center = center
                    )
                    if (editorState.showVertexIndices) {
                        drawContext.canvas.nativeCanvas.drawText(
                            index.toString(),
                            center.x + 10f,
                            center.y - 10f,
                            android.graphics.Paint().apply {
                                this.color = android.graphics.Color.DKGRAY
                                textSize = 28f
                                isAntiAlias = true
                            }
                        )
                    }
                }
            }

            if (freehandMode && strokePoints.isNotEmpty()) {
                val strokePath = Path().apply {
                    strokePoints.forEachIndexed { i, pt ->
                        val c = pt.toCanvas(t)
                        if (i == 0) moveTo(c.x, c.y) else lineTo(c.x, c.y)
                    }
                }
                drawPath(strokePath, Color(0xFF1E88E5), style = Stroke(width = 3f))
            }
        }
    }
}
