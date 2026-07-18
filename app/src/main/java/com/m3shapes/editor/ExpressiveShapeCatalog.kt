package com.m3shapes.editor

import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.transformed
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Port of the official Material 3 expressive shape library (35 shapes).
 * Based on androidx.compose.material3.MaterialShapes (AOSP).
 */
object ExpressiveShapeCatalog {
    private val cornerRound15 = CornerRounding(radius = .15f)
    private val cornerRound20 = CornerRounding(radius = .2f)
    private val cornerRound30 = CornerRounding(radius = .3f)
    private val cornerRound50 = CornerRounding(radius = .5f)
    private val cornerRound100 = CornerRounding(radius = 1f)

    private val rotateNeg45 = matrixRotate(-45f)
    private val rotateNeg90 = matrixRotate(-90f)
    private val rotateNeg135 = matrixRotate(-135f)

    private var circleShape: RoundedPolygon? = null
    private var squareShape: RoundedPolygon? = null
    private var slantedShape: RoundedPolygon? = null
    private var archShape: RoundedPolygon? = null
    private var fanShape: RoundedPolygon? = null
    private var arrowShape: RoundedPolygon? = null
    private var semiCircleShape: RoundedPolygon? = null
    private var ovalShape: RoundedPolygon? = null
    private var pillShape: RoundedPolygon? = null
    private var triangleShape: RoundedPolygon? = null
    private var diamondShape: RoundedPolygon? = null
    private var clamShellShape: RoundedPolygon? = null
    private var pentagonShape: RoundedPolygon? = null
    private var gemShape: RoundedPolygon? = null
    private var verySunnyShape: RoundedPolygon? = null
    private var sunnyShape: RoundedPolygon? = null
    private var cookie4Shape: RoundedPolygon? = null
    private var cookie6Shape: RoundedPolygon? = null
    private var cookie7Shape: RoundedPolygon? = null
    private var cookie9Shape: RoundedPolygon? = null
    private var cookie12Shape: RoundedPolygon? = null
    private var ghostishShape: RoundedPolygon? = null
    private var clover4Shape: RoundedPolygon? = null
    private var clover8Shape: RoundedPolygon? = null
    private var burstShape: RoundedPolygon? = null
    private var softBurstShape: RoundedPolygon? = null
    private var boomShape: RoundedPolygon? = null
    private var softBoomShape: RoundedPolygon? = null
    private var flowerShape: RoundedPolygon? = null
    private var puffyShape: RoundedPolygon? = null
    private var puffyDiamondShape: RoundedPolygon? = null
    private var pixelCircleShape: RoundedPolygon? = null
    private var pixelTriangleShape: RoundedPolygon? = null
    private var bunShape: RoundedPolygon? = null
    private var heartShape: RoundedPolygon? = null

