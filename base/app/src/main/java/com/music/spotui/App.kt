package com.music.spotui

import com.music.spotui.ui.theme.tone
import androidx.compose.foundation.border
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import com.music.spotui.ui.theme.AppText

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import com.music.spotui.ui.components.pressable
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.material3.Scaffold
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.music.spotui.ui.navigation.MainBottomNavigation
import com.music.spotui.ui.navigation.MyNavHost
import com.music.spotui.ui.navigation.navigateSingleTop


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun App() {
    val navController = rememberNavController()

    // Debug and beta builds: `myspoti-cli nav <route>` (see CliReceiver).
    LaunchedEffect(Unit) { com.music.spotui.cli.CliReceiver.nav.collect { r -> runCatching { navController.navigate(r) } } }
    // A page the onboarding asked for ("See the extensions").
    LaunchedEffect(Unit) {
        com.music.spotui.ui.screens.Setup.openAfter?.let { r -> com.music.spotui.ui.screens.Setup.openAfter = null; runCatching { navController.navigate(r) } }
    }

    // "See the extensions" in the dialog a download shows when no download extension is installed.
    LaunchedEffect(Unit) { com.music.spotui.ui.components.DownloadAsk.openRoute.collect { r -> runCatching { navController.navigateSingleTop(r) } } }

    UpdatePrompt()
    com.music.spotui.ui.components.CommunityAnnouncement()
    // The playlists at the top of the library, fetched ahead on Wi-Fi (see PagePrefetch).
    LaunchedEffect(Unit) { com.music.spotui.data.api.PagePrefetch.start() }

    // A collaborative-playlist invitation was opened (see InviteLinks): its playlist page offers "Join".
    val invite by com.music.spotui.data.api.InviteLinks.pending.collectAsState()
    LaunchedEffect(invite) {
        val i = invite?.takeIf { !it.opened } ?: return@LaunchedEffect
        app.myspoti.host.android.MyspotiRuntime.api.ready.first { it }
        i.opened = true
        navController.navigateSingleTop(com.music.spotui.ui.navigation.playlistRoute(i.playlistId))
    }

    // A Blend invitation was opened (see InviteLinks): who invites, and "Join", over the app.
    val blendLink by com.music.spotui.data.api.InviteLinks.blend.collectAsState()
    blendLink?.let { link ->
        com.music.spotui.ui.screens.BlendInviteSheet(link, navController) {
            if (com.music.spotui.data.api.InviteLinks.blend.value === link) com.music.spotui.data.api.InviteLinks.blend.value = null
        }
    }

    // Genius's explanations of the song playing, read while it starts: ready when the lyrics open.
    val geniusLang = com.music.spotui.ui.components.readerLanguage()
    val playingUri = com.music.spotui.di.SongPlayer.nowPlaying.value.uri
    val playingCover = com.music.spotui.di.Graph.currentSongState.coverUri.value
    val liveCoverPalette = com.music.spotui.ui.look.rememberArtworkColors(playingCover.takeIf { it.isNotBlank() })
    val targetTrackAccent = remember(liveCoverPalette, playingCover) {
        if (playingCover.isBlank()) null else com.music.spotui.ui.immersive.Immersive.accentFromPalette(liveCoverPalette.colors)
    }
    val animatedTrackAccent by androidx.compose.animation.animateColorAsState(
        targetValue = targetTrackAccent ?: androidx.compose.ui.graphics.Color(0xFFFF4F7B),
        animationSpec = androidx.compose.animation.core.tween(750),
        label = "appTrackAccent",
    )
    com.music.spotui.ui.immersive.Immersive.trackAccent = if (targetTrackAccent != null) animatedTrackAccent else null
    LaunchedEffect(playingUri) {
        if (!playingUri.startsWith("spotify:track:")) return@LaunchedEffect
        kotlinx.coroutines.delay(2_000) // the song's own loads first
        val st = com.music.spotui.di.Graph.currentSongState
        com.music.spotui.data.api.GeniusMeaning.prefetch(playingUri.substringAfterLast(':'), st.title.value, st.singer.value, geniusLang)
    }
    // A shared link to a list / artist / podcast: its page, over whatever was open.
    val pageLink by com.music.spotui.data.api.InviteLinks.page.collectAsState()
    LaunchedEffect(pageLink) {
        val uri = pageLink ?: return@LaunchedEffect
        com.music.spotui.data.api.InviteLinks.page.value = null
        val id = uri.substringAfterLast(':')
        com.music.spotui.ui.navigation.PlayerSheet.close()
        runCatching {
            navController.navigateSingleTop(when (uri.split(':')[1]) {
                "playlist" -> com.music.spotui.ui.navigation.playlistRoute(id)
                "album" -> com.music.spotui.ui.navigation.albumRoute("", "", id)
                "artist" -> com.music.spotui.ui.navigation.artistRoute("artist", id)
                else -> com.music.spotui.ui.navigation.showRoute(id)
            })
        }
    }
    val shareTarget by com.music.spotui.ui.components.SpotifyCode.request.collectAsState()
    shareTarget?.let { t -> com.music.spotui.ui.components.SpotifyCodeSheet(t) { com.music.spotui.ui.components.SpotifyCode.request.value = null } }

    // The look (Settings > Display) switches the shell live. The nav host keeps one place in the
    // tree in both looks, so a switch keeps the back stack, the pages' state and the playback.
    val immersive = com.music.spotui.ui.navigation.isImmersive()
    val haze = com.music.spotui.ui.immersive.shell.rememberImmersiveHazeState()
    // Liquid Glass (Settings > Display): the pages record themselves for the glass to refract.
    // Not provided while it is off or unsupported, so nothing is recorded for no reader.
    val glassPage = com.music.spotui.ui.immersive.shell.rememberImmersiveBackdrop()
    val liquidGlass = immersive && com.music.spotui.ui.immersive.shell.rememberLiquidGlassActive()
    // Without it (Android 12+), the frost blurs the same recording, which follows page and tab
    // transitions frame by frame where Haze lagged behind.
    val frostPage = immersive && !liquidGlass && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    // The floating chrome's fold (iOS 26): pages scroll it through immersiveChromeScroll().
    val chrome = com.music.spotui.ui.immersive.shell.rememberImmersiveChromeState()
    androidx.compose.runtime.CompositionLocalProvider(
        com.music.spotui.ui.look.LocalReduceMotion provides com.music.spotui.ui.look.rememberReduceMotion(),
        com.music.spotui.ui.immersive.shell.LocalImmersiveHaze provides if (immersive) haze else null,
        com.music.spotui.ui.immersive.shell.LocalImmersiveBackdrop provides if (liquidGlass) glassPage else null,
        com.music.spotui.ui.immersive.shell.LocalImmersiveFrostPage provides if (frostPage) glassPage else null,
        com.music.spotui.ui.immersive.shell.LocalImmersiveChrome provides if (immersive) chrome else null,
    ) {
    // Large windows (tablets, foldables, DeX) and a phone on its side: Classic's tabs in a rail at
    // the side (at the bottom they took a third of a landscape phone's height); Immersive lays out
    // its own bar (ImmersiveBottomChrome). Portrait phones keep the bottom bars.
    val rail = com.music.spotui.ui.components.windowSize().navigationRail
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
      androidx.compose.foundation.layout.Row(Modifier.fillMaxSize()) {
        // Before the Scaffold, in a group of its own: the nav host keeps its place when the
        // window grows or shrinks past the threshold (back stack and pages kept).
        if (rail && !immersive) com.music.spotui.ui.navigation.MainNavigationRail(navController)
        Scaffold(
            modifier = Modifier.weight(1f),
            // Transparent: Dormire's backdrop (wavy black or the user's image) shows under the pages.
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                if (!immersive) MainBottomNavigation(navController)
            }
        ) {
            MyNavHost(
                navHostController = navController,
                // Immersive: each page marks its own list as what the glass bars blur (a source on
                // the whole host would hold the pages' own top bars too).
                modifier = if (immersive) Modifier.background(com.music.spotui.ui.theme.AppPage) else Modifier,
            )
        }
      }
        if (immersive) ImmersiveShell(navController)
        // Over the pages and the bottom bar: pulling it down shows them, already in place.
        else com.music.spotui.ui.navigation.PlayerOverlay(navController)
        // "Not interested" and its Undo, over the pages and the players.
        com.music.spotui.ui.components.HiddenSongUndo()
        // A sponsor read skipped in a podcast (SponsorBlock), and its Undo.
        com.music.spotui.podcast.SponsorSkipUndo()
    }
    }
}



