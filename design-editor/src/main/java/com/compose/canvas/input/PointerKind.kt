package com.compose.canvas.input

import androidx.compose.ui.input.pointer.PointerType

enum class PointerKind { Touch, Stylus, Mouse, Unknown }

fun PointerType.toKind(): PointerKind = when (this) {
    PointerType.Touch -> PointerKind.Touch
    PointerType.Stylus -> PointerKind.Stylus
    PointerType.Mouse -> PointerKind.Mouse
    else -> PointerKind.Unknown
}

fun PointerKind.hitTolerancePx(): Float = when (this) {
    PointerKind.Touch, PointerKind.Unknown -> 24f
    PointerKind.Stylus, PointerKind.Mouse -> 10f
}
