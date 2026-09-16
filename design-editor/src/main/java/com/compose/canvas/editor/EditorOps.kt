package com.compose.canvas.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.compose.canvas.core.BoundsCalculator
import com.compose.canvas.core.FillKind
import com.compose.canvas.core.FillStyle
import com.compose.canvas.core.GroupObject
import com.compose.canvas.core.LinearGradientResource
import com.compose.canvas.core.ObjectSemantics
import com.compose.canvas.core.ObjectTransform
import com.compose.canvas.core.RadialGradientResource
import com.compose.canvas.core.SceneNode
import com.compose.canvas.core.StrokeStyle
import com.compose.canvas.core.VectorLayer
import com.compose.canvas.core.VectorObject
import com.compose.canvas.core.WorldTransform
import com.compose.canvas.geometry.GeometryToPath
import com.compose.canvas.geometry.ObjectId
import com.compose.canvas.geometry.PresetGeometry
import com.compose.canvas.geometry.RectF
import com.compose.canvas.geometry.TextGeometry
import com.compose.canvas.geometry.newObjectId
import com.compose.canvas.history.GenericCommand
import com.compose.canvas.math.TransformUtils
import com.compose.canvas.metadata.AnchorKind
import com.compose.canvas.metadata.ContentArea
import com.compose.canvas.metadata.NamedAnchor
import com.compose.canvas.metadata.Port
import com.compose.canvas.metadata.PortType
import com.compose.canvas.tools.path.BooleanOp
import com.compose.canvas.tools.path.BooleanOps
import com.compose.canvas.tools.path.HandleRef
import com.compose.canvas.tools.path.NodeRef
import com.compose.canvas.tools.path.NodeType
import com.compose.canvas.tools.path.PathSimplifier
import com.compose.canvas.tools.path.SegmentRef
import com.compose.canvas.tools.path.closeSubpath
import com.compose.canvas.tools.path.convertNode
import com.compose.canvas.tools.path.deleteNode
import com.compose.canvas.tools.path.dragHandle
import com.compose.canvas.tools.path.insertNodeOnSegment
import com.compose.canvas.tools.path.moveNode
import com.compose.canvas.tools.path.openSubpath
import com.compose.canvas.tools.path.pointsToPath
import com.compose.canvas.tools.path.toPathData
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private fun EditorState.commit(after: com.compose.canvas.core.VectorDocument, afterSel: SelectionState = selection, label: String) {
    applyCommand(GenericCommand(document, after, selection, afterSel, label))
}

val EditorState.canGroup: Boolean get() = selection.selectedIds.size >= 2 && !activeLayerLocked
val EditorState.canUngroup: Boolean
    get() = selection.selectedIds.any { document.findNode(it) is GroupObject } && !activeLayerLocked

fun EditorState.groupSelected() {
    if (!canGroup) return
    val layerIndex = activeLayerIndex.coerceIn(0, document.layers.lastIndex)
    val layer = document.layers[layerIndex]
    val children = layer.objects.filter { it.id in selection.selectedIds }
    if (children.size < 2) return
    val group = GroupObject(name = "Gruppe ${children.size}", children = children)
    var objects = layer.objects.filterNot { it.id in selection.selectedIds }
    objects = objects + group
    commit(
        document.replaceLayer(layerIndex, layer.copy(objects = objects)),
        SelectionState(setOf(group.id)),
        "Group"
    )
}

fun EditorState.ungroupSelected() {
    if (!canUngroup) return
    val layerIndex = activeLayerIndex.coerceIn(0, document.layers.lastIndex)
    var layer = document.layers[layerIndex]
    val childIds = mutableSetOf<ObjectId>()
    for (id in selection.selectedIds.toList()) {
        val idx = layer.objects.indexOfFirst { it.id == id }
        val group = layer.objects.getOrNull(idx) as? GroupObject ?: continue
        val baked = if (group.transform == ObjectTransform()) group.children else {
            group.children.map { child ->
                val world = TransformUtils.multiply(
                    TransformUtils.fromObjectTransform(group.transform),
                    TransformUtils.fromObjectTransform(child.transform)
                )
                child.withTransform(TransformUtils.decomposeObjectTransform(world))
            }
        }
        childIds += baked.map { it.id }
        val mutable = layer.objects.toMutableList()
        mutable.removeAt(idx)
        mutable.addAll(idx, baked)
        layer = layer.copy(objects = mutable)
    }
    commit(document.replaceLayer(layerIndex, layer), SelectionState(childIds), "Ungroup")
}

