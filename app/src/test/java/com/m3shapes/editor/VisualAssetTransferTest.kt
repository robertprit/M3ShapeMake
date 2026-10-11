package com.m3shapes.editor

import com.compose.canvas.metadata.PortType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualAssetTransferTest {
    @Test
    fun selectedObjectTransferKeepsNamedPorts() {
        val source = VisualTaskerBuiltInAssets.all.first { it.identity.assetId == "vt.block.statement" }
        val selected = source.design.allNodes().single()

        val transferred = source.forVisualTaskerTransfer(listOf(selected))
        assertEquals("selection", transferred.metadata["transferScope"])
        assertTrue(transferred.design.metadata.ports.any { it.name == "previous" && it.portType == PortType.PREVIOUS })
        assertTrue(transferred.design.metadata.ports.any { it.name == "next" && it.portType == PortType.NEXT })
        assertEquals(2, transferred.design.allObjects().single().semantics.ports.size)
    }

    @Test
    fun wholeAssetTransferIsSelfContainedAndKeepsPorts() {
        val source = VisualTaskerBuiltInAssets.all.first { it.identity.assetId == "vt.block.reporter-any" }

        val transferred = source.forVisualTaskerTransfer(emptyList())

        assertEquals("asset", transferred.metadata["transferScope"])
        assertTrue(transferred.design.metadata.ports.any { it.name == "output" })
        assertEquals(listOf("default"), transferred.states.map { it.id })
        assertTrue(transferred.transitions.isEmpty())
        assertTrue(transferred.animations.isEmpty())
        assertTrue(transferred.sequences.isEmpty())
    }
}
