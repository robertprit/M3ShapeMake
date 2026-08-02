package com.m3shapes.editor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun M3ShapeExportPanel(
    namespace: String,
    onNamespaceChange: (String) -> Unit,
    bundleId: String,
    onBundleIdChange: (String) -> Unit,
    statusText: String,
    onCopyJson: () -> Unit,
    onValidate: () -> Unit,
    onSaveLast: () -> Unit,
    onLoadLast: () -> Unit,
    onExportFile: () -> Unit,
    onImportFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Asset Bundle",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Local .m3shape.json — clipboard, internal save/load, or SAF file export/import.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = namespace,
            onValueChange = onNamespaceChange,
            label = { Text("Namespace") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bundleId,
            onValueChange = onBundleIdChange,
            label = { Text("Bundle ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilledTonalButton(onClick = onCopyJson, modifier = Modifier.weight(1f)) {
                Text("Copy Bundle JSON", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(onClick = onValidate, modifier = Modifier.weight(1f)) {
                Text("Validate Bundle", style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onSaveLast, modifier = Modifier.weight(1f)) {
                Text("Save Last Bundle", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(onClick = onLoadLast, modifier = Modifier.weight(1f)) {
                Text("Load Last Bundle", style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onExportFile, modifier = Modifier.weight(1f)) {
                Text("Export File", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(onClick = onImportFile, modifier = Modifier.weight(1f)) {
                Text("Import File", style = MaterialTheme.typography.labelSmall)
            }
        }
        if (statusText.isNotBlank()) {
            Text(
                statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
