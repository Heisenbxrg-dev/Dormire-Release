package com.music.spotui.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun AppBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().graphicsLayer()) {
        WavyBackdrop()
        TouchGlow()
    }
}
