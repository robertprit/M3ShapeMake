package com.compose.canvas.editor

import androidx.compose.ui.geometry.Offset
import com.compose.canvas.core.*
import com.compose.canvas.geometry.*
import org.junit.Assert.*
import org.junit.Test

class EditorStateTest {

    private fun makeEditor() = EditorState(
        initialDocument = VectorDocument(
            layers = listOf(VectorLayer(name = "L1"))
        )
    )

    @Test
    fun `adding rectangle creates object on active layer`() {
        val editor = makeEditor()
        val rect = VectorObject(
            geometry = RectGeometry(width = 100f, height = 50f),
            transform = ObjectTransform(x = 10f, y = 20f)
        )

        editor.addObjectToActiveLayer(rect)

        val found = editor.document.findObject(rect.id)
        assertNotNull(found)
        assertEquals(100f, (found!!.geometry as RectGeometry).width, 0.001f)
        assertTrue(editor.selection.selectedIds.contains(rect.id))
    }

    @Test
    fun `move selected object changes transform not geometry`() {
        val editor = makeEditor()
        val rect = VectorObject(
            geometry = RectGeometry(width = 100f, height = 50f),
            transform = ObjectTransform(x = 10f, y = 20f)
        )
        editor.addObjectToActiveLayer(rect)

        editor.startDrag(Offset(500f, 500f))
        editor.updateDrag(Offset(530f, 510f))
        editor.endDrag()

        val moved = editor.document.findObject(rect.id)
        assertEquals(40f, moved!!.transform.x, 0.001f)
        assertEquals(30f, moved.transform.y, 0.001f)
        val geom = moved.geometry as RectGeometry
        assertEquals(100f, geom.width, 0.001f)
        assertEquals(50f, geom.height, 0.001f)
    }

    @Test
    fun `gesture produces single history entry`() {
        val editor = makeEditor()
        val rect = VectorObject(geometry = RectGeometry(width = 50f, height = 50f))
        editor.addObjectToActiveLayer(rect)

        val sizeBefore = editor.history.undoCount

        editor.startDrag(Offset(10f, 10f))
        repeat(50) { i ->
            editor.updateDrag(Offset(10f + i, 10f + i))
        }
        editor.endDrag()

        assertEquals(sizeBefore + 1, editor.history.undoCount)
    }

    @Test
    fun `undo restores previous document`() {
        val editor = makeEditor()
        val before = editor.document
        val rect = VectorObject(geometry = RectGeometry(width = 50f, height = 50f))
        editor.addObjectToActiveLayer(rect)
        assertNotEquals(before.layers[0].objects.size, editor.document.layers[0].objects.size)

        val undone = editor.undo()
        assertTrue(undone)
        assertEquals(before, editor.document)
    }

    @Test
    fun `redo restores undone change`() {
        val editor = makeEditor()
        val rect = VectorObject(name = "RectA", geometry = RectGeometry(width = 50f, height = 50f))
        editor.addObjectToActiveLayer(rect)
        editor.undo()
        editor.redo()

        val restored = editor.document.findObject(rect.id)
        assertNotNull(restored)
        assertEquals("RectA", restored!!.name)
    }

    @Test
    fun `create shape via drag produces correct geometry`() {
        val editor = makeEditor()
        editor.setTool(EditorTool.Rectangle)
        editor.startCreateShape(GeometryType.Rectangle, Offset(100f, 100f))
        editor.updateCreateShape(Offset(250f, 320f))

        val created = editor.finishCreateShape()
        assertNotNull(created)

        val geom = created!!.geometry as RectGeometry
        assertEquals(150f, geom.width, 0.001f)
        assertEquals(220f, geom.height, 0.001f)
        assertEquals(100f, created.transform.x, 0.001f)
        assertEquals(100f, created.transform.y, 0.001f)
    }

    @Test
    fun `create ellipse is centered on drag bounds`() {
        val editor = makeEditor()
        editor.startCreateShape(GeometryType.Ellipse, Offset(0f, 0f))
        editor.updateCreateShape(Offset(100f, 50f))
        val created = editor.finishCreateShape()!!
        val geom = created.geometry as EllipseGeometry
        assertEquals(50f, geom.radiusX, 0.001f)
        assertEquals(25f, geom.radiusY, 0.001f)
        assertEquals(50f, created.transform.x, 0.001f)
        assertEquals(25f, created.transform.y, 0.001f)
    }

