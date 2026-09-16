package com.compose.canvas.viewport

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class ViewportController {
    var state by mutableStateOf(ViewportState())
        private set

    fun setScreenSize(width: Float, height: Float) {
        if (state.screenWidth != width || state.screenHeight != height) {
            state = state.withScreenSize(width, height)
        }
    }

    fun zoomAt(newZoom: Float, screenX: Float, screenY: Float) {
        state = state.zoomAtPoint(newZoom, screenX, screenY)
    }

    fun panBy(screenDx: Float, screenDy: Float) {
        state = state.panBy(screenDx, screenDy)
    }

    fun fitToDocument(docWidth: Float, docHeight: Float) {
        state = state.fitToDocument(docWidth, docHeight)
    }

    fun resetTo100() {
        state = state.resetTo100()
    }
}