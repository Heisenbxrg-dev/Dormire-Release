package com.music.spotui.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.data.preferences.CustomAccentPref
import com.music.spotui.data.preferences.DORMIRE_RED
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.tone

/** Quick picks in the swipeable carousel. */
private val PRESETS = listOf(
    DORMIRE_RED, Color(0xFFB30000), Color(0xFFFF3B3B), Color(0xFFFF2D55), Color(0xFFE94E77),
    Color(0xFFFF6A1A), Color(0xFFFF9500), Color(0xFFFFC107), Color(0xFFC6FF00), Color(0xFF1DB954),
    Color(0xFF00E5A0), Color(0xFF00BCD4), Color(0xFF2196F3), Color(0xFF3D5AFE), Color(0xFF7C4DFF),
    Color(0xFFD500F9), Color(0xFFFFFFFF), Color(0xFFBDBDBD),
)

private fun hsvOf(c: Color): FloatArray = FloatArray(3).also { android.graphics.Color.colorToHSV(c.toArgb(), it) }

/**
 * Dormire's accent picker: the page stays wavy black, only the accent (red by default) changes.
 * Slide a finger along the colour strip, fine-tune intensity and brightness, or swipe through
 * the quick picks. Every change shows live in the preview and in the whole app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val start = remember { hsvOf(CustomAccentPref.color) }
    var hue by remember { mutableFloatStateOf(start[0]) }
    var sat by remember { mutableFloatStateOf(start[1].coerceIn(0.3f, 1f)) }
    var value by remember { mutableFloatStateOf(start[2].coerceIn(0.35f, 1f)) }
    fun current() = Color.hsv(hue, sat, value)
    fun live() = CustomAccentPref.preview(current())
    fun save() = CustomAccentPref.setColor(ctx, current())
    fun pick(c: Color) {
        val hsv = hsvOf(c)
        hue = hsv[0]; sat = hsv[1].coerceIn(0f, 1f); value = hsv[2].coerceIn(0.35f, 1f)
        CustomAccentPref.setColor(ctx, c)
    }

    LookSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = tone(0xFF0E0E0E),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text("Colore accento", color = AppText, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp))
            Text("Lo sfondo resta nero ondulato. Scorri col dito sui colori.", color = AppMuted, fontSize = 13.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 14.dp))

            AccentPreview()

            SwitchRow(
                label = "Usa colore personalizzato",
                checked = CustomAccentPref.enabled,
                onChecked = { CustomAccentPref.setEnabled(ctx, it) },
            )

            if (CustomAccentPref.enabled) {
                Label("Colore")
                GradientBar(
                    colors = (0..6).map { Color.hsv((it * 60f) % 360f, 1f, 1f) },
                    fraction = hue / 360f,
                    thumb = Color.hsv(hue, 1f, 1f),
                    onChange = { hue = (it * 360f).coerceIn(0f, 359.9f); live() },
                    onDone = { save() },
                )
                Label("Intensità")
                GradientBar(
                    colors = listOf(Color.hsv(hue, 0.3f, value), Color.hsv(hue, 1f, value)),
                    fraction = (sat - 0.3f) / 0.7f,
                    thumb = current(),
                    onChange = { sat = 0.3f + it * 0.7f; live() },
                    onDone = { save() },
                )
                Label("Luminosità")
                GradientBar(
                    colors = listOf(Color.hsv(hue, sat, 0.35f), Color.hsv(hue, sat, 1f)),
                    fraction = (value - 0.35f) / 0.65f,
                    thumb = current(),
                    onChange = { value = 0.35f + it * 0.65f; live() },
                    onDone = { save() },
                )

                Label("Scelte rapide")
                val listState = rememberLazyListState()
                LazyRow(
                    state = listState,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    items(PRESETS) { c ->
                        val selected = c.toArgb() == CustomAccentPref.color.toArgb()
                        Box(
                            Modifier
                                .size(if (selected) 52.dp else 46.dp)
                                .drawBehind {
                                    drawCircle(Brush.radialGradient(listOf(c.copy(alpha = 0.55f), Color.Transparent)),
                                        radius = size.minDimension * 0.75f)
                                }
                                .clip(CircleShape)
                                .background(c)
                                .border(if (selected) 3.dp else 1.dp, if (selected) AppText else tone(0xFF3E3E3E), CircleShape)
                                .pressable(0.9f) { pick(c) },
                        )
                    }
                }

                Text(
                    "Ripristina rosso Dormire",
                    color = AppAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp, top = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { pick(DORMIRE_RED) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/** A small wavy-black card with a sample button and glow, so the choice is seen in context. */
