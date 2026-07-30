# 🤖 Jarvis AI — Offline-First Android Voice Assistant

A voice-controlled Android assistant that runs most commands **entirely on-device** —
no internet required for app control, calls, texts, settings, time/date, math, or notes.
When it doesn't recognize something locally *and* you're online, it falls back to
Google's Gemini API for general conversation.

---

## ✨ Features

- 🎙️ **Wake word activation** — say "Jarvis" to trigger listening (also recognizes Hindi/romanized variants)
- ⚡ **Fully offline command engine** — works in airplane mode
- ☁️ **Cloud fallback** — Gemini API handles anything the offline brain can't
- 🔵 **Arc Reactor–style HUD** — animated, state-aware UI (idle / listening / thinking / speaking)
- 🔒 **Crash-safe** — permission denials and network failures degrade gracefully instead of crashing
- 🛠️ **On-device crash reporter** — if something does crash, reopening the app shows the full stack trace right on screen (no PC, adb, or root needed to diagnose it)
- 💾 **Persistent memory** — notes survive app restarts and reboots
- 🛰️ **Live system HUD** — clock, battery, network signal, CPU/RAM, location, weather
- ⚙️ **Settings screen** — change voice type/speed, your name, and view/clear remembered notes, all visible and tappable instead of voice-only
- 📊 **Usage Stats screen** — total commands run, current day streak, most-used apps and contacts
- 💬 **Text chat** — type messages in a dedicated chat screen (same offline-first brain as voice)
- 🎨 **Full-screen HUD theme** — no grey ActionBar; status bar matches the dark cyan HUD

---

## 🧠 What Works Offline (No Internet Needed)

| Category | Example commands |
|---|---|
| **Apps** | "open camera", "open whatsapp", "launch settings", "open [any installed app]" (Hindi: "camera kholo") |
| **Calls & SMS** | "call mom", "please call priya", "text john saying I'm on my way" (Hindi order: "priya ko call karo", "mummy ko sms bhejo") |
| **Device settings** | "turn on flashlight", "wifi" / "wi-fi", "bluetooth", "airplane mode" (Hindi: "torch jalao") |
| **Volume** | "volume up/down", "mute", "full volume" (Hindi: "awaaz badhao", "awaaz kam karo") |
| **Music** | "play music", "pause song", "next song", "previous song" (Hindi: "gaana chalao/roko") |
| **Alarm & timer** | "set alarm for 7:30", "set a timer for 5 minutes" (Hindi: "alarm laga do") |
| **Time & date** | "what time is it", "what's today's date" |
| **Battery** | "what's my battery", "battery level" |
| **Math** | "what is 45 plus 20", "20 divided by 4" |
| **Notes** | "remember to call the plumber", "what did you remember", "forget that" |
| **Small talk** | "good morning jarvis", "how are you", "who are you", "help" |
| **Voice customization** | "change voice to male/female/robot", "speak faster/slower" — takes effect on the very next thing Jarvis says. Also adjustable from the in-app Settings screen. |
| **Auto Learn Mode** | Silently learns which apps/contacts you use at which times; ask "my routine" or "daily summary" for a recap, or Jarvis will proactively surface a learned habit once it has enough data |
| **Vision (on-device)** | "read this text" (OCR), "what do you see" (object detection), "how many faces" (face detection) — opens the camera scanner, all processed on-device via ML Kit |
| **Web search** | "search for nearest hospital", "google best biryani recipe" |
| **App lock** | "set my pin to 1234", then "lock whatsapp" / "unlock whatsapp" — PIN-gated per app via the accessibility service |
| **PC connect** | "connect to pc" opens a same-Wi-Fi bridge so a PC can send Jarvis text commands over a local socket; "disconnect pc" closes it |
| **Weather** | "what's the weather", "mausam kaisa hai" — uses your live location + OpenWeather |
| **Daily summary** | Jarvis posts a notification each evening with a recap of your day's habits, on top of the on-demand "my routine" command |

