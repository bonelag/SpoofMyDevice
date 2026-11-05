# SpoofMyDevice Project Memory

Last standardized: 2026-11-05 (Asia/Saigon)

## Recording Policy

- Keep this file in English.
- Record durable technical context: architecture, code changes, decisions, verification, artifacts, failures, and unresolved risks.
- Do not store greetings, routine chat, or command-by-command transcripts.
- Distinguish inspected evidence from newly executed verification.
- Distinguish static analysis from runtime/device evidence.
- Preserve user-owned working-tree changes. Do not assume ignored build artifacts are absent because Git does not list them.

## Project Snapshot

- Repository: `F:/Code/SpoofMyDevice`
- Branch: `main`, tracking `origin/main`
- Latest commit before the Miuix batch: `2d181f4` (moves configured apps to the top)
- `.codegraph/` is absent, so CodeGraph is not available unless the user later creates an index.
- Product: Android application and legacy Xposed/LSPosed module for per-application device-property spoofing.
- Application ID: `com.spoofmydevice`
- Namespace: `com.devicespooflab.hooks`
- Version: `2.0.4` (`versionCode` 13)
- SDK levels: `minSdk` 26, `targetSdk` 37, `compileSdk` 37
- UI stack: Kotlin, Jetpack Compose, Material 3, and Miuix.
- Hook stack: Java and the legacy Xposed API from the local `api-82.jar`.
- The Miuix implementation spans Gradle configuration, activities, settings, resources, documentation, and dedicated Miuix screen files.

## Architecture and Runtime Flow

### Application UI

- `MainActivity.kt` starts the Compose application.
- `SpoofMyDeviceApp.kt` selects the saved UI style and routes to either the existing Material UI or `MiuixApp`.
- Main destinations cover Home, Apps, Profiles, and Settings.
- Separate activities provide profile editing, safe-mode configuration, diagnostics, real-device information, and app-profile assignment.

### Xposed Module

- `app/src/main/assets/xposed_init` registers `MainHook`.
- `MainHook.java` installs hooks only for eligible application processes.
- Scope should contain ordinary applications only. Do not select Android System Framework or system applications.
- Hook groups cover `Build`, system properties, Java properties, locale, and process-based property reads such as `getprop`.

### Profile Storage and Resolution

- `AppProfileStore` persists state in `app_profiles.json`.
- An app-specific assignment takes precedence over the default profile.
- If neither assignment exists, hooks preserve real device values.
- `ConfigProvider` exposes complete profile state through `get_state`.
- `ConfigManager` performs transport and parsing work on a background worker and publishes an immutable runtime snapshot.
- Runtime transport can use XSharedPreferences, the state file, or the provider. Hook callbacks do not perform blocking configuration I/O.
- A `null` loader result means the source could not be read; `RuntimeConfigStore` keeps the last valid snapshot.
- A valid empty snapshot means assignments were intentionally removed and must clear prior spoofing state.
- State-file reads are capped at 4 MiB and continue to EOF within that limit.
- Profile changes trigger a coalesced reload through an `AtomicBoolean`. Receiver-registration failure remains retryable and does not prevent an initial configuration load.

## Boot-Safety Baseline

- Reported device: iQOO Z10 Turbo Plus.
- Runtime environment: temporary KernelSU root, soft reboot, and closed-source LSPosed 2.2.1.
- `HookSafetyPolicy` requires the real boot-complete state and stopped boot animation before enabling risky spoofing paths.
- Protected UIDs, packages, and runtime-state properties are excluded.
- `MainHook` ignores secondary `LoadedApk` callbacks when `isFirstApplication` is false.
- `Application.attach` validates that the received `Context` belongs to the expected package.
- Original `Build` values are captured before system-property hooks and spoofed only after other hook groups are ready.
- `BuildHooks` restores original values when no active profile exists. `getSerial()` and `getRadioVersion()` respect profile and toggle state.
- Configuration reads formerly capable of blocking startup now run on the configuration worker.
- SMD Boot Guard must remain installed until device testing proves repeated successful soft reboots with SpoofMyDevice enabled.

