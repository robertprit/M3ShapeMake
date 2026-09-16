package com.compose.canvas.asset

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.compose.canvas.core.CanvasSettings
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.interchange.ComposePathExport
import com.compose.canvas.interchange.DocumentCodec
import com.compose.canvas.interchange.ExportResult
import com.compose.canvas.interchange.KotlinShapeExport
import com.compose.canvas.interchange.SvgExport
import com.compose.canvas.geometry.RectF
import org.json.JSONArray
import org.json.JSONObject

const val SHAPEMAKER_DOCUMENT_FORMAT = "shapemaker-document"
const val SHAPEMAKER_DOCUMENT_VERSION = 1

data class ShapeMakerMetadata(
    val documentId: String,
    val name: String,
    val version: Int = 1,
    val tags: Set<String> = emptySet()
)

data class ShapeMakerLayerSnapshot(
    val layerId: String,
    val name: String,
    val visible: Boolean,
    val locked: Boolean,
    val zOrder: Int
)

data class ShapeMakerAppearanceSnapshot(
    val objectId: String,
    val fillColorArgb: Int?,
    val fillAlpha: Float,
    val strokeColorArgb: Int?,
    val strokeAlpha: Float,
    val strokeWidth: Float
)

data class ShapeMakerAssets(
    val geometry: VectorDocument,
    val layers: List<ShapeMakerLayerSnapshot>,
    val appearance: List<ShapeMakerAppearanceSnapshot>,
    val content: Map<String, String> = emptyMap()
)

data class ShapeMakerState(
    val id: String,
    val name: String,
    val document: VectorDocument
)

enum class ShapeMakerMotionPreset {
    Standard,
    Expressive,
    Linear,
    Spring,
    Bounce,
    Custom
}

enum class ShapeMakerRepeatMode {
    None,
    Restart,
    Reverse,
    PingPong
}

data class ShapeMakerTransition(
    val id: String,
    val fromState: String,
    val toState: String,
    val durationMs: Int,
    val delayMs: Int = 0,
    val easing: CubicBezier = CubicBezier(),
    val motionPreset: ShapeMakerMotionPreset = ShapeMakerMotionPreset.Standard,
    val interpolation: MotionInterpolation = MotionInterpolation.TWEEN,
    val repeatMode: ShapeMakerRepeatMode = ShapeMakerRepeatMode.None,
    val bidirectional: Boolean = false,
    val propertyOverrides: Map<MotionChannel, MotionRange> = emptyMap()
)

data class ShapeMakerAnimation(
    val id: String,
    val name: String,
    val steps: List<MotionStep>,
    val tracks: List<PropertyTrack> = emptyList(),
    val events: List<VisualEvent> = emptyList()
)

data class ShapeMakerSequence(
    val id: String,
    val name: String,
    val transitionIds: List<String> = emptyList(),
    val animationIds: List<String> = emptyList()
)

data class ShapeMakerDocument(
    val metadata: ShapeMakerMetadata,
    val canvas: CanvasSettings,
    val assets: ShapeMakerAssets,
    val states: List<ShapeMakerState> = emptyList(),
    val transitions: List<ShapeMakerTransition> = emptyList(),
    val animations: List<ShapeMakerAnimation> = emptyList(),
    val sequences: List<ShapeMakerSequence> = emptyList(),
    val interactionMetadata: Map<String, String> = emptyMap()
)

interface ShapeMakerRenderer<T> {
    fun render(document: ShapeMakerDocument): T
}

interface ShapeMakerSerializer {
    val format: String
    fun encode(document: ShapeMakerDocument): String
    fun decode(text: String): ShapeMakerDocument
}

interface ShapeMakerExporter<T> {
    val target: String
    fun export(document: ShapeMakerDocument): T
}

object ShapeMakerLivePreviewRenderer : ShapeMakerRenderer<VectorDocument> {
    override fun render(document: ShapeMakerDocument): VectorDocument =
        document.assets.geometry.copy(
            name = document.metadata.name,
            canvas = document.canvas
        )
}

object ShapeMakerDocumentCodec : ShapeMakerSerializer {
    override val format: String = SHAPEMAKER_DOCUMENT_FORMAT

