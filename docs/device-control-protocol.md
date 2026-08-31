# Device Control Protocol

## What this is

Beyond syncing songs (see `device-sync-protocol.md`), the app can also
control the hardware player remotely and manage it: play, pause, and skip
it from your phone, browse the files on its SD card, rename it, restart it,
and push a firmware update to it. This document lists the HTTP requests the
app sends to do each of those things, and exactly what it expects back.

Same situation as the sync protocol. This is a contract the app side has
already fully built, written ahead of the firmware that will actually
answer these requests. The firmware doesn't exist yet, the toolchain itself
hasn't even been chosen. This document is the spec to build that firmware
against later, so nothing has to be reverse-engineered from the Kotlin
code.

Every request here goes to the same on-device web server as pairing
(`/api/status`, `/api/wifi`) and song sync. There's one HTTP server on the
device, not several.

---

## Authentication and transport

None of this is built yet, and it needs to be before any of these endpoints
actually run on real firmware. Right now the wire contract above has no
credential on any request, which is fine for writing the contract but not
fine for a device that can factory-reset itself, delete arbitrary files, or
flash new firmware because anyone on the same Wi-Fi network sent it a
request.

Firmware needs a per-device pairing token, issued once during the initial
pairing handshake (the same moment `/api/status` first succeeds) and then
required on every request after that, factory reset, file delete, Wi-Fi
mode changes, and both OTA endpoints included, not just the destructive
ones. A request without a valid token should be rejected outright.

The connection itself also needs to move to HTTPS (self-signed is fine for
a SoftAP/LAN device like this) instead of plain HTTP, so the token and any
file contents aren't sent in the clear over Wi-Fi.

---

## Remote playback control

This is what powers the "Now Playing on Device" screen: play, pause, and
skip buttons plus a volume slider that control the hardware player's own
playback (not the phone's), and shows what track it's currently on.

`GET /api/playback` asks "what are you playing right now?" The device
answers with its current playback state:

```json
{
  "isPlaying": true,
  "trackTitle": "One More Time",
  "trackArtist": "Daft Punk",
  "positionMs": 12000,
  "durationMs": 210000,
  "volumePercent": 70,
  "shuffleEnabled": false,
  "repeatMode": 0
}
```

The app asks this every couple of seconds while that screen is open, and
stops asking the moment you leave the screen or put the phone in your
pocket. More on that below.

`repeatMode` is a number: 0 means repeat off, 1 means repeat one song, 2
means repeat everything. These exact numbers match Android's own Media3
library convention, so the app's existing "which repeat icon to show" logic
works without needing a translation step.

`POST /api/playback` means "do this." The body is one word describing the
action:
```json
{ "action": "play" }
```
Valid actions: `play`, `pause`, `next`, `previous`, `toggle_shuffle`,
`cycle_repeat`.

`POST /api/volume` sets the device's own playback volume:
```json
{ "percent": 70 }
```

`POST /api/seek` jumps to a specific point in the current song:
```json
{ "positionMs": 45000 }
```
(that example jumps to 45 seconds in)

### Why polling, and why it stops

There's no live push connection here, like a WebSocket. The app just asks
"what's playing?" over and over, a plain request then answer, every couple
of seconds. That's the simplest thing that works, and it genuinely only
runs while you have that screen open and the app is in the foreground. If
you background the app or lock your phone while looking at it, the asking
stops immediately, and picks back up the moment you return. That matters
because it means the phone isn't quietly draining battery and Wi-Fi polling
a device you're not even looking at.

---

## Device management

Things you'd do occasionally, not every day. Found on the Device Settings
screen in the app.

- `POST /api/device/name` body `{ "name": "My Player" }`: give the device a
  custom name (shown in the app instead of the generic default).
- `POST /api/wifi/mode` body `{ "mode": "softap" }` or
  `{ "mode": "station" }`: switch the device back to broadcasting its own
  Wi-Fi hotspot (`softap`), or tell it to join a home network instead
  (`station`). Actually joining a specific home network, giving it the
  Wi-Fi password, is a separate, already-existing request (`/api/wifi`).
  This endpoint is just for switching modes.
- `POST /api/device/restart`: no body. A plain reboot, nothing is erased.
  Same as unplugging and plugging back in.
- `POST /api/device/reset`: no body. Factory reset, erases every song and
  setting on the device. The app always shows a confirmation dialog before
  calling this. Firmware doesn't need to double-confirm on its side, but
  should treat this as genuinely destructive and not reversible.
- `POST /api/library/rescan`: no body. Tells the device to re-scan its own
  SD card from scratch. Useful if someone copied song files onto the card
  directly, using a card reader on a computer, instead of using the app's
  sync feature. The device wouldn't otherwise know those files exist until
  it re-scans.
- `POST /api/device/time` body `{ "epochMs": 1735689600000 }`: sets the
  device's internal clock to this Unix timestamp (milliseconds since
  January 1, 1970, the standard way computers represent the current time
  as one big number). This exists because the hardware's parts list has no
  battery-backed real-time-clock chip, so the device's clock resets to zero
  every time it loses power. The app sends its own current phone time so
  file timestamps on the device stay roughly correct.

All of the above return a simple `{ "ok": true }`, or `false` with an
`error` message. Same shape as the sync protocol's responses.

---

## Storage browser

Lets you look at what files are actually on the device's SD card, and
delete individual ones. Separate from the song-sync feature, which only
handles adding songs.

