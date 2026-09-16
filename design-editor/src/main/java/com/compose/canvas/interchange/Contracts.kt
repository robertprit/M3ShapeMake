package com.compose.canvas.interchange

import com.compose.canvas.core.VectorDocument

enum class DiagnosticLevel { INFO, WARNING, UNSUPPORTED, ERROR }

data class ImportDiagnostic(
    val level: DiagnosticLevel,
    val code: String,
    val message: String
)

data class ImportResult(
    val document: VectorDocument?,
    val diagnostics: List<ImportDiagnostic> = emptyList()
)

data class ExportWarning(
    val code: String,
    val message: String
)

data class ExportResult(
    val text: String,
    val warnings: List<ExportWarning> = emptyList()
)

fun interface SvgImporter {
    fun import(svg: String): ImportResult
}

fun interface SvgExporter {
    fun export(document: VectorDocument): ExportResult
}

fun interface ComposePathExporter {
    fun export(document: VectorDocument, normalize: Boolean): ExportResult
}

fun interface KotlinShapeExporter {
    fun export(document: VectorDocument): ExportResult
}

fun interface MetadataExporter {
    fun export(document: VectorDocument): ExportResult
}
