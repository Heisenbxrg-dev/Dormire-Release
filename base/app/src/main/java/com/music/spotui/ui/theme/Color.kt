package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * The page background colour.
 *
 * Priority (high to low):
 *  1. A background image picked by the user -> transparent (the image is drawn by AppBackdrop).
 *  2. The Dormire custom background colour (Settings -> Colori app).
 *  3. The AMOLED black setting.
 *  4. An extension's look background.
 *  5. The default #121212.
 */
val AppBackground: Color get() {
    if (com.music.spotui.data.preferences.BackgroundImagePref.active) return Color.Transparent
    val custom = com.music.spotui.data.preferences.CustomBackgroundPref
    if (custom.enabled) return custom.color
    if (app.myspoti.power.EnergySettings.oledBlack) return Color.Black
    return com.music.spotui.extensions.ExtensionLook.color(app.myspoti.extensions.UiStyle.BACKGROUND) ?: Color(0xFF121212)
}
val GridBackground: Color get() = com.music.spotui.extensions.ExtensionLook.color(app.myspoti.extensions.UiStyle.SURFACE) ?: tone(0xFF2A2A2A)
/**
 * The accent (buttons, active tabs, sliders).
 *
 * Priority (high to low):
 *  1. The Dormire custom accent (Settings -> Colori app) -> red by default.
 *  2. An extension's look accent.
 *  3. Material You wallpaper accent (see DynamicColorPref).
 *  4. The stock accent chosen from the preset palette.
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
fun headerTint(tint: Color, amount: Float): Color = lerp(tint, Color.Black, amount)
