package com.compose.canvas.editor

import com.compose.canvas.core.SceneNode
import com.compose.canvas.core.GroupObject
import com.compose.canvas.core.WorldTransform
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
                        applyTransient(node, document, selection, drag, resize, rotate)
                    }
                )
            }
        )
    }

    private fun applyTransient(
        node: SceneNode,
        document: VectorDocument,
        selection: SelectionState,
        drag: DragState,
        resize: ResizeSession,
        rotate: RotateSession
    ): SceneNode {
        if (node.locked) return node
        if (node.id !in selection.selectedIds) {
            return if (node is GroupObject) {
                node.copy(children = node.children.map {
                    applyTransient(it, document, selection, drag, resize, rotate)
                })
            } else node
        }
        if (resize.active) {
            val start = resize.startBounds ?: return node
            val current = resize.currentBounds ?: return node
            return node.withTransform(resizeTransform(document, node, start, current))
        }
        if (rotate.active) {
            val delta = rotate.currentAngle - rotate.startAngle
            return node.withTransform(node.transform.copy(rotation = node.transform.rotation + delta))
        }
        if (drag.active) {
            val t = node.transform
            val parentInverse = WorldTransform.parentMatrixOf(document, node.id)?.invert()
            val delta = parentInverse?.transformVector(drag.deltaDoc) ?: drag.deltaDoc
            return node.withTransform(
                t.copy(x = t.x + delta.x, y = t.y + delta.y)
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

    fun resizeTransform(
        document: VectorDocument,
        node: SceneNode,
        oldWorldBounds: RectF,
        newWorldBounds: RectF
    ): com.compose.canvas.core.ObjectTransform {
        val parentInverse = WorldTransform.parentMatrixOf(document, node.id)?.invert()
            ?: return resizeTransform(node.transform, oldWorldBounds, newWorldBounds)
        return resizeTransform(
            node.transform,
            oldWorldBounds.transformBy(parentInverse),
            newWorldBounds.transformBy(parentInverse)
        )
    }

    private fun RectF.transformBy(matrix: com.compose.canvas.math.TransformMatrix): RectF {
        val points = listOf(
            matrix.transformPoint(left, top),
            matrix.transformPoint(right, top),
            matrix.transformPoint(left, bottom),
            matrix.transformPoint(right, bottom)
        )
        return RectF(
            left = points.minOf { it.x },
            top = points.minOf { it.y },
            right = points.maxOf { it.x },
            bottom = points.maxOf { it.y }
        )
    }
}
