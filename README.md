# Amber TV

A custom, ad-free Android TV home screen for Philips Google TV (TA1 / PH1M_WW_9972).

## Project direction

- Native Android app: Kotlin + Jetpack Compose for TV.
- Designed for 10-foot viewing and D-pad / remote navigation.
- Home screen inspired by the calm, cinematic feel of tvOS and digital-frame interfaces; no copied CleanFrame or unlicensed launcher assets.
- Installed-app discovery through PackageManager.
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

## Status

Early scaffold. The first milestone is a stable, attractive launcher screen and correct remote focus behavior. Philips HOME takeover is a separate device-validation milestone.
