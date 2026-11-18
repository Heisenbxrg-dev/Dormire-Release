# Dormire – modifiche (sovrascrivi questi file nel tuo repository)

## Cosa è cambiato
- Nome visibile ovunque: "Dormire" (etichetta app, testi in tutte le lingue).
- Discord rimosso: niente popup community, niente bottone, niente riga Discord in "Info".
  (CommunityMenuButton / CommunityAnnouncement ora non mostrano nulla.)
- Riga "GitHub" in Info: compare solo se imposti tu UPDATE_REPO (gradle property `updateRepo`).
- Nuova icona: note musicali rosse su sfondo nero (vector, disegno originale).
- Impostazioni > Aspetto/Display:
  - "Colori app": popup con DUE colori (pulsanti/accenti + sfondo). Default rosso #E50914 + nero.
  - "Immagine di sfondo": scegli una foto dalla galleria, regola l'oscuramento, rimuovila.
- Tutte le funzioni esistenti restano invariate (motore, Spotify, automix, estensioni, download, ecc.).

## Restano (licenza GPL-3.0, obbligatorio)
LICENSE, AGENTS.md, THIRD_PARTY_NOTICES.md, header di copyright e la riga
"Origin: Myspoti (GPL-3.0)" nella schermata Info.
I nomi interni del codice (package app.myspoti, chiavi delle preferenze) NON sono stati rinominati:
cambiarli cancellerebbe login, download offline cifrati e impostazioni salvate.

## Unica dipendenza esterna rimasta del vecchio autore
Il catalogo estensioni scarica l'indice da
raw.githubusercontent.com/kota-zo/myspoti-extensions (ExtensionStore.kt, OFFICIAL_REPO).
Se vuoi renderlo tuo: fai un fork di quel repo sul tuo GitHub e cambia quella riga.

## Build con GitHub
1. Copia la cartella `base/` di questo zip sopra la tua (sovrascrivi).
2. Commit + push su main → GitHub Actions "Android Build" crea l'APK.
3. Scaricalo da Actions > ultimo run > Artifacts > dormire-debug-apk.
