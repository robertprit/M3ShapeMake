package com.m3shapes.editor

object M3ShapeAssetValidator {
    fun validateBundle(bundle: M3ShapeMakerAssetBundle): M3ValidationResult {
        val diagnostics = mutableListOf<M3ImportDiagnostic>()

        if (bundle.format != M3_SHAPE_ASSET_FORMAT) {
            diagnostics += error(
                "FORMAT_MISMATCH",
                "format must be '$M3_SHAPE_ASSET_FORMAT'",
                jsonPath = "$.format"
            )
        }
        if (bundle.schemaVersion != M3_SHAPE_ASSET_SCHEMA_VERSION) {
            diagnostics += error(
                "SCHEMA_VERSION",
                "schemaVersion must be $M3_SHAPE_ASSET_SCHEMA_VERSION",
                jsonPath = "$.schemaVersion"
            )
        }
        if (bundle.bundleId.isBlank()) {
            diagnostics += error("BUNDLE_ID_BLANK", "bundleId must not be blank", jsonPath = "$.bundleId")
        }
        if (bundle.namespace.isBlank()) {
            diagnostics += error("NAMESPACE_BLANK", "namespace must not be blank", jsonPath = "$.namespace")
        }

        val shapeIds = mutableSetOf<String>()
        bundle.shapes.forEachIndexed { index, shape ->
            val path = "$.shapes[$index]"
            if (shape.id.isBlank()) {
                diagnostics += error("SHAPE_ID_BLANK", "Shape id must not be blank", jsonPath = "$path.id")
            } else {
                val effectiveId = "${shape.namespace}/${shape.id}"
                if (!shapeIds.add(effectiveId)) {
                    diagnostics += error(
                        "SHAPE_ID_DUPLICATE",
                        "Duplicate shape id '$effectiveId'",
                        assetId = shape.id,
                        jsonPath = "$path.id"
                    )
                }
            }
            if (shape.namespace.isBlank()) {
                diagnostics += error(
                    "SHAPE_NAMESPACE_BLANK",
                    "Shape namespace must not be blank",
                    assetId = shape.id,
                    jsonPath = "$path.namespace"
                )
            }
            if (shape.pathData.isBlank()) {
                diagnostics += error(
                    "PATH_DATA_BLANK",
                    "pathData must not be blank",
                    assetId = shape.id,
                    jsonPath = "$path.pathData"
                )
            }
            if (!shape.viewport.width.isFinitePositive() || !shape.viewport.height.isFinitePositive()) {
                diagnostics += error(
                    "VIEWPORT_INVALID",
                    "viewport width/height must be finite and > 0",
                    assetId = shape.id,
                    jsonPath = "$path.viewport"
                )
            }
            shape.vertices.forEachIndexed { vi, vertex ->
                val vPath = "$path.vertices[$vi]"
                if (vertex.id.isBlank()) {
                    diagnostics += error(
                        "VERTEX_ID_BLANK",
                        "Vertex id must not be blank",
                        assetId = shape.id,
                        jsonPath = "$vPath.id"
                    )
                }
                if (!vertex.x.isFiniteNumber() || !vertex.y.isFiniteNumber() ||
                    !vertex.rounding.isFiniteNumber() || !vertex.smoothing.isFiniteNumber()
                ) {
                    diagnostics += error(
                        "VERTEX_NAN",
                        "Vertex coordinates/rounding/smoothing must be finite",
                        assetId = shape.id,
                        jsonPath = vPath
                    )
                }
            }
        }

        val knownShapeIds = bundle.shapes.map { it.id }.filter { it.isNotBlank() }.toSet()

        bundle.animations.forEachIndexed { index, sequence ->
            val path = "$.animations[$index]"
            if (sequence.id.isBlank()) {
                diagnostics += error(
                    "ANIM_ID_BLANK",
                    "Animation sequence id must not be blank",
                    jsonPath = "$path.id"
                )
            }
            sequence.steps.forEachIndexed { si, step ->
                val sPath = "$path.steps[$si]"
                if (step.durationMs <= 0) {
                    diagnostics += error(
                        "DURATION_INVALID",
                        "durationMs must be > 0",
                        assetId = step.id,
                        jsonPath = "$sPath.durationMs"
                    )
                }
                if (step.holdMs < 0) {
                    diagnostics += error(
                        "HOLD_INVALID",
                        "holdMs must be >= 0",
                        assetId = step.id,
                        jsonPath = "$sPath.holdMs"
                    )
                }
                listOf(
                    step.stiffness, step.damping, step.mass,
                    step.morphStart, step.morphEnd,
                    step.rotateStart, step.rotateEnd,
                    step.scaleStart, step.scaleEnd,
                    step.alphaStart, step.alphaEnd,
                    step.colorStart, step.colorEnd,
                    step.offsetXStart, step.offsetXEnd,
                    step.offsetYStart, step.offsetYEnd,
                    step.morphProgress, step.rotation, step.scale, step.alpha,
                    step.wobble, step.bounce
                ).forEachIndexed { fi, value ->
                    if (!value.isFiniteNumber()) {
                        diagnostics += error(
                            "ANIM_FLOAT_NAN",
                            "Animation float fields must be finite (index $fi)",
                            assetId = step.id,
                            jsonPath = sPath
                        )
                        return@forEachIndexed
                    }
                }
                step.animations.forEach { anim ->
                    if (AnimationKind.entries.none { it.name.equals(anim, ignoreCase = true) ||
                            it.label.equals(anim, ignoreCase = true) }
                    ) {
                        diagnostics += warning(
                            "UNKNOWN_ANIMATION",
                            "Unknown animation type: $anim",
                            assetId = step.id,
                            jsonPath = "$sPath.animations"
                        )
                    }
                }
                if (StudioEasing.entries.none { it.name.equals(step.easing, ignoreCase = true) ||
                        it.label.equals(step.easing, ignoreCase = true) }
                ) {
                    diagnostics += warning(
                        "UNKNOWN_EASING",
                        "Unknown easing: ${step.easing}",
                        assetId = step.id,
                        jsonPath = "$sPath.easing"
                    )
                }
            }
        }

        bundle.blockVisuals.forEachIndexed { index, block ->
            validateVisualRefs(
                diagnostics,
                knownShapeIds,
                block.id,
                block.shapeAssetId,
                "$.blockVisuals[$index]",
                block.dockingZones.map { it.bounds },
                block.sockets.map { it.bounds },
                block.textAnchors.map { it.bounds },
                connectionPoints = emptyList()
            )
        }

        bundle.flowNodeVisuals.forEachIndexed { index, node ->
            validateVisualRefs(
                diagnostics,
                knownShapeIds,
                node.id,
                node.shapeAssetId,
                "$.flowNodeVisuals[$index]",
                emptyList(),
                emptyList(),
                node.labelAnchors.map { it.bounds },
                connectionPoints = node.connectionPoints
            )
        }

        val hasErrors = diagnostics.any { it.severity == M3DiagnosticSeverity.Error }
        if (!hasErrors) {
            diagnostics += M3ImportDiagnostic(
                severity = M3DiagnosticSeverity.Info,
                code = "OK",
                message = "${bundle.shapes.size} shapes, " +
                    "${bundle.animations.size} animation sequence(s), " +
                    "${bundle.blockVisuals.size} block visuals, " +
                    "${bundle.flowNodeVisuals.size} flow node visuals."
            )
        }
        return M3ValidationResult(valid = !hasErrors, diagnostics = diagnostics)
    }

