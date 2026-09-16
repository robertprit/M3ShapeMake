package com.compose.canvas.interchange

import androidx.compose.ui.graphics.Color
import com.compose.canvas.core.CanvasSettings
import com.compose.canvas.core.Definitions
import com.compose.canvas.core.FillKind
import com.compose.canvas.core.FillStyle
import com.compose.canvas.core.GradientResource
import com.compose.canvas.core.GradientStop
import com.compose.canvas.core.GroupObject
import com.compose.canvas.core.LinearGradientResource
import com.compose.canvas.core.ObjectTransform
import com.compose.canvas.core.RadialGradientResource
import com.compose.canvas.core.SceneNode
import com.compose.canvas.core.StrokeCap
import com.compose.canvas.core.StrokeJoin
import com.compose.canvas.core.StrokeStyle
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorLayer
import com.compose.canvas.core.VectorObject
import com.compose.canvas.geometry.EllipseGeometry
import com.compose.canvas.geometry.LineGeometry
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.RectF
import com.compose.canvas.geometry.RectGeometry
import com.compose.canvas.geometry.TextAlign
import com.compose.canvas.geometry.TextGeometry
import com.compose.canvas.geometry.newObjectId
import com.compose.canvas.math.TransformMatrix
import com.compose.canvas.math.TransformUtils
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

object SvgImport : SvgImporter {
    private val unsupportedTags = setOf(
        "filter", "mask", "clipPath", "clippath", "pattern", "image", "use",
        "marker", "symbol", "foreignObject", "style", "script", "animate",
        "animateTransform", "switch", "defs"
    )

    override fun import(svg: String): ImportResult {
        val diagnostics = mutableListOf<ImportDiagnostic>()
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val parsed = try {
            factory.newDocumentBuilder().parse(ByteArrayInputStream(svg.toByteArray(Charsets.UTF_8)))
        } catch (e: Exception) {
            diagnostics += ImportDiagnostic(DiagnosticLevel.ERROR, "PARSE_ERROR", e.message ?: "Invalid SVG")
            return ImportResult(null, diagnostics)
        }
        val root = parsed.documentElement
        if (root == null || !root.tagName.substringAfter(':').equals("svg", ignoreCase = true)) {
            diagnostics += ImportDiagnostic(DiagnosticLevel.ERROR, "NOT_SVG", "Root element is not <svg>")
            return ImportResult(null, diagnostics)
        }

        val viewBox = parseViewBox(root.getAttribute("viewBox"))
        val width = parseLength(root.getAttribute("width"), viewBox?.width ?: 1000f)
        val height = parseLength(root.getAttribute("height"), viewBox?.height ?: 1000f)
        val canvas = CanvasSettings(
            width = width,
            height = height,
            viewBox = viewBox,
            backgroundTransparent = true
        )

        val gradients = mutableListOf<GradientResource>()
        collectGradients(root, gradients, diagnostics)

        val objects = mutableListOf<SceneNode>()
        parseChildren(root, objects, StyleContext(), TransformMatrix(), gradients, diagnostics)

        val doc = VectorDocument(
            name = root.getAttribute("id").ifBlank { "Imported" },
            canvas = canvas,
            layers = listOf(VectorLayer(name = "SVG", objects = objects)),
            definitions = Definitions(gradients = gradients)
        )
        return ImportResult(doc, diagnostics)
    }

    private fun collectGradients(
        element: Element,
        out: MutableList<GradientResource>,
        diagnostics: MutableList<ImportDiagnostic>
    ) {
        val children = element.childNodes
        for (i in 0 until children.length) {
            val n = children.item(i)
            if (n !is Element) continue
            val tag = n.localName ?: n.tagName.substringAfter(':')
            when (tag.lowercase()) {
                "defs" -> collectGradients(n, out, diagnostics)
                "lineargradient" -> out += parseLinear(n)
                "radialgradient" -> out += parseRadial(n)
                else -> if (n.hasChildNodes()) collectGradients(n, out, diagnostics)
            }
        }
    }

    private fun parseLinear(el: Element): LinearGradientResource {
        val id = el.getAttribute("id").ifBlank { newObjectId() }
        return LinearGradientResource(
            id = id,
            name = id,
            x1 = parseFloat(el.getAttribute("x1"), 0f),
            y1 = parseFloat(el.getAttribute("y1"), 0f),
            x2 = parseFloat(el.getAttribute("x2"), 1f),
            y2 = parseFloat(el.getAttribute("y2"), 0f),
            stops = parseStops(el)
        )
    }

