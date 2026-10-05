package dev.shashank.peekaboo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.shashank.peekaboo.R

/**
 * Palette: one warm graphite neutral ramp and exactly two signal colours.
 *
 *  - [Accent] (lime) means "on / you / go": the running guard, your face, primary actions, switches.
 *  - [Alert] (red) means "someone is looking": peeks, peek counts, destructive actions.
 *
 * Everything else is neutral. No gradients, no glows: hierarchy comes from the surface steps
 * (Bg → Surface → Raised) and the three text tones.
 */
object Ink {
    val Bg = Color(0xFF0E0E0C)
    val Surface = Color(0xFF171715)
    val Raised = Color(0xFF22221F)
    val Sunken = Color(0xFF2C2C28)
    val Line = Color(0xFF282825)
    val LineStrong = Color(0xFF3A3A35)

    val Text = Color(0xFFF2F0EA)
    val TextMuted = Color(0xFFA3A099)
    val TextFaint = Color(0xFF6E6C66)

    val Accent = Color(0xFFD4F25A)
    val OnAccent = Color(0xFF151A04)
    val AccentSoft = Color(0x1FD4F25A)

    val Alert = Color(0xFFFF5640)
    val OnAlert = Color(0xFF1F0603)
    val AlertSoft = Color(0x24FF5640)
}

/** The single colour that represents the guard's state on the eye, status chip and live dot. */
@Immutable
data class Mood(val color: Color) {
    companion object {
        val Off = Mood(Ink.TextFaint)
        val Idle = Mood(Ink.TextMuted)
        val Safe = Mood(Ink.Accent)
        val Peek = Mood(Ink.Alert)
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

/** Small uppercase monospace label for section names, timestamps and units. */
val Eyebrow = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.08.em)
val MonoValue = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 13.sp)

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 72.sp, lineHeight = 72.sp, letterSpacing = (-0.045).em),
    displayMedium = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 46.sp, lineHeight = 48.sp, letterSpacing = (-0.04).em),
    displaySmall = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.03).em),
    headlineLarge = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.025).em),
    headlineMedium = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.02).em),
    titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = (-0.015).em),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp, letterSpacing = (-0.01).em),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp, letterSpacing = (-0.01).em),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = (-0.01).em),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
)

@Composable
fun PeekTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ink.Accent,
            onPrimary = Ink.OnAccent,
            secondary = Ink.TextMuted,
            tertiary = Ink.Alert,
            background = Ink.Bg,
            surface = Ink.Surface,
            surfaceVariant = Ink.Raised,
            surfaceContainer = Ink.Surface,
            surfaceContainerHigh = Ink.Raised,
            surfaceContainerLow = Ink.Surface,
            onBackground = Ink.Text,
            onSurface = Ink.Text,
            onSurfaceVariant = Ink.TextMuted,
            error = Ink.Alert,
            outline = Ink.Line,
            outlineVariant = Ink.Line,
        ),
        typography = typography,
        content = content,
    )
}
