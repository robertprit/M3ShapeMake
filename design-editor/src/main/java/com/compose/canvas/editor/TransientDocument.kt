package com.compose.canvas.editor

import com.compose.canvas.core.SceneNode
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.geometry.RectF

/**
 * Applies in-progress move/resize to a copy of the document for display only.
 * The stored document is unchanged until pointer-up commits one history entry.
 */
object TransientDocument {
    fun forDisplay(
        document: VectorDocument,
        selection: SelectionState,
        drag: DragState,
        resize: ResizeSession,
        rotate: RotateSession = RotateSession()
    ): VectorDocument {
        if (!drag.active && !resize.active && !rotate.active) return document
        return document.copy(
            layers = document.layers.map { layer ->
                layer.copy(
                    objects = layer.objects.map { node ->
                        applyTransient(node, selection, drag, resize, rotate)
                    }
                )
            }
        )
    }

    private fun applyTransient(
        node: SceneNode,
        selection: SelectionState,
        drag: DragState,
        resize: ResizeSession,
        rotate: RotateSession
    ): SceneNode {
        if (node.id !in selection.selectedIds || node.locked) return node
        if (resize.active) {
            val start = resize.startBounds ?: return node
            val current = resize.currentBounds ?: return node
            return node.withTransform(resizeTransform(node.transform, start, current))
        }
        if (rotate.active) {
            val delta = rotate.currentAngle - rotate.startAngle
            return node.withTransform(node.transform.copy(rotation = node.transform.rotation + delta))
        }
        if (drag.active) {
            val t = node.transform
            return node.withTransform(
                t.copy(x = t.x + drag.deltaDoc.x, y = t.y + drag.deltaDoc.y)
            )
        }
        return node
    }

    fun resizeTransform(
        transform: com.compose.canvas.core.ObjectTransform,
        oldBounds: RectF,
        newBounds: RectF
    ): com.compose.canvas.core.ObjectTransform {
        val scaleX = if (oldBounds.width < 0.01f) 1f else newBounds.width / oldBounds.width
        val scaleY = if (oldBounds.height < 0.01f) 1f else newBounds.height / oldBounds.height
        val localX = transform.x - oldBounds.left
        val localY = transform.y - oldBounds.top
        return transform.copy(
            x = newBounds.left + localX * scaleX,
            y = newBounds.top + localY * scaleY,
            scaleX = transform.scaleX * scaleX,
            scaleY = transform.scaleY * scaleY
        )
    }
}
