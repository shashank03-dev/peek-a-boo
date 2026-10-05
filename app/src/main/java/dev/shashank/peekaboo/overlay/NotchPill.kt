package dev.shashank.peekaboo.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shashank.peekaboo.ui.theme.Ink
import dev.shashank.peekaboo.ui.theme.Inter
import kotlinx.coroutines.delay

/** Dynamic-Island style capsule: "Peeping · 2" with a live, rolling counter. */
@Composable
fun NotchPill(peepers: Int, visible: Boolean, topOffsetPx: Int) {
    val topDp = with(LocalDensity.current) { topOffsetPx.toDp() }
    Box(Modifier.padding(top = topDp + 6.dp, start = 12.dp, end = 12.dp, bottom = 14.dp)) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.4f) +
                fadeIn(tween(120)),
            exit = scaleOut(tween(220), targetScale = 0.5f) + fadeOut(tween(200)),
        ) {
            var expanded by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(140)
                expanded = true
            }
            val pulse = rememberInfiniteTransition(label = "pulse")
            val glow by pulse.animateFloat(
                initialValue = 0.55f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "glow",
            )
            Row(
                modifier = Modifier
                    .shadow(12.dp, RoundedCornerShape(50))
                    .background(Color.Black, RoundedCornerShape(50))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                    .animateContentSize(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow))
                    .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .scale(0.9f + glow * 0.1f)
                        .background(Ink.Alert.copy(alpha = 0.18f * glow), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Visibility, null, tint = Ink.Alert, modifier = Modifier.size(16.dp))
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandHorizontally(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Peeping",
                            style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White),
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            Modifier
                                .background(Ink.Alert, RoundedCornerShape(50))
                                .padding(horizontal = 9.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(
                                targetState = peepers.coerceAtLeast(1),
                                transitionSpec = {
                                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                                },
                                label = "count",
                            ) { n ->
                                Text(
                                    "$n",
                                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White),
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .padding(end = 4.dp)
                                .size(7.dp)
                                .alpha(glow)
                                .background(Ink.Alert, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
