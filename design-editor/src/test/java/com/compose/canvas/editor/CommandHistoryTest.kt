package com.compose.canvas.editor

import com.compose.canvas.core.ObjectTransform
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorObject
import com.compose.canvas.geometry.RectGeometry
import com.compose.canvas.history.AddObjectCommand
import com.compose.canvas.history.CommandHistory
import com.compose.canvas.history.EditorCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandHistoryTest {

    private fun makeCommand(label: String): EditorCommand {
        val before = VectorDocument(name = "doc before")
        val after = VectorDocument(name = label)
        val rect = VectorObject(geometry = RectGeometry(width = 10f, height = 10f))
        return AddObjectCommand(rect, 0, before, after, SelectionState(), SelectionState())
    }

    @Test
    fun `push then undo pops and feeds redo`() {
        val history = CommandHistory(maxSteps = 10)
        val cmd = makeCommand("Add")
        history.push(cmd)

        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
        assertEquals(1, history.undoCount)

        val popped = history.popUndo()
        assertEquals(cmd, popped)
        assertFalse(history.canUndo)
        assertTrue(history.canRedo)
        assertEquals(1, history.redoCount)
    }

    @Test
    fun `redo returns command to undo stack`() {
        val history = CommandHistory()
        val cmd = makeCommand("Add")
        history.push(cmd)
        history.popUndo()

        val redone = history.popRedo()
        assertEquals(cmd, redone)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }

    @Test
    fun `new push clears redo stack`() {
        val history = CommandHistory()
        val first = makeCommand("first")
        val second = makeCommand("second")
        history.push(first)
        history.popUndo()

        history.push(second)
        assertFalse(history.canRedo)
        assertEquals(null, history.popRedo())
    }

    @Test
    fun `history is bounded by max steps`() {
        val history = CommandHistory(maxSteps = 3)
        repeat(5) { history.push(makeCommand("cmd$it")) }
        assertEquals(3, history.undoCount)
    }

    @Test
    fun `commands with payloads introspect`() {
        val rect = VectorObject(
            geometry = RectGeometry(width = 10f, height = 10f),
            transform = ObjectTransform(x = 1f, y = 2f)
        )
        val before = VectorDocument()
        val after = before.addObjectToLayer(0, rect)
        val undoSelection = SelectionState()
        val redoSelection = SelectionState(selectedIds = setOf(rect.id))
        val cmd = AddObjectCommand(rect, 0, before, after, undoSelection, redoSelection)

        assertEquals(rect, cmd.node)
        assertEquals("Add ${rect.name}", cmd.label)
        assertEquals(redoSelection, cmd.redoSelection)
        assertEquals(undoSelection, cmd.undoSelection)
    }

    @Test
    fun `applying command surfaces new state`() {
        val editor = EditorState()
        val before = editor.document
        val rect = VectorObject(geometry = RectGeometry(width = 10f, height = 10f))
        val after = before.addObjectToLayer(0, rect)
        val cmd = AddObjectCommand(rect, 0, before, after, SelectionState(), SelectionState(setOf(rect.id)))

        cmd.redo(editor)
        assertEquals(after, editor.document)
        assertTrue(editor.selection.selectedIds.contains(rect.id))
    }
}
