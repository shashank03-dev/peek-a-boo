package dev.shashank.peekaboo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.Mood
import dev.shashank.peekaboo.ui.theme.Night
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
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
    OrbMode.Alert -> 1.12f
}

/** The current guard mood, so the background and accents across every tab follow the guard. */
val LocalMood = compositionLocalOf { Mood.Safe }

@Composable
private fun animatedMood(mood: Mood): Mood {
    val a by animateColorAsState(mood.primary, tween(900), label = "m1")
    val b by animateColorAsState(mood.secondary, tween(900), label = "m2")
    val c by animateColorAsState(mood.tertiary, tween(900), label = "m3")
    return Mood(a, b, c)
}

/**
 * Living background: three slow-drifting light blobs tinted by the mood, a faint dot grid like a
 * scanner screen, and a vignette that keeps text readable. Pure radial gradients, so it looks the
 * same on every Android version without needing RenderEffect blur.
 */
@Composable
fun Aurora(mood: Mood, modifier: Modifier = Modifier, intensity: Float = 1f) {
    val m = animatedMood(mood)
    val t = rememberInfiniteTransition(label = "aurora")
    val phase by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(26_000, easing = LinearEasing)), label = "phase")
    val strength by animateFloatAsState(intensity, tween(800), label = "strength")

    Canvas(modifier.fillMaxSize()) {
        drawRect(Night.Void)
        val w = size.width
        val h = size.height
        fun blob(color: Color, cx: Float, cy: Float, r: Float, alpha: Float) {
            drawCircle(
                Brush.radialGradient(
                    0f to color.copy(alpha = alpha * strength),
                    0.45f to color.copy(alpha = alpha * 0.45f * strength),
                    1f to Color.Transparent,
                    center = Offset(cx, cy), radius = r,
                ),
                radius = r, center = Offset(cx, cy),
            )
        }
        blob(m.primary, w * (0.25f + 0.18f * sin(phase)), h * (0.10f + 0.06f * cos(phase * 2)), w * 0.95f, 0.30f)
        blob(m.secondary, w * (0.85f + 0.12f * cos(phase)), h * (0.26f + 0.08f * sin(phase)), w * 0.85f, 0.24f)
        blob(m.tertiary, w * (0.45f + 0.25f * sin(phase + 2f)), h * (0.62f + 0.10f * cos(phase + 1f)), w * 1.1f, 0.18f)

        drawDotGrid(spacing = 22.dp.toPx(), fadeTo = h * 0.55f)

        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.45f to Night.Void.copy(alpha = 0.35f),
                1f to Night.Void.copy(alpha = 0.92f),
            ),
        )
    }
}

private fun DrawScope.drawDotGrid(spacing: Float, fadeTo: Float) {
    val dot = 0.9.dp.toPx()
    var y = spacing / 2f
    while (y < fadeTo) {
        val a = 0.075f * (1f - y / fadeTo)
        var x = spacing / 2f
        while (x < size.width) {
            drawCircle(Color.White.copy(alpha = a), dot, Offset(x, y))
            x += spacing
        }
        y += spacing
    }
}

/**
 * The hero: a glass marble with a living eye inside. It glances around while guarding, drifts
 * sleepily while standing by, shuts when the guard is off and stares wide open, ringed by alarm
 * ripples, when someone peeks.
 */
