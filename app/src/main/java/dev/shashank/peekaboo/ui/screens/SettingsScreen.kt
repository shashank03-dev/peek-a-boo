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
import dev.shashank.peekaboo.data.ShieldMode
import dev.shashank.peekaboo.data.ShieldStyle
import dev.shashank.peekaboo.overlay.ShieldOverlay
import dev.shashank.peekaboo.ui.Route
import dev.shashank.peekaboo.ui.components.SegmentedControl
import dev.shashank.peekaboo.ui.components.Tag
import androidx.compose.material.icons.rounded.AppShortcut
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import dev.shashank.peekaboo.overlay.NotchOverlay
import dev.shashank.peekaboo.overlay.NOTCH_EXIT_MS
import dev.shashank.peekaboo.overlay.NotchPill
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.Permissions
import dev.shashank.peekaboo.ui.components.ElasticSlider
import dev.shashank.peekaboo.ui.components.HoldButton
import dev.shashank.peekaboo.ui.components.ListRow
import dev.shashank.peekaboo.ui.components.PeekSwitch
import dev.shashank.peekaboo.ui.components.ScreenHeader
import dev.shashank.peekaboo.ui.components.Section
import dev.shashank.peekaboo.ui.components.bouncyClick
import dev.shashank.peekaboo.ui.components.panel
import dev.shashank.peekaboo.ui.rememberPermissions
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.Inter
import dev.shashank.peekaboo.ui.theme.MonoValue
import dev.shashank.peekaboo.ui.theme.Ink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: MainViewModel, contentPadding: PaddingValues) {
    val ctx = LocalContext.current
    val s = vm.settings.collectAsStateWithLifecycle().value ?: return
    val perms by rememberPermissions()
    val isPro by vm.isPro.collectAsStateWithLifecycle()
    val owner by vm.owner.collectAsStateWithLifecycle()
    var strength by remember(s.shieldStrength) { mutableFloatStateOf(s.shieldStrength.toFloat()) }
    var previewingShield by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var offset by remember(s.notchOffsetDp) { mutableFloatStateOf(s.notchOffsetDp.toFloat()) }

    fun open(intent: Intent) = runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

    fun previewNotch() {
        if (!Permissions.overlay(ctx)) {
            open(Permissions.overlayIntent(ctx))
            return
        }
        val overlay = NotchOverlay(ctx.applicationContext)
        val visible = mutableStateOf(true)
        val density = ctx.resources.displayMetrics.density
        val pillTop = overlay.cameraBottomPx() + ((offset.toInt() + 6) * density).toInt()
        if (!overlay.show(pillTop) { spec -> NotchPill(peepers = 2, visible = visible.value, spec = spec) }) return
        scope.launch {
            try {
                delay(3000); visible.value = false
                delay(NOTCH_EXIT_MS)
            } finally {
                overlay.hide()
            }
        }
    }

    /** Anyone can try the shield for a few seconds; that's the best sales pitch it has. */
    fun previewShield() {
        if (!Permissions.overlay(ctx)) {
            open(Permissions.overlayIntent(ctx))
            return
        }
        if (previewingShield) return
        val shield = ShieldOverlay(ctx.applicationContext)
        shield.show(s.shieldStyle, strength.toInt())
        previewingShield = true
        scope.launch {
            try {
                delay(5000)
            } finally {
                shield.hide()
                previewingShield = false
            }
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

        if (!isPro) ProBanner { vm.open(Route.Pro) }

        Text("SENSITIVITY", style = Eyebrow, color = Ink.TextFaint, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Sensitivity.entries.forEach { level ->
                SensitivityCard(
                    level = level,
                    icon = when (level) {
                        Sensitivity.Low -> Icons.Rounded.Spa
                        Sensitivity.Balanced -> Icons.Rounded.Balance
                        Sensitivity.High -> Icons.Rounded.RemoveRedEye
                    },
                    selected = s.sensitivity == level,
                    modifier = Modifier.weight(1f),
                ) { vm.setSensitivity(level) }
            }
        }
        Text(
            "Counts a peek after ${s.sensitivity.dwellMs / 1000f}s of looking." +
                if (s.sensitivity == Sensitivity.High) " Also catches people further away." else "",
            style = MaterialTheme.typography.bodySmall, color = Ink.TextFaint,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
        )

        Section(footer = "Strict mode counts anyone who isn't your enrolled face, even if they're the one holding the phone.") {
            ListRow("Strict mode", icon = Icons.Rounded.Shield, showDivider = false, onClick = { vm.setStrict(!s.strictMode) }, chevron = false, trailing = {
                PeekSwitch(s.strictMode, vm::setStrict)
            })
        }

        Section(header = "Alerts") {
            ListRow("Peek notch", subtitle = "Pops up under the camera", icon = Icons.Rounded.SmartButton, onClick = { vm.setShowNotch(!s.showNotch) }, chevron = false, trailing = {
                PeekSwitch(s.showNotch, vm::setShowNotch)
            })
            ListRow("Haptic tap", subtitle = "A soft buzz when a peek starts", icon = Icons.Rounded.Vibration, onClick = { vm.setHaptics(!s.haptics) }, chevron = false, trailing = {
                PeekSwitch(s.haptics, vm::setHaptics)
            })
            ListRow("Peeker snapshot", subtitle = "Kept privately on this phone", icon = Icons.Rounded.PhotoCamera, showDivider = false, onClick = { vm.setSnapshots(!s.snapshots) }, chevron = false, trailing = {
                PeekSwitch(s.snapshots, vm::setSnapshots)
            })
        }

        Row(Modifier.padding(start = 4.dp, top = 16.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("PRIVACY SHIELD", style = Eyebrow, color = Ink.TextFaint)
            Spacer(Modifier.width(8.dp))
            Tag("Pro", Ink.AccentSoft, Ink.Accent)
        }
        Column(Modifier.fillMaxWidth().panel().padding(16.dp)) {
            Text("Turns on", style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            SegmentedControl(
                options = ShieldMode.entries.map { it.label },
                selected = (if (isPro) s.shieldMode else ShieldMode.Off).ordinal,
                onSelect = { i -> vm.withPro { vm.setShieldMode(ShieldMode.entries[i]) } },
            )
            Spacer(Modifier.height(16.dp))
            Text("Look", style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            SegmentedControl(
                options = ShieldStyle.entries.map { it.label },
                selected = s.shieldStyle.ordinal,
                onSelect = { i -> vm.setShieldStyle(ShieldStyle.entries[i]) },
            )
            Spacer(Modifier.height(16.dp))
            Text("Strength", style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted, modifier = Modifier.padding(start = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ElasticSlider(
                    value = strength,
                    range = ShieldOverlay.MIN_STRENGTH.toFloat()..ShieldOverlay.MAX_STRENGTH.toFloat(),
                    onValueChange = { strength = it },
                    onValueChangeFinished = { vm.setShieldStrength(strength.toInt()) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text("${strength.toInt()}%", style = MonoValue, color = Ink.TextMuted, modifier = Modifier.width(44.dp))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(CircleShape)
                    .bouncyClick(onClick = ::previewShield)
                    .background(Ink.Raised)
                    .border(1.dp, Ink.LineStrong, CircleShape)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayCircle, null, tint = Ink.Text, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (previewingShield) "Shield on for 5 seconds…" else "Try it for 5 seconds",
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Ink.Text,
                )
            }
        }
        Text(
            "Darkens the screen and lays a fine pattern over it. Right in front of the phone you can still read; " +
                "from beside you or a step away, text washes out. Taps still go through.",
            style = MaterialTheme.typography.bodySmall, color = Ink.TextFaint,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
        )

        Section(footer = "Someone-else alert needs your face set up on the You tab, so it can tell you apart.") {
            ListRow(
                "Protected apps",
                subtitle = when {
                    !s.protectedOnly -> "Shield works in every app"
                    s.protectedApps.isEmpty() -> "No apps chosen yet"
                    else -> "Only in ${s.protectedApps.size} app${if (s.protectedApps.size == 1) "" else "s"}"
                },
                icon = Icons.Rounded.AppShortcut,
                onClick = { vm.withPro { vm.open(Route.Apps) } },
            )
            ListRow(
                "Someone-else alert",
                subtitle = if (owner == null) "Set up your face first" else "Logs a photo if someone else uses your phone",
                icon = Icons.Rounded.PersonSearch,
                onClick = if (owner == null) null else ({ vm.withPro { vm.setStrangerAlert(!s.strangerAlert) } }),
                chevron = false,
                showDivider = false,
                trailing = { PeekSwitch(isPro && s.strangerAlert && owner != null, { v -> vm.withPro { vm.setStrangerAlert(v) } }, enabled = owner != null) },
            )
        }

        Text("NOTCH POSITION", style = Eyebrow, color = Ink.TextFaint, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 10.dp))
        Column(Modifier.fillMaxWidth().panel().padding(16.dp)) {
            NotchMockup(offset)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ElasticSlider(
                    value = offset,
                    range = -20f..80f,
                    onValueChange = { offset = it },
                    onValueChangeFinished = { vm.setNotchOffset(offset.toInt()) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text("${offset.toInt()}dp", style = MonoValue, color = Ink.TextMuted, modifier = Modifier.width(44.dp))
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(CircleShape)
                    .bouncyClick(onClick = ::previewNotch)
                    .background(Ink.Raised)
                    .border(1.dp, Ink.LineStrong, CircleShape)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayCircle, null, tint = Ink.Text, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Preview on screen", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Ink.Text)
            }
        }

        Section(header = "Stay alive", footer = "The camera is only used while your phone is unlocked. Unrestricted battery stops Android from putting the guard to sleep.") {
            ListRow("Start after reboot", icon = Icons.Rounded.PowerSettingsNew, onClick = { vm.setStartOnBoot(!s.startOnBoot) }, chevron = false, trailing = {
                PeekSwitch(s.startOnBoot, vm::setStartOnBoot)
            })
            PermissionRow("Battery", if (perms.battery) "Unrestricted" else "Restricted", perms.battery, Icons.Rounded.BatteryChargingFull) { open(Permissions.batteryIntent(ctx)) }
            PermissionRow("Display over apps", if (perms.overlay) "Allowed" else "Needed for the notch", perms.overlay, Icons.Rounded.Layers) { open(Permissions.overlayIntent(ctx)) }
            PermissionRow("Camera", if (perms.camera) "Allowed" else "Needed to see peekers", perms.camera, Icons.Rounded.CameraFront) { open(Permissions.appSettingsIntent(ctx)) }
            PermissionRow("Notifications", if (perms.notifications) "Allowed" else "Off", perms.notifications, Icons.Rounded.Notifications, last = true) { open(Permissions.appSettingsIntent(ctx)) }
        }

        Text("DATA", style = Eyebrow, color = Ink.TextFaint, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 12.dp))
        HoldButton("Hold to clear history", "History cleared", icon = Icons.Rounded.DeleteForever, onHold = vm::clearHistory)
        Text(
            "Deletes every recorded peek and snapshot. Hold to confirm.",
            style = MaterialTheme.typography.bodySmall, color = Ink.TextFaint,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        )

        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Peek-a-Boo ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted)
            Spacer(Modifier.height(4.dp))
            Text("Everything stays on this phone.", style = MaterialTheme.typography.bodySmall, color = Ink.TextFaint)
        }
    }
}

@Composable
internal fun PermissionRow(title: String, status: String, ok: Boolean, icon: ImageVector, last: Boolean = false, onClick: () -> Unit) {
    ListRow(
        title,
        icon = icon,
        showDivider = !last,
        onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (ok) Ink.Accent else Ink.Alert))
                Spacer(Modifier.width(8.dp))
                Text(status, style = MaterialTheme.typography.bodySmall, color = if (ok) Ink.TextMuted else Ink.Text)
            }
        },
    )
}

