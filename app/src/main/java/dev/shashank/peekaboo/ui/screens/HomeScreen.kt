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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shashank.peekaboo.data.Reports
import dev.shashank.peekaboo.service.GuardState
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.Permissions
import dev.shashank.peekaboo.ui.TabBarHeight
import dev.shashank.peekaboo.ui.components.EyeOrb
import dev.shashank.peekaboo.ui.components.IconBadge
import dev.shashank.peekaboo.ui.components.OrbMode
import dev.shashank.peekaboo.ui.components.Panel
import dev.shashank.peekaboo.ui.components.PrimaryButton
import dev.shashank.peekaboo.ui.components.SecondaryButton
import dev.shashank.peekaboo.ui.components.SectionLabel
import dev.shashank.peekaboo.ui.components.Sparkline
import dev.shashank.peekaboo.ui.components.StatusChip
import dev.shashank.peekaboo.ui.components.Tag
import dev.shashank.peekaboo.ui.components.bouncyClick
import dev.shashank.peekaboo.ui.components.mood
import dev.shashank.peekaboo.ui.components.panel
import dev.shashank.peekaboo.ui.rememberOrbMode
import dev.shashank.peekaboo.ui.rememberPermissions
import dev.shashank.peekaboo.ui.theme.Ink
import dev.shashank.peekaboo.ui.theme.InterDisplay
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

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setGuard(true)
    }
    val requestCamera = { cameraLauncher.launch(Manifest.permission.CAMERA) }
    val openOverlay = { ctx.startActivity(Permissions.overlayIntent(ctx).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

    // When the guard is off, starting it is the one thing to do: pin it in the thumb zone above the tab bar.
    val pinStart = !enabled
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + if (pinStart) 72.dp else 0.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TopBar(mode) }

        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                EyeOrb(mode, size = 264.dp)
                Spacer(Modifier.height(8.dp))
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        (fadeIn(tween(300)) + slideInVertically { it / 3 }) togetherWith (fadeOut(tween(150)) + slideOutVertically { -it / 3 })
                    },
                    label = "status",
                ) { m ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when (m) {
                                OrbMode.Off -> "Guard is off"
                                OrbMode.Idle -> "On standby"
                                OrbMode.Guarding -> "Watching your back"
                                OrbMode.Alert -> if (peepers > 1) "$peepers people are peeking" else "Someone's peeking"
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            color = if (m == OrbMode.Alert) Ink.Alert else Ink.Text,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when (m) {
                                OrbMode.Off -> "Turn it on to catch shoulder surfers."
                                OrbMode.Idle -> "Resumes the moment you unlock."
                                OrbMode.Guarding -> liveLine(faces, ownerInView, owner != null)
                                OrbMode.Alert -> "Tilt your screen away."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink.TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        if (enabled) {
            item { SecondaryButton("Pause guard", icon = Icons.Rounded.Pause) { vm.setGuard(false) } }
        }

        val steps = listOf(
            SetupStep("Front camera", "So it can see who's looking", Icons.Rounded.CameraFront, perms.camera, requestCamera),
            SetupStep("Peek notch", "Show the alert over other apps", Icons.Rounded.Layers, perms.overlay, openOverlay),
            SetupStep("Your face", "So you never count as a peeker", Icons.Rounded.Face, owner != null, openFace),
        )
        if (steps.any { !it.done }) {
            item { SetupCard(steps) }
        }

        item { SectionLabel("Today", Modifier.padding(top = 20.dp)) }
        item {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PeeksTile(report.peeks, report.hourly.toList(), Modifier.weight(1.15f).fillMaxHeight().bouncyClick(onClick = openReport))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallTile("People", "${report.people}", Modifier.bouncyClick(onClick = openReport))
                    SmallTile("Longest", Reports.formatDuration(report.longestPeekMs), Modifier.bouncyClick(onClick = openReport))
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("Latest", Modifier.weight(1f))
                Row(
                    Modifier.clip(CircleShape).bouncyClick(onClick = openReport).padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("See all", style = MaterialTheme.typography.labelMedium, color = Ink.TextMuted)
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Ink.TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (events.isEmpty()) {
            item {
                Panel(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.Check, Ink.Accent, size = 40.dp, background = Ink.AccentSoft)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("All clear today", style = MaterialTheme.typography.titleMedium, color = Ink.Text)
                            Spacer(Modifier.height(4.dp))
                            Text("No one has looked at your screen.", style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted)
                        }
                    }
                }
            }
        } else {
            events.take(3).forEach { e -> item(key = e.id) { PeekRow(e, onClick = openReport) } }
        }
    }

    if (pinStart) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Ink.Bg.copy(alpha = 0f), Ink.Bg), startY = 0f, endY = 48f))
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = TabBarHeight + navBottom + 32.dp),
        ) {
            PrimaryButton("Start guard", icon = Icons.Rounded.RemoveRedEye) {
                if (Permissions.camera(ctx)) vm.setGuard(true) else requestCamera()
            }
        }
    }
    }
}

