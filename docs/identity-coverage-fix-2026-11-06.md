# Identity coverage repair, 2026-11-06

> Update for 2.0.6, 2026-11-09: this report describes the November 6 identity-fix artifact. The later November 8 investigation identified native tracing reentry and newer-SDK API-selection failures. Current source uses a thread-local native recursion guard, retains bionic property handles/serials, and caps active SDK properties at the physical OS API. The snapshot-serial behavior below is historical. Combined startup/System Extra and renewed soft-reboot validation remain outstanding; see the dated investigation in [MEMORY.md](../MEMORY.md).

The screenshots show a Pixel profile mixed with the physical iQOO model, MediaTek CPU/topology, kernel, components, RAM, and build description. This repair addresses their common property, file, and shell read paths inside eligible scoped processes. It does not patch Device Info HW's private classes or replace its UI text.

The user limited this pass to source fixes and building an APK. Initial ADB discovery returned no connected device. This APK has not been installed or validated on a phone, and the earlier three successful soft reboots apply to the separate boot-safe APK.

## Source findings and changes

| Surface | Inspected source gap | Resulting behavior |
| --- | --- | --- |
| Marketing/model/build description | Startup caches can run before hooks; OEM aliases may contain older physical values | The initial lightweight profile can publish before virtual-file preparation; canonical profile aliases override old OEM copies |
| CPU and topology | Direct CPU helpers disagreed with per-core files; policy directories were absent | One immutable identity supplies CPU parts, MIDR, frequency clusters, counts, and cpufreq policy files |
| Kernel | Java `os.version` was unhooked; direct `uname -r` returned `Linux` | Java properties, proc files, and supported uname options share the configured kernel view |
| Components | Trailing slashes, shell `cd`/glob loops, and several inventory roots bypassed exact matching | POSIX normalization and closed bus/regulator/device-tree inventories redirect the supported readers |
| RAM/DDR | DDR aliases/files were missing; older memory hooks used a fixed capacity | Assigned-profile RAM and optional RAM-type metadata supply the supported memory and DDR surfaces |
| Network and graphics | Invalid MAC overrides could fall back to real addresses; G5 driver aliases still selected Mali | MAC fallback stays valid and consistent; driver aliases follow the configured GPU, with platform selection reads preserved |

The target's static read paths were recovered from the existing Device Info HW 5.25.0 JADX output: `b1.S`, `b1.J.h`, `f1.e/l`, `h1.c/e`, `b1.F/AbstractC0356n/G/H`, and the long-lived `t1.a` shell. The saved APK SHA-256 remains `EB5D290355D9F7C8DCB7EB7ABEA08B08C409211CCA33AF4DE037BCCE422FDA90`. These are ordinary-tool static observations, not new REA decompilation or device execution.

`IdentityPaths` normalizes POSIX paths independently of the host OS. `ShellIdentityCommand` rewrites readonly argv without evaluating caller text. Generated shell functions handle options, multiple files, `cd`, relative globs, missing dump aliases, and function reset on profile changes. Original process streams and lifecycle remain delegated.

Native ARM hooks additionally cover bionic 64-bit/fortified open variants, lstat/fstatat/readlink variants, and property reads with a null name buffer. Alias symbols are deduplicated by address. Property serials follow snapshot changes, and removed virtual values clear their output. Late application-library ranges use a fixed atomic ELF table; its linker callbacks perform no allocation, I/O, linker enumeration, or global locking. Driver/ABI selection reads retain physical values for recognized platform callers.

First-application, UID, package, application-flag, and fresh boot-readiness checks remain required. Initial cache waiting is capped at 100 ms. Full preference/provider transport and file preparation remain on the worker. Live reloads prepare complete views before publishing a new snapshot; failed reads preserve the last valid configuration, and valid unassignment clears it.

## Optional metadata

Both editors accept a `hardwareProperties` JSON object. Values are strings, with keys restricted to the hardware/kernel/sensor/network/extended-CPU namespaces. For example:

```json
{
  "hardware.ram_bytes": "17179869184",
  "hardware.ram_type": "LPDDR5X",
  "sensor.accelerometer.name": "Configured accelerometer",
  "sensor.accelerometer.vendor": "Configured vendor",
  "hardware.pmic": "Configured PMIC",
  "kernel.release": "6.6-configured"
}
```

Fallback component labels, RAM-vendor labels, and kernel strings are synthetic profile data. They are not verified factory BOM values. Supply verified component metadata when exact factory identification is required.

## Validation and artifact

Fresh `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` verification passed with JDK 21. Results: 60 tests across 14 suites, zero failures/errors/skips, and lint with zero errors and 122 warnings. All four packaged ABIs built successfully. App metadata remains `com.spoofmydevice`, version `2.0.5`, versionCode 13, min SDK 26, target SDK 37.

Review artifact: [SpoofMyDevice-2.0.5-identity-fix.apk](../release/SpoofMyDevice-2.0.5-identity-fix.apk), 30,803,263 bytes. SHA-256: `D20FAE1D5643E72D20D903AC11CF337332410A7499D7A5CE51A074292C04B183`. The adjacent checksum file matches the source and release-copy hashes.

APK Signature Scheme v2 cryptographic verification passed with Android SDK apksigner and the existing Android Debug certificate, SHA-256 `6fd52f5e1da33a135198423d0c8582c787d53ede6da499a3c4841f2b91f216cf`. ZIP inspection confirmed the new Java classes/shell marker and the ARM native API variants in the actual APK. REA signing inspection parent Evidence is `ev_0a3999fa7033376e0762b8c463ec9a91d97ebf15744cdf39206df96e246b3ccf`; summary Evidence is `ev_aa333edd9ef0927dc586cd77fadbbdce2ea9523bf9766e11f87c9015209c0050`. REA establishes declared signer/content-digest consistency; the separate apksigner run verifies the signature. Its two records are retained in `build/spoof-coverage/identity-fix/signature-evidence.json`.

Regression coverage includes executed host-shell programs with fixture commands, literal metacharacters, trailing slashes, options, multiple files, `cd`/globs, function reset, argv preservation, invalid MAC input, shared CPU/RAM/network views, immutable inventories, platform-driver caller filtering, and initial snapshot publication. Host-shell behavior is controlled test evidence and does not establish Android runtime behavior.

Native hooks are built for `arm64-v8a` and `armeabi-v7a`; `x86` and `x86_64` retain unsupported native stubs and Java fallback.

## Runtime boundary

Device Info HW and another scoped application still require a live comparison after installing the changed APK. Force stop and relaunch targets after saving a profile or changing assignments. Cold starts with unavailable or stale configuration can still read real values before a valid snapshot arrives. Disabled fields intentionally preserve original values.

Raw syscalls, unsupported external-child/complex-command paths, physical execution capabilities, the actual kernel, and hardware-backed attestation remain outside complete coverage. Readonly API redirection does not implement another device's hardware. Shell `cd` into a virtual inventory changes that shell's working directory to the cache, so subsequent relative operations occur there.

Initial REA discovery Evidence: `ev_0331fb4808d47c464aca9e4294f32ddd49988c2518e945d8369ac91103e3c5e4`, with no artifact identity. A later current-server export contained zero records after the client interruption; the earlier returned observation must not be described as a successfully retained server bundle.

See [boot-safe validation](boot-safety-validation-2026-11-06.md) for the earlier APK's device results. They do not validate this native/shell build.
