package com.compose.canvas.core

import com.compose.canvas.geometry.Geometry
import com.compose.canvas.geometry.ObjectId
import com.compose.canvas.geometry.RectF
import com.compose.canvas.geometry.newObjectId
import com.compose.canvas.math.TransformMatrix
import com.compose.canvas.math.TransformUtils

sealed interface SceneNode {
    val id: ObjectId
    val name: String
    val visible: Boolean
    val locked: Boolean
    val transform: ObjectTransform

    fun withTransform(transform: ObjectTransform): SceneNode
    fun withName(name: String): SceneNode
    fun withVisible(visible: Boolean): SceneNode
    fun withLocked(locked: Boolean): SceneNode
    fun deepCopy(): SceneNode
    fun subtree(): List<SceneNode>
    fun leafObjects(): List<VectorObject>
}

data class VectorObject(
    override val id: ObjectId = newObjectId(),
    override val name: String = "Object",
    override val visible: Boolean = true,
    override val locked: Boolean = false,
    val geometry: Geometry,
    override val transform: ObjectTransform = ObjectTransform(),
    val fill: FillStyle = FillStyle(),
    val stroke: StrokeStyle = StrokeStyle(),
    val semantics: ObjectSemantics = ObjectSemantics()
) : SceneNode {
    override fun withTransform(transform: ObjectTransform) = copy(transform = transform)
    override fun withName(name: String) = copy(name = name)
    override fun withVisible(visible: Boolean) = copy(visible = visible)
    override fun withLocked(locked: Boolean) = copy(locked = locked)
    override fun deepCopy() = copy(id = newObjectId(), name = "$name copy")
    override fun subtree() = listOf(this)
    override fun leafObjects() = listOf(this)
}

data class GroupObject(
    override val id: ObjectId = newObjectId(),
    override val name: String = "Group",
    override val visible: Boolean = true,
    override val locked: Boolean = false,
    override val transform: ObjectTransform = ObjectTransform(),
    val children: List<SceneNode> = emptyList()
) : SceneNode {
    override fun withTransform(transform: ObjectTransform) = copy(transform = transform)
    override fun withName(name: String) = copy(name = name)
    override fun withVisible(visible: Boolean) = copy(visible = visible)
    override fun withLocked(locked: Boolean) = copy(locked = locked)
    override fun deepCopy() = copy(
        id = newObjectId(),
        name = "$name copy",
        children = children.map { it.deepCopy() }
    )
    override fun subtree() = listOf(this) + children.flatMap { it.subtree() }
    override fun leafObjects() = children.flatMap { it.leafObjects() }

    fun replaceChild(obj: SceneNode): GroupObject = copy(
        children = children.map { child ->
            when {
                child.id == obj.id -> obj
                child is GroupObject -> child.replaceChild(obj)
                else -> child
            }
        }
    )

    fun removeChild(id: ObjectId): GroupObject = copy(
        children = children.mapNotNull { child ->
            when {
                child.id == id -> null
                child is GroupObject -> child.removeChild(id)
                else -> child
            }
        }
    )
}

object BoundsCalculator {
    fun of(node: SceneNode): RectF? = worldBounds(node, TransformMatrix())

    fun ofAll(nodes: List<SceneNode>): RectF? {
        var result: RectF? = null
        for (node in nodes) {
            val b = of(node) ?: continue
            result = if (result == null) b else result.union(b)
        }
        return result
    }

    private fun worldBounds(node: SceneNode, parent: TransformMatrix): RectF? {
        if (!node.visible) return null
        val world = TransformUtils.multiply(parent, TransformUtils.fromObjectTransform(node.transform))
        return when (node) {
            is VectorObject -> geometryWorldBounds(node.geometry.boundingRect(), world)
            is GroupObject -> node.children.mapNotNull { worldBounds(it, world) }
                .reduceOrNull { a, b -> a.union(b) }
        }
    }

    private fun geometryWorldBounds(local: RectF, m: TransformMatrix): RectF {
        val tl = m.transformPoint(local.left, local.top)
        val tr = m.transformPoint(local.right, local.top)
        val bl = m.transformPoint(local.left, local.bottom)
        val br = m.transformPoint(local.right, local.bottom)
        val xs = listOf(tl.x, tr.x, bl.x, br.x)
        val ys = listOf(tl.y, tr.y, bl.y, br.y)
        return RectF(xs.min(), ys.min(), xs.max(), ys.max())
    }
}

object WorldTransform {
    fun matrixOf(document: VectorDocument, id: ObjectId): TransformMatrix? {
        fun walk(nodes: List<SceneNode>, parent: TransformMatrix): TransformMatrix? {
            for (node in nodes) {
                val world = TransformUtils.multiply(parent, TransformUtils.fromObjectTransform(node.transform))
                if (node.id == id) return world
                if (node is GroupObject) {
                    walk(node.children, world)?.let { return it }
                }
            }
            return null
        }
        return walk(document.layers.flatMap { it.objects }, TransformMatrix())
    }
}
