package dev.shashank.peekaboo.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shashank.peekaboo.ui.theme.Ink
import dev.shashank.peekaboo.ui.theme.Inter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Dynamic-Island style capsule: "Peeping · 2" with a live, rolling counter.
 *
 * It drips out of the camera as a liquid blob (a metaball neck joins it to the camera and
 * pinches off), lands with a spring, then stretches open with an overshoot and a little squash.
 * Exit plays it backwards and the blob is sucked back up into the camera.
 *
 * Every frame of the motion happens in the draw phase: no recomposition, no relayout, and the
 * overlay window never resizes.
 */
@Composable
fun NotchPill(peepers: Int, visible: Boolean, spec: NotchSpec) {
    val motion = remember { PillMotion() }
    var content by remember { mutableStateOf(IntSize.Zero) }
    val ready = content != IntSize.Zero
    val bridge = remember { Path() }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow = pulse.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "glow",
    )

    LaunchedEffect(visible, ready) {
        if (!ready) return@LaunchedEffect
        if (visible) motion.enter(content.height.toFloat()) { content.width.toFloat() }
        else motion.exit(content.height.toFloat())
    }
    // The counter growing from 9 to 10 widens the pill; follow it with the same spring.
    LaunchedEffect(content.width) {
        if (visible && motion.expanded) motion.width.animateTo(content.width.toFloat(), PillMotion.OpenSpring)
    }

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                if (!ready) return@drawBehind
                val a = motion.alpha.value
                if (a <= 0f) return@drawBehind
                val f = motion.frame(spec, content, size.width)
                val black = Color.Black.copy(alpha = a)

                val neck = motion.neck.value.coerceIn(0f, NECK_MAX)
                if (neck > 0.01f) {
                    drawCircle(black, spec.cameraR, Offset(spec.cameraX, spec.cameraY))
                    if (gooBridge(bridge, spec.cameraX, spec.cameraY, spec.cameraR, f.cx, f.cy, min(f.w, f.h) / 2f, neck)) {
                        drawPath(bridge, black)
                    }
                }
                val r = min(f.w, f.h) / 2f
                drawRoundRect(black, Offset(f.cx - f.w / 2f, f.cy - f.h / 2f), Size(f.w, f.h), CornerRadius(r, r))
                if (neck <= 0.01f && f.open > 0.9f) {
                    drawRoundRect(
                        Color.White.copy(alpha = 0.08f * a), Offset(f.cx - f.w / 2f, f.cy - f.h / 2f), Size(f.w, f.h),
                        CornerRadius(r, r), style = Stroke(width = 1.dp.toPx()),
                    )
                }
            },
    ) {
        PillContent(
            peepers = peepers,
            glow = glow,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset { IntOffset(0, spec.pillTopPx) }
                .onSizeChanged { content = it }
                .graphicsLayer {
                    if (!ready) {
                        alpha = 0f
                        return@graphicsLayer
                    }
                    val f = motion.frame(spec, content, spec.windowWidthPx.toFloat())
                    // Content fades in over the last part of the stretch, like the island's live activity.
                    val reveal = ((f.open - 0.45f) / 0.55f).coerceIn(0f, 1f)
                    alpha = motion.alpha.value * reveal
                    val s = 0.86f + 0.14f * f.open.coerceAtMost(1f)
                    scaleX = s
                    scaleY = s
                    translationY = f.cy - (spec.pillTopPx + content.height / 2f)
                }
                .drawWithContent {
                    if (!ready) return@drawWithContent
                    val f = motion.frame(spec, content, spec.windowWidthPx.toFloat())
                    val left = (spec.windowWidthPx - size.width) / 2f
                    // Only what the capsule currently covers is visible, so the text is revealed by the
                    // pill opening rather than sliding around.
                    clipRect(f.cx - f.w / 2f - left, -size.height, f.cx + f.w / 2f - left, size.height * 2f) {
                        this@drawWithContent.drawContent()
                    }
                },
        )
    }
}

