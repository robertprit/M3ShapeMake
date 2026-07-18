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

data class AnimationStep(
    val id: String,
    val name: String,
    val enabledAnimations: Set<AnimationKind>,
    val stiffness: Float,
    val damping: Float,
    val mass: Float,
    val easing: StudioEasing,
    val reverse: Boolean,
    val morphRange: FloatRangeValue,
    val rotateRange: FloatRangeValue,
    val scaleRange: FloatRangeValue,
    val alphaRange: FloatRangeValue,
    val colorRange: FloatRangeValue,
    val offsetXRange: FloatRangeValue,
    val offsetYRange: FloatRangeValue,
    val holdMs: Int = 0
) {
    val durationMs: Int get() = estimateDurationMs(stiffness, mass)
}

data class AnimationSequence(
    val id: String,
    val name: String,
    val steps: List<AnimationStep> = emptyList()
)

fun createAnimationStepFromCurrentConfig(
    stepNumber: Int,
    enabledAnimations: Set<AnimationKind>,
    stiffness: Float,
    damping: Float,
    mass: Float,
    easing: StudioEasing,
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
    name = "Step $stepNumber",
    enabledAnimations = enabledAnimations,
    stiffness = stiffness.coerceIn(50f, 10_000f),
    damping = damping.coerceIn(0.05f, 1.5f),
    mass = mass.coerceIn(0.1f, 10f),
    easing = easing,
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
    val animLabel = if (enabledAnimations.isEmpty()) {
        "Manual"
    } else {
        enabledAnimations.joinToString(", ") { it.label }
    }
    val dir = if (reverse) "rev" else "fwd"
    return "$name · $animLabel · k=${stiffness.toInt()} · m=${"%.1f".format(mass)} · $dir · ${easing.label}"
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
