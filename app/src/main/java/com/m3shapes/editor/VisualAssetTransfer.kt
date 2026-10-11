package com.m3shapes.editor

import com.compose.canvas.asset.VisualAsset
import com.compose.canvas.asset.VisualState
import com.compose.canvas.core.SceneNode
import com.compose.canvas.core.ShapeMetadata
import com.compose.canvas.core.VectorLayer

internal fun VisualAsset.forVisualTaskerTransfer(selectedNodes: List<SceneNode>): VisualAsset {
    val transferNodes = selectedNodes.ifEmpty { design.layers.flatMap(VectorLayer::objects) }
    val selectedObjects = transferNodes.flatMap(SceneNode::leafObjects)
    val selectedDocument = design.copy(
        name = selectedNodes.singleOrNull()?.name ?: identity.name,
        layers = listOf(VectorLayer(name = "VisualTasker Transfer", objects = transferNodes)),
        metadata = ShapeMetadata(
            ports = selectedObjects.flatMap { it.semantics.ports }.distinctBy { it.id },
            anchors = selectedObjects.flatMap { it.semantics.anchors }.distinctBy { it.id },
            contentAreas = selectedObjects.flatMap { it.semantics.contentAreas }.distinctBy { it.id },
        ),
    )
    return copy(
        identity = identity.copy(name = selectedDocument.name),
        design = selectedDocument,
        states = listOf(VisualState("default", "Default", selectedDocument)),
        transitions = emptyList(),
        animations = emptyList(),
        sequences = emptyList(),
        metadata = metadata + (
            "transferScope" to if (selectedNodes.isEmpty()) "asset" else "selection"
        ),
    )
}
