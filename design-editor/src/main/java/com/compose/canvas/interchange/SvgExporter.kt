package com.compose.canvas.interchange

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.compose.canvas.core.FillKind
import com.compose.canvas.core.FillStyle
import com.compose.canvas.core.LinearGradientResource
import com.compose.canvas.core.RadialGradientResource
import com.compose.canvas.core.SceneNode
import com.compose.canvas.core.StrokeCap
import com.compose.canvas.core.StrokeStyle
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorObject
import com.compose.canvas.geometry.EllipseGeometry
import com.compose.canvas.geometry.GeometryToPath
import com.compose.canvas.geometry.LineGeometry
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import com.compose.canvas.geometry.RectGeometry
import com.compose.canvas.geometry.TextGeometry
import com.compose.canvas.math.TransformUtils
import kotlin.math.roundToInt

object SvgExport : SvgExporter {
    override fun export(document: VectorDocument): ExportResult {
        val warnings = mutableListOf<ExportWarning>()
        val vb = document.canvas.effectiveViewBox
        val defs = buildString {
            if (document.definitions.gradients.isNotEmpty()) {
                appendLine("  <defs>")
                document.definitions.gradients.forEach { g ->
                    appendLine(gradientSvg(g))
                }
                appendLine("  </defs>")
            }
        }
        val body = buildString {
            document.layers.filter { it.visible }.forEach { layer ->
                appendLine("  <g data-layer=\"${escape(layer.name)}\">")
                layer.objects.filter { it.visible }.forEach { append(nodeSvg(it, document, "    ", warnings)) }
                appendLine("  </g>")
            }
        }
        val svg = """
            |<svg xmlns="http://www.w3.org/2000/svg" viewBox="${fmt(vb.left)} ${fmt(vb.top)} ${fmt(vb.width)} ${fmt(vb.height)}">
            |$defs$body</svg>
        """.trimMargin()
        return ExportResult(svg, warnings)
    }

    private fun nodeSvg(node: SceneNode, doc: VectorDocument, indent: String, warnings: MutableList<ExportWarning>): String =
        when (node) {
            is com.compose.canvas.core.GroupObject -> {
                val t = transformAttr(node.transform)
                buildString {
                    appendLine("$indent<g$t>")
                    node.children.filter { it.visible }.forEach { append(nodeSvg(it, doc, "$indent  ", warnings)) }
                    appendLine("$indent</g>")
                }
            }
            is VectorObject -> objectSvg(node, doc, indent, warnings)
        }

    private fun objectSvg(obj: VectorObject, doc: VectorDocument, indent: String, warnings: MutableList<ExportWarning>): String {
        val style = styleAttr(obj, doc, warnings)
        val t = transformAttr(obj.transform)
        return when (val g = obj.geometry) {
            is RectGeometry ->
                "$indent<rect x=\"0\" y=\"0\" width=\"${fmt(g.width)}\" height=\"${fmt(g.height)}\"" +
                    (if (g.cornerRadius > 0f) " rx=\"${fmt(g.cornerRadius)}\"" else "") +
                    "$t $style/>\n"
            is EllipseGeometry ->
                "$indent<ellipse cx=\"0\" cy=\"0\" rx=\"${fmt(g.radiusX)}\" ry=\"${fmt(g.radiusY)}\"$t $style/>\n"
            is LineGeometry ->
                "$indent<line x1=\"${fmt(g.startX)}\" y1=\"${fmt(g.startY)}\" x2=\"${fmt(g.endX)}\" y2=\"${fmt(g.endY)}\"$t $style/>\n"
            is TextGeometry ->
                "$indent<text x=\"0\" y=\"${fmt(g.fontSize)}\" font-family=\"${escape(g.fontFamily)}\" font-size=\"${fmt(g.fontSize)}\" font-weight=\"${g.fontWeight}\"$t $style>${escape(g.text)}</text>\n"
            else -> {
                val path = GeometryToPath.toPath(g, obj.semantics.parameters)
                "$indent<path d=\"${pathD(path)}\"$t $style/>\n"
            }
        }
    }

