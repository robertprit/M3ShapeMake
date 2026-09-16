package com.compose.canvas.interchange

import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorObject
import com.compose.canvas.geometry.GeometryToPath
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import com.compose.canvas.geometry.PresetGeometry
import org.json.JSONArray
import org.json.JSONObject

object ComposePathExport : ComposePathExporter {
    override fun export(document: VectorDocument, normalize: Boolean): ExportResult {
        val objects = document.allObjects()
        if (objects.isEmpty()) {
            return ExportResult(
                "val path = Path()",
                listOf(ExportWarning("EMPTY", "No objects to export"))
            )
        }
        val vb = document.canvas.effectiveViewBox
        val sx = if (normalize && vb.width > 0f) 1f / vb.width else 1f
        val sy = if (normalize && vb.height > 0f) 1f / vb.height else 1f
        val body = buildString {
            objects.forEachIndexed { index, obj ->
                val geom = GeometryToPath.toPath(obj.geometry, obj.semantics.parameters)
                val shifted = shift(geom, obj.transform.x, obj.transform.y, sx, sy)
                if (objects.size == 1) {
                    append(ComposePathCodegen.generate(shifted))
                } else {
                    appendLine(ComposePathCodegen.generate(shifted, "path${index + 1}"))
                    appendLine()
                }
            }
        }
        val warnings = mutableListOf<ExportWarning>()
        if (objects.any { it.fill.kind.name.contains("Gradient") }) {
            warnings += ExportWarning("GRADIENT", "Compose Path export does not include fills or gradients")
        }
        return ExportResult(body.trim(), warnings)
    }
}

object KotlinShapeExport : KotlinShapeExporter {
    override fun export(document: VectorDocument): ExportResult {
        val className = sanitizeClassName(document.name) + "Shape"
        val obj = document.allObjects().firstOrNull()
        val params = linkedMapOf<String, Float>()
        params.putAll(document.metadata.parameters)
        obj?.semantics?.parameters?.let { params.putAll(it) }
        val geom = if (obj != null) GeometryToPath.toPath(obj.geometry, obj.semantics.parameters)
        else PathGeometry(emptyList())
        val vb = document.canvas.effectiveViewBox
        val w = vb.width.coerceAtLeast(1f)
        val h = vb.height.coerceAtLeast(1f)
        val dx = obj?.transform?.x ?: 0f
        val dy = obj?.transform?.y ?: 0f
        val ctor = if (params.isEmpty()) "" else params.entries.joinToString(",\n    ") { (k, v) ->
            "private val ${sanitizeIdent(k)}: Float = ${fmt(v)}f"
        }.let { "(\n    $it\n)" }
        val pathBody = buildString {
            for (seg in geom.segments) {
                when (seg) {
                    is PathSegment.MoveTo ->
                        appendLine("            moveTo(${scale("x", seg.x + dx, w)}, ${scale("y", seg.y + dy, h)})")
                    is PathSegment.LineTo ->
                        appendLine("            lineTo(${scale("x", seg.x + dx, w)}, ${scale("y", seg.y + dy, h)})")
                    is PathSegment.QuadraticBezierTo ->
                        appendLine(
                            "            quadraticBezierTo(${scale("x", seg.cx + dx, w)}, ${scale("y", seg.cy + dy, h)}, " +
                                "${scale("x", seg.x + dx, w)}, ${scale("y", seg.y + dy, h)})"
                        )
                    is PathSegment.CubicBezierTo ->
                        appendLine(
                            "            cubicTo(${scale("x", seg.cx1 + dx, w)}, ${scale("y", seg.cy1 + dy, h)}, " +
                                "${scale("x", seg.cx2 + dx, w)}, ${scale("y", seg.cy2 + dy, h)}, " +
                                "${scale("x", seg.x + dx, w)}, ${scale("y", seg.y + dy, h)})"
                        )
                    PathSegment.Close -> appendLine("            close()")
                }
            }
        }
        val warnings = mutableListOf<ExportWarning>()
        if (document.allObjects().size > 1) {
            warnings += ExportWarning("MULTI_OBJECT", "Kotlin Shape uses the first object; group/boolean first for a single outline")
        }
        if (obj?.geometry is PresetGeometry) {
            warnings += ExportWarning("BAKED_PATH", "Parameter values are constructor properties; the outline is baked at export time")
        }
        val src = """
            |import androidx.compose.ui.geometry.Size
            |import androidx.compose.ui.graphics.Outline
            |import androidx.compose.ui.graphics.Path
            |import androidx.compose.ui.graphics.Shape
            |import androidx.compose.ui.unit.Density
            |import androidx.compose.ui.unit.LayoutDirection
            |
            |class $className$ctor : Shape {
            |    override fun createOutline(
            |        size: Size,
            |        layoutDirection: LayoutDirection,
            |        density: Density
            |    ): Outline {
            |        val path = Path().apply {
            |$pathBody        }
            |        return Outline.Generic(path)
            |    }
            |}
        """.trimMargin()
        return ExportResult(src, warnings)
    }

