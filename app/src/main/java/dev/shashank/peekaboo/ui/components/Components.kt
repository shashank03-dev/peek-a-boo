package dev.shashank.peekaboo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.Ink

/** Springy press feedback with a light haptic tick. */
fun Modifier.bouncyClick(enabled: Boolean = true, pressedScale: Float = 0.97f, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val haptics = LocalHapticFeedback.current
    val dim by animateFloatAsState(if (pressed) 0.82f else 1f, spring(dampingRatio = 1f, stiffness = 900f), label = "dim")
    graphicsLayer { scaleX = scale; scaleY = scale; alpha = dim }
        .clickable(interactionSource = source, indication = null, enabled = enabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

/** A flat panel: solid surface with a hairline border. */
fun Modifier.panel(shape: Shape = RoundedCornerShape(20.dp), color: Color = Ink.Surface, border: Color = Ink.Line): Modifier =
    clip(shape).background(color).border(1.dp, border, shape)

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(24.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.panel().padding(padding), content = content)
}

/** Screen header: small label over a large title. */
@Composable
fun ScreenHeader(title: String, eyebrow: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(eyebrow.uppercase(), style = Eyebrow, color = Ink.TextFaint)
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.displaySmall, color = Ink.Text)
        }
        trailing?.invoke()
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .bouncyClick(pressedScale = 0.9f, onClick = onClick)
            .panel(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = Ink.Text, modifier = Modifier.size(19.dp))
    }
}

/** Pill status chip: coloured dot, neutral label. */
@Composable
fun StatusChip(label: String, color: Color, live: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier
            .panel(CircleShape)
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseDot(color, live = live)
        Spacer(Modifier.width(4.dp))
        Text(label.uppercase(), style = Eyebrow, color = Ink.Text)
    }
}

/**
 * Switch (React Bits "SquishSwitch"): the thumb stretches along its travel in proportion to its
 * speed and swells while pressed, then settles with a soft spring.
 */
@Composable
fun PeekSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val x = remember { Animatable(if (checked) 1f else 0f) }
    LaunchedEffect(checked) { x.animateTo(if (checked) 1f else 0f, spring(dampingRatio = 0.72f, stiffness = 380f)) }
    val swell by animateFloatAsState(if (pressed) 1.16f else 1f, spring(dampingRatio = 0.6f, stiffness = 520f), label = "swell")
    val track by animateColorAsState(if (checked) Ink.Accent else Ink.Sunken, spring(stiffness = 300f), label = "track")
    val thumb by animateColorAsState(if (checked) Ink.OnAccent else Ink.TextMuted, spring(stiffness = 300f), label = "thumbColor")
    val haptics = LocalHapticFeedback.current
    val travel = 20.dp
    Box(
        Modifier
            .size(width = 48.dp, height = 28.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(CircleShape)
            .background(track)
            .clickable(enabled = enabled, indication = null, interactionSource = source) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .offset { IntOffset((x.value * travel.toPx()).roundToInt(), 0) }
                .size(20.dp)
                .graphicsLayer {
                    // velocity is in fractions/s; convert to dp/s for a speed-based stretch.
                    val speed = abs(x.velocity) * travel.value
                    val stretch = 1f + (speed / 600f).coerceAtMost(0.4f)
                    scaleX = stretch * swell
                    scaleY = swell / stretch
                }
                .background(thumb, CircleShape)
        )
    }
}

/**
 * Segmented control (React Bits "RubberSegment"): the pill stretches toward the new segment, can
 * be dragged, rubber-bands past the ends and snaps to the nearest segment on release.
 */
@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .panel(RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        val n = options.size
        val segPx = constraints.maxWidth.toFloat() / n
        val totalPx = constraints.maxWidth.toFloat()
        val edges = remember(segPx) { PillEdges(selected * segPx, (selected + 1) * segPx) }
        var dragging by remember { mutableStateOf(false) }
        LaunchedEffect(selected, segPx) { if (!dragging) edges.moveTo(selected * segPx, (selected + 1) * segPx) }
        val density = LocalDensity.current

        Box(
            Modifier
                .offset { IntOffset(edges.l.value.roundToInt(), 0) }
                .width(with(density) { (edges.r.value - edges.l.value).coerceAtLeast(0f).toDp() })
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(Ink.Sunken)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .pointerInput(segPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = {
                            val center = (edges.l.value + edges.r.value) / 2f
                            val target = (center / segPx).toInt().coerceIn(0, n - 1)
                            dragging = false
                            scope.launch { edges.moveTo(target * segPx, (target + 1) * segPx) }
                            if (target != selected) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(target)
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            scope.launch { edges.moveTo(selected * segPx, (selected + 1) * segPx) }
                        },
                    ) { change, dx ->
                        change.consume()
                        var l = edges.l.value + dx
                        if (l < 0f) l = -rubberBand(-l, segPx, 0.35f)
                        if (l + segPx > totalPx) l = totalPx - segPx + rubberBand(l + segPx - totalPx, segPx, 0.35f)
                        scope.launch { edges.snap(l, l + segPx) }
                    }
                },
        ) {
            options.forEachIndexed { i, label ->
                // Text brightens with how much of the pill covers its segment.
                val overlap = ((minOf(edges.r.value, (i + 1) * segPx) - maxOf(edges.l.value, i * segPx)) / segPx).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                            if (i != selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(i)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = lerp(Ink.TextMuted, Ink.Text, overlap))
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = Eyebrow, color = Ink.TextFaint, modifier = modifier.padding(start = 4.dp, top = 8.dp))
}

/** Grouped list section. */
@Composable
fun Section(header: String? = null, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        if (header != null) {
            Text(header.uppercase(), style = Eyebrow, color = Ink.TextFaint, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        }
        Column(Modifier.fillMaxWidth().panel(), content = content)
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = Ink.TextFaint,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )
        }
    }
}

/** Monochrome icon on a raised square; tint only when the row means something (red = destructive). */
@Composable
fun IconBadge(icon: ImageVector, tint: Color = Ink.TextMuted, size: Dp = 34.dp, background: Color = Ink.Raised) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun ListRow(
    title: String,
    icon: ImageVector? = null,
    iconTint: Color = Ink.TextMuted,
    subtitle: String? = null,
    titleColor: Color = Ink.Text,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    chevron: Boolean = onClick != null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                IconBadge(icon, iconTint)
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ink.TextMuted)
                }
            }
            trailing?.invoke()
            if (chevron) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Ink.TextFaint, modifier = Modifier.size(20.dp))
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .padding(start = if (icon != null) 66.dp else 16.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Ink.Line)
            )
        }
    }
}

/** Primary action: solid accent capsule. */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    container: Color = Ink.Accent,
    content: Color = Ink.OnAccent,
    spark: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .bouncyClick(enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(CircleShape)
            .background(container)
            .then(if (spark) Modifier.clickSpark(content.copy(alpha = 0.7f)) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, null, tint = content, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = content, textAlign = TextAlign.Center)
        }
    }
}

/** Secondary action: neutral capsule. */
@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Ink.Text,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .bouncyClick(onClick = onClick)
            .panel(CircleShape, color = Ink.Raised, border = Ink.LineStrong),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}

/** Small solid tag, e.g. "FIX" or a status. */
@Composable
fun Tag(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = Eyebrow,
        color = content,
        modifier = modifier.clip(CircleShape).background(container).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
