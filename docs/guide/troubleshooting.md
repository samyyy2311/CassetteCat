# Troubleshooting

## Library

### My songs don't show up

Work through these in order:

1. **Permission**: open Android **Settings > Apps > CassetteCat > Permissions** and make sure **Music and audio** is allowed.
2. **Scan folders**: open **Settings > Library & Hardware > Scan Folders**. With **Only these folders**, the folder holding your music must be in the list. With **Everything except these folders**, it must not be.
3. **Short files**: **Settings > Customisation > Storage & Cache > Ignore Short Audio Clips** hides anything under 30 seconds.
4. **Source filter**: in the Library, open the refine button and check the source isn't set to a server only.
5. **Rescan**: pull down on the Library screen.

If a file still doesn't appear, Android's media scanner may not have indexed it yet. Restarting the phone makes Android scan again.

### A song shows the wrong title, artist or cover

- **Title or artist**: use **Edit Details** in the song's options. Your changes stay in CassetteCat and don't touch the file. **Revert to Original Tags** undoes them.
- **Cover**: use **Search Album Cover Online**, or pick an image from your phone. **Reset to Original Cover** goes back to the cover embedded in the file.

### The same song appears twice

You probably have two copies of the file. **Settings > Customisation > Storage & Cache > Duplicates** lists them with their bitrates so you can delete one.

## Servers and streaming

### My Subsonic or Jellyfin server won't connect

1. Open the server address in your phone's browser. If the browser can't reach it, CassetteCat can't either. Check the phone is on the right network, or that the server is reachable from outside your home if you're away.
2. Include the port if your server uses one, for example `http://192.168.1.20:4533` for Navidrome's default port.
3. Use `https://` if the server has a certificate. For `http://` addresses, CassetteCat warns you first; continue only for a server on your own network.
4. For a self-signed certificate, check the fingerprint in the **Untrusted certificate** message matches your server, then tap **Trust & Connect**.
5. Make sure **Offline Blackout Mode** is off.

Older Subsonic servers may refuse the sign-in. CassetteCat uses token sign-in, which needs Subsonic API 1.13.0 or newer.

### A server shows "Unavailable"

The row in Settings says why and when it last checked. Once the server is back, tap **Retry**.

### Downloads don't start

- **Settings > Library & Hardware > Downloads > Wi-Fi only** makes downloads wait for Wi-Fi.
- The **Storage limit** may be full. Remove old downloads or raise the limit.

## Playback

### Music stops when the screen is off

Some phones close apps in the background to save battery. In Android **Settings > Apps > CassetteCat > Battery**, choose **Unrestricted**. The wording differs between phone makers.

### Songs skip on their own

**Shake to Skip** or **Wave to Skip** may be on. Turn them off in **Settings > Customisation > Now Playing & Gestures**, or set **Shake Sensitivity** to Firm or Strong.

### Some songs are much louder than others

Turn on **Settings > Customisation > Audio Engine > Volume Normalisation (ReplayGain)**. It works on songs with ReplayGain tags; adjust **ReplayGain Pre-Amp Gain** for songs without them.

### Music is quieter than expected

**Volume Limit (Ear Protection)** in **Settings > Customisation > Audio Engine** caps the volume. Raise **Maximum Volume** or turn it off. Check the equalizer's **Preamp Gain** too.

### The equalizer does nothing

The equalizer attaches to the song that's playing. If it says **Audio DSP Idle**, start a song and open it again.

### Lyrics are missing or out of time

- **No lyrics**: check **LrcLib** is on in **Settings > External Services**, then try **Search & match different lyrics**.
- **Wrong lyrics**: use **Search & match different lyrics** and pick a closer match.
- **Lyrics early or late**: use **Tune lyrics sync** to shift them in 100 ms steps.

## CassetteCat Desktop

### My computer isn't listed

- Both devices must be on the same Wi-Fi network. Guest networks and some office or hotel networks stop devices from seeing each other.
- On the computer, check CassetteCat Desktop is open and **Settings > Network & Services > Control From Your Phone** is on.
- Check a firewall on the computer isn't blocking CassetteCat. On Windows, if a firewall prompt appeared when you turned on Phone Remote, allow access on private networks.
- If it still isn't found, [pair by address](desktop.md#pair-by-address-instead).

### "That code doesn't match the one on the computer"

Copy the address again from the computer and enter all of it, including the part after `#`. After ten wrong codes in a row the computer refuses all requests for one minute, so wait a minute before trying again.

### "The pairing code has changed"

Someone clicked **New Code** on the computer. Enter the new code shown under **Settings > Network & Services** on the computer.

### A song didn't move to the other device

Songs are matched by title and artist. If the other library has different tags for the song, or doesn't have it, it can't move. Make the title and artist match.

### Backups to the computer fail

The computer must be on, with CassetteCat Desktop open, on the same Wi-Fi. The phone tries again the next day, or tap **Back Up to Computer** in **Settings > Backup & Restore**.

## Listening Room

### I can't find the room

Some networks stop devices from finding each other. Ask the host for the address on their screen and use **Join by address**.

### The room closed by itself

A room closes if nobody joins within two minutes. Start it again when your guests are ready.

## Reporting a problem

1. Check the [open issues](https://github.com/samyyy2311/CassetteCat/issues) to see if it's already reported.
2. If the app crashed, open **Settings > Privacy & Security > Crash Log** and tap **Share Crash Log**.
3. [Open a bug report](https://github.com/samyyy2311/CassetteCat/issues/new?template=bug_report.yml) with your app version, phone and Android version, what you did and what happened.

Remove server addresses, passwords and other personal details from anything you attach.
