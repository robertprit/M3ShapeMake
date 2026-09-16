package com.compose.canvas.core

import com.compose.canvas.geometry.ObjectId
import com.compose.canvas.geometry.RectF

/**
 * Canvas size in **document units**, independent of dp/px. [viewBox] defaults to
 * the full canvas (`0,0,width,height`) and is the SVG viewBox source later.
 */
data class CanvasSettings(
    val width: Float = 1000f,
    val height: Float = 1000f,
    val viewBox: RectF? = null,
    val backgroundTransparent: Boolean = true
) {
    val effectiveViewBox: RectF
        get() = viewBox ?: RectF(0f, 0f, width, height)
}

/**
 * Single source of truth for the editor.
 *
 * ```
 * VectorDocument
 *  ├─ Canvas
 *  ├─ Layers → SceneNode[]
 *  ├─ Definitions           (gradients)
 *  ├─ ShapeMetadata         (ports/anchors/content/parameters)
 *  └─ DocumentSettings
 * ```
 *
 * Viewport zoom/pan, selection, and open panels are **not** stored here.
 */
data class VectorDocument(
    val name: String = "Untitled",
    val canvas: CanvasSettings = CanvasSettings(),
    val layers: List<VectorLayer> = listOf(VectorLayer(name = "Layer 1")),
    val definitions: Definitions = Definitions(),
    val metadata: ShapeMetadata = ShapeMetadata(),
    val settings: DocumentSettings = DocumentSettings(),
    val documentFormatVersion: Int = CURRENT_FORMAT_VERSION
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }

    fun allObjects(): List<VectorObject> =
        layers.filter { it.visible }.flatMap { layer ->
            layer.leafObjects().filter { it.visible }
        }

    fun allNodes(): List<SceneNode> =
        layers.filter { it.visible }.flatMap { layer ->
            layer.allNodes().filter { it.visible }
        }

    fun findObject(id: ObjectId): VectorObject? =
        layers.flatMap { it.leafObjects() }.find { it.id == id }

    fun findNode(id: ObjectId): SceneNode? =
        layers.asSequence().mapNotNull { it.find(id) }.firstOrNull()

    fun findLayerIndexOf(id: ObjectId): Int? {
        val index = layers.indexOfFirst { layer -> layer.find(id) != null }
        return index.takeIf { it >= 0 }
    }

    fun addObjectToLayer(layerIndex: Int, obj: SceneNode): VectorDocument {
        if (layerIndex !in layers.indices) return this
        return copy(
            layers = layers.mapIndexed { i, layer ->
                if (i == layerIndex) layer.addObject(obj) else layer
            }
        )
    }

    fun removeObject(id: ObjectId): VectorDocument =
        copy(layers = layers.map { it.removeObject(id) })

    fun replaceObject(obj: SceneNode): VectorDocument =
        copy(layers = layers.map { it.replaceObject(obj) })

    fun addLayer(layer: VectorLayer = VectorLayer(name = "Layer ${layers.size + 1}")): VectorDocument =
        copy(layers = layers + layer)

    fun removeLayer(index: Int): VectorDocument {
        if (layers.size <= 1) return this
        return copy(layers = layers.toMutableList().apply { removeAt(index) })
    }

    fun moveLayer(fromIndex: Int, toIndex: Int): VectorDocument {
        if (fromIndex !in layers.indices || toIndex !in layers.indices) return this
        val mutable = layers.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return copy(layers = mutable)
    }

    fun replaceLayer(index: Int, layer: VectorLayer): VectorDocument {
        if (index !in layers.indices) return this
        return copy(layers = layers.mapIndexed { i, l -> if (i == index) layer else l })
    }
}
