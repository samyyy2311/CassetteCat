# CLAUDE.md

# Software Engineering Guidelines

## 1. Core Objective

Build software that is: Correct, Simple, Secure, Maintainable, Performant, Testable, Consistent with the existing codebase.

The objective is not to produce the most code. The objective is to produce the best software with the least unnecessary complexity.

---

## 2. Critical Rule: Code Economy

Write the minimum amount of code necessary to implement the required behavior correctly, without sacrificing readability, correctness, security, testability, or maintainability. Before adding code, look for ways to reuse, simplify, or consolidate existing code, and remove unnecessary abstractions or obsolete code. Complexity must be justified by a real requirement.

---

## 3. Before Writing Code

1. Understand the requirement. 2. Inspect the relevant existing code. 3. Search the repository for existing implementations. 4. Identify existing patterns/conventions. 5. Check how related functionality currently works. 6. Determine the smallest reasonable change. 7. Consider edge cases and failure modes. 8. Only then modify the code.

---

## 4. Development Workflow

**Understand -> Inspect -> Plan -> Implement -> Verify -> Review -> Simplify**

- **Understand**: what needs to change, what must stay unchanged, what constraints exist.
- **Inspect**: search for existing components, functions, utilities, patterns, tests before creating anything new.
- **Plan**: for non-trivial changes, note files to modify, reusable code, simplest approach, risks.
- **Implement**: make the smallest safe change.
- **Verify**: run tests, type checking, linting, build, manual verification.
- **Review**: check the diff for unnecessary/unrelated changes, dead code, debug statements, security issues.
- **Simplify**: if it can be safely made simpler, simplify it.

---

## 5. DRY: Don't Repeat Yourself

Reuse existing functions/components, centralize shared constants, avoid duplicate implementations of the same behavior. Don't force unrelated code into an abstraction just to remove a few repeated lines; simple duplication can beat a bad abstraction.

---

## 6. KISS: Keep It Simple

Prefer simple, obvious solutions. Before introducing a new library, abstraction layer, design pattern, or state-management system, ask whether the existing architecture already solves the problem. Avoid clever code.

---

## 7. YAGNI: You Aren't Gonna Need It

Do not implement functionality that isn't currently required; no unused options, speculative APIs, hypothetical plugins, or unnecessary config. Implement what's needed now.

---

## 8. Existing Code First

Before creating something new, search the repository for similar functionality, reusable components, existing utilities, types, validation, error handling, styling, or patterns.

---

## 9. Architecture

Respect existing architecture unless there's a clear reason to change it. Prefer high cohesion, low coupling, clear boundaries, explicit dependencies, separation of concerns. Don't introduce a new pattern just because it's popular.

---

## 10. Avoid Overengineering

Don't build enterprise layering (Controller -> Service -> Repository -> Interface -> Adapter -> DTO -> Mapper) unless it solves an actual problem. Abstractions should exist for real value (reuse, isolation, testability, complexity management), not to demonstrate patterns.

---

## 11. SOLID

Apply where it improves the design, not mechanically:
- **SRP**: clear responsibility per module/function/class.
- **OCP**: extensible without constant modification of stable core logic, when it benefits the project.
- **LSP**: subtypes behave consistently with base-type contracts.
- **ISP**: don't force consumers to depend on unused functionality.
- **DIP**: depend on stable abstractions where useful, not unnecessary concretions.

---

## 12. Functions

Prefer small, focused functions. Avoid giant functions, excessive nesting/parameters, hidden side effects. Use early returns when they improve readability. Don't extract every few lines into a function just to make it "small."

---

## 13. Components and Modules

Avoid components that simultaneously fetch, transform, validate, and mutate data plus handle unrelated UI state. Separate responsibilities when it genuinely improves the design; don't over-split simple functionality.

---

## 14. Naming

Use descriptive names (`calculateInvoiceTotal()` not `calc()`; `customerId` not `id` when ambiguous). Avoid unnecessary abbreviations. Names should communicate intent.

---

## 15. Comments

Comments explain *why*, not *what*. Don't restate obvious code. Don't leave commented-out code; delete obsolete code.

---

## 16. Types

Use the type system properly: precise types, reuse existing types, avoid duplicate definitions, avoid `any` unless necessary, avoid unnecessarily complex generics.

---

## 17. Error Handling

Never silently swallow errors. Validate external input. Provide useful error messages and context. Handle expected failures; let unexpected failures surface appropriately. Don't wrap everything in unnecessary try/catch, and don't catch-and-ignore.

---

## 18. Security

