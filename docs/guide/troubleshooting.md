# Troubleshooting & Common Issues

## 1. Network & Server Connections

### Cleartext (HTTP) Connections on Local LAN
* **Symptom**: Inability to connect to local Subsonic or Jellyfin servers running on HTTP (non-HTTPS).
* **Explanation**: Android blocks unencrypted cleartext HTTP traffic by default. CassetteCat explicitly configures `android:usesCleartextTraffic="true"` in `AndroidManifest.xml` to allow connecting to self-hosted LAN media servers. If using custom Android builds or profiles, ensure cleartext traffic is permitted.

### Subsonic Salt & Token Authentication
* **Symptom**: Authentication failure on older Subsonic servers.
* **Explanation**: CassetteCat uses the modern Subsonic token authentication protocol (`md5(password + salt)`). Ensure your Subsonic-compatible server (e.g. Navidrome, Gonic, Airsonic) is configured to support API version 1.13.0 or newer.

---

## 2. Storage & Permissions

### Missing Audio Files in MediaStore
* **Symptom**: Local audio files located in custom folders do not appear in the library.
* **Resolution**:
  1. Check **Settings > Library & Media > Manage Scan Folders**.
  2. Ensure the directory containing your music is added to the folder whitelist and not excluded in the blacklist.
  3. On Android 13+, confirm that `READ_MEDIA_AUDIO` permission has been granted in system settings.

---

## 3. Build & Toolchain

### AGP 9.0 Kotlin Plugin Error
* **Symptom**: Build fails with `Plugin [id: 'org.jetbrains.kotlin.android'] was not found` or `Kotlin plugin is no longer required with AGP 9.0`.
* **Resolution**: Android Gradle Plugin 9.0 includes Kotlin support natively. Do not apply `org.jetbrains.kotlin.android` in `build.gradle.kts`; only apply `libs.plugins.kotlin.compose` and `libs.plugins.kotlin.serialization`.

### Missing JDK 17
* **Symptom**: `Unsupported class file major version` during `./gradlew assembleDebug`.
* **Resolution**: Ensure your `JAVA_HOME` environment variable points to a valid JDK 17 installation (such as Eclipse Temurin 17).
