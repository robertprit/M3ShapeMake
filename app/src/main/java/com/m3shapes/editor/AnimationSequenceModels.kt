package com.m3shapes.editor

data class FloatRangeValue(
    val start: Float,
    val end: Float
) {
    val closed: ClosedFloatingPointRange<Float> get() = start..end

    fun coerced(bounds: ClosedFloatingPointRange<Float>): FloatRangeValue {
        val lo = minOf(start, end).coerceIn(bounds.start, bounds.endInclusive)
        val hi = maxOf(start, end).coerceIn(bounds.start, bounds.endInclusive)
        return FloatRangeValue(start = lo, end = hi.coerceAtLeast(lo))
    }

    fun display(reverse: Boolean): Float = if (reverse) end else start
}

/**
 * A cubic Bézier easing profile with control points P1=(x1, y1) and P2=(x2, y2).
 * The curve runs from (0, 0) to (1, 1). x-coordinates are time (clamped to 0..1),
 * y-coordinates are output progress (may overshoot 0..1 for bounce/anticipation).
 */
data class BezierProfile(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
) {
    val isValid: Boolean
        get() = x1.isFinite() && y1.isFinite() && x2.isFinite() && y2.isFinite()

    val isDefault: Boolean
        get() = matches(Linear)

    fun clamped(): BezierProfile = BezierProfile(
        x1 = x1.coerceIn(0f, 1f),
        y1 = y1.coerceIn(-1f, 2f),
        x2 = x2.coerceIn(0f, 1f),
        y2 = y2.coerceIn(-1f, 2f)
    )

    fun matches(other: BezierProfile, epsilon: Float = 0.005f): Boolean =
        kotlin.math.abs(x1 - other.x1) <= epsilon &&
            kotlin.math.abs(y1 - other.y1) <= epsilon &&
            kotlin.math.abs(x2 - other.x2) <= epsilon &&
            kotlin.math.abs(y2 - other.y2) <= epsilon

    fun curveX(t: Float): Float {
        val u = 1f - t
        return 3f * u * u * t * x1 + 3f * u * t * t * x2 + t * t * t
    }

    fun curveY(t: Float): Float {
        val u = 1f - t
        return 3f * u * u * t * y1 + 3f * u * t * t * y2 + t * t * t
    }

    companion object {
        val Linear = BezierProfile(0f, 0f, 1f, 1f)
        val EaseIn = BezierProfile(0.42f, 0f, 1f, 1f)
        val EaseOut = BezierProfile(0f, 0f, 0.58f, 1f)
        val EaseInOut = BezierProfile(0.42f, 0f, 0.58f, 1f)
        val Anticipate = BezierProfile(0.36f, 0f, 0.66f, -0.56f)
        val Overshoot = BezierProfile(0.34f, 1.56f, 0.64f, 1f)
    }
}

data class AnimationStep(
    val id: String,
    val name: String,
    val enabledAnimations: Set<AnimationKind>,
    val startMs: Int,
    val endMs: Int,
    val durationMs: Int,
    val profile: BezierProfile,
    val reverse: Boolean,
    val morphRange: FloatRangeValue,
    val rotateRange: FloatRangeValue,
    val scaleRange: FloatRangeValue,
    val alphaRange: FloatRangeValue,
    val colorRange: FloatRangeValue,
    val offsetXRange: FloatRangeValue,
    val offsetYRange: FloatRangeValue,
    val holdMs: Int = 0
)

enum class SequencerDirection(val label: String) {
    Forward("Vorwaerts"),
    Reverse("Rueckwaerts"),
    PingPong("PingPong"),
    StartEnd("Start-End")
}

enum class SequencerPlayMode(val label: String) {
    Once("1x"),
    Loop("Loop")
}

data class AnimationSequence(
    val id: String,
    val name: String,
    val steps: List<AnimationStep> = emptyList()
)

