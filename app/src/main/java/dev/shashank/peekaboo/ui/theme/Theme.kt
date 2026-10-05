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
 * Palette: one cool graphite neutral ramp and two soft signal colours.
 *
 *  - [Accent] (periwinkle) means "on / you / go": the running guard, your face, primary actions,
 *    switches.
 *  - [Alert] (dusty coral) means "someone is looking": peeks, peek counts, destructive actions.
 *
 * Both are deliberately desaturated so the screen stays calm; everything else is neutral, and
 * hierarchy comes from the surface steps (Bg → Surface → Raised) and the three text tones.
 */
object Ink {
    val Bg = Color(0xFF0F0F12)
    val Surface = Color(0xFF18181C)
    val Raised = Color(0xFF222228)
    val Sunken = Color(0xFF2C2C33)
    val Line = Color(0xFF26262C)
    val LineStrong = Color(0xFF383840)

    val Text = Color(0xFFF1F0F5)
    val TextMuted = Color(0xFFA4A3AE)
    val TextFaint = Color(0xFF706F7A)

    val Accent = Color(0xFFAEB6FF)
    val OnAccent = Color(0xFF161936)
    val AccentSoft = Color(0x24AEB6FF)

    val Alert = Color(0xFFEE8C7C)
    val OnAlert = Color(0xFF2A0C07)
    val AlertSoft = Color(0x24EE8C7C)
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

/**
 * Type scale: four sizes, three weights.
 *  56 Bold display  — hero numbers
 *  30 Bold display  — screen titles, hero status
 *  15 Regular / SemiBold — body, row titles, buttons
 *  12 Regular / SemiBold — captions, labels, metadata
 * Numbers use tabular figures so counts don't jitter as they change.
 */
private val Display = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
private val Hero = Display.copy(fontSize = 56.sp, lineHeight = 60.sp, letterSpacing = (-0.03).em)
private val Title = Display.copy(fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.025).em)
private val Body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em)
private val BodyStrong = Body.copy(fontWeight = FontWeight.SemiBold)
private val Caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)

/** Small uppercase monospace label for section names and timestamps (caption size). */
val Eyebrow = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.06.em)
val MonoValue = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 12.sp)

private val typography = Typography(
    displayLarge = Hero,
    displayMedium = Hero,
    displaySmall = Title,
    headlineLarge = Title,
    headlineMedium = Title,
    headlineSmall = Title,
    titleLarge = BodyStrong,
    titleMedium = BodyStrong,
    titleSmall = BodyStrong,
    bodyLarge = Body,
    bodyMedium = Body,
    bodySmall = Caption,
    labelLarge = BodyStrong,
    labelMedium = Caption.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = Caption.copy(fontWeight = FontWeight.SemiBold),
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
