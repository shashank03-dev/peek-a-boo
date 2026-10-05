package dev.shashank.peekaboo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import dev.shashank.peekaboo.ui.screens.FaceScreen
import dev.shashank.peekaboo.ui.screens.HomeScreen
import dev.shashank.peekaboo.ui.screens.InsightsScreen
import dev.shashank.peekaboo.ui.screens.OnboardingScreen
import dev.shashank.peekaboo.ui.screens.SettingsScreen
import dev.shashank.peekaboo.ui.theme.Ios

/** Re-checks runtime permissions every time the app comes back to the foreground. */
@Composable
fun rememberPermissions(): State<PermissionSnapshot> {
    val ctx = LocalContext.current
    val state = remember { mutableStateOf(PermissionSnapshot.of(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state.value = PermissionSnapshot.of(ctx) }
    return state
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Guard("Guard", Icons.Rounded.Shield),
    Report("Report", Icons.Rounded.BarChart),
    Face("Face ID", Icons.Rounded.Face),
    Settings("Settings", Icons.Rounded.Settings),
}

val TabBarHeight = 64.dp

@Composable
fun PeekRoot(vm: MainViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings
    Box(Modifier.fillMaxSize().background(Ios.Background)) {
        if (s != null) {
            AnimatedContent(
                targetState = s.onboardingDone,
                transitionSpec = { (fadeIn(tween(400)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(250)) },
                label = "root",
            ) { done ->
                if (done) MainTabs(vm) else OnboardingScreen(vm)
            }
        }
    }
}

@Composable
private fun MainTabs(vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Guard) }
    val haze = remember { HazeState() }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(bottom = TabBarHeight + navBottom + 24.dp)

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().haze(haze)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(spring(stiffness = Spring.StiffnessMediumLow), initialScale = 0.985f)) togetherWith
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

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .hazeChild(
                    haze,
                    style = HazeStyle(
                        backgroundColor = Ios.Background,
                        tint = HazeTint(Color(0xFF121214).copy(alpha = 0.72f)),
                        blurRadius = 28.dp,
                        noiseFactor = 0.04f,
                    ),
                )
        ) {
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(Ios.Separator))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(TabBarHeight)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tab.entries.forEach { t ->
                    TabItem(t, selected = t == tab, modifier = Modifier.weight(1f)) { tab = t }
                }
            }
            Spacer(Modifier.height(navBottom))
        }
    }
}

@Composable
private fun TabItem(tab: Tab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val color = if (selected) Ios.Blue else Ios.Gray
    Column(
        modifier
            .fillMaxHeight()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) Ios.Blue.copy(alpha = 0.14f) else Color.Transparent)
                .then(if (selected) Modifier.border(0.5.dp, Ios.Blue.copy(alpha = 0.2f), RoundedCornerShape(50)) else Modifier)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Icon(tab.icon, tab.label, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
