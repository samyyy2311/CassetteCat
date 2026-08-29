# Changelog

All notable changes to CassetteCat are documented in this file.

## [1.6.1] - Performance & Reliability

### Performance
* Reduced memory usage and smoother scrolling across the library and Now Playing screens.
* Lower memory usage from the equalizer's audio effects.

### Fixes
* Fixed Playlist Suggestions occasionally splitting one genre into separate suggestions when songs were tagged with different capitalization (e.g. "Rock" vs "ROCK").

## [1.6.0] - Drive Mode, Wave to Skip & Reliability Fixes

### Playback
* **Drive Mode**: a stripped, high-contrast full-screen playback view with large touch targets, launches automatically when connected to a car's Bluetooth audio.
* **Wave to Skip**: wave your hand over the top of your phone to skip to the next track.
* **Audio Quality Badge**: Now Playing shows a Lossless/Hi-Res badge with format, bit depth, and sample rate details.
* Fixed playback sometimes stopping early instead of continuing to the next queued song, and next/previous sometimes not following the queue order.
* Shuffle now re-randomizes the upcoming queue every time it's turned on, and restores the original order when turned off.
* Fixed the seek bar visually snapping back after a manual seek; dragging it now feels more responsive.
* Fixed a flip-to-pause edge case leaving the shake and wave-to-skip sensors active while paused.
* Rewritten shake-to-skip detection with fewer false triggers from being in a pocket or bag.

### Library & Lyrics
* **Custom Lyrics Editor**: write and save your own lyrics for a song from Now Playing, and optionally contribute them to LRCLIB.
* Smart Playlists support mutually-exclusive rule groups.
* Higher-quality album art fetching, with cleaner matching for remastered/deluxe edition releases.

### Customization
* Fonts are now fully bundled offline: added VT323 and Monocraft, no longer depends on Google Play Services for downloadable fonts.

### Fixes
* Fixed Equalizer Reset silently re-enabling a disabled equalizer, leaving a stale calibration label, and not appearing for preamp-only adjustments.
* Fixed radio search occasionally showing results for the wrong country after quickly switching filters.
* Radio Browser being fully unreachable now shows a clear retry option instead of an endless "Loading stations…".
* Custom radio stations now require a valid stream URL before saving.
* Fixed a connection error being shown as a raw technical message when a server URL was entered without "https://".
* Fixed nearby Listening Room discovery continuing to run in the background after leaving the sheet.
* Reduced memory usage from playlist cover images.

## [1.5.0] - Gestures, Smart Playlists & Custom Fonts

### Playback
* **Shake to Skip**: Shake your phone to skip to the next track, with adjustable sensitivity.
* **Flip to Pause**: Flip your phone face-down to pause, flip it back up to resume.
* Mini player swipe-to-skip can now be turned off in Settings.

### Library
* **Smart Playlists**: Auto-populated playlists built from rules like Recently Added, Favorites Only, song length, or decade, instead of a manually curated song list.
* **Edit Song Tags**: Override a local song's title, artist, album, or release year from Now Playing, without touching the file on disk.
* Recent searches are now remembered and quickly reselectable.

### Lyrics
* **Manual Lyrics Search**: Search LRCLIB directly and pick a specific match when the automatic lookup finds the wrong song.

### Customization
* **Choose Your Font**: Pick from Space Grotesk, IBM Plex Sans, Outfit, Inter, Plus Jakarta Sans, or Silkscreen for the UI, with a separate font choice for lyrics.
* **Now Playing Backdrop Style**: Choose between AMOLED Pure Black, Ambient Glow, or Liquid Gradient.

## [1.4.5] - Fixes & Reliability

### Fixes
* Fixed shuffle sometimes duplicating songs already in the queue.
* Fixed "Add to Queue" behaving identically to "Play Next" instead of appending to the end of the queue.
* Fixed the back button collapsing the whole Now Playing sheet instead of returning to the Player view from Lyrics/Queue.
* Fixed equalizer presets sometimes applying the wrong preset on devices with an unnamed preset slot.
* Fixed a brief "Connecting…" flash when changing local tracks.
* Fixed a potential resource leak from rapidly tapping "Find Nearby Rooms" in Listening Room.
* Fixed the playback notification's icon.

### Performance
* Smoother scrolling through long song lists and search results.
* Faster equalizer preset loading.

## [1.4.1] - Shortcuts & Playback Polish

### Playback
* Save the current queue as a playlist and start an Instant Mix from Now Playing.
* See the connected Bluetooth audio device and jump directly to Bluetooth settings.

### Library & Streaming
* Refine library views with clearer sorting and filtering controls.
* See Subsonic and Jellyfin library status, song counts, and refresh failures in Settings.

### Launcher
* Added shortcuts for Shuffle All, Play Favorites, and Radio Favorites with distinct icons.

## [1.4.0] - Jellyfin Quick Connect & Playback Fixes

### Streaming
* **Jellyfin Quick Connect**: Sign in without typing a password by approving a code from an already-signed-in client.
* Fixed Jellyfin and Subsonic libraries sometimes failing to load songs.

### Playback
* Fixed a brief pause when skipping to the next track.
* Fixed Autoplay replaying the song that just ended before moving to the next pick, and fixed manually pressing Next at the end of the queue not triggering Autoplay.
* **Volume Limit (Ear Protection)**: optional hard cap on maximum output level, independent of system volume.
* External media button presses (e.g. from a swipe-to-skip widget) now reach the app even when it isn't in the foreground.

### Display
* Now Playing screen supports landscape orientation.
* Fixed the app restarting itself on screen rotation.
* Fixed the bottom navigation bar and Now Playing controls being covered on phones with a taller system navigation bar.

### Fixes
* Fixed a crash on launch affecting devices running Android 13 and below.

## [1.3.1] - Performance & Reliability

### Performance
* Smoother transitions between songs during playback.
* Playlist import (M3U), backup restore, and starting a download no longer briefly freeze the UI.
* Faster song selection when adding songs to a playlist.

### Also
* Home screen icon now supports Android 13+ themed icons.

## [1.3.0] - Internet Radio & Onboarding Fixes

### Radio
* **Internet Radio**: Search and browse tens of thousands of live stations from Radio Browser by name, tag, or country. Favorite stations sync to Android Auto, which also gets a dedicated Radio browse tree and voice search support.

### Stats
* **Rewind**: Export your monthly listening stats as a shareable poster.

### Fixes
* **Onboarding**: Fixed the folder setup step letting you continue with an empty folder selection.
* **Gapless Playback**: Fixed the Gapless Playback setting not working correctly.

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
