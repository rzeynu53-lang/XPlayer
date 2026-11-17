# X Player (Android)

X Player (`com.xplayer.media`) is a 100% offline, privacy-first local audio and video player for Android built with **Kotlin**, **Jetpack Compose (Material 3)**, **AndroidX Media3 (ExoPlayer + MediaSessionService)**, **Room**, and **DataStore**.

## Key Features
- **Zero Network & Zero Tracking:** Does not request `android.permission.INTERNET` or `AD_ID`. No ads, analytics, or telemetry.
- **Granular Media Permission Flows:** Full support for Android 14+ (`READ_MEDIA_VISUAL_USER_SELECTED`), Android 13 (`READ_MEDIA_AUDIO`, `READ_MEDIA_VIDEO`), and Android 12L and below (`READ_EXTERNAL_STORAGE`), plus zero-permission single-file playback via the system document picker (`Intent.ACTION_OPEN_DOCUMENT`) and external file intents (`Intent.ACTION_VIEW`).
- **Background Audio & MediaSession:** Foreground `MediaSessionService` with lock-screen/notification transport controls, audio focus management, and headset unplug (`AudioBecomingNoisy`) auto-pause.
- **Full-Featured Video Player:** ExoPlayer surface with Picture-in-Picture (PiP), double-tap 10s seek gestures, screen orientation toggle, control lock, aspect ratio switching (Fit / Zoom / Fill), playback speed (`0.5x`–`2.0x`), and automatic resume from last watched timestamp.
- **Resilient Error & Metadata Handling:** Gracefully sanitizes missing or `<unknown>` MediaStore tags and automatically skips corrupted or unsupported media files without crashing.
- **Playlists, Favorites & Folders:** Persistent local playlists and favorites backed by Room, plus automatic folder grouping, search, and multi-criteria sorting.

## Requirements
- **Android Studio:** Ladybug (2024.2.1) or newer
- **JDK:** Java 17
- **Android SDK:** `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`
- **Gradle / AGP:** Gradle 8.10.2 / Android Gradle Plugin 8.7.3

## Build Instructions

1. **Unzip the project:**
   ```bash
   unzip XPlayer-Android-Studio-Project.zip
   cd XPlayer
   ```

2. **Configure Android SDK:**
   Ensure `ANDROID_HOME` is set in your environment, or create a `local.properties` file in the root directory:
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   ```

3. **Build the Debug APK:**
   ```bash
   gradle :app:assembleDebug
   ```
   *The ZIP supplied with this project contains an incomplete `gradle-wrapper.jar`. Until that file is replaced by a genuine Gradle wrapper JAR, `./gradlew` will not run. The included GitHub Actions workflow builds with a directly installed Gradle 8.10.2 and does not depend on the broken wrapper.*

4. **Locate the Output Debug APK:**
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

5. **Run Unit & Instrumented Tests:**
   ```bash
   # Run local JVM unit tests (JUnit4 + Robolectric)
   ./gradlew testDebugUnitTest

   # Run instrumented UI & playback lifecycle tests on a connected device/emulator
   ./gradlew connectedDebugAndroidTest
   ```


## Build APK with GitHub Actions

1. Upload this project to a GitHub repository (keep the `.github/workflows/android-build.yml` file).
2. Open the repository's **Actions** tab.
3. Select **Build X Player Android APK** and tap **Run workflow** (or push to `main`/`master`).
4. Wait for the build and unit-test jobs to finish.
5. Download the `XPlayer-debug-apk` artifact from the successful workflow run.

A successful workflow is required before treating the APK as verified.