@Composable
fun EyeOrb(mode: OrbMode, modifier: Modifier = Modifier, size: Dp = 260.dp) {
    val m = animatedMood(mode.mood())
    val t = rememberInfiniteTransition(label = "eye")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "spin")
    val ripple by t.animateFloat(0f, 1f, infiniteRepeatable(tween(if (mode == OrbMode.Alert) 1100 else 3200, easing = LinearEasing)), label = "ripple")
    val breathe by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")

    val open = remember { Animatable(mode.eyeOpenness()) }
    val lookX = remember { Animatable(0f) }
    val lookY = remember { Animatable(0f) }
    val pupil by animateFloatAsState(
        when (mode) { OrbMode.Alert -> 0.62f; OrbMode.Guarding -> 0.42f; else -> 0.36f },
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow), label = "pupil",
    )

    // Eyelid: the base openness for the mode, plus the occasional blink.
    LaunchedEffect(mode) {
        val target = mode.eyeOpenness()
        open.animateTo(target, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
        if (mode == OrbMode.Off) return@LaunchedEffect
        while (true) {
            delay(Random.nextLong(2600, 5200))
            open.animateTo(0f, tween(90))
            open.animateTo(target, tween(170, easing = FastOutSlowInEasing))
        }
    }
    // Gaze: quick saccades while guarding, a slow drift while idle, dead centre otherwise.
    LaunchedEffect(mode) {
        when (mode) {
            OrbMode.Guarding, OrbMode.Idle -> {
                val reach = if (mode == OrbMode.Guarding) 1f else 0.5f
                val motion = if (mode == OrbMode.Guarding) spring<Float>(dampingRatio = 0.7f, stiffness = 900f) else tween(1400, easing = FastOutSlowInEasing)
                while (true) {
                    val x = (Random.nextFloat() * 2f - 1f) * reach
                    val y = (Random.nextFloat() * 2f - 1f) * reach * 0.45f
                    launch { lookY.animateTo(y, motion) }
                    lookX.animateTo(x, motion)
                    delay(if (mode == OrbMode.Guarding) Random.nextLong(700, 2200) else Random.nextLong(1800, 3600))
                }
            }
            else -> {
                launch { lookY.animateTo(0f, spring(dampingRatio = 0.6f)) }
                lookX.animateTo(0f, spring(dampingRatio = 0.6f))
            }
        }
    }

    val live = mode == OrbMode.Guarding || mode == OrbMode.Alert

    Canvas(modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = center
        val core = r * 0.5f * (1f + if (live) breathe * 0.025f else 0f)

        // Halo.
        drawCircle(
            Brush.radialGradient(
                0f to m.primary.copy(alpha = 0.42f),
                0.5f to m.secondary.copy(alpha = 0.12f),
                1f to Color.Transparent,
                center = c, radius = r,
            ),
            r, c,
        )

        // Ripples.
        if (live) {
            val count = if (mode == OrbMode.Alert) 3 else 2
            for (i in 0 until count) {
                val p = (ripple + i / count.toFloat()) % 1f
                drawCircle(
                    m.primary.copy(alpha = (1f - p) * if (mode == OrbMode.Alert) 0.7f else 0.35f),
                    radius = core + (r - core) * p,
                    center = c,
                    style = Stroke(width = (if (mode == OrbMode.Alert) 2.dp else 1.2.dp).toPx()),
                )
            }
        }

        // Orbit rings: two scanner arcs turning against each other.
        val ring1 = core * 1.32f
        val ring2 = core * 1.58f
        rotate(spin, c) {
            drawArc(
                Brush.sweepGradient(listOf(Color.Transparent, m.primary.copy(alpha = 0.9f), Color.Transparent), c),
                startAngle = 0f, sweepAngle = 300f, useCenter = false,
                topLeft = Offset(c.x - ring1, c.y - ring1), size = Size(ring1 * 2, ring1 * 2),
                style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        rotate(-spin * 0.6f, c) {
            drawCircle(
                m.secondary.copy(alpha = 0.32f), ring2, c,
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 9.dp.toPx()))),
            )
            drawCircle(m.primary, 3.dp.toPx(), Offset(c.x + ring2, c.y))
        }

        // Glass marble.
        drawCircle(
            Brush.radialGradient(
                listOf(lerp(m.primary, Color.White, 0.25f), m.secondary, lerp(m.tertiary, Night.Void, 0.55f)),
                center = Offset(c.x - core * 0.35f, c.y - core * 0.45f), radius = core * 1.8f,
            ),
            core, c,
        )
        drawCircle(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), startY = c.y - core, endY = c.y),
            core, c, style = Stroke(1.2.dp.toPx()),
        )

        // The eye.
        val ew = core * 0.74f
        val eh = core * 0.62f * open.value.coerceAtLeast(0.02f)
        val lid = Path().apply {
            moveTo(c.x - ew, c.y)
            cubicTo(c.x - ew * 0.5f, c.y - eh, c.x + ew * 0.5f, c.y - eh, c.x + ew, c.y)
            cubicTo(c.x + ew * 0.5f, c.y + eh, c.x - ew * 0.5f, c.y + eh, c.x - ew, c.y)
            close()
        }
        if (open.value < 0.18f) {
            // Asleep: a closed lid curving down, with three lashes.
            val sleep = Path().apply {
                moveTo(c.x - ew * 0.8f, c.y - core * 0.04f)
                quadraticBezierTo(c.x, c.y + core * 0.3f, c.x + ew * 0.8f, c.y - core * 0.04f)
            }
            val ink = Night.Void.copy(alpha = 0.85f)
            drawPath(sleep, ink, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            for (k in -1..1) {
                val bx = c.x + k * ew * 0.42f
                val by = c.y + core * (if (k == 0) 0.13f else 0.08f)
                drawLine(ink, Offset(bx, by), Offset(bx + k * ew * 0.1f, by + core * 0.13f), 2.4.dp.toPx(), cap = StrokeCap.Round)
            }
        } else {
            drawPath(lid, Night.Void.copy(alpha = 0.88f))
            clipPath(lid) {
                val irisR = core * 0.36f
                val ic = Offset(c.x + lookX.value * ew * 0.42f, c.y + lookY.value * core * 0.3f)
                drawCircle(
                    Brush.radialGradient(listOf(Color.White, m.primary, m.secondary), center = ic, radius = irisR * 1.1f),
                    irisR, ic,
                )
                drawCircle(Night.Void, irisR * pupil, ic)
                drawCircle(Color.White.copy(alpha = 0.9f), irisR * 0.14f, Offset(ic.x + irisR * 0.32f, ic.y - irisR * 0.34f))
            }
            drawPath(lid, Color.White.copy(alpha = 0.22f), style = Stroke(1.dp.toPx()))
        }

        // Specular highlight on the glass.
        drawOval(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), startY = c.y - core * 0.92f, endY = c.y - core * 0.35f),
            topLeft = Offset(c.x - core * 0.55f, c.y - core * 0.9f),
            size = Size(core * 1.1f, core * 0.55f),
        )
    }
}

