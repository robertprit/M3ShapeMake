package com.compose.canvas.editor

import com.compose.canvas.core.BoundsCalculator
import com.compose.canvas.core.ObjectTransform
import com.compose.canvas.core.SceneNode

enum class AlignmentMode(val displayName: String) {
    AlignLeft("Links"),
    AlignHCenter("Horizontal mitte"),
    AlignRight("Rechts"),
    AlignTop("Oben"),
    AlignVCenter("Vertikal mitte"),
    AlignBottom("Unten"),
    DistributeH("Horizontal verteilen"),
    DistributeV("Vertikal verteilen")
}

object AlignmentEngine {
    fun apply(nodes: List<SceneNode>, mode: AlignmentMode, canvasWidth: Float = 0f, canvasHeight: Float = 0f): List<SceneNode> {
        if (nodes.size < 2 && mode != AlignmentMode.AlignLeft) {
            // still allow align-to-canvas for one object via dedicated overloads
        }
        if (nodes.isEmpty()) return nodes
        val bounds = nodes.map { it to BoundsCalculator.of(it) }
        val valid = bounds.mapNotNull { (n, b) -> b?.let { n to it } }
        if (valid.isEmpty()) return nodes
        val unionLeft = valid.minOf { it.second.left }
        val unionRight = valid.maxOf { it.second.right }
        val unionTop = valid.minOf { it.second.top }
        val unionBottom = valid.maxOf { it.second.bottom }
        val unionCx = (unionLeft + unionRight) / 2f
        val unionCy = (unionTop + unionBottom) / 2f

        return when (mode) {
            AlignmentMode.AlignLeft -> valid.map { (n, b) -> n.nudge(unionLeft - b.left, 0f) }
            AlignmentMode.AlignRight -> valid.map { (n, b) -> n.nudge(unionRight - b.right, 0f) }
            AlignmentMode.AlignHCenter -> valid.map { (n, b) -> n.nudge(unionCx - b.centerX, 0f) }
            AlignmentMode.AlignTop -> valid.map { (n, b) -> n.nudge(0f, unionTop - b.top) }
            AlignmentMode.AlignBottom -> valid.map { (n, b) -> n.nudge(0f, unionBottom - b.bottom) }
            AlignmentMode.AlignVCenter -> valid.map { (n, b) -> n.nudge(0f, unionCy - b.centerY) }
            AlignmentMode.DistributeH -> distribute(valid, horizontal = true)
            AlignmentMode.DistributeV -> distribute(valid, horizontal = false)
        }.let { moved ->
            val byId = moved.associateBy { it.id }
            nodes.map { byId[it.id] ?: it }
        }
    }

    private fun distribute(nodes: List<Pair<SceneNode, com.compose.canvas.geometry.RectF>>, horizontal: Boolean): List<SceneNode> {
        if (nodes.size < 3) return nodes.map { it.first }
        val sorted = if (horizontal) nodes.sortedBy { it.second.centerX } else nodes.sortedBy { it.second.centerY }
        val first = sorted.first().second
        val last = sorted.last().second
        val start = if (horizontal) first.centerX else first.centerY
        val end = if (horizontal) last.centerX else last.centerY
        val step = (end - start) / (sorted.size - 1)
        return sorted.mapIndexed { i, (n, b) ->
            val target = start + step * i
            if (horizontal) n.nudge(target - b.centerX, 0f) else n.nudge(0f, target - b.centerY)
        }
    }

    private fun SceneNode.nudge(dx: Float, dy: Float): SceneNode =
        withTransform(transform.copy(x = transform.x + dx, y = transform.y + dy))
}

class ClipboardState {
    var nodes: List<SceneNode> = emptyList()
        private set
    val isEmpty get() = nodes.isEmpty()
    fun set(value: List<SceneNode>) {
        nodes = value.map { it.deepCopy() }
    }
}
