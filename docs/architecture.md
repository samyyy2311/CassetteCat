# System Architecture

CassetteCat is designed around two main components:
1. **Android Application (`app/`)**: A native music player that operates both as a standalone player and as a companion for physical devices.
2. **Hardware Player & Firmware (`hardware/`, `firmware/`)**: A dedicated open-hardware portable audio player powered by an ESP32-S3 microcontroller.

```
┌─────────────────────────────────────────────────────────────┐
│                    CassetteCat System                       │
├───────────────────────────────┬─────────────────────────────┤
│       Android Companion       │       Hardware Player       │
│                               │                             │
│  ┌─────────────────────────┐  │  ┌────────────────────────┐ │
│  │   Jetpack Compose UI    │  │  │  ST7735R 128x160 LCD   │ │
│  └────────────┬────────────┘  │  └───────────┬────────────┘ │
│               │               │              │              │
│  ┌────────────▼────────────┐  │  ┌───────────▼────────────┐ │
│  │    Media3 / ExoPlayer   │  │  │    PCM5102 I2S DAC     │ │
│  └────────────┬────────────┘  │  └───────────┬────────────┘ │
│               │               │              │              │
│  ┌────────────▼────────────┐  │  ┌───────────▼────────────┐ │
│  │     Data Layer          │  │  │    ESP32-S3 Micro      │ │
│  │ Local/Subsonic/Jellyfin │  │  │ HTTP Server / SD Card  │ │
│  └────────────┬────────────┘  │  └───────────┬────────────┘ │
│               │               │              │              │
│               └──────── Wi-Fi Sync ──────────┘              │
│                    (SoftAP / mDNS)                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 1. Android Application Architecture

The Android app follows standard MVVM architecture with a single-direction data flow and a lightweight repository layer.

### UI Layer
* **Framework**: Jetpack Compose using Material 3 base components styled under the "Owned Device" design system.
* **Navigation**: Single-activity architecture (`MainActivity`) hosting `CassetteCatNavHost` and the bottom-sheet shell (`MainShell`).
* **Transitions**: Non-spring, linear `220ms` mechanical slide transitions (`MechanicalTransitions.kt`).
* **Components**: Custom tactile controls (`TransportButton` for transport actions and `PressDepthIconButton` for navigation/action rows) simulating mechanical push buttons.

### Playback Layer
* **Audio Engine**: AndroidX Media3 (`ExoPlayer`) hosted inside a bound `MediaSessionService` (`PlaybackService.kt`).
* **State Bridge**: `PlaybackRepository.kt` connects Media3 `Player.Listener` callbacks with Kotlin Coroutine `StateFlow` streams.
* **Controls**: Foreground service notification with media playback actions, lock-screen controls, and system media routing.

### Data Layer
* **Library Aggregation**: `LibraryViewModel` concurrently queries all configured music sources and aggregates them into a unified list:
  * `LocalLibraryRepository`: Queries Android `MediaStore.Audio` with folder filtering via Storage Access Framework.
  * `SubsonicLibraryRepository`: Interfaces with Subsonic-compatible APIs (salt/token authentication).
  * `JellyfinLibraryRepository`: Communicates with Jellyfin REST endpoints via token authentication.
* **Storage**:
  * Preferences, server configurations, and play statistics are stored using Jetpack DataStore (`PreferencesDataStore`).
  * Sensitive credentials (server passwords and auth tokens) are encrypted via AES-256/GCM using keys stored in the hardware-backed `AndroidKeyStore` (`CredentialStore.kt`).

---

## 2. Hardware and Connectivity Architecture

The hardware player operates independently from an SD card, while offering Wi-Fi connectivity for synchronization with the Android app.

### Communication Channels
* **SoftAP Mode**: The ESP32 creates a local Wi-Fi hotspot. The Android app connects using `WifiNetworkSpecifier` without requiring an existing router.
* **Station Mode**: The ESP32 joins the local home Wi-Fi network and advertises its service via mDNS/NSD.
* **Protocol**: HTTP/REST endpoints hosted on the ESP32 for library synchronization, file upload to SD card, and firmware OTA updates (`esp_https_ota`).