/** Rounded bars with a glowing highlight bar, for the hourly and weekly charts. */
@Composable
fun BarChart(
    values: List<Int>,
    labels: List<String>,
    highlight: Int?,
    brushColors: List<Color>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
) {
    val inspection = LocalInspectionMode.current
    val progress = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(values) {
        if (inspection) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessVeryLow))
    }
    val maxV = max(1, values.maxOrNull() ?: 0)
    val niceMax = when {
        maxV <= 4 -> 4
        maxV <= 10 -> 10
        else -> ((maxV + 4) / 5) * 5
    }
    val top = brushColors.first()
    val bottom = brushColors.last()
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Canvas(Modifier.weight(1f).height(height)) {
                val n = values.size.coerceAtLeast(1)
                val slot = size.width / n
                val barW = (slot * 0.56f).coerceAtMost(26.dp.toPx())
                for (g in 0..2) {
                    val y = size.height * g / 2f
                    drawLine(
                        Night.Hairline, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx())),
                    )
                }
                values.forEachIndexed { i, v ->
                    val x = slot * i + (slot - barW) / 2f
                    val isHi = highlight == i
                    drawRoundRect(
                        Color.White.copy(alpha = 0.035f),
                        Offset(x, 0f), Size(barW, size.height), CornerRadius(barW / 2f),
                    )
                    if (v > 0) {
                        val h = max((v.toFloat() / niceMax) * size.height * progress.value, barW)
                        val y = size.height - h
                        val dim = highlight != null && !isHi
                        if (isHi) {
                            drawRoundRect(
                                Brush.radialGradient(listOf(top.copy(alpha = 0.45f), Color.Transparent), center = Offset(x + barW / 2, y + barW / 2), radius = barW * 2.2f),
                                Offset(x - barW, y - barW), Size(barW * 3, h + barW * 2), CornerRadius(barW * 1.5f),
                            )
                        }
                        drawRoundRect(
                            Brush.verticalGradient(
                                listOf(top.copy(alpha = if (dim) 0.4f else 1f), bottom.copy(alpha = if (dim) 0.22f else 0.85f)),
                                startY = y, endY = size.height,
                            ),
                            Offset(x, y), Size(barW, h), CornerRadius(barW / 2f),
                        )
                    }
                }
            }
            Column(Modifier.height(height), verticalArrangement = Arrangement.SpaceBetween) {
                listOf(niceMax, niceMax / 2, 0).forEach {
                    Text("  $it", style = Eyebrow, color = Night.TextFaint)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Row(Modifier.weight(1f)) {
                // Fewer labels than bars: each label covers an equal run of bars, starting at its first bar.
                val span = values.size / labels.size.coerceAtLeast(1)
                labels.forEachIndexed { i, label ->
                    val hi = highlight != null && highlight / span.coerceAtLeast(1) == i && span == 1
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = if (span > 1) TextAlign.Start else TextAlign.Center,
                        style = Eyebrow,
                        color = if (hi) Night.Text else Night.TextFaint,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            Spacer(Modifier.size(width = 26.dp, height = 1.dp))
        }
    }
}

/** Smooth area sparkline used inside the bento tiles. */
@Composable
fun Sparkline(values: List<Int>, color: Color, modifier: Modifier = Modifier) {
    val inspection = LocalInspectionMode.current
    val progress = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(values) {
        if (inspection) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
    }
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val maxV = max(1, values.max()).toFloat()
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)
        val pts = values.mapIndexed { i, v ->
            Offset(i * stepX, size.height - (v / maxV) * size.height * 0.86f * progress.value - 2.dp.toPx())
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
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        val last = pts.last()
        drawCircle(color.copy(alpha = 0.3f), 6.dp.toPx(), last)
        drawCircle(color, 3.dp.toPx(), last)
    }
}

