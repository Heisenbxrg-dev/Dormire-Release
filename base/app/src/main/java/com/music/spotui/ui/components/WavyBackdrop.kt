package com.music.spotui.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.music.spotui.ui.theme.AppAccent
import kotlin.math.exp
import kotlin.math.sin

/**
 * Dormire's page: black with embossed, flowing contour lines (the texture of the app icon) and a
 * slow breathing glow in the accent colour. The lines are built once per size (drawWithCache) and
 * only the glow layer animates, so it costs almost nothing while scrolling.
 */
@Composable
fun WavyBackdrop(modifier: Modifier = Modifier, glow: Boolean = true) {
    val accent = AppAccent
    Box(modifier.fillMaxSize().background(Color(0xFF050505))) {
        Spacer(
            Modifier.fillMaxSize().drawWithCache {
                val w = size.width
                val h = size.height
                val lines = 40
                val step = h / lines
                val paths = (0..lines + 2).map { i ->
                    Path().apply {
                        val base = (i - 1) * step
                        var x = -24f
                        moveTo(x, waveY(x, base, i, w, h))
                        while (x <= w + 24f) {
                            x += 10f
                            lineTo(x, waveY(x, base, i, w, h))
                        }
                    }
                }
                val shadow = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                val body = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                val highlight = Stroke(width = 0.6.dp.toPx(), cap = StrokeCap.Round)
                val down = 2.dp.toPx()
                val up = -0.9.dp.toPx()
                onDrawBehind {
                    for (p in paths) {
                        translate(top = down) { drawPath(p, Color.Black.copy(alpha = 0.85f), style = shadow) }
                        drawPath(p, Color(0xFF191919), style = body)
                        translate(top = up) { drawPath(p, Color(0xFF2E2E2E), style = highlight) }
                    }
                }
            }
        )
        if (glow) {
            val pulse = rememberInfiniteTransition(label = "dormire-glow")
                .animateFloat(
                    initialValue = 0.55f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                    label = "dormire-glow-alpha",
                )
            Spacer(
                Modifier.fillMaxSize()
                    .graphicsLayer { alpha = pulse.value }
                    .drawBehind {
                        drawRect(Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.20f), accent.copy(alpha = 0.05f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 1.02f),
                            radius = size.width * 1.05f,
                        ))
                        drawRect(Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.95f, size.height * 0.08f),
                            radius = size.width * 0.7f,
                        ))
                    }
            )
        }
    }
}

/** Flowing contour lines: two sines plus a soft swirl, like the grooves in the icon. */
private fun waveY(x: Float, base: Float, i: Int, w: Float, h: Float): Float {
    val nx = x / w
    val ny = base / h
    val dx = nx - 0.68f
    val dy = ny - 0.38f
    val swirl = exp(-(dx * dx + dy * dy) * 7f) * 0.05f * h * sin(nx * 9f + i * 0.25f)
    return base +
        sin(nx * 6.2f + i * 0.32f) * h * 0.018f +
        sin(nx * 13f - i * 0.21f + 1.7f) * h * 0.006f +
        swirl
}

/** Where the finger last touched the screen: the backdrop answers with a soft glow there. */
object BackdropTouch {
    var point by mutableStateOf(Offset.Unspecified)
    var stamp by mutableLongStateOf(0L)

    fun touched(at: Offset) {
        point = at
        stamp = System.nanoTime()
    }
}

/** A glow in the accent colour where the finger lands, fading out in about a second. */
@Composable
fun TouchGlow() {
    val accent = AppAccent
    val p = BackdropTouch.point
    val stamp = BackdropTouch.stamp
    val fade = remember { Animatable(0f) }
    LaunchedEffect(stamp) {
        if (stamp == 0L) return@LaunchedEffect
        fade.snapTo(1f)
        fade.animateTo(0f, tween(1100, easing = FastOutSlowInEasing))
    }
    Spacer(
        Modifier.fillMaxSize().drawBehind {
            val v = fade.value
            if (v > 0f && p.isSpecified) {
                val r = 200.dp.toPx() * (1.3f - 0.3f * v)
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.28f * v), Color.Transparent), center = p, radius = r),
                    radius = r, center = p,
                )
            }
        }
    )
}
