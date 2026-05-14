# REDRIVER2 Android

This module builds the Android v1 port for `arm64-v8a`.

## Build

```powershell
cd C:\Proyectos\redriver2\android
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

The debug APK is written to:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## Runtime data

The APK does not package original game data. On first launch, select either the
`DRIVER2` folder or a folder containing `DRIVER2`. The launcher copies it into
app-private storage and writes `config.ini` for the native game.

## Controls

No touch controls are implemented in v1. Use an Android-recognized gamepad; SDL2
GameController input is passed through to PsyCross.

## Native stack

The app builds REDRIVER2, PsyCross, SDL2, and OpenAL Soft with CMake through the
Android Gradle plugin. The initial plan called for `ndk-build`, but OpenAL Soft's
maintained Android path is CMake, so the app uses one native build system for all
native libraries.