@Composable
private fun PillContent(peepers: Int, glow: State<Float>, modifier: Modifier) {
    Row(
        modifier.padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .graphicsLayer {
                    val g = glow.value
                    scaleX = 0.9f + g * 0.1f
                    scaleY = scaleX
                }
                .drawBehind { drawCircle(Ink.Alert.copy(alpha = 0.18f * glow.value)) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Visibility, null, tint = Ink.Alert, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "Peeping",
            maxLines = 1,
            softWrap = false,
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
                    (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 500f)) { it } + fadeIn()) togetherWith
                        (slideOutVertically { -it } + fadeOut())
                },
                label = "count",
            ) { n ->
                Text(
                    "$n",
                    maxLines = 1,
                    style = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(7.dp)
                .graphicsLayer { alpha = glow.value }
                .background(Ink.Alert, CircleShape),
        )
    }
}

private const val NECK_MAX = 0.6f

/** Pill geometry for one frame, in window pixels. [open] is 0 for a blob and 1 at full width. */
private class PillFrame(val cx: Float, val cy: Float, val w: Float, val h: Float, val open: Float)

private class PillMotion {
    /** 0 = sitting on the camera, 1 = resting under it. Springs past 1 for the landing bounce. */
    val drop = Animatable(0f)
    /** Blob size: 0 = camera-sized, 1 = pill height. */
    val grow = Animatable(0f)
    /** Metaball neck strength joining the blob to the camera. */
    val neck = Animatable(0f)
    /** Pill width in px once it starts opening; 0 while it is still a blob. */
    val width = Animatable(0f)
    val alpha = Animatable(0f)
    var expanded = false
        private set

    fun frame(spec: NotchSpec, content: IntSize, windowW: Float): PillFrame {
        val h = content.height.toFloat()
        val targetW = content.width.toFloat()
        val restCy = spec.pillTopPx + h / 2f
        val d = drop.value
        val base = lerp(spec.cameraR * 2f, h, grow.value)
        val cx = lerp(spec.cameraX, windowW / 2f, d.coerceIn(0f, 1f))
        val cy = lerp(spec.cameraY, restCy, d)
        val wv = width.value
        return if (wv <= 0f) {
            // Falling blob: stretch along the direction of travel in proportion to its speed.
            val speed = abs(drop.velocity * (restCy - spec.cameraY))
            val s = (speed / 6000f).coerceAtMost(0.22f)
            PillFrame(cx, cy, base * (1f - s * 0.45f), base * (1f + s), 0f)
        } else {
            val w = max(wv, base)
            // Squash on the overshoot and while stretching fast, so the volume reads as conserved.
            val over = ((w - targetW) / targetW).coerceAtLeast(0f)
            val stretch = (width.velocity / 30000f).coerceIn(0f, 0.06f)
            val hh = h * (1f - over * 0.6f - stretch).coerceAtLeast(0.86f)
            val open = if (targetW > h) ((w - h) / (targetW - h)).coerceAtLeast(0f) else 1f
            PillFrame(cx, cy, w, hh, open)
        }
    }

    suspend fun enter(h: Float, targetW: () -> Float) = coroutineScope {
        // Let a freshly attached window draw a couple of frames first so first-composition work
        // never lands inside the animation.
        repeat(2) { withFrameNanos { } }
        alpha.snapTo(1f)
        if (width.value <= 0f) {
            neck.snapTo(NECK_MAX)
            launch { grow.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 520f)) }
            launch { drop.animateTo(1f, spring(dampingRatio = 0.52f, stiffness = 340f)) }
            snapshotFlow { drop.value }.first { it >= 0.8f }
        } else {
            launch { grow.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 520f)) }
            launch { drop.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 400f)) }
        }
        expanded = true
        // The neck pinches off as the pill starts to open.
        launch { neck.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 520f)) }
        if (width.value < h) width.snapTo(h)
        width.animateTo(targetW(), OpenSpring)
    }

    suspend fun exit(h: Float) = coroutineScope {
        if (alpha.value <= 0f) return@coroutineScope
        expanded = false
        if (width.value > 0f) {
            width.animateTo(h, tween(190, easing = FastOutSlowInEasing))
            width.snapTo(0f)
        }
        launch { neck.animateTo(NECK_MAX, tween(140)) }
        launch { grow.animateTo(0f, tween(240, easing = FastOutLinearInEasing)) }
        drop.animateTo(0f, tween(240, easing = FastOutLinearInEasing))
        alpha.snapTo(0f)
        neck.snapTo(0f)
    }

    companion object {
        /** Apple-ish bouncy open: visible overshoot, settles in ~half a second. */
        val OpenSpring = spring<Float>(dampingRatio = 0.55f, stiffness = 280f)
    }
}

