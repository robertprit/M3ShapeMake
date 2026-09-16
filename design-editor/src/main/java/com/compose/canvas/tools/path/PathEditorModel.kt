package com.compose.canvas.tools.path

import androidx.compose.ui.geometry.Offset
import com.compose.canvas.geometry.Geometry
import com.compose.canvas.geometry.GeometryToPath
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment

enum class NodeType { Corner, Smooth, Symmetric }

data class NodeRef(val subpathIndex: Int, val nodeIndex: Int)
data class HandleRef(val nodeRef: NodeRef, val outgoing: Boolean)
data class SegmentRef(val subpathIndex: Int, val nodeIndex: Int)

data class PathNode(
    val point: Offset,
    val type: NodeType = NodeType.Corner,
    val handleIn: Offset? = null,
    val handleOut: Offset? = null
)

data class PathSubpath(
    val nodes: List<PathNode> = emptyList(),
    val closed: Boolean = false
)

data class PathData(
    val subpaths: List<PathSubpath> = emptyList()
) {
    fun node(ref: NodeRef): PathNode? =
        subpaths.getOrNull(ref.subpathIndex)?.nodes?.getOrNull(ref.nodeIndex)

    fun toGeometry(): PathGeometry {
        val segs = mutableListOf<PathSegment>()
        for (sub in subpaths) {
            if (sub.nodes.isEmpty()) continue
            val first = sub.nodes.first()
            segs += PathSegment.MoveTo(first.point.x, first.point.y)
            for (i in 0 until sub.nodes.size - 1) {
                val a = sub.nodes[i]
                val b = sub.nodes[i + 1]
                segs += segmentBetween(a, b)
            }
            if (sub.closed && sub.nodes.size >= 2) {
                segs += segmentBetween(sub.nodes.last(), sub.nodes.first())
                segs += PathSegment.Close
            }
        }
        return PathGeometry(segs, closed = subpaths.any { it.closed })
    }
}

private fun segmentBetween(a: PathNode, b: PathNode): PathSegment {
    val out = a.handleOut
    val inn = b.handleIn
    return when {
        out != null && inn != null ->
            PathSegment.CubicBezierTo(out.x, out.y, inn.x, inn.y, b.point.x, b.point.y)
        out != null ->
            PathSegment.QuadraticBezierTo(out.x, out.y, b.point.x, b.point.y)
        inn != null ->
            PathSegment.QuadraticBezierTo(inn.x, inn.y, b.point.x, b.point.y)
        else -> PathSegment.LineTo(b.point.x, b.point.y)
    }
}

fun toPathData(geometry: Geometry): PathData {
    val path = GeometryToPath.toPath(geometry)
    val subpaths = mutableListOf<PathSubpath>()
    var nodes = mutableListOf<PathNode>()
    var closed = false
    var pendingOut: Offset? = null
    fun flush() {
        if (nodes.isNotEmpty()) {
            subpaths += PathSubpath(nodes.toList(), closed)
            nodes = mutableListOf()
        }
        closed = false
        pendingOut = null
    }
    for (seg in path.segments) {
        when (seg) {
            is PathSegment.MoveTo -> {
                flush()
                nodes += PathNode(Offset(seg.x, seg.y))
            }
            is PathSegment.LineTo -> nodes += PathNode(Offset(seg.x, seg.y))
            is PathSegment.QuadraticBezierTo -> {
                val last = nodes.lastOrNull()
                if (last != null) {
                    nodes[nodes.lastIndex] = last.copy(
                        handleOut = Offset(seg.cx, seg.cy),
                        type = NodeType.Smooth
                    )
                }
                nodes += PathNode(
                    point = Offset(seg.x, seg.y),
                    handleIn = Offset(seg.cx, seg.cy),
                    type = NodeType.Smooth
                )
            }
            is PathSegment.CubicBezierTo -> {
                val last = nodes.lastOrNull()
                if (last != null) {
                    nodes[nodes.lastIndex] = last.copy(
                        handleOut = Offset(seg.cx1, seg.cy1),
                        type = NodeType.Smooth
                    )
                }
                nodes += PathNode(
                    point = Offset(seg.x, seg.y),
                    handleIn = Offset(seg.cx2, seg.cy2),
                    type = NodeType.Smooth
                )
            }
            PathSegment.Close -> closed = true
        }
    }
    flush()
    return PathData(subpaths)
}

fun PathData.moveNode(ref: NodeRef, point: Offset): PathData {
    val node = node(ref) ?: return this
    val delta = point - node.point
    return updateNode(ref) { n ->
        n.copy(
            point = point,
            handleIn = n.handleIn?.plus(delta),
            handleOut = n.handleOut?.plus(delta)
        )
    }
}

