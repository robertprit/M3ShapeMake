package com.m3shapes.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

data class SvgImportResult(
    val document: SvgDocument,
    val warnings: List<String>
)

object SvgDocumentCodec {
    fun export(document: SvgDocument): String {
        val body = document.elements.sortedBy { it.zIndex }.joinToString("\n") { element ->
            when (element) {
                is SvgRectElement -> """
<rect id="${element.id}" x="${f(element.x)}" y="${f(element.y)}" width="${f(element.width)}" height="${f(element.height)}" fill="${hex(element.style.fill)}" stroke="${hex(element.style.stroke)}" stroke-width="${f(element.style.strokeWidth)}" />
""".trim()

                is SvgCircleElement -> """
<circle id="${element.id}" cx="${f(element.cx)}" cy="${f(element.cy)}" r="${f(element.radius)}" fill="${hex(element.style.fill)}" stroke="${hex(element.style.stroke)}" stroke-width="${f(element.style.strokeWidth)}" />
""".trim()

                is SvgPolygonElement -> """
<polygon id="${element.id}" points="${element.points.joinToString(" ") { "${f(it.x)},${f(it.y)}" }}" fill="${hex(element.style.fill)}" stroke="${hex(element.style.stroke)}" stroke-width="${f(element.style.strokeWidth)}" />
""".trim()

                is SvgPathElement -> """
<path id="${element.id}" d="${toPathData(element)}" fill="${hex(element.style.fill)}" stroke="${hex(element.style.stroke)}" stroke-width="${f(element.style.strokeWidth)}" />
""".trim()
            }
        }
        return """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${f(document.width)} ${f(document.height)}">
$body
</svg>
""".trim()
    }

