package com.compose.canvas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.compose.canvas.core.BoundsCalculator
import com.compose.canvas.core.WorldTransform
import com.compose.canvas.editor.EditorState
import com.compose.canvas.editor.EditorTool
import com.compose.canvas.editor.GeometryType
import com.compose.canvas.editor.HandlePosition
import com.compose.canvas.editor.addAnchor
import com.compose.canvas.editor.addContentArea
import com.compose.canvas.editor.addPencilPoint
import com.compose.canvas.editor.addPort
import com.compose.canvas.editor.addText
import com.compose.canvas.editor.cancelPencil
import com.compose.canvas.editor.clearSnapGuides
import com.compose.canvas.editor.commitPathEdit
import com.compose.canvas.editor.finishMarquee
import com.compose.canvas.editor.finishPathEditDrag
import com.compose.canvas.editor.finishPencil
import com.compose.canvas.editor.finishRotate
import com.compose.canvas.editor.pathEditWorldTransform
import com.compose.canvas.editor.selectPathHandle
import com.compose.canvas.editor.selectPathNode
import com.compose.canvas.editor.snapPoint
import com.compose.canvas.editor.startMarquee
import com.compose.canvas.editor.startPathEditHandleDrag
import com.compose.canvas.editor.startPathEditNodeDrag
import com.compose.canvas.editor.startPencil
import com.compose.canvas.editor.startRotate
import com.compose.canvas.editor.updateMarquee
import com.compose.canvas.editor.updatePathEditDrag
import com.compose.canvas.editor.updateRotate
import com.compose.canvas.geometry.RectF
import com.compose.canvas.hit.HitTarget
import com.compose.canvas.hit.HitTesting
import com.compose.canvas.input.hitTolerancePx
import com.compose.canvas.input.toKind
import com.compose.canvas.overlay.EditorOverlayRenderer
import com.compose.canvas.overlay.HandleHitType
import com.compose.canvas.overlay.SelectionRenderer
import com.compose.canvas.overlay.overlayObjects
import com.compose.canvas.renderer.VectorRenderer
import com.compose.canvas.viewport.ViewportController
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun EditorCanvas(
    editorState: EditorState,
    viewportController: ViewportController,
    modifier: Modifier = Modifier
) {
    var fitted by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE8E8E8))
            .clipToBounds()
            .onSizeChanged { size: IntSize ->
                viewportController.setScreenSize(size.width.toFloat(), size.height.toFloat())
                if (!fitted && size.width > 0 && size.height > 0) {
                    viewportController.fitToDocument(
                        editorState.document.canvas.width,
                        editorState.document.canvas.height
                    )
                    fitted = true
                }
            }
            .pointerInput(editorState, viewportController) {
                awaitPointerEventScope {
                    var toolPointer: PointerId? = null
                    var lastCentroid: Offset? = null
                    var lastSpan: Float? = null
                    var viewportGesture = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }

                        if (event.type == PointerEventType.Scroll) {
                            val pointer = event.changes.firstOrNull() ?: continue
                            val delta = pointer.scrollDelta.y
                            if (delta != 0f) {
                                val factor = if (delta < 0f) 1.1f else 1f / 1.1f
                                val z = viewportController.state.clampZoom(
                                    viewportController.state.zoom * factor
                                )
                                viewportController.zoomAt(z, pointer.position.x, pointer.position.y)
                                pointer.consume()
                            }
                            continue
                        }

                        if (pressed.size >= 2) {
                            if (toolPointer != null) {
                                cancelToolGesture(editorState)
                                toolPointer = null
                            }
                            viewportGesture = true
                            val p0 = pressed[0].position
                            val p1 = pressed[1].position
                            val centroid = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
                            val span = hypot(p1.x - p0.x, p1.y - p0.y)
                            val prevC = lastCentroid
                            val prevS = lastSpan
                            if (prevC != null && prevS != null && prevS > 8f && span > 8f) {
                                val factor = span / prevS
                                val z = viewportController.state.clampZoom(
                                    viewportController.state.zoom * factor
                                )
                                viewportController.zoomAt(z, centroid.x, centroid.y)
                                viewportController.panBy(centroid.x - prevC.x, centroid.y - prevC.y)
                            }
                            lastCentroid = centroid
                            lastSpan = span
                            pressed.forEach { it.consume() }
                            continue
                        }

                        lastCentroid = null
                        lastSpan = null

                        if (viewportGesture && pressed.isEmpty()) {
                            viewportGesture = false
                            continue
                        }
                        if (viewportGesture) continue

                        val pointer = event.changes.firstOrNull() ?: continue
                        when (event.type) {
                            PointerEventType.Press -> {
                                if (toolPointer == null) {
                                    toolPointer = pointer.id
                                    handlePointerDown(
                                        editorState,
                                        viewportController,
                                        pointer.position.x,
                                        pointer.position.y,
                                        pointer.type.toKind().hitTolerancePx()
                                    )
                                }
                            }
                            PointerEventType.Move -> {
                                if (pointer.id == toolPointer) {
                                    handlePointerMove(
                                        editorState,
                                        viewportController,
                                        pointer.position.x,
                                        pointer.position.y
                                    )
                                }
                            }
                            PointerEventType.Release -> {
                                if (pointer.id == toolPointer) {
                                    handlePointerUp(editorState)
                                    toolPointer = null
                                }
                            }
                            else -> Unit
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val viewport = viewportController.state.copy(
                screenWidth = size.width,
                screenHeight = size.height
            )
            val display = editorState.displayDocument()
            VectorRenderer.drawDocument(
                this,
                display,
                viewport,
                gridVisible = editorState.gridVisible && !editorState.previewMode,
                gridSize = editorState.gridSize
            )
            if (!editorState.previewMode) {
                val selected = display.allNodes().filter { it.id in editorState.selection.selectedIds }
                if (selected.isNotEmpty() && !editorState.pathEdit.active) {
                    SelectionRenderer.drawSelection(
                        this,
                        selected,
                        viewport,
                        overrideBounds = editorState.currentResizeBounds
                    )
                }
                EditorOverlayRenderer.drawCreatePreview(this, editorState.createShapeState, viewport)
                EditorOverlayRenderer.drawPathPreview(this, editorState.pathDrawingState, viewport)
                EditorOverlayRenderer.drawPencilPreview(this, editorState.pencilDrawing, viewport)
                EditorOverlayRenderer.drawMarquee(this, editorState.marquee, viewport)
                EditorOverlayRenderer.drawSnapGuides(this, editorState.snapGuides, viewport)
                EditorOverlayRenderer.drawMetadata(this, editorState.overlayObjects(), viewport, display)
                if (editorState.pathEdit.active) {
                    EditorOverlayRenderer.drawPathEdit(
                        this,
                        editorState.pathEdit,
                        viewport,
                        editorState.pathEditWorldTransform
                    )
                }
            }
        }

        if (editorState.pathDrawingState.active) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { editorState.finishPath(closed = false) }) {
                    Text("Fertig")
                }
                OutlinedButton(onClick = { editorState.finishPath(closed = true) }) {
                    Text("Schließen")
                }
                OutlinedButton(onClick = { editorState.cancelPath() }) {
                    Text("Abbrechen")
                }
            }
        }
        if (editorState.pathEdit.active) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { editorState.commitPathEdit() }) { Text("Pfad übernehmen") }
            }
        }
    }
}

