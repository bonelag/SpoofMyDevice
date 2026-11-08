# Additional Android Preset References

Added 34 Android-device presets from [Rahulkum7017/SpoofMyDevice_Devices](https://github.com/Rahulkum7017/SpoofMyDevice_Devices) at commit [a0fa844cb6a637af597adcde4c66d68253d1027d](https://github.com/Rahulkum7017/SpoofMyDevice_Devices/tree/a0fa844cb6a637af597adcde4c66d68253d1027d). The original nine presets were preserved.

The reference supplies model, Build, display, operator, and SoC fields. CPU layouts, parts, frequencies/cache, RAM/DDR, logical storage, graphics strings, GNSS labels, and kernel metadata were added as explicit configuration values for the current app. These files are configured identity examples, not verified factory captures.

Build IDs/display IDs and descriptions were normalized to each reference fingerprint's release/build/incremental tokens. Nothing Phone (3) uses Snapdragon 8s Gen 4 / SM8735 / Adreno 825 rather than the reference's Snapdragon 8 Elite. Model labels omit repeated brand names in the catalog.

Meizu 22 Pro and Motorola Edge 60 Ultra retain unverified reference model assumptions and are marked as reference simulations in their summaries. The Apple simulations and HarmonyOS-labeled Huawei entry were omitted from the selected Android-device scope. Existing IDs were not imported twice.

Added graphics strings use generic OpenGL ES 3.2 / GLSL ES 3.20 and conservative Vulkan API labels. The 0.0.0 driver version is an unspecified version marker. Baseband MPSS, build hosts/users, GNSS generation years, and kernel strings are generic configured metadata; timestamps are derived from the reference security-patch date at midnight UTC. Per-device component vendors, MAC addresses, and hardware-backed identifiers use the app's existing behavior unless configured by the user.

## Source files

| Local preset | Pinned reference | Supplemental SoC identity |
| --- | --- | --- |
| [ASUS ROG Phone 9 Pro.json](<ASUS ROG Phone 9 Pro.json>) | [ROG Phone 9 Pro.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/ROG%20Phone%209%20Pro.json) | Snapdragon 8 Elite |
| [HONOR Magic7 Pro.json](<HONOR Magic7 Pro.json>) | [HONOR Magic7 Pro.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/HONOR%20Magic7%20Pro.json) | Snapdragon 8 Elite |
| [iQOO 12.json](<iQOO 12.json>) | [iQOO 12.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/iQOO%2012.json) | Snapdragon 8 Gen 3 |
| [iQOO 13.json](<iQOO 13.json>) | [iQOO 13.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/iQOO%2013.json) | Snapdragon 8 Elite |
| [Meizu 22 Pro.json](<Meizu 22 Pro.json>) | [Meizu 22 Pro.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Meizu%2022%20Pro.json) | Snapdragon 8 Elite; reference simulation |
| [Motorola Edge 60 Ultra.json](<Motorola Edge 60 Ultra.json>) | [Motorola Edge 60 Ultra.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Motorola%20Edge%2060%20Ultra.json) | Snapdragon 8 Elite; reference simulation |
| [Nothing Phone 3.json](<Nothing Phone 3.json>) | [Nothing Phone 3.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Nothing%20Phone%203.json) | Snapdragon 8s Gen 4 |
| [OnePlus 10 Pro.json](<OnePlus 10 Pro.json>) | [OnePlus 10 Pro.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/OnePlus%2010%20Pro.json) | Snapdragon 8 Gen 1 |
| [OnePlus 10T.json](<OnePlus 10T.json>) | [OnePlus 10T.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/OnePlus%2010T.json) | Snapdragon 8+ Gen 1 |
| [OnePlus 11.json](<OnePlus 11.json>) | [OnePlus 11.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/OnePlus%2011.json) | Snapdragon 8 Gen 2 |
| [OnePlus 12.json](<OnePlus 12.json>) | [OnePlus 12.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/OnePlus%2012.json) | Snapdragon 8 Gen 3 |
| [OnePlus 13.json](<OnePlus 13.json>) | [OnePlus 13.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/OnePlus%2013.json) | Snapdragon 8 Elite |
| [OnePlus 9 Pro.json](<OnePlus 9 Pro.json>) | [OnePlus 9 Pro.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/OnePlus%209%20Pro.json) | Snapdragon 888 |
| [OnePlus Ace 2 Pro.json](<OnePlus Ace 2 Pro.json>) | [OnePlus Ace 2 Pro.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/OnePlus%20Ace%202%20Pro.json) | Snapdragon 8 Gen 2 |
| [OnePlus Ace 3 Pro.json](<OnePlus Ace 3 Pro.json>) | [OnePlus Ace 3 Pro.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/OnePlus%20Ace%203%20Pro.json) | Snapdragon 8 Gen 3 |
| [OnePlus Ace 5 Pro.json](<OnePlus Ace 5 Pro.json>) | [OnePlus Ace 5 Pro.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/OnePlus%20Ace%205%20Pro.json) | Snapdragon 8 Elite |
| [OnePlus Open.json](<OnePlus Open.json>) | [OnePlus Open.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/OnePlus%20Open.json) | Snapdragon 8 Gen 2 |
| [OPPO Find X8 Ultra.json](<OPPO Find X8 Ultra.json>) | [OPPO Find X8 Ultra.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/OPPO%20Find%20X8%20Ultra.json) | Snapdragon 8 Elite |
| [Realme GT 2 Pro.json](<Realme GT 2 Pro.json>) | [Realme GT 2 Pro.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Realme%20GT%202%20Pro.json) | Snapdragon 8 Gen 1 |
| [Realme GT 3.json](<Realme GT 3.json>) | [Realme GT 3.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Realme%20GT%203.json) | Snapdragon 8+ Gen 1 |
| [Realme GT 5 Pro.json](<Realme GT 5 Pro.json>) | [Realme GT 5 Pro.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Realme%20GT%205%20Pro.json) | Snapdragon 8 Gen 3 |
| [Realme GT 6.json](<Realme GT 6.json>) | [Realme GT 6.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Realme%20GT%206.json) | Snapdragon 8s Gen 3 |
| [Realme GT 7 Pro.json](<Realme GT 7 Pro.json>) | [Realme GT 7 Pro.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Realme%20GT%207%20Pro.json) | Snapdragon 8 Elite |
| [REDMAGIC 10 Pro.json](<REDMAGIC 10 Pro.json>) | [Nubia 10 Pro.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Nubia%2010%20Pro.json) | Snapdragon 8 Elite |
| [Samsung Galaxy S22 Ultra.json](<Samsung Galaxy S22 Ultra.json>) | [Samsung Galaxy S22 Ultra.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Samsung%20Galaxy%20S22%20Ultra.json) | Snapdragon 8 Gen 1 |
| [Samsung Galaxy S23 Ultra.json](<Samsung Galaxy S23 Ultra.json>) | [Samsung Galaxy S23 Ultra.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Samsung%20Galaxy%20S23%20Ultra.json) | Snapdragon 8 Gen 2 for Galaxy |
| [Samsung Galaxy S24 Ultra.json](<Samsung Galaxy S24 Ultra.json>) | [Samsung SM-S928B.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Samsung%20SM-S928B.json) | Snapdragon 8 Gen 3 for Galaxy |
| [Samsung Galaxy Z Flip6.json](<Samsung Galaxy Z Flip6.json>) | [Samsung Galaxy Z Flip6.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Samsung%20Galaxy%20Z%20Flip6.json) | Snapdragon 8 Gen 3 for Galaxy |
| [Samsung Galaxy Z Fold6.json](<Samsung Galaxy Z Fold6.json>) | [Samsung Galaxy Z Fold6.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Samsung%20Galaxy%20Z%20Fold6.json) | Snapdragon 8 Gen 3 for Galaxy |
| [Sony Xperia 1 VI.json](<Sony Xperia 1 VI.json>) | [Xperia 1 VI.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Xperia%201%20VI.json) | Snapdragon 8 Gen 3 |
| [Vivo X100 Ultra.json](<Vivo X100 Ultra.json>) | [Vivo X100 Ultra.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Vivo%20X100%20Ultra.json) | Snapdragon 8 Gen 3 |
| [Vivo X200 Ultra.json](<Vivo X200 Ultra.json>) | [Vivo X200 Ultra.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Vivo%20X200%20Ultra.json) | Snapdragon 8 Elite |
| [Xiaomi 14 Ultra.json](<Xiaomi 14 Ultra.json>) | [Xiaomi 14 Ultra.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Xiaomi%2014%20Ultra.json) | Snapdragon 8 Gen 3 |
| [Xiaomi 15 Pro.json](<Xiaomi 15 Pro.json>) | [Xiaomi 15 Pro.json](https://raw.githubusercontent.com/Rahulkum7017/SpoofMyDevice_Devices/a0fa844cb6a637af597adcde4c66d68253d1027d/Xiaomi%2015%20Pro.json) | Snapdragon 8 Elite |

## Validation boundary

All presets must preserve the supported profile schema, string-valued hardware metadata, unique IDs, bounded per-core arrays, coherent CPU/GPU mappings, and valid serialized configuration. Host-JVM checks exercise the existing catalog, profile/config round-trips, and identity views. They do not establish Android device execution or factory authenticity.

The source blobs and SHA-256 hashes, pre-import preset hashes, and normalization ledger are retained locally under build/preset-import. Refresh the configured GitHub source and reselect/save a profile to apply an updated preset.
