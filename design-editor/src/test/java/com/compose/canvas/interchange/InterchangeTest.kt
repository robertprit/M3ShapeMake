package com.compose.canvas.interchange

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.compose.canvas.core.FillStyle
import com.compose.canvas.core.ObjectSemantics
import com.compose.canvas.core.ObjectTransform
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorLayer
import com.compose.canvas.core.VectorObject
import com.compose.canvas.editor.AlignmentEngine
import com.compose.canvas.editor.AlignmentMode
import com.compose.canvas.editor.GridSettings
import com.compose.canvas.editor.SnapEngine
import com.compose.canvas.editor.SnapTarget
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import com.compose.canvas.geometry.RectGeometry
import com.compose.canvas.metadata.Port
import com.compose.canvas.metadata.PortType
import com.compose.canvas.tools.path.BooleanOp
import com.compose.canvas.tools.path.BooleanOps
import com.compose.canvas.tools.path.PathSimplifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InterchangeTest {

    @Test
    fun `svg path parser supports cubics and close`() {
        val geom = SvgPathParser.parse("M 0 20 L 20 0 C 10 5 15 8 40 20 Z")
        assertEquals(4, geom.segments.size)
        assertTrue(geom.segments.last() is PathSegment.Close)
        assertTrue(geom.segments[2] is PathSegment.CubicBezierTo)
    }

    @Test
    fun `svg import then export keeps rect geometry`() {
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 120" width="300" height="120">
              <rect x="10" y="20" width="80" height="40" rx="8" fill="#6750A4" stroke="#000000" stroke-width="2"/>
            </svg>
        """.trimIndent()
        val imported = SvgImport.import(svg)
        assertTrue(imported.diagnostics.none { it.level == DiagnosticLevel.ERROR })
        val doc = imported.document!!
        val obj = doc.allObjects().single()
        val g = obj.geometry as RectGeometry
        assertEquals(80f, g.width, 0.01f)
        assertEquals(40f, g.height, 0.01f)
        val out = SvgExport.export(doc).text
        val again = SvgImport.import(out).document!!
        val g2 = again.allObjects().single().geometry as RectGeometry
        assertEquals(g.width, g2.width, 0.01f)
        assertEquals(g.height, g2.height, 0.01f)
    }

    @Test
    fun `svg import reports unsupported filter`() {
        val svg = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10">
              <filter id="f"><feGaussianBlur stdDeviation="2"/></filter>
              <rect width="10" height="10"/>
            </svg>
        """.trimIndent()
        val result = SvgImport.import(svg)
        assertTrue(result.diagnostics.any { it.code.contains("FILTER") })
    }

    @Test
    fun `metadata export is versioned`() {
        val doc = VectorDocument(
            name = "LogicBlock",
            layers = listOf(
                VectorLayer(
                    objects = listOf(
                        VectorObject(
                            name = "Block",
                            geometry = RectGeometry(300f, 120f, 12f),
                            semantics = ObjectSemantics(
                                parameters = mapOf("notchWidth" to 18f),
                                ports = listOf(Port(name = "in", x = 150f, y = 0f, portType = PortType.VALUE_INPUT))
                            )
                        )
                    )
                )
            )
        )
        val json = MetadataExport.export(doc).text
        assertTrue(json.contains("\"format\": \"vt-shape\""))
        assertTrue(json.contains("\"formatVersion\": 1"))
        assertTrue(json.contains("notchWidth"))
        assertTrue(json.contains("VALUE_INPUT"))
    }

    @Test
    fun `compose path export contains commands`() {
        val doc = VectorDocument(
            layers = listOf(
                VectorLayer(
                    objects = listOf(
                        VectorObject(
                            geometry = PathGeometry(
                                listOf(
                                    PathSegment.MoveTo(0f, 20f),
                                    PathSegment.LineTo(20f, 0f),
                                    PathSegment.Close
                                ),
                                closed = true
                            )
                        )
                    )
                )
            )
        )
        val code = ComposePathExport.export(doc, false).text
        assertTrue(code.contains("moveTo(0f, 20f)"))
        assertTrue(code.contains("lineTo(20f, 0f)"))
    }

    @Test
    fun `kotlin shape export creates Shape class`() {
        val doc = VectorDocument(
            name = "LogicBlock",
            layers = listOf(
                VectorLayer(objects = listOf(VectorObject(geometry = RectGeometry(100f, 40f))))
            )
        )
        val src = KotlinShapeExport.export(doc).text
        assertTrue(src.contains("class LogicBlockShape"))
        assertTrue(src.contains("createOutline"))
        assertTrue(src.contains("Outline.Generic"))
    }

    @Test
    fun `boolean intersection of overlapping squares`() {
        val a = listOf(Offset(0f, 0f), Offset(10f, 0f), Offset(10f, 10f), Offset(0f, 10f))
        val b = listOf(Offset(5f, 5f), Offset(15f, 5f), Offset(15f, 15f), Offset(5f, 15f))
        val out = BooleanOps.apply(a, b, BooleanOp.Intersection)
        assertTrue(out.size >= 3)
        assertTrue(BooleanOps.pointInPolygon(Offset(7f, 7f), out))
        assertTrue(!BooleanOps.pointInPolygon(Offset(1f, 1f), out))
    }

    @Test
    fun `path simplifier reduces points`() {
        val pts = (0..200).map { Offset(it.toFloat(), if (it % 20 == 0) 2f else 0f) }
        val simple = PathSimplifier.simplify(pts, 3f)
        assertTrue(simple.size < pts.size)
        assertTrue(simple.size >= 2)
    }

    @Test
    fun `snap engine snaps to grid`() {
        val doc = VectorDocument()
        val (snapped, guides) = SnapEngine.snap(
            Offset(53f, 48f),
            doc,
            GridSettings(enabled = true, size = 50f, snap = true),
            threshold = 10f
        )
        assertEquals(50f, snapped.x, 0.01f)
        assertEquals(50f, snapped.y, 0.01f)
        assertTrue(guides.any { it.target == SnapTarget.Grid })
    }

    @Test
    fun `alignment left shares min x`() {
        val a = VectorObject(geometry = RectGeometry(20f, 20f), transform = ObjectTransform(x = 80f, y = 0f), fill = FillStyle(color = Color.Black))
        val b = VectorObject(geometry = RectGeometry(20f, 20f), transform = ObjectTransform(x = 10f, y = 40f), fill = FillStyle(color = Color.Black))
        val aligned = AlignmentEngine.apply(listOf(a, b), AlignmentMode.AlignLeft)
        val xs = aligned.map { com.compose.canvas.core.BoundsCalculator.of(it)!!.left }
        assertEquals(xs[0], xs[1], 0.01f)
    }

    @Test
    fun `document codec roundtrips group and preset`() {
        val child = VectorObject(name = "c", geometry = RectGeometry(10f, 10f))
        val group = com.compose.canvas.core.GroupObject(name = "g", children = listOf(child))
        val doc = VectorDocument(layers = listOf(VectorLayer(objects = listOf(group))))
        val restored = DocumentCodec.deserialize(DocumentCodec.serialize(doc))
        val g = restored.layers[0].objects.single() as com.compose.canvas.core.GroupObject
        assertEquals("g", g.name)
        assertEquals(1, g.children.size)
    }
}
