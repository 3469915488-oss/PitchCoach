# Testing Environment

PitchCoach needs JDK 17 and the Android SDK before Gradle tests and Android builds can run.

## Downloads

- Android Studio: https://developer.android.com/studio
- Android command line tools: https://developer.android.com/studio#command-tools
- Eclipse Temurin JDK 17: https://adoptium.net/temurin/releases/?version=17
- Gradle releases: https://gradle.org/releases/

On Apple Silicon Macs, choose macOS aarch64/ARM64 builds. On Intel Macs, choose macOS x64 builds.

## Recommended Setup

- JDK: 17
- Android Gradle Plugin: 9.1.0
- Compile SDK: 36
- Build tools: 36.1.0
- Gradle: use the wrapper checked into this repository

After installing Android Studio, open it once and install:

- Android SDK Platform 36
- Android SDK Build-Tools 36.1.0
- Android SDK Platform-Tools

If you use Android Studio's bundled JDK on macOS:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

If you install Temurin JDK 17 separately, point `JAVA_HOME` to that JDK instead.

## Run Tests and Builds

Run the JVM test suite:

```sh
./gradlew :app:testDebugUnitTest
```

Build the debug APK:

```sh
./gradlew :app:assembleDebug
```

Run both:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon
```

Android Studio can also open this folder directly and sync the project. The Room/Robolectric repository test is pinned to SDK 35 because Robolectric's Android 36 sandbox requires Java 21, while this project is currently verified on JDK 17.
