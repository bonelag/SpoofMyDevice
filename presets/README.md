# SpoofMyDevice Device Presets

Each JSON file configures a device identity for the app's supported Java, native, property, and shell readers. The catalog accepts JSON files at a configured GitHub repository root, in its `presets` directory, or at a source URL pointing directly to that directory.

## Included configurations

Every preset explicitly sets per-core CPU frequencies and parts, RAM capacity/type, storage capacity, marketing name, graphics-driver identity, and kernel metadata.

| Preset | CPU layout | Maximum clock | RAM | Logical storage |
| --- | --- | --- | --- | --- |
| Pixel 7 Pro | Tensor G2, 4 + 2 + 2 cores | 2.85 GHz | 12 GiB LPDDR5 | 256 GiB |
| Pixel 8 Pro | Tensor G3, 4 + 4 + 1 cores | 2.91 GHz | 12 GiB LPDDR5X | 256 GiB |
| Pixel 9 Pro XL | Tensor G4, 4 + 3 + 1 cores | 3.10 GHz | 16 GiB LPDDR5X | 256 GiB |
| Pixel 10 Pro | Tensor G5, 2 + 5 + 1 cores | 3.78 GHz | 16 GiB LPDDR5X | 256 GiB |
| Pixel 11 Pro | Synthetic Tensor G6, 4 + 2 + 1 cores | 4.11 GHz | 16 GiB LPDDR5X | 256 GiB |
| Pixel 11 Pro XL | Synthetic Tensor G6, 4 + 2 + 1 cores | 4.11 GHz | 16 GiB LPDDR5X | 512 GiB |
| Galaxy S25 Ultra | Snapdragon 8 Elite for Galaxy, 6 + 2 cores | 4.47 GHz | 12 GiB LPDDR5X | 256 GiB |
| Galaxy Tab S8 Ultra (SM-X900) | Snapdragon 8 Gen 1, 4 + 3 + 1 cores | 3.00 GHz | 12 GiB LPDDR5 | 256 GiB |
| Xiaomi 15 Ultra | Snapdragon 8 Elite, 6 + 2 cores | 4.32 GHz | 16 GiB LPDDR5X | 512 GiB |
| Galaxy S22 Ultra | Snapdragon 8 Gen 1, 4 + 3 + 1 cores | 3.00 GHz | 12 GiB LPDDR5 | 256 GiB |
| Galaxy S23 Ultra | Snapdragon 8 Gen 2 for Galaxy, 3 + 4 + 1 cores | 3.36 GHz | 12 GiB LPDDR5X | 256 GiB |
| Galaxy S24 Ultra | Snapdragon 8 Gen 3 for Galaxy, 2 + 3 + 2 + 1 cores | 3.39 GHz | 12 GiB LPDDR5X | 256 GiB |
| Galaxy Z Flip6 | Snapdragon 8 Gen 3 for Galaxy, 2 + 3 + 2 + 1 cores | 3.39 GHz | 12 GiB LPDDR5X | 256 GiB |
| Galaxy Z Fold6 | Snapdragon 8 Gen 3 for Galaxy, 2 + 3 + 2 + 1 cores | 3.39 GHz | 12 GiB LPDDR5X | 512 GiB |
| HONOR Magic7 Pro | Snapdragon 8 Elite, 6 + 2 cores | 4.32 GHz | 12 GiB LPDDR5X | 512 GiB |
| iQOO 12 | Snapdragon 8 Gen 3, 2 + 3 + 2 + 1 cores | 3.30 GHz | 16 GiB LPDDR5X | 512 GiB |
| iQOO 13 | Snapdragon 8 Elite, 6 + 2 cores | 4.32 GHz | 16 GiB LPDDR5X | 512 GiB |
| Meizu 22 Pro (reference simulation) | Snapdragon 8 Elite, 6 + 2 cores | 4.32 GHz | 16 GiB LPDDR5X | 512 GiB |
| Motorola Edge 60 Ultra (reference simulation) | Snapdragon 8 Elite, 6 + 2 cores | 4.32 GHz | 16 GiB LPDDR5X | 512 GiB |
| Nothing Phone (3) | Snapdragon 8s Gen 4, 2 + 2 + 3 + 1 cores | 3.21 GHz | 12 GiB LPDDR5X | 256 GiB |
| OnePlus 10 Pro | Snapdragon 8 Gen 1, 4 + 3 + 1 cores | 3.00 GHz | 12 GiB LPDDR5 | 256 GiB |
| OnePlus 10T | Snapdragon 8+ Gen 1, 4 + 3 + 1 cores | 3.20 GHz | 16 GiB LPDDR5 | 256 GiB |
| OnePlus 11 | Snapdragon 8 Gen 2, 3 + 4 + 1 cores | 3.20 GHz | 16 GiB LPDDR5X | 256 GiB |
| OnePlus 12 | Snapdragon 8 Gen 3, 2 + 3 + 2 + 1 cores | 3.30 GHz | 16 GiB LPDDR5X | 512 GiB |
| OnePlus 13 | Snapdragon 8 Elite, 6 + 2 cores | 4.32 GHz | 16 GiB LPDDR5X | 512 GiB |
| OnePlus 9 Pro | Snapdragon 888, 4 + 3 + 1 cores | 2.84 GHz | 12 GiB LPDDR5 | 256 GiB |
| OnePlus Ace 2 Pro | Snapdragon 8 Gen 2, 3 + 4 + 1 cores | 3.20 GHz | 16 GiB LPDDR5X | 512 GiB |
| OnePlus Ace 3 Pro | Snapdragon 8 Gen 3, 2 + 3 + 2 + 1 cores | 3.30 GHz | 16 GiB LPDDR5X | 512 GiB |
| OnePlus Ace 5 Pro | Snapdragon 8 Elite, 6 + 2 cores | 4.32 GHz | 16 GiB LPDDR5X | 512 GiB |
| OnePlus Open | Snapdragon 8 Gen 2, 3 + 4 + 1 cores | 3.20 GHz | 16 GiB LPDDR5X | 512 GiB |
| OPPO Find X8 Ultra | Snapdragon 8 Elite, 6 + 2 cores | 4.32 GHz | 16 GiB LPDDR5X | 512 GiB |
| realme GT 2 Pro | Snapdragon 8 Gen 1, 4 + 3 + 1 cores | 3.00 GHz | 12 GiB LPDDR5 | 256 GiB |
| realme GT 3 | Snapdragon 8+ Gen 1, 4 + 3 + 1 cores | 3.20 GHz | 16 GiB LPDDR5 | 256 GiB |
| realme GT 5 Pro | Snapdragon 8 Gen 3, 2 + 3 + 2 + 1 cores | 3.30 GHz | 16 GiB LPDDR5X | 512 GiB |
| realme GT 6 | Snapdragon 8s Gen 3, 3 + 4 + 1 cores | 3.00 GHz | 16 GiB LPDDR5X | 512 GiB |
| realme GT 7 Pro | Snapdragon 8 Elite, 6 + 2 cores | 4.32 GHz | 16 GiB LPDDR5X | 512 GiB |
| REDMAGIC 10 Pro | Snapdragon 8 Elite, 6 + 2 cores | 4.32 GHz | 16 GiB LPDDR5X | 512 GiB |
| ROG Phone 9 Pro | Snapdragon 8 Elite, 6 + 2 cores | 4.32 GHz | 16 GiB LPDDR5X | 512 GiB |
| Sony Xperia 1 VI | Snapdragon 8 Gen 3, 2 + 3 + 2 + 1 cores | 3.30 GHz | 12 GiB LPDDR5X | 256 GiB |
| vivo X100 Ultra | Snapdragon 8 Gen 3, 2 + 3 + 2 + 1 cores | 3.30 GHz | 16 GiB LPDDR5X | 512 GiB |
| vivo X200 Ultra | Snapdragon 8 Elite, 6 + 2 cores | 4.32 GHz | 16 GiB LPDDR5X | 512 GiB |
| Xiaomi 14 Ultra | Snapdragon 8 Gen 3, 2 + 3 + 2 + 1 cores | 3.30 GHz | 16 GiB LPDDR5X | 512 GiB |
| Xiaomi 15 Pro | Snapdragon 8 Elite, 6 + 2 cores | 4.32 GHz | 16 GiB LPDDR5X | 512 GiB |

