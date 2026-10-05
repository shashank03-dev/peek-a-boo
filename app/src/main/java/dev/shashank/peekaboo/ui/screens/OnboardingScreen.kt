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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.shashank.peekaboo.ui.MainViewModel
import dev.shashank.peekaboo.ui.Permissions
import dev.shashank.peekaboo.ui.components.Aurora
import dev.shashank.peekaboo.ui.components.EyeOrb
import dev.shashank.peekaboo.ui.components.GlowButton
import dev.shashank.peekaboo.ui.components.OrbMode
import dev.shashank.peekaboo.ui.components.bouncyClick
import dev.shashank.peekaboo.ui.rememberPermissions
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.Mood
import dev.shashank.peekaboo.ui.theme.Night

private data class Step(
    val eyebrow: String,
    val title: String,
    val body: String,
    val icon: ImageVector?,
    val mood: Mood,
    val cta: String,
)

private val Notify = Mood(Night.Amber, Night.Ember, Color(0xFF7A2FFF))

private val steps = listOf(
    Step(
        "Shoulder-surfer detector",
        "Someone's always\nwatching.",
        "Now you'll know. A tiny notch slides out under your camera the moment a stranger reads your screen.",
        null, Mood.Safe, "Get started",
    ),
    Step(
        "Step 1 · Camera",
        "Eyes in the\nback of your head.",
        "The front camera looks for faces only while your phone is unlocked. Nothing ever leaves the device.",
        Icons.Rounded.CameraFront, Mood.Idle, "Allow camera",
    ),
    Step(
        "Step 2 · Notch",
        "Caught in\nthe act.",
        "Allow display over other apps so the “Peeping · 2” alert can appear right under your camera, in any app.",
        Icons.Rounded.Layers, Mood.Peek, "Allow overlay",
    ),
    Step(
        "Step 3 · Notifications",
        "Quietly\non duty.",
        "A small persistent notification keeps the guard running and shows today's peek count.",
        Icons.Rounded.Notifications, Notify, "Allow notifications",
    ),
    Step(
        "Step 4 · Background",
        "Never\nnods off.",
        "Let Peek-a-Boo run unrestricted so Android doesn't put the guard to sleep.",
        Icons.Rounded.BatteryChargingFull, Mood.Safe, "Allow background",
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
    Box(Modifier.fillMaxSize()) {
        Aurora(step.mood)
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            ProgressSegments(steps.size, index, step.mood)
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
                        if (s.icon == null) EyeOrb(OrbMode.Guarding, size = 280.dp) else GlyphHalo(s.icon, s.mood)
                    }
                    Spacer(Modifier.height(36.dp))
                    Text(s.eyebrow.uppercase(), style = Eyebrow, color = s.mood.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        s.title,
                        style = if (i == 0) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall,
                        color = Night.Text,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(s.body, style = MaterialTheme.typography.bodyLarge, color = Night.TextDim)
                }
            }
            GlowButton(step.cta, step.mood.brush, glow = step.mood.primary, onClick = ::act)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                if (index != 0) {
                    Text(
                        "Not now",
                        style = MaterialTheme.typography.labelLarge,
                        color = Night.TextDim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clip(CircleShape).bouncyClick { next() }.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Big glass tile with the step's glyph, orbited by a dashed ring and a travelling spark. */
@Composable
internal fun GlyphHalo(icon: ImageVector, mood: Mood) {
    val t = rememberInfiniteTransition(label = "halo")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")
    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(Brush.radialGradient(listOf(mood.primary.copy(alpha = 0.4f), Color.Transparent), center, r), r)
            drawCircle(mood.primary.copy(alpha = 0.25f), r * 0.78f, style = Stroke(1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 8.dp.toPx()))))
            val a = Math.toRadians(spin.toDouble())
            val p = androidx.compose.ui.geometry.Offset(center.x + r * 0.78f * kotlin.math.cos(a).toFloat(), center.y + r * 0.78f * kotlin.math.sin(a).toFloat())
            drawCircle(mood.primary.copy(alpha = 0.35f), 9.dp.toPx(), p)
            drawCircle(mood.primary, 4.dp.toPx(), p)
        }
        Box(
            Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Brush.linearGradient(listOf(mood.primary.copy(alpha = 0.3f), mood.secondary.copy(alpha = 0.12f))))
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), mood.primary.copy(alpha = 0.15f))), RoundedCornerShape(40.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(58.dp))
        }
    }
}

@Composable
internal fun ProgressSegments(count: Int, selected: Int, mood: Mood) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val fill by animateFloatAsState(if (i <= selected) 1f else 0f, spring(dampingRatio = 0.8f), label = "seg")
            Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))) {
                Box(Modifier.fillMaxWidth(fill).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(mood.primary, mood.secondary))))
            }
        }
    }
}
