package com.compose.canvas.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.compose.canvas.ui.components.ColorPickerDialog
import com.compose.canvas.ui.components.inspectorFieldColors
import com.compose.canvas.ui.components.parseHexColor
import com.compose.canvas.ui.theme.ChromeBorder
import com.compose.canvas.ui.theme.ChromeOnSurface
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
import com.compose.canvas.editor.insertPreset
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
import com.compose.canvas.tools.path.BooleanOp
import com.compose.canvas.tools.path.NodeType

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
        Column(
            modifier = Modifier
                .heightIn(max = 420.dp)
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
                Text("Keine Auswahl", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Presets", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { editorState.insertPreset("logicBlock") }) { Text("Logic Block") }
                TextButton(onClick = { editorState.insertPreset("cookie") }) { Text("Cookie") }
                TextButton(onClick = { editorState.insertPreset("pill") }) { Text("Pill") }
                TextButton(onClick = { editorState.insertPreset("arch") }) { Text("Bogen") }
            }
            if (single != null) {
                ObjectInspector(obj = single, editorState = editorState)
            }
        }
    }
}

@Composable
private fun wrapChips(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), content = { content() })
}

@Composable
private fun ObjectInspector(obj: VectorObject, editorState: EditorState) {
    val bounds = BoundsCalculator.of(obj)
    OutlinedTextField(
        value = obj.name,
        onValueChange = { editorState.updateObject(obj.copy(name = it)) },
        label = { Text("Name", fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = InspectorValueStyle,
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
                textStyle = InspectorValueStyle,
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
    }
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

private val InspectorValueStyle = TextStyle(
    fontSize = 16.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.Medium,
    color = ChromeOnSurface
)

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
                .border(1.dp, ChromeBorder, RoundedCornerShape(8.dp))
                .clickable { pickerOpen = true }
        )
        OutlinedTextField(
            value = if (color == Color.Unspecified) "" else String.format("#%06X", 0xFFFFFF and color.toArgb()),
            onValueChange = { hex -> parseHexColor(hex)?.let(onColor) },
            label = { Text("Hex", fontSize = 13.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = InspectorValueStyle,
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
                    .border(1.dp, ChromeBorder, RoundedCornerShape(6.dp))
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
        Text(label, style = MaterialTheme.typography.labelLarge, color = ChromeOnSurface)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .border(1.dp, ChromeBorder, RoundedCornerShape(8.dp))
                .background(Color.White, RoundedCornerShape(8.dp))
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
                textStyle = InspectorValueStyle,
                cursorBrush = SolidColor(ChromeOnSurface),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun fmt(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else "%.1f".format(value)