    override fun encode(document: ShapeMakerDocument): String = JSONObject().apply {
        put("format", SHAPEMAKER_DOCUMENT_FORMAT)
        put("version", SHAPEMAKER_DOCUMENT_VERSION)
        put("metadata", JSONObject().apply {
            put("documentId", document.metadata.documentId)
            put("name", document.metadata.name)
            put("version", document.metadata.version)
            put("tags", JSONArray().apply {
                document.metadata.tags.sorted().forEach(::put)
            })
        })
        put("canvas", JSONObject().apply {
            put("width", document.canvas.width.toDouble())
            put("height", document.canvas.height.toDouble())
            put("backgroundTransparent", document.canvas.backgroundTransparent)
            document.canvas.viewBox?.let { box ->
                put("viewBox", JSONObject().apply {
                    put("left", box.left.toDouble())
                    put("top", box.top.toDouble())
                    put("right", box.right.toDouble())
                    put("bottom", box.bottom.toDouble())
                })
            }
        })
        put("assets", JSONObject().apply {
            put("geometry", JSONObject(DocumentCodec.serialize(document.assets.geometry)))
            put("layers", JSONArray().apply {
                document.assets.layers.forEach { layer ->
                    put(JSONObject().apply {
                        put("layerId", layer.layerId)
                        put("name", layer.name)
                        put("visible", layer.visible)
                        put("locked", layer.locked)
                        put("zOrder", layer.zOrder)
                    })
                }
            })
            put("appearance", JSONArray().apply {
                document.assets.appearance.forEach { item ->
                    put(JSONObject().apply {
                        put("objectId", item.objectId)
                        put("fillColorArgb", item.fillColorArgb)
                        put("fillAlpha", item.fillAlpha.toDouble())
                        put("strokeColorArgb", item.strokeColorArgb)
                        put("strokeAlpha", item.strokeAlpha.toDouble())
                        put("strokeWidth", item.strokeWidth.toDouble())
                    })
                }
            })
            put("content", JSONObject().apply {
                document.assets.content.forEach { (k, v) -> put(k, v) }
            })
        })
        put("states", JSONArray().apply {
            document.states.forEach { state ->
                put(JSONObject().apply {
                    put("id", state.id)
                    put("name", state.name)
                    put("document", JSONObject(DocumentCodec.serialize(state.document)))
                })
            }
        })
        put("transitions", JSONArray().apply {
            document.transitions.forEach { transition ->
                put(encodeTransition(transition))
            }
        })
        put("animations", JSONArray().apply {
            document.animations.forEach { animation ->
                put(JSONObject().apply {
                    put("id", animation.id)
                    put("name", animation.name)
                    put("steps", JSONArray().apply {
                        animation.steps.forEach { put(encodeStep(it)) }
                    })
                    put("tracks", JSONArray().apply {
                        animation.tracks.forEach { track -> put(encodeTrack(track)) }
                    })
                    put("events", JSONArray().apply {
                        animation.events.forEach { event -> put(encodeEvent(event)) }
                    })
                })
            }
        })
        put("sequences", JSONArray().apply {
            document.sequences.forEach { sequence ->
                put(JSONObject().apply {
                    put("id", sequence.id)
                    put("name", sequence.name)
                    put("transitionIds", JSONArray().apply { sequence.transitionIds.forEach(::put) })
                    put("animationIds", JSONArray().apply { sequence.animationIds.forEach(::put) })
                })
            }
        })
        put("interactionMetadata", JSONObject().apply {
            document.interactionMetadata.forEach { (k, v) -> put(k, v) }
        })
    }.toString(2)

