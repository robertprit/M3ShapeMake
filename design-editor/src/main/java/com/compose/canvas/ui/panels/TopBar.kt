package com.compose.canvas.ui.panels

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.compose.canvas.editor.EditorState
import com.compose.canvas.ui.theme.ChromeBorder
import com.compose.canvas.ui.theme.ChromeIcon
import com.compose.canvas.ui.theme.ChromeIconDisabled
import com.compose.canvas.ui.theme.ChromeIconMuted
import com.compose.canvas.ui.theme.ChromeOnSurface
import com.compose.canvas.ui.theme.ChromeSurface
import com.compose.canvas.viewport.ViewportController
import kotlin.math.roundToInt

@Composable
fun TopBar(
    editorState: EditorState,
    viewportController: ViewportController,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.border(1.dp, ChromeBorder),
        color = ChromeSurface,
        contentColor = ChromeOnSurface,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            EditorToolsStrip(editorState = editorState)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                ChromeIconButton(
                    onClick = { editorState.undo() },
                    enabled = editorState.history.canUndo,
                    icon = Icons.Default.Undo,
                    contentDescription = "Rückgängig"
                )
                ChromeIconButton(
                    onClick = { editorState.redo() },
                    enabled = editorState.history.canRedo,
                    icon = Icons.Default.Redo,
                    contentDescription = "Wiederholen"
                )

                Spacer(Modifier.width(4.dp))

                val zoomPercent = (viewportController.state.zoom * 100).roundToInt()
                Text("$zoomPercent%", style = MaterialTheme.typography.labelLarge, color = ChromeOnSurface)

                ChromeIconButton(
                    onClick = {
                        val z = viewportController.state.clampZoom(viewportController.state.zoom * 1.25f)
                        viewportController.zoomAt(
                            z,
                            viewportController.state.screenWidth / 2f,
                            viewportController.state.screenHeight / 2f
                        )
                    },
                    icon = Icons.Default.ZoomIn,
                    contentDescription = "Vergrößern"
                )
                ChromeIconButton(
                    onClick = {
                        val z = viewportController.state.clampZoom(viewportController.state.zoom / 1.25f)
                        viewportController.zoomAt(
                            z,
                            viewportController.state.screenWidth / 2f,
                            viewportController.state.screenHeight / 2f
                        )
                    },
                    icon = Icons.Default.ZoomOut,
                    contentDescription = "Verkleinern"
                )
                ChromeIconButton(
                    onClick = {
                        viewportController.fitToDocument(
                            editorState.document.canvas.width,
                            editorState.document.canvas.height
                        )
                    },
                    icon = Icons.Default.FitScreen,
                    contentDescription = "An Fenster anpassen"
                )
                TextButton(onClick = { viewportController.resetTo100() }) {
                    Text("100%", style = MaterialTheme.typography.labelMedium, color = ChromeOnSurface)
                }

                ChromeIconButton(
                    onClick = { editorState.setGridEnabled(!editorState.gridVisible) },
                    icon = if (editorState.gridVisible) Icons.Default.GridOn else Icons.Default.GridOff,
                    contentDescription = "Raster",
                    tint = if (editorState.gridVisible) MaterialTheme.colorScheme.primary else ChromeIconMuted
                )
                TextButton(onClick = { editorState.snapEnabled = !editorState.snapEnabled }) {
                    Text(
                        if (editorState.snapEnabled) "Snap" else "Frei",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (editorState.snapEnabled) MaterialTheme.colorScheme.primary else ChromeIconMuted
                    )
                }
                ChromeIconButton(
                    onClick = { editorState.previewMode = !editorState.previewMode },
                    icon = if (editorState.previewMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Vorschau"
                )
                ChromeIconButton(
                    onClick = {
                        editorState.panelState = editorState.panelState.copy(
                            exportExpanded = !editorState.panelState.exportExpanded
                        )
                    },
                    icon = Icons.Default.IosShare,
                    contentDescription = "Export"
                )

                Spacer(Modifier.weight(1f))
                Text(
                    editorState.document.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = ChromeOnSurface
                )
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ChromeIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    tint: Color = ChromeIcon
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = ChromeIcon,
            disabledContentColor = ChromeIconDisabled
        )
    ) {
        Icon(
            icon,
            contentDescription,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) tint else ChromeIconDisabled
        )
    }
}
