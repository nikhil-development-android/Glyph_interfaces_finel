# ⚡ Nothing Phone (2a) Glyph Interface Controller & HyperOS Porting Bridge

An open-source, full-featured **Glyph Interface Controller, Sound Composer, Music Visualizer, and Hardware Driver Bridge** designed specifically for the **Nothing Phone (2a)** ("pacman"). 

Built with **Jetpack Compose (Material 3)**, **Kotlin Coroutines**, **AudioTrack PCM Synthesizer**, and **Gemini 3.5 Flash AI** for universal installation across **Android 14, 15, 16, and 17** (Stock Nothing OS, HyperOS Ports, AOSP, LineageOS, and custom ROMs).

---

## 🗺️ Project Architecture & File Map

```text
glyph-interface/
├── app/
│   ├── build.gradle.kts                   # Dependencies, SDK target (35), Gemini Secret configuration
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml        # Permissions (Vibrate, Audio, Internet, Notifications)
│           ├── java/com/example/
│           │   ├── MainActivity.kt        # Root Activity, iOS-style spring transitions & screen routing
│           │   ├── audio/
│           │   │   └── GlyphSoundSynth.kt # Real-time Low-latency PCM Synthesizer (Nothing chimes, kicks, sweeps)
│           │   ├── gemini/
│           │   │   └── GeminiService.kt   # Gemini 3.5 Flash AI pattern generation & Natural Voice Command engine
│           │   ├── hardware/
│           │   │   └── GlyphHardwareManager.kt # Root/Sysfs execution bridge (`/sys/class/leds/aw210xx_led`)
│           │   ├── model/
│           │   │   └── GlyphModels.kt     # State data classes, Ringtone presets, Sysfs node models, Enums
│           │   ├── ui/
│           │   │   ├── components/
│           │   │   │   ├── GlyphCanvas.kt         # 26-Channel Canvas Vector Rendering (24 Arc Segments + 2 Strips)
│           │   │   │   └── GlyphFloatingNavBar.kt # Floating pill navigation bar (Home, Timer, Call, Gemini, Settings)
│           │   │   ├── screens/
│           │   │   │   ├── GlyphHomeScreen.kt     # Main Dashboard (Visualizer, Soundboard, Presets, Sysfs Driver)
│           │   │   │   ├── GlyphTimerScreen.kt    # Dedicated Arc Countdown Timer (Presets, Flip to Glyph)
│           │   │   │   ├── GlyphPatternsScreen.kt # Call & Ringtone Choreographies (Abra, Anna, Beetle, etc.)
│           │   │   │   ├── GlyphGeminiScreen.kt   # Gemini AI Pattern Composer & Multi-lingual Voice Controller
│           │   │   │   └── GlyphSettingsScreen.kt # Theme selector (Dark/Light/System), 120Hz mode, Root Terminal
│           │   │   └── theme/
│           │   │       ├── Color.kt               # Sophisticated Dark & Nothing Light signature palettes
│           │   │       ├── Theme.kt               # Material 3 Color Schemes & dynamic theme wrapper
│           │   │       └── Type.kt                # Monospace & Sans-serif typography definitions
│           └── res/
│               ├── values/strings.xml     # App name and resource strings
│               └── mipmap-*/              # Adaptive vector launcher icons
├── metadata.json                          # Platform identification & Gemini API capabilities
├── settings.gradle.kts                    # Project modules and plugin repositories
└── README.md                              # Complete system documentation & porting guide
```

---

## 📂 Detailed File Breakdown & Responsibilities

| File | Purpose & Functionality |
| :--- | :--- |
| **`MainActivity.kt`** | Entry point of the app. Initializes `enableEdgeToEdge()`, observes `themeMode`, and wraps navigation with iOS-style spring transitions (`slideInHorizontally` + `fadeIn` / `slideOutHorizontally` + `fadeOut`). |
| **`GlyphModels.kt`** | Core state container (`GlyphState`) maintaining 24 float segments for Strip 1, Strip 2 & 3 intensities, active presets, ringtone metadata, timer states, and `AppScreen` navigation routes. |
| **`GlyphViewModel.kt`** | Central MVVM controller. Drives real-time 120Hz LED animation loops, timer countdown ticks, ringtone choreography timelines, Gemini API calls, and hardware sysfs writes. |
| **`GlyphCanvas.kt`** | High-performance Compose `Canvas` drawing the authentic Nothing Phone (2a) back layout: 24 arc segments with precise mathematical angles, red recording LED, camera cutouts, and dual diagonal strips. |
| **`GlyphFloatingNavBar.kt`** | Glassmorphic floating navigation bar featuring 5 quick destinations with interactive scale and spring feedback. |
| **`GlyphTimerScreen.kt`** | Dedicated timer screen. Depletes the 24-segment arc clockwise as time elapses with audible countdown clicks and completion flashes. |
| **`GlyphPatternsScreen.kt`** | Ringtone choreography hub. Recreates the 12 signature Nothing Phone (2a) call choreographies (*Abra, Anna, Beetle, Clwb, Coded, Crossing, Dolphin, Hammer, Latency, Plot, Rapid, Vibrate*). |
| **`GlyphGeminiScreen.kt`** | Powered by Gemini 3.5 Flash. Generates new multi-frame LED patterns from text prompts and parses natural language voice commands (in English, Hindi, and Hinglish). |
| **`GlyphSettingsScreen.kt`** | Appearance customization (Dark/Light/System), 120Hz ProMotion switch, and integrated Root Sysfs Terminal for testing kernel drivers. |
| **`GlyphSoundSynth.kt`** | Custom procedural audio synthesizer using low-latency `AudioTrack` PCM generation (Sine, Square, Noise, FM sweeps) avoiding external audio asset dependencies. |
| **`GlyphHardwareManager.kt`** | Direct bridge to Linux kernel sysfs nodes via `su -c` commands to control actual AW210xx LED IC drivers on physical Nothing Phone (2a) hardware. |

