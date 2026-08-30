# CassetteCat

<p align="center">
  <img src="assets/logo.svg" width="128" height="128" alt="CassetteCat Logo" />
</p>

<p align="center">
  <strong>A privacy-first Android music player for local media ownership and self-hosted streaming.</strong>
</p>

<p align="center">
  <a href="CHANGELOG.md"><img src="https://img.shields.io/badge/v1.6.3-E55B3C?style=flat-square&logo=git&logoColor=white" alt="v1.6.3" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/GPL--3.0-A42E2B?style=flat-square&logo=gnu&logoColor=white" alt="GPL-3.0" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android%208.0+-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose" /></a>
  <a href="https://alternativeto.net/software/cassettecat/about/"><img src="https://img.shields.io/badge/AlternativeTo-0289D5?style=flat-square&logo=alternativeto&logoColor=white" alt="AlternativeTo" /></a>
  <a href="https://github.com/samyyy2311/CassetteCat/releases"><img src="https://img.shields.io/github/downloads/samyyy2311/CassetteCat/total?style=flat-square&color=2A2A2A&labelColor=2A2A2A&label=Downloads&logo=github&logoColor=white" alt="GitHub downloads" /></a>
</p>

---

## Install

<table align="center">
  <tr>
    <td align="center"><a href="https://play.google.com/store/apps/details?id=in.caffeinelabs.cassettecat"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="45" /></a></td>
    <td align="center"><a href="https://github.com/samyyy2311/CassetteCat/releases/latest"><img src="assets/badge_github.png" alt="Download APK from GitHub Releases" height="31" /></a></td>
    <td align="center"><a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/samyyy2311/CassetteCat"><img src="assets/badge_obtainium.png" alt="Install CassetteCat with Obtainium" height="45" /></a></td>
    <td align="center"><img src="assets/badge_fdroid.png" alt="F-Droid (pending)" height="45" /></td>
    <td align="center"><img src="assets/badge_openapk.png" alt="OpenAPK (pending)" height="45" /></td>
  </tr>
</table>

<p align="center">
  <strong>Android 8.0+</strong><br />
  Download the latest APK directly from GitHub or use Obtainium for automatic update tracking. F-Droid and OpenAPK listings are pending.
</p>

---

## Screenshots

<p align="center">
  <img src="assets/screenshots/framed/01_home.png" width="23%" alt="Home Screen" />
  <img src="assets/screenshots/framed/02_library_albums.png" width="23%" alt="Library" />
  <img src="assets/screenshots/framed/03_library_artists.png" width="23%" alt="Artists" />
  <img src="assets/screenshots/framed/04_radio.png" width="23%" alt="Radio" />
</p>
<p align="center">
  <img src="assets/screenshots/framed/05_instrumental.png" width="23%" alt="Lyrics (Instrumental)" />
  <img src="assets/screenshots/framed/06_now_playing.png" width="23%" alt="Now Playing" />
  <img src="assets/screenshots/framed/07_queue.png" width="23%" alt="Up Next Queue" />
  <img src="assets/screenshots/framed/08_settings.png" width="23%" alt="Settings" />
</p>

---

## Features

### Audio & Playback

- **Media3 Playback Engine**: Background audio playback with lock screen
  controls, system media notifications, and state restoration on launch.
