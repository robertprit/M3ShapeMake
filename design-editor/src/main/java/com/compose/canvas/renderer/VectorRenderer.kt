package com.compose.canvas.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import com.compose.canvas.core.CanvasSettings
import com.compose.canvas.core.FillKind
import com.compose.canvas.core.FillStyle
import com.compose.canvas.core.LinearGradientResource
import com.compose.canvas.core.RadialGradientResource
import com.compose.canvas.core.SceneNode
import com.compose.canvas.core.StrokeCap
import com.compose.canvas.core.StrokeJoin
import com.compose.canvas.core.StrokeStyle
import com.compose.canvas.core.VectorDocument
import com.compose.canvas.core.VectorObject
import com.compose.canvas.geometry.EllipseGeometry
import com.compose.canvas.geometry.LineGeometry
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.RectGeometry
import com.compose.canvas.math.TransformMatrix
import com.compose.canvas.math.TransformUtils
import com.compose.canvas.viewport.ViewportState

/**
 * Draws the document only. No selection, handles, snap guides, or tool previews.
 * Safe to reuse outside the editor (preview, export rasterization later).
 */
object VectorRenderer {

    fun drawDocument(
        scope: DrawScope,
        document: VectorDocument,
        viewport: ViewportState,
        gridVisible: Boolean = true,
        gridSize: Float = 50f
    ) {
        drawCanvasBackground(scope, document.canvas, viewport)
        if (gridVisible) {
            GridRenderer.draw(scope, document.canvas, viewport, gridSize)
        }

        val viewportMatrix = TransformMatrix(
            m00 = viewport.zoom,
            m11 = viewport.zoom,
            m02 = -viewport.panX * viewport.zoom,
            m12 = -viewport.panY * viewport.zoom
        )
        for (layer in document.layers) {
            if (!layer.visible) continue
            for (node in layer.objects) {
                if (!node.visible) continue
                drawNode(scope, node, document, viewportMatrix, TransformMatrix())
            }
        }
    }

    private fun drawNode(
        scope: DrawScope,
        node: SceneNode,
        document: VectorDocument,
        viewport: TransformMatrix,
        parentWorld: TransformMatrix
    ) {
        val world = TransformUtils.multiply(
            parentWorld,
            TransformUtils.fromObjectTransform(node.transform)
        )
        when (node) {
            is VectorObject -> drawObjectWithMatrix(
                scope,
                node,
                document,
                TransformUtils.multiply(viewport, world)
            )
            is com.compose.canvas.core.GroupObject -> {
                for (child in node.children) {
                    if (!child.visible) continue
                    drawNode(scope, child, document, viewport, world)
                }
            }
        }
    }

    private fun drawCanvasBackground(
        scope: DrawScope,
        canvas: CanvasSettings,
        viewport: ViewportState
    ) {
        val box = canvas.effectiveViewBox
        val topLeft = viewport.documentToScreen(box.left, box.top)
        val bottomRight = viewport.documentToScreen(box.right, box.bottom)
        val size = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y)

        if (!canvas.backgroundTransparent) {
            scope.drawRect(color = Color.White, topLeft = topLeft, size = size)
        } else {
            drawCheckerboard(scope, topLeft, size)
        }

