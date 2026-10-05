package dev.shashank.peekaboo.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartButton
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.shashank.peekaboo.BuildConfig
import dev.shashank.peekaboo.app
import dev.shashank.peekaboo.data.Sensitivity
import dev.shashank.peekaboo.overlay.NotchOverlay
import dev.shashank.peekaboo.overlay.NotchPill
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.Permissions
import dev.shashank.peekaboo.ui.components.IosSwitch
import dev.shashank.peekaboo.ui.components.LargeTitle
import dev.shashank.peekaboo.ui.components.ListRow
import dev.shashank.peekaboo.ui.components.Section
import dev.shashank.peekaboo.ui.components.SegmentedControl
import dev.shashank.peekaboo.ui.rememberPermissions
import dev.shashank.peekaboo.ui.theme.Ios
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
        LargeTitle("Settings")

        Section(header = "Detection", footer = "${s.sensitivity.label}: a peek is counted after someone looks for ${s.sensitivity.dwellMs / 1000f}s. Paranoid also catches people further away.") {
            Column(Modifier.padding(16.dp)) {
                SegmentedControl(
                    Sensitivity.entries.map { it.label },
                    s.sensitivity.ordinal,
                    { vm.setSensitivity(Sensitivity.entries[it]) },
                )
            }
        }

        Section(footer = "Strict mode counts anyone who isn't your enrolled face — even if they're holding the phone.") {
            ListRow("Strict mode", icon = Icons.Rounded.Shield, iconColor = Ios.Indigo, showDivider = false, trailing = {
                IosSwitch(s.strictMode, vm::setStrict)
            })
        }

        Section(header = "Alerts") {
            ListRow("Peek notch", subtitle = "Pops up under the camera", icon = Icons.Rounded.SmartButton, iconColor = Ios.Pink, trailing = {
                IosSwitch(s.showNotch, vm::setShowNotch)
            })
            ListRow("Haptic tap", subtitle = "A subtle buzz when a peek starts", icon = Icons.Rounded.Vibration, iconColor = Ios.Orange, trailing = {
                IosSwitch(s.haptics, vm::setHaptics)
            })
            ListRow("Peeker snapshot", subtitle = "Saved privately on this phone", icon = Icons.Rounded.PhotoCamera, iconColor = Ios.Teal, trailing = {
                IosSwitch(s.snapshots, vm::setSnapshots)
            })
            ListRow("Preview the notch", icon = Icons.Rounded.Visibility, iconColor = Ios.Purple, showDivider = false, onClick = ::previewNotch)
        }

        Section(header = "Notch position", footer = "Nudge the notch so it sits right below your camera.") {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Slider(
                    value = offset,
                    onValueChange = { offset = it },
                    onValueChangeFinished = { vm.setNotchOffset(offset.toInt()) },
                    valueRange = -20f..80f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Ios.Blue,
                        inactiveTrackColor = Ios.Fill,
                    ),
                )
                Text("${offset.toInt()} dp", style = MaterialTheme.typography.bodySmall, color = Ios.Secondary)
            }
        }

        Section(header = "Stay alive", footer = "Peek-a-Boo only uses the camera while your phone is unlocked. Disabling battery optimisation stops Android from killing the guard.") {
            ListRow("Start after reboot", icon = Icons.Rounded.PowerSettingsNew, iconColor = Ios.Green, trailing = {
                IosSwitch(s.startOnBoot, vm::setStartOnBoot)
            })
            ListRow(
                "Battery optimisation",
                subtitle = if (perms.battery) "Unrestricted ✓" else "Restricted — tap to fix",
                icon = Icons.Rounded.BatteryChargingFull, iconColor = Ios.Green,
                onClick = { open(Permissions.batteryIntent(ctx)) },
            )
            ListRow(
                "Display over apps",
                subtitle = if (perms.overlay) "Allowed ✓" else "Needed for the notch",
                icon = Icons.Rounded.Layers, iconColor = Ios.Blue,
                onClick = { open(Permissions.overlayIntent(ctx)) },
            )
            ListRow(
                "Camera",
                subtitle = if (perms.camera) "Allowed ✓" else "Needed to see peekers",
                icon = Icons.Rounded.CameraFront, iconColor = Ios.Gray,
                onClick = { open(Permissions.appSettingsIntent(ctx)) },
            )
            ListRow(
                "Notifications",
                subtitle = if (perms.notifications) "Allowed ✓" else "Off",
                icon = Icons.Rounded.Notifications, iconColor = Ios.Red, showDivider = false,
                onClick = { open(Permissions.appSettingsIntent(ctx)) },
            )
        }

        Section(header = "Data") {
            ListRow(
                "Clear peek history", icon = Icons.Rounded.DeleteForever, iconColor = Ios.Red,
                titleColor = Ios.Red, chevron = false, showDivider = false,
                onClick = { confirmClear = true },
            )
        }

        Section(footer = "Everything is processed on-device. No images or data ever leave your phone.") {
            ListRow("Version", icon = Icons.Rounded.Info, iconColor = Ios.Gray, showDivider = false, trailing = {
                Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodyLarge, color = Ios.Secondary)
            })
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = Ios.CardElevated,
            title = { Text("Clear all peeks?") },
            text = { Text("This deletes every recorded peek and snapshot. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text("Clear", color = Ios.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = Ios.Blue) }
            },
        )
    }
}
