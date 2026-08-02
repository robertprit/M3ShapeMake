package com.m3shapes.editor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object M3ShapeAssetCodec {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    fun encodeBundle(bundle: M3ShapeMakerAssetBundle): String =
        json.encodeToString(bundle)

    fun decodeBundle(text: String): M3ShapeMakerAssetBundle {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Bundle JSON is empty")
        }
        val bundle = try {
            json.decodeFromString<M3ShapeMakerAssetBundle>(trimmed)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("Invalid bundle JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to parse bundle JSON: ${e.message}", e)
        }

        if (bundle.format != M3_SHAPE_ASSET_FORMAT) {
            throw IllegalArgumentException(
                "Unsupported format '${bundle.format}'. Expected '$M3_SHAPE_ASSET_FORMAT'."
            )
        }
        if (bundle.schemaVersion != M3_SHAPE_ASSET_SCHEMA_VERSION) {
            throw IllegalArgumentException(
                "Unsupported schemaVersion ${bundle.schemaVersion}. " +
                    "Expected $M3_SHAPE_ASSET_SCHEMA_VERSION."
            )
        }
        return bundle
    }
}