        scope.drawRect(
            color = Color(0xFF9E9E9E),
            topLeft = topLeft,
            size = size,
            style = Stroke(width = 1f)
        )
    }

    private fun drawCheckerboard(scope: DrawScope, topLeft: Offset, size: Size) {
        val cell = 16f
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = 0f
            var col = 0
            while (x < size.width) {
                val dark = (row + col) % 2 == 0
                scope.drawRect(
                    color = if (dark) Color(0xFFF5F5F5) else Color(0xFFE0E0E0),
                    topLeft = Offset(topLeft.x + x, topLeft.y + y),
                    size = Size(
                        minOf(cell, size.width - x),
                        minOf(cell, size.height - y)
                    )
                )
                x += cell
                col++
            }
            y += cell
            row++
        }
    }

    fun drawObject(scope: DrawScope, obj: VectorObject, document: VectorDocument? = null) {
        drawObjectWithMatrix(
            scope,
            obj,
            document,
            TransformUtils.fromObjectTransform(obj.transform)
        )
    }

    private fun drawObjectWithMatrix(
        scope: DrawScope,
        obj: VectorObject,
        document: VectorDocument?,
        matrix: TransformMatrix
    ) {
        scope.withTransform({
            transform(matrix.toComposeMatrix())
        }) {
            when (val geom = obj.geometry) {
                is RectGeometry -> drawRect(this, geom, obj.fill, obj.stroke, document)
                is EllipseGeometry -> drawEllipse(this, geom, obj.fill, obj.stroke, document)
                is LineGeometry -> drawLine(this, geom, obj.stroke)
                is com.compose.canvas.geometry.TextGeometry -> drawText(this, geom, obj.fill)
                else -> {
                    val pathGeom = com.compose.canvas.geometry.GeometryToPath.toPath(geom, obj.semantics.parameters)
                    drawPath(this, pathGeom, obj.fill, obj.stroke, document)
                }
            }
        }
    }

    private fun TransformMatrix.toComposeMatrix(): Matrix = Matrix().also { result ->
        result[0, 0] = m00
        result[1, 0] = m01
        result[3, 0] = m02
        result[0, 1] = m10
        result[1, 1] = m11
        result[3, 1] = m12
        result[0, 3] = m20
        result[1, 3] = m21
        result[3, 3] = m22
    }

    private fun drawRect(
        scope: DrawScope,
        geom: RectGeometry,
        fill: FillStyle,
        stroke: StrokeStyle,
        document: VectorDocument?
    ) {
        val size = Size(geom.width, geom.height)
        val path = if (geom.cornerRadius > 0f) {
            Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect = Rect(Offset.Zero, size),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            geom.cornerRadius,
                            geom.cornerRadius
                        )
                    )
                )
            }
        } else null

        paintFill(scope, fill, document, com.compose.canvas.geometry.RectF(0f, 0f, geom.width, geom.height)) { color, brush ->
            if (brush != null) {
                if (path != null) scope.drawPath(path, brush = brush)
                else scope.drawRect(brush = brush, topLeft = Offset.Zero, size = size)
            } else if (color != null) {
                if (path != null) scope.drawPath(path, color = color)
                else scope.drawRect(color = color, topLeft = Offset.Zero, size = size)
            }
        }

        if (stroke.width > 0f && stroke.color != Color.Unspecified) {
            val style = stroke.toDrawStroke()
            val color = stroke.color.copy(alpha = stroke.alpha)
            if (path != null) scope.drawPath(path, color = color, style = style)
            else scope.drawRect(color = color, topLeft = Offset.Zero, size = size, style = style)
        }
    }

    private fun drawEllipse(
        scope: DrawScope,
        geom: EllipseGeometry,
        fill: FillStyle,
        stroke: StrokeStyle,
        document: VectorDocument?
    ) {
        val topLeft = Offset(-geom.radiusX, -geom.radiusY)
        val size = Size(geom.radiusX * 2f, geom.radiusY * 2f)
        paintFill(
            scope,
            fill,
            document,
            com.compose.canvas.geometry.RectF(-geom.radiusX, -geom.radiusY, geom.radiusX, geom.radiusY)
        ) { color, brush ->
            if (brush != null) scope.drawOval(brush = brush, topLeft = topLeft, size = size)
            else if (color != null) scope.drawOval(color = color, topLeft = topLeft, size = size)
        }
        if (stroke.width > 0f && stroke.color != Color.Unspecified) {
            scope.drawOval(
                color = stroke.color.copy(alpha = stroke.alpha),
                topLeft = topLeft,
                size = size,
                style = Stroke(width = stroke.width)
            )
        }
    }

    private fun drawLine(scope: DrawScope, geom: LineGeometry, stroke: StrokeStyle) {
        if (stroke.width <= 0f) return
        scope.drawLine(
            color = stroke.color.copy(alpha = stroke.alpha),
            start = Offset(geom.startX, geom.startY),
            end = Offset(geom.endX, geom.endY),
            strokeWidth = stroke.width,
            cap = when (stroke.cap) {
                StrokeCap.Butt -> androidx.compose.ui.graphics.StrokeCap.Butt
                StrokeCap.Round -> androidx.compose.ui.graphics.StrokeCap.Round
                StrokeCap.Square -> androidx.compose.ui.graphics.StrokeCap.Square
            }
        )
    }

    private fun drawPath(
        scope: DrawScope,
        geom: PathGeometry,
        fill: FillStyle,
        stroke: StrokeStyle,
        document: VectorDocument? = null
    ) {
        val path = PathBuilder.toComposePath(geom)
        paintFill(scope, fill, document, geom.boundingRect()) { color, brush ->
            if (brush != null) scope.drawPath(path, brush = brush)
            else if (color != null) scope.drawPath(path, color = color)
        }
        if (stroke.width > 0f && stroke.color != Color.Unspecified) {
            scope.drawPath(
                path,
                color = stroke.color.copy(alpha = stroke.alpha),
                style = stroke.toDrawStroke()
            )
        }
    }

    private fun drawText(scope: DrawScope, geom: com.compose.canvas.geometry.TextGeometry, fill: FillStyle) {
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = if (fill.color == Color.Unspecified) android.graphics.Color.BLACK else fill.color.copy(alpha = fill.alpha).let {
                android.graphics.Color.argb(
                    (it.alpha * 255).toInt(),
                    (it.red * 255).toInt(),
                    (it.green * 255).toInt(),
                    (it.blue * 255).toInt()
                )
            }
            textSize = geom.fontSize
            typeface = android.graphics.Typeface.create(geom.fontFamily, when {
                geom.fontWeight >= 700 -> android.graphics.Typeface.BOLD
                else -> android.graphics.Typeface.NORMAL
            })
            letterSpacing = if (geom.fontSize > 0f) geom.letterSpacing / geom.fontSize else 0f
            textAlign = when (geom.alignment) {
                com.compose.canvas.geometry.TextAlign.Left -> android.graphics.Paint.Align.LEFT
                com.compose.canvas.geometry.TextAlign.Center -> android.graphics.Paint.Align.CENTER
                com.compose.canvas.geometry.TextAlign.Right -> android.graphics.Paint.Align.RIGHT
            }
        }
        val x = when (geom.alignment) {
            com.compose.canvas.geometry.TextAlign.Left -> 0f
            com.compose.canvas.geometry.TextAlign.Center -> geom.boundingRect().width / 2f
            com.compose.canvas.geometry.TextAlign.Right -> geom.boundingRect().width
        }
        geom.text.split('\n').forEachIndexed { i, line ->
            scope.drawContext.canvas.nativeCanvas.drawText(
                line,
                x,
                geom.fontSize * (i + 1) * geom.lineHeight,
                paint
            )
        }
    }

    private fun paintFill(
        scope: DrawScope,
        fill: FillStyle,
        document: VectorDocument?,
        bounds: com.compose.canvas.geometry.RectF,
        block: (Color?, Brush?) -> Unit
    ) {
        when (fill.kind) {
            FillKind.None -> block(null, null)
            FillKind.Solid -> {
                if (fill.color == Color.Unspecified) block(null, null)
                else block(fill.color.copy(alpha = fill.alpha), null)
            }
            FillKind.LinearGradient, FillKind.RadialGradient -> {
                val res = fill.gradientId?.let { document?.definitions?.gradient(it) }
                val w = bounds.width.coerceAtLeast(1f)
                val h = bounds.height.coerceAtLeast(1f)
                val brush = when (res) {
                    is LinearGradientResource -> {
                        val x1 = bounds.left + res.x1 * w
                        val y1 = bounds.top + res.y1 * h
                        val x2 = bounds.left + res.x2 * w
                        val y2 = bounds.top + res.y2 * h
                        Brush.linearGradient(
                            colorStops = res.stops.map { it.offset to it.color.copy(alpha = it.alpha * fill.alpha) }.toTypedArray(),
                            start = Offset(x1, y1),
                            end = Offset(x2, y2)
                        )
                    }
                    is RadialGradientResource -> Brush.radialGradient(
                        colorStops = res.stops.map { it.offset to it.color.copy(alpha = it.alpha * fill.alpha) }.toTypedArray(),
                        center = Offset(bounds.left + res.cx * w, bounds.top + res.cy * h),
                        radius = res.r * maxOf(w, h)
                    )
                    else -> SolidColor(fill.color.takeIf { it != Color.Unspecified } ?: Color(0xFF6750A4))
                }
                block(null, brush)
            }
        }
    }

    private fun StrokeStyle.toDrawStroke(): Stroke = Stroke(
        width = width,
        cap = when (cap) {
            StrokeCap.Butt -> androidx.compose.ui.graphics.StrokeCap.Butt
            StrokeCap.Round -> androidx.compose.ui.graphics.StrokeCap.Round
            StrokeCap.Square -> androidx.compose.ui.graphics.StrokeCap.Square
        },
        join = when (join) {
            StrokeJoin.Miter -> androidx.compose.ui.graphics.StrokeJoin.Miter
            StrokeJoin.Round -> androidx.compose.ui.graphics.StrokeJoin.Round
            StrokeJoin.Bevel -> androidx.compose.ui.graphics.StrokeJoin.Bevel
        },
        pathEffect = if (dash.size >= 2) PathEffect.dashPathEffect(dash.toFloatArray()) else null
    )
}

