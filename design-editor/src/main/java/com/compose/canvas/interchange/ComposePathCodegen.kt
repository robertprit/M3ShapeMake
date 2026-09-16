package com.compose.canvas.interchange

import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment

/**
 * Phase 6 building block: Kotlin source for a Compose Path.
 * Used by the document export UI to keep geometry → code conversion deterministic.
 */
object ComposePathCodegen {
    fun generate(geom: PathGeometry, valName: String = "path"): String {
        val body = buildString {
            for (seg in geom.segments) {
                when (seg) {
                    is PathSegment.MoveTo -> appendLine("    moveTo(${fmt(seg.x)}f, ${fmt(seg.y)}f)")
                    is PathSegment.LineTo -> appendLine("    lineTo(${fmt(seg.x)}f, ${fmt(seg.y)}f)")
                    is PathSegment.QuadraticBezierTo ->
                        appendLine("    quadraticBezierTo(${fmt(seg.cx)}f, ${fmt(seg.cy)}f, ${fmt(seg.x)}f, ${fmt(seg.y)}f)")
                    is PathSegment.CubicBezierTo ->
                        appendLine(
                            "    cubicTo(${fmt(seg.cx1)}f, ${fmt(seg.cy1)}f, ${fmt(seg.cx2)}f, ${fmt(seg.cy2)}f, ${fmt(seg.x)}f, ${fmt(seg.y)}f)"
                        )
                    is PathSegment.Close -> appendLine("    close()")
                }
            }
        }
        return "val $valName = Path().apply {\n$body}"
    }

    private fun fmt(value: Float): String {
        return if (value == value.toInt().toFloat()) value.toInt().toString()
        else "%.4f".format(value).trimEnd('0').trimEnd('.')
    }
}
