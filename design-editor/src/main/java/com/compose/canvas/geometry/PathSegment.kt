package com.compose.canvas.geometry

/**
 * Neutral path commands. This is **not** an SVG `d` string; exporters map
 * these segments to SVG / Compose Path / Kotlin source later.
 */
sealed interface PathSegment {
    data class MoveTo(val x: Float, val y: Float) : PathSegment
    data class LineTo(val x: Float, val y: Float) : PathSegment
    data class QuadraticBezierTo(
        val cx: Float,
        val cy: Float,
        val x: Float,
        val y: Float
    ) : PathSegment
    data class CubicBezierTo(
        val cx1: Float,
        val cy1: Float,
        val cx2: Float,
        val cy2: Float,
        val x: Float,
        val y: Float
    ) : PathSegment
    data object Close : PathSegment
}
