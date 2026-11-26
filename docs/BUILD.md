# Build Instructions

This guide walks you through building OpenBridge from source on Linux/macOS/Windows.

## Prerequisites

### 1. Android Studio (recommended)

Download from https://developer.android.com/studio — version Hedgehog (2023.1.1) or newer.

### 2. OR: Command-line tools only

If you prefer CLI:

- **JDK 17+**: `sudo apt install openjdk-17-jdk` (Linux) / `brew install openjdk@17` (macOS)
- **Android SDK Command-line Tools**: https://developer.android.com/tools
- **Gradle**: not required, we use the Gradle wrapper (`./gradlew`)

### 3. Verify installation

```bash
java -version  # should show 17 or newer
echo $ANDROID_HOME  # should point to your SDK
ls $ANDROID_HOME/platform-tools/adb  # should exist
```

## Clone and Build

```bash
# Clone
git clone https://github.com/<your-org>/openbridge.git
cd openbridge

# Build debug APK
./gradlew assembleDebug

# Output:
# app/build/outputs/apk/debug/app-debug.apk
```

## Build Variants

| Variant | Command | Purpose |
|---|---|---|
| Debug | `./gradlew assembleDebug` | Development, signed with debug key |
| Release (unsigned) | `./gradlew assembleRelease` | For signing with your own key |
| Release (signed) | `./gradlew assembleRelease -PsignRelease` | For distribution |

## Sign a Release APK

### 1. Generate a keystore (one-time)

```bash
keytool -genkey -v -keystore openbridge.jks \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -alias openbridge
```

**Keep this file safe. If you lose it, you can never update the app on users' devices.**

### 2. Configure signing

Create `~/.gradle/gradle.properties` (NOT in the repo):

```properties
OPENBRIDGE_KEYSTORE_PATH=/path/to/openbridge.jks
OPENBRIDGE_KEYSTORE_PASSWORD=your_keystore_password
OPENBRIDGE_KEY_ALIAS=openbridge
OPENBRIDGE_KEY_PASSWORD=your_key_password
```

### 3. Build signed release

```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

## Install on a device

```bash
# Enable USB debugging on your phone (Settings → Developer options)
# Connect via USB

./gradlew installDebug
# OR
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Run Tests

```bash
# Unit tests (fast, JVM)
./gradlew test

# Instrumented tests (requires emulator or device)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Code style
./gradlew ktlintCheck
./gradlew detekt
```

## Common Issues

### "SDK location not found"

Set `ANDROID_HOME`:

```bash
export ANDROID_HOME=/path/to/Android/Sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

### "Gradle build failed: OutOfMemoryError"

Increase JVM heap in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4g
```

### "ktlint failed"

Auto-fix:

```bash
./gradlew ktlintFormat
```

## Reproducible Build Verification

To verify that a release APK was built from a specific commit:

```bash
# 1. Get the official release SHA256
shasum -a 256 official-release.apk

# 2. Checkout the exact tag
git checkout v1.0.0

# 3. Build with reproducible settings
./gradlew assembleRelease -PreproducibleBuild

# 4. Compare SHA256
shasum -a 256 app/build/outputs/apk/release/app-release.apk
```

If both SHA256 hashes match, the APK was built from this source.

See [`docs/VERIFY.md`](VERIFY.md) for more details.