private fun liveLine(faces: Int, ownerInView: Boolean, enrolled: Boolean): String = when {
    faces == 0 -> "No one in view."
    ownerInView -> if (faces == 1) "Only you, recognised." else "You and ${faces - 1} nearby."
    faces == 1 -> if (enrolled) "1 face in view." else "Just you."
    else -> "$faces faces in view."
}

@Composable
internal fun TopBar(mode: OrbMode) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Peek-a-Boo", style = MaterialTheme.typography.titleMedium, color = Ink.Text)
            Text(
                SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted,
            )
        }
        val (label, live) = when (mode) {
            OrbMode.Off -> "Off" to false
            OrbMode.Idle -> "Standby" to false
            OrbMode.Guarding -> "Live" to true
            OrbMode.Alert -> "Peek" to true
        }
        StatusChip(label, mode.mood().color, live)
    }
}

internal data class SetupStep(val title: String, val body: String, val icon: ImageVector, val done: Boolean, val fix: () -> Unit)

@Composable
internal fun SetupCard(steps: List<SetupStep>) {
    val doneCount = steps.count { it.done }
    val progress by animateFloatAsState(doneCount / steps.size.toFloat(), tween(600), label = "setup")
    Panel(Modifier.fillMaxWidth(), padding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Finish setup", style = MaterialTheme.typography.titleLarge, color = Ink.Text, modifier = Modifier.weight(1f))
            Text("$doneCount/${steps.size}", style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), color = Ink.TextMuted)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Ink.Sunken)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(Ink.Accent))
        }
        Spacer(Modifier.height(8.dp))
        steps.forEach { s ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .bouncyClick(enabled = !s.done, onClick = s.fix)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (s.done) IconBadge(Icons.Rounded.Check, Ink.Accent, size = 32.dp, background = Ink.AccentSoft)
                else IconBadge(s.icon, Ink.Text, size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, style = MaterialTheme.typography.titleMedium, color = if (s.done) Ink.TextFaint else Ink.Text)
                    Text(s.body, style = MaterialTheme.typography.bodySmall, color = Ink.TextFaint)
                }
                if (!s.done) Tag("Set up", Ink.Text, Ink.Bg)
            }
        }
    }
}

@Composable
internal fun PeeksTile(peeks: Int, hourly: List<Int>, modifier: Modifier) {
    val animated by animateIntAsState(peeks, tween(700), label = "peeks")
    val hot = peeks > 0
    Column(modifier.panel().padding(20.dp)) {
        Text("Peeks", style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted)
        Spacer(Modifier.weight(1f))
        Text(
            "$animated",
            style = MaterialTheme.typography.displayLarge,
            color = if (hot) Ink.Alert else Ink.Text,
        )
        Spacer(Modifier.height(8.dp))
        Sparkline(hourly, if (hot) Ink.Alert else Ink.LineStrong, Modifier.fillMaxWidth().height(28.dp))
    }
}

@Composable
internal fun SmallTile(label: String, value: String, modifier: Modifier) {
    Column(modifier.fillMaxWidth().panel().padding(20.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.headlineLarge, color = Ink.Text, maxLines = 1)
    }
}
