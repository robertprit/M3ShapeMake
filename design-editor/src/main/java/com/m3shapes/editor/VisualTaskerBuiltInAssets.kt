package com.m3shapes.editor

import androidx.compose.ui.graphics.Color
import com.compose.canvas.asset.AssetLibrary
import com.compose.canvas.asset.VisualAsset
import com.compose.canvas.asset.VisualAssetIdentity
import com.compose.canvas.asset.VisualAssetType
import com.compose.canvas.asset.VisualState
import com.compose.canvas.core.CanvasSettings
import com.compose.canvas.core.FillStyle
import com.compose.canvas.core.ObjectSemantics
import com.compose.canvas.core.ShapeMetadata
import com.compose.canvas.core.StrokeJoin
import com.compose.canvas.core.StrokeStyle
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorLayer
import com.compose.canvas.core.VectorObject
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import com.compose.canvas.geometry.RectF
import com.compose.canvas.metadata.ContentArea
import com.compose.canvas.metadata.Port
import com.compose.canvas.metadata.PortType
import com.compose.canvas.metadata.ResizeMode
import com.compose.canvas.metadata.ResizeRules
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Canonical editable snapshots of the silhouettes used by VisualTasker editors. */
object VisualTaskerBuiltInAssets {
    private const val NODE_SIZE = 128f
    private const val BLOCK_WIDTH = 288f
    private const val BLOCK_HEIGHT = 48f
    private const val REPORTER_WIDTH = 74f
    private const val REPORTER_HEIGHT = 40f
    private const val NESTED_INDENT = 32f

    val all: List<VisualAsset> by lazy { blockAssets() + nodeAssets() }

    /** Adds missing built-ins without replacing a user's edited or versioned copy. */
    fun seed(library: AssetLibrary): Int = all.count { asset ->
        if (library.get(asset.identity.assetId) != null) return@count false
        library.save(asset)
        true
    }

    private fun blockAssets(): List<VisualAsset> = listOf(
        blockAsset(
            id = "statement",
            name = "Statement Block",
            width = BLOCK_WIDTH,
            height = BLOCK_HEIGHT,
            path = statementPath(BLOCK_WIDTH, BLOCK_HEIGHT, hasPrevious = true),
            ports = stackPorts(BLOCK_WIDTH, BLOCK_HEIGHT, previous = true),
            tags = setOf("statement", "stack"),
        ),
        blockAsset(
            id = "start",
            name = "Start Block",
            width = BLOCK_WIDTH,
            height = BLOCK_HEIGHT,
            path = statementPath(BLOCK_WIDTH, BLOCK_HEIGHT, hasPrevious = false),
            ports = stackPorts(BLOCK_WIDTH, BLOCK_HEIGHT, previous = false),
            tags = setOf("start", "terminator"),
        ),
        blockAsset(
            id = "control-container",
            name = "Control Container",
            width = 164f,
            height = 176f,
            path = containerPath(164f, 176f, listOf()),
            ports = containerPorts(164f, 176f, listOf()),
            tags = setOf("control", "container", "branch"),
        ),
        blockAsset(
            id = "if-elseif-else",
            name = "If ElseIf Else Container",
            width = 164f,
            height = 308f,
            path = containerPath(164f, 308f, listOf(116f, 204f)),
            ports = containerPorts(164f, 308f, listOf(44f, 132f, 220f)),
            tags = setOf("control", "container", "branch", "if"),
        ),
        reporterAsset("reporter-number", "Number Reporter", reporterPill(), "Number", setOf("number", "pill")),
        reporterAsset("reporter-string", "String Reporter", roundedRect(REPORTER_WIDTH, REPORTER_HEIGHT, 6f), "String", setOf("string", "rectangle")),
        reporterAsset("reporter-boolean", "Boolean Reporter", polygon(REPORTER_WIDTH, REPORTER_HEIGHT, .5f to .08f, .88f to .92f, .12f to .92f), "Boolean", setOf("boolean", "triangle")),
        reporterAsset("reporter-any", "Any Reporter", polygon(REPORTER_WIDTH, REPORTER_HEIGHT, .5f to .08f, .94f to .5f, .5f to .92f, .06f to .5f), "Any", setOf("any", "diamond")),
        reporterAsset("reporter-custom", "Custom Reporter", archPath(REPORTER_WIDTH, REPORTER_HEIGHT), "Custom", setOf("custom", "arch")),
        reporterAsset("reporter-fallback", "Fallback Reporter", polygon(REPORTER_WIDTH, REPORTER_HEIGHT, .2f to 0f, .8f to 0f, 1f to .5f, .8f to 1f, .2f to 1f, 0f to .5f), "Fallback", setOf("fallback", "clamshell")),
        blockAsset(
            id = "inline-reporter",
            name = "Inline Reporter",
            width = 92f,
            height = REPORTER_HEIGHT,
            path = inlineReporterPath(92f, REPORTER_HEIGHT),
            ports = listOf(valueOutput(0f, REPORTER_HEIGHT / 2f, "Any")),
            tags = setOf("reporter", "inline", "output"),
        ),
    )