    fun polygon(id: ExpressiveShapeId): RoundedPolygon = when (id) {
        ExpressiveShapeId.Circle -> circleShape ?: circle().normalized().also { circleShape = it }
        ExpressiveShapeId.Square -> squareShape ?: square().normalized().also { squareShape = it }
        ExpressiveShapeId.Slanted -> slantedShape ?: slanted().normalized().also { slantedShape = it }
        ExpressiveShapeId.Arch -> archShape ?: arch().normalized().also { archShape = it }
        ExpressiveShapeId.Fan -> fanShape ?: fan().normalized().also { fanShape = it }
        ExpressiveShapeId.Arrow -> arrowShape ?: arrow().normalized().also { arrowShape = it }
        ExpressiveShapeId.SemiCircle -> semiCircleShape ?: semiCircle().normalized().also { semiCircleShape = it }
        ExpressiveShapeId.Oval -> ovalShape ?: oval().normalized().also { ovalShape = it }
        ExpressiveShapeId.Pill -> pillShape ?: pill().normalized().also { pillShape = it }
        ExpressiveShapeId.Triangle -> triangleShape ?: triangle().normalized().also { triangleShape = it }
        ExpressiveShapeId.Diamond -> diamondShape ?: diamond().normalized().also { diamondShape = it }
        ExpressiveShapeId.ClamShell -> clamShellShape ?: clamShell().normalized().also { clamShellShape = it }
        ExpressiveShapeId.Pentagon -> pentagonShape ?: pentagon().normalized().also { pentagonShape = it }
        ExpressiveShapeId.Gem -> gemShape ?: gem().normalized().also { gemShape = it }
        ExpressiveShapeId.Sunny -> sunnyShape ?: sunny().normalized().also { sunnyShape = it }
        ExpressiveShapeId.VerySunny -> verySunnyShape ?: verySunny().normalized().also { verySunnyShape = it }
        ExpressiveShapeId.Cookie4Sided -> cookie4Shape ?: cookie4().normalized().also { cookie4Shape = it }
        ExpressiveShapeId.Cookie6Sided -> cookie6Shape ?: cookie6().normalized().also { cookie6Shape = it }
        ExpressiveShapeId.Cookie7Sided -> cookie7Shape ?: cookie7().normalized().also { cookie7Shape = it }
        ExpressiveShapeId.Cookie9Sided -> cookie9Shape ?: cookie9().normalized().also { cookie9Shape = it }
        ExpressiveShapeId.Cookie12Sided -> cookie12Shape ?: cookie12().normalized().also { cookie12Shape = it }
        ExpressiveShapeId.Ghostish -> ghostishShape ?: ghostish().normalized().also { ghostishShape = it }
        ExpressiveShapeId.Clover4Leaf -> clover4Shape ?: clover4().normalized().also { clover4Shape = it }
        ExpressiveShapeId.Clover8Leaf -> clover8Shape ?: clover8().normalized().also { clover8Shape = it }
        ExpressiveShapeId.Burst -> burstShape ?: burst().normalized().also { burstShape = it }
        ExpressiveShapeId.SoftBurst -> softBurstShape ?: softBurst().normalized().also { softBurstShape = it }
        ExpressiveShapeId.Boom -> boomShape ?: boom().normalized().also { boomShape = it }
        ExpressiveShapeId.SoftBoom -> softBoomShape ?: softBoom().normalized().also { softBoomShape = it }
        ExpressiveShapeId.Flower -> flowerShape ?: flower().normalized().also { flowerShape = it }
        ExpressiveShapeId.Puffy -> puffyShape ?: puffy().normalized().also { puffyShape = it }
        ExpressiveShapeId.PuffyDiamond -> puffyDiamondShape ?: puffyDiamond().normalized().also { puffyDiamondShape = it }
        ExpressiveShapeId.PixelCircle -> pixelCircleShape ?: pixelCircle().normalized().also { pixelCircleShape = it }
        ExpressiveShapeId.PixelTriangle -> pixelTriangleShape ?: pixelTriangle().normalized().also { pixelTriangleShape = it }
        ExpressiveShapeId.Bun -> bunShape ?: bun().normalized().also { bunShape = it }
        ExpressiveShapeId.Heart -> heartShape ?: heart().normalized().also { heartShape = it }
    }

    private fun matrixRotate(degrees: Float): Matrix = Matrix().apply { postRotate(degrees) }

    private fun RoundedPolygon.transformedMatrix(matrix: Matrix): RoundedPolygon = transformed(matrix)

    private fun circle(numVertices: Int = 10): RoundedPolygon =
        RoundedPolygon.circle(numVertices = numVertices)

    private fun square(): RoundedPolygon =
        RoundedPolygon.rectangle(width = 1f, height = 1f, rounding = cornerRound30)

