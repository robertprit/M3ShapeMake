package com.m3shapes.editor

fun ShapeSource.toM3ShapeAsset(
    namespace: String,
    id: String,
    name: String,
    pathData: String
): M3ShapeAsset {
    val kind = when (this) {
        is ShapeSource.Material -> M3ShapeAssetKind.MaterialPreset
        is ShapeSource.Custom -> M3ShapeAssetKind.ParametricPolygon
        is ShapeSource.Drawn -> M3ShapeAssetKind.DrawnPolygon
    }
    val vertices = when (this) {
        is ShapeSource.Drawn -> ensureDrawnCornerIds(config).toEditableVertices().map {
            M3Vertex(
                id = it.id,
                x = it.x,
                y = it.y,
                rounding = it.rounding,
                smoothing = it.smoothing,
                locked = it.locked
            )
        }
        else -> emptyList()
    }
    val metadata = when (this) {
        is ShapeSource.Material -> mapOf(
            "materialId" to shapeId.name,
            "composeRef" to shapeId.composeRef
        )
        is ShapeSource.Custom -> mapOf(
            "numVertices" to config.numVertices.toString(),
            "rounding" to config.rounding.toString(),
            "smoothing" to config.smoothing.toString()
        )
        is ShapeSource.Drawn -> mapOf(
            "defaultRounding" to config.rounding.toString(),
            "defaultSmoothing" to config.smoothing.toString(),
            "vertexCount" to config.vertices.size.toString()
        )
    }
    return M3ShapeAsset(
        namespace = namespace,
        id = id,
        name = name,
        kind = kind,
        pathData = pathData,
        vertices = vertices,
        metadata = metadata
    )
}

fun M3ShapeAsset.toShapeSource(): ShapeSource? = when (kind) {
    M3ShapeAssetKind.MaterialPreset -> {
        val materialName = metadata["materialId"]
        val shapeId = materialName?.let { name ->
            ExpressiveShapeId.entries.find { it.name == name }
        }
        shapeId?.let { ShapeSource.Material(it) }
    }
    M3ShapeAssetKind.ParametricPolygon -> {
        val num = metadata["numVertices"]?.toIntOrNull() ?: 4
        val rounding = metadata["rounding"]?.toFloatOrNull() ?: 0.5f
        val smoothing = metadata["smoothing"]?.toFloatOrNull() ?: 0f
        ShapeSource.Custom(
            FormConfig(
                numVertices = num.coerceIn(3, 12),
                rounding = rounding.coerceIn(0f, 1f),
                smoothing = smoothing.coerceIn(0f, 1f)
            )
        )
    }
    M3ShapeAssetKind.DrawnPolygon -> {
        if (vertices.size < 3) null
        else {
            val editable = vertices.map {
                EditableVertex(
                    id = it.id.ifBlank { "v0" },
                    x = it.x,
                    y = it.y,
                    rounding = it.rounding.coerceIn(0f, 1f),
                    smoothing = it.smoothing.coerceIn(0f, 1f),
                    locked = it.locked
                )
            }
            ShapeSource.Drawn(
                editable.toDrawnPolygonConfig(
                    fallbackRounding = metadata["defaultRounding"]?.toFloatOrNull() ?: 0f,
                    fallbackSmoothing = metadata["defaultSmoothing"]?.toFloatOrNull() ?: 0f
                )
            )
        }
    }
    else -> null
}

fun AnimationStep.toM3AnimationStepAsset(): M3AnimationStepAsset = M3AnimationStepAsset(
    id = id,
    name = name,
    durationMs = durationMs.coerceAtLeast(1),
    easing = easing.name,
    animations = enabledAnimations.map { it.name },
    stiffness = stiffness,
    damping = damping,
    mass = mass,
    reverse = reverse,
    morphStart = morphRange.start,
    morphEnd = morphRange.end,
    rotateStart = rotateRange.start,
    rotateEnd = rotateRange.end,
    scaleStart = scaleRange.start,
    scaleEnd = scaleRange.end,
    alphaStart = alphaRange.start,
    alphaEnd = alphaRange.end,
    colorStart = colorRange.start,
    colorEnd = colorRange.end,
    offsetXStart = offsetXRange.start,
    offsetXEnd = offsetXRange.end,
    offsetYStart = offsetYRange.start,
    offsetYEnd = offsetYRange.end,
    morphProgress = morphRange.display(reverse),
    rotation = rotateRange.display(reverse),
    scale = scaleRange.display(reverse),
    alpha = alphaRange.display(reverse),
    wobble = 0f,
    bounce = 0f,
    holdMs = holdMs.coerceAtLeast(0)
)

