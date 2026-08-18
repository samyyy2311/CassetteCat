# Changelog

All notable changes to CassetteCat are documented in this file.

## [1.0.0] - Initial Release

### Audio & Playback
* High-fidelity audio engine built on AndroidX Media3 (ExoPlayer).
* Hardware-accelerated DSP effects: parametric equalizer, bass boost, and spatial virtualizer.
* Real-time synchronized and plain lyrics via embedded LRC/ID3 tags and LRCLIB.
* Configurable sleep timer and precise playback speed adjustment.

### Cloud & Streaming
* Native Subsonic API integration compatible with Navidrome, Gonic, and Airsonic.
* Jellyfin music library streaming with secure token authentication.
* Offline song downloads with dedicated cache management and background sync.
* User-owned scrobbling support for ListenBrainz and Libre.fm.

### Companion Hardware
* Wireless pairing and sync for standalone ESP32 audio players over direct SoftAP hotspot or local Wi-Fi mDNS.
* Live hardware telemetry: battery level, charging status, SD card capacity, and firmware version.

### Library & User Experience
* Fast local search across songs, artists, and albums with artist portraits.
* Custom playlist creation, cover art customization, and M3U/M3U8 import and export.
* Device-local listening stats: monthly playback time, top artists, and repeat metrics.
* Fine-grained folder scan filters (whitelist and blacklist modes).
* Full JSON backup and restore for playlists, favorites, and user settings.
* Credits and open-source attributions in Settings.

### Privacy & Architecture
* 100% local-first: no tracking, no analytics, and zero Google Play Services dependencies.
* Server passwords encrypted with hardware-backed Android Keystore.
* Free and open-source software licensed under GPL-3.0.
