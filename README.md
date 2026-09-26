# Android AirDrop (net.j4dy.AndroidAirDrop)

An open-source Android app enabling seamless one-way file transfer from Android to macOS using Apple's AirDrop protocol over local Wi-Fi.

This project is a modern continuation of [WarpShare](https://github.com/moseoridev/WarpShare) (originally created by the MoKee Open Source Project and later maintained by moseoridev), redesigned for modern Android versions (Android 14+) with improved discovery reliability, modern Jetpack Compose UI, and direct system share sheet integration.

---

## 🚀 How It Works

AirDrop on macOS operates over both AWDL (Apple Wireless Direct Link) and local Wi-Fi multicast DNS fallback.

1. **Discovery (mDNS):** When AirDrop is set to **"Everyone"** on a Mac, macOS advertises the `_airdrop._tcp` service over Bonjour / Multicast DNS.
2. **Handshake (TLS / HTTP):** The Android app discovers the Mac's IP address and connects to port `8770` over TLS using an AirDrop-compatible trust manager.
3. **Metadata (`/Discover` & `/Ask`):** The app sends device information and file metadata packaged as an Apple Property List (`.plist`). macOS presents a prompt asking the user to Accept or Decline.
4. **Data Stream (`/Upload`):** Upon acceptance, files are packed into a CPIO archive stream and uploaded directly to the Mac's Downloads folder.

---

## 📋 Requirements & Limitations

- **Local Network:** Both the Android device and the Mac must be connected to the **same Wi-Fi network** (without AP/client isolation).
- **Mac AirDrop Visibility:** AirDrop on macOS must be set to **"Everyone"** (due to Apple ID cryptographic certificates required for "Contacts Only"). Note that macOS automatically reverts "Everyone" after 10 minutes.
- **Direction:** Android $\rightarrow$ Mac only.
- **File Size Limit:** ~4.2 GB per single file transfer due to standard CPIO 32-bit header constraints (sufficient for standard everyday sharing).

---

## 🗺️ Roadmap & Milestones

Check the [GitHub Issues](https://github.com/j4dy/net.j4dy.AndroidAirDrop/issues) for the detailed feature tracking and progress:

1. **Phase 1: Foundation & Project Genesis** - Modern Gradle setup, Android 14+ target, Kotlin 2.x, core package structure.
2. **Phase 2: Discovery Engine Modernization** - Robust mDNS resolver combining Android `NsdManager` and fallback `JmDNS` to prevent OEM multicast drop issues.
3. **Phase 3: AirDrop Protocol State Machine** - Porting and modernizing TLS socket factories, plist serialization, and `/Discover`, `/Ask`, `/Upload` HTTP flow.
4. **Phase 4: Modern Compose UI & Android Share Sheet Integration** - Jetpack Compose radar scanner, transfer progress notifications, and system `ACTION_SEND` intent handling.
5. **Phase 5: Diagnostics & Troubleshooting Tooling** - In-app Wi-Fi diagnostics, subnet/AP isolation detection, connection status guides.
6. **Phase 6: CPIO / Archive Format Enhancements (Lowest Priority)** - Retaining standard CPIO packaging (< 4.2 GB) and deferring larger chunking formats.

---

## 📜 Acknowledgments & License

- Forked and inspired by **WarpShare** ([moseoridev/WarpShare](https://github.com/moseoridev/WarpShare) and [MoKee Open Source Project](https://github.com/vinint/MoKee-WarpShare)).
- Licensed under the **Apache License 2.0**.
- *AirDrop is a trademark of Apple Inc.*