## Boot-Hang Fix Work, 2026-11-04 to 2026-11-05

### Configuration Reliability

- Added `ProfileConfigResolver.java` to resolve complete profile state and distinguish transport failure from valid unassignment.
- Changed `RuntimeConfigStore.reload()` to return success state and preserve the existing snapshot when its loader returns `null`.
- Added `AppProfileStore.readStateContent(Context)` for non-exported state reads.
- Added `ConfigProvider.METHOD_GET_STATE` and returned complete JSON state in a `Bundle`.
- Updated `ConfigManager` to read one preferences snapshot with `getAll()`, prefer complete JSON state when present, and remove superseded parser/resolver paths.
- Added `org.json:json:20240303` as a test-only dependency.

### Hook Initialization

- Updated `MainHook` with first-application filtering, protected-process handling, context validation, safe `Build` ordering, coalesced reloads, and retryable receiver registration.
- Retained the earlier worker and boot-policy safeguards.
- Removed unnecessary logging from protected-process rejection paths.

### Process and `getprop` Rewriting

- Changed `RewritingProcess` from whole-stream buffering to line-by-line stdout rewriting.
- Added `OutputRewriter`, EOF finalization, and one-time fixed-output behavior.
- Lines are buffered up to 64 KiB. Oversized lines pass through unchanged instead of growing without bound.
- Delegation to the real process remains intact for stdin, stderr, `waitFor`, `exitValue`, `isAlive`, `destroy`, and `destroyForcibly`.
- Added `GetPropOutputRewriter.java` to rewrite relevant `getprop` lines, preserve unrelated and boot-state properties, and append missing spoofed properties at EOF.
- `GetPropHooks` avoids double wrapping and uses conservative parsing. It does not rewrite complex, quoted, or escaped commands; `cat` requires exactly one file; unsupported `getprop` option/argument combinations pass through.

### Tests, Resources, and Documentation

- Added `ProfileConfigResolverTest.java` and `GetPropOutputRewriterTest.java`.
- Expanded `RewritingProcessTest.java` for streaming before EOF, oversized unterminated lines, one-time fixed output, and destruction of the real process.
- Retained tests for hook safety, runtime state, device profiles, and random generation.
- Added 36 missing strings to each of `values-ko`, `values-ja`, and `values-zh-rCN`, covering tabs and device-property fields.
- Updated `README.md` with boot-safety behavior, asynchronous profile-loading limits, supported scope, and Boot Guard testing guidance.
- Added `docs/boot-hang-investigation.md` with static evidence, hashes, fixed blocking paths, limitations, and a device-validation workflow.

### Intermediate Failures Resolved

- First unit-test compilation failed with 18 unhandled `JSONException` declarations in `ProfileConfigResolverTest`; test methods/helpers were corrected.
- First lint run failed with 36 `MissingTranslation` errors and 119 warnings; missing localized strings were added. Final boot-fix lint completed with 0 errors and 119 pre-existing warnings.

## Miuix UI Work, 2026-11-05

