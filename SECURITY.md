# Security Policy

## Supported Versions

| Version | Supported          |
|---------|--------------------|
| latest main branch | ✅ |
| tagged releases    | ✅ |
| older versions     | ❌ (upgrade required) |

## Reporting a Vulnerability

**Please DO NOT report security vulnerabilities through public GitHub issues.**

Instead, please report them via one of these channels:

### Option 1: Encrypted email (preferred)

Send an email to `security@openbridge.example.org` (replace with project's actual address).
Encrypt your report with our PGP key (see `SECURITY_PGP_KEY` in this repository).

### Option 2: GitHub Security Advisories

Use GitHub's "Security Advisories" feature on this repository:
1. Go to the repository's "Security" tab
2. Click "Report a vulnerability"
3. Fill in the details

## What to include in your report

- Description of the vulnerability
- Steps to reproduce
- Affected versions (commit hash or tag)
- Potential impact
- Suggested fix (if any)
- Your name/handle for credit (optional)

## Response timeline

- **Acknowledgment**: within 48 hours
- **Initial assessment**: within 7 days
- **Fix or mitigation**: depends on severity, target 30 days for HIGH/CRITICAL
- **Public disclosure**: after fix is released, coordinated with reporter

## Disclosure

We follow **coordinated disclosure**:
1. Reporter reports privately
2. We acknowledge and assess
3. We develop and test a fix
4. Fix is released
5. Public advisory is published (with credit to reporter unless they prefer to remain anonymous)

## Threat Model

See [`docs/SECURITY.md`](docs/SECURITY.md) for the full threat model based on OWASP MASVS L2.

## Security measures in this project

- ✅ TLS everywhere, with SSL Pinning on all API calls (if any API is used)
- ✅ EncryptedSharedPreferences for all sensitive data
- ✅ Android Keystore with `setUserAuthenticationRequired(true)` for crypto keys
- ✅ No hardcoded secrets (no API keys, no HMAC keys, no private keys)
- ✅ Minimal permission set (only what's necessary, see `AndroidManifest.xml`)
- ✅ No trackers/analytics/ad SDKs
- ✅ No closed-source native libraries (or source published alongside)
- ✅ ProGuard/R8 with public mapping.txt for reproducible builds
- ✅ Reproducible builds with published SHA256 hashes
- ✅ Audit log (opt-in) for all sensitive data accesses
- ✅ Open-source — anyone can verify the code

## PGP Key

```
[TODO: Paste project PGP public key here when generated]
```

## Acknowledgments

We thank security researchers who report vulnerabilities responsibly. See `HALL_OF_FAME.md` (created upon first report).
