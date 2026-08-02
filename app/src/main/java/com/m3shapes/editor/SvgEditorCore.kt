package com.m3shapes.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.UUID

private const val EPSILON = 0.0001f

data class SvgStyle(
    val fill: Color = Color(0xFF6750A4),
    val stroke: Color = Color(0xFF1F1F1F),
    val strokeWidth: Float = 2f
)

sealed interface SvgElement {
    val id: String
    val name: String
    val zIndex: Int
    val visible: Boolean
    val locked: Boolean
    val groupId: String?
    val style: SvgStyle

    fun withMeta(
        name: String = this.name,
        zIndex: Int = this.zIndex,
        visible: Boolean = this.visible,
        locked: Boolean = this.locked,
        groupId: String? = this.groupId,
        style: SvgStyle = this.style
    ): SvgElement

    fun moveBy(dx: Float, dy: Float): SvgElement
    fun scaleAround(center: Offset, factor: Float): SvgElement
    fun rotateAround(center: Offset, degrees: Float): SvgElement
    fun bounds(): SvgBounds
    fun hitTest(point: Offset, radius: Float = 12f): Boolean
}

data class SvgRectElement(
    override val id: String = newSvgId(),
    override val name: String = "Rect",
    override val zIndex: Int = 0,
    override val visible: Boolean = true,
    override val locked: Boolean = false,
    override val groupId: String? = null,
    override val style: SvgStyle = SvgStyle(),
    val x: Float = 56f,
    val y: Float = 56f,
    val width: Float = 92f,
    val height: Float = 92f,
    val rotation: Float = 0f
) : SvgElement {
    override fun withMeta(
        name: String,
        zIndex: Int,
        visible: Boolean,
        locked: Boolean,
        groupId: String?,
        style: SvgStyle
    ): SvgRectElement = copy(
        name = name,
        zIndex = zIndex,
        visible = visible,
        locked = locked,
        groupId = groupId,
        style = style
    )

    override fun moveBy(dx: Float, dy: Float): SvgRectElement = copy(x = x + dx, y = y + dy)

    override fun scaleAround(center: Offset, factor: Float): SvgRectElement {
        val currentCenter = Offset(x + width / 2f, y + height / 2f)
        val scaledCenter = scalePoint(currentCenter, center, factor)
        val w = (width * factor).coerceAtLeast(4f)
        val h = (height * factor).coerceAtLeast(4f)
        return copy(
            x = scaledCenter.x - w / 2f,
            y = scaledCenter.y - h / 2f,
            width = w,
            height = h
        )
    }

    override fun rotateAround(center: Offset, degrees: Float): SvgRectElement {
        val c = Offset(x + width / 2f, y + height / 2f)
        val rotatedCenter = rotatePoint(c, center, degrees)
        return copy(
            x = rotatedCenter.x - width / 2f,
            y = rotatedCenter.y - height / 2f,
            rotation = (rotation + degrees) % 360f
        )
    }

    override fun bounds(): SvgBounds = SvgBounds(x, y, x + width, y + height)

    override fun hitTest(point: Offset, radius: Float): Boolean =
        point.x >= x - radius &&
            point.x <= x + width + radius &&
            point.y >= y - radius &&
            point.y <= y + height + radius
}

data class SvgCircleElement(
    override val id: String = newSvgId(),
    override val name: String = "Circle",
    override val zIndex: Int = 0,
    override val visible: Boolean = true,
    override val locked: Boolean = false,
    override val groupId: String? = null,
    override val style: SvgStyle = SvgStyle(),
    val cx: Float = 128f,
    val cy: Float = 128f,
    val radius: Float = 46f
) : SvgElement {
    override fun withMeta(
        name: String,
        zIndex: Int,
        visible: Boolean,
        locked: Boolean,
        groupId: String?,
        style: SvgStyle
    ): SvgCircleElement = copy(
        name = name,
        zIndex = zIndex,
        visible = visible,
        locked = locked,
        groupId = groupId,
        style = style
    )

    override fun moveBy(dx: Float, dy: Float): SvgCircleElement = copy(cx = cx + dx, cy = cy + dy)

    override fun scaleAround(center: Offset, factor: Float): SvgCircleElement {
        val c = scalePoint(Offset(cx, cy), center, factor)
        return copy(cx = c.x, cy = c.y, radius = (radius * factor).coerceAtLeast(3f))
    }

    override fun rotateAround(center: Offset, degrees: Float): SvgCircleElement {
        val c = rotatePoint(Offset(cx, cy), center, degrees)
        return copy(cx = c.x, cy = c.y)
    }

    override fun bounds(): SvgBounds = SvgBounds(cx - radius, cy - radius, cx + radius, cy + radius)

    override fun hitTest(point: Offset, radius: Float): Boolean {
        val d = distance(point, Offset(cx, cy))
        return d <= this.radius + radius
    }
}

