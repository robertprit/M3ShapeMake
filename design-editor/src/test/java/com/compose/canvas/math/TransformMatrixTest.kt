package com.compose.canvas.math

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class TransformMatrixTest {

    @Test
    fun `inverse restores original point`() {
        val matrix = TransformUtils.translation(50f, 30f)
        val composed = TransformUtils.multiply(
            TransformUtils.scale(2f, 2f),
            matrix
        )

        val point = Offset(100f, 200f)
        val transformed = composed.transformPoint(point)
        val inverse = composed.invert()
        assert(inverse != null)

        val restored = inverse!!.transformPoint(transformed)
        assertEquals(point.x, restored.x, 0.001f)
        assertEquals(point.y, restored.y, 0.001f)
    }

    @Test
    fun `rotation keeps distance from origin`() {
        val rotation = TransformUtils.rotation(90f)
        val point = rotation.transformPoint(Offset(10f, 0f))
        assertEquals(0f, point.x, 0.001f)
        assertEquals(10f, point.y, 0.001f)
    }

    @Test
    fun `translation moves point correctly`() {
        val translation = TransformUtils.translation(5f, -3f)
        val point = translation.transformPoint(Offset(1f, 1f))
        assertEquals(6f, point.x, 0.001f)
        assertEquals(-2f, point.y, 0.001f)
    }

    @Test
    fun `non-invertible matrix returns null`() {
        val degenerate = TransformMatrix(
            m00 = 0f, m01 = 0f, m02 = 0f,
            m10 = 0f, m11 = 0f, m12 = 0f
        )
        assert(degenerate.invert() == null)
    }
}