fun EditorState.copySelected() {
    val nodes = selection.selectedIds.mapNotNull { document.findNode(it) }.filter { !it.locked }
    if (nodes.isNotEmpty()) clipboard.set(nodes)
}

fun EditorState.cutSelected() {
    copySelected()
    deleteSelected()
}

fun EditorState.paste() {
    if (clipboard.isEmpty || activeLayerLocked) return
    var doc = document
    val ids = mutableSetOf<ObjectId>()
    for (node in clipboard.nodes) {
        val copy = node.deepCopy().let { n ->
            n.withTransform(n.transform.copy(x = n.transform.x + 20f, y = n.transform.y + 20f))
        }
        doc = doc.addObjectToLayer(activeLayerIndex, copy)
        ids += copy.id
    }
    commit(doc, SelectionState(ids), "Paste")
}

fun EditorState.alignSelected(mode: AlignmentMode) {
    if (selection.selectedIds.size < 2 || activeLayerLocked) return
    val layerIndex = activeLayerIndex.coerceIn(0, document.layers.lastIndex)
    val layer = document.layers[layerIndex]
    val selected = layer.objects.filter { it.id in selection.selectedIds }
    val aligned = AlignmentEngine.apply(selected, mode)
    val map = aligned.associateBy { it.id }
    commit(
        document.replaceLayer(layerIndex, layer.copy(objects = layer.objects.map { map[it.id] ?: it })),
        selection,
        mode.displayName
    )
}

fun EditorState.flipSelected(horizontal: Boolean) {
    if (selection.isEmpty || activeLayerLocked) return
    var doc = document
    for (id in selection.selectedIds) {
        val node = doc.findNode(id) ?: continue
        if (node.locked) continue
        val t = node.transform
        doc = doc.replaceObject(
            node.withTransform(
                if (horizontal) t.copy(scaleX = -t.scaleX) else t.copy(scaleY = -t.scaleY)
            )
        )
    }
    commit(doc, selection, if (horizontal) "Flip H" else "Flip V")
}

fun EditorState.convertSelectedToPath() {
    val obj = selectedObjects().singleOrNull() ?: return
    if (obj.locked) return
    val path = GeometryToPath.toPath(obj.geometry, obj.semantics.parameters)
    commit(document.replaceObject(obj.copy(geometry = path)), selection, "Convert to Path")
}

fun EditorState.addLayer() {
    commit(document.addLayer(), selection, "Add layer")
}

fun EditorState.removeLayer(index: Int) {
    if (document.layers.size <= 1) return
    commit(document.removeLayer(index), selection, "Remove layer")
}

fun EditorState.moveLayer(from: Int, to: Int) {
    commit(document.moveLayer(from, to), selection, "Move layer")
}

fun EditorState.updateLayer(index: Int, layer: VectorLayer) {
    commit(document.replaceLayer(index, layer), selection, "Edit layer")
}

fun EditorState.setActiveLayer(index: Int) {
    selectLayer(index)
}

fun EditorState.reorderObjectInLayer(from: Int, to: Int) {
    val i = activeLayerIndex.coerceIn(0, document.layers.lastIndex)
    val layer = document.layers[i]
    commit(document.replaceLayer(i, layer.moveObject(from, to)), selection, "Reorder")
}

