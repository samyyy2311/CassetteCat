# Changelog

All notable changes to CassetteCat are documented in this file.

## [1.2.0] - Android Auto & Real Hotspot Pairing

### Companion Hardware
* **Real SoftAP Auto-Connect**: On Android 13+, pairing to the player's direct hotspot now associates automatically via `WifiNetworkSpecifier` instead of assuming the user already switched Wi-Fi networks manually. Older devices keep the existing connect-then-scan flow.
* **Disconnect Fixed**: The companion Disconnect button now actually disconnects instead of restarting hotspot discovery.

### Device Integration
* **Android Auto & Automotive OS**: Browse your local library (Liked Songs, Playlists, Albums, Artists, All Songs) and control playback directly from the car.
* **Quick Settings Tile**: Play, pause, and see what's playing without opening the app.

### Polish
* Haptic feedback now fires on every toggle switch throughout Settings, not just the row tap.
* Fixed cramped accent color swatch spacing in Theme customization.

## [1.1.0] - Share Cards & UI Polish

### Share & Social
* **1:1 Share Card Export**: Exported posters for both Song and Lyrics cards now render identically to the in-app preview card.
* **Ultra-HD Resolution**: Exported cards render at 2160 × 2700 with subpixel antialiasing and lossless PNG output.
* **Atmospheric Background Blur**: Upgraded live backdrop shader to generate silk-smooth Gaussian blur directly from current song artwork with dark gradient scrims.
* **Authentic Branding & Icons**: Integrated Lucide quote marks, cassette tape glyphs, Space Grotesk Bold, and IBM Plex Mono fonts directly into the canvas rendering pipeline.
* **Native App Launcher Icons**: Share tray now dynamically queries and displays installed WhatsApp and Instagram launcher icons directly from device `PackageManager`.
* **Now Playing Share Access**: Connected the 3-dots Now Playing menu's Share option directly to the interactive screenshot and lyrics card sheet.

### UI & Credits
* Refreshed Credits screen with official vector logos for Android, Internet Archive, Simple Icons, and GNU GPL v3.0.
* Scaled and balanced share action pill vectors for seamless visual padding.

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