/** Milliseconds the pill needs to finish its exit before the overlay window can be detached. */
const val NOTCH_EXIT_MS = 520L

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/**
 * Metaball bridge between two circles (after varun.ca/metaballs). [spread] (0..0.6) is the neck
 * width: 0 pinches it to nothing. The neck also thins out on its own as the circles separate,
 * so the drip snaps smoothly instead of popping.
 *
 * @return false when there is nothing to draw.
 */
private fun gooBridge(path: Path, x1: Float, y1: Float, r1: Float, x2: Float, y2: Float, r2: Float, spread: Float): Boolean {
    path.reset()
    val d = hypot(x2 - x1, y2 - y1)
    if (r1 <= 0f || r2 <= 0f || d <= abs(r1 - r2)) return false
    val reach = (r1 + r2) * 2.4f
    val v = spread * (1f - ((d - (r1 + r2)) / reach).coerceIn(0f, 1f))
    // A thinner neck than this reads as a string, not liquid: let it snap.
    if (v <= 0.2f) return false

    val u1: Float
    val u2: Float
    if (d < r1 + r2) {
        u1 = acos(((r1 * r1 + d * d - r2 * r2) / (2 * r1 * d)).coerceIn(-1f, 1f))
        u2 = acos(((r2 * r2 + d * d - r1 * r1) / (2 * r2 * d)).coerceIn(-1f, 1f))
    } else {
        u1 = 0f
        u2 = 0f
    }
    val between = atan2(y2 - y1, x2 - x1)
    val maxSpread = acos(((r1 - r2) / d).coerceIn(-1f, 1f))
    val pi = PI.toFloat()
    val a1 = between + u1 + (maxSpread - u1) * v
    val a2 = between - u1 - (maxSpread - u1) * v
    val a3 = between + pi - u2 - (pi - u2 - maxSpread) * v
    val a4 = between - pi + u2 + (pi - u2 - maxSpread) * v

    val p1x = x1 + r1 * cos(a1); val p1y = y1 + r1 * sin(a1)
    val p2x = x1 + r1 * cos(a2); val p2y = y1 + r1 * sin(a2)
    val p3x = x2 + r2 * cos(a3); val p3y = y2 + r2 * sin(a3)
    val p4x = x2 + r2 * cos(a4); val p4y = y2 + r2 * sin(a4)

    val handle = min(v * 2.4f, hypot(p3x - p1x, p3y - p1y) / (r1 + r2))
    val h1 = r1 * handle
    val h2 = r2 * handle
    val half = pi / 2f
    path.moveTo(p1x, p1y)
    path.cubicTo(
        p1x + h1 * cos(a1 - half), p1y + h1 * sin(a1 - half),
        p3x + h2 * cos(a3 + half), p3y + h2 * sin(a3 + half),
        p3x, p3y,
    )
    path.lineTo(p4x, p4y)
    path.cubicTo(
        p4x + h2 * cos(a4 - half), p4y + h2 * sin(a4 - half),
        p2x + h1 * cos(a2 + half), p2y + h1 * sin(a2 + half),
        p2x, p2y,
    )
    path.close()
    return true
}