    fun import(svgText: String): SvgImportResult {
        val warnings = mutableListOf<String>()
        val viewBox = Regex("""viewBox\s*=\s*"([^"]+)"""").find(svgText)?.groupValues?.getOrNull(1)
        val (width, height) = viewBox?.split(" ")
            ?.takeIf { it.size == 4 }
            ?.let { parts ->
                val w = parts[2].toFloatOrNull()
                val h = parts[3].toFloatOrNull()
                if (w != null && h != null) w to h else null
            } ?: (256f to 256f)

        val parsed = mutableListOf<SvgElement>()
        parseTags("rect", svgText).forEach { attrs ->
            parsed += SvgRectElement(
                id = attrs["id"] ?: newSvgId(),
                name = attrs["id"] ?: "Rect",
                x = attrs["x"].toFloatSafe(48f),
                y = attrs["y"].toFloatSafe(48f),
                width = attrs["width"].toFloatSafe(64f),
                height = attrs["height"].toFloatSafe(64f),
                style = attrs.toStyle()
            )
        }
        parseTags("circle", svgText).forEach { attrs ->
            parsed += SvgCircleElement(
                id = attrs["id"] ?: newSvgId(),
                name = attrs["id"] ?: "Circle",
                cx = attrs["cx"].toFloatSafe(128f),
                cy = attrs["cy"].toFloatSafe(128f),
                radius = attrs["r"].toFloatSafe(32f),
                style = attrs.toStyle()
            )
        }
        parseTags("polygon", svgText).forEach { attrs ->
            val points = parsePoints(attrs["points"])
            if (points.size < 3) {
                warnings += "Polygon '${attrs["id"] ?: "unknown"}' wurde übersprungen (zu wenige Punkte)."
            } else {
                parsed += SvgPolygonElement(
                    id = attrs["id"] ?: newSvgId(),
                    name = attrs["id"] ?: "Polygon",
                    points = points,
                    style = attrs.toStyle()
                )
            }
        }
        parseTags("path", svgText).forEach { attrs ->
            val d = attrs["d"]
            if (d.isNullOrBlank()) {
                warnings += "Path '${attrs["id"] ?: "unknown"}' ohne d-Attribut wurde übersprungen."
            } else {
                val nodes = parsePathNodes(d)
                if (nodes.size < 2) {
                    warnings += "Path '${attrs["id"] ?: "unknown"}' konnte nur teilweise importiert werden."
                } else {
                    parsed += SvgPathElement(
                        id = attrs["id"] ?: newSvgId(),
                        name = attrs["id"] ?: "Path",
                        closed = d.contains("z", ignoreCase = true),
                        nodes = nodes,
                        style = attrs.toStyle()
                    )
                }
            }
        }

        if (parsed.isEmpty()) {
            warnings += "Keine unterstützten Elemente gefunden (unterstützt: rect, circle, polygon, path)."
        }

        val doc = SvgDocument(width = width, height = height).withElements(parsed)
        return SvgImportResult(document = doc, warnings = warnings)
    }
}

private fun parseTags(tag: String, text: String): List<Map<String, String>> {
    val openClose = Regex("""<$tag\s+([^>/]*?)\s*/?>""", RegexOption.IGNORE_CASE)
    return openClose.findAll(text).map { match ->
        val attributes = match.groupValues[1]
        Regex("""([A-Za-z_:][-A-Za-z0-9_:.]*)\s*=\s*"([^"]*)"""")
            .findAll(attributes)
            .associate { it.groupValues[1] to it.groupValues[2] }
    }.toList()
}

private fun parsePoints(raw: String?): List<Offset> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.trim()
        .split(Regex("""\s+"""))
        .mapNotNull { token ->
            val parts = token.split(",")
            if (parts.size != 2) return@mapNotNull null
            val x = parts[0].toFloatOrNull() ?: return@mapNotNull null
            val y = parts[1].toFloatOrNull() ?: return@mapNotNull null
            Offset(x, y)
        }
}

private fun parsePathNodes(d: String): List<SvgPathNode> {
    val normalized = d
        .replace(",", " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
    if (normalized.isBlank()) return emptyList()
    val tokens = normalized.split(" ")
    val nodes = mutableListOf<SvgPathNode>()
    var mode = "M"
    var i = 0
    while (i < tokens.size) {
        val token = tokens[i]
        if (token.matches(Regex("""[A-Za-z]"""))) {
            mode = token.uppercase()
            i += 1
            continue
        }
        when (mode) {
            "M", "L" -> {
                val x = token.toFloatOrNull()
                val y = tokens.getOrNull(i + 1)?.toFloatOrNull()
                if (x != null && y != null) {
                    nodes += SvgPathNode(
                        anchor = Offset(x, y),
                        type = if (mode == "M" && nodes.isEmpty()) SvgPathSegmentType.Move else SvgPathSegmentType.Line
                    )
                    i += 2
                } else {
                    i += 1
                }
            }
            "C" -> {
                val values = (0..5).map { offset -> tokens.getOrNull(i + offset)?.toFloatOrNull() }
                if (values.all { it != null }) {
                    val out = Offset(values[0]!!, values[1]!!)
                    val inHandle = Offset(values[2]!!, values[3]!!)
                    val anchor = Offset(values[4]!!, values[5]!!)
                    val prev = nodes.lastOrNull()
                    if (prev != null) {
                        nodes[nodes.lastIndex] = prev.copy(outHandle = out)
                    }
                    nodes += SvgPathNode(
                        anchor = anchor,
                        inHandle = inHandle,
                        type = SvgPathSegmentType.Cubic
                    )
                    i += 6
                } else {
                    i += 1
                }
            }
            else -> i += 1
        }
    }
    return nodes
}

private fun Map<String, String>.toStyle(): SvgStyle = SvgStyle(
    fill = parseColor(this["fill"], Color(0xFF6750A4)),
    stroke = parseColor(this["stroke"], Color(0xFF1F1F1F)),
    strokeWidth = this["stroke-width"].toFloatSafe(2f)
)

private fun parseColor(raw: String?, fallback: Color): Color {
    val normalized = raw?.trim().orEmpty()
    if (normalized.isEmpty() || normalized.equals("none", ignoreCase = true)) return Color.Transparent
    return runCatching {
        when {
            normalized.startsWith("#") -> {
                val hex = normalized.removePrefix("#")
                val argb = when (hex.length) {
                    3 -> {
                        val expanded = hex.map { "$it$it" }.joinToString("")
                        (0xFF shl 24) or expanded.toInt(16)
                    }
                    6 -> (0xFF shl 24) or hex.toInt(16)
                    8 -> hex.toLong(16).toInt()
                    else -> return@runCatching fallback
                }
                Color(argb)
            }
            else -> fallback
        }
    }.getOrElse { fallback }
}

private fun toPathData(path: SvgPathElement): String {
    if (path.nodes.isEmpty()) return ""
    val sb = StringBuilder()
    path.nodes.forEachIndexed { index, node ->
        if (index == 0) {
            sb.append("M ${f(node.anchor.x)} ${f(node.anchor.y)} ")
            return@forEachIndexed
        }
        val prev = path.nodes[index - 1]
        val out = prev.outHandle
        val inn = node.inHandle
        if (out != null || inn != null || node.type == SvgPathSegmentType.Cubic) {
            val c1 = out ?: prev.anchor
            val c2 = inn ?: node.anchor
            sb.append(
                "C ${f(c1.x)} ${f(c1.y)} ${f(c2.x)} ${f(c2.y)} ${f(node.anchor.x)} ${f(node.anchor.y)} "
            )
        } else {
            sb.append("L ${f(node.anchor.x)} ${f(node.anchor.y)} ")
        }
    }
    if (path.closed) sb.append("Z")
    return sb.toString().trim()
}

private fun f(value: Float): String {
    val rounded = (value * 100f).roundToInt() / 100f
    return if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
}

private fun hex(color: Color): String {
    val argb = color.toArgb()
    val r = argb shr 16 and 0xFF
    val g = argb shr 8 and 0xFF
    val b = argb and 0xFF
    return "#%02X%02X%02X".format(r, g, b)
}

private fun String?.toFloatSafe(fallback: Float): Float = this?.toFloatOrNull() ?: fallback
