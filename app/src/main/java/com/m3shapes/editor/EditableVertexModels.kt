package com.m3shapes.editor

import kotlin.math.roundToInt

data class EditableVertex(
    val id: String,
    val x: Float,
    val y: Float,
    val rounding: Float = 0f,
    val smoothing: Float = 0f,
    val locked: Boolean = false
)

fun DrawnPolygonConfig.toEditableVertices(): List<EditableVertex> {
    return vertices.mapIndexed { index, point ->
        val corner = when {
            perVertexRounding.size == vertices.size -> perVertexRounding[index]
            else -> perVertexRounding.getOrNull(index)
        }
        EditableVertex(
            id = corner?.vertexId ?: "v$index",
            x = point.x,
            y = point.y,
            rounding = corner?.rounding ?: rounding,
            smoothing = corner?.smoothing ?: smoothing,
            locked = corner?.locked ?: false
        )
    }
}

fun List<EditableVertex>.toDrawnPolygonConfig(
    fallbackRounding: Float = 0f,
    fallbackSmoothing: Float = 0f
): DrawnPolygonConfig = DrawnPolygonConfig(
    vertices = map { VertexPoint(it.x, it.y) },
    rounding = fallbackRounding,
    smoothing = fallbackSmoothing,
    perVertexRounding = map {
        VertexCornerConfig(
            vertexId = it.id,
            rounding = it.rounding,
            smoothing = it.smoothing,
            locked = it.locked
        )
    }
)

fun nextVertexId(existing: List<EditableVertex>): String {
    val maxIndex = existing.mapNotNull { vertex ->
        vertex.id.removePrefix("v").toIntOrNull()
    }.maxOrNull() ?: (existing.size - 1)
    return "v${maxIndex + 1}"
}

fun moveSelectedVertices(
    vertices: List<EditableVertex>,
    selectedIds: Set<String>,
    dx: Float,
    dy: Float,
    snapToGrid: Boolean,
    gridSize: Float
): List<EditableVertex> {
    if (selectedIds.isEmpty()) return vertices
    return vertices.map { vertex ->
        if (vertex.id !in selectedIds || vertex.locked) return@map vertex
        var nx = (vertex.x + dx).coerceIn(0f, 256f)
        var ny = (vertex.y + dy).coerceIn(0f, 256f)
        if (snapToGrid && gridSize > 0f) {
            nx = (nx / gridSize).roundToInt() * gridSize
            ny = (ny / gridSize).roundToInt() * gridSize
            nx = nx.coerceIn(0f, 256f)
            ny = ny.coerceIn(0f, 256f)
        }
        vertex.copy(x = nx, y = ny)
    }
}

fun insertVertexAfterSelection(
    vertices: List<EditableVertex>,
    lastSelectedVertexId: String?
): List<EditableVertex> {
    val newId = nextVertexId(vertices)
    if (vertices.isEmpty()) {
        return listOf(EditableVertex(id = newId, x = 128f, y = 128f))
    }
    val insertAfter = lastSelectedVertexId?.let { id -> vertices.indexOfFirst { it.id == id } }
        ?.takeIf { it >= 0 }
        ?: vertices.lastIndex
    val current = vertices[insertAfter]
    val next = vertices.getOrNull((insertAfter + 1) % vertices.size)
    val (nx, ny) = if (next != null && vertices.size > 1) {
        (current.x + next.x) / 2f to (current.y + next.y) / 2f
    } else {
        (current.x + 16f).coerceIn(0f, 256f) to (current.y + 16f).coerceIn(0f, 256f)
    }
    val newVertex = EditableVertex(
        id = newId,
        x = nx,
        y = ny,
        rounding = current.rounding,
        smoothing = current.smoothing
    )
    return vertices.toMutableList().apply { add(insertAfter + 1, newVertex) }
}

fun ensureDrawnCornerIds(config: DrawnPolygonConfig): DrawnPolygonConfig {
    if (config.perVertexRounding.size == config.vertices.size) return config
    return config.copy(
        perVertexRounding = config.vertices.mapIndexed { index, _ ->
            config.perVertexRounding.getOrNull(index)
                ?: VertexCornerConfig(
                    vertexId = "v$index",
                    rounding = config.rounding,
                    smoothing = config.smoothing
                )
        }
    )
}
