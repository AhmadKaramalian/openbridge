# BLE GATT Protocol Specification

**Version**: 0.1 (draft)
**Status**: Pre-alpha — subject to change

## Overview

OpenBridge exposes a Bluetooth LE GATT server that the iPhone app connects to. The protocol is designed for:

- **Confidentiality**: All payload encrypted with AES-GCM after ECDH key exchange
- **Integrity**: AES-GCM authentication tag + sequence numbers
- **Replay protection**: 32-bit sequence counter, reset on re-pair
- **Mutual authentication**: iPhone proves possession of pairing key
- **Frame multiplexing**: Multiple logical streams over a single characteristic

## Service UUID

```
OpenBridge Service:  8f3c1a00-5b8f-4c2a-9d3e-2f6a1b0c9d01
```

> **Note**: This UUID is intentionally different from any other app's. We generate new UUIDs for v5 namespace `6ba7b810-9dad-11d1-80b4-00c04fd430c8` with name "openbridge-v0".

## Characteristics

| # | UUID (suffix) | Properties | Purpose |
|---|---|---|---|
| 1 | `8f3c1a01-...` | write, write-without-response | iPhone → Android commands |
| 2 | `8f3c1a02-...` | read, notify | Android → iPhone events |
| 3 | `8f3c1a03-...` | write, read | Bulk transfer (contact sync) |
| 4 | `8f3c1a04-...` | read | Status info (connection state, version) |

## Pairing Flow

```
iPhone                    Android
  │                          │
  │  1. BLE Connect          │
  ├─────────────────────────►│
  │                          │
  │  2. Read STATUS          │
  ├─────────────────────────►│
  │  ◄─────── version, etc ──┤
  │                          │
  │  3. Write COMMAND:       │
  │     {op: "pair_start",   │
  │      pubkey: P_i}        │
  ├─────────────────────────►│
  │                          │
  │           4. Generate ECDH keypair (P_a, p_a)
  │              Derive shared secret K = ECDH(p_a, P_i)
  │              Derive session keys: K_enc, K_mac
  │              Generate 6-digit code C
  │                          │
  │  5. Notify EVENT:        │
  │     {op: "pair_confirm", │
  │      pubkey: P_a,        │
  │      sig: sig_p_a(...)}  │
  │  ◄───────────────────────┤
  │                          │
  │     [User enters C on iPhone]
  │                          │
  │  6. Write COMMAND:       │
  │     {op: "pair_complete",│
  │      code: H(C),         │
  │      sig: sig_p_i(...)}  │
  ├─────────────────────────►│
  │                          │
  │           7. Verify code & signature
  │              Persist pairing: {iPhone_pubkey, K}
  │                          │
  │  8. Notify EVENT:        │
  │     {op: "paired"}       │
  │  ◄───────────────────────┤
  │                          │
  ▼                          ▼
   Encrypted session established
```

## Frame Format (Encrypted)

After pairing, all payloads are encrypted:

```
┌───────────┬───────────────┬─────────────────────────┬──────────────┐
│  seq (4B) │  nonce (12B)  │  ciphertext (variable)  │  tag (16B)   │
└───────────┴───────────────┴─────────────────────────┴──────────────┘
```

- **seq**: 32-bit big-endian sequence number, increments per frame
- **nonce**: 12-byte random, MUST be unique per (key, seq)
- **ciphertext**: AES-GCM encrypted plaintext
- **tag**: 16-byte GCM authentication tag

The AAD (Additional Authenticated Data) is:
- For COMMAND writes: `b"cmd" || seq || char_uuid`
- For EVENT notifies: `b"evt" || seq || char_uuid`

## Command Format (Plaintext)

JSON-encoded, max 1024 bytes per command. Larger operations use the bulk transfer characteristic.

```json
{
  "op": "send_sms",
  "to": "+989123456789",
  "body": "Hello",
  "id": "uuid-v4",
  "ts": 1234567890
}
```

## Command Operations

