# CassetteCat

<p align="center">
  <img src="assets/play_store_icon_512.png" width="128" height="128" alt="CassetteCat Logo" style="border-radius: 28px;" />
</p>

<p align="center">
  <strong>A privacy-first Android music player for local media ownership and self-hosted streaming.</strong>
</p>

<p align="center">
  Desktop app: <a href="https://github.com/samyyy2311/CassetteCat-Desktop">CassetteCat Desktop</a> (Windows, macOS, Linux)
</p>

<p align="center">
  Website: <a href="https://cassettecat.caffeinelabs.in">cassettecat.caffeinelabs.in</a> · User guide: <a href="https://cassettecat.caffeinelabs.in/docs">cassettecat.caffeinelabs.in/docs</a>
</p>

<p align="center">
  <a href="CHANGELOG.md"><img src="https://img.shields.io/badge/v1.8.0-2A2A2A?style=flat-square&logo=git&logoColor=E55B3C" alt="Version 1.8.0" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/GPL--3.0-2A2A2A?style=flat-square&logo=gnu&logoColor=white" alt="GPL-3.0" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android_8.0+-2A2A2A?style=flat-square&logo=android&logoColor=3DDC84" alt="Android 8.0+" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2A2A2A?style=flat-square&logo=kotlin&logoColor=7F52FF" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Compose-2A2A2A?style=flat-square&logo=jetpackcompose&logoColor=4285F4" alt="Jetpack Compose" /></a>
  <a href="https://github.com/samyyy2311/CassetteCat/releases"><img src="https://img.shields.io/github/downloads/samyyy2311/CassetteCat/total?style=flat-square&color=2A2A2A&labelColor=2A2A2A&label=downloads&logo=github&logoColor=white" alt="GitHub downloads" /></a>
  <br />
  <a href="https://hosted.weblate.org/engage/cassettecat/"><img src="https://img.shields.io/badge/Translate-2A2A2A?style=flat-square&logo=weblate&logoColor=2ECCAA" alt="Translate on Weblate" /></a>
  <a href="https://alternativeto.net/software/cassettecat/about/"><img src="https://img.shields.io/badge/AlternativeTo-2A2A2A?style=flat-square&logo=alternativeto&logoColor=white" alt="AlternativeTo" /></a>
  <a href="https://coderabbit.ai"><img src="https://img.shields.io/coderabbit/prs/github/samyyy2311/CassetteCat?style=flat-square&color=2A2A2A&labelColor=2A2A2A&label=CodeRabbit&logo=coderabbit&logoColor=FF570A" alt="CodeRabbit pull request reviews" /></a>
  <a href="https://app.codacy.com/gh/samyyy2311/CassetteCat/dashboard"><img src="https://img.shields.io/codacy/grade/a71f402be7e74c80927fe95ec0a931ca?style=flat-square&color=2A2A2A&labelColor=2A2A2A&label=Codacy&logo=codacy&logoColor=white" alt="Codacy code quality" /></a>
  <a href="https://www.codefactor.io/repository/github/samyyy2311/cassettecat"><img src="https://img.shields.io/codefactor/grade/github/samyyy2311/CassetteCat?style=flat-square&color=2A2A2A&labelColor=2A2A2A&label=CodeFactor&logo=codefactor&logoColor=F44A6A" alt="CodeFactor grade" /></a>
  <a href="https://www.bestpractices.dev/projects/15444"><img src="https://img.shields.io/cii/level/15444?style=flat-square&color=2A2A2A&labelColor=2A2A2A&label=OpenSSF&logo=linuxfoundation&logoColor=white" alt="OpenSSF Best Practices" /></a>
</p>

---

## Install

<table align="center">
  <tr>
    <td align="center"><a href="https://play.google.com/store/apps/details?id=in.caffeinelabs.cassettecat"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="45" /></a></td>
    <td align="center"><a href="https://github.com/samyyy2311/CassetteCat/releases/latest"><img src="assets/badge_github.png" alt="Download APK from GitHub Releases" height="31" /></a></td>
    <td align="center"><a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/samyyy2311/CassetteCat"><img src="assets/badge_obtainium.png" alt="Install CassetteCat with Obtainium" height="45" /></a></td>
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
- **A–Z Rail**: Jump through long lists with a letter badge and a light tick as
  you pass each letter.
- **Search**: Finds songs, artists, and albums as you type, matching several
  words at once.
- **Subsonic API**: Connects to Navidrome, Gonic, and Airsonic with token
  authentication.
- **Jellyfin**: Direct streaming and music library browsing from your Jellyfin
  server, with Quick Connect sign-in support.
