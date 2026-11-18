package com.music.spotui.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.ui.theme.Montserrat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Dormire's launch screen: about 2 seconds of the wavy black with "DORMIRE" on fire and the
 * tagline under it, then a fade into the app. Touches are swallowed while it is up.
 */
@Composable
fun DormireSplash(onFinished: () -> Unit) {
    val screen = remember { Animatable(1f) }
    val title = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { title.animateTo(1f, tween(550, easing = FastOutSlowInEasing)) }
        launch { delay(450); tagline.animateTo(1f, tween(500)) }
        delay(1700)
        screen.animateTo(0f, tween(380))
        onFinished()
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screen.value }
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        WavyBackdrop(glow = false)
        // A warm glow behind the title, like the fire lighting the grooves.
        Spacer(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.radialGradient(
                listOf(Color(0x55FF2A00), Color(0x18E50914), Color.Transparent),
                center = Offset(size.width / 2f, size.height * 0.47f),
                radius = size.width * 0.8f,
            ))
        })
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.graphicsLayer {
                alpha = title.value
                val s = 0.85f + 0.15f * title.value
                scaleX = s; scaleY = s
            }) { FireTitle("DORMIRE") }
            Text(
                "Il sonno profondo prima di GtaVI",
                color = Color(0xFFD9D9D9),
                fontFamily = Montserrat,
                fontSize = 13.sp,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp).graphicsLayer {
                    alpha = tagline.value
                    translationY = (1f - tagline.value) * 18f
                },
            )
        }
    }
}

/** "DORMIRE" burning: flame tongues behind, a flickering red glow, a moving fire gradient, embers. */
@Composable
private fun FireTitle(text: String) {
    val fire = rememberInfiniteTransition(label = "fire")
    val t = fire.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "fire-t")
    val flicker = fire.animateFloat(
        0.8f, 0.8f,
        infiniteRepeatable(keyframes {
            durationMillis = 900
            0.8f at 0; 1f at 110; 0.82f at 250; 0.96f at 400; 0.75f at 580; 1f at 760; 0.8f at 900
        }),
        label = "fire-flicker",
    )
    val style = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Black, fontSize = 54.sp, letterSpacing = 6.sp)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 36.dp)) {
        // Flames rising from the letters (drawn past the top of the text on purpose).
        Canvas(Modifier.matchParentSize()) {
            val tongues = 20
            val slot = size.width / tongues
            val baseY = size.height * 0.80f
            for (i in 0 until tongues) {
                val phase = t.value * 1.3f + i * 0.137f
                val flameH = size.height * (0.55f + 0.55f * abs(sin(2f * PI.toFloat() * phase))) * (0.85f + 0.15f * sin(i * 1.7f))
                val cx = slot * (i + 0.5f)
                val half = slot * 0.85f
                val sway = sin(2f * PI.toFloat() * (t.value * 2f + i * 0.31f)) * half * 0.45f
                val tipY = baseY - flameH
                val path = Path().apply {
                    moveTo(cx - half, baseY)
                    quadraticTo(cx - half * 0.7f, baseY - flameH * 0.55f, cx + sway, tipY)
                    quadraticTo(cx + half * 0.7f, baseY - flameH * 0.55f, cx + half, baseY)
                    close()
                }
                drawPath(path, Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0x99E50914), Color(0xCCFF5A00), Color(0xDDFFB300)),
                    startY = tipY, endY = baseY,
                ), alpha = 0.55f + 0.35f * flicker.value)
            }
        }
        // Red glow under the letters.
        Text(
            text,
            style = style.copy(color = Color(0xFFFF3300), shadow = Shadow(Color(0xFFFF2200), Offset.Zero, blurRadius = 48f)),
            modifier = Modifier.graphicsLayer { alpha = flicker.value },
        )
        // The letters themselves, filled with a fire gradient that keeps flowing upward.
        Text(
            text,
            style = style.copy(color = Color.White),
            modifier = Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val h = size.height
                    val start = -t.value * h
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color(0xFF7A0000), Color(0xFFE50914), Color(0xFFFF5A00), Color(0xFFFFB300), Color(0xFFFFF1C1)),
                            startY = start, endY = start + h,
                            tileMode = TileMode.Mirror,
                        ),
                        blendMode = BlendMode.SrcAtop,
                    )
                },
        )
        // Embers drifting up.
        Canvas(Modifier.matchParentSize()) {
            for (i in 0 until 34) {
                val speed = 0.6f + (i * 37 % 10) / 10f
                val p = (t.value * speed + i * 0.173f) % 1f
                val x = size.width * ((i * 53 % 100) / 100f) + sin(p * 6f + i) * 10.dp.toPx()
                val y = size.height * 0.85f - p * size.height * 2.1f
                val r = (1.2f + (i % 3)) * (1f - p * 0.6f)
                drawCircle(
                    if (i % 2 == 0) Color(0xFFFFB74D) else Color(0xFFFF5A00),
                    radius = r.dp.toPx() / 2f + 1f,
                    center = Offset(x, y),
                    alpha = (1f - p) * 0.9f,
                )
            }
        }
    }
}
