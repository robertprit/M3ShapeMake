package com.compose.canvas.metadata

import com.compose.canvas.geometry.ObjectId
import com.compose.canvas.geometry.RectF
import com.compose.canvas.geometry.newObjectId

/**
 * Host-neutral semantic contracts stored on the document so ports, anchors,
 * content areas, parameters and resize behavior round-trip with the geometry.
 */
data class Port(
    val id: ObjectId = newObjectId(),
    val name: String = "port",
    val x: Float = 0f,
    val y: Float = 0f,
    val directionDegrees: Float = 0f,
    val normalDegrees: Float = 0f,
    val portType: PortType = PortType.CUSTOM,
    val connectionType: String = "",
    val valueType: String = "",
    val snapRadius: Float = 8f
)

enum class PortType {
    VALUE_INPUT,
    VALUE_OUTPUT,
    STATEMENT_INPUT,
    PREVIOUS,
    NEXT,
    FLOW_INPUT,
    FLOW_OUTPUT,
    CUSTOM
}

data class NamedAnchor(
    val id: ObjectId = newObjectId(),
    val name: String = "anchor",
    val x: Float = 0f,
    val y: Float = 0f,
    val kind: AnchorKind = AnchorKind.CUSTOM
)

enum class AnchorKind {
    LABEL,
    ICON,
    TITLE,
    BADGE,
    CUSTOM
}

data class ContentArea(
    val id: ObjectId = newObjectId(),
    val name: String = "content",
    val bounds: RectF = RectF.Empty,
    val padding: Float = 0f,
    val contentType: String = ""
)

enum class ResizeMode { FIXED, STRETCH, SCALE, ANCHOR }

/** Host-neutral behavior for resizing an object inside a changing viewport/container. */
data class ResizeRules(
    val horizontal: ResizeMode = ResizeMode.SCALE,
    val vertical: ResizeMode = ResizeMode.SCALE,
    val preserveAspectRatio: Boolean = true,
    val minWidth: Float = 0f,
    val minHeight: Float = 0f,
    val maxWidth: Float? = null,
    val maxHeight: Float? = null
)
