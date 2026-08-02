package com.m3shapes.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AnimationStudioTab(
    morph: Morph,
    morphProgress: Float,
    canvasRotation: Float,
    canvasScale: Float,
    canvasAlpha: Float,
    canvasOffsetX: Float,
    canvasOffsetY: Float,
    canvasFillColor: Color,
    loopPlaying: Boolean,
    onLoopPlayingChange: (Boolean) -> Unit,
    durationMs: Int,
    onDurationMsChange: (Int) -> Unit,
    profile: BezierProfile,
    onProfileChange: (BezierProfile) -> Unit,
    timelineManualProgress: Float,
    onTimelineManualProgressChange: (Float) -> Unit,
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
    animTargetColor: Color,
    onAnimTargetColorChange: (Color) -> Unit,
    reverseAnimation: Boolean,
    onReverseAnimationChange: (Boolean) -> Unit,
    enabledAnimations: Set<AnimationKind>,
    onEnabledAnimationsChange: (Set<AnimationKind>) -> Unit,
    studioColors: List<Color>,
    animationSteps: List<AnimationStep>,
    selectedStepId: String?,
    sequencePlaying: Boolean,
    sequencerPlayheadMs: Int,
    sequencerAddKind: AnimationKind,
    onSequencerAddKindChange: (AnimationKind) -> Unit,
    sequencerAddStartMs: Int,
    onSequencerAddStartMsChange: (Int) -> Unit,
    sequencerDirection: SequencerDirection,
    onSequencerDirectionChange: (SequencerDirection) -> Unit,
    sequencerPlayMode: SequencerPlayMode,
    onSequencerPlayModeChange: (SequencerPlayMode) -> Unit,
    onAddCurrentStep: () -> Unit,
    onApplyStep: (AnimationStep) -> Unit,
    onDuplicateStep: (AnimationStep) -> Unit,
    onDeleteStep: (AnimationStep) -> Unit,
    onUpdateStepTimeline: (AnimationStep) -> Unit,
    onPlaySequence: () -> Unit,
    onStopSequence: () -> Unit,
    onClearSequence: () -> Unit,
    assetNamespace: String,
    onAssetNamespaceChange: (String) -> Unit,
    assetBundleId: String,
    onAssetBundleIdChange: (String) -> Unit,
    assetStatusText: String,
    onCopyBundleJson: () -> Unit,
    onValidateBundle: () -> Unit,
    onSaveLastBundle: () -> Unit,
    onLoadLastBundle: () -> Unit,
    onExportFile: () -> Unit,
    onImportFile: () -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(200.dp)) {
                val path = morph.toPath(progress = morphProgress).asComposePath()
                val ms = size.width / 256f * canvasScale
                translate(left = canvasOffsetX, top = canvasOffsetY) {
                    rotate(
                        canvasRotation,
                        pivot = Offset(size.width / 2f, size.height / 2f)
                    ) {
                        scale(ms, ms, pivot = Offset.Zero) {
                            drawPath(
                                path,
                                canvasFillColor.copy(alpha = canvasAlpha),
                                style = Fill
                            )
                            drawPath(
                                path,
                                Color.Red.copy(alpha = 0.35f),
                                style = Stroke(width = 1.5f)
                            )
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Animation Studio",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                if (sequencePlaying) onStopSequence() else onLoopPlayingChange(!loopPlaying)
            }) {
                Icon(
                    if (loopPlaying || sequencePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    "Preview Play/Pause"
                )
            }
            Text(
                "${durationMs} ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text("Timing Profile", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
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
        BezierCurveEditor(
            profile = profile,
            onProfileChange = onProfileChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
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
                onChange = { onProfileChange(profile.copy(y1 = it.coerceIn(0f, 1f))) },
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
                onChange = { onProfileChange(profile.copy(y2 = it.coerceIn(0f, 1f))) },
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Checkbox(
                checked = reverseAnimation,
                onCheckedChange = onReverseAnimationChange
            )
            Text("Reverse", style = MaterialTheme.typography.bodySmall)
            Text(
                if (reverseAnimation) "Ende → Start" else "Start → Ende",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text("Animation", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AnimationKind.entries.forEach { kind ->
                FilterChip(
                    selected = kind in enabledAnimations,
                    onClick = {
                        onEnabledAnimationsChange(
                            if (kind in enabledAnimations) enabledAnimations - kind
                            else enabledAnimations + kind
                        )
                    },
                    label = { Text(kind.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        if (AnimationKind.Morph in enabledAnimations) {
            ParamRangeSlider(
                label = "Morph",
                value = morphRange,
                onValueChange = onMorphRangeChange,
                valueRange = 0f..1f,
                format = { "${(it * 100).roundToInt()}%" }
            )
        }
        if (AnimationKind.Rotate in enabledAnimations) {
            ParamRangeSlider(
                label = "Rotate",
                value = rotateRange,
                onValueChange = onRotateRangeChange,
                valueRange = 0f..360f,
                format = { "${it.roundToInt()}°" }
            )
        }
        if (AnimationKind.Scale in enabledAnimations) {
            ParamRangeSlider(
                label = "Scale",
                value = scaleRange,
                onValueChange = onScaleRangeChange,
                valueRange = 0.5f..2f,
                format = { "${(it * 100).roundToInt()}%" }
            )
        }
        if (AnimationKind.Alpha in enabledAnimations) {
            ParamRangeSlider(
                label = "Alpha",
                value = alphaRange,
                onValueChange = onAlphaRangeChange,
                valueRange = 0.05f..1f,
                format = { "${(it * 100).roundToInt()}%" }
            )
        }
        if (AnimationKind.Colour in enabledAnimations) {
            ParamRangeSlider(
                label = "Blend",
                value = colorRange,
                onValueChange = onColorRangeChange,
                valueRange = 0f..1f,
                format = { "${(it * 100).roundToInt()}%" }
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
        if (AnimationKind.Position in enabledAnimations) {
            ParamRangeSlider(
                label = "Pos X",
                value = offsetXRange,
                onValueChange = onOffsetXRangeChange,
                valueRange = -80f..80f,
                format = { it.roundToInt().toString() }
            )
            ParamRangeSlider(
                label = "Pos Y",
                value = offsetYRange,
                onValueChange = onOffsetYRangeChange,
                valueRange = -80f..80f,
                format = { it.roundToInt().toString() }
            )
        }

        AnimationSequencerPanel(
            steps = animationSteps,
            selectedStepId = selectedStepId,
            sequencePlaying = sequencePlaying,
            playheadMs = sequencerPlayheadMs,
            addKind = sequencerAddKind,
            onAddKindChange = onSequencerAddKindChange,
            addStartMs = sequencerAddStartMs,
            onAddStartMsChange = onSequencerAddStartMsChange,
            direction = sequencerDirection,
            onDirectionChange = onSequencerDirectionChange,
            playMode = sequencerPlayMode,
            onPlayModeChange = onSequencerPlayModeChange,
            onAddCurrent = onAddCurrentStep,
            onApply = onApplyStep,
            onDuplicate = onDuplicateStep,
            onDelete = onDeleteStep,
            onUpdateTimeline = onUpdateStepTimeline,
            onPlaySequence = onPlaySequence,
            onStopSequence = onStopSequence,
            onClearSequence = onClearSequence
        )

        M3ShapeExportPanel(
            namespace = assetNamespace,
            onNamespaceChange = onAssetNamespaceChange,
            bundleId = assetBundleId,
            onBundleIdChange = onAssetBundleIdChange,
            statusText = assetStatusText,
            onCopyJson = onCopyBundleJson,
            onValidate = onValidateBundle,
            onSaveLast = onSaveLastBundle,
            onLoadLast = onLoadLastBundle,
            onExportFile = onExportFile,
            onImportFile = onImportFile
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParamRangeSlider(
    label: String,
    value: FloatRangeValue,
    onValueChange: (FloatRangeValue) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    format: (Float) -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
}

