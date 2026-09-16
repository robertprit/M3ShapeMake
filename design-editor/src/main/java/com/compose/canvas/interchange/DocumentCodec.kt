package com.compose.canvas.interchange

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.compose.canvas.core.CanvasSettings
import com.compose.canvas.core.Definitions
import com.compose.canvas.core.DocumentSettings
import com.compose.canvas.core.FillKind
import com.compose.canvas.core.FillStyle
import com.compose.canvas.core.GradientStop
import com.compose.canvas.core.GroupObject
import com.compose.canvas.core.LinearGradientResource
import com.compose.canvas.core.ObjectSemantics
import com.compose.canvas.core.ObjectTransform
import com.compose.canvas.core.RadialGradientResource
import com.compose.canvas.core.SceneNode
import com.compose.canvas.core.ShapeMetadata
import com.compose.canvas.core.StrokeCap
import com.compose.canvas.core.StrokeJoin
import com.compose.canvas.core.StrokeStyle
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorLayer
import com.compose.canvas.core.VectorObject
import com.compose.canvas.geometry.ArcGeometry
import com.compose.canvas.geometry.EllipseGeometry
import com.compose.canvas.geometry.Geometry
import com.compose.canvas.geometry.LineGeometry
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import com.compose.canvas.geometry.PolygonGeometry
import com.compose.canvas.geometry.PresetGeometry
import com.compose.canvas.geometry.RectF
import com.compose.canvas.geometry.RectGeometry
import com.compose.canvas.geometry.StarGeometry
import com.compose.canvas.geometry.TextAlign
import com.compose.canvas.geometry.TextGeometry
import com.compose.canvas.geometry.TriangleGeometry
import com.compose.canvas.geometry.newObjectId
import com.compose.canvas.metadata.AnchorKind
import com.compose.canvas.metadata.ContentArea
import com.compose.canvas.metadata.NamedAnchor
import com.compose.canvas.metadata.Port
import com.compose.canvas.metadata.PortType
import com.compose.canvas.metadata.ResizeMode
import com.compose.canvas.metadata.ResizeRules
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loss-free `.vshape` codec. SVG is never the project format.
 * Versioned via [FORMAT] + `documentFormatVersion`.
 */
object DocumentCodec {
    const val FORMAT = "compose-canvas-document"
    const val CURRENT_VERSION = 1

