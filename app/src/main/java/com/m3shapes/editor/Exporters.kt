package com.m3shapes.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

fun generateMorphAvdXml(
    pathData1: String,
    pathData2: String,
    durationMs: Int = 3000,
    fillColorHex: String = "#FF6750A4"
): String = """<?xml version="1.0" encoding="utf-8"?>
<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt">
    <aapt:attr name="android:drawable">
        <vector
            android:width="256dp"
            android:height="256dp"
            android:viewportWidth="256"
            android:viewportHeight="256">
            <path
                android:name="shape"
                android:fillColor="$fillColorHex"
                android:pathData="$pathData1" />
        </vector>
    </aapt:attr>
    <target android:name="shape">
        <aapt:attr name="android:animation">
            <objectAnimator
                android:duration="$durationMs"
                android:repeatMode="reverse"
                android:repeatCount="infinite"
                android:propertyName="pathData"
                android:valueFrom="$pathData1"
                android:valueTo="$pathData2"
                android:valueType="pathType" />
        </aapt:attr>
    </target>
</animated-vector>"""

fun generateShapeSvg(pathData: String, fillHex: String = "#6750A4"): String =
    """<svg viewBox="0 0 256 256" xmlns="http://www.w3.org/2000/svg"><path d="$pathData" fill="$fillHex"/></svg>"""