/**
 * The Immersive look's floating chrome: the mini player capsule over the tab bar (both hidden
 * on the full-screen routes; folded into one row while a page scrolls down), and its player over
 * everything.
 */
@Composable
private fun ImmersiveShell(navController: androidx.navigation.NavHostController) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        com.music.spotui.ui.immersive.shell.ImmersiveBottomChrome(
            navController,
            miniPlayer = { m -> com.music.spotui.ui.immersive.player.ImmersiveMiniPlayer(m) },
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
        )
        com.music.spotui.ui.immersive.player.ImmersivePlayerOverlay(navController)
    }
}

/**
 * The release the update card shows. The launch check fills it; so does the manual check in
 * Settings, which is why it is not private to [UpdatePrompt].
 */
internal val updateRelease = mutableStateOf<com.music.spotui.data.api.UpdateChecker.Release?>(null)

/**
 * "New version" dialog: checked at launch and on every return to the app (off the UI thread), shown as soon as the answer is
 * known. "Later" hides it; Settings > About offers the update again.
 */
@Composable
private fun UpdatePrompt() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var release by updateRelease
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.LaunchedEffect(owner) {
        // Each time the app comes to the foreground (the check itself skips when the last one is
        // under an hour old), then every 6 h while it stays open. Turned off in Settings: no
        // automatic check; the manual one there still asks.
        owner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                if (com.music.spotui.data.preferences.AutoUpdateCheckPref.on) {
                    // The request runs on the IO dispatcher, nothing waits for it.
                    com.music.spotui.data.api.UpdateChecker.check(context)?.let { release = it }
                }
                kotlinx.coroutines.delay(com.music.spotui.data.api.UpdateChecker.PERIODIC_MS)
            }
        }
    }
    val r = release ?: return
    com.music.spotui.extensions.ProtectedSurfaceEffect(app.myspoti.extensions.ProtectedSurface.UPDATE)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val updater = com.music.spotui.data.api.AppUpdater
    val state by updater.state.collectAsState()
    val start = { scope.launch { updater.update(context, r) }; Unit }
    // Back from the "Install unknown apps" page with the switch on: carry on by itself.
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME &&
                updater.state.value == com.music.spotui.data.api.AppUpdater.State.NeedsPermission &&
                updater.canInstall(context)) start()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val dismiss = { updater.reset(); com.music.spotui.data.api.UpdateChecker.dismiss(context, r); release = null }
    val res = context.resources
    val status = when (val s = state) {
        is com.music.spotui.data.api.AppUpdater.State.Downloading ->
            res.getString(app.myspoti.engine.R.string.update_downloading, (s.progress.coerceAtLeast(0f) * 100).toInt())
        com.music.spotui.data.api.AppUpdater.State.Installing -> res.getString(app.myspoti.engine.R.string.update_installing)
        com.music.spotui.data.api.AppUpdater.State.NeedsPermission -> res.getString(app.myspoti.engine.R.string.update_needs_permission)
        is com.music.spotui.data.api.AppUpdater.State.Failed ->
            if (s.onlyByHand) s.message else res.getString(app.myspoti.engine.R.string.update_failed, s.message)
        com.music.spotui.data.api.AppUpdater.State.Idle -> null
    }
    val busy = state is com.music.spotui.data.api.AppUpdater.State.Downloading || state == com.music.spotui.data.api.AppUpdater.State.Installing
    val byHand = (state as? com.music.spotui.data.api.AppUpdater.State.Failed)?.onlyByHand == true
    UpdateDialog(
        version = r.version,
        notes = r.notes,
        status = status,
        progress = (state as? com.music.spotui.data.api.AppUpdater.State.Downloading)?.progress,
        button = res.getString(when (state) {
            com.music.spotui.data.api.AppUpdater.State.NeedsPermission -> app.myspoti.engine.R.string.update_allow
            is com.music.spotui.data.api.AppUpdater.State.Failed ->
                if (byHand) app.myspoti.engine.R.string.sys_update_open_page else app.myspoti.engine.R.string.update_retry
            else -> app.myspoti.engine.R.string.version_sunsetting_dialog_cta_update
        }),
        onUpdate = {
            when {
                busy -> Unit
                // A release without an APK for this app, or one Android refused over it: its page.
                r.apkUrl.isBlank() || byHand -> {
                    updater.reset()
                    release = null
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(r.pageUrl))
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                state == com.music.spotui.data.api.AppUpdater.State.NeedsPermission && !updater.canInstall(context) ->
                    updater.openInstallSettings(context)
                else -> start()
            }
        },
        onLater = { if (!busy) dismiss() },
    )
}

