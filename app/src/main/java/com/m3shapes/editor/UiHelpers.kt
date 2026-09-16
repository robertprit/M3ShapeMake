package com.m3shapes.editor

import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow

internal val studioColors = listOf(
    Color(0xFF6750A4), Color(0xFF1E88E5), Color(0xFF00897B),
    Color(0xFF43A047), Color(0xFFFFB300), Color(0xFFE53935),
    Color(0xFFD81B60), Color(0xFF212121)
)

internal fun motionProgress(
    raw: Float,
    type: MotionPathType,
    profile: BezierProfile,
    springStiffness: Float,
    springDamping: Float,
    bounceGravity: Float
): Float {
    val t = raw.coerceIn(0f, 1f)
    return when (type) {
        MotionPathType.Tween -> profile.curveY(t)
        MotionPathType.Spring -> {
            val damping = springDamping.coerceIn(0.05f, 2f)
            val stiffness = springStiffness.coerceIn(50f, 2_000f)
            val frequency = (stiffness / 450f).coerceIn(0.25f, 5f)
            val decay = (damping * 3.2f).coerceIn(0.2f, 6f)
            1f - (exp((-decay * t).toDouble()).toFloat() *
                cos(t * frequency * Math.PI.toFloat() * 2f))
        }
        MotionPathType.Bounce -> gravityBounceEasing(bounceGravity).transform(t)
    }
}

internal fun gravityBounceEasing(gravity: Float): Easing {
    val g = gravity.coerceIn(0.1f, 3f)
    return Easing { tRaw ->
        val t = tRaw.coerceIn(0f, 1f)
        val bounces = (2f + g * 2.2f).coerceIn(2f, 8f)
        val decay = (2.8f + g * 1.9f).coerceIn(2f, 10f)
        val oscillation = kotlin.math.abs(cos(t * bounces * Math.PI.toFloat()))
        val envelope = exp((-decay * t).toDouble()).toFloat()
        val value = 1f - (oscillation * envelope).coerceIn(0f, 1f)
        value.pow((0.9f + g * 0.2f).coerceIn(0.9f, 1.5f)).coerceIn(0f, 1f)
    }
}
