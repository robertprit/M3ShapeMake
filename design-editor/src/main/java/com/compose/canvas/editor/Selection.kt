package com.compose.canvas.editor

import com.compose.canvas.geometry.ObjectId

enum class EditorTool {
    Select,
    Rectangle,
    Ellipse,
    Line,
    Polygon,
    Star,
    Triangle,
    Path,
    Pencil,
    Text,
    EditPath,
    Port,
    Anchor,
    ContentArea,
    Gradient,
    Pan
}

enum class GeometryType { Rectangle, Ellipse, Line, Polygon, Star, Triangle }

enum class HandlePosition {
    TopLeft, TopCenter, TopRight,
    MiddleLeft, MiddleRight,
    BottomLeft, BottomCenter, BottomRight,
    Rotate
}

data class SelectionState(
    val selectedIds: Set<ObjectId> = emptySet()
) {
    val isEmpty: Boolean get() = selectedIds.isEmpty()
    val isSingle: Boolean get() = selectedIds.size == 1
    val singleId: ObjectId? get() = selectedIds.firstOrNull()

    fun select(id: ObjectId): SelectionState = copy(selectedIds = setOf(id))
    fun toggleSelection(id: ObjectId): SelectionState = copy(
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    )
    fun clear(): SelectionState = copy(selectedIds = emptySet())
}

data class DragState(
    val active: Boolean = false,
    val startDocPoint: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero,
    val currentDocPoint: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero
) {
    val deltaDoc: androidx.compose.ui.geometry.Offset
        get() = currentDocPoint - startDocPoint
}

data class CreateShapeState(
    val active: Boolean = false,
    val geometryType: GeometryType = GeometryType.Rectangle,
    val startDocPoint: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero,
    val currentDocPoint: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero
)

data class PathDrawingState(
    val active: Boolean = false,
    val points: List<androidx.compose.ui.geometry.Offset> = emptyList()
)

data class ResizeSession(
    val active: Boolean = false,
    val handle: HandlePosition? = null,
    val startBounds: com.compose.canvas.geometry.RectF? = null,
    val currentBounds: com.compose.canvas.geometry.RectF? = null
)

/** Chrome that must not mutate [com.compose.canvas.core.VectorDocument]. */
data class PanelState(
    val toolsExpanded: Boolean = true,
    val inspectorExpanded: Boolean = true,
    val layersExpanded: Boolean = true,
    val exportExpanded: Boolean = false
)

data class MarqueeState(
    val active: Boolean = false,
    val start: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero,
    val current: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero
)

data class RotateSession(
    val active: Boolean = false,
    val center: androidx.compose.ui.geometry.Offset = androidx.compose.ui.geometry.Offset.Zero,
    val startAngle: Float = 0f,
    val currentAngle: Float = 0f
)

data class PathEditState(
    val active: Boolean = false,
    val objectId: com.compose.canvas.geometry.ObjectId? = null,
    val data: com.compose.canvas.tools.path.PathData = com.compose.canvas.tools.path.PathData(),
    val selectedNode: com.compose.canvas.tools.path.NodeRef? = null,
    val selectedHandle: com.compose.canvas.tools.path.HandleRef? = null,
    val dragTargetNode: com.compose.canvas.tools.path.NodeRef? = null,
    val dragTargetHandle: com.compose.canvas.tools.path.HandleRef? = null,
    val dragStartPoint: androidx.compose.ui.geometry.Offset? = null
)

data class PencilDrawingState(
    val active: Boolean = false,
    val points: List<androidx.compose.ui.geometry.Offset> = emptyList()
)
