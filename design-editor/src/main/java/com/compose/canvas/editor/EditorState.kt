package com.compose.canvas.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.compose.canvas.core.FillStyle
import com.compose.canvas.core.ObjectTransform
import com.compose.canvas.core.SceneNode
import com.compose.canvas.core.StrokeStyle
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorObject
import com.compose.canvas.geometry.EllipseGeometry
import com.compose.canvas.geometry.LineGeometry
import com.compose.canvas.geometry.ObjectId
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import com.compose.canvas.geometry.RectF
import com.compose.canvas.geometry.RectGeometry
import com.compose.canvas.history.AddObjectCommand
import com.compose.canvas.history.CommandHistory
import com.compose.canvas.history.DuplicateObjectCommand
import com.compose.canvas.history.EditorCommand
import com.compose.canvas.history.MoveObjectCommand
import com.compose.canvas.history.RemoveObjectCommand
import com.compose.canvas.history.ReplaceObjectCommand
import com.compose.canvas.history.ResizeObjectCommand
import kotlin.math.abs

class EditorState(
    initialDocument: VectorDocument = VectorDocument()
) {
    var document: VectorDocument by mutableStateOf(initialDocument)
        private set

    var selection: SelectionState by mutableStateOf(SelectionState())
        private set

    var activeTool: EditorTool by mutableStateOf(EditorTool.Select)
        private set

    var activeLayerIndex by mutableIntStateOf(0)
        private set

    val history = CommandHistory()

    var dragState: DragState by mutableStateOf(DragState())
        private set

    var createShapeState: CreateShapeState by mutableStateOf(CreateShapeState())
        private set

    var pathDrawingState: PathDrawingState by mutableStateOf(PathDrawingState())
        private set

    var resizeSession: ResizeSession by mutableStateOf(ResizeSession())
        private set

    var gridVisible by mutableStateOf(true)
    var gridSize by mutableStateOf(50f)
    var snapEnabled by mutableStateOf(true)
    var panelState by mutableStateOf(PanelState())
    val clipboard = ClipboardState()
    var pathEdit: PathEditState by mutableStateOf(PathEditState())
        private set
    var pencilDrawing: PencilDrawingState by mutableStateOf(PencilDrawingState())
        private set
    var marquee: MarqueeState by mutableStateOf(MarqueeState())
        private set
    var rotateSession: RotateSession by mutableStateOf(RotateSession())
        private set
    var snapGuides: List<SnapResult> by mutableStateOf(emptyList())
        private set
    var recentColors: List<Color> by mutableStateOf(
        listOf(Color(0xFF6750A4), Color(0xFF006D3C), Color.White, Color.Black)
    )
        private set
    var previewMode by mutableStateOf(false)
    var simplifyTolerance by mutableStateOf(4f)
    var keepBooleanOriginals by mutableStateOf(false)

    fun applyPathEdit(state: PathEditState) { pathEdit = state }
    fun applyPencil(state: PencilDrawingState) { pencilDrawing = state }
    fun applyMarquee(state: MarqueeState) { marquee = state }
    fun applyRotate(state: RotateSession) { rotateSession = state }
    internal fun applySnapGuides(guides: List<SnapResult>) { snapGuides = guides }
    fun rememberColor(color: Color) {
        if (color == Color.Unspecified) return
        recentColors = (listOf(color) + recentColors.filterNot { it == color }).take(8)
    }

    val gridSettings: GridSettings
        get() = GridSettings(enabled = gridVisible, size = gridSize, snap = snapEnabled)

    val resizing: Boolean get() = resizeSession.active
    val resizeHandle: HandlePosition? get() = resizeSession.handle
    val resizeStartBounds: RectF? get() = resizeSession.startBounds
    val currentResizeBounds: RectF? get() = resizeSession.currentBounds

    fun displayDocument(): VectorDocument =
        TransientDocument.forDisplay(document, selection, dragState, resizeSession, rotateSession)

    fun replaceDocument(doc: VectorDocument) {
        document = doc
        selection = SelectionState()
        history.clear()
        abortPathEdit()
        cancelPath()
        cancelPencil()
        resetDrag()
        resetResize()
        createShapeState = CreateShapeState()
        marquee = MarqueeState()
        rotateSession = RotateSession()
        snapGuides = emptyList()
        previewMode = false
        activeLayerIndex = 0
    }

    val activeLayerLocked: Boolean
        get() = document.layers.getOrNull(activeLayerIndex)?.locked == true

    internal fun restoreDocument(doc: VectorDocument) {
        document = doc
    }

    internal fun restoreSelection(sel: SelectionState) {
        selection = sel
    }

    internal fun applyCommand(cmd: EditorCommand) {
        history.push(cmd)
        cmd.redo(this)
    }

    fun setTool(tool: EditorTool) {
        if (pathEdit.active && tool != EditorTool.EditPath) commitPathEdit()
        activeTool = tool
        if (tool == EditorTool.Rectangle || tool == EditorTool.Ellipse ||
            tool == EditorTool.Line || tool == EditorTool.Path ||
            tool == EditorTool.Polygon || tool == EditorTool.Star ||
            tool == EditorTool.Triangle || tool == EditorTool.Pencil || tool == EditorTool.Text
        ) {
            selection = selection.clear()
        }
        if (tool != EditorTool.Path) pathDrawingState = PathDrawingState()
        if (tool != EditorTool.Pencil) pencilDrawing = PencilDrawingState()
        if (tool == EditorTool.EditPath && selection.isSingle) enterPathEditing(selection.singleId!!)
        resetDrag()
        resetResize()
        createShapeState = CreateShapeState()
        marquee = MarqueeState()
        rotateSession = RotateSession()
    }

    fun selectObject(id: ObjectId, addToSelection: Boolean = false) {
        selection = if (addToSelection) selection.toggleSelection(id) else selection.select(id)
    }

    fun clearSelection() {
        selection = selection.clear()
    }

    fun selectedNodes(): List<SceneNode> =
        document.allNodes().filter { it.id in selection.selectedIds }

    fun selectedObjects(): List<VectorObject> =
        document.allObjects().filter { it.id in selection.selectedIds }

    fun undo(): Boolean {
        val cmd = history.popUndo() ?: return false
        cmd.undo(this)
        return true
    }

    fun redo(): Boolean {
        val cmd = history.popRedo() ?: return false
        cmd.redo(this)
        return true
    }

    fun addObjectToActiveLayer(obj: SceneNode) {
        if (activeLayerLocked) return
        val before = document
        val beforeSel = selection
        val layerIndex = activeLayerIndex.coerceIn(0, document.layers.lastIndex)
        val afterDoc = document.addObjectToLayer(layerIndex, obj)
        val afterSel = SelectionState(selectedIds = setOf(obj.id))
        applyCommand(AddObjectCommand(obj, layerIndex, before, afterDoc, beforeSel, afterSel))
    }

    fun deleteSelected() = deleteObjects(selection.selectedIds)

    fun deleteObject(id: ObjectId) = deleteObjects(listOf(id))

    fun deleteObjects(ids: Collection<ObjectId>) {
        if (ids.isEmpty() || activeLayerLocked) return
        val removable = ids.filter { id -> document.findNode(id)?.locked != true }
        if (removable.isEmpty()) return
        val before = document
        val beforeSel = selection
        var doc = document
        for (id in removable) doc = doc.removeObject(id)
        val afterSel = SelectionState(beforeSel.selectedIds - removable.toSet())
        applyCommand(RemoveObjectCommand(removable, before, doc, beforeSel, afterSel))
    }

    fun duplicateSelected() {
        if (selection.isEmpty || activeLayerLocked) return
        val before = document
        val beforeSel = selection
        val newIds = mutableListOf<ObjectId>()
        var doc = document
        val layerIndex = activeLayerIndex.coerceIn(0, document.layers.lastIndex)
        for (id in selection.selectedIds) {
            val node = doc.findNode(id) ?: continue
            if (node.locked) continue
            val copied = offsetNode(node.deepCopy(), 20f, 20f)
            doc = doc.addObjectToLayer(layerIndex, copied)
            newIds.add(copied.id)
        }
        if (newIds.isEmpty()) return
        applyCommand(
            DuplicateObjectCommand(before, doc, beforeSel, SelectionState(newIds.toSet()))
        )
    }

    fun updateObject(obj: VectorObject) {
        val old = document.findNode(obj.id) ?: return
        if (old == obj) return
        applyCommand(
            ReplaceObjectCommand(
                beforeDocument = document,
                afterDocument = document.replaceObject(obj),
                undoSelection = selection,
                redoSelection = selection,
                label = "Edit ${obj.name}"
            )
        )
    }

    fun updateNode(node: SceneNode) {
        val old = document.findNode(node.id) ?: return
        if (old == node) return
        applyCommand(
            ReplaceObjectCommand(
                beforeDocument = document,
                afterDocument = document.replaceObject(node),
                undoSelection = selection,
                redoSelection = selection
            )
        )
    }

    fun startDrag(startDoc: Offset) {
        dragState = DragState(active = true, startDocPoint = startDoc, currentDocPoint = startDoc)
    }

    fun updateDrag(currentDoc: Offset) {
        dragState = dragState.copy(currentDocPoint = currentDoc)
    }

    fun endDrag() {
        val delta = dragState.deltaDoc
        if (dragState.active && (delta.x != 0f || delta.y != 0f) && !activeLayerLocked) {
            val before = document
            var doc = document
            for (id in selection.selectedIds) {
                val node = doc.findNode(id) ?: continue
                if (node.locked) continue
                doc = doc.replaceObject(offsetNode(node, delta.x, delta.y))
            }
            if (doc != document) {
                applyCommand(MoveObjectCommand(delta.x, delta.y, before, doc, selection, selection))
            }
        }
        dragState = DragState()
    }

    fun resetDrag() {
        dragState = DragState()
    }

    fun startCreateShape(type: GeometryType, startDoc: Offset) {
        createShapeState = CreateShapeState(
            active = true,
            geometryType = type,
            startDocPoint = startDoc,
            currentDocPoint = startDoc
        )
    }

    fun updateCreateShape(currentDoc: Offset) {
        createShapeState = createShapeState.copy(currentDocPoint = currentDoc)
    }

    fun finishCreateShape(): VectorObject? {
        val state = createShapeState
        if (!state.active) return null
        createShapeState = CreateShapeState()
        if (activeLayerLocked) return null

        val start = state.startDocPoint
        val current = state.currentDocPoint
        val dx = current.x - start.x
        val dy = current.y - start.y
        val left = minOf(start.x, current.x)
        val top = minOf(start.y, current.y)
        val w = abs(dx)
        val h = abs(dy)
        if (w < 2f && h < 2f) return null

        val obj = when (state.geometryType) {
            GeometryType.Rectangle -> VectorObject(
                name = "Rectangle",
                geometry = RectGeometry(width = w, height = h),
                transform = ObjectTransform(x = left, y = top),
                fill = defaultFill()
            )
            GeometryType.Ellipse -> VectorObject(
                name = "Ellipse",
                geometry = EllipseGeometry(radiusX = w / 2f, radiusY = h / 2f),
                transform = ObjectTransform(x = left + w / 2f, y = top + h / 2f),
                fill = defaultFill()
            )
            GeometryType.Line -> VectorObject(
                name = "Line",
                geometry = LineGeometry(0f, 0f, dx, dy),
                transform = ObjectTransform(x = start.x, y = start.y),
                fill = FillStyle(kind = com.compose.canvas.core.FillKind.None),
                stroke = StrokeStyle(color = Color(0xFF6750A4), width = 2f)
            )
            GeometryType.Triangle -> VectorObject(
                name = "Triangle",
                geometry = com.compose.canvas.geometry.TriangleGeometry(w, h),
                transform = ObjectTransform(x = left, y = top),
                fill = defaultFill()
            )
            GeometryType.Polygon -> VectorObject(
                name = "Polygon",
                geometry = com.compose.canvas.geometry.PolygonGeometry(6, minOf(w, h) / 2f),
                transform = ObjectTransform(x = left + w / 2f, y = top + h / 2f),
                fill = defaultFill()
            )
            GeometryType.Star -> VectorObject(
                name = "Star",
                geometry = com.compose.canvas.geometry.StarGeometry(5, minOf(w, h) / 2f, minOf(w, h) / 4f),
                transform = ObjectTransform(x = left + w / 2f, y = top + h / 2f),
                fill = defaultFill()
            )
        }
        addObjectToActiveLayer(obj)
        return obj
    }

    fun startPath(point: Offset) {
        pathDrawingState = if (!pathDrawingState.active) {
            PathDrawingState(active = true, points = listOf(point))
        } else {
            pathDrawingState.copy(points = pathDrawingState.points + point)
        }
    }

    fun finishPath(closed: Boolean = false) {
        if (!pathDrawingState.active) return
        val points = pathDrawingState.points
        pathDrawingState = PathDrawingState()
        if (points.size < 2 || activeLayerLocked) return
        val segments = buildList {
            add(PathSegment.MoveTo(points.first().x, points.first().y))
            for (i in 1 until points.size) {
                add(PathSegment.LineTo(points[i].x, points[i].y))
            }
            if (closed) add(PathSegment.Close)
        }
        val obj = VectorObject(
            name = "Path",
            geometry = PathGeometry(segments = segments, closed = closed),
            fill = if (closed) defaultFill() else FillStyle(),
            stroke = StrokeStyle(color = Color(0xFF006D3C), width = 2f)
        )
        addObjectToActiveLayer(obj)
    }

    fun cancelPath() {
        pathDrawingState = PathDrawingState()
    }

    fun cancelCreateShape() {
        createShapeState = CreateShapeState()
    }

    fun startResize(handle: HandlePosition, bounds: RectF) {
        resizeSession = ResizeSession(
            active = true,
            handle = handle,
            startBounds = bounds,
            currentBounds = bounds
        )
    }

    fun updateResizeBounds(newBounds: RectF) {
        resizeSession = resizeSession.copy(currentBounds = newBounds)
    }

    fun finishResize(newBounds: RectF) {
        if (!resizeSession.active || selection.isEmpty) {
            resetResize()
            return
        }
        val oldBounds = resizeSession.startBounds ?: run {
            resetResize()
            return
        }
        if (oldBounds.width < 0.01f || oldBounds.height < 0.01f) {
            resetResize()
            return
        }
        val before = document
        var doc = document
        for (id in selection.selectedIds) {
            val node = doc.findNode(id) ?: continue
            if (node.locked) continue
            val newTransform = TransientDocument.resizeTransform(node.transform, oldBounds, newBounds)
            doc = doc.replaceObject(node.withTransform(newTransform))
        }
        if (doc != before) {
            applyCommand(ResizeObjectCommand(before, doc, selection, selection))
        }
        resetResize()
    }

    fun resetResize() {
        resizeSession = ResizeSession()
    }

    fun selectLayer(index: Int) {
        if (index in document.layers.indices) activeLayerIndex = index
    }

    fun setGridEnabled(enabled: Boolean) {
        gridVisible = enabled
    }

    fun updateGridSize(size: Float) {
        gridSize = size.coerceAtLeast(4f)
    }

    private fun offsetNode(node: SceneNode, dx: Float, dy: Float): SceneNode {
        val t = node.transform
        return node.withTransform(t.copy(x = t.x + dx, y = t.y + dy))
    }

    private fun defaultFill() = FillStyle(color = Color(0xFF6750A4)).also { rememberColor(it.color) }
}
