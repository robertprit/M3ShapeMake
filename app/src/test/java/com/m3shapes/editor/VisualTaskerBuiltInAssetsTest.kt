package com.m3shapes.editor

import com.compose.canvas.asset.EmaCodec
import com.compose.canvas.asset.InMemoryAssetLibrary
import com.compose.canvas.asset.VisualAssetType
import com.compose.canvas.metadata.PortType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualTaskerBuiltInAssetsTest {
    @Test
    fun catalogContainsUniqueValidBlockAndNodeAssets() {
        val assets = VisualTaskerBuiltInAssets.all

        assertEquals(41, assets.size)
        assertEquals(assets.size, assets.map { it.identity.assetId }.toSet().size)
        assertTrue(assets.any { it.identity.type == VisualAssetType.BLOCK_SHAPE })
        assertTrue(assets.any { it.identity.type == VisualAssetType.NODE_SHAPE })
        assets.forEach { asset ->
            val validation = EmaCodec.validate(asset)
            assertTrue("${asset.identity.assetId}: ${validation.diagnostics}", validation.valid)
            assertTrue(asset.design.allObjects().isNotEmpty())
        }
    }

    @Test
    fun seedingIsIdempotentAndPreservesSemanticPorts() {
        val library = InMemoryAssetLibrary()

        assertEquals(41, VisualTaskerBuiltInAssets.seed(library))
        assertEquals(0, VisualTaskerBuiltInAssets.seed(library))

        val statement = library.get("vt.block.statement")
        assertNotNull(statement)
        assertTrue(statement!!.design.metadata.ports.any { it.portType == PortType.PREVIOUS })
        assertTrue(statement.design.metadata.ports.any { it.portType == PortType.NEXT })

        val node = library.get("vt.node.diamond")
        assertNotNull(node)
        assertTrue(node!!.design.metadata.ports.any { it.portType == PortType.FLOW_INPUT })
        assertTrue(node.design.metadata.ports.any { it.portType == PortType.VALUE_OUTPUT })
    }
}