    private fun validateVisualRefs(
        diagnostics: MutableList<M3ImportDiagnostic>,
        knownShapeIds: Set<String>,
        assetId: String,
        shapeAssetId: String,
        path: String,
        dockingRects: List<M3Rect>,
        socketRects: List<M3Rect>,
        textRects: List<M3Rect>,
        connectionPoints: List<M3ConnectionPoint>
    ) {
        if (shapeAssetId.isBlank()) {
            diagnostics += error(
                "SHAPE_REF_BLANK",
                "shapeAssetId must not be blank",
                assetId = assetId,
                jsonPath = "$path.shapeAssetId"
            )
        } else if (shapeAssetId !in knownShapeIds) {
            diagnostics += error(
                "SHAPE_REF_MISSING",
                "Referenced shapeAssetId '$shapeAssetId' does not exist",
                assetId = assetId,
                jsonPath = "$path.shapeAssetId"
            )
        }
        (dockingRects + socketRects + textRects).forEachIndexed { i, rect ->
            if (!rect.width.isFinitePositive() || !rect.height.isFinitePositive() ||
                !rect.x.isFiniteNumber() || !rect.y.isFiniteNumber()
            ) {
                diagnostics += error(
                    "RECT_INVALID",
                    "Rect must have finite positive width/height",
                    assetId = assetId,
                    jsonPath = "$path.rects[$i]"
                )
            }
        }
        connectionPoints.forEachIndexed { i, point ->
            if (point.position !in 0f..1f || !point.position.isFiniteNumber()) {
                diagnostics += error(
                    "CONNECTION_POSITION",
                    "Connection position must be in 0f..1f",
                    assetId = assetId,
                    jsonPath = "$path.connectionPoints[$i].position"
                )
            }
        }
    }

    private fun error(
        code: String,
        message: String,
        assetId: String? = null,
        jsonPath: String? = null
    ) = M3ImportDiagnostic(M3DiagnosticSeverity.Error, code, message, assetId, jsonPath)

    private fun warning(
        code: String,
        message: String,
        assetId: String? = null,
        jsonPath: String? = null
    ) = M3ImportDiagnostic(M3DiagnosticSeverity.Warning, code, message, assetId, jsonPath)
}

private fun Float.isFiniteNumber(): Boolean = !isNaN() && isFinite()

private fun Float.isFinitePositive(): Boolean = isFiniteNumber() && this > 0f
