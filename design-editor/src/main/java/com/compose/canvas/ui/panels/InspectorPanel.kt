package com.compose.canvas.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.compose.canvas.ui.components.ColorPickerDialog
import com.compose.canvas.ui.components.inspectorFieldColors
import com.compose.canvas.ui.components.parseHexColor
import com.compose.canvas.core.BoundsCalculator
import com.compose.canvas.core.FillKind
import com.compose.canvas.core.StrokeCap
import com.compose.canvas.core.StrokeJoin
import com.compose.canvas.core.VectorObject
import com.compose.canvas.editor.AlignmentMode
import com.compose.canvas.editor.EditorState
import com.compose.canvas.editor.alignSelected
import com.compose.canvas.editor.applyBooleanOp
import com.compose.canvas.editor.applyLinearGradient
import com.compose.canvas.editor.applyRadialGradient
import com.compose.canvas.editor.convertSelectedNode
import com.compose.canvas.editor.convertSelectedToPath
import com.compose.canvas.editor.deleteSelectedNode
import com.compose.canvas.editor.flipSelected
import com.compose.canvas.editor.groupSelected
import com.compose.canvas.editor.insertMaterialShape
import com.compose.canvas.editor.materialShapePath
import com.compose.canvas.editor.setObjectParameter
import com.compose.canvas.editor.simplifySelectedPath
import com.compose.canvas.editor.toggleSelectedPathClosed
import com.compose.canvas.editor.ungroupSelected
import com.compose.canvas.geometry.EllipseGeometry
import com.compose.canvas.geometry.LineGeometry
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PresetGeometry
import com.compose.canvas.geometry.RectGeometry
import com.compose.canvas.geometry.StarGeometry
import com.compose.canvas.geometry.TextGeometry
import com.compose.canvas.geometry.TriangleGeometry
import com.compose.canvas.geometry.PathSegment
import com.compose.canvas.metadata.Port
import com.compose.canvas.metadata.PortType
import com.compose.canvas.tools.path.BooleanOp
import com.compose.canvas.tools.path.NodeType
import com.m3shapes.editor.ExpressiveShapeId

@Composable
fun InspectorPanel(
    editorState: EditorState,
    modifier: Modifier = Modifier
) {
    val selected = editorState.selectedNodes()
    val single = selected.singleOrNull() as? VectorObject

    FloatingPanel(
        title = "Eigenschaften",
        expanded = editorState.panelState.inspectorExpanded,
        onToggle = {
            editorState.panelState = editorState.panelState.copy(
                inspectorExpanded = !editorState.panelState.inspectorExpanded
            )
        },
        modifier = modifier.width(300.dp)
    ) {
        InspectorContent(editorState)
    }
}

@Composable
fun InspectorContent(
    editorState: EditorState,
    modifier: Modifier = Modifier,
) {
    val selected = editorState.selectedNodes()
    val single = selected.singleOrNull() as? VectorObject
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
            if (selected.size >= 2) {
                Text("${selected.size} Objekte", style = MaterialTheme.typography.bodyMedium)
                DeleteElementButton(onClick = { editorState.deleteSelected() }, label = "Elemente löschen")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { editorState.groupSelected() }) { Text("Gruppe") }
                    TextButton(onClick = { editorState.ungroupSelected() }) { Text("Lösen") }
                }
                Text("Ausrichtung", style = MaterialTheme.typography.labelMedium)
                wrapChips {
                    AlignmentMode.entries.forEach { mode ->
                        FilterChip(selected = false, onClick = { editorState.alignSelected(mode) }, label = { Text(mode.displayName) })
                    }
                }
                Text("Boolesch", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { editorState.applyBooleanOp(BooleanOp.Union) }) { Text("∪") }
                    TextButton(onClick = { editorState.applyBooleanOp(BooleanOp.Difference) }) { Text("−") }
                    TextButton(onClick = { editorState.applyBooleanOp(BooleanOp.Intersection) }) { Text("∩") }
                    TextButton(onClick = { editorState.applyBooleanOp(BooleanOp.Xor) }) { Text("⊕") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { editorState.flipSelected(true) }) { Text("Spiegeln H") }
                    TextButton(onClick = { editorState.flipSelected(false) }) { Text("Spiegeln V") }
                }
            }
            if (single == null && selected.isEmpty()) {
                CanvasInspector(editorState)
            }
            if (single != null) {
                ObjectInspector(obj = single, editorState = editorState)
            }
    }
}

