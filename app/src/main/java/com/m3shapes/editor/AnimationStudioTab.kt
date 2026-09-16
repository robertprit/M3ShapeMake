package com.m3shapes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.compose.canvas.asset.PreparedPathMorph
import com.compose.canvas.geometry.PathGeometry
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AnimationStudioTab(
    morph: PreparedPathMorph?,
    durationMs: Int,
    onDurationMsChange: (Int) -> Unit,
    profile: BezierProfile,
    onProfileChange: (BezierProfile) -> Unit,
    motionPathType: MotionPathType,
    onMotionPathTypeChange: (MotionPathType) -> Unit,
    springStiffness: Float,
    onSpringStiffnessChange: (Float) -> Unit,
    springDamping: Float,
    onSpringDampingChange: (Float) -> Unit,
    bounceGravity: Float,
    onBounceGravityChange: (Float) -> Unit,
    morphRange: FloatRangeValue,
    onMorphRangeChange: (FloatRangeValue) -> Unit,
    rotateRange: FloatRangeValue,
    onRotateRangeChange: (FloatRangeValue) -> Unit,
    scaleRange: FloatRangeValue,
    onScaleRangeChange: (FloatRangeValue) -> Unit,
    alphaRange: FloatRangeValue,
    onAlphaRangeChange: (FloatRangeValue) -> Unit,
    colorRange: FloatRangeValue,
    onColorRangeChange: (FloatRangeValue) -> Unit,
    offsetXRange: FloatRangeValue,
    onOffsetXRangeChange: (FloatRangeValue) -> Unit,
    offsetYRange: FloatRangeValue,
    onOffsetYRangeChange: (FloatRangeValue) -> Unit,
    staggerRange: FloatRangeValue,
    onStaggerRangeChange: (FloatRangeValue) -> Unit,
    strokeRange: FloatRangeValue,
    onStrokeRangeChange: (FloatRangeValue) -> Unit,
    blurRange: FloatRangeValue,
    onBlurRangeChange: (FloatRangeValue) -> Unit,
    gradientRange: FloatRangeValue,
    onGradientRangeChange: (FloatRangeValue) -> Unit,
    sourceColor: Color,
    animTargetColor: Color,
    onAnimTargetColorChange: (Color) -> Unit,
    playbackMode: PlaybackMode,
    onPlaybackModeChange: (PlaybackMode) -> Unit,
    studioColors: List<Color>
) {

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Animation Studio",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${durationMs} ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text("Motion Path", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MotionPathType.entries.forEach { mode ->
                FilterChip(
                    selected = mode == motionPathType,
                    onClick = { onMotionPathTypeChange(mode) },
                    label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        when (motionPathType) {
            MotionPathType.Tween -> {
                Text(
                    "Tween nutzt den Bezier-Pfad.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            MotionPathType.Spring -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Stiff", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp))
                    Slider(
                        value = springStiffness,
                        onValueChange = onSpringStiffnessChange,
                        valueRange = 50f..2_000f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        springStiffness.roundToInt().toString(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(64.dp),
                        textAlign = TextAlign.End
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Damp", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp))
                    Slider(
                        value = springDamping,
                        onValueChange = onSpringDampingChange,
                        valueRange = 0.05f..2f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "%.2f".format(springDamping),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(64.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
            MotionPathType.Bounce -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Gravity", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp))
                    Slider(
                        value = bounceGravity,
                        onValueChange = onBounceGravityChange,
                        valueRange = 0.1f..3f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "%.2f".format(bounceGravity),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(64.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
        BezierCurveEditor(
            profile = profile,
            onProfileChange = onProfileChange,
            motionPathType = motionPathType,
            springStiffness = springStiffness,
            springDamping = springDamping,
            bounceGravity = bounceGravity,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
        if (motionPathType == MotionPathType.Tween) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BezierCoordField(
                    label = "x1",
                    value = profile.x1,
                    onChange = { onProfileChange(profile.copy(x1 = it.coerceIn(0f, 1f))) },
                    modifier = Modifier.weight(1f)
                )
                BezierCoordField(
                    label = "y1",
                    value = profile.y1,
                    onChange = { onProfileChange(profile.copy(y1 = it.coerceIn(-2f, 3f))) },
                    modifier = Modifier.weight(1f)
                )
                BezierCoordField(
                    label = "x2",
                    value = profile.x2,
                    onChange = { onProfileChange(profile.copy(x2 = it.coerceIn(0f, 1f))) },
                    modifier = Modifier.weight(1f)
                )
                BezierCoordField(
                    label = "y2",
                    value = profile.y2,
                    onChange = { onProfileChange(profile.copy(y2 = it.coerceIn(-2f, 3f))) },
                    modifier = Modifier.weight(1f)
                )
            }
            Text("Presets", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StudioEasing.entries.forEach { preset ->
                    val selected = profile.matches(preset.profile())
                    FilterChip(
                        selected = selected,
                        onClick = { onProfileChange(preset.profile()) },
                        label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
        Text("Wiedergabe", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PlaybackMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == playbackMode,
                    onClick = { onPlaybackModeChange(mode) },
                    label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Text(
            when (playbackMode) {
                PlaybackMode.Normal -> "Start → Ende"
                PlaybackMode.Reverse -> "Ende → Start"
                PlaybackMode.PingPong -> "Start → Ende → Start"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text("Animation", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Duration", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp))
            Slider(
                value = durationMs.toFloat(),
                onValueChange = { onDurationMsChange((it / 50f).roundToInt() * 50) },
                valueRange = 200f..4_000f,
                modifier = Modifier.weight(1f)
            )
            Text(
                "$durationMs ms",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(64.dp),
                textAlign = TextAlign.End
            )
        }
        ParamRangeSlider(
            label = "Morph",
            value = morphRange,
            onValueChange = onMorphRangeChange,
            valueRange = 0f..1f,
            format = { "${(it * 100).roundToInt()}%" },
            preview = {
                IsolatedAnimationPreview(
                    kind = IsolatedPreviewKind.Morph,
                    morph = morph,
                    range = morphRange,
                    durationMs = durationMs,
                    profile = profile,
                    motionPathType = motionPathType,
                    springStiffness = springStiffness,
                    springDamping = springDamping,
                    bounceGravity = bounceGravity,
                    playbackMode = playbackMode,
                    sourceColor = sourceColor,
                    targetColor = animTargetColor
                )
            }
        )
        ParamRangeSlider(
            label = "Spin",
            value = rotateRange,
            onValueChange = onRotateRangeChange,
            valueRange = 0f..360f,
            format = { "${it.roundToInt()}°" },
            preview = {
                IsolatedAnimationPreview(
                    kind = IsolatedPreviewKind.Rotate,
                    morph = morph,
                    range = rotateRange,
                    durationMs = durationMs,
                    profile = profile,
                    motionPathType = motionPathType,
                    springStiffness = springStiffness,
                    springDamping = springDamping,
                    bounceGravity = bounceGravity,
                    playbackMode = playbackMode,
                    sourceColor = sourceColor,
                    targetColor = animTargetColor
                )
            }
        )
        ParamRangeSlider(
            label = "Size",
            value = scaleRange,
            onValueChange = onScaleRangeChange,
            valueRange = 0.5f..2f,
            format = { "${(it * 100).roundToInt()}%" },
            preview = {
                IsolatedAnimationPreview(
                    kind = IsolatedPreviewKind.Scale,
                    morph = morph,
                    range = scaleRange,
                    durationMs = durationMs,
                    profile = profile,
                    motionPathType = motionPathType,
                    springStiffness = springStiffness,
                    springDamping = springDamping,
                    bounceGravity = bounceGravity,
                    playbackMode = playbackMode,
                    sourceColor = sourceColor,
                    targetColor = animTargetColor
                )
            }
        )
        ParamRangeSlider(
            label = "Fade",
            value = alphaRange,
            onValueChange = onAlphaRangeChange,
            valueRange = 0.05f..1f,
            format = { "${(it * 100).roundToInt()}%" },
            preview = {
                IsolatedAnimationPreview(
                    kind = IsolatedPreviewKind.Alpha,
                    morph = morph,
                    range = alphaRange,
                    durationMs = durationMs,
                    profile = profile,
                    motionPathType = motionPathType,
                    springStiffness = springStiffness,
                    springDamping = springDamping,
                    bounceGravity = bounceGravity,
                    playbackMode = playbackMode,
                    sourceColor = sourceColor,
                    targetColor = animTargetColor
                )
            }
        )
        ParamRangeSlider(
            label = "Move X",
            value = offsetXRange,
            onValueChange = onOffsetXRangeChange,
            valueRange = -80f..80f,
            format = { it.roundToInt().toString() },
            preview = {
                IsolatedAnimationPreview(
                    kind = IsolatedPreviewKind.OffsetX,
                    morph = morph,
                    range = offsetXRange,
                    durationMs = durationMs,
                    profile = profile,
                    motionPathType = motionPathType,
                    springStiffness = springStiffness,
                    springDamping = springDamping,
                    bounceGravity = bounceGravity,
                    playbackMode = playbackMode,
                    sourceColor = sourceColor,
                    targetColor = animTargetColor
                )
            }
        )
        ParamRangeSlider(
            label = "Move Y",
            value = offsetYRange,
            onValueChange = onOffsetYRangeChange,
            valueRange = -80f..80f,
            format = { it.roundToInt().toString() },
            preview = {
                IsolatedAnimationPreview(
                    kind = IsolatedPreviewKind.OffsetY,
                    morph = morph,
                    range = offsetYRange,
                    durationMs = durationMs,
                    profile = profile,
                    motionPathType = motionPathType,
                    springStiffness = springStiffness,
                    springDamping = springDamping,
                    bounceGravity = bounceGravity,
                    playbackMode = playbackMode,
                    sourceColor = sourceColor,
                    targetColor = animTargetColor
                )
            }
        )
        ParamRangeSlider(
            label = "Blend",
            value = colorRange,
            onValueChange = onColorRangeChange,
            valueRange = 0f..1f,
            format = { "${(it * 100).roundToInt()}%" },
            preview = {
                IsolatedAnimationPreview(
                    kind = IsolatedPreviewKind.Colour,
                    morph = morph,
                    range = colorRange,
                    durationMs = durationMs,
                    profile = profile,
                    motionPathType = motionPathType,
                    springStiffness = springStiffness,
                    springDamping = springDamping,
                    bounceGravity = bounceGravity,
                    playbackMode = playbackMode,
                    sourceColor = sourceColor,
                    targetColor = animTargetColor
                )
            }
        )
        ParamRangeSlider(
            label = "Stagger",
            value = staggerRange,
            onValueChange = onStaggerRangeChange,
            valueRange = 0f..0.85f,
            format = { "${(it * 100).roundToInt()}%" },
            preview = {
                IsolatedAnimationPreview(
                    kind = IsolatedPreviewKind.Stagger,
                    morph = morph,
                    range = staggerRange,
                    durationMs = durationMs,
                    profile = profile,
                    motionPathType = motionPathType,
                    springStiffness = springStiffness,
                    springDamping = springDamping,
                    bounceGravity = bounceGravity,
                    playbackMode = playbackMode,
                    sourceColor = sourceColor,
                    targetColor = animTargetColor
                )
            }
        )
        ParamRangeSlider(
            label = "Stroke",
            value = strokeRange,
            onValueChange = onStrokeRangeChange,
            valueRange = 0f..1f,
            format = { "${(it * 100).roundToInt()}%" },
            preview = {
                IsolatedAnimationPreview(
                    kind = IsolatedPreviewKind.Stroke,
                    morph = morph,
                    range = strokeRange,
                    durationMs = durationMs,
                    profile = profile,
                    motionPathType = motionPathType,
                    springStiffness = springStiffness,
                    springDamping = springDamping,
                    bounceGravity = bounceGravity,
                    playbackMode = playbackMode,
                    sourceColor = sourceColor,
                    targetColor = animTargetColor
                )
            }
        )
        ParamRangeSlider(
            label = "Blur",
            value = blurRange,
            onValueChange = onBlurRangeChange,
            valueRange = 0f..24f,
            format = { "${it.roundToInt()} dp" },
            preview = {
                IsolatedAnimationPreview(
                    kind = IsolatedPreviewKind.Blur,
                    morph = morph,
                    range = blurRange,
                    durationMs = durationMs,
                    profile = profile,
                    motionPathType = motionPathType,
                    springStiffness = springStiffness,
                    springDamping = springDamping,
                    bounceGravity = bounceGravity,
                    playbackMode = playbackMode,
                    sourceColor = sourceColor,
                    targetColor = animTargetColor
                )
            }
        )
        ParamRangeSlider(
            label = "Gradient",
            value = gradientRange,
            onValueChange = onGradientRangeChange,
            valueRange = 0f..1f,
            format = { "${(it * 100).roundToInt()}%" },
            preview = {
                IsolatedAnimationPreview(
                    kind = IsolatedPreviewKind.Gradient,
                    morph = morph,
                    range = gradientRange,
                    durationMs = durationMs,
                    profile = profile,
                    motionPathType = motionPathType,
                    springStiffness = springStiffness,
                    springDamping = springDamping,
                    bounceGravity = bounceGravity,
                    playbackMode = playbackMode,
                    sourceColor = sourceColor,
                    targetColor = animTargetColor
                )
            }
        )
        Text(
            "Ziel-Farbe",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            studioColors.forEach { color ->
                val selected = color == animTargetColor
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                        .pointerInput(color) {
                            detectTapGestures { onAnimTargetColorChange(color) }
                        }
                )
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParamRangeSlider(
    label: String,
    value: FloatRangeValue,
    onValueChange: (FloatRangeValue) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    preview: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(52.dp))
                Text(
                    "${format(value.start)} → ${format(value.end)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
            RangeSlider(
                value = value.closed,
                onValueChange = { onValueChange(FloatRangeValue(it.start, it.endInclusive)) },
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth()
            )
        }
        preview()
    }
}

private enum class IsolatedPreviewKind {
    Morph, Rotate, Scale, Alpha, Colour, OffsetX, OffsetY, Stagger, Stroke, Blur, Gradient
}

@Composable
private fun IsolatedAnimationPreview(
    kind: IsolatedPreviewKind,
    morph: PreparedPathMorph?,
    range: FloatRangeValue,
    durationMs: Int,
    profile: BezierProfile,
    motionPathType: MotionPathType,
    springStiffness: Float,
    springDamping: Float,
    bounceGravity: Float,
    playbackMode: PlaybackMode,
    sourceColor: Color,
    targetColor: Color
) {
    var raw by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(durationMs, playbackMode) {
        val startNanos = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val elapsedMs = (now - startNanos) / 1_000_000L
            raw = playbackMode.timelinePhase(elapsedMs, durationMs)
        }
    }
    val eased = motionProgress(
        raw = raw,
        type = motionPathType,
        profile = profile,
        springStiffness = springStiffness,
        springDamping = springDamping,
        bounceGravity = bounceGravity
    )
    val t = playbackMode.applyDirection(eased)
    val animated = range.start + (range.end - range.start) * t
    val restPath = morph?.start ?: PathGeometry(emptyList())
    val path = when {
        kind == IsolatedPreviewKind.Morph && morph != null ->
            morph.pathAt(animated.coerceIn(0f, 1f))
        kind == IsolatedPreviewKind.Stagger && morph != null ->
            morph.pathAt(t.coerceIn(0f, 1f), animated.coerceIn(0f, 0.95f))
        else -> restPath
    }
    val staggerReferencePath = if (kind == IsolatedPreviewKind.Stagger && morph != null) {
        morph.pathAt(t.coerceIn(0f, 1f), 0f)
    } else {
        null
    }
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (staggerReferencePath != null) {
            ShapePreviewCanvas(
                path = staggerReferencePath,
                alpha = 0.2f,
                fillColor = sourceColor,
                fillVisible = false,
                modifier = Modifier.size(68.dp)
            )
        }
        ShapePreviewCanvas(
            path = path,
            rotation = if (kind == IsolatedPreviewKind.Rotate) animated else 0f,
            scale = if (kind == IsolatedPreviewKind.Scale) animated else 1f,
            alpha = if (kind == IsolatedPreviewKind.Alpha) animated else 1f,
            offsetX = if (kind == IsolatedPreviewKind.OffsetX) animated else 0f,
            offsetY = if (kind == IsolatedPreviewKind.OffsetY) animated else 0f,
            fillColor = if (kind == IsolatedPreviewKind.Colour) {
                lerp(sourceColor, targetColor, animated.coerceIn(0f, 1f))
            } else {
                sourceColor
            },
            strokeProgress = if (kind == IsolatedPreviewKind.Stroke) animated else 0f,
            blurRadius = if (kind == IsolatedPreviewKind.Blur) animated else 0f,
            gradientProgress = when (kind) {
                IsolatedPreviewKind.Gradient -> animated.coerceIn(0.02f, 1f)
                else -> 0f
            },
            gradientEndColor = targetColor,
            fillVisible = kind != IsolatedPreviewKind.Stroke,
            modifier = Modifier.size(68.dp)
        )
        if (kind == IsolatedPreviewKind.Stagger) {
            Text(
                text = "${(animated.coerceIn(0f, 0.95f) * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}