Treat all external input as untrusted. Consider authN/authZ, input validation, output escaping, injection (SQL/XSS/CSRF/SSRF/path/command), sensitive data exposure, rate limiting, access control. Never hardcode secrets, commit credentials, expose API keys, trust client-side authorization, or disable security controls for convenience.

---

## 19. Database

Understand the existing schema before modifying it. Follow naming conventions, respect constraints, use transactions where atomicity matters, avoid unnecessary/N+1 queries, add indexes when justified, keep migrations safe, respect existing authorization/RLS policies.

---

## 20. APIs

Validate inputs and authorization. Return consistent responses with appropriate status codes. Avoid unnecessary API calls or breaking existing clients; preserve backwards compatibility when required. Don't create a new endpoint if an existing one can reasonably support the requirement.

---

## 21. Frontend

Reuse existing components, styles, and design tokens. Avoid duplicating UI logic; keep state local when practical, avoid unnecessary global state. Handle loading, error, empty, and success states. Consider responsive behavior and accessibility. Don't introduce a new UI library for one component without strong reason.

---

## 22. Dependencies

Before adding one: check if the project or language/framework already provides the functionality, consider a small local implementation, bundle size, maintenance cost, security, and licensing. Don't add a dependency for trivial functionality.

---

## 23. Performance

Measure before optimizing. Avoid unnecessary API requests, N+1 queries, repeated expensive calculations, unnecessary re-renders, oversized resources, inefficient algorithms. Don't sacrifice readability for negligible gains.

---

## 24. Testing

Test behavior, not implementation details. Prioritize critical business logic, important flows, edge/failure cases, security-sensitive behavior, regression prevention. Don't write meaningless tests just to raise coverage numbers.

---

## 25. Verification

Never assume code works because it looks correct. After changes: inspect the diff, run tests/type-checking/linting, build when appropriate, verify actual behavior. Don't stop after writing code.

---

## 26. Refactoring

Preserve existing behavior unless change is intentional. Reduce complexity, remove duplication/dead code/obsolete abstractions, keep the diff focused. Don't rewrite large parts of the project out of preference.

---

## 27. Scope Control

Stay within the requested scope. If asked to fix one thing, don't also rewrite unrelated architecture or components unless genuinely required. If you find an unrelated problem, mention it rather than silently expanding the task.

---

## 28. Preserve Existing Behavior

Before modifying shared code, determine where it's used, what depends on it, what callers assume, and whether tests cover it. Prefer backward-compatible changes.

---

## 29. Git Discipline

Keep changes focused: small logical commits, meaningful messages, clean diffs. Never commit secrets or unnecessary generated files, modify unrelated files, rewrite history without permission, or delete others' work. Inspect `git diff` / `git status` before finishing.

---

## 30. No Fake Completion

Never claim something is tested, working, fixed, deployed, or verified unless it actually was. State explicitly what was not verified. Never pretend a command succeeded if it wasn't run.

---

## 31. Ambiguous Requirements

Don't invent important requirements. If ambiguity materially affects implementation, ask for clarification. If minor, choose the simplest reasonable interpretation and proceed; don't block progress over trivial decisions.

---

## 32. Bad Existing Code

Don't automatically rewrite bad existing code. If it doesn't affect the current task, leave it (optionally mention it). If it directly affects the task, make the smallest necessary improvement; don't turn every task into a cleanup project.

---

## 33. Anti-Patterns

Avoid: overengineering, premature optimization/abstraction, excessive design patterns/comments/documentation/validation/error-handling/file-splitting/dependencies, giant functions/components, deep inheritance, duplicate implementations, dead/commented-out code, magic numbers, hardcoded secrets, speculative features, unnecessary config/state/API calls.

---

## 34. Code Reduction

After implementing, review for unnecessary complexity: removable code/abstractions, consolidatable helpers/files, avoidable dependencies, removable state/config. Remove what can go without reducing functionality, correctness, security, testability, or maintainability. Don't reduce code merely for line count.

---

## 35. Code Review Mindset

Review as if another engineer maintains it for years. Ask about correctness, simplicity, maintainability, security (can this be abused?), reliability (what if dependencies fail?), performance, testing, and scope (did this change anything unrelated?).

---

## 36. Decision Hierarchy

When choosing between implementations, prioritize in order: Correctness, Security, Simplicity, Readability, Maintainability, Reliability, Performance, Testability, Extensibility. Don't choose an approach merely because it's more sophisticated.

---

## 37. Practical 10-Second Check

Before finalizing: Am I duplicating existing code? Can this be simpler? Unnecessary abstraction/files/dependencies/complexity? Changed unrelated behavior? Handled important errors? Security issue? Actually tested? Inspected the diff?

