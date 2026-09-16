package com.compose.canvas.history

import com.compose.canvas.core.SceneNode
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.editor.SelectionState
import com.compose.canvas.geometry.ObjectId

class AddObjectCommand(
    val node: SceneNode,
    val layerIndex: Int,
    beforeDocument: VectorDocument,
    afterDocument: VectorDocument,
    undoSelection: SelectionState,
    redoSelection: SelectionState
) : DocumentCommand(beforeDocument, afterDocument, undoSelection, redoSelection, "Add ${node.name}")

class RemoveObjectCommand(
    val removedIds: List<ObjectId>,
    beforeDocument: VectorDocument,
    afterDocument: VectorDocument,
    undoSelection: SelectionState,
    redoSelection: SelectionState
) : DocumentCommand(
    beforeDocument,
    afterDocument,
    undoSelection,
    redoSelection,
    "Delete"
)

class DuplicateObjectCommand(
    beforeDocument: VectorDocument,
    afterDocument: VectorDocument,
    undoSelection: SelectionState,
    redoSelection: SelectionState
) : DocumentCommand(beforeDocument, afterDocument, undoSelection, redoSelection, "Duplicate")

class MoveObjectCommand(
    val dx: Float,
    val dy: Float,
    beforeDocument: VectorDocument,
    afterDocument: VectorDocument,
    undoSelection: SelectionState,
    redoSelection: SelectionState
) : DocumentCommand(beforeDocument, afterDocument, undoSelection, redoSelection, "Move")

class ResizeObjectCommand(
    beforeDocument: VectorDocument,
    afterDocument: VectorDocument,
    undoSelection: SelectionState,
    redoSelection: SelectionState
) : DocumentCommand(beforeDocument, afterDocument, undoSelection, redoSelection, "Resize")

class ReplaceObjectCommand(
    beforeDocument: VectorDocument,
    afterDocument: VectorDocument,
    undoSelection: SelectionState,
    redoSelection: SelectionState,
    label: String = "Edit"
) : DocumentCommand(beforeDocument, afterDocument, undoSelection, redoSelection, label)

class GenericCommand(
    beforeDocument: VectorDocument,
    afterDocument: VectorDocument,
    undoSelection: SelectionState,
    redoSelection: SelectionState,
    label: String
) : DocumentCommand(beforeDocument, afterDocument, undoSelection, redoSelection, label)
