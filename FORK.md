# Fork notes

**Revera** (`caruno-git/Revera`) is a modified GPL-3.0 fork of [Myspoti](https://github.com/kota-zo/myspoti) / [kota-zo/Orpheon](https://github.com/kota-zo/Orpheon), named as the origin per the project's GPL-3.0 notice (`LICENSE`, `AGENTS.md`).

Modifications in this fork:

- Rebranded package and app labels to **Revera** (`app.revera`, updates via `caruno-git/Revera`).
- Visible downloads saved in `Download/Revera` (with backwards-compatible reading of `Download/Orpheon` and `Music/Orpheon`).
- Lyrics and cover embedded in the downloaded file (USLT+SYLT / APIC for MP3, ©lyr / covr for M4A, Vorbis LYRICS + PICTURE + `.lrc` sidecar for FLAC), before the public scan or the private encryption.
- A lyrics badge in the track rows (checked over Wi-Fi only, cached).
- Server-verified profile badges and launcher icons (`ECDSA P-256`).
- Rust ARM64 CDN audio transport (`orpheon-audio-transport`) and Compose backdrop rendering optimizations.
- Login flow and diagnostic log improvements.

Upstream license (GPL-3.0) and credits are kept unchanged.

## Private build (this copy)

Private, unpublished build derived from Revera (caruno-git) → Myspoti / Orpheon (kota-zo), GPL-3.0.
Changes: own application id (`-PappId`, default `app.myrevera`), update check off, badge server off.
Licence and upstream credits are kept. If this build is ever distributed to anyone, it must stay GPL-3.0 with its complete source available and keep the credits.

## Rename to Dormire (this copy)

Private rename of the app to **Dormire**: app label, theme, application id (`app.dormire`), log/user-agent names, the Download folder (`Download/Dormire`, with `Download/Revera` still read). The origin credits (Revera → Myspoti / Orpheon), the license and the upstream URLs are kept unchanged.
