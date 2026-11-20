package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Dormire's solid "ink" black: text on accent buttons, icon tints, rows that must hide what is
 * under them (swipe actions). Pages themselves use [AppPage] so the wavy backdrop shows through.
 */
val AppBackground: Color get() =
    com.music.spotui.extensions.ExtensionLook.color(app.myspoti.extensions.UiStyle.BACKGROUND) ?: Color.Black

/**
 * The colour a whole page paints under its content: transparent, so the app-wide backdrop
 * (Dormire's wavy black, or the user's image) is visible behind every screen.
 */
val AppPage: Color get() = Color.Transparent

val GridBackground: Color get() = com.music.spotui.extensions.ExtensionLook.color(app.myspoti.extensions.UiStyle.SURFACE) ?: tone(0xFF2A2A2A)

/**
 * The accent (buttons, active tabs, sliders, glows).
 * Priority: Dormire custom accent (red by default) -> extension look -> Material You -> stock preset.
 */
val AppPalette: Color get() = AppAccent
val AppAccent: Color get() {
    val custom = com.music.spotui.data.preferences.CustomAccentPref
    if (custom.enabled) return custom.color
    return com.music.spotui.extensions.ExtensionLook.color(app.myspoti.extensions.UiStyle.ACCENT)
        ?: com.music.spotui.data.preferences.DynamicColorPref.accent()
        ?: com.music.spotui.data.api.SupportReminder.accentColor
}

/** Primary text and icons. */
val AppText: Color = Color.White
/** Secondary text and icons. */
val AppMuted: Color = Color(0xFFB3B3B3)
/** Destructive actions (delete, leave, block) and failures shown as text. */
val AppDestructive = Color(0xFFF15E6C)
/** What a tint is mixed with to make a header. */
val AppShade: Color = Color.Black

/** One of the palette's raised greys (#181818 ... #3E3E3E): "a bit raised from the page". */
fun tone(argb: Long): Color = Color(argb)

/** The cover's colour as a page header: darkened toward black, so the title stays readable. */
fun headerTint(tint: Color, amount: Float): Color = lerp(Color(0xFF7A1019), Color.Black, amount)