    private fun styleAttr(obj: VectorObject, doc: VectorDocument, warnings: MutableList<ExportWarning>): String {
        val fill = when (obj.fill.kind) {
            FillKind.None -> "none"
            FillKind.Solid -> if (obj.fill.color == Color.Unspecified) "none" else hex(obj.fill.color)
            FillKind.LinearGradient, FillKind.RadialGradient -> {
                val id = obj.fill.gradientId
                if (id == null) {
                    warnings += ExportWarning("GRADIENT_MISSING", "Fill gradient missing on ${obj.name}")
                    "none"
                } else "url(#$id)"
            }
        }
        val fillOp = obj.fill.alpha
        val stroke = if (obj.stroke.width <= 0f) "none" else hex(obj.stroke.color)
        val sb = StringBuilder("fill=\"$fill\" stroke=\"$stroke\" stroke-width=\"${fmt(obj.stroke.width)}\"")
        if (fillOp < 0.999f) sb.append(" fill-opacity=\"${fmt(fillOp)}\"")
        if (obj.stroke.alpha < 0.999f) sb.append(" stroke-opacity=\"${fmt(obj.stroke.alpha)}\"")
        if (obj.stroke.cap != StrokeCap.Butt) sb.append(" stroke-linecap=\"${obj.stroke.cap.name.lowercase()}\"")
        if (obj.stroke.dash.isNotEmpty()) sb.append(" stroke-dasharray=\"${obj.stroke.dash.joinToString(",") { fmt(it) }}\"")
        return sb.toString()
    }

    private fun transformAttr(t: com.compose.canvas.core.ObjectTransform): String {
        if (t == com.compose.canvas.core.ObjectTransform()) return ""
        val parts = mutableListOf<String>()
        if (t.x != 0f || t.y != 0f) parts += "translate(${fmt(t.x)} ${fmt(t.y)})"
        if (t.rotation != 0f) parts += "rotate(${fmt(t.rotation)})"
        if (t.scaleX != 1f || t.scaleY != 1f) parts += "scale(${fmt(t.scaleX)} ${fmt(t.scaleY)})"
        return if (parts.isEmpty()) "" else " transform=\"${parts.joinToString(" ")}\""
    }

    fun pathD(path: PathGeometry): String = buildString {
        for (seg in path.segments) {
            when (seg) {
                is PathSegment.MoveTo -> append("M ${fmt(seg.x)} ${fmt(seg.y)} ")
                is PathSegment.LineTo -> append("L ${fmt(seg.x)} ${fmt(seg.y)} ")
                is PathSegment.QuadraticBezierTo -> append("Q ${fmt(seg.cx)} ${fmt(seg.cy)} ${fmt(seg.x)} ${fmt(seg.y)} ")
                is PathSegment.CubicBezierTo ->
                    append("C ${fmt(seg.cx1)} ${fmt(seg.cy1)} ${fmt(seg.cx2)} ${fmt(seg.cy2)} ${fmt(seg.x)} ${fmt(seg.y)} ")
                PathSegment.Close -> append("Z ")
            }
        }
    }.trim()

    private fun gradientSvg(g: com.compose.canvas.core.GradientResource): String = when (g) {
        is LinearGradientResource -> {
            val stops = g.stops.joinToString("\n") { s ->
                "      <stop offset=\"${(s.offset * 100).roundToInt()}%\" stop-color=\"${hex(s.color)}\" stop-opacity=\"${fmt(s.alpha)}\"/>"
            }
            "    <linearGradient id=\"${g.id}\" x1=\"${fmt(g.x1)}\" y1=\"${fmt(g.y1)}\" x2=\"${fmt(g.x2)}\" y2=\"${fmt(g.y2)}\">\n$stops\n    </linearGradient>"
        }
        is RadialGradientResource -> {
            val stops = g.stops.joinToString("\n") { s ->
                "      <stop offset=\"${(s.offset * 100).roundToInt()}%\" stop-color=\"${hex(s.color)}\" stop-opacity=\"${fmt(s.alpha)}\"/>"
            }
            "    <radialGradient id=\"${g.id}\" cx=\"${fmt(g.cx)}\" cy=\"${fmt(g.cy)}\" r=\"${fmt(g.r)}\">\n$stops\n    </radialGradient>"
        }
        else -> ""
    }

    private fun hex(c: Color): String = String.format("#%06X", 0xFFFFFF and c.toArgb())
    private fun fmt(v: Float): String = if (v == v.toInt().toFloat()) v.toInt().toString() else "%.3f".format(v)
    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
