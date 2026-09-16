package com.compose.canvas.editor

import androidx.compose.ui.geometry.Offset
import com.compose.canvas.core.BoundsCalculator
import com.compose.canvas.core.SceneNode
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.geometry.RectF
import kotlin.math.abs

enum class SnapTarget { Grid, Guide, ObjectEdge, ObjectCenter, Node, Anchor, Port, ContentBounds }

data class SnapResult(
    val position: Offset,
    val target: SnapTarget,
    val axis: Axis,
    val distance: Float
)

enum class Axis { X, Y, Both }

data class GridSettings(
    val enabled: Boolean = true,
    val size: Float = 50f,
    val snap: Boolean = true
)

object SnapEngine {
    fun snap(
        point: Offset,
        document: VectorDocument,
        grid: GridSettings,
        excludeIds: Set<String> = emptySet(),
        threshold: Float = 8f
    ): Pair<Offset, List<SnapResult>> {
        var x = point.x
        var y = point.y
        val guides = mutableListOf<SnapResult>()
        if (grid.snap && grid.enabled && grid.size > 0f) {
            val gx = (point.x / grid.size).toInt() * grid.size
            val gy = (point.y / grid.size).toInt() * grid.size
            val nx = gx + if (abs(point.x - gx) < abs(point.x - (gx + grid.size))) 0f else grid.size
            val ny = gy + if (abs(point.y - gy) < abs(point.y - (gy + grid.size))) 0f else grid.size
            if (abs(point.x - nx) <= threshold) {
                x = nx
                guides += SnapResult(Offset(x, point.y), SnapTarget.Grid, Axis.X, abs(point.x - nx))
            }
            if (abs(point.y - ny) <= threshold) {
                y = ny
                guides += SnapResult(Offset(point.x, y), SnapTarget.Grid, Axis.Y, abs(point.y - ny))
            }
        }
        val nodes = document.allNodes().filter { it.id !in excludeIds && it.visible }
        var bestX = threshold + 1f
        var bestY = threshold + 1f
        for (node in nodes) {
            val b = BoundsCalculator.of(node) ?: continue
            val candidatesX = listOf(b.left, b.centerX, b.right)
            val candidatesY = listOf(b.top, b.centerY, b.bottom)
            for (cx in candidatesX) {
                val d = abs(point.x - cx)
                if (d < bestX && d <= threshold) {
                    bestX = d
                    x = cx
                    val kind = when (cx) {
                        b.centerX -> SnapTarget.ObjectCenter
                        else -> SnapTarget.ObjectEdge
                    }
                    guides.removeAll { it.axis == Axis.X && it.target != SnapTarget.Grid }
                    guides += SnapResult(Offset(x, point.y), kind, Axis.X, d)
                }
            }
            for (cy in candidatesY) {
                val d = abs(point.y - cy)
                if (d < bestY && d <= threshold) {
                    bestY = d
                    y = cy
                    val kind = when (cy) {
                        b.centerY -> SnapTarget.ObjectCenter
                        else -> SnapTarget.ObjectEdge
                    }
                    guides.removeAll { it.axis == Axis.Y && it.target != SnapTarget.Grid }
                    guides += SnapResult(Offset(point.x, y), kind, Axis.Y, d)
                }
            }
        }
        return Offset(x, y) to guides
    }

    fun maybeSnap(point: Offset, grid: GridSettings): Offset {
        if (!grid.snap || !grid.enabled || grid.size <= 0f) return point
        val nx = (point.x / grid.size).toInt() * grid.size
        val ny = (point.y / grid.size).toInt() * grid.size
        val x = if (abs(point.x - nx) < abs(point.x - (nx + grid.size))) nx else nx + grid.size
        val y = if (abs(point.y - ny) < abs(point.y - (ny + grid.size))) ny else ny + grid.size
        return Offset(x, y)
    }
}
