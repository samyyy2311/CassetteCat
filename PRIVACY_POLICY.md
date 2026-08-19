# Privacy Policy for CassetteCat

**Last Updated:** August 18, 2026

CassetteCat ("we", "our", or "the app") is a free and open-source audio player developed with a strict **privacy-first, local-first** architecture. We believe that your listening habits and personal data belong solely to you.

---

## 1. Information We Collect

### A. Personal Information
**We do not collect, store, transmit, or sell any personally identifiable information (PII).** 

There are no account registrations with CassetteCat, no user tracking, no profiling, and no advertising networks.

### B. Device & Audio Files Access
CassetteCat requests permission to access your device's audio files (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`) strictly to scan, organize, and play your local music files on your device. 
* Audio file indexing and metadata parsing happen 100% locally on your device.
* Your files are never uploaded to any external server.

### C. Self-Hosted Server Credentials
If you connect CassetteCat to a self-hosted media server (such as Subsonic, Navidrome, Gonic, Airsonic, or Jellyfin):
* Your server URLs, usernames, and authentication tokens/passwords are stored **locally on your device**.
* Sensitive credentials are encrypted using hardware-backed encryption (`AndroidKeyStore` with AES-256-GCM).
* Credentials are sent exclusively to your specified server endpoint to facilitate direct media streaming.

---

## 2. Third-Party Services & Network Requests

CassetteCat only makes network requests to external services when directly necessary for features you use:

1. **Lyrics (LRCLIB):**
   * When fetching synchronized or plain lyrics, track titles and artist names are queried against the public [LRCLIB](https://lrclib.net) API.
2. **Metadata & Artwork (MusicBrainz / Cover Art Archive / TheAudioDB / Deezer):**
   * Public metadata APIs are queried using artist and album names to fetch album covers and artist biographies.
3. **Optional Scrobbling (ListenBrainz / Libre.fm):**
   * If you explicitly configure scrobbling, playback logs (track title, artist, time played) are submitted to your chosen scrobbling service using the user token you provide.
4. **Offline Blackout Mode:**
   * You can enable "Offline Blackout Mode" in Settings at any time to instantly cut all outbound network requests across the entire application.

---

## 3. Analytics, Tracking & Advertising

* **No Ads:** CassetteCat contains zero advertisements.
* **No Analytics:** We do not include Google Analytics, Firebase, Crashlytics, Facebook SDK, or any third-party telemetry.
* **No Trackers:** There are no behavioral trackers or background data collectors embedded in the app.

---

## 4. Data Retention & Deletion

All data generated within the app (playlists, playback history, local listening stats, and server logins) is stored locally in your device's app storage.

You can delete your data at any time by:
* Using the **"Clear Credentials"** or **"Reset Listening Record"** options in App Settings.
* Clearing the app data from Android System Settings (*Settings > Apps > CassetteCat > Storage > Clear Data*).
* Uninstalling the application.

---

## 5. Children's Privacy

CassetteCat does not address anyone under the age of 13 and does not knowingly collect any personally identifiable information from children.

---

## 6. Open Source Transparency

CassetteCat is free and open-source software licensed under the **GNU General Public License v3.0 (GPL-3.0)**. The complete source code is publicly auditable at:
[https://github.com/samyyy2311/CassetteCat](https://github.com/samyyy2311/CassetteCat)

---

## 7. Changes to This Policy

Any updates to this Privacy Policy will be reflected on this page with a revised "Last Updated" date.

---

## 8. Contact Us

If you have any questions or feedback regarding this Privacy Policy, please open an issue on GitHub:
* GitHub: [https://github.com/samyyy2311/CassetteCat/issues](https://github.com/samyyy2311/CassetteCat/issues)
