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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.shashank.peekaboo.ui.theme.Eyebrow
import dev.shashank.peekaboo.ui.theme.Night

/** Springy press feedback with a light haptic tick. */
fun Modifier.bouncyClick(enabled: Boolean = true, pressedScale: Float = 0.965f, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val haptics = LocalHapticFeedback.current
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

/** Frosted surface: translucent fill with a top-lit hairline border, like a pane of glass. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(28.dp), fill: Color = Night.Glass, glow: Color? = null): Modifier =
    this
        .clip(shape)
        .background(fill)
        .then(
            if (glow != null) Modifier.background(
                Brush.radialGradient(listOf(glow.copy(alpha = 0.22f), Color.Transparent), center = Offset(0f, 0f), radius = 900f)
            ) else Modifier
        )
        .border(
            1.dp,
            Brush.verticalGradient(listOf(Night.StrokeBright, Night.Hairline, Color.White.copy(alpha = 0.04f))),
            shape,
        )

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    glow: Color? = null,
    corner: Dp = 28.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.glass(RoundedCornerShape(corner), glow = glow).padding(padding), content = content)
}

/** Screen header: monospace eyebrow over a big display title. */
@Composable
fun ScreenHeader(title: String, eyebrow: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(eyebrow.uppercase(), style = Eyebrow, color = Night.TextDim)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.displaySmall, color = Night.Text)
        }
        trailing?.invoke()
    }
}

/** Gradient-filled text for hero numbers. */
@Composable
fun GradientText(text: String, style: TextStyle, brush: Brush, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    Text(text, modifier = modifier, style = style.copy(brush = brush), textAlign = textAlign, maxLines = 1)
}

/** Round glass icon button. */
@Composable
fun GlassIconButton(icon: ImageVector, contentDescription: String, tint: Color = Night.Text, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .bouncyClick(pressedScale = 0.9f, onClick = onClick)
            .glass(CircleShape, fill = Night.GlassStrong),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** Pill status chip with a pulsing dot. */
@Composable
fun StatusChip(label: String, color: Color, live: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.28f), CircleShape)
            .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseDot(color, live = live)
        Spacer(Modifier.width(4.dp))
        Text(label.uppercase(), style = Eyebrow.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

/** Toggle with a gradient track and a glowing thumb. */
@Composable
fun NightSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val offset by animateDpAsState(
        if (checked) 22.dp else 0.dp,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "thumb",
    )
    val trackAlpha by animateFloatAsState(if (checked) 1f else 0f, label = "track")
    val thumb by animateColorAsState(if (checked) Color.White else Color(0xFFBFC1D0), label = "thumbColor")
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .size(width = 52.dp, height = 30.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .background(Brush.linearGradient(listOf(Night.Mint.copy(alpha = trackAlpha), Night.Teal.copy(alpha = trackAlpha))))
            .clickable(enabled = enabled, indication = null, interactionSource = remember { MutableInteractionSource() }) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(24.dp)
                .shadow(6.dp, CircleShape, ambientColor = Night.Mint, spotColor = Night.Mint)
                .background(thumb, CircleShape)
        )
    }
}

/** Glass segmented control with a sliding, glowing selection pill. */
@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .glass(CircleShape)
            .padding(4.dp)
    ) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * selected, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow), label = "seg")
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .border(1.dp, Brush.verticalGradient(listOf(Night.StrokeBright, Color.Transparent)), CircleShape)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                val color by animateColorAsState(if (i == selected) Night.Text else Night.TextDim, label = "segText")
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

/** A grouped glass section with a monospace header. */
@Composable
fun Section(header: String? = null, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        if (header != null) {
            Text(
                header.uppercase(),
                style = Eyebrow,
                color = Night.TextDim,
                modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
            )
        }
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)), content = content)
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = Night.TextFaint,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp),
            )
        }
    }
}

/** Soft tinted icon badge: a coloured glyph on a glow of the same colour. */
@Composable
fun IconBadge(icon: ImageVector, color: Color, size: Dp = 36.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
fun ListRow(
    title: String,
    icon: ImageVector? = null,
    iconColor: Color = Night.Mint,
    subtitle: String? = null,
    titleColor: Color = Night.Text,
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                IconBadge(icon, iconColor)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = titleColor)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Night.TextDim)
                }
            }
            trailing?.invoke()
            if (chevron) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = Night.TextFaint, modifier = Modifier.size(14.dp))
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .padding(start = if (icon != null) 66.dp else 16.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Night.Hairline)
            )
        }
    }
}

/** Primary call to action: a luminous gradient capsule with a coloured glow underneath. */
@Composable
fun GlowButton(
    text: String,
    brush: Brush,
    glow: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color = Night.Void,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(60.dp)
            .bouncyClick(enabled, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .drawBehind {
                // Soft elliptical glow pooled under the capsule.
                scale(scaleX = 1f, scaleY = 0.32f, pivot = Offset(size.width / 2f, size.height * 0.9f)) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(glow.copy(alpha = 0.55f), glow.copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height * 0.9f),
                            radius = size.width * 0.55f,
                        ),
                        radius = size.width * 0.55f,
                        center = Offset(size.width / 2f, size.height * 0.9f),
                    )
                }
            }
            .clip(CircleShape)
            .background(brush)
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.Transparent)), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor, textAlign = TextAlign.Center)
        }
    }
}

/** Secondary action: a glass capsule. */
@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Night.Text,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .bouncyClick(onClick = onClick)
            .glass(CircleShape, fill = Night.GlassStrong),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = color)
        }
    }
}