private fun cancelToolGesture(editorState: EditorState) {
    editorState.resetDrag()
    editorState.resetResize()
    if (editorState.createShapeState.active) editorState.cancelCreateShape()
    if (editorState.pencilDrawing.active) editorState.cancelPencil()
    if (editorState.marquee.active) editorState.applyMarquee(com.compose.canvas.editor.MarqueeState())
    if (editorState.rotateSession.active) editorState.applyRotate(com.compose.canvas.editor.RotateSession())
    if (editorState.pathEdit.dragTargetNode != null || editorState.pathEdit.dragTargetHandle != null) {
        editorState.finishPathEditDrag()
    }
    editorState.clearSnapGuides()
}

private fun snapped(editorState: EditorState, raw: Offset): Offset =
    if (editorState.snapEnabled) editorState.snapPoint(raw) else raw.also { editorState.clearSnapGuides() }

private fun handlePointerDown(
    editorState: EditorState,
    viewportController: ViewportController,
    screenX: Float,
    screenY: Float,
    hitTolerance: Float
) {
    val raw = viewportController.state.screenToDocument(screenX, screenY)
    val docPt = snapped(editorState, raw)
    when (editorState.activeTool) {
        EditorTool.Select -> {
            val selected = editorState.selectedNodes()
            val bounds = BoundsCalculator.ofAll(selected)
            val handleHit = bounds?.let {
                SelectionRenderer.hitTestHandle(screenX, screenY, it, viewportController.state)
            }
            if (handleHit != null && bounds != null) {
                if (handleHit == HandleHitType.Rotate) {
                    val angle = atan2(raw.y - bounds.centerY, raw.x - bounds.centerX) * 180f / Math.PI.toFloat()
                    editorState.startRotate(Offset(bounds.centerX, bounds.centerY), angle)
                } else {
                    SelectionRenderer.toHandlePosition(handleHit)?.let { editorState.startResize(it, bounds) }
                }
                return
            }
            val hit = HitTesting.hitTest(
                screenX, screenY, editorState.document, viewportController.state, hitTolerance
            )
            val id = when (val t = hit.target) {
                is HitTarget.Shape -> t.id
                is HitTarget.Stroke -> t.id
                is HitTarget.Port -> t.objectId
                is HitTarget.Anchor -> t.objectId
                HitTarget.None -> null
            }
            if (id != null) {
                if (id !in editorState.selection.selectedIds) {
                    editorState.selectObject(id)
                }
                editorState.startDrag(docPt)
            } else {
                editorState.startMarquee(raw)
            }
        }
        EditorTool.Rectangle -> editorState.startCreateShape(GeometryType.Rectangle, docPt)
        EditorTool.Ellipse -> editorState.startCreateShape(GeometryType.Ellipse, docPt)
        EditorTool.Line -> editorState.startCreateShape(GeometryType.Line, docPt)
        EditorTool.Triangle -> editorState.startCreateShape(GeometryType.Triangle, docPt)
        EditorTool.Polygon -> editorState.startCreateShape(GeometryType.Polygon, docPt)
        EditorTool.Star -> editorState.startCreateShape(GeometryType.Star, docPt)
        EditorTool.Path -> editorState.startPath(docPt)
        EditorTool.Pencil -> editorState.startPencil(docPt)
        EditorTool.Text -> editorState.addText(docPt)
        EditorTool.EditPath -> {
            if (!editorState.pathEdit.active && editorState.selection.isSingle) {
                editorState.setTool(EditorTool.EditPath)
            }
            val (node, handle) = EditorOverlayRenderer.hitPathEdit(
                screenX, screenY, editorState.pathEdit, viewportController.state,
                editorState.pathEditWorldTransform, hitTolerance
            )
            val local = toLocal(raw, editorState)
            when {
                handle != null -> {
                    editorState.selectPathHandle(handle)
                    editorState.startPathEditHandleDrag(handle, local)
                }
                node != null -> {
                    editorState.selectPathNode(node)
                    editorState.startPathEditNodeDrag(node, local)
                }
            }
        }
        EditorTool.Port -> {
            val local = toLocal(raw, editorState)
            editorState.addPort(local)
        }
        EditorTool.Anchor -> {
            val local = toLocal(raw, editorState)
            editorState.addAnchor(local)
        }
        EditorTool.ContentArea -> editorState.startMarquee(raw)
        EditorTool.Gradient -> Unit
        EditorTool.Pan -> editorState.startDrag(docPt)
    }
}

