package dev.shashank.peekaboo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.Ink
import dev.shashank.peekaboo.ui.theme.Mood
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

enum class OrbMode { Off, Idle, Guarding, Alert }

fun OrbMode.mood(): Mood = when (this) {
    OrbMode.Off -> Mood.Off
    OrbMode.Idle -> Mood.Idle
    OrbMode.Guarding -> Mood.Safe
    OrbMode.Alert -> Mood.Peek
}

private fun OrbMode.eyeOpenness() = when (this) {
    OrbMode.Off -> 0.06f
    OrbMode.Idle -> 0.55f
    OrbMode.Guarding -> 0.9f
    OrbMode.Alert -> 1.1f
}

/** The current guard mood, so accents across every tab follow the guard. */
val LocalMood = compositionLocalOf { Mood.Safe }

/**
 * The hero mark: a flat, graphic eye on a disc inside two hairline rings. The iris carries the
 * state colour. It glances around while guarding, drifts on standby, sleeps when off and stares
 * wide open, with expanding red rings, during a peek.
 */
@Composable
fun EyeOrb(mode: OrbMode, modifier: Modifier = Modifier, size: Dp = 260.dp) {
    val iris by animateColorAsState(mode.mood().color, tween(500), label = "iris")
    val t = rememberInfiniteTransition(label = "eye")
    val orbit by t.animateFloat(0f, 360f, infiniteRepeatable(tween(12_000, easing = LinearEasing)), label = "orbit")
    val ripple by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing)), label = "ripple")

    val open = remember { Animatable(mode.eyeOpenness()) }
    val lookX = remember { Animatable(0f) }
    val lookY = remember { Animatable(0f) }
    val pupil by animateFloatAsState(
        when (mode) { OrbMode.Alert -> 0.6f; OrbMode.Guarding -> 0.42f; else -> 0.36f },
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow), label = "pupil",
    )

    LaunchedEffect(mode) {
        val target = mode.eyeOpenness()
        open.animateTo(target, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow))
        if (mode == OrbMode.Off) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(2800, 5600))
            open.animateTo(0f, tween(90))
            open.animateTo(target, tween(170, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(mode) {
        when (mode) {
            OrbMode.Guarding, OrbMode.Idle -> {
                val reach = if (mode == OrbMode.Guarding) 1f else 0.5f
                val motion = if (mode == OrbMode.Guarding) spring<Float>(dampingRatio = 0.75f, stiffness = 900f) else tween(1400, easing = FastOutSlowInEasing)
                while (true) {
                    val x = (Random.nextFloat() * 2f - 1f) * reach
                    val y = (Random.nextFloat() * 2f - 1f) * reach * 0.45f
                    launch { lookY.animateTo(y, motion) }
                    lookX.animateTo(x, motion)
                    delay(if (mode == OrbMode.Guarding) Random.nextLong(800, 2400) else Random.nextLong(1800, 3600))
                }
            }
            else -> {
                launch { lookY.animateTo(0f, spring(dampingRatio = 0.6f)) }
                lookX.animateTo(0f, spring(dampingRatio = 0.6f))
            }
        }
    }

    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = center
        val core = r * 0.56f
        val hair = 1.dp.toPx()

        // Peek: flat red rings travelling outward.
        if (mode == OrbMode.Alert) {
            for (i in 0 until 2) {
                val p = (ripple + i / 2f) % 1f
                drawCircle(Ink.Alert.copy(alpha = (1f - p) * 0.6f), core + (r - core) * p, c, style = Stroke(1.5.dp.toPx()))
            }
        }

        // Hairline rings, with a small marker travelling round the outer one while the guard runs.
        drawCircle(Ink.Line, r - hair, c, style = Stroke(hair))
        drawCircle(Ink.Line, core * 1.32f, c, style = Stroke(hair))
        if (mode == OrbMode.Guarding || mode == OrbMode.Alert) {
            val a = Math.toRadians(orbit.toDouble() - 90)
            drawCircle(iris, 3.dp.toPx(), Offset(c.x + (r - hair) * cos(a).toFloat(), c.y + (r - hair) * sin(a).toFloat()))
        }

        // Disc.
        drawCircle(Ink.Raised, core, c)
        drawCircle(Ink.LineStrong, core, c, style = Stroke(hair))

        val ew = core * 0.72f
        val eh = core * 0.6f * open.value.coerceAtLeast(0.02f)
        if (open.value < 0.18f) {
            // Asleep: closed lid with three lashes.
            val lid = Path().apply {
                moveTo(c.x - ew * 0.78f, c.y - core * 0.04f)
                quadraticBezierTo(c.x, c.y + core * 0.28f, c.x + ew * 0.78f, c.y - core * 0.04f)
            }
            drawPath(lid, Ink.TextMuted, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            for (k in -1..1) {
                val bx = c.x + k * ew * 0.42f
                val by = c.y + core * (if (k == 0) 0.12f else 0.075f)
                drawLine(Ink.TextMuted, Offset(bx, by), Offset(bx + k * ew * 0.1f, by + core * 0.12f), 2.dp.toPx(), cap = StrokeCap.Round)
            }
        } else {
            val almond = Path().apply {
                moveTo(c.x - ew, c.y)
                cubicTo(c.x - ew * 0.5f, c.y - eh, c.x + ew * 0.5f, c.y - eh, c.x + ew, c.y)
                cubicTo(c.x + ew * 0.5f, c.y + eh, c.x - ew * 0.5f, c.y + eh, c.x - ew, c.y)
                close()
            }
            drawPath(almond, Ink.Text)
            clipPath(almond) {
                val irisR = core * 0.34f
                val ic = Offset(c.x + lookX.value * ew * 0.42f, c.y + lookY.value * core * 0.3f)
                drawCircle(iris, irisR, ic)
                drawCircle(Ink.Bg, irisR * pupil, ic)
                drawCircle(Ink.Text, irisR * 0.13f, Offset(ic.x + irisR * 0.3f, ic.y - irisR * 0.32f))
            }
        }
    }
}

/** Flat rounded bars: neutral, with the highlighted bar in [accent]. */
@Composable
fun BarChart(
    values: List<Int>,
    labels: List<String>,
    highlight: Int?,
    accent: Color,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
) {
    val inspection = LocalInspectionMode.current
    val progress = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(values) {
        if (inspection) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessVeryLow))
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
                val barW = (slot * 0.6f).coerceAtMost(24.dp.toPx())
                for (g in 0..2) {
                    val y = (size.height - 0.5f) * g / 2f
                    drawLine(Ink.Line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                values.forEachIndexed { i, v ->
                    if (v <= 0) return@forEachIndexed
                    val x = slot * i + (slot - barW) / 2f
                    val h = max((v.toFloat() / niceMax) * size.height * progress.value, 3.dp.toPx())
                    drawRoundRect(
                        if (i == highlight) accent else Ink.LineStrong,
                        Offset(x, size.height - h), Size(barW, h), CornerRadius(minOf(barW / 2f, 4.dp.toPx())),
                    )
                }
            }
            Column(Modifier.height(height), verticalArrangement = Arrangement.SpaceBetween) {
                listOf(niceMax, niceMax / 2, 0).forEach {
                    Text("  $it", style = Eyebrow, color = Ink.TextFaint)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Row(Modifier.weight(1f)) {
                // Fewer labels than bars: each label covers an equal run of bars, starting at its first bar.
                val span = values.size / labels.size.coerceAtLeast(1)
                labels.forEachIndexed { i, label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = if (span > 1) TextAlign.Start else TextAlign.Center,
                        style = Eyebrow,
                        color = if (span == 1 && i == highlight) Ink.Text else Ink.TextFaint,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            Spacer(Modifier.size(width = 26.dp, height = 1.dp))
        }
    }
}

/** Thin line sparkline used inside the stat tiles. */
@Composable
fun Sparkline(values: List<Int>, color: Color, modifier: Modifier = Modifier) {
    val inspection = LocalInspectionMode.current
    val progress = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(values) {
        if (inspection) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val maxV = max(1, values.max()).toFloat()
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)
        val pts = values.mapIndexed { i, v ->
            Offset(i * stepX, size.height - (v / maxV) * (size.height - 4.dp.toPx()) * progress.value - 2.dp.toPx())
        }
        val line = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val p0 = pts[i - 1]
                val p1 = pts[i]
                val mx = (p0.x + p1.x) / 2f
                cubicTo(mx, p0.y, mx, p1.y, p1.x, p1.y)
            }
        }
        drawPath(line, color, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Face ID style ring of ticks that fill as samples are captured. */
@Composable
fun TickRing(progress: Float, modifier: Modifier = Modifier, ticks: Int = 72, color: Color = Ink.Accent) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val inner = r * 0.88f
        val filled = (progress.coerceIn(0f, 1f) * ticks).toInt()
        for (i in 0 until ticks) {
            val a = Math.toRadians((i * 360.0 / ticks) - 90.0)
            val cs = cos(a).toFloat()
            val sn = sin(a).toFloat()
            val on = i < filled
            val outer = if (on) r else r * 0.96f
            drawLine(
                if (on) color else Ink.LineStrong,
                Offset(center.x + inner * cs, center.y + inner * sn),
                Offset(center.x + outer * cs, center.y + outer * sn),
                strokeWidth = 2.4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Status dot; pulses softly while live. */
@Composable
fun PulseDot(color: Color, modifier: Modifier = Modifier, live: Boolean = true) {
    val t = rememberInfiniteTransition(label = "dot")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "p")
    Box(modifier.size(12.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            if (live) drawCircle(color.copy(alpha = (1f - p) * 0.45f), radius = size.minDimension / 2f * (0.35f + p * 0.65f))
            drawCircle(color, radius = 3.dp.toPx())
        }
    }
}
