package dev.shashank.peekaboo.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.face.Face
import dev.shashank.peekaboo.data.OwnerProfile
import dev.shashank.peekaboo.detect.FaceSignature
import dev.shashank.peekaboo.detect.PeekAnalyzer
import dev.shashank.peekaboo.detect.cameraProvider
import dev.shashank.peekaboo.detect.toUprightBitmap
import dev.shashank.peekaboo.service.GuardState
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.Permissions
import dev.shashank.peekaboo.ui.components.Panel
import dev.shashank.peekaboo.ui.components.PrimaryButton
import dev.shashank.peekaboo.ui.components.SecondaryButton
import dev.shashank.peekaboo.ui.components.ListRow
import dev.shashank.peekaboo.ui.components.ScreenHeader
import dev.shashank.peekaboo.ui.components.Section
import dev.shashank.peekaboo.ui.components.TickRing
import dev.shashank.peekaboo.ui.theme.Ink
import dev.shashank.peekaboo.ui.theme.InterDisplay
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

private enum class FaceMode { Overview, Enroll, Test }

private const val TARGET_SAMPLES = 20

@Composable
fun FaceScreen(vm: MainViewModel, contentPadding: PaddingValues) {
    val owner by vm.owner.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(FaceMode.Overview) }
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) mode = FaceMode.Enroll
    }
    val startEnroll = {
        if (Permissions.camera(ctx)) mode = FaceMode.Enroll else launcher.launch(Manifest.permission.CAMERA)
    }

    AnimatedContent(
        mode,
        transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.95f)) togetherWith fadeOut(tween(150)) },
        label = "face",
    ) { m ->
        when (m) {
            FaceMode.Overview -> FaceOverview(
                owner = owner,
                contentPadding = contentPadding,
                onEnroll = startEnroll,
                onTest = { mode = FaceMode.Test },
                onRemove = { vm.clearOwner() },
            )
            FaceMode.Enroll -> EnrollView(
                contentPadding = contentPadding,
                onDone = {
                    vm.saveOwner(it)
                    mode = FaceMode.Overview
                },
                onCancel = { mode = FaceMode.Overview },
            )
            FaceMode.Test -> TestView(owner, contentPadding) { mode = FaceMode.Overview }
        }
    }
}

/** Face glyph on a disc inside a tick ring: lime ticks and a check once enrolled, a scan line before. */
@Composable
internal fun FaceBadge(enrolled: Boolean, size: Dp) {
    val t = rememberInfiniteTransition(label = "scan")
    val scan by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "line")
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        TickRing(if (enrolled) 1f else 0f, Modifier.size(size))
        Box(
            Modifier
                .size(size * 0.66f)
                .clip(CircleShape)
                .background(Ink.Raised)
                .border(1.dp, Ink.LineStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Face, null, tint = if (enrolled) Ink.Text else Ink.TextMuted, modifier = Modifier.size(size * 0.32f))
            if (!enrolled) {
                Canvas(Modifier.fillMaxSize()) {
                    val y = this.size.height * scan
                    drawLine(Ink.Accent.copy(alpha = 0.8f), Offset(this.size.width * 0.18f, y), Offset(this.size.width * 0.82f, y), 1.5.dp.toPx())
                }
            }
        }
        if (enrolled) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = size * 0.12f, bottom = size * 0.12f)
                    .size(size * 0.2f)
                    .clip(CircleShape)
                    .background(Ink.Accent)
                    .border(4.dp, Ink.Bg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, null, tint = Ink.OnAccent, modifier = Modifier.size(size * 0.1f))
            }
        }
    }
}