Anything not recognized above is sent to **Gemini** — but only if the device has a
validated internet connection. Fully offline and unrecognized? Jarvis says so honestly
instead of hanging or crashing.

---

## 🏗️ Architecture

```
Voice input (SpeechToText)  OR  Text chat (ChatActivity → submitText)
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
```

**Key design principle:** the offline brain is tried before the cloud, every time. Gemini
is a fallback, not the primary path — this is what keeps the assistant responsive and
crash-free with no connectivity.

---

## 📂 Project Structure

```
JarvisAI/
├── app/src/main/java/com/jarvis/assistant/
│   ├── MainActivity.kt                  # UI, permissions, service binding, HUD updates
│   ├── chat/
│   │   └── ChatActivity.kt              # Text chat UI (bubbles + EditText)
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
│   │   ├── HudOverlayView.kt            # Grid + particles (corner brackets removed)
│   │   └── Typewriter.kt                # Animated text reveal
│   ├── settings/
│   │   ├── SettingsActivity.kt          # Voice, name, notes
│   │   └── StatsActivity.kt             # Usage dashboard
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
```

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
```

### Set your API keys (never hardcode these in source)
```bash
export GEMINI_API_KEY="your_gemini_key_here"
export OPENWEATHER_API_KEY="your_openweather_key_here"   # optional
```

### Build a debug APK
```bash
chmod +x gradlew
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Install on a connected device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🤖 Building via GitHub Actions (no local Android SDK needed)

This repo includes `.github/workflows/build.yml`, which builds a debug APK automatically
on every push to `main`.

1. Add your keys as **repository secrets**:
   `Settings -> Secrets and variables -> Actions -> New repository secret`
   - `GEMINI_API_KEY`
   - `OPENWEATHER_API_KEY` (optional)
2. Push to `main`, or trigger manually from the **Actions** tab -> *Android Build* -> *Run workflow*.
3. Once the run finishes, download the APK from the **Artifacts** section of that run.

---

## 🔑 Required Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Voice input |
| `CALL_PHONE` | "Call [contact]" |
| `SEND_SMS` | "Text [contact] saying..." |
| `READ_CONTACTS` | Resolving contact names to phone numbers |
| `ACCESS_FINE/COARSE_LOCATION` | Weather lookup |
| `POST_NOTIFICATIONS` | Foreground service notification (Android 13+) |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MICROPHONE` | Keeps the assistant listening in the background |

Grant these when prompted on first launch. The **Accessibility Service** (for future
screen-reading features) must be enabled manually: `Settings -> Accessibility -> Jarvis`.

---

## 🛡️ Safety & Reliability Notes

- Every command that touches the network (Gemini, weather) is wrapped in try/catch —
  a dropped connection degrades to a spoken message, never a crash.
- Denied permissions (e.g. no `CALL_PHONE`) are caught as `SecurityException` and reported
  back conversationally instead of crashing the service.
- Notes ("remember...") are stored in `SharedPreferences`, so they survive process death —
  including when Android's battery optimization kills the background service.

See `CHANGES.md` for the full history of bugs found and fixed during development.

---

## 🧩 Tech Stack

- **Kotlin** + Android SDK (min/target as configured in `app/build.gradle.kts`)
- **Coroutines** for async work (`Dispatchers.Main` scope in the service)
- **Android SpeechRecognizer** + **TextToSpeech** for voice I/O
- **Gemini API** for cloud fallback reasoning
- **OpenWeatherMap API** for weather (optional)
- **GitHub Actions** for CI builds

---

## 📄 License

MIT — feel free to fork, modify, and build on this.

---

## 🙋 Contributing

Found a bug or want to add an offline command? `OfflineBrain.kt` is the place to add new
keyword matches — no cloud dependency required. Pull requests welcome.

---

## 👤 Author / Contributor

**[naveen-anon](https://github.com/naveen-anon)**

