# How to

Step-by-step recipes for common changes. Paths are relative to `app/app/src/main/java/in/caffeinelabs/cassettecat/` unless they start with `app/`.

## Adding a setting

The **Gapless Playback** switch is a complete small example to copy. A setting touches five places.

1. **Store it** in `data/settings/AppPreferencesRepository.kt`:
   - add a DataStore key, for example `private val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")`;
   - add a field with its default to `data class AppPreferences`, for example `val gaplessPlayback: Boolean = true`;
   - read it in the `preferences` flow: `gaplessPlayback = prefs[GAPLESS_PLAYBACK] ?: true`;
   - add a setter: `suspend fun setGaplessPlayback(enabled: Boolean)`.
2. **Back it up**: add the same field to `BackupAppPreferences` in `data/backup/BackupBundle.kt`, copy it in `exportForBackup()` and write it back in `restoreFromBackup()`. Give the backup field a default, so backups made before your change still restore.
3. **Expose it** in `ui/screens/settings/SettingsViewModel.kt` with a function that launches the setter:

   ```kotlin
   fun setGaplessPlayback(enabled: Boolean) {
       viewModelScope.launch { appPreferencesRepository.setGaplessPlayback(enabled) }
   }
   ```

4. **Show it**: add a row to the right screen, usually in `ui/screens/settings/CustomizationScreen.kt`:

   ```kotlin
   ToggleRow(
       title = stringResource(AppR.string.customization_gapless),
       subtitle = stringResource(AppR.string.customization_gapless_description),
       checked = prefs.gaplessPlayback,
       onCheckedChange = viewModel::setGaplessPlayback,
       iconRes = R.drawable.lucide_ic_disc,
   )
   ```

5. **Use it** where it takes effect, by collecting `AppPreferencesRepository.preferences`. Gapless is applied in `data/playback/PlaybackService.kt`.

Add the title and description strings as described in [Adding text](#adding-text).

## Adding a screen

1. Write the screen as a `@Composable` in the matching `ui/screens/<area>/` package. Take a ViewModel and an `onBack` callback, and pass any navigation out as callbacks instead of navigating from inside the screen.
2. Add a route constant to `object MainRoute` in `ui/navigation/MainShell.kt`, for example `const val DUPLICATES = "main/settings/duplicates"`.
3. Register it in the same file's `NavHost`:

   ```kotlin
   composable(MainRoute.DUPLICATES) {
       DuplicatesScreen(
           libraryViewModel = libraryViewModel,
           onBack = { navController.popBackStack() },
           listBottomPadding = contentPadding.calculateBottomPadding()
       )
   }
   ```

4. Open it from another screen through a callback that calls `navController.navigate(MainRoute.YOUR_ROUTE)`.

Pass `listBottomPadding` through to scrolling lists so the last item isn't hidden behind the mini-player.

## Adding text

Every string the user can see goes in a resource file in `app/app/src/main/res/values/`. Strings are split by area, for example `strings_library.xml` or `strings_customization.xml`; add to the file for the screen you're changing. Don't hardcode text in Kotlin.

- Brand and service names that must not be translated get `translatable="false"`.
- Use `<plurals>` for counts such as "3 songs".
- Read [TRANSLATING.md](../../TRANSLATING.md) before adding or changing translations.

## Changing the backup format

`BackupBundle` is written as JSON and must restore in every later version of the app. CassetteCat Desktop keeps copies of the phone's backups without reading them, so old backups can come back months later.

- **Add** fields with default values. Old backups then still load.
- **Don't rename or remove** fields; old backups would lose that data on restore.
- If the meaning of existing data changes, raise `version` in `BackupBundle` and handle older versions when restoring.

## Changing the Desktop Remote API

The phone's client (`data/device/DeviceControlApiClient.kt`) and the desktop's server (`src/remote_control.cpp` in the desktop repository) must agree. Update [desktop-remote-protocol.md](desktop-remote-protocol.md) in the same pull request. Phones and computers update at different times, so new fields should be optional on both sides.
