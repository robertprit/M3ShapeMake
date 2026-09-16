package com.compose.canvas.geometry

/** Axis-aligned rectangle in a single coordinate space (document or screen). */
data class RectF(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float): Boolean =
        x in left..right && y in top..bottom

    fun offset(dx: Float, dy: Float) = RectF(
        left + dx, top + dy, right + dx, bottom + dy
    )

    fun union(other: RectF): RectF = RectF(
        minOf(left, other.left),
        minOf(top, other.top),
        maxOf(right, other.right),
        maxOf(bottom, other.bottom)
    )

    companion object {
        val Empty = RectF(0f, 0f, 0f, 0f)
    }
}
