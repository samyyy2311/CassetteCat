# Architecture

CassetteCat is a single Android app in `app/`. It plays music from the phone, from Subsonic and Jellyfin servers, and from internet radio. It can also pair with [CassetteCat Desktop](https://github.com/samyyy2311/CassetteCat-Desktop) on the same Wi-Fi network.

## UI

* **Framework**: Jetpack Compose with Material 3 components and the app's own theme (see [android.md](android.md)).
* **Navigation**: One activity (`MainActivity`) hosts `CassetteCatNavHost` and the bottom-sheet shell (`MainShell`).
* **Transitions**: Linear 220 ms slide transitions (`MechanicalTransitions.kt`).
* **Controls**: `TransportButton` for transport actions and `PressDepthIconButton` for navigation and action rows.

## Playback

* **Engine**: AndroidX Media3 ExoPlayer, hosted in `PlaybackService`, a `MediaLibraryService`. The library it exposes is what Android Auto browses.
* **State**: `PlaybackRepository` turns Media3 `Player.Listener` callbacks into Kotlin `StateFlow` streams for the UI.
* **System controls**: The media notification, lock screen controls and the system output switcher all go through the Media3 session.

## Data

* **Library**: `LibraryViewModel` queries every configured source at once and merges the results:
  * `LocalLibraryRepository` reads `MediaStore.Audio`, limited to the folders picked with the Storage Access Framework.
  * `SubsonicLibraryRepository` talks to Subsonic-compatible servers using salt and token authentication.
  * `JellyfinLibraryRepository` talks to Jellyfin's REST API with an access token.
* **Storage**:
  * Settings, server details and listening statistics are kept in Jetpack DataStore.
  * Server passwords and tokens are encrypted with AES-256-GCM using a key held in the Android Keystore (`CredentialStore.kt`).

## CassetteCat Desktop

The phone and the desktop app talk over HTTP on the local network. The desktop runs the server; the phone is the client. [desktop-remote-protocol.md](desktop-remote-protocol.md) lists every request.

* **Finding the computer**: `DesktopDiscovery` broadcasts a UDP probe, and each running desktop replies with its name and port.
* **Pairing**: The phone asks to pair, the person at the computer allows it, and the phone receives a six-character code. Every later request carries that code.
* **Control**: `DesktopRemoteRepository` polls the desktop's playback state and sends play, pause, skip, seek, volume and queue commands through `DeviceControlApiClient`.
* **Output switcher**: `DesktopRouteProvider` lists the paired computer in Android's output switcher, so playback can move to it from the system media controls.
* **Sync**: Likes, playlists, listening history and a backup of the app's data are exchanged with the paired computer.
