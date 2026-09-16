package com.compose.canvas.asset

import com.compose.canvas.core.VectorDocument

const val EMA_FORMAT = "emscript_motion_asset"
const val EMA_SCHEMA_VERSION = 1
const val EMA_FILE_EXTENSION = "ema"
const val EMA_MIME_TYPE = "application/vnd.emscript.motion+json"

enum class VisualAssetType {
    SHAPE,
    ANIMATED_SHAPE,
    BLOCK_SHAPE,
    NODE_SHAPE,
    ICON,
    VISUAL_COMPONENT,
    ACTOR
}

data class VisualAssetIdentity(
    val assetId: String,
    val name: String,
    val type: VisualAssetType = VisualAssetType.ANIMATED_SHAPE,
    val version: Int = 1,
    val tags: Set<String> = emptySet()
)

data class VisualState(
    val id: String,
    val name: String,
    val document: VectorDocument
)

enum class MotionChannel {
    MORPH,
    ROTATE,
    SCALE,
    ALPHA,
    COLOR,
    POSITION_X,
    POSITION_Y,
    STAGGER,
    STROKE,
    BLUR,
    GRADIENT
}

enum class MotionInterpolation {
    TWEEN,
    SPRING,
    BOUNCE
}

data class MotionRange(
    val start: Float,
    val end: Float
)

data class CubicBezier(
    val x1: Float = 0.42f,
    val y1: Float = 0f,
    val x2: Float = 0.58f,
    val y2: Float = 1f
)

data class MotionStep(
    val id: String,
    val name: String,
    val channels: Set<MotionChannel>,
    val startMs: Int,
    val endMs: Int,
    val easing: CubicBezier = CubicBezier(),
    val interpolation: MotionInterpolation = MotionInterpolation.TWEEN,
    val reverse: Boolean = false,
    val ranges: Map<MotionChannel, MotionRange> = emptyMap(),
    val springStiffness: Float = 400f,
    val springDamping: Float = 0.75f,
    val bounceGravity: Float = 1f,
    val holdMs: Int = 0
)

data class MotionSequence(
    val id: String,
    val name: String,
    val steps: List<MotionStep> = emptyList(),
    val tracks: List<PropertyTrack> = emptyList(),
    val events: List<VisualEvent> = emptyList()
)

/**
 * Canonical, host-independent authoring model. UI state such as selection,
 * viewport and expanded panels intentionally does not live here.
 */
data class VisualAsset(
    val identity: VisualAssetIdentity,
    val design: VectorDocument,
    val states: List<VisualState> = emptyList(),
    val transitions: List<ShapeMakerTransition> = emptyList(),
    val animations: List<MotionSequence> = emptyList(),
    val sequences: List<ShapeMakerSequence> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    fun state(id: String): VisualState? = states.firstOrNull { it.id == id }

    fun upsertState(id: String, name: String, document: VectorDocument): VisualAsset {
        val next = VisualState(id = id, name = name, document = document)
        val index = states.indexOfFirst { it.id == id }
        return copy(
            states = if (index < 0) states + next else states.mapIndexed { i, state ->
                if (i == index) next else state
            }
        )
    }
}

fun newVisualAsset(
    assetId: String = "asset-1",
    name: String = "Untitled",
    document: VectorDocument = VectorDocument(name = name)
): VisualAsset = VisualAsset(
    identity = VisualAssetIdentity(assetId = assetId, name = name),
    design = document,
    states = listOf(
        VisualState("start", "Start", document),
        VisualState("mid", "Mitte", VectorDocument(name = "Mitte")),
        VisualState("end", "Ende", document)
    )
)
