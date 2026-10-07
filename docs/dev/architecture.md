# Architecture

CassetteCat is one Android app module (`app/app/`) written in Kotlin with Jetpack Compose. It plays music from the phone, from Subsonic and Jellyfin servers, and from internet radio, and it can pair with [CassetteCat Desktop](https://github.com/samyyy2311/CassetteCat-Desktop) over Wi-Fi.

There is no dependency-injection framework. `CassetteCatApplication`, the ViewModels and the playback service create the objects they need. Repositories are plain classes, usually built from a `Context`.

All source paths below are relative to `app/app/src/main/java/in/caffeinelabs/cassettecat/`.

## Layers

```
Compose screens (ui/screens)
      │  read StateFlow, call functions
ViewModels (LibraryViewModel, PlaybackViewModel, SettingsViewModel, …)
      │
Repositories (data/…)  ──  DataStore, MediaStore, HTTP clients
      │
PlaybackRepository  ──  Media3 MediaController
      │  binds to
PlaybackService (MediaLibraryService + ExoPlayer)
```

State flows one way: repositories expose `StateFlow`s, ViewModels combine them into UI state, and screens call ViewModel functions to change anything.

## UI (`ui/`)

| Package | Contents |
|---|---|
| `ui/navigation` | `MainActivity`'s content: `CassetteCatNavHost` and `MainShell`, which holds the bottom tabs, the mini-player and every route in `MainRoute`. |
| `ui/screens/*` | One package per area: `home`, `library`, `nowplaying`, `radio`, `search`, `settings`, `stats`, `onboarding`. |
| `ui/components` | Shared pieces: `AlbumArt`, `MiniPlayerBar`, `QueueList`, `FastScrollIndexRail`, `TransportButton`, `PressDepthIconButton`, `EmptyState`. |
| `ui/playback` | `PlaybackViewModel`: queue actions, listen tracking, Desktop Remote hand-off. |
| `ui/theme` | Colours, fonts and the Material 3 theme. See [Theme and design](theme.md). |
| `ui/widget` | The home screen widget (`CassetteWidgetProvider`) and the Quick Settings tile (`PlaybackTileService`). |

Screens take a ViewModel and callbacks; they don't navigate or reach into repositories themselves. Navigation lives in `MainShell`.

## Playback (`data/playback/`)

- **`PlaybackService`** is a Media3 `MediaLibraryService` that owns the ExoPlayer instance. It runs the media session and notification, applies the gapless setting, and serves the browsable library in `MediaLibraryTree` to media browsers.
- **`PlaybackRepository`** connects to the service through a `MediaController` and turns player callbacks into a `StateFlow<PlaybackUiState>`. It also applies crossfade, ReplayGain (with `ReplayGainHelper`) and the volume limit, and works out the current track's format for the quality badge (`extractAudioFormat`).
- **`EqualizerController`** attaches Android's `Equalizer`, `BassBoost` and virtualizer effects to the player's audio session. Presets and levels are stored by `EqualizerSettingsRepository`.
- **`AudioWaveformProcessor`** is a Media3 `AudioProcessor` that feeds the waveform animation in the lyrics view.

The service is the only owner of the player. UI code never talks to ExoPlayer directly; it goes through `PlaybackRepository`.

## Library and sources (`data/library/`, `data/streaming/`)

- **`LocalLibraryRepository`** (`data/library/local`) queries `MediaStore.Audio`, applies the folder filter from `LibraryFolderRepository`, and listens for MediaStore changes so new files appear without a manual rescan.
- **`SubsonicLibraryRepository`** and **`JellyfinLibraryRepository`** (`data/streaming/subsonic`, `data/streaming/jellyfin`) fetch server libraries. `StreamingServerRepository` stores server details; `HttpClient` and `CertificateTrust` handle HTTPS and user-trusted certificates.
- **`LibraryViewModel`** queries every enabled source at once and merges the results into one list of `Song`s. `MusicSource` on each `Song` says where it came from: `Local`, `Subsonic`, `Jellyfin`, `Radio`, `ListeningRoomHost` or `Desktop`.
- **`PlaylistRepository`**, **`FavoritesRepository`** and **`SongMetadataOverridesRepository`** store playlists, likes and user edits to song details. Edits are overrides kept in the app; audio files are never rewritten.
- **`M3uPlaylistFile`** parses and writes `.m3u`/`.m3u8` playlists.

## Other data packages

| Package | Responsibility |
|---|---|
| `data/settings` | `AppPreferencesRepository` (app settings) and `ServiceSettingsRepository` (which online services are on). See [Adding a setting](how-to.md#adding-a-setting). |
| `data/download` | Offline downloads: `SongDownloadService` (a Media3 download service), `DownloadCache` and download settings. |
| `data/radio` | `RadioBrowserApiClient`, radio favourites and history. |
| `data/stats` | `ListeningStatsRepository`: listens, Listening Record and milestones. |
| `data/scrobble` | ListenBrainz and Libre.fm clients and `ScrobbleManager`. |
| `data/backup` | `BackupBundle` (the backup file format) and `BackupRepository`. |
| `data/listeningroom` | Listening Room hosting and joining (`LocalListeningRoomRepository`) and the host's audio relay (`RoomAudioServer`). |
| `data/device` | CassetteCat Desktop: `DesktopDiscovery`, `DesktopRemoteRepository`, `DeviceControlApiClient`, `DesktopRouteProvider` and `DesktopSessionPlayer`. Also the shake, flip and wave gesture detectors, and the client code for the planned hardware player. |
| `data/diagnostics` | `CrashLogRepository`: the on-device crash log. |
| `data/update` | `GitHubUpdateChecker`: checks GitHub for a newer release. |

## Storage

- **Settings and small state** use Jetpack DataStore, for example the `app_preferences` store in `AppPreferencesRepository`.
- **Server passwords and tokens** are encrypted with AES-256-GCM, using a key in the Android Keystore (`data/streaming/CredentialStore.kt`). The key can't leave the device, which is why backups can't include credentials.
- **Android backup** includes app data except credentials, caches, the crash log and scrobbling accounts. The rules are in `res/xml/data_extraction_rules.xml` and `res/xml/backup_rules.xml`.

## CassetteCat Desktop

The desktop runs an HTTP server and the phone is the client. [desktop-remote-protocol.md](desktop-remote-protocol.md) lists every request.

- `DesktopDiscovery` finds computers with a UDP broadcast.
- `DesktopRemoteRepository` handles pairing, polling, likes and playlist sync, listening history and the daily backup.
- `DesktopSessionPlayer` is a Media3 `SimpleBasePlayer` that represents the computer. While the phone controls the desktop, the media session, notification and lock screen drive this player instead of ExoPlayer.
- `DesktopRouteProvider` publishes the computer as a route in Android's output switcher.

## Listening Room

The host registers a `_cassettecat-room._tcp.` service with Android's network service discovery, on a random port, and accepts up to eight guests. Host and guests exchange newline-delimited JSON over TCP, authenticated with a six-character room code. `RoomAudioServer` streams the host's audio, so guests can hear songs that exist only on the host's phone. A room with no guests closes after two minutes.