    private fun nodeAssets(): List<VisualAsset> = NodeShape.entries.map { shape ->
        val path = nodePath(shape, NODE_SIZE, NODE_SIZE)
        val ports = listOf(
            Port(name = "flowIn", x = NODE_SIZE / 2f, y = 0f, directionDegrees = -90f, normalDegrees = -90f, portType = PortType.FLOW_INPUT, connectionType = "flow", snapRadius = 16f),
            Port(name = "flowOut", x = NODE_SIZE / 2f, y = NODE_SIZE, directionDegrees = 90f, normalDegrees = 90f, portType = PortType.FLOW_OUTPUT, connectionType = "flow", snapRadius = 16f),
            Port(name = "valueIn", x = 0f, y = NODE_SIZE / 2f, directionDegrees = 180f, normalDegrees = 180f, portType = PortType.VALUE_INPUT, connectionType = "data", valueType = "Any", snapRadius = 16f),
            Port(name = "valueOut", x = NODE_SIZE, y = NODE_SIZE / 2f, directionDegrees = 0f, normalDegrees = 0f, portType = PortType.VALUE_OUTPUT, connectionType = "data", valueType = "Any", snapRadius = 16f),
        )
        visualAsset(
            assetId = "vt.node.${shape.id}",
            name = "${shape.label} Node",
            type = VisualAssetType.NODE_SHAPE,
            width = NODE_SIZE,
            height = NODE_SIZE,
            geometry = path,
            fill = Color(0xFF5C6BC0),
            ports = ports,
            contentAreas = listOf(ContentArea(name = "label", bounds = RectF(22f, 22f, 106f, 106f), padding = 6f, contentType = "node-label")),
            tags = setOf("visualtasker", "builtin", "flowchart", "node", shape.id),
            source = "FlowchartNodeGeometry:${shape.sourceName}",
            resizeRules = ResizeRules(horizontal = ResizeMode.SCALE, vertical = ResizeMode.SCALE, preserveAspectRatio = true, minWidth = 32f, minHeight = 32f),
        )
    }

    private fun blockAsset(
        id: String,
        name: String,
        width: Float,
        height: Float,
        path: PathGeometry,
        ports: List<Port>,
        tags: Set<String>,
    ): VisualAsset = visualAsset(
        assetId = "vt.block.$id",
        name = name,
        type = VisualAssetType.BLOCK_SHAPE,
        width = width,
        height = height,
        geometry = path,
        fill = Color(0xFF6D4053),
        ports = ports,
        contentAreas = listOf(ContentArea(name = "fields", bounds = RectF(14f, 8f, width - 14f, minOf(40f, height - 8f)), padding = 4f, contentType = "block-fields")),
        tags = setOf("visualtasker", "builtin", "blockeditor", "block") + tags,
        source = "BlockShapes:$id",
        resizeRules = ResizeRules(horizontal = ResizeMode.STRETCH, vertical = ResizeMode.STRETCH, preserveAspectRatio = false, minWidth = 36f, minHeight = 28f),
    )

