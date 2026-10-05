package dev.shashank.peekaboo.ui.components

import androidx.compose.animation.animateColorAsState
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
    graphicsLayer { scaleX = scale; scaleY = scale }
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

@Composable
fun PeekSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val offset by animateDpAsState(
        if (checked) 20.dp else 0.dp,
        spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "thumb",
    )
    val track by animateColorAsState(if (checked) Ink.Accent else Ink.Sunken, label = "track")
    val thumb by animateColorAsState(if (checked) Ink.OnAccent else Ink.TextMuted, label = "thumbColor")
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .size(width = 48.dp, height = 28.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(CircleShape)
            .background(track)
            .clickable(enabled = enabled, indication = null, interactionSource = remember { MutableInteractionSource() }) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(4.dp),
    ) {
        Box(Modifier.offset(x = offset).size(20.dp).background(thumb, CircleShape))
    }
}

@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .panel(RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * selected, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "seg")
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(Ink.Sunken)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                val color by animateColorAsState(if (i == selected) Ink.Text else Ink.TextMuted, label = "segText")
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
                    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
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
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .bouncyClick(enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(CircleShape)
            .background(container),
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
