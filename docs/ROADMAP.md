# Roadmap

## Phases

### Phase 1: Project Foundation (week 1-2) ✅
- [x] Repository setup (GitHub, license, README)
- [x] Build system (Gradle Kotlin DSL, version catalog)
- [x] CI/CD (GitHub Actions: build, test, release)
- [x] Documentation skeleton (architecture, protocol, security, privacy)
- [x] SECURITY.md (vulnerability disclosure)
- [x] CONTRIBUTING.md
- [ ] Initial Compose UI (welcome screen, setup wizard stub)
- [ ] AndroidManifest.xml with minimal permission set

### Phase 2: Clean-Room Reimplementation (week 3-8)
- [ ] BLE GATT server implementation
  - [ ] Service + 4 characteristics
  - [ ] Pairing flow (ECDH + 6-digit code)
  - [ ] Encrypted frame protocol (AES-GCM)
  - [ ] Sequence number / replay protection
  - [ ] Bulk transfer (chunked)
- [ ] Telephony bridge
  - [ ] SMS send (SmsManager)
  - [ ] SMS receive (SmsReceiver + ContentObserver)
  - [ ] Contacts read (read-only)
  - [ ] Call log read
  - [ ] Phone state listener
- [ ] InCallService (optional)
  - [ ] Call state callbacks
  - [ ] Answer/reject from iPhone
  - [ ] Audio routing to BLE
- [ ] UI
  - [ ] Setup wizard (permissions, pairing)
  - [ ] Dashboard (status, paired device, controls)
  - [ ] Settings (InCallService toggle, audit log, reset)
  - [ ] About page

### Phase 3: Security Hardening (week 9-10)
- [ ] EncryptedSharedPreferences for all sensitive data
- [ ] Android Keystore with `setUserAuthenticationRequired` (where applicable)
- [ ] ProGuard/R8 with published mapping.txt
- [ ] Reproducible build settings
- [ ] Audit log UI (export, clear)
- [ ] Unit tests for crypto utilities
- [ ] Unit tests for BLE protocol parser

### Phase 4: Polish & First Alpha (week 11-12)
- [ ] RTL testing (Persian, Arabic, Hebrew)
- [ ] Dark mode testing
- [ ] Accessibility (TalkBack)
- [ ] First alpha release on GitHub Releases
- [ ] F-Droid submission preparation
- [ ] Documentation review

### Phase 5: Community & Beyond (ongoing)
- [ ] Bug bounty program
- [ ] Independent security audit
- [ ] iPhone companion app development
- [ ] Formal protocol verification (Tamarin/ProVerif)

## iPhone Companion App

The iPhone app is a separate project (Swift, iOS 16+). It will:
- Scan BLE for OpenBridge service UUID
- Implement the pairing flow client-side
- Provide call/SMS/contacts UI mirroring the Android side
- Be open-source (same GPLv3)

Repository: `openbridge/ios` (to be created)

## Limitations We Acknowledge

1. **One-way audio on Android 13+ without root**: We accept this limitation rather than request `WRITE_SECURE_SETTINGS` or use Shizuku-style hacks.
2. **No SMS MMS support** in first alpha (only text SMS).
3. **No group messaging**.
4. **No RCS support** (Google hasn't opened the API).
5. **Single paired iPhone** at a time.

## What We Will NOT Implement

- ❌ **License server** — never
- ❌ **Hardware attestation** — not needed (no server to prove to)
- ❌ **Tracker/analytics** — never
- ❌ **Closed-source native libs** — if needed, source will be in this repo
- ❌ **DIAG_CMD-style support session** — debugging is via standard Android tools
- ❌ **Autostart on boot** by default — opt-in only (will require user to grant `RECEIVE_BOOT_COMPLETED` manually)
- ❌ **Contact writes** — read-only by design
