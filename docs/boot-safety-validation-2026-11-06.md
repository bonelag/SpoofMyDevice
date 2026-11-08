# Boot safety fix and device validation, 2026-11-06

The current APK completed three KernelSU soft reboots on the connected iQOO V2507A with SpoofMyDevice enabled and no SMD Boot Guard installed. Device Info HW remained the ordinary-app scope. Profiles reporting API 36 and 37 still produced Pixel identities and Android 16/17 in the target after reboot.

These results cover the tested device, runtime, target, and configurations. They do not establish the exact cause of the earlier hang: the affected APK had been uninstalled to recover, and the historical watchdog/crash tags inspected after recovery contained no matching stack. Successful trials must be distinguished from proof of a particular historical crash or deadlock.

## Source changes

- [MainHook.java](../app/src/main/java/com/devicespooflab/hooks/MainHook.java) now applies primary-application, protected UID/process/package, application-flag, and boot checks before the companion activation branch. Loading the companion APK in a system host or as a secondary package no longer resolves its UI classes or installs activation hooks there.
- The `LoadedApk` callback registers the application-attach hook without initializing configuration classes or starting configuration transport. A verified, eligible `Application.attach` context is required before configuration and the main handler are initialized.
- Configuration reads remain on the worker. Hook installation is posted to the main handler after attach; reloads require an attached context/handler, and applying a snapshot rechecks process eligibility.
- [HookSafetyPolicy.java](../app/src/main/java/com/devicespooflab/hooks/utils/HookSafetyPolicy.java) exposes the shared eligibility decision. Three new [policy tests](../app/src/test/java/com/devicespooflab/hooks/utils/HookSafetyPolicyTest.java) cover protected companion hosts, secondary packages, and valid primary application processes.

The profile schema, explicit-assignment/default precedence, failed-read preservation, valid-unassignment behavior, Safe Mode, streaming process rewriting, and app version remain unchanged. Configuration is asynchronous; an app can still cache real values before its first snapshot is ready.

## Tested environment

| Item | Observed value |
| --- | --- |
| Physical device | iQOO V2507A, reported as iQOO Z10 Turbo+ by Device Info HW |
| Physical Android API | 35 |
| Root manager | KernelSU Manager v3.3.1, versionCode 32680 |
| Xposed runtime | LSPosed 2.2.1, versionCode 7912, API 102 |
| Module APK | SpoofMyDevice 2.0.5, versionCode 13 |
| Selected scope | `ru.andr7e.deviceinfohw`, user 0 |
| Target version | Device Info HW 5.25.0, targetSdk 35 |
| Boot Guard | Absent from `/data/adb/modules`; no guard was installed for testing |
| Other modules | Existing modules/scopes retained |

Each controlled trial executed the installed `/data/adb/ksud soft-reboot` command. The observer retained the previous runtime PIDs, streamed logcat, and sampled boot properties and new Zygote/system_server/SurfaceFlinger/launcher processes. Completion required consecutive ready samples plus an enabled SpoofMyDevice state. Module and scope state were read through the installed LSPosed CLI after each reboot.

## Results

| Trial | Assigned profile | Reported API | Time to verified completion | Result |
| --- | --- | --- | --- | --- |
| 1 | Xiaomi 15 Ultra | 35 | 38.9 s | Home screen rendered; module enabled |
| 2 | Pixel 10 Pro | 36 | 36.9 s | Boot complete; module enabled; target UI reported Pixel 10 Pro / Android 16 |
| 3 | Pixel 11 Pro | 37 | 40.3 s | Boot complete; module enabled; target UI reported Pixel 11 Pro / Android 17 |

The ordinary-app scope remained Device Info HW. No trial disabled SpoofMyDevice or depended on a module that disables it during boot. Temporary Pixel assignments were explicitly approved, and the latest pre-trial configuration was restored afterward and compared by canonical JSON hash.

Device Info HW's native/kernel and some model-name surfaces continued to report physical information. That is an existing spoofing-coverage limit, rather than evidence that all hardware identity surfaces were changed. UIAutomator returned a null root node for this target, so screenshots and activity/log evidence were used. OriginOS marks the target `vHidden=1`; explicit activity launch through root worked when ordinary launcher-intent resolution was filtered.

## Build and artifact

Fresh validation command, using JDK 21:

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

- JVM tests: 25 across seven suites, zero failures/errors/skips.
- Debug build: passed.
- Lint: zero errors, 118 warnings.
- APK signature verification: valid APK Signature Scheme v2 with the existing Android Debug signer.
- Artifact: `release/SpoofMyDevice-2.0.5-boot-safe.apk`, 25,817,418 bytes.
- SHA-256: `FCE0676EC85207C0B4760F9E98521EB9B4F8798FB513CF96E55B86C2A3FCBAED`.

The APK was installed on the tested phone and its device-side hash matched the local artifact. No commit, push, release tag, or published release was created during this validation.

## Retained evidence

Local captures are ignored by Git and remain under `build/boot-recovery/`:

- `baseline-after-removal/`: prior logs, selected DropBox tags, process/package inventory, recovered state, LSPosed database copies, and capture ledger.
- `baseline-2.0.5.apk` and `apk-MainHook.java`: pre-change artifact and its inspected startup code.
- `soft-reboot-1/`, `soft-reboot-2-sdk36/`, `soft-reboot-3-sdk37/`: runtime samples, module/scope JSON, logcat, LSPosed logs, window observations, and screenshots.
- `state-before-sdk-validation.json` and `state-applied-restore.json`: pre-trial and restored state; identifier contents are intentionally omitted from this report.

Initial REA process-list Evidence is `ev_a6e07571fbdb79dd8c22c6747b465cccb4ed552d3d0b3f4c702b38490291102a`. It records a live process inventory and has no artifact identity. APK static analysis used the installed JADX CLI because the REA headless JADX provider was unavailable; those decompiled observations and ADB captures are ordinary-tool evidence, not invented REA Evidence.

Final process-list Evidence is `ev_aea8e25f097ae10f1b43c6f901c0905e76bf860e28d5effe0185d19eb2185b66`; it records the live companion, scoped target, launcher, Zygote, system_server, and SurfaceFlinger after the trials. Both retained records are exported in `build/boot-recovery/rea-runtime-evidence.json` and retain the artifact-identity limitation.

Historical static findings remain in [Boot Hang Investigation](boot-hang-investigation.md).
