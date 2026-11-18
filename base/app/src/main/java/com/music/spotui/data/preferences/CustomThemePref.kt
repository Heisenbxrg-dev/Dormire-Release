package com.music.spotui.data.preferences

/*
 * Dormire custom theme: the user picks a pair of colours (accent + surface tint) and, optionally,
 * an image used as the app's background. All three are kept as Compose state so every screen
 * redraws when one changes. When nothing is set the app falls back to the stock palette.
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

/**
 * The accent colour (buttons, active tabs, highlights) chosen in Settings. Defaults to
 * Dormire's signature red. Overrides the Material You dynamic colour and the stock accent
 * palette when [enabled].
 */
object CustomAccentPref {
    private const val KEY_ENABLED = "custom_accent_enabled"
    private const val KEY_COLOR = "custom_accent_color"

    private const val DEFAULT = 0xFFE50914.toInt()

    var enabled by mutableStateOf(prefs(com.music.spotui.di.Graph.app).getBoolean(KEY_ENABLED, true))
        private set

    var color by mutableStateOf(Color(prefs(com.music.spotui.di.Graph.app).getInt(KEY_COLOR, DEFAULT)))
        private set

    fun setEnabled(c: Context, value: Boolean) {
        enabled = value
        prefs(c).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun setColor(c: Context, newColor: Color) {
        color = newColor
        prefs(c).edit().putInt(KEY_COLOR, newColor.toArgb()).apply()
    }
}

/**
 * The page background colour chosen in Settings. Defaults to pure black. Only used when
 * [enabled] AND no [BackgroundImagePref] is set.
 */
object CustomBackgroundPref {
    private const val KEY_ENABLED = "custom_bg_enabled"
    private const val KEY_COLOR = "custom_bg_color"

    private const val DEFAULT = 0xFF000000.toInt()

    var enabled by mutableStateOf(prefs(com.music.spotui.di.Graph.app).getBoolean(KEY_ENABLED, true))
        private set

    var color by mutableStateOf(Color(prefs(com.music.spotui.di.Graph.app).getInt(KEY_COLOR, DEFAULT)))
        private set

    fun setEnabled(c: Context, value: Boolean) {
        enabled = value
        prefs(c).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun setColor(c: Context, newColor: Color) {
        color = newColor
        prefs(c).edit().putInt(KEY_COLOR, newColor.toArgb()).apply()
    }
}

/**
 * The image picked by the user in Settings, drawn behind every screen. Content URI, persisted
 * via content-resolver permission so it survives reboots. Clearing it goes back to a flat colour.
 */
object BackgroundImagePref {
    private const val KEY_URI = "custom_bg_image"
    private const val KEY_DIM = "custom_bg_image_dim"

    var uri by mutableStateOf(
        prefs(com.music.spotui.di.Graph.app).getString(KEY_URI, null)?.let { runCatching { Uri.parse(it) }.getOrNull() }
    )
        private set

    /** 0f = image untouched, 1f = fully dark overlay. Default 0.45f so text stays readable. */
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

    /** True when the user picked an image. */
    val active: Boolean get() = uri != null
}
