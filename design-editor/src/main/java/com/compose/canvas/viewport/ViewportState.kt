package com.compose.canvas.viewport

import androidx.compose.ui.geometry.Offset
import com.compose.canvas.geometry.RectF
import kotlin.math.max
import kotlin.math.min

data class ViewportState(
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val screenWidth: Float = 1080f,
    val screenHeight: Float = 1920f
) {
    companion object {
        const val MIN_ZOOM = 0.1f
        const val MAX_ZOOM = 20f
    }

    fun documentToScreen(docX: Float, docY: Float): Offset {
        val sx = (docX - panX) * zoom
        val sy = (docY - panY) * zoom
        return Offset(sx, sy)
    }

    fun documentToScreen(docPt: Offset): Offset = documentToScreen(docPt.x, docPt.y)

    fun screenToDocument(scrX: Float, scrY: Float): Offset {
        val dx = scrX / zoom + panX
        val dy = scrY / zoom + panY
        return Offset(dx, dy)
    }

    fun screenToDocument(scrPt: Offset): Offset = screenToDocument(scrPt.x, scrPt.y)

    fun documentRectToScreenRect(rect: RectF): RectF {
        val topLeft = documentToScreen(rect.left, rect.top)
        val bottomRight = documentToScreen(rect.right, rect.bottom)
        return RectF(
            left = min(topLeft.x, bottomRight.x),
            top = min(topLeft.y, bottomRight.y),
            right = max(topLeft.x, bottomRight.x),
            bottom = max(topLeft.y, bottomRight.y)
        )
    }

    fun clampZoom(z: Float): Float = z.coerceIn(MIN_ZOOM, MAX_ZOOM)

    fun zoomAtPoint(newZoom: Float, screenX: Float, screenY: Float): ViewportState {
        val clampedZoom = clampZoom(newZoom)
        val docBefore = screenToDocument(screenX, screenY)
        val result = copy(zoom = clampedZoom)
        val docAfter = result.screenToDocument(screenX, screenY)
        return result.copy(
            panX = result.panX + (docBefore.x - docAfter.x),
            panY = result.panY + (docBefore.y - docAfter.y)
        )
    }

    fun panBy(dxScreen: Float, dyScreen: Float): ViewportState {
        val dxDoc = dxScreen / zoom
        val dyDoc = dyScreen / zoom
        return copy(panX = panX - dxDoc, panY = panY - dyDoc)
    }

    fun fitToDocument(docWidth: Float, docHeight: Float): ViewportState {
        if (docWidth <= 0f || docHeight <= 0f) return this
        val padding = 40f
        val scaleX = (screenWidth - padding * 2) / docWidth
        val scaleY = (screenHeight - padding * 2) / docHeight
        val newZoom = clampZoom(min(scaleX, scaleY))
        return copy(
            zoom = newZoom,
            panX = -(screenWidth / newZoom - docWidth) / 2f,
            panY = -(screenHeight / newZoom - docHeight) / 2f
        )
    }

    fun resetTo100(): ViewportState = copy(zoom = 1f, panX = 0f, panY = 0f)

    fun visibleDocumentBounds(): RectF {
        val topLeft = screenToDocument(0f, 0f)
        val bottomRight = screenToDocument(screenWidth, screenHeight)
        return RectF(
            left = min(topLeft.x, bottomRight.x),
            top = min(topLeft.y, bottomRight.y),
            right = max(topLeft.x, bottomRight.x),
            bottom = max(topLeft.y, bottomRight.y)
        )
    }

    fun withScreenSize(width: Float, height: Float): ViewportState =
        copy(screenWidth = width, screenHeight = height)
}