data class SvgPolygonElement(
    override val id: String = newSvgId(),
    override val name: String = "Polygon",
    override val zIndex: Int = 0,
    override val visible: Boolean = true,
    override val locked: Boolean = false,
    override val groupId: String? = null,
    override val style: SvgStyle = SvgStyle(),
    val points: List<Offset> = listOf(
        Offset(64f, 64f),
        Offset(164f, 74f),
        Offset(128f, 168f)
    )
) : SvgElement {
    override fun withMeta(
        name: String,
        zIndex: Int,
        visible: Boolean,
        locked: Boolean,
        groupId: String?,
        style: SvgStyle
    ): SvgPolygonElement = copy(
        name = name,
        zIndex = zIndex,
        visible = visible,
        locked = locked,
        groupId = groupId,
        style = style
    )

    override fun moveBy(dx: Float, dy: Float): SvgPolygonElement =
        copy(points = points.map { it + Offset(dx, dy) })

    override fun scaleAround(center: Offset, factor: Float): SvgPolygonElement =
        copy(points = points.map { scalePoint(it, center, factor) })

    override fun rotateAround(center: Offset, degrees: Float): SvgPolygonElement =
        copy(points = points.map { rotatePoint(it, center, degrees) })

    override fun bounds(): SvgBounds = boundsFromPoints(points)

    override fun hitTest(point: Offset, radius: Float): Boolean {
        if (points.isEmpty()) return false
        if (pointInPolygon(point, points)) return true
        return points.any { distance(it, point) <= radius }
    }
}

enum class SvgPathSegmentType { Move, Line, Cubic }

data class SvgPathNode(
    val id: String = newSvgId(),
    val anchor: Offset,
    val inHandle: Offset? = null,
    val outHandle: Offset? = null,
    val type: SvgPathSegmentType = SvgPathSegmentType.Line
)

data class SvgPathElement(
    override val id: String = newSvgId(),
    override val name: String = "Path",
    override val zIndex: Int = 0,
    override val visible: Boolean = true,
    override val locked: Boolean = false,
    override val groupId: String? = null,
    override val style: SvgStyle = SvgStyle(),
    val closed: Boolean = true,
    val nodes: List<SvgPathNode> = listOf(
        SvgPathNode(anchor = Offset(72f, 72f), type = SvgPathSegmentType.Move),
        SvgPathNode(anchor = Offset(172f, 72f)),
        SvgPathNode(anchor = Offset(172f, 172f)),
        SvgPathNode(anchor = Offset(72f, 172f))
    )
) : SvgElement {
    override fun withMeta(
        name: String,
        zIndex: Int,
        visible: Boolean,
        locked: Boolean,
        groupId: String?,
        style: SvgStyle
    ): SvgPathElement = copy(
        name = name,
        zIndex = zIndex,
        visible = visible,
        locked = locked,
        groupId = groupId,
        style = style
    )

    override fun moveBy(dx: Float, dy: Float): SvgPathElement = copy(
        nodes = nodes.map { node ->
            node.copy(
                anchor = node.anchor + Offset(dx, dy),
                inHandle = node.inHandle?.plus(Offset(dx, dy)),
                outHandle = node.outHandle?.plus(Offset(dx, dy))
            )
        }
    )

    override fun scaleAround(center: Offset, factor: Float): SvgPathElement = copy(
        nodes = nodes.map { node ->
            node.copy(
                anchor = scalePoint(node.anchor, center, factor),
                inHandle = node.inHandle?.let { scalePoint(it, center, factor) },
                outHandle = node.outHandle?.let { scalePoint(it, center, factor) }
            )
        }
    )

    override fun rotateAround(center: Offset, degrees: Float): SvgPathElement = copy(
        nodes = nodes.map { node ->
            node.copy(
                anchor = rotatePoint(node.anchor, center, degrees),
                inHandle = node.inHandle?.let { rotatePoint(it, center, degrees) },
                outHandle = node.outHandle?.let { rotatePoint(it, center, degrees) }
            )
        }
    )

    override fun bounds(): SvgBounds = boundsFromPoints(
        nodes.flatMap { n ->
            listOfNotNull(n.anchor, n.inHandle, n.outHandle)
        }
    )

    override fun hitTest(point: Offset, radius: Float): Boolean {
        if (nodes.isEmpty()) return false
        return nodes.any { distance(point, it.anchor) <= radius * 1.25f } || pointInPolygon(
            point,
            nodes.map { it.anchor }
        )
    }
}