    @Test
    fun `delete removes object and clears selection`() {
        val editor = makeEditor()
        val rect = VectorObject(geometry = RectGeometry(width = 50f, height = 50f))
        editor.addObjectToActiveLayer(rect)
        assertTrue(editor.document.findObject(rect.id) != null)

        editor.deleteSelected()
        assertNull(editor.document.findObject(rect.id))
        assertTrue(editor.selection.isEmpty)
    }

    @Test
    fun `deleteObject removes only that element`() {
        val editor = makeEditor()
        val first = VectorObject(name = "A", geometry = RectGeometry(width = 40f, height = 40f))
        val second = VectorObject(name = "B", geometry = RectGeometry(width = 40f, height = 40f))
        editor.addObjectToActiveLayer(first)
        editor.addObjectToActiveLayer(second)

        editor.deleteObject(first.id)

        assertNull(editor.document.findObject(first.id))
        assertNotNull(editor.document.findObject(second.id))
        assertTrue(editor.selection.selectedIds.contains(second.id))
    }

    @Test
    fun `duplicate creates copy with offset`() {
        val editor = makeEditor()
        val rect = VectorObject(geometry = RectGeometry(width = 50f, height = 50f))
        editor.addObjectToActiveLayer(rect)

        editor.duplicateSelected()

        val all = editor.document.allObjects()
        assertEquals(2, all.size)
    }

    @Test
    fun `resize scales transform and position`() {
        val editor = makeEditor()
        val rect = VectorObject(
            geometry = RectGeometry(width = 100f, height = 100f),
            transform = ObjectTransform(x = 50f, y = 50f)
        )
        editor.addObjectToActiveLayer(rect)

        editor.startResize(
            HandlePosition.BottomRight,
            RectF(50f, 50f, 150f, 150f)
        )
        editor.updateResizeBounds(RectF(50f, 50f, 200f, 150f))
        editor.finishResize(RectF(50f, 50f, 200f, 150f))

        val resized = editor.document.findObject(rect.id)!!
        assertEquals(50f, resized.transform.x, 0.001f)
        assertEquals(50f, resized.transform.y, 0.001f)
        assertEquals(1.5f, resized.transform.scaleX, 0.001f)
        assertEquals(1f, resized.transform.scaleY, 0.001f)
    }

    @Test
    fun `resize top-left shifts position correctly`() {
        val editor = makeEditor()
        val rect = VectorObject(
            geometry = RectGeometry(width = 100f, height = 100f),
            transform = ObjectTransform(x = 100f, y = 100f)
        )
        editor.addObjectToActiveLayer(rect)

        // start bounds: (100,100,200,200)
        editor.startResize(HandlePosition.TopLeft, RectF(100f, 100f, 200f, 200f))
        // new top-left moves to 80, 80 → bounds become (80,80,200,200)
        editor.finishResize(RectF(80f, 80f, 200f, 200f))

        val resized = editor.document.findObject(rect.id)!!
        // localX = 100-100=0; localY = 100-100=0; scaleX = 120/100=1.2
        assertEquals(80f, resized.transform.x, 0.001f)
        assertEquals(80f, resized.transform.y, 0.001f)
        assertEquals(1.2f, resized.transform.scaleX, 0.001f)
        assertEquals(1.2f, resized.transform.scaleY, 0.001f)
    }

    @Test
    fun `zoom and pan do not affect stored document coordinates`() {
        val editor = makeEditor()
        val rect = VectorObject(
            geometry = RectGeometry(width = 100f, height = 50f),
            transform = ObjectTransform(x = 123f, y = 456f)
        )
        editor.addObjectToActiveLayer(rect)

        val before = editor.document.findObject(rect.id)!!.transform

        // Simulate heavy viewport manipulation
        repeat(25) {
            val vp = com.compose.canvas.viewport.ViewportState(
                zoom = 0.5f + it * 0.1f,
                panX = it * 11f,
                panY = -it * 7f
            )
            vp.screenToDocument(500f, 500f)
            vp.documentToScreen(before.x, before.y)
        }

        val after = editor.document.findObject(rect.id)!!.transform
        assertEquals(before.x, after.x, 0.001f)
        assertEquals(before.y, after.y, 0.001f)
        assertEquals(before.scaleX, after.scaleX, 0.001f)
    }
}