    private fun reporterAsset(id: String, name: String, path: PathGeometry, valueType: String, tags: Set<String>): VisualAsset =
        blockAsset(
            id = id,
            name = name,
            width = REPORTER_WIDTH,
            height = REPORTER_HEIGHT,
            path = path,
            ports = listOf(valueOutput(0f, REPORTER_HEIGHT / 2f, valueType)),
            tags = setOf("reporter", "value-output") + tags,
        )

    private fun visualAsset(
        assetId: String,
        name: String,
        type: VisualAssetType,
        width: Float,
        height: Float,
        geometry: PathGeometry,
        fill: Color,
        ports: List<Port>,
        contentAreas: List<ContentArea>,
        tags: Set<String>,
        source: String,
        resizeRules: ResizeRules,
    ): VisualAsset {
        val semantics = ObjectSemantics(ports = ports, contentAreas = contentAreas, resizeRules = resizeRules)
        val document = VectorDocument(
            name = name,
            canvas = CanvasSettings(width = width, height = height, viewBox = RectF(0f, 0f, width, height)),
            layers = listOf(
                VectorLayer(
                    name = "Shape",
                    objects = listOf(
                        VectorObject(
                            name = name,
                            geometry = geometry,
                            fill = FillStyle(color = fill),
                            stroke = StrokeStyle(color = Color(0xFFDAD6E0), width = 1.5f, join = StrokeJoin.Round),
                            semantics = semantics,
                        )
                    ),
                )
            ),
            metadata = ShapeMetadata(ports = ports, contentAreas = contentAreas),
        )
        return VisualAsset(
            identity = VisualAssetIdentity(assetId = assetId, name = name, type = type, tags = tags),
            design = document,
            states = listOf(VisualState("default", "Default", document)),
            metadata = mapOf(
                "builtin" to "true",
                "source" to source,
                "sourceWidth" to width.toString(),
                "sourceHeight" to height.toString(),
            ),
        )
    }

    private fun stackPorts(width: Float, height: Float, previous: Boolean): List<Port> = buildList {
        if (previous) add(Port(name = "previous", x = NESTED_INDENT, y = 0f, directionDegrees = -90f, normalDegrees = -90f, portType = PortType.PREVIOUS, connectionType = "statement", snapRadius = 18f))
        add(Port(name = "next", x = width / 2f, y = height, directionDegrees = 90f, normalDegrees = 90f, portType = PortType.NEXT, connectionType = "statement", snapRadius = 18f))
    }

    private fun containerPorts(width: Float, height: Float, branches: List<Float>): List<Port> = buildList {
        addAll(stackPorts(width, height, previous = true))
        branches.forEachIndexed { index, y ->
            add(Port(name = "branch${index + 1}", x = NESTED_INDENT, y = y, directionDegrees = 90f, normalDegrees = 90f, portType = PortType.STATEMENT_INPUT, connectionType = "statement", snapRadius = 18f))
        }
    }

    private fun valueOutput(x: Float, y: Float, valueType: String) = Port(
        name = "output",
        x = x,
        y = y,
        directionDegrees = 180f,
        normalDegrees = 180f,
        portType = PortType.VALUE_OUTPUT,
        connectionType = "value",
        valueType = valueType,
        snapRadius = 16f,
    )

