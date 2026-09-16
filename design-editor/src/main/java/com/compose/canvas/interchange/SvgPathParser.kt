package com.compose.canvas.interchange

import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.geometry.PathSegment
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * SVG path `d` parser. Supports M/L/H/V/C/S/Q/T/A/Z (absolute and relative).
 */
object SvgPathParser {
    fun parse(d: String): PathGeometry {
        val tokens = tokenize(d)
        val segments = mutableListOf<PathSegment>()
        var i = 0
        var cmd = 'M'
        var x = 0f
        var y = 0f
        var startX = 0f
        var startY = 0f
        var lastCx = 0f
        var lastCy = 0f
        var lastWasCubic = false
        var lastWasQuad = false
        var qx = 0f
        var qy = 0f

        fun nextNumber(): Float {
            if (i >= tokens.size) return 0f
            val n = tokens[i].toFloatOrNull() ?: 0f
            i++
            return n
        }

        fun hasNumber(): Boolean = i < tokens.size && tokens[i].toFloatOrNull() != null

        while (i < tokens.size) {
            val t = tokens[i]
            if (t.length == 1 && t[0].isLetter()) {
                cmd = t[0]
                i++
            }
            when (cmd) {
                'M', 'm' -> {
                    val rel = cmd == 'm'
                    val nx = nextNumber()
                    val ny = nextNumber()
                    x = if (rel) x + nx else nx
                    y = if (rel) y + ny else ny
                    startX = x
                    startY = y
                    segments += PathSegment.MoveTo(x, y)
                    lastWasCubic = false
                    lastWasQuad = false
                    cmd = if (rel) 'l' else 'L'
                }
                'L', 'l' -> {
                    val rel = cmd == 'l'
                    val nx = nextNumber()
                    val ny = nextNumber()
                    x = if (rel) x + nx else nx
                    y = if (rel) y + ny else ny
                    segments += PathSegment.LineTo(x, y)
                    lastWasCubic = false
                    lastWasQuad = false
                }
                'H', 'h' -> {
                    val nx = nextNumber()
                    x = if (cmd == 'h') x + nx else nx
                    segments += PathSegment.LineTo(x, y)
                    lastWasCubic = false
                    lastWasQuad = false
                }
                'V', 'v' -> {
                    val ny = nextNumber()
                    y = if (cmd == 'v') y + ny else ny
                    segments += PathSegment.LineTo(x, y)
                    lastWasCubic = false
                    lastWasQuad = false
                }
                'C', 'c' -> {
                    val rel = cmd == 'c'
                    val x1 = nextNumber().let { if (rel) it + x else it }
                    val y1 = nextNumber().let { if (rel) it + y else it }
                    val x2 = nextNumber().let { if (rel) it + x else it }
                    val y2 = nextNumber().let { if (rel) it + y else it }
                    val nx = nextNumber().let { if (rel) it + x else it }
                    val ny = nextNumber().let { if (rel) it + y else it }
                    segments += PathSegment.CubicBezierTo(x1, y1, x2, y2, nx, ny)
                    lastCx = x2
                    lastCy = y2
                    x = nx
                    y = ny
                    lastWasCubic = true
                    lastWasQuad = false
                }
                'S', 's' -> {
                    val rel = cmd == 's'
                    val x1 = if (lastWasCubic) 2f * x - lastCx else x
                    val y1 = if (lastWasCubic) 2f * y - lastCy else y
                    val x2 = nextNumber().let { if (rel) it + x else it }
                    val y2 = nextNumber().let { if (rel) it + y else it }
                    val nx = nextNumber().let { if (rel) it + x else it }
                    val ny = nextNumber().let { if (rel) it + y else it }
                    segments += PathSegment.CubicBezierTo(x1, y1, x2, y2, nx, ny)
                    lastCx = x2
                    lastCy = y2
                    x = nx
                    y = ny
                    lastWasCubic = true
                    lastWasQuad = false
                }
                'Q', 'q' -> {
                    val rel = cmd == 'q'
                    val cx = nextNumber().let { if (rel) it + x else it }
                    val cy = nextNumber().let { if (rel) it + y else it }
                    val nx = nextNumber().let { if (rel) it + x else it }
                    val ny = nextNumber().let { if (rel) it + y else it }
                    segments += PathSegment.QuadraticBezierTo(cx, cy, nx, ny)
                    qx = cx
                    qy = cy
                    x = nx
                    y = ny
                    lastWasQuad = true
                    lastWasCubic = false
                }
                'T', 't' -> {
                    val rel = cmd == 't'
                    val cx = if (lastWasQuad) 2f * x - qx else x
                    val cy = if (lastWasQuad) 2f * y - qy else y
                    val nx = nextNumber().let { if (rel) it + x else it }
                    val ny = nextNumber().let { if (rel) it + y else it }
                    segments += PathSegment.QuadraticBezierTo(cx, cy, nx, ny)
                    qx = cx
                    qy = cy
                    x = nx
                    y = ny
                    lastWasQuad = true
                    lastWasCubic = false
                }
                'A', 'a' -> {
                    val rel = cmd == 'a'
                    val rx = abs(nextNumber())
                    val ry = abs(nextNumber())
                    val rot = nextNumber()
                    val large = nextNumber() != 0f
                    val sweep = nextNumber() != 0f
                    val nx = nextNumber().let { if (rel) it + x else it }
                    val ny = nextNumber().let { if (rel) it + y else it }
                    segments += arcToCubics(x, y, nx, ny, rx, ry, rot, large, sweep)
                    x = nx
                    y = ny
                    lastWasCubic = false
                    lastWasQuad = false
                }
                'Z', 'z' -> {
                    segments += PathSegment.Close
                    x = startX
                    y = startY
                    lastWasCubic = false
                    lastWasQuad = false
                    if (hasNumber()) {
                        // ignore extra
                    }
                }
                else -> i++
            }
        }
        val closed = segments.any { it is PathSegment.Close }
        return PathGeometry(segments, closed)
    }

