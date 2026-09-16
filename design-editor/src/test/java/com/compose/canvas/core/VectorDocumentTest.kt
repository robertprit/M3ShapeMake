package com.compose.canvas.core

import com.compose.canvas.geometry.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VectorDocumentTest {

    @Test
    fun `bounding rect of rect geometry`() {
        val geom = RectGeometry(width = 100f, height = 50f)
        val bounds = geom.boundingRect()
        assertEquals(0f, bounds.left, 0.001f)
        assertEquals(0f, bounds.top, 0.001f)
        assertEquals(100f, bounds.right, 0.001f)
        assertEquals(50f, bounds.bottom, 0.001f)
    }

    @Test
    fun `bounding rect of ellipse geometry`() {
        val geom = EllipseGeometry(radiusX = 30f, radiusY = 20f)
        val bounds = geom.boundingRect()
        assertEquals(-30f, bounds.left, 0.001f)
        assertEquals(-20f, bounds.top, 0.001f)
        assertEquals(30f, bounds.right, 0.001f)
        assertEquals(20f, bounds.bottom, 0.001f)
    }

    @Test
    fun `bounding rect of path geometry covers all points`() {
        val geom = PathGeometry(
            segments = listOf(
                PathSegment.MoveTo(0f, 0f),
                PathSegment.LineTo(100f, 50f),
                PathSegment.CubicBezierTo(50f, 50f, 75f, 25f, 150f, 75f),
                PathSegment.QuadraticBezierTo(120f, 90f, 80f, 90f)
            ),
            closed = false
        )
        val bounds = geom.boundingRect()
        assertEquals(0f, bounds.left, 0.001f)
        assertEquals(0f, bounds.top, 0.001f)
        assertEquals(150f, bounds.right, 0.001f)
        assertEquals(90f, bounds.bottom, 0.001f)
    }

    @Test
    fun `layer add and remove object`() {
        val layer = VectorLayer(name = "L1")
        val rect = com.compose.canvas.core.VectorObject(
            geometry = RectGeometry(width = 10f, height = 10f)
        )

        val withObj = layer.addObject(rect)
        assertEquals(1, withObj.objects.size)

        val removed = withObj.removeObject(rect.id)
        assertEquals(0, removed.objects.size)
    }

    @Test
    fun `document remove object across layer`() {
        val obj = VectorObject(geometry = RectGeometry(width = 10f, height = 10f))
        val doc = VectorDocument(layers = listOf(VectorLayer(objects = listOf(obj))))

        val updated = doc.removeObject(obj.id)
        assertNull(updated.findObject(obj.id))
    }

    @Test
    fun `document replace object`() {
        val obj = VectorObject(name = "Old", geometry = RectGeometry(width = 10f, height = 10f))
        val doc = VectorDocument(layers = listOf(VectorLayer(objects = listOf(obj))))

        val updated = doc.replaceObject(obj.copy(name = "New"))
        assertEquals("New", updated.findObject(obj.id)!!.name)
    }

    @Test
    fun `moving layer pos`() {
        val l1 = VectorLayer(name = "A")
        val l2 = VectorLayer(name = "B")
        val l3 = VectorLayer(name = "C")
        val doc = VectorDocument(layers = listOf(l1, l2, l3))

        val moved = doc.moveLayer(0, 2)
        assertEquals("B", moved.layers[0].name)
        assertEquals("C", moved.layers[1].name)
        assertEquals("A", moved.layers[2].name)
    }
}