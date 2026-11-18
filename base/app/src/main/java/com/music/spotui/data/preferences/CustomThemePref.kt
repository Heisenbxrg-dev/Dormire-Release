package com.music.spotui.data.preferences

/*
 * Dormire custom theme: the accent colour (red by default) and, optionally, an image used as the
 * app's background. The page itself is always Dormire's wavy black (see WavyBackdrop) unless an
 * image is picked. Kept as Compose state so every screen redraws when one changes.
 */

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

private const val PREF = "dormire_theme"

private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

/** Dormire's signature red. */
val DORMIRE_RED = Color(0xFFE50914)

/**
 * The accent colour (buttons, active tabs, sliders, glows) chosen in Settings. Overrides the
 * Material You colour and the stock accent palette while [enabled].
 */
object CustomAccentPref {
    private const val KEY_ENABLED = "custom_accent_enabled"
    private const val KEY_COLOR = "custom_accent_color"

    var enabled by mutableStateOf(prefs(com.music.spotui.di.Graph.app).getBoolean(KEY_ENABLED, true))
        private set

    var color by mutableStateOf(Color(prefs(com.music.spotui.di.Graph.app).getInt(KEY_COLOR, DORMIRE_RED.toArgb())))
        private set

    fun setEnabled(c: Context, value: Boolean) {
        enabled = value
        prefs(c).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    /** Live change while the finger slides on the picker (not saved yet). */
    fun preview(newColor: Color) {
        color = newColor
    }

    fun setColor(c: Context, newColor: Color) {
        color = newColor
        prefs(c).edit().putInt(KEY_COLOR, newColor.toArgb()).apply()
    }
}

/**
 * The image picked by the user in Settings, drawn behind every screen instead of the wavy black.
 * Content URI with a persisted read permission so it survives reboots.
 */
object BackgroundImagePref {
    private const val KEY_URI = "custom_bg_image"
    private const val KEY_DIM = "custom_bg_image_dim"

    var uri by mutableStateOf(
        prefs(com.music.spotui.di.Graph.app).getString(KEY_URI, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }
    )
        private set

    /** 0f = image untouched, 0.9f = almost black. Default 0.45f so text stays readable. */
    var dim by mutableStateOf(prefs(com.music.spotui.di.Graph.app).getFloat(KEY_DIM, 0.45f))
        private set

    fun set(c: Context, newUri: Uri?) {
        uri = newUri
        val editor = prefs(c).edit()
        if (newUri == null) editor.remove(KEY_URI) else editor.putString(KEY_URI, newUri.toString())
        editor.apply()
    }

    fun setDim(c: Context, newDim: Float) {
        dim = newDim.coerceIn(0f, 1f)
        prefs(c).edit().putFloat(KEY_DIM, dim).apply()
    }

    val active: Boolean get() = uri != null
}
