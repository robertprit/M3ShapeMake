package com.m3shapes.editor

import kotlinx.serialization.Serializable

const val M3_SHAPE_ASSET_FORMAT = "m3shapemaker_asset_bundle"
const val M3_SHAPE_ASSET_SCHEMA_VERSION = 1
const val M3_SHAPE_MAKER_APP_VERSION = "1.0.0"

@Serializable
data class M3ShapeMakerProducer(
    val name: String = "M3ShapeMaker",
    val version: String = M3_SHAPE_MAKER_APP_VERSION
)

@Serializable
data class M3ShapeMakerAssetBundle(
    val format: String = M3_SHAPE_ASSET_FORMAT,
    val schemaVersion: Int = M3_SHAPE_ASSET_SCHEMA_VERSION,
    val producer: M3ShapeMakerProducer = M3ShapeMakerProducer(),
    val bundleId: String,
    val namespace: String,
    val shapes: List<M3ShapeAsset> = emptyList(),
    val animations: List<M3AnimationSequenceAsset> = emptyList(),
    val blockVisuals: List<M3BlockVisualDefinition> = emptyList(),
    val flowNodeVisuals: List<M3FlowNodeVisualDefinition> = emptyList()
)

@Serializable
enum class M3ShapeAssetKind {
    MaterialPreset,
    ParametricPolygon,
    DrawnPolygon,
    BlockShape,
    FlowNodeShape,
    Decorative,
    MaskOnly
}

@Serializable
data class M3Viewport(
    val width: Float = 256f,
    val height: Float = 256f
)

@Serializable
data class M3Vertex(
    val id: String,
    val x: Float,
    val y: Float,
    val rounding: Float = 0f,
    val smoothing: Float = 0f,
    val locked: Boolean = false
)

@Serializable
data class M3ShapeAsset(
    val type: String = "shape_asset",
    val schemaVersion: Int = 1,
    val namespace: String,
    val id: String,
    val name: String,
    val kind: M3ShapeAssetKind,
    val viewport: M3Viewport = M3Viewport(),
    val pathData: String,
    val vertices: List<M3Vertex> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class M3AnimationStepAsset(
    val id: String,
    val name: String = "",
    val durationMs: Int,
    val easing: String,
    val animations: List<String> = emptyList(),
    val stiffness: Float = 400f,
    val damping: Float = 0.75f,
    val mass: Float = 1f,
    val reverse: Boolean = false,
    val morphStart: Float = 0f,
    val morphEnd: Float = 1f,
    val rotateStart: Float = 0f,
    val rotateEnd: Float = 360f,
    val scaleStart: Float = 0.88f,
    val scaleEnd: Float = 1.18f,
    val alphaStart: Float = 0.45f,
    val alphaEnd: Float = 1f,
    val colorStart: Float = 0f,
    val colorEnd: Float = 1f,
    val offsetXStart: Float = -28f,
    val offsetXEnd: Float = 28f,
    val offsetYStart: Float = -22f,
    val offsetYEnd: Float = 22f,
    /** Legacy/display fields for schema compatibility. */
    val morphProgress: Float = 0f,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val alpha: Float = 1f,
    val wobble: Float = 0f,
    val bounce: Float = 0f,
    val holdMs: Int = 0
)

@Serializable
data class M3AnimationSequenceAsset(
    val type: String = "animation_sequence",
    val schemaVersion: Int = 1,
    val namespace: String,
    val id: String,
    val name: String,
    val steps: List<M3AnimationStepAsset> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class M3Point(
    val x: Float,
    val y: Float
)

@Serializable
data class M3Rect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

@Serializable
data class M3DockingZone(
    val id: String,
    val kind: String,
    val bounds: M3Rect,
    val snapPoint: M3Point,
    val accepts: List<String> = emptyList(),
    val priority: Int = 0
)

@Serializable
data class M3BlockSocket(
    val id: String,
    val kind: String,
    val bounds: M3Rect,
    val accepts: List<String> = emptyList()
)

@Serializable
data class M3ConnectionPoint(
    val id: String,
    val side: String,
    val position: Float,
    val normalizedPosition: M3Point? = null,
    val direction: String,
    val accepts: List<String> = emptyList(),
    val maxConnections: Int = 1
)

@Serializable
data class M3TextAnchor(
    val id: String,
    val bounds: M3Rect,
    val alignment: String = "Center",
    val maxLines: Int = 1
)

@Serializable
data class M3BlockVisualDefinition(
    val type: String = "block_visual_definition",
    val schemaVersion: Int = 1,
    val namespace: String,
    val id: String,
    val name: String,
    val shapeAssetId: String,
    val dockingZones: List<M3DockingZone> = emptyList(),
    val sockets: List<M3BlockSocket> = emptyList(),
    val textAnchors: List<M3TextAnchor> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class M3FlowNodeVisualDefinition(
    val type: String = "flow_node_visual_definition",
    val schemaVersion: Int = 1,
    val namespace: String,
    val id: String,
    val name: String,
    val shapeAssetId: String,
    val connectionPoints: List<M3ConnectionPoint> = emptyList(),
    val labelAnchors: List<M3TextAnchor> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

enum class M3DiagnosticSeverity {
    Info,
    Warning,
    Error
}

data class M3ImportDiagnostic(
    val severity: M3DiagnosticSeverity,
    val code: String,
    val message: String,
    val assetId: String? = null,
    val jsonPath: String? = null
)

data class M3ValidationResult(
    val valid: Boolean,
    val diagnostics: List<M3ImportDiagnostic>
) {
    val errors: List<M3ImportDiagnostic> get() = diagnostics.filter { it.severity == M3DiagnosticSeverity.Error }
    val warnings: List<M3ImportDiagnostic> get() = diagnostics.filter { it.severity == M3DiagnosticSeverity.Warning }

    fun summaryLine(): String = when {
        !valid -> "Bundle invalid: ${errors.size} error(s), ${warnings.size} warning(s)."
        warnings.isNotEmpty() -> "Bundle valid with ${warnings.size} warning(s)."
        else -> "Bundle valid."
    }
}