    private enum class NodeShape(val id: String, val label: String, val sourceName: String) {
        Circle("circle", "Circle", "Circle"), Slanted("slanted", "Slanted", "Slanted"), Arch("arch", "Arch", "Arch"),
        Diamond("diamond", "Diamond", "Diamond"), ClamShell("clam-shell", "Clam Shell", "ClamShell"), Bun("bun", "Bun", "Bun"),
        PuffyDiamond("puffy-diamond", "Puffy Diamond", "PuffyDiamond"), Fan("fan", "Fan", "Fan"), Pentagon("pentagon", "Pentagon", "Pentagon"),
        Arrow("arrow", "Arrow", "Arrow"), Cookie4("cookie-4", "Cookie 4", "Cookie4"), Octagon("octagon", "Octagon", "Octagon"),
        Eye("eye", "Eye", "Eye"), Triangle("triangle", "Triangle", "Triangle"), Gem("gem", "Gem", "Gem"),
        SemiCircle("semi-circle", "Semi Circle", "SemiCircle"), Pill("pill", "Pill", "Pill"), Operator("operator", "Operator", "Operator"),
        SoftBurst("soft-burst", "Soft Burst", "SoftBurst"), Sunny("sunny", "Sunny", "Sunny"), VerySunny("very-sunny", "Very Sunny", "VerySunny"),
        Ghostish("ghostish", "Ghostish", "Ghostish"), Flower("flower", "Flower", "Flower"), Clover4("clover-4", "Clover 4", "Clover4"),
        Burst("burst", "Burst", "Burst"), FunctionCall("function-call", "Function Call", "FunctionCall"),
        TryStart("try-start", "Try Start", "TryStart"), Catch("catch", "Catch", "Catch"), TryEnd("try-end", "Try End", "TryEnd"),
        Default("default", "Default", "Default"),
    }

    private fun statementPath(w: Float, h: Float, hasPrevious: Boolean): PathGeometry = path {
        val corner = 12f
        move(corner, 0f)
        if (hasPrevious) {
            line(20f, 0f); cubic(26f, 0f, 26f, 7f, 32f, 7f); cubic(38f, 7f, 38f, 0f, 44f, 0f)
        }
        line(w - corner, 0f); roundTopRight(w, corner); line(w, h - corner); roundBottomRight(w, h, corner)
        val center = w / 2f
        line(center + 12f, h); cubic(center + 6f, h, center + 6f, h - 7f, center, h - 7f)
        cubic(center - 6f, h - 7f, center - 6f, h, center - 12f, h)
        line(corner, h); roundBottomLeft(h, corner); line(0f, corner); roundTopLeft(corner); close()
    }

    private fun containerPath(w: Float, h: Float, dividers: List<Float>): PathGeometry = path {
        val c = 12f
        val header = 44f
        val footer = 24f
        val bodyBottom = h - footer
        move(c, 0f); line(20f, 0f); cubic(26f, 0f, 26f, 7f, 32f, 7f); cubic(38f, 7f, 38f, 0f, 44f, 0f)
        line(w - c, 0f); roundTopRight(w, c); line(w, header - c); quad(w, header, w - c, header)
        line(NESTED_INDENT + c, header); quad(NESTED_INDENT, header, NESTED_INDENT, header + c)
        dividers.forEach { y ->
            line(NESTED_INDENT, y - c); quad(NESTED_INDENT, y, NESTED_INDENT + c, y)
            line(w - 22f, y); quad(w, y, w, y + 22f); quad(w, y + 44f, w - 22f, y + 44f)
            line(NESTED_INDENT + c, y + 44f); quad(NESTED_INDENT, y + 44f, NESTED_INDENT, y + 44f + c)
        }
        line(NESTED_INDENT, bodyBottom - c); quad(NESTED_INDENT, bodyBottom, NESTED_INDENT + c, bodyBottom)
        line(w - c, bodyBottom); quad(w, bodyBottom, w, bodyBottom + c); line(w, h - c); roundBottomRight(w, h, c)
        val center = w / 2f
        line(center + 12f, h); cubic(center + 6f, h, center + 6f, h - 7f, center, h - 7f); cubic(center - 6f, h - 7f, center - 6f, h, center - 12f, h)
        line(c, h); roundBottomLeft(h, c); line(0f, c); roundTopLeft(c); close()
    }

    private fun reporterPill() = roundedRect(REPORTER_WIDTH, REPORTER_HEIGHT, 13f)

    private fun inlineReporterPath(w: Float, h: Float): PathGeometry = path {
        val bodyLeft = 7f
        val radius = 10f
        move(bodyLeft + radius, 0f); line(w - radius, 0f); quad(w, 0f, w, radius); line(w, h - radius); quad(w, h, w - radius, h)
        line(bodyLeft + radius, h); quad(bodyLeft, h, bodyLeft, h - radius); line(bodyLeft, h / 2f + 12f)
        cubic(bodyLeft, h / 2f + 6f, 3.5f, h / 2f + 6f, 0f, h / 2f); cubic(3.5f, h / 2f - 6f, bodyLeft, h / 2f - 6f, bodyLeft, h / 2f - 12f)
        line(bodyLeft, radius); quad(bodyLeft, 0f, bodyLeft + radius, 0f); close()
    }

