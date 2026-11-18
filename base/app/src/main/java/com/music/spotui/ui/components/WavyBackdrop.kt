package com.music.spotui.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.music.spotui.ui.theme.AppAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.sin

/*
 * Dormire's page: black with embossed, flowing contour lines (the texture of the app icon) and a
 * soft accent glow.
 *
 * Performance: the lines are painted ONCE into a bitmap on a background thread and cached per
 * size; on screen the backdrop is a single bitmap + two gradient rects in its own layer, with no
 * running animation. Scrolling and page changes never redraw it.
 */

private object WavyCache {
    private val map = LinkedHashMap<Long, ImageBitmap>()

    private fun key(w: Int, h: Int) = (w.toLong() shl 32) or h.toLong()

    @Synchronized fun peek(w: Int, h: Int): ImageBitmap? = map[key(w, h)]

    @Synchronized fun put(w: Int, h: Int, b: ImageBitmap) {
        map[key(w, h)] = b
        // The screen in both orientations plus the small preview card is all we ever need.
        while (map.size > 3) map.remove(map.keys.first())
    }
}

private fun renderWaves(w: Int, h: Int, density: Float): ImageBitmap {
    WavyCache.peek(w, h)?.let { return it }
    val bmp = ImageBitmap(w, h, ImageBitmapConfig.Argb8888)
    val fw = w.toFloat()
    val fh = h.toFloat()
    CanvasDrawScope().draw(Density(density), LayoutDirection.Ltr, Canvas(bmp), Size(fw, fh)) {
        drawRect(Color(0xFF050505))
        val lines = 40
        val step = fh / lines
        val shadow = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        val body = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
        val highlight = Stroke(width = 0.6.dp.toPx(), cap = StrokeCap.Round)
        val down = 2.dp.toPx()
        val up = -0.9.dp.toPx()
        for (i in 0..lines + 2) {
            val base = (i - 1) * step
            val p = Path()
            var x = -24f
            p.moveTo(x, waveY(x, base, i, fw, fh))
            while (x <= fw + 24f) {
                x += 12f
                p.lineTo(x, waveY(x, base, i, fw, fh))
            }
            translate(top = down) { drawPath(p, Color.Black.copy(alpha = 0.85f), style = shadow) }
            drawPath(p, Color(0xFF191919), style = body)
            translate(top = up) { drawPath(p, Color(0xFF2E2E2E), style = highlight) }
        }
    }
    bmp.prepareToDraw()
    WavyCache.put(w, h, bmp)
    return bmp
}

@Composable
fun WavyBackdrop(modifier: Modifier = Modifier, glow: Boolean = true) {
    val accent = AppAccent
    val density = LocalDensity.current.density
    BoxWithConstraints(modifier.fillMaxSize().graphicsLayer().background(Color(0xFF050505))) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        if (w > 0 && h > 0 && w < 100_000 && h < 100_000) {
            val waves by produceState(WavyCache.peek(w, h), w, h) {
                // Size changed (rotation, split screen, resize): redraw instead of stretching the old image.
                val cur = value
                if (cur == null || cur.width != w || cur.height != h) {
                    value = WavyCache.peek(w, h) ?: withContext(Dispatchers.Default) { renderWaves(w, h, density) }
                }
            }
            waves?.let {
                Image(it, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            }
        }
        if (glow) {
            // Static glow: two gradient rects, no animation (nothing redraws while the app runs).
            Spacer(
                Modifier.fillMaxSize().drawBehind {
                    drawRect(Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 1.02f),
                        radius = size.width * 1.05f,
                    ))
                    drawRect(Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.09f), Color.Transparent),
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

/**
 * A glow in the accent colour where the finger lands, fading out in under a second. Lives in its
 * own layer and only draws while fading, so it never makes the pages above it redraw.
 */
@Composable
fun TouchGlow() {
    val accent = AppAccent
    val p = BackdropTouch.point
    val stamp = BackdropTouch.stamp
    val fade = remember { Animatable(0f) }
    LaunchedEffect(stamp) {
        if (stamp == 0L) return@LaunchedEffect
        fade.snapTo(1f)
        fade.animateTo(0f, tween(800, easing = FastOutSlowInEasing))
    }
    Spacer(
        Modifier.fillMaxSize().graphicsLayer().drawBehind {
            val v = fade.value
            if (v > 0f && p.isSpecified) {
                val r = 170.dp.toPx() * (1.3f - 0.3f * v)
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.26f * v), Color.Transparent), center = p, radius = r),
                    radius = r, center = p,
                )
            }
        }
    )
}
