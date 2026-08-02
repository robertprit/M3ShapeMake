package com.m3shapes.editor

data class ShapeViewportState(
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f
)

data class ShapeEditorState(
    val selectedVertexIds: Set<String> = emptySet(),
    val lastSelectedVertexId: String? = null,
    val viewport: ShapeViewportState = ShapeViewportState(),
    val multiSelectEnabled: Boolean = false,
    val snapToGrid: Boolean = false,
    val gridSize: Float = 8f,
    val showVertexIndices: Boolean = true
)

fun ShapeEditorState.selectVertex(vertexId: String): ShapeEditorState {
    val nextSelected = if (multiSelectEnabled) {
        if (vertexId in selectedVertexIds) selectedVertexIds - vertexId else selectedVertexIds + vertexId
    } else {
        setOf(vertexId)
    }
    return copy(
        selectedVertexIds = nextSelected,
        lastSelectedVertexId = vertexId
    )
}

fun ShapeEditorState.replaceSelection(vertexId: String): ShapeEditorState = copy(
    selectedVertexIds = setOf(vertexId),
    lastSelectedVertexId = vertexId
)

fun ShapeEditorState.resetViewport(): ShapeEditorState = copy(
    viewport = ShapeViewportState()
)
