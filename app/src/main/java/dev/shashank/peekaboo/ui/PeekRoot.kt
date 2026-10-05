package dev.shashank.peekaboo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.shashank.peekaboo.service.GuardState
import dev.shashank.peekaboo.ui.components.LocalMood
import dev.shashank.peekaboo.ui.components.OrbMode
import dev.shashank.peekaboo.ui.components.mood
import dev.shashank.peekaboo.ui.screens.FaceScreen
import dev.shashank.peekaboo.ui.screens.HomeScreen
import dev.shashank.peekaboo.ui.screens.InsightsScreen
import dev.shashank.peekaboo.ui.screens.OnboardingScreen
import dev.shashank.peekaboo.ui.screens.SettingsScreen
import dev.shashank.peekaboo.ui.theme.Ink

/** Re-checks runtime permissions every time the app comes back to the foreground. */
@Composable
fun rememberPermissions(): State<PermissionSnapshot> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(PermissionSnapshot.of(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state.value = PermissionSnapshot.of(ctx) }
    return state
}

internal enum class Tab(val label: String, val icon: ImageVector) {
    Guard("Watch", Icons.Rounded.RemoveRedEye),
    Report("Activity", Icons.Rounded.Insights),
    Face("You", Icons.Rounded.Face),
    Settings("Tune", Icons.Rounded.Tune),
}

val TabBarHeight = 64.dp

/** Live guard state folded into the four visual modes the whole UI is built around. */
@Composable
fun rememberOrbMode(guardEnabled: Boolean): OrbMode {
    val running by GuardState.running.collectAsStateWithLifecycle()
    val scanning by GuardState.scanning.collectAsStateWithLifecycle()
    val peek by GuardState.peekActive.collectAsStateWithLifecycle()
    return when {
        !guardEnabled -> OrbMode.Off
        peek -> OrbMode.Alert
        running && scanning -> OrbMode.Guarding
        else -> OrbMode.Idle
    }
}

@Composable
fun PeekRoot(vm: MainViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings
    Box(Modifier.fillMaxSize().background(Ink.Bg)) {
        if (s != null) {
            AnimatedContent(
                targetState = s.onboardingDone,
                transitionSpec = { (fadeIn(tween(500)) + scaleIn(initialScale = 0.94f)) togetherWith fadeOut(tween(250)) },
                label = "root",
            ) { done ->
                if (done) MainTabs(vm, s.guardEnabled) else OnboardingScreen(vm)
            }
        }
    }
}

@Composable
private fun MainTabs(vm: MainViewModel, guardEnabled: Boolean) {
    var tab by rememberSaveable { mutableStateOf(Tab.Guard) }
    val haze = remember { HazeState() }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(bottom = TabBarHeight + navBottom + 40.dp)
    val mode = rememberOrbMode(guardEnabled)
    val mood = mode.mood()

    CompositionLocalProvider(LocalMood provides mood) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Ink.Bg).haze(haze)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (fadeIn(tween(260)) + slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { 40 * dir }) togetherWith
                            fadeOut(tween(120))
                    },
                    label = "tabs",
                ) { t ->
                    when (t) {
                        Tab.Guard -> HomeScreen(vm, contentPadding, openReport = { tab = Tab.Report }, openFace = { tab = Tab.Face })
                        Tab.Report -> InsightsScreen(vm, contentPadding)
                        Tab.Face -> FaceScreen(vm, contentPadding)
                        Tab.Settings -> SettingsScreen(vm, contentPadding)
                    }
                }
            }

            FloatingTabBar(
                selected = tab,
                haze = haze,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBottom + 16.dp, start = 20.dp, end = 20.dp),
            ) { tab = it }
        }
    }
}

@Composable
internal fun FloatingTabBar(selected: Tab, haze: HazeState, modifier: Modifier, onSelect: (Tab) -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .height(TabBarHeight)
            .shadow(16.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CircleShape)
            .hazeChild(
                haze,
                style = HazeStyle(
                    backgroundColor = Ink.Bg,
                    tint = HazeTint(Ink.Surface.copy(alpha = 0.88f)),
                    blurRadius = 24.dp,
                    noiseFactor = 0f,
                ),
            )
            .border(1.dp, Ink.Line, CircleShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { t ->
            TabItem(t, selected = t == selected, modifier = Modifier.weight(if (t == selected) 1.7f else 1f)) { onSelect(t) }
        }
    }
}

@Composable
private fun TabItem(tab: Tab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val bg by animateColorAsState(if (selected) Ink.Text else Ink.Text.copy(alpha = 0f), tween(200), label = "tabBg")
    val fg by animateColorAsState(if (selected) Ink.Bg else Ink.TextMuted, tween(200), label = "tabFg")
    Row(
        modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .background(bg)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tab.icon, tab.label, tint = fg, modifier = Modifier.size(21.dp))
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkHorizontally(tween(150)) + fadeOut(tween(100)),
        ) {
            Text(
                tab.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = fg,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