    override fun decode(text: String): ShapeMakerDocument {
        val root = JSONObject(text)
        require(root.optString("format") == SHAPEMAKER_DOCUMENT_FORMAT) {
            "Unsupported ShapeMakerDocument format: ${root.optString("format")}"
        }
        val version = root.optInt("version", -1)
        require(version in 1..SHAPEMAKER_DOCUMENT_VERSION) {
            "Unsupported ShapeMakerDocument version: $version"
        }
        val metadataJson = root.optJSONObject("metadata") ?: JSONObject()
        val canvasJson = root.optJSONObject("canvas") ?: JSONObject()
        val viewBox = canvasJson.optJSONObject("viewBox")?.let { box ->
            RectF(
                left = box.optDouble("left").toFloat(),
                top = box.optDouble("top").toFloat(),
                right = box.optDouble("right").toFloat(),
                bottom = box.optDouble("bottom").toFloat()
            )
        }
        val canvas = CanvasSettings(
            width = canvasJson.optDouble("width", 1000.0).toFloat(),
            height = canvasJson.optDouble("height", 1000.0).toFloat(),
            viewBox = viewBox,
            backgroundTransparent = canvasJson.optBoolean("backgroundTransparent", true)
        )
        val assetsJson = root.optJSONObject("assets") ?: JSONObject()
        val geometry = assetsJson.optJSONObject("geometry")?.toString()
            ?.let(DocumentCodec::deserialize)
            ?: VectorDocument(canvas = canvas)
        val layers = assetsJson.optJSONArray("layers").objects().mapIndexed { index, json ->
            ShapeMakerLayerSnapshot(
                layerId = json.optString("layerId"),
                name = json.optString("name", "Layer ${index + 1}"),
                visible = json.optBoolean("visible", true),
                locked = json.optBoolean("locked", false),
                zOrder = json.optInt("zOrder", index)
            )
        }
        val appearance = assetsJson.optJSONArray("appearance").objects().map { json ->
            ShapeMakerAppearanceSnapshot(
                objectId = json.optString("objectId"),
                fillColorArgb = json.optIntOrNull("fillColorArgb"),
                fillAlpha = json.optDouble("fillAlpha", 1.0).toFloat(),
                strokeColorArgb = json.optIntOrNull("strokeColorArgb"),
                strokeAlpha = json.optDouble("strokeAlpha", 1.0).toFloat(),
                strokeWidth = json.optDouble("strokeWidth", 1.0).toFloat()
            )
        }
        val content = assetsJson.optJSONObject("content").toStringMap()
        val states = root.optJSONArray("states").objects().map { state ->
            ShapeMakerState(
                id = state.optString("id"),
                name = state.optString("name", "State"),
                document = state.optJSONObject("document")?.toString()
                    ?.let(DocumentCodec::deserialize)
                    ?: geometry
            )
        }
        val transitions = root.optJSONArray("transitions").objects().map(::decodeTransition)
        val animations = root.optJSONArray("animations").objects().map { animation ->
            ShapeMakerAnimation(
                id = animation.optString("id"),
                name = animation.optString("name", "Animation"),
                steps = animation.optJSONArray("steps").objects().map(::decodeStep),
                tracks = animation.optJSONArray("tracks").objects().map(::decodeTrack),
                events = animation.optJSONArray("events").objects().map(::decodeEvent)
            )
        }
        val sequences = root.optJSONArray("sequences").objects().map { sequence ->
            ShapeMakerSequence(
                id = sequence.optString("id"),
                name = sequence.optString("name", "Sequence"),
                transitionIds = sequence.optJSONArray("transitionIds").strings(),
                animationIds = sequence.optJSONArray("animationIds").strings()
            )
        }
        return ShapeMakerDocument(
            metadata = ShapeMakerMetadata(
                documentId = metadataJson.optString("documentId", "asset-1"),
                name = metadataJson.optString("name", "Untitled"),
                version = metadataJson.optInt("version", 1),
                tags = metadataJson.optJSONArray("tags").strings().toSet()
            ),
            canvas = canvas,
            assets = ShapeMakerAssets(
                geometry = geometry,
                layers = layers.ifEmpty { geometry.layers.mapIndexed { index, layer ->
                    ShapeMakerLayerSnapshot(
                        layerId = layer.id,
                        name = layer.name,
                        visible = layer.visible,
                        locked = layer.locked,
                        zOrder = index
                    )
                } },
                appearance = appearance.ifEmpty {
                    geometry.layers.flatMap { it.leafObjects() }.map { obj ->
                        ShapeMakerAppearanceSnapshot(
                            objectId = obj.id,
                            fillColorArgb = obj.fill.color.toNullableArgb(),
                            fillAlpha = obj.fill.alpha,
                            strokeColorArgb = obj.stroke.color.toNullableArgb(),
                            strokeAlpha = obj.stroke.alpha,
                            strokeWidth = obj.stroke.width
                        )
                    }
                },
                content = content
            ),
            states = states,
            transitions = transitions,
            animations = animations,
            sequences = sequences,
            interactionMetadata = root.optJSONObject("interactionMetadata").toStringMap()
        )
    }
}

object ShapeMakerComposePathExporter : ShapeMakerExporter<ExportResult> {
    override val target: String = "compose-path"

    override fun export(document: ShapeMakerDocument): ExportResult =
        ComposePathExport.export(ShapeMakerLivePreviewRenderer.render(document), normalize = true)
}

object ShapeMakerSvgExporter : ShapeMakerExporter<ExportResult> {
    override val target: String = "svg"

    override fun export(document: ShapeMakerDocument): ExportResult =
        SvgExport.export(ShapeMakerLivePreviewRenderer.render(document))
}

object ShapeMakerKotlinShapeExporter : ShapeMakerExporter<ExportResult> {
    override val target: String = "kotlin-shape"

    override fun export(document: ShapeMakerDocument): ExportResult =
        KotlinShapeExport.export(ShapeMakerLivePreviewRenderer.render(document))
}

