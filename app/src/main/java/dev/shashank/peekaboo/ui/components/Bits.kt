package dev.shashank.peekaboo.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.shashank.peekaboo.ui.theme.Ink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/*
 * Native Compose ports of a handful of React Bits components (reactbits.dev), tuned to feel like
 * iOS: critically-damped springs for UI movement, a touch of bounce only where something "lands".
 */

/** Shared spring curves, roughly matching UIKit's default spring animations. */
object Springs {
    /** Default UI movement: no overshoot, ~0.35s. */
    fun <T> ui() = spring<T>(dampingRatio = 0.9f, stiffness = 380f)
    /** Quick, responsive moves (thumbs, pills leading edge). */
    fun <T> snappy() = spring<T>(dampingRatio = 0.85f, stiffness = 650f)
    /** Something landing: a little bounce. */
    fun <T> bouncy() = spring<T>(dampingRatio = 0.6f, stiffness = 420f)
    /** Slow, soft settle. */
    fun <T> soft() = spring<T>(dampingRatio = 0.9f, stiffness = 200f)
}

/** iOS-style rubber banding: the further you pull past an edge, the harder it resists. */
fun rubberBand(over: Float, dimension: Float, c: Float = 0.55f): Float =
    (over * dimension * c) / (dimension + c * abs(over))

// ---------------------------------------------------------------------------------------------
// Counter: each digit rolls vertically like an odometer.
// ---------------------------------------------------------------------------------------------

@Composable
fun RollingCounter(value: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val v = value.coerceAtLeast(0)
    val digits = v.toString().length
    Row(modifier) {
        for (i in 0 until digits) {
            val place = 10.0.pow(digits - i - 1).toInt()
            key(digits - i) { RollingDigit(v / place, style, color) }
        }
    }
}

