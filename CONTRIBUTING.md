# Contributing to CassetteCat

Thank you for contributing to CassetteCat. We welcome improvements, bug fixes, and feedback from the community.

## Engineering Principles

Before opening a pull request, please review our core engineering principles:

1. **Code Economy and Simplicity**
   - Write the minimum amount of code necessary to solve the problem well.
   - Avoid unnecessary abstractions, wrapper layers, or speculative features.
   - Prefer standard library and platform APIs over introducing new dependencies.

2. **FOSS and F-Droid Compatibility**
   - Do not add proprietary libraries or services dependent on Google Play Services.
   - All source code in `app/` and `firmware/` is licensed under GPL-3.0.
   - Hardware designs in `hardware/` are licensed under CERN-OHL-S-2.0.

3. **Design Consistency**
   - Respect the Owned Device theme: neutral surfaces, Record Red `#B3483A` indicator accents used sparingly (never as a background or large fill), IBM Plex Sans/Mono typography, and tactile press-depth button feedback.
   - Navigation transitions use linear 220ms sliding animations without spring or bounce easing.

4. **Independent Implementation**
   - All code is written from scratch. Do not copy or adapt code from other projects.

---

## Development Setup

- **IDE**: Android Studio Ladybug (2024.2.1) or newer
- **JDK**: Version 17 (Eclipse Temurin recommended)
- **SDK**: `compileSdk 37`, `minSdk 26`

### Building and Testing

```bash
cd app
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

---

## Pull Request Guidelines

1. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. Keep commits atomic, clean, and focused on the problem at hand.
3. Verify that the app builds without errors or warnings.
4. Submit your pull request with a concise explanation of what was changed and how it was verified.

---

## Reporting Issues

Use the repository issue templates for bug reports and feature requests. Provide relevant device logs and reproduction steps where applicable.
