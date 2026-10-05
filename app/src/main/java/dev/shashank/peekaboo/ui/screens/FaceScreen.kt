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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import dev.shashank.peekaboo.detect.toUprightBitmap
import dev.shashank.peekaboo.detect.cameraProvider
import dev.shashank.peekaboo.service.GuardState
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.Permissions
import dev.shashank.peekaboo.ui.components.Card
import dev.shashank.peekaboo.ui.components.LargeTitle
import dev.shashank.peekaboo.ui.components.ListRow
import dev.shashank.peekaboo.ui.components.PrimaryButton
import dev.shashank.peekaboo.ui.components.SecondaryButton
import dev.shashank.peekaboo.ui.components.Section
import dev.shashank.peekaboo.ui.components.TickRing
import dev.shashank.peekaboo.ui.theme.Ios
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
        transitionSpec = { (fadeIn(tween(250)) + scaleIn(initialScale = 0.97f)) togetherWith fadeOut(tween(150)) },
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

@Composable
private fun FaceOverview(
    owner: OwnerProfile?,
    contentPadding: PaddingValues,
    onEnroll: () -> Unit,
    onTest: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(bottom = contentPadding.calculateBottomPadding()),
    ) {
        LargeTitle("Face ID", subtitle = "Never flag yourself")
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                TickRing(if (owner != null) 1f else 0f, Modifier.fillMaxSize(), color = Ios.Green)
                Box(
                    Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(if (owner != null) Ios.GuardGradient else Ios.IdleGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (owner != null) Icons.Rounded.CheckCircle else Icons.Rounded.Face,
                        null, tint = Color.White, modifier = Modifier.size(64.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            if (owner != null) "Your face is set up" else "Teach Peek-a-Boo your face",
            style = MaterialTheme.typography.headlineMedium, color = Ios.Label,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (owner != null) "You're recognised and never counted as a peeper — even when a friend is holding your phone and you look over."
            else "Look at the camera and slowly move your head in a circle. It takes about 10 seconds.",
            style = MaterialTheme.typography.bodyMedium, color = Ios.Secondary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(24.dp))
        if (owner == null) {
            PrimaryButton("Set Up Face ID", Ios.GuardGradient, icon = Icons.Rounded.Face, onClick = onEnroll)
        } else {
            val fmt = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())
            Section(header = "Your face") {
                ListRow("Samples", icon = Icons.Rounded.Face, iconColor = Ios.Green, trailing = {
                    Text("${owner.samples.size}", style = MaterialTheme.typography.bodyLarge, color = Ios.Secondary)
                })
                ListRow("Enrolled", icon = Icons.Rounded.Lock, iconColor = Ios.Blue, showDivider = false, trailing = {
                    Text(fmt.format(Date(owner.enrolledAt)), style = MaterialTheme.typography.bodyMedium, color = Ios.Secondary)
                })
            }
            Section {
                ListRow("Test recognition", icon = Icons.Rounded.Science, iconColor = Ios.Purple, onClick = onTest)
                ListRow("Set up again", icon = Icons.Rounded.Refresh, iconColor = Ios.Orange, onClick = onEnroll)
                ListRow("Remove my face", icon = Icons.Rounded.Delete, iconColor = Ios.Red, titleColor = Ios.Red, showDivider = false, chevron = false, onClick = onRemove)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "🔒 Your face never leaves this phone. Peek-a-Boo stores a compact pattern signature, not a photo.",
            style = MaterialTheme.typography.bodySmall, color = Ios.Secondary,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
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
            .background(Ios.Card),
    )
}

@Composable
private fun EnrollView(contentPadding: PaddingValues, onDone: (OwnerProfile) -> Unit, onCancel: () -> Unit) {
    val samples = remember { mutableStateListOf<FloatArray>() }
    var hint by remember { mutableStateOf("Position your face in the circle") }
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
        LargeTitle("Face ID", subtitle = "Setting up")
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            FaceCamera(250.dp) { bmp, faces ->
                if (samples.size >= TARGET_SAMPLES) return@FaceCamera
                val now = System.currentTimeMillis()
                val f = faces.singleOrNull()
                val newHint = when {
                    faces.isEmpty() -> "Position your face in the circle"
                    faces.size > 1 -> "Only you in the frame, please"
                    f!!.boundingBox.width() < bmp.width * 0.22f -> "Move a little closer"
                    abs(f.headEulerAngleY) > 35 || abs(f.headEulerAngleX) > 25 -> "Look a bit more toward the screen"
                    else -> null
                }
                if (newHint != null) {
                    hint = newHint
                    return@FaceCamera
                }
                hint = "Move your head slowly to complete the circle"
                if (now - lastAt < 230) return@FaceCamera
                val face = f!!
                val box = android.graphics.Rect(face.boundingBox).apply { intersect(0, 0, bmp.width, bmp.height) }
                val sig = FaceSignature.compute(bmp, box, face.headEulerAngleZ)
                lastAt = now
                samples.add(sig)
            }
            TickRing(progress, Modifier.fillMaxSize(), color = Ios.Green)
        }
        Spacer(Modifier.height(24.dp))
        AnimatedContent(hint, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "hint") {
            Text(it, style = MaterialTheme.typography.titleMedium, color = Ios.Label, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(6.dp))
        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = Ios.Secondary)
        Spacer(Modifier.weight(1f))
        SecondaryButton("Cancel", color = Ios.Gray, onClick = onCancel)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TestView(owner: OwnerProfile?, contentPadding: PaddingValues, onClose: () -> Unit) {
    var label by remember { mutableStateOf("Looking…") }
    var isYou by remember { mutableStateOf<Boolean?>(null) }
    var match by remember { mutableFloatStateOf(0f) }
    val color = when (isYou) {
        true -> Ios.Green
        false -> Ios.Red
        null -> Ios.Gray
    }
    val animatedMatch by animateFloatAsState(match, label = "match")

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(bottom = contentPadding.calculateBottomPadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LargeTitle("Face ID", subtitle = "Recognition test")
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.size(270.dp).border(4.dp, color, CircleShape).padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            FaceCamera(250.dp) { bmp, faces ->
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
                label = if (you) "It's you ✓" else "Not you — would count as a peeper"
                match = (1f - (d / (o.threshold * 2f))).coerceIn(0f, 1f)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(label, style = MaterialTheme.typography.headlineMedium, color = color, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Match", style = MaterialTheme.typography.bodyMedium, color = Ios.Secondary, modifier = Modifier.weight(1f))
                Text("${(animatedMatch * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = Ios.Label)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(Ios.Fill)) {
                Box(Modifier.fillMaxWidth(animatedMatch).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
            }
            Spacer(Modifier.height(8.dp))
            Text("Ask a friend to look at the camera — they should show as \"Not you\".", style = MaterialTheme.typography.bodySmall, color = Ios.Secondary)
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("Done", Ios.IdleGradient, onClick = onClose)
        Spacer(Modifier.height(16.dp))
    }
}
