# Backup, restore and privacy

## What a backup contains

A CassetteCat backup holds your playlists, favourites, listening statistics, folder filters and settings.

It does not hold:

- **Your music files.** Copy those yourself.
- **Server passwords and tokens.** They're encrypted with a key that never leaves your phone, so they can't be exported. After restoring, sign in to Subsonic or Jellyfin again.

## Back up to a file

1. Open **Settings > Data & Updates > Backup & Restore**.
2. Tap **Create Backup** and choose where to save the file, for example your Downloads folder or a cloud drive.

Make a new backup before changing phones or uninstalling the app.

## Restore from a file

1. On the new phone, install CassetteCat.
2. During setup, tap **Restore from Backup** on the **Make it yours** step. Or, later, open **Settings > Backup & Restore > Restore from Backup**.
3. Pick the backup file.

Restoring replaces the playlists, favourites and settings on the phone. It can't be undone, so back up first if the phone already has data you want to keep.

## Back up to your computer

If your phone is paired with CassetteCat Desktop, it backs up to the computer once a day when you open the app. See [Using CassetteCat with your computer](desktop.md#back-up-to-your-computer).

## Android's own backup

If Android backup is turned on for your phone, Android also saves CassetteCat's data with your Google account and restores it when you set up a new phone. Saved server credentials, cached files, the crash log and scrobbling accounts are excluded.

## Privacy

CassetteCat has no analytics, no advertising and no tracking. It doesn't use Google Play Services.

- **Listening data** is stored only on your phone. Turn it off with **Settings > Privacy & Security > Collect listening activity**.
- **Server credentials** are encrypted with the Android Keystore. **Disconnect all servers and remove credentials** deletes them.
- **Crashes** are written to a log on your phone and never sent anywhere. To report a bug, open **Privacy & Security > Crash Log** and use **Share Crash Log** to send it yourself.
- **Online services** such as cover art, lyrics and radio each have a switch in **Settings > External Services**. **Offline Blackout Mode** turns off all of them at once. See [Streaming, downloads and radio](streaming-and-radio.md#online-services).

The full [privacy policy](https://cassettecat.caffeinelabs.in/privacy.html) covers what each online service receives.
