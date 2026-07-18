package com.m3shapes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimationSequencerPanel(
    steps: List<AnimationStep>,
    selectedStepId: String?,
    sequencePlaying: Boolean,
    onAddCurrent: () -> Unit,
    onApply: (AnimationStep) -> Unit,
    onDuplicate: (AnimationStep) -> Unit,
    onDelete: (AnimationStep) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
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
            "Store and replay full animation configurations.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalButton(onClick = onAddCurrent, enabled = !sequencePlaying) {
                Text("Add Current", style = MaterialTheme.typography.labelSmall)
            }
            if (sequencePlaying) {
                OutlinedButton(onClick = onStopSequence) {
                    Text("Stop Sequence", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                OutlinedButton(
                    onClick = onPlaySequence,
                    enabled = steps.isNotEmpty()
                ) {
                    Text("Play Sequence", style = MaterialTheme.typography.labelSmall)
                }
            }
            OutlinedButton(
                onClick = onClearSequence,
                enabled = steps.isNotEmpty() && !sequencePlaying
            ) {
                Text("Clear", style = MaterialTheme.typography.labelSmall)
            }
        }

        if (steps.isEmpty()) {
            Text(
                "Noch keine Steps. Speichere die aktuelle Konfiguration mit „Add Current“.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        steps.forEachIndexed { index, step ->
            val selected = step.id == selectedStepId
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(step.summary(), style = MaterialTheme.typography.bodySmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = { onApply(step) },
                        enabled = !sequencePlaying
                    ) { Text("Apply", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { onDuplicate(step) },
                        enabled = !sequencePlaying
                    ) { Text("Duplicate", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { onDelete(step) },
                        enabled = !sequencePlaying
                    ) { Text("Delete", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { onMoveUp(index) },
                        enabled = !sequencePlaying && index > 0
                    ) { Text("↑", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { onMoveDown(index) },
                        enabled = !sequencePlaying && index < steps.lastIndex
                    ) { Text("↓", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}
