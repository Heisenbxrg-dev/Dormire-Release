package com.music.spotui.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Cinzel (OFL): classic engraved capitals for the title. */
private val TitleFont = FontFamily(Font(R.font.dormire_cinzel, FontWeight.ExtraBold))
/** Cormorant Garamond Italic (OFL): an elegant italic for the tagline. */
private val TaglineFont = FontFamily(Font(R.font.dormire_cormorant_italic, FontWeight.Medium, FontStyle.Italic))

private const val TOTAL_MS = 4000L
private const val FADE_OUT_MS = 450

/**
 * Dormire's launch screen: 4 seconds of "DORMIRE" burning over the wavy black, with the tagline
 * under it, then a fade into the app. Touches are swallowed while it is up.
 *
 * Kept light: the backdrop is the cached bitmap, the glows only change layer alpha/scale, and
 * just the title-sized area redraws each frame.
 */
@Composable
fun DormireSplash(onFinished: () -> Unit) {
    val screen = remember { Animatable(1f) }
    val title = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val ornament = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { title.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
        launch { delay(700); ornament.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        launch { delay(1000); tagline.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
        delay(TOTAL_MS - FADE_OUT_MS)
        screen.animateTo(0f, tween(FADE_OUT_MS))
        onFinished()
    }

    val heat = rememberInfiniteTransition(label = "heat")
    val pulse = heat.animateFloat(0.75f, 1f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "heat-pulse")

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screen.value }
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        WavyBackdrop(glow = false)
        // Deep vignette so the title is the only bright thing on screen.
        Spacer(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.radialGradient(
                listOf(Color.Transparent, Color(0xCC000000)),
                center = Offset(size.width / 2f, size.height * 0.48f),
                radius = size.maxDimension * 0.62f,
            ))
        })
        // Heat behind the title: drawn once, only its layer alpha/scale breathe.
        Spacer(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    alpha = pulse.value * title.value
                    scaleX = 0.92f + 0.08f * pulse.value
                    scaleY = scaleX
                }
                .drawBehind {
                    drawRect(Brush.radialGradient(
                        listOf(Color(0x66FF3A00), Color(0x22E50914), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.47f),
                        radius = size.width * 0.62f,
                    ))
                },
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.graphicsLayer {
                alpha = title.value
                // Flames and embers draw past the title bounds: fade without clipping them.
                compositingStrategy = CompositingStrategy.ModulateAlpha
                val s = 1.12f - 0.12f * title.value
                scaleX = s; scaleY = s
                translationY = (1f - title.value) * 24f
            }) { FireTitle("DORMIRE") }

            Ornament(progress = ornament)

            Text(
                "Il sonno profondo prima di GtaVI",
                style = TextStyle(
                    fontFamily = TaglineFont,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    fontSize = 19.sp,
                    letterSpacing = 0.6.sp,
                    color = Color(0xFFEADBC8),
                    shadow = Shadow(Color(0x99000000), Offset(0f, 2f), 6f),
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.padding(horizontal = 32.dp).graphicsLayer {
                    alpha = tagline.value
                    translationY = (1f - tagline.value) * 16f
                },
            )
        }
    }
}

/** A thin line – diamond – line under the title, growing out from the centre. */
@Composable
private fun Ornament(progress: Animatable<Float, *>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(top = 10.dp, bottom = 12.dp).graphicsLayer { alpha = progress.value },
    ) {
        Box(Modifier.width(72.dp).height(1.dp).graphicsLayer { scaleX = progress.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f) }
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFFF6A1A)))))
        Box(Modifier.padding(horizontal = 10.dp).size(6.dp).rotate(45f).background(Color(0xFFFFB300)))
        Box(Modifier.width(72.dp).height(1.dp).graphicsLayer { scaleX = progress.value; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }
            .background(Brush.horizontalGradient(listOf(Color(0xFFFF6A1A), Color.Transparent))))
    }
}