    private fun slanted(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.926f, 0.970f), CornerRounding(0.189f, 0.811f)),
            PointNRound(Offset(-0.021f, 0.967f), CornerRounding(0.187f, 0.057f)),
        ),
        reps = 2,
    )

    private fun arch(): RoundedPolygon =
        RoundedPolygon(
            numVertices = 4,
            perVertexRounding = listOf(cornerRound100, cornerRound100, cornerRound20, cornerRound20),
        ).transformedMatrix(rotateNeg135)

    private fun fan(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(1.004f, 1.000f), CornerRounding(0.148f, 0.417f)),
            PointNRound(Offset(0.000f, 1.000f), CornerRounding(0.151f)),
            PointNRound(Offset(0.000f, -0.003f), CornerRounding(0.148f)),
            PointNRound(Offset(0.978f, 0.020f), CornerRounding(0.803f)),
        ),
        reps = 1,
    )

    private fun arrow(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.500f, 0.892f), CornerRounding(0.313f)),
            PointNRound(Offset(-0.216f, 1.050f), CornerRounding(0.207f)),
            PointNRound(Offset(0.499f, -0.160f), CornerRounding(0.215f, 1.000f)),
            PointNRound(Offset(1.225f, 1.060f), CornerRounding(0.211f)),
        ),
        reps = 1,
    )

    private fun semiCircle(): RoundedPolygon =
        RoundedPolygon.rectangle(
            width = 1.6f,
            height = 1f,
            perVertexRounding = listOf(cornerRound20, cornerRound20, cornerRound100, cornerRound100),
        )

    private fun oval(): RoundedPolygon {
        val m = Matrix().apply { postScale(1f, 0.64f) }
        return RoundedPolygon.circle().transformedMatrix(m).transformedMatrix(rotateNeg45)
    }

    private fun pill(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.961f, 0.039f), CornerRounding(0.426f)),
            PointNRound(Offset(1.001f, 0.428f)),
            PointNRound(Offset(1.000f, 0.609f), CornerRounding(1.000f)),
        ),
        reps = 2,
        mirroring = true,
    )

    private fun triangle(): RoundedPolygon =
        RoundedPolygon(numVertices = 3, rounding = cornerRound20).transformedMatrix(rotateNeg90)

    private fun diamond(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.500f, 1.096f), CornerRounding(0.151f, 0.524f)),
            PointNRound(Offset(0.040f, 0.500f), CornerRounding(0.159f)),
        ),
        reps = 2,
    )

    private fun clamShell(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.171f, 0.841f), CornerRounding(0.159f)),
            PointNRound(Offset(-0.020f, 0.500f), CornerRounding(0.140f)),
            PointNRound(Offset(0.170f, 0.159f), CornerRounding(0.159f)),
        ),
        reps = 2,
    )

    private fun pentagon(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.500f, -0.009f), CornerRounding(0.172f)),
            PointNRound(Offset(1.030f, 0.365f), CornerRounding(0.164f)),
            PointNRound(Offset(0.828f, 0.970f), CornerRounding(0.169f)),
        ),
        reps = 1,
        mirroring = true,
    )

    private fun gem(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.499f, 1.023f), CornerRounding(0.241f, 0.778f)),
            PointNRound(Offset(-0.005f, 0.792f), CornerRounding(0.208f)),
            PointNRound(Offset(0.073f, 0.258f), CornerRounding(0.228f)),
            PointNRound(Offset(0.433f, 0.000f), CornerRounding(0.491f)),
        ),
        reps = 1,
        mirroring = true,
    )

    private fun sunny(): RoundedPolygon =
        RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = .8f, rounding = cornerRound15)

    private fun verySunny(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.500f, 1.080f), CornerRounding(0.085f)),
            PointNRound(Offset(0.358f, 0.843f), CornerRounding(0.085f)),
        ),
        reps = 8,
    )

    private fun cookie4(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(1.237f, 1.236f), CornerRounding(0.258f)),
            PointNRound(Offset(0.500f, 0.918f), CornerRounding(0.233f)),
        ),
        reps = 4,
    )

    private fun cookie6(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.723f, 0.884f), CornerRounding(0.394f)),
            PointNRound(Offset(0.500f, 1.099f), CornerRounding(0.398f)),
        ),
        reps = 6,
    )

    private fun cookie7(): RoundedPolygon =
        RoundedPolygon.star(numVerticesPerRadius = 7, innerRadius = .75f, rounding = cornerRound50)
            .transformedMatrix(rotateNeg90)

    private fun cookie9(): RoundedPolygon =
        RoundedPolygon.star(numVerticesPerRadius = 9, innerRadius = .8f, rounding = cornerRound50)
            .transformedMatrix(rotateNeg90)

    private fun cookie12(): RoundedPolygon =
        RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = .8f, rounding = cornerRound50)
            .transformedMatrix(rotateNeg90)

    private fun ghostish(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.500f, 0f), CornerRounding(1.000f)),
            PointNRound(Offset(1f, 0f), CornerRounding(1.000f)),
            PointNRound(Offset(1f, 1.140f), CornerRounding(0.254f, 0.106f)),
            PointNRound(Offset(0.575f, 0.906f), CornerRounding(0.253f)),
        ),
        reps = 1,
        mirroring = true,
    )

    private fun clover4(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.500f, 0.074f)),
            PointNRound(Offset(0.725f, -0.099f), CornerRounding(0.476f)),
        ),
        reps = 4,
        mirroring = true,
    )

    private fun clover8(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.500f, 0.036f)),
            PointNRound(Offset(0.758f, -0.101f), CornerRounding(0.209f)),
        ),
        reps = 8,
    )

    private fun burst(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.500f, -0.006f), CornerRounding(0.006f)),
            PointNRound(Offset(0.592f, 0.158f), CornerRounding(0.006f)),
        ),
        reps = 12,
    )

    private fun softBurst(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.193f, 0.277f), CornerRounding(0.053f)),
            PointNRound(Offset(0.176f, 0.055f), CornerRounding(0.053f)),
        ),
        reps = 10,
    )

    private fun boom(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.457f, 0.296f), CornerRounding(0.007f)),
            PointNRound(Offset(0.500f, -0.051f), CornerRounding(0.007f)),
        ),
        reps = 15,
    )

    private fun softBoom(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.733f, 0.454f)),
            PointNRound(Offset(0.839f, 0.437f), CornerRounding(0.532f)),
            PointNRound(Offset(0.949f, 0.449f), CornerRounding(0.439f, 1.000f)),
            PointNRound(Offset(0.998f, 0.478f), CornerRounding(0.174f)),
        ),
        reps = 16,
        mirroring = true,
    )

    private fun flower(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.370f, 0.187f)),
            PointNRound(Offset(0.416f, 0.049f), CornerRounding(0.381f)),
            PointNRound(Offset(0.479f, 0.001f), CornerRounding(0.095f)),
        ),
        reps = 8,
        mirroring = true,
    )

    private fun puffy(): RoundedPolygon {
        val m = Matrix().apply { postScale(1f, 0.742f) }
        return customPolygon(
            listOf(
                PointNRound(Offset(0.500f, 0.053f)),
                PointNRound(Offset(0.545f, -0.040f), CornerRounding(0.405f)),
                PointNRound(Offset(0.670f, -0.035f), CornerRounding(0.426f)),
                PointNRound(Offset(0.717f, 0.066f), CornerRounding(0.574f)),
                PointNRound(Offset(0.722f, 0.128f)),
                PointNRound(Offset(0.777f, 0.002f), CornerRounding(0.360f)),
                PointNRound(Offset(0.914f, 0.149f), CornerRounding(0.660f)),
                PointNRound(Offset(0.926f, 0.289f), CornerRounding(0.660f)),
                PointNRound(Offset(0.881f, 0.346f)),
                PointNRound(Offset(0.940f, 0.344f), CornerRounding(0.126f)),
                PointNRound(Offset(1.003f, 0.437f), CornerRounding(0.255f)),
            ),
            reps = 2,
            mirroring = true,
        ).transformedMatrix(m)
    }

    private fun puffyDiamond(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.870f, 0.130f), CornerRounding(0.146f)),
            PointNRound(Offset(0.818f, 0.357f)),
            PointNRound(Offset(1.000f, 0.332f), CornerRounding(0.853f)),
        ),
        reps = 4,
        mirroring = true,
    )

    private fun pixelCircle(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.500f, 0.000f)),
            PointNRound(Offset(0.704f, 0.000f)),
            PointNRound(Offset(0.704f, 0.065f)),
            PointNRound(Offset(0.843f, 0.065f)),
            PointNRound(Offset(0.843f, 0.148f)),
            PointNRound(Offset(0.926f, 0.148f)),
            PointNRound(Offset(0.926f, 0.296f)),
            PointNRound(Offset(1.000f, 0.296f)),
        ),
        reps = 2,
        mirroring = true,
    )

    private fun pixelTriangle(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.110f, 0.500f)),
            PointNRound(Offset(0.113f, 0.000f)),
            PointNRound(Offset(0.287f, 0.000f)),
            PointNRound(Offset(0.287f, 0.087f)),
            PointNRound(Offset(0.421f, 0.087f)),
            PointNRound(Offset(0.421f, 0.170f)),
            PointNRound(Offset(0.560f, 0.170f)),
            PointNRound(Offset(0.560f, 0.265f)),
            PointNRound(Offset(0.674f, 0.265f)),
            PointNRound(Offset(0.675f, 0.344f)),
            PointNRound(Offset(0.789f, 0.344f)),
            PointNRound(Offset(0.789f, 0.439f)),
            PointNRound(Offset(0.888f, 0.439f)),
        ),
        reps = 1,
        mirroring = true,
    )

    private fun bun(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.796f, 0.500f)),
            PointNRound(Offset(0.853f, 0.518f), CornerRounding(1f)),
            PointNRound(Offset(0.992f, 0.631f), CornerRounding(1f)),
            PointNRound(Offset(0.968f, 1.000f), CornerRounding(1f)),
        ),
        reps = 2,
        mirroring = true,
    )

    private fun heart(): RoundedPolygon = customPolygon(
        listOf(
            PointNRound(Offset(0.500f, 0.268f), CornerRounding(0.016f)),
            PointNRound(Offset(0.792f, -0.066f), CornerRounding(0.958f)),
            PointNRound(Offset(1.064f, 0.276f), CornerRounding(1.000f)),
            PointNRound(Offset(0.501f, 0.946f), CornerRounding(0.129f)),
        ),
        reps = 1,
        mirroring = true,
    )

    private data class PointNRound(
        val o: Offset,
        val r: CornerRounding = CornerRounding.Unrounded,
    )

    private fun doRepeat(
        points: List<PointNRound>,
        reps: Int,
        center: Offset,
        mirroring: Boolean,
    ): List<PointNRound> =
        if (mirroring) {
            buildList {
                val angles = points.map { (it.o - center).angleDegrees() }
                val distances = points.map { (it.o - center).getDistance() }
                val actualReps = reps * 2
                val sectionAngle = 360f / actualReps
                repeat(actualReps) { rep ->
                    points.indices.forEach { index ->
                        val i = if (rep % 2 == 0) index else points.lastIndex - index
                        if (i > 0 || rep % 2 == 0) {
                            val a = (
                                sectionAngle * rep +
                                    if (rep % 2 == 0) angles[i]
                                    else sectionAngle - angles[i] + 2 * angles[0]
                                ).toRadians()
                            val finalPoint = Offset(cos(a), sin(a)) * distances[i] + center
                            add(PointNRound(finalPoint, points[i].r))
                        }
                    }
                }
            }
        } else {
            val np = points.size
            (0 until np * reps).map { index ->
                val point = points[index % np].o.rotateDegrees((index / np) * 360f / reps, center)
                PointNRound(point, points[index % np].r)
            }
        }

    private fun Offset.rotateDegrees(angle: Float, center: Offset = Offset.Zero): Offset =
        angle.toRadians().let { a ->
            val off = this - center
            Offset(off.x * cos(a) - off.y * sin(a), off.x * sin(a) + off.y * cos(a)) + center
        }

    private fun Float.toRadians(): Float = this / 360f * 2 * PI.toFloat()

    private fun Offset.angleDegrees(): Float = atan2(y, x) * 180f / PI.toFloat()

    private fun Offset.getDistance(): Float = kotlin.math.sqrt(x * x + y * y)

    private fun customPolygon(
        pnr: List<PointNRound>,
        reps: Int,
        center: Offset = Offset(0.5f, 0.5f),
        mirroring: Boolean = false,
    ): RoundedPolygon {
        val actualPoints = doRepeat(pnr, reps, center, mirroring)
        return RoundedPolygon(
            vertices = FloatArray(actualPoints.size * 2) { ix ->
                actualPoints[ix / 2].o.let { if (ix % 2 == 0) it.x else it.y }
            },
            perVertexRounding = buildList { for (p in actualPoints) add(p.r) },
            centerX = center.x,
            centerY = center.y,
        )
    }
}

fun ExpressiveShapeId.toRoundedPolygon(): RoundedPolygon = ExpressiveShapeCatalog.polygon(this)
