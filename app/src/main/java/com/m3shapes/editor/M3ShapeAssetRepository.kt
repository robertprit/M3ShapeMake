package com.m3shapes.editor

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface M3ShapeAssetRepository {
    suspend fun saveLastBundle(bundle: M3ShapeMakerAssetBundle)
    suspend fun loadLastBundle(): M3ShapeMakerAssetBundle?
}

class FileM3ShapeAssetRepository(
    private val context: Context
) : M3ShapeAssetRepository {

    private val file: File
        get() = File(context.filesDir, LAST_BUNDLE_FILENAME)

    override suspend fun saveLastBundle(bundle: M3ShapeMakerAssetBundle) = withContext(Dispatchers.IO) {
        val json = M3ShapeAssetCodec.encodeBundle(bundle)
        file.writeText(json)
    }

    override suspend fun loadLastBundle(): M3ShapeMakerAssetBundle? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext null
        M3ShapeAssetCodec.decodeBundle(file.readText())
    }

    companion object {
        const val LAST_BUNDLE_FILENAME = "last_m3shape_bundle.json"
    }
}
