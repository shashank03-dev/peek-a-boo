package dev.shashank.peekaboo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.shashank.peekaboo.R

/** iOS dark-mode system palette. */
object Ios {
    val Background = Color(0xFF000000)
    val Card = Color(0xFF1C1C1E)
    val CardElevated = Color(0xFF2C2C2E)
    val Fill = Color(0xFF3A3A3C)
    val Separator = Color(0x5C545458)
    val Label = Color(0xFFFFFFFF)
    val Secondary = Color(0x99EBEBF5)
    val Tertiary = Color(0x4DEBEBF5)
    val Red = Color(0xFFFF453A)
    val Pink = Color(0xFFFF375F)
    val Orange = Color(0xFFFF9F0A)
    val Yellow = Color(0xFFFFD60A)
    val Green = Color(0xFF30D158)
    val Mint = Color(0xFF66D4CF)
    val Teal = Color(0xFF40C8E0)
    val Blue = Color(0xFF0A84FF)
    val Indigo = Color(0xFF5E5CE6)
    val Purple = Color(0xFFBF5AF2)
    val Gray = Color(0xFF8E8E93)

    val GuardGradient = Brush.linearGradient(listOf(Color(0xFF34C759), Color(0xFF30B0C7)))
    val AlertGradient = Brush.linearGradient(listOf(Color(0xFFFF375F), Color(0xFFFF9F0A)))
    val IdleGradient = Brush.linearGradient(listOf(Color(0xFF5E5CE6), Color(0xFF0A84FF)))
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

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Black, fontSize = 64.sp, letterSpacing = (-0.04).em),
    displayMedium = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = (-0.03).em),
    headlineLarge = TextStyle(fontFamily = InterDisplay, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.02).em),
    titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = (-0.01).em),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = (-0.01).em),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 17.sp, letterSpacing = (-0.01).em),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, letterSpacing = (-0.005).em),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.02.em),
)

@Composable
fun PeekTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ios.Blue,
            secondary = Ios.Green,
            tertiary = Ios.Pink,
            background = Ios.Background,
            surface = Ios.Card,
            surfaceVariant = Ios.CardElevated,
            onPrimary = Color.White,
            onBackground = Ios.Label,
            onSurface = Ios.Label,
            onSurfaceVariant = Ios.Secondary,
            error = Ios.Red,
            outline = Ios.Separator,
        ),
        typography = typography,
        content = content,
    )
}
