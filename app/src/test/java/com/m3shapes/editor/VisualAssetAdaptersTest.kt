package com.m3shapes.editor

import com.compose.canvas.asset.MotionChannel
import com.compose.canvas.asset.MotionInterpolation
import com.compose.canvas.asset.newVisualAsset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualAssetAdaptersTest {
    @Test
    fun visualAssetDescriptorExposesStableWssBridgeFields() {
        val asset = newVisualAsset(
            assetId = "block-alert",
            name = "Alert Block",
        ).copy(
            identity = newVisualAsset("block-alert", "Alert Block")
                .identity
                .copy(tags = setOf("block", "warning"))
        )

        val descriptor = asset.toWssDescriptor()

        assertEquals("block-alert", descriptor.assetId)
        assertEquals("Alert Block", descriptor.name)
        assertEquals("ANIMATED_SHAPE", descriptor.type)
        assertEquals(3, descriptor.stateCount)
        assertEquals(0, descriptor.animationCount)
        assertEquals("application/vnd.emscript.motion+json", descriptor.interchangeFormat)
        assertTrue("block" in descriptor.tags)
    }

    @Test
    fun animationStepRoundtripPreservesCurrentSequencerFields() {
        val source = AnimationStep(
            id = "step-7",
            name = "Move and morph",
            enabledAnimations = setOf(AnimationKind.Morph, AnimationKind.Position),
            startMs = 250,
            endMs = 1_500,
            durationMs = 1_250,
            profile = BezierProfile(0.2f, -0.3f, 0.8f, 1.4f),
            reverse = true,
            morphRange = FloatRangeValue(0.1f, 0.9f),
            rotateRange = FloatRangeValue(10f, 270f),
            scaleRange = FloatRangeValue(0.7f, 1.4f),
            alphaRange = FloatRangeValue(0.2f, 0.8f),
            colorRange = FloatRangeValue(0.15f, 0.75f),
            offsetXRange = FloatRangeValue(-40f, 50f),
            offsetYRange = FloatRangeValue(-30f, 60f),
            staggerRange = FloatRangeValue(0.1f, 0.7f),
            strokeRange = FloatRangeValue(0.05f, 0.95f),
            blurRange = FloatRangeValue(2f, 18f),
            gradientRange = FloatRangeValue(0.2f, 0.9f),
            motionPath = MotionPathType.Spring,
            springStiffness = 850f,
            springDamping = 0.62f,
            bounceGravity = 1.4f,
            holdMs = 90
        )

        val canonical = source.toMotionStep()
        val restored = canonical.toAnimationStep()

        assertTrue(MotionChannel.MORPH in canonical.channels)
        assertTrue(MotionChannel.POSITION_X in canonical.channels)
        assertTrue(MotionChannel.POSITION_Y in canonical.channels)
        assertEquals(0.1f, canonical.ranges.getValue(MotionChannel.STAGGER).start)
        assertEquals(0.95f, canonical.ranges.getValue(MotionChannel.STROKE).end)
        assertEquals(18f, canonical.ranges.getValue(MotionChannel.BLUR).end)
        assertEquals(0.2f, canonical.ranges.getValue(MotionChannel.GRADIENT).start)
        assertEquals(MotionInterpolation.SPRING, canonical.interpolation)
        assertEquals(source, restored)

        val positionStep = source.copy(
            id = "step-8",
            name = "Position",
            enabledAnimations = setOf(AnimationKind.Position),
            startMs = 1_600,
            endMs = 2_200,
            durationMs = 600
        )
        val sequence = listOf(
            source.copy(enabledAnimations = setOf(AnimationKind.Morph)),
            positionStep
        ).toMotionSequence()
        assertEquals(3, sequence.tracks.size)
        assertTrue(sequence.tracks.all { it.keyframes.size == 2 })
        assertTrue(sequence.tracks.any { it.channel == MotionChannel.POSITION_Y })
    }
}
