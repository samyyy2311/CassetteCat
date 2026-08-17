# Hardware Specification (Prototype)

This document outlines the verified hardware bill of materials (BOM), pin mapping, and power architecture for the prototype CassetteCat companion player.

## Bill of Materials (BOM)

| Component | Part / Module | Description / Notes |
|---|---|---|
| **Main Controller** | ESP32-S3-WROOM-N16R8 DevKitC | Dual USB-C, 16MB Flash, 8MB PSRAM |
| **Audio DAC** | Adafruit PCM5102 | 32-bit I2S Stereo DAC (line out) |
| **Display & Storage** | 1.8" TFT LCD (ST7735R) | 128×160 resolution, 4-wire SPI, onboard microSD slot |
| **Battery** | NOVA 103450 LiPo (2000mAh) | Includes integrated battery protection PCB |
| **Charger** | TP4056 USB-C Module | Single-cell Li-ion/LiPo charging |
| **Boost Converter** | MT3608 Module | Step-up regulator supplying 5V to ESP32 VIN |
| **Input Module** | 8-Key Touch Button Module | Capacitive touch interface |

---

## ESP32-S3 Pin Mapping

The verified pin assignments for the ESP32-S3 module:

| Subsystem | Signal | ESP32-S3 GPIO | Notes |
|---|---|---|---|
| **I2S Audio (PCM5102)** | BCK (Bit Clock) | GPIO 4 | Audio clock line |
| | WS (Word Select / LRCLK) | GPIO 5 | Left/Right channel select |
| | DATA (Data Out / DOUT) | GPIO 6 | Serial audio data stream |
| **I2C Bus** | SDA | GPIO 18 | Peripheral data line |
| | SCL | GPIO 8 | Peripheral clock line |
| **Touch Buttons** | Key inputs | GPIO 9, 10, 11, 12 | Digital touch inputs |
| **Power Control** | Deep Sleep Wake | GPIO 12 | Soft power toggle |
| **Display & MicroSD SPI** | Dedicated SPI | Assigned per board layout | Dedicated bus for ST7735R + SD |

---

## Power Distribution Architecture

```
[ LiPo Battery (3.7V) ] 
       │
       ├────► [ TP4056 Charger ] ◄──── [ USB-C 5V Input ]
       │
       └────► [ MT3608 Boost Converter (5V) ] 
                     │
                     ├────► [ ESP32-S3 VIN (onboard 3.3V LDO) ]
                     ├────► [ ST7735R Display (3.3V/5V compatible) ]
                     └────► [ PCM5102 I2S DAC (3.3V) ]
```

---

## Licensing & CAD Files

Hardware design files, schematics, and mechanical CAD models located in `hardware/` are licensed under [CERN-OHL-S-2.0](../hardware/LICENSE).
