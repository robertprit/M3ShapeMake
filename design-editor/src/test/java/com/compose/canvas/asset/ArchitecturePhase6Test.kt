package com.compose.canvas.asset

import com.compose.canvas.core.ObjectSemantics
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorLayer
import com.compose.canvas.core.VectorObject
import com.compose.canvas.geometry.EllipseGeometry
import com.compose.canvas.interchange.DocumentCodec
import com.compose.canvas.interchange.ImageVectorExport
import com.compose.canvas.interchange.VectorDrawableExport
import com.compose.canvas.interchange.VisualMetadataExport
import com.compose.canvas.metadata.ResizeMode
import com.compose.canvas.metadata.ResizeRules
import com.compose.canvas.runtime.VisualAssetRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitecturePhase6Test {
    private fun document(name: String = "Badge") = VectorDocument(
        name = name,
        layers = listOf(
            VectorLayer(
                name = "Design",
                objects = listOf(
                    VectorObject(
                        id = "circle",
                        name = "Circle",
                        geometry = EllipseGeometry(64f, 64f),
                        semantics = ObjectSemantics(
                            resizeRules = ResizeRules(
                                horizontal = ResizeMode.STRETCH,
                                vertical = ResizeMode.ANCHOR,
                                preserveAspectRatio = false,
                                minWidth = 24f
                            )
                        )
                    )
                )
            )
        )
    )

    private fun asset(version: Int = 1): VisualAsset {
        val start = document("Start")
        val end = document("End")
        return VisualAsset(
            identity = VisualAssetIdentity("badge", "Badge", version = version, tags = setOf("status", "demo")),
            design = start,
            states = listOf(VisualState("start", "Start", start), VisualState("end", "End", end)),
            transitions = listOf(
                ShapeMakerTransition(
                    id = "activate",
                    fromState = "start",
                    toState = "end",
                    durationMs = 400,
                    propertyOverrides = mapOf(MotionChannel.SCALE to MotionRange(1f, 1.2f))
                )
            ),
            animations = listOf(
                MotionSequence(
                    id = "pulse",
                    name = "Pulse",
                    tracks = listOf(
                        PropertyTrack(
                            id = "scale",
                            name = "Scale",
                            channel = MotionChannel.SCALE,
                            keyframes = listOf(
                                PropertyKeyframe("scale-0", 0, 1f),
                                PropertyKeyframe("scale-1", 400, 1.2f)
                            )
                        )
                    ),
                    events = listOf(VisualEvent("done", "Done", 400, mapOf("state" to "end")))
                )
            ),
            sequences = listOf(ShapeMakerSequence("main", "Main", listOf("activate"), listOf("pulse")))
        )
    }

    @Test
    fun resizeRulesAndTimelineRoundtripWithoutLoss() {
        val original = asset()
        val decoded = EmaCodec.decode(EmaCodec.encode(original))
        val document = DocumentCodec.deserialize(DocumentCodec.serialize(original.design))

        assertEquals(original.animations, decoded.animations)
        assertEquals(original.transitions, decoded.transitions)
        assertEquals(ResizeMode.STRETCH, document.allObjects().single().semantics.resizeRules.horizontal)
        assertFalse(document.allObjects().single().semantics.resizeRules.preserveAspectRatio)
    }

    @Test
    fun librarySearchesTagsAndKeepsVersions() {
        val library = InMemoryAssetLibrary()
        library.save(asset(1))
        library.save(asset(2))

        assertEquals(listOf(2, 1), library.versions("badge"))
        assertEquals(2, library.get("badge")?.identity?.version)
        assertEquals(2, library.search(AssetLibraryQuery(tags = setOf("status"))).size)
        assertTrue(library.search(AssetLibraryQuery(text = "missing")).isEmpty())
    }

    @Test
    fun runtimeControlsStateTransitionPropertiesTracksAndEvents() {
        val runtime = VisualAssetRuntime(asset())

        assertEquals("start", runtime.setState("start").stateId)
        assertEquals(1.1f, runtime.playTransition("activate", 0.5f).properties[MotionChannel.SCALE]!!, 0.001f)
        assertEquals(1.1f, runtime.playSequence("pulse", 200).properties[MotionChannel.SCALE]!!, 0.001f)
        assertEquals("done", runtime.playSequence("pulse", 400).events.single().id)
        assertEquals("end", runtime.playTransition("activate", 1f).stateId)
    }

    @Test
    fun additionalExportTargetsContainGeometryAndStateMetadata() {
        val document = asset().toShapeMakerDocument()

        val vector = VectorDrawableExport.export(document)
        val imageVector = ImageVectorExport.export(document)
        val metadata = VisualMetadataExport.export(document)

        assertTrue(vector.text.contains("android:pathData"))
        assertTrue(imageVector.text.contains("PathParser"))
        assertTrue(metadata.text.contains("activate"))
        assertTrue(metadata.text.contains("pulse"))
    }
}
