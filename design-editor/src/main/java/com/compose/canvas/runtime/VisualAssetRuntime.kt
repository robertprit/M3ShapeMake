package com.compose.canvas.runtime

import com.compose.canvas.asset.MotionChannel
import com.compose.canvas.asset.ShapeMakerTransition
import com.compose.canvas.asset.VisualAsset
import com.compose.canvas.asset.VisualEvent
import com.compose.canvas.core.VectorDocument

data class RuntimeFrame(
    val stateId: String,
    val document: VectorDocument,
    val properties: Map<MotionChannel, Float> = emptyMap(),
    val events: List<VisualEvent> = emptyList()
)

/** Host-independent control surface. It owns no Android or Compose state. */
class VisualAssetRuntime(private val asset: VisualAsset) {
    private var stateId: String = asset.states.firstOrNull()?.id.orEmpty()
    private val properties = mutableMapOf<MotionChannel, Float>()

    fun setState(id: String): RuntimeFrame {
        val state = requireNotNull(asset.state(id)) { "Unknown visual state: $id" }
        stateId = state.id
        return frame(document = state.document)
    }

    fun setProperty(channel: MotionChannel, value: Float): RuntimeFrame {
        require(value.isFinite()) { "Property values must be finite" }
        properties[channel] = value
        return frame()
    }

    fun playTransition(id: String, progress: Float): RuntimeFrame {
        val transition = requireNotNull(asset.transitions.firstOrNull { it.id == id }) {
            "Unknown transition: $id"
        }
        val normalized = progress.coerceIn(0f, 1f)
        transition.propertyOverrides.forEach { (channel, range) ->
            properties[channel] = range.start + (range.end - range.start) * normalized
        }
        if (normalized >= 1f) stateId = transition.toState
        return frame(document = documentFor(transition, normalized))
    }

    fun playSequence(id: String, timeMs: Int): RuntimeFrame {
        val orchestration = asset.sequences.firstOrNull { it.id == id }
        val animationId = orchestration?.animationIds?.firstOrNull() ?: id
        val sequence = requireNotNull(asset.animations.firstOrNull { it.id == animationId }) {
            "Unknown animation: $id"
        }
        orchestration?.transitionIds?.firstOrNull()?.let { transitionId ->
            val transition = asset.transitions.firstOrNull { it.id == transitionId }
            if (transition != null) {
                val progress = timeMs.toFloat() / transition.durationMs.coerceAtLeast(1)
                playTransition(transition.id, progress)
            }
        }
        sequence.tracks.forEach { track -> track.valueAt(timeMs)?.let { properties[track.channel] = it } }
        val events = sequence.events.filter { it.timeMs == timeMs }
        return frame(events = events)
    }

    fun snapshot(): RuntimeFrame = frame()

    private fun documentFor(transition: ShapeMakerTransition, progress: Float): VectorDocument =
        asset.state(if (progress < 1f) transition.fromState else transition.toState)?.document ?: asset.design

    private fun frame(
        document: VectorDocument = asset.state(stateId)?.document ?: asset.design,
        events: List<VisualEvent> = emptyList()
    ) = RuntimeFrame(stateId, document, properties.toMap(), events)
}
