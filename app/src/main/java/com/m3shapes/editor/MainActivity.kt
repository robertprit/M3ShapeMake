package com.m3shapes.editor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import com.compose.canvas.asset.EmaCodec
import com.compose.canvas.asset.VisualAsset
import com.compose.canvas.asset.VisualAssetType
import com.compose.canvas.ui.theme.EditorTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestedAssetType = intent
            .takeIf { it.action == ShapeMakerBridgeContract.ACTION_CREATE_VISUAL_ASSET }
            ?.getStringExtra(ShapeMakerBridgeContract.EXTRA_REQUESTED_ASSET_TYPE)
            ?.let { value -> VisualAssetType.entries.firstOrNull { it.name == value } }
        setContent {
            val systemDarkMode = isSystemInDarkTheme()
            var darkMode by rememberSaveable { mutableStateOf(systemDarkMode) }
            EditorTheme(darkMode = darkMode) {
                Surface(Modifier.fillMaxSize()) {
                    VisualAssetStudioApp(
                        requestedAssetType = requestedAssetType,
                        onReturnAsset = ::deliverVisualAsset,
                        darkMode = darkMode,
                        onToggleDarkMode = { darkMode = !darkMode },
                    )
                }
            }
        }
    }

    private fun deliverVisualAsset(asset: VisualAsset) {
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
        val result = Intent().apply {
            data = uri
            type = "application/vnd.emscript.motion+json"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(ShapeMakerBridgeContract.EXTRA_RESULT_ASSET_ID, asset.identity.assetId)
        }
        if (intent.action == ShapeMakerBridgeContract.ACTION_CREATE_VISUAL_ASSET) {
            setResult(Activity.RESULT_OK, result)
            finish()
            return
        }
        grantUriPermission(
            ShapeMakerBridgeContract.VISUALTASKER_PACKAGE,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        startActivity(
            Intent(ShapeMakerBridgeContract.ACTION_IMPORT_VISUAL_ASSET)
                .setClassName(
                    ShapeMakerBridgeContract.VISUALTASKER_PACKAGE,
                    ShapeMakerBridgeContract.VISUALTASKER_ACTIVITY,
                )
                .setDataAndType(uri, "application/vnd.emscript.motion+json")
                .addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
                .putExtra(ShapeMakerBridgeContract.EXTRA_RESULT_ASSET_ID, asset.identity.assetId),
        )
    }
}