- Kept the existing Material interface and added a selectable MIUI-style interface.
- Added `top.yukonga.miuix.kmp:miuix-ui:0.9.4`.
- Added `UI_STYLE_MATERIAL`, `UI_STYLE_MIUIX`, `KEY_UI_STYLE`, `getUiStyle()`, `setUiStyle()`, and `normalizeUiStyle()` to `AppSettingsStore.java`.
- New installations/default settings use Miuix. Users can switch both directions between Miuix and Material.
- Added `uiStyle` state and `updateUiStyle()` to `SpoofAppState.kt`.
- Updated `SpoofMyDeviceTheme.kt` to use `MiuixTheme` with a Monet light/dark `ThemeController`, the selected seed color, `Spec2025`, and `TonalSpot`; Material retains `MaterialExpressiveTheme`.
- Updated `MainActivity.kt` to pass the selected UI style.
- Made `AppDestination` internal and routed Miuix selections to `MiuixApp(appState)`.
- Added `MiuixApp.kt` with top app bar, bottom navigation, wide-screen navigation rail, snackbar host, and Apps menu.
- Added `MiuixScreens.kt` with shared Miuix rows, cards, dialogs, section titles, Home/Apps/Profiles/Settings screens, search, and pull-to-refresh.
- Added `MiuixSubScreens.kt` with Miuix versions of profile-editor loading/editing, safe mode, diagnostics, app information, and app-profile assignment.
- Changed reusable editor fields and field lists in `ProfileEditorScreen.kt` to internal visibility.
- Updated `ProfileEditorActivity`, `SafeModeAppsActivity`, `DiagnosticsActivity`, `RealInfoActivity`, and `AppProfileAssignmentActivity` to select UI from persisted settings.
- Added an interface-style selector to both Material and Miuix Settings screens.
- Added UI-style strings to default, Simplified Chinese, Korean, and Japanese resources.
- Corrected the `ToggleableState` import to `androidx.compose.ui.state.ToggleableState`.
- Removed unintended UTF-8 byte-order marks from edited source files.
- Version remains `2.0.4`; no version bump was made for this work.

## Verification Record

### Boot-Fix Baseline

- Command set: `gradlew.bat --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` with `JAVA_HOME=F:/Reserve_Engineer/jdk-21`.
- Unit tests: 22 total, 0 failures, 0 errors.
  - `DeviceProfileTest`: 1
  - `GetPropOutputRewriterTest`: 2
  - `HookSafetyPolicyTest`: 4
  - `ProfileConfigResolverTest`: 5
  - `RandomGeneratorTest`: 1
  - `RewritingProcessTest`: 6
  - `RuntimeConfigStoreTest`: 3
- `assembleDebug`: passed.
- `lintDebug`: passed with 0 errors and 119 warnings.
- `git diff --check`: passed after boot-fix work.

### Initial Miuix Implementation

- `:app:compileDebugKotlin`: passed.
- `:app:testDebugUnitTest`: passed.
- Existing XML reports inspected during this memory rewrite contain 22 tests, 0 failures, 0 errors, and 0 skipped tests across seven suites.
- Tests were not rerun during the memory rewrite.
- At this initial stage, full `assembleDebug`, `lintDebug`, APK signing, and device testing had not been rerun. The later visual-redesign record supersedes this build status.

## Boot-Fix APK Artifact

- Path: `F:/Code/SpoofMyDevice/release/SpoofMyDevice-2.0.4-bootfix.apk`
- Checksum file: `release/SpoofMyDevice-2.0.4-bootfix.apk.sha256`
- Size: 26,236,882 bytes
- SHA-256: `92DE78E2786EA73A0B4B96F94B0297B9FDB10B9852329B8F06776BF22FD79A0B`
- Package: `com.spoofmydevice`
- Version: `2.0.4` (`versionCode` 13)
- SDK metadata: `minSdk` 26, `targetSdk` 37
- Signature: APK Signature Scheme v2, one Android Debug signer, RSA 2048-bit
- Certificate SHA-256: `6fd52f5e1da33a135198423d0c8582c787d53ede6da499a3c4841f2b91f216cf`
- Source APK and release copy hashes matched when the artifact was created.
- `release/` and build outputs are ignored by Git; absence from `git status` does not mean the files are missing.

## Static LSPosed Evidence