data class SvgBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val center: Offset get() = Offset((left + right) / 2f, (top + bottom) / 2f)

    fun inflate(padding: Float): SvgBounds = SvgBounds(
        left = left - padding,
        top = top - padding,
        right = right + padding,
        bottom = bottom + padding
    )
}

enum class SvgTool {
    Select,
    Rect,
    Circle,
    Polygon,
    Path,
    PathNodes
}

data class SvgEditorUiState(
    val tool: SvgTool = SvgTool.Select,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val selectedNodeElementId: String? = null,
    val selectedNodeId: String? = null,
    val diagnostics: List<String> = emptyList()
)

data class SvgDocument(
    val width: Float = 256f,
    val height: Float = 256f,
    val elements: List<SvgElement> = emptyList(),
    val selectedElementIds: Set<String> = emptySet(),
    val groups: Set<String> = emptySet()
) {
    fun selectedElements(): List<SvgElement> = elements.filter { it.id in selectedElementIds }
}

data class SvgCommandEntry(
    val label: String,
    val before: SvgDocument,
    val after: SvgDocument
)

data class SvgCommandHistory(
    val present: SvgDocument = SvgDocument(),
    val past: List<SvgCommandEntry> = emptyList(),
    val future: List<SvgCommandEntry> = emptyList()
) {
    fun execute(label: String, reducer: (SvgDocument) -> SvgDocument): SvgCommandHistory {
        val before = present
        val after = reducer(before)
        if (before == after) return this
        val entry = SvgCommandEntry(label = label, before = before, after = after)
        return copy(
            present = after,
            past = past + entry,
            future = emptyList()
        )
    }

    fun undo(): SvgCommandHistory {
        val entry = past.lastOrNull() ?: return this
        return copy(
            present = entry.before,
            past = past.dropLast(1),
            future = listOf(entry) + future
        )
    }

    fun redo(): SvgCommandHistory {
        val entry = future.firstOrNull() ?: return this
        return copy(
            present = entry.after,
            past = past + entry,
            future = future.drop(1)
        )
    }
}

fun newSvgId(): String = UUID.randomUUID().toString().replace("-", "").take(10)

fun SvgDocument.withElements(newElements: List<SvgElement>): SvgDocument {
    val sorted = newElements.sortedBy { it.zIndex }.mapIndexed { index, element ->
        element.withMeta(zIndex = index)
    }
    return copy(elements = sorted)
}

fun SvgDocument.addElement(element: SvgElement): SvgDocument {
    val normalized = element.withMeta(zIndex = elements.size)
    return withElements(elements + normalized).copy(
        selectedElementIds = setOf(normalized.id)
    )
}

fun SvgDocument.replaceElement(updated: SvgElement): SvgDocument = withElements(
    elements.map { if (it.id == updated.id) updated else it }
)

fun SvgDocument.removeSelected(): SvgDocument {
    if (selectedElementIds.isEmpty()) return this
    val remaining = elements.filterNot { it.id in selectedElementIds }
    return withElements(remaining).copy(selectedElementIds = emptySet())
}

fun SvgDocument.selectTopMost(point: Offset): SvgDocument {
    val hit = elements
        .asReversed()
        .firstOrNull { it.visible && !it.locked && it.hitTest(point) }
    return copy(selectedElementIds = hit?.let { setOf(it.id) } ?: emptySet())
}

fun SvgDocument.selectById(id: String, additive: Boolean = false): SvgDocument {
    if (elements.none { it.id == id }) return this
    val next = if (additive) {
        if (id in selectedElementIds) selectedElementIds - id else selectedElementIds + id
    } else {
        setOf(id)
    }
    return copy(selectedElementIds = next)
}

fun SvgDocument.moveSelection(dx: Float, dy: Float): SvgDocument = withElements(
    elements.map { if (it.id in selectedElementIds && !it.locked) it.moveBy(dx, dy) else it }
)

