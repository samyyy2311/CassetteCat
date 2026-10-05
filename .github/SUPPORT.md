# Getting Support for CassetteCat (Android)

Thank you for using CassetteCat. Here is how to get help, report problems, and propose improvements.

---

## 1. Questions & Setup Help

Before opening an issue, check whether your question is already covered:

* **[README.md](../README.md)**: Details installation options (Google Play, GitHub Releases, Obtainium), streaming setup (Subsonic/Navidrome, Jellyfin), and companion features.
* **[Search Existing Issues](https://github.com/samyyy2311/CassetteCat/issues?q=is%3Aissue)**: Check open and closed issues to see if a question or problem has already been addressed.
* **General Questions**: If you cannot find an answer, open an issue using the general question format.

---

## 2. Reporting Bugs

If you encounter an issue or crash:

1. **Verify your version**: Check *Settings > About & Legal* to confirm you are on the latest release.
2. **Search open and closed issues**: Avoid duplicate reports for already tracked problems.
3. **Use the appropriate issue template**:
   * **App Bugs**: Use the [Bug Report](https://github.com/samyyy2311/CassetteCat/issues/new?template=bug_report.md) template. Please include:
     * Device model (e.g., Google Pixel 8, Samsung Galaxy S23).
     * Android version and API level (e.g., Android 14 / API 34).
     * Audio source involved (Local MediaStore, Subsonic/Navidrome, Jellyfin, or Internet Radio).
     * Steps to reproduce the bug.
     * Logcat output or crash stacktraces (via db logcat -d) where applicable.
   * **Companion Player Issues**: For ESP32 physical player firmware or hardware issues, use the [Hardware / Firmware Issue](https://github.com/samyyy2311/CassetteCat/issues/new?template=hardware_firmware_issue.md) template.

---

## 3. Feature Requests

To suggest improvements or new features:

* Check existing [Feature Requests](https://github.com/samyyy2311/CassetteCat/issues?q=is%3Aissue+label%3Aenhancement) to upvote or join the discussion.
* Open a new proposal using the [Feature Request](https://github.com/samyyy2311/CassetteCat/issues/new?template=feature_request.md) template, describing the practical use case and how it fits the player's local-first philosophy.

---

## 4. Contributing & Translations

If you want to contribute to development:

* Read [CONTRIBUTING.md](../CONTRIBUTING.md) for project guidelines, build requirements (Android Studio, JDK 17, compileSdk 37), and code economy principles.
* Review [CODE_PATTERNS.md](../CODE_PATTERNS.md) for as-built Kotlin/Compose architecture patterns (MVVM + Repository, Media3, DataStore).
* Read [DESIGN.md](../DESIGN.md) and [DESIGN_SYSTEM.md](../DESIGN_SYSTEM.md) for UX and design tokens.
* See [TRANSLATING.md](../TRANSLATING.md) to add or update language translations in XML resources.
* Review [AI_DISCLOSURE.md](../AI_DISCLOSURE.md) for expectations regarding AI-assisted contributions.

---

## 5. Security Vulnerabilities

Please **do not** report security vulnerabilities through public GitHub issues. Follow our [Security Policy](../SECURITY.md) to submit reports privately.