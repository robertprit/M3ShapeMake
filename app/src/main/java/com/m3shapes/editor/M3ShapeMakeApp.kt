package com.m3shapes.editor

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.graphics.shapes.Morph
import kotlinx.coroutines.coroutineScope
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
    var durationMs by remember { mutableIntStateOf(1_200) }
    var profile by remember { mutableStateOf(StudioEasing.EaseInOut.profile()) }
    var reverseAnimation by remember { mutableStateOf(false) }
    var loopPlaying by remember { mutableStateOf(false) }

    var animationSteps by remember { mutableStateOf(emptyList<AnimationStep>()) }
    var sequencePlaying by remember { mutableStateOf(false) }
    var selectedStepId by remember { mutableStateOf<String?>(null) }
    var nextStepIndex by remember { mutableIntStateOf(1) }
    var sequencerAddKind by remember { mutableStateOf(AnimationKind.Morph) }
    var sequencerAddStartMs by remember { mutableIntStateOf(0) }
    var sequencerDirection by remember { mutableStateOf(SequencerDirection.Forward) }
    var sequencerPlayMode by remember { mutableStateOf(SequencerPlayMode.Once) }
    var sequencerPlayheadMs by remember { mutableIntStateOf(0) }

    fun applyAnimationStep(step: AnimationStep) {
        enabledAnimations = step.enabledAnimations
        durationMs = step.durationMs
        profile = step.profile
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

    fun applyValidatedBundle(bundle: M3ShapeMakerAssetBundle, validation: M3ValidationResult) {
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
    }

    fun importFailureMessage(error: Throwable): String {
        val msg = error.message.orEmpty()
        return when (error) {
            is IllegalArgumentException -> when {
                msg.contains("schemaVersion", ignoreCase = true) ->
                    "Import failed: unsupported schema"
                msg.contains("Unsupported format", ignoreCase = true) ||
                    msg.contains("Invalid bundle JSON", ignoreCase = true) ||
                    msg.contains("Failed to parse", ignoreCase = true) ->
                    "Import failed: invalid JSON"
                msg.contains("empty", ignoreCase = true) ->
                    "Import failed: empty file"
                else -> "Import failed: ${error.message ?: "invalid JSON"}"
            }
            else -> "Import failed: ${error.message ?: "unknown error"}"
        }
    }

    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            assetStatusText = "Export canceled"
            pendingExportJson = null
            return@rememberLauncherForActivityResult
        }
        val json = pendingExportJson
        pendingExportJson = null
        if (json == null) {
            assetStatusText = "Export failed: no bundle prepared"
            Toast.makeText(context, assetStatusText, Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        writeTextToUri(context, uri, json).fold(
            onSuccess = {
                assetStatusText = "Export successful"
                Toast.makeText(context, "Bundle exportiert", Toast.LENGTH_SHORT).show()
            },
            onFailure = { error ->
                assetStatusText = "Export failed: ${error.message ?: "unknown error"}"
                Toast.makeText(context, assetStatusText, Toast.LENGTH_SHORT).show()
            }
        )
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            assetStatusText = "Import canceled"
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                val json = readTextFromUri(context, uri).getOrElse { error ->
                    assetStatusText = "Import failed: ${error.message ?: "file not readable"}"
                    Toast.makeText(context, assetStatusText, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                if (json.isBlank()) {
                    assetStatusText = "Import failed: empty file"
                    Toast.makeText(context, assetStatusText, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val bundle = try {
                    M3ShapeAssetCodec.decodeBundle(json)
                } catch (e: IllegalArgumentException) {
                    assetStatusText = importFailureMessage(e)
                    Toast.makeText(context, assetStatusText, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val validation = M3ShapeAssetValidator.validateBundle(bundle)
                if (!validation.valid) {
                    assetStatusText = "Import failed: validation errors\n" +
                        formatDiagnostics(validation)
                    Toast.makeText(context, "Bundle ungültig", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                applyValidatedBundle(bundle, validation)
                assetStatusText = "Import successful\n$assetStatusText"
                Toast.makeText(context, "Bundle importiert", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                assetStatusText = importFailureMessage(e)
                Toast.makeText(context, assetStatusText, Toast.LENGTH_SHORT).show()
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
        durationMs,
        profile,
        reverseAnimation,
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

        val effectiveDurationMs = durationMs.coerceAtLeast(1)
        val tweenSpec = tween<Float>(effectiveDurationMs, easing = profile.toEasing())

        coroutineScope {
            if (AnimationKind.Morph in enabledAnimations) {
                launch {
                    while (true) {
                        morphAnim.animateTo(morphTo, tweenSpec)
                        morphAnim.animateTo(morphFrom, tweenSpec)
                    }
                }
            }
            if (AnimationKind.Rotate in enabledAnimations) {
                launch {
                    while (true) {
                        rotationAnim.animateTo(rotateTo, tweenSpec)
                        rotationAnim.animateTo(rotateFrom, tweenSpec)
                    }
                }
            }
            if (AnimationKind.Scale in enabledAnimations) {
                launch {
                    while (true) {
                        scaleAnim.animateTo(scaleTo, tweenSpec)
                        scaleAnim.animateTo(scaleFrom, tweenSpec)
                    }
                }
            }
            if (AnimationKind.Alpha in enabledAnimations) {
                launch {
                    while (true) {
                        alphaAnim.animateTo(alphaTo, tweenSpec)
                        alphaAnim.animateTo(alphaFrom, tweenSpec)
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
                        offsetXAnim.animateTo(offsetXTo, tweenSpec)
                        offsetXAnim.animateTo(offsetXFrom, tweenSpec)
                    }
                }
                launch {
                    while (true) {
                        offsetYAnim.animateTo(offsetYTo, tweenSpec)
                        offsetYAnim.animateTo(offsetYFrom, tweenSpec)
                    }
                }
            }
        }
    }

    LaunchedEffect(sequencePlaying) {
        if (!sequencePlaying) return@LaunchedEffect
        loopPlaying = false
        sequencerPlayheadMs = 0
        val stepsSnapshot = animationSteps.normalizedTimeline()
        if (stepsSnapshot.isEmpty()) {
            sequencePlaying = false
            return@LaunchedEffect
        }
        val totalMs = (stepsSnapshot.maxOfOrNull { it.endMs } ?: 0).coerceAtLeast(1)
        val cycleMs = when (sequencerDirection) {
            SequencerDirection.PingPong -> totalMs * 2
            else -> totalMs
        }.coerceAtLeast(1)

        fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
        suspend fun applyTimelineAt(timeMs: Int) {
            var m = morphRange.display(reverseAnimation)
            var r = rotateRange.display(reverseAnimation)
            var s = scaleRange.display(reverseAnimation)
            var a = alphaRange.display(reverseAnimation)
            var c = colorRange.display(reverseAnimation)
            var ox = offsetXRange.display(reverseAnimation)
            var oy = offsetYRange.display(reverseAnimation)

            AnimationKind.entries.forEach { kind ->
                val step = stepsSnapshot
                    .filter { it.kindOrFallback() == kind }
                    .lastOrNull { timeMs in it.startMs until it.endMs }
                if (step != null) {
                    val duration = (step.endMs - step.startMs).coerceAtLeast(1)
                    val raw = ((timeMs - step.startMs).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    val eased = step.profile.curveY(raw).coerceIn(0f, 1f)
                    when (kind) {
                        AnimationKind.Morph -> {
                            val from = if (step.reverse) step.morphRange.end else step.morphRange.start
                            val to = if (step.reverse) step.morphRange.start else step.morphRange.end
                            m = lerp(from, to, eased)
                        }
                        AnimationKind.Rotate -> {
                            val from = if (step.reverse) step.rotateRange.end else step.rotateRange.start
                            val to = if (step.reverse) step.rotateRange.start else step.rotateRange.end
                            r = lerp(from, to, eased)
                        }
                        AnimationKind.Scale -> {
                            val from = if (step.reverse) step.scaleRange.end else step.scaleRange.start
                            val to = if (step.reverse) step.scaleRange.start else step.scaleRange.end
                            s = lerp(from, to, eased)
                        }
                        AnimationKind.Alpha -> {
                            val from = if (step.reverse) step.alphaRange.end else step.alphaRange.start
                            val to = if (step.reverse) step.alphaRange.start else step.alphaRange.end
                            a = lerp(from, to, eased)
                        }
                        AnimationKind.Colour -> {
                            val from = if (step.reverse) step.colorRange.end else step.colorRange.start
                            val to = if (step.reverse) step.colorRange.start else step.colorRange.end
                            c = lerp(from, to, eased)
                        }
                        AnimationKind.Position -> {
                            val fromX = if (step.reverse) step.offsetXRange.end else step.offsetXRange.start
                            val toX = if (step.reverse) step.offsetXRange.start else step.offsetXRange.end
                            val fromY = if (step.reverse) step.offsetYRange.end else step.offsetYRange.start
                            val toY = if (step.reverse) step.offsetYRange.start else step.offsetYRange.end
                            ox = lerp(fromX, toX, eased)
                            oy = lerp(fromY, toY, eased)
                        }
                    }
                }
            }
            morphAnim.snapTo(m)
            rotationAnim.snapTo(r)
            scaleAnim.snapTo(s)
            alphaAnim.snapTo(a)
            colorAnim.snapTo(c)
            offsetXAnim.snapTo(ox)
            offsetYAnim.snapTo(oy)
        }

        var startNanos = 0L
        withFrameNanos { startNanos = it }
        while (sequencePlaying) {
            val now = withFrameNanos { it }
            val elapsed = ((now - startNanos) / 1_000_000L).toInt()
            val reachedEnd = elapsed >= cycleMs
            val local = if (sequencerPlayMode == SequencerPlayMode.Loop) {
                elapsed % cycleMs
            } else {
                elapsed.coerceAtMost(cycleMs)
            }
            val timelineMs = when (sequencerDirection) {
                SequencerDirection.Forward, SequencerDirection.StartEnd -> local.coerceIn(0, totalMs)
                SequencerDirection.Reverse -> (totalMs - local).coerceIn(0, totalMs)
                SequencerDirection.PingPong -> if (local <= totalMs) local else (totalMs - (local - totalMs))
            }
            sequencerPlayheadMs = timelineMs
            applyTimelineAt(timelineMs)
            if (sequencerPlayMode == SequencerPlayMode.Once && reachedEnd) {
                sequencePlaying = false
            }
        }
    }

    val morphProgress = if (sequencePlaying) {
        morphAnim.value
    } else if (loopPlaying && AnimationKind.Morph in enabledAnimations) {
        morphAnim.value
    } else {
        timelineManualProgress
    }
    val canvasRotation = if (sequencePlaying) {
        rotationAnim.value
    } else if (loopPlaying && AnimationKind.Rotate in enabledAnimations) {
        rotationAnim.value
    } else {
        rotateRange.display(reverseAnimation)
    }
    val canvasScale = if (sequencePlaying) {
        scaleAnim.value
    } else if (loopPlaying && AnimationKind.Scale in enabledAnimations) {
        scaleAnim.value
    } else {
        scaleRange.display(reverseAnimation)
    }
    val canvasAlpha = if (sequencePlaying) {
        alphaAnim.value
    } else if (loopPlaying && AnimationKind.Alpha in enabledAnimations) {
        alphaAnim.value
    } else {
        alphaRange.display(reverseAnimation)
    }
    val canvasColorBlend = if (sequencePlaying) {
        colorAnim.value
    } else if (loopPlaying && AnimationKind.Colour in enabledAnimations) {
        colorAnim.value
    } else {
        colorRange.display(reverseAnimation)
    }
    val canvasOffsetX = if (sequencePlaying) {
        offsetXAnim.value
    } else if (loopPlaying && AnimationKind.Position in enabledAnimations) {
        offsetXAnim.value
    } else {
        offsetXRange.display(reverseAnimation)
    }
    val canvasOffsetY = if (sequencePlaying) {
        offsetYAnim.value
    } else if (loopPlaying && AnimationKind.Position in enabledAnimations) {
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
                            "Shape Editor · Native SVG Editor · Animation Studio",
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
                    text = { Text("SVG Editor") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
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
                1 -> NativeSvgEditorTab()
                2 -> AnimationStudioTab(
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
                    durationMs = durationMs,
                    onDurationMsChange = { durationMs = it },
                    profile = profile,
                    onProfileChange = { profile = it },
                    reverseAnimation = reverseAnimation,
                    onReverseAnimationChange = { reverseAnimation = it },
                    enabledAnimations = enabledAnimations,
                    onEnabledAnimationsChange = { enabledAnimations = it },
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
                    animTargetColor = animTargetColor,
                    onAnimTargetColorChange = { animTargetColor = it },
                    studioColors = studioColors,
                    animationSteps = animationSteps,
                    selectedStepId = selectedStepId,
                    sequencePlaying = sequencePlaying,
                    sequencerPlayheadMs = sequencerPlayheadMs,
                    sequencerAddKind = sequencerAddKind,
                    onSequencerAddKindChange = { sequencerAddKind = it },
                    sequencerAddStartMs = sequencerAddStartMs,
                    onSequencerAddStartMsChange = { sequencerAddStartMs = it.coerceAtLeast(0) },
                    sequencerDirection = sequencerDirection,
                    onSequencerDirectionChange = { sequencerDirection = it },
                    sequencerPlayMode = sequencerPlayMode,
                    onSequencerPlayModeChange = { sequencerPlayMode = it },
                    onAddCurrentStep = {
                        val start = sequencerAddStartMs.coerceAtLeast(0)
                        val step = createAnimationStepFromCurrentConfig(
                            stepNumber = nextStepIndex,
                            kind = sequencerAddKind,
                            startMs = start,
                            durationMs = durationMs,
                            profile = profile,
                            reverse = reverseAnimation,
                            morphRange = morphRange,
                            rotateRange = rotateRange,
                            scaleRange = scaleRange,
                            alphaRange = alphaRange,
                            colorRange = colorRange,
                            offsetXRange = offsetXRange,
                            offsetYRange = offsetYRange
                        )
                        val placed = resolveNoOverlapForKind(animationSteps, step)
                        animationSteps = (animationSteps + placed).normalizedTimeline()
                        sequencerAddStartMs = (placed.endMs + 50)
                        selectedStepId = step.id
                        nextStepIndex += 1
                    },
                    onApplyStep = { step ->
                        sequencePlaying = false
                        applyAnimationStep(step)
                    },
                    onDuplicateStep = { step ->
                        val copy = resolveNoOverlapForKind(
                            animationSteps,
                            step.duplicated(
                            newId = "step_$nextStepIndex",
                            newName = "Step $nextStepIndex"
                            ).withTimeline(step.endMs + 50, step.endMs + 50 + step.durationMs)
                        )
                        val index = animationSteps.indexOfFirst { it.id == step.id }
                        animationSteps = if (index >= 0) {
                            animationSteps.toMutableList().apply { add(index + 1, copy) }.normalizedTimeline()
                        } else {
                            (animationSteps + copy).normalizedTimeline()
                        }
                        selectedStepId = copy.id
                        nextStepIndex += 1
                    },
                    onDeleteStep = { step ->
                        animationSteps = animationSteps.filterNot { it.id == step.id }
                        if (selectedStepId == step.id) selectedStepId = null
                    },
                    onUpdateStepTimeline = { updated ->
                        val merged = animationSteps.map { if (it.id == updated.id) updated else it }
                        val resolved = merged.firstOrNull { it.id == updated.id }?.let { candidate ->
                            resolveNoOverlapForKind(merged, candidate)
                        } ?: updated
                        animationSteps = merged.map { if (it.id == updated.id) resolved else it }.normalizedTimeline()
                    },
                    onPlaySequence = {
                        if (animationSteps.isEmpty()) return@AnimationStudioTab
                        loopPlaying = false
                        sequencePlaying = true
                    },
                    onStopSequence = { sequencePlaying = false },
                    onClearSequence = {
                        sequencePlaying = false
                        sequencerPlayheadMs = 0
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
                                applyValidatedBundle(bundle, validation)
                                Toast.makeText(context, "Bundle geladen", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                assetStatusText = "Load failed: ${e.message}"
                                Toast.makeText(context, assetStatusText, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onExportFile = {
                        try {
                            val bundle = buildCurrentBundle()
                            val validation = M3ShapeAssetValidator.validateBundle(bundle)
                            if (!validation.valid) {
                                assetStatusText = "Export blocked.\n" + formatDiagnostics(validation)
                                Toast.makeText(context, "Bundle invalid – not exported", Toast.LENGTH_SHORT).show()
                            } else {
                                pendingExportJson = M3ShapeAssetCodec.encodeBundle(bundle)
                                exportFileLauncher.launch(defaultM3ShapeExportFilename(assetBundleId))
                            }
                        } catch (e: Exception) {
                            assetStatusText = "Export failed: ${e.message ?: "unknown error"}"
                            Toast.makeText(context, assetStatusText, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onImportFile = {
                        importFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    }
                )
            }
        }
    }
}
