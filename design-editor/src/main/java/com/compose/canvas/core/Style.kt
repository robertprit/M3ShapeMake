package com.compose.canvas.core

import androidx.compose.ui.graphics.Color
import com.compose.canvas.geometry.ObjectId
import com.compose.canvas.geometry.newObjectId

enum class FillKind { None, Solid, LinearGradient, RadialGradient }

data class FillStyle(
    val kind: FillKind = FillKind.Solid,
    val color: Color = Color.Unspecified,
    val alpha: Float = 1f,
    val gradientId: ObjectId? = null
) {
    val isVisible: Boolean
        get() = when (kind) {
            FillKind.None -> false
            FillKind.Solid -> color != Color.Unspecified
            FillKind.LinearGradient, FillKind.RadialGradient -> gradientId != null
        }
}

enum class StrokeCap { Butt, Round, Square }
enum class StrokeJoin { Miter, Round, Bevel }

data class StrokeStyle(
    val color: Color = Color.Black,
    val width: Float = 1f,
    val alpha: Float = 1f,
    val cap: StrokeCap = StrokeCap.Butt,
    val join: StrokeJoin = StrokeJoin.Miter,
    val dash: List<Float> = emptyList()
)

data class GradientStop(
    val offset: Float,
    val color: Color,
    val alpha: Float = 1f
)

data class LinearGradientResource(
    override val id: ObjectId = newObjectId(),
    override val name: String = "Linear",
    val x1: Float = 0f,
    val y1: Float = 0f,
    val x2: Float = 1f,
    val y2: Float = 0f,
    val stops: List<GradientStop> = listOf(
        GradientStop(0f, Color(0xFF6750A4)),
        GradientStop(1f, Color(0xFFD0BCFF))
    )
) : GradientResource

data class RadialGradientResource(
    override val id: ObjectId = newObjectId(),
    override val name: String = "Radial",
    val cx: Float = 0.5f,
    val cy: Float = 0.5f,
    val r: Float = 0.5f,
    val stops: List<GradientStop> = listOf(
        GradientStop(0f, Color.White),
        GradientStop(1f, Color(0xFF6750A4))
    )
) : GradientResource
