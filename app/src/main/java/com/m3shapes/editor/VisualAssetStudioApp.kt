package com.m3shapes.editor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.compose.canvas.VectorShapeEditorApp
import com.compose.canvas.asset.EMA_FILE_EXTENSION
import com.compose.canvas.asset.EMA_MIME_TYPE
import com.compose.canvas.asset.EmaCodec
import com.compose.canvas.asset.AssetLibraryQuery
import com.compose.canvas.asset.DirectoryAssetLibrary
import com.compose.canvas.asset.CubicBezier
import com.compose.canvas.asset.MotionChannel
import com.compose.canvas.asset.MotionInterpolation
import com.compose.canvas.asset.MotionRange
import com.compose.canvas.asset.PathMorpher
import com.compose.canvas.asset.ShapeMakerMotionPreset
import com.compose.canvas.asset.ShapeMakerRepeatMode
import com.compose.canvas.asset.ShapeMakerSequence
import com.compose.canvas.asset.ShapeMakerTransition
import com.compose.canvas.asset.VisualAsset
import com.compose.canvas.asset.VisualAssetIdentity
import com.compose.canvas.asset.VisualAssetType
import com.compose.canvas.asset.newVisualAsset
import com.compose.canvas.editor.EditorState
import com.compose.canvas.geometry.PathGeometry
import com.compose.canvas.ui.theme.EditorTheme
import kotlinx.coroutines.launch
import java.io.File

