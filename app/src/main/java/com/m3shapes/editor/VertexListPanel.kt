package com.m3shapes.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun VertexListPanel(
    vertices: List<EditableVertex>,
    selectedVertexIds: Set<String>,
    multiSelectEnabled: Boolean,
    onSelectVertex: (String) -> Unit,
    onUpdateVertex: (EditableVertex) -> Unit,
    onDeleteVertex: (String) -> Unit,
    onToggleLocked: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Vertices", style = MaterialTheme.typography.titleSmall)
        vertices.forEachIndexed { index, vertex ->
            val selected = vertex.id in selectedVertexIds
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
                        else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectVertex(vertex.id) }
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("#$index", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(36.dp))
                    Text(
                        "X ${vertex.x.roundToInt()}  Y ${vertex.y.roundToInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = vertex.locked,
                            onCheckedChange = { onToggleLocked(vertex.id) }
                        )
                        Text("Lock", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { onDeleteVertex(vertex.id) },
                        enabled = vertices.size > 3
                    ) {
                        Text("Del", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text("X", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = vertex.x,
                    onValueChange = { onUpdateVertex(vertex.copy(x = it)) },
                    valueRange = 0f..256f,
                    enabled = !vertex.locked
                )
                Text("Y", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = vertex.y,
                    onValueChange = { onUpdateVertex(vertex.copy(y = it)) },
                    valueRange = 0f..256f,
                    enabled = !vertex.locked
                )
                Text(
                    "Rounding ${"%.2f".format(vertex.rounding)}",
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = vertex.rounding,
                    onValueChange = { onUpdateVertex(vertex.copy(rounding = it)) },
                    valueRange = 0f..1f
                )
                Text(
                    "Smoothing ${"%.2f".format(vertex.smoothing)}",
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = vertex.smoothing,
                    onValueChange = { onUpdateVertex(vertex.copy(smoothing = it)) },
                    valueRange = 0f..1f
                )
            }
        }
        if (multiSelectEnabled) {
            Text(
                "Multi-Select aktiv",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
