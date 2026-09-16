package com.compose.canvas.asset

import com.compose.canvas.core.FillStyle
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorLayer
import com.compose.canvas.core.VectorObject
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import com.compose.canvas.geometry.PresetGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmaCodecTest {
    private val square = PathGeometry(
        listOf(
            PathSegment.MoveTo(0f, 0f),
            PathSegment.LineTo(100f, 0f),
            PathSegment.LineTo(100f, 100f),
            PathSegment.LineTo(0f, 100f),
            PathSegment.Close
        ),
        closed = true
    )

    private fun document(name: String, path: PathGeometry = square): VectorDocument = VectorDocument(
        name = name,
        layers = listOf(
            VectorLayer(
                name = "Layer 1",
                objects = listOf(
                    VectorObject(
                        name = "Material Square",
                        geometry = PresetGeometry(
                            presetId = "material3:Square",
                            width = 100f,
                            height = 100f,
                            definition = path
                        ),
                        fill = FillStyle()
                    )
                )
            )
        )
    )

    @Test
    fun emaRoundtripPreservesDesignStatesPresetAndAnimation() {
        val start = document("Start")
        val end = document("End", square.copy(segments = square.segments.map { segment ->
            when (segment) {
                is PathSegment.MoveTo -> segment.copy(x = segment.x + 10f)
                is PathSegment.LineTo -> segment.copy(x = segment.x + 10f)
                else -> segment
            }
        }))
        val asset = VisualAsset(
            identity = VisualAssetIdentity("shape-1", "Shape One", tags = setOf("m3", "demo")),
            design = start,
            states = listOf(VisualState("start", "Start", start), VisualState("end", "End", end)),
            transitions = listOf(
                ShapeMakerTransition(
                    id = "start-active",
                    fromState = "start",
                    toState = "end",
                    durationMs = 900,
                    motionPreset = ShapeMakerMotionPreset.Expressive,
                    bidirectional = true,
                    propertyOverrides = mapOf(MotionChannel.SCALE to MotionRange(0.9f, 1.1f))
                )
            ),
            animations = listOf(
                MotionSequence(
                    "main",
                    "Main",
                    listOf(
                        MotionStep(
                            id = "step-1",
                            name = "Morph",
                            channels = setOf(MotionChannel.MORPH, MotionChannel.ROTATE),
                            startMs = 0,
                            endMs = 900,
                            ranges = mapOf(MotionChannel.MORPH to MotionRange(0f, 1f))
                        )
                    )
                )
            ),
            sequences = listOf(
                ShapeMakerSequence(
                    id = "demo",
                    name = "Demo",
                    transitionIds = listOf("start-active"),
                    animationIds = listOf("main")
                )
            )
        )

        val decoded = EmaCodec.decode(EmaCodec.encode(asset))

        assertEquals(asset.identity, decoded.identity)
        assertEquals(asset.states.map { it.id }, decoded.states.map { it.id })
        assertEquals(asset.transitions, decoded.transitions)
        assertEquals(asset.animations, decoded.animations)
        assertEquals(asset.sequences, decoded.sequences)
        val preset = decoded.design.allObjects().single().geometry as PresetGeometry
        assertEquals("material3:Square", preset.presetId)
        assertEquals(square, preset.definition)
        assertTrue(EmaCodec.validate(decoded).valid)
    }

    @Test
    fun validationRejectsBrokenTransitionAndSequenceReferences() {
        val document = document("Start")
        val asset = VisualAsset(
            identity = VisualAssetIdentity("shape-1", "Shape One"),
            design = document,
            states = listOf(VisualState("start", "Start", document)),
            transitions = listOf(
                ShapeMakerTransition("broken", "start", "missing", durationMs = 0)
            ),
            sequences = listOf(
                ShapeMakerSequence(
                    id = "broken-sequence",
                    name = "Broken",
                    transitionIds = listOf("unknown"),
                    animationIds = listOf("unknown")
                )
            )
        )

        val codes = EmaCodec.validate(asset).diagnostics.filter { it.isError }.map { it.code }.toSet()

        assertTrue("TRANSITION_TO" in codes)
        assertTrue("TRANSITION_TIMING" in codes)
        assertTrue("SEQUENCE_TRANSITION" in codes)
        assertTrue("SEQUENCE_ANIMATION" in codes)
    }

    @Test
    fun pathMorphResamplesDifferentTopologies() {
        val compatible = PathMorpher.prepare(document("Start"), document("End"))
        assertTrue(compatible.compatible)
        assertEquals(square.segments.size, compatible.morph!!.pathAt(0.5f).segments.size)

        val triangle = PathGeometry(
            listOf(
                PathSegment.MoveTo(0f, 0f),
                PathSegment.LineTo(100f, 0f),
                PathSegment.LineTo(50f, 100f),
                PathSegment.Close
            ),
            closed = true
        )
        val resampled = PathMorpher.prepare(document("Start"), document("End", triangle))
        assertTrue(resampled.compatible)
        assertEquals(null, resampled.diagnostic)
        val mid = resampled.morph!!.pathAt(0.5f)
        assertEquals(resampled.morph!!.start.segments.size, resampled.morph!!.end.segments.size)
        assertEquals(resampled.morph!!.start.segments.size, mid.segments.size)
    }

    @Test
    fun pathMorphStaggerDelaysLaterSegments() {
        val start = PathGeometry(
            listOf(
                PathSegment.MoveTo(0f, 0f),
                PathSegment.LineTo(100f, 0f),
                PathSegment.LineTo(100f, 100f),
                PathSegment.LineTo(0f, 100f),
                PathSegment.Close
            ),
            closed = true
        )
        val end = PathGeometry(
            listOf(
                PathSegment.MoveTo(0f, 0f),
                PathSegment.LineTo(200f, 0f),
                PathSegment.LineTo(200f, 200f),
                PathSegment.LineTo(0f, 200f),
                PathSegment.Close
            ),
            closed = true
        )
        val morph = PreparedPathMorph(start, end)
        val staggered = morph.pathAt(0.25f, stagger = 0.8f)
        fun progress(from: Float, to: Float, current: Float): Float {
            val span = to - from
            return if (kotlin.math.abs(span) < 0.001f) 0f else (current - from) / span
        }
        val firstNow = staggered.segments[1] as PathSegment.LineTo
        val lastNow = staggered.segments[3] as PathSegment.LineTo
        assertTrue(progress(100f, 200f, firstNow.x) > progress(100f, 200f, lastNow.y))
    }
}
