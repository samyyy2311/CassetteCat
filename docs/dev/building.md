# Building and testing

## Requirements

- **JDK 17**, for example Eclipse Temurin 17. Gradle and the unit tests both run on Java 17.
- **Android SDK** with platform 37. `compileSdk` and `targetSdk` are 37; `minSdk` is 26 (Android 8.0).
- **Android Studio** is optional but recommended. Open the `app/` folder, not the repository root.

The Gradle project lives in `app/`; the module is `app/app/`. Run every Gradle command from `app/`.

## Build and run

```bash
cd app
./gradlew assembleDebug        # debug APK
./gradlew installDebug         # build and install on a connected phone or emulator
./gradlew testDebugUnitTest    # JVM unit tests
./gradlew lintDebug            # Android lint
```

The debug APK is written to `app/app/build/outputs/apk/debug/app-debug.apk`. It installs as a separate app, **CassetteCat Debug** (`in.caffeinelabs.cassettecat.debug`), beside the released one, so testing never replaces your real install or its data. Its launcher shortcuts still open the released app.

CI runs `testDebugUnitTest`, `assembleDebug` and `lintDebug` on every pull request, so run the same three before pushing.

## Things that trip people up

- **The package name starts with `in`**, which is a Kotlin keyword. Package declarations and imports need backticks:

  ```kotlin
  package `in`.caffeinelabs.cassettecat.ui.screens.home
  ```

- **No Kotlin Android plugin.** Android Gradle Plugin 9 compiles Kotlin itself. Adding `org.jetbrains.kotlin.android` fails the build with "Plugin was not found" or "Kotlin plugin is no longer required". The module applies only `kotlin-compose` and `kotlin-serialization`.
- **`Unsupported class file major version`** means Gradle is running on a different Java version. Point `JAVA_HOME` at JDK 17.
- **Dependency versions** are in `app/gradle/libs.versions.toml`. Dependabot proposes updates weekly; minor and patch updates arrive grouped in one pull request.

## Tests

Unit tests are in `app/app/src/test/java/in/caffeinelabs/cassettecat/CoreLogicTest.kt` and run on the JVM with JUnit 4, Mockito and Robolectric. There are no instrumented tests on a device.

Test pure logic: parsing, matching, sorting, state calculations. Prefer real objects over mocks, and mock only Android or network boundaries.

## Versions

`versionName` and `versionCode` come from, in order of priority:

1. **CI**: the release workflow sets `GITHUB_REF_NAME` (a tag such as `v1.8.0`) and `VERSION_CODE`.
2. **Gradle properties**: `./gradlew assembleRelease -PversionName=1.8.0 -PversionCode=26`.
3. **Defaults** in `app/app/build.gradle.kts`, used for everyday debug builds.

## Release signing

A release build without signing details still builds, but unsigned. To sign locally, create `app/keystore.properties`. It's listed in `.gitignore`; never commit it.

```properties
RELEASE_STORE_FILE=/absolute/path/to/release.keystore
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

Then run `./gradlew assembleRelease bundleRelease`. The same values can come from the environment variables `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`, which is how CI provides them.

When `REQUIRE_PRODUCTION_SIGNING=true`, a release build fails unless signing details and a valid `VERSION_CODE` are present. CI sets this so an unsigned APK can never be published by mistake. See [Releasing](releasing.md).

Release builds are minified and resource-shrunk with R8. Keep rules are in `app/app/proguard-rules.pro`.
