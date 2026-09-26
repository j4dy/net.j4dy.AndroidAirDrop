# Android AirDrop (net.j4dy.AndroidAirDrop)

> [!WARNING]
> ### 🛑 Project Discontinued / Inactive
> **This project is no longer actively developed or continued.**
> Starting around **November 2025**, Android's built-in system **Quick Share** introduced native compatibility with Apple devices ("Share with Apple devices" via the AirDrop protocol). 
> Because this functionality is now natively integrated into modern Android system Quick Share without requiring custom third-party APKs or workarounds, maintaining this standalone continuation is no longer necessary.
>
> For reference or custom ROM implementations, the reverse-engineered AirDrop protocol and mDNS codebase remain available in this repository under the Apache 2.0 License.

---

An open-source Android app enabling one-way file transfer from Android to macOS using Apple's AirDrop protocol over local Wi-Fi.

This project was originally initiated as a modern continuation of [WarpShare](https://github.com/moseoridev/WarpShare) (created by the MoKee Open Source Project and moseoridev), rebuilt for modern Android (Android 14+) with Jetpack Compose Material 3 and system share sheet integration.

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

## 📜 Acknowledgments & License

- Forked and inspired by **WarpShare** ([moseoridev/WarpShare](https://github.com/moseoridev/WarpShare) and [MoKee Open Source Project](https://github.com/vinint/MoKee-WarpShare)).
- Licensed under the **Apache License 2.0**.
- *AirDrop is a trademark of Apple Inc.*
