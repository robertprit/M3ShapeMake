package com.compose.canvas.overlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.compose.canvas.core.BoundsCalculator
import com.compose.canvas.core.SceneNode
import com.compose.canvas.editor.HandlePosition
import com.compose.canvas.geometry.RectF
import com.compose.canvas.viewport.ViewportState

enum class HandleHitType {
    TopLeft, TopCenter, TopRight,
    MiddleLeft, MiddleRight,
    BottomLeft, BottomCenter, BottomRight,
    Rotate
}

/**
 * Selection chrome only. Handle size is constant in **screen pixels**.
 */
object SelectionRenderer {
    private val color = Color(0xFF1565C0)
    private const val HANDLE_SIZE = 18f
    private const val HIT_PADDING = 10f
    private const val ROTATE_OFFSET = 36f

    fun drawSelection(
        scope: DrawScope,
        nodes: List<SceneNode>,
        viewport: ViewportState,
        overrideBounds: RectF? = null
    ) {
        val bounds = overrideBounds ?: BoundsCalculator.ofAll(nodes) ?: return
        val tl = viewport.documentToScreen(bounds.left, bounds.top)
        val br = viewport.documentToScreen(bounds.right, bounds.bottom)
        val screenRect = Rect(tl, br)

        scope.drawRect(
            color = color.copy(alpha = 0.08f),
            topLeft = screenRect.topLeft,
            size = screenRect.size
        )
        scope.drawRect(
            color = color,
            topLeft = screenRect.topLeft,
            size = screenRect.size,
            style = Stroke(width = 2f)
        )
        handleCenters(screenRect).forEach { drawHandle(scope, it) }
        val rotate = rotateHandle(screenRect)
        scope.drawLine(
            color = color,
            start = Offset(screenRect.center.x, screenRect.top),
            end = rotate,
            strokeWidth = 2f
        )
        scope.drawCircle(color = Color.White, radius = HANDLE_SIZE / 2f, center = rotate)
        scope.drawCircle(color = color, radius = HANDLE_SIZE / 2f, center = rotate, style = Stroke(2f))
    }

    fun hitTestHandle(
        screenX: Float,
        screenY: Float,
        bounds: RectF,
        viewport: ViewportState
    ): HandleHitType? {
        val tl = viewport.documentToScreen(bounds.left, bounds.top)
        val br = viewport.documentToScreen(bounds.right, bounds.bottom)
        val screenRect = Rect(tl, br)
        val half = HANDLE_SIZE / 2f + HIT_PADDING
        val rotate = rotateHandle(screenRect)
        if (kotlin.math.abs(screenX - rotate.x) <= half && kotlin.math.abs(screenY - rotate.y) <= half) {
            return HandleHitType.Rotate
        }
        val types = HandleHitType.entries.filter { it != HandleHitType.Rotate }
        handleCenters(screenRect).forEachIndexed { index, center ->
            if (kotlin.math.abs(screenX - center.x) <= half &&
                kotlin.math.abs(screenY - center.y) <= half
            ) {
                return types[index]
            }
        }
        return null
    }

    fun toHandlePosition(hit: HandleHitType): HandlePosition? = when (hit) {
        HandleHitType.TopLeft -> HandlePosition.TopLeft
        HandleHitType.TopCenter -> HandlePosition.TopCenter
        HandleHitType.TopRight -> HandlePosition.TopRight
        HandleHitType.MiddleLeft -> HandlePosition.MiddleLeft
        HandleHitType.MiddleRight -> HandlePosition.MiddleRight
        HandleHitType.BottomLeft -> HandlePosition.BottomLeft
        HandleHitType.BottomCenter -> HandlePosition.BottomCenter
        HandleHitType.BottomRight -> HandlePosition.BottomRight
        HandleHitType.Rotate -> HandlePosition.Rotate
    }

    private fun rotateHandle(rect: Rect) = Offset(rect.center.x, rect.top - ROTATE_OFFSET)

    private fun handleCenters(rect: Rect): List<Offset> = listOf(
        rect.topLeft,
        Offset(rect.center.x, rect.top),
        rect.topRight,
        Offset(rect.left, rect.center.y),
        Offset(rect.right, rect.center.y),
        rect.bottomLeft,
        Offset(rect.center.x, rect.bottom),
        rect.bottomRight
    )

    private fun drawHandle(scope: DrawScope, center: Offset) {
        val half = HANDLE_SIZE / 2f
        scope.drawRect(
            color = Color.White,
            topLeft = Offset(center.x - half, center.y - half),
            size = Size(HANDLE_SIZE, HANDLE_SIZE)
        )
        scope.drawRect(
            color = color,
            topLeft = Offset(center.x - half, center.y - half),
            size = Size(HANDLE_SIZE, HANDLE_SIZE),
            style = Stroke(width = 2f)
        )
    }
}