    private fun archPath(w: Float, h: Float): PathGeometry = path {
        val radius = minOf(h * .7f, w / 2f)
        move(radius, 0f); line(w - radius, 0f); quad(w, 0f, w, radius); line(w, h); line(0f, h); line(0f, radius); quad(0f, 0f, radius, 0f); close()
    }

    private fun nodePath(shape: NodeShape, w: Float, h: Float): PathGeometry = when (shape) {
        NodeShape.Circle -> ellipse(w, h)
        NodeShape.Slanted -> polygon(w, h, .14f to 0f, 1f to 0f, .86f to 1f, 0f to 1f)
        NodeShape.Arch -> path { move(0f, h); line(0f, h * .48f); cubic(0f, h * .16f, w * .18f, 0f, w * .5f, 0f); cubic(w * .82f, 0f, w, h * .16f, w, h * .48f); line(w, h); close() }
        NodeShape.Diamond -> polygon(w, h, .5f to 0f, 1f to .5f, .5f to 1f, 0f to .5f)
        NodeShape.ClamShell -> polygon(w, h, .16f to 0f, .84f to 0f, 1f to .5f, .84f to 1f, .16f to 1f, 0f to .5f)
        NodeShape.Bun -> path { move(w * .22f, 0f); cubic(w * .02f, 0f, 0f, h * .2f, 0f, h * .5f); cubic(0f, h * .8f, w * .02f, h, w * .22f, h); cubic(w * .42f, h, w * .42f, h * .74f, w * .5f, h * .74f); cubic(w * .58f, h * .74f, w * .58f, h, w * .78f, h); cubic(w * .98f, h, w, h * .8f, w, h * .5f); cubic(w, h * .2f, w * .98f, 0f, w * .78f, 0f); close() }
        NodeShape.PuffyDiamond -> puffyDiamond(w, h)
        NodeShape.Fan -> path { move(0f, 0f); cubic(w * .5f, 0f, w, h * .5f, w, h); line(0f, h); close() }
        NodeShape.Pentagon -> polygon(w, h, .5f to 0f, 1f to .34f, .82f to 1f, .18f to 1f, 0f to .34f)
        NodeShape.Arrow -> polygon(w, h, 0f to .18f, .62f to .18f, .62f to 0f, 1f to .5f, .62f to 1f, .62f to .82f, 0f to .82f)
        NodeShape.Cookie4 -> path { move(w * .5f, 0f); cubic(w * .68f, 0f, w * .68f, h * .2f, w, h * .2f); cubic(w, h * .5f, w * .8f, h * .5f, w * .8f, h * .8f); cubic(w * .5f, h * .8f, w * .5f, h, w * .2f, h * .8f); cubic(w * .2f, h * .5f, 0f, h * .5f, 0f, h * .2f); cubic(w * .32f, h * .2f, w * .32f, 0f, w * .5f, 0f); close() }
        NodeShape.Octagon -> polygon(w, h, .18f to 0f, .82f to 0f, 1f to .18f, 1f to .82f, .82f to 1f, .18f to 1f, 0f to .82f, 0f to .18f)
        NodeShape.Eye -> path { move(0f, h * .5f); cubic(w * .2f, 0f, w * .8f, 0f, w, h * .5f); cubic(w * .8f, h, w * .2f, h, 0f, h * .5f); close() }
        NodeShape.Triangle -> polygon(w, h, .5f to 0f, 1f to 1f, 0f to 1f)
        NodeShape.Gem -> polygon(w, h, .5f to 0f, .9f to .2f, 1f to .68f, .68f to 1f, .32f to 1f, 0f to .68f, .1f to .2f)
        NodeShape.SemiCircle -> path { move(0f, h); line(0f, h * .5f); cubic(0f, h * .22f, w * .22f, 0f, w * .5f, 0f); cubic(w * .78f, 0f, w, h * .22f, w, h * .5f); line(w, h); close() }
        NodeShape.Pill -> path { move(w * .28f, 0f); cubic(w * .1f, 0f, 0f, h * .18f, 0f, h * .42f); cubic(0f, h * .68f, w * .14f, h * .9f, w * .34f, h * .96f); cubic(w * .54f, h, w * .78f, h, w * .9f, h * .84f); cubic(w, h * .7f, w, h * .42f, w * .9f, h * .26f); cubic(w * .78f, h * .06f, w * .54f, 0f, w * .28f, 0f); close() }
        NodeShape.Operator -> path { move(w * .14f, h * .06f); line(w * .86f, h * .06f); cubic(w, h * .2f, w, h * .8f, w * .86f, h * .94f); line(w * .14f, h * .94f); cubic(0f, h * .8f, 0f, h * .2f, w * .14f, h * .06f); close() }
        NodeShape.SoftBurst -> radialShape(w, h, 8, .78f, true)
        NodeShape.Sunny -> radialShape(w, h, 10, .78f, true)
        NodeShape.VerySunny -> radialShape(w, h, 14, .68f, true)
        NodeShape.Ghostish -> path { move(0f, h); line(0f, h * .28f); cubic(0f, h * .1f, w * .14f, 0f, w * .32f, 0f); cubic(w * .44f, 0f, w * .44f, h * .15f, w * .5f, h * .15f); cubic(w * .56f, h * .15f, w * .56f, 0f, w * .68f, 0f); cubic(w * .86f, 0f, w, h * .1f, w, h * .28f); line(w, h); cubic(w * .78f, h * .84f, w * .7f, h, w * .5f, h); cubic(w * .3f, h, w * .22f, h * .84f, 0f, h); close() }
        NodeShape.Flower -> radialShape(w, h, 6, .72f, true)
        NodeShape.Clover4 -> clover4(w, h)
        NodeShape.Burst -> radialShape(w, h, 12, .56f, false)
        NodeShape.FunctionCall -> polygon(w, h, .12f to 0f, .88f to 0f, 1f to .5f, .88f to 1f, .12f to 1f, 0f to .5f)
        NodeShape.TryStart -> polygon(w, h, 0f to 0f, .78f to 0f, 1f to .22f, 1f to 1f, 0f to 1f)
        NodeShape.Catch -> polygon(w, h, .22f to 0f, 1f to 0f, 1f to 1f, .22f to 1f, 0f to .5f)
        NodeShape.TryEnd -> polygon(w, h, 0f to 0f, 1f to 0f, 1f to .78f, .78f to 1f, 0f to 1f)
        NodeShape.Default -> roundedRect(w, h, w * .18f)
    }

