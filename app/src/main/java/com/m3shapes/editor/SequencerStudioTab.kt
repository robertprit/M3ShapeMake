package com.m3shapes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.compose.canvas.geometry.PathGeometry

@Composable
fun SequencerStudioTab(
    morphPath: PathGeometry,
    canvasRotation: Float,
    canvasScale: Float,
    canvasAlpha: Float,
    canvasOffsetX: Float,
    canvasOffsetY: Float,
    canvasFillColor: Color,
    canvasStroke: Float,
    canvasBlur: Float,
    canvasGradient: Float,
    canvasGradientEnd: Color,
    animationSteps: List<AnimationStep>,
    selectedStepId: String?,
    sequencePlaying: Boolean,
    sequencerPlayheadMs: Int,
    sequencerAddKind: AnimationKind,
    onSequencerAddKindChange: (AnimationKind) -> Unit,
    onAddCurrentStep: () -> Unit,
    onAddCurrentAllKinds: () -> Unit,
    onApplyStep: (AnimationStep) -> Unit,
    onDuplicateStep: (AnimationStep) -> Unit,
    onDeleteStep: (AnimationStep) -> Unit,
    onSelectStep: (AnimationStep) -> Unit,
    onUpdateStepTimeline: (AnimationStep) -> Unit,
    onPlaySequence: () -> Unit,
    onStopSequence: () -> Unit,
    onClearSequence: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AnimationPreviewCard(
            path = morphPath,
            rotation = canvasRotation,
            scale = canvasScale,
            alpha = canvasAlpha,
            offsetX = canvasOffsetX,
            offsetY = canvasOffsetY,
            fillColor = canvasFillColor,
            strokeProgress = canvasStroke,
            blurRadius = canvasBlur,
            gradientProgress = canvasGradient,
            gradientEndColor = canvasGradientEnd
        )
        AnimationSequencerPanel(
            steps = animationSteps,
            selectedStepId = selectedStepId,
            sequencePlaying = sequencePlaying,
            playheadMs = sequencerPlayheadMs,
            addKind = sequencerAddKind,
            onAddKindChange = onSequencerAddKindChange,
            onAddCurrent = onAddCurrentStep,
            onAddCurrentAllKinds = onAddCurrentAllKinds,
            onApply = onApplyStep,
            onDuplicate = onDuplicateStep,
            onDelete = onDeleteStep,
            onSelectStep = onSelectStep,
            onUpdateTimeline = onUpdateStepTimeline,
            onPlaySequence = onPlaySequence,
            onStopSequence = onStopSequence,
            onClearSequence = onClearSequence
        )
    }
}
