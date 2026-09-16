package com.m3shapes.editor

import com.compose.canvas.asset.CubicBezier
import com.compose.canvas.asset.MotionChannel
import com.compose.canvas.asset.MotionInterpolation
import com.compose.canvas.asset.MotionRange
import com.compose.canvas.asset.MotionSequence
import com.compose.canvas.asset.MotionStep
import com.compose.canvas.asset.PropertyKeyframe
import com.compose.canvas.asset.PropertyTrack
import com.compose.canvas.core.FillStyle
import com.compose.canvas.core.ObjectTransform
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorLayer
import com.compose.canvas.core.VectorObject
import com.compose.canvas.asset.VisualAsset
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import com.compose.canvas.geometry.PresetGeometry
import androidx.compose.ui.graphics.Color

data class WssVisualAssetDescriptor(
    val assetId: String,
    val name: String,
    val type: String,
    val version: Int,
    val tags: Set<String>,
    val stateCount: Int,
    val animationCount: Int,
    val portCount: Int,
    val anchorCount: Int,
    val contentAreaCount: Int,
    val interchangeFormat: String = "application/vnd.emscript.motion+json"
)

fun VisualAsset.toWssDescriptor(): WssVisualAssetDescriptor =
    design.allObjects().let { objects ->
    WssVisualAssetDescriptor(
        assetId = identity.assetId,
        name = identity.name,
        type = identity.type.name,
        version = identity.version,
        tags = identity.tags,
        stateCount = states.size,
        animationCount = animations.sumOf { it.steps.size },
        portCount = design.metadata.ports.size + objects.sumOf { it.semantics.ports.size },
        anchorCount = design.metadata.anchors.size + objects.sumOf { it.semantics.anchors.size },
        contentAreaCount = design.metadata.contentAreas.size + objects.sumOf { it.semantics.contentAreas.size }
    )
}

fun AnimationStep.toMotionStep(): MotionStep = MotionStep(
    id = id,
    name = name,
    channels = buildSet {
        enabledAnimations.forEach { kind ->
            when (kind) {
                AnimationKind.Morph -> add(MotionChannel.MORPH)
                AnimationKind.Rotate -> add(MotionChannel.ROTATE)
                AnimationKind.Scale -> add(MotionChannel.SCALE)
                AnimationKind.Alpha -> add(MotionChannel.ALPHA)
                AnimationKind.Colour -> add(MotionChannel.COLOR)
                AnimationKind.Position -> {
                    add(MotionChannel.POSITION_X)
                    add(MotionChannel.POSITION_Y)
                }
                AnimationKind.Stagger -> add(MotionChannel.STAGGER)
                AnimationKind.Stroke -> add(MotionChannel.STROKE)
                AnimationKind.Blur -> add(MotionChannel.BLUR)
                AnimationKind.Gradient -> add(MotionChannel.GRADIENT)
            }
        }
    },
    startMs = startMs,
    endMs = endMs,
    easing = CubicBezier(profile.x1, profile.y1, profile.x2, profile.y2),
    interpolation = when (motionPath) {
        MotionPathType.Tween -> MotionInterpolation.TWEEN
        MotionPathType.Spring -> MotionInterpolation.SPRING
        MotionPathType.Bounce -> MotionInterpolation.BOUNCE
    },
    reverse = reverse,
    ranges = mapOf(
        MotionChannel.MORPH to morphRange.toMotionRange(),
        MotionChannel.ROTATE to rotateRange.toMotionRange(),
        MotionChannel.SCALE to scaleRange.toMotionRange(),
        MotionChannel.ALPHA to alphaRange.toMotionRange(),
        MotionChannel.COLOR to colorRange.toMotionRange(),
        MotionChannel.POSITION_X to offsetXRange.toMotionRange(),
        MotionChannel.POSITION_Y to offsetYRange.toMotionRange(),
        MotionChannel.STAGGER to staggerRange.toMotionRange(),
        MotionChannel.STROKE to strokeRange.toMotionRange(),
        MotionChannel.BLUR to blurRange.toMotionRange(),
        MotionChannel.GRADIENT to gradientRange.toMotionRange()
    ),
    springStiffness = springStiffness,
    springDamping = springDamping,
    bounceGravity = bounceGravity,
    holdMs = holdMs
)

