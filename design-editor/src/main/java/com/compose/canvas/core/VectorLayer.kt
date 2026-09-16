package com.compose.canvas.core

import com.compose.canvas.geometry.ObjectId
import com.compose.canvas.geometry.newObjectId

data class VectorLayer(
    val id: ObjectId = newObjectId(),
    val name: String = "Layer",
    val visible: Boolean = true,
    val locked: Boolean = false,
    val objects: List<SceneNode> = emptyList()
) {
    fun addObject(obj: SceneNode): VectorLayer = copy(objects = objects + obj)

    fun removeObject(id: ObjectId): VectorLayer =
        copy(objects = objects.mapNotNull { node ->
            when {
                node.id == id -> null
                node is GroupObject -> node.removeChild(id)
                else -> node
            }
        })

    fun replaceObject(obj: SceneNode): VectorLayer =
        copy(objects = objects.map { node ->
            when {
                node.id == obj.id -> obj
                node is GroupObject -> node.replaceChild(obj)
                else -> node
            }
        })

    fun find(id: ObjectId): SceneNode? =
        objects.asSequence().flatMap { it.subtree() }.find { it.id == id }

    fun leafObjects(): List<VectorObject> = objects.flatMap { it.leafObjects() }

    fun allNodes(): List<SceneNode> = objects.flatMap { it.subtree() }

    fun moveObject(fromIndex: Int, toIndex: Int): VectorLayer {
        if (fromIndex !in objects.indices || toIndex !in objects.indices) return this
        val mutable = objects.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return copy(objects = mutable)
    }
}
