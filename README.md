# OpenBridge

> **An open-source, secure bridge between your Android phone's SIM and your iPhone — no servers, no trackers, no license lock-in.**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform: Android](https://img.shields.io/badge/Platform-Android%2012+-green.svg)](https://developer.android.com)
[![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0-purple.svg)](https://kotlinlang.org)

---

## ✨ What is OpenBridge?

OpenBridge lets you use your Android phone's SIM card from your iPhone via Bluetooth LE — receive/make calls, send/receive SMS, and view contacts, all on your iPhone while the Android phone stays in your bag.

This is a **clean-room reimplementation** inspired by the iTell app, but:

- ✅ **100% open source** (GPLv3) — no closed-source components, no obfuscated code
- ✅ **No license server** — works fully offline, forever
- ✅ **No vendor lock-in** — no company can shut it down
- ✅ **No trackers / analytics / ads** — zero data leaves your device
- ✅ **No third-party IP geolocation** — uses `Locale`, not your IP
- ✅ **SSL Pinning** — even on rooted devices, MITM is harder
- ✅ **Encrypted storage** — all sensitive data in `EncryptedSharedPreferences`
- ✅ **Reproducible builds** — verify the APK you install matches the source
- ✅ **Audit log** — opt-in, transparent, exportable

## 📱 How it works

```
┌─────────────┐         BLE GATT          ┌──────────────┐
│   iPhone    │  ←─────────────────────→  │   Android    │
│  (your app) │   (custom protocol)       │  OpenBridge  │
└─────────────┘                            └──────────────┘
                                                  │
                                                  │ system APIs
                                                  ▼
                                          ┌──────────────┐
                                          │  SIM / SMS   │
                                          │  Contacts    │
                                          │  Call Log    │
                                          └──────────────┘
```

## 🔧 Status

**Pre-alpha.** This repository contains the project skeleton and design docs. The protocol and core modules are being implemented. See [`docs/ROADMAP.md`](docs/ROADMAP.md) for the development plan.

## 📖 Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — System architecture and component design
- [`docs/PROTOCOL.md`](docs/PROTOCOL.md) — BLE GATT protocol specification
- [`docs/SECURITY.md`](docs/SECURITY.md) — Threat model and security policy
- [`docs/PRIVACY.md`](docs/PRIVACY.md) — Privacy policy (what data, where it goes)
- [`docs/BUILD.md`](docs/BUILD.md) — Build from source instructions
- [`docs/VERIFY.md`](docs/VERIFY.md) — Verify a release is built from this source

## 🛡️ Security

If you discover a vulnerability, please see [`SECURITY.md`](SECURITY.md) for responsible disclosure.

## 🤝 Contributing

Contributions welcome! Please read [`CONTRIBUTING.md`](CONTRIBUTING.md) first.

## 📜 License

GNU General Public License v3. See [`LICENSE`](LICENSE).

## 🌍 Why this project exists

Apps like iTell charge money and lock users into a server dependency — if the company shuts down or your internet is restricted (sanctions, network outages, traveling), the app stops working after 30 days. We believe a phone bridge is too important to be locked behind a paywall and a server. OpenBridge exists to provide a free, audit-able, community-owned alternative.

---

<div dir="rtl">

# OpenBridge (فارسی)

> **پل متن‌باز و امن بین سیم‌کارت گوشی اندروید شما و آیفون — بدون سرور، بدون ردیاب، بدون قفل لایسنس.**

## معرفی

OpenBridge به شما اجازه می‌دهد سیم‌کارت گوشی اندرویدی خود را از طریق Bluetooth LE روی آیفون مدیریت کنید — تماس بگیرید و بگیرید، SMS بفرستید و بگیرید، مخاطبین را ببینید. این پروژه یک **بازنویسی clean-room** الهام گرفته از اپ iTell است، اما کاملاً متن‌باز و بدون وابستگی به سرور.

## وضعیت

**pre-alpha.** این repository شامل skeleton پروژه و مستندات طراحی است. برای نقشه راه توسعه به [`docs/ROADMAP.md`](docs/ROADMAP.md) مراجعه کنید.

## چرا این پروژه وجود دارد

اپ‌هایی مثل iTell پول می‌گیرند و کاربر را به سرور قفل می‌کنند — اگر شرکت تعطیل شود یا اینترنت شما محدود باشد (تحریم، قطعی شبکه، سفر)، اپ بعد از ۳۰ روز متوقف می‌شود. ما معتقدیم یک پل گوشی خیلی مهم است تا پشت paywall و سرور قفل شود. OpenBridge برای ارائه‌ی یک جایگزین رایگان، قابل audit، و متعلق به جامعه وجود دارد.

</div>