object GridRenderer {
    fun draw(
        scope: DrawScope,
        canvas: CanvasSettings,
        viewport: ViewportState,
        gridSize: Float
    ) {
        if (gridSize <= 0f) return
        val box = canvas.effectiveViewBox
        val visible = viewport.visibleDocumentBounds()
        val startX = (maxOf(visible.left, box.left) / gridSize).toInt() * gridSize
        val endX = minOf(visible.right, box.right)
        val startY = (maxOf(visible.top, box.top) / gridSize).toInt() * gridSize
        val endY = minOf(visible.bottom, box.bottom)
        val color = Color(0xFFBDBDBD).copy(alpha = 0.55f)

        var x = startX
        while (x <= endX + 0.01f) {
            if (x >= box.left - 0.01f) {
                scope.drawLine(
                    color = color,
                    start = viewport.documentToScreen(x, box.top),
                    end = viewport.documentToScreen(x, box.bottom),
                    strokeWidth = if (kotlin.math.abs(x / gridSize) % 5f < 0.01f) 1f else 0.5f
                )
            }
            x += gridSize
        }
        var y = startY
        while (y <= endY + 0.01f) {
            if (y >= box.top - 0.01f) {
                scope.drawLine(
                    color = color,
                    start = viewport.documentToScreen(box.left, y),
                    end = viewport.documentToScreen(box.right, y),
                    strokeWidth = if (kotlin.math.abs(y / gridSize) % 5f < 0.01f) 1f else 0.5f
                )
            }
            y += gridSize
        }
    }
}
