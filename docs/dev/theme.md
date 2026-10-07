# Theme and design

CassetteCat's interface is dark, with silver for neutral controls and one accent colour for whatever is active. The theme lives in `ui/theme/` (paths relative to `app/app/src/main/java/in/caffeinelabs/cassettecat/`).

## Colours

Defined in `ui/theme/Color.kt` and applied in `ui/theme/Theme.kt`:

| Token | Value | Material role | Use |
|---|---|---|---|
| `Background` | `#000000` | `background` | Screen background |
| `Surface` | `#1C1A18` | `surface` | Panels and sheets |
| `SurfaceVariant` | `#262320` | | Raised panels |
| `Silver` | `#C4C4C0` | `primary` | Neutral controls |
| `SilverDim` | `#6E6C68` | | Inactive icons |
| `TextPrimary` | `#F5F0EC` | | Main text |
| `TextSecondary` | `#A8A29A` | | Supporting text |
| Accent | see below | `tertiary`, `error` | Active states and transport highlights only |

Use `MaterialTheme.colorScheme` in screens rather than the raw tokens, so AMOLED mode and the user's accent apply.

## Accent

The user picks the accent in **Customisation > Theme**. The options are defined in `ThemeAccent` in `data/settings/AppPreferencesRepository.kt`:

| Accent | Colour |
|---|---|
| Record Red (default) | `#C23B30` |
| Cassette Amber | `#F59E0B` |
| Electric Cyan | `#06B6D4` |
| Neon Emerald | `#10B981` |
| Tape Magenta | `#EC4899` |
| Monochrome | `#C4C4C0` |
| Custom | any hex colour the user enters |

**Artwork Accent** replaces the accent with a colour taken from the current album art while music plays. Because the accent can be almost any colour, never use it as the only way to show state, and never put long text in it.

## Typography

Fonts are bundled in `res/font/` and selected in `ui/theme/Type.kt`. The app font is chosen by the user: Space Grotesk (default), IBM Plex Sans, IBM Plex Mono, Silkscreen, VT323, Monocraft, or the system's sans, serif or monospace font. IBM Plex Mono is used for technical readouts such as the audio format line.

## Components

Reuse these from `ui/components/` before building new controls:

- **`TransportButton`**: the round push button for play, pause, skip, shuffle and repeat, with a press-depth animation and haptic tick.
- **`PressDepthIconButton`**: a bare icon button with the same press feel, for navigation and action rows.
- **`AlbumArt`** and **`ArtistImage`**: album and artist artwork with a fallback when none is available.
- **`EmptyState`**: icon, title, message and an optional action for empty lists.
- **`FastScrollIndexRail`**: the A to Z rail for long lists.

## Motion

Screen transitions are linear 220 ms slides (`TRANSITION_MS` in `MechanicalTransitions.kt`). Keep new animations short and tied to a state change.
