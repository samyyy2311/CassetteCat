# Firmware Architecture & Development

The CassetteCat firmware powers the ESP32-S3 hardware companion audio player.

## Architectural Goals

1. **Standalone Playback**: Mount SD card over SPI/SDMMC, decode audio streams (FLAC, MP3, WAV, AAC), and stream via DMA-backed I2S to the PCM5102 DAC.
2. **Companion Synchronization**:
   - Host an HTTP/REST server on the ESP32 for file transfer, metadata synchronization, and diagnostics.
   - Support both SoftAP mode (standalone Wi-Fi network) and Station mode (joining existing LAN with mDNS advertising).
   - Song sync wire contract is defined in [device-sync-protocol.md](device-sync-protocol.md), built app-side ahead of firmware.
   - Remote playback, device management, storage browsing, and OTA upload contracts are defined in [device-control-protocol.md](device-control-protocol.md), also built app-side ahead of firmware.
3. **Firmware Updates**: Utilize ESP-IDF's built-in `esp_https_ota` framework for over-the-air firmware updates initiated by the mobile app.

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
