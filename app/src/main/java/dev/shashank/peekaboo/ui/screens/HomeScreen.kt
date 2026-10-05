package dev.shashank.peekaboo.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shashank.peekaboo.data.Reports
import dev.shashank.peekaboo.service.GuardState
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.Permissions
import dev.shashank.peekaboo.ui.components.EyeOrb
import dev.shashank.peekaboo.ui.components.GhostButton
import dev.shashank.peekaboo.ui.components.GlassCard
import dev.shashank.peekaboo.ui.components.GlowButton
import dev.shashank.peekaboo.ui.components.GradientText
import dev.shashank.peekaboo.ui.components.IconBadge
import dev.shashank.peekaboo.ui.components.OrbMode
import dev.shashank.peekaboo.ui.components.Sparkline
import dev.shashank.peekaboo.ui.components.StatusChip
import dev.shashank.peekaboo.ui.components.bouncyClick
import dev.shashank.peekaboo.ui.components.glass
import dev.shashank.peekaboo.ui.components.mood
import dev.shashank.peekaboo.ui.rememberOrbMode
import dev.shashank.peekaboo.ui.rememberPermissions
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.Mood
import dev.shashank.peekaboo.ui.theme.Night
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(vm: MainViewModel, contentPadding: PaddingValues, openReport: () -> Unit, openFace: () -> Unit) {
    val ctx = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val report by vm.today.collectAsStateWithLifecycle()
    val events by vm.todayEvents.collectAsStateWithLifecycle()
    val owner by vm.owner.collectAsStateWithLifecycle()
    val peepers by GuardState.peepersNow.collectAsStateWithLifecycle()
    val faces by GuardState.facesInView.collectAsStateWithLifecycle()
    val ownerInView by GuardState.ownerInView.collectAsStateWithLifecycle()
    val perms by rememberPermissions()
    val enabled = settings?.guardEnabled == true
    val mode = rememberOrbMode(enabled)
    val mood = mode.mood()

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setGuard(true)
    }
    val requestCamera = { cameraLauncher.launch(Manifest.permission.CAMERA) }
    val openOverlay = { ctx.startActivity(Permissions.overlayIntent(ctx).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TopBar(mode, mood) }

        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                EyeOrb(mode, size = 290.dp)
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInVertically { it / 2 }) togetherWith (fadeOut(tween(150)) + slideOutVertically { -it / 2 })
                    },
                    label = "status",
                ) { m ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when (m) {
                                OrbMode.Off -> "Eyes closed."
                                OrbMode.Idle -> "On standby."
                                OrbMode.Guarding -> "Watching your back."
                                OrbMode.Alert -> if (peepers > 1) "$peepers people peeking." else "Someone's peeking."
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            color = Night.Text,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when (m) {
                                OrbMode.Off -> "Start the guard to catch shoulder surfers"
                                OrbMode.Idle -> "Wakes up the moment you unlock"
                                OrbMode.Guarding -> liveLine(faces, ownerInView, owner != null)
                                OrbMode.Alert -> "Tilt your screen away"
                            }.uppercase(),
                            style = Eyebrow,
                            color = if (m == OrbMode.Alert) Night.Hot else Night.TextDim,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        item {
            if (enabled) {
                GhostButton("Pause guard", icon = Icons.Rounded.Pause) { vm.setGuard(false) }
            } else {
                GlowButton("Start watching", Mood.Safe.brush, glow = Night.Mint, icon = Icons.Rounded.RemoveRedEye) {
                    if (Permissions.camera(ctx)) vm.setGuard(true) else requestCamera()
                }
            }
        }

        val steps = listOf(
            SetupStep("Front camera", "So it can see who's looking", Icons.Rounded.CameraFront, perms.camera, requestCamera),
            SetupStep("Peek notch", "Draw the alert over other apps", Icons.Rounded.Layers, perms.overlay, openOverlay),
            SetupStep("Your face", "So you never count as a peeker", Icons.Rounded.Face, owner != null, openFace),
        )
        if (steps.any { !it.done }) {
            item { SetupCard(steps) }
        }

        item { SectionLabel("Today") }
        item {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PeeksTile(report.peeks, report.hourly.toList(), Modifier.weight(1.2f).fillMaxHeight().bouncyClick(onClick = openReport))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallTile("People", "${report.people}", Icons.Rounded.Groups, Night.Violet, Modifier.bouncyClick(onClick = openReport))
                    SmallTile("Longest", Reports.formatDuration(report.longestPeekMs), Icons.Rounded.Timer, Night.Teal, Modifier.bouncyClick(onClick = openReport))
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Latest", Modifier.weight(1f))
                Row(
                    Modifier.clip(CircleShape).bouncyClick(onClick = openReport).padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("ALL ACTIVITY", style = Eyebrow, color = Night.TextDim)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Night.TextDim, modifier = Modifier.size(14.dp))
                }
            }
        }
        if (events.isEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth(), glow = Night.Mint) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.CheckCircle, Night.Mint, size = 44.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("All clear today", style = MaterialTheme.typography.titleMedium, color = Night.Text)
                            Spacer(Modifier.height(2.dp))
                            Text("Nobody has looked at your screen. Keep it that way.", style = MaterialTheme.typography.bodySmall, color = Night.TextDim)
                        }
                    }
                }
            }
        } else {
            events.take(3).forEach { e -> item(key = e.id) { PeekRow(e, onClick = openReport) } }
        }
    }
}

