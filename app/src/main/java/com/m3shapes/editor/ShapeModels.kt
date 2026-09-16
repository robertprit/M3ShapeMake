package com.m3shapes.editor

enum class ExpressiveShapeId(val label: String, val composeRef: String) {
    Arch("Arch", "MaterialShapes.Arch"),
    Arrow("Arrow", "MaterialShapes.Arrow"),
    Boom("Boom", "MaterialShapes.Boom"),
    Bun("Bun", "MaterialShapes.Bun"),
    Burst("Burst", "MaterialShapes.Burst"),
    Circle("Circle", "MaterialShapes.Circle"),
    ClamShell("ClamShell", "MaterialShapes.ClamShell"),
    Clover4Leaf("Clover 4", "MaterialShapes.Clover4Leaf"),
    Clover8Leaf("Clover 8", "MaterialShapes.Clover8Leaf"),
    Cookie12Sided("Cookie 12", "MaterialShapes.Cookie12Sided"),
    Cookie4Sided("Cookie 4", "MaterialShapes.Cookie4Sided"),
    Cookie6Sided("Cookie 6", "MaterialShapes.Cookie6Sided"),
    Cookie7Sided("Cookie 7", "MaterialShapes.Cookie7Sided"),
    Cookie9Sided("Cookie 9", "MaterialShapes.Cookie9Sided"),
    Diamond("Diamond", "MaterialShapes.Diamond"),
    Fan("Fan", "MaterialShapes.Fan"),
    Flower("Flower", "MaterialShapes.Flower"),
    Gem("Gem", "MaterialShapes.Gem"),
    Ghostish("Ghostish", "MaterialShapes.Ghostish"),
    Heart("Heart", "MaterialShapes.Heart"),
    Oval("Oval", "MaterialShapes.Oval"),
    Pentagon("Pentagon", "MaterialShapes.Pentagon"),
    Pill("Pill", "MaterialShapes.Pill"),
    PixelCircle("Pixel Circle", "MaterialShapes.PixelCircle"),
    PixelTriangle("Pixel Triangle", "MaterialShapes.PixelTriangle"),
    Puffy("Puffy", "MaterialShapes.Puffy"),
    PuffyDiamond("Puffy Diamond", "MaterialShapes.PuffyDiamond"),
    SemiCircle("Semi Circle", "MaterialShapes.SemiCircle"),
    Slanted("Slanted", "MaterialShapes.Slanted"),
    SoftBoom("Soft Boom", "MaterialShapes.SoftBoom"),
    SoftBurst("Soft Burst", "MaterialShapes.SoftBurst"),
    Square("Square", "MaterialShapes.Square"),
    Sunny("Sunny", "MaterialShapes.Sunny"),
    Triangle("Triangle", "MaterialShapes.Triangle"),
    VerySunny("Very Sunny", "MaterialShapes.VerySunny");

    companion object {
        val all: List<ExpressiveShapeId> = entries
    }
}

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