fun VisualAsset.toShapeMakerDocument(): ShapeMakerDocument {
    val geometry = design
    return ShapeMakerDocument(
        metadata = ShapeMakerMetadata(
            documentId = identity.assetId,
            name = identity.name,
            version = identity.version,
            tags = identity.tags
        ),
        canvas = geometry.canvas,
        assets = ShapeMakerAssets(
            geometry = geometry,
            layers = geometry.layers.mapIndexed { index, layer ->
                ShapeMakerLayerSnapshot(
                    layerId = layer.id,
                    name = layer.name,
                    visible = layer.visible,
                    locked = layer.locked,
                    zOrder = index
                )
            },
            appearance = geometry.layers.flatMap { it.leafObjects() }.map { obj ->
                ShapeMakerAppearanceSnapshot(
                    objectId = obj.id,
                    fillColorArgb = obj.fill.color.toNullableArgb(),
                    fillAlpha = obj.fill.alpha,
                    strokeColorArgb = obj.stroke.color.toNullableArgb(),
                    strokeAlpha = obj.stroke.alpha,
                    strokeWidth = obj.stroke.width
                )
            }
        ),
        states = states.map { ShapeMakerState(it.id, it.name, it.document) },
        transitions = transitions,
        animations = animations.map { sequence ->
            ShapeMakerAnimation(
                id = sequence.id,
                name = sequence.name,
                steps = sequence.steps,
                tracks = sequence.tracks,
                events = sequence.events
            )
        },
        sequences = sequences,
        interactionMetadata = metadata.filterKeys { it.startsWith("interaction.") }
    )
}

fun ShapeMakerDocument.toVisualAsset(type: VisualAssetType = VisualAssetType.ANIMATED_SHAPE): VisualAsset =
    VisualAsset(
        identity = VisualAssetIdentity(
            assetId = metadata.documentId,
            name = metadata.name,
            type = type,
            version = metadata.version,
            tags = metadata.tags
        ),
        design = ShapeMakerLivePreviewRenderer.render(this),
        states = states.map { VisualState(it.id, it.name, it.document) },
        transitions = transitions,
        animations = animations.map { MotionSequence(it.id, it.name, it.steps, it.tracks, it.events) },
        sequences = sequences,
        metadata = buildMap {
            putAll(assets.content)
            putAll(interactionMetadata)
        }
    )

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
        json.optJSONObject("propertyOverrides")?.let { rangeJson ->
            val keys = rangeJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val channel = runCatching { enumValueOf<MotionChannel>(key) }.getOrNull() ?: continue
                val range = rangeJson.optJSONObject(key) ?: continue
                put(
                    channel,
                    MotionRange(
                        start = range.optDouble("start").toFloat(),
                        end = range.optDouble("end").toFloat()
                    )
                )
            }
        }
    }
    return ShapeMakerTransition(
        id = json.optString("id"),
        fromState = json.optString("fromState"),
        toState = json.optString("toState"),
        durationMs = json.optInt("durationMs", 300),
        delayMs = json.optInt("delayMs", 0),
        easing = CubicBezier(
            x1 = easing.optDouble("x1", 0.42).toFloat(),
            y1 = easing.optDouble("y1", 0.0).toFloat(),
            x2 = easing.optDouble("x2", 0.58).toFloat(),
            y2 = easing.optDouble("y2", 1.0).toFloat()
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
    id = json.optString("id"),
    name = json.optString("name", "Track"),
    channel = runCatching { enumValueOf<MotionChannel>(json.optString("channel")) }
        .getOrDefault(MotionChannel.MORPH),
    targetObjectId = json.optString("targetObjectId").takeIf(String::isNotBlank),
    keyframes = json.optJSONArray("keyframes").objects().map { keyframe ->
        val easing = keyframe.optJSONObject("easing") ?: JSONObject()
        PropertyKeyframe(
            id = keyframe.optString("id"),
            timeMs = keyframe.optInt("timeMs"),
            value = keyframe.optDouble("value").toFloat(),
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
        id = json.optString("id"),
        name = json.optString("name", "Event"),
        timeMs = json.optInt("timeMs"),
        payload = payload
    )
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
                put(channel, MotionRange(value.optDouble("start").toFloat(), value.optDouble("end").toFloat()))
            }
        }
    }
    return MotionStep(
        id = json.optString("id"),
        name = json.optString("name", "Step"),
        channels = json.optJSONArray("channels").strings().mapNotNull {
            runCatching { enumValueOf<MotionChannel>(it) }.getOrNull()
        }.toSet(),
        startMs = json.optInt("startMs"),
        endMs = json.optInt("endMs"),
        easing = CubicBezier(
            easing.optDouble("x1", 0.42).toFloat(),
            easing.optDouble("y1", 0.0).toFloat(),
            easing.optDouble("x2", 0.58).toFloat(),
            easing.optDouble("y2", 1.0).toFloat()
        ),
        interpolation = runCatching {
            enumValueOf<MotionInterpolation>(json.optString("interpolation"))
        }.getOrDefault(MotionInterpolation.TWEEN),
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

private fun JSONObject?.toStringMap(): Map<String, String> {
    if (this == null) return emptyMap()
    val out = mutableMapOf<String, String>()
    val keys = keys()
    while (keys.hasNext()) {
        val key = keys.next()
        out[key] = optString(key)
    }
    return out
}

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (isNull(key) || !has(key)) null else optInt(key)

private fun Color.toNullableArgb(): Int? =
    if (this == Color.Unspecified) null else toArgb()
