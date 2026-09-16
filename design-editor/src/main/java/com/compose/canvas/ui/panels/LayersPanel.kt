package com.compose.canvas.ui.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.compose.canvas.core.GroupObject
import com.compose.canvas.core.SceneNode
import com.compose.canvas.core.VectorObject
import com.compose.canvas.editor.EditorState
import com.compose.canvas.editor.addLayer
import com.compose.canvas.editor.moveLayer
import com.compose.canvas.editor.removeLayer
import com.compose.canvas.editor.reorderObjectInLayer
import com.compose.canvas.editor.setActiveLayer
import com.compose.canvas.editor.updateLayer
import com.compose.canvas.ui.theme.ChromeIcon
import com.compose.canvas.ui.theme.ChromeIconMuted

@Composable
fun LayersPanel(
    editorState: EditorState,
    modifier: Modifier = Modifier
) {
    FloatingPanel(
        title = "Ebenen",
        expanded = editorState.panelState.layersExpanded,
        onToggle = {
            editorState.panelState = editorState.panelState.copy(
                layersExpanded = !editorState.panelState.layersExpanded
            )
        },
        modifier = modifier.width(240.dp)
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LayerIconButton(onClick = { editorState.addLayer() }) {
                    Icon(Icons.Default.Add, "Ebene", tint = ChromeIcon)
                }
                LayerIconButton(onClick = { editorState.removeLayer(editorState.activeLayerIndex) }) {
                    Icon(Icons.Default.Delete, "Ebene löschen", tint = ChromeIcon)
                }
                LayerIconButton(onClick = {
                    val i = editorState.activeLayerIndex
                    if (i > 0) editorState.moveLayer(i, i - 1)
                }) { Icon(Icons.Default.KeyboardArrowUp, "Hoch", tint = ChromeIcon) }
                LayerIconButton(onClick = {
                    val i = editorState.activeLayerIndex
                    if (i < editorState.document.layers.lastIndex) editorState.moveLayer(i, i + 1)
                }) { Icon(Icons.Default.KeyboardArrowDown, "Runter", tint = ChromeIcon) }
            }
            editorState.document.layers.forEachIndexed { index, layer ->
                val active = index == editorState.activeLayerIndex
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editorState.setActiveLayer(index) }
                        .padding(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            layer.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        LayerIconButton(onClick = { editorState.updateLayer(index, layer.copy(visible = !layer.visible)) }) {
                            Icon(
                                if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                "Sichtbar",
                                tint = if (layer.visible) ChromeIcon else ChromeIconMuted
                            )
                        }
                        LayerIconButton(onClick = { editorState.updateLayer(index, layer.copy(locked = !layer.locked)) }) {
                            Icon(
                                if (layer.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                "Sperren",
                                tint = ChromeIcon
                            )
                        }
                    }
                    layer.objects.asReversed().forEachIndexed { rev, node ->
                        val from = layer.objects.lastIndex - rev
                        NodeRow(node, editorState, from)
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeRow(node: SceneNode, editorState: EditorState, index: Int) {
    val selected = node.id in editorState.selection.selectedIds
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { editorState.selectObject(node.id) }
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            when (node) {
                is GroupObject -> "▢ ${node.name}"
                is VectorObject -> node.name
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        LayerIconButton(onClick = {
            editorState.reorderObjectInLayer(
                index,
                (index + 1).coerceAtMost(editorState.document.layers[editorState.activeLayerIndex].objects.lastIndex)
            )
        }) {
            Icon(Icons.Default.KeyboardArrowUp, "Vor", tint = ChromeIcon)
        }
        LayerIconButton(onClick = { editorState.deleteObject(node.id) }) {
            Icon(Icons.Default.Delete, "Element löschen", tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun LayerIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(contentColor = ChromeIcon)
    ) {
        content()
    }
}
