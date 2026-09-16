package com.compose.canvas.asset

/** A typed, host-neutral property keyframe. Values use document units. */
data class PropertyKeyframe(
    val id: String,
    val timeMs: Int,
    val value: Float,
    val easing: CubicBezier = CubicBezier()
)

data class PropertyTrack(
    val id: String,
    val name: String,
    val channel: MotionChannel,
    val targetObjectId: String? = null,
    val keyframes: List<PropertyKeyframe> = emptyList()
) {
    fun valueAt(timeMs: Int): Float? {
        val ordered = keyframes.sortedBy { it.timeMs }
        val before = ordered.lastOrNull { it.timeMs <= timeMs } ?: return ordered.firstOrNull()?.value
        val after = ordered.firstOrNull { it.timeMs >= timeMs } ?: return before.value
        if (before.id == after.id || after.timeMs == before.timeMs) return before.value
        val fraction = ((timeMs - before.timeMs).toFloat() / (after.timeMs - before.timeMs)).coerceIn(0f, 1f)
        return before.value + (after.value - before.value) * fraction
    }
}

data class VisualEvent(
    val id: String,
    val name: String,
    val timeMs: Int,
    val payload: Map<String, String> = emptyMap()
)