private fun liveLine(faces: Int, ownerInView: Boolean, enrolled: Boolean): String = when {
    faces == 0 -> "No one in view"
    ownerInView -> if (faces == 1) "Only you · recognised" else "You + ${faces - 1} nearby"
    faces == 1 -> if (enrolled) "1 face in view" else "Just you"
    else -> "$faces faces in view"
}

@Composable
internal fun TopBar(mode: OrbMode, mood: Mood) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                SimpleDateFormat("EEE · d MMM", Locale.getDefault()).format(Date()).uppercase(),
                style = Eyebrow, color = Night.TextDim,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("peek", style = MaterialTheme.typography.headlineMedium, color = Night.Text)
                Text("·a·", style = MaterialTheme.typography.headlineMedium, color = mood.primary)
                Text("boo", style = MaterialTheme.typography.headlineMedium, color = Night.Text)
            }
        }
        val (label, live) = when (mode) {
            OrbMode.Off -> "Off" to false
            OrbMode.Idle -> "Standby" to false
            OrbMode.Guarding -> "Live" to true
            OrbMode.Alert -> "Peek" to true
        }
        StatusChip(label, mood.primary, live)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = Eyebrow, color = Night.TextDim, modifier = modifier.padding(start = 4.dp, top = 8.dp))
}

internal data class SetupStep(val title: String, val body: String, val icon: ImageVector, val done: Boolean, val fix: () -> Unit)

@Composable
internal fun SetupCard(steps: List<SetupStep>) {
    val doneCount = steps.count { it.done }
    val progress by animateFloatAsState(doneCount / steps.size.toFloat(), tween(600), label = "setup")
    GlassCard(Modifier.fillMaxWidth(), glow = Night.Amber, padding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("FINISH SETUP", style = Eyebrow, color = Night.Amber)
                Spacer(Modifier.height(4.dp))
                Text("$doneCount of ${steps.size} done", style = MaterialTheme.typography.titleLarge, color = Night.Text)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
            Box(
                Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Night.Amber, Night.Ember)))
            )
        }
        Spacer(Modifier.height(8.dp))
        steps.forEach { s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .bouncyClick(enabled = !s.done, onClick = s.fix)
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(if (s.done) Icons.Rounded.CheckCircle else s.icon, if (s.done) Night.Mint else Night.Amber, size = 34.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, style = MaterialTheme.typography.titleMedium, color = if (s.done) Night.TextDim else Night.Text)
                    Text(s.body, style = MaterialTheme.typography.bodySmall, color = Night.TextFaint)
                }
                if (!s.done) {
                    Text(
                        "FIX",
                        style = Eyebrow,
                        color = Night.Void,
                        modifier = Modifier.clip(CircleShape).background(Night.Amber).padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PeeksTile(peeks: Int, hourly: List<Int>, modifier: Modifier) {
    val animated by animateIntAsState(peeks, tween(900), label = "peeks")
    val hot = peeks > 0
    Column(
        modifier
            .glass(RoundedCornerShape(28.dp), glow = if (hot) Night.Hot else Night.Mint)
            .padding(18.dp),
    ) {
        Text("PEEKS", style = Eyebrow, color = if (hot) Night.Hot else Night.Mint)
        Spacer(Modifier.weight(1f))
        GradientText(
            "$animated",
            MaterialTheme.typography.displayLarge,
            if (hot) Mood.Peek.brush else Brush.verticalGradient(listOf(Night.Text, Night.TextDim)),
        )
        Spacer(Modifier.height(6.dp))
        Sparkline(hourly, if (hot) Night.Hot else Night.Mint, Modifier.fillMaxWidth().height(36.dp))
    }
}

@Composable
internal fun SmallTile(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Column(modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label.uppercase(), style = Eyebrow, color = color)
        }
        Spacer(Modifier.height(14.dp))
        Text(value, style = MaterialTheme.typography.headlineLarge, color = Night.Text, maxLines = 1)
    }
}
