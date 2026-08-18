# CassetteCat

A music player and DIY hardware companion project from CaffeineLabs, focused on physical device aesthetics, local media ownership, and self-hosted streaming.

CassetteCat consists of two interconnected parts:
- **[app/](app/)**: A native Android music player and hardware companion built from scratch using Jetpack Compose and AndroidX Media3.
- **[firmware/](firmware/)** and **[hardware/](hardware/)**: A companion DIY hardware audio player based on the ESP32-S3, PCM5102 I2S DAC, and ST7735R display.

CassetteCat is not a physical tape player. The cassette identity is an aesthetic homage to standalone media players and hardware ownership.

---

## Android App Features

### Library & Streaming
- **Local Audio**: Scans audio using `MediaStore.Audio` with configurable folder whitelists and blacklists via the Storage Access Framework.
- **Subsonic Streaming**: Connects to Subsonic-compatible servers (Navidrome, Airsonic, Gonic, LMS) using token authentication.
- **Jellyfin Streaming**: Direct music playback and library integration from self-hosted Jellyfin instances.
- **Unified Aggregation**: Parallel background fetching across all configured sources with isolated error handling.

### Interface & Design
- **Owned Device Theme**: Industrial dark-mode palette (neutral metals, deep black surfaces, and Signal Red `#C23B30` indicator accents).
- **Tactile Controls**: Custom `TransportButton` and `PressDepthIconButton` components featuring mechanical press-depth offsets and subtle haptic feedback.
- **Typography**: IBM Plex Sans for general UI and IBM Plex Mono for technical readouts and duration displays.
- **Mechanical Motion**: Linear 220ms sliding transitions without bounce or spring overshoot.

### Audio & Playback
- **AndroidX Media3**: Foreground playback engine using ExoPlayer, supporting system media notifications and lock screen controls.
- **Now Playing Sheet**: Draggable bottom sheet with a horizontal track-switching pager and album art carousel.
- **Lyrics**: Support for embedded ID3 and Vorbis synchronized and plain lyrics.
- **Listening Log**: Device-styled play statistics tracking top tracks, artists, and albums with local artwork caching.
- **Playlists & Search**: Local playlist creation and instant client-side library filtering by title, artist, or album.

---

## Hardware Companion (In Progress)

The hardware companion is an open-source pocket player designed to sync with the Android app over Wi-Fi (SoftAP and station mode):

- **Microcontroller**: ESP32-S3 (16MB Flash, 8MB PSRAM)
- **DAC**: Adafruit PCM5102 I2S 32-bit DAC
- **Display**: 1.8" 128x160 TFT LCD (ST7735R) with onboard microSD slot
- **Power**: LiPo battery with TP4056 USB-C charger and MT3608 boost regulator
- **Input**: 8-key capacitive touch button matrix
- **Sync**: Wi-Fi bulk file transfer and ESP-IDF OTA updates from the Android app

---

## Repository Structure & Documentation

```
CassetteCat/
├── .github/        GitHub Actions CI workflows and issue templates
├── app/            Native Android application source code
│   ├── app/        Application module (Compose, Media3, DataStore)
│   └── gradle/     Version catalog and build configuration
├── firmware/       ESP32-S3 firmware source code
├── hardware/       PCB schematics, board layouts, and CAD models
└── docs/           Architecture specifications and development notes
```

For in-depth guides, see:
- [System Architecture](docs/architecture.md)
- [Android Development Guide](docs/android.md)
- [Hardware Specification](docs/hardware.md)
- [Firmware Architecture](docs/firmware.md)
- [Troubleshooting & FAQs](docs/troubleshooting.md)


---

## Building the Android App

### Requirements
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK 37 (minSdk 26)

### Command Line Build

```bash
cd app
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The output APK will be placed at:
```
app/app/build/outputs/apk/debug/app-debug.apk
```

---

## Contributing & Development Philosophy

Please review [CONTRIBUTING.md](CONTRIBUTING.md) for contribution guidelines.

Core principles:
- **Code Economy**: Write the minimum code required to solve problems cleanly. Avoid speculative abstractions.
- **F-Droid Compatibility**: No proprietary tracking or closed Google Play Services dependencies.
- **Clean Implementation**: All application code is written from scratch.

---

## Support & Sponsor

If you enjoy CassetteCat and want to support ongoing development:

- **[Ko-fi](https://ko-fi.com/samyyy2311)**
- **[Buy Me a Coffee](https://buymeacoffee.com/samyyy2311)**

> 20% of all sponsorship proceeds are donated directly to [LRCLIB](https://lrclib.net) to support free, open-source synchronized lyrics infrastructure.

---

## Credits & Attribution

CassetteCat is powered by open data, open-source libraries, and community infrastructure:

### Services & Data
- **[LRCLIB](https://lrclib.net)**: Real-time synchronized and plain lyrics.
- **[Cover Art Archive](https://coverartarchive.org)**: High-resolution archival CD and vinyl cover scans.
- **[Wikipedia & Wikimedia](https://wikipedia.org)**: Artist biographies and album backgrounds (CC BY-SA 4.0).
- **[MusicBrainz](https://musicbrainz.org)**: Open music encyclopedia and metadata references.
- **[ListenBrainz](https://listenbrainz.org)**: Open listening data and scrobbling infrastructure.
- **[Libre.fm](https://libre.fm)**: Free software music scrobbling network.
- **[Deezer](https://deezer.com)** & **[TheAudioDB](https://theaudiodb.com)**: Metadata and artist imagery references.

### Libraries & Design
- **[AutoEq](https://github.com/jaakkopasanen/AutoEq)**: Calibrated headphone frequency response profiles.
- **[AndroidX Media3](https://developer.android.com/media/media3)**: Audio streaming, foreground service, and cache architecture.
- **[Jetpack Compose & Material 3](https://developer.android.com/jetpack/compose)**: Declarative Android UI toolkit.
- **[Lucide Icons](https://lucide.dev)**: Clean open-source iconography (ISC License).
- **[Simple Icons](https://simpleicons.org)**: Authentic open-source brand iconography (CC0 1.0).
- **[IBM Plex](https://github.com/IBM/plex)**: Typefaces designed by Mike Abbink and Bold Monday for IBM (OFL 1.1).
- **[Space Grotesk](https://github.com/floriankarsten/space-grotesk)**: Display typography by Florian Karsten (OFL 1.1).
- **[OkHttp](https://square.github.io/okhttp/)**: High-performance HTTP client.

---

## License

- **Software (`app/` and `firmware/`)**: [GPL-3.0](LICENSE)
- **Hardware (`hardware/`)**: [CERN-OHL-S-2.0](hardware/LICENSE)