- `LSPosed-v2.2.1.zip` SHA-256: `ED37DE2D2097AA6BA871377E7BF4AC719B708484F9468928E6ABAE10AB311E74`
- `framework.dex` SHA-256: `73CCEB77D02950CF7F224900F23A896506A0164AB7008E1430E502FF985D0140`
- Decompiled `defpackage/f1.java` shows callbacks originating from `LoadedApk` and derives `isFirstApplication` from `ActivityThread`.
- `XposedInit` invokes `initZygote` only for `IXposedHookZygoteInit`; SpoofMyDevice implements only `IXposedHookLoadPackage`.
- `XSharedPreferences` construction performs synchronous initialization/read work, and getters can wait for loading. SpoofMyDevice now invokes those paths only on its configuration worker.
- Stored REA attempts did not produce successful Evidence IDs: ZIP compression was unsupported, `REA_JADX_MCP_JAR`/`jadx-headless-mcp` was unavailable, and the `pwntools-elf` provider targeted Linux x64 rather than the Windows host.
- These findings are static observations from hashes and existing decompiled code, not runtime traces.

## Known Limitations and Required Device Validation

- No ADB connection, installation, reboot, or live device trace was available during boot-fix work.
- No evidence proves a Zygote crash or establishes the final root cause of the iQOO black-screen/boot-hang behavior.
- Native and ROM-specific paths have not been fully inspected.
- Asynchronous loading can allow a cold-started app to read/cache real values before the spoof profile becomes available.
- A process started during boot may retain real values until it is force-stopped and relaunched after boot.
- Keep SMD Boot Guard installed. Test several soft reboots with SpoofMyDevice enabled, verify the display and scoped applications, and only then consider removing Boot Guard.
- The existing APK is debug-signed and predates current uncommitted Miuix work.

## Memory Standardization, 2026-11-05

- Rewrote `MEMORY.md` entirely in English.
- Consolidated repeated Vietnamese and transliterated-Vietnamese entries into stable technical sections.
- Removed greetings and routine command transcripts while preserving code changes, logic, verification, artifacts, failures, and unresolved risks.
- Rechecked repository status, build configuration, test-file inventory, and existing unit-test reports.
- Corrected the latest test summary to 22 tests across seven suites; the previous statement of eight tests was inaccurate.
- Post-rewrite validation found no non-ASCII text, and `git diff --check -- MEMORY.md` passed.
- Did not modify application code, rerun Gradle tasks, connect a device, commit, or push during this rewrite.

## README Rewrite, 2026-11-05

- Replaced `README.md` with a professional, source-backed technical document.
- Verified claims against Gradle metadata, `AndroidManifest.xml`, Xposed registration, hook classes, profile storage, preset loading, CI configuration, permissions, Boot Guard scripts, current unit-test reports, and project memory.
- Added project badges, scope warnings, capability groups, a Mermaid runtime architecture diagram, profile-resolution and compatibility tables, rooted and non-root installation workflows, Safe Mode behavior, boot-safety details, preset schema, permission rationale, build/test commands, verification status, repository layout, limitations, license terms, and acknowledgements.
- Documented that the current Boot Guard disables SpoofMyDevice during `post-fs-data` and requires manual re-enabling because `service.sh` intentionally exits without enabling the module.
- Documented that direct network access is limited to preset retrieval from the configured GitHub source; no telemetry backend is configured in the inspected source.
- Excluded existing screenshots because they show an outdated version and navigation structure.
- Validated all local README links and ran `git diff --check -- README.md` successfully.
- Did not change application code, run Gradle tasks, connect a device, commit, or push during the README rewrite.

## Miuix Visual Redesign, 2026-11-05

