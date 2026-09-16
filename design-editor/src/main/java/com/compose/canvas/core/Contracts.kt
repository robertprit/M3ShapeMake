package com.compose.canvas.core

import com.compose.canvas.geometry.ObjectId
import com.compose.canvas.metadata.ContentArea
import com.compose.canvas.metadata.NamedAnchor
import com.compose.canvas.metadata.Port
import com.compose.canvas.metadata.ResizeRules

data class Definitions(
    val gradients: List<GradientResource> = emptyList()
) {
    fun gradient(id: ObjectId): GradientResource? = gradients.find { it.id == id }

    fun upsert(gradient: GradientResource): Definitions {
        val without = gradients.filterNot { it.id == gradient.id }
        return copy(gradients = without + gradient)
    }

    fun remove(id: ObjectId): Definitions = copy(gradients = gradients.filterNot { it.id == id })
}

sealed interface GradientResource {
    val id: ObjectId
    val name: String
}

data class DocumentSettings(
    val gridSize: Float = 50f,
    val snapEnabled: Boolean = true
)

data class ShapeMetadata(
    val parameters: Map<String, Float> = emptyMap(),
    val ports: List<Port> = emptyList(),
    val anchors: List<NamedAnchor> = emptyList(),
    val contentAreas: List<ContentArea> = emptyList()
)

data class ObjectSemantics(
    val parameters: Map<String, Float> = emptyMap(),
    val ports: List<Port> = emptyList(),
    val anchors: List<NamedAnchor> = emptyList(),
    val contentAreas: List<ContentArea> = emptyList(),
    val resizeRules: ResizeRules = ResizeRules()
)
