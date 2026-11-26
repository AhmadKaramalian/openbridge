# Verifying a Release

OpenBridge publishes reproducible builds. This means you can verify that the APK you install was built from the published source code, with no modifications.

## What you need

- The APK file you downloaded (e.g., from GitHub Releases)
- The SHA256 hash published alongside the release
- The GPG signature published alongside the release
- The project's GPG public key (from `SECURITY.md`)
- A Linux/macOS environment with `sha256sum`, `gpg`, and `git`

## Step 1: Verify the SHA256 hash

```bash
# Calculate the hash of your APK
sha256sum openbridge-v1.0.0.apk
# Output: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855  openbridge-v1.0.0.apk

# Compare with the published hash
echo "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855  openbridge-v1.0.0.apk" | sha256sum -c -
# Output: openbridge-v1.0.0.apk: OK
```

If the hashes match, the APK file hasn't been corrupted or modified in transit.

## Step 2: Verify the GPG signature

```bash
# Import the project's public key
gpg --import SECURITY_PGP_KEY

# Verify the signature
gpg --verify openbridge-v1.0.0.apk.sig openbridge-v1.0.0.apk
# Output: Good signature from "OpenBridge <security@openbridge.example.org>"
```

If verification succeeds, the APK was published by the project maintainers.

## Step 3: Reproduce the build (most thorough)

This step is optional but provides the strongest guarantee: you build the APK from source and verify it's byte-for-byte identical to the published one.

```bash
# 1. Clone the repository
git clone https://github.com/<your-org>/openbridge.git
cd openbridge

# 2. Checkout the exact release tag
git checkout v1.0.0
git log -1  # verify the commit hash matches the release notes

# 3. Set up build environment
# See docs/BUILD.md for prerequisites

# 4. Build with reproducible mode
./gradlew assembleRelease -PreproducibleBuild

# 5. Compare
sha256sum app/build/outputs/apk/release/app-release.apk
sha256sum /path/to/downloaded/openbridge-v1.0.0.apk

# If both hashes match, the published APK was built from this exact source.
```

## Why reproduce builds?

Without reproducible builds, you have to trust that:

- The published source code matches what was built
- The build environment didn't inject anything malicious
- The signing key wasn't used to sign a different (malicious) APK

With reproducible builds, anyone can verify that the APK they install was built from the public source code, eliminating this trust.

## Reproducibility details

OpenBridge builds are reproducible because:

1. **No timestamps in APK**: build timestamps are fixed to the commit time
2. **No build-specific IDs**: no random UUIDs, no machine-specific paths
3. **Sorted file ordering**: ZIP entries are sorted deterministically
4. **Fixed tool versions**: Gradle, Kotlin, AGP versions are pinned in `gradle/libs.versions.toml`
5. **Reproducible native builds** (if applicable): NDK version pinned, no PGO

## Reporting a mismatch

If your reproduced build produces a different SHA256 than the published one:

1. **Don't install the published APK**
2. Open a security issue (see `SECURITY.md`)
3. Include:
   - The published APK SHA256
   - Your reproduced APK SHA256
   - The commit hash you built from
   - Your OS, JDK version, Android SDK version

We will investigate and either fix the reproducibility issue or revoke the release.