fun M3AnimationStepAsset.toAnimationStep(): AnimationStep {
    val kinds = animations.mapNotNull { raw ->
        AnimationKind.entries.find {
            it.name.equals(raw, ignoreCase = true) || it.label.equals(raw, ignoreCase = true)
        }
    }.toSet()
    val easingValue = StudioEasing.entries.find {
        it.name.equals(easing, ignoreCase = true) || it.label.equals(easing, ignoreCase = true)
    } ?: StudioEasing.EaseInOut
    return AnimationStep(
        id = id.ifBlank { "step_imported" },
        name = name.ifBlank { id.ifBlank { "Imported Step" } },
        enabledAnimations = kinds,
        stiffness = stiffness.coerceIn(50f, 10_000f),
        damping = damping.coerceIn(0.05f, 1.5f),
        mass = mass.coerceIn(0.1f, 10f),
        easing = easingValue,
        reverse = reverse,
        morphRange = FloatRangeValue(morphStart, morphEnd).coerced(0f..1f),
        rotateRange = FloatRangeValue(rotateStart, rotateEnd).coerced(0f..360f),
        scaleRange = FloatRangeValue(scaleStart, scaleEnd).coerced(0.5f..2f),
        alphaRange = FloatRangeValue(alphaStart, alphaEnd).coerced(0.05f..1f),
        colorRange = FloatRangeValue(colorStart, colorEnd).coerced(0f..1f),
        offsetXRange = FloatRangeValue(offsetXStart, offsetXEnd).coerced(-80f..80f),
        offsetYRange = FloatRangeValue(offsetYStart, offsetYEnd).coerced(-80f..80f),
        holdMs = holdMs.coerceAtLeast(0)
    )
}

fun AnimationSequence.toM3AnimationSequenceAsset(
    namespace: String,
    id: String,
    name: String
): M3AnimationSequenceAsset = M3AnimationSequenceAsset(
    namespace = namespace,
    id = id,
    name = name,
    steps = steps.map { it.toM3AnimationStepAsset() }
)

fun M3AnimationSequenceAsset.toAnimationSteps(): List<AnimationStep> =
    steps.map { it.toAnimationStep() }

fun createBundleFromCurrentSession(
    namespace: String,
    bundleId: String,
    shapeA: ShapeSource,
    shapeB: ShapeSource,
    resolvedAPathData: String,
    resolvedBPathData: String,
    animationSteps: List<AnimationStep>
): M3ShapeMakerAssetBundle {
    val ns = namespace.trim().ifBlank { "m3shapes.local" }
    val bid = bundleId.trim().ifBlank { "bundle_${System.currentTimeMillis()}" }
    val shapes = listOf(
        shapeA.toM3ShapeAsset(ns, "shape_a", shapeLabel(shapeA), resolvedAPathData),
        shapeB.toM3ShapeAsset(ns, "shape_b", shapeLabel(shapeB), resolvedBPathData)
    )
    val animations = if (animationSteps.isEmpty()) {
        emptyList()
    } else {
        listOf(
            AnimationSequence(
                id = "main_sequence",
                name = "Main Sequence",
                steps = animationSteps
            ).toM3AnimationSequenceAsset(ns, "main_sequence", "Main Sequence")
        )
    }
    return M3ShapeMakerAssetBundle(
        bundleId = bid,
        namespace = ns,
        shapes = shapes,
        animations = animations
    )
}

data class M3BundleRestoreResult(
    val shapeA: ShapeSource?,
    val shapeB: ShapeSource?,
    val modeA: ShapeMode?,
    val modeB: ShapeMode?,
    val animationSteps: List<AnimationStep>,
    val diagnostics: List<M3ImportDiagnostic>
)

fun restoreSessionFromBundle(bundle: M3ShapeMakerAssetBundle): M3BundleRestoreResult {
    val diagnostics = mutableListOf<M3ImportDiagnostic>()
    val shapeAAsset = bundle.shapes.find { it.id == "shape_a" } ?: bundle.shapes.getOrNull(0)
    val shapeBAsset = bundle.shapes.find { it.id == "shape_b" } ?: bundle.shapes.getOrNull(1)

    fun restoreShape(asset: M3ShapeAsset?, label: String): Pair<ShapeSource?, ShapeMode?> {
        if (asset == null) {
            diagnostics += M3ImportDiagnostic(
                M3DiagnosticSeverity.Warning,
                "SHAPE_MISSING",
                "No $label found in bundle"
            )
            return null to null
        }
        val source = asset.toShapeSource()
        if (source == null) {
            diagnostics += M3ImportDiagnostic(
                M3DiagnosticSeverity.Warning,
                "SHAPE_UNMAPPED",
                "Could not restore $label (${asset.kind})",
                assetId = asset.id
            )
            return null to null
        }
        val mode = when (source) {
            is ShapeSource.Material -> ShapeMode.Material
            is ShapeSource.Custom -> ShapeMode.Parametric
            is ShapeSource.Drawn -> ShapeMode.Editor
        }
        return source to mode
    }

    val (shapeA, modeA) = restoreShape(shapeAAsset, "shape_a")
    val (shapeB, modeB) = restoreShape(shapeBAsset, "shape_b")

    val sequence = bundle.animations.find { it.id == "main_sequence" } ?: bundle.animations.firstOrNull()
    val steps = sequence?.toAnimationSteps().orEmpty()
    if (bundle.animations.isNotEmpty() && steps.isEmpty()) {
        diagnostics += M3ImportDiagnostic(
            M3DiagnosticSeverity.Warning,
            "ANIM_EMPTY",
            "Animation sequence present but no steps restored"
        )
    }

    return M3BundleRestoreResult(
        shapeA = shapeA,
        shapeB = shapeB,
        modeA = modeA,
        modeB = modeB,
        animationSteps = steps,
        diagnostics = diagnostics
    )
}
