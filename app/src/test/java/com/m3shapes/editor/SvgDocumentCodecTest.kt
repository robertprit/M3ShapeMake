package com.m3shapes.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SvgDocumentCodecTest {
    @Test
    fun exportThenImport_preservesSupportedElements() {
        val doc = SvgDocument(width = 320f, height = 240f).withElements(
            listOf(
                SvgRectElement(name = "A"),
                SvgCircleElement(name = "B"),
                SvgPolygonElement(name = "C"),
                SvgPathElement(name = "D")
            )
        )
        val svg = SvgDocumentCodec.export(doc)
        val imported = SvgDocumentCodec.import(svg)

        assertEquals(4, imported.document.elements.size)
        assertEquals(320f, imported.document.width)
        assertEquals(240f, imported.document.height)
    }

    @Test
    fun import_unknownTag_emitsWarning() {
        val svg = """
<svg viewBox="0 0 256 256" xmlns="http://www.w3.org/2000/svg">
    <ellipse cx="10" cy="10" rx="4" ry="4" />
</svg>
""".trimIndent()
        val imported = SvgDocumentCodec.import(svg)
        assertTrue(imported.warnings.isNotEmpty())
    }
}