@Composable
internal fun SensitivityCard(level: Sensitivity, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .bouncyClick(onClick = onClick)
            .panel(
                RoundedCornerShape(16.dp),
                color = if (selected) Ink.Raised else Ink.Surface,
                border = if (selected) Ink.Accent else Ink.Line,
            )
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = if (selected) Ink.Accent else Ink.TextMuted, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text(level.label, style = MaterialTheme.typography.titleMedium, color = if (selected) Ink.Text else Ink.TextMuted)
        Spacer(Modifier.height(4.dp))
        Text("${level.dwellMs / 1000f}s", style = MonoValue, color = Ink.TextFaint)
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
            .background(Ink.Bg)
            .border(1.dp, Ink.LineStrong, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 14.dp, bottomEnd = 14.dp)),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("9:41", style = MonoValue.copy(fontSize = 11.sp), color = Ink.TextMuted)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(width = 16.dp, height = 8.dp).clip(RoundedCornerShape(2.dp)).background(Ink.TextMuted))
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
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Visibility, null, tint = Ink.Alert, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text("Peeping", style = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 10.sp), color = Color.White)
            Spacer(Modifier.width(6.dp))
            Text(
                "2",
                style = androidx.compose.ui.text.TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 9.sp),
                color = Color.White,
                modifier = Modifier.clip(CircleShape).background(Ink.Alert).padding(horizontal = 5.dp),
            )
        }
    }
}

/** The upsell at the top of Tune for free users. */
@Composable
private fun ProBanner(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
            .bouncyClick(onClick = onClick)
            .panel(RoundedCornerShape(20.dp), color = Ink.Raised, border = Ink.Accent.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Ink.AccentSoft), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = Ink.Accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Get Peek-a-Boo Pro", style = MaterialTheme.typography.titleMedium, color = Ink.Text)
            Spacer(Modifier.height(2.dp))
            Text("Privacy Shield, protected apps and someone-else alert", style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Ink.TextFaint, modifier = Modifier.size(20.dp))
    }
}
