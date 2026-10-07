# Streaming, downloads and radio

## Connecting a Subsonic or Navidrome server

CassetteCat works with servers that speak the Subsonic API, including [Navidrome](https://www.navidrome.org/), gonic and Airsonic.

1. Open **Settings > Streaming & Services > Subsonic**.
2. Enter the **Server URL**, for example `https://music.example.com` or `http://192.168.1.20:4533`.
3. Enter your **Username** and **Password**, then tap **Connect**.

When it works, the row in Settings shows the server name and how many songs it has, and the songs appear in your Library alongside the ones on your phone. Use the source filter in the Library to show only one.

## Connecting a Jellyfin server

1. Open **Settings > Streaming & Services > Jellyfin**.
2. Enter the **Server URL**.
3. Either enter your username and password, or tap **Sign in with Quick Connect**. Quick Connect shows a code; enter it in Jellyfin under your profile's **Quick Connect** page. No password is typed on the phone.

## Security warnings when connecting

- **Use HTTPS if possible**: you see this when the URL starts with `http://`. Your password travels unencrypted, so only continue for a server on your own network.
- **Untrusted certificate**: you see this for a server with a self-signed certificate, which is common for home servers. It shows the certificate's fingerprint. Tap **Trust & Connect** only if it matches your server's certificate.

Passwords and tokens are encrypted on the phone with the Android Keystore. To remove them all, use **Settings > Privacy & Security > Disconnect all servers and remove credentials**.

If a server stops responding, its row in Settings says **Unavailable** with the reason. Tap **Retry** once it's back.

## Downloads

Downloading saves a streamed song on your phone so it plays without a connection.

- **One song**: tap the three dots beside it, then **Download**.
- **An album or playlist**: **Download Album** or **Download All** in its options.
- **Liked songs**: **Settings > Library & Hardware > Downloads > Download Liked Songs**.

The **Downloads** screen also has:

- **Wi-Fi only**: waits for an unmetered network before downloading.
- **Auto-download Favorites**: downloads streamed songs as soon as you like them.
- **Storage limit**: 512 MB, 1 GB, 2 GB, 5 GB or 10 GB. A new limit takes effect the next time the download cache starts.
- **Remove oldest download** and **Remove all downloads** to free space.

Separately, CassetteCat keeps a cache of recently streamed audio so replays don't use data again. Set its size in **Settings > Customisation > Storage & Cache > Streaming Cache Quota**.

## Internet radio

The **Radio** tab plays live stations from [Radio Browser](https://www.radio-browser.info), a free directory of tens of thousands of stations.

- **Search** by station name.
- **Filter** by country, state or province, language, or genre tag. Active filters show at the top; **Clear all** removes them.
- **Sort** by popularity, trending, name, country or bitrate.
- Tap the heart on a station to add it to **Favorites**. **Recently played** keeps the last stations you listened to.
- **Add custom station** plays any stream. Enter a name and a stream URL starting with `http://` or `https://`.

Radio favourites are also available in Android Auto and from the app's home screen shortcut. See [Car, widgets and shortcuts](car-widgets-shortcuts.md).

## Online services

**Settings > Streaming & Services > External Services** lists every online service CassetteCat can use. Each can be turned off on its own:

| Service | Used for |
|---|---|
| **Deezer** | Artist images and album artwork |
| **TheAudioDB** | Artist images when Deezer has none |
| **LrcLib** | Synced lyrics when a song has none |
| **Cover Art Archive** | CD and vinyl artwork from MusicBrainz |
| **Wikipedia** | The About text for artists and albums |
| **GitHub** | Checking for new app versions |
| **Radio Browser** | Searching and browsing radio stations |

**Offline Blackout Mode** in the same section turns off every network feature at once: streaming, online lookups, radio, update checks and the connection to CassetteCat Desktop. Songs on your phone and downloaded songs still play.

## Scrobbling

Scrobbling sends what you listen to to an online profile. Open **Settings > Streaming & Services > Scrobbling**:

- **ListenBrainz**: tap **Connect Account** and paste your user token. Find it on [listenbrainz.org](https://listenbrainz.org) under your profile settings.
- **Libre.fm**: tap **Connect Account** and log in with your Libre.fm username and password.

Both receive finished listens and what's playing right now. **Disconnect** removes the account from the phone.
