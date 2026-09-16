package com.compose.canvas.renderer

import androidx.compose.ui.graphics.Path
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment

/** Maps the document path model to a Compose [Path]. Used by the renderer and later by exporters. */
object PathBuilder {
    fun toComposePath(geom: PathGeometry): Path {
        val path = Path()
        for (seg in geom.segments) {
            when (seg) {
                is PathSegment.MoveTo -> path.moveTo(seg.x, seg.y)
                is PathSegment.LineTo -> path.lineTo(seg.x, seg.y)
                is PathSegment.QuadraticBezierTo -> path.quadraticBezierTo(seg.cx, seg.cy, seg.x, seg.y)
                is PathSegment.CubicBezierTo -> path.cubicTo(seg.cx1, seg.cy1, seg.cx2, seg.cy2, seg.x, seg.y)
                is PathSegment.Close -> path.close()
            }
        }
        return path
    }
}
