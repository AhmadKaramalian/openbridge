# Privacy Policy

**Last updated**: 2026-11-26

## TL;DR

OpenBridge collects **no data**. No analytics, no telemetry, no crash reports, no advertising IDs. Your data stays on your device.

## What data does OpenBridge access?

OpenBridge needs to access the following data on your Android phone to fulfill its function (bridging to your iPhone):

| Data | Why | Where it goes |
|---|---|---|
| SMS messages (incoming & outgoing) | Forward to iPhone, send from iPhone | Encrypted over BLE to your paired iPhone |
| Contacts list | Sync to iPhone for display | Encrypted over BLE to your paired iPhone |
| Call log | Display on iPhone | Encrypted over BLE to your paired iPhone |
| Phone state (ringing, in-call) | Notify iPhone of incoming calls | Encrypted over BLE to your paired iPhone |
| Phone number (your SIM's) | Display on iPhone | Encrypted over BLE to your paired iPhone |

**None of this data is sent to us, to any server, or to any third party.**

## What data does OpenBridge store locally?

- **Pairing key**: stored in Android Keystore (hardware-backed if available)
- **Pairing identifier**: stored in `EncryptedSharedPreferences`
- **App settings**: stored in regular SharedPreferences (no sensitive data)
- **Audit log (opt-in)**: stored in app-private storage, exportable by user

## What data does OpenBridge send to the network?

**Nothing.** OpenBridge has no internet access. The `INTERNET` permission is **not requested** in the manifest.

You can verify this:

1. Open `app/src/main/AndroidManifest.xml` — no `<uses-permission android:name="android.permission.INTERNET"/>`
2. Run `nm app/build/outputs/apk/release/app-release.apk` — no network libraries
3. Monitor your network traffic with a firewall (NetGuard, AFWall) — OpenBridge makes zero connections

## Third-party services

**None.** OpenBridge does not use:

- Google Firebase, Google Analytics, Google Crashlytics
- AppsFlyer, Adjust, Mixpanel, Amplitude, Segment
- Any ad SDK
- Any IP geolocation service (we use `Locale` instead)
- Any push notification service

## Bluetooth LE

OpenBridge uses Bluetooth LE to communicate with your paired iPhone. The BLE traffic is encrypted with AES-GCM using a key derived from an ECDH exchange. Only your paired iPhone can decrypt the data.

## Audit Log (Opt-in)

If you enable the Audit Log in Settings, OpenBridge records:

- When an SMS was sent/received (timestamp, peer number length, message length)
- When a contact was accessed (timestamp, count)
- When a call was made/received (timestamp, peer number length, duration)
- When a BLE command was received (timestamp, op type)

**The audit log does NOT record message contents or contact details.** It only records metadata for your own verification.

The audit log is:
- Stored locally only
- Never sent anywhere
- Exportable as a JSON file
- Clearable at any time

## Permissions requested

| Permission | Why |
|---|---|
| `CALL_PHONE` | Dial from iPhone |
| `ANSWER_PHONE_CALLS` | Answer from iPhone |
| `READ_CALL_LOG` | Show call history on iPhone |
| `SEND_SMS` | Send SMS from iPhone |
| `RECEIVE_SMS` | Notify iPhone of new SMS |
| `READ_SMS` | Sync SMS history |
| `READ_CONTACTS` | Sync contacts to iPhone |
| `READ_PHONE_STATE` | Detect calls |
| `READ_PHONE_NUMBERS` | Show your SIM number on iPhone |
| `BLUETOOTH_ADVERTISE` | Make iPhone discover your phone |
| `BLUETOOTH_CONNECT` | Maintain BLE connection |
| `FOREGROUND_SERVICE` | Run bridge in background |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | BLE foreground service type |
| `POST_NOTIFICATIONS` | Notify you of events |
| `WAKE_LOCK` | Keep CPU during call |

**Permissions NOT requested** (and why):

- ❌ `INTERNET` — no network access needed
- ❌ `ACCESS_FINE_LOCATION` — not needed on Android 13+ for BLE
- ❌ `WRITE_CONTACTS` — contacts are read-only in OpenBridge
- ❌ `WRITE_SECURE_SETTINGS` — no system-level settings changes
- ❌ `RECEIVE_BOOT_COMPLETED` — autostart is opt-in only, enabled per-session
- ❌ `ACCESS_NETWORK_STATE` — no network operations

## Children's privacy

OpenBridge is not directed at children. We don't knowingly collect any data from anyone, including children.

## Changes to this policy

We will update this policy if the app's data practices change. All changes are committed to this repository and tagged in releases.

## Contact

Open an issue on GitHub for any privacy-related questions.

## License

This document is licensed under CC0 1.0 Universal (Public Domain).