fun EditorState.applyBooleanOp(op: BooleanOp, keepOriginals: Boolean = keepBooleanOriginals) {
    if (activeLayerLocked || selection.selectedIds.size < 2) return
    val objs = selection.selectedIds.mapNotNull { document.findObject(it) }
    if (objs.size < 2) return
    val worldPolys = objs.map { obj ->
        val m = WorldTransform.matrixOf(document, obj.id) ?: return
        BooleanOps.objectToWorldPolygon(obj) { m.transformPoint(it) }
    }
    var region = worldPolys.first()
    for (i in 1 until worldPolys.size) {
        region = BooleanOps.apply(region, worldPolys[i], op)
    }
    if (region.size < 3) return
    val primary = objs.first()
    val inverse = WorldTransform.matrixOf(document, primary.id)?.invert() ?: return
    val local = region.map { inverse.transformPoint(it) }
    var doc = document.replaceObject(primary.copy(geometry = BooleanOps.loopsToPath(listOf(local))))
    if (!keepOriginals) {
        objs.drop(1).forEach { doc = doc.removeObject(it.id) }
    }
    commit(doc, SelectionState(setOf(primary.id)), op.name)
}

fun EditorState.startPencil(point: Offset) {
    applyPencil(PencilDrawingState(true, listOf(point)))
}

fun EditorState.addPencilPoint(point: Offset) {
    if (!pencilDrawing.active) applyPencil(PencilDrawingState(true, listOf(point)))
    else applyPencil(pencilDrawing.copy(points = pencilDrawing.points + point))
}

fun EditorState.finishPencil() {
    val pts = pencilDrawing.points
    applyPencil(PencilDrawingState())
    if (pts.size < 2 || activeLayerLocked) return
    val simplified = PathSimplifier.simplify(pts, simplifyTolerance)
    val geom = pointsToPath(simplified, false)
    addObjectToActiveLayer(
        VectorObject(
            name = "Freihand",
            geometry = geom,
            fill = FillStyle(kind = FillKind.None),
            stroke = StrokeStyle(color = Color(0xFF006D3C), width = 2f)
        )
    )
}

fun EditorState.cancelPencil() { applyPencil(PencilDrawingState()) }

fun EditorState.simplifySelectedPath(tolerance: Float = simplifyTolerance) {
    val obj = selectedObjects().singleOrNull() ?: return
    val data = toPathData(obj.geometry)
    val sub = data.subpaths.map { s ->
        val pts = PathSimplifier.simplify(s.nodes.map { it.point }, tolerance)
        if (pts.size < 2) s else s.copy(nodes = pts.map { com.compose.canvas.tools.path.PathNode(it) })
    }
    commit(document.replaceObject(obj.copy(geometry = data.copy(subpaths = sub).toGeometry())), selection, "Simplify")
}

fun EditorState.enterPathEditing(id: ObjectId) {
    val obj = document.findNode(id) as? VectorObject ?: return
    applyPathEdit(PathEditState(active = true, objectId = id, data = toPathData(obj.geometry)))
}

fun EditorState.commitPathEdit() {
    val state = pathEdit
    if (!state.active) return
    val obj = state.objectId?.let { document.findNode(it) as? VectorObject }
    if (obj != null) {
        val edited = obj.copy(geometry = state.data.toGeometry())
        if (edited != obj) updateObject(edited)
    }
    applyPathEdit(PathEditState())
}

fun EditorState.abortPathEdit() { applyPathEdit(PathEditState()) }

val EditorState.pathEditWorldTransform
    get() = pathEdit.objectId?.let { WorldTransform.matrixOf(document, it) }
        ?: com.compose.canvas.math.TransformMatrix()

val EditorState.pathEditDragActive
    get() = pathEdit.dragTargetNode != null || pathEdit.dragTargetHandle != null

fun EditorState.startPathEditNodeDrag(ref: NodeRef, start: Offset) {
    applyPathEdit(pathEdit.copy(dragTargetNode = ref, dragTargetHandle = null, dragStartPoint = start))
}

fun EditorState.startPathEditHandleDrag(ref: HandleRef, start: Offset) {
    applyPathEdit(pathEdit.copy(dragTargetHandle = ref, dragTargetNode = null, dragStartPoint = start))
}

