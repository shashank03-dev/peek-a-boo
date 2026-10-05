package dev.shashank.peekaboo.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.Permissions
import dev.shashank.peekaboo.ui.components.OrbMode
import dev.shashank.peekaboo.ui.components.PrimaryButton
import dev.shashank.peekaboo.ui.components.RadarOrb
import dev.shashank.peekaboo.ui.components.bouncyClick
import dev.shashank.peekaboo.ui.rememberPermissions
import dev.shashank.peekaboo.ui.theme.Ios

private data class Step(
    val title: String,
    val body: String,
    val icon: ImageVector?,
    val brush: Brush,
    val cta: String,
)

private val steps = listOf(
    Step(
        "Peek-a-Boo",
        "Know when someone is reading over your shoulder. A tiny notch pops up under your camera the moment a stranger looks at your screen.",
        null, Ios.GuardGradient, "Get Started",
    ),
    Step(
        "Front camera",
        "The front camera watches for faces only while your phone is unlocked. Everything stays on your device.",
        Icons.Rounded.CameraFront, Ios.IdleGradient, "Allow Camera",
    ),
    Step(
        "The peek notch",
        "Allow display over other apps so the “Peeping · 2” alert can appear right under your camera, in any app.",
        Icons.Rounded.Layers, Ios.AlertGradient, "Allow Overlay",
    ),
    Step(
        "Stay in the loop",
        "A quiet, persistent notification keeps the guard alive and shows today's peek count.",
        Icons.Rounded.Notifications, Brush.linearGradient(listOf(Ios.Red, Ios.Pink)), "Allow Notifications",
    ),
    Step(
        "Always on",
        "Let Peek-a-Boo run unrestricted so Android doesn't put the guard to sleep.",
        Icons.Rounded.BatteryChargingFull, Brush.linearGradient(listOf(Ios.Green, Ios.Mint)), "Allow Background",
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

    Column(
        Modifier
            .fillMaxSize()
            .background(Ios.Background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        PageDots(steps.size, index)
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 3 * dir } + fadeIn(tween(250))) togetherWith
                    (slideOutHorizontally(tween(200)) { -it / 3 * dir } + fadeOut(tween(150)))
            },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { i ->
            val step = steps[i]
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (step.icon == null) {
                    RadarOrb(OrbMode.Guarding, size = 240.dp)
                } else {
                    Box(
                        Modifier.size(132.dp).clip(RoundedCornerShape(36.dp)).background(step.brush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(step.icon, null, tint = Color.White, modifier = Modifier.size(68.dp))
                    }
                }
                Spacer(Modifier.height(36.dp))
                Text(
                    step.title,
                    style = if (i == 0) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                    color = Ios.Label, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(step.body, style = MaterialTheme.typography.bodyLarge, color = Ios.Secondary, textAlign = TextAlign.Center)
            }
        }
        PrimaryButton(steps[index].cta, steps[index].brush, onClick = ::act)
        Spacer(Modifier.height(10.dp))
        Text(
            if (index == 0) " " else "Not now",
            style = MaterialTheme.typography.bodyLarge,
            color = Ios.Secondary,
            modifier = Modifier
                .padding(8.dp)
                .bouncyClick(enabled = index != 0) { next() },
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PageDots(count: Int, selected: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val w by animateDpAsState(if (i == selected) 22.dp else 7.dp, spring(dampingRatio = 0.7f), label = "dot")
            Box(
                Modifier
                    .height(7.dp)
                    .width(w)
                    .clip(CircleShape)
                    .background(if (i == selected) Ios.Label else Ios.Fill)
            )
        }
    }
}