    private fun puffyDiamond(w: Float, h: Float): PathGeometry = path {
        move(w * .5f, 0f); cubic(w * .64f, 0f, w * .67f, h * .18f, w * .78f, h * .22f); cubic(w * .9f, h * .27f, w, h * .34f, w, h * .5f)
        cubic(w, h * .66f, w * .9f, h * .73f, w * .78f, h * .78f); cubic(w * .67f, h * .82f, w * .64f, h, w * .5f, h)
        cubic(w * .36f, h, w * .33f, h * .82f, w * .22f, h * .78f); cubic(w * .1f, h * .73f, 0f, h * .66f, 0f, h * .5f)
        cubic(0f, h * .34f, w * .1f, h * .27f, w * .22f, h * .22f); cubic(w * .33f, h * .18f, w * .36f, 0f, w * .5f, 0f); close()
    }

    private fun clover4(w: Float, h: Float): PathGeometry = path {
        move(w * .5f, h * .16f); cubic(w * .58f, 0f, w, 0f, w, h * .32f); cubic(w, h * .48f, w * .84f, h * .5f, w * .84f, h * .5f)
        cubic(w, h * .5f, w, h * .92f, w * .68f, h); cubic(w * .52f, h, w * .5f, h * .84f, w * .5f, h * .84f)
        cubic(w * .5f, h, w * .08f, h, 0f, h * .68f); cubic(0f, h * .52f, w * .16f, h * .5f, w * .16f, h * .5f)
        cubic(0f, h * .5f, 0f, h * .08f, w * .32f, 0f); cubic(w * .48f, 0f, w * .5f, h * .16f, w * .5f, h * .16f); close()
    }

