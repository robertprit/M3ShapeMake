package com.m3shapes.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimationSequencerPanel(
    steps: List<AnimationStep>,
    selectedStepId: String?,
    sequencePlaying: Boolean,
    playheadMs: Int,
    addKind: AnimationKind,
    onAddKindChange: (AnimationKind) -> Unit,
    addStartMs: Int,
    onAddStartMsChange: (Int) -> Unit,
    direction: SequencerDirection,
    onDirectionChange: (SequencerDirection) -> Unit,
    playMode: SequencerPlayMode,
    onPlayModeChange: (SequencerPlayMode) -> Unit,
    onAddCurrent: () -> Unit,
    onApply: (AnimationStep) -> Unit,
    onDuplicate: (AnimationStep) -> Unit,
    onDelete: (AnimationStep) -> Unit,
    onUpdateTimeline: (AnimationStep) -> Unit,
    onPlaySequence: () -> Unit,
    onStopSequence: () -> Unit,
    onClearSequence: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Sequencer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(
            "Timeline mit manuellem Start/Ende je Effekt (ein Effekt pro Step).",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AnimationKind.entries.forEach { kind ->
                OutlinedButton(onClick = { onAddKindChange(kind) }) {
                    Text(
                        if (kind == addKind) "• ${kind.label}" else kind.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = addStartMs.toString(),
                onValueChange = { raw -> onAddStartMsChange(raw.toIntOrNull() ?: 0) },
                label = { Text("Start ms") },
                singleLine = true,
                modifier = Modifier.width(120.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text(
                "Dauer = aktuelle Duration im Studio",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SequencerDirection.entries.forEach { mode ->
                OutlinedButton(onClick = { onDirectionChange(mode) }) {
                    Text(
                        if (mode == direction) "• ${mode.label}" else mode.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            SequencerPlayMode.entries.forEach { mode ->
                OutlinedButton(onClick = { onPlayModeChange(mode) }) {
                    Text(
                        if (mode == playMode) "• ${mode.label}" else mode.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalButton(onClick = onAddCurrent, enabled = !sequencePlaying) {
                Text("Add Current", style = MaterialTheme.typography.labelSmall)
            }
            if (sequencePlaying) {
                OutlinedButton(onClick = onStopSequence) {
                    Text("Stop", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                OutlinedButton(
                    onClick = onPlaySequence,
                    enabled = steps.isNotEmpty()
                ) {
                    Text("Play", style = MaterialTheme.typography.labelSmall)
                }
            }
            OutlinedButton(
                onClick = onClearSequence,
                enabled = steps.isNotEmpty() && !sequencePlaying
            ) {
                Text("Clear", style = MaterialTheme.typography.labelSmall)
            }
        }

        TimelineEditor(
            steps = steps,
            playheadMs = playheadMs,
            sequencePlaying = sequencePlaying,
            onUpdateStep = onUpdateTimeline,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .padding(8.dp)
        )

        if (steps.isEmpty()) {
            Text(
                "Noch keine Steps. Speichere die aktuelle Konfiguration mit „Add Current“.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            steps.forEachIndexed { index, step ->
                StepCard(
                    step = step,
                    index = index,
                    isSelected = step.id == selectedStepId,
                    isPlaying = sequencePlaying,
                    onApply = { onApply(step) },
                    onDuplicate = { onDuplicate(step) },
                    onDelete = { onDelete(step) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepCard(
    step: AnimationStep,
    index: Int,
    isSelected: Boolean,
    isPlaying: Boolean,
    onApply: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(10.dp)
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                RoundedCornerShape(10.dp)
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BezierMiniPreview(
                profile = step.profile,
                modifier = Modifier
                    .width(64.dp)
                    .height(32.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    step.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    step.summary(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "#${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isPlaying) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(onClick = onApply) {
                    Text("Apply", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = onDuplicate) {
                    Text("Duplicate", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = onDelete) {
                    Text("Delete", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TimelineEditor(
    steps: List<AnimationStep>,
    playheadMs: Int,
    sequencePlaying: Boolean,
    onUpdateStep: (AnimationStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestSteps = rememberUpdatedState(steps.normalizedTimeline())
    val latestOnUpdate = rememberUpdatedState(onUpdateStep)
    val totalMs = (steps.maxOfOrNull { it.endMs } ?: 1000).coerceAtLeast(1000)

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val current = latestSteps.value
                    if (current.isEmpty()) return@detectDragGestures
                    val pxPerMs = size.width / totalMs.toFloat()
                    val nearest = current.minByOrNull { step ->
                        val centerX = ((step.startMs + step.endMs) * 0.5f) * pxPerMs
                        kotlin.math.abs(centerX - change.position.x)
                    } ?: return@detectDragGestures
                    val nearStart = kotlin.math.abs(nearest.startMs * pxPerMs - change.position.x) <= 14f
                    val nearEnd = kotlin.math.abs(nearest.endMs * pxPerMs - change.position.x) <= 14f
                    val deltaMs = (dragAmount.x / pxPerMs).roundToInt()
                    val candidate = when {
                        nearStart -> nearest.withTimeline(nearest.startMs + deltaMs, nearest.endMs)
                        nearEnd -> nearest.withTimeline(nearest.startMs, nearest.endMs + deltaMs)
                        else -> nearest.withTimeline(nearest.startMs + deltaMs, nearest.endMs + deltaMs)
                    }
                    latestOnUpdate.value(resolveNoOverlapForKind(current, candidate))
                }
            }
    ) {
        if (steps.isEmpty()) return@Canvas
        val axisY = size.height - 12f
        drawLine(
            color = Color(0xFF7A7A7A),
            start = Offset(0f, axisY),
            end = Offset(size.width, axisY),
            strokeWidth = 2f
        )
        steps.normalizedTimeline().forEach { step ->
            val color = step.kindOrFallback().timelineColor()
            val x0 = step.startMs / totalMs.toFloat() * size.width
            val x1 = step.endMs / totalMs.toFloat() * size.width
            val top = 10f + step.kindOrFallback().ordinal * 11f
            val h = 9f
            drawRoundRect(
                color = color.copy(alpha = 0.35f),
                topLeft = Offset(x0, top),
                size = androidx.compose.ui.geometry.Size((x1 - x0).coerceAtLeast(6f), h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawCircle(color = color, radius = 4.5f, center = Offset(x0, top + h / 2f))
            drawCircle(color = color, radius = 4.5f, center = Offset(x1, top + h / 2f))
        }

        val playheadX = (playheadMs.coerceIn(0, totalMs) / totalMs.toFloat()) * size.width
        val playheadColor = if (sequencePlaying) Color(0xFF1E88E5) else Color(0xFF90A4AE)
        drawLine(
            color = playheadColor,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, axisY),
            strokeWidth = 2.5f
        )
        drawCircle(
            color = playheadColor,
            radius = 5f,
            center = Offset(playheadX, axisY)
        )
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 24f
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText(
            "${playheadMs}ms",
            (playheadX + 6f).coerceAtMost(size.width - 70f),
            18f,
            textPaint
        )
    }
}

private fun AnimationKind.timelineColor(): Color = when (this) {
    AnimationKind.Morph -> Color(0xFFAB47BC)
    AnimationKind.Rotate -> Color(0xFF26A69A)
    AnimationKind.Scale -> Color(0xFF42A5F5)
    AnimationKind.Alpha -> Color(0xFFFFA726)
    AnimationKind.Colour -> Color(0xFFEF5350)
    AnimationKind.Position -> Color(0xFF8D6E63)
}
