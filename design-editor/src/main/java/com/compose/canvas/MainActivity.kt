package com.compose.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.editor.EditorState
import com.compose.canvas.editor.PanelState
import com.compose.canvas.editor.copySelected
import com.compose.canvas.editor.cutSelected
import com.compose.canvas.editor.groupSelected
import com.compose.canvas.editor.paste
import com.compose.canvas.editor.ungroupSelected
import com.compose.canvas.ui.components.EditorCanvas
import com.compose.canvas.ui.panels.ExportPanel
import com.compose.canvas.ui.panels.InspectorPanel
import com.compose.canvas.ui.panels.LayersPanel
import com.compose.canvas.ui.panels.TopBar
import com.compose.canvas.viewport.ViewportController

@Composable
fun VectorShapeEditorApp(
    editorState: EditorState = remember {
        EditorState(initialDocument = VectorDocument(name = "Untitled"))
    },
    modifier: Modifier = Modifier
) {
    val viewportController = remember { ViewportController() }
    val focusRequester = remember { FocusRequester() }
    var compactLayoutInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event -> handleShortcut(editorState, event) }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxWidth < 700.dp
            LaunchedEffect(compact) {
                if (compact && !compactLayoutInitialized) {
                    editorState.panelState = PanelState(
                        inspectorExpanded = false,
                        layersExpanded = false,
                        exportExpanded = false
                    )
                    compactLayoutInitialized = true
                }
            }
            EditorCanvas(
                editorState = editorState,
                viewportController = viewportController,
                modifier = Modifier.fillMaxSize()
            )
            TopBar(
                editorState = editorState,
                viewportController = viewportController,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            InspectorPanel(
                editorState = editorState,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
            LayersPanel(
                editorState = editorState,
                modifier = Modifier
                    .align(if (compact) Alignment.BottomStart else Alignment.CenterEnd)
                    .padding(
                        start = if (compact) 8.dp else 0.dp,
                        end = if (compact) 0.dp else 8.dp,
                        top = if (compact) 0.dp else 100.dp,
                        bottom = if (compact) 8.dp else 0.dp
                    )
            )
            if (!compact || editorState.panelState.exportExpanded) {
                ExportPanel(
                    editorState = editorState,
                    modifier = Modifier
                        .align(if (compact) Alignment.CenterEnd else Alignment.TopEnd)
                        .padding(
                            top = if (compact) 0.dp else 100.dp,
                            end = 8.dp
                        )
                )
            }
        }
    }
}

private fun handleShortcut(editorState: EditorState, event: KeyEvent): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val isCtrl = event.isCtrlPressed || event.isMetaPressed
    return when (event.key) {
        Key.Z -> {
            if (event.isShiftPressed) editorState.redo() else editorState.undo()
            true
        }
        Key.Y -> {
            if (isCtrl) {
                editorState.redo()
                true
            } else false
        }
        Key.D -> {
            if (isCtrl) {
                editorState.duplicateSelected()
                true
            } else false
        }
        Key.C -> {
            if (isCtrl) {
                editorState.copySelected()
                true
            } else false
        }
        Key.X -> {
            if (isCtrl) {
                editorState.cutSelected()
                true
            } else false
        }
        Key.V -> {
            if (isCtrl) {
                editorState.paste()
                true
            } else false
        }
        Key.G -> {
            if (isCtrl && event.isShiftPressed) {
                editorState.ungroupSelected()
                true
            } else if (isCtrl) {
                editorState.groupSelected()
                true
            } else false
        }
        Key.Delete, Key.Backspace -> {
            editorState.deleteSelected()
            true
        }
        Key.Escape -> {
            editorState.cancelPath()
            editorState.clearSelection()
            true
        }
        else -> false
    }
}
