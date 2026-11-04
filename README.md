# SpoofMyDevice

SpoofMyDevice 2.0 is an Xposed module and companion app for building, saving, and applying spoofed Android device profiles to selected apps.

The companion UI is written entirely in Jetpack Compose and uses Material 3 Expressive components, a Tonal Spot palette, and the 2025 Material color specification.

## Features

- Reusable preset-based and fully editable custom profiles
- Default Profile support for automatic spoofing of all Xposed-scoped apps without per-app assignment
- Independent app-to-profile assignments
- Dedicated profile management and per-app profile assignment screens
- Live profile propagation to already hooked scoped apps
- Per-field enable controls and one-tap randomization for advanced identifiers
- Safe Mode that keeps the real `Build.VERSION` and `ro.*.build.version.*` values for selected apps
- Model, fingerprint, telephony, and identifier hooks remain enabled in Safe Mode
- System apps, system UIDs, isolated processes, launchers, and input methods are excluded from spoofing
- Optional resolution, density, window metrics, and configuration spoofing
- Locale, timezone, Build ID, and Display ID spoofing
- English, Korean, Japanese, and Simplified Chinese UI
- Runtime diagnostics and app information screens
- Rooted LSPosed and non-root patch-manager workflows

## Requirements

- Android 8.0 or newer
- One of the following module runtimes:
  - LSPosed on a rooted device
  - A non-root patch manager that implements the legacy Xposed API, such as LSPatch or NPatch

## Rooted LSPosed Setup

1. Install the SpoofMyDevice APK and open it once.
2. Enable SpoofMyDevice in LSPosed.
3. Add ordinary target apps to the module scope. System apps and Android System Framework are excluded.
4. Open **Profiles**, create a profile, then select a preset or edit its custom values and save it.
5. *(Optional)* Set a profile as **Default** in the Profiles tab. Any app in the Xposed scope without an explicit profile assignment will automatically use the default profile.
6. Open **Apps** to assign specific profiles to individual apps if needed (overriding the default profile).
7. Force stop and relaunch newly assigned target apps.

## Non-root Setup

1. Install the SpoofMyDevice APK normally and open it once.
2. In LSPatch or NPatch, register SpoofMyDevice as the Xposed module for the target APK.
3. Patch the target APK and install the patched result.
4. Create and save a profile in **Profiles**, set it as Default or assign it directly to the patched app in **Apps**.
5. Relaunch the patched target app.

Patched APK signatures differ from the original signatures. Apps that enforce signature, installer, or integrity checks may reject a patched build, and an official app update replaces the patched APK.

## Boot Safety

Version 2.0.4 reads fresh, original `sys.boot_completed` and `init.svc.bootanim` values before starting target-process work. Spoofing starts only when boot is complete and bootanimation is stopped. Processes started during boot keep real values until they are force stopped and relaunched after boot.

Profile reads and config-change handling run on a background worker. Hook callbacks use cached configuration and never reload preferences or call the companion provider. Temporary read failures retain the last valid profile; a valid unassignment clears the runtime profile and restores captured Build fields. Shell output is rewritten as the caller reads it, with the original process exit status, cancellation, stdin, and stderr preserved.

These changes remove blocking paths found in source. They have not yet been verified on the reported iQOO Z10 Turbo Plus / KernelSU / LSPosed 2.2.1 soft reboot. Keep Boot Guard installed until the updated module has passed device testing with SpoofMyDevice enabled through reboot. See [the investigation and device checks](docs/boot-hang-investigation.md).

## Safe Mode

Safe Mode exists to prevent crashes when the spoofed profile reports a newer Android version than the physical device. For selected apps it bypasses only:

- `Build.VERSION` fields
- `ro.*.build.version.*` properties

Model, fingerprint, network, telephony, and identifier spoofing still applies. Android System Framework is never hooked, regardless of Safe Mode selection. Safe Mode protection also applies when an app receives spoofing via the Default Profile.

## Configuration & Profile Resolution

The companion app stores reusable profiles, app assignments, default profile selection, and global runtime options together in one app-private `app_profiles.json` file.

When a hooked application process requests a configuration:
1. **Explicit Assignment:** If the package has a specific profile assigned in **Apps**, that profile is applied.
2. **Default Profile:** If no specific profile is assigned, but a **Default Profile** is configured in **Profiles**, the default profile is applied.
3. **Unassigned:** If no explicit assignment exists and no Default Profile is set, the package is left unhooked.

Applying a profile broadcasts a config-change event so running target processes can reload it asynchronously. During a cold start, an app can read or cache real identifiers before the worker finishes loading its profile. Relaunch target apps after profile changes, and force stop processes that started during boot. Some apps cache identifiers internally and require a force stop after profile changes.

## Build From Source

```bash
git clone https://github.com/BuSung-dev/SpoofMyDevice.git
cd SpoofMyDevice
./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Disclaimer

This project is intended for research, testing, and authorized use. Follow applicable laws, platform rules, and the policies of the apps being tested.