    private fun parseRadial(el: Element): RadialGradientResource {
        val id = el.getAttribute("id").ifBlank { newObjectId() }
        return RadialGradientResource(
            id = id,
            name = id,
            cx = parseFloat(el.getAttribute("cx"), 0.5f),
            cy = parseFloat(el.getAttribute("cy"), 0.5f),
            r = parseFloat(el.getAttribute("r"), 0.5f),
            stops = parseStops(el)
        )
    }

    private fun parseStops(el: Element): List<GradientStop> {
        val stops = mutableListOf<GradientStop>()
        val children = el.childNodes
        for (i in 0 until children.length) {
            val n = children.item(i)
            if (n !is Element) continue
            val tag = (n.localName ?: n.tagName).lowercase()
            if (tag != "stop") continue
            val offsetRaw = n.getAttribute("offset").ifBlank { "0" }
            val offset = if (offsetRaw.endsWith("%")) offsetRaw.dropLast(1).toFloatOrNull()?.div(100f) ?: 0f
            else offsetRaw.toFloatOrNull() ?: 0f
            val color = parsePaint(n.getAttribute("stop-color").ifBlank { "#000000" }).color ?: Color.Black
            val alpha = parseFloat(n.getAttribute("stop-opacity"), 1f)
            stops += GradientStop(offset, color, alpha)
        }
        return stops.ifEmpty {
            listOf(GradientStop(0f, Color.Black), GradientStop(1f, Color.White))
        }
    }

