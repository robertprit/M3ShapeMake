package com.m3shapes.editor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import com.compose.canvas.asset.EmaCodec
import com.compose.canvas.asset.VisualAsset
import com.compose.canvas.asset.VisualAssetType
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestedAssetType = intent
            .takeIf { it.action == ShapeMakerBridgeContract.ACTION_CREATE_VISUAL_ASSET }
            ?.getStringExtra(ShapeMakerBridgeContract.EXTRA_REQUESTED_ASSET_TYPE)
            ?.let { value -> VisualAssetType.entries.firstOrNull { it.name == value } }
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF6750A4),
                    primaryContainer = Color(0xFFEADDFF)
                )
            ) {
                Surface(Modifier.fillMaxSize()) {
                    VisualAssetStudioApp(
                        requestedAssetType = requestedAssetType,
                        onReturnAsset = if (requestedAssetType == null) null else ::returnVisualAsset,
                    )
                }
            }
        }
    }

    private fun returnVisualAsset(asset: VisualAsset) {
        val exportDirectory = File(cacheDir, "visual-asset-exports").apply { mkdirs() }
        val stableName = asset.identity.assetId
            .lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-', '.', '_')
            .ifBlank { "visual-asset" }
        val file = File(exportDirectory, "$stableName-v${asset.identity.version}.ema")
        file.writeText(EmaCodec.encode(asset))
        val uri = FileProvider.getUriForFile(
            this,
            packageName + ShapeMakerBridgeContract.FILE_PROVIDER_AUTHORITY_SUFFIX,
            file,
        )
        setResult(
            Activity.RESULT_OK,
            Intent().apply {
                data = uri
                type = "application/vnd.emscript.motion+json"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(ShapeMakerBridgeContract.EXTRA_RESULT_ASSET_ID, asset.identity.assetId)
            },
        )
        finish()
    }
}
