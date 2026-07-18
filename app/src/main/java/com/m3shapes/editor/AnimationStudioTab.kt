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
    springStiffness: Float,
    onSpringStiffnessChange: (Float) -> Unit,
    springDamping: Float,
    onSpringDampingChange: (Float) -> Unit,
    springMass: Float,
    onSpringMassChange: (Float) -> Unit,
    reverseAnimation: Boolean,
    onReverseAnimationChange: (Boolean) -> Unit,
    enabledAnimations: Set<AnimationKind>,
    onEnabledAnimationsChange: (Set<AnimationKind>) -> Unit,
    selectedEasing: StudioEasing,
    onSelectedEasingChange: (StudioEasing) -> Unit,
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
    useMorphSteps: Boolean,
    onUseMorphStepsChange: (Boolean) -> Unit,
    morphSteps: List<Float>,
    onMorphStepsChange: (List<Float>) -> Unit,
    selectedMorphStepIndex: Int,
    onSelectedMorphStepIndexChange: (Int) -> Unit,
    animTargetColor: Color,
    onAnimTargetColorChange: (Color) -> Unit,
    animationSteps: List<AnimationStep>,
    selectedStepId: String?,
    sequencePlaying: Boolean,
    onAddCurrentStep: () -> Unit,
    onApplyStep: (AnimationStep) -> Unit,
    onDuplicateStep: (AnimationStep) -> Unit,
    onDeleteStep: (AnimationStep) -> Unit,
    onMoveStepUp: (Int) -> Unit,
    onMoveStepDown: (Int) -> Unit,
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
    onLoadLastBundle: () -> Unit
) {
    val estimatedMs = estimateDurationMs(springStiffness, springMass)

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
            IconButton(onClick = { onLoopPlayingChange(!loopPlaying) }) {
                Icon(
                    if (loopPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    "Play/Pause"
                )
            }
            Text(
                "~${estimatedMs} ms",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text("Spring Tokens", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Stiffness", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp))
            Slider(
                value = springStiffness,
                onValueChange = onSpringStiffnessChange,
                valueRange = 50f..2_000f,
                modifier = Modifier.weight(1f)
            )
            Text(
                springStiffness.roundToInt().toString(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.End
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Damping", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp))
            Slider(
                value = springDamping,
                onValueChange = onSpringDampingChange,
                valueRange = 0.05f..1.5f,
                modifier = Modifier.weight(1f)
            )
            Text(
                "%.2f".format(springDamping),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.End
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mass", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(72.dp))
            Slider(
                value = springMass,
                onValueChange = onSpringMassChange,
                valueRange = 0.1f..10f,
                modifier = Modifier.weight(1f)
            )
            Text(
                "%.2f".format(springMass),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.End
            )
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

        Text("Morph Steps", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(
            "Intermediate morph positions for the current animation.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = useMorphSteps,
                onClick = { onUseMorphStepsChange(!useMorphSteps) },
                label = { Text("Morph-Sequence", style = MaterialTheme.typography.labelSmall) }
            )
            OutlinedButton(
                onClick = {
                    val p = morphProgress.coerceIn(0f, 1f)
                    val next = (morphSteps + p).distinct().sorted()
                    onMorphStepsChange(next)
                    onSelectedMorphStepIndexChange(next.indexOfFirst { kotlin.math.abs(it - p) < 0.0001f })
                }
            ) { Text("+ Step", style = MaterialTheme.typography.labelSmall) }
            OutlinedButton(
                onClick = {
                    val steps = morphSteps.distinct().sorted()
                    if (steps.size <= 2) return@OutlinedButton
                    val clampedIndex = selectedMorphStepIndex.coerceIn(0, steps.lastIndex)
                    val v = steps[clampedIndex]
                    if (v == 0f || v == 1f) return@OutlinedButton
                    onMorphStepsChange(steps.filterNot { it == v })
                    onSelectedMorphStepIndexChange(0)
                }
            ) { Text("− Step", style = MaterialTheme.typography.labelSmall) }
        }
        StepLine(
            steps = morphSteps,
            selectedIndex = selectedMorphStepIndex,
            onSelectedIndexChange = onSelectedMorphStepIndexChange,
            onStepsChange = onMorphStepsChange,
            onScrub = onTimelineManualProgressChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
        )

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

        Text("Easing", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            StudioEasing.entries.forEach { easing ->
                FilterChip(
                    selected = selectedEasing == easing,
                    onClick = { onSelectedEasingChange(easing) },
                    label = { Text(easing.label, style = MaterialTheme.typography.labelSmall) }
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
            onAddCurrent = onAddCurrentStep,
            onApply = onApplyStep,
            onDuplicate = onDuplicateStep,
            onDelete = onDeleteStep,
            onMoveUp = onMoveStepUp,
            onMoveDown = onMoveStepDown,
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
            onLoadLast = onLoadLastBundle
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

@Composable
internal fun StepLine(
    steps: List<Float>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onStepsChange: (List<Float>) -> Unit,
    onScrub: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val sorted = remember(steps) { steps.map { it.coerceIn(0f, 1f) }.distinct().sorted() }
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = modifier
            .pointerInput(sorted, selectedIndex) {
                detectTapGestures { pos ->
                    val p = (pos.x / size.width).coerceIn(0f, 1f)
                    val nearestIndex = sorted.indices.minByOrNull { ix ->
                        kotlin.math.abs(sorted[ix] - p)
                    } ?: 0
                    val nearest = sorted[nearestIndex]
                    if (kotlin.math.abs(nearest - p) < 0.05f) {
                        onSelectedIndexChange(nearestIndex)
                        onScrub(nearest)
                    } else {
                        onScrub(p)
                    }
                }
            }
            .pointerInput(sorted, selectedIndex) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        change.consume()
                        if (sorted.isEmpty()) return@detectDragGestures
                        val ix = selectedIndex.coerceIn(0, sorted.lastIndex)
                        val p = (change.position.x / size.width).coerceIn(0f, 1f)
                        val v = sorted[ix]
                        if (v == 0f || v == 1f) return@detectDragGestures
                        val updated = sorted.toMutableList()
                        updated[ix] = p
                        val normalized = updated.map { it.coerceIn(0f, 1f) }.distinct().sorted()
                        onStepsChange(normalized)
                        onSelectedIndexChange(
                            normalized.indexOfFirst { kotlin.math.abs(it - p) < 0.0001f }.coerceAtLeast(0)
                        )
                        onScrub(p)
                    }
                )
            }
    ) {
        val midY = size.height / 2f
        val lineColor = onSurfaceVariant.copy(alpha = 0.65f)
        drawLine(lineColor, Offset(12f, midY), Offset(size.width - 12f, midY), strokeWidth = 3f)

        sorted.forEachIndexed { ix, v ->
            val x = 12f + (size.width - 24f) * v
            val selected = ix == selectedIndex
            drawCircle(
                color = if (selected) primary else onSurfaceVariant,
                radius = if (selected) 8f else 6f,
                center = Offset(x, midY)
            )
            drawCircle(
                color = Color.White,
                radius = if (selected) 3.5f else 3f,
                center = Offset(x, midY)
            )
        }
    }
}