fun EditorState.updatePathEditDrag(local: Offset) {
    val state = pathEdit
    if (!state.active) return
    val handle = state.dragTargetHandle
    if (handle != null) {
        applyPathEdit(state.copy(data = state.data.dragHandle(handle, local), selectedHandle = handle))
        return
    }
    val target = state.dragTargetNode ?: return
    val start = state.dragStartPoint ?: local
    val node = state.data.node(target) ?: return
    applyPathEdit(state.copy(data = state.data.moveNode(target, node.point + (local - start)), selectedNode = target))
}

fun EditorState.finishPathEditDrag() {
    applyPathEdit(pathEdit.copy(dragTargetNode = null, dragTargetHandle = null, dragStartPoint = null))
}

fun EditorState.selectPathNode(ref: NodeRef?) {
    applyPathEdit(pathEdit.copy(selectedNode = ref, selectedHandle = null))
}

fun EditorState.selectPathHandle(ref: HandleRef?) {
    applyPathEdit(pathEdit.copy(selectedHandle = ref, selectedNode = ref?.nodeRef))
}

fun EditorState.convertSelectedNode(type: NodeType) {
    val ref = pathEdit.selectedNode ?: return
    applyPathEdit(pathEdit.copy(data = pathEdit.data.convertNode(ref, type)))
}

fun EditorState.deleteSelectedNode() {
    val ref = pathEdit.selectedNode ?: return
    applyPathEdit(pathEdit.copy(data = pathEdit.data.deleteNode(ref), selectedNode = null, selectedHandle = null))
}

fun EditorState.insertPathNodeOnSegment(ref: SegmentRef, t: Float = 0.5f) {
    applyPathEdit(pathEdit.copy(data = pathEdit.data.insertNodeOnSegment(ref, t), selectedNode = NodeRef(ref.subpathIndex, ref.nodeIndex + 1)))
}

fun EditorState.toggleSelectedPathClosed() {
    val idx = pathEdit.selectedNode?.subpathIndex ?: return
    val sub = pathEdit.data.subpaths.getOrNull(idx) ?: return
    applyPathEdit(
        pathEdit.copy(
            data = if (sub.closed) pathEdit.data.openSubpath(idx) else pathEdit.data.closeSubpath(idx)
        )
    )
}

fun EditorState.startMarquee(start: Offset) = applyMarquee(MarqueeState(true, start, start))
fun EditorState.updateMarquee(current: Offset) = applyMarquee(marquee.copy(current = current))
fun EditorState.finishMarquee() {
    if (!marquee.active) return
    val r = RectF(
        minOf(marquee.start.x, marquee.current.x),
        minOf(marquee.start.y, marquee.current.y),
        maxOf(marquee.start.x, marquee.current.x),
        maxOf(marquee.start.y, marquee.current.y)
    )
    applyMarquee(MarqueeState())
    if (r.width < 4f && r.height < 4f) return
    val ids = document.allNodes().filter { node ->
        val b = BoundsCalculator.of(node) ?: return@filter false
        b.left < r.right && b.right > r.left && b.top < r.bottom && b.bottom > r.top
    }.map { it.id }.toSet()
    restoreSelection(SelectionState(ids))
}

fun EditorState.startRotate(center: Offset, startAngle: Float) {
    applyRotate(RotateSession(true, center, startAngle, startAngle))
}

fun EditorState.updateRotate(currentAngle: Float) {
    applyRotate(rotateSession.copy(currentAngle = currentAngle))
}

fun EditorState.finishRotate() {
    val session = rotateSession
    applyRotate(RotateSession())
    if (!session.active) return
    val delta = session.currentAngle - session.startAngle
    if (kotlin.math.abs(delta) < 0.2f) return
    var doc = document
    for (id in selection.selectedIds) {
        val node = doc.findNode(id) ?: continue
        if (node.locked) continue
        doc = doc.replaceObject(node.withTransform(node.transform.copy(rotation = node.transform.rotation + delta)))
    }
    commit(doc, selection, "Rotate")
}

fun EditorState.snapPoint(raw: Offset): Offset {
    val (snapped, guides) = SnapEngine.snap(raw, document, gridSettings, selection.selectedIds, 10f)
    applySnapGuides(guides)
    return snapped
}

fun EditorState.clearSnapGuides() = applySnapGuides(emptyList())

