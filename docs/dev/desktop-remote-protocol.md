# Desktop Remote protocol

This is the HTTP API that CassetteCat Desktop serves and the Android app calls. The desktop implements it in `src/remote_control.cpp`; the phone side is `DeviceControlApiClient` and `DesktopRemoteRepository`.

## Discovery

The phone broadcasts the UDP datagram `CASSETTECAT_DISCOVER` to port 47800. Each desktop with Phone Remote turned on replies:

```json
{"name": "DESKTOP-1234", "port": 47800}
```

`port` is the HTTP port to use. The desktop only answers private, link-local or loopback addresses.

## Requests

* Requests and responses are JSON over plain HTTP.
* The desktop accepts requests only from private, link-local or loopback addresses; anything else gets `403`.
* Every request except pairing needs `Authorization: Bearer <code>`, where `<code>` is the six-character pairing code. `GET /api/artwork` can pass it as `?code=` instead, because image loaders cannot set headers.
* `X-Device-Name` names the phone. The desktop shows it as the device in control.
* Ten wrong codes in a row lock the server for one minute; during that time every request gets `429`.
* Unless noted, a successful request returns `{"ok":true}` and a failed one `{"ok":false}`.

## Pairing

| Request | Body | Response |
|---|---|---|
| `POST /api/pair-request` | `{"name": "Pixel 8"}` | `{"id": "..."}`. The desktop shows an Allow / Deny prompt. `409` if another request is already waiting. |
| `GET /api/pair-request?id=...` | | `{"status":"waiting"}`, `{"status":"denied"}`, or `{"status":"allowed","code":"ABC234"}`. `404` once the request has expired. |

An unanswered request expires after 60 seconds.

## Playback

| Request | Body | Notes |
|---|---|---|
| `GET /api/playback` | | Current state, below. |
| `POST /api/playback` | `{"action": "..."}` | `play`, `pause`, `next`, `previous`, `toggle_shuffle` or `cycle_repeat`. |
| `POST /api/seek` | `{"positionMs": 61000}` | |
| `POST /api/volume` | `{"percent": 70}` | Clamped to 0 to 100. |
| `GET /api/artwork?key=...` | | The cover for the current or a queued track, as JPEG or PNG. `key` comes from `artworkKey`. |

`GET /api/playback` returns:

```json
{
  "isPlaying": true,
  "trackTitle": "Training Season",
  "trackArtist": "Dua Lipa",
  "positionMs": 15000,
  "durationMs": 209000,
  "volumePercent": 100,
  "shuffleEnabled": false,
  "repeatMode": 0,
  "artworkKey": "...",
  "deviceName": "DESKTOP-1234",
  "handoffRequested": false
}
```

`repeatMode` is 0 for off, 1 for repeat all and 2 for repeat one. `handoffRequested` is `true` once after the desktop asks the phone to take over playback.

## Queue

| Request | Body | Notes |
|---|---|---|
| `GET /api/queue` | | `{"tracks": [{"index", "title", "artist", "durationMs", "artworkKey"}]}`: the next tracks in the desktop's queue. |
| `POST /api/queue` | `{"index": 4}` | Play the queued track at `index`. |
| `POST /api/queue/move` | `{"from": 5, "to": 3}` | |
| `POST /api/queue/remove` | `{"index": 6}` | |
| `POST /api/queue/next` | `{"title": "...", "artist": "..."}` | Play this track next, if the desktop library has it. |
| `POST /api/handoff` | `{"tracks": [{"title", "artist"}], "index": 0, "positionMs": 0, "playing": true}` | Continue the phone's queue on the desktop. |

## Phone playback

While the phone plays music itself, it reports what is playing so the desktop can show it and send commands back:

`POST /api/phone-state` with `{"title": "...", "artist": "...", "isPlaying": true}`. An empty `title` means the phone stopped. The response carries commands for the phone:

```json
{"ok": true, "commands": ["pause"], "playNext": [{"title": "...", "artist": "..."}], "likesRevision": 3}
```

`commands` can contain `play`, `pause`, `next`, `previous` and `handoff`.

## Sync

The two libraries are matched by title and artist, ignoring case. A match key is the lowercase title, the character U+001F, then the lowercase artist.

| Request | Body | Response |
|---|---|---|
| `GET /api/likes` | | `{"library": [keys], "liked": [keys], "revision": 3}` |
| `POST /api/likes` | `{"like": [keys], "unlike": [keys]}` | |
| `GET /api/playlists` | | `{"playlists": [{"name", "tracks": [{"title", "artist"}]}]}` |
| `POST /api/playlists` | `{"name": "...", "tracks": [{"title", "artist"}]}` | `{"ok": true, "matched": 12, "total": 14}`. Replaces a desktop playlist with the same name. |
| `GET /api/listens?since=...` | | `{"listens": [...]}`: listens recorded on the desktop after `since`. |
| `POST /api/listens` | `{"listens": [{"at", "title", "artist", "album", "genre", "ms"}]}` | `at` is Unix time in milliseconds; `ms` is how long the track played. |
| `GET /api/backup` | | The last backup the phone uploaded. `404` if there is none. |
| `POST /api/backup` | The app's backup JSON | The desktop keeps the previous backup beside the new one. |

`likesRevision` in the phone-state response changes whenever the desktop's likes or library change, so the phone knows when to sync again.

## Limits

* Request bodies: 64 KB, or 16 MB for `/api/backup`, `/api/likes`, `/api/playlists` and `/api/listens`. Larger bodies get `413`.
* Only a paired phone may send a body over 64 KB.
* A connection that has not finished its request after 5 seconds is closed.