    fun serialize(document: VectorDocument): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("documentFormatVersion", document.documentFormatVersion)
        root.put("formatVersion", document.documentFormatVersion)
        root.put("name", document.name)
        root.put("canvas", JSONObject().apply {
            put("width", document.canvas.width.toDouble())
            put("height", document.canvas.height.toDouble())
            put("backgroundTransparent", document.canvas.backgroundTransparent)
            document.canvas.viewBox?.let { box ->
                put("viewBox", JSONObject().apply {
                    put("left", box.left.toDouble())
                    put("top", box.top.toDouble())
                    put("right", box.right.toDouble())
                    put("bottom", box.bottom.toDouble())
                })
            }
        })
        root.put("settings", JSONObject().apply {
            put("gridSize", document.settings.gridSize.toDouble())
            put("snapEnabled", document.settings.snapEnabled)
        })
        root.put("definitions", serializeDefinitions(document.definitions))
        root.put("metadata", serializeMetadata(document.metadata))
        root.put("layers", JSONArray().apply {
            document.layers.forEach { put(serializeLayer(it)) }
        })
        return root.toString()
    }

    fun deserialize(json: String): VectorDocument {
        val root = JSONObject(json)
        val format = root.optString("format")
        if (format != FORMAT) {
            throw IllegalArgumentException("Unsupported document format: $format")
        }
        val version = root.optInt(
            "documentFormatVersion",
            root.optInt("formatVersion", -1)
        )
        if (version > CURRENT_VERSION) {
            throw IllegalArgumentException("Document version $version is newer than supported $CURRENT_VERSION")
        }
        val canvasJson = root.optJSONObject("canvas") ?: JSONObject()
        val viewBoxJson = canvasJson.optJSONObject("viewBox")
        val canvas = CanvasSettings(
            width = canvasJson.optDouble("width", 1000.0).toFloat(),
            height = canvasJson.optDouble("height", 1000.0).toFloat(),
            backgroundTransparent = canvasJson.optBoolean("backgroundTransparent", true),
            viewBox = viewBoxJson?.let {
                RectF(
                    it.optDouble("left").toFloat(),
                    it.optDouble("top").toFloat(),
                    it.optDouble("right").toFloat(),
                    it.optDouble("bottom").toFloat()
                )
            }
        )
        val settingsJson = root.optJSONObject("settings") ?: JSONObject()
        val settings = DocumentSettings(
            gridSize = settingsJson.optDouble("gridSize", 50.0).toFloat(),
            snapEnabled = settingsJson.optBoolean("snapEnabled", false)
        )
        val layers = mutableListOf<VectorLayer>()
        val layersJson = root.optJSONArray("layers") ?: JSONArray()
        for (i in 0 until layersJson.length()) {
            layers.add(deserializeLayer(layersJson.getJSONObject(i)))
        }
        if (layers.isEmpty()) layers.add(VectorLayer(name = "Layer 1"))
        return VectorDocument(
            name = root.optString("name", "Untitled"),
            canvas = canvas,
            layers = layers,
            definitions = deserializeDefinitions(root.optJSONObject("definitions") ?: JSONObject()),
            metadata = deserializeMetadata(root.optJSONObject("metadata") ?: JSONObject()),
            settings = settings,
            documentFormatVersion = version.takeIf { it > 0 } ?: CURRENT_VERSION
        )
    }

    private fun serializeLayer(layer: VectorLayer): JSONObject = JSONObject().apply {
        put("id", layer.id)
        put("name", layer.name)
        put("visible", layer.visible)
        put("locked", layer.locked)
        put("objects", JSONArray().apply {
            layer.objects.forEach { put(serializeNode(it)) }
        })
    }

    private fun deserializeLayer(json: JSONObject): VectorLayer {
        val objects = mutableListOf<SceneNode>()
        val objectsJson = json.optJSONArray("objects") ?: JSONArray()
        for (i in 0 until objectsJson.length()) {
            objects.add(deserializeNode(objectsJson.getJSONObject(i)))
        }
        return VectorLayer(
            id = json.optString("id", newObjectId()),
            name = json.optString("name", "Layer"),
            visible = json.optBoolean("visible", true),
            locked = json.optBoolean("locked", false),
            objects = objects
        )
    }

    private fun serializeNode(node: SceneNode): JSONObject = when (node) {
        is VectorObject -> serializeObject(node)
        is GroupObject -> JSONObject().apply {
            put("type", "group")
            put("id", node.id)
            put("name", node.name)
            put("visible", node.visible)
            put("locked", node.locked)
            put("transform", serializeTransform(node.transform))
            put("children", JSONArray().apply { node.children.forEach { put(serializeNode(it)) } })
        }
    }

    private fun deserializeNode(json: JSONObject): SceneNode {
        val type = json.optString("type", "object")
        if (type == "group") {
            val children = mutableListOf<SceneNode>()
            val arr = json.optJSONArray("children") ?: JSONArray()
            for (i in 0 until arr.length()) children.add(deserializeNode(arr.getJSONObject(i)))
            return GroupObject(
                id = json.optString("id", newObjectId()),
                name = json.optString("name", "Group"),
                visible = json.optBoolean("visible", true),
                locked = json.optBoolean("locked", false),
                transform = json.optJSONObject("transform")?.let { deserializeTransform(it) } ?: ObjectTransform(),
                children = children
            )
        }
        return deserializeObject(json)
    }

    private fun serializeObject(obj: VectorObject): JSONObject = JSONObject().apply {
        put("type", "object")
        put("id", obj.id)
        put("name", obj.name)
        put("visible", obj.visible)
        put("locked", obj.locked)
        put("geometry", serializeGeometry(obj.geometry))
        put("transform", serializeTransform(obj.transform))
        put("fill", JSONObject().apply {
            put("kind", obj.fill.kind.name)
            putColor(this, obj.fill.color)
            put("alpha", obj.fill.alpha.toDouble())
            obj.fill.gradientId?.let { put("gradientId", it) }
        })
        put("stroke", serializeStroke(obj.stroke))
        put("semantics", serializeSemantics(obj.semantics))
    }

    private fun deserializeObject(json: JSONObject): VectorObject = VectorObject(
        id = json.optString("id", newObjectId()),
        name = json.optString("name", "Object"),
        visible = json.optBoolean("visible", true),
        locked = json.optBoolean("locked", false),
        geometry = deserializeGeometry(json.getJSONObject("geometry")),
        transform = json.optJSONObject("transform")?.let { deserializeTransform(it) } ?: ObjectTransform(),
        fill = json.optJSONObject("fill")?.let {
            FillStyle(
                kind = runCatching { enumValueOf<FillKind>(it.optString("kind", "Solid")) }.getOrDefault(FillKind.Solid),
                color = getColor(it),
                alpha = it.optDouble("alpha", 1.0).toFloat(),
                gradientId = it.optString("gradientId").ifBlank { null }
            )
        } ?: FillStyle(),
        stroke = json.optJSONObject("stroke")?.let { deserializeStroke(it) } ?: StrokeStyle(),
        semantics = json.optJSONObject("semantics")?.let { deserializeSemantics(it) } ?: ObjectSemantics()
    )

    private fun serializeTransform(transform: ObjectTransform): JSONObject = JSONObject().apply {
        put("x", transform.x.toDouble())
        put("y", transform.y.toDouble())
        put("rotation", transform.rotation.toDouble())
        put("scaleX", transform.scaleX.toDouble())
        put("scaleY", transform.scaleY.toDouble())
    }

    private fun deserializeTransform(json: JSONObject): ObjectTransform = ObjectTransform(
        x = json.optDouble("x", 0.0).toFloat(),
        y = json.optDouble("y", 0.0).toFloat(),
        rotation = json.optDouble("rotation", 0.0).toFloat(),
        scaleX = json.optDouble("scaleX", 1.0).toFloat(),
        scaleY = json.optDouble("scaleY", 1.0).toFloat()
    )

    private fun serializeGeometry(geometry: Geometry): JSONObject {
        val json = JSONObject()
        when (geometry) {
            is RectGeometry -> {
                json.put("type", "rect")
                json.put("width", geometry.width.toDouble())
                json.put("height", geometry.height.toDouble())
                json.put("cornerRadius", geometry.cornerRadius.toDouble())
            }
            is EllipseGeometry -> {
                json.put("type", "ellipse")
                json.put("radiusX", geometry.radiusX.toDouble())
                json.put("radiusY", geometry.radiusY.toDouble())
            }
            is LineGeometry -> {
                json.put("type", "line")
                json.put("startX", geometry.startX.toDouble())
                json.put("startY", geometry.startY.toDouble())
                json.put("endX", geometry.endX.toDouble())
                json.put("endY", geometry.endY.toDouble())
            }
            is PathGeometry -> {
                json.put("type", "path")
                json.put("closed", geometry.closed)
                json.put("segments", JSONArray().apply {
                    geometry.segments.forEach { put(serializeSegment(it)) }
                })
            }
            is TriangleGeometry -> {
                json.put("type", "triangle")
                json.put("width", geometry.width.toDouble())
                json.put("height", geometry.height.toDouble())
            }
            is PolygonGeometry -> {
                json.put("type", "polygon")
                json.put("sides", geometry.sides)
                json.put("radius", geometry.radius.toDouble())
            }
            is StarGeometry -> {
                json.put("type", "star")
                json.put("points", geometry.points)
                json.put("outerRadius", geometry.outerRadius.toDouble())
                json.put("innerRadius", geometry.innerRadius.toDouble())
            }
            is ArcGeometry -> {
                json.put("type", "arc")
                json.put("radiusX", geometry.radiusX.toDouble())
                json.put("radiusY", geometry.radiusY.toDouble())
                json.put("startDegrees", geometry.startDegrees.toDouble())
                json.put("sweepDegrees", geometry.sweepDegrees.toDouble())
            }
            is TextGeometry -> {
                json.put("type", "text")
                json.put("text", geometry.text)
                json.put("fontFamily", geometry.fontFamily)
                json.put("fontSize", geometry.fontSize.toDouble())
                json.put("fontWeight", geometry.fontWeight)
                json.put("alignment", geometry.alignment.name)
                json.put("letterSpacing", geometry.letterSpacing.toDouble())
                json.put("lineHeight", geometry.lineHeight.toDouble())
            }
            is PresetGeometry -> {
                json.put("type", "preset")
                json.put("presetId", geometry.presetId)
                json.put("width", geometry.width.toDouble())
                json.put("height", geometry.height.toDouble())
                json.put("parameters", JSONObject().apply {
                    geometry.parameters.forEach { (k, v) -> put(k, v.toDouble()) }
                })
                geometry.definition?.let { definition ->
                    json.put("definition", serializeGeometry(definition))
                }
            }
        }
        return json
    }

    private fun deserializeGeometry(json: JSONObject): Geometry = when (json.getString("type")) {
        "rect" -> RectGeometry(
            width = json.getDouble("width").toFloat(),
            height = json.getDouble("height").toFloat(),
            cornerRadius = json.optDouble("cornerRadius", 0.0).toFloat()
        )
        "ellipse" -> EllipseGeometry(
            radiusX = json.getDouble("radiusX").toFloat(),
            radiusY = json.getDouble("radiusY").toFloat()
        )
        "line" -> LineGeometry(
            startX = json.optDouble("startX", 0.0).toFloat(),
            startY = json.optDouble("startY", 0.0).toFloat(),
            endX = json.getDouble("endX").toFloat(),
            endY = json.getDouble("endY").toFloat()
        )
        "path" -> {
            val segments = mutableListOf<PathSegment>()
            val segs = json.getJSONArray("segments")
            for (i in 0 until segs.length()) {
                segments.add(deserializeSegment(segs.getJSONObject(i)))
            }
            PathGeometry(segments = segments, closed = json.optBoolean("closed", false))
        }
        "triangle" -> TriangleGeometry(
            json.getDouble("width").toFloat(),
            json.getDouble("height").toFloat()
        )
        "polygon" -> PolygonGeometry(
            json.optInt("sides", 6),
            json.getDouble("radius").toFloat()
        )
        "star" -> StarGeometry(
            json.optInt("points", 5),
            json.getDouble("outerRadius").toFloat(),
            json.getDouble("innerRadius").toFloat()
        )
        "arc" -> ArcGeometry(
            json.getDouble("radiusX").toFloat(),
            json.getDouble("radiusY").toFloat(),
            json.optDouble("startDegrees").toFloat(),
            json.optDouble("sweepDegrees", 90.0).toFloat()
        )
        "text" -> TextGeometry(
            text = json.optString("text"),
            fontFamily = json.optString("fontFamily", "sans-serif"),
            fontSize = json.optDouble("fontSize", 16.0).toFloat(),
            fontWeight = json.optInt("fontWeight", 400),
            alignment = runCatching { enumValueOf<TextAlign>(json.optString("alignment", "Left")) }
                .getOrDefault(TextAlign.Left),
            letterSpacing = json.optDouble("letterSpacing").toFloat(),
            lineHeight = json.optDouble("lineHeight", 1.2).toFloat()
        )
        "preset" -> {
            val params = mutableMapOf<String, Float>()
            json.optJSONObject("parameters")?.let { nested ->
                val nk = nested.keys()
                while (nk.hasNext()) {
                    val k = nk.next()
                    if (nested.opt(k) is Number) params[k] = nested.getDouble(k).toFloat()
                }
            }
            PresetGeometry(
                json.optString("presetId"),
                json.getDouble("width").toFloat(),
                json.getDouble("height").toFloat(),
                params,
                json.optJSONObject("definition")?.let { deserializeGeometry(it) as? PathGeometry }
            )
        }
        else -> throw IllegalArgumentException("Unknown geometry type: ${json.getString("type")}")
    }

    private fun serializeSegment(segment: PathSegment): JSONObject {
        val json = JSONObject()
        when (segment) {
            is PathSegment.MoveTo -> {
                json.put("op", "M"); json.put("x", segment.x.toDouble()); json.put("y", segment.y.toDouble())
            }
            is PathSegment.LineTo -> {
                json.put("op", "L"); json.put("x", segment.x.toDouble()); json.put("y", segment.y.toDouble())
            }
            is PathSegment.QuadraticBezierTo -> {
                json.put("op", "Q")
                json.put("cx", segment.cx.toDouble()); json.put("cy", segment.cy.toDouble())
                json.put("x", segment.x.toDouble()); json.put("y", segment.y.toDouble())
            }
            is PathSegment.CubicBezierTo -> {
                json.put("op", "C")
                json.put("cx1", segment.cx1.toDouble()); json.put("cy1", segment.cy1.toDouble())
                json.put("cx2", segment.cx2.toDouble()); json.put("cy2", segment.cy2.toDouble())
                json.put("x", segment.x.toDouble()); json.put("y", segment.y.toDouble())
            }
            is PathSegment.Close -> json.put("op", "Z")
        }
        return json
    }

    private fun deserializeSegment(json: JSONObject): PathSegment = when (json.getString("op")) {
        "M" -> PathSegment.MoveTo(json.getDouble("x").toFloat(), json.getDouble("y").toFloat())
        "L" -> PathSegment.LineTo(json.getDouble("x").toFloat(), json.getDouble("y").toFloat())
        "Q" -> PathSegment.QuadraticBezierTo(
            json.getDouble("cx").toFloat(), json.getDouble("cy").toFloat(),
            json.getDouble("x").toFloat(), json.getDouble("y").toFloat()
        )
        "C" -> PathSegment.CubicBezierTo(
            json.getDouble("cx1").toFloat(), json.getDouble("cy1").toFloat(),
            json.getDouble("cx2").toFloat(), json.getDouble("cy2").toFloat(),
            json.getDouble("x").toFloat(), json.getDouble("y").toFloat()
        )
        "Z" -> PathSegment.Close
        else -> throw IllegalArgumentException("Unknown segment op: ${json.getString("op")}")
    }

    private fun serializeStroke(stroke: StrokeStyle): JSONObject = JSONObject().apply {
        putColor(this, stroke.color)
        put("alpha", stroke.alpha.toDouble())
        put("width", stroke.width.toDouble())
        put("cap", stroke.cap.name)
        put("join", stroke.join.name)
        if (stroke.dash.isNotEmpty()) {
            put("dash", JSONArray().apply { stroke.dash.forEach { put(it.toDouble()) } })
        }
    }

    private fun deserializeStroke(json: JSONObject): StrokeStyle = StrokeStyle(
        color = getColor(json),
        alpha = json.optDouble("alpha", 1.0).toFloat(),
        width = json.optDouble("width", 1.0).toFloat(),
        cap = runCatching { enumValueOf<StrokeCap>(json.optString("cap", "Butt")) }.getOrDefault(StrokeCap.Butt),
        join = runCatching { enumValueOf<StrokeJoin>(json.optString("join", "Miter")) }.getOrDefault(StrokeJoin.Miter),
        dash = json.optJSONArray("dash")?.let { arr ->
            (0 until arr.length()).map { arr.getDouble(it).toFloat() }
        } ?: emptyList()
    )

    private fun serializeDefinitions(defs: Definitions): JSONObject = JSONObject().apply {
        put("gradients", JSONArray().apply {
            defs.gradients.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id)
                    put("name", g.name)
                    when (g) {
                        is LinearGradientResource -> {
                            put("type", "linear")
                            put("x1", g.x1.toDouble()); put("y1", g.y1.toDouble())
                            put("x2", g.x2.toDouble()); put("y2", g.y2.toDouble())
                            put("stops", serializeStops(g.stops))
                        }
                        is RadialGradientResource -> {
                            put("type", "radial")
                            put("cx", g.cx.toDouble()); put("cy", g.cy.toDouble()); put("r", g.r.toDouble())
                            put("stops", serializeStops(g.stops))
                        }
                    }
                })
            }
        })
    }

    private fun deserializeDefinitions(json: JSONObject): Definitions {
        val arr = json.optJSONArray("gradients") ?: return Definitions()
        val list = mutableListOf<com.compose.canvas.core.GradientResource>()
        for (i in 0 until arr.length()) {
            val g = arr.getJSONObject(i)
            val stops = deserializeStops(g.optJSONArray("stops"))
            list += when (g.optString("type")) {
                "radial" -> RadialGradientResource(
                    id = g.optString("id", newObjectId()),
                    name = g.optString("name", "Radial"),
                    cx = g.optDouble("cx", 0.5).toFloat(),
                    cy = g.optDouble("cy", 0.5).toFloat(),
                    r = g.optDouble("r", 0.5).toFloat(),
                    stops = stops
                )
                else -> LinearGradientResource(
                    id = g.optString("id", newObjectId()),
                    name = g.optString("name", "Linear"),
                    x1 = g.optDouble("x1").toFloat(),
                    y1 = g.optDouble("y1").toFloat(),
                    x2 = g.optDouble("x2", 1.0).toFloat(),
                    y2 = g.optDouble("y2").toFloat(),
                    stops = stops
                )
            }
        }
        return Definitions(list)
    }

    private fun serializeStops(stops: List<GradientStop>): JSONArray = JSONArray().apply {
        stops.forEach { s ->
            put(JSONObject().apply {
                put("offset", s.offset.toDouble())
                putColor(this, s.color)
                put("alpha", s.alpha.toDouble())
            })
        }
    }

    private fun deserializeStops(arr: JSONArray?): List<GradientStop> {
        if (arr == null || arr.length() == 0) return listOf(GradientStop(0f, Color.White), GradientStop(1f, Color.Black))
        return (0 until arr.length()).map { i ->
            val s = arr.getJSONObject(i)
            GradientStop(
                offset = s.optDouble("offset").toFloat(),
                color = getColor(s),
                alpha = s.optDouble("alpha", 1.0).toFloat()
            )
        }
    }

    private fun serializeMetadata(meta: ShapeMetadata): JSONObject = JSONObject().apply {
        put("parameters", JSONObject().apply {
            meta.parameters.forEach { (k, v) -> put(k, v.toDouble()) }
        })
        put("ports", JSONArray().apply { meta.ports.forEach { put(serializePort(it)) } })
        put("anchors", JSONArray().apply { meta.anchors.forEach { put(serializeAnchor(it)) } })
        put("contentAreas", JSONArray().apply { meta.contentAreas.forEach { put(serializeContentArea(it)) } })
    }

    private fun deserializeMetadata(json: JSONObject): ShapeMetadata {
        val paramsJson = json.optJSONObject("parameters") ?: json
        val parameters = mutableMapOf<String, Float>()
        val keys = paramsJson.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (k in setOf("ports", "anchors", "contentAreas", "parameters")) continue
            if (paramsJson.opt(k) is Number) parameters[k] = paramsJson.getDouble(k).toFloat()
        }
        json.optJSONObject("parameters")?.let { nested ->
            val nk = nested.keys()
            while (nk.hasNext()) {
                val k = nk.next()
                if (nested.opt(k) is Number) parameters[k] = nested.getDouble(k).toFloat()
            }
        }
        return ShapeMetadata(
            parameters = parameters,
            ports = jsonArray(json, "ports") { deserializePort(it) },
            anchors = jsonArray(json, "anchors") { deserializeAnchor(it) },
            contentAreas = jsonArray(json, "contentAreas") { deserializeContentArea(it) }
        )
    }

    private fun serializeSemantics(sem: ObjectSemantics): JSONObject = JSONObject().apply {
        put("parameters", JSONObject().apply {
            sem.parameters.forEach { (k, v) -> put(k, v.toDouble()) }
        })
        put("ports", JSONArray().apply { sem.ports.forEach { put(serializePort(it)) } })
        put("anchors", JSONArray().apply { sem.anchors.forEach { put(serializeAnchor(it)) } })
        put("contentAreas", JSONArray().apply { sem.contentAreas.forEach { put(serializeContentArea(it)) } })
        put("resizeRules", serializeResizeRules(sem.resizeRules))
    }

    private fun deserializeSemantics(json: JSONObject): ObjectSemantics {
        val params = mutableMapOf<String, Float>()
        json.optJSONObject("parameters")?.let { nested ->
            val nk = nested.keys()
            while (nk.hasNext()) {
                val k = nk.next()
                if (nested.opt(k) is Number) params[k] = nested.getDouble(k).toFloat()
            }
        }
        return ObjectSemantics(
            parameters = params,
            ports = jsonArray(json, "ports") { deserializePort(it) },
            anchors = jsonArray(json, "anchors") { deserializeAnchor(it) },
            contentAreas = jsonArray(json, "contentAreas") { deserializeContentArea(it) },
            resizeRules = json.optJSONObject("resizeRules")?.let(::deserializeResizeRules) ?: ResizeRules()
        )
    }

    private fun serializeResizeRules(rules: ResizeRules): JSONObject = JSONObject().apply {
        put("horizontal", rules.horizontal.name)
        put("vertical", rules.vertical.name)
        put("preserveAspectRatio", rules.preserveAspectRatio)
        put("minWidth", rules.minWidth.toDouble())
        put("minHeight", rules.minHeight.toDouble())
        rules.maxWidth?.let { put("maxWidth", it.toDouble()) }
        rules.maxHeight?.let { put("maxHeight", it.toDouble()) }
    }

    private fun deserializeResizeRules(json: JSONObject): ResizeRules = ResizeRules(
        horizontal = runCatching { enumValueOf<ResizeMode>(json.optString("horizontal")) }
            .getOrDefault(ResizeMode.SCALE),
        vertical = runCatching { enumValueOf<ResizeMode>(json.optString("vertical")) }
            .getOrDefault(ResizeMode.SCALE),
        preserveAspectRatio = json.optBoolean("preserveAspectRatio", true),
        minWidth = json.optDouble("minWidth", 0.0).toFloat(),
        minHeight = json.optDouble("minHeight", 0.0).toFloat(),
        maxWidth = if (json.has("maxWidth")) json.optDouble("maxWidth").toFloat() else null,
        maxHeight = if (json.has("maxHeight")) json.optDouble("maxHeight").toFloat() else null
    )

    private fun serializePort(port: Port): JSONObject = JSONObject().apply {
        put("id", port.id)
        put("name", port.name)
        put("x", port.x.toDouble())
        put("y", port.y.toDouble())
        put("directionDegrees", port.directionDegrees.toDouble())
        put("normalDegrees", port.normalDegrees.toDouble())
        put("portType", port.portType.name)
        put("connectionType", port.connectionType)
        put("valueType", port.valueType)
        put("snapRadius", port.snapRadius.toDouble())
    }

    private fun deserializePort(json: JSONObject): Port = Port(
        id = json.optString("id", newObjectId()),
        name = json.optString("name", "port"),
        x = json.optDouble("x").toFloat(),
        y = json.optDouble("y").toFloat(),
        directionDegrees = json.optDouble("directionDegrees").toFloat(),
        normalDegrees = json.optDouble("normalDegrees").toFloat(),
        portType = runCatching { enumValueOf<PortType>(json.optString("portType", "CUSTOM")) }
            .getOrDefault(PortType.CUSTOM),
        connectionType = json.optString("connectionType"),
        valueType = json.optString("valueType"),
        snapRadius = json.optDouble("snapRadius", 8.0).toFloat()
    )

    private fun serializeAnchor(anchor: NamedAnchor): JSONObject = JSONObject().apply {
        put("id", anchor.id)
        put("name", anchor.name)
        put("x", anchor.x.toDouble())
        put("y", anchor.y.toDouble())
        put("kind", anchor.kind.name)
    }

    private fun deserializeAnchor(json: JSONObject): NamedAnchor = NamedAnchor(
        id = json.optString("id", newObjectId()),
        name = json.optString("name", "anchor"),
        x = json.optDouble("x").toFloat(),
        y = json.optDouble("y").toFloat(),
        kind = runCatching { enumValueOf<AnchorKind>(json.optString("kind", "CUSTOM")) }
            .getOrDefault(AnchorKind.CUSTOM)
    )

    private fun serializeContentArea(area: ContentArea): JSONObject = JSONObject().apply {
        put("id", area.id)
        put("name", area.name)
        put("padding", area.padding.toDouble())
        put("contentType", area.contentType)
        put("bounds", JSONObject().apply {
            put("left", area.bounds.left.toDouble())
            put("top", area.bounds.top.toDouble())
            put("right", area.bounds.right.toDouble())
            put("bottom", area.bounds.bottom.toDouble())
        })
    }

    private fun deserializeContentArea(json: JSONObject): ContentArea {
        val b = json.optJSONObject("bounds") ?: JSONObject()
        return ContentArea(
            id = json.optString("id", newObjectId()),
            name = json.optString("name", "content"),
            padding = json.optDouble("padding").toFloat(),
            contentType = json.optString("contentType"),
            bounds = RectF(
                b.optDouble("left").toFloat(),
                b.optDouble("top").toFloat(),
                b.optDouble("right").toFloat(),
                b.optDouble("bottom").toFloat()
            )
        )
    }

    private fun putColor(json: JSONObject, color: Color) {
        if (color == Color.Unspecified) {
            json.put("colorSet", false)
        } else {
            json.put("colorSet", true)
            json.put("argb", color.toArgb())
        }
    }

    private fun getColor(json: JSONObject): Color {
        if (!json.optBoolean("colorSet", false)) return Color.Unspecified
        return Color(json.optInt("argb"))
    }

    private fun <T> jsonArray(parent: JSONObject, key: String, map: (JSONObject) -> T): List<T> {
        val arr = parent.optJSONArray(key) ?: return emptyList()
        val out = mutableListOf<T>()
        for (i in 0 until arr.length()) out.add(map(arr.getJSONObject(i)))
        return out
    }
}