    private fun tokenize(d: String): List<String> {
        val out = mutableListOf<String>()
        val buf = StringBuilder()
        fun flush() {
            if (buf.isNotEmpty()) {
                out += buf.toString()
                buf.clear()
            }
        }
        for (c in d) {
            when {
                c.isLetter() && c != 'e' && c != 'E' -> {
                    flush()
                    out += c.toString()
                }
                c == ',' || c.isWhitespace() -> flush()
                c == '-' && buf.isNotEmpty() && buf.last() != 'e' && buf.last() != 'E' -> {
                    flush()
                    buf.append(c)
                }
                else -> buf.append(c)
            }
        }
        flush()
        return out
    }

    private fun arcToCubics(
        x1: Float, y1: Float, x2: Float, y2: Float,
        rxIn: Float, ryIn: Float, angleDeg: Float, large: Boolean, sweep: Boolean
    ): List<PathSegment> {
        if (rxIn == 0f || ryIn == 0f || (x1 == x2 && y1 == y2)) {
            return listOf(PathSegment.LineTo(x2, y2))
        }
        val phi = Math.toRadians(angleDeg.toDouble())
        val cosPhi = cos(phi)
        val sinPhi = sin(phi)
        val dx = (x1 - x2) / 2.0
        val dy = (y1 - y2) / 2.0
        val x1p = cosPhi * dx + sinPhi * dy
        val y1p = -sinPhi * dx + cosPhi * dy
        var rx = rxIn.toDouble()
        var ry = ryIn.toDouble()
        val lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry)
        if (lambda > 1.0) {
            val s = sqrt(lambda)
            rx *= s
            ry *= s
        }
        val sign = if (large == sweep) -1.0 else 1.0
        val num = (rx * rx * ry * ry - rx * rx * y1p * y1p - ry * ry * x1p * x1p)
            .coerceAtLeast(0.0)
        val den = (rx * rx * y1p * y1p + ry * ry * x1p * x1p).let { if (it == 0.0) 1e-12 else it }
        val coef = sign * sqrt(num / den)
        val cxp = coef * (rx * y1p) / ry
        val cyp = coef * -(ry * x1p) / rx
        val cx = cosPhi * cxp - sinPhi * cyp + (x1 + x2) / 2.0
        val cy = sinPhi * cxp + cosPhi * cyp + (y1 + y2) / 2.0
        val startVecX = (x1p - cxp) / rx
        val startVecY = (y1p - cyp) / ry
        val endVecX = (-x1p - cxp) / rx
        val endVecY = (-y1p - cyp) / ry
        var theta1 = atan2(startVecY, startVecX)
        var dtheta = atan2(endVecY, endVecX) - theta1
        if (!sweep && dtheta > 0) dtheta -= 2 * PI
        if (sweep && dtheta < 0) dtheta += 2 * PI
        val segments = ceil(abs(dtheta) / (PI / 2)).toInt().coerceAtLeast(1).coerceAtMost(8)
        val segs = mutableListOf<PathSegment>()
        val dt = dtheta / segments
        for (i in 0 until segments) {
            val t0 = theta1 + i * dt
            val t1 = t0 + dt
            val alpha = sin(dt) * (sqrt(4 + 3 * (tan(dt / 2).let { it * it })) - 1) / 3
            val p0 = arcPoint(cx, cy, rx, ry, phi, t0)
            val p1 = arcPoint(cx, cy, rx, ry, phi, t1)
            val d0 = arcDerivative(cx, cy, rx, ry, phi, t0)
            val d1 = arcDerivative(cx, cy, rx, ry, phi, t1)
            segs += PathSegment.CubicBezierTo(
                (p0.first + alpha * d0.first).toFloat(),
                (p0.second + alpha * d0.second).toFloat(),
                (p1.first - alpha * d1.first).toFloat(),
                (p1.second - alpha * d1.second).toFloat(),
                p1.first.toFloat(),
                p1.second.toFloat()
            )
        }
        return segs
    }

    private fun tan(v: Double) = kotlin.math.tan(v)

    private fun arcPoint(
        cx: Double, cy: Double, rx: Double, ry: Double, phi: Double, theta: Double
    ): Pair<Double, Double> {
        val cosPhi = cos(phi)
        val sinPhi = sin(phi)
        val x = rx * cos(theta)
        val y = ry * sin(theta)
        return (cosPhi * x - sinPhi * y + cx) to (sinPhi * x + cosPhi * y + cy)
    }

    private fun arcDerivative(
        cx: Double, cy: Double, rx: Double, ry: Double, phi: Double, theta: Double
    ): Pair<Double, Double> {
        val cosPhi = cos(phi)
        val sinPhi = sin(phi)
        val dx = -rx * sin(theta)
        val dy = ry * cos(theta)
        return (cosPhi * dx - sinPhi * dy) to (sinPhi * dx + cosPhi * dy)
    }
}
