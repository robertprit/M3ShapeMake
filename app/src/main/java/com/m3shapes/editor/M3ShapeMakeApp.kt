package com.m3shapes.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.graphics.shapes.Morph
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3ShapeMakeApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val assetRepository = remember { FileM3ShapeAssetRepository(context.applicationContext) }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    var shapeA by remember { mutableStateOf<ShapeSource>(ShapeSource.Material(ExpressiveShapeId.Square)) }
    var shapeB by remember { mutableStateOf<ShapeSource>(ShapeSource.Material(ExpressiveShapeId.Circle)) }
    var modeA by remember { mutableStateOf(ShapeMode.Material) }
    var modeB by remember { mutableStateOf(ShapeMode.Material) }
    var customA by remember { mutableStateOf(FormConfig(numVertices = 4, rounding = 0.9f, smoothing = 1.0f)) }
    var customB by remember { mutableStateOf(FormConfig(numVertices = 12, rounding = 0.5f, smoothing = 0.5f)) }
    var drawnA by remember { mutableStateOf(defaultDrawnPolygon(4)) }
    var drawnB by remember { mutableStateOf(defaultDrawnPolygon(6)) }
    var freehandModeA by remember { mutableStateOf(false) }
    var freehandModeB by remember { mutableStateOf(false) }
    var overlayA by remember { mutableStateOf(EditorOverlay.Grid4) }
    var overlayB by remember { mutableStateOf(EditorOverlay.Grid4) }
    var formTab by remember { mutableIntStateOf(0) }

    var assetNamespace by rememberSaveable { mutableStateOf("visualtasker.core") }
    var assetBundleId by rememberSaveable { mutableStateOf("my_shapes") }
    var assetStatusText by remember { mutableStateOf("") }

    var timelineManualProgress by remember { mutableFloatStateOf(0f) }
    var morphRange by remember { mutableStateOf(FloatRangeValue(0f, 1f)) }
    var rotateRange by remember { mutableStateOf(FloatRangeValue(0f, 360f)) }
    var scaleRange by remember { mutableStateOf(FloatRangeValue(0.88f, 1.18f)) }
    var alphaRange by remember { mutableStateOf(FloatRangeValue(0.45f, 1f)) }
    var colorRange by remember { mutableStateOf(FloatRangeValue(0f, 1f)) }
    var offsetXRange by remember { mutableStateOf(FloatRangeValue(-28f, 28f)) }
    var offsetYRange by remember { mutableStateOf(FloatRangeValue(-22f, 22f)) }
    var enabledAnimations by remember {
        mutableStateOf(setOf(AnimationKind.Morph, AnimationKind.Rotate))
    }
    var studioSelectedColor by remember { mutableStateOf(studioColors[0]) }
    var animTargetColor by remember { mutableStateOf(studioColors[5]) }
    var springStiffness by remember { mutableFloatStateOf(400f) }
    var springDamping by remember { mutableFloatStateOf(0.75f) }
    var springMass by remember { mutableFloatStateOf(1f) }
    var selectedEasing by remember { mutableStateOf(StudioEasing.EaseInOut) }
    var reverseAnimation by remember { mutableStateOf(false) }
    var loopPlaying by remember { mutableStateOf(false) }

    var useMorphSteps by remember { mutableStateOf(false) }
    var morphSteps by remember { mutableStateOf(listOf(0f, 1f)) }
    var selectedMorphStepIndex by remember { mutableIntStateOf(0) }

    var animationSteps by remember { mutableStateOf(emptyList<AnimationStep>()) }
    var sequencePlaying by remember { mutableStateOf(false) }
    var selectedStepId by remember { mutableStateOf<String?>(null) }
    var nextStepIndex by remember { mutableIntStateOf(1) }

    fun applyAnimationStep(step: AnimationStep) {
        enabledAnimations = step.enabledAnimations
        springStiffness = step.stiffness
        springDamping = step.damping
        springMass = step.mass
        selectedEasing = step.easing
        reverseAnimation = step.reverse
        morphRange = step.morphRange
        rotateRange = step.rotateRange
        scaleRange = step.scaleRange
        alphaRange = step.alphaRange
        colorRange = step.colorRange
        offsetXRange = step.offsetXRange
        offsetYRange = step.offsetYRange
        timelineManualProgress = step.morphRange.display(step.reverse)
        loopPlaying = false
        selectedStepId = step.id
    }

    val resolvedA = remember(shapeA, modeA, customA, drawnA) {
        resolveShape(sourceForMode(modeA, shapeA, customA, drawnA))
    }
    val resolvedB = remember(shapeB, modeB, customB, drawnB) {
        resolveShape(sourceForMode(modeB, shapeB, customB, drawnB))
    }
    val exportSourceA = sourceForMode(modeA, shapeA, customA, drawnA)
    val exportSourceB = sourceForMode(modeB, shapeB, customB, drawnB)

    fun buildCurrentBundle(): M3ShapeMakerAssetBundle = createBundleFromCurrentSession(
        namespace = assetNamespace,
        bundleId = assetBundleId,
        shapeA = exportSourceA,
        shapeB = exportSourceB,
        resolvedAPathData = resolvedA.toVpPathData(),
        resolvedBPathData = resolvedB.toVpPathData(),
        animationSteps = animationSteps
    )

    fun formatDiagnostics(result: M3ValidationResult, extra: List<M3ImportDiagnostic> = emptyList()): String {
        val all = result.diagnostics + extra
        val body = buildString {
            append(result.summaryLine())
            val infos = all.filter { it.severity == M3DiagnosticSeverity.Info }
            infos.forEach { append("\n").append(it.message) }
            val issues = all.filter { it.severity != M3DiagnosticSeverity.Info }
            if (issues.isNotEmpty()) {
                append("\n")
                issues.take(8).forEach { d ->
                    append("\n- ").append(d.message)
                }
                if (issues.size > 8) append("\n- … ${issues.size - 8} more")
            }
        }
        return body
    }

    fun applyRestoredShape(source: ShapeSource, forA: Boolean) {
        when (source) {
            is ShapeSource.Material -> {
                if (forA) {
                    shapeA = source
                    modeA = ShapeMode.Material
                } else {
                    shapeB = source
                    modeB = ShapeMode.Material
                }
            }
            is ShapeSource.Custom -> {
                if (forA) {
                    customA = source.config
                    modeA = ShapeMode.Parametric
                } else {
                    customB = source.config
                    modeB = ShapeMode.Parametric
                }
            }
            is ShapeSource.Drawn -> {
                if (forA) {
                    drawnA = fitDrawnPolygonConfig(ensureDrawnCornerIds(source.config))
                    modeA = ShapeMode.Editor
                } else {
                    drawnB = fitDrawnPolygonConfig(ensureDrawnCornerIds(source.config))
                    modeB = ShapeMode.Editor
                }
            }
        }
    }

    val morphAnim = remember { Animatable(0f) }
    val rotationAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }
    val alphaAnim = remember { Animatable(1f) }
    val colorAnim = remember { Animatable(0f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }

    LaunchedEffect(
        loopPlaying,
        enabledAnimations,
        springStiffness,
        springDamping,
        springMass,
        selectedEasing,
        reverseAnimation,
        useMorphSteps,
        morphSteps,
        morphRange,
        rotateRange,
        scaleRange,
        alphaRange,
        colorRange,
        offsetXRange,
        offsetYRange
    ) {
        if (!loopPlaying || sequencePlaying) return@LaunchedEffect

        fun pingPongTargets(range: FloatRangeValue): Pair<Float, Float> {
            val from = if (reverseAnimation) range.end else range.start
            val to = if (reverseAnimation) range.start else range.end
            return from to to
        }

        val (morphFrom, morphTo) = pingPongTargets(morphRange)
        val (rotateFrom, rotateTo) = pingPongTargets(rotateRange)
        val (scaleFrom, scaleTo) = pingPongTargets(scaleRange)
        val (alphaFrom, alphaTo) = pingPongTargets(alphaRange)
        val (colorFrom, colorTo) = pingPongTargets(colorRange)
        val (offsetXFrom, offsetXTo) = pingPongTargets(offsetXRange)
        val (offsetYFrom, offsetYTo) = pingPongTargets(offsetYRange)

        morphAnim.snapTo(morphFrom)
        rotationAnim.snapTo(rotateFrom)
        scaleAnim.snapTo(scaleFrom)
        alphaAnim.snapTo(alphaFrom)
        colorAnim.snapTo(colorFrom)
        offsetXAnim.snapTo(offsetXFrom)
        offsetYAnim.snapTo(offsetYFrom)

        val durationMs = estimateDurationMs(springStiffness, springMass)
        val tweenSpec = tween<Float>(durationMs, easing = selectedEasing.toEasing())
        val springSpec = spring<Float>(
            dampingRatio = springDamping,
            stiffness = springStiffnessForMass(springStiffness, springMass)
        )

        coroutineScope {
            if (AnimationKind.Morph in enabledAnimations) {
                launch {
                    while (true) {
                        val steps = morphSteps
                            .map { it.coerceIn(0f, 1f) }
                            .distinct()
                            .sorted()

                        if (useMorphSteps && steps.size >= 2) {
                            val ordered = if (reverseAnimation) steps.asReversed() else steps
                            val segmentMs = (durationMs / (ordered.size - 1)).coerceAtLeast(80)
                            val segmentSpec = tween<Float>(segmentMs, easing = selectedEasing.toEasing())
                            morphAnim.snapTo(ordered.first())
                            for (i in 0 until ordered.lastIndex) {
                                morphAnim.animateTo(ordered[i + 1], segmentSpec)
                            }
                            morphAnim.snapTo(ordered.first())
                        } else {
                            morphAnim.animateTo(morphTo, tweenSpec)
                            morphAnim.animateTo(morphFrom, tweenSpec)
                        }
                    }
                }
            }
            if (AnimationKind.Rotate in enabledAnimations) {
                launch {
                    while (true) {
                        rotationAnim.animateTo(rotateTo, tweenSpec)
                        rotationAnim.snapTo(rotateFrom)
                    }
                }
            }
            if (AnimationKind.Scale in enabledAnimations) {
                launch {
                    while (true) {
                        scaleAnim.animateTo(scaleTo, springSpec)
                        scaleAnim.animateTo(scaleFrom, springSpec)
                    }
                }
            }
            if (AnimationKind.Alpha in enabledAnimations) {
                launch {
                    while (true) {
                        alphaAnim.animateTo(alphaTo, springSpec)
                        alphaAnim.animateTo(alphaFrom, springSpec)
                    }
                }
            }
            if (AnimationKind.Colour in enabledAnimations) {
                launch {
                    while (true) {
                        colorAnim.animateTo(colorTo, tweenSpec)
                        colorAnim.animateTo(colorFrom, tweenSpec)
                    }
                }
            }
            if (AnimationKind.Position in enabledAnimations) {
                launch {
                    while (true) {
                        offsetXAnim.animateTo(offsetXTo, springSpec)
                        offsetXAnim.animateTo(offsetXFrom, springSpec)
                    }
                }
                launch {
                    while (true) {
                        offsetYAnim.animateTo(offsetYTo, springSpec)
                        offsetYAnim.animateTo(offsetYFrom, springSpec)
                    }
                }
            }
        }
    }

    LaunchedEffect(sequencePlaying) {
        if (!sequencePlaying) return@LaunchedEffect
        loopPlaying = false
        val stepsSnapshot = animationSteps
        for (step in stepsSnapshot) {
            applyAnimationStep(step)
            delay(step.durationMs.toLong())
            if (step.holdMs > 0) {
                delay(step.holdMs.toLong())
            }
        }
        sequencePlaying = false
    }

    val morphProgress = if (loopPlaying && AnimationKind.Morph in enabledAnimations) {
        morphAnim.value
    } else {
        timelineManualProgress
    }
    val canvasRotation = if (loopPlaying && AnimationKind.Rotate in enabledAnimations) {
        rotationAnim.value
    } else {
        rotateRange.display(reverseAnimation)
    }
    val canvasScale = if (loopPlaying && AnimationKind.Scale in enabledAnimations) {
        scaleAnim.value
    } else {
        scaleRange.display(reverseAnimation)
    }
    val canvasAlpha = if (loopPlaying && AnimationKind.Alpha in enabledAnimations) {
        alphaAnim.value
    } else {
        alphaRange.display(reverseAnimation)
    }
    val canvasColorBlend = if (loopPlaying && AnimationKind.Colour in enabledAnimations) {
        colorAnim.value
    } else {
        colorRange.display(reverseAnimation)
    }
    val canvasOffsetX = if (loopPlaying && AnimationKind.Position in enabledAnimations) {
        offsetXAnim.value
    } else {
        offsetXRange.display(reverseAnimation)
    }
    val canvasOffsetY = if (loopPlaying && AnimationKind.Position in enabledAnimations) {
        offsetYAnim.value
    } else {
        offsetYRange.display(reverseAnimation)
    }
    val canvasFillColor = androidx.compose.ui.graphics.lerp(
        studioSelectedColor,
        animTargetColor,
        canvasColorBlend
    )

    val morph = remember(resolvedA, resolvedB) { Morph(resolvedA, resolvedB) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("M3ShapeMake", fontWeight = FontWeight.Bold)
                        Text(
                            "Shape Editor · Animation Studio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Shape Editor") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Animation Studio") }
                )
            }

            when (selectedTab) {
                0 -> ShapeEditorTab(
                    formTab = formTab,
                    onFormTabChange = { formTab = it },
                    modeA = modeA,
                    modeB = modeB,
                    onModeAChange = { modeA = it },
                    onModeBChange = { modeB = it },
                    materialA = (shapeA as? ShapeSource.Material)?.shapeId ?: ExpressiveShapeId.Square,
                    materialB = (shapeB as? ShapeSource.Material)?.shapeId ?: ExpressiveShapeId.Circle,
                    onMaterialA = { shapeA = ShapeSource.Material(it) },
                    onMaterialB = { shapeB = ShapeSource.Material(it) },
                    customA = customA,
                    customB = customB,
                    onCustomAChange = { customA = it },
                    onCustomBChange = { customB = it },
                    drawnA = drawnA,
                    drawnB = drawnB,
                    onDrawnAChange = { drawnA = it },
                    onDrawnBChange = { drawnB = it },
                    freehandModeA = freehandModeA,
                    freehandModeB = freehandModeB,
                    onFreehandAChange = { freehandModeA = it },
                    onFreehandBChange = { freehandModeB = it },
                    overlayA = overlayA,
                    overlayB = overlayB,
                    onOverlayAChange = { overlayA = it },
                    onOverlayBChange = { overlayB = it },
                    resolvedA = resolvedA,
                    resolvedB = resolvedB,
                    exportSourceA = exportSourceA,
                    exportSourceB = exportSourceB,
                    studioSelectedColor = studioSelectedColor,
                    onStudioSelectedColorChange = { studioSelectedColor = it }
                )
                1 -> AnimationStudioTab(
                    morph = morph,
                    morphProgress = morphProgress,
                    canvasRotation = canvasRotation,
                    canvasScale = canvasScale,
                    canvasAlpha = canvasAlpha,
                    canvasOffsetX = canvasOffsetX,
                    canvasOffsetY = canvasOffsetY,
                    canvasFillColor = canvasFillColor,
                    loopPlaying = loopPlaying,
                    onLoopPlayingChange = {
                        if (it) sequencePlaying = false
                        loopPlaying = it
                    },
                    springStiffness = springStiffness,
                    onSpringStiffnessChange = { springStiffness = it },
                    springDamping = springDamping,
                    onSpringDampingChange = { springDamping = it },
                    springMass = springMass,
                    onSpringMassChange = { springMass = it },
                    reverseAnimation = reverseAnimation,
                    onReverseAnimationChange = { reverseAnimation = it },
                    enabledAnimations = enabledAnimations,
                    onEnabledAnimationsChange = { enabledAnimations = it },
                    selectedEasing = selectedEasing,
                    onSelectedEasingChange = { selectedEasing = it },
                    timelineManualProgress = timelineManualProgress,
                    onTimelineManualProgressChange = {
                        timelineManualProgress = it
                        loopPlaying = false
                    },
                    morphRange = morphRange,
                    onMorphRangeChange = {
                        morphRange = it
                        timelineManualProgress = it.display(reverseAnimation)
                        loopPlaying = false
                    },
                    rotateRange = rotateRange,
                    onRotateRangeChange = {
                        rotateRange = it
                        loopPlaying = false
                    },
                    scaleRange = scaleRange,
                    onScaleRangeChange = {
                        scaleRange = it
                        loopPlaying = false
                    },
                    alphaRange = alphaRange,
                    onAlphaRangeChange = {
                        alphaRange = it
                        loopPlaying = false
                    },
                    colorRange = colorRange,
                    onColorRangeChange = {
                        colorRange = it
                        loopPlaying = false
                    },
                    offsetXRange = offsetXRange,
                    onOffsetXRangeChange = {
                        offsetXRange = it
                        loopPlaying = false
                    },
                    offsetYRange = offsetYRange,
                    onOffsetYRangeChange = {
                        offsetYRange = it
                        loopPlaying = false
                    },
                    useMorphSteps = useMorphSteps,
                    onUseMorphStepsChange = { useMorphSteps = it },
                    morphSteps = morphSteps,
                    onMorphStepsChange = { morphSteps = it },
                    selectedMorphStepIndex = selectedMorphStepIndex,
                    onSelectedMorphStepIndexChange = { selectedMorphStepIndex = it },
                    animTargetColor = animTargetColor,
                    onAnimTargetColorChange = { animTargetColor = it },
                    animationSteps = animationSteps,
                    selectedStepId = selectedStepId,
                    sequencePlaying = sequencePlaying,
                    onAddCurrentStep = {
                        val step = createAnimationStepFromCurrentConfig(
                            stepNumber = nextStepIndex,
                            enabledAnimations = enabledAnimations,
                            stiffness = springStiffness,
                            damping = springDamping,
                            mass = springMass,
                            easing = selectedEasing,
                            reverse = reverseAnimation,
                            morphRange = morphRange,
                            rotateRange = rotateRange,
                            scaleRange = scaleRange,
                            alphaRange = alphaRange,
                            colorRange = colorRange,
                            offsetXRange = offsetXRange,
                            offsetYRange = offsetYRange
                        )
                        animationSteps = animationSteps + step
                        selectedStepId = step.id
                        nextStepIndex += 1
                    },
                    onApplyStep = { step ->
                        sequencePlaying = false
                        applyAnimationStep(step)
                    },
                    onDuplicateStep = { step ->
                        val copy = step.duplicated(
                            newId = "step_$nextStepIndex",
                            newName = "Step $nextStepIndex"
                        )
                        val index = animationSteps.indexOfFirst { it.id == step.id }
                        animationSteps = if (index >= 0) {
                            animationSteps.toMutableList().apply { add(index + 1, copy) }
                        } else {
                            animationSteps + copy
                        }
                        selectedStepId = copy.id
                        nextStepIndex += 1
                    },
                    onDeleteStep = { step ->
                        animationSteps = animationSteps.filterNot { it.id == step.id }
                        if (selectedStepId == step.id) selectedStepId = null
                    },
                    onMoveStepUp = { index ->
                        animationSteps = animationSteps.moveStep(index, -1)
                    },
                    onMoveStepDown = { index ->
                        animationSteps = animationSteps.moveStep(index, 1)
                    },
                    onPlaySequence = {
                        if (animationSteps.isEmpty()) return@AnimationStudioTab
                        loopPlaying = false
                        sequencePlaying = true
                    },
                    onStopSequence = { sequencePlaying = false },
                    onClearSequence = {
                        sequencePlaying = false
                        animationSteps = emptyList()
                        selectedStepId = null
                    },
                    assetNamespace = assetNamespace,
                    onAssetNamespaceChange = { assetNamespace = it },
                    assetBundleId = assetBundleId,
                    onAssetBundleIdChange = { assetBundleId = it },
                    assetStatusText = assetStatusText,
                    onCopyBundleJson = {
                        try {
                            val bundle = buildCurrentBundle()
                            val validation = M3ShapeAssetValidator.validateBundle(bundle)
                            val json = M3ShapeAssetCodec.encodeBundle(bundle)
                            copyToClipboard(context, "M3ShapeBundle", json, "Bundle JSON kopiert")
                            assetStatusText = formatDiagnostics(validation)
                        } catch (e: Exception) {
                            assetStatusText = "Copy failed: ${e.message}"
                            Toast.makeText(context, assetStatusText, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onValidateBundle = {
                        try {
                            val bundle = buildCurrentBundle()
                            val validation = M3ShapeAssetValidator.validateBundle(bundle)
                            assetStatusText = formatDiagnostics(validation)
                        } catch (e: Exception) {
                            assetStatusText = "Validate failed: ${e.message}"
                        }
                    },
                    onSaveLastBundle = {
                        scope.launch {
                            try {
                                val bundle = buildCurrentBundle()
                                val validation = M3ShapeAssetValidator.validateBundle(bundle)
                                if (!validation.valid) {
                                    assetStatusText = "Save blocked.\n" + formatDiagnostics(validation)
                                    Toast.makeText(context, "Bundle invalid – not saved", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                assetRepository.saveLastBundle(bundle)
                                assetStatusText = "Saved to ${FileM3ShapeAssetRepository.LAST_BUNDLE_FILENAME}\n" +
                                    formatDiagnostics(validation)
                                Toast.makeText(context, "Bundle gespeichert", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                assetStatusText = "Save failed: ${e.message}"
                                Toast.makeText(context, assetStatusText, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onLoadLastBundle = {
                        scope.launch {
                            try {
                                val bundle = assetRepository.loadLastBundle()
                                if (bundle == null) {
                                    assetStatusText = "No saved bundle found."
                                    Toast.makeText(context, assetStatusText, Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val validation = M3ShapeAssetValidator.validateBundle(bundle)
                                if (!validation.valid) {
                                    assetStatusText = "Load blocked.\n" + formatDiagnostics(validation)
                                    Toast.makeText(context, "Saved bundle invalid", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                assetNamespace = bundle.namespace
                                assetBundleId = bundle.bundleId
                                val restored = restoreSessionFromBundle(bundle)
                                restored.shapeA?.let { applyRestoredShape(it, forA = true) }
                                restored.shapeB?.let { applyRestoredShape(it, forA = false) }
                                if (restored.animationSteps.isNotEmpty()) {
                                    animationSteps = restored.animationSteps
                                    selectedStepId = restored.animationSteps.firstOrNull()?.id
                                    nextStepIndex = restored.animationSteps.size + 1
                                }
                                sequencePlaying = false
                                loopPlaying = false
                                assetStatusText = formatDiagnostics(validation, restored.diagnostics)
                                Toast.makeText(context, "Bundle geladen", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                assetStatusText = "Load failed: ${e.message}"
                                Toast.makeText(context, assetStatusText, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}
