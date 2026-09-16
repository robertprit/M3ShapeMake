package com.compose.canvas.core

/**
 * Local object transform in document space.
 *
 * Visual world position is `translate(x, y) * rotate(rotation) * scale(scaleX, scaleY)`
 * applied to local geometry. Zoom/pan never write these values.
 */
data class ObjectTransform(
    val x: Float = 0f,
    val y: Float = 0f,
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f
)
