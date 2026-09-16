package com.compose.canvas.math

import androidx.compose.ui.geometry.Offset
import com.compose.canvas.viewport.ViewportState
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewportTransformsTest {

    @Test
    fun `document to screen and back preserves coordinates`() {
        val viewport = ViewportState(zoom = 2.5f, panX = 120f, panY = -40f)
        val docPt = Offset(300f, 500f)

        val screen = viewport.documentToScreen(docPt)
        val restored = viewport.screenToDocument(screen)

        assertEquals(docPt.x, restored.x, 0.001f)
        assertEquals(docPt.y, restored.y, 0.001f)
    }

    @Test
    fun `zoom at point keeps document point under cursor`() {
        val viewport = ViewportState(zoom = 1f, panX = 0f, panY = 0f)
        val cursorScreen = Offset(200f, 150f)

        val zoomed = viewport.zoomAtPoint(2f, cursorScreen.x, cursorScreen.y)

        val docBefore = viewport.screenToDocument(cursorScreen)
        val docAfter = zoomed.screenToDocument(cursorScreen)

        assertEquals(docBefore.x, docAfter.x, 0.001f)
        assertEquals(docBefore.y, docAfter.y, 0.001f)
        assertEquals(2f, zoomed.zoom, 0.001f)
    }

    @Test
    fun `pan does not change document coordinates`() {
        val viewport = ViewportState(zoom = 1f, panX = 0f, panY = 0f)
        val shapeX = 250f
        val shapeY = 180f

        val panned = viewport.panBy(50f, -30f)

        // panBy(+50px,+...) shifts the document right: docPoint appears 50px further right
        val screenAfter = panned.documentToScreen(shapeX, shapeY)
        val screenBefore = viewport.documentToScreen(shapeX, shapeY)
        assertEquals(screenBefore.x + 50f, screenAfter.x, 0.001f)
        assertEquals(screenBefore.y - 30f, screenAfter.y, 0.001f)

        val restored = panned.screenToDocument(screenAfter)
        assertEquals(shapeX, restored.x, 0.001f)
        assertEquals(shapeY, restored.y, 0.001f)
    }

    @Test
    fun `zoom clamps to bounds`() {
        val viewport = ViewportState(zoom = 1f)
        val zoomedOut = viewport.zoomAtPoint(0.001f, 100f, 100f)
        val zoomedIn = zoomedOut.zoomAtPoint(999f, 100f, 100f)

        assertEquals(ViewportState.MIN_ZOOM, zoomedOut.zoom, 0.0001f)
        assertEquals(ViewportState.MAX_ZOOM, zoomedIn.zoom, 0.0001f)
    }

    @Test
    fun `fit to document sets full visibility`() {
        val viewport = ViewportState(
            zoom = 1f, panX = 0f, panY = 0f,
            screenWidth = 1000f, screenHeight = 800f
        )
        val fitted = viewport.fitToDocument(500f, 400f)

        val docOriginScreen = fitted.documentToScreen(0f, 0f)
        val docCornerScreen = fitted.documentToScreen(500f, 400f)

        // zoom = min(920/500, 720/400) = 1.8; doc is 900x720 px on 1000x800 screen
        assertEquals(50f, docOriginScreen.x, 0.5f)
        assertEquals(40f, docOriginScreen.y, 0.5f)
        assertEquals(950f, docCornerScreen.x, 0.5f)
        assertEquals(760f, docCornerScreen.y, 0.5f)

        // Round trip keeps document coordinates
        val restored = fitted.screenToDocument(docOriginScreen)
        assertEquals(0f, restored.x, 0.001f)
        assertEquals(0f, restored.y, 0.001f)
    }
}