- Redesigned the Miuix interface after the user reported that it still looked too similar to Material and supplied LSPosed Miuix screenshots as the visual reference.
- Kept the Material interface unchanged. The work is code-native Compose UI; no generated image assets were used.
- Reviewed the current Miuix screens and the upstream Miuix 0.9.4 component source before editing.
- Updated `MiuixApp.kt` so the root scaffold, large top app bar, and bottom navigation use the Miuix surface palette; removed the navigation divider and applied distinct selected/unselected navigation colors.
- Reworked shared Miuix components in `MiuixScreens.kt`: 20 dp grouped cards, MIUI-style muted section labels, official `BasicComponent` typography/layout, squircle icon containers, larger app icons, subtle dividers, trailing arrows, and compact status pills.
- Replaced the Home status row with a 176 dp rounded hero card using deep green/red dark-mode surfaces, stronger hierarchy, active-profile information, and an oversized clipped status icon modeled after the supplied screenshots.
- Reworked the Apps screen with a capsule `InputField`, horizontally scrollable All/Configured/Unassigned filter pills, 20 dp app cards, 52 dp rounded icons, stronger titles, package summaries, and assignment badges.
- Reworked profile cards with larger padding, squircle profile icons, stronger titles, assignment metadata, and a highlighted default-profile pill.
- Reworked Miuix Settings from a Material-like theme button grid into grouped preference cards. Theme, system colors, color style, interface style, language, runtime, preset, and tool controls now follow MIUI list hierarchy and omit unnecessary leading icons.
- Added a theme-selection dialog to the Miuix Settings flow.
- Applied the larger shared card style to Miuix profile editing, preset selection, Safe Mode, diagnostics, App Info, and app-profile assignment subscreens.
- Added localized app-filter strings in English, Simplified Chinese, Korean, and Japanese.
- Updated the README verification table to reflect the successful full build and current lint result.
- Verification: `:app:compileDebugKotlin` passed; `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:lintDebug` completed successfully.
- Unit tests: 22 passed, 0 failures, 0 errors. Lint: 0 errors and 118 warnings.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`, 29,381,508 bytes, SHA-256 `EC1BDF9BDDA3154A9DD3A0FC96E99D4485AFBE60CF761F5C352304784721E7F6`.
- No emulator/device screenshot or live visual validation was available. Final spacing and density should be checked on the target phone.

## Commit and Push, 2026-11-05

- The user authorized committing and pushing the complete Miuix implementation, visual redesign, activity routing, settings, translations, README rewrite, project instructions, and memory updates.
- Target branch: `main`; target remote: `origin` (`https://github.com/bonelag/SpoofMyDevice`).
- Commit message: `Redesign Miuix interface and documentation`.
- Excluded `LSPosed-v2.2.1.zip` from the commit because it is a local reverse-engineering input, not application source or a distributable project artifact.
- Pre-commit verification remained green: 22 unit tests, `assembleDebug`, and `lintDebug` with 0 errors and 118 warnings.
- No release tag or GitHub Release was requested.

## Source Orientation, 2026-11-05

- Reviewed repository instructions and memory, README, Gradle configuration, Android manifest, Xposed registration, application entry points, shared UI state, profile storage/resolution, representative hook implementations, Miuix components and routing, preset loading, Boot Guard source/scripts, CI, investigation notes, and existing test/lint reports.
- Current baseline: `main` at `a623620` (`Redesign Miuix interface and documentation`). No `.codegraph/` index exists; source inspection used direct reads and ripgrep without creating an index.
- Source inventory: 55 production Java/Kotlin files, seven JVM test source files, and nine JSON device presets. App metadata remains `com.spoofmydevice`, version `2.0.4` / code 13, minimum API 26, target/compile API 37.
- UI flow: `MainActivity` creates `SpoofAppState`; `SpoofMyDeviceApp` selects Material or default Miuix. Both interfaces share profile/settings operations and four destinations: Home, Profiles, Apps, and Settings. Separate activities handle editing, assignment, Safe Mode, diagnostics, and device information.
- Profile state format version 1 requires at least one saved profile and contains `profiles`, `assignments`, and `globalProperties`. `ConfigFileManager` renders property text with preset/mode metadata. `AppProfileStore` migrates legacy configuration, exports complete state to shared preferences and a best-effort temporary file, and broadcasts configuration changes.
- Runtime flow: `MainHook` filters eligible first-application processes after real boot readiness, prefetches configuration on a single background executor, validates `Application.attach`, and applies captured/restorable Build values on the main handler. Transport precedence is XSharedPreferences, direct file, then provider; hooks read immutable snapshots. Failed reads preserve prior state, while valid unassignment clears spoofing.
- Per-app assignments override the optional default profile. Safe Mode preserves Android version values for selected packages. Apps without an active assignment/default retain real values; asynchronous startup still permits early reads of real values.
- Shell handling rewrites supported `getprop`, `uname`, and single-file CPU information reads. Complex shell commands pass through. `RewritingProcess` bounds line buffering to 64 KiB and delegates child-process lifecycle, stdin, and stderr.
- Presets use the configured GitHub source (default `https://github.com/bonelag/SpoofMyDevice`), local `remote_device_presets.json` cache, a current-device preset, and cache fallback when refresh fails. HTTP connect/read timeouts are 8/12 seconds.
- Boot Guard helper source updates LSPosed's SQLite module state and attempts daemon notification over its Unix socket. `post-fs-data.sh` disables SpoofMyDevice; `service.sh` exits immediately and requires manual re-enabling. `module.prop` still describes automatic re-enabling, unlike the current script and README.
- Existing report inspection confirmed 22 tests across seven suites, zero failures/errors/skips, and lint with zero errors and 118 warnings. No Gradle build/test task, APK installation, device connection, or reboot was executed in this orientation pass; live LSPosed and reported iQOO soft-reboot behavior remain unverified.
- Only this memory report was added. Application source and the pre-existing untracked `LSPosed-v2.2.1.zip` were left untouched. Documentation validation: `git diff --check -- MEMORY.md`.

