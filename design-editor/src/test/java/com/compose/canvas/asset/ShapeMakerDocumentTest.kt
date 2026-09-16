package com.compose.canvas.asset

import com.compose.canvas.core.VectorDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeMakerDocumentTest {
    @Test
    fun codecRoundtripPreservesDocumentSections() {
        val base = newVisualAsset(
            assetId = "watchdog",
            name = "Watchdog",
            document = VectorDocument(name = "Watchdog")
        )
        val source = base.toShapeMakerDocument().copy(
            transitions = listOf(
                ShapeMakerTransition(
                    id = "idle-active",
                    fromState = "start",
                    toState = "end",
                    durationMs = 450,
                    delayMs = 20,
                    motionPreset = ShapeMakerMotionPreset.Expressive,
                    repeatMode = ShapeMakerRepeatMode.PingPong,
                    bidirectional = true,
                    propertyOverrides = mapOf(
                        MotionChannel.SCALE to MotionRange(0.9f, 1.1f),
                        MotionChannel.ALPHA to MotionRange(0.3f, 1f)
                    )
                )
            ),
            sequences = listOf(
                ShapeMakerSequence(
                    id = "main",
                    name = "Main",
                    transitionIds = listOf("idle-active"),
                    animationIds = listOf("main")
                )
            ),
            interactionMetadata = mapOf(
                "interaction.tap" to "setState:end"
            )
        )

        val encoded = ShapeMakerDocumentCodec.encode(source)
        val decoded = ShapeMakerDocumentCodec.decode(encoded)

        assertEquals(source.metadata, decoded.metadata)
        assertEquals(source.canvas, decoded.canvas)
        assertEquals(source.states.map { it.id }, decoded.states.map { it.id })
        assertEquals(source.transitions, decoded.transitions)
        assertEquals(source.sequences, decoded.sequences)
        assertEquals("setState:end", decoded.interactionMetadata["interaction.tap"])
    }

    @Test
    fun visualAssetBridgeAndExportersStayInteroperable() {
        val original = newVisualAsset(assetId = "badge", name = "Badge")
        val doc = original.toShapeMakerDocument().copy(
            transitions = listOf(
                ShapeMakerTransition(
                    id = "pulse",
                    fromState = "start",
                    toState = "end",
                    durationMs = 300
                )
            )
        )

        val bridged = doc.toVisualAsset()
        val restored = bridged.toShapeMakerDocument()
        val composePath = ShapeMakerComposePathExporter.export(doc)
        val svg = ShapeMakerSvgExporter.export(doc)

        assertEquals(doc.metadata.documentId, bridged.identity.assetId)
        assertEquals(doc.transitions, restored.transitions)
        assertTrue(composePath.text.contains("Path"))
        assertTrue(svg.text.contains("<svg"))
    }
}