private fun handlePointerMove(
    editorState: EditorState,
    viewportController: ViewportController,
    screenX: Float,
    screenY: Float
) {
    val viewport = viewportController.state
    val raw = viewport.screenToDocument(screenX, screenY)
    val docPt = snapped(editorState, raw)
    when (editorState.activeTool) {
        EditorTool.Select -> {
            when {
                editorState.rotateSession.active -> {
                    val c = editorState.rotateSession.center
                    val angle = atan2(raw.y - c.y, raw.x - c.x) * 180f / Math.PI.toFloat()
                    editorState.updateRotate(angle)
                }
                editorState.dragState.active -> editorState.updateDrag(docPt)
                editorState.resizing -> updateResizeBounds(editorState, viewportController, screenX, screenY)
                editorState.marquee.active -> editorState.updateMarquee(raw)
            }
        }
        EditorTool.Rectangle, EditorTool.Ellipse, EditorTool.Line,
        EditorTool.Triangle, EditorTool.Polygon, EditorTool.Star -> {
            if (editorState.createShapeState.active) editorState.updateCreateShape(docPt)
        }
        EditorTool.Pan -> {
            if (editorState.dragState.active) {
                val prev = editorState.dragState.currentDocPoint
                val dxScreen = (raw.x - prev.x) * viewport.zoom
                val dyScreen = (raw.y - prev.y) * viewport.zoom
                viewportController.panBy(dxScreen, dyScreen)
                editorState.updateDrag(raw)
            }
        }
        EditorTool.Pencil -> if (editorState.pencilDrawing.active) editorState.addPencilPoint(docPt)
        EditorTool.EditPath -> {
            if (editorState.pathEdit.dragTargetNode != null || editorState.pathEdit.dragTargetHandle != null) {
                editorState.updatePathEditDrag(toLocal(raw, editorState))
            }
        }
        EditorTool.ContentArea -> if (editorState.marquee.active) editorState.updateMarquee(raw)
        EditorTool.Path, EditorTool.Text, EditorTool.Port, EditorTool.Anchor, EditorTool.Gradient -> Unit
    }
}