    private fun parseChildren(
        parent: Element,
        out: MutableList<SceneNode>,
        style: StyleContext,
        transform: TransformMatrix,
        gradients: List<GradientResource>,
        diagnostics: MutableList<ImportDiagnostic>
    ) {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val n = children.item(i)
            if (n.nodeType != Node.ELEMENT_NODE) continue
            val el = n as Element
            parseElement(el, out, style, transform, gradients, diagnostics)
        }
    }

    private fun parseElement(
        el: Element,
        out: MutableList<SceneNode>,
        parentStyle: StyleContext,
        parentTransform: TransformMatrix,
        gradients: List<GradientResource>,
        diagnostics: MutableList<ImportDiagnostic>
    ) {
        val tag = (el.localName ?: el.tagName.substringAfter(':')).lowercase()
        if (tag == "defs" || tag == "title" || tag == "desc" || tag == "metadata") return
        if (tag in unsupportedTags) {
            diagnostics += ImportDiagnostic(
                DiagnosticLevel.UNSUPPORTED,
                "UNSUPPORTED_${tag.uppercase()}",
                "<$tag> is not imported"
            )
            return
        }
        val style = parentStyle.merge(el)
        val local = parseTransform(el.getAttribute("transform"), diagnostics)
        val world = TransformUtils.multiply(parentTransform, local)
        val objTransform = TransformUtils.decomposeObjectTransform(world)

        when (tag) {
            "svg" -> parseChildren(el, out, style, world, gradients, diagnostics)
            "g" -> {
                val children = mutableListOf<SceneNode>()
                parseChildren(el, children, style, TransformMatrix(), gradients, diagnostics)
                if (children.size == 1 && objTransform == ObjectTransform()) {
                    out += children.first()
                } else if (children.isNotEmpty()) {
                    out += GroupObject(
                        name = el.getAttribute("id").ifBlank { "Group" },
                        transform = objTransform,
                        children = children
                    )
                }
            }
            "rect" -> {
                val x = parseFloat(el.getAttribute("x"), 0f)
                val y = parseFloat(el.getAttribute("y"), 0f)
                val w = parseFloat(el.getAttribute("width"), 0f)
                val h = parseFloat(el.getAttribute("height"), 0f)
                val rx = parseFloat(el.getAttribute("rx"), parseFloat(el.getAttribute("ry"), 0f))
                out += styledObject(
                    name = el.getAttribute("id").ifBlank { "Rect" },
                    geometry = RectGeometry(w, h, rx),
                    transform = objTransform.copy(x = objTransform.x + x, y = objTransform.y + y),
                    style = style,
                    gradients = gradients
                )
            }
            "circle" -> {
                val cx = parseFloat(el.getAttribute("cx"), 0f)
                val cy = parseFloat(el.getAttribute("cy"), 0f)
                val r = parseFloat(el.getAttribute("r"), 0f)
                out += styledObject(
                    name = el.getAttribute("id").ifBlank { "Circle" },
                    geometry = EllipseGeometry(r, r),
                    transform = objTransform.copy(x = objTransform.x + cx, y = objTransform.y + cy),
                    style = style,
                    gradients = gradients
                )
            }
            "ellipse" -> {
                val cx = parseFloat(el.getAttribute("cx"), 0f)
                val cy = parseFloat(el.getAttribute("cy"), 0f)
                val rx = parseFloat(el.getAttribute("rx"), 0f)
                val ry = parseFloat(el.getAttribute("ry"), 0f)
                out += styledObject(
                    name = el.getAttribute("id").ifBlank { "Ellipse" },
                    geometry = EllipseGeometry(rx, ry),
                    transform = objTransform.copy(x = objTransform.x + cx, y = objTransform.y + cy),
                    style = style,
                    gradients = gradients
                )
            }
            "line" -> {
                out += styledObject(
                    name = el.getAttribute("id").ifBlank { "Line" },
                    geometry = LineGeometry(
                        parseFloat(el.getAttribute("x1"), 0f),
                        parseFloat(el.getAttribute("y1"), 0f),
                        parseFloat(el.getAttribute("x2"), 0f),
                        parseFloat(el.getAttribute("y2"), 0f)
                    ),
                    transform = objTransform,
                    style = style.copy(fill = "none"),
                    gradients = gradients
                )
            }
            "polygon", "polyline" -> {
                val pts = parsePoints(el.getAttribute("points"))
                if (pts.size < 2) {
                    diagnostics += ImportDiagnostic(DiagnosticLevel.WARNING, "INVALID_POINTS", "Empty $tag")
                    return
                }
                val closed = tag == "polygon"
                val segs = buildList {
                    add(com.compose.canvas.geometry.PathSegment.MoveTo(pts[0].first, pts[0].second))
                    pts.drop(1).forEach { add(com.compose.canvas.geometry.PathSegment.LineTo(it.first, it.second)) }
                    if (closed) add(com.compose.canvas.geometry.PathSegment.Close)
                }
                out += styledObject(
                    name = el.getAttribute("id").ifBlank { tag.replaceFirstChar { it.uppercase() } },
                    geometry = PathGeometry(segs, closed),
                    transform = objTransform,
                    style = style,
                    gradients = gradients
                )
            }
            "path" -> {
                val d = el.getAttribute("d")
                if (d.isBlank()) {
                    diagnostics += ImportDiagnostic(DiagnosticLevel.WARNING, "INVALID_PATH", "Empty path data")
                    return
                }
                val geom = try {
                    SvgPathParser.parse(d)
                } catch (e: Exception) {
                    diagnostics += ImportDiagnostic(DiagnosticLevel.ERROR, "INVALID_PATH", e.message ?: "Bad path")
                    return
                }
                out += styledObject(
                    name = el.getAttribute("id").ifBlank { "Path" },
                    geometry = geom,
                    transform = objTransform,
                    style = style,
                    gradients = gradients
                )
            }
            "text" -> {
                val x = parseFloat(el.getAttribute("x"), 0f)
                val y = parseFloat(el.getAttribute("y"), 0f)
                val size = parseFloat(el.getAttribute("font-size"), 16f)
                val family = el.getAttribute("font-family").ifBlank { "sans-serif" }
                val weight = el.getAttribute("font-weight").toIntOrNull() ?: 400
                val text = el.textContent?.trim().orEmpty()
                out += styledObject(
                    name = el.getAttribute("id").ifBlank { "Text" },
                    geometry = TextGeometry(
                        text = text,
                        fontFamily = family,
                        fontSize = size,
                        fontWeight = weight,
                        alignment = TextAlign.Left
                    ),
                    transform = objTransform.copy(x = objTransform.x + x, y = objTransform.y + y - size),
                    style = style,
                    gradients = gradients
                )
            }
            else -> diagnostics += ImportDiagnostic(
                DiagnosticLevel.UNSUPPORTED,
                "UNSUPPORTED_ELEMENT",
                "<$tag> is not imported"
            )
        }
    }

    private fun styledObject(
        name: String,
        geometry: com.compose.canvas.geometry.Geometry,
        transform: ObjectTransform,
        style: StyleContext,
        gradients: List<GradientResource>
    ): VectorObject {
        val fillPaint = parsePaint(style.fill)
        val strokePaint = parsePaint(style.stroke)
        val fill = when {
            style.fill == "none" || fillPaint.none -> FillStyle(kind = FillKind.None)
            fillPaint.gradientId != null -> FillStyle(
                kind = if (gradients.find { it.id == fillPaint.gradientId } is RadialGradientResource)
                    FillKind.RadialGradient else FillKind.LinearGradient,
                gradientId = fillPaint.gradientId,
                alpha = style.opacity * style.fillOpacity
            )
            else -> FillStyle(
                kind = FillKind.Solid,
                color = fillPaint.color ?: Color.Unspecified,
                alpha = style.opacity * style.fillOpacity
            )
        }
        val stroke = StrokeStyle(
            color = if (style.stroke == "none") Color.Unspecified else (strokePaint.color ?: Color.Black),
            width = if (style.stroke == "none") 0f else style.strokeWidth,
            alpha = style.opacity * style.strokeOpacity,
            cap = style.cap,
            join = style.join,
            dash = style.dash
        )
        return VectorObject(name = name, geometry = geometry, transform = transform, fill = fill, stroke = stroke)
    }

    private data class StyleContext(
        val fill: String = "#000000",
        val stroke: String = "none",
        val strokeWidth: Float = 1f,
        val opacity: Float = 1f,
        val fillOpacity: Float = 1f,
        val strokeOpacity: Float = 1f,
        val cap: StrokeCap = StrokeCap.Butt,
        val join: StrokeJoin = StrokeJoin.Miter,
        val dash: List<Float> = emptyList()
    ) {
        fun merge(el: Element): StyleContext {
            val attrs = presentation(el)
            return copy(
                fill = attrs["fill"] ?: fill,
                stroke = attrs["stroke"] ?: stroke,
                strokeWidth = attrs["stroke-width"]?.let { parseFloat(it, strokeWidth) } ?: strokeWidth,
                opacity = attrs["opacity"]?.let { parseFloat(it, opacity) } ?: opacity,
                fillOpacity = attrs["fill-opacity"]?.let { parseFloat(it, fillOpacity) } ?: fillOpacity,
                strokeOpacity = attrs["stroke-opacity"]?.let { parseFloat(it, strokeOpacity) } ?: strokeOpacity,
                cap = attrs["stroke-linecap"]?.let {
                    when (it.lowercase()) {
                        "round" -> StrokeCap.Round
                        "square" -> StrokeCap.Square
                        else -> StrokeCap.Butt
                    }
                } ?: cap,
                join = attrs["stroke-linejoin"]?.let {
                    when (it.lowercase()) {
                        "round" -> StrokeJoin.Round
                        "bevel" -> StrokeJoin.Bevel
                        else -> StrokeJoin.Miter
                    }
                } ?: join,
                dash = attrs["stroke-dasharray"]?.let { parseDash(it) } ?: dash
            )
        }
    }

    private fun presentation(el: Element): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val style = el.getAttribute("style")
        if (style.isNotBlank()) {
            style.split(';').forEach { part ->
                val idx = part.indexOf(':')
                if (idx > 0) map[part.substring(0, idx).trim()] = part.substring(idx + 1).trim()
            }
        }
        listOf(
            "fill", "stroke", "stroke-width", "opacity", "fill-opacity", "stroke-opacity",
            "stroke-linecap", "stroke-linejoin", "stroke-dasharray", "font-size", "font-family", "font-weight"
        ).forEach { key ->
            val v = el.getAttribute(key)
            if (v.isNotBlank()) map[key] = v
        }
        return map
    }

    private data class Paint(val none: Boolean = false, val color: Color? = null, val gradientId: String? = null)

    private fun parsePaint(raw: String): Paint {
        val v = raw.trim()
        if (v.isEmpty() || v.equals("none", true)) return Paint(none = true)
        if (v.startsWith("url(")) {
            val id = v.removePrefix("url(").removeSuffix(")").trim().removePrefix("#").trim('\'', '"')
            return Paint(gradientId = id)
        }
        return Paint(color = parseColor(v))
    }

    private fun parseColor(raw: String): Color? {
        val v = raw.trim()
        return try {
            when {
                v.startsWith("#") && v.length == 4 -> {
                    val r = v[1].digitToInt(16) * 17
                    val g = v[2].digitToInt(16) * 17
                    val b = v[3].digitToInt(16) * 17
                    Color(r / 255f, g / 255f, b / 255f)
                }
                v.startsWith("#") && v.length >= 7 -> {
                    val r = v.substring(1, 3).toInt(16)
                    val g = v.substring(3, 5).toInt(16)
                    val b = v.substring(5, 7).toInt(16)
                    Color(r / 255f, g / 255f, b / 255f)
                }
                v.startsWith("rgb") -> {
                    val nums = Regex("""[\d.]+""").findAll(v).map { it.value.toFloat() }.toList()
                    if (nums.size >= 3) Color(nums[0] / 255f, nums[1] / 255f, nums[2] / 255f, nums.getOrNull(3) ?: 1f)
                    else Color.Black
                }
                else -> namedColor(v)
            }
        } catch (_: Exception) {
            Color.Black
        }
    }

    private fun namedColor(name: String): Color? = when (name.lowercase()) {
        "black" -> Color.Black
        "white" -> Color.White
        "red" -> Color.Red
        "green" -> Color(0xFF008000)
        "blue" -> Color.Blue
        "none" -> null
        else -> Color.Black
    }

    private fun parseTransform(raw: String, diagnostics: MutableList<ImportDiagnostic>): TransformMatrix {
        if (raw.isBlank()) return TransformMatrix()
        var m = TransformMatrix()
        val regex = Regex("""([a-zA-Z]+)\s*\(([^)]*)\)""")
        for (match in regex.findAll(raw)) {
            val name = match.groupValues[1].lowercase()
            val nums = match.groupValues[2].split(',', ' ').mapNotNull { it.trim().toFloatOrNull() }
            m = when (name) {
                "translate" -> TransformUtils.multiply(m, TransformUtils.translation(nums.getOrElse(0) { 0f }, nums.getOrElse(1) { 0f }))
                "scale" -> TransformUtils.multiply(m, TransformUtils.scale(nums.getOrElse(0) { 1f }, nums.getOrElse(1) { nums.getOrElse(0) { 1f } }))
                "rotate" -> {
                    val a = nums.getOrElse(0) { 0f }
                    val cx = nums.getOrElse(1) { 0f }
                    val cy = nums.getOrElse(2) { 0f }
                    var r = TransformUtils.translation(cx, cy)
                    r = TransformUtils.multiply(r, TransformUtils.rotation(a))
                    r = TransformUtils.multiply(r, TransformUtils.translation(-cx, -cy))
                    TransformUtils.multiply(m, r)
                }
                "matrix" -> if (nums.size >= 6) {
                    TransformUtils.multiply(
                        m,
                        TransformMatrix(
                            m00 = nums[0], m01 = nums[2], m02 = nums[4],
                            m10 = nums[1], m11 = nums[3], m12 = nums[5]
                        )
                    )
                } else m
                else -> {
                    diagnostics += ImportDiagnostic(DiagnosticLevel.WARNING, "UNKNOWN_TRANSFORM", name)
                    m
                }
            }
        }
        return m
    }

    private fun parseViewBox(raw: String): RectF? {
        val nums = raw.split(',', ' ').mapNotNull { it.trim().toFloatOrNull() }
        if (nums.size < 4) return null
        return RectF(nums[0], nums[1], nums[0] + nums[2], nums[1] + nums[3])
    }

    private fun parseLength(raw: String, fallback: Float): Float {
        if (raw.isBlank()) return fallback
        return raw.replace("px", "").replace("pt", "").trim().toFloatOrNull() ?: fallback
    }

    private fun parseFloat(raw: String, fallback: Float): Float =
        raw.replace("px", "").trim().toFloatOrNull() ?: fallback

    private fun parsePoints(raw: String): List<Pair<Float, Float>> {
        val nums = raw.split(',', ' ').mapNotNull { it.trim().toFloatOrNull() }
        val out = mutableListOf<Pair<Float, Float>>()
        var i = 0
        while (i + 1 < nums.size) {
            out += nums[i] to nums[i + 1]
            i += 2
        }
        return out
    }

    private fun parseDash(raw: String): List<Float> =
        if (raw.equals("none", true)) emptyList()
        else raw.split(',', ' ').mapNotNull { it.trim().toFloatOrNull() }
}