fun PathData.dragHandle(ref: HandleRef, handlePoint: Offset): PathData {
    val node = node(ref.nodeRef) ?: return this
    return updateNode(ref.nodeRef) { n ->
        if (ref.outgoing) {
            val out = handlePoint
            val inn = when (n.type) {
                NodeType.Corner -> n.handleIn
                NodeType.Smooth -> n.handleIn?.let { mirrorAround(n.point, out, keepLength = false, existing = it) }
                NodeType.Symmetric -> n.handleIn?.let { mirrorAround(n.point, out, keepLength = true, existing = it) }
            }
            n.copy(handleOut = out, handleIn = inn)
        } else {
            val inn = handlePoint
            val out = when (n.type) {
                NodeType.Corner -> n.handleOut
                NodeType.Smooth -> n.handleOut?.let { mirrorAround(n.point, inn, keepLength = false, existing = it) }
                NodeType.Symmetric -> n.handleOut?.let { mirrorAround(n.point, inn, keepLength = true, existing = it) }
            }
            n.copy(handleIn = inn, handleOut = out)
        }
    }
}

fun PathData.convertNode(ref: NodeRef, type: NodeType): PathData = updateNode(ref) { n ->
    when (type) {
        NodeType.Corner -> n.copy(type = type)
        NodeType.Smooth, NodeType.Symmetric -> {
            val out = n.handleOut ?: n.point + Offset(20f, 0f)
            val inn = n.handleIn ?: Offset(n.point.x * 2 - out.x, n.point.y * 2 - out.y)
            n.copy(type = type, handleOut = out, handleIn = inn)
        }
    }
}

fun PathData.deleteNode(ref: NodeRef): PathData {
    val sub = subpaths.getOrNull(ref.subpathIndex) ?: return this
    if (sub.nodes.size <= 2) return this
    val nodes = sub.nodes.toMutableList().also { it.removeAt(ref.nodeIndex) }
    return copy(subpaths = subpaths.toMutableList().also { it[ref.subpathIndex] = sub.copy(nodes = nodes) })
}

fun PathData.insertNodeOnSegment(ref: SegmentRef, t: Float): PathData {
    val sub = subpaths.getOrNull(ref.subpathIndex) ?: return this
    val a = sub.nodes.getOrNull(ref.nodeIndex) ?: return this
    val b = sub.nodes.getOrNull(ref.nodeIndex + 1) ?: sub.nodes.firstOrNull() ?: return this
    val p = Offset(a.point.x + (b.point.x - a.point.x) * t, a.point.y + (b.point.y - a.point.y) * t)
    val nodes = sub.nodes.toMutableList()
    nodes.add(ref.nodeIndex + 1, PathNode(p))
    return copy(subpaths = subpaths.toMutableList().also { it[ref.subpathIndex] = sub.copy(nodes = nodes) })
}

fun PathData.closeSubpath(index: Int): PathData {
    val sub = subpaths.getOrNull(index) ?: return this
    return copy(subpaths = subpaths.toMutableList().also { it[index] = sub.copy(closed = true) })
}

fun PathData.openSubpath(index: Int): PathData {
    val sub = subpaths.getOrNull(index) ?: return this
    return copy(subpaths = subpaths.toMutableList().also { it[index] = sub.copy(closed = false) })
}

private fun PathData.updateNode(ref: NodeRef, transform: (PathNode) -> PathNode): PathData {
    val sub = subpaths.getOrNull(ref.subpathIndex) ?: return this
    val nodes = sub.nodes.toMutableList()
    if (ref.nodeIndex !in nodes.indices) return this
    nodes[ref.nodeIndex] = transform(nodes[ref.nodeIndex])
    return copy(subpaths = subpaths.toMutableList().also { it[ref.subpathIndex] = sub.copy(nodes = nodes) })
}

private fun mirrorAround(center: Offset, other: Offset, keepLength: Boolean, existing: Offset): Offset {
    val vx = other.x - center.x
    val vy = other.y - center.y
    return if (keepLength) {
        Offset(center.x - vx, center.y - vy)
    } else {
        val elen = hypotOff(existing - center)
        val olen = hypotOff(Offset(vx, vy)).let { if (it < 1e-4f) 1f else it }
        Offset(center.x - vx / olen * elen, center.y - vy / olen * elen)
    }
}

private fun hypotOff(o: Offset) = kotlin.math.hypot(o.x, o.y)

private operator fun Offset.plus(o: Offset) = Offset(x + o.x, y + o.y)
private operator fun Offset.minus(o: Offset) = Offset(x - o.x, y - o.y)
