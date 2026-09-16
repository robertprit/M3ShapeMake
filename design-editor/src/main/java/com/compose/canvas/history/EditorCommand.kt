package com.compose.canvas.history

import com.compose.canvas.core.VectorDocument
import com.compose.canvas.editor.EditorState
import com.compose.canvas.editor.SelectionState

/**
 * Reversible document operation. Continuous gestures (move/resize) emit **one**
 * command on pointer-up, never per-move events.
 */
interface EditorCommand {
    val label: String
    fun undo(state: EditorState)
    fun redo(state: EditorState)
}

open class DocumentCommand(
    val beforeDocument: VectorDocument,
    val afterDocument: VectorDocument,
    val undoSelection: SelectionState,
    val redoSelection: SelectionState,
    override val label: String
) : EditorCommand {
    override fun undo(state: EditorState) {
        state.restoreDocument(beforeDocument)
        state.restoreSelection(undoSelection)
    }

    override fun redo(state: EditorState) {
        state.restoreDocument(afterDocument)
        state.restoreSelection(redoSelection)
    }
}

class CommandHistory(
    private val maxSteps: Int = 100
) {
    private val undoStack = mutableListOf<EditorCommand>()
    private val redoStack = mutableListOf<EditorCommand>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size

    fun push(cmd: EditorCommand) {
        undoStack.add(cmd)
        if (undoStack.size > maxSteps) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun popUndo(): EditorCommand? {
        if (undoStack.isEmpty()) return null
        val cmd = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(cmd)
        return cmd
    }

    fun popRedo(): EditorCommand? {
        if (redoStack.isEmpty()) return null
        val cmd = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(cmd)
        return cmd
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