@Composable
private fun CanvasInspector(editorState: EditorState) {
    val document = editorState.document
    val canvas = document.canvas
    val viewBox = canvas.effectiveViewBox
    Text("Viewport", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Text(document.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(canvas.width, { width ->
            editorState.updateDocumentProperties(
                document.copy(canvas = canvas.copy(width = width.coerceAtLeast(1f))),
                "Canvas width",
            )
        }, "Breite", Modifier.weight(1f))
        NumberField(canvas.height, { height ->
            editorState.updateDocumentProperties(
                document.copy(canvas = canvas.copy(height = height.coerceAtLeast(1f))),
                "Canvas height",
            )
        }, "Höhe", Modifier.weight(1f))
    }
    Text("ViewBox", style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(viewBox.left, { value -> updateViewBox(editorState, viewBox.copy(left = value)) }, "X", Modifier.weight(1f))
        NumberField(viewBox.top, { value -> updateViewBox(editorState, viewBox.copy(top = value)) }, "Y", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(viewBox.width, { value -> updateViewBox(editorState, viewBox.copy(right = viewBox.left + value.coerceAtLeast(1f))) }, "ViewBox B", Modifier.weight(1f))
        NumberField(viewBox.height, { value -> updateViewBox(editorState, viewBox.copy(bottom = viewBox.top + value.coerceAtLeast(1f))) }, "ViewBox H", Modifier.weight(1f))
    }
    HorizontalDivider()
    Text("Raster", style = MaterialTheme.typography.labelMedium)
    NumberField(editorState.gridSize, { value -> editorState.gridSize = value.coerceAtLeast(1f) }, "Rastergröße", Modifier.fillMaxWidth())
    ToggleRow("Raster sichtbar", editorState.gridVisible) { editorState.gridVisible = it }
    ToggleRow("Am Raster einrasten", editorState.snapEnabled) { editorState.snapEnabled = it }
    ToggleRow("Transparenter Hintergrund", canvas.backgroundTransparent) { transparent ->
        editorState.updateDocumentProperties(
            document.copy(canvas = canvas.copy(backgroundTransparent = transparent)),
            "Canvas background",
        )
    }
    HorizontalDivider()
    Text("Material 3 Shapes", style = MaterialTheme.typography.labelMedium)
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ExpressiveShapeId.all, key = { it.name }) { shape ->
            MaterialShapeCarouselItem(
                shape = shape,
                onClick = { editorState.insertMaterialShape(shape) },
            )
        }
    }
}