---

## 38. Recommended Claude Code Workflow

For non-trivial tasks: Understand -> Inspect -> Plan -> Implement -> Verify -> Review -> Simplify -> **Report** (state what changed, what was tested, what was not tested, remaining concerns).

---

## 39. Useful Prompting Rules

- General: "Inspect the existing implementation first. Reuse existing patterns and make the smallest safe change. Do not refactor unrelated code."
- Bug fixes: "Find the root cause first. Fix it with the smallest change possible. Do not rewrite unrelated code."
- New features: "Inspect how similar functionality is already implemented. Follow the existing architecture instead of introducing a new pattern unless necessary."
- Cleanup: "Audit this area for unnecessary complexity, duplication, dead code, and redundant abstractions. Do not change behavior."
- Final verification: "Run the relevant tests, typecheck, lint, and build if applicable. Inspect the final diff and fix anything introduced by the changes."
- Code reduction: "Review the implementation for unnecessary code and abstraction. Simplify anything that can be safely removed while preserving functionality, correctness, security, and maintainability."

---

## 40. Final Principle

The best implementation is not the one with the most architecture, abstractions, or code.

Prefer: Simple > Clever · Necessary > Possible · Reuse > Duplicate · Explicit > Magical · Focused change > Rewrite · Measured optimization > Guessing · Existing pattern > New pattern · Minimum complexity > Maximum architecture · Working simplicity > Theoretical flexibility.

**The goal is maximum useful functionality with minimum unnecessary complexity.**

---

# 41. CassetteCat Project Guardrails

Everything above is general engineering discipline. This section is project-specific and non-negotiable; it overrides general judgment calls where the two conflict.

## Project Overview

CassetteCat is a music player under CaffeineLabs, with two connected components:

