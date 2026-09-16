package com.compose.canvas.ui.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.compose.canvas.editor.EditorState
import com.compose.canvas.editor.EditorTool

private data class ToolItem(val tool: EditorTool, val icon: ImageVector, val label: String)

private val tools = listOf(
    ToolItem(EditorTool.Select, Icons.Default.NearMe, "Auswahl"),
    ToolItem(EditorTool.Rectangle, Icons.Default.Rectangle, "Rechteck"),
    ToolItem(EditorTool.Ellipse, Icons.Default.Circle, "Ellipse"),
    ToolItem(EditorTool.Line, Icons.Default.HorizontalRule, "Linie"),
    ToolItem(EditorTool.Triangle, Icons.Default.ChangeHistory, "Dreieck"),
    ToolItem(EditorTool.Polygon, Icons.Default.Hexagon, "Polygon"),
    ToolItem(EditorTool.Star, Icons.Default.Star, "Stern"),
    ToolItem(EditorTool.Path, Icons.Default.Edit, "Pfad"),
    ToolItem(EditorTool.Pencil, Icons.Default.Gesture, "Freihand"),
    ToolItem(EditorTool.Text, Icons.Default.TextFields, "Text"),
    ToolItem(EditorTool.EditPath, Icons.Default.Timeline, "Knoten"),
    ToolItem(EditorTool.Port, Icons.Default.RadioButtonChecked, "Port"),
    ToolItem(EditorTool.Anchor, Icons.Default.Anchor, "Anker"),
    ToolItem(EditorTool.ContentArea, Icons.Default.CropFree, "Content Area"),
    ToolItem(EditorTool.Gradient, Icons.Default.Gradient, "Verlauf"),
    ToolItem(EditorTool.Pan, Icons.Default.PanTool, "Verschieben")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorToolsStrip(
    editorState: EditorState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tools.forEach { item ->
            val active = editorState.activeTool == item.tool
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text(item.label) } },
                state = rememberTooltipState()
            ) {
                Surface(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { editorState.setTool(item.tool) },
                    color = if (active) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
