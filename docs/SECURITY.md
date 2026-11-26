# Security Design — Threat Model

Based on OWASP MASVS L2 and STRIDE.

## Assets

| Asset | Storage | Sensitivity |
|---|---|---|
| SMS content | Telephony provider + BLE transport | CRITICAL |
| Contacts | ContactsContract + BLE transport | CRITICAL |
| Call log | CallLog provider + BLE transport | HIGH |
| Live call audio | Audio framework + BLE transport | CRITICAL |
| Phone number | TelephonyManager | HIGH |
| Pairing key | Android Keystore | HIGH |
| Audit log | App-private storage | MEDIUM |

## STRIDE Analysis

### Spoofing

**Threat**: Attacker spoofs the iPhone to receive SMS/calls.
**Mitigation**: BLE pairing requires 6-digit confirmation code entered on iPhone. After pairing, all commands must be signed with the iPhone's persistent key. Replays prevented via sequence numbers.

**Threat**: Attacker spoofs the Android phone to capture iPhone data.
**Mitigation**: iPhone verifies Android's signature during pairing. iPhone stores Android's public key and refuses unsigned events.

### Tampering

**Threat**: Attacker modifies the APK to inject malware.
**Mitigation**: APK is signed by project maintainers. Users verify signature (see `docs/VERIFY.md`). Reproducible builds allow anyone to verify APK matches source.

**Threat**: Attacker tampers with BLE traffic in transit.
**Mitigation**: AES-GCM provides both confidentiality and integrity. Out-of-order or duplicate frames are rejected via sequence numbers and sliding window.

### Repudiation

**Threat**: User denies sending/receiving SMS.
**Mitigation**: Audit log (opt-in) records metadata of all operations. Cannot record content (privacy).

### Information Disclosure

**Threat**: Attacker intercepts BLE traffic.
**Mitigation**: All BLE payload is AES-GCM encrypted with key derived from ECDH. Only paired iPhone can decrypt.

**Threat**: Attacker reads data from lost/stolen device.
**Mitigation**: Sensitive data in EncryptedSharedPreferences with hardware-backed key. Pairing key in Keystore with `setRandomizedEncryptionRequired(true)`.

**Threat**: Attacker reads audit log.
**Mitigation**: Audit log is in app-private storage, not accessible to other apps or via USB without unlock. User can export and delete.

### Denial of Service

**Threat**: Attacker floods COMMAND characteristic with bogus commands.
**Mitigation**: Rate limiting (see `docs/PROTOCOL.md`). After N failed decrypts in T seconds, BLE connection is dropped and re-pairing required.

**Threat**: App crashes and restarts in a loop.
**Mitigation**: BridgeService uses exponential backoff for restarts. After 5 crashes in 10 minutes, service stops and notifies user.

### Elevation of Privilege

**Threat**: Attacker exploits a vulnerability to gain elevated access.
**Mitigation**: Minimal permission set. No `WRITE_SECURE_SETTINGS`. No reflection. No runtime DEX loading. No shell access. No native code (or source published).

## Permission Analysis

See `docs/PRIVACY.md` for the full permission list with justifications.

**Compared to similar apps**, OpenBridge requests:

- ❌ No `INTERNET` (most apps need this; we don't)
- ❌ No `WRITE_CONTACTS` (most apps write; we read-only)
- ❌ No `WRITE_SECURE_SETTINGS` (some apps need this for shell access; we don't)
- ❌ No `ACCESS_FINE_LOCATION` (legacy BLE scan; not needed on Android 13+)
- ❌ No `RECEIVE_BOOT_COMPLETED` (autostart is opt-in)

## Cryptographic Choices

| Purpose | Algorithm | Parameters |
|---|---|---|
| BLE pairing key exchange | ECDH | secp256r1 (P-256) |
| BLE session encryption | AES-GCM | 256-bit key, 12-byte nonce, 16-byte tag |
| Audit log integrity | HMAC-SHA256 | 256-bit key, derived from Keystore |
| APK signing | RSA | 4096-bit (release key) |
| Build attestation | SHA256 | for reproducible builds |

## Comparison with iTell

| Aspect | iTell | OpenBridge |
|---|---|---|
| Source code | Closed | Open (GPLv3) |
| License server | Yes (online check every 30 days) | None (fully offline) |
| Hardware attestation | Yes (Keymaster) | Not needed (no server) |
| SSL Pinning | No | N/A (no network) |
| Hardcoded secrets | 1 HMAC key, 2 ECDSA pubkeys | None |
| Trackers/analytics | None | None |
| Obfuscation | Heavy (R8) | Minimal (mapping.txt published) |
| Native libs | Yes (closed) | None (or source published) |
| Diagnostic command channel | Yes (signed by vendor) | None (not needed) |
| Autostart | Yes (default on) | Opt-in |
| InCallService | Yes (mandatory) | Opt-in |
| Vendor lock-in | High | None |
| Reproducible builds | No | Yes |

## Known Limitations

1. **No two-way audio on Android 13+ without root**: Apple's audio routing restrictions require either root or shell-level access. OpenBridge accepts one-way audio (iPhone hears caller, but mic doesn't reach caller) on these devices rather than requesting shell access.

2. **Pairing requires both devices present**: BLE pairing is local. No remote pairing (by design — security).

3. **Single iPhone paired at a time**: Only one iPhone can be paired per Android phone. This is by design (simpler key management, clearer UX).

4. **No SMS/call history beyond what's in providers**: OpenBridge doesn't keep its own database of SMS/calls. It reads from the system providers each time.

## Future Security Work

- [ ] Formal verification of BLE protocol (with Tamarin or ProVerif)
- [ ] Independent security audit (Cure53, Trail of Bits, or similar)
- [ ] Fuzzing of BLE message parser
- [ ] Penetration testing on a real device
- [ ] Bug bounty program (after stable release)