fun EditorState.addText(at: Offset) {
    addObjectToActiveLayer(
        VectorObject(
            name = "Text",
            geometry = TextGeometry("Text"),
            transform = ObjectTransform(x = at.x, y = at.y),
            fill = FillStyle(color = Color.Black)
        )
    )
}

fun EditorState.insertPreset(presetId: String) {
    val params = when (presetId) {
        "logicBlock" -> mapOf(
            "cornerRadius" to 12f, "notchWidth" to 18f, "notchDepth" to 6f,
            "connectorWidth" to 16f, "connectorDepth" to 8f
        )
        "cookie" -> mapOf("sides" to 8f, "innerRatio" to 0.82f)
        else -> emptyMap()
    }
    addObjectToActiveLayer(
        VectorObject(
            name = presetId,
            geometry = PresetGeometry(presetId, 240f, 120f, params),
            transform = ObjectTransform(x = 80f, y = 80f),
            fill = FillStyle(color = Color(0xFF6750A4)),
            semantics = ObjectSemantics(parameters = params)
        )
    )
}

fun EditorState.addPort(local: Offset, type: PortType = PortType.CUSTOM) {
    val obj = selectedObjects().singleOrNull() ?: return
    val port = Port(name = type.name.lowercase(), x = local.x, y = local.y, portType = type)
    updateObject(obj.copy(semantics = obj.semantics.copy(ports = obj.semantics.ports + port)))
}

fun EditorState.addAnchor(local: Offset, kind: AnchorKind = AnchorKind.CUSTOM) {
    val obj = selectedObjects().singleOrNull() ?: return
    val a = NamedAnchor(name = kind.name.lowercase(), x = local.x, y = local.y, kind = kind)
    updateObject(obj.copy(semantics = obj.semantics.copy(anchors = obj.semantics.anchors + a)))
}

fun EditorState.addContentArea(bounds: RectF) {
    val obj = selectedObjects().singleOrNull() ?: return
    val area = ContentArea(name = "content", bounds = bounds, contentType = "child")
    updateObject(obj.copy(semantics = obj.semantics.copy(contentAreas = obj.semantics.contentAreas + area)))
}

fun EditorState.setObjectParameter(key: String, value: Float) {
    val obj = selectedObjects().singleOrNull() ?: return
    val params = obj.semantics.parameters + (key to value)
    val geom = obj.geometry
    val newGeom = when (geom) {
        is PresetGeometry -> geom.copy(parameters = geom.parameters + (key to value))
        is com.compose.canvas.geometry.RectGeometry -> when (key) {
            "width" -> geom.copy(width = value)
            "height" -> geom.copy(height = value)
            "cornerRadius" -> geom.copy(cornerRadius = value)
            else -> geom
        }
        else -> geom
    }
    updateObject(obj.copy(geometry = newGeom, semantics = obj.semantics.copy(parameters = params)))
}

fun EditorState.applyLinearGradient() {
    val obj = selectedObjects().singleOrNull() ?: return
    val g = LinearGradientResource()
    val after = document.copy(definitions = document.definitions.upsert(g))
        .replaceObject(obj.copy(fill = FillStyle(kind = FillKind.LinearGradient, color = obj.fill.color, gradientId = g.id)))
    commit(after, selection, "Linear gradient")
}

fun EditorState.applyRadialGradient() {
    val obj = selectedObjects().singleOrNull() ?: return
    val g = RadialGradientResource()
    val after = document.copy(definitions = document.definitions.upsert(g))
        .replaceObject(obj.copy(fill = FillStyle(kind = FillKind.RadialGradient, color = obj.fill.color, gradientId = g.id)))
    commit(after, selection, "Radial gradient")
}

fun EditorState.updateGradient(resource: com.compose.canvas.core.GradientResource) {
    commit(document.copy(definitions = document.definitions.upsert(resource)), selection, "Edit gradient")
}

private operator fun Offset.plus(o: Offset) = Offset(x + o.x, y + o.y)
private operator fun Offset.minus(o: Offset) = Offset(x - o.x, y - o.y)
