package com.m3shapes.editor

import android.graphics.PathMeasure
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.renderer.PathBuilder
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun ShapePreviewCanvas(
    path: PathGeometry,
    rotation: Float = 0f,
    scale: Float = 1f,
    alpha: Float = 1f,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    fillColor: Color,
    strokeProgress: Float = 0f,
    blurRadius: Float = 0f,
    gradientProgress: Float = 0f,
    gradientEndColor: Color = fillColor,
    fillVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val safeAlpha = alpha.coerceIn(0f, 1f)
    val blurMod = if (blurRadius > 0.4f) {
        Modifier.blur(blurRadius.dp, BlurredEdgeTreatment.Unbounded)
    } else {
        Modifier
    }
    Canvas(modifier.then(blurMod)) {
        val composePath = PathBuilder.toComposePath(path)
        val ms = size.width / 256f * scale
        val offsetScale = size.width / 200f
        translate(left = offsetX * offsetScale, top = offsetY * offsetScale) {
            rotate(
                rotation,
                pivot = Offset(size.width / 2f, size.height / 2f)
            ) {
                scale(ms, ms, pivot = Offset.Zero) {
                    if (fillVisible) {
                        drawFilledPath(
                            path = composePath,
                            fillColor = fillColor,
                            gradientEndColor = gradientEndColor,
                            gradientProgress = gradientProgress,
                            alpha = safeAlpha
                        )
                    }
                    drawPath(
                        composePath,
                        Color.Red.copy(alpha = 0.35f * safeAlpha),
                        style = Stroke(width = 1.5f)
                    )
                    drawTrimmedStroke(
                        path = composePath,
                        progress = strokeProgress,
                        color = if (gradientProgress > 0.001f) {
                            gradientEndColor.copy(alpha = safeAlpha)
                        } else {
                            fillColor.copy(alpha = safeAlpha)
                        }
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawFilledPath(
    path: Path,
    fillColor: Color,
    gradientEndColor: Color,
    gradientProgress: Float,
    alpha: Float
) {
    val amount = gradientProgress.coerceIn(0f, 1f)
    if (amount <= 0.001f) {
        drawPath(path, fillColor.copy(alpha = alpha), style = Fill)
        return
    }
    val radians = Math.toRadians(amount * 360.0)
    val dx = cos(radians).toFloat()
    val dy = sin(radians).toFloat()
    val extent = 180f
    val travel = (amount - 0.5f) * extent
    val center = Offset(128f, 128f)
    val brush = Brush.linearGradient(
        colors = listOf(fillColor.copy(alpha = alpha), gradientEndColor.copy(alpha = alpha)),
        start = Offset(
            center.x - dx * extent + dx * travel,
            center.y - dy * extent + dy * travel
        ),
        end = Offset(
            center.x + dx * extent + dx * travel,
            center.y + dy * extent + dy * travel
        )
    )
    drawPath(path, brush = brush, style = Fill)
}

private fun DrawScope.drawTrimmedStroke(
    path: Path,
    progress: Float,
    color: Color,
    strokeWidth: Float = 8f
) {
    val amount = progress.coerceIn(0f, 1f)
    if (amount <= 0.001f) return
    val stroke = Stroke(
        width = strokeWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    if (amount >= 0.999f) {
        drawPath(path, color, style = stroke)
        return
    }
    val measure = PathMeasure(path.asAndroidPath(), false)
    val dest = AndroidPath()
    var remaining = 0f
    do {
        remaining += measure.length
    } while (measure.nextContour())
    val target = remaining * amount
    val rewind = PathMeasure(path.asAndroidPath(), false)
    var consumed = 0f
    do {
        val length = rewind.length
        if (length > 0f && consumed < target) {
            val take = minOf(length, target - consumed)
            rewind.getSegment(0f, take, dest, true)
            consumed += take
        }
    } while (consumed < target && rewind.nextContour())
    if (!dest.isEmpty) {
        drawPath(dest.asComposePath(), color, style = stroke)
    }
}

@Composable
internal fun AnimationPreviewCard(
    path: PathGeometry,
    rotation: Float,
    scale: Float,
    alpha: Float,
    offsetX: Float,
    offsetY: Float,
    fillColor: Color,
    strokeProgress: Float = 0f,
    blurRadius: Float = 0f,
    gradientProgress: Float = 0f,
    gradientEndColor: Color = fillColor,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        ShapePreviewCanvas(
            path = path,
            rotation = rotation,
            scale = scale,
            alpha = alpha,
            offsetX = offsetX,
            offsetY = offsetY,
            fillColor = fillColor,
            strokeProgress = strokeProgress,
            blurRadius = blurRadius,
            gradientProgress = gradientProgress,
            gradientEndColor = gradientEndColor,
            modifier = Modifier.size(200.dp)
        )
    }
}

@Composable
internal fun StateThumbnail(
    label: String,
    path: PathGeometry?,
    fillColor: Color,
    size: Dp = 72.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (path == null || path.segments.isEmpty()) {
            androidx.compose.material3.Text(
                label.take(1),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            ShapePreviewCanvas(
                path = path,
                fillColor = fillColor,
                modifier = Modifier.size(size - 8.dp)
            )
        }
    }
}
