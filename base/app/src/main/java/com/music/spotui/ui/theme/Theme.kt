package com.music.spotui.ui.theme

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Spotify's dark palette, so the Material components that read the scheme (switches,
 * sliders, dialogs, spinners, text fields, menus) look like the official app: accent
 * green on black, #121212 background, #1F1F1F/#282828 raised surfaces, #B3B3B3
 * secondary text, and no tonal tint on elevated surfaces.
 */
private fun spotifyColorScheme(accent: Color, background: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color.Black,
    primaryContainer = accent,
    onPrimaryContainer = Color.Black,
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

@Composable
fun SpotuiTheme(
    content: @Composable () -> Unit
) {
    val energy by app.myspoti.power.EnergySettings.state.collectAsStateWithLifecycle()
    val policy = energy.policy
    androidx.compose.runtime.LaunchedEffect(policy.videoMaxHeight, policy.allowBackgroundLookups) {
        com.music.spotui.video.MusicVideoController.onEnergyPolicyChanged()
    }
    // A new wallpaper recreates the activity: its accent is read again (Material You, see DynamicColorPref).
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    androidx.compose.runtime.remember(configuration) { com.music.spotui.data.preferences.DynamicColorPref.refresh() }
    // Dark only, like Spotify: a saved choice from the withdrawn light palette is ignored.
    // AppBackground may be Color.Transparent when the user picked a background image; feed the
    // scheme a solid black in that case so Material surfaces keep readable contrast.
    val imageBg = com.music.spotui.data.preferences.BackgroundImagePref.active
    val schemeBg = if (imageBg) Color.Black else AppBackground
    val visualScheme: ColorScheme = spotifyColorScheme(AppAccent, schemeBg)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.attributes = window.attributes.apply { preferredRefreshRate = policy.displayRefreshCap }
            window.statusBarColor = Color.Transparent.toArgb()
            // White icons on the dark palette.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            // B19: the navigation bar was left to the system — a white band under the always-dark
            // UI (reported in the modal sheets, where the bar was opaque) with dark icons on it.
            window.navigationBarColor = Color.Transparent.toArgb()
            // Without this Android lays its own translucent scrim on a transparent bar (API 29+),
            // which showed as a differently coloured strip under the full-screen Canvas.
            window.isNavigationBarContrastEnforced = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            // Behind the first frame and under overscroll: the palette's page colour (always a solid
            // colour here; the custom image is drawn on top of it by AppBackdrop).
            val windowBg = if (imageBg) Color.Black else AppBackground
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(windowBg.toArgb()))
        }
    }

    MaterialTheme(
        colorScheme = visualScheme,
        typography = Typography,
    ) {
        // Bare Text() uses LocalTextStyle, which MaterialTheme does NOT derive
        // from the typography — so provide Montserrat as the global default so
        // every screen picks it up without touching each Text call.
        // An extension's look may make the text a little smaller or larger (ui.styles.text.scale).
        val density = androidx.compose.ui.platform.LocalDensity.current
        val scale = com.music.spotui.extensions.ExtensionLook.textScale
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides
                if (scale == 1f) density else androidx.compose.ui.unit.Density(density.density, density.fontScale * scale),
        ) {
            androidx.compose.material3.ProvideTextStyle(
                value = androidx.compose.material3.LocalTextStyle.current.copy(
                    fontFamily = Montserrat,
                    letterSpacing = (-0.2).sp,
                ),
                content = {
                    // The user's chosen background image, dimmed, sits behind every screen. Without
                    // an image the content is drawn exactly as before (no extra wrapper).
                    if (imageBg) {
                        Box(Modifier.fillMaxSize()) {
                            com.music.spotui.ui.components.AppBackdrop()
                            content()
                        }
                    } else content()
                },
            )
        }
    }
}
