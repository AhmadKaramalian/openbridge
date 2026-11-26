# Contributing to OpenBridge

Thanks for your interest in contributing! 🎉

## Code of Conduct

Be respectful, inclusive, and constructive. We're building something for everyone.

## Getting Started

1. **Fork** the repository
2. **Clone** your fork: `git clone https://github.com/<your-username>/openbridge.git`
3. **Create a branch**: `git checkout -b feature/your-feature-name`
4. **Make changes** following the code style below
5. **Test** your changes: `./gradlew test`
6. **Commit** with a clear message (see format below)
7. **Push** and open a Pull Request

## Code Style

- **Language**: Kotlin 2.0+
- **Style**: Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Formatter**: ktlint (run `./gradlew ktlintFormat` before committing)
- **Lint**: detekt (run `./gradlew detekt` before committing)
- **Tests**: Required for new functionality. Aim for ≥80% coverage on new code.
- **Comments**: Use KDoc for public APIs. Explain *why*, not *what*.

## Commit Message Format

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`, `security`

**Example**:
```
feat(ble): add GATT server characteristic for SMS notifications

Adds a new BLE characteristic (UUID 8f3c1a01-...) that notifies the
paired iPhone when a new SMS is received on the Android phone.

Closes #42
```

## Pull Request Process

1. **Small PRs preferred** — under 500 lines if possible
2. **One feature/fix per PR** — don't mix unrelated changes
3. **Update documentation** if your change affects public API or user-facing behavior
4. **Add tests** for new functionality
5. **CI must pass** — all checks green before merge
6. **Squash and merge** — we squash PRs into single commits on `main`

## Project Structure

```
openbridge/
├── app/                  # Android app module
│   └── src/main/java/com/openbridge/android/
│       ├── ui/           # Compose UI
│       ├── bridge/       # BridgeService (foreground service)
│       ├── ble/          # BLE GATT server
│       ├── telephony/    # SMS/Contacts/CallLog bridge
│       ├── call/         # InCallService
│       ├── security/     # Crypto, storage, SSL pinning
│       └── util/         # Utilities
├── docs/                 # Documentation
└── scripts/              # Build/release scripts
```

## Testing

- **Unit tests**: `./gradlew test` — JVM tests, fast
- **Instrumented tests**: `./gradlew connectedAndroidTest` — requires emulator/device
- **Manual testing**: see `docs/TESTING.md` (to be created)

## Security-sensitive changes

If your PR affects security (crypto, permissions, network, BLE protocol):

1. **Mention it explicitly** in the PR description
2. **Update `docs/SECURITY.md`** if threat model changes
3. **Add tests** for the security property
4. **Expect longer review** — security changes get extra scrutiny

## Release Process

Releases are handled by maintainers. The process:

1. Update `CHANGELOG.md`
2. Tag: `git tag v1.0.0`
3. Push tag: `git push origin v1.0.0`
4. CI builds and publishes APK + signature + SHA256 to GitHub Releases

## License

By contributing, you agree that your contributions will be licensed under the GPLv3 license.

## Questions?

Open an issue with the `question` label, or reach out on our communication channels (see README).
