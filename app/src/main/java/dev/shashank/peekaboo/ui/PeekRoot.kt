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
import dev.shashank.peekaboo.ui.components.Aurora
import dev.shashank.peekaboo.ui.components.LocalMood
import dev.shashank.peekaboo.ui.components.OrbMode
import dev.shashank.peekaboo.ui.components.mood
import dev.shashank.peekaboo.ui.screens.FaceScreen
import dev.shashank.peekaboo.ui.screens.HomeScreen
import dev.shashank.peekaboo.ui.screens.InsightsScreen
import dev.shashank.peekaboo.ui.screens.OnboardingScreen
import dev.shashank.peekaboo.ui.screens.SettingsScreen
import dev.shashank.peekaboo.ui.theme.Mood
import dev.shashank.peekaboo.ui.theme.Night

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
    Box(Modifier.fillMaxSize().background(Night.Void)) {
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
            Box(Modifier.fillMaxSize().haze(haze)) {
                // The guard tab gets the full aurora; other tabs keep a calmer wash of the same mood.
                Aurora(mood, intensity = if (tab == Tab.Guard) 1f else 0.55f)
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
                accent = mood,
                haze = haze,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBottom + 14.dp, start = 20.dp, end = 20.dp),
            ) { tab = it }
        }
    }
}

@Composable
internal fun FloatingTabBar(selected: Tab, accent: Mood, haze: HazeState, modifier: Modifier, onSelect: (Tab) -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .height(TabBarHeight)
            .shadow(24.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(CircleShape)
            .hazeChild(
                haze,
                style = HazeStyle(
                    backgroundColor = Night.Void,
                    tint = HazeTint(Color(0xFF14141E).copy(alpha = 0.62f)),
                    blurRadius = 30.dp,
                    noiseFactor = 0.05f,
                ),
            )
            .border(1.dp, Brush.verticalGradient(listOf(Night.StrokeBright, Night.Hairline)), CircleShape)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { t ->
            TabItem(t, selected = t == selected, accent = accent, modifier = Modifier.weight(if (t == selected) 1.7f else 1f)) { onSelect(t) }
        }
    }
}

@Composable
private fun TabItem(tab: Tab, selected: Boolean, accent: Mood, modifier: Modifier, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val iconColor by animateColorAsState(if (selected) Night.Void else Night.TextDim, label = "tabIcon")
    Box(
        modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = selected,
            enter = fadeIn(tween(200)) + scaleIn(spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.6f),
            exit = fadeOut(tween(120)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(Brush.linearGradient(listOf(accent.primary, accent.secondary))))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(tab.icon, tab.label, tint = iconColor, modifier = Modifier.size(22.dp))
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkHorizontally(tween(150)) + fadeOut(tween(100)),
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Night.Void,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}
