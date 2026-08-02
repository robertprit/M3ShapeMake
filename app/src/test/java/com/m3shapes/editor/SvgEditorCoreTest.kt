package com.m3shapes.editor

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SvgEditorCoreTest {
    @Test
    fun commandHistory_undoRedo_restoresDocument() {
        val initial = SvgDocument().withElements(listOf(SvgRectElement(x = 10f, y = 20f)))
        val history = SvgCommandHistory(present = initial)
        val id = initial.elements.first().id

        val moved = history.execute("move") { doc ->
            doc.copy(selectedElementIds = setOf(id)).moveSelection(dx = 20f, dy = 30f)
        }
        val movedRect = moved.present.elements.first() as SvgRectElement
        assertEquals(30f, movedRect.x)
        assertEquals(50f, movedRect.y)

        val undone = moved.undo()
        val undoneRect = undone.present.elements.first() as SvgRectElement
        assertEquals(10f, undoneRect.x)
        assertEquals(20f, undoneRect.y)

        val redone = undone.redo()
        val redoneRect = redone.present.elements.first() as SvgRectElement
        assertEquals(30f, redoneRect.x)
        assertEquals(50f, redoneRect.y)
    }

    @Test
    fun insertDeletePathNode_changesNodeCount() {
        val path = SvgPathElement(
            nodes = listOf(
                SvgPathNode(id = "a", anchor = Offset(0f, 0f), type = SvgPathSegmentType.Move),
                SvgPathNode(id = "b", anchor = Offset(10f, 0f)),
                SvgPathNode(id = "c", anchor = Offset(10f, 10f))
            )
        )
        val doc = SvgDocument().withElements(listOf(path))
        val ui = SvgEditorUiState(selectedNodeElementId = path.id, selectedNodeId = "a")

        val (withInsert, insertedUi) = doc.insertNodeAfterSelection(ui)
        val insertedPath = withInsert.elements.first() as SvgPathElement
        assertEquals(4, insertedPath.nodes.size)
        assertTrue(insertedUi.selectedNodeId != null)

        val (withDelete, _) = withInsert.deleteSelectedNode(insertedUi)
        val deletedPath = withDelete.elements.first() as SvgPathElement
        assertEquals(3, deletedPath.nodes.size)
    }
}
