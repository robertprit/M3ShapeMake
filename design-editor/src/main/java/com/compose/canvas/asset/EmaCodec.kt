package com.compose.canvas.asset

import com.compose.canvas.interchange.DocumentCodec
import org.json.JSONArray
import org.json.JSONObject

data class EmaDiagnostic(
    val code: String,
    val message: String,
    val isError: Boolean
)

data class EmaValidationResult(
    val diagnostics: List<EmaDiagnostic>
) {
    val valid: Boolean get() = diagnostics.none { it.isError }
}

/** Versioned JSON codec for `.ema` (EMScript Motion Asset) files. */
object EmaCodec {
    fun encode(asset: VisualAsset): String = JSONObject().apply {
        put("format", EMA_FORMAT)
        put("schemaVersion", EMA_SCHEMA_VERSION)
        put("asset", JSONObject().apply {
            put("assetId", asset.identity.assetId)
            put("name", asset.identity.name)
            put("type", asset.identity.type.name)
            put("version", asset.identity.version)
            put("tags", JSONArray().apply { asset.identity.tags.sorted().forEach(::put) })
            put("design", JSONObject(DocumentCodec.serialize(asset.design)))
            put("states", JSONArray().apply {
                asset.states.forEach { state ->
                    put(JSONObject().apply {
                        put("id", state.id)
                        put("name", state.name)
                        put("document", JSONObject(DocumentCodec.serialize(state.document)))
                    })
                }
            })
            put("transitions", JSONArray().apply {
                asset.transitions.forEach { put(encodeTransition(it)) }
            })
            put("animations", JSONArray().apply { asset.animations.forEach { put(encodeSequence(it)) } })
            put("sequences", JSONArray().apply {
                asset.sequences.forEach { sequence ->
                    put(JSONObject().apply {
                        put("id", sequence.id)
                        put("name", sequence.name)
                        put("transitionIds", JSONArray().apply { sequence.transitionIds.forEach(::put) })
                        put("animationIds", JSONArray().apply { sequence.animationIds.forEach(::put) })
                    })
                }
            })
            put("metadata", JSONObject().apply { asset.metadata.forEach { (key, value) -> put(key, value) } })
        })
    }.toString(2)

