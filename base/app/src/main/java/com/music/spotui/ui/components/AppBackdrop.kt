package com.music.spotui.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.data.preferences.BackgroundImagePref

/**
 * The bottom layer of the app, under every page (pages paint AppPage = transparent):
 * Dormire's wavy black, or the user's image dimmed by the Settings slider. A soft accent glow
 * answers each touch.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AppBackdrop(modifier: Modifier = Modifier) {
    val uri = BackgroundImagePref.uri
    Box(modifier.fillMaxSize()) {
        if (uri == null) {
            WavyBackdrop()
        } else {
            Box(Modifier.fillMaxSize().background(Color.Black))
            GlideImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = BackgroundImagePref.dim)))
        }
        TouchGlow()
    }
}
