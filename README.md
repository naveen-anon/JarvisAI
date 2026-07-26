# 🤖 Jarvis AI — Offline-First Android Voice Assistant

A voice-controlled Android assistant that runs most commands **entirely on-device** —
no internet required for app control, calls, texts, settings, time/date, math, or notes.
When it doesn't recognize something locally *and* you're online, it falls back to
Google's Gemini API for general conversation.

---

## ✨ Features

- 🎙️ **Wake word activation** — say "Jarvis" to trigger listening
- ⚡ **Fully offline command engine** — works in airplane mode
- ☁️ **Cloud fallback** — Gemini API handles anything the offline brain can't
- 🔵 **Arc Reactor–style HUD** — animated, state-aware UI (idle / listening / thinking / speaking)
- 🔒 **Crash-safe** — permission denials and network failures degrade gracefully instead of crashing
- 💾 **Persistent memory** — notes survive app restarts and reboots
- 🛰️ **Live system HUD** — clock, battery, network signal, CPU/RAM, location, weather

---

## 🧠 What Works Offline (No Internet Needed)

| Category | Example commands |
|---|---|
| **Apps** | "open camera", "open whatsapp", "launch settings", "open [any installed app]" |
| **Calls & SMS** | "call mom", "text john saying I'm on my way" |
| **Device settings** | "turn on flashlight", "wifi", "bluetooth", "airplane mode" |
| **Time & date** | "what time is it", "what's today's date" |
| **Battery** | "what's my battery", "battery level" |
| **Math** | "what is 45 plus 20", "20 divided by 4" |
| **Notes** | "remember to call the plumber", "what did you remember", "forget that" |
| **Small talk** | "good morning jarvis", "how are you", "who are you", "help" |

Anything not recognized above is sent to **Gemini** — but only if the device has a
validated internet connection. Fully offline and unrecognized? Jarvis says so honestly
instead of hanging or crashing.

---

## 🏗️ Architecture
Voice input (SpeechToText)
|
v
AssistantForegroundService
|
+-> OfflineBrain.handle(speech)     <- tried FIRST, zero network calls
|        |
|        +- matched  -> CommandExecutor runs it, reply returned
|        +- no match -> returns null
|
+-> if null AND NetworkStatusManager.isOnline():
GeminiClient.getCommand(speech) -> CommandExecutor.execute(...)
else:
"I'm offline and don't have a local command for that yet."
|
v
TextToSpeechHelper.speak(reply)
|
v
MainActivity (AssistantListener) updates the Arc Reactor HUD, transcript, and response text
**Key design principle:** the offline brain is tried before the cloud, every time. Gemini
is a fallback, not the primary path — this is what keeps the assistant responsive and
crash-free with no connectivity.

---

## 📂 Project Structure
JarvisAI/
├── app/src/main/java/com/jarvis/assistant/
│   ├── MainActivity.kt                  # UI, permissions, service binding, HUD updates
│   ├── brain/
│   │   ├── OfflineBrain.kt              # The on-device reasoning engine
│   │   └── BrainState.kt                # IDLE / LISTENING / THINKING / SPEAKING / ERROR
│   ├── service/
│   │   └── AssistantForegroundService.kt # Always-alive service, routes offline-first
│   ├── ai/
│   │   └── GeminiClient.kt              # Cloud fallback (online-only)
│   ├── executor/
│   │   └── CommandExecutor.kt           # Actually opens apps, places calls, sends SMS, toggles settings
│   ├── model/
│   │   └── AssistantCommand.kt          # Structured command data class + ActionType enum
│   ├── voice/
│   │   ├── SpeechToText.kt              # Wake-word + one-shot listening
│   │   └── TextToSpeechHelper.kt        # Spoken responses
│   ├── ui/
│   │   ├── ArcReactorView.kt            # Animated HUD centerpiece
│   │   ├── WaveformView.kt              # Listening animation
│   │   ├── HudOverlayView.kt            # Scanline/overlay effect
│   │   └── Typewriter.kt                # Animated text reveal
│   ├── util/
│   │   ├── PersistentMemory.kt          # SharedPreferences-backed notes
│   │   ├── NetworkStatusManager.kt      # isOnline() + signal label
│   │   ├── LocationHelper.kt            # Location for weather
│   │   ├── WeatherClient.kt             # OpenWeatherMap (online-only)
│   │   ├── SystemStatusManager.kt       # Clock + battery ticker
│   │   └── PerformanceMonitor.kt        # CPU/RAM usage
│   └── accessibility/
│       └── JarvisAccessibilityService.kt # Screen-reading hook (optional, manual enable)
├── app/build.gradle.kts                 # Dependencies, API key build config fields
├── .github/workflows/build.yml          # CI: builds a debug APK on every push
└── CHANGES.md                           # Detailed bug-fix changelog
---

## 🚀 Setup & Build

### Prerequisites
- Android Studio (or just a JDK 17 + Android SDK for command-line builds)
- A free Gemini API key from https://ai.google.dev/ (only needed for cloud fallback)
- Optional: a free OpenWeatherMap API key from https://openweathermap.org/api for the weather HUD

### Clone
```bash
git clone https://github.com/naveen-anon/JarvisAI.git
cd JarvisAI
export GEMINI_API_KEY="your_gemini_key_here"
export OPENWEATHER_API_KEY="your_openweather_key_here"   # optional
chmod +x gradlew
./gradlew assembleDebug
```
Output: app/build/outputs/apk/debug/app-debug.apk
### Install on a connected device
adb install app/build/outputs/apk/debug/app-debug.apk