fun SvgDocument.scaleSelection(factor: Float): SvgDocument {
    val selected = selectedElements().filterNot { it.locked }
    if (selected.isEmpty()) return this
    val center = boundsOf(selected).center
    return withElements(
        elements.map { if (it.id in selectedElementIds && !it.locked) it.scaleAround(center, factor) else it }
    )
}

fun SvgDocument.rotateSelection(degrees: Float): SvgDocument {
    val selected = selectedElements().filterNot { it.locked }
    if (selected.isEmpty()) return this
    val center = boundsOf(selected).center
    return withElements(
        elements.map { if (it.id in selectedElementIds && !it.locked) it.rotateAround(center, degrees) else it }
    )
}

fun SvgDocument.toggleVisibility(id: String): SvgDocument = replaceElementMeta(id) {
    it.withMeta(visible = !it.visible)
}

fun SvgDocument.toggleLock(id: String): SvgDocument = replaceElementMeta(id) {
    it.withMeta(locked = !it.locked)
}

fun SvgDocument.bringSelectionForward(): SvgDocument {
    if (selectedElementIds.isEmpty()) return this
    val mutable = elements.toMutableList()
    for (index in mutable.lastIndex - 1 downTo 0) {
        val current = mutable[index]
        val next = mutable[index + 1]
        if (current.id in selectedElementIds && next.id !in selectedElementIds) {
            mutable[index] = next
            mutable[index + 1] = current
        }
    }
    return withElements(mutable)
}

fun SvgDocument.sendSelectionBackward(): SvgDocument {
    if (selectedElementIds.isEmpty()) return this
    val mutable = elements.toMutableList()
    for (index in 1..mutable.lastIndex) {
        val current = mutable[index]
        val prev = mutable[index - 1]
        if (current.id in selectedElementIds && prev.id !in selectedElementIds) {
            mutable[index] = prev
            mutable[index - 1] = current
        }
    }
    return withElements(mutable)
}

fun SvgDocument.groupSelection(): SvgDocument {
    if (selectedElementIds.size < 2) return this
    val group = "grp_${newSvgId().take(6)}"
    return copy(
        groups = groups + group
    ).withElements(
        elements.map { e ->
            if (e.id in selectedElementIds) e.withMeta(groupId = group) else e
        }
    )
}

fun SvgDocument.ungroupSelection(): SvgDocument = withElements(
    elements.map { e ->
        if (e.id in selectedElementIds) e.withMeta(groupId = null) else e
    }
)

fun SvgDocument.updatePathNode(
    elementId: String,
    nodeId: String,
    anchor: Offset,
    inHandle: Offset? = null,
    outHandle: Offset? = null,
    type: SvgPathSegmentType? = null
): SvgDocument = withElements(
    elements.map { element ->
        if (element.id != elementId) return@map element
        when (element) {
            is SvgPathElement -> {
                element.copy(
                    nodes = element.nodes.map { node ->
                        if (node.id == nodeId) {
                            node.copy(
                                anchor = anchor,
                                inHandle = inHandle ?: node.inHandle,
                                outHandle = outHandle ?: node.outHandle,
                                type = type ?: node.type
                            )
                        } else node
                    }
                )
            }
            is SvgPolygonElement -> {
                val idx = nodeId.removePrefix("p").toIntOrNull()
                if (idx == null || idx !in element.points.indices) element
                else element.copy(points = element.points.toMutableList().apply { this[idx] = anchor })
            }
            else -> element
        }
    }
)

fun SvgDocument.insertNodeAfterSelection(ui: SvgEditorUiState): Pair<SvgDocument, SvgEditorUiState> {
    val elementId = ui.selectedNodeElementId ?: return this to ui
    val nodeId = ui.selectedNodeId ?: return this to ui
    var inserted: String? = null
    val updated = withElements(
        elements.map { element ->
            if (element.id != elementId) return@map element
            when (element) {
                is SvgPathElement -> {
                    val idx = element.nodes.indexOfFirst { it.id == nodeId }
                    if (idx < 0) element
                    else {
                        val a = element.nodes[idx].anchor
                        val b = element.nodes.getOrNull((idx + 1) % element.nodes.size)?.anchor ?: a + Offset(18f, 0f)
                        val next = SvgPathNode(
                            id = newSvgId(),
                            anchor = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f),
                            type = SvgPathSegmentType.Line
                        )
                        inserted = next.id
                        element.copy(nodes = element.nodes.toMutableList().apply { add(idx + 1, next) })
                    }
                }
                is SvgPolygonElement -> {
                    val idx = nodeId.removePrefix("p").toIntOrNull()
                    if (idx == null || idx !in element.points.indices) element
                    else {
                        val a = element.points[idx]
                        val b = element.points[(idx + 1) % element.points.size]
                        val p = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                        inserted = "p${idx + 1}"
                        element.copy(points = element.points.toMutableList().apply { add(idx + 1, p) })
                    }
                }
                else -> element
            }
        }
    )
    return updated to ui.copy(selectedNodeId = inserted ?: ui.selectedNodeId)
}