@Composable
private fun AccentPreview() {
    val accent by animateColorAsState(AppAccent, label = "accent-preview")
    Box(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(132.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
    ) {
        WavyBackdrop(glow = true)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(18.dp).fillMaxWidth().height(96.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("DORMIRE", color = AppText, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp,
                    style = TextStyle(shadow = Shadow(accent, Offset.Zero, 28f)))
                Text("Anteprima", color = AppMuted, fontSize = 12.sp)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.height(44.dp)
                    .drawBehind {
                        drawRoundRect(accent.copy(alpha = 0.35f), topLeft = Offset(-6f, -6f),
                            size = Size(size.width + 12f, size.height + 12f), cornerRadius = CornerRadius(size.height))
                    }
                    .clip(RoundedCornerShape(50)).background(accent)
                    .padding(horizontal = 22.dp),
            ) {
                Text("Riproduci", color = if (accent.luminance() > 0.55f) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = AppText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp))
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            .clickable { onChecked(!checked) }
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(label, color = AppText, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Switch(
            checked = checked, onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppText,
                checkedTrackColor = AppAccent,
                uncheckedThumbColor = AppMuted,
                uncheckedTrackColor = tone(0xFF3E3E3E),
            ),
        )
    }
}

/** A colour strip with a round thumb: tap or slide a finger along it. */
@Composable
private fun GradientBar(colors: List<Color>, fraction: Float, thumb: Color, onChange: (Float) -> Unit, onDone: () -> Unit) {
    val change by rememberUpdatedState(onChange)
    val done by rememberUpdatedState(onDone)
    Box(
        Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth().height(44.dp)
            .pointerInput(Unit) {
                detectTapGestures { change((it.x / size.width).coerceIn(0f, 1f)); done() }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(onDragEnd = { done() }, onDragCancel = { done() }) { c, _ ->
                    c.consume()
                    change((c.position.x / size.width).coerceIn(0f, 1f))
                }
            }
            .drawBehind {
                val barH = 16.dp.toPx()
                val top = (size.height - barH) / 2f
                drawRoundRect(Brush.horizontalGradient(colors), topLeft = Offset(0f, top),
                    size = Size(size.width, barH), cornerRadius = CornerRadius(barH / 2f))
                val r = 13.dp.toPx()
                val cx = (fraction.coerceIn(0f, 1f) * size.width).coerceIn(r, size.width - r)
                val cy = size.height / 2f
                drawCircle(thumb.copy(alpha = 0.45f), radius = r * 1.6f, center = Offset(cx, cy))
                drawCircle(Color.Black.copy(alpha = 0.5f), radius = r + 1.dp.toPx(), center = Offset(cx, cy + 1.dp.toPx()))
                drawCircle(Color.White, radius = r, center = Offset(cx, cy))
                drawCircle(thumb, radius = r - 3.dp.toPx(), center = Offset(cx, cy))
            },
    )
}

/**
 * The Settings row for the background image: pick one, change how dark it is, or remove it to go
 * back to Dormire's wavy black. OPEN_DOCUMENT so the permission survives reboots.
 */
@Composable
fun BackgroundImagePickerRow() {
    val ctx = LocalContext.current
    val pref = com.music.spotui.data.preferences.BackgroundImagePref

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            pref.set(ctx, uri)
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Immagine di sfondo", color = AppText, fontSize = 16.sp)
                Text(
                    if (pref.active) "Immagine personalizzata attiva" else "Nero ondulato Dormire (predefinito)",
                    color = AppMuted, fontSize = 12.sp,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .border(1.dp, AppAccent, RoundedCornerShape(50))
                    .pressable(0.94f) { launcher.launch(arrayOf("image/*")) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(if (pref.active) "Cambia" else "Scegli", color = AppAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (pref.active) {
            Text("Oscuramento: ${(pref.dim * 100).toInt()}%", color = AppMuted, fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp))
            var dim by remember(pref.dim) { mutableFloatStateOf(pref.dim) }
            Slider(
                value = dim,
                onValueChange = { dim = it },
                onValueChangeFinished = { pref.setDim(ctx, dim) },
                valueRange = 0f..0.9f,
                colors = SliderDefaults.colors(
                    thumbColor = AppAccent,
                    activeTrackColor = AppAccent,
                    inactiveTrackColor = tone(0xFF3E3E3E),
                ),
            )
            Text(
                "Rimuovi immagine",
                color = com.music.spotui.ui.theme.AppDestructive, fontSize = 14.sp,
                modifier = Modifier.clickable { pref.set(ctx, null) }.padding(vertical = 6.dp),
            )
        }
    }
}

/** The Settings row that opens the accent picker, with a glowing swatch of the current colour. */
@Composable
fun CustomColorsRow(onOpen: () -> Unit) {
    val accent = AppAccent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Colore accento", color = AppText, fontSize = 16.sp)
            Text("Scorri col dito per scegliere il colore di pulsanti e dettagli", color = AppMuted, fontSize = 12.sp)
        }
        Box(
            Modifier.size(30.dp)
                .drawBehind { drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.6f), Color.Transparent)), radius = size.minDimension) }
                .clip(CircleShape).background(accent)
                .border(2.dp, Color.Black, CircleShape),
        )
    }
}
