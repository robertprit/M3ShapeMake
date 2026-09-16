package com.compose.canvas.math

import androidx.compose.ui.geometry.Offset
import com.compose.canvas.core.ObjectTransform
import org.junit.Assert.assertEquals
import org.junit.Test

class TransformDecomposeTest {

    @Test
    fun `fromObjectTransform maps origin to translation`() {
        val m = TransformUtils.fromObjectTransform(
            ObjectTransform(x = 100f, y = 50f, rotation = 0f, scaleX = 1f, scaleY = 1f)
        )
        val p = m.transformPoint(0f, 0f)
        assertEquals(100f, p.x, 0.001f)
        assertEquals(50f, p.y, 0.001f)
    }

    @Test
    fun `fromObjectTransform applies scale before rotation`() {
        val m = TransformUtils.fromObjectTransform(
            ObjectTransform(x = 0f, y = 0f, rotation = 0f, scaleX = 2f, scaleY = 3f)
        )
        val p = m.transformPoint(10f, 10f)
        assertEquals(20f, p.x, 0.001f)
        assertEquals(30f, p.y, 0.001f)
    }

    @Test
    fun `decompose roundtrips translation rotation and scale`() {
        val original = ObjectTransform(x = 150f, y = -40f, rotation = 30f, scaleX = 2f, scaleY = 1.5f)
        val matrix = TransformUtils.fromObjectTransform(original)
        val restored = TransformUtils.decomposeObjectTransform(matrix)

        assertEquals(original.x, restored.x, 0.01f)
        assertEquals(original.y, restored.y, 0.01f)
        assertEquals(original.rotation, restored.rotation, 0.01f)
        assertEquals(original.scaleX, restored.scaleX, 0.01f)
        assertEquals(original.scaleY, restored.scaleY, 0.01f)
    }

    @Test
    fun `decomposed transform maps same points`() {
        val original = ObjectTransform(x = 100f, y = 10f, rotation = 45f, scaleX = 1.2f, scaleY = 0.8f)
        val matrix = TransformUtils.fromObjectTransform(original)
        val restoredMatrix = TransformUtils.fromObjectTransform(
            TransformUtils.decomposeObjectTransform(matrix)
        )

        val testPoint = Offset(50f, 25f)
        val expected = matrix.transformPoint(testPoint)
        val actual = restoredMatrix.transformPoint(testPoint)
        assertEquals(expected.x, actual.x, 0.01f)
        assertEquals(expected.y, actual.y, 0.01f)
    }

    @Test
    fun `composed parent and child matrix decomposes`() {
        val parent = ObjectTransform(x = 100f, y = 50f, rotation = 0f, scaleX = 2f, scaleY = 2f)
        val child = ObjectTransform(x = 10f, y = 20f, rotation = 0f, scaleX = 1f, scaleY = 1f)

        val world = TransformUtils.multiply(
            TransformUtils.fromObjectTransform(parent),
            TransformUtils.fromObjectTransform(child)
        )
        val baked = TransformUtils.decomposeObjectTransform(world)

        // Final world position == group space applied first: 100 + 2*10
        assertEquals(120f, baked.x, 0.01f)
        assertEquals(90f, baked.y, 0.01f)
        assertEquals(2f, baked.scaleX, 0.01f)
        assertEquals(2f, baked.scaleY, 0.01f)
    }
}