package dev.shashank.peekaboo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.shashank.peekaboo.ui.theme.Ios
import kotlin.math.max

enum class OrbMode { Off, Idle, Guarding, Alert }

/** The hero radar: breathing rings and a sweeping beam while guarding, red pulse during a peek. */
@Composable
fun RadarOrb(mode: OrbMode, modifier: Modifier = Modifier, size: Dp = 240.dp) {
    val color by animateColorAsState(
        when (mode) {
            OrbMode.Off -> Ios.Gray
            OrbMode.Idle -> Ios.Indigo
            OrbMode.Guarding -> Ios.Green
            OrbMode.Alert -> Ios.Red
        },
        tween(500),
        label = "orb",
    )
    val t = rememberInfiniteTransition(label = "radar")
    val sweep by t.animateFloat(0f, 360f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "sweep")
    val ripple by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(if (mode == OrbMode.Alert) 900 else 2400, easing = LinearEasing)),
        label = "ripple",
    )
    val breathe by t.animateFloat(
        0.94f, 1.0f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val live = mode == OrbMode.Guarding || mode == OrbMode.Alert

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2f
            val c = center
            // glow
            drawCircle(
                Brush.radialGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent), c, r),
                r, c,
            )
            if (live) {
                // expanding ripples
                for (i in 0 until 3) {
                    val p = (ripple + i / 3f) % 1f
                    drawCircle(
                        color.copy(alpha = (1f - p) * 0.5f),
                        radius = r * (0.42f + p * 0.58f),
                        center = c,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
                // radar sweep
                rotate(sweep, c) {
                    drawArc(
                        Brush.sweepGradient(
                            0f to Color.Transparent, 0.82f to Color.Transparent, 1f to color.copy(alpha = 0.45f),
                            center = c,
                        ),
                        startAngle = 0f, sweepAngle = 360f, useCenter = true,
                        topLeft = Offset(c.x - r * 0.86f, c.y - r * 0.86f),
                        size = Size(r * 1.72f, r * 1.72f),
                    )
                }
            }
            // static guide rings
            listOf(0.86f, 0.62f).forEach { f ->
                drawCircle(
                    color.copy(alpha = 0.22f), r * f, c,
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))),
                )
            }
        }
        Box(
            Modifier
                .size(size * 0.38f)
                .scale(if (live) breathe else 1f),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(size * 0.38f)) {
                drawCircle(Brush.linearGradient(listOf(color, color.copy(alpha = 0.55f))))
                drawCircle(Color.White.copy(alpha = 0.18f), style = Stroke(1.dp.toPx()))
            }
            Icon(
                if (mode == OrbMode.Off) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.17f),
            )
        }
    }
}

/** Apple Health style bar chart with rounded bars that grow in. */
@Composable
fun BarChart(
    values: List<Int>,
    labels: List<String>,
    highlight: Int?,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 170.dp,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        progress.snapTo(0f)
        progress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
    }
    val maxV = max(1, values.maxOrNull() ?: 0)
    val niceMax = when {
        maxV <= 4 -> 4
        maxV <= 10 -> 10
        else -> ((maxV + 4) / 5) * 5
    }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Canvas(Modifier.weight(1f).height(height)) {
                val n = values.size.coerceAtLeast(1)
                val slot = size.width / n
                val barW = (slot * 0.62f).coerceAtMost(22.dp.toPx())
                // grid
                for (g in 0..2) {
                    val y = size.height * g / 2f
                    drawLine(
                        Ios.Separator, Offset(0f, y), Offset(size.width, y), 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)),
                    )
                }
                values.forEachIndexed { i, v ->
                    val h = (v.toFloat() / niceMax) * size.height * progress.value
                    val x = slot * i + (slot - barW) / 2f
                    val isHi = highlight == i
                    val c = if (highlight == null || isHi) color else color.copy(alpha = 0.45f)
                    // track
                    drawRoundRect(
                        Color.White.copy(alpha = 0.04f),
                        Offset(x, 0f), Size(barW, size.height),
                        CornerRadius(barW / 2f),
                    )
                    if (v > 0) {
                        val hh = max(h, barW * 0.6f)
                        drawRoundRect(
                            Brush.verticalGradient(listOf(c, c.copy(alpha = 0.7f)), startY = size.height - hh, endY = size.height),
                            Offset(x, size.height - hh), Size(barW, hh),
                            CornerRadius(barW / 2f),
                        )
                    }
                }
            }
            Column(Modifier.height(height), verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Text(" $niceMax", style = MaterialTheme.typography.labelSmall, color = Ios.Secondary)
                Text(" ${niceMax / 2}", style = MaterialTheme.typography.labelSmall, color = Ios.Secondary)
                Text(" 0", style = MaterialTheme.typography.labelSmall, color = Ios.Secondary)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Row(Modifier.weight(1f)) {
                labels.forEach {
                    Text(
                        it,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = Ios.Secondary,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.size(width = 22.dp, height = 1.dp))
        }
    }
}

/** iOS Face ID style tick ring that fills as enrollment samples are captured. */
@Composable
fun TickRing(progress: Float, modifier: Modifier = Modifier, ticks: Int = 60, color: Color = Ios.Green) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val inner = r * 0.90f
        val filled = (progress.coerceIn(0f, 1f) * ticks).toInt()
        for (i in 0 until ticks) {
            val a = Math.toRadians((i * 360.0 / ticks) - 90.0)
            val cos = kotlin.math.cos(a).toFloat()
            val sin = kotlin.math.sin(a).toFloat()
            val on = i < filled
            drawLine(
                if (on) color else Color.White.copy(alpha = 0.22f),
                Offset(center.x + inner * cos, center.y + inner * sin),
                Offset(center.x + (if (on) r else r * 0.97f) * cos, center.y + (if (on) r else r * 0.97f) * sin),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