Capacities describe the selected simulated variant. One GiB is `1024^3` bytes; configured storage totals are logical capacities, not measured usable space. CPU stepping/cache metadata and kernel strings are configured identity data rather than verified factory captures. Both Pixel 11/Tensor G6 presets are synthetic examples. Meizu 22 Pro and Motorola Edge 60 Ultra retain unverified reference assumptions; their summaries identify them as reference simulations.

The catalog contains 43 presets, including 34 additional Android-device configurations referenced from [Rahulkum7017/SpoofMyDevice_Devices](https://github.com/Rahulkum7017/SpoofMyDevice_Devices). [SOURCES.md](SOURCES.md) records the pinned source files, parameter choices, corrections, and validation boundary.

## JSON format

The outer object contains a stable `id`, catalog labels, a `summary`, and a `profile`. Standard profile fields configure Build, screen, radio, GPU, and CPU identity. Additional supported hardware values belong inside `profile.hardwareProperties`.

This example shows the hardware-related fields; [Google Pixel 10 Pro.json](<Google Pixel 10 Pro.json>) contains the complete preset.

```json
{
  "id": "pixel_10_pro",
  "brandLabel": "Google",
  "modelLabel": "Pixel 10 Pro",
  "summary": "Tensor G5 - 16 GiB RAM / 256 GiB storage - Android 16",
  "profile": {
    "brand": "google",
    "manufacturer": "Google",
    "model": "Pixel 10 Pro",
    "buildRelease": "16",
    "buildSdk": 36,
    "socModel": "Google Tensor G5",
    "socManufacturer": "Google",
    "cpuHardware": "Google Tensor G5",
    "cpuCores": 8,
    "gpuVendor": "Imagination Technologies",
    "gpuRenderer": "PowerVR DXT-48-1536",
    "hardwareProperties": {
      "hardware.market_name": "Pixel 10 Pro",
      "hardware.ram_bytes": "17179869184",
      "hardware.ram_type": "LPDDR5X",
      "hardware.storage_bytes": "274877906944",
      "hardware.graphics_driver": "powervr",
      "cpu.max_frequencies": "2250000,2250000,3050000,3050000,3050000,3050000,3050000,3780000",
      "cpu.min_frequency": "300000",
      "cpu.parts": "0xd80,0xd80,0xd87,0xd87,0xd87,0xd87,0xd87,0xd82",
      "cpu.implementer": "0x41",
      "cpu.cache_l3": "12288K",
      "kernel.release": "6.6.0-android16-laguna",
      "kernel.version": "#1 SMP PREEMPT Wed Aug 20 00:00:00 UTC 2025",
      "kernel.machine": "aarch64",
      "kernel.hostname": "localhost"
    }
  }
}
```

All `hardwareProperties` values must be strings. Keys use letters, digits, `_`, `.`, or `-` in the hardware/kernel/sensor/network/extended-CPU namespaces. Values must fit one line and contain no newline, carriage return, or NUL. `cpuHardware` and `cpuCores` remain ordinary profile fields.

## Supported hardware overrides

| Property | Format and effect |
| --- | --- |
| `hardware.market_name` | Human-readable name for marketing/OEM aliases; `profile.model` remains the Build model |
| `hardware.ram_bytes` | Decimal byte count for RAM/DDR aliases, procfs memory data, and supported memory APIs |
| `hardware.ram_type` | DDR label, such as `LPDDR5` or `LPDDR5X` |
| `hardware.storage_bytes` | Decimal logical byte count for the supported storage API |
| `hardware.graphics_driver` | `mali`, `adreno`, or `powervr`, matching the profile's GPU identity |
| `cpu.max_frequencies` | Comma-separated per-core maximum frequencies in kHz, indexed from CPU 0 |
| `cpu.min_frequency` | Minimum frequency in kHz for the virtual CPU files |
| `cpu.parts` | Comma-separated per-core hexadecimal ARM/Qualcomm part IDs for CPU info and MIDR |
| `cpu.implementer` | Hexadecimal implementer ID; these presets use `0x41` for Cortex cores and `0x51` for Oryon cores |
| `cpu.cache_l3` | L3 cache size in KiB with a `K` suffix, such as `12288K` |
| `kernel.release`, `kernel.version` | Configured kernel identity for procfs, Java properties, and supported `uname` readers |
| `kernel.machine`, `kernel.hostname` | Machine architecture and hostname for the same kernel view |

Both CPU arrays must contain exactly `cpuCores` entries. The current app accepts 1-32 cores, frequencies from 1,000 to 10,000,000 kHz, and part IDs from `0x000` to `0xfff`; malformed arrays fall back to the app's default layout. Each preset provides valid arrays so its visible CPU topology follows the configured profile.

Optional overrides include `cpu.features`, `cpu.variant`, `cpu.revision`, `hardware.ram_vendor`, component names such as `hardware.audio`/`hardware.pmic`/`hardware.wifi`, and sensor names/vendors such as `sensor.accelerometer.name`/`sensor.accelerometer.vendor`. Supply captured component metadata when exact component identification matters. Unspecified sensors/components use profile-derived labels.

Network addresses derive deterministically from the profile. `network.wifi_mac` and `network.bluetooth_mac` accept colon-separated six-byte MAC addresses when an explicit override is needed.

## Applying an updated preset

1. Make the updated JSON files available through the configured GitHub preset source, then refresh the catalog in the app.
2. Create a profile from the refreshed preset, or select that preset again in an existing profile and save it. Saved profiles retain their previous values until edited.
3. Assign the saved profile to intended scoped apps, then force stop and relaunch those apps.

Per-field switches, Safe Mode, and process/boot safeguards still apply. Requested `buildSdk` values remain in saved presets; the runtime caps a higher requested API at the physical OS API so applications do not select unavailable methods. These presets configure supported identity readers; physical execution capabilities and hardware-backed attestation remain properties of the real device.