private const val LAST_EMA_FILE = "last_asset.ema"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualAssetStudioApp(
    requestedAssetType: VisualAssetType? = null,
    onReturnAsset: ((VisualAsset) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialDocument = remember { materialPresetDocument(ExpressiveShapeId.Square) }
    val editorState = remember { EditorState(initialDocument) }
    var asset by remember {
        mutableStateOf(
            newVisualAsset(
                assetId = "shape-1",
                name = "Material Shape",
                document = initialDocument
            ).let { initialAsset ->
                requestedAssetType?.let { type ->
                    initialAsset.copy(identity = initialAsset.identity.copy(type = type))
                } ?: initialAsset
            }
        )
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedPreset by remember { mutableStateOf(ExpressiveShapeId.Square) }
    var presetMenuOpen by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("DESIGN bereit · Start und Ende sind identisch") }
    var pendingExport by remember { mutableStateOf("") }
    val assetLibrary = remember(context.filesDir) { DirectoryAssetLibrary(File(context.filesDir, "asset-library")) }
    var libraryQuery by rememberSaveable { mutableStateOf("") }
    var libraryRevision by remember { mutableIntStateOf(0) }

    var timelineManualProgress by remember { mutableFloatStateOf(0f) }
    var morphRange by remember { mutableStateOf(FloatRangeValue(0f, 1f)) }
    var rotateRange by remember { mutableStateOf(FloatRangeValue(0f, 360f)) }
    var scaleRange by remember { mutableStateOf(FloatRangeValue(0.88f, 1.18f)) }
    var alphaRange by remember { mutableStateOf(FloatRangeValue(0.45f, 1f)) }
    var colorRange by remember { mutableStateOf(FloatRangeValue(0f, 1f)) }
    var offsetXRange by remember { mutableStateOf(FloatRangeValue(-28f, 28f)) }
    var offsetYRange by remember { mutableStateOf(FloatRangeValue(-22f, 22f)) }
    var staggerRange by remember { mutableStateOf(FloatRangeValue(0f, 0.65f)) }
    var strokeRange by remember { mutableStateOf(FloatRangeValue(0f, 1f)) }
    var blurRange by remember { mutableStateOf(FloatRangeValue(0f, 16f)) }
    var gradientRange by remember { mutableStateOf(FloatRangeValue(0f, 1f)) }
    var sourceColor by remember { mutableStateOf(studioColors[0]) }
    var targetColor by remember { mutableStateOf(studioColors[5]) }
    var durationMs by remember { mutableIntStateOf(1_200) }
    var profile by remember { mutableStateOf(StudioEasing.EaseInOut.profile()) }
    var motionPathType by remember { mutableStateOf(MotionPathType.Tween) }
    var springStiffness by remember { mutableFloatStateOf(400f) }
    var springDamping by remember { mutableFloatStateOf(0.75f) }
    var bounceGravity by remember { mutableFloatStateOf(1f) }
    var playbackMode by remember { mutableStateOf(PlaybackMode.Normal) }
    var animationSteps by remember { mutableStateOf(emptyList<AnimationStep>()) }
    var sequencePlaying by remember { mutableStateOf(false) }
    var selectedStepId by remember { mutableStateOf<String?>(null) }
    var nextStepIndex by remember { mutableIntStateOf(1) }
    var sequencerAddKind by remember { mutableStateOf(AnimationKind.Morph) }
    var sequencerPlayheadMs by remember { mutableIntStateOf(0) }

    val morphAnimation = remember { Animatable(0f) }
    val rotationAnimation = remember { Animatable(0f) }
    val scaleAnimation = remember { Animatable(1f) }
    val alphaAnimation = remember { Animatable(1f) }
    val colorAnimation = remember { Animatable(0f) }
    val offsetXAnimation = remember { Animatable(0f) }
    val offsetYAnimation = remember { Animatable(0f) }
    val staggerAnimation = remember { Animatable(0f) }
    val strokeAnimation = remember { Animatable(0f) }
    val blurAnimation = remember { Animatable(0f) }
    val gradientAnimation = remember { Animatable(0f) }

    fun currentAsset(): VisualAsset {
        val generatedAnimation = animationSteps.toMotionSequence()
        val previousAnimation = asset.animations.firstOrNull { it.id == generatedAnimation.id }
        val animation = generatedAnimation.copy(
            tracks = generatedAnimation.tracks.ifEmpty { previousAnimation?.tracks.orEmpty() },
            events = previousAnimation?.events.orEmpty()
        )
        val transition = ShapeMakerTransition(
            id = "start-end",
            fromState = "start",
            toState = "end",
            durationMs = durationMs,
            easing = CubicBezier(profile.x1, profile.y1, profile.x2, profile.y2),
            motionPreset = when (motionPathType) {
                MotionPathType.Tween -> ShapeMakerMotionPreset.Custom
                MotionPathType.Spring -> ShapeMakerMotionPreset.Spring
                MotionPathType.Bounce -> ShapeMakerMotionPreset.Bounce
            },
            interpolation = when (motionPathType) {
                MotionPathType.Tween -> MotionInterpolation.TWEEN
                MotionPathType.Spring -> MotionInterpolation.SPRING
                MotionPathType.Bounce -> MotionInterpolation.BOUNCE
            },
            repeatMode = when (playbackMode) {
                PlaybackMode.Normal -> ShapeMakerRepeatMode.None
                PlaybackMode.Reverse -> ShapeMakerRepeatMode.Reverse
                PlaybackMode.PingPong -> ShapeMakerRepeatMode.PingPong
            },
            bidirectional = playbackMode == PlaybackMode.PingPong,
            propertyOverrides = mapOf(
                MotionChannel.MORPH to MotionRange(morphRange.start, morphRange.end),
                MotionChannel.ROTATE to MotionRange(rotateRange.start, rotateRange.end),
                MotionChannel.SCALE to MotionRange(scaleRange.start, scaleRange.end),
                MotionChannel.ALPHA to MotionRange(alphaRange.start, alphaRange.end),
                MotionChannel.COLOR to MotionRange(colorRange.start, colorRange.end),
                MotionChannel.POSITION_X to MotionRange(offsetXRange.start, offsetXRange.end),
                MotionChannel.POSITION_Y to MotionRange(offsetYRange.start, offsetYRange.end),
                MotionChannel.STAGGER to MotionRange(staggerRange.start, staggerRange.end),
                MotionChannel.STROKE to MotionRange(strokeRange.start, strokeRange.end),
                MotionChannel.BLUR to MotionRange(blurRange.start, blurRange.end),
                MotionChannel.GRADIENT to MotionRange(gradientRange.start, gradientRange.end)
            )
        )
        return asset.copy(
            identity = asset.identity.copy(name = editorState.document.name),
            design = editorState.document,
            transitions = listOf(transition),
            animations = listOf(animation),
            sequences = listOf(
                ShapeMakerSequence(
                    id = "main",
                    name = "Main",
                    transitionIds = listOf(transition.id),
                    animationIds = listOf(animation.id)
                )
            )
        )
    }

    fun applyLoadedAsset(loaded: VisualAsset) {
        asset = loaded
        editorState.replaceDocument(loaded.design)
        animationSteps = loaded.animations.firstOrNull()?.steps?.map { it.toAnimationStep() }.orEmpty()
        loaded.transitions.firstOrNull()?.let { transition ->
            durationMs = transition.durationMs
            profile = BezierProfile(
                transition.easing.x1,
                transition.easing.y1,
                transition.easing.x2,
                transition.easing.y2
            )
            motionPathType = when (transition.interpolation) {
                MotionInterpolation.TWEEN -> MotionPathType.Tween
                MotionInterpolation.SPRING -> MotionPathType.Spring
                MotionInterpolation.BOUNCE -> MotionPathType.Bounce
            }
            playbackMode = when (transition.repeatMode) {
                ShapeMakerRepeatMode.Reverse -> PlaybackMode.Reverse
                ShapeMakerRepeatMode.PingPong -> PlaybackMode.PingPong
                ShapeMakerRepeatMode.None, ShapeMakerRepeatMode.Restart -> PlaybackMode.Normal
            }
            transition.propertyOverrides[MotionChannel.MORPH]?.let { morphRange = FloatRangeValue(it.start, it.end) }
            transition.propertyOverrides[MotionChannel.ROTATE]?.let { rotateRange = FloatRangeValue(it.start, it.end) }
            transition.propertyOverrides[MotionChannel.SCALE]?.let { scaleRange = FloatRangeValue(it.start, it.end) }
            transition.propertyOverrides[MotionChannel.ALPHA]?.let { alphaRange = FloatRangeValue(it.start, it.end) }
            transition.propertyOverrides[MotionChannel.COLOR]?.let { colorRange = FloatRangeValue(it.start, it.end) }
            transition.propertyOverrides[MotionChannel.POSITION_X]?.let { offsetXRange = FloatRangeValue(it.start, it.end) }
            transition.propertyOverrides[MotionChannel.POSITION_Y]?.let { offsetYRange = FloatRangeValue(it.start, it.end) }
            transition.propertyOverrides[MotionChannel.STAGGER]?.let { staggerRange = FloatRangeValue(it.start, it.end) }
            transition.propertyOverrides[MotionChannel.STROKE]?.let { strokeRange = FloatRangeValue(it.start, it.end) }
            transition.propertyOverrides[MotionChannel.BLUR]?.let { blurRange = FloatRangeValue(it.start, it.end) }
            transition.propertyOverrides[MotionChannel.GRADIENT]?.let { gradientRange = FloatRangeValue(it.start, it.end) }
        }
        nextStepIndex = animationSteps.size + 1
        selectedStepId = null
        sequencePlaying = false
        val validation = EmaCodec.validate(loaded)
        statusText = if (validation.diagnostics.isEmpty()) {
            ".ema geladen · ${loaded.identity.name}"
        } else {
            validation.diagnostics.joinToString("\n") { it.message }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(EMA_MIME_TYPE)
    ) { uri ->
        if (uri != null && pendingExport.isNotBlank()) {
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(pendingExport) }
                        ?: error("Ausgabestream nicht verfügbar")
                }.onSuccess {
                    statusText = ".ema exportiert"
                    Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
                }.onFailure {
                    statusText = "Export fehlgeschlagen: ${it.message}"
                }
                pendingExport = ""
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Eingabestream nicht verfügbar")
                    EmaCodec.decode(text)
                }.onSuccess(::applyLoadedAsset).onFailure {
                    statusText = "Import fehlgeschlagen: ${it.message}"
                    Toast.makeText(context, statusText, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val morphPreparation = remember(asset.states) {
        PathMorpher.prepare(
            asset.state("start")?.document,
            asset.state("mid")?.document,
            asset.state("end")?.document
        )
    }

    fun applyAnimationStep(step: AnimationStep) {
        durationMs = step.durationMs
        profile = step.profile
        motionPathType = step.motionPath
        springStiffness = step.springStiffness
        springDamping = step.springDamping
        bounceGravity = step.bounceGravity
        playbackMode = if (step.reverse) PlaybackMode.Reverse else PlaybackMode.Normal
        morphRange = step.morphRange
        rotateRange = step.rotateRange
        scaleRange = step.scaleRange
        alphaRange = step.alphaRange
        colorRange = step.colorRange
        offsetXRange = step.offsetXRange
        offsetYRange = step.offsetYRange
        staggerRange = step.staggerRange
        strokeRange = step.strokeRange
        blurRange = step.blurRange
        gradientRange = step.gradientRange
        timelineManualProgress = step.morphRange.display(step.reverse)
        selectedStepId = step.id
    }

    LaunchedEffect(sequencePlaying, animationSteps) {
        if (!sequencePlaying) return@LaunchedEffect
        val steps = animationSteps.normalizedTimeline()
        if (steps.isEmpty()) {
            sequencePlaying = false
            return@LaunchedEffect
        }
        val totalMs = (steps.maxOfOrNull { it.endMs } ?: 0).coerceAtLeast(1)
        val startNanos = withFrameNanos { it }

        fun stepProgress(step: AnimationStep, raw: Float): Float = motionProgress(
            raw = raw,
            type = step.motionPath,
            profile = step.profile,
            springStiffness = step.springStiffness,
            springDamping = step.springDamping,
            bounceGravity = step.bounceGravity
        )

        fun valueAt(
            kind: AnimationKind,
            timeMs: Int,
            fallback: FloatRangeValue,
            rangeOf: (AnimationStep) -> FloatRangeValue = { it.rangeFor(kind) }
        ): Float {
            val step = steps.filter { it.kindOrFallback() == kind && it.startMs <= timeMs }
                .maxByOrNull { it.startMs } ?: return fallback.display(playbackMode.reverse)
            val duration = (step.endMs - step.startMs).coerceAtLeast(1)
            val raw = ((timeMs - step.startMs).toFloat() / duration).coerceIn(0f, 1f)
            val progress = stepProgress(step, raw)
            val range = rangeOf(step)
            val from = if (step.reverse) range.end else range.start
            val to = if (step.reverse) range.start else range.end
            return from + (to - from) * progress
        }

        while (sequencePlaying) {
            val now = withFrameNanos { it }
            val elapsed = ((now - startNanos) / 1_000_000L).toInt().coerceAtMost(totalMs)
            sequencerPlayheadMs = elapsed
            morphAnimation.snapTo(valueAt(AnimationKind.Morph, elapsed, morphRange))
            rotationAnimation.snapTo(valueAt(AnimationKind.Rotate, elapsed, rotateRange))
            scaleAnimation.snapTo(valueAt(AnimationKind.Scale, elapsed, scaleRange))
            alphaAnimation.snapTo(valueAt(AnimationKind.Alpha, elapsed, alphaRange))
            colorAnimation.snapTo(valueAt(AnimationKind.Colour, elapsed, colorRange))
            offsetXAnimation.snapTo(valueAt(AnimationKind.Position, elapsed, offsetXRange))
            offsetYAnimation.snapTo(valueAt(AnimationKind.Position, elapsed, offsetYRange) { it.offsetYRange })
            staggerAnimation.snapTo(valueAt(AnimationKind.Stagger, elapsed, staggerRange))
            strokeAnimation.snapTo(valueAt(AnimationKind.Stroke, elapsed, strokeRange))
            blurAnimation.snapTo(valueAt(AnimationKind.Blur, elapsed, blurRange))
            gradientAnimation.snapTo(valueAt(AnimationKind.Gradient, elapsed, gradientRange))
            if (elapsed >= totalMs) sequencePlaying = false
        }
    }

    val morphProgress = if (sequencePlaying) morphAnimation.value else timelineManualProgress
    val canvasStagger = if (sequencePlaying) staggerAnimation.value else staggerRange.display(playbackMode.reverse)
    val previewPath = morphPreparation.morph?.pathAt(morphProgress, canvasStagger)
        ?: PathMorpher.previewPath(asset.state("start")?.document)
        ?: PathMorpher.previewPath(editorState.document)
        ?: PathGeometry(emptyList())
    val canvasRotation = if (sequencePlaying) rotationAnimation.value else rotateRange.display(playbackMode.reverse)
    val canvasScale = if (sequencePlaying) scaleAnimation.value else scaleRange.display(playbackMode.reverse)
    val canvasAlpha = if (sequencePlaying) alphaAnimation.value else alphaRange.display(playbackMode.reverse)
    val canvasColor = lerp(
        sourceColor,
        targetColor,
        (if (sequencePlaying) colorAnimation.value else colorRange.display(playbackMode.reverse)).coerceIn(0f, 1f)
    )
    val canvasOffsetX = if (sequencePlaying) offsetXAnimation.value else offsetXRange.display(playbackMode.reverse)
    val canvasOffsetY = if (sequencePlaying) offsetYAnimation.value else offsetYRange.display(playbackMode.reverse)
    val canvasStroke = if (sequencePlaying) strokeAnimation.value else strokeRange.display(playbackMode.reverse)
    val canvasBlur = if (sequencePlaying) blurAnimation.value else blurRange.display(playbackMode.reverse)
    val canvasGradient = if (sequencePlaying) gradientAnimation.value else gradientRange.display(playbackMode.reverse)

    fun createStep(kind: AnimationKind, number: Int, startMs: Int): AnimationStep =
        createAnimationStepFromCurrentConfig(
            stepNumber = number,
            kind = kind,
            startMs = startMs,
            durationMs = durationMs,
            profile = profile,
            reverse = playbackMode.reverse,
            morphRange = morphRange,
            rotateRange = rotateRange,
            scaleRange = scaleRange,
            alphaRange = alphaRange,
            colorRange = colorRange,
            offsetXRange = offsetXRange,
            offsetYRange = offsetYRange,
            staggerRange = staggerRange,
            strokeRange = strokeRange,
            blurRange = blurRange,
            gradientRange = gradientRange,
            motionPath = motionPathType,
            springStiffness = springStiffness,
            springDamping = springDamping,
            bounceGravity = bounceGravity
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("M3ShapeMaker", fontWeight = FontWeight.Bold)
                        Text("VisualAsset · .ema", style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                listOf("DESIGN", "ANIMATE", "SEQUENCER", "LIBRARY").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
                    )
                }
            }
            when (selectedTab) {
                0 -> Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box {
                            OutlinedButton(onClick = { presetMenuOpen = true }) {
                                Text("M3: ${selectedPreset.label}")
                            }
                            DropdownMenu(
                                expanded = presetMenuOpen,
                                onDismissRequest = { presetMenuOpen = false }
                            ) {
                                ExpressiveShapeId.all.forEach { preset ->
                                    DropdownMenuItem(
                                        text = { Text(preset.label) },
                                        onClick = {
                                            selectedPreset = preset
                                            presetMenuOpen = false
                                            editorState.replaceDocument(materialPresetDocument(preset))
                                            asset = currentAsset().copy(design = editorState.document)
                                            statusText = "${preset.label} als parametrisches M3-Preset geladen"
                                        }
                                    )
                                }
                            }
                        }
                        Button(onClick = {
                            asset = currentAsset().upsertState("start", "Start", editorState.document)
                            statusText = "Startzustand gespeichert"
                        }) { Text("Start setzen") }
                        Button(onClick = {
                            asset = currentAsset().upsertState("mid", "Mitte", editorState.document)
                            val check = PathMorpher.prepare(
                                asset.state("start")?.document,
                                asset.state("mid")?.document,
                                asset.state("end")?.document
                            )
                            statusText = check.diagnostic ?: "Mittelzustand gespeichert · Morph kompatibel"
                        }) { Text("Mitte setzen") }
                        Button(onClick = {
                            asset = currentAsset().upsertState("end", "Ende", editorState.document)
                            val check = PathMorpher.prepare(
                                asset.state("start")?.document,
                                asset.state("mid")?.document,
                                asset.state("end")?.document
                            )
                            statusText = check.diagnostic ?: "Endzustand gespeichert · Morph kompatibel"
                        }) { Text("Ende setzen") }
                        OutlinedButton(onClick = {
                            val snapshot = currentAsset()
                            val validation = EmaCodec.validate(snapshot)
                            statusText = if (validation.valid) ".ema gültig" else validation.diagnostics.joinToString("\n") { it.message }
                        }) { Text("Prüfen") }
                        OutlinedButton(onClick = {
                            runCatching {
                                File(context.filesDir, LAST_EMA_FILE).writeText(EmaCodec.encode(currentAsset()))
                            }.onSuccess { statusText = "Lokal als $LAST_EMA_FILE gespeichert" }
                                .onFailure { statusText = "Speichern fehlgeschlagen: ${it.message}" }
                        }) { Text("Lokal sichern") }
                        OutlinedButton(onClick = {
                            runCatching {
                                EmaCodec.decode(File(context.filesDir, LAST_EMA_FILE).readText())
                            }.onSuccess(::applyLoadedAsset)
                                .onFailure { statusText = "Laden fehlgeschlagen: ${it.message}" }
                        }) { Text("Lokal laden") }
                        OutlinedButton(onClick = {
                            pendingExport = EmaCodec.encode(currentAsset())
                            exportLauncher.launch("${asset.identity.assetId}.$EMA_FILE_EXTENSION")
                        }) { Text(".ema Export") }
                        onReturnAsset?.let { returnAsset ->
                            Button(onClick = { returnAsset(currentAsset()) }) {
                                Text("An VisualTasker übergeben")
                            }
                        }
                        OutlinedButton(onClick = {
                            importLauncher.launch(arrayOf(EMA_MIME_TYPE, "application/json", "*/*"))
                        }) { Text(".ema Import") }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Start", style = MaterialTheme.typography.labelSmall)
                            StateThumbnail(
                                label = "S",
                                path = PathMorpher.previewPath(asset.state("start")?.document),
                                fillColor = PathMorpher.previewFill(asset.state("start")?.document)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Mitte", style = MaterialTheme.typography.labelSmall)
                            StateThumbnail(
                                label = "M",
                                path = PathMorpher.previewPath(asset.state("mid")?.document),
                                fillColor = PathMorpher.previewFill(asset.state("mid")?.document)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ende", style = MaterialTheme.typography.labelSmall)
                            StateThumbnail(
                                label = "E",
                                path = PathMorpher.previewPath(asset.state("end")?.document),
                                fillColor = PathMorpher.previewFill(asset.state("end")?.document)
                            )
                        }
                    }
                    Text(
                        statusText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (statusText.contains("fehl", ignoreCase = true) || statusText.contains("unterschied", ignoreCase = true)) {
                            MaterialTheme.colorScheme.error
                        } else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    EditorTheme {
                        VectorShapeEditorApp(
                            editorState = editorState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                1 -> Column(Modifier.fillMaxSize()) {
                    morphPreparation.diagnostic?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    AnimationStudioTab(
                        morph = morphPreparation.morph,
                        durationMs = durationMs,
                        onDurationMsChange = { durationMs = it },
                        profile = profile,
                        onProfileChange = { profile = it },
                        motionPathType = motionPathType,
                        onMotionPathTypeChange = { motionPathType = it },
                        springStiffness = springStiffness,
                        onSpringStiffnessChange = { springStiffness = it },
                        springDamping = springDamping,
                        onSpringDampingChange = { springDamping = it },
                        bounceGravity = bounceGravity,
                        onBounceGravityChange = { bounceGravity = it },
                        morphRange = morphRange,
                        onMorphRangeChange = {
                            morphRange = it
                            timelineManualProgress = it.display(playbackMode.reverse)
                        },
                        rotateRange = rotateRange,
                        onRotateRangeChange = { rotateRange = it },
                        scaleRange = scaleRange,
                        onScaleRangeChange = { scaleRange = it },
                        alphaRange = alphaRange,
                        onAlphaRangeChange = { alphaRange = it },
                        colorRange = colorRange,
                        onColorRangeChange = { colorRange = it },
                        offsetXRange = offsetXRange,
                        onOffsetXRangeChange = { offsetXRange = it },
                        offsetYRange = offsetYRange,
                        onOffsetYRangeChange = { offsetYRange = it },
                        staggerRange = staggerRange,
                        onStaggerRangeChange = { staggerRange = it },
                        strokeRange = strokeRange,
                        onStrokeRangeChange = { strokeRange = it },
                        blurRange = blurRange,
                        onBlurRangeChange = { blurRange = it },
                        gradientRange = gradientRange,
                        onGradientRangeChange = { gradientRange = it },
                        sourceColor = sourceColor,
                        animTargetColor = targetColor,
                        onAnimTargetColorChange = { targetColor = it },
                        playbackMode = playbackMode,
                        onPlaybackModeChange = { playbackMode = it },
                        studioColors = studioColors
                    )
                }
                2 -> Column(Modifier.fillMaxSize()) {
                    morphPreparation.diagnostic?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    SequencerStudioTab(
                        morphPath = previewPath,
                        canvasRotation = canvasRotation,
                        canvasScale = canvasScale,
                        canvasAlpha = canvasAlpha,
                        canvasOffsetX = canvasOffsetX,
                        canvasOffsetY = canvasOffsetY,
                        canvasFillColor = canvasColor,
                        canvasStroke = canvasStroke,
                        canvasBlur = canvasBlur,
                        canvasGradient = canvasGradient,
                        canvasGradientEnd = targetColor,
                        animationSteps = animationSteps,
                        selectedStepId = selectedStepId,
                        sequencePlaying = sequencePlaying,
                        sequencerPlayheadMs = sequencerPlayheadMs,
                        sequencerAddKind = sequencerAddKind,
                        onSequencerAddKindChange = { sequencerAddKind = it },
                        onAddCurrentStep = {
                            val start = animationSteps.filter { it.kindOrFallback() == sequencerAddKind }
                                .maxOfOrNull { it.endMs } ?: 0
                            val step = resolveNoOverlapForKind(
                                animationSteps,
                                createStep(sequencerAddKind, nextStepIndex, start)
                            )
                            animationSteps = (animationSteps + step).normalizedTimeline()
                            selectedStepId = step.id
                            nextStepIndex += 1
                        },
                        onAddCurrentAllKinds = {
                            var working = animationSteps
                            var number = nextStepIndex
                            AnimationKind.entries.forEach { kind ->
                                val start = working.filter { it.kindOrFallback() == kind }.maxOfOrNull { it.endMs } ?: 0
                                val step = resolveNoOverlapForKind(working, createStep(kind, number, start))
                                working = (working + step).normalizedTimeline()
                                number += 1
                            }
                            animationSteps = working
                            selectedStepId = working.lastOrNull()?.id
                            nextStepIndex = number
                        },
                        onApplyStep = {
                            sequencePlaying = false
                            applyAnimationStep(it)
                        },
                        onDuplicateStep = { step ->
                            val copy = resolveNoOverlapForKind(
                                animationSteps,
                                step.duplicated("step_$nextStepIndex", "Step $nextStepIndex")
                                    .withTimeline(step.endMs + 50, step.endMs + 50 + step.durationMs)
                            )
                            animationSteps = (animationSteps + copy).normalizedTimeline()
                            selectedStepId = copy.id
                            nextStepIndex += 1
                        },
                        onDeleteStep = { step ->
                            animationSteps = animationSteps.filterNot { it.id == step.id }
                            if (selectedStepId == step.id) selectedStepId = null
                        },
                        onSelectStep = { selectedStepId = it.id },
                        onUpdateStepTimeline = { updated ->
                            animationSteps = animationSteps.map { if (it.id == updated.id) updated else it }.normalizedTimeline()
                        },
                        onPlaySequence = {
                            if (animationSteps.isNotEmpty()) sequencePlaying = true
                        },
                        onStopSequence = { sequencePlaying = false },
                        onClearSequence = {
                            sequencePlaying = false
                            sequencerPlayheadMs = 0
                            animationSteps = emptyList()
                            selectedStepId = null
                        }
                    )
                }
                3 -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Asset Library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = libraryQuery,
                        onValueChange = { libraryQuery = it },
                        label = { Text("Name, ID oder Tag suchen") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        val snapshot = currentAsset()
                        val nextVersion = (assetLibrary.versions(snapshot.identity.assetId).maxOrNull() ?: 0) + 1
                        val versioned = snapshot.copy(identity = snapshot.identity.copy(version = nextVersion))
                        assetLibrary.save(versioned)
                        asset = versioned
                        libraryRevision += 1
                        statusText = "${versioned.identity.name} v$nextVersion in Library gespeichert"
                    }) { Text("Aktuelles Asset versionieren") }
                    val results = remember(libraryQuery, libraryRevision) {
                        assetLibrary.search(AssetLibraryQuery(text = libraryQuery))
                    }
                    Text("${results.size} Treffer", style = MaterialTheme.typography.labelMedium)
                    results.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.identity.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${item.identity.assetId} · v${item.identity.version} · ${item.identity.tags.joinToString()}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            OutlinedButton(onClick = { applyLoadedAsset(item); selectedTab = 0 }) {
                                Text("Öffnen")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun AnimationStep.rangeFor(kind: AnimationKind): FloatRangeValue = when (kind) {
    AnimationKind.Morph -> morphRange
    AnimationKind.Rotate -> rotateRange
    AnimationKind.Scale -> scaleRange
    AnimationKind.Alpha -> alphaRange
    AnimationKind.Colour -> colorRange
    AnimationKind.Position -> offsetXRange
    AnimationKind.Stagger -> staggerRange
    AnimationKind.Stroke -> strokeRange
    AnimationKind.Blur -> blurRange
    AnimationKind.Gradient -> gradientRange
}
