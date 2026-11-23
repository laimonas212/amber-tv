# Amber TV

A custom, ad-free Android TV home screen for Philips Google TV (TA1 / PH1M_WW_9972).

## Project direction

- Native Android app: Kotlin + Jetpack Compose for TV.
- Designed for 10-foot viewing and D-pad / remote navigation.
- Home screen inspired by the calm, cinematic feel of tvOS and digital-frame interfaces; no copied CleanFrame or unlicensed launcher assets.
- Installed-app discovery through PackageManager, with real app icons and D-pad launch actions.
- Default HOME role is tested only after the APK works as a normal app.
- Philips `org.droidtv.homeintentresolver` behavior is treated as device-specific and must be validated before changing system components.

## Target device

Diagnostics supplied by the owner:

| Property | Value |
|---|---|
| Manufacturer | TPV |
| Model | Philips Google TV TA1 |
| Device / board | PH1M_WW_9972 |
| Android | 14 |
| SDK | 34 |
| Build | UKN2.241117.001 / 3000669 |
| Google launcher | `com.google.android.apps.tv.launcherx` |
| Philips HOME resolver | `org.droidtv.homeintentresolver` |

## Build

Requirements: JDK 17 and Android SDK Platform 35 (Android 14 TV device is supported; the app targets API 34). The included Gradle Wrapper downloads Gradle 8.9 automatically.

```bash
./gradlew :app:assembleDebug
```

Open the project in Android Studio and let it sync the Gradle project.

Install without changing the default launcher:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open Amber TV normally first, then verify app discovery and remote navigation. Do not disable or uninstall Google TV / Philips system packages during initial testing.

## Release build (recommended for daily use)

The debug APK is unminified and debuggable, so it starts much slower on the TV. For daily use build the release variant (R8 shrinking, non-debuggable, signed with the debug key so it installs without a keystore):

```bash
./gradlew :app:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

GitHub Actions builds both `amber-tv-debug` and `amber-tv-release` artifacts. Note: Android only allows an in-place update when the signing key matches the installed app; if `adb install -r` reports a signature mismatch, uninstall the old build first.

## HOME testing and rollback

Only after the app has been tested as a normal app:

```bash
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
adb shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME
```

If Android presents the HOME app chooser, select Amber TV and choose Always. If the Philips resolver intercepts HOME, collect diagnostics before attempting any workaround:

```bash
adb shell dumpsys package org.droidtv.homeintentresolver
adb shell dumpsys package dev.amber.tv
adb shell cmd role holders android.app.role.HOME
```

To restore Google TV as the default HOME app (where supported):

```bash
adb shell cmd package set-home-activity com.google.android.apps.tv.launcherx/.home.HomeActivity
```

The exact HOME routing on Philips firmware may differ. No system package is disabled by this project or its installer.

## Amber Ambient screensaver

Amber Ambient is registered as an Android `DreamService` and can be selected in the TV's Android screensaver settings.

- Settings are stored in the active Amber TV profile.
- Clock and date visibility, 12/24-hour time, optional Vilnius weather, image rotation interval, clock position and dimming are configurable.
- Backgrounds reuse the cached remote wallpaper catalog, so the screensaver can start with the last successful catalog while offline.
- Open Amber TV → Settings → Amber Ambient → Android screensaver settings, then select Amber Ambient and choose the system idle timeout.

The Philips TV's own screensaver policy and power behavior still need validation on the TA1 device. The app cannot silently change Android's protected screensaver selection or system idle timeout.

## Status

Milestone 1: first launcher prototype with a cinematic home screen, installed TV-app discovery, actual app icons, remote focus, and app launching. Next: run the APK on the Philips TA1, refine focus/layout from the real screen, then validate HOME-key routing and rollback behavior.
