package com.compose.canvas.editor

import com.m3shapes.editor.ExpressiveShapeId
import org.junit.Assert.assertEquals
import org.junit.Test

class MaterialShapePresetsTest {
    @Test
    fun `material shape catalog exposes complete material 3 set`() {
        assertEquals(35, ExpressiveShapeId.all.size)
        assertEquals(ExpressiveShapeId.all.size, ExpressiveShapeId.all.map { it.composeRef }.distinct().size)
    }
}