    private fun scale(axis: String, value: Float, doc: Float): String {
        val factor = "size.${if (axis == "x") "width" else "height"}"
        return "($factor * ${fmt(value / doc)}f)"
    }
}

object MetadataExport : MetadataExporter {
    override fun export(document: VectorDocument): ExportResult {
        val vb = document.canvas.effectiveViewBox
        val objects = document.allObjects()
        val params = linkedMapOf<String, Float>()
        params.putAll(document.metadata.parameters)
        objects.forEach { params.putAll(it.semantics.parameters) }
        val ports = JSONArray()
        val anchors = JSONArray()
        val areas = JSONArray()
        objects.forEach { obj ->
            obj.semantics.ports.forEach { p ->
                ports.put(JSONObject().apply {
                    put("id", p.id); put("name", p.name)
                    put("x", p.x.toDouble()); put("y", p.y.toDouble())
                    put("portType", p.portType.name)
                })
            }
            obj.semantics.anchors.forEach { a ->
                anchors.put(JSONObject().apply {
                    put("id", a.id); put("name", a.name)
                    put("x", a.x.toDouble()); put("y", a.y.toDouble())
                    put("kind", a.kind.name)
                })
            }
            obj.semantics.contentAreas.forEach { c ->
                areas.put(JSONObject().apply {
                    put("id", c.id); put("name", c.name)
                    put("contentType", c.contentType)
                    put("bounds", JSONObject().apply {
                        put("left", c.bounds.left.toDouble())
                        put("top", c.bounds.top.toDouble())
                        put("right", c.bounds.right.toDouble())
                        put("bottom", c.bounds.bottom.toDouble())
                    })
                })
            }
        }
        val json = JSONObject().apply {
            put("format", "vt-shape")
            put("formatVersion", 1)
            put("version", 1)
            put("name", document.name)
            put("viewBox", JSONObject().apply {
                put("width", vb.width.toDouble())
                put("height", vb.height.toDouble())
            })
            put("parameters", JSONObject().apply {
                params.forEach { (k, v) -> put(k, v.toDouble()) }
            })
            put("ports", ports)
            put("anchors", anchors)
            put("contentAreas", areas)
        }
        return ExportResult(json.toString(2))
    }
}

private fun shift(geom: PathGeometry, dx: Float, dy: Float, sx: Float, sy: Float): PathGeometry {
    fun tx(x: Float) = (x + dx) * sx
    fun ty(y: Float) = (y + dy) * sy
    return PathGeometry(
        geom.segments.map { seg ->
            when (seg) {
                is PathSegment.MoveTo -> PathSegment.MoveTo(tx(seg.x), ty(seg.y))
                is PathSegment.LineTo -> PathSegment.LineTo(tx(seg.x), ty(seg.y))
                is PathSegment.QuadraticBezierTo ->
                    PathSegment.QuadraticBezierTo(tx(seg.cx), ty(seg.cy), tx(seg.x), ty(seg.y))
                is PathSegment.CubicBezierTo -> PathSegment.CubicBezierTo(
                    tx(seg.cx1), ty(seg.cy1), tx(seg.cx2), ty(seg.cy2), tx(seg.x), ty(seg.y)
                )
                PathSegment.Close -> PathSegment.Close
            }
        },
        geom.closed
    )
}

private fun sanitizeClassName(name: String): String {
    val filtered = name.filter { it.isLetterOrDigit() }
    return filtered.replaceFirstChar { it.uppercase() }.ifBlank { "Custom" }
}

private fun sanitizeIdent(name: String): String {
    val raw = buildString {
        name.forEachIndexed { i, c ->
            append(if (c.isLetterOrDigit()) c else '_')
            if (i == 0 && c.isDigit()) insert(0, '_')
        }
    }.ifBlank { "param" }
    return raw
}

private fun fmt(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString() else "%.4f".format(v).trimEnd('0').trimEnd('.')