- **Internet Radio**: Search and browse tens of thousands of live stations from
  [Radio Browser](https://www.radio-browser.info) by name, tag, or country, with
  favorites and Android Auto support.
- **Offline Cache**: Download and cache streaming tracks for offline playback.
- **Scrobbling**: Track your listening on [Last.fm](https://www.last.fm),
  [ListenBrainz](https://listenbrainz.org), and [Libre.fm](https://libre.fm).
- **Listening Record**: Your top songs, artists, and albums by month or year,
  your listening history, and a Rewind poster to share.
- **Listening Room**: Sync playback with nearby devices over your local Wi-Fi.

### Planned Companion Hardware

> The companion protocol is present in the Android app, but the `firmware/` and
> `hardware/` directories currently contain design placeholders rather than
> buildable firmware or fabrication files.

- **Wireless Pairing**: Connect to a standalone ESP32 CassetteCat player over a
  direct SoftAP hotspot or your local Wi-Fi network via mDNS. On Android 13+,
  hotspot pairing associates automatically via `WifiNetworkSpecifier`.
- **Live Telemetry**: Battery level, charging status, SD card capacity, and
  firmware version, all from the Pairing screen.

### Device Integration

- **Android Auto**: Browse your local library (Liked Songs, Playlists, Albums,
  Artists, All Songs) and radio favourites, search, and control playback from
  the car.
- **Quick Settings Tile**: Play, pause, and see what's playing without opening
  the app.
- **Desktop Remote**: Pick your computer from the devices button to control
  [CassetteCat Desktop](https://github.com/samyyy2311/CassetteCat-Desktop) on
  the same Wi-Fi, move your song, queue and position between the two, and
  browse the computer's music from your phone.

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
- **Share Cards**: Share a song or a few lines of its lyrics as a 2160×2700
  image.
- **Home Screen Widget**: Resizable, with the current song and artwork, play
  controls, shuffle, repeat, and like, in your accent colour. Play works even
  after the app is closed.

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

- [User guide](https://cassettecat.caffeinelabs.in/docs): setup, every feature, and troubleshooting
- [Developer guide](docs/README.md#working-on-the-code): building, architecture, how-tos and releasing
- [Privacy Policy](PRIVACY_POLICY.md)
- [Third-Party Notices](THIRD_PARTY_NOTICES.md)

---

## Contributing

Contributions are welcome.

See [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines, and
[AI_DISCLOSURE.md](AI_DISCLOSURE.md) for how AI assistance is used in this
project.

### Translate CassetteCat

Translations are done on [Weblate](https://hosted.weblate.org/engage/cassettecat/), in your browser, with no
coding needed. Pick a language, or add yours, and Weblate sends the changes here as a pull request. See
[TRANSLATING.md](TRANSLATING.md) for a few guidelines.

---

## Support

<p align="center">
  <a href="https://ko-fi.com/samyyy2311"><img src="https://img.shields.io/badge/Ko--fi-FF5E5B?style=flat-square&logo=kofi&logoColor=white" alt="Ko-fi" /></a>
  <a href="https://buymeacoffee.com/samyyy2311"><img src="https://img.shields.io/badge/Buy%20Me%20a%20Coffee-FFDD00?style=flat-square&logo=buymeacoffee&logoColor=black" alt="Buy Me a Coffee" /></a>
</p>

---

## Credits

### Services & Data

| Project | Used for |
| :--- | :--- |
| [LRCLIB](https://lrclib.net) | Synchronized and plain lyrics |
| [Cover Art Archive](https://coverartarchive.org) | Album artwork from MetaBrainz and the Internet Archive |
| [MusicBrainz](https://musicbrainz.org) | Music metadata and encyclopedia |
| [Last.fm](https://www.last.fm) | Scrobbling |
| [ListenBrainz](https://listenbrainz.org) | Open scrobbling |
| [Radio Browser](https://www.radio-browser.info) | Internet radio station directory |
| [Libre.fm](https://libre.fm) | Free software scrobbling (GNU FM) |
| [Wikipedia](https://wikipedia.org) | Artist biographies (CC BY-SA 4.0) |
| [Deezer](https://deezer.com) and [TheAudioDB](https://theaudiodb.com) | Artist imagery and metadata |
| [GitHub](https://github.com) | Release update checks |

### Libraries & Design

| Project | Used for |
| :--- | :--- |
| [AutoEq](https://github.com/jaakkopasanen/AutoEq) | Headphone EQ profiles by Jaakko Pasanen |
| [AndroidX Media3](https://developer.android.com/media/media3) | Playback engine and caching |
| [Jetpack Compose](https://developer.android.com/jetpack/compose) | UI toolkit |
| [Lucide](https://lucide.dev) | App logo and icons (ISC) |
| [Simple Icons](https://simpleicons.org) | Brand icons (CC0 1.0) |
| [OkHttp](https://github.com/square/okhttp) | HTTP client |
| [IBM Plex](https://github.com/IBM/plex) | Typefaces (OFL 1.1) |
| [Space Grotesk](https://github.com/floriankarsten/space-grotesk) | Display font by Florian Karsten (OFL 1.1) |

---

## Desktop

CassetteCat is also available for Windows, macOS, and Linux at
[CassetteCat-Desktop](https://github.com/samyyy2311/CassetteCat-Desktop).

---

## Acknowledgements

<p align="center">
  <a href="https://coderabbit.ai"><img src="assets/coderabbit_logo.svg" width="56" height="56" alt="CodeRabbit" /></a>
  <a href="https://www.codacy.com"><img src="assets/codacy_logo.svg" width="56" height="56" alt="Codacy" /></a>
  <a href="https://www.codefactor.io"><img src="assets/codefactor_logo.svg" width="56" height="56" alt="CodeFactor" /></a>
  <a href="https://www.mintlify.com"><img src="assets/mintlify_logo.svg" width="56" height="56" alt="Mintlify" /></a>
  <a href="https://hosted.weblate.org/engage/cassettecat/"><img src="assets/weblate_logo.svg" width="56" height="56" alt="Weblate" /></a>
</p>

<p align="center">
  Thanks to <a href="https://coderabbit.ai">CodeRabbit</a> for free code reviews on open-source projects like this one, to <a href="https://hosted.weblate.org/engage/cassettecat/">Weblate</a> for hosting translations, and to <a href="https://www.codacy.com">Codacy</a>, <a href="https://www.codefactor.io">CodeFactor</a> and <a href="https://www.mintlify.com">Mintlify</a> for their free plans for open source.
</p>

---

## License

CassetteCat is licensed under the
[GNU General Public License v3.0 (GPL-3.0)](LICENSE).
