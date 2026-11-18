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
 * The bottom layer of every screen: the user's chosen background image, dimmed by the Settings
 * slider so text stays readable. Rendered behind the Compose tree so no existing screen needs to
 * know it exists. When no image is set this composable draws nothing.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AppBackdrop(modifier: Modifier = Modifier) {
    val uri = BackgroundImagePref.uri ?: return
    val dim = BackgroundImagePref.dim
    Box(modifier.fillMaxSize()) {
        GlideImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
    }
}
