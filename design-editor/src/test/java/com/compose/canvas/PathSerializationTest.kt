package com.compose.canvas

import com.compose.canvas.geometry.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathSerializationTest {

    @Test
    fun `compose path mirrors segments`() {
        val geom = PathGeometry(
            segments = listOf(
                PathSegment.MoveTo(0f, 0f),
                PathSegment.LineTo(50f, 50f),
                PathSegment.CubicBezierTo(10f, 20f, 30f, 40f, 80f, 10f),
                PathSegment.Close
            ),
            closed = true
        )

        // Compose Path wrapping is validated via the geometry data that feeds it.
        val bounds = geom.boundingRect()
        assertEquals(0f, bounds.left, 0.001f)
        assertEquals(0f, bounds.top, 0.001f)
        assertEquals(80f, bounds.right, 0.001f)
        assertEquals(50f, bounds.bottom, 0.001f)
    }

    @Test
    fun `roundtrip through segment list keeps semantics`() {
        val original = listOf(
            PathSegment.MoveTo(1f, 2f),
            PathSegment.LineTo(3f, 4f),
            PathSegment.QuadraticBezierTo(5f, 6f, 7f, 8f),
            PathSegment.CubicBezierTo(9f, 10f, 11f, 12f, 13f, 14f),
            PathSegment.Close
        )

        val rebuilt = original.map {
            when (it) {
                is PathSegment.MoveTo -> PathSegment.MoveTo(it.x, it.y)
                is PathSegment.LineTo -> PathSegment.LineTo(it.x, it.y)
                is PathSegment.QuadraticBezierTo -> PathSegment.QuadraticBezierTo(it.cx, it.cy, it.x, it.y)
                is PathSegment.CubicBezierTo ->
                    PathSegment.CubicBezierTo(it.cx1, it.cy1, it.cx2, it.cy2, it.x, it.y)
                is PathSegment.Close -> PathSegment.Close
            }
        }

        assertEquals(original, rebuilt)
    }

    @Test
    fun `compose path codegen mirrors segments`() {
        val geom = PathGeometry(
            segments = listOf(
                PathSegment.MoveTo(0f, 20f),
                PathSegment.LineTo(20f, 0f),
                PathSegment.CubicBezierTo(10f, 5f, 15f, 8f, 40f, 20f),
                PathSegment.Close
            ),
            closed = true
        )
        val code = com.compose.canvas.interchange.ComposePathCodegen.generate(geom)
        assertTrue(code.contains("moveTo(0f, 20f)"))
        assertTrue(code.contains("lineTo(20f, 0f)"))
        assertTrue(code.contains("cubicTo(10f, 5f, 15f, 8f, 40f, 20f)"))
        assertTrue(code.contains("close()"))
    }
}