@Composable
internal fun FaceOverview(
    owner: OwnerProfile?,
    contentPadding: PaddingValues,
    onEnroll: () -> Unit,
    onTest: () -> Unit,
    onRemove: () -> Unit,
) {
    var confirmRemove by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(bottom = contentPadding.calculateBottomPadding()),
    ) {
        ScreenHeader("You", if (owner != null) "Face ID on" else "Face ID off")
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { FaceBadge(owner != null, 200.dp) }
        Spacer(Modifier.height(24.dp))
        Text(
            if (owner != null) "You're recognised" else "Teach it your face",
            style = MaterialTheme.typography.headlineLarge, color = Ink.Text,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (owner != null) "You're never counted as a peeker, even when a friend is holding your phone and you lean in."
            else "Look at the camera and slowly circle your head. About ten seconds, and you'll never be flagged as a peeker.",
            style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(24.dp))
        if (owner == null) {
            PrimaryButton("Scan my face", icon = Icons.Rounded.Fingerprint, onClick = onEnroll)
        } else {
            val fmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FactTile("Samples", "${owner.samples.size}", Modifier.weight(1f))
                FactTile("Enrolled", fmt.format(Date(owner.enrolledAt)), Modifier.weight(1f))
            }
            Section {
                ListRow("Test recognition", subtitle = "Check it knows you from a friend", icon = Icons.Rounded.Science, onClick = onTest)
                ListRow("Scan again", subtitle = "New glasses, haircut or lighting", icon = Icons.Rounded.Refresh, onClick = onEnroll)
                ListRow("Remove my face", icon = Icons.Rounded.DeleteOutline, iconTint = Ink.Alert, titleColor = Ink.Alert, showDivider = false, chevron = false, onClick = { confirmRemove = true })
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = Ink.TextFaint, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text("Stored on this phone as a pattern, not a photo.", style = MaterialTheme.typography.bodySmall, color = Ink.TextFaint, maxLines = 1)
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            containerColor = Ink.Raised,
            title = { Text("Remove your face?", color = Ink.Text) },
            text = { Text("You'll be counted as a peeker whenever someone else is looking too, until you scan again.", color = Ink.TextMuted) },
            confirmButton = { TextButton(onClick = { onRemove(); confirmRemove = false }) { Text("Remove", color = Ink.Alert) } },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Keep", color = Ink.Text) } },
        )
    }
}

@Composable
internal fun FactTile(label: String, value: String, modifier: Modifier) {
    Panel(modifier, padding = PaddingValues(16.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"), color = Ink.Text, maxLines = 1)
    }
}

/** Front camera preview in a circle plus a stream of upright frames and detected faces. */
@Composable
private fun FaceCamera(size: Dp, onFrame: (Bitmap, List<Face>) -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val analyzer = remember { PeekAnalyzer() }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val callback by rememberUpdatedState(onFrame)

    DisposableEffect(Unit) {
        GuardState.acquireCamera()
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(ctx).get().unbindAll() }
            GuardState.releaseCamera()
            executor.shutdown()
            analyzer.close()
        }
    }
    LaunchedEffect(Unit) {
        // Wait for the guard service to let go of the camera before we take it.
        GuardState.scanning.first { !it }
        val provider = ctx.cameraProvider()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                ).build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
        analysis.setAnalyzer(executor) { proxy ->
            proxy.use {
                val bmp = it.toUprightBitmap()
                val faces = runCatching { analyzer.detect(bmp) }.getOrDefault(emptyList())
                callback(bmp, faces)
            }
        }
        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
        }
    }
    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Ink.Surface),
    )
}

