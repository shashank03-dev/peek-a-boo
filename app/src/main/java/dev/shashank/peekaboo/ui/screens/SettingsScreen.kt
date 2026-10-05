package dev.shashank.peekaboo.ui.screens

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shashank.peekaboo.BuildConfig
import dev.shashank.peekaboo.app
import dev.shashank.peekaboo.data.Sensitivity
import dev.shashank.peekaboo.overlay.NotchOverlay
import dev.shashank.peekaboo.overlay.NotchPill
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.Permissions
import dev.shashank.peekaboo.ui.components.ListRow
import dev.shashank.peekaboo.ui.components.NightSwitch
import dev.shashank.peekaboo.ui.components.ScreenHeader
import dev.shashank.peekaboo.ui.components.Section
import dev.shashank.peekaboo.ui.components.bouncyClick
import dev.shashank.peekaboo.ui.components.glass
import dev.shashank.peekaboo.ui.rememberPermissions
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.Inter
import dev.shashank.peekaboo.ui.theme.MonoValue
import dev.shashank.peekaboo.ui.theme.Night
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: MainViewModel, contentPadding: PaddingValues) {
    val ctx = LocalContext.current
    val s = vm.settings.collectAsStateWithLifecycle().value ?: return
    val perms by rememberPermissions()
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    var offset by remember(s.notchOffsetDp) { mutableFloatStateOf(s.notchOffsetDp.toFloat()) }

    fun open(intent: Intent) = runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

    fun previewNotch() {
        if (!Permissions.overlay(ctx)) {
            open(Permissions.overlayIntent(ctx))
            return
        }
        val overlay = NotchOverlay(ctx.applicationContext)
        val visible = mutableStateOf(false)
        overlay.show {
            val settings by ctx.app.settings.settings.collectAsStateWithLifecycle(initialValue = s)
            val density = ctx.resources.displayMetrics.density
            NotchPill(
                peepers = 2,
                visible = visible.value,
                topOffsetPx = overlay.cameraBottomPx() + (settings.notchOffsetDp * density).toInt(),
            )
        }
        scope.launch {
            delay(100); visible.value = true
            delay(3000); visible.value = false
            delay(400); overlay.hide()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .padding(bottom = contentPadding.calculateBottomPadding()),
    ) {
        ScreenHeader("Tune", "Make it yours")

        Text("SENSITIVITY", style = Eyebrow, color = Night.TextDim, modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Sensitivity.entries.forEach { level ->
                SensitivityCard(
                    level = level,
                    icon = when (level) {
                        Sensitivity.Low -> Icons.Rounded.Spa
                        Sensitivity.Balanced -> Icons.Rounded.Balance
                        Sensitivity.High -> Icons.Rounded.RemoveRedEye
                    },
                    color = when (level) {
                        Sensitivity.Low -> Night.Teal
                        Sensitivity.Balanced -> Night.Violet
                        Sensitivity.High -> Night.Hot
                    },
                    selected = s.sensitivity == level,
                    modifier = Modifier.weight(1f),
                ) { vm.setSensitivity(level) }
            }
        }
        Text(
            "Counts a peek after ${s.sensitivity.dwellMs / 1000f}s of looking." +
                if (s.sensitivity == Sensitivity.High) " Also catches people further away." else "",
            style = MaterialTheme.typography.bodySmall, color = Night.TextFaint,
            modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 6.dp),
        )

        Section(footer = "Strict mode counts anyone who isn't your enrolled face, even if they're the one holding the phone.") {
            ListRow("Strict mode", icon = Icons.Rounded.Shield, iconColor = Night.Violet, showDivider = false, trailing = {
                NightSwitch(s.strictMode, vm::setStrict)
            })
        }

        Section(header = "Alerts") {
            ListRow("Peek notch", subtitle = "Pops up under the camera", icon = Icons.Rounded.SmartButton, iconColor = Night.Hot, trailing = {
                NightSwitch(s.showNotch, vm::setShowNotch)
            })
            ListRow("Haptic tap", subtitle = "A soft buzz when a peek starts", icon = Icons.Rounded.Vibration, iconColor = Night.Ember, trailing = {
                NightSwitch(s.haptics, vm::setHaptics)
            })
            ListRow("Peeker snapshot", subtitle = "Kept privately on this phone", icon = Icons.Rounded.PhotoCamera, iconColor = Night.Teal, showDivider = false, trailing = {
                NightSwitch(s.snapshots, vm::setSnapshots)
            })
        }

        Text("NOTCH POSITION", style = Eyebrow, color = Night.TextDim, modifier = Modifier.padding(start = 6.dp, top = 20.dp, bottom = 10.dp))
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)).padding(16.dp)) {
            NotchMockup(offset)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = offset,
                    onValueChange = { offset = it },
                    onValueChangeFinished = { vm.setNotchOffset(offset.toInt()) },
                    valueRange = -20f..80f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Night.Hot,
                        inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                    ),
                )
                Spacer(Modifier.width(12.dp))
                Text("${offset.toInt()}dp", style = MonoValue, color = Night.TextDim, modifier = Modifier.width(44.dp))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(CircleShape)
                    .bouncyClick(onClick = ::previewNotch)
                    .background(Night.Hot.copy(alpha = 0.12f))
                    .border(1.dp, Night.Hot.copy(alpha = 0.3f), CircleShape)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayCircle, null, tint = Night.Hot, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Preview on screen", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Night.Hot)
            }
        }

        Section(header = "Stay alive", footer = "The camera is only used while your phone is unlocked. Unrestricted battery stops Android from putting the guard to sleep.") {
            ListRow("Start after reboot", icon = Icons.Rounded.PowerSettingsNew, iconColor = Night.Mint, trailing = {
                NightSwitch(s.startOnBoot, vm::setStartOnBoot)
            })
            PermissionRow("Battery", if (perms.battery) "Unrestricted" else "Restricted", perms.battery, Icons.Rounded.BatteryChargingFull) { open(Permissions.batteryIntent(ctx)) }
            PermissionRow("Display over apps", if (perms.overlay) "Allowed" else "Needed for the notch", perms.overlay, Icons.Rounded.Layers) { open(Permissions.overlayIntent(ctx)) }
            PermissionRow("Camera", if (perms.camera) "Allowed" else "Needed to see peekers", perms.camera, Icons.Rounded.CameraFront) { open(Permissions.appSettingsIntent(ctx)) }
            PermissionRow("Notifications", if (perms.notifications) "Allowed" else "Off", perms.notifications, Icons.Rounded.Notifications, last = true) { open(Permissions.appSettingsIntent(ctx)) }
        }

        Section(header = "Data") {
            ListRow(
                "Clear peek history", subtitle = "Every peek and snapshot", icon = Icons.Rounded.DeleteForever, iconColor = Night.Hot,
                titleColor = Night.Hot, chevron = false, showDivider = false,
                onClick = { confirmClear = true },
            )
        }

        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("peek", style = MaterialTheme.typography.titleLarge, color = Night.TextDim)
                Text("·a·", style = MaterialTheme.typography.titleLarge, color = Night.Mint)
                Text("boo", style = MaterialTheme.typography.titleLarge, color = Night.TextDim)
            }
            Spacer(Modifier.height(4.dp))
            Text("V${BuildConfig.VERSION_NAME} · 100% ON-DEVICE", style = Eyebrow, color = Night.TextFaint)
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Color(0xFF14141E),
            title = { Text("Clear all peeks?", color = Night.Text) },
            text = { Text("This deletes every recorded peek and snapshot. It can't be undone.", color = Night.TextDim) },
            confirmButton = {
                TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text("Clear", color = Night.Hot) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = Night.Text) }
            },
        )
    }
}

