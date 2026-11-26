# OpenBridge Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial project skeleton
- `BridgeService` (foreground service)
- `BleServer` (GATT server with new OpenBridge UUIDs)
- `TelephonyBridge` (SMS, Contacts, CallLog read access)
- `SmsReceiver` (incoming SMS broadcast receiver)
- `OpenBridgeInCallService` (optional InCallService)
- `SecureStorage` (EncryptedSharedPreferences wrapper)
- `CryptoUtil` (Android Keystore ECDH/ECDSA helpers)
- `AuditLog` (opt-in, metadata-only audit log)
- Compose UI scaffold (`MainActivity`, `OpenBridgeTheme`)
- Persian (fa) and English (en) string resources
- Documentation: README, ARCHITECTURE, PROTOCOL, SECURITY, PRIVACY, BUILD, VERIFY, ROADMAP
- GitHub Actions CI: build, test, release, security scan

### Security
- Minimal permission set (no INTERNET, no WRITE_CONTACTS, no WRITE_SECURE_SETTINGS)
- All sensitive data in EncryptedSharedPreferences
- Pairing keys in Android Keystore (hardware-backed when available)
- Audit log records metadata only, never content
- No third-party SDKs, no trackers, no analytics
- Reproducible build configuration

### Notes
- This is pre-alpha software. Do not use on production devices.
- BLE protocol is at version 0.1 and may change without backward compatibility.
- iPhone companion app is not yet available.
