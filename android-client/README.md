# MPA Android Client

Android VPN client for **MPA (Mariittov Private Access)** based on [sing-box](https://github.com/SagerNet/sing-box).  
Supports VLESS + XTLS-Vision + Reality — the same protocol as the desktop client.

> **Note:** This is a client app only. You need your own VLESS+Reality server and either a `vless://` link, subscription URL, or activation key from the MPA Telegram bot.
> 

## Features

- VLESS + Reality + uTLS — full support
- Three input formats: `vless://` link, subscription URL, activation key
- Automatic hourly subscription refresh (WorkManager)
- Dark theme — same palette as the desktop client (#12161C / #E8A33D copper / #5FBF9F teal)
- Space Grotesk + JetBrains Mono fonts
- Animated background
- Per-server TCP ping
- Foreground VPN service with quick-disconnect notification
- Auto-start on boot

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material3 |
| VPN core | sing-box (libbox) |
| Storage | DataStore Preferences |
| Serialization | Gson |
| Async | Kotlin Coroutines + Flow |
| Background tasks | WorkManager |

## Building

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17
- Go 1.22+ (for building libbox)
- [gomobile](https://pkg.go.dev/golang.org/x/mobile/cmd/gomobile)

---

### Step 1 — Build libbox.aar

libbox is the Android library for sing-box. It is **not included in the repo** and must be built from source.

```bash
# Install gomobile if you haven't already
go install golang.org/x/mobile/cmd/gomobile@latest
go install golang.org/x/mobile/cmd/gobind@latest
gomobile init

# Clone sing-box
git clone https://github.com/SagerNet/sing-box
cd sing-box

# Build libbox with all tags needed for VLESS+Reality+uTLS
gomobile bind -v \
  -androidapi 21 \
  -target android/arm64,android/arm,android/amd64,android/386 \
  -tags "with_gvisor,with_quic,with_dhcp,with_wireguard,with_utls,with_reality_server,with_clash_api" \
  -o libbox.aar \
  github.com/sagernet/sing-box/experimental/libbox
```

> **Important:** The `with_utls` and `with_gvisor` tags are required. Without `with_utls` — Reality won't work. Without `with_gvisor` — the TUN stack won't start.

Copy the output files to the project:

```
app/libs/libbox.aar
app/libs/libbox-sources.jar   # optional, for IDE sources
```

---

### Step 2 — Add fonts

Download and place TTF files in `app/src/main/res/font/`:

| File | Source |
|---|---|
| `space_grotesk_regular.ttf` | [Google Fonts — Space Grotesk](https://fonts.google.com/specimen/Space+Grotesk) |
| `space_grotesk_medium.ttf` | same |
| `space_grotesk_semibold.ttf` | same |
| `space_grotesk_bold.ttf` | same |
| `jetbrains_mono_regular.ttf` | [JetBrains Mono](https://www.jetbrains.com/legalnotices/font/) |

Both fonts are free and open-source (SIL Open Font License).

---

### Step 3 — Configure the activation API (optional)

If you use activation keys from the MPA Telegram bot, set the API URL at build time:

```bash
./gradlew assembleDebug -PMPA_ACTIVATION_API=https://your-server.example.com
```

Or export as an environment variable:

```bash
export MPA_ACTIVATION_API=https://your-server.example.com
./gradlew assembleDebug
```

If you only use `vless://` links or subscription URLs — skip this step.

---

### Step 4 — Build

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (requires a signing keystore)
./gradlew assembleRelease
```

APK will be in `app/build/outputs/apk/`.

---

## Usage

1. Open the app
2. Tap **Добавить** (Add)
3. Paste one of:
   - `vless://...` — direct VLESS link
   - `https://...` — subscription URL (base64 or plain)
   - Activation key from the MPA bot (digits only)
4. Tap the power button to connect

The app will ask for VPN permission on first connect.

## Architecture

```
MainActivity
    └── MainViewModel              ← state + business logic
         ├── ProfileRepository     ← DataStore, stores server profiles
         ├── SubscriptionResolver  ← parses vless:// / subscription / activation key
         └── MpaVpnService         ← foreground VPN service
              └── SingBoxConfig    ← builds sing-box JSON config
```

### How the VPN works

Android does not allow direct TUN device access without root. The flow is:

1. `VpnService.Builder` creates a TUN interface and returns a file descriptor
2. The file descriptor is passed to sing-box via `PlatformInterface.openTun()`
3. sing-box reads/writes packets through that fd
4. `VpnService.protect()` is called on sing-box's outbound sockets so they bypass the TUN (no routing loop)
5. All platform flags (`usePlatformAutoDetectInterfaceControl`, `usePlatformDefaultInterfaceMonitor`, `usePlatformInterfaceGetter`) are set to `true` — this tells sing-box not to use netlink sockets directly (which are banned by SELinux on Android without root)

Key config decisions:
- `auto_route: false` — routes are set by `VpnService.Builder`, not sing-box (sing-box's auto_route uses netlink — banned)
- `stack: gvisor` — userspace TCP/IP stack, no kernel socket calls
- `strict_route: false` — not set, avoids interface bind calls that require netlink

## License

MIT
