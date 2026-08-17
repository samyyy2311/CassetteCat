# Android Development Guide

## Environment Setup

### Requirements
* **Android Studio**: Ladybug (2024.2.1) or newer
* **JDK**: Version 17 (e.g. Eclipse Temurin)
* **SDK Tools**:
  * `compileSdk`: 37
  * `minSdk`: 26 (Android 8.0 Oreo)
  * `targetSdk`: 37

### Build System & Toolchain
* **Android Gradle Plugin (AGP)**: 9.x (Kotlin compiler is integrated directly into AGP 9.0; do not add the standalone `org.jetbrains.kotlin.android` plugin).
* **Package Identifier**: `in.caffeinelabs.cassettecat`. Because `in` is a reserved keyword in Kotlin, all package declarations and imports must use backticks:
  ```kotlin
  package `in`.caffeinelabs.cassettecat
  ```

---

## Build Commands

From the `app/` directory:

```bash
# Compile and build the debug APK
./gradlew assembleDebug

# Run JVM unit tests
./gradlew testDebugUnitTest

# Run Android lint
./gradlew lintDebug

# Build unsigned release APK
./gradlew assembleRelease
```

---

## Design System & Theming

CassetteCat uses the **Owned Device** design system:

### 1. Color Palette (`ui/theme/Color.kt`)
* **Background**: `#000000` (Pure Black)
* **Surface Panels**: `#1C1A18` (Dark Charcoal)
* **Metal Surfaces / Primary**: `#C4C4C0` (Silver Neutral)
* **Text Primary**: `#F5F0EC` (Off-white)
* **Text Secondary**: `#A8A29A` (Muted)
* **Active Indicator / Accent**: `#C23B30` (Signal Red) — assigned to `colorScheme.tertiary` and `colorScheme.error`. Reserved strictly for active indicators and transport accents.

### 2. Typography (`ui/theme/Type.kt`)
* **Body / UI Labels**: IBM Plex Sans
* **Timestamps / Technical Readouts**: IBM Plex Mono

### 3. Tactile Components
* `TransportButton.kt`: Circular push button with 3D gradient cap, active border, and press-depth physics.
* `PressDepthIconButton.kt`: Bare icon button maintaining identical tactile press-depth animations and haptic feedback.

---

## Release Signing

Release signing should be performed using a private keystore configured locally or via CI environment variables:

1. Create a `keystore.properties` file in `app/` (this file is excluded by `.gitignore`):
   ```properties
   storeFile=/path/to/release.keystore
   storePassword=your_keystore_password
   keyAlias=your_key_alias
   keyPassword=your_key_password
   ```
2. For local release builds, configure your signing config in `app/app/build.gradle.kts` referencing these properties.
