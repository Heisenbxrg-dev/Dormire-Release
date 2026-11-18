package com.music.spotui.data.api

import com.music.spotui.data.entity.LyricLine
import com.music.spotui.data.entity.Lyrics
import java.io.File

/**
 * Lyrics the user typed, pasted or imported (.lrc / .txt) for a song, kept on this phone for good
 * (no expiry, unlike the lyrics cache) and always preferred by [LyricsApi.fetch]. Keyed by
 * title + artist, so they follow a file even if the media index gives it a new id.
 * Text with LRC timestamps ("[01:23.45] line") is shown synced; anything else as plain text.
 */
object CustomLyrics {
    const val PROVIDER = "Personal"

    private val dir: File? by lazy {
        runCatching { File(com.music.spotui.di.Graph.app.filesDir, "custom_lyrics").also { it.mkdirs() } }.getOrNull()
    }

    private fun keyOf(track: LyricsApi.Track) =
        "${track.title.trim().lowercase()}|${track.artist.substringBefore(",").trim().lowercase()}"

    private fun file(track: LyricsApi.Track): File? {
        val sha = java.security.MessageDigest.getInstance("SHA-1").digest(keyOf(track).toByteArray())
        return dir?.let { File(it, sha.joinToString("") { b -> "%02x".format(b) } + ".txt") }
    }

    /** The raw text kept for [track], or null. */
    fun raw(track: LyricsApi.Track): String? =
        runCatching { file(track)?.takeIf { it.exists() }?.readText() }.getOrNull()?.takeIf { it.isNotBlank() }

    fun has(track: LyricsApi.Track) = raw(track) != null

    /** Keeps [text] for [track] (blank removes it). False when it holds no line at all. */
    fun save(track: LyricsApi.Track, text: String): Boolean {
        if (text.isBlank()) { remove(track); return true }
        if (parse(text) == null) return false
        return runCatching {
            com.music.spotui.data.writeTextSafely(file(track) ?: return false, text)
            true
        }.getOrDefault(false)
    }

    fun remove(track: LyricsApi.Track) { runCatching { file(track)?.delete() } }

    /** The lyrics kept for [track], parsed, or null when there are none. */
    fun lyrics(track: LyricsApi.Track): Lyrics? = raw(track)?.let(::parse)

    /** [text] as lyrics: line-synced when it carries LRC timestamps, plain otherwise; null when empty. */
    fun parse(text: String): Lyrics? {
        val clean = text.removePrefix("﻿").replace("\r\n", "\n").replace('\r', '\n')
        val timed = LyricsApi.parseLrc(clean)
        if (timed.isNotEmpty()) return Lyrics(lines = timed, synced = true, provider = PROVIDER)
        // Plain text: the [ar:]/[ti:] tags of an LRC without times are dropped, blank lines are kept once (stanza breaks).
        val tag = Regex("""^\[[a-zA-Z]+:.*]$""")
        val lines = ArrayList<String>()
        for (raw in clean.split('\n')) {
            val l = raw.trim()
            if (tag.matches(l)) continue
            if (l.isEmpty() && (lines.isEmpty() || lines.last().isEmpty())) continue
            lines += l
        }
        while (lines.isNotEmpty() && lines.last().isEmpty()) lines.removeAt(lines.lastIndex)
        if (lines.isEmpty()) return null
        return Lyrics(lines = lines.map { LyricLine(0, it) }, synced = false, provider = PROVIDER)
    }
}