/**
 * The update card: a dark sheet lit softly from the top, the version in the title face between two
 * fine rules, the release notes, and one rounded button that downloads and installs it in place.
 */
@Composable
private fun UpdateDialog(
    version: String, notes: String, status: String?, progress: Float?, button: String,
    onUpdate: () -> Unit, onLater: () -> Unit,
) {
    val accent = com.music.spotui.ui.theme.AppAccent
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
    val display = androidx.compose.ui.text.font.FontFamily(
        androidx.compose.ui.text.font.Font(app.myspoti.engine.R.font.dormire_cinzel, androidx.compose.ui.text.font.FontWeight.Bold),
    )
    val white = androidx.compose.ui.graphics.Color.White
    val clear = androidx.compose.ui.graphics.Color.Transparent
    // Only the buttons close it: a touch outside must not (the back gesture still means "Later").
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onLater,
        properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false),
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(tone(0xFF202026), tone(0xFF111115))))
                .drawBehind {
                    drawRect(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            listOf(accent.copy(alpha = 0.20f), clear),
                            center = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                            radius = size.width * 0.85f,
                        ),
                    )
                }
                .border(1.dp, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(white.copy(alpha = 0.20f), white.copy(alpha = 0.05f))), shape)
                .padding(horizontal = 26.dp, vertical = 30.dp),
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(app.myspoti.engine.R.drawable.dormire_icon),
                contentDescription = null,
                modifier = Modifier.size(64.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .border(1.dp, white.copy(alpha = 0.14f), androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .background(tone(0xFF181818)),
            )
            androidx.compose.material3.Text(
                androidx.compose.ui.res.stringResource(app.myspoti.engine.R.string.main_update_available).uppercase(),
                color = accent, fontFamily = com.music.spotui.ui.theme.AppSans,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 2.2.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
            androidx.compose.material3.Text(
                version, color = AppText, fontFamily = display, fontSize = 34.sp, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            // Two fine rules around a small diamond.
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.padding(top = 14.dp).fillMaxWidth(0.55f),
            ) {
                androidx.compose.foundation.layout.Box(Modifier.weight(1f).height(1.dp)
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(clear, accent.copy(alpha = 0.75f)))))
                androidx.compose.foundation.layout.Box(Modifier.padding(horizontal = 10.dp).size(6.dp).rotate(45f).background(accent))
                androidx.compose.foundation.layout.Box(Modifier.weight(1f).height(1.dp)
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(accent.copy(alpha = 0.75f), clear))))
            }
            // What's new (the release's CHANGELOG section), scrollable if long.
            if (notes.isNotBlank()) androidx.compose.material3.Text(
                notes, color = AppText.copy(alpha = 0.78f),
                fontFamily = com.music.spotui.ui.theme.AppSans, fontSize = 14.sp, lineHeight = 21.sp,
                modifier = Modifier.padding(top = 18.dp).fillMaxWidth().heightIn(max = 220.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            )
            // The download and install, under the notes.
            if (status != null) androidx.compose.material3.Text(
                status, color = AppText.copy(alpha = 0.85f),
                fontFamily = com.music.spotui.ui.theme.AppSans, fontSize = 13.sp, letterSpacing = 0.3.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp).fillMaxWidth(),
            )
            if (progress != null) {
                val bar = Modifier.padding(top = 10.dp).fillMaxWidth().height(3.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                if (progress < 0f) androidx.compose.material3.LinearProgressIndicator(
                    modifier = bar, color = accent, trackColor = white.copy(alpha = 0.12f))
                else androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress }, modifier = bar, color = accent,
                    trackColor = white.copy(alpha = 0.12f), drawStopIndicator = {})
            }
            androidx.compose.foundation.layout.Box(
                contentAlignment = androidx.compose.ui.Alignment.Center,
                modifier = Modifier
                    .padding(top = 26.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .pressable(0.97f, onClick = onUpdate)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.80f)))),
            ) {
                androidx.compose.material3.Text(
                    button,
                    color = androidx.compose.ui.graphics.Color.Black, fontFamily = com.music.spotui.ui.theme.AppSans,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp, letterSpacing = 0.4.sp,
                )
            }
            androidx.compose.material3.Text(
                androidx.compose.ui.res.stringResource(app.myspoti.engine.R.string.age_assurance_intro_cta_not_now),
                color = AppText.copy(alpha = 0.6f), fontFamily = com.music.spotui.ui.theme.AppSans,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp).pressable(0.96f, onClick = onLater).padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
    }
}
