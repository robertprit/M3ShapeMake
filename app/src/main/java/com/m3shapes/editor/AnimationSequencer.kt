package com.m3shapes.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    onAddCurrent: () -> Unit,
    onAddCurrentAllKinds: () -> Unit,
    onApply: (AnimationStep) -> Unit,
    onDuplicate: (AnimationStep) -> Unit,
    onDelete: (AnimationStep) -> Unit,
    onSelectStep: (AnimationStep) -> Unit,
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

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalButton(onClick = onAddCurrent, enabled = !sequencePlaying) {
                Text("Add Current", style = MaterialTheme.typography.labelSmall)
            }
            FilledTonalButton(onClick = onAddCurrentAllKinds, enabled = !sequencePlaying) {
                Text("Add Current (All)", style = MaterialTheme.typography.labelSmall)
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
            selectedStepId = selectedStepId,
            onSelectStep = onSelectStep,
            onUpdateStep = onUpdateTimeline,
            modifier = Modifier
                .fillMaxWidth()
                .height(timelineCardHeight())
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .padding(8.dp)
        )

        StepIconGrid(
            steps = steps,
            selectedStepId = selectedStepId,
            onSelectStep = onSelectStep
        )

        val selected = steps.firstOrNull { it.id == selectedStepId }
        if (steps.isEmpty()) {
            Text(
                "Noch keine Steps. Speichere die aktuelle Konfiguration mit „Add Current“.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (selected != null) {
            StepInfoCard(
                step = selected,
                index = steps.indexOf(selected),
                isPlaying = sequencePlaying,
                onApply = { onApply(selected) },
                onDuplicate = { onDuplicate(selected) },
                onDelete = { onDelete(selected) }
            )
        } else {
            Text(
                "Step antippen, um Details zu sehen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StepIconGrid(
    steps: List<AnimationStep>,
    selectedStepId: String?,
    onSelectStep: (AnimationStep) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(6) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                repeat(24) { col ->
                    val step = steps.getOrNull(row * 24 + col)
                    val selected = step?.id == selectedStepId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                when {
                                    step == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    selected -> MaterialTheme.colorScheme.primaryContainer
                                    else -> step.kindOrFallback().timelineColor().copy(alpha = 0.18f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(2.dp)
                            )
                            .then(
                                if (step != null) Modifier.clickable { onSelectStep(step) }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (step != null) {
                            Icon(
                                imageVector = step.kindOrFallback().gridIcon(),
                                contentDescription = step.name,
                                tint = if (selected) MaterialTheme.colorScheme.primary
                                else step.kindOrFallback().timelineColor(),
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepInfoCard(
    step: AnimationStep,
    index: Int,
    isPlaying: Boolean,
    onApply: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
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
                motionPathType = step.motionPath,
                springStiffness = step.springStiffness,
                springDamping = step.springDamping,
                bounceGravity = step.bounceGravity,
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
    selectedStepId: String?,
    onSelectStep: (AnimationStep) -> Unit,
    onUpdateStep: (AnimationStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestSteps = rememberUpdatedState(steps.normalizedTimeline())
    val latestOnSelect = rememberUpdatedState(onSelectStep)
    val latestOnUpdate = rememberUpdatedState(onUpdateStep)
    val latestSelectedId = rememberUpdatedState(selectedStepId)
    val totalMs = (steps.maxOfOrNull { it.endMs } ?: 1000).coerceAtLeast(1000)
    var dragTargetStepId by remember { mutableStateOf<String?>(null) }
    var dragTargetHandle by remember { mutableStateOf(TimelineDragHandle.Body) }
    var dragRemainderPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val metrics = remember(density) {
        with(density) {
            TimelineMetrics(
                laneHeight = 28.dp.toPx(),
                laneGap = 32.dp.toPx(),
                topPadding = 28.dp.toPx(),
                leftInset = 76.dp.toPx(),
                rightPadding = 10.dp.toPx(),
                pinTouchRadius = 20.dp.toPx(),
                labelTextSize = 12.dp.toPx()
            )
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val current = latestSteps.value
                    if (current.isEmpty()) return@detectTapGestures
                    val tapped = pickStepAt(
                        steps = current,
                        selectedStepId = latestSelectedId.value,
                        offset = offset,
                        totalMs = totalMs,
                        widthPx = size.width.toFloat(),
                        metrics = metrics
                    ) ?: return@detectTapGestures
                    latestOnSelect.value(tapped)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val current = latestSteps.value
                        if (current.isEmpty()) return@detectDragGestures
                        val widthPx = (size.width - metrics.leftInset - metrics.rightPadding).coerceAtLeast(1f)
                        val selected = latestSelectedId.value?.let { id -> current.firstOrNull { it.id == id } }
                        val pinHit = selected?.let { step ->
                            val x0 = metrics.leftInset + (step.startMs / totalMs.toFloat()) * widthPx
                            val x1 = metrics.leftInset + (step.endMs / totalMs.toFloat()) * widthPx
                            val pinY = metrics.pinY(step)
                            when {
                                (offset - Offset(x0, pinY)).getDistance() <= metrics.pinTouchRadius ->
                                    TimelineDragHandle.Start
                                (offset - Offset(x1, pinY)).getDistance() <= metrics.pinTouchRadius ->
                                    TimelineDragHandle.End
                                else -> null
                            }
                        }
                        val nearest = if (pinHit != null && selected != null) {
                            selected
                        } else {
                            pickStepAt(
                                steps = current,
                                selectedStepId = latestSelectedId.value,
                                offset = offset,
                                totalMs = totalMs,
                                widthPx = size.width.toFloat(),
                                metrics = metrics
                            ) ?: return@detectDragGestures
                        }
                        val x0 = metrics.leftInset + (nearest.startMs / totalMs.toFloat()) * widthPx
                        val x1 = metrics.leftInset + (nearest.endMs / totalMs.toFloat()) * widthPx
                        dragTargetStepId = nearest.id
                        dragRemainderPx = 0f
                        dragTargetHandle = pinHit ?: when {
                            kotlin.math.abs(x0 - offset.x) <= metrics.pinTouchRadius -> TimelineDragHandle.Start
                            kotlin.math.abs(x1 - offset.x) <= metrics.pinTouchRadius -> TimelineDragHandle.End
                            else -> TimelineDragHandle.Body
                        }
                        latestOnSelect.value(nearest)
                    },
                    onDragCancel = {
                        dragTargetStepId = null
                        dragTargetHandle = TimelineDragHandle.Body
                        dragRemainderPx = 0f
                    },
                    onDragEnd = {
                        dragTargetStepId = null
                        dragTargetHandle = TimelineDragHandle.Body
                        dragRemainderPx = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val current = latestSteps.value
                    if (current.isEmpty()) return@detectDragGestures
                    val stepId = dragTargetStepId ?: return@detectDragGestures
                    val step = current.firstOrNull { it.id == stepId } ?: return@detectDragGestures
                    val widthPx = (size.width - metrics.leftInset - metrics.rightPadding).coerceAtLeast(1f)
                    val pxPerMs = widthPx / totalMs.toFloat()
                    val snapMs = 25
                    val pxPerSnap = pxPerMs * snapMs
                    dragRemainderPx += dragAmount.x
                    val snapSteps = (dragRemainderPx / pxPerSnap).toInt()
                    if (snapSteps == 0) return@detectDragGestures
                    dragRemainderPx -= snapSteps * pxPerSnap
                    val deltaMs = snapSteps * snapMs
                    val candidate = when (dragTargetHandle) {
                        TimelineDragHandle.Start -> step.withTimeline(step.startMs + deltaMs, step.endMs)
                        TimelineDragHandle.End -> step.withTimeline(step.startMs, step.endMs + deltaMs)
                        TimelineDragHandle.Body -> step.withTimeline(step.startMs + deltaMs, step.endMs + deltaMs)
                    }
                    latestOnUpdate.value(
                        boundCandidateWithinLane(
                            steps = current,
                            original = step,
                            candidate = candidate,
                            handle = dragTargetHandle
                        )
                    )
                }
            }
    ) {
        val laneSpan = metrics.laneSpan
        val widthPx = (size.width - metrics.leftInset - metrics.rightPadding).coerceAtLeast(1f)
        val axisY = size.height - 12f
        repeat(TIMELINE_LANE_COUNT) { index ->
            val kind = AnimationKind.entries.getOrNull(index)
            val top = metrics.topPadding + index * laneSpan
            val selectedLane = kind != null && selectedStepId != null && steps.any {
                it.id == selectedStepId && it.kindOrFallback() == kind
            }
            drawRoundRect(
                color = if (selectedLane) Color(0x302196F3) else Color(0x14000000),
                topLeft = Offset(metrics.leftInset, top),
                size = androidx.compose.ui.geometry.Size(widthPx, metrics.laneHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = metrics.labelTextSize
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                laneLabel(index),
                6f,
                top + metrics.laneHeight - 3f,
                textPaint
            )
        }
        drawLine(
            color = Color(0xFF7A7A7A),
            start = Offset(metrics.leftInset, axisY),
            end = Offset(size.width - metrics.rightPadding, axisY),
            strokeWidth = 2f
        )
        steps.normalizedTimeline().forEach { step ->
            val color = step.kindOrFallback().timelineColor()
            val x0 = metrics.leftInset + (step.startMs / totalMs.toFloat()) * widthPx
            val x1 = metrics.leftInset + (step.endMs / totalMs.toFloat()) * widthPx
            val top = metrics.topPadding + step.kindOrFallback().laneIndex() * laneSpan + 2f
            val h = metrics.laneHeight - 4f
            val selected = step.id == selectedStepId
            drawRoundRect(
                color = color.copy(alpha = if (selected) 0.6f else 0.4f),
                topLeft = Offset(x0, top),
                size = androidx.compose.ui.geometry.Size((x1 - x0).coerceAtLeast(10f), h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            if (selected) {
                drawPinMarker(Offset(x0, top), color)
                drawPinMarker(Offset(x1, top), color)
            } else {
                drawCircle(color = color, radius = 5f, center = Offset(x0, top + h / 2f))
                drawCircle(color = color, radius = 5f, center = Offset(x1, top + h / 2f))
            }
        }

        val playheadX = metrics.leftInset + (playheadMs.coerceIn(0, totalMs) / totalMs.toFloat()) * widthPx
        val playheadColor = if (sequencePlaying) Color(0xFF1E88E5) else Color(0xFF90A4AE)
        drawLine(
            color = playheadColor,
            start = Offset(playheadX, metrics.topPadding - 2f),
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
            (playheadX + 6f).coerceAtMost(size.width - 84f),
            metrics.topPadding + 4f,
            textPaint
        )
    }
}

private data class TimelineMetrics(
    val laneHeight: Float = 18f,
    val laneGap: Float = 20f,
    val topPadding: Float = 18f,
    val leftInset: Float = 72f,
    val rightPadding: Float = 10f,
    val pinTouchRadius: Float = 36f,
    val labelTextSize: Float = 22f
) {
    val laneSpan: Float get() = laneHeight + laneGap
    fun pinY(step: AnimationStep): Float =
        topPadding + step.kindOrFallback().laneIndex() * laneSpan - 18f
}

private const val TIMELINE_LANE_COUNT = 10

private fun timelineCardHeight() =
    28.dp * TIMELINE_LANE_COUNT + 32.dp * (TIMELINE_LANE_COUNT - 1) + 72.dp

private fun AnimationKind.laneIndex(): Int = ordinal.coerceIn(0, TIMELINE_LANE_COUNT - 1)

private fun laneLabel(index: Int): String =
    AnimationKind.entries.getOrNull(index)?.label ?: "Frei ${index - AnimationKind.entries.size + 1}"

private fun DrawScope.drawPinMarker(tip: Offset, color: Color) {
    val headRadius = 9f
    val head = Offset(tip.x, tip.y - 20f)
    val pin = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(head.x - 7f, head.y + 3f)
        lineTo(head.x + 7f, head.y + 3f)
        close()
    }
    drawPath(pin, color)
    drawCircle(color, headRadius, head)
    drawCircle(Color.White, 3.5f, head)
}

private enum class TimelineDragHandle { Start, End, Body }

private fun boundCandidateWithinLane(
    steps: List<AnimationStep>,
    original: AnimationStep,
    candidate: AnimationStep,
    handle: TimelineDragHandle
): AnimationStep {
    val minDuration = 100
    val sameKindOrdered = steps
        .filter { it.id != original.id && it.kindOrFallback() == original.kindOrFallback() }
        .sortedBy { it.startMs }
    val laneWithOriginal = (sameKindOrdered + original).sortedBy { it.startMs }
    val originalIndex = laneWithOriginal.indexOfFirst { it.id == original.id }
    val prev = laneWithOriginal.getOrNull(originalIndex - 1)
    val next = laneWithOriginal.getOrNull(originalIndex + 1)

    val minStart = prev?.endMs ?: 0
    val maxEnd = next?.startMs ?: Int.MAX_VALUE
    val candidateDuration = (candidate.endMs - candidate.startMs).coerceAtLeast(minDuration)

    return when (handle) {
        TimelineDragHandle.Start -> {
            val end = candidate.endMs.coerceAtMost(maxEnd)
            val boundedStart = candidate.startMs
                .coerceAtLeast(minStart)
                .coerceAtMost(end - minDuration)
            candidate.copy(
                startMs = boundedStart,
                endMs = end.coerceAtLeast(boundedStart + minDuration),
                durationMs = (end - boundedStart).coerceAtLeast(minDuration)
            )
        }
        TimelineDragHandle.End -> {
            val start = candidate.startMs.coerceAtLeast(minStart)
            val boundedEnd = candidate.endMs
                .coerceAtMost(maxEnd)
                .coerceAtLeast(start + minDuration)
            candidate.copy(
                startMs = start,
                endMs = boundedEnd,
                durationMs = (boundedEnd - start).coerceAtLeast(minDuration)
            )
        }
        TimelineDragHandle.Body -> {
            val maxStart = if (maxEnd == Int.MAX_VALUE) Int.MAX_VALUE else (maxEnd - candidateDuration)
            val boundedStart = candidate.startMs
                .coerceAtLeast(minStart)
                .coerceAtMost(maxStart)
            val safeStart = boundedStart.coerceAtLeast(0)
            val boundedEnd = safeStart + candidateDuration
            candidate.copy(
                startMs = safeStart,
                endMs = boundedEnd,
                durationMs = candidateDuration
            )
        }
    }
}

private fun pickStepAt(
    steps: List<AnimationStep>,
    selectedStepId: String?,
    offset: Offset,
    totalMs: Int,
    widthPx: Float,
    metrics: TimelineMetrics
): AnimationStep? {
    val usableWidth = (widthPx - metrics.leftInset - metrics.rightPadding).coerceAtLeast(1f)
    val laneSpan = metrics.laneSpan
    val selected = selectedStepId?.let { id -> steps.firstOrNull { it.id == id } }
    fun laneBounds(step: AnimationStep): Pair<Float, Float> {
        val top = metrics.topPadding + step.kindOrFallback().laneIndex() * laneSpan
        return top to top + metrics.laneHeight
    }
    fun xBounds(step: AnimationStep): Pair<Float, Float> {
        val startX = metrics.leftInset + (step.startMs / totalMs.toFloat()) * usableWidth
        val endX = metrics.leftInset + (step.endMs / totalMs.toFloat()) * usableWidth
        return startX to endX
    }
    // Sticky selection: if pointer remains in selected step neighborhood, keep it.
    if (selected != null) {
        val (laneTop, laneBottom) = laneBounds(selected)
        val (x0, x1) = xBounds(selected)
        val pinY = metrics.pinY(selected)
        if (offset.y in (pinY - 24f)..(laneBottom + 12f) &&
            offset.x in (x0 - 24f)..(x1 + 24f)
        ) return selected
        if (offset.y in (laneTop - 12f)..(laneBottom + 12f) &&
            offset.x in (x0 - 18f)..(x1 + 18f)
        ) return selected
    }
    val laneFiltered = steps.filter { step ->
        val (laneTop, laneBottom) = laneBounds(step)
        offset.y in (laneTop - 12f)..(laneBottom + 12f)
    }
    val candidates = if (laneFiltered.isNotEmpty()) laneFiltered else steps
    val containsPoint = candidates.filter { step ->
        val (x0, x1) = xBounds(step)
        offset.x in (x0 - 8f)..(x1 + 8f)
    }
    val narrowed = if (containsPoint.isNotEmpty()) containsPoint else candidates
    return narrowed.minByOrNull { step ->
        val (x0, x1) = xBounds(step)
        val (laneTop, laneBottom) = laneBounds(step)
        val laneMid = (laneTop + laneBottom) / 2f
        val dx = when {
            offset.x < x0 -> x0 - offset.x
            offset.x > x1 -> offset.x - x1
            else -> 0f
        }
        val dy = kotlin.math.abs(offset.y - laneMid) * 0.45f
        dx + dy
    }
}

private fun AnimationKind.gridIcon(): ImageVector = when (this) {
    AnimationKind.Morph -> Icons.Default.AutoFixHigh
    AnimationKind.Rotate -> Icons.Default.RotateRight
    AnimationKind.Scale -> Icons.Default.ZoomOutMap
    AnimationKind.Alpha -> Icons.Default.Opacity
    AnimationKind.Colour -> Icons.Default.Palette
    AnimationKind.Position -> Icons.Default.OpenWith
    AnimationKind.Stagger -> Icons.Default.LinearScale
    AnimationKind.Stroke -> Icons.Default.Gesture
    AnimationKind.Blur -> Icons.Default.BlurOn
    AnimationKind.Gradient -> Icons.Default.Gradient
}

private fun AnimationKind.timelineColor(): Color = when (this) {
    AnimationKind.Morph -> Color(0xFFAB47BC)
    AnimationKind.Rotate -> Color(0xFF26A69A)
    AnimationKind.Scale -> Color(0xFF42A5F5)
    AnimationKind.Alpha -> Color(0xFFFFA726)
    AnimationKind.Colour -> Color(0xFFEF5350)
    AnimationKind.Position -> Color(0xFF8D6E63)
    AnimationKind.Stagger -> Color(0xFF7E57C2)
    AnimationKind.Stroke -> Color(0xFF5C6BC0)
    AnimationKind.Blur -> Color(0xFF78909C)
    AnimationKind.Gradient -> Color(0xFF26C6DA)
}
