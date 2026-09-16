package com.compose.canvas.ui.panels

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.editor.EditorState
import com.compose.canvas.interchange.ComposePathExport
import com.compose.canvas.interchange.DocumentCodec
import com.compose.canvas.interchange.ExportResult
import com.compose.canvas.interchange.KotlinShapeExport
import com.compose.canvas.interchange.MetadataExport
import com.compose.canvas.interchange.ImageVectorExport
import com.compose.canvas.interchange.SvgExport
import com.compose.canvas.interchange.SvgImport
import com.compose.canvas.interchange.VectorDrawableExport
import com.compose.canvas.interchange.VisualMetadataExport
import com.compose.canvas.asset.newVisualAsset
import com.compose.canvas.asset.toShapeMakerDocument

private enum class ExportKind { Svg, ComposePath, KotlinShape, ImageVector, VectorDrawable, Metadata, VisualMetadata, Project }

@Composable
fun ExportPanel(
    editorState: EditorState,
    modifier: Modifier = Modifier
) {
    var kind by remember { mutableStateOf(ExportKind.Svg) }
    var lastDiagnostics by remember { mutableStateOf<List<String>>(emptyList()) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val result = remember(editorState.document, kind, editorState.selection) {
        exportOf(kind, editorState.document)
    }
    val openSvg = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@rememberLauncherForActivityResult
        val imported = SvgImport.import(text)
        imported.document?.let { editorState.replaceDocument(it) }
        lastDiagnostics = imported.diagnostics.map { "${it.level} ${it.code}: ${it.message}" }
    }
    val openProject = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@rememberLauncherForActivityResult
        runCatching { editorState.replaceDocument(DocumentCodec.deserialize(text)) }
    }

    FloatingPanel(
        title = "Export",
        expanded = editorState.panelState.exportExpanded,
        onToggle = {
            editorState.panelState = editorState.panelState.copy(
                exportExpanded = !editorState.panelState.exportExpanded
            )
        },
        modifier = modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ExportKind.entries.forEach { k ->
                    FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) })
                }
            }
            result.warnings.forEach {
                Text("${it.code}: ${it.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            lastDiagnostics.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            Text(
                result.text.take(1600) + if (result.text.length > 1600) "…" else "",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(result.text)) }) { Text("Kopieren") }
                OutlinedButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, result.text)
                    }
                    context.startActivity(Intent.createChooser(send, "Export"))
                }) { Text("Teilen") }
            }
            TextButton(onClick = { openSvg.launch(arrayOf("image/svg+xml", "text/xml", "*/*")) }) { Text("SVG importieren") }
            TextButton(onClick = { openProject.launch(arrayOf("application/json", "*/*")) }) { Text("Projekt öffnen (.vshape)") }
        }
    }
}

private val ExportKind.label: String
    get() = when (this) {
        ExportKind.Svg -> "SVG"
        ExportKind.ComposePath -> "Path"
        ExportKind.KotlinShape -> "Shape"
        ExportKind.ImageVector -> "ImageVector"
        ExportKind.VectorDrawable -> "Vector"
        ExportKind.Metadata -> "Meta"
        ExportKind.VisualMetadata -> "Visual"
        ExportKind.Project -> ".vshape"
    }

private fun exportOf(kind: ExportKind, document: VectorDocument): ExportResult = when (kind) {
    ExportKind.Svg -> SvgExport.export(document)
    ExportKind.ComposePath -> ComposePathExport.export(document, normalize = false)
    ExportKind.KotlinShape -> KotlinShapeExport.export(document)
    ExportKind.ImageVector -> ImageVectorExport.export(
        newVisualAsset(name = document.name, document = document).toShapeMakerDocument()
    )
    ExportKind.VectorDrawable -> VectorDrawableExport.export(
        newVisualAsset(name = document.name, document = document).toShapeMakerDocument()
    )
    ExportKind.Metadata -> MetadataExport.export(document)
    ExportKind.VisualMetadata -> VisualMetadataExport.export(
        newVisualAsset(name = document.name, document = document).toShapeMakerDocument()
    )
    ExportKind.Project -> ExportResult(DocumentCodec.serialize(document))
}
