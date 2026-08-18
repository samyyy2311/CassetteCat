# CassetteCat

<p align="center">
  <img src="assets/logo.svg" width="128" height="128" alt="CassetteCat Logo" />
</p>

<p align="center">
  <strong>A privacy-first Android music player for local media ownership and self-hosted streaming.</strong>
</p>

<p align="center">
  <a href="CHANGELOG.md"><img src="https://img.shields.io/badge/v1.1.0-E55B3C?style=flat-square&logo=git&logoColor=white" alt="v1.1.0" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/GPL--3.0-A42E2B?style=flat-square&logo=gnu&logoColor=white" alt="GPL-3.0" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android%208.0+-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose" /></a>
</p>

---

## Download

<p align="center">
  <a href="https://github.com/samyyy2311/CassetteCat/releases/latest"><img src="https://img.shields.io/badge/GitHub%20Releases-181717?style=flat-square&logo=github&logoColor=white" alt="GitHub Releases" /></a>
  <a href="https://github.com/ImranR98/Obtainium"><img src="https://img.shields.io/badge/Obtainium-4A154B?style=flat-square&logo=git&logoColor=white" alt="Obtainium" /></a>
</p>

---

## Features

### Audio & Playback
- **Media3 Playback**: Background audio playback with lock screen controls and system media notifications.
- **Hardware Equalizer & DSP**: System graphic EQ, bass boost, virtualizer, and loudness enhancer. Includes calibrated headphone presets powered by [AutoEq](https://github.com/jaakkopasanen/AutoEq).
- **Synchronized Lyrics**: Real-time karaoke-style lyrics via [LRCLIB](https://lrclib.net) and embedded ID3 tags.
- **Sleep Timer**: Built-in sleep timer to automatically pause playback.

### Library & Streaming
- **Local Audio**: Scans device audio with configurable folder whitelists and blacklists.
- **Subsonic API**: Connects to Navidrome, Gonic, and Airsonic with token authentication.
- **Jellyfin**: Direct streaming and music library browsing from your Jellyfin server.
- **Offline Cache**: Download and cache streaming songs for offline playback.
- **Scrobbling**: Track your listening on [ListenBrainz](https://listenbrainz.org) and [Libre.fm](https://libre.fm).
- **Listening Log**: Local playback statistics tracking your top tracks, artists, and albums.

### Design & UI
- **Tactile Dark Theme**: Sleek industrial dark interface with mechanical button animations and haptic feedback.
- **Ultra-HD Share Cards**: Export 2160×2700 poster cards for tracks and synchronized lyric excerpts.
- **Home Screen Widget**: Control playback and see current song artwork directly from your home screen.

### Privacy
- Local-first architecture: no analytics, no third-party tracking, zero Google Play Services dependencies.
- Streaming server credentials encrypted with AES-256 via the hardware-backed `AndroidKeyStore`.
- Fully F-Droid compatible and open source under GPL-3.0.

---

## Building

```bash
cd app
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Output: `app/app/build/outputs/apk/debug/app-debug.apk`

---

## Documentation

- [System Architecture](docs/architecture.md)
- [Android Development Guide](docs/android.md)
- [Troubleshooting & FAQs](docs/troubleshooting.md)

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

---

## Support

<p align="center">
  <a href="https://ko-fi.com/samyyy2311"><img src="https://img.shields.io/badge/Ko--fi-FF5E5B?style=flat-square&logo=kofi&logoColor=white" alt="Ko-fi" /></a>
  <a href="https://buymeacoffee.com/samyyy2311"><img src="https://img.shields.io/badge/Buy%20Me%20a%20Coffee-FFDD00?style=flat-square&logo=buymeacoffee&logoColor=black" alt="Buy Me a Coffee" /></a>
</p>

---

## Credits

### Services & Data
- <a href="https://lrclib.net"><img src="https://img.shields.io/badge/LRCLIB-38BDF8?style=flat-square" alt="LRCLIB" /></a> Synchronized and plain lyrics.
- <a href="https://coverartarchive.org"><img src="https://img.shields.io/badge/Cover%20Art%20Archive-20656C?style=flat-square" alt="Cover Art Archive" /></a> Album artwork from MetaBrainz and the Internet Archive.
- <a href="https://musicbrainz.org"><img src="https://img.shields.io/badge/MusicBrainz-EB743B?style=flat-square&logo=musicbrainz&logoColor=white" alt="MusicBrainz" /></a> Music metadata and encyclopedia.
- <a href="https://listenbrainz.org"><img src="https://img.shields.io/badge/ListenBrainz-EB743B?style=flat-square&logo=listenbrainz&logoColor=white" alt="ListenBrainz" /></a> Open scrobbling.
- <a href="https://libre.fm"><img src="https://img.shields.io/badge/Libre.fm-990000?style=flat-square&logo=gnu&logoColor=white" alt="Libre.fm" /></a> Free software scrobbling (GNU FM).
- <a href="https://wikipedia.org"><img src="https://img.shields.io/badge/Wikipedia-000000?style=flat-square&logo=wikipedia&logoColor=white" alt="Wikipedia" /></a> Artist biographies (CC BY-SA 4.0).
- <a href="https://deezer.com"><img src="https://img.shields.io/badge/Deezer-FEAA2D?style=flat-square&logo=deezer&logoColor=white" alt="Deezer" /></a> <a href="https://theaudiodb.com"><img src="https://img.shields.io/badge/TheAudioDB-242424?style=flat-square" alt="TheAudioDB" /></a> Artist imagery and metadata.

### Libraries & Design
- <a href="https://github.com/jaakkopasanen/AutoEq"><img src="https://img.shields.io/badge/AutoEq-1C1917?style=flat-square" alt="AutoEq" /></a> Headphone EQ profiles by Jaakko Pasanen.
- <a href="https://developer.android.com/media/media3"><img src="https://img.shields.io/badge/Media3-3DDC84?style=flat-square&logo=android&logoColor=white" alt="AndroidX Media3" /></a> Playback engine and caching.
- <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a> UI toolkit.
- <a href="https://lucide.dev"><img src="https://img.shields.io/badge/Lucide-F56565?style=flat-square&logo=lucide&logoColor=white" alt="Lucide" /></a> App logo and icons (ISC).
- <a href="https://simpleicons.org"><img src="https://img.shields.io/badge/Simple%20Icons-111111?style=flat-square&logo=simpleicons&logoColor=white" alt="Simple Icons" /></a> Brand icons (CC0 1.0).
- <a href="https://github.com/square/okhttp"><img src="https://img.shields.io/badge/OkHttp-006AFF?style=flat-square&logo=square&logoColor=white" alt="OkHttp" /></a> HTTP client.
- <a href="https://github.com/IBM/plex"><img src="https://img.shields.io/badge/IBM%20Plex-0F62FE?style=flat-square&logo=ibm&logoColor=white" alt="IBM Plex" /></a> Typefaces (OFL 1.1).
- <a href="https://github.com/floriankarsten/space-grotesk"><img src="https://img.shields.io/badge/Space%20Grotesk-242424?style=flat-square" alt="Space Grotesk" /></a> Display font by Florian Karsten (OFL 1.1).

---

## License

CassetteCat is licensed under the [GNU General Public License v3.0 (GPL-3.0)](LICENSE).