`GET /api/files?path=` asks "what's in this folder?" One folder level at a
time, not a deep recursive listing. The app calls this again each time you
tap into a subfolder:

```json
[
  { "name": "Discovery", "path": "Daft Punk/Discovery", "isDirectory": true, "sizeBytes": 0 },
  { "name": "cover.jpg", "path": "Daft Punk/cover.jpg", "isDirectory": false, "sizeBytes": 84213 }
]
```

An empty (or omitted) `path=` means "list the root folder." Entries with
`isDirectory: true` are folders you can tap into for another
`GET /api/files?path=...` call. Entries with `isDirectory: false` are
actual files, shown with their size.

`DELETE /api/files?path=...` deletes whatever's at that exact path. If the
path is a folder, firmware deletes it recursively along with everything
inside it, this isn't left as an implementation detail, both the app's
confirmation dialog and the storage browser assume deleting a folder
actually removes its contents.

Before deleting anything, firmware must resolve `path` to a canonical path
(no `..` segments, no symlink tricks, no equivalent-but-differently-written
path) and confirm it's still underneath the device's music storage root.
Anything that resolves outside that root gets rejected, not deleted. The
app shows a confirmation dialog before calling this, same as factory reset.

---

## Firmware updates (OTA, over the air)

Two different ways an update can happen, because the primary way needs
something that doesn't exist publicly yet.

### The primary way: the device downloads it itself

`POST /api/ota/from-url` body:
```json
{ "url": "https://github.com/.../firmware.bin", "sha256": "<hex digest>" }
```

The app checks GitHub for the latest published release of this project,
looks for a `.bin` file attached to it, and if it finds one, sends the
device that file's public download link. `sha256` is the digest GitHub
itself publishes for that release asset (the Releases API returns a
`digest` field per asset), not something the app computes, so there's no
separate companion file to keep in sync with the `.bin`. The device then
downloads the file itself directly from GitHub, computes its own SHA-256
over what it received, and compares it against `sha256` before flashing.
The phone is just the messenger telling it where to look and what to
expect, not something the file passes through. This is the standard way
ESP32 devices are supposed to update (`esp_https_ota`, a built-in ESP-IDF
feature), simpler and more robust than routing a big file through the
phone.

### The fallback way: the app sends the file directly

`POST /api/ota`, a multipart upload (same style as song sync), with a
`file` part containing the firmware `.bin`, and an `X-Firmware-Sha256`
header carrying the app-computed SHA-256 digest of that same file. Used
when there's a build to install that isn't a public GitHub release yet,
for example a developer testing a work-in-progress firmware build before
it's officially published. Firmware hashes the bytes it actually received
and compares against the header before flashing, so a corrupted transfer
gets rejected instead of flashed.

Both ways respond the same:
```json
{ "ok": true, "error": null }
```
and the device flashes the new firmware and reboots on success. Firmware
must not flash a `.bin` just because it arrived: both paths above carry a
digest specifically so firmware can verify the received bytes before
flashing, and an image that fails validation gets rejected with
`{ "ok": false, "error": "..." }`, not flashed. For the manual upload path,
the trust boundary is "this device already trusts whoever holds its
pairing token" (per the authentication section above); the digest there is
about catching a corrupted transfer, not about who's allowed to upload.

### One thing worth knowing about the GitHub check

The app checks GitHub's "latest release" for the whole project, which today
mostly means app releases (APK files), since firmware doesn't exist yet.
Once firmware starts shipping its own releases too, if an app release and a
firmware release end up interleaved, whichever one happened most recently,
the app might check "latest release" and find an app release with no
firmware `.bin` attached. It already handles that gracefully, showing "no
firmware releases yet" rather than an error, but it's not ideal. Once
firmware releases become a regular thing, it's worth giving them their own
tag pattern, something like `firmware-v1.0.0` instead of sharing the app's
`v1.0.0` tags, so this check always finds the right one.

---

## What's deliberately not built yet

- Byte-by-byte progress for firmware updates. The app shows "updating,
  please wait," not a percentage bar. Same reasoning as song sync: keep
  things simple for now, revisit if it's ever actually annoying.
- A live push connection (WebSocket) for instant status updates. Right now
  everything here is "ask, then get an answer." Nothing on the device
  pushes information to the app on its own. That's simpler to build first.
  A live connection would only be worth the added complexity later, if
  polling every couple of seconds ever turns out not to feel responsive
  enough.
- Equalizer or audio tuning controls on the device itself, remapping the
  physical buttons, or controlling its screen. None of these have a
  contract yet because the hardware side hasn't decided what's even
  possible. The current board (PCM5102 DAC, no dedicated amplifier) and
  firmware plans don't have an audio DSP story worked out, there's no
  concept of buttons being reassignable in the firmware yet, and the
  display driver isn't built. Adding endpoints for these now would mean
  guessing at capabilities that don't exist.

---

## Where the app-side code lives

- `data/device/DeviceControlApiClient.kt`: makes every HTTP call described
  above (playback, device management, files, OTA).
- `data/device/DevicePlaybackRepository.kt`: the one piece with ongoing
  state. Keeps polling `/api/playback` while the Now Playing screen is
  open, and stops the moment it isn't.
- `data/device/GitHubFirmwareReleaseClient.kt`: the GitHub "is there a newer
  release" check.
- The screens: `ui/screens/settings/DeviceNowPlayingScreen.kt`,
  `DeviceStorageScreen.kt`, `DeviceFirmwareScreen.kt`,
  `DeviceSettingsScreen.kt`.
