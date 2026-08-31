# Device Song Sync Protocol

## What this is

CassetteCat isn't just a phone app. It's paired with a small standalone
hardware music player: an ESP32-based device with its own SD card, its own
speaker output, its own buttons. This document describes how songs get from
your phone's library onto that device's SD card.

The two sides talk over Wi-Fi using plain HTTP requests, the same local web
server the device uses for pairing (checking its status, joining your
Wi-Fi) and firmware updates. There's no separate sync server, just a few
more endpoints on the same one.

This is a contract, not a finished feature. The Android app side is fully
built and ready. The firmware side, the code that actually runs on the
ESP32 and needs to respond to these requests, hasn't been written yet,
because the firmware itself (what toolchain, what structure) hasn't been
decided. This document exists so whoever writes the firmware later knows
exactly what the app expects, without reverse-engineering it from Kotlin
code.

## How the whole thing works, step by step

1. The app connects to the device (already built, separate from this doc,
   see the pairing flow).
2. The app asks the device what songs it already has.
   `GET /api/library`
3. The app compares that list against the phone's own music library and
   works out which songs are missing, or a different size than what's
   already on the SD card, meaning it should be replaced.
4. For each missing song, the app sends the actual audio file to the
   device, one at a time.
   `POST /api/sync/songs`
5. The device writes each file to its SD card and confirms it worked.
6. Once every song is sent, the app asks the device what it has again
   (step 2), so its "X of Y songs on device" count stays accurate.

That's the entire flow. No queuing system, no background job on the
device. Just "ask what you have," then "here's a file, save it," repeated
per song.

## The actual endpoints

### `GET /api/library`: what songs do you have?

The device answers with a list of every song file currently on its SD
card:

```json
[
  { "path": "Daft Punk/Discovery/01 One More Time.mp3", "sizeBytes": 5242880 },
  { "path": "Daft Punk/Discovery/02 Aerodynamic.mp3", "sizeBytes": 4981234 }
]
```

Each entry is just a file path (relative to wherever the device stores
music) and its size in bytes. That's genuinely all the app needs to figure
out what's missing. It compares each song's path and size against what's on
the phone, and anything that doesn't match, or isn't in the list at all,
gets queued to send.

Why not a checksum, a hash of the file's actual contents, instead of just
size? Because computing a checksum means the ESP32 has to read and hash
every single file just to answer this one request. Real CPU work on a tiny
embedded chip, every time the app asks. Comparing file size is nearly free
by comparison, and "same path, same size" is a good enough guess for now.
If that ever turns out not to be accurate enough, say two different songs
happen to be exactly the same size, a checksum field can be added later
without breaking this contract. It would just be one more optional field
in each entry.

### `POST /api/sync/songs`: here's a song, save it

One request per song. The request has two parts, this is a standard
"multipart" HTTP upload, the same mechanism a web form uses to upload a
file:

- `metadata` part (JSON text): describes the song.
  ```json
  { "path": "Daft Punk/Discovery/01 One More Time.mp3", "title": "One More Time", "artist": "Daft Punk", "sizeBytes": 5242880 }
  ```
- `file` part: the actual raw audio file bytes.

The device should:
1. Create whatever folders are needed for `path` (e.g. `Daft Punk/Discovery/`
   if they don't already exist).
2. Write the file there, replacing anything already at that exact path.
3. Reply with:
   ```json
   { "ok": true, "error": null }
   ```
   or, if something went wrong (SD card full, write failed, etc.):
   ```json
   { "ok": false, "error": "SD card full" }
   ```

The app sends these one song at a time, waiting for each reply before
sending the next. That's deliberate. An ESP32 running a small embedded web
server almost certainly can't handle several large file uploads arriving at
once, so the app doesn't try.

### `DELETE /api/sync/songs?path=...`: remove this song

Deletes one file from the SD card by its path. This exists in the contract
now so the endpoint list doesn't need to change later, but the app doesn't
call this yet. There's no "remove a synced song" button built into the UI
for this pass. It's here so firmware can implement the full set once,
rather than getting a surprise new endpoint added after the fact.

### Storage space used and total

No separate endpoint for this. It reuses `/api/status` (the same one used
for pairing), which already includes `storageUsedBytes` and
`storageTotalBytes`. No need for sync to duplicate that.

## What's deliberately left out of this version

- Resuming an interrupted upload. If a transfer fails partway (Wi-Fi drops,
  phone goes out of range), the app just retries the whole song from the
  start next time, rather than picking up where it left off. Fine for now,
  song files aren't huge, and prototype-stage libraries aren't massive
  either. Worth revisiting if this turns out to be annoying in practice.
- Uploading multiple songs at the same time. Explained above: one at a
  time, on purpose.

## Where the app-side code lives

- `data/device/SyncApiClient.kt`: makes the actual HTTP calls described
  above.
- `data/device/DeviceSyncRepository.kt`: keeps track of which songs are
  pending, uploading, done, or failed, and drives the one-at-a-time upload
  loop.
- `ui/screens/settings/DeviceSyncScreen.kt`: the screen you see in the app
  (Settings, then CassetteCat Player, then Sync Songs).
