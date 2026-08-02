package com.m3shapes.editor

import android.content.Context
import android.net.Uri
import java.io.IOException

fun defaultM3ShapeExportFilename(bundleId: String): String {
    val id = bundleId.trim()
    return if (id.isBlank()) "m3shapemaker_bundle.m3shape.json" else "$id.m3shape.json"
}

fun writeTextToUri(context: Context, uri: Uri, text: String): Result<Unit> {
    return try {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
        } ?: return Result.failure(IOException("Cannot open output stream for writing"))
        Result.success(Unit)
    } catch (e: SecurityException) {
        Result.failure(e)
    } catch (e: IOException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(IOException(e.message ?: "Write failed", e))
    }
}

fun readTextFromUri(context: Context, uri: Uri): Result<String> {
    return try {
        val text = context.contentResolver.openInputStream(uri)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: return Result.failure(IOException("Cannot open input stream for reading"))
        Result.success(text)
    } catch (e: SecurityException) {
        Result.failure(e)
    } catch (e: IOException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(IOException(e.message ?: "Read failed", e))
    }
}