    private fun radialShape(w: Float, h: Float, points: Int, innerRatio: Float, rounded: Boolean): PathGeometry {
        val vertices = List(points * 2) { index ->
            val angle = -PI / 2.0 + index * PI / points
            val radius = minOf(w, h) / 2f * if (index % 2 == 0) 1f else innerRatio
            (w / 2f + cos(angle).toFloat() * radius) to (h / 2f + sin(angle).toFloat() * radius)
        }
        return path {
            if (!rounded) {
                vertices.forEachIndexed { index, point -> if (index == 0) move(point.first, point.second) else line(point.first, point.second) }
            } else {
                vertices.forEachIndexed { index, point ->
                    val previous = vertices[(index - 1 + vertices.size) % vertices.size]
                    val next = vertices[(index + 1) % vertices.size]
                    val entry = point.first + (previous.first - point.first) * .22f to point.second + (previous.second - point.second) * .22f
                    val exit = point.first + (next.first - point.first) * .22f to point.second + (next.second - point.second) * .22f
                    if (index == 0) move(entry.first, entry.second) else line(entry.first, entry.second)
                    quad(point.first, point.second, exit.first, exit.second)
                }
            }
            close()
        }
    }

    private fun polygon(w: Float, h: Float, vararg points: Pair<Float, Float>): PathGeometry = path {
        points.forEachIndexed { index, point -> if (index == 0) move(w * point.first, h * point.second) else line(w * point.first, h * point.second) }
        close()
    }

    private fun roundedRect(w: Float, h: Float, radius: Float): PathGeometry = path {
        val r = minOf(radius, w / 2f, h / 2f)
        move(r, 0f); line(w - r, 0f); quad(w, 0f, w, r); line(w, h - r); quad(w, h, w - r, h)
        line(r, h); quad(0f, h, 0f, h - r); line(0f, r); quad(0f, 0f, r, 0f); close()
    }

    private fun ellipse(w: Float, h: Float): PathGeometry = path {
        val k = .5522848f
        move(w / 2f, 0f); cubic(w / 2f + w / 2f * k, 0f, w, h / 2f - h / 2f * k, w, h / 2f)
        cubic(w, h / 2f + h / 2f * k, w / 2f + w / 2f * k, h, w / 2f, h)
        cubic(w / 2f - w / 2f * k, h, 0f, h / 2f + h / 2f * k, 0f, h / 2f)
        cubic(0f, h / 2f - h / 2f * k, w / 2f - w / 2f * k, 0f, w / 2f, 0f); close()
    }

    private fun path(block: PathBuilder.() -> Unit): PathGeometry = PathBuilder().apply(block).build()

    private class PathBuilder {
        private val segments = mutableListOf<PathSegment>()
        fun move(x: Float, y: Float) { segments += PathSegment.MoveTo(x, y) }
        fun line(x: Float, y: Float) { segments += PathSegment.LineTo(x, y) }
        fun quad(cx: Float, cy: Float, x: Float, y: Float) { segments += PathSegment.QuadraticBezierTo(cx, cy, x, y) }
        fun cubic(cx1: Float, cy1: Float, cx2: Float, cy2: Float, x: Float, y: Float) { segments += PathSegment.CubicBezierTo(cx1, cy1, cx2, cy2, x, y) }
        fun close() { segments += PathSegment.Close }
        fun roundTopRight(w: Float, radius: Float) = quad(w, 0f, w, radius)
        fun roundBottomRight(w: Float, h: Float, radius: Float) = quad(w, h, w - radius, h)
        fun roundBottomLeft(h: Float, radius: Float) = quad(0f, h, 0f, h - radius)
        fun roundTopLeft(radius: Float) = quad(0f, 0f, radius, 0f)
        fun build() = PathGeometry(segments.toList(), closed = true)
    }
}
