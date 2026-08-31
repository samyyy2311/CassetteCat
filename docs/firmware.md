# Firmware Architecture & Development

The CassetteCat firmware powers the ESP32-S3 hardware companion audio player.

## Architectural Goals

1. **Standalone Playback**: Mount SD card over SPI/SDMMC, decode audio streams (FLAC, MP3, WAV, AAC), and stream via DMA-backed I2S to the PCM5102 DAC.
2. **Companion Synchronization**:
   - Host an HTTP/REST server on the ESP32 for file transfer, metadata synchronization, and diagnostics.
   - Support both SoftAP mode (standalone Wi-Fi network) and Station mode (joining existing LAN with mDNS advertising).
   - Song sync wire contract is defined in [device-sync-protocol.md](device-sync-protocol.md), built app-side ahead of firmware.
   - Remote playback, device management, storage browsing, and OTA upload contracts are defined in [device-control-protocol.md](device-control-protocol.md), also built app-side ahead of firmware.
3. **Firmware Updates**: Utilize ESP-IDF's built-in `esp_https_ota` framework for the primary device-initiated OTA flow (`POST /api/ota/from-url`). The fallback path, where the app sends the `.bin` directly over `POST /api/ota` as a multipart upload, doesn't go through `esp_https_ota` since there's no URL to fetch from; firmware parses the multipart body itself and drives the lower-level `esp_ota_begin` / `esp_ota_write` / `esp_ota_end` / `esp_ota_set_boot_partition` sequence directly against the received bytes. Both paths are specified in [device-control-protocol.md](device-control-protocol.md).

---

## Planned Firmware Subsystems

```
┌─────────────────────────────────────────────────────────────┐
│                    ESP32-S3 Firmware                        │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────────────────┐       ┌───────────────────────┐  │
│  │   UI & Display Engine │       │ Audio Pipeline (I2S)  │  │
│  │     (ST7735R Driver)  │       │ (PCM5102 + DMA Buff)  │  │
│  └───────────▲───────────┘       └───────────▲───────────┘  │
│              │                               │              │
│  ┌───────────┴───────────────────────────────┴───────────┐  │
│  │                     Core Player Loop                  │  │
│  └───────────▲───────────────────────────────▲───────────┘  │
│              │                               │              │
│  ┌───────────┴───────────┐       ┌───────────┴───────────┐  │
│  │ SD Card Storage Engine│       │  Wi-Fi & HTTP Server  │  │
│  │    (FATFS / SPI)      │       │ (SoftAP/Station/mDNS) │  │
│  └───────────────────────┘       └───────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Toolchain & Build Setup (Future)

* **Target Framework**: ESP-IDF v5.x (recommended for native Wi-Fi throughput and robust OTA support).
* **Source Code**: Source code will be located in the `firmware/` directory once scaffolding begins.
* **Licensing**: All firmware code in `firmware/` is licensed under [GPL-3.0](../LICENSE).
