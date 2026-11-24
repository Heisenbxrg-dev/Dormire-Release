package com.music.spotui.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.music.spotui.data.api.CustomLyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.myspoti.engine.R
import com.music.spotui.data.api.LyricsApi
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.tone
import com.music.spotui.ui.viewmodel.LyricsViewModel

/**
 * "Try another source": the providers that can give this song's lyrics, the one shown marked,
 * the word-timed ones named, the ones set aside for now dimmed. A tap asks that one (the lyrics
 * shown stay until it answers); the sheet closes on lyrics, and stays with a toast when the
 * provider had none. Shared by both looks, like the other pickers behind the song options.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LyricsSourceSheet(onDismiss: () -> Unit, vm: LyricsViewModel = viewModel()) {
    val context = LocalContext.current
    val track = remember { SongPlayer.currentLyricsTrack() }
    if (track == null) { LaunchedEffect(Unit) { onDismiss() }; return }
    val state by vm.state.collectAsState()
    val trying by vm.trying.collectAsState()
    val current = (state as? LyricsViewModel.State.Loaded)?.lyrics?.let { LyricsApi.sourceOf(it) }
    val sources = remember(track) { vm.sources(track) }

    // The answer to a try: new lyrics close the sheet, a provider with nothing says so.
    var asked by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(trying) {
        val id = trying
        if (id != null) { asked = id; return@LaunchedEffect }
        val was = asked ?: return@LaunchedEffect
        asked = null
        if (vm.triedNothing.value == was) {
            val name = sources.firstOrNull { it.id == was }?.name ?: was
            Toast.makeText(context, context.getString(R.string.lyrics_source_nothing, name), Toast.LENGTH_SHORT).show()
        } else onDismiss()
    }

    var editing by remember { mutableStateOf(false) }
    if (editing) {
        CustomLyricsDialog(track, onClose = { editing = false }, onSaved = {
            vm.load(track, force = true)
            onDismiss()
        })
    }

    val accent = com.music.spotui.ui.theme.AppAccent
    val card = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    LookSheet(onDismissRequest = onDismiss, containerColor = tone(0xFF140A0C)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            // Title in the title face, with a short red rule under it.
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp)) {
                Text(
                    stringResource(R.string.lyrics_other_source), color = AppText, fontSize = 20.sp, letterSpacing = 1.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                        androidx.compose.ui.text.font.Font(app.myspoti.engine.R.font.dormire_cinzel, FontWeight.Bold),
                    ),
                )
                androidx.compose.foundation.layout.Box(
                    Modifier.padding(top = 8.dp).width(44.dp).height(3.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)).background(accent),
                )
            }
            val mine = remember(track) { CustomLyrics.has(track) }
            val mineShown = mine && current == null
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(card)
                    .background(if (mineShown) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
                    .border(if (mineShown) 1.5.dp else 1.dp, if (mineShown) accent else Color.White.copy(alpha = 0.08f), card)
                    .clickable { editing = true }.padding(16.dp, 14.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.lyrics_custom), color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(
                        stringResource(if (mine) R.string.lyrics_custom_saved else R.string.lyrics_custom_hint),
                        color = AppMuted, fontSize = 12.sp,
                    )
                }
                if (mineShown) Icon(Icons.Default.Check, contentDescription = null, tint = accent)
            }
            sources.forEach { s ->
                val here = s.id == current
                val detail = listOfNotNull(
                    if (here) stringResource(R.string.lyrics_source_shown) else null,
                    if (s.words) stringResource(R.string.lyrics_source_words) else null,
                    if (s.setAside != null) stringResource(R.string.lyrics_source_aside) else null,
                ).joinToString(" · ")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        // Set aside: dimmed, still tappable (asking by hand ignores the break).
                        .alpha(if (s.setAside != null) 0.45f else 1f)
                        .clip(card)
                        // The one shown is lit in red; the others are dim rounded cards.
                        .background(if (here) accent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
                        .border(if (here) 1.5.dp else 1.dp, if (here) accent else Color.White.copy(alpha = 0.08f), card)
                        .clickable(enabled = trying == null) { vm.loadFrom(track, s.id) }
                        .padding(16.dp, 14.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name, color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        if (detail.isNotEmpty()) Text(detail, color = if (here) accent else AppMuted, fontSize = 12.sp)
                    }
                    if (trying == s.id) CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    else if (here) Icon(Icons.Default.Check, contentDescription = null, tint = accent)
                }
            }
        }
    }
}

/**
 * Type, paste or import (.lrc / .txt) the lyrics of [track]; saved on this phone and used before any
 * other source. Text with LRC timestamps is shown synced. Empty and saved (or "Remove") drops them.
 */
@Composable
private fun CustomLyricsDialog(track: LyricsApi.Track, onClose: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val existing = remember(track) { CustomLyrics.raw(track) }
    var text by remember { mutableStateOf(existing.orEmpty()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val read = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        }.getOrNull()
        if (read.isNullOrBlank()) Toast.makeText(context, R.string.lyrics_custom_unreadable, Toast.LENGTH_SHORT).show()
        else text = read
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.lyrics_custom)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.lyrics_custom_placeholder)) },
                    modifier = Modifier.fillMaxWidth(), minLines = 8, maxLines = 12,
                )
                TextButton(onClick = { picker.launch(arrayOf("text/*", "application/octet-stream", "*/*")) }) {
                    Text(stringResource(R.string.lyrics_custom_import))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (CustomLyrics.save(track, text)) onSaved()
                else Toast.makeText(context, R.string.lyrics_custom_unreadable, Toast.LENGTH_SHORT).show()
            }) { Text(stringResource(R.string.lyrics_custom_save)) }
        },
        dismissButton = {
            Row {
                if (existing != null) TextButton(onClick = { CustomLyrics.remove(track); onSaved() }) {
                    Text(stringResource(R.string.lyrics_custom_remove))
                }
                TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
