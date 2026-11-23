package com.music.spotui.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppBackground: Color = Color.Black
val AppPage: Color = Color.Transparent
val GridBackground: Color = Color(0xFF2A2A2A)
val AppAccent: Color = Color(0xFFE50914)
val AppPalette: Color = AppAccent
val AppText: Color = Color.White
val AppMuted: Color = Color(0xFFB3B3B3)
val AppDestructive: Color = Color(0xFFF15E6C)
val AppShade: Color = Color.Black

fun tone(argb: Long): Color = Color(argb)
fun headerTint(tint: Color, amount: Float): Color = lerp(Color(0xFF7A1019), Color.Black, amount)

private fun spotifyColorScheme(accent: Color, background: Color) = darkColorScheme(
    primary = accent,
    onPrimary = if (accent.luminance() > 0.55f) Color.Black else Color.White,
    primaryContainer = accent,
    onPrimaryContainer = if (accent.luminance() > 0.55f) Color.Black else Color.White,
    secondary = Color.White,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color.White,
    tertiary = accent,
    onTertiary = Color.Black,
    background = background,
    onBackground = Color.White,
    surface = background,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB3B3B3),
    surfaceTint = Color.Transparent,
    surfaceDim = background,
    surfaceBright = Color(0xFF2A2A2A),
    surfaceContainerLowest = background,
    surfaceContainerLow = Color(0xFF181818),
    surfaceContainer = Color(0xFF1F1F1F),
    surfaceContainerHigh = Color(0xFF282828),
    surfaceContainerHighest = Color(0xFF3E3E3E),
    inverseSurface = Color.White,
    inverseOnSurface = Color.Black,
    inversePrimary = accent,
    outline = Color(0xFF727272),
    outlineVariant = Color(0xFF3E3E3E),
    error = Color(0xFFF3727F),
    onError = Color.Black,
    scrim = Color.Black,
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp, lineHeight = 24.sp),
)

@Composable
fun SpotuiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = spotifyColorScheme(AppAccent, AppBackground),
        typography = AppTypography,
    ) {
        ProvideTextStyle(
            value = LocalTextStyle.current.copy(letterSpacing = (-0.2).sp),
            content = content,
        )
    }
}