    fun decode(text: String): VisualAsset {
        val root = JSONObject(text)
        require(root.optString("format") == EMA_FORMAT) {
            "Unsupported EMA format: ${root.optString("format")}"
        }
        require(root.optInt("schemaVersion", -1) == EMA_SCHEMA_VERSION) {
            "Unsupported EMA schema version: ${root.optInt("schemaVersion", -1)}"
        }
        val json = root.getJSONObject("asset")
        val identity = VisualAssetIdentity(
            assetId = json.getString("assetId"),
            name = json.getString("name"),
            type = enumValueOf(json.optString("type", VisualAssetType.ANIMATED_SHAPE.name)),
            version = json.optInt("version", 1),
            tags = json.optJSONArray("tags").strings().toSet()
        )
        val states = json.optJSONArray("states").objects().map { state ->
            VisualState(
                id = state.getString("id"),
                name = state.getString("name"),
                document = DocumentCodec.deserialize(state.getJSONObject("document").toString())
            )
        }
        val metadata = buildMap {
            json.optJSONObject("metadata")?.let { objectJson ->
                val keys = objectJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, objectJson.optString(key))
                }
            }
        }
        return VisualAsset(
            identity = identity,
            design = DocumentCodec.deserialize(json.getJSONObject("design").toString()),
            states = states,
            transitions = json.optJSONArray("transitions").objects().map(::decodeTransition),
            animations = json.optJSONArray("animations").objects().map(::decodeSequence),
            sequences = json.optJSONArray("sequences").objects().map { sequence ->
                ShapeMakerSequence(
                    id = sequence.getString("id"),
                    name = sequence.optString("name", "Sequence"),
                    transitionIds = sequence.optJSONArray("transitionIds").strings(),
                    animationIds = sequence.optJSONArray("animationIds").strings()
                )
            },
            metadata = metadata
        ).also { asset ->
            val validation = validate(asset)
            require(validation.valid) {
                validation.diagnostics.filter { it.isError }.joinToString("; ") { "${it.code}: ${it.message}" }
            }
        }
    }

    fun validate(asset: VisualAsset): EmaValidationResult {
        val diagnostics = mutableListOf<EmaDiagnostic>()
        fun error(code: String, message: String) {
            diagnostics += EmaDiagnostic(code, message, isError = true)
        }
        fun warning(code: String, message: String) {
            diagnostics += EmaDiagnostic(code, message, isError = false)
        }

        if (asset.identity.assetId.isBlank()) error("ASSET_ID", "assetId must not be blank")
        if (asset.identity.name.isBlank()) error("ASSET_NAME", "name must not be blank")
        if (asset.identity.version < 1) error("ASSET_VERSION", "version must be at least 1")
        val duplicateStates = asset.states.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
        duplicateStates.forEach { error("STATE_ID", "Duplicate state id: $it") }
        val stateIds = asset.states.mapTo(mutableSetOf()) { it.id }
        val duplicateTransitions = asset.transitions.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
        duplicateTransitions.forEach { error("TRANSITION_ID", "Duplicate transition id: $it") }
        asset.transitions.forEach { transition ->
            if (transition.fromState !in stateIds) {
                error("TRANSITION_FROM", "Transition ${transition.id} references missing state ${transition.fromState}")
            }
            if (transition.toState !in stateIds) {
                error("TRANSITION_TO", "Transition ${transition.id} references missing state ${transition.toState}")
            }
            if (transition.durationMs <= 0 || transition.delayMs < 0) {
                error("TRANSITION_TIMING", "Transition ${transition.id} has invalid timing")
            }
        }
        val transitionIds = asset.transitions.mapTo(mutableSetOf()) { it.id }
        val animationIds = asset.animations.mapTo(mutableSetOf()) { it.id }
        asset.sequences.forEach { sequence ->
            sequence.transitionIds.filterNot(transitionIds::contains).forEach {
                error("SEQUENCE_TRANSITION", "Sequence ${sequence.id} references missing transition $it")
            }
            sequence.animationIds.filterNot(animationIds::contains).forEach {
                error("SEQUENCE_ANIMATION", "Sequence ${sequence.id} references missing animation $it")
            }
        }
        if (asset.state("start") == null) warning("MORPH_START", "No named start state is defined")
        if (asset.state("end") == null) warning("MORPH_END", "No named end state is defined")
        asset.animations.flatMap { it.steps }.forEach { step ->
            if (step.id.isBlank()) error("STEP_ID", "Animation step id must not be blank")
            if (step.startMs < 0 || step.endMs <= step.startMs) {
                error("STEP_TIMING", "Step ${step.id} has an invalid time range")
            }
            if (step.channels.isEmpty()) warning("STEP_CHANNELS", "Step ${step.id} has no channels")
            if (!step.springStiffness.isFinite() || !step.springDamping.isFinite() || !step.bounceGravity.isFinite()) {
                error("STEP_PHYSICS", "Step ${step.id} contains non-finite motion parameters")
            }
        }
        asset.animations.forEach { animation ->
            animation.tracks.forEach { track ->
                if (track.id.isBlank()) error("TRACK_ID", "Property track id must not be blank")
                val duplicateKeyframes = track.keyframes.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
                duplicateKeyframes.forEach { error("KEYFRAME_ID", "Duplicate keyframe id in ${track.id}: $it") }
                track.keyframes.forEach { keyframe ->
                    if (keyframe.timeMs < 0 || !keyframe.value.isFinite()) {
                        error("KEYFRAME_VALUE", "Keyframe ${keyframe.id} has invalid time or value")
                    }
                }
            }
            val duplicateEvents = animation.events.groupingBy { it.id }.eachCount().filterValues { it > 1 }.keys
            duplicateEvents.forEach { error("EVENT_ID", "Duplicate event id in ${animation.id}: $it") }
            animation.events.filter { it.timeMs < 0 }.forEach {
                error("EVENT_TIME", "Event ${it.id} has invalid time")
            }
        }
        return EmaValidationResult(diagnostics)
    }

    private fun encodeSequence(sequence: MotionSequence): JSONObject = JSONObject().apply {
        put("id", sequence.id)
        put("name", sequence.name)
        put("steps", JSONArray().apply { sequence.steps.forEach { put(encodeStep(it)) } })
        put("tracks", JSONArray().apply { sequence.tracks.forEach { put(encodeTrack(it)) } })
        put("events", JSONArray().apply { sequence.events.forEach { put(encodeEvent(it)) } })
    }

    private fun encodeTransition(transition: ShapeMakerTransition): JSONObject = JSONObject().apply {
        put("id", transition.id)
        put("fromState", transition.fromState)
        put("toState", transition.toState)
        put("durationMs", transition.durationMs)
        put("delayMs", transition.delayMs)
        put("easing", JSONObject().apply {
            put("x1", transition.easing.x1.toDouble())
            put("y1", transition.easing.y1.toDouble())
            put("x2", transition.easing.x2.toDouble())
            put("y2", transition.easing.y2.toDouble())
        })
        put("motionPreset", transition.motionPreset.name)
        put("interpolation", transition.interpolation.name)
        put("repeatMode", transition.repeatMode.name)
        put("bidirectional", transition.bidirectional)
        put("propertyOverrides", JSONObject().apply {
            transition.propertyOverrides.forEach { (channel, range) ->
                put(channel.name, JSONObject().apply {
                    put("start", range.start.toDouble())
                    put("end", range.end.toDouble())
                })
            }
        })
    }

    private fun decodeTransition(json: JSONObject): ShapeMakerTransition {
        val easing = json.optJSONObject("easing") ?: JSONObject()
        val ranges = buildMap {
            json.optJSONObject("propertyOverrides")?.let { values ->
                val keys = values.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val channel = runCatching { enumValueOf<MotionChannel>(key) }.getOrNull() ?: continue
                    val value = values.optJSONObject(key) ?: continue
                    put(channel, MotionRange(value.optDouble("start").toFloat(), value.optDouble("end").toFloat()))
                }
            }
        }
        return ShapeMakerTransition(
            id = json.getString("id"),
            fromState = json.getString("fromState"),
            toState = json.getString("toState"),
            durationMs = json.optInt("durationMs", 300),
            delayMs = json.optInt("delayMs", 0),
            easing = CubicBezier(
                easing.optDouble("x1", 0.42).toFloat(),
                easing.optDouble("y1", 0.0).toFloat(),
                easing.optDouble("x2", 0.58).toFloat(),
                easing.optDouble("y2", 1.0).toFloat()
            ),
            motionPreset = runCatching {
                enumValueOf<ShapeMakerMotionPreset>(json.optString("motionPreset"))
            }.getOrDefault(ShapeMakerMotionPreset.Standard),
            interpolation = runCatching {
                enumValueOf<MotionInterpolation>(json.optString("interpolation"))
            }.getOrDefault(MotionInterpolation.TWEEN),
            repeatMode = runCatching {
                enumValueOf<ShapeMakerRepeatMode>(json.optString("repeatMode"))
            }.getOrDefault(ShapeMakerRepeatMode.None),
            bidirectional = json.optBoolean("bidirectional", false),
            propertyOverrides = ranges
        )
    }

    private fun decodeSequence(json: JSONObject): MotionSequence = MotionSequence(
        id = json.getString("id"),
        name = json.optString("name", "Animation"),
        steps = json.optJSONArray("steps").objects().map(::decodeStep),
        tracks = json.optJSONArray("tracks").objects().map(::decodeTrack),
        events = json.optJSONArray("events").objects().map(::decodeEvent)
    )

    private fun encodeTrack(track: PropertyTrack): JSONObject = JSONObject().apply {
        put("id", track.id)
        put("name", track.name)
        put("channel", track.channel.name)
        track.targetObjectId?.let { put("targetObjectId", it) }
        put("keyframes", JSONArray().apply {
            track.keyframes.sortedBy { it.timeMs }.forEach { keyframe ->
                put(JSONObject().apply {
                    put("id", keyframe.id)
                    put("timeMs", keyframe.timeMs)
                    put("value", keyframe.value.toDouble())
                    put("easing", JSONObject().apply {
                        put("x1", keyframe.easing.x1.toDouble())
                        put("y1", keyframe.easing.y1.toDouble())
                        put("x2", keyframe.easing.x2.toDouble())
                        put("y2", keyframe.easing.y2.toDouble())
                    })
                })
            }
        })
    }

    private fun decodeTrack(json: JSONObject): PropertyTrack = PropertyTrack(
        id = json.getString("id"),
        name = json.optString("name", "Track"),
        channel = enumValueOf(json.getString("channel")),
        targetObjectId = json.optString("targetObjectId").takeIf(String::isNotBlank),
        keyframes = json.optJSONArray("keyframes").objects().map { keyframe ->
            val easing = keyframe.optJSONObject("easing") ?: JSONObject()
            PropertyKeyframe(
                id = keyframe.getString("id"),
                timeMs = keyframe.getInt("timeMs"),
                value = keyframe.getDouble("value").toFloat(),
                easing = CubicBezier(
                    easing.optDouble("x1", 0.42).toFloat(),
                    easing.optDouble("y1", 0.0).toFloat(),
                    easing.optDouble("x2", 0.58).toFloat(),
                    easing.optDouble("y2", 1.0).toFloat()
                )
            )
        }
    )

    private fun encodeEvent(event: VisualEvent): JSONObject = JSONObject().apply {
        put("id", event.id)
        put("name", event.name)
        put("timeMs", event.timeMs)
        put("payload", JSONObject().apply { event.payload.forEach { (key, value) -> put(key, value) } })
    }

    private fun decodeEvent(json: JSONObject): VisualEvent {
        val payload = mutableMapOf<String, String>()
        json.optJSONObject("payload")?.let { values ->
            val keys = values.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                payload[key] = values.optString(key)
            }
        }
        return VisualEvent(
            id = json.getString("id"),
            name = json.optString("name", "Event"),
            timeMs = json.getInt("timeMs"),
            payload = payload
        )
    }

    private fun encodeStep(step: MotionStep): JSONObject = JSONObject().apply {
        put("id", step.id)
        put("name", step.name)
        put("channels", JSONArray().apply { step.channels.sortedBy { it.name }.forEach { put(it.name) } })
        put("startMs", step.startMs)
        put("endMs", step.endMs)
        put("easing", JSONObject().apply {
            put("x1", step.easing.x1.toDouble())
            put("y1", step.easing.y1.toDouble())
            put("x2", step.easing.x2.toDouble())
            put("y2", step.easing.y2.toDouble())
        })
        put("interpolation", step.interpolation.name)
        put("reverse", step.reverse)
        put("ranges", JSONObject().apply {
            step.ranges.forEach { (channel, range) ->
                put(channel.name, JSONObject().apply {
                    put("start", range.start.toDouble())
                    put("end", range.end.toDouble())
                })
            }
        })
        put("springStiffness", step.springStiffness.toDouble())
        put("springDamping", step.springDamping.toDouble())
        put("bounceGravity", step.bounceGravity.toDouble())
        put("holdMs", step.holdMs)
    }

    private fun decodeStep(json: JSONObject): MotionStep {
        val easing = json.optJSONObject("easing") ?: JSONObject()
        val ranges = buildMap {
            json.optJSONObject("ranges")?.let { rangeJson ->
                val keys = rangeJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val channel = runCatching { enumValueOf<MotionChannel>(key) }.getOrNull() ?: continue
                    val value = rangeJson.getJSONObject(key)
                    put(channel, MotionRange(value.getDouble("start").toFloat(), value.getDouble("end").toFloat()))
                }
            }
        }
        return MotionStep(
            id = json.getString("id"),
            name = json.optString("name", "Step"),
            channels = json.optJSONArray("channels").strings().mapNotNull {
                runCatching { enumValueOf<MotionChannel>(it) }.getOrNull()
            }.toSet(),
            startMs = json.getInt("startMs"),
            endMs = json.getInt("endMs"),
            easing = CubicBezier(
                easing.optDouble("x1", 0.42).toFloat(),
                easing.optDouble("y1", 0.0).toFloat(),
                easing.optDouble("x2", 0.58).toFloat(),
                easing.optDouble("y2", 1.0).toFloat()
            ),
            interpolation = enumValueOf(json.optString("interpolation", MotionInterpolation.TWEEN.name)),
            reverse = json.optBoolean("reverse", false),
            ranges = ranges,
            springStiffness = json.optDouble("springStiffness", 400.0).toFloat(),
            springDamping = json.optDouble("springDamping", 0.75).toFloat(),
            bounceGravity = json.optDouble("bounceGravity", 1.0).toFloat(),
            holdMs = json.optInt("holdMs", 0)
        )
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }
}