- **app/**: Android app, built from scratch (not a Rhythm fork). Talks to the hardware player.
- **firmware/**: ESP32-based firmware for a DIY hardware audio player, inspired by Tangara.
- **hardware/**: PCB schematics, board layouts, and mechanical design files.
- **docs/**: project documentation.

Not a tape-based device; cassette branding is aesthetic only.

## Hard Rule: No Tangara Reuse

Do not port, copy, or adapt code, schematics, or design files from any Tangara repository (`tangara-fw`, `tangara-hw`, `tangara-themes`, `tangara-assembly`, `tangara-samd-fw`), even when opened for reference. "Inspired by" means conceptual/aesthetic inspiration only; architecture, pin layouts, and UI concepts can be studied, but not structure, variable names, or logic copied. Everything in `app/`, `firmware/`, and `hardware/` is written from scratch.

## Licensing: Enforce on Every File Added

- `app/` and `firmware/`: GPL-3.0 (root `LICENSE`)
- `hardware/`: CERN-OHL-S-2.0 (`hardware/LICENSE`)

Never introduce a dependency whose license is incompatible with GPL-3.0 in `app/`/`firmware/`, or incompatible with CERN-OHL-S-2.0 in `hardware/`, without flagging it explicitly first. No proprietary analytics or closed-source Gradle dependencies; F-Droid compatibility must be preserved.

## Hardware: Prototype Phase Only

Current goal: prove the core loop (display, SD card, decode, I2S, sound, button input) plus Wi-Fi connectivity to the app (see Connectivity below). Do not build or suggest amp circuitry, haptics, Bluetooth, a browser-based web control UI, or enclosure/case work unless explicitly asked. These remain deferred to a later v2 hardware revision, this is YAGNI (§7) applied specifically to the hardware roadmap.

### Connectivity (in scope: app-to-device sync)

The app is both a standalone local music player and a companion for the physical device: song library sync to the device's SD card, firmware OTA updates, and other device-management features to be defined later.

- **Transport: Wi-Fi only, no Bluetooth.** Decided because song files and firmware images are bulk transfers where throughput matters; BLE realistically tops out in the tens-of-KB/s range and would make syncing a real music library take hours. If a future feature needs low-power always-on status/control, that's a v2 addition to revisit, not a reason to add BLE now.
- **Wi-Fi mode: support both.** SoftAP (device runs its own hotspot, phone connects directly, works anywhere without a home network) and station mode (device joins an existing Wi-Fi network). This means firmware needs a provisioning flow to configure/switch modes, and the app needs both connection paths handled (SoftAP: Android `WifiNetworkSpecifier` to associate without dropping cellular internet, API 29+; station: local discovery via NSD/mDNS).
- **Protocol: HTTP(S) server on-device.** Use ESP-IDF's built-in `esp_https_ota` for firmware OTA rather than a custom implementation, it's mature and well-documented. Song sync and any future status/control features should reuse this same HTTP/WebSocket surface rather than adding a second transport.
- This is distinct from the deferred "browser-based web control UI" above, that item means a general-purpose UI reachable from any browser. What's in scope here is specifically the CassetteCat app talking to the device's local API, not a public web UI.

### BOM (prototype)

| Component | Purpose | Notes |
|---|---|---|
| ESP32-S3-WROOM-N16R8 DevKitC (DOIT, dual USB-C) | Main controller | 16MB flash, 8MB PSRAM, native USB |
| Adafruit PCM5102 I2S DAC | Audio output | No dedicated amp stage yet |
| 1.8" TFT LCD, 128x160, ST7735R, 4-wire SPI | Display + storage | Robu.in SKU 62060, ₹279. Functionally equivalent to Tangara's ER-TFT018-4 reference (same resolution, same interface, compatible controller family: ST7735R vs ST7735S). Onboard 3.3V regulator and 3/5V level shifter, compatible directly with ESP32-S3 logic, no additional regulation needed. Its onboard microSD slot replaces the standalone DFRobot Fermion module, so that module is dropped from the BOM |
| NOVA 103450 2000mAh LiPo | Power | Built-in protection PCB |
| TP4056 USB-C charging module | Battery charging | |
| MT3608 boost converter | Voltage regulation | Raw LiPo voltage insufficient for ESP32 VIN regulator |
| 8-Key Touch Button Module | User input | |

Sourcing: Indian domestic suppliers only (Robu.in, Amazon India), no importing.

### Open Decisions (confirm with user before assuming)

- **Display + SD SPI pins**: display gets a dedicated SPI bus (not shared with any other peripheral), and its onboard microSD slot is used in place of the old DFRobot module. Specific GPIOs not yet chosen, still needs a decision before wiring or writing driver code.

### Pin Mapping (ESP32-S3, use exactly, confirm before reassigning)

- I2S: GPIO 4/5/6
- I2C: GPIO 18/8
- Buttons: GPIO 9/10/11/12
- Power: soft power via deep sleep on GPIO12 (no physical switch in prototype)
- Display + SD SPI (dedicated bus): not yet assigned, see Open Decisions above
- Wi-Fi: no GPIOs needed, radio is on-die on the ESP32-S3 module

## Tech Stack

`app/`, decided:

- Language: Kotlin
- UI: Jetpack Compose (not XML views)
- Architecture: MVVM with a Repository layer, keep it simple, no unnecessary additional layering (see §10)
- Playback: Media3 (ExoPlayer)
- F-Droid constraint: avoid proprietary/Google Play Services-dependent libraries where FOSS alternatives exist (e.g. no Firebase-only solutions)
- Device connectivity: the app is both a standalone local player and a companion for the physical device (song sync, firmware OTA, more later). Wi-Fi only, talks to an HTTP(S)/WebSocket server the device runs, no Bluetooth. Must handle both SoftAP (device's own hotspot) and station mode (device on the user's Wi-Fi), see the Hardware Connectivity section above for the reasoning. SoftAP connection on Android should use `WifiNetworkSpecifier` (API 29+) so the app doesn't force the user to lose cellular internet; below API 29 there's no equivalent, the user has to switch Wi-Fi manually, this is a real minSdk 26 gap to design around, not an oversight to silently drop.
- Build: AGP 9.x has Kotlin support built in, do not add the separate `org.jetbrains.kotlin.android` plugin, it will fail the build with "no longer required since AGP 9.0". Only `org.jetbrains.kotlin.plugin.compose` is needed alongside `com.android.application`.
- compileSdk/targetSdk 37, minSdk 26. Some current androidx releases (core-ktx 1.19.0, lifecycle 2.11.0) require compileSdk 37+, confirmed by an actual `./gradlew assembleDebug` run, not assumed.
- Package id is `in.caffeinelabs.cassettecat`. Since `in` is a Kotlin reserved keyword, every file's `package` line and any cross-package `import` must backtick-escape it: `` package `in`.caffeinelabs.cassettecat ``. This was a deliberate, confirmed choice, not an oversight, do not "fix" it by renaming the package.

`firmware/`, not yet decided:

Do not scaffold Arduino vs. ESP-IDF vs. PlatformIO unless already evident from existing files. Ask first, this is §31 (Ambiguous Requirements) applied: framework choice materially affects implementation, so it isn't a minor decision to default on.

## Theming: Two Themes, Both Dark-Mode Only

CassetteCat supports exactly two themes, both dark-mode only, there is no light mode. Only "Owned Device" needs real work right now, "Minimal" is deferred, but the theme system must stay swappable: route color/type through `MaterialTheme.colorScheme` / `Typography`, never hardcode Owned Device's tokens as literals at call sites, that's what lets Minimal slot in later as a token swap instead of a rewrite.

### Owned Device (default, current direction, supersedes earlier "nostalgic-modern", Neo-Brutalist/8-bit, and vaporwave explorations)

Core idea: CassetteCat should feel like a physical device you own and control, Walkman/iPod-era, not a subscription app. Every screen is a device face, not a website.

- Skeuomorphic honesty: industrial-design skeuomorphism (looks manufactured), not glossy 2010-era iOS skeuomorphism. Real button/control depth, visible press states, physical-feeling shadows. **Implemented for transport controls**: `ui/components/TransportButton.kt` (circular, press-depth offset, animated elevation, gradient cap, `tertiary` accent border when active), shared by Now Playing's play/skip row and the mini-player. **Implemented for all icon buttons**: `ui/components/PressDepthIconButton.kt` extends the same press-depth offset physics to all 16 bare icon button call sites across the app.
- Mostly neutral surfaces (silver/black/off-white dominate every screen), excitement from form/materiality, not saturation. Don't wrap content in a `Card`/container purely for grouping if the fill color is only barely distinguishable from the background, that renders as an invisible wrapper, not a real container, content sits directly on the background instead.
- Now-playing screen = the "front panel," album art inset like a physical screen. Playback controls styled as physical buttons (`TransportButton`, see above). A circular scrub/volume control (click-wheel-like: rotate to scrub, tap to select) is **still not built**: the scrubber is a linear fader-style slider (`ScrubberControl` in `NowPlayingScreen.kt`), not a rotary control.
- Motion is mechanical, not bouncy: panels slide like physical parts, no spring/bounce overshoot easing. **Implemented for navigation**: `MechanicalTransitions.kt` defines fixed-duration `tween(220ms)` transitions with no spring/overshoot easing across both outer and inner `NavHost`s.
- Icons: Lucide (`com.composables:icons-lucide-android`), bare, no colored badge/chip container behind them, thin-line geometric style reads technical/neutral rather than playful.

**Typography:** IBM Plex Sans (UI text) + IBM Plex Mono (technical readouts, track times/EQ values, anything that should read as a device display rather than app copy). Chosen after a live on-device comparison against Space Grotesk + Space Mono and Inter + JetBrains Mono, IBM Plex was designed by IBM specifically to embody an engineering company's identity, closest direct match to industrial-design skeuomorphism. Both are bundled as TTFs under `res/font/` (`ibm_plex_sans_variable.ttf`, `ibm_plex_mono_regular.ttf`, `ibm_plex_mono_semibold.ttf`), OFL-licensed, attribution under `assets/fonts/`. Space Grotesk/Mono files are still bundled too (lost the comparison but harmless to leave for now); if reviving them, `Type.kt` has a comment showing what to swap.

**Color** (`ui/theme/Color.kt`):

| Role | Hex | Usage |
|---|---|---|
| Accent (Record Red) | `#B3483A` | Sparingly only: button active states, playing indicators, outlines, small icons. Never a background or large fill. |
| Background | `#000000` | Primary app background |
| Surface | `#1C1A18` | Cards, elevated panels, now-playing background |
| Neutral/Silver | `#C4C4C0` | Metal-feeling surfaces, control bodies, dividers |
| Text primary | `#F5F0EC` | Primary text on dark backgrounds |
| Text secondary | `#A8A29A` | Muted text, timestamps, secondary labels |

In `Theme.kt`, `primary`/`secondary` map to the silver neutrals, not Record Red, because Material3's default filled `Button`/`FAB` use `colorScheme.primary` as a background fill, and red must never be a background/large fill. Record Red lives on `tertiary`/`error` instead, the roles Material3 treats as small accents rather than default chrome. Reference `colorScheme.tertiary` at call sites for "the accent," never a hardcoded red literal.

### Minimal (deferred, do not build yet)

Pure black/white/grayscale, no accent color, no skeuomorphic depth/texture, for users who prefer flat modern minimalism. Uses the same pixel-art mascot as Owned Device, the mascot's 8-bit/pixel-art style is a property of the mascot asset itself, shared across both themes, not a separate theme. Don't start on this until explicitly asked.

## CAD Workflow

`hardware/` case design happens in Creo. Export STEP + STL alongside native Creo files on every commit so the design stays usable by non-Creo tools/users.