fun MotionStep.toAnimationStep(): AnimationStep {
    fun range(channel: MotionChannel, fallback: FloatRangeValue): FloatRangeValue =
        ranges[channel]?.let { FloatRangeValue(it.start, it.end) } ?: fallback

    return AnimationStep(
        id = id,
        name = name,
        enabledAnimations = buildSet {
            if (MotionChannel.MORPH in channels) add(AnimationKind.Morph)
            if (MotionChannel.ROTATE in channels) add(AnimationKind.Rotate)
            if (MotionChannel.SCALE in channels) add(AnimationKind.Scale)
            if (MotionChannel.ALPHA in channels) add(AnimationKind.Alpha)
            if (MotionChannel.COLOR in channels) add(AnimationKind.Colour)
            if (MotionChannel.POSITION_X in channels || MotionChannel.POSITION_Y in channels) {
                add(AnimationKind.Position)
            }
            if (MotionChannel.STAGGER in channels) add(AnimationKind.Stagger)
            if (MotionChannel.STROKE in channels) add(AnimationKind.Stroke)
            if (MotionChannel.BLUR in channels) add(AnimationKind.Blur)
            if (MotionChannel.GRADIENT in channels) add(AnimationKind.Gradient)
        },
        startMs = startMs,
        endMs = endMs,
        durationMs = (endMs - startMs).coerceAtLeast(1),
        profile = BezierProfile(easing.x1, easing.y1, easing.x2, easing.y2),
        reverse = reverse,
        morphRange = range(MotionChannel.MORPH, FloatRangeValue(0f, 1f)),
        rotateRange = range(MotionChannel.ROTATE, FloatRangeValue(0f, 360f)),
        scaleRange = range(MotionChannel.SCALE, FloatRangeValue(0.88f, 1.18f)),
        alphaRange = range(MotionChannel.ALPHA, FloatRangeValue(0.45f, 1f)),
        colorRange = range(MotionChannel.COLOR, FloatRangeValue(0f, 1f)),
        offsetXRange = range(MotionChannel.POSITION_X, FloatRangeValue(-28f, 28f)),
        offsetYRange = range(MotionChannel.POSITION_Y, FloatRangeValue(-22f, 22f)),
        staggerRange = range(MotionChannel.STAGGER, FloatRangeValue(0f, 0.65f)),
        strokeRange = range(MotionChannel.STROKE, FloatRangeValue(0f, 1f)),
        blurRange = range(MotionChannel.BLUR, FloatRangeValue(0f, 16f)),
        gradientRange = range(MotionChannel.GRADIENT, FloatRangeValue(0f, 1f)),
        motionPath = when (interpolation) {
            MotionInterpolation.TWEEN -> MotionPathType.Tween
            MotionInterpolation.SPRING -> MotionPathType.Spring
            MotionInterpolation.BOUNCE -> MotionPathType.Bounce
        },
        springStiffness = springStiffness,
        springDamping = springDamping,
        bounceGravity = bounceGravity,
        holdMs = holdMs
    )
}

fun List<AnimationStep>.toMotionSequence(): MotionSequence {
    val normalized = normalizedTimeline()
    val tracks = normalized.flatMap { step ->
        val motionStep = step.toMotionStep()
        motionStep.ranges.filterKeys { it in motionStep.channels }.map { (channel, range) ->
            PropertyTrack(
                id = "${step.id}-${channel.name.lowercase()}",
                name = "${step.name} · ${channel.name}",
                channel = channel,
                keyframes = listOf(
                    PropertyKeyframe("${step.id}-${channel.name}-start", step.startMs, range.start, motionStep.easing),
                    PropertyKeyframe("${step.id}-${channel.name}-end", step.endMs, range.end, motionStep.easing)
                )
            )
        }
    }
    return MotionSequence(
        id = "main",
        name = "Main",
        steps = normalized.map(AnimationStep::toMotionStep),
        tracks = tracks
    )
}

private fun FloatRangeValue.toMotionRange() = MotionRange(start, end)

fun materialPresetDocument(shape: ExpressiveShapeId): VectorDocument {
    val definition = shape.toRoundedPolygon().fitToViewport().toPathGeometry()
    val preset = PresetGeometry(
        presetId = "material3:${shape.name}",
        width = 256f,
        height = 256f,
        definition = definition
    )
    return VectorDocument(
        name = shape.label,
        layers = listOf(
            VectorLayer(
                name = "Design",
                objects = listOf(
                    VectorObject(
                        name = shape.label,
                        geometry = preset,
                        transform = ObjectTransform(x = 372f, y = 372f),
                        fill = FillStyle(color = Color(0xFF6750A4))
                    )
                )
            )
        )
    )
}

private fun androidx.graphics.shapes.RoundedPolygon.toPathGeometry(): PathGeometry {
    val first = cubics.firstOrNull()
        ?: return PathGeometry(emptyList(), closed = true)
    return PathGeometry(
        segments = buildList {
            add(PathSegment.MoveTo(first.anchor0X, first.anchor0Y))
            cubics.forEach { cubic ->
                add(
                    PathSegment.CubicBezierTo(
                        cubic.control0X,
                        cubic.control0Y,
                        cubic.control1X,
                        cubic.control1Y,
                        cubic.anchor1X,
                        cubic.anchor1Y
                    )
                )
            }
            add(PathSegment.Close)
        },
        closed = true
    )
}