fun SvgDocument.deleteSelectedNode(ui: SvgEditorUiState): Pair<SvgDocument, SvgEditorUiState> {
    val elementId = ui.selectedNodeElementId ?: return this to ui
    val nodeId = ui.selectedNodeId ?: return this to ui
    val updated = withElements(
        elements.map { element ->
            if (element.id != elementId) return@map element
            when (element) {
                is SvgPathElement -> {
                    if (element.nodes.size <= 2) element
                    else element.copy(nodes = element.nodes.filterNot { it.id == nodeId })
                }
                is SvgPolygonElement -> {
                    val idx = nodeId.removePrefix("p").toIntOrNull()
                    if (idx == null || idx !in element.points.indices || element.points.size <= 3) element
                    else element.copy(points = element.points.filterIndexed { index, _ -> index != idx })
                }
                else -> element
            }
        }
    )
    return updated to ui.copy(selectedNodeId = null, selectedNodeElementId = null)
}

fun SvgDocument.findNearestNode(point: Offset, radius: Float = 14f): Pair<String, String>? {
    var best: Pair<String, String>? = null
    var bestDistance = radius
    elements.forEach { element ->
        when (element) {
            is SvgPathElement -> element.nodes.forEach { node ->
                val d = distance(node.anchor, point)
                if (d <= bestDistance) {
                    bestDistance = d
                    best = element.id to node.id
                }
            }
            is SvgPolygonElement -> element.points.forEachIndexed { index, p ->
                val d = distance(p, point)
                if (d <= bestDistance) {
                    bestDistance = d
                    best = element.id to "p$index"
                }
            }
            else -> Unit
        }
    }
    return best
}

fun boundsOf(elements: List<SvgElement>): SvgBounds {
    if (elements.isEmpty()) return SvgBounds(0f, 0f, 0f, 0f)
    return elements
        .map { it.bounds() }
        .reduce { acc, b ->
            SvgBounds(
                left = min(acc.left, b.left),
                top = min(acc.top, b.top),
                right = max(acc.right, b.right),
                bottom = max(acc.bottom, b.bottom)
            )
        }
}

private fun SvgDocument.replaceElementMeta(id: String, mutate: (SvgElement) -> SvgElement): SvgDocument =
    withElements(elements.map { if (it.id == id) mutate(it) else it })

fun boundsFromPoints(points: List<Offset>): SvgBounds {
    if (points.isEmpty()) return SvgBounds(0f, 0f, 0f, 0f)
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    points.forEach { p ->
        minX = min(minX, p.x)
        minY = min(minY, p.y)
        maxX = max(maxX, p.x)
        maxY = max(maxY, p.y)
    }
    return SvgBounds(minX, minY, maxX, maxY)
}

fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

fun scalePoint(point: Offset, pivot: Offset, factor: Float): Offset = Offset(
    x = pivot.x + (point.x - pivot.x) * factor,
    y = pivot.y + (point.y - pivot.y) * factor
)

fun rotatePoint(point: Offset, pivot: Offset, degrees: Float): Offset {
    if (kotlin.math.abs(degrees) <= EPSILON) return point
    val radians = degrees / 180f * PI.toFloat()
    val px = point.x - pivot.x
    val py = point.y - pivot.y
    val c = cos(radians)
    val s = sin(radians)
    return Offset(
        x = px * c - py * s + pivot.x,
        y = px * s + py * c + pivot.y
    )
}

private fun pointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    if (polygon.size < 3) return false
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val pi = polygon[i]
        val pj = polygon[j]
        val intersect = ((pi.y > point.y) != (pj.y > point.y)) &&
            (point.x < (pj.x - pi.x) * (point.y - pi.y) / ((pj.y - pi.y).takeIf { kotlin.math.abs(it) > EPSILON } ?: EPSILON) + pi.x)
        if (intersect) inside = !inside
        j = i
    }
    return inside
}
