package com.m3shapes.editor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val SVG_CANVAS_SIZE = 256f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NativeSvgEditorTab(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var history by remember {
        mutableStateOf(
            SvgCommandHistory(
                present = SvgDocument().withElements(
                    listOf(
                        SvgRectElement(name = "StartRect"),
                        SvgCircleElement(name = "StartCircle", cx = 152f, cy = 152f)
                    )
                )
            )
        )
    }
    var liveDocument by remember { mutableStateOf(history.present) }
    var ui by remember { mutableStateOf(SvgEditorUiState()) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragCommitLabel by remember { mutableStateOf<String?>(null) }
    var dragDirty by remember { mutableStateOf(false) }
    var documentForPendingExport by remember { mutableStateOf<SvgDocument?>(null) }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/svg+xml")
    ) { uri ->
        val doc = documentForPendingExport
        documentForPendingExport = null
        if (uri == null || doc == null) return@rememberLauncherForActivityResult
        val payload = SvgDocumentCodec.export(doc)
        writeTextToUri(context, uri, payload).fold(
            onSuccess = { Toast.makeText(context, "SVG exportiert", Toast.LENGTH_SHORT).show() },
            onFailure = { Toast.makeText(context, "Export fehlgeschlagen: ${it.message}", Toast.LENGTH_SHORT).show() }
        )
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = readTextFromUri(context, uri).getOrElse { error ->
            Toast.makeText(context, "Import fehlgeschlagen: ${error.message}", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val imported = SvgDocumentCodec.import(text)
        history = history.execute("Import SVG") { imported.document }
        liveDocument = history.present
        ui = ui.copy(diagnostics = imported.warnings)
    }

    val document = liveDocument
    val selected = document.selectedElements()
    val selectionBounds = remember(document.elements, document.selectedElementIds) {
        if (selected.isEmpty()) null else boundsOf(selected)
    }

    fun execute(label: String, reducer: (SvgDocument) -> SvgDocument) {
        history = history.execute(label, reducer)
        liveDocument = history.present
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Native SVG Editor (ohne WebView)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SvgTool.entries.forEach { tool ->
                FilterChip(
                    selected = ui.tool == tool,
                    onClick = { ui = ui.copy(tool = tool) },
                    label = { Text(tool.name) }
                )
            }
            OutlinedButton(
                onClick = {
                    history = history.undo()
                    liveDocument = history.present
                },
                enabled = history.past.isNotEmpty()
            ) { Text("Undo") }
            OutlinedButton(
                onClick = {
                    history = history.redo()
                    liveDocument = history.present
                },
                enabled = history.future.isNotEmpty()
            ) { Text("Redo") }
            OutlinedButton(onClick = { execute("Delete selection") { it.removeSelected() } }, enabled = document.selectedElementIds.isNotEmpty()) { Text("Delete") }
            OutlinedButton(
                onClick = {
                    execute("Insert node") { doc ->
                        val (nextDoc, nextUi) = doc.insertNodeAfterSelection(ui)
                        ui = nextUi
                        nextDoc
                    }
                },
                enabled = ui.selectedNodeElementId != null
            ) { Text("+ Node") }
            OutlinedButton(
                onClick = {
                    execute("Delete node") { doc ->
                        val (nextDoc, nextUi) = doc.deleteSelectedNode(ui)
                        ui = nextUi
                        nextDoc
                    }
                },
                enabled = ui.selectedNodeElementId != null
            ) { Text("- Node") }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(liveDocument, ui.tool, ui.zoom, ui.panX, ui.panY) {
                                detectDragGestures(
                                    onDragStart = { pointer ->
                                        val viewportPoint = pointer.toSvgViewport(size.width.toFloat(), size.height.toFloat(), ui)
                                        dragStart = viewportPoint
                                        dragCommitLabel = null
                                        dragDirty = false
                                        when (ui.tool) {
                                            SvgTool.Select -> {
                                                liveDocument = liveDocument.selectTopMost(viewportPoint)
                                            }
                                            SvgTool.Rect -> {
                                                execute("Add rect") {
                                                    it.addElement(
                                                        SvgRectElement(
                                                            x = viewportPoint.x - 36f,
                                                            y = viewportPoint.y - 24f,
                                                            width = 72f,
                                                            height = 48f,
                                                            name = "Rect ${it.elements.size + 1}"
                                                        )
                                                    )
                                                }
                                            }
                                            SvgTool.Circle -> {
                                                execute("Add circle") {
                                                    it.addElement(
                                                        SvgCircleElement(
                                                            cx = viewportPoint.x,
                                                            cy = viewportPoint.y,
                                                            radius = 34f,
                                                            name = "Circle ${it.elements.size + 1}"
                                                        )
                                                    )
                                                }
                                            }
                                            SvgTool.Polygon -> {
                                                execute("Add polygon") {
                                                    it.addElement(
                                                        SvgPolygonElement(
                                                            points = listOf(
                                                                viewportPoint + Offset(0f, -38f),
                                                                viewportPoint + Offset(34f, 28f),
                                                                viewportPoint + Offset(-34f, 28f)
                                                            ),
                                                            name = "Polygon ${it.elements.size + 1}"
                                                        )
                                                    )
                                                }
                                            }
                                            SvgTool.Path -> {
                                                execute("Add path") {
                                                    it.addElement(
                                                        SvgPathElement(
                                                            name = "Path ${it.elements.size + 1}",
                                                            closed = true,
                                                            nodes = listOf(
                                                                SvgPathNode(anchor = viewportPoint + Offset(-40f, -30f), type = SvgPathSegmentType.Move),
                                                                SvgPathNode(anchor = viewportPoint + Offset(45f, -25f), outHandle = viewportPoint + Offset(60f, 6f), type = SvgPathSegmentType.Cubic),
                                                                SvgPathNode(anchor = viewportPoint + Offset(30f, 38f), inHandle = viewportPoint + Offset(48f, 25f), type = SvgPathSegmentType.Cubic),
                                                                SvgPathNode(anchor = viewportPoint + Offset(-36f, 34f))
                                                            )
                                                        )
                                                    )
                                                }
                                            }
                                            SvgTool.PathNodes -> {
                                                val hit = liveDocument.findNearestNode(viewportPoint)
                                                if (hit != null) {
                                                    ui = ui.copy(selectedNodeElementId = hit.first, selectedNodeId = hit.second)
                                                    liveDocument = liveDocument.selectById(hit.first)
                                                } else {
                                                    ui = ui.copy(selectedNodeElementId = null, selectedNodeId = null)
                                                }
                                            }
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val dx = dragAmount.x / ui.zoom
                                        val dy = dragAmount.y / ui.zoom
                                        when (ui.tool) {
                                            SvgTool.Select -> {
                                                if (liveDocument.selectedElementIds.isNotEmpty()) {
                                                    liveDocument = liveDocument.moveSelection(dx, dy)
                                                    dragCommitLabel = "Move selection"
                                                    dragDirty = true
                                                } else {
                                                    ui = ui.copy(panX = ui.panX + dragAmount.x, panY = ui.panY + dragAmount.y)
                                                }
                                            }
                                            SvgTool.PathNodes -> {
                                                val eid = ui.selectedNodeElementId
                                                val nid = ui.selectedNodeId
                                                if (eid != null && nid != null) {
                                                    val element = liveDocument.elements.firstOrNull { it.id == eid }
                                                    val current = when (element) {
                                                        is SvgPathElement -> element.nodes.firstOrNull { it.id == nid }?.anchor
                                                        is SvgPolygonElement -> nid.removePrefix("p").toIntOrNull()
                                                            ?.let { idx -> element.points.getOrNull(idx) }
                                                        else -> null
                                                    }
                                                    if (current != null) {
                                                        liveDocument = liveDocument.updatePathNode(
                                                            elementId = eid,
                                                            nodeId = nid,
                                                            anchor = current + Offset(dx, dy)
                                                        )
                                                        dragCommitLabel = "Move node"
                                                        dragDirty = true
                                                    }
                                                } else {
                                                    ui = ui.copy(panX = ui.panX + dragAmount.x, panY = ui.panY + dragAmount.y)
                                                }
                                            }
                                            else -> Unit
                                        }
                                    },
                                    onDragEnd = {
                                        if (dragDirty) {
                                            val label = dragCommitLabel ?: "Drag move"
                                            history = history.execute(label) { liveDocument }
                                            liveDocument = history.present
                                        }
                                        dragStart = null
                                        dragCommitLabel = null
                                        dragDirty = false
                                    },
                                    onDragCancel = {
                                        if (dragDirty) {
                                            liveDocument = history.present
                                        }
                                        dragStart = null
                                        dragCommitLabel = null
                                        dragDirty = false
                                    }
                                )
                            }
                    ) {
                        drawSvgGrid(size.width, size.height, ui)
                        val t = viewportTransform(size.width, size.height, ui)
                        document.elements.sortedBy { it.zIndex }.forEach { element ->
                            if (!element.visible) return@forEach
                            drawElement(element, t)
                        }

                        selectionBounds?.let { bounds ->
                            val b = bounds.toCanvas(t)
                            drawRect(
                                color = Color(0xFF1E88E5),
                                topLeft = Offset(b.left, b.top),
                                size = androidx.compose.ui.geometry.Size(b.right - b.left, b.bottom - b.top),
                                style = Stroke(width = 2f)
                            )
                        }
                        drawPathNodeOverlay(document, ui, t)
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Transform", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { execute("Scale down") { it.scaleSelection(0.9f) } },
                            enabled = document.selectedElementIds.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("- Scale") }
                        OutlinedButton(
                            onClick = { execute("Scale up") { it.scaleSelection(1.1f) } },
                            enabled = document.selectedElementIds.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("+ Scale") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { execute("Rotate left") { it.rotateSelection(-15f) } },
                            enabled = document.selectedElementIds.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("Rotate -15") }
                        OutlinedButton(
                            onClick = { execute("Rotate right") { it.rotateSelection(15f) } },
                            enabled = document.selectedElementIds.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("Rotate +15") }
                    }
                    Text("Zoom ${(ui.zoom * 100f).roundToInt()}%")
                    Slider(
                        value = ui.zoom,
                        onValueChange = { ui = ui.copy(zoom = it.coerceIn(0.4f, 4f)) },
                        valueRange = 0.4f..4f
                    )
                    OutlinedButton(onClick = { ui = ui.copy(zoom = 1f, panX = 0f, panY = 0f) }) { Text("Reset View") }

                    HorizontalDivider()
                    Text("Layer / Group", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { execute("Bring forward") { it.bringSelectionForward() } },
                            enabled = document.selectedElementIds.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("Vor") }
                        OutlinedButton(
                            onClick = { execute("Send backward") { it.sendSelectionBackward() } },
                            enabled = document.selectedElementIds.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("Zurück") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { execute("Group") { it.groupSelection() } },
                            enabled = document.selectedElementIds.size >= 2,
                            modifier = Modifier.weight(1f)
                        ) { Text("Group") }
                        OutlinedButton(
                            onClick = { execute("Ungroup") { it.ungroupSelection() } },
                            enabled = document.selectedElementIds.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) { Text("Ungroup") }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        document.elements.asReversed().forEach { element ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (element.id in document.selectedElementIds) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { execute("Select layer") { it.selectById(element.id) } }
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${element.name} (${element::class.simpleName?.removePrefix("Svg")?.removeSuffix("Element")})",
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    OutlinedButton(
                                        onClick = { execute("Toggle visible") { it.toggleVisibility(element.id) } },
                                        modifier = Modifier.height(28.dp)
                                    ) { Text(if (element.visible) "Vis" else "Hid") }
                                    Spacer(Modifier.width(4.dp))
                                    OutlinedButton(
                                        onClick = { execute("Toggle lock") { it.toggleLock(element.id) } },
                                        modifier = Modifier.height(28.dp)
                                    ) { Text(if (element.locked) "Lock" else "Open") }
                                }
                            }
                        }
                    }

                    HorizontalDivider()
                    Text("Import / Export", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                documentForPendingExport = document
                                exportFileLauncher.launch("native_editor_export.svg")
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Export SVG") }
                        OutlinedButton(
                            onClick = { importFileLauncher.launch(arrayOf("image/svg+xml", "text/*", "*/*")) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Import SVG") }
                    }
                    OutlinedButton(
                        onClick = {
                            copyToClipboard(
                                context,
                                label = "SVG",
                                text = SvgDocumentCodec.export(document),
                                toast = "SVG in Clipboard"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("SVG kopieren") }

                    if (ui.diagnostics.isNotEmpty()) {
                        Text("Import-Hinweise", fontWeight = FontWeight.SemiBold)
                        ui.diagnostics.forEach { line ->
                            Text("• $line", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private data class SvgCanvasTransform(
    val zoomScale: Float,
    val offsetX: Float,
    val offsetY: Float
)

private fun viewportTransform(width: Float, height: Float, ui: SvgEditorUiState): SvgCanvasTransform {
    val base = min(width, height) / SVG_CANVAS_SIZE
    val zoomScale = base * ui.zoom
    val contentSize = SVG_CANVAS_SIZE * zoomScale
    return SvgCanvasTransform(
        zoomScale = zoomScale,
        offsetX = (width - contentSize) / 2f + ui.panX,
        offsetY = (height - contentSize) / 2f + ui.panY
    )
}

private fun Offset.toSvgViewport(width: Float, height: Float, ui: SvgEditorUiState): Offset {
    val t = viewportTransform(width, height, ui)
    return Offset(
        x = (x - t.offsetX) / t.zoomScale,
        y = (y - t.offsetY) / t.zoomScale
    )
}

private data class CanvasBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

private fun SvgBounds.toCanvas(transform: SvgCanvasTransform): CanvasBounds = CanvasBounds(
    left = left * transform.zoomScale + transform.offsetX,
    top = top * transform.zoomScale + transform.offsetY,
    right = right * transform.zoomScale + transform.offsetX,
    bottom = bottom * transform.zoomScale + transform.offsetY
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSvgGrid(
    width: Float,
    height: Float,
    ui: SvgEditorUiState
) {
    val t = viewportTransform(width, height, ui)
    val step = SVG_CANVAS_SIZE / 8f
    for (x in 0..8) {
        val px = x * step * t.zoomScale + t.offsetX
        drawLine(
            color = Color(0xFFEDEDED),
            start = Offset(px, t.offsetY),
            end = Offset(px, t.offsetY + SVG_CANVAS_SIZE * t.zoomScale),
            strokeWidth = if (x % 4 == 0) 1.2f else 0.7f
        )
    }
    for (y in 0..8) {
        val py = y * step * t.zoomScale + t.offsetY
        drawLine(
            color = Color(0xFFEDEDED),
            start = Offset(t.offsetX, py),
            end = Offset(t.offsetX + SVG_CANVAS_SIZE * t.zoomScale, py),
            strokeWidth = if (y % 4 == 0) 1.2f else 0.7f
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawElement(
    element: SvgElement,
    t: SvgCanvasTransform
) {
    when (element) {
        is SvgRectElement -> {
            val cx = element.x + element.width / 2f
            val cy = element.y + element.height / 2f
            rotate(
                degrees = element.rotation,
                pivot = Offset(cx * t.zoomScale + t.offsetX, cy * t.zoomScale + t.offsetY)
            ) {
                drawRect(
                    color = element.style.fill.copy(alpha = 0.35f),
                    topLeft = Offset(element.x * t.zoomScale + t.offsetX, element.y * t.zoomScale + t.offsetY),
                    size = androidx.compose.ui.geometry.Size(element.width * t.zoomScale, element.height * t.zoomScale),
                    style = Fill
                )
                drawRect(
                    color = element.style.stroke,
                    topLeft = Offset(element.x * t.zoomScale + t.offsetX, element.y * t.zoomScale + t.offsetY),
                    size = androidx.compose.ui.geometry.Size(element.width * t.zoomScale, element.height * t.zoomScale),
                    style = Stroke(width = max(1f, element.style.strokeWidth * t.zoomScale * 0.15f))
                )
            }
        }
        is SvgCircleElement -> {
            drawCircle(
                color = element.style.fill.copy(alpha = 0.35f),
                radius = element.radius * t.zoomScale,
                center = Offset(element.cx * t.zoomScale + t.offsetX, element.cy * t.zoomScale + t.offsetY)
            )
            drawCircle(
                color = element.style.stroke,
                radius = element.radius * t.zoomScale,
                center = Offset(element.cx * t.zoomScale + t.offsetX, element.cy * t.zoomScale + t.offsetY),
                style = Stroke(width = max(1f, element.style.strokeWidth * t.zoomScale * 0.15f))
            )
        }
        is SvgPolygonElement -> {
            if (element.points.size < 2) return
            val p = Path()
            element.points.forEachIndexed { index, pt ->
                val c = Offset(pt.x * t.zoomScale + t.offsetX, pt.y * t.zoomScale + t.offsetY)
                if (index == 0) p.moveTo(c.x, c.y) else p.lineTo(c.x, c.y)
            }
            p.close()
            drawPath(p, color = element.style.fill.copy(alpha = 0.35f), style = Fill)
            drawPath(
                p,
                color = element.style.stroke,
                style = Stroke(width = max(1f, element.style.strokeWidth * t.zoomScale * 0.15f))
            )
        }
        is SvgPathElement -> {
            if (element.nodes.size < 2) return
            val p = Path()
            element.nodes.forEachIndexed { index, node ->
                val anchor = Offset(node.anchor.x * t.zoomScale + t.offsetX, node.anchor.y * t.zoomScale + t.offsetY)
                if (index == 0) {
                    p.moveTo(anchor.x, anchor.y)
                } else {
                    val prev = element.nodes[index - 1]
                    val out = prev.outHandle
                    val inn = node.inHandle
                    if (out != null || inn != null || node.type == SvgPathSegmentType.Cubic) {
                        val c1 = (out ?: prev.anchor)
                        val c2 = (inn ?: node.anchor)
                        p.cubicTo(
                            c1.x * t.zoomScale + t.offsetX,
                            c1.y * t.zoomScale + t.offsetY,
                            c2.x * t.zoomScale + t.offsetX,
                            c2.y * t.zoomScale + t.offsetY,
                            anchor.x,
                            anchor.y
                        )
                    } else {
                        p.lineTo(anchor.x, anchor.y)
                    }
                }
            }
            if (element.closed) p.close()
            drawPath(p, color = element.style.fill.copy(alpha = 0.22f), style = Fill)
            drawPath(
                p,
                color = element.style.stroke,
                style = Stroke(width = max(1f, element.style.strokeWidth * t.zoomScale * 0.15f))
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPathNodeOverlay(
    document: SvgDocument,
    ui: SvgEditorUiState,
    t: SvgCanvasTransform
) {
    if (ui.tool != SvgTool.PathNodes) return
    document.elements.forEach { element ->
        when (element) {
            is SvgPathElement -> {
                element.nodes.forEach { node ->
                    val center = Offset(node.anchor.x * t.zoomScale + t.offsetX, node.anchor.y * t.zoomScale + t.offsetY)
                    val selected = ui.selectedNodeElementId == element.id && ui.selectedNodeId == node.id
                    val color = if (selected) Color(0xFFE53935) else Color(0xFF1E88E5)
                    drawCircle(color = color, radius = if (selected) 7f else 5f, center = center)
                    drawCircle(color = Color.White, radius = 2f, center = center)
                    val inH = node.inHandle
                    val outH = node.outHandle
                    if (inH != null) {
                        val inC = Offset(inH.x * t.zoomScale + t.offsetX, inH.y * t.zoomScale + t.offsetY)
                        drawLine(Color(0xFFB0BEC5), center, inC, strokeWidth = 1.2f)
                        drawCircle(Color(0xFF607D8B), radius = 3.5f, center = inC)
                    }
                    if (outH != null) {
                        val outC = Offset(outH.x * t.zoomScale + t.offsetX, outH.y * t.zoomScale + t.offsetY)
                        drawLine(Color(0xFFB0BEC5), center, outC, strokeWidth = 1.2f)
                        drawCircle(Color(0xFF607D8B), radius = 3.5f, center = outC)
                    }
                }
            }
            is SvgPolygonElement -> {
                element.points.forEachIndexed { index, point ->
                    val center = Offset(point.x * t.zoomScale + t.offsetX, point.y * t.zoomScale + t.offsetY)
                    val nodeId = "p$index"
                    val selected = ui.selectedNodeElementId == element.id && ui.selectedNodeId == nodeId
                    drawCircle(
                        color = if (selected) Color(0xFFE53935) else Color(0xFF1E88E5),
                        radius = if (selected) 7f else 5f,
                        center = center
                    )
                    drawCircle(color = Color.White, radius = 2f, center = center)
                }
            }
            else -> Unit
        }
    }
}