@Composable
private fun MaterialShapeCarouselItem(
    shape: ExpressiveShapeId,
    onClick: () -> Unit,
) {
    val geometry = remember(shape) { materialShapePath(shape) }
    val path = remember(geometry) { geometry.toComposePath() }
    Surface(
        modifier = Modifier
            .width(82.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            val color = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.size(58.dp)) {
                withTransform({
                    scale(
                        scaleX = size.width / 256f,
                        scaleY = size.height / 256f,
                        pivot = androidx.compose.ui.geometry.Offset.Zero,
                    )
                }) {
                    drawPath(path = path, color = color)
                }
            }
            Text(
                text = shape.label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

private fun PathGeometry.toComposePath(): Path = Path().apply {
    segments.forEach { segment ->
        when (segment) {
            is PathSegment.MoveTo -> moveTo(segment.x, segment.y)
            is PathSegment.LineTo -> lineTo(segment.x, segment.y)
            is PathSegment.QuadraticBezierTo -> quadraticTo(segment.cx, segment.cy, segment.x, segment.y)
            is PathSegment.CubicBezierTo -> cubicTo(
                segment.cx1,
                segment.cy1,
                segment.cx2,
                segment.cy2,
                segment.x,
                segment.y,
            )
            PathSegment.Close -> close()
        }
    }
}

private fun updateViewBox(editorState: EditorState, viewBox: com.compose.canvas.geometry.RectF) {
    val document = editorState.document
    editorState.updateDocumentProperties(
        document.copy(canvas = document.canvas.copy(viewBox = viewBox)),
        "Edit viewBox",
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun wrapChips(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), content = { content() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObjectInspector(obj: VectorObject, editorState: EditorState) {
    val bounds = BoundsCalculator.of(obj)
    OutlinedTextField(
        value = obj.name,
        onValueChange = { editorState.updateObject(obj.copy(name = it)) },
        label = { Text("Name", fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        colors = inspectorFieldColors()
    )
    DeleteElementButton(onClick = { editorState.deleteObject(obj.id) })
    Text(geometryLabel(obj), style = MaterialTheme.typography.bodyMedium)

    HorizontalDivider()
    Text("Transform", style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(obj.transform.x, { editorState.updateObject(obj.copy(transform = obj.transform.copy(x = it))) }, "X", Modifier.weight(1f))
        NumberField(obj.transform.y, { editorState.updateObject(obj.copy(transform = obj.transform.copy(y = it))) }, "Y", Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(bounds?.width ?: 0f, { newW ->
            val w = bounds?.width ?: return@NumberField
            if (w > 0.01f) editorState.updateObject(obj.copy(transform = obj.transform.copy(scaleX = obj.transform.scaleX * (newW / w))))
        }, "Breite", Modifier.weight(1f))
        NumberField(bounds?.height ?: 0f, { newH ->
            val h = bounds?.height ?: return@NumberField
            if (h > 0.01f) editorState.updateObject(obj.copy(transform = obj.transform.copy(scaleY = obj.transform.scaleY * (newH / h))))
        }, "Höhe", Modifier.weight(1f))
    }
    NumberField(obj.transform.rotation, { editorState.updateObject(obj.copy(transform = obj.transform.copy(rotation = it))) }, "Rotation", Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField(obj.transform.scaleX, { editorState.updateObject(obj.copy(transform = obj.transform.copy(scaleX = it))) }, "Skalierung X", Modifier.weight(1f))
        NumberField(obj.transform.scaleY, { editorState.updateObject(obj.copy(transform = obj.transform.copy(scaleY = it))) }, "Skalierung Y", Modifier.weight(1f))
    }

    when (val g = obj.geometry) {
        is RectGeometry -> NumberField(g.cornerRadius, { editorState.updateObject(obj.copy(geometry = g.copy(cornerRadius = it))) }, "Eckenradius", Modifier.fillMaxWidth())
        is TextGeometry -> {
            OutlinedTextField(
                value = g.text,
                onValueChange = { editorState.updateObject(obj.copy(geometry = g.copy(text = it))) },
                label = { Text("Text", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                colors = inspectorFieldColors()
            )
            NumberField(g.fontSize, { editorState.updateObject(obj.copy(geometry = g.copy(fontSize = it))) }, "Schriftgröße", Modifier.fillMaxWidth())
        }
        is PresetGeometry -> {
            g.parameters.forEach { (k, v) ->
                NumberField(v, { editorState.setObjectParameter(k, it) }, k, Modifier.fillMaxWidth())
            }
        }
        else -> Unit
    }

    HorizontalDivider()
    Text("Füllung", style = MaterialTheme.typography.labelMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FillKind.entries.forEach { kind ->
            FilterChip(
                selected = obj.fill.kind == kind,
                onClick = {
                    when (kind) {
                        FillKind.LinearGradient -> editorState.applyLinearGradient()
                        FillKind.RadialGradient -> editorState.applyRadialGradient()
                        else -> editorState.updateObject(obj.copy(fill = obj.fill.copy(kind = kind)))
                    }
                },
                label = { Text(kind.name.take(6)) }
            )
        }
    }
    ColorRow(
        obj.fill.color,
        obj.fill.alpha,
        editorState.recentColors,
        { c -> editorState.rememberColor(c); editorState.updateObject(obj.copy(fill = obj.fill.copy(color = c, kind = FillKind.Solid))) },
        { a -> editorState.updateObject(obj.copy(fill = obj.fill.copy(alpha = a))) }
    )

    Text("Kontur", style = MaterialTheme.typography.labelMedium)
    NumberField(obj.stroke.width, { editorState.updateObject(obj.copy(stroke = obj.stroke.copy(width = it.coerceAtLeast(0f)))) }, "Stärke", Modifier.fillMaxWidth())
    ColorRow(obj.stroke.color, obj.stroke.alpha, editorState.recentColors, { c ->
        editorState.rememberColor(c)
        editorState.updateObject(obj.copy(stroke = obj.stroke.copy(color = c)))
    }, { a -> editorState.updateObject(obj.copy(stroke = obj.stroke.copy(alpha = a))) })
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StrokeCap.entries.forEach { cap ->
            FilterChip(selected = obj.stroke.cap == cap, onClick = { editorState.updateObject(obj.copy(stroke = obj.stroke.copy(cap = cap))) }, label = { Text(cap.name) })
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StrokeJoin.entries.forEach { join ->
            FilterChip(selected = obj.stroke.join == join, onClick = { editorState.updateObject(obj.copy(stroke = obj.stroke.copy(join = join))) }, label = { Text(join.name) })
        }
    }

    HorizontalDivider()
    OutlinedButton(onClick = { editorState.convertSelectedToPath() }, modifier = Modifier.fillMaxWidth()) {
        Text("In Pfad umwandeln")
    }
    OutlinedButton(onClick = { editorState.simplifySelectedPath() }, modifier = Modifier.fillMaxWidth()) {
        Text("Pfad vereinfachen")
    }
    if (editorState.pathEdit.active) {
        Text("Knoten", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { editorState.convertSelectedNode(NodeType.Corner) }) { Text("Ecke") }
            TextButton(onClick = { editorState.convertSelectedNode(NodeType.Smooth) }) { Text("Glatt") }
            TextButton(onClick = { editorState.convertSelectedNode(NodeType.Symmetric) }) { Text("Sym.") }
        }
        Row {
            TextButton(onClick = { editorState.deleteSelectedNode() }) { Text("Knoten löschen") }
            TextButton(onClick = { editorState.toggleSelectedPathClosed() }) { Text("Öffnen/Schließen") }
        }
    }
    if (obj.semantics.ports.isNotEmpty() || obj.semantics.anchors.isNotEmpty()) {
        Text("Semantik", style = MaterialTheme.typography.labelSmall)
        Text("Ports: ${obj.semantics.ports.size} · Anker: ${obj.semantics.anchors.size} · Flächen: ${obj.semantics.contentAreas.size}", style = MaterialTheme.typography.bodySmall)
        obj.semantics.ports.forEach { port ->
            var typeMenuOpen by remember(port.id) { mutableStateOf(false) }
            OutlinedTextField(
                value = port.name,
                onValueChange = { name ->
                    updateSemanticPort(editorState, obj, port.id) { it.copy(name = name) }
                },
                label = { Text("Portname") },
                supportingText = {
                    Text(
                        listOf(port.connectionType, port.valueType)
                            .filter(String::isNotBlank)
                            .joinToString(" · ")
                            .ifBlank { "CUSTOM" },
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ExposedDropdownMenuBox(
                expanded = typeMenuOpen,
                onExpandedChange = { typeMenuOpen = !typeMenuOpen },
            ) {
                OutlinedTextField(
                    value = port.portType.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Porttyp") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenuOpen) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = typeMenuOpen,
                    onDismissRequest = { typeMenuOpen = false },
                ) {
                    PortType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                updateSemanticPort(editorState, obj, port.id) { it.copy(portType = type) }
                                typeMenuOpen = false
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun updateSemanticPort(
    editorState: EditorState,
    obj: VectorObject,
    portId: String,
    transform: (Port) -> Port,
) {
    val currentPort = obj.semantics.ports.firstOrNull { it.id == portId } ?: return
    val updatedPort = transform(currentPort)
    val updatedObject = obj.copy(
        semantics = obj.semantics.copy(
            ports = obj.semantics.ports.map { if (it.id == portId) updatedPort else it },
        ),
    )
    val metadataPorts = editorState.document.metadata.ports
    val updatedMetadataPorts = if (metadataPorts.any { it.id == portId }) {
        metadataPorts.map { if (it.id == portId) updatedPort else it }
    } else {
        metadataPorts + updatedPort
    }
    editorState.updateDocumentProperties(
        editorState.document
            .replaceObject(updatedObject)
            .copy(metadata = editorState.document.metadata.copy(ports = updatedMetadataPorts)),
        label = "Edit port ${currentPort.name}",
    )
}

private fun geometryLabel(obj: VectorObject): String = when (val g = obj.geometry) {
    is RectGeometry -> "Rechteck ${fmt(g.width)} × ${fmt(g.height)}"
    is EllipseGeometry -> "Ellipse rx=${fmt(g.radiusX)} ry=${fmt(g.radiusY)}"
    is LineGeometry -> "Linie"
    is PathGeometry -> "Pfad · ${g.segments.size} Segmente"
    is TriangleGeometry -> "Dreieck"
    is StarGeometry -> "Stern ${g.points}"
    is TextGeometry -> "Text"
    is PresetGeometry -> "Preset ${g.presetId}"
    else -> g::class.simpleName ?: "Form"
}

@Composable
private fun DeleteElementButton(
    onClick: () -> Unit,
    label: String = "Element löschen"
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
    ) {
        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(label, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ColorRow(
    color: Color,
    alpha: Float,
    recents: List<Color>,
    onColor: (Color) -> Unit,
    onAlpha: (Float) -> Unit
) {
    var pickerOpen by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = alpha), RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .clickable { pickerOpen = true }
        )
        OutlinedTextField(
            value = if (color == Color.Unspecified) "" else String.format("#%06X", 0xFFFFFF and color.toArgb()),
            onValueChange = { hex -> parseHexColor(hex)?.let(onColor) },
            label = { Text("Hex", fontSize = 13.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            colors = inspectorFieldColors()
        )
    }
    TextButton(onClick = { pickerOpen = true }) {
        Text("Farbwähler öffnen", style = MaterialTheme.typography.bodyMedium)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        recents.forEach { c ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(c, RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                    .clickable { onColor(c) }
            )
        }
    }
    NumberField(alpha, { onAlpha(it.coerceIn(0f, 1f)) }, "Deckkraft", Modifier.fillMaxWidth())
    if (pickerOpen) {
        ColorPickerDialog(
            initial = color,
            initialAlpha = alpha,
            recents = recents,
            onDismiss = { pickerOpen = false },
            onConfirm = { picked, pickedAlpha ->
                onColor(picked)
                onAlpha(pickedAlpha)
                pickerOpen = false
            }
        )
    }
}

@Composable
private fun NumberField(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(fmt(value)) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    it.toFloatOrNull()?.let(onValueChange)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun fmt(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else "%.1f".format(value)