fun generateComposeExport(
    shapeA: ShapeSource,
    shapeB: ShapeSource,
    enabledAnimations: Set<AnimationKind>,
    stiffness: Float,
    damping: Float,
    mass: Float,
    easing: StudioEasing,
    reverse: Boolean,
    morphRange: FloatRangeValue,
    rotateRange: FloatRangeValue,
    scaleRange: FloatRangeValue,
    alphaRange: FloatRangeValue,
    colorRange: FloatRangeValue,
    offsetXRange: FloatRangeValue,
    offsetYRange: FloatRangeValue,
    fillColor: Color,
    targetColor: Color
): String {
    // TODO: Extend generateComposeExport to emit full Sequencer step lists, not only the live config.
    val colorHex = "#%08X".format(fillColor.toArgb())
    val targetHex = "#%08X".format(targetColor.toArgb())
    val shapeARef = shapeComposeReference(shapeA)
    val shapeBRef = shapeComposeReference(shapeB)
    val shapeALabel = shapeLabel(shapeA)
    val shapeBLabel = shapeLabel(shapeB)
    val needsExpressive = shapeA is ShapeSource.Material || shapeB is ShapeSource.Material
    val needsCustomPolygon = shapeA is ShapeSource.Custom || shapeB is ShapeSource.Custom ||
        shapeA is ShapeSource.Drawn || shapeB is ShapeSource.Drawn
    val durationMs = estimateDurationMs(stiffness, mass)
    val k = "%.1f".format(java.util.Locale.US, stiffness)
    val d = "%.2f".format(java.util.Locale.US, damping)
    val m = "%.2f".format(java.util.Locale.US, mass)
    val effectiveK = "%.1f".format(java.util.Locale.US, springStiffnessForMass(stiffness, mass))
    fun f(v: Float) = "%.2f".format(java.util.Locale.US, v)

    fun targets(range: FloatRangeValue): Pair<String, String> {
        val from = if (reverse) range.end else range.start
        val to = if (reverse) range.start else range.end
        return "${f(from)}f" to "${f(to)}f"
    }

    val imports = buildList {
        add("import androidx.compose.animation.core.*")
        add("import androidx.compose.foundation.Canvas")
        add("import androidx.compose.foundation.layout.size")
        add("import androidx.compose.runtime.*")
        add("import androidx.compose.ui.Modifier")
        add("import androidx.compose.ui.geometry.Offset")
        add("import androidx.compose.ui.graphics.Color")
        add("import androidx.compose.ui.graphics.asComposePath")
        add("import androidx.compose.ui.graphics.drawscope.Fill")
        add("import androidx.compose.ui.graphics.drawscope.rotate")
        add("import androidx.compose.ui.graphics.drawscope.scale")
        add("import androidx.compose.ui.graphics.drawscope.translate")
        add("import androidx.compose.ui.unit.dp")
        add("import androidx.graphics.shapes.Morph")
        if (needsExpressive) {
            add("import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
            add("import androidx.compose.material3.MaterialShapes")
        }
        if (needsCustomPolygon) {
            add("import androidx.graphics.shapes.CornerRounding")
            add("import androidx.graphics.shapes.RoundedPolygon")
        }
    }.joinToString("\n")

    val optIn = if (needsExpressive) "@OptIn(ExperimentalMaterial3ExpressiveApi::class)\n" else ""
    val springSpec =
        "spring(dampingRatio = ${d}f, stiffness = ${effectiveK}f)"
    val tweenSpec =
        "tween($durationMs, easing = ${easing.composeRef})"

    fun loopBlock(label: String, from: String, to: String, useSpring: Boolean): String {
        val spec = if (useSpring) springSpec else tweenSpec
        return """
    val $label by animateFloatAsState(
        targetValue = if (playing) $to else $from,
        animationSpec = infiniteRepeatable(
            animation = $spec,
            repeatMode = RepeatMode.Reverse
        ),
        label = "$label"
    )""".trimIndent()
    }

    val (morphFrom, morphTo) = targets(morphRange)
    val (rotateFrom, rotateTo) = targets(rotateRange)
    val (scaleFrom, scaleTo) = targets(scaleRange)
    val (alphaFrom, alphaTo) = targets(alphaRange)
    val (colorFrom, colorTo) = targets(colorRange)
    val (offsetXFrom, offsetXTo) = targets(offsetXRange)
    val (offsetYFrom, offsetYTo) = targets(offsetYRange)

    val animationBlocks = buildList {
        if (AnimationKind.Morph in enabledAnimations) {
            add(loopBlock("morphProgress", morphFrom, morphTo, useSpring = false))
        } else {
            add("    val morphProgress = $morphFrom")
        }
        if (AnimationKind.Rotate in enabledAnimations) {
            add(
                """
    val rotation by animateFloatAsState(
        targetValue = if (playing) $rotateTo else $rotateFrom,
        animationSpec = infiniteRepeatable(animation = $tweenSpec),
        label = "rotation"
    )""".trimIndent()
            )
        } else {
            add("    val rotation = $rotateFrom")
        }
        if (AnimationKind.Scale in enabledAnimations) {
            add(loopBlock("scale", scaleFrom, scaleTo, useSpring = true))
        } else {
            add("    val scale = $scaleFrom")
        }
        if (AnimationKind.Alpha in enabledAnimations) {
            add(loopBlock("alpha", alphaFrom, alphaTo, useSpring = true))
        } else {
            add("    val alpha = $alphaFrom")
        }
        if (AnimationKind.Colour in enabledAnimations) {
            add(loopBlock("colorBlend", colorFrom, colorTo, useSpring = false))
        } else {
            add("    val colorBlend = $colorFrom")
        }
        if (AnimationKind.Position in enabledAnimations) {
            add(loopBlock("offsetX", offsetXFrom, offsetXTo, useSpring = true))
            add(loopBlock("offsetY", offsetYFrom, offsetYTo, useSpring = true))
        } else {
            add("    val offsetX = $offsetXFrom")
            add("    val offsetY = $offsetYFrom")
        }
    }.joinToString("\n")

    val dir = if (reverse) "Reverse" else "Forward"
    return """
// M3ShapeMake Preview
// $shapeALabel -> $shapeBLabel
// Spring: stiffness=$k damping=$d mass=$m (k_eff=$effectiveK) · $dir · Easing: ${easing.label}
$imports

${optIn}@Composable
fun M3AnimationStudioPreview(
    playing: Boolean = true,
    modifier: Modifier = Modifier
) {
$animationBlocks

    val polygonA = remember { $shapeARef }
    val polygonB = remember { $shapeBRef }
    val morph = remember(polygonA, polygonB) { Morph(polygonA, polygonB) }
    val fill = androidx.compose.ui.graphics.lerp(Color($colorHex), Color($targetHex), colorBlend)

    Canvas(modifier = modifier.size(256.dp)) {
        val path = morph.toPath(progress = morphProgress).asComposePath()
        val matrixScale = size.width / 256f * scale
        translate(offsetX, offsetY) {
            rotate(rotation, pivot = Offset(size.width / 2f, size.height / 2f)) {
                scale(matrixScale, matrixScale, pivot = Offset.Zero) {
                    drawPath(path, color = fill.copy(alpha = alpha), style = Fill)
                }
            }
        }
    }
}
""".trimIndent()
}

fun generateDrawnShapeExport(config: DrawnPolygonConfig, fillColor: Color): String {
    val colorHex = "#%08X".format(fillColor.toArgb())
    val polygonRef = drawnPolygonComposeReference(config)
    return """
// Gezeichnetes Polygon – Compose Export
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

@Composable
fun DrawnShapePreview(modifier: Modifier = Modifier) {
    val polygon = remember {
        $polygonRef
    }
    Canvas(modifier = modifier.size(256.dp)) {
        val path = polygon.toPath().asComposePath()
        drawPath(path, color = Color($colorHex), style = Fill)
    }
}
""".trimIndent()
}

fun generateShapeComposePreview(source: ShapeSource, fillColor: Color): String {
    if (source is ShapeSource.Drawn) {
        return generateDrawnShapeExport(source.config, fillColor)
    }
    val colorHex = "#%08X".format(fillColor.toArgb())
    val shapeRef = shapeComposeReference(source)
    val label = shapeLabel(source)
    val needsExpressive = source is ShapeSource.Material
    val needsCustomPolygon = source is ShapeSource.Custom
    val imports = buildList {
        add("import androidx.compose.foundation.Canvas")
        add("import androidx.compose.foundation.layout.size")
        add("import androidx.compose.runtime.Composable")
        add("import androidx.compose.runtime.remember")
        add("import androidx.compose.ui.Modifier")
        add("import androidx.compose.ui.graphics.Color")
        add("import androidx.compose.ui.graphics.asComposePath")
        add("import androidx.compose.ui.graphics.drawscope.Fill")
        add("import androidx.compose.ui.unit.dp")
        add("import androidx.graphics.shapes.toPath")
        if (needsExpressive) {
            add("import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
            add("import androidx.compose.material3.MaterialShapes")
        }
        if (needsCustomPolygon) {
            add("import androidx.graphics.shapes.CornerRounding")
            add("import androidx.graphics.shapes.RoundedPolygon")
        }
    }.joinToString("\n")
    val optIn = if (needsExpressive) "@OptIn(ExperimentalMaterial3ExpressiveApi::class)\n" else ""
    return """
// $label – Compose Preview
$imports

${optIn}@Composable
fun ShapePreview(modifier: Modifier = Modifier) {
    val polygon = remember { $shapeRef }
    Canvas(modifier = modifier.size(256.dp)) {
        val path = polygon.toPath().asComposePath()
        drawPath(path, color = Color($colorHex), style = Fill)
    }
}
""".trimIndent()
}