@Composable
private fun RollingDigit(target: Int, style: TextStyle, color: Color) {
    val anim = remember { Animatable(target.toFloat()) }
    LaunchedEffect(target) { anim.animateTo(target.toFloat(), spring(dampingRatio = 0.85f, stiffness = 160f)) }
    Box(Modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        Text("0", style = style, color = Color.Transparent)
        val place = ((anim.value % 10f) + 10f) % 10f
        for (n in 0..9) {
            var offset = (10f + n - place) % 10f
            if (offset > 5f) offset -= 10f
            if (abs(offset) >= 1f) continue
            Text(
                "$n", style = style, color = color,
                modifier = Modifier.graphicsLayer {
                    translationY = offset * size.height
                    alpha = 1f - abs(offset) * 0.6f
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// HoldButton: press and hold to confirm a destructive action; the fill sweeps across.
// ---------------------------------------------------------------------------------------------

@Composable
fun HoldButton(
    text: String,
    doneText: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    holdMillis: Int = 1300,
    fill: Color = Ink.Alert,
    onFill: Color = Ink.OnAlert,
    onHold: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    var done by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, Springs.snappy(), label = "holdScale")
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val label = when {
        done -> doneText
        hint -> "Keep holding…"
        else -> text
    }

    LaunchedEffect(hint) {
        if (hint) {
            delay(1400)
            hint = false
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale; scaleY = scale
                translationX = shake.value
            }
            .clip(CircleShape)
            .background(Ink.Raised)
            .border(1.dp, Ink.LineStrong, CircleShape)
            .pointerInputHold(
                enabled = !done,
                onPress = {
                    pressed = true
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch {
                        try {
                            progress.animateTo(1f, tween(((1f - progress.value) * holdMillis).toInt(), easing = LinearEasing))
                            done = true
                            pressed = false
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onHold()
                            delay(1500)
                            done = false
                            progress.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
                        } catch (_: CancellationException) {
                        }
                    }
                },
                onRelease = { job ->
                    pressed = false
                    if (!done) {
                        val tapped = progress.value < 0.2f
                        job?.cancel()
                        scope.launch { progress.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
                        if (tapped) {
                            hint = true
                            scope.launch {
                                for (x in listOf(-8f, 7f, -5f, 3f, 0f)) shake.animateTo(x, tween(45))
                            }
                        }
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        HoldLabel(label, icon, Ink.Alert)
        Box(
            Modifier
                .matchParentSize()
                .drawWithContent { clipRect(right = size.width * progress.value) { this@drawWithContent.drawContent() } }
                .background(fill),
            contentAlignment = Alignment.Center,
        ) {
            HoldLabel(label, icon, onFill)
        }
    }
}

@Composable
private fun HoldLabel(label: String, icon: ImageVector?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        AnimatedContent(
            label,
            transitionSpec = { (androidx.compose.animation.fadeIn(tween(160)) togetherWith fadeOut(tween(100))) using SizeTransform(clip = false) },
            label = "holdLabel",
        ) { Text(it, style = MaterialTheme.typography.labelLarge, color = color) }
    }
}

/** Press/release tracking that hands the progress job back on release. */
private fun Modifier.pointerInputHold(
    enabled: Boolean,
    onPress: () -> kotlinx.coroutines.Job?,
    onRelease: (kotlinx.coroutines.Job?) -> Unit,
): Modifier = this.then(
    if (!enabled) Modifier else Modifier.pointerInputCompat(onPress, onRelease)
)

private fun Modifier.pointerInputCompat(
    onPress: () -> kotlinx.coroutines.Job?,
    onRelease: (kotlinx.coroutines.Job?) -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        detectTapGestures(onPress = {
            val job = onPress()
            tryAwaitRelease()
            onRelease(job)
        })
    }
)

// ---------------------------------------------------------------------------------------------
// SpringCheck: box fills with a springy overshoot, then the tick draws itself.
// ---------------------------------------------------------------------------------------------

@Composable
fun SpringCheck(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val t = remember { Animatable(if (checked) 1f else 0f) }
    LaunchedEffect(checked) { t.animateTo(if (checked) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 420f)) }
    val path = remember { Path() }
    val seg = remember { Path() }
    val measure = remember { PathMeasure() }
    Canvas(modifier.size(size)) {
        val v = t.value
        val swell = 1f + 0.3f * (v - 1f).coerceAtLeast(0f)
        val r = CornerRadius(this.size.minDimension * 0.3f)
        scale(swell) {
            drawRoundRect(Ink.Raised, cornerRadius = r)
            drawRoundRect(Ink.LineStrong, cornerRadius = r, style = Stroke(1.dp.toPx()))
            scale(v.coerceAtLeast(0f)) { drawRoundRect(Ink.Accent, cornerRadius = r) }
            val w = this.size.width
            val h = this.size.height
            path.reset()
            path.moveTo(w * 0.28f, h * 0.52f)
            path.lineTo(w * 0.44f, h * 0.67f)
            path.lineTo(w * 0.73f, h * 0.36f)
            measure.setPath(path, false)
            seg.reset()
            measure.getSegment(0f, measure.length * v.coerceIn(0f, 1f), seg, true)
            drawPath(seg, Ink.OnAccent, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

/** Draws a strike-through across the content that grows with [progress]. */
fun Modifier.strike(progress: Float, color: Color = Ink.TextFaint): Modifier = drawWithContent {
    drawContent()
    if (progress > 0f) {
        val y = size.height * 0.36f
        drawLine(color, Offset(0f, y), Offset(size.width * progress, y), 1.5.dp.toPx(), cap = StrokeCap.Round)
    }
}

// ---------------------------------------------------------------------------------------------
// ClickSpark: a tiny burst of lines from the touch point.
// ---------------------------------------------------------------------------------------------

private class Spark(val at: Offset, val p: Animatable<Float, *>)

fun Modifier.clickSpark(color: Color, count: Int = 8): Modifier = composed {
    val sparks = remember { mutableStateListOf<Spark>() }
    val scope = rememberCoroutineScope()
    this
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val s = Spark(down.position, Animatable(0f))
                sparks += s
                scope.launch {
                    s.p.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
                    sparks -= s
                }
            }
        }
        .drawWithContent {
            drawContent()
            sparks.forEach { s ->
                val p = s.p.value
                val r0 = 8.dp.toPx() + 22.dp.toPx() * p
                val len = 9.dp.toPx() * (1f - p)
                for (i in 0 until count) {
                    val a = Math.toRadians(i * 360.0 / count - 90.0)
                    val dx = cos(a).toFloat()
                    val dy = sin(a).toFloat()
                    drawLine(
                        color.copy(alpha = 1f - p),
                        Offset(s.at.x + dx * r0, s.at.y + dy * r0),
                        Offset(s.at.x + dx * (r0 + len), s.at.y + dy * (r0 + len)),
                        2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
}

// ---------------------------------------------------------------------------------------------
// AnimatedList: rows fade and rise in, staggered.
// ---------------------------------------------------------------------------------------------

fun Modifier.staggerIn(index: Int): Modifier = composed {
    val inspection = LocalInspectionMode.current
    val a = remember { Animatable(if (inspection) 1f else 0f) }
    LaunchedEffect(Unit) {
        delay(min(index, 8) * 45L)
        a.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 240f))
    }
    graphicsLayer {
        alpha = a.value
        translationY = (1f - a.value) * 18.dp.toPx()
    }
}

// ---------------------------------------------------------------------------------------------
// BlurText: words resolve from a blur, one after another.
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlurText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val inspection = LocalInspectionMode.current
    AnimatedContent(
        text,
        transitionSpec = { EnterTransition.None togetherWith fadeOut(tween(120)) },
        modifier = modifier,
        label = "blurText",
    ) { t ->
        val words = t.split(" ")
        FlowRow(horizontalArrangement = Arrangement.Center) {
            words.forEachIndexed { i, w ->
                val a = remember { Animatable(if (inspection) 1f else 0f) }
                LaunchedEffect(Unit) {
                    delay(i * 60L)
                    a.animateTo(1f, spring(dampingRatio = 0.9f, stiffness = 200f))
                }
                Text(
                    if (i < words.lastIndex) "$w " else w,
                    style = style,
                    color = color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = a.value
                            translationY = (1f - a.value) * 10.dp.toPx()
                        }
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.blur((10f * (1f - a.value)).dp, BlurredEdgeTreatment.Unbounded)
                            } else Modifier
                        ),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// RotatingText: cycles words; each letter springs up into place, the old word lifts away.
// ---------------------------------------------------------------------------------------------

@Composable
fun RotatingWords(words: List<String>, style: TextStyle, color: Color, modifier: Modifier = Modifier, intervalMs: Long = 2300) {
    var i by remember { mutableIntStateOf(0) }
    val inspection = LocalInspectionMode.current
    LaunchedEffect(Unit) {
        if (inspection) return@LaunchedEffect
        while (true) {
            delay(intervalMs)
            i = (i + 1) % words.size
        }
    }
    AnimatedContent(
        i,
        transitionSpec = {
            EnterTransition.None togetherWith (slideOutVertically(Springs.ui()) { -it / 2 } + fadeOut(tween(160))) using SizeTransform(clip = false)
        },
        modifier = modifier.clipToBounds(),
        label = "rotating",
    ) { idx ->
        Row {
            words[idx].forEachIndexed { c, ch ->
                val a = remember { Animatable(if (inspection) 1f else 0f) }
                LaunchedEffect(Unit) {
                    delay(c * 22L)
                    a.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 380f))
                }
                Text(
                    ch.toString(), style = style, color = color,
                    modifier = Modifier.graphicsLayer {
                        translationY = (1f - a.value) * size.height
                        alpha = a.value
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// SwipeRow: swipe left to reveal Delete; swipe far enough and it commits on release.
// ---------------------------------------------------------------------------------------------

@Composable
fun SwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit,
) {
    var dx by remember { mutableFloatStateOf(0f) }
    var width by remember { mutableFloatStateOf(1f) }
    var armed by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val actionW = with(density) { 88.dp.toPx() }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    suspend fun settleTo(target: Float, bouncy: Boolean = false) {
        animate(dx, target, animationSpec = if (bouncy) spring(dampingRatio = 0.8f, stiffness = 420f) else Springs.ui()) { v, _ -> dx = v }
    }

    fun commit() = scope.launch {
        animate(dx, -width * 1.05f, animationSpec = tween(200, easing = FastOutSlowInEasing)) { v, _ -> dx = v }
        onDelete()
    }

    Box(modifier.onSizeChanged { width = it.width.toFloat() }) {
        val reveal = (-dx).coerceAtLeast(0f)
        if (reveal > 0.5f) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(Ink.Alert)
                    .clickable { commit() },
                contentAlignment = Alignment.CenterEnd,
            ) {
                val p = (reveal / actionW).coerceIn(0f, 1f)
                Icon(
                    Icons.Rounded.DeleteOutline, "Delete", tint = Ink.OnAlert,
                    modifier = Modifier
                        .padding(end = 32.dp)
                        .size(22.dp)
                        .graphicsLayer {
                            val s = 0.6f + 0.4f * p + if (armed) 0.15f else 0f
                            scaleX = s; scaleY = s
                            alpha = p
                        },
                )
            }
        }
        Box(
            Modifier
                .graphicsLayer { translationX = dx }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    armed -> { commit(); armed = false }
                                    -dx > actionW * 0.5f -> settleTo(-actionW, bouncy = true)
                                    else -> settleTo(0f)
                                }
                            }
                        },
                        onDragCancel = { scope.launch { settleTo(0f) } },
                    ) { change, drag ->
                        change.consume()
                        val raw = dx + drag
                        dx = if (raw > 0f) rubberBand(raw, width) else raw.coerceAtLeast(-width)
                        val nowArmed = -dx > width * 0.55f
                        if (nowArmed != armed) {
                            armed = nowArmed
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                },
        ) { content() }
    }
}

// ---------------------------------------------------------------------------------------------
// SwipeToast: a small toast with an action; flick it down to dismiss.
// ---------------------------------------------------------------------------------------------

@Composable
fun SwipeToast(message: String, action: String, onAction: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var dy by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    Row(
        modifier
            .graphicsLayer {
                translationY = dy
                alpha = 1f - (dy / 300f).coerceIn(0f, 0.8f)
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (dy > 60f) {
                                animate(dy, 400f, animationSpec = tween(180)) { v, _ -> dy = v }
                                onDismiss()
                            } else {
                                animate(dy, 0f, animationSpec = Springs.bouncy()) { v, _ -> dy = v }
                            }
                        }
                    },
                ) { change, drag ->
                    change.consume()
                    val raw = dy + drag
                    dy = if (raw < 0f) -rubberBand(-raw, 200f) else raw
                }
            }
            .fillMaxWidth()
            .panel(RoundedCornerShape(16.dp), color = Ink.Raised, border = Ink.LineStrong)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Ink.Text, modifier = Modifier.weight(1f))
        Text(
            action,
            style = MaterialTheme.typography.labelLarge,
            color = Ink.Accent,
            modifier = Modifier
                .clip(CircleShape)
                .bouncyClick(onClick = onAction)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// ElasticSlider: the track stretches when you pull past either end, then springs back.
// ---------------------------------------------------------------------------------------------

@Composable
fun ElasticSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var widthPx by remember { mutableFloatStateOf(1f) }
    var over by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val thumb by animateDpAsState(if (dragging) 26.dp else 22.dp, Springs.bouncy(), label = "thumb")
    val track by animateDpAsState(if (dragging) 8.dp else 6.dp, Springs.snappy(), label = "track")
    val span = range.endInclusive - range.start
    val fraction = ((value - range.start) / span).coerceIn(0f, 1f)

    fun setFrom(x: Float) {
        val raw = x / widthPx
        over = when {
            raw < 0f -> -rubberBand(-x, widthPx, 0.3f)
            raw > 1f -> rubberBand(x - widthPx, widthPx, 0.3f)
            else -> 0f
        }
        onValueChange(range.start + raw.coerceIn(0f, 1f) * span)
    }

    fun release() {
        dragging = false
        scope.launch { animate(over, 0f, animationSpec = Springs.bouncy()) { v, _ -> over = v } }
        onValueChangeFinished()
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    setFrom(pos.x)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    release()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { pos ->
                        dragging = true
                        setFrom(pos.x)
                    },
                    onDragEnd = { release() },
                    onDragCancel = { release() },
                ) { change, _ ->
                    change.consume()
                    setFrom(change.position.x)
                }
            },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val h = track.toPx() * (1f - (abs(over) / size.width).coerceAtMost(0.3f))
            val left = over.coerceAtMost(0f)
            val right = size.width + over.coerceAtLeast(0f)
            val cy = size.height / 2f
            val r = CornerRadius(h / 2f)
            drawRoundRect(Ink.Sunken, Offset(left, cy - h / 2f), Size(right - left, h), r)
            val tx = left + (right - left) * fraction
            drawRoundRect(Ink.Text, Offset(left, cy - h / 2f), Size(tx - left, h), r)
            val tr = thumb.toPx() / 2f
            drawCircle(Color.Black.copy(alpha = 0.25f), tr + 2.dp.toPx(), Offset(tx, cy + 1.dp.toPx()))
            drawCircle(Ink.Text, tr, Offset(tx, cy))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Pill edges shared by the rubber segmented control and the tab bar: the leading edge moves
// first and the trailing edge catches up, so the pill stretches in the direction of travel.
// ---------------------------------------------------------------------------------------------

class PillEdges(left: Float, right: Float) {
    val l = Animatable(left)
    val r = Animatable(right)

    suspend fun moveTo(tl: Float, tr: Float) = coroutineScope {
        val goingRight = tl > l.value
        val lead = spring<Float>(dampingRatio = 0.82f, stiffness = 700f)
        val trail = spring<Float>(dampingRatio = 0.8f, stiffness = 260f)
        launch { l.animateTo(tl, if (goingRight) trail else lead) }
        launch { r.animateTo(tr, if (goingRight) lead else trail) }
    }

    suspend fun snap(tl: Float, tr: Float) {
        l.snapTo(tl)
        r.snapTo(tr)
    }
}