---

## 🛠️ Nothing Phone (2a) Hardware & Driver Specifications

The Nothing Phone (2a) codenamed **`pacman`** uses an **AW210xx** LED Driver IC over I2C to control **3 LED strips divided into 26 channels**:

```
                              ┌─────────────────────────┐
                              │     NOTHING PHONE (2a)   │
                              │                         │
                              │    ┌──────────────┐     │
  [Strip 1: 24 Arc Segments] ─┼──> │ (📷)     (📷) │     │
  (/sys/class/leds/           │    │  \  24 ARC / │ <───┼── [Strip 2: Top-Right Slant]
   aw210xx_led/led_1..24)     │    └──────────────┘     │   (/sys/class/leds/aw210xx_led/led_25)
                              │                         │
                              │                         │
  [Strip 3: Bottom Slant] ────┼───────────\             │
  (/sys/class/leds/           │            \            │
   aw210xx_led/led_26)        │             \           │
                              └─────────────────────────┘
```

### Sysfs Node Mapping Table

| LED Strip | Channel / Index | Sysfs Path | Value Range |
| :--- | :--- | :--- | :--- |
| **Master White LEDs** | All 26 Channels | `/sys/class/leds/aw210xx_led/all_white_leds_br` | `0` - `4095` |
| **Strip 1 (Camera Arc)** | Channels `1` to `24` | `/sys/class/leds/aw210xx_led/led_1_br` ... `led_24_br` | `0` - `4095` |
| **Strip 2 (Top Slant)** | Channel `25` | `/sys/class/leds/aw210xx_led/led_25_br` | `0` - `4095` |
| **Strip 3 (Bottom Slant)**| Channel `26` | `/sys/class/leds/aw210xx_led/led_26_br` | `0` - `4095` |
| **Red Video Indicator** | Red LED | `/sys/class/leds/aw210xx_led/red_led_br` | `0` - `255` |
| **Brightness Scaling** | Global Scaling | `/sys/class/leds/aw210xx_led/brightness` | `0` - `255` |

---

## 🚀 How to Port & Implement on HyperOS / Custom ROMs

When porting HyperOS or custom AOSP ROMs to Nothing Phone (2a), follow these steps to enable full Glyph hardware control:

### 1. Kernel Drivers & Device Tree
Ensure your ported kernel has the AW210xx driver compiled:
```kconfig
CONFIG_LEDS_AW210XX=y
CONFIG_LEDS_AW210XX_NOTHING=y
```

### 2. Sysfs Permissions in `init.target.rc`
Add permission rules in your ROM's `init.target.rc` so the app and system services can write to the LEDs without requiring root:
```rc
on boot
    chown system system /sys/class/leds/aw210xx_led/all_white_leds_br
    chmod 0666 /sys/class/leds/aw210xx_led/all_white_leds_br

    chown system system /sys/class/leds/aw210xx_led/brightness
    chmod 0666 /sys/class/leds/aw210xx_led/brightness

    # Grant write permissions to all 26 LED channels
    chmod 0666 /sys/class/leds/aw210xx_led/led_*_br
    chown system system /sys/class/leds/aw210xx_led/led_*_br
```

### 3. SELinux Policy (`file_contexts` & `system_app.te`)
In `device/nothing/pacman/sepolicy/vendor/`:
```text
# file_contexts
/sys/devices/platform/.*aw210xx.*/leds/aw210xx_led(/.*)?    u:object_r:sysfs_glyph_leds:s0
```
```text
# system_app.te
allow system_app sysfs_glyph_leds:file rw_file_perms;
allow system_app sysfs_glyph_leds:dir r_dir_perms;
```

---

## 🤖 Gemini 3.5 AI Configuration

The application includes native Gemini 3.5 Flash integration. To use custom AI choreography:
1. Open the **Secrets panel in Google AI Studio**.
2. Add your `GEMINI_API_KEY`.
3. The app automatically injects the key into `BuildConfig.GEMINI_API_KEY` at compile time.
4. If no API key is provided, the app seamlessly falls back to procedural math choreographies so functionality is never blocked.

---

## 📦 Building and Exporting

### Compiling with Gradle:
```bash
# Debug APK
gradle :app:assembleDebug

# Release APK / AAB
gradle :app:assembleRelease
gradle :app:bundleRelease
```

### Universal Compatibility:
- **Min SDK**: `26` (Android 8.0 Oreo)
- **Target SDK**: `35` (Android 15 / 16 / 17 ready)
- **Architecture**: `arm64-v8a`, `armeabi-v7a`, `x86_64`

---

## 📄 License
This project is licensed under the **Apache License 2.0** — free to use, modify, port, and distribute for personal and commercial custom ROM projects.
