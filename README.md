# QuickLens 🔍
### Universal Circle to Search & Visual Search Assistant for All Android Devices

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-brightgreen.svg)](https://android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Architecture](https://img.shields.io/badge/Architecture-Kotlin%20%7C%20Jetpack%20Compose-purple.svg)](https://developer.android.com/jetpack/compose)
[![No Root Required](https://img.shields.io/badge/Root-Not%20Required-success.svg)](#)

**QuickLens** brings Google's **Circle to Search** experience to **any Android phone** without requiring root, custom ROMs, unlocked bootloaders, or PC/ADB commands. 

Unlike brand-exclusive solutions (like MICTS for Xiaomi), **QuickLens is completely universal** — engineered to work seamlessly on Xiaomi/HyperOS, Samsung One UI, Google Pixel, Motorola, OnePlus/OxygenOS, Nothing OS, Realme, Vivo, and generic AOSP devices.

---

## ✨ Features

- 🔍 **Universal Circle to Search Trigger**
  - Hold the bottom navigation bar or Home button to launch Circle to Search / Google Lens instantly from any app or screen.
  - Native zero-permission trigger using Android `IVoiceInteractionManagerService` bypass whenever supported.
  - Automatic fallback to high-speed Accessibility screenshot capture.

- 👆 **Dual Navigation Mode Support (Universal Compatibility)**
  - **Gesture Navigation:**
    - **Stationary Hold (~350ms):** Triggers Circle to Search with precision haptic feedback.
    - **Swipe Up:** Cleanly proxies `GLOBAL_ACTION_HOME` so apps exit to Home without blocking touches or causing lag. Completely silent (no unwanted vibration).
    - **Swipe Up & Hold:** Proxies `GLOBAL_ACTION_RECENTS` with haptic feedback to open Recent Apps.
  - **3-Button Navigation Bar:**
    - **Quick Tap:** Passes Home tap immediately (`GLOBAL_ACTION_HOME`), ensuring the system Home button is 100% responsive and never blocked.
    - **Long-Press:** Triggers Circle to Search just like Google Pixel and Galaxy S24 series.
    - **Adaptive Pill:** Automatically hides our overlay pill in 3-Button mode to preserve your device's native system button icons.

- 🎛️ **Full Pill & Trigger Customization**
  - **Adjustable Size:** Choose any width from **`60dp`** (minimalist compact, default) up to **`240dp`** (wide area).
  - **Flexible Positioning:** Place the trigger at **Center** (default), **Left**, **Right**, or anywhere in between using fine-tuning sliders.
  - **Visual Pill Guide:** Optional subtle translucent pill indicator to guide your touch.

- 📱 **Launcher Tap-to-Search**
  - Tap the QuickLens app icon from your home screen or app drawer to launch visual search immediately.
  - Long-press the app icon and select **Settings** to customize preferences at any time.

- 🛡️ **HyperOS / MIUI Background Protection**
  - Built-in diagnostics and direct shortcuts to MIUI Autostart and Battery Optimization settings so OEM task killers never kill the service.

- ⚡ **Zero-Permission Privacy & Performance**
  - Operates locally via standard Accessibility Services.
  - Zero analytics, zero tracking, zero network requests — completely open-source and privacy-focused.

---

## 🛠️ How It Works

QuickLens uses a dual-engine architecture to ensure compatibility across every Android OEM:

```
┌─────────────────────────────────────────────────────────────┐
│                    QuickLens Trigger                        │
│      (Navigation Bar Hold / Home Long-Press / Launcher)     │
└──────────────────────────────┬──────────────────────────────┘
                               │
               Is Native Circle to Search available?
               ┌───────────────┴───────────────┐
              YES                              NO
               │                               │
    Invoke HiddenApiBypass            Accessibility Service
  IVoiceInteractionManagerService       Instant Screen Capture
               │                               │
   Native Circle to Search              Send to Google Lens
    Directly Over Current App            Over Current App
```

1. **Native Engine (`CircleToSearchInvoker`)**: Bypasses Android hidden API restrictions using `org.lsposed.hiddenapibypass` to directly call `IVoiceInteractionManagerService.showSessionFromSession()`. If your device has Google app installed with Circle to Search support, it invokes the native overlay instantly with zero screenshot delay.
2. **Accessibility Engine (`QuickLensAccessibilityService`)**: For devices where Google restricts native Circle to Search, QuickLens takes an instantaneous hardware screenshot via Accessibility APIs and opens Google Lens directly.

---

## 🚀 Getting Started

### Prerequisites
- Android 8.0 (Oreo / API 26) or higher.
- Google App installed (with Google Lens enabled).

### Setup
1. **Install QuickLens APK** on your device.
2. **Enable Accessibility Service:**
   - Open QuickLens Settings.
   - Tap **"Enable Service"** on the Accessibility status card and toggle QuickLens **ON**.
3. **Configure Navigation:**
   - Keep default settings for a sleek **60dp Center Pill** or customize size and position in **Trigger Methods**.
4. *(Xiaomi / HyperOS only)*:
   - Tap **"Open Battery Settings"** and set to **No restrictions**.
   - Tap **"Open Autostart"** and toggle QuickLens **ON** to prevent background termination.

---

## 📦 Building from Source

QuickLens is built with 100% Kotlin and modern Jetpack Compose.

```bash
# Clone the repository
git clone https://github.com/arghyar32/QuickLens.git
cd QuickLens

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

---

## 🔒 Permissions Used

| Permission | Purpose |
|------------|---------|
| `BIND_ACCESSIBILITY_SERVICE` | Detect navigation gestures, proxy Home/Recents, and capture screen for visual search |
| `VIBRATE` | Provide tactile feedback during trigger and recents activation |
| `FOREGROUND_SERVICE` | Fallback background service execution |

QuickLens requires **no internet permission** (`android.permission.INTERNET` is not even declared).

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!
Feel free to check [issues page](https://github.com/arghyar32/QuickLens/issues) if you'd like to contribute.

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
