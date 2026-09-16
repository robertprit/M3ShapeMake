package com.m3shapes.runtime.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.compose.canvas.core.FillStyle
import com.compose.canvas.core.ObjectTransform
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorLayer
import com.compose.canvas.core.VectorObject
import com.compose.canvas.asset.newVisualAsset
import com.compose.canvas.geometry.EllipseGeometry
import com.compose.canvas.renderer.VectorRenderer
import com.compose.canvas.runtime.VisualAssetRuntime
import com.compose.canvas.viewport.ViewportState

/** Separate consumer proving that the runtime API does not depend on the editor app module. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val document = VectorDocument(
            name = "Runtime Sample",
            layers = listOf(
                VectorLayer(
                    objects = listOf(
                        VectorObject(
                            name = "Circle",
                            geometry = EllipseGeometry(96f, 96f),
                            transform = ObjectTransform(x = 128f, y = 128f),
                            fill = FillStyle(color = Color(0xFF6750A4))
                        )
                    )
                )
            )
        )
        val runtime = VisualAssetRuntime(newVisualAsset(name = "Runtime Sample", document = document))
        val frame = runtime.setState("start")
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("ShapeMaker Runtime · state=${frame.stateId}")
                        Canvas(Modifier.fillMaxSize()) {
                            val viewport = ViewportState(screenWidth = size.width, screenHeight = size.height)
                                .fitToDocument(frame.document.canvas.width, frame.document.canvas.height)
                            VectorRenderer.drawDocument(this, frame.document, viewport, gridVisible = false)
                        }
                    }
                }
            }
        }
    }
}
