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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.shashank.peekaboo.ui.theme.Ios

/** Springy press feedback, like UIKit buttons. */
fun Modifier.bouncyClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val haptics = LocalHapticFeedback.current
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Ios.Card)
            .border(0.5.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(24.dp))
            .padding(padding),
        content = content,
    )
}

@Composable
fun LargeTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(
                    subtitle.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Ios.Secondary,
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineLarge, color = Ios.Label)
        }
        trailing?.invoke()
    }
}

@Composable
fun IosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val track by animateColorAsState(if (checked) Ios.Green else Ios.Fill, label = "track")
    val offset by animateDpAsState(
        if (checked) 20.dp else 0.dp,
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "thumb",
    )
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .size(width = 51.dp, height = 31.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(CircleShape)
            .background(track)
            .clickable(enabled = enabled, indication = null, interactionSource = remember { MutableInteractionSource() }) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(27.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF767680).copy(alpha = 0.24f))
            .padding(2.dp)
    ) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * selected, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow), label = "seg")
        Box(
            Modifier
                .offset(x = x)
                .width(segment)
                .fillMaxHeight()
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .background(Color(0xFF636366), RoundedCornerShape(8.dp))
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
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
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = Ios.Label,
                    )
                }
            }
        }
    }
}

/** iOS "inset grouped" list section. */
@Composable
fun Section(header: String? = null, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        if (header != null) {
            Text(
                header.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Ios.Secondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 7.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Ios.Card),
            content = content,
        )
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = Ios.Secondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp),
            )
        }
    }
}

@Composable
fun IconTile(icon: ImageVector, color: Color, size: Dp = 30.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.24f))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.62f))
    }
}

@Composable
fun ListRow(
    title: String,
    icon: ImageVector? = null,
    iconColor: Color = Ios.Blue,
    subtitle: String? = null,
    titleColor: Color = Ios.Label,
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
                .padding(horizontal = 16.dp, vertical = if (subtitle != null) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                IconTile(icon, iconColor)
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Ios.Secondary)
                }
            }
            trailing?.invoke()
            if (chevron) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
                    tint = Ios.Tertiary, modifier = Modifier.size(22.dp),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                Modifier.padding(start = if (icon != null) 60.dp else 16.dp),
                thickness = 0.5.dp,
                color = Ios.Separator,
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    brush: Brush,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .bouncyClick(enabled, onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .clip(RoundedCornerShape(18.dp))
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, color: Color = Ios.Blue, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .bouncyClick(onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
fun StatTile(
    value: String,
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Card(modifier, padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
        }
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = Ios.Label, maxLines = 1)
    }
}
