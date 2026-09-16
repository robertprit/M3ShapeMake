package com.compose.canvas.core

import androidx.compose.ui.graphics.Color
import com.compose.canvas.interchange.DocumentCodec
import com.compose.canvas.geometry.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DocumentCodecRoundtripTest {

    private fun makeComplexDocument(): VectorDocument {
        val rect = VectorObject(
            name = "Block",
            geometry = RectGeometry(width = 300f, height = 120f, cornerRadius = 12f),
            transform = ObjectTransform(x = 100f, y = 200f, rotation = 15f, scaleX = 1.5f, scaleY = 1f),
            fill = FillStyle(color = Color(0xFF6750A4), alpha = 0.8f),
            stroke = StrokeStyle(
                color = Color(0xFF49454F),
                width = 2.5f,
                alpha = 1f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Bevel
            )
        )
        val ellipse = VectorObject(
            name = "Port",
            geometry = EllipseGeometry(radiusX = 10f, radiusY = 8f),
            transform = ObjectTransform(x = 250f, y = 260f),
            fill = FillStyle(color = Color(0xFFFFFFFF))
        )
        val path = VectorObject(
            name = "Arrow",
            geometry = PathGeometry(
                segments = listOf(
                    PathSegment.MoveTo(0f, 0f),
                    PathSegment.CubicBezierTo(10f, 20f, 30f, 40f, 50f, 60f),
                    PathSegment.QuadraticBezierTo(20f, 15f, 5f, 0f),
                    PathSegment.Close
                ),
                closed = true
            ),
            stroke = StrokeStyle(color = Color(0xFF006D3C), width = 3f)
        )
        return VectorDocument(
            name = "LogicBlock",
            canvas = CanvasSettings(width = 800f, height = 600f, backgroundTransparent = false),
            layers = listOf(
                VectorLayer(name = "Back", objects = listOf(rect)),
                VectorLayer(name = "Front", objects = listOf(ellipse, path))
            ),
            metadata = ShapeMetadata(
                parameters = mapOf("cornerRadius" to 12f, "notchWidth" to 18f)
            )
        )
    }

    @Test
    fun `roundtrip preserves document semantics`() {
        val original = makeComplexDocument()
        val serialized = DocumentCodec.serialize(original)
        val restored = DocumentCodec.deserialize(serialized)

        assertEquals(original, restored)
    }

    @Test
    fun `roundtrip twice is stable`() {
        val original = makeComplexDocument()
        val once = DocumentCodec.deserialize(DocumentCodec.serialize(original))
        val twice = DocumentCodec.deserialize(DocumentCodec.serialize(once))
        assertEquals(once, twice)
    }

    @Test
    fun `serialized format is versioned`() {
        val json = DocumentCodec.serialize(makeComplexDocument())
        org.junit.Assert.assertTrue(json.contains("\"format\":\"compose-canvas-document\""))
        org.junit.Assert.assertTrue(json.contains("\"documentFormatVersion\":1"))
        org.junit.Assert.assertTrue(json.contains("\"formatVersion\":1"))
    }

    @Test
    fun `empty document roundtrips`() {
        val original = VectorDocument()
        val restored = DocumentCodec.deserialize(DocumentCodec.serialize(original))
        assertEquals(original, restored)
    }

    @Test
    fun `newer version is rejected`() {
        val json = DocumentCodec.serialize(makeComplexDocument())
        val bumped = json
            .replace("\"documentFormatVersion\":1", "\"documentFormatVersion\":999")
            .replace("\"formatVersion\":1", "\"formatVersion\":999")

        assertThrows(IllegalArgumentException::class.java) {
            DocumentCodec.deserialize(bumped)
        }
    }

    @Test
    fun `unsupported format is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            DocumentCodec.deserialize("{\"format\":\"other\"}")
        }
    }
}