| op | Direction | Fields | Response |
|---|---|---|---|
| `pair_start` | iPhone→Android | `pubkey` | EVENT pair_confirm |
| `pair_complete` | iPhone→Android | `code`, `sig` | EVENT paired |
| `send_sms` | iPhone→Android | `to`, `body`, `id` | EVENT sms_sent / sms_failed |
| `dial` | iPhone→Android | `to`, `id` | EVENT dial_result |
| `end_call` | iPhone→Android | `call_id` | EVENT call_ended |
| `answer_call` | iPhone→Android | `call_id` | EVENT call_answered |
| `sync_contacts` | iPhone→Android | (none) | Bulk transfer on characteristic 3 |
| `get_call_log` | iPhone→Android | `limit`, `offset` | EVENT call_log with payload |
| `get_status` | iPhone→Android | (none) | EVENT status |
| `reset_pairing` | iPhone→Android | (none) | EVENT pairing_reset |

## Event Format

```json
{
  "type": "sms_received",
  "ts": 1234567890,
  "data": { ... }
}
```

## Event Types

| type | Trigger | data fields |
|---|---|---|
| `pair_confirm` | pair_start received | `pubkey`, `sig` |
| `paired` | pair_complete verified | (none) |
| `sms_received` | Android SMS observer | `from`, `body`, `ts`, `id` |
| `sms_sent` | send_sms success | `id`, `sms_id` |
| `sms_failed` | send_sms failure | `id`, `error` |
| `call_incoming` | Telecom call added | `from`, `call_id`, `ts` |
| `call_answered` | answer_call success | `call_id` |
| `call_ended` | call ended | `call_id`, `reason` |
| `dial_result` | dial success/fail | `id`, `call_id` or `error` |
| `status` | get_status reply | `state`, `paired`, `version` |
| `pairing_reset` | reset_pairing | (none) |

## Bulk Transfer (Characteristic 3)

For large data (e.g., contact list), use a chunked transfer protocol:

```
┌────────────┬───────────┬───────────┬─────────────┐
│ msg_id(16B)│ chunk_idx │ chunk_cnt │  payload    │
└────────────┴───────────┴───────────┴─────────────┘
```

- `msg_id`: UUID v4, same for all chunks of one message
- `chunk_idx`: 16-bit, 0-indexed
- `chunk_cnt`: 16-bit, total chunks
- `payload`: up to MTU-7 bytes (typically 18-180 bytes)

After all chunks received, the receiver reassembles and processes the plaintext.

## Error Codes

| code | meaning |
|---|---|
| `0x01` | Invalid JSON |
| `0x02` | Unknown op |
| `0x03` | Missing field |
| `0x04` | Not paired |
| `0x05` | Permission denied |
| `0x06` | Decrypt failure |
| `0x07` | Replay detected |
| `0x08` | Rate limited |
| `0x09` | Internal error |

## Rate Limiting

To prevent abuse:

- `send_sms`: max 30 per minute, 100 per hour
- `dial`: max 10 per minute
- `sync_contacts`: max 1 per 10 minutes

Rate limits are per-pairing, reset on re-pair.

## Versioning

The protocol version is sent in the STATUS characteristic:

```
version: "0.1"
protocol_min: "0.1"
protocol_max: "0.1"
```

The iPhone should refuse to connect if its protocol version is outside the Android's `[protocol_min, protocol_max]` range.

## Security Considerations

### Replay Protection

Every encrypted frame includes a 32-bit sequence number. The receiver maintains a sliding window of last 64 seen sequence numbers per pairing. Out-of-window or duplicate frames are rejected.

### Forward Secrecy

Each pairing generates a new ECDH keypair. If the pairing is reset, the old session keys are securely erased from memory and disk.

### Key Storage

Pairing keys are stored in the Android Keystore with `setUserAuthenticationRequired(false)` (for background operation) but `setRandomizedEncryptionRequired(true)`. Access requires the app's UID.

### Pairing Code Brute Force

The 6-digit pairing code (10^6 possibilities) is valid for 60 seconds. After 3 failed attempts, the pairing session is reset and a new code is generated. Brute-forcing 6 digits in 60s over BLE (which has a ~100ms round trip) is infeasible (max ~600 attempts).

## Open Questions

1. Should we support multiple paired iPhones simultaneously? (Currently: 1)
2. Should contact sync be one-way (read-only) or two-way? (Currently: read-only)
3. Should we expose call recording capability? (Currently: no)
4. Should audio be encrypted or just integrity-protected? (Currently: encrypted)
