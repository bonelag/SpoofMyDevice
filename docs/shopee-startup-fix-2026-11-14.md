# Shopee startup compatibility repair, 2026-11-14

Shopee 3.85.27 remained at its promotional splash when SpoofMyDevice 2.0.6 applied the user's Samsung Galaxy Z Flip6 profile. The connected iQOO V2507A runs physical API 35; the saved profile requests API 34. The module remained enabled, with Shopee and Device Info HW in its user-0 scope.

## Runtime evidence

The captured Shopee APK contains ShadowHook 1.1.1, while the module contains ShadowHook 2.0.1. Their Java class loaders and native namespaces are separate, but both engines modify the same process linker. The startup stack reaches Shopee's ShadowHook initializer and the linker's constructor dispatch, then enters Shopee's native crash handler. That handler waits on `pthread_join` while loader activity is blocked. The main thread waits for MediaTek launch-boost class initialization, and Android later reports an ANR for a queued boot-completed receiver.

The app's library Build ID is `8411b301c9aafef50292dca701d9bc8ce7f2172c`, matching both the captured current stack and the historical November 13 SIGTRAP. REA ELF-note Evidence `ev_b62a8571cd408622a077fe601033ad69aeaf2db15d3a026eebf2403e0fe6da57` binds that library to SHA-256 `f0a22bcd1fc9bb7bcbf0b54fc587a0afd8a700742d7ea6af29656ddbca382cda`.

The first candidate added physical-SDK scopes around native initialization, but the user and a fresh stack confirmed that the splash still stalled. Avoiding the additional module-owned native engine allowed Shopee to render its home page with the original profile. The user independently confirmed access, and a captured screenshot corroborates the home page.

## Implementation

- `NativeRuntimeConflictPolicy` resolves the host's ShadowHook class without running its initializer and compares its owning loader with the module's loader. Parent delegation to the module's own dependency does not count as a host runtime. An incomplete host runtime is handled conservatively.
- `NativeIdentity.install` receives the target class loader and skips direct native installation when the host contains its own ShadowHook engine. It retains the module's normal native backend for other supported applications.
- The existing conditional runtime-SDK guard also covers supported ShadowHook/ByteHook initializers, including R8-renamed synchronized Java wrappers and `nativeInit`. Native getters and ordinary application methods are excluded. Host native-library class loaders are not classified as Chromium Handler loaders.
- Profile storage, assignments, Safe Mode, toggles, process/boot checks, native source, and version metadata remain unchanged. No preset or application data was reset.

## Coverage and boundary

Java Build values, Java-visible system properties, supported Java identity readers, and supported shell/process rewriting continue to use the assigned profile. Apps with a host-owned ShadowHook runtime do not receive direct native identity hooks; their raw native reads can return physical values. This compatibility behavior prevents the conflicting native installation and does not claim complete native spoofing for Shopee.

Applications without the conflict retain the existing ARM64/ARMv7 native implementation. Packaged x86/x86_64 stubs and existing native recursion, serial, and path-index behavior are preserved.

## Validation

Fresh verification uses `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`. The regression suite covers host-owned runtimes without premature initialization, delegation to module-owned dependencies, absent/bootstrap loaders, incomplete runtime dependencies, native initializer selection, and restoration of a lower reported SDK after initialization.

The device workflow installs only changed, signature-verified APKs, relaunches Shopee, captures process/window/log evidence, and compares the final profile state and module scope with their baseline. Retained local evidence is under ignored `build/shopee-startup-2026-11-14/`; final results and artifact identity are recorded in [MEMORY.md](../MEMORY.md).

The original boot ID is `488caf38-6916-4333-8ddf-284165d99e62`. Baseline Zygote, system_server, and SurfaceFlinger PIDs are 28228, 28759, and 28226. No reboot, soft reboot, Zygote/system-service restart, application-data clearing, or profile/scope mutation was performed. This is app-startup validation and does not constitute a soft-reboot test.