/** "DORMIRE" burning: crisp engraved letters filled with a flowing fire gradient, a soft flicker, small flames and embers. */
@Composable
private fun FireTitle(text: String) {
    val fire = rememberInfiniteTransition(label = "fire")
    val t = fire.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "fire-t")
    val flicker = fire.animateFloat(
        0.85f, 0.85f,
        infiniteRepeatable(keyframes {
            durationMillis = 1100
            0.85f at 0; 1f at 140; 0.88f at 320; 0.97f at 520; 0.82f at 720; 1f at 900; 0.85f at 1100
        }),
        label = "fire-flicker",
    )
    val style = TextStyle(fontFamily = TitleFont, fontWeight = FontWeight.ExtraBold, fontSize = 50.sp, letterSpacing = 7.sp)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 30.dp, start = 16.dp, end = 16.dp)) {
        // Small, soft flames licking the top of the letters (drawn past the text bounds on purpose).
        Canvas(Modifier.matchParentSize().graphicsLayer()) {
            val tongues = 14
            val slot = size.width / tongues
            val baseY = size.height * 0.55f
            for (i in 0 until tongues) {
                val phase = t.value * 1.4f + i * 0.161f
                val flameH = size.height * (0.35f + 0.4f * abs(sin(2f * PI.toFloat() * phase))) * (0.85f + 0.15f * sin(i * 1.7f))
                val cx = slot * (i + 0.5f)
                val half = slot * 0.6f
                val sway = sin(2f * PI.toFloat() * (t.value * 2f + i * 0.31f)) * half * 0.5f
                val tipY = baseY - flameH
                val path = Path().apply {
                    moveTo(cx - half, baseY)
                    quadraticTo(cx - half * 0.6f, baseY - flameH * 0.55f, cx + sway, tipY)
                    quadraticTo(cx + half * 0.6f, baseY - flameH * 0.55f, cx + half, baseY)
                    close()
                }
                drawPath(path, Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0x66E50914), Color(0x99FF5A00), Color(0x88FFB300)),
                    startY = tipY, endY = baseY,
                ), alpha = 0.45f * flicker.value)
            }
        }
        // Soft glow: static text with a blurred shadow; only its layer alpha flickers (no redraw).
        Text(
            text,
            style = style.copy(color = Color(0xFFFF3300), shadow = Shadow(Color(0xDDFF3300), Offset.Zero, blurRadius = 36f)),
            modifier = Modifier.graphicsLayer {
                alpha = flicker.value
                // No offscreen buffer: the blurred glow must not be clipped to the text bounds.
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
        )
        // Crisp letters with a fire gradient flowing upward, and a dark-red edge for depth.
        Text(
            text,
            style = style.copy(color = Color.White, shadow = Shadow(Color(0xFF5A0000), Offset(0f, 3f), 2f)),
            modifier = Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val h = size.height
                    val start = -t.value * h * 1.2f
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFFF4D6), Color(0xFFFFC233), Color(0xFFFF6A1A), Color(0xFFE50914), Color(0xFFFF6A1A), Color(0xFFFFC233)),
                            startY = start, endY = start + h * 1.2f,
                            tileMode = TileMode.Mirror,
                        ),
                        blendMode = BlendMode.SrcAtop,
                    )
                },
        )
        // Embers drifting up from the letters.
        Canvas(Modifier.matchParentSize().graphicsLayer()) {
            for (i in 0 until 22) {
                val speed = 0.5f + (i * 37 % 10) / 12f
                val p = (t.value * speed + i * 0.173f) % 1f
                val x = size.width * ((i * 53 % 100) / 100f) + sin(p * 6f + i) * 8.dp.toPx()
                val y = size.height * 0.6f - p * size.height * 1.8f
                drawCircle(
                    if (i % 2 == 0) Color(0xFFFFC266) else Color(0xFFFF6A1A),
                    radius = (1.6f - p).dp.toPx(),
                    center = Offset(x, y),
                    alpha = (1f - p) * 0.85f,
                )
            }
        }
    }
}
