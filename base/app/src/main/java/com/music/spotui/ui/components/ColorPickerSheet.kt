package com.music.spotui.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.spotui.data.preferences.CustomAccentPref
import com.music.spotui.data.preferences.CustomBackgroundPref
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.tone

/** The two palettes the user can pick from: one for the accent, one for the page background. */
private val ACCENT_PRESETS = listOf(
    Color(0xFFE50914), // Dormire red (default)
    Color(0xFFB30000), // dark red
    Color(0xFFFF4D4D), // bright red
    Color(0xFFE94E77), // pink
    Color(0xFFFF6A1A), // orange
    Color(0xFFFFC107), // amber
    Color(0xFF4CAF50), // green
    Color(0xFF1DB954), // spotify green
    Color(0xFF2196F3), // blue
    Color(0xFF7C4DFF), // violet
    Color(0xFFFFFFFF), // white
    Color(0xFFBDBDBD), // grey
)

private val BACKGROUND_PRESETS = listOf(
    Color(0xFF000000), // pure black (default)
    Color(0xFF0A0A0A), // near black
    Color(0xFF121212), // Spotify dark
    Color(0xFF1A0000), // very dark red
    Color(0xFF1C1C1C), // soft dark
    Color(0xFF0D1B2A), // dark blue
    Color(0xFF14110F), // warm dark
    Color(0xFF1B1B2F), // dark indigo
    Color(0xFF0F2027), // dark teal
    Color(0xFF2E1A1A), // dark wine
    Color(0xFFF5F5F5), // white (unusual but allowed)
    Color(0xFFE0E0E0), // light grey
)

/**
 * The Dormire "two-colour" picker: a modal sheet that lets the user set the accent and the page
 * background with a tap each. Covers the stock accent preset row -- the custom accent overrides
 * whatever was picked there as long as it stays on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    LookSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = tone(0xFF181818),
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 24.dp)) {
            Text(
                "Colori app",
                color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            )

            // Accent --------------------------------------------------------------
            SectionHeader("Colore pulsanti e accenti")
            CustomSwitchRow(
                label = "Usa colore personalizzato",
                checked = CustomAccentPref.enabled,
                onChecked = { CustomAccentPref.setEnabled(ctx, it) },
            )
            if (CustomAccentPref.enabled) {
                SwatchGrid(
                    presets = ACCENT_PRESETS,
                    selected = CustomAccentPref.color,
                    onPick = { CustomAccentPref.setColor(ctx, it) },
                )
            }

            Divider12()

            // Background ----------------------------------------------------------
            SectionHeader("Colore di sfondo")
            CustomSwitchRow(
                label = "Usa colore personalizzato",
                checked = CustomBackgroundPref.enabled,
                onChecked = { CustomBackgroundPref.setEnabled(ctx, it) },
            )
            if (CustomBackgroundPref.enabled) {
                SwatchGrid(
                    presets = BACKGROUND_PRESETS,
                    selected = CustomBackgroundPref.color,
                    onPick = { CustomBackgroundPref.setColor(ctx, it) },
                )
            }

            Text(
                "Suggerimento: se imposti un'immagine di sfondo questo colore non si vede.",
                color = AppMuted, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text, color = AppText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun CustomSwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(label, color = AppText, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Box(contentAlignment = Alignment.CenterEnd) {
            Switch(
                checked = checked, onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AppText,
                    checkedTrackColor = com.music.spotui.ui.theme.AppAccent,
                    uncheckedThumbColor = AppMuted,
                    uncheckedTrackColor = tone(0xFF3E3E3E),
                ),
            )
        }
    }
}

@Composable
private fun SwatchGrid(presets: List<Color>, selected: Color, onPick: (Color) -> Unit) {
    // Two rows of six: gives the "two colours at a time" feel the brief asked for, grouped by tone.
    val chunks = presets.chunked(6)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        for (row in chunks) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            ) {
                for (c in row) {
                    val isSelected = sameColor(c, selected)
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(if (isSelected) Modifier.border(3.dp, AppText, CircleShape)
                                else Modifier.border(1.dp, tone(0xFF3E3E3E), CircleShape))
                            .clickable { onPick(c) },
                    )
                }
            }
        }
    }
}

private fun sameColor(a: Color, b: Color): Boolean = a.toArgb() == b.toArgb()

@Composable
private fun Divider12() {
    androidx.compose.material3.HorizontalDivider(
        color = tone(0xFF2A2A2A),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

/**
 * The Settings row for the background image: shows the current state, lets the user pick an image,
 * clear it, or change the dim overlay. The picker itself uses the OPEN_DOCUMENT contract so URI
 * permissions survive reboots.
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
                ctx.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            pref.set(ctx, uri)
        }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Immagine di sfondo", color = AppText, fontSize = 16.sp)
                Text(
                    if (pref.active) "Un'immagine personalizzata è attiva" else "Nessuna immagine selezionata",
                    color = AppMuted, fontSize = 12.sp,
                )
            }
            Text(
                if (pref.active) "Cambia" else "Scegli",
                color = com.music.spotui.ui.theme.AppAccent, fontSize = 14.sp,
                modifier = Modifier.clickable {
                    launcher.launch(arrayOf("image/*"))
                }.padding(horizontal = 8.dp, vertical = 6.dp),
            )
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
                    thumbColor = com.music.spotui.ui.theme.AppAccent,
                    activeTrackColor = com.music.spotui.ui.theme.AppAccent,
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

/**
 * The Settings row that opens the two-colour picker. Shows a live preview of the current
 * accent + background pair.
 */
@Composable
fun CustomColorsRow(onOpen: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Colori app", color = AppText, fontSize = 16.sp)
            Text("Scegli colore dei pulsanti e dello sfondo", color = AppMuted, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(com.music.spotui.ui.theme.AppAccent)
                .border(1.dp, tone(0xFF3E3E3E), CircleShape))
            Box(Modifier.size(22.dp).clip(CircleShape)
                .background(if (CustomBackgroundPref.enabled) CustomBackgroundPref.color else Color(0xFF121212))
                .border(1.dp, tone(0xFF3E3E3E), CircleShape))
        }
    }
}