@Composable
internal fun PermissionRow(title: String, status: String, ok: Boolean, icon: ImageVector, last: Boolean = false, onClick: () -> Unit) {
    ListRow(
        title,
        icon = icon,
        iconColor = if (ok) Night.Mint else Night.Amber,
        showDivider = !last,
        onClick = onClick,
        trailing = {
            Text(
                status.uppercase(),
                style = Eyebrow,
                color = if (ok) Night.Mint else Night.Amber,
                modifier = Modifier
                    .clip(CircleShape)
                    .background((if (ok) Night.Mint else Night.Amber).copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        },
    )
}

@Composable
internal fun SensitivityCard(level: Sensitivity, icon: ImageVector, color: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) color else Color.Transparent, label = "sensBorder")
    Column(
        modifier
            .bouncyClick(onClick = onClick)
            .glass(RoundedCornerShape(22.dp), fill = if (selected) color.copy(alpha = 0.14f) else Night.Glass)
            .border(1.5.dp, border, RoundedCornerShape(22.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (selected) color else Night.TextDim, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(10.dp))
        Text(level.label, style = MaterialTheme.typography.titleMedium, color = if (selected) Night.Text else Night.TextDim)
        Spacer(Modifier.height(2.dp))
        Text("${level.dwellMs / 1000f}S", style = Eyebrow, color = if (selected) color else Night.TextFaint)
    }
}

/** A miniature phone top showing where the notch will sit as the slider moves. */
@Composable
internal fun NotchMockup(offsetDp: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1A1A28), Color(0xFF0C0C14))))
            .border(1.dp, Night.Stroke, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 14.dp, bottomEnd = 14.dp)),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("9:41", style = MonoValue.copy(fontSize = 11.sp), color = Night.TextDim)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(width = 16.dp, height = 8.dp).clip(RoundedCornerShape(2.dp)).background(Night.TextDim))
        }
        // Camera punch-hole.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
        )
        // Notch, scaled to roughly half size.
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = (26f + (offsetDp * 0.5f).coerceAtLeast(-10f)).dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(1.dp, Night.Hot.copy(alpha = 0.5f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Visibility, null, tint = Night.Hot, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text("Peeping", style = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.sp), color = Color.White)
            Spacer(Modifier.width(6.dp))
            Text(
                "2",
                style = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 9.sp),
                color = Color.White,
                modifier = Modifier.clip(CircleShape).background(Night.Hot).padding(horizontal = 5.dp),
            )
        }
    }
}