- **Hardware Equalizer & DSP**: 5-band graphic EQ, bass boost, virtualizer, and
  loudness enhancer. Includes calibrated headphone presets powered by
  [AutoEq](https://github.com/jaakkopasanen/AutoEq).
- **Volume Normalization (ReplayGain)**: Automatic track volume balancing to
  avoid sudden loudness spikes.
- **Volume Limit (Ear Protection)**: Optional hard cap on maximum output level,
  independent of system volume.
- **Synchronized Lyrics**: Real-time karaoke-style lyrics via
  [LRCLIB](https://lrclib.net), embedded ID3 tags, and local `.lrc` sidecars
  with configurable typography scaling and keep-screen-awake mode.
- **Sleep Timer & Smart Disconnect**: Sleep timer to pause playback, plus
  instant pause when headphones or Bluetooth disconnect.

### Library & Streaming

- **Local Audio Scanning**: Scans device audio with configurable folder
  whitelists, blacklists, and short audio clip filtering.
- **Tape Index Rail**: Industrial tactile A–Z fast scroller with an interactive
  HUD letter badge and haptic clock ticks for lightning-fast library browsing.
- **Smart Relevance Search**: Real-time multi-token relevance search matching
  across song titles, artists, and albums.
- **Subsonic API**: Connects to Navidrome, Gonic, and Airsonic with token
  authentication.
- **Jellyfin**: Direct streaming and music library browsing from your Jellyfin
  server, with Quick Connect sign-in support.
- **Internet Radio**: Search and browse tens of thousands of live stations from
  [Radio Browser](https://www.radio-browser.info) by name, tag, or country, with
  favorites and Android Auto support.
- **Offline Cache**: Download and cache streaming tracks for offline playback.
- **Scrobbling**: Track your listening on
  [ListenBrainz](https://listenbrainz.org) and [Libre.fm](https://libre.fm).
- **Listening Log**: Local playback statistics tracking your top tracks,
  artists, and albums.
- **Listening Room**: Sync playback with nearby devices over your local Wi-Fi.

### Planned Companion Hardware

> The companion protocol is present in the Android app, but the `firmware/`
> and `hardware/` directories currently contain design placeholders rather
> than buildable firmware or fabrication files.

- **Wireless Pairing**: Connect to a standalone ESP32 CassetteCat player over a
  direct SoftAP hotspot or your local Wi-Fi network via mDNS. On Android 13+,
  hotspot pairing associates automatically via `WifiNetworkSpecifier`.
- **Live Telemetry**: Battery level, charging status, SD card capacity, and
  firmware version, all from the Pairing screen.

### Device Integration

- **Android Auto & Android Automotive OS**: Browse your local library (Liked
  Songs, Playlists, Albums, Artists, All Songs) and control playback directly
  from the car.
- **Quick Settings Tile**: Play, pause, and see what's playing without opening
  the app.

### Design & Customization

- **Curated Theme Accents**: 6 dynamic color palettes: _Record Red_, _Cassette
  Amber_, _Electric Cyan_, _Neon Emerald_, _Tape Magenta_, and _Monochrome
  Silver_.
- **Pure Black AMOLED Mode**: Pitch black `#000000` surface backgrounds for true
  OLED blacks and maximum battery efficiency.
- **Now Playing & Artwork Styling**: Configurable album art corner radii (Curved
  16dp, Soft 8dp, Vinyl Square 0dp) and interactive remaining time countdown
  toggle (`-02:45`).
- **Navigation & Startup Preferences**: Configurable default landing screen
  (_Home_, _Library_, or _Last active tab_) and default Library section.
- **Ultra-HD Share Cards**: Export 2160×2700 poster cards for tracks and
  synchronized lyric excerpts.
- **Rewind**: Export your monthly listening stats as a shareable poster.
- **Home Screen Widget**: Control playback and view current song artwork
  directly from your home screen.

### Privacy

- Local-first architecture: no analytics, no third-party tracking, zero Google
  Play Services dependencies.
- Streaming server credentials encrypted with AES-256 via the hardware-backed
  `AndroidKeyStore`.
- Fully open source under GPL-3.0.
- F-Droid submission pending.

---

## Building

```bash
cd app
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Output:

```text
app/app/build/outputs/apk/debug/app-debug.apk
```

---

## Documentation

- [System Architecture](docs/architecture.md)
- [Android Development Guide](docs/android.md)
- [Troubleshooting & FAQs](docs/troubleshooting.md)
- [Privacy Policy](PRIVACY_POLICY.md)

---

## Contributing

Contributions are welcome.

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines, and [AI_DISCLOSURE.md](AI_DISCLOSURE.md) for how AI assistance is used in this project.

---

## Support

<p align="center">
  <a href="https://ko-fi.com/samyyy2311"><img src="https://img.shields.io/badge/Ko--fi-FF5E5B?style=flat-square&logo=kofi&logoColor=white" alt="Ko-fi" /></a>
  <a href="https://buymeacoffee.com/samyyy2311"><img src="https://img.shields.io/badge/Buy%20Me%20a%20Coffee-FFDD00?style=flat-square&logo=buymeacoffee&logoColor=black" alt="Buy Me a Coffee" /></a>
</p>

---

## Credits

### Services & Data

- <a href="https://lrclib.net"><img src="https://img.shields.io/badge/LRCLIB-38BDF8?style=flat-square" alt="LRCLIB" /></a>
  Synchronized and plain lyrics.
- <a href="https://coverartarchive.org"><img src="https://img.shields.io/badge/Cover%20Art%20Archive-20656C?style=flat-square" alt="Cover Art Archive" /></a>
  Album artwork from MetaBrainz and the Internet Archive.
- <a href="https://musicbrainz.org"><img src="https://img.shields.io/badge/MusicBrainz-EB743B?style=flat-square&logo=musicbrainz&logoColor=white" alt="MusicBrainz" /></a>
  Music metadata and encyclopedia.
- <a href="https://listenbrainz.org"><img src="https://img.shields.io/badge/ListenBrainz-EB743B?style=flat-square&logo=listenbrainz&logoColor=white" alt="ListenBrainz" /></a>
  Open scrobbling.
- <a href="https://www.radio-browser.info"><img src="https://img.shields.io/badge/Radio%20Browser-1C1917?style=flat-square" alt="Radio Browser" /></a>
  Internet radio station directory.
- <a href="https://libre.fm"><img src="https://img.shields.io/badge/Libre.fm-990000?style=flat-square&logo=gnu&logoColor=white" alt="Libre.fm" /></a>
  Free software scrobbling (GNU FM).
- <a href="https://wikipedia.org"><img src="https://img.shields.io/badge/Wikipedia-000000?style=flat-square&logo=wikipedia&logoColor=white" alt="Wikipedia" /></a>
  Artist biographies (CC BY-SA 4.0).
- <a href="https://deezer.com"><img src="https://img.shields.io/badge/Deezer-FEAA2D?style=flat-square&logo=deezer&logoColor=white" alt="Deezer" /></a>
  <a href="https://theaudiodb.com"><img src="https://img.shields.io/badge/TheAudioDB-6599CD?style=flat-square" alt="TheAudioDB" /></a>
  Artist imagery and metadata.
- <a href="https://github.com"><img src="https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white" alt="GitHub" /></a>
  Release update checks.

### Libraries & Design

- <a href="https://github.com/jaakkopasanen/AutoEq"><img src="https://img.shields.io/badge/AutoEq-1C1917?style=flat-square" alt="AutoEq" /></a>
  Headphone EQ profiles by Jaakko Pasanen.
- <a href="https://developer.android.com/media/media3"><img src="https://img.shields.io/badge/Media3-3DDC84?style=flat-square&logo=android&logoColor=white" alt="AndroidX Media3" /></a>
  Playback engine and caching.
- <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" /></a>
  UI toolkit.
- <a href="https://lucide.dev"><img src="https://img.shields.io/badge/Lucide-F56565?style=flat-square&logo=lucide&logoColor=white" alt="Lucide" /></a>
  App logo and icons (ISC).
- <a href="https://simpleicons.org"><img src="https://img.shields.io/badge/Simple%20Icons-111111?style=flat-square&logo=simpleicons&logoColor=white" alt="Simple Icons" /></a>
  Brand icons (CC0 1.0).
- <a href="https://github.com/square/okhttp"><img src="https://img.shields.io/badge/OkHttp-006AFF?style=flat-square&logo=square&logoColor=white" alt="OkHttp" /></a>
  HTTP client.
- <a href="https://github.com/IBM/plex"><img src="https://img.shields.io/badge/IBM%20Plex-0F62FE?style=flat-square&logo=ibm&logoColor=white" alt="IBM Plex" /></a>
  Typefaces (OFL 1.1).
- <a href="https://github.com/floriankarsten/space-grotesk"><img src="https://img.shields.io/badge/Space%20Grotesk-242424?style=flat-square" alt="Space Grotesk" /></a>
  Display font by Florian Karsten (OFL 1.1).

---

## License

CassetteCat is licensed under the
[GNU General Public License v3.0 (GPL-3.0)](LICENSE).
