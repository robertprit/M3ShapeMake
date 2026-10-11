package com.m3shapes.editor

enum class AnimationKind(val label: String) {
    Morph("Morph"),
    Rotate("Spin"),
    Scale("Size"),
    Alpha("Fade"),
    Colour("Blend"),
    Position("Move"),
    Stagger("Stagger"),
    Stroke("Stroke"),
    Blur("Blur"),
    Gradient("Gradient")
}

/** Named Bézier easing presets used as shortcuts to fill a [BezierProfile]. */
enum class StudioEasing(
    val label: String,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
) {
    Linear("Linear", 0f, 0f, 1f, 1f),
    EaseIn("Ease-In", 0.42f, 0f, 1f, 1f),
    EaseOut("Ease-Out", 0f, 0f, 0.58f, 1f),
    EaseInOut("Ease-In-Out", 0.42f, 0f, 0.58f, 1f),
    Anticipate("Anticipate", 0.36f, 0f, 0.66f, -0.56f),
    Overshoot("Overshoot", 0.34f, 1.56f, 0.64f, 1f);

    fun profile(): BezierProfile = BezierProfile(x1, y1, x2, y2)
}
