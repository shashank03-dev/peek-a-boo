package dev.shashank.peekaboo.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shashank.peekaboo.data.Reports
import dev.shashank.peekaboo.service.GuardState
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.Permissions
import dev.shashank.peekaboo.ui.components.Card
import dev.shashank.peekaboo.ui.components.IconTile
import dev.shashank.peekaboo.ui.components.LargeTitle
import dev.shashank.peekaboo.ui.components.OrbMode
import dev.shashank.peekaboo.ui.components.PrimaryButton
import dev.shashank.peekaboo.ui.components.RadarOrb
import dev.shashank.peekaboo.ui.components.StatTile
import dev.shashank.peekaboo.ui.components.bouncyClick
import dev.shashank.peekaboo.ui.rememberPermissions
import dev.shashank.peekaboo.ui.theme.Ios
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
    val running by GuardState.running.collectAsStateWithLifecycle()
    val scanning by GuardState.scanning.collectAsStateWithLifecycle()
    val peek by GuardState.peekActive.collectAsStateWithLifecycle()
    val peepers by GuardState.peepersNow.collectAsStateWithLifecycle()
    val faces by GuardState.facesInView.collectAsStateWithLifecycle()
    val ownerInView by GuardState.ownerInView.collectAsStateWithLifecycle()
    val perms by rememberPermissions()
    val enabled = settings?.guardEnabled == true

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setGuard(true)
    }

    val mode = when {
        !enabled -> OrbMode.Off
        peek -> OrbMode.Alert
        running && scanning -> OrbMode.Guarding
        else -> OrbMode.Idle
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LargeTitle(
                "Peek-a-Boo",
                subtitle = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
            ) { StatusChip(mode) }
        }

        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                RadarOrb(mode, size = 260.dp)
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = { (fadeIn() + slideInVertically { it / 3 }) togetherWith (fadeOut() + slideOutVertically { -it / 3 }) },
                    label = "status",
                ) { m ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when (m) {
                                OrbMode.Off -> "Guard is off"
                                OrbMode.Idle -> "Standing by"
                                OrbMode.Guarding -> "Guarding your screen"
                                OrbMode.Alert -> if (peepers > 1) "$peepers people peeping!" else "Someone's peeping!"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = Ios.Label,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when (m) {
                                OrbMode.Off -> "Turn it on to catch shoulder surfers"
                                OrbMode.Idle -> "Resumes the moment you unlock"
                                OrbMode.Guarding -> liveLine(faces, ownerInView, owner != null)
                                OrbMode.Alert -> "Tilt your screen 🙈"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ios.Secondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        item {
            PrimaryButton(
                text = if (enabled) "Stop Guard" else "Start Guard",
                brush = if (enabled) Brush.linearGradient(listOf(Ios.CardElevated, Ios.Fill)) else Ios.GuardGradient,
                icon = if (enabled) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            ) {
                when {
                    enabled -> vm.setGuard(false)
                    Permissions.camera(ctx) -> vm.setGuard(true)
                    else -> cameraLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }

        if (!perms.camera || !perms.overlay) {
            item {
                WarningCard(
                    title = if (!perms.camera) "Camera access needed" else "Allow the peek notch",
                    body = if (!perms.camera) "Peek-a-Boo needs the front camera to see who's looking."
                    else "Let Peek-a-Boo draw over other apps so the alert can appear under your camera.",
                ) {
                    if (!perms.camera) cameraLauncher.launch(Manifest.permission.CAMERA)
                    else ctx.startActivity(Permissions.overlayIntent(ctx).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("${report.peeks}", "Peeks today", Icons.Rounded.Visibility, Ios.Pink, Modifier.weight(1f).bouncyClick(onClick = openReport))
                StatTile("${report.people}", "People", Icons.Rounded.Groups, Ios.Orange, Modifier.weight(1f).bouncyClick(onClick = openReport))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(Reports.formatDuration(report.longestPeekMs), "Longest", Icons.Rounded.Timer, Ios.Teal, Modifier.weight(1f))
                StatTile(report.lastPeekAt?.let { relative(it) } ?: "—", "Last peek", Icons.Rounded.AccessTime, Ios.Indigo, Modifier.weight(1f))
            }
        }

        if (owner == null) {
            item {
                Card(Modifier.fillMaxWidth().bouncyClick(onClick = openFace)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Rounded.Face, Ios.Green, size = 44.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Teach it your face", style = MaterialTheme.typography.titleMedium, color = Ios.Label)
                            Text(
                                "So you're never counted as a peeper — even when someone else holds your phone.",
                                style = MaterialTheme.typography.bodySmall, color = Ios.Secondary,
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Recent peeks", style = MaterialTheme.typography.titleLarge, color = Ios.Label, modifier = Modifier.weight(1f))
                    Text("See all", style = MaterialTheme.typography.bodyMedium, color = Ios.Blue, modifier = Modifier.bouncyClick(onClick = openReport))
                }
                if (events.isEmpty()) {
                    Text(
                        "No one has peeked today. Nice and private ✨",
                        style = MaterialTheme.typography.bodyMedium, color = Ios.Secondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    )
                } else {
                    events.take(3).forEach { PeekListItem(it, onClick = openReport) }
                }
            }
        }
    }
}

private fun liveLine(faces: Int, ownerInView: Boolean, enrolled: Boolean): String = when {
    faces == 0 -> "No one in view"
    ownerInView -> if (faces == 1) "Only you · recognised ✓" else "You + ${faces - 1} nearby"
    faces == 1 -> if (enrolled) "1 face in view" else "Just you"
    else -> "$faces faces in view"
}

@Composable
private fun StatusChip(mode: OrbMode) {
    val (label, color) = when (mode) {
        OrbMode.Off -> "OFF" to Ios.Gray
        OrbMode.Idle -> "READY" to Ios.Indigo
        OrbMode.Guarding -> "LIVE" to Ios.Green
        OrbMode.Alert -> "PEEK" to Ios.Red
    }
    Row(
        Modifier
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun WarningCard(title: String, body: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(Ios.Orange.copy(alpha = 0.14f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.WarningAmber, null, tint = Ios.Orange, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Ios.Secondary)
        }
        Text("Fix", style = MaterialTheme.typography.labelLarge, color = Ios.Orange)
    }
}
