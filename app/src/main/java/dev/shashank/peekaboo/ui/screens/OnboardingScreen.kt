package dev.shashank.peekaboo.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.Permissions
import dev.shashank.peekaboo.ui.components.EyeOrb
import dev.shashank.peekaboo.ui.components.PrimaryButton
import dev.shashank.peekaboo.ui.components.OrbMode
import dev.shashank.peekaboo.ui.components.bouncyClick
import dev.shashank.peekaboo.ui.rememberPermissions
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.Ink

private data class Step(
    val eyebrow: String,
    val title: String,
    val body: String,
    val icon: ImageVector?,
    val cta: String,
)

private val steps = listOf(
    Step(
        "Shoulder-surfer detector",
        "Know when someone\nreads your screen.",
        "A small notch slides out under your camera the moment a stranger looks at your phone.",
        null, "Get started",
    ),
    Step(
        "Step 1 · Camera",
        "Eyes in the\nback of your head",
        "The front camera looks for faces only while your phone is unlocked. Nothing ever leaves the device.",
        Icons.Rounded.CameraFront, "Allow camera",
    ),
    Step(
        "Step 2 · Notch",
        "Caught in\nthe act",
        "Allow display over other apps so the “Peeping · 2” alert can appear right under your camera, in any app.",
        Icons.Rounded.Layers, "Allow overlay",
    ),
    Step(
        "Step 3 · Notifications",
        "Quietly\non duty",
        "A small persistent notification keeps the guard running and shows today's peek count.",
        Icons.Rounded.Notifications, "Allow notifications",
    ),
    Step(
        "Step 4 · Background",
        "Never\nnods off",
        "Let Peek-a-Boo run unrestricted so Android doesn't put the guard to sleep.",
        Icons.Rounded.BatteryChargingFull, "Allow background",
    ),
)

@Composable
fun OnboardingScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    var index by rememberSaveable { mutableIntStateOf(0) }
    val perms by rememberPermissions()

    fun next() {
        if (index < steps.lastIndex) index++ else {
            vm.finishOnboarding()
            if (Permissions.camera(ctx)) vm.setGuard(true)
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { next() }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { next() }

    // Settings-screen permissions: advance once the user comes back with it granted.
    LaunchedEffect(perms, index) {
        if ((index == 2 && perms.overlay) || (index == 4 && perms.battery)) next()
    }

    fun act() {
        when (index) {
            1 -> if (perms.camera) next() else camera.launch(Manifest.permission.CAMERA)
            2 -> if (perms.overlay) next() else ctx.startActivity(Permissions.overlayIntent(ctx).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            3 -> if (perms.notifications || Build.VERSION.SDK_INT < 33) next() else notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            4 -> if (perms.battery) next() else runCatching {
                ctx.startActivity(Permissions.batteryIntent(ctx).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { next() }
            else -> next()
        }
    }

    val step = steps[index]
    Box(Modifier.fillMaxSize().background(Ink.Bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            ProgressSegments(steps.size, index)
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 4 * dir } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(200)) { -it / 4 * dir } + fadeOut(tween(150)))
                },
                modifier = Modifier.weight(1f),
                label = "onboarding",
            ) { i ->
                val s = steps[i]
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (s.icon == null) EyeOrb(OrbMode.Guarding, size = 264.dp) else GlyphHalo(s.icon)
                    }
                    Spacer(Modifier.height(40.dp))
                    Text(s.eyebrow.uppercase(), style = Eyebrow, color = Ink.TextMuted)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        s.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = Ink.Text,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(s.body, style = MaterialTheme.typography.bodyLarge, color = Ink.TextMuted)
                }
            }
            PrimaryButton(step.cta, onClick = ::act)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                if (index != 0) {
                    Text(
                        "Not now",
                        style = MaterialTheme.typography.labelLarge,
                        color = Ink.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clip(CircleShape).bouncyClick { next() }.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** The step's glyph on a raised tile inside two hairline rings. */
@Composable
internal fun GlyphHalo(icon: ImageVector) {
    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(Ink.Line, r - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
            drawCircle(Ink.Line, r * 0.74f, style = Stroke(1.dp.toPx()))
        }
        Box(
            Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Ink.Raised)
                .border(1.dp, Ink.LineStrong, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Ink.Text, modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
internal fun ProgressSegments(count: Int, selected: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val fill by animateFloatAsState(if (i <= selected) 1f else 0f, spring(dampingRatio = 0.85f), label = "seg")
            Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Ink.Sunken)) {
                Box(Modifier.fillMaxWidth(fill).fillMaxHeight().clip(CircleShape).background(Ink.Text))
            }
        }
    }
}
