# Playback and sound

## Now Playing

Tap the mini-player to open Now Playing. Besides play, pause, skip, shuffle and repeat, it has:

- **Heart**: likes the song.
- **Lyrics**: synced lyrics that follow the song. See [Lyrics](#lyrics).
- **Queue**: what plays next, plus your history. Drag a song's handle to reorder it, or remove it from the queue.
- **Quality badge**: shows Hi-Res Lossless, Lossless or the codec. Tap it for the format, sample rate, bit depth and bitrate.
- **More**: everything else, grouped into three sections:
  - **Track & Metadata**: share the song or its file, edit details, download, credits, search for a cover.
  - **Playback & Output**: audio output, equalizer, playback speed, sleep timer.
  - **Features & Modes**: Listening Room and Drive Mode.

Set how Now Playing looks in **Settings > Customisation > Now Playing & Gestures**:

- **Backdrop Style**: **Atmosphere Blur** (blurred artwork with a dark layer over it), **Liquid Gradient** (moving colours taken from the artwork), **Ambient Glow**, or **Obsidian** (plain black).
- **Album Art Corner Style**: Curved, Soft or Square.
- **Show Remaining Time**: shows `-02:45` instead of the song's length.

## Gestures

In **Settings > Customisation > Now Playing & Gestures**. The two swipe gestures are on by default; the others are off.

| Gesture | What it does |
|---|---|
| **Shake to Skip** | Shake the phone to skip. **Shake Sensitivity** goes from Gentle to Strong; pick Firm or Strong if songs skip while you walk. |
| **Flip to Pause** | Put the phone face-down to pause; pick it up to resume. |
| **Wave to Skip** | Wave a finger over the proximity sensor at the top of the screen. |
| **Mini-Player Swipe to Skip** | Swipe across the mini-player to change songs. |
| **Swipe Up on Art for Lyrics** | Swipe up on the artwork to open lyrics. |

## Audio settings

**Settings > Customisation > Audio Engine**:

- **Crossfade Duration**: fades one song into the next over 2, 4, 6, 8 or 12 seconds. Off by default.
- **Gapless Playback**: removes the silence between songs that run into each other, such as live albums. On by default.
- **Volume Normalisation (ReplayGain)**: evens out loudness between songs that have ReplayGain tags. **ReplayGain Pre-Amp Gain** adjusts songs without those tags.
- **Mono Audio Downmix**: combines left and right into both ears, for listening with one earbud.
- **Volume Limit (Ear Protection)**: caps how loud CassetteCat can play, from 50% to 100%, whatever the phone's volume is set to.
- **Resume Queue on Launch**: reopens the app on the song and queue you left.
- **Autoplay**: keeps playing similar songs when the queue runs out.
- **Pause on Disconnect**: pauses when headphones or Bluetooth audio disconnect.
- **Auto Drive Mode**: opens Drive Mode when your car's Bluetooth audio connects. See [Car, widgets and shortcuts](car-widgets-shortcuts.md).
- **Tactile Haptic Feedback**: small vibrations when you scrub, scroll and tap.

## Equalizer

Open **Settings > Equalizer**, or **More > Equalizer** in Now Playing. The equalizer only attaches while something is playing, so start a song first.

- **Headphone Calibration (AutoEq)**: search for your headphones by brand or model, for example "Sony" or "HD 600", and pick them. CassetteCat applies a measured correction curve from the [AutoEq](https://github.com/jaakkopasanen/AutoEq) project.
- **Presets**: built-in presets, plus any you save with **Save Preset**. A preset stores the band levels, bass boost and preamp gain.
- **Frequency Bands**: drag each band up or down.
- **Audio Enhancements**: **Bass Boost** and **Surround Virtualizer**.
- **Loudness & Dynamics**: **Preamp Gain** and **Loudness Normalization**.

**Reset EQ** puts everything back to flat.

## Playback speed and pitch

**More > Playback Speed** in Now Playing opens **Tempo & Pitch Tuning**. Speed goes from 0.5x to 2x; pitch can be shifted separately, from 0.75x to 1.25x. Both reset to normal with the 1.0x option.

## Sleep timer

From Now Playing, **More > Sleep Timer** stops playback after 5, 10, 15, 30, 45, 60, 90 or 120 minutes, or at the **End of current song**.

**Settings > Sleep timer** has the same timer plus how it ends:

- **Gentle volume fade out**: lowers the volume before stopping. **Fade-out Duration** is 10 to 60 seconds.
- **Wait for song to finish**: lets the current song end instead of cutting it off.

While a timer runs, the same screens show **Sleep timer is on** and a **Turn off** button.

## Lyrics

CassetteCat uses lyrics in this order:

1. lyrics stored in the song file, or an `.lrc` file with the same name next to the song;
2. if there are none, [LRCLIB](https://lrclib.net), a free lyrics database. This needs **LrcLib** turned on in **Settings > External Services**.

Turn on **Settings > Customisation > Lyrics > Prioritize Local .lrc Sidecars** to always prefer your own `.lrc` files over online lyrics.

In the lyrics view:

- **Search & match different lyrics** looks up other versions on LRCLIB.
- **Add Custom** lets you paste lyrics. Synced lyrics use the `[mm:ss.xx]` format, for example `[00:12.30]First line`. Tick **Contribute to LRCLIB** to share them with everyone.
- **Tune lyrics sync** shifts the timing in 100 ms steps when lyrics run early or late.
- Select up to five lines to share them as an image.

**Settings > Customisation > Lyrics** sets the font size and family, centred or left alignment, how the current line is highlighted, and **Keep Screen Awake for Lyrics**.

## Audio output

**More > Audio output** shows where sound is going and links to Bluetooth settings. To play on your computer, see [Using CassetteCat with your computer](desktop.md).
