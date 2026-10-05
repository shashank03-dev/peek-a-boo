package dev.shashank.peekaboo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.shashank.peekaboo.R

/**
 * "Night Watch" palette: a near-black void, frosted glass surfaces and a small set of luminous
 * signal colours. Each guard state owns one hue so the whole screen tells you what's happening.
 */
object Night {
    val Void = Color(0xFF05050A)
    val Deep = Color(0xFF0B0B14)
    val Glass = Color(0x0FFFFFFF)
    val GlassStrong = Color(0x1AFFFFFF)
    val GlassPressed = Color(0x24FFFFFF)
    val Stroke = Color(0x1AFFFFFF)
    val StrokeBright = Color(0x33FFFFFF)
    val Hairline = Color(0x12FFFFFF)

    val Text = Color(0xFFF4F4F8)
    val TextDim = Color(0x9EE9E9F2)
    val TextFaint = Color(0x5CE9E9F2)

    /** Guarding / safe. */
    val Mint = Color(0xFF4DFFC3)
    val Teal = Color(0xFF21D4FD)
    /** Standing by. */
    val Violet = Color(0xFF8C7BFF)
    val Indigo = Color(0xFF5468FF)
    /** A peek is happening. */
    val Hot = Color(0xFFFF3B6B)
    val Ember = Color(0xFFFF8A3D)
    /** Neutral warnings. */
    val Amber = Color(0xFFFFC94D)
    val Slate = Color(0xFF7B7F95)

    val SafeBrush = Brush.linearGradient(listOf(Mint, Teal))
    val IdleBrush = Brush.linearGradient(listOf(Violet, Indigo))
    val PeekBrush = Brush.linearGradient(listOf(Hot, Ember))
    val SlateBrush = Brush.linearGradient(listOf(Color(0xFF3A3D50), Color(0xFF262838)))
    val Iridescent = Brush.linearGradient(listOf(Mint, Teal, Violet, Hot))
}

/** The colours that follow the guard state through the aurora, eye, chips and buttons. */
@Immutable
data class Mood(val primary: Color, val secondary: Color, val tertiary: Color) {
    val brush get() = Brush.linearGradient(listOf(primary, secondary))

    companion object {
        val Off = Mood(Color(0xFFA9ADC8), Color(0xFF555A73), Color(0xFF2E2A45))
        val Idle = Mood(Night.Violet, Night.Indigo, Color(0xFF3A1F7A))
        val Safe = Mood(Night.Mint, Night.Teal, Night.Indigo)
        val Peek = Mood(Night.Hot, Night.Ember, Color(0xFF8A1FFF))
    }
}

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val InterDisplay = FontFamily(
    Font(R.font.interdisplay_bold, FontWeight.Bold),
    Font(R.font.interdisplay_black, FontWeight.Black),
)

val Mono = FontFamily(
    Font(R.font.jetbrainsmono_medium, FontWeight.Medium),
    Font(R.font.jetbrainsmono_bold, FontWeight.Bold),
)

/** Small uppercase monospace label used for eyebrows, timestamps and units. */
val Eyebrow = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.14.em)
val MonoValue = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.02.em)

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Black, fontSize = 88.sp, lineHeight = 88.sp, letterSpacing = (-0.055).em),
    displayMedium = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Black, fontSize = 52.sp, lineHeight = 54.sp, letterSpacing = (-0.045).em),
    displaySmall = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-0.04).em),
    headlineLarge = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.03).em),
    headlineMedium = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.025).em),
    titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = (-0.015).em),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = (-0.01).em),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp, letterSpacing = (-0.01).em),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = (-0.005).em),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = (-0.01).em),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.02.em),
)

@Composable
fun PeekTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Night.Mint,
            secondary = Night.Violet,
            tertiary = Night.Hot,
            background = Night.Void,
            surface = Night.Deep,
            surfaceVariant = Color(0xFF15151F),
            surfaceContainer = Color(0xFF12121B),
            surfaceContainerHigh = Color(0xFF181823),
            onPrimary = Night.Void,
            onBackground = Night.Text,
            onSurface = Night.Text,
            onSurfaceVariant = Night.TextDim,
            error = Night.Hot,
            outline = Night.Stroke,
        ),
        typography = typography,
        content = content,
    )
}
