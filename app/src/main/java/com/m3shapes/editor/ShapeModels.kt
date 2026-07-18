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

data class FormConfig(
    val numVertices: Int = 4,
    val rounding: Float = 0.5f,
    val smoothing: Float = 0f
)

data class VertexPoint(
    val x: Float,
    val y: Float
)

data class VertexCornerConfig(
    val vertexId: String,
    val rounding: Float = 0f,
    val smoothing: Float = 0f,
    val locked: Boolean = false
)

data class DrawnPolygonConfig(
    val vertices: List<VertexPoint> = emptyList(),
    val rounding: Float = 0f,
    val smoothing: Float = 0f,
    val perVertexRounding: List<VertexCornerConfig> = emptyList()
)

enum class ShapeMode {
    Material,
    Parametric,
    Editor
}

sealed class ShapeSource {
    data class Material(val shapeId: ExpressiveShapeId) : ShapeSource()
    data class Custom(val config: FormConfig) : ShapeSource()
    data class Drawn(val config: DrawnPolygonConfig) : ShapeSource()
}

enum class AnimationKind(val label: String) {
    Morph("Morph"),
    Rotate("Rotate"),
    Scale("Scale"),
    Alpha("Alpha"),
    Colour("Colour"),
    Position("Position")
}

enum class StudioEasing(val label: String, val composeRef: String) {
    Linear("Linear", "LinearEasing"),
    EaseIn("Ease-In", "FastOutLinearInEasing"),
    EaseOut("Ease-Out", "LinearOutSlowInEasing"),
    EaseInOut("Ease-In-Out", "FastOutSlowInEasing"),
    Anticipate("Anticipate", "CubicBezierEasing(0.36f, 0f, 0.66f, -0.56f)"),
    Overshoot("Overshoot", "CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)")
}
