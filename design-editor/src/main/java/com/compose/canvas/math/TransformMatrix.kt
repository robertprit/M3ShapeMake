package com.compose.canvas.math

import androidx.compose.ui.geometry.Offset

data class TransformMatrix(
    val m00: Float = 1f, val m01: Float = 0f, val m02: Float = 0f,
    val m10: Float = 0f, val m11: Float = 1f, val m12: Float = 0f,
    val m20: Float = 0f, val m21: Float = 0f, val m22: Float = 1f
) {
    fun transformPoint(x: Float, y: Float): Offset {
        val nx = m00 * x + m01 * y + m02
        val ny = m10 * x + m11 * y + m12
        return Offset(nx, ny)
    }

    fun transformPoint(p: Offset): Offset = transformPoint(p.x, p.y)

    fun translate(dx: Float, dy: Float): TransformMatrix = copy(
        m02 = m02 + dx,
        m12 = m12 + dy
    )

    fun scale(sx: Float, sy: Float): TransformMatrix = copy(
        m00 = m00 * sx, m01 = m01 * sy,
        m10 = m10 * sx, m11 = m11 * sy
    )

    fun rotate(degrees: Float): TransformMatrix {
        val rad = Math.toRadians(degrees.toDouble()).toFloat()
        val cos = kotlin.math.cos(rad)
        val sin = kotlin.math.sin(rad)
        return copy(
            m00 = m00 * cos - m01 * sin,
            m01 = m00 * sin + m01 * cos,
            m10 = m10 * cos - m11 * sin,
            m11 = m10 * sin + m11 * cos
        )
    }

    fun invert(): TransformMatrix? {
        val det = m00 * (m11 * m22 - m12 * m21) -
                m01 * (m10 * m22 - m12 * m20) +
                m02 * (m10 * m21 - m11 * m20)
        if (kotlin.math.abs(det) < 1e-10f) return null
        val invDet = 1f / det
        return copy(
            m00 = (m11 * m22 - m12 * m21) * invDet,
            m01 = (m02 * m21 - m01 * m22) * invDet,
            m02 = (m01 * m12 - m02 * m11) * invDet,
            m10 = (m12 * m20 - m10 * m22) * invDet,
            m11 = (m00 * m22 - m02 * m20) * invDet,
            m12 = (m02 * m10 - m00 * m12) * invDet,
            m20 = (m10 * m21 - m11 * m20) * invDet,
            m21 = (m01 * m20 - m00 * m21) * invDet,
            m22 = (m00 * m11 - m01 * m10) * invDet
        )
    }
}

object TransformUtils {
    fun translation(x: Float, y: Float): TransformMatrix =
        TransformMatrix(m02 = x, m12 = y)

    fun scale(sx: Float, sy: Float): TransformMatrix =
        TransformMatrix(m00 = sx, m11 = sy)

    fun rotation(degrees: Float): TransformMatrix {
        val rad = Math.toRadians(degrees.toDouble()).toFloat()
        val cos = kotlin.math.cos(rad)
        val sin = kotlin.math.sin(rad)
        return TransformMatrix(m00 = cos, m01 = -sin, m10 = sin, m11 = cos)
    }

    fun multiply(a: TransformMatrix, b: TransformMatrix): TransformMatrix = TransformMatrix(
        m00 = a.m00 * b.m00 + a.m01 * b.m10 + a.m02 * b.m20,
        m01 = a.m00 * b.m01 + a.m01 * b.m11 + a.m02 * b.m21,
        m02 = a.m00 * b.m02 + a.m01 * b.m12 + a.m02 * b.m22,
        m10 = a.m10 * b.m00 + a.m11 * b.m10 + a.m12 * b.m20,
        m11 = a.m10 * b.m01 + a.m11 * b.m11 + a.m12 * b.m21,
        m12 = a.m10 * b.m02 + a.m11 * b.m12 + a.m12 * b.m22,
        m20 = a.m20 * b.m00 + a.m21 * b.m10 + a.m22 * b.m20,
        m21 = a.m20 * b.m01 + a.m21 * b.m11 + a.m22 * b.m21,
        m22 = a.m20 * b.m02 + a.m21 * b.m12 + a.m22 * b.m22
    )

    fun fromObjectTransform(t: com.compose.canvas.core.ObjectTransform): TransformMatrix {
        val s = scale(t.scaleX, t.scaleY)
        val r = rotation(t.rotation)
        val rs = multiply(r, s)
        return multiply(translation(t.x, t.y), rs)
    }

    /**
     * Decomposes a general affine matrix (translation * rotation * scale) back into an
     * [com.compose.canvas.core.ObjectTransform] using polar decomposition of the linear part.
     */
    fun decomposeObjectTransform(m: TransformMatrix): com.compose.canvas.core.ObjectTransform {
        val x = m.m02
        val y = m.m12

        var r00 = m.m00
        var r01 = m.m01
        var r10 = m.m10
        var r11 = m.m11

        repeat(20) {
            val det = r00 * r11 - r01 * r10
            if (kotlin.math.abs(det) < 1e-10f) return@repeat
            val inv = 1f / det
            // R = (R + R^-T) / 2 where det>0 keeps a proper rotation
            val n00 = 0.5f * (r00 + r11 * inv)
            val n01 = 0.5f * (r01 - r10 * inv)
            val n10 = 0.5f * (r10 - r01 * inv)
            val n11 = 0.5f * (r11 + r00 * inv)
            r00 = n00; r01 = n01; r10 = n10; r11 = n11
        }

        // S = R^T * A
        val s00 = r00 * m.m00 + r10 * m.m10
        val s01 = r00 * m.m01 + r10 * m.m11
        val s10 = r01 * m.m00 + r11 * m.m10
        val s11 = r01 * m.m01 + r11 * m.m11

        var scaleX = s00
        var scaleY = s11
        var rotation = kotlin.math.atan2(r10, r00) * 180f / kotlin.math.PI.toFloat()

        if (scaleX < 0f || scaleY < 0f) {
            // Handle reflections by flipping scale into rotation-free space where possible.
            val flipX = if (scaleX < 0f) -1f else 1f
            val flipY = if (scaleY < 0f) -1f else 1f
            scaleX *= flipX
            scaleY *= flipY
            rotation = kotlin.math.atan2(r10 * flipX, r00 * flipX) * 180f / kotlin.math.PI.toFloat()
        }

        return com.compose.canvas.core.ObjectTransform(
            x = x,
            y = y,
            rotation = rotation,
            scaleX = scaleX,
            scaleY = scaleY
        )
    }
}