fun createAnimationStepFromCurrentConfig(
    stepNumber: Int,
    kind: AnimationKind,
    startMs: Int,
    durationMs: Int,
    profile: BezierProfile,
    reverse: Boolean,
    morphRange: FloatRangeValue,
    rotateRange: FloatRangeValue,
    scaleRange: FloatRangeValue,
    alphaRange: FloatRangeValue,
    colorRange: FloatRangeValue,
    offsetXRange: FloatRangeValue,
    offsetYRange: FloatRangeValue,
    holdMs: Int = 0
): AnimationStep = AnimationStep(
    id = "step_$stepNumber",
    name = "${kind.label} $stepNumber",
    enabledAnimations = setOf(kind),
    startMs = startMs.coerceAtLeast(0),
    endMs = (startMs.coerceAtLeast(0) + durationMs.coerceIn(100, 10_000)),
    durationMs = durationMs.coerceIn(100, 10_000),
    profile = profile.clamped(),
    reverse = reverse,
    morphRange = morphRange.coerced(0f..1f),
    rotateRange = rotateRange.coerced(0f..360f),
    scaleRange = scaleRange.coerced(0.5f..2f),
    alphaRange = alphaRange.coerced(0.05f..1f),
    colorRange = colorRange.coerced(0f..1f),
    offsetXRange = offsetXRange.coerced(-80f..80f),
    offsetYRange = offsetYRange.coerced(-80f..80f),
    holdMs = holdMs.coerceAtLeast(0)
)

fun AnimationStep.summary(): String {
    val animLabel = enabledAnimations.firstOrNull()?.label ?: "Manual"
    val dir = if (reverse) "rev" else "fwd"
    val profileLabel = StudioEasing.entries.firstOrNull { it.profile().matches(profile) }?.label
        ?: "Custom"
    return "$name · $animLabel · ${startMs}..${endMs}ms · $profileLabel · $dir"
}

fun AnimationStep.duplicated(newId: String, newName: String): AnimationStep =
    copy(id = newId, name = newName)

fun List<AnimationStep>.moveStep(index: Int, delta: Int): List<AnimationStep> {
    val target = index + delta
    if (index !in indices || target !in indices) return this
    return toMutableList().apply {
        val item = removeAt(index)
        add(target, item)
    }
}

fun AnimationStep.kindOrNull(): AnimationKind? = enabledAnimations.firstOrNull()

fun AnimationStep.kindOrFallback(): AnimationKind = kindOrNull() ?: AnimationKind.Morph

fun AnimationStep.withTimeline(startMs: Int, endMs: Int): AnimationStep {
    val safeStart = startMs.coerceAtLeast(0)
    val safeEnd = endMs.coerceAtLeast(safeStart + 100)
    return copy(
        startMs = safeStart,
        endMs = safeEnd,
        durationMs = (safeEnd - safeStart).coerceAtLeast(100)
    )
}

fun List<AnimationStep>.normalizedTimeline(): List<AnimationStep> =
    sortedBy { it.startMs }.map { step ->
        val safeStart = step.startMs.coerceAtLeast(0)
        val safeEnd = step.endMs.coerceAtLeast(safeStart + 100)
        step.copy(
            enabledAnimations = setOf(step.kindOrFallback()),
            startMs = safeStart,
            endMs = safeEnd,
            durationMs = (safeEnd - safeStart).coerceAtLeast(100)
        )
    }

fun resolveNoOverlapForKind(existing: List<AnimationStep>, candidate: AnimationStep): AnimationStep {
    val kind = candidate.kindOrFallback()
    val duration = (candidate.endMs - candidate.startMs).coerceAtLeast(100)
    var start = candidate.startMs.coerceAtLeast(0)
    var end = start + duration
    val sameKind = existing
        .filter { it.id != candidate.id && it.kindOrFallback() == kind }
        .sortedBy { it.startMs }
    for (other in sameKind) {
        val overlap = start < other.endMs && end > other.startMs
        if (overlap) {
            start = other.endMs
            end = start + duration
        }
    }
    return candidate.withTimeline(start, end)
}