## Boot-Hang Historical Clarification, 2026-11-05

- Compared the source immediately before `adcaf5a` (`Fix boot`) with that commit and the current safety/configuration/process code, using repository history and `docs/boot-hang-investigation.md`. This was source analysis, without new device or binary-runtime evidence.
- Confirmed that old `ConfigManager.init()` called synchronous reload, synchronized getters could invoke `ensureFreshConfig()`, and `MainHook` reloaded directly from `Application.attach` and broadcast callbacks. The old ordered-broadcast transport could wait on a latch for 250 ms in addition to preference, file, and provider work.
- Confirmed that the old boot gate cached `bootCompleted=true`, accepted any non-empty value except `0`, and did not check boot-animation state. It also installed the attach callback while boot was incomplete. The replacement reads original properties afresh and requires exact `sys.boot_completed=1` plus `init.svc.bootanim=stopped` before target-process initialization.
- Confirmed that old `GetPropHooks.buildReplacementProcess()` drained stdout to EOF, then stderr, then waited for process exit before returning a replacement. This could block the caller and can deadlock when a child fills stderr while stdout remains open; this mechanism was not observed on the reported device.
- The fix moved configuration transport off startup/hook callbacks, published immutable snapshots, streamed subprocess stdout with a 64 KiB line bound while preserving process lifecycle, strengthened process/package filtering, and corrected Build capture/application ordering. Boot Guard separately disables the module during boot as a recovery workaround.
- The pre-fix `MainHook` comment claimed a specific 55-second watchdog/SurfaceFlinger/MediaTek failure chain. No matching device trace is recorded, so that comment must not be presented as a confirmed root cause. Existing evidence does not establish a Zygote crash or the final cause of the iQOO boot hang.
- Historical JVM/build success supports the source fixes, not successful soft reboot on the target phone. No tests, Gradle tasks, device operations, application-source edits, commits, or pushes were performed for this clarification. Only this memory report was appended and checked with `git diff --check -- MEMORY.md`.

## Miuix Profile Editor Readability and Vietnamese, 2026-11-05