private fun handlePointerUp(editorState: EditorState) {
    when (editorState.activeTool) {
        EditorTool.Select -> {
            if (editorState.rotateSession.active) editorState.finishRotate()
            if (editorState.dragState.active) editorState.endDrag()
            if (editorState.resizing) {
                val bounds = editorState.currentResizeBounds ?: editorState.resizeStartBounds
                if (bounds != null) editorState.finishResize(bounds)
                else editorState.resetResize()
            }
            if (editorState.marquee.active) editorState.finishMarquee()
        }
        EditorTool.Rectangle, EditorTool.Ellipse, EditorTool.Line,
        EditorTool.Triangle, EditorTool.Polygon, EditorTool.Star -> {
            if (editorState.createShapeState.active) editorState.finishCreateShape()
        }
        EditorTool.Pan -> editorState.resetDrag()
        EditorTool.Pencil -> editorState.finishPencil()
        EditorTool.EditPath -> editorState.finishPathEditDrag()
        EditorTool.ContentArea -> {
            if (editorState.marquee.active) {
                val m = editorState.marquee
                val world = RectF(
                    minOf(m.start.x, m.current.x),
                    minOf(m.start.y, m.current.y),
                    maxOf(m.start.x, m.current.x),
                    maxOf(m.start.y, m.current.y)
                )
                editorState.applyMarquee(com.compose.canvas.editor.MarqueeState())
                val id = editorState.selection.singleId
                val matrix = id?.let { WorldTransform.matrixOf(editorState.document, it)?.invert() }
                if (matrix != null && world.width > 4f && world.height > 4f) {
                    val a = matrix.transformPoint(world.left, world.top)
                    val b = matrix.transformPoint(world.right, world.bottom)
                    editorState.addContentArea(
                        RectF(minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x, b.x), maxOf(a.y, b.y))
                    )
                }
            }
        }
        EditorTool.Path, EditorTool.Text, EditorTool.Port, EditorTool.Anchor, EditorTool.Gradient -> Unit
    }
    editorState.clearSnapGuides()
}

private fun toLocal(doc: Offset, editorState: EditorState): Offset {
    val id = editorState.pathEdit.objectId ?: editorState.selection.singleId ?: return doc
    val inv = WorldTransform.matrixOf(editorState.document, id)?.invert() ?: return doc
    return inv.transformPoint(doc)
}

private fun updateResizeBounds(
    editorState: EditorState,
    viewportController: ViewportController,
    screenX: Float,
    screenY: Float
) {
    val startBounds = editorState.resizeStartBounds ?: return
    val handle = editorState.resizeHandle ?: return
    if (handle == HandlePosition.Rotate) return
    val raw = viewportController.state.screenToDocument(screenX, screenY)
    val docPt = snapped(editorState, raw)
    var left = startBounds.left
    var top = startBounds.top
    var right = startBounds.right
    var bottom = startBounds.bottom
    when (handle) {
        HandlePosition.TopLeft -> { left = docPt.x; top = docPt.y }
        HandlePosition.TopCenter -> top = docPt.y
        HandlePosition.TopRight -> { top = docPt.y; right = docPt.x }
        HandlePosition.MiddleLeft -> left = docPt.x
        HandlePosition.MiddleRight -> right = docPt.x
        HandlePosition.BottomLeft -> { left = docPt.x; bottom = docPt.y }
        HandlePosition.BottomCenter -> bottom = docPt.y
        HandlePosition.BottomRight -> { right = docPt.x; bottom = docPt.y }
        HandlePosition.Rotate -> Unit
    }
    if (left >= right) left = right - 1f
    if (top >= bottom) top = bottom - 1f
    editorState.updateResizeBounds(RectF(left, top, right, bottom))
}
