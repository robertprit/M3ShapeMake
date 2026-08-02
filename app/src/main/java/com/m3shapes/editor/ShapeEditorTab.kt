package com.m3shapes.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShapeEditorTab(
    formTab: Int,
    onFormTabChange: (Int) -> Unit,
    modeA: ShapeMode,
    modeB: ShapeMode,
    onModeAChange: (ShapeMode) -> Unit,
    onModeBChange: (ShapeMode) -> Unit,
    materialA: ExpressiveShapeId,
    materialB: ExpressiveShapeId,
    onMaterialA: (ExpressiveShapeId) -> Unit,
    onMaterialB: (ExpressiveShapeId) -> Unit,
    customA: FormConfig,
    customB: FormConfig,
    onCustomAChange: (FormConfig) -> Unit,
    onCustomBChange: (FormConfig) -> Unit,
    drawnA: DrawnPolygonConfig,
    drawnB: DrawnPolygonConfig,
    onDrawnAChange: (DrawnPolygonConfig) -> Unit,
    onDrawnBChange: (DrawnPolygonConfig) -> Unit,
    freehandModeA: Boolean,
    freehandModeB: Boolean,
    onFreehandAChange: (Boolean) -> Unit,
    onFreehandBChange: (Boolean) -> Unit,
    overlayA: EditorOverlay,
    overlayB: EditorOverlay,
    onOverlayAChange: (EditorOverlay) -> Unit,
    onOverlayBChange: (EditorOverlay) -> Unit,
    resolvedA: RoundedPolygon,
    resolvedB: RoundedPolygon,
    exportSourceA: ShapeSource,
    exportSourceB: ShapeSource,
    studioSelectedColor: Color,
    onStudioSelectedColorChange: (Color) -> Unit
) {
    val activeMode = if (formTab == 0) modeA else modeB
    val activeSource = if (formTab == 0) exportSourceA else exportSourceB
    val activeMaterial = if (formTab == 0) materialA else materialB
    val activeCustom = if (formTab == 0) customA else customB
    val activeDrawn = if (formTab == 0) drawnA else drawnB
    val activeFreehand = if (formTab == 0) freehandModeA else freehandModeB
    val activeOverlay = if (formTab == 0) overlayA else overlayB
    val activeResolved = if (formTab == 0) resolvedA else resolvedB

    val setMode: (ShapeMode) -> Unit = { if (formTab == 0) onModeAChange(it) else onModeBChange(it) }
    val saveCustom: (FormConfig) -> Unit = { if (formTab == 0) onCustomAChange(it) else onCustomBChange(it) }
    val saveDrawn: (DrawnPolygonConfig) -> Unit = { if (formTab == 0) onDrawnAChange(it) else onDrawnBChange(it) }
    val saveMaterial: (ExpressiveShapeId) -> Unit = { if (formTab == 0) onMaterialA(it) else onMaterialB(it) }
    val setFreehand: (Boolean) -> Unit = { if (formTab == 0) onFreehandAChange(it) else onFreehandBChange(it) }
    val setOverlay: (EditorOverlay) -> Unit = { if (formTab == 0) onOverlayAChange(it) else onOverlayBChange(it) }

    var editorStateA by remember { mutableStateOf(ShapeEditorState()) }
    var editorStateB by remember { mutableStateOf(ShapeEditorState()) }
    var lastDisplayModeA by remember { mutableStateOf(ShapeMode.Material) }
    var lastDisplayModeB by remember { mutableStateOf(ShapeMode.Material) }
    val activeEditorState = if (formTab == 0) editorStateA else editorStateB
    val lastDisplayMode = if (formTab == 0) lastDisplayModeA else lastDisplayModeB
    val setEditorState: (ShapeEditorState) -> Unit = {
        if (formTab == 0) editorStateA = it else editorStateB = it
    }
    val setLastDisplayMode: (ShapeMode) -> Unit = {
        if (formTab == 0) lastDisplayModeA = it else lastDisplayModeB = it
    }

    fun importDisplayedShapeIntoEditor() {
        val source = when (lastDisplayMode) {
            ShapeMode.Material -> ShapeSource.Material(activeMaterial)
            ShapeMode.Parametric -> ShapeSource.Custom(activeCustom)
            ShapeMode.Editor -> ShapeSource.Material(activeMaterial)
        }
        val polygon = resolveShape(source)
        val rounding = if (source is ShapeSource.Custom) source.config.rounding else 0f
        val smoothing = if (source is ShapeSource.Custom) source.config.smoothing else 0f
        saveDrawn(
            fitDrawnPolygonConfig(
                drawnFromRoundedPolygon(polygon, rounding = rounding, smoothing = smoothing)
            )
        )
        setEditorState(ShapeEditorState())
    }

    val previewPath = remember(activeResolved) { activeResolved.toPath().asComposePath() }
    val editableVertices = remember(activeDrawn) { ensureDrawnCornerIds(activeDrawn).toEditableVertices() }
    val fillHex = "#%06X".format(studioSelectedColor.toArgb() and 0xFFFFFF)
    val pathData = remember(activeResolved) { activeResolved.toVpPathData() }
    val svgPreview = remember(pathData, fillHex) { generateShapeSvg(pathData, fillHex) }
    val composePreview = remember(activeSource, studioSelectedColor) {
        generateShapeComposePreview(activeSource, studioSelectedColor)
    }

    fun pushEditable(updated: List<EditableVertex>) {
        saveDrawn(
            updated.toDrawnPolygonConfig(
                fallbackRounding = activeDrawn.rounding,
                fallbackSmoothing = activeDrawn.smoothing
            )
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TabRow(selectedTabIndex = formTab, modifier = Modifier.width(200.dp)) {
                    Tab(
                        selected = formTab == 0,
                        onClick = { onFormTabChange(0) },
                        text = { Text("Form A") }
                    )
                    Tab(
                        selected = formTab == 1,
                        onClick = { onFormTabChange(1) },
                        text = { Text("Form B") }
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    shapeLabel(activeSource),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val s = size.width / 256f
                scale(s, s, pivot = Offset.Zero) {
                    drawPath(previewPath, studioSelectedColor, style = Fill)
                    drawPath(previewPath, Color.Red.copy(alpha = 0.35f), style = Stroke(width = 1.5f))
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Formauswahl / Polygon",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                CodePreviewCard(title = "SVG Preview", code = svgPreview)
                CodePreviewCard(title = "Kotlin Compose Preview", code = composePreview)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShapeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = activeMode == mode,
                            onClick = {
                                if (mode == ShapeMode.Material || mode == ShapeMode.Parametric) {
                                    setLastDisplayMode(mode)
                                }
                                setMode(mode)
                            },
                            label = {
                                Text(
                                    when (mode) {
                                        ShapeMode.Material -> "Material"
                                        ShapeMode.Parametric -> "Parametrisch"
                                        ShapeMode.Editor -> "Editor"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                when (activeMode) {
                    ShapeMode.Editor -> {
                        Text(
                            if (activeFreehand) "Freihand zeichnen…" else "Eckpunkte ziehen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "View: ${(activeEditorState.viewport.zoom * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(84.dp)
                            )
                            OutlinedButton(
                                onClick = {
                                    setEditorState(
                                        activeEditorState.copy(
                                            viewport = activeEditorState.viewport.copy(
                                                zoom = (activeEditorState.viewport.zoom - 0.25f).coerceIn(0.25f, 4f)
                                            )
                                        )
                                    )
                                }
                            ) { Text("−", style = MaterialTheme.typography.labelSmall) }
                            OutlinedButton(
                                onClick = {
                                    setEditorState(
                                        activeEditorState.copy(
                                            viewport = activeEditorState.viewport.copy(
                                                zoom = (activeEditorState.viewport.zoom + 0.25f).coerceIn(0.25f, 4f)
                                            )
                                        )
                                    )
                                }
                            ) { Text("+", style = MaterialTheme.typography.labelSmall) }
                            OutlinedButton(onClick = { setEditorState(activeEditorState.resetViewport()) }) {
                                Text("Reset", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Slider(
                            value = activeEditorState.viewport.zoom,
                            onValueChange = {
                                setEditorState(
                                    activeEditorState.copy(
                                        viewport = activeEditorState.viewport.copy(zoom = it)
                                    )
                                )
                            },
                            valueRange = 0.25f..4f
                        )

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = activeEditorState.multiSelectEnabled,
                                onClick = {
                                    setEditorState(
                                        activeEditorState.copy(
                                            multiSelectEnabled = !activeEditorState.multiSelectEnabled
                                        )
                                    )
                                },
                                label = { Text("Multi Select", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = activeEditorState.snapToGrid,
                                onClick = {
                                    setEditorState(
                                        activeEditorState.copy(snapToGrid = !activeEditorState.snapToGrid)
                                    )
                                },
                                label = { Text("Snap to Grid", style = MaterialTheme.typography.labelSmall) }
                            )
                            FilterChip(
                                selected = activeEditorState.showVertexIndices,
                                onClick = {
                                    setEditorState(
                                        activeEditorState.copy(
                                            showVertexIndices = !activeEditorState.showVertexIndices
                                        )
                                    )
                                },
                                label = { Text("Show Indices", style = MaterialTheme.typography.labelSmall) }
                            )
                        }

                        Text("Overlay", style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            EditorOverlay.entries.forEach { ov ->
                                FilterChip(
                                    selected = activeOverlay == ov,
                                    onClick = { setOverlay(ov) },
                                    label = {
                                        Text(
                                            when (ov) {
                                                EditorOverlay.Off -> "Aus"
                                                EditorOverlay.Grid4 -> "Grid 4"
                                                EditorOverlay.Grid8 -> "Grid 8"
                                                EditorOverlay.Dots8 -> "Dots"
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = activeFreehand,
                                onClick = { setFreehand(!activeFreehand) },
                                label = { Text("Freihand", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(Icons.Filled.Edit, null, Modifier.size(14.dp)) }
                            )
                            OutlinedButton(
                                onClick = {
                                    saveDrawn(fitDrawnPolygonConfig(defaultDrawnPolygon(4)))
                                    setEditorState(ShapeEditorState())
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Refresh, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reset Shape", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        OutlinedButton(
                            onClick = { importDisplayedShapeIntoEditor() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val sourceLabel = when (lastDisplayMode) {
                                ShapeMode.Material -> activeMaterial.label
                                ShapeMode.Parametric -> "Parametrisch (${activeCustom.numVertices})"
                                ShapeMode.Editor -> activeMaterial.label
                            }
                            Text(
                                "Form übernehmen: $sourceLabel",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Text("Default Rundung", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = activeDrawn.rounding,
                            onValueChange = { saveDrawn(activeDrawn.copy(rounding = it)) }
                        )
                        Text("Default Glättung", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = activeDrawn.smoothing,
                            onValueChange = { saveDrawn(activeDrawn.copy(smoothing = it)) }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (editableVertices.size >= 24) return@OutlinedButton
                                    val inserted = insertVertexAfterSelection(
                                        editableVertices,
                                        activeEditorState.lastSelectedVertexId
                                    )
                                    val insertedId = run {
                                        val after = activeEditorState.lastSelectedVertexId
                                            ?.let { id -> editableVertices.indexOfFirst { it.id == id } }
                                            ?.takeIf { it >= 0 }
                                        if (after != null && after + 1 < inserted.size) inserted[after + 1].id
                                        else inserted.last().id
                                    }
                                    pushEditable(inserted)
                                    setEditorState(
                                        activeEditorState.copy(
                                            selectedVertexIds = setOf(insertedId),
                                            lastSelectedVertexId = insertedId
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                enabled = editableVertices.size < 24
                            ) { Text("+ Vertex", style = MaterialTheme.typography.labelSmall) }
                            OutlinedButton(
                                onClick = {
                                    val id = activeEditorState.lastSelectedVertexId ?: return@OutlinedButton
                                    if (editableVertices.size <= 3) return@OutlinedButton
                                    val remaining = editableVertices.filterNot { it.id == id }
                                    pushEditable(remaining)
                                    setEditorState(
                                        activeEditorState.copy(
                                            selectedVertexIds = emptySet(),
                                            lastSelectedVertexId = null
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                enabled = editableVertices.size > 3 &&
                                    activeEditorState.lastSelectedVertexId != null
                            ) { Text("− Vertex", style = MaterialTheme.typography.labelSmall) }
                        }

                        PolygonEditorCanvas(
                            config = ensureDrawnCornerIds(activeDrawn),
                            onConfigChange = saveDrawn,
                            fillColor = studioSelectedColor,
                            freehandMode = activeFreehand,
                            overlay = activeOverlay,
                            editorState = activeEditorState,
                            onEditorStateChange = setEditorState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(420.dp)
                        )

                        VertexListPanel(
                            vertices = editableVertices,
                            selectedVertexIds = activeEditorState.selectedVertexIds,
                            multiSelectEnabled = activeEditorState.multiSelectEnabled,
                            onSelectVertex = { id -> setEditorState(activeEditorState.selectVertex(id)) },
                            onUpdateVertex = { updated ->
                                pushEditable(
                                    editableVertices.map { if (it.id == updated.id) updated else it }
                                )
                            },
                            onDeleteVertex = { id ->
                                if (editableVertices.size <= 3) return@VertexListPanel
                                pushEditable(editableVertices.filterNot { it.id == id })
                                setEditorState(
                                    activeEditorState.copy(
                                        selectedVertexIds = activeEditorState.selectedVertexIds - id,
                                        lastSelectedVertexId = activeEditorState.lastSelectedVertexId
                                            ?.takeUnless { it == id }
                                    )
                                )
                            },
                            onToggleLocked = { id ->
                                pushEditable(
                                    editableVertices.map {
                                        if (it.id == id) it.copy(locked = !it.locked) else it
                                    }
                                )
                            }
                        )
                    }
                    ShapeMode.Parametric -> {
                        Text(
                            "Vertex editing is available in Editor mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Ecken: ${activeCustom.numVertices}", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = activeCustom.numVertices.toFloat(),
                            onValueChange = {
                                setLastDisplayMode(ShapeMode.Parametric)
                                saveCustom(activeCustom.copy(numVertices = it.roundToInt()))
                            },
                            valueRange = 3f..12f
                        )
                        Text("Rundung", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = activeCustom.rounding,
                            onValueChange = {
                                setLastDisplayMode(ShapeMode.Parametric)
                                saveCustom(activeCustom.copy(rounding = it))
                            }
                        )
                        Text("Glättung", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = activeCustom.smoothing,
                            onValueChange = {
                                setLastDisplayMode(ShapeMode.Parametric)
                                saveCustom(activeCustom.copy(smoothing = it))
                            }
                        )
                    }
                    ShapeMode.Material -> {
                        Text(
                            "Vertex editing is available in Editor mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.height(360.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(ExpressiveShapeId.all) { shapeId ->
                                val selected = activeMaterial == shapeId
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            setLastDisplayMode(ShapeMode.Material)
                                            saveMaterial(shapeId)
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                        }
                                    )
                                ) {
                                    Column(
                                        Modifier.padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Canvas(Modifier.size(36.dp)) {
                                            val mini = shapeId.toRoundedPolygon().fitToViewport()
                                            val miniPath = mini.toPath().asComposePath()
                                            val s = size.minDimension / 256f
                                            scale(s, s, pivot = Offset.Zero) {
                                                drawPath(miniPath, studioSelectedColor, style = Fill)
                                            }
                                        }
                                        Text(
                                            shapeId.label,
                                            fontSize = 9.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            lineHeight = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                Text("Farbe", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    studioColors.forEach { color ->
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (studioSelectedColor == color) 2.dp else 0.dp,
                                    Color.Black,
                                    CircleShape
                                )
                                .clickable { onStudioSelectedColorChange(color) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CodePreviewCard(title: String, code: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
