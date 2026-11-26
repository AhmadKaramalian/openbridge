# Architecture

## Overview

OpenBridge is an Android app that bridges a phone's SIM card functionality (calls, SMS, contacts, call log) to a paired iPhone via Bluetooth LE. The architecture follows these principles:

1. **No server dependency** — the app works fully offline
2. **Minimal permissions** — only what's strictly necessary
3. **Defensive security** — SSL pinning, encrypted storage, audit logging
4. **Modular design** — each component is testable in isolation
5. **Observable** — every sensitive operation is logged (opt-in audit)

## Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                          Android Phone                       │
│                                                              │
│  ┌──────────────┐    ┌───────────────────────────────────┐  │
│  │  MainActivity │    │           BridgeService           │  │
│  │   (Compose)   │◄──►│        (Foreground Service)       │  │
│  │   Setup wizard│    │                                   │  │
│  │   Permissions  │    │  ┌──────────┐  ┌─────────────┐  │  │
│  │   Status       │    │  │ BleServer│  │TelephonyBrig│  │  │
│  └──────────────┘    │  │ (GATT)   │  │ (SMS/Contacts│  │  │
│                      │  └────┬─────┘  └──────┬──────┘  │  │
│  ┌──────────────┐    │       │               │         │  │
│  │  InCallSvc   │◄───┼───────┼───────────────┘         │  │
│  │  (optional)  │    │       │                         │  │
│  └──────────────┘    │       │                         │  │
│                      └───────┼─────────────────────────┘  │
│                              │                             │
│                              ▼ BLE                         │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
                       ┌───────────────┐
                       │     iPhone    │
                       │  (paired app) │
                       └───────────────┘
```

## Components

### MainActivity (UI Layer)

- **Jetpack Compose** based UI
- Setup wizard guiding the user through:
  - Granting permissions (with explanation for each)
  - Pairing with iPhone (QR code or manual code)
  - Testing connection
- Status dashboard showing:
  - Connection state
  - Permissions status
  - Audit log access
- Settings page with:
  - Toggle InCallService on/off
  - Toggle autostart on boot
  - Audit log export
  - Data reset

### BridgeService (Core)

A foreground service that runs continuously while OpenBridge is active. It coordinates:

- **BleServer** — the GATT server exposing characteristics to the iPhone
- **TelephonyBridge** — bridges SMS/Contacts/CallLog to/from the iPhone
- **CallController** — manages call state and audio routing

The service uses a single-threaded command processor to serialize BLE commands from the iPhone and ensure consistency.

### BleServer (BLE Layer)

Implements a GATT server with these characteristics (see `docs/PROTOCOL.md` for full spec):

- `COMMAND` (write) — iPhone writes commands (send SMS, dial, etc.)
- `EVENT` (notify) — Android notifies iPhone of events (SMS received, call incoming)
- `CONTACTS_SYNC` (read) — bulk contact sync
- `STATUS` (read) — connection/pairing state

Pairing uses an Elliptic Curve Diffie-Hellman key exchange with a 6-digit confirmation code displayed on Android and entered on iPhone. All subsequent traffic is encrypted with AES-GCM using the derived key.

### TelephonyBridge (Telephony Layer)

Wraps Android system APIs:

- **SmsManager** for sending SMS
- **ContentObserver** on `Telephony.Sms.CONTENT_URI` for incoming SMS
- **ContactsContract** for contact reads (no writes by default)
- **CallLog** for call history reads
- **TelecomManager** for call management (when InCallService is enabled)

All accesses are logged to the AuditLog.

### InCallService (Call Layer, optional)

A `InCallService` that takes over the call UI. **Off by default** — user must explicitly enable it in Settings.

When enabled, it:
- Receives `onCallAdded` callbacks
- Notifies the iPhone of incoming calls
- Routes audio to/from the iPhone via BLE
- Allows the iPhone to answer/reject calls

### Security Utilities

- **CryptoUtil** — AES-GCM, ECDH, ECDSA helpers using Android Keystore
- **SecureStorage** — wrapper around `EncryptedSharedPreferences`
- **AuditLog** — append-only, opt-in log of sensitive operations
- **CertificatePinnerConfig** — OkHttp certificate pinning configuration

## Data Flow Examples

### Incoming SMS

```
Android Telephony.Sms provider
   │
   ▼ ContentObserver
TelephonyBridge.onSmsReceived()
   │
   ├──► AuditLog.record("sms_received", from, length)
   │
   ▼
BleServer.notifyEvent(SMS_RECEIVED, payload)
   │
   ▼ AES-GCM encrypted over BLE
iPhone displays the SMS
```

### Outgoing SMS (from iPhone)

```
iPhone writes to COMMAND characteristic
   │
   ▼ AES-GCM decrypted
BleServer parses command (send_sms, to, body)
   │
   ▼
TelephonyBridge.sendSms(to, body)
   │
   ├──► AuditLog.record("sms_sent", to, length)
   │
   ▼
SmsManager.sendTextMessage()
   │
   ▼
Confirmation written back to COMMAND response
```

## Security Boundaries

| Boundary | Protection |
|---|---|
| Network (if any) | TLS + SSL Pinning |
| Local storage | EncryptedSharedPreferences + Keystore |
| BLE transport | ECDH key exchange + AES-GCM encryption |
| BLE pairing | 6-digit confirmation code |
| Permission scope | Minimal (see AndroidManifest.xml) |
| Audit trail | Opt-in append-only log |
| Build integrity | Reproducible + SHA256 + GPG signature |

## Decisions and Trade-offs

### Why no server at all?

- No vendor lock-in
- No single point of failure
- No data collection possible
- Works in any network condition

### Why GPLv3?

- Forces derivatives to stay open-source
- Aligns with user-freedom values
- Compatible with future AGPL migration if needed

### Why is InCallService optional?

It's the most invasive permission set (takes over call UI). Some users may only need SMS/Contacts bridging. Defaulting to off respects the principle of least privilege.

### Why Compose over XML?

- Modern Android standard
- Better tooling and preview support
- Single-source-of-truth for UI state
- Easier to write UI tests

### Why no Shizuku-style shell access?

iTell uses ADB-pairing-based shell access for audio routing on certain Android versions. This adds complexity and security risk. OpenBridge accepts reduced audio functionality on some devices in exchange for:
- Simpler security model
- No `WRITE_SECURE_SETTINGS` permission
- No ADB dependency for end users
- Easier reproducible builds