- Inspected the Miuix editor, shared editor field definitions, language persistence/theme configuration, all string resources, manifest, and the published Miuix 0.9.4 Kotlin component sources. No CodeGraph index exists; no index was created.
- Found that editor labels were embedded in narrow text fields sharing a row with checkboxes and randomization buttons. Ordinary fields were capped at one line, longer fields at five lines, and Miuix tabs used one-line ellipsis with widths unrelated to their labels.
- Reworked `MiuixEditorForm` into one `LazyColumn` containing profile details, tabs, optional identifier actions, and fields. The whole form now scrolls, consumes scaffold insets, and applies IME padding so fixed header content does not crowd out fields when the keyboard opens.
- Added shared `MiuixEditorFieldCard`: labels wrap above full-width inputs; checkboxes and randomization controls stay in the label row. Inputs wrap and grow without a line-count cap, retain numeric keyboard selection, use the Done IME action, and expose their labels through accessibility semantics. Existing field values, toggles, and randomization logic remain in `ProfileEditorState`.
- Moved the profile-name label above its full-width input. Tab width and height now account for measured localized text, bold selection, and font scale; the official Miuix tab row retains horizontal scrolling. Identifier action buttons use `FlowRow` instead of fixed half-width buttons.
- Added `LANGUAGE_VIETNAMESE = "vi"` and normalization in `AppSettingsStore`; registered Vietnamese in both Material and Miuix language selectors.
- Added `values-vi/strings.xml` with all 269 translatable app strings, including editor fields, Settings, Home, profile assignment/default controls, dialogs, diagnostics, Safe Mode, and app information. Product names, API identifiers, URLs, and format placeholders were preserved.
- Removed erroneous `translatable="false"` flags from eight default-profile UI strings and supplied their Korean, Japanese, and Simplified Chinese translations. Added the native language name `Tiếng Việt` to every locale's language picker resources. The existing non-translatable Xposed metadata description remains separate from translated app UI.
- Added five supported languages (`en`, `vi`, `ko`, `ja`, `zh-CN`) to `res/xml/locales_config.xml` and linked it from the manifest for Android 13+ app-language settings. Added `tools:targetApi="33"` to document the API-gated manifest attribute; app minimum SDK remains 26.
- Updated README localization coverage. App version remains `2.0.4` / code 13. No Xposed hook, runtime configuration, Boot Guard, or profile-storage logic was changed.
- Resource validation parsed every locale, checked duplicate/missing/extra keys and format-placeholder multisets, and confirmed 269 matching strings per locale with zero discrepancies. Newly edited Kotlin and new XML resources were checked for UTF-8 BOMs; none were present.
- Verification with `JAVA_HOME=F:/Reserve_Engineer/jdk-21`: `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:lintDebug` passed. All 22 tests across seven suites passed with zero failures/errors/skips. The first lint pass reported one new `UnusedAttribute` warning for `localeConfig`; after the tools API annotation, repeated assemble/lint passed with zero errors and the original 118 warnings. No new test source files were added.
- Debug APK signature verification passed with APK Signature Scheme v2 and the existing Android Debug certificate SHA-256 `6fd52f5e1da33a135198423d0c8582c787d53ede6da499a3c4841f2b91f216cf`. AAPT inspection confirmed package/version metadata, the Vietnamese application label, and all five packaged locale-config entries.
- Review artifact: `release/SpoofMyDevice-2.0.4-miuix-vi.apk`, 29,381,580 bytes, SHA-256 `D1A0E8E09696FFD70A6647C644A0A2DF68EBA8F025A31CB17A533EC91DE6D0F4`; adjacent `.sha256` file records the checksum.
- ADB detected a connected device reporting model `V2507A`, API 35, 1080x2400 display, and font scale 1.0. Existing internal-file inspection through `run-as` failed with `readable or writable by others: 40755`; the pre-installed provider also could not be found by the `content` CLI. A readable shared temporary export contained five profiles and two assignments; only counts and a canonical JSON hash were recorded in `build/qa/miuix-editor-vi/device-baseline.json`.
- Launched the existing companion activity once and prepared an ignored local UI-inspection helper at `build/qa/miuix-editor-vi/device_ui.py`. Capture was stopped by its foreground-app guard because another app was active. No screenshot was captured, new APK installed, device setting/profile modified, target app restarted, or reboot performed. Live visual confirmation remains outstanding; ADB connectivity does not validate the earlier boot-hang fix.
- Preserved earlier memory changes and the untracked `LSPosed-v2.2.1.zip`. No commit, push, tag, or published release was made. Final documentation/source validation used `git diff --check`.