@Composable
private fun EnrollView(contentPadding: PaddingValues, onDone: (OwnerProfile) -> Unit, onCancel: () -> Unit) {
    val samples = remember { mutableStateListOf<FloatArray>() }
    var hint by remember { mutableStateOf("Put your face in the circle") }
    var lastAt by remember { mutableLongStateOf(0L) }
    val haptics = LocalHapticFeedback.current
    val progress by animateFloatAsState(
        samples.size / TARGET_SAMPLES.toFloat(),
        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow),
        label = "enroll",
    )
    val done = samples.size >= TARGET_SAMPLES

    LaunchedEffect(done) {
        if (done) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            val list = samples.toList()
            onDone(OwnerProfile(list, FaceSignature.calibrate(list), System.currentTimeMillis()))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(bottom = contentPadding.calculateBottomPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeader("Scanning", "Face ID setup")
        Spacer(Modifier.height(20.dp))
        Box(Modifier.size(310.dp), contentAlignment = Alignment.Center) {
            FaceCamera(240.dp) { bmp, faces ->
                if (samples.size >= TARGET_SAMPLES) return@FaceCamera
                val now = System.currentTimeMillis()
                val f = faces.singleOrNull()
                val newHint = when {
                    faces.isEmpty() -> "Put your face in the circle"
                    faces.size > 1 -> "Just you in the frame, please"
                    f!!.boundingBox.width() < bmp.width * 0.22f -> "Come a little closer"
                    abs(f.headEulerAngleY) > 35 || abs(f.headEulerAngleX) > 25 -> "Look a bit more toward the screen"
                    else -> null
                }
                if (newHint != null) {
                    hint = newHint
                    return@FaceCamera
                }
                hint = "Slowly circle your head"
                if (now - lastAt < 230) return@FaceCamera
                val face = f!!
                val box = android.graphics.Rect(face.boundingBox).apply { intersect(0, 0, bmp.width, bmp.height) }
                val sig = FaceSignature.compute(bmp, box, face.headEulerAngleZ)
                lastAt = now
                samples.add(sig)
            }
            TickRing(progress, Modifier.size(290.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"), color = Ink.Text,
        )
        Spacer(Modifier.height(4.dp))
        AnimatedContent(hint, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "hint") {
            Text(it, style = MaterialTheme.typography.titleMedium, color = Ink.Text, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(1f))
        SecondaryButton("Cancel", color = Ink.TextMuted, onClick = onCancel)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TestView(owner: OwnerProfile?, contentPadding: PaddingValues, onClose: () -> Unit) {
    var label by remember { mutableStateOf("Looking…") }
    var isYou by remember { mutableStateOf<Boolean?>(null) }
    var match by remember { mutableFloatStateOf(0f) }
    val color by animateColorAsState(
        when (isYou) {
            true -> Ink.Accent
            false -> Ink.Alert
            null -> Ink.LineStrong
        },
        label = "testColor",
    )
    val animatedMatch by animateFloatAsState(match, label = "match")

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(bottom = contentPadding.calculateBottomPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenHeader("Who's this?", "Recognition test")
        Spacer(Modifier.height(20.dp))
        Box(Modifier.size(290.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(color, r * 0.9f, style = Stroke(3.dp.toPx()))
            }
            FaceCamera(240.dp) { bmp, faces ->
                val o = owner ?: return@FaceCamera
                val f = faces.maxByOrNull { it.boundingBox.width() }
                if (f == null) {
                    label = "No face in view"; isYou = null; match = 0f
                    return@FaceCamera
                }
                val box = android.graphics.Rect(f.boundingBox).apply { intersect(0, 0, bmp.width, bmp.height) }
                val d = FaceSignature.minDistance(o.samples, FaceSignature.compute(bmp, box, f.headEulerAngleZ))
                val you = d <= o.threshold
                isYou = you
                label = if (you) "That's you" else "Stranger: would count as a peek"
                match = (1f - (d / (o.threshold * 2f))).coerceIn(0f, 1f)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(label, style = MaterialTheme.typography.headlineMedium, color = color, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Panel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Match", style = MaterialTheme.typography.bodyMedium, color = Ink.TextMuted, modifier = Modifier.weight(1f))
                Text("${(animatedMatch * 100).toInt()}%", style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"), color = Ink.Text)
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(Ink.Sunken)) {
                Box(Modifier.fillMaxWidth(animatedMatch).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
            }
            Spacer(Modifier.height(10.dp))
            Text("Ask a friend to look at the camera. They should show up as a stranger.", style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted)
        }
        Spacer(Modifier.weight(1f))
        SecondaryButton("Done", onClick = onClose)
        Spacer(Modifier.height(16.dp))
    }
}