/** Face ID style ring of ticks that fill with a gradient sweep as samples are captured. */
@Composable
fun TickRing(progress: Float, modifier: Modifier = Modifier, ticks: Int = 72, colors: List<Color> = listOf(Night.Mint, Night.Teal)) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val inner = r * 0.88f
        val filled = (progress.coerceIn(0f, 1f) * ticks).toInt()
        for (i in 0 until ticks) {
            val a = Math.toRadians((i * 360.0 / ticks) - 90.0)
            val cs = cos(a).toFloat()
            val sn = sin(a).toFloat()
            val on = i < filled
            val col = if (on) lerp(colors.first(), colors.last(), i / ticks.toFloat()) else Color.White.copy(alpha = 0.14f)
            val outer = if (on) r else r * 0.96f
            drawLine(
                col,
                Offset(center.x + inner * cs, center.y + inner * sn),
                Offset(center.x + outer * cs, center.y + outer * sn),
                strokeWidth = 2.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Small animated live dot with a soft pulse, for status chips. */
@Composable
fun PulseDot(color: Color, modifier: Modifier = Modifier, live: Boolean = true) {
    val t = rememberInfiniteTransition(label = "dot")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "p")
    Box(modifier.size(14.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            if (live) drawCircle(color.copy(alpha = (1f - p) * 0.6f), radius = size.minDimension / 2f * (0.4f + p * 0.6f))
            drawCircle(color, radius = 3.5.dp.toPx())
        }
    }
}
