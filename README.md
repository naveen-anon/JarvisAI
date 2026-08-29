# 🤖 Jarvis AI — Offline-First Android Voice Assistant

A voice-controlled Android assistant that runs most commands **entirely on-device** —
no internet required for app control, calls, texts, WhatsApp drafts, settings, time/date,
math, or notes. When it doesn't recognize something locally *and* you're online, it falls
back to Google's Gemini API for general conversation.

**Public download (no login):**  
https://github.com/naveen-anon/JarvisAI/releases/latest/download/app-debug.apk

---

## ✨ Features

- 🎙️ **Wake word activation** — say "Jarvis" (also Hindi/romanized: जार्विस, jarwis, jaarvis, jarviz)
- 🔇 **Soft-voice listening** — longer end-of-speech silence + multi-candidate transcripts so quieter speech is less likely to be cut off
- ⚡ **Fully offline command engine** — works in airplane mode
- ☁️ **Cloud fallback** — Gemini API for anything the offline brain can't handle
- 🔵 **Arc Reactor HUD** — animated, state-aware UI (idle / listening / thinking / speaking)
- 💬 **Text chat** — type commands when you can't speak (same offline-first brain)
- 📱 **WhatsApp messages** — open chat with contact + pre-filled text (tap Send once; WhatsApp does not allow silent auto-send)
- 📞 **Calls & SMS** — English and Hindi word order ("priya ko call karo", "mummy ko sms bhejo")
- 🔒 **Crash-safe** — permission/network failures degrade to spoken errors, not crashes
- 🛠️ **On-device crash reporter** — next launch shows the stack trace on screen (no adb/root)
- 💾 **Persistent memory** — notes survive restarts
- 🛰️ **Live system HUD** — clock, battery, network, CPU/RAM, location, weather
- ⚙️ **Settings + Usage Stats** — voice, name, notes, streak, top apps/contacts
- 🎨 **Full-screen HUD theme** — no grey ActionBar; status bar matches `#03080E`
- 📷 **On-device vision** — OCR, object detection, face detection (ML Kit, no frames leave the device)
- 🔐 **App lock by voice** — PIN-gated apps via Accessibility Service
- 💻 **PC bridge** — same-Wi-Fi TCP commands on port 8765
- 📰 **Product updates API** — Cloudflare Worker + KV (`jarvis-backend`) for a public updates feed + admin posts
- 📲 **Telegram messages** — same pre-filled-chat pattern as WhatsApp
- 🎙️ **Voice authentication** — optional on-device voiceprint match, checked once per session
- 🧩 **Home screen widget** — one tap to start listening without opening the app
- 🔔 **Lock-screen quick action** — "🎙 Listen" button on the notification, tappable without unlocking
- 💬 **Feedback screen** — sends straight to the developer's `jarvis-site` backend
- 🧭 **Smart App Navigator** — handles compound commands like "open Flipkart and search watches under 500"
- 🤖 **Dual cloud brain** — Gemini or Groq (fast, free-tier Llama) as the online fallback
- 🌊 **Liquid glass UI** — translucent frosted panels with specular top-edge highlights, app-wide

---

## 🧠 What Works Offline (No Internet Needed)

| Category | Example commands |
|---|---|
| **Apps** | "open camera", "open whatsapp", "camera kholo" |
| **Calls** | "call mom", "priya ko call karo" |
| **SMS** | "text john saying I'm on my way", "mummy ko sms bhejo" |
| **WhatsApp** | "whatsapp pe Priya ko sms kro hello", "Priya ko whatsapp pe message bhejo ki aa raha hoon", "whatsapp mom saying I'm coming" |
| **Telegram** | "telegram pe Priya ko sms kro hello", "Priya ko telegram pe message bhejo ki aa raha hoon" |
| **Device settings** | "torch jalao", "wifi", "bluetooth", "airplane mode" |
| **Volume / music** | "awaaz badhao", "mute", "gaana chalao/roko", "next song" |
| **Alarm & timer** | "set alarm for 7:30", "5 minute ka timer laga do" |
| **Time / date / battery** | "what time is it", "battery level" |
| **Math** | "what is 45 plus 20" |
| **Notes** | "remember to call the plumber", "what did you remember" |
| **Small talk** | "good morning", "how are you", "help" |
| **Vision** | "read this text", "what do you see", "how many faces" |
| **Web search** | "search for nearest hospital" *(opens browser; needs network for results)* |
| **App lock** | "set my pin to 1234", "lock whatsapp" |
| **PC connect** | "connect to pc", "disconnect pc" |
| **Weather** | "mausam kaisa hai" *(needs network + location)* |
| **Routine** | "my routine", "meri routine" |

Anything not recognized offline is sent to **Gemini** only if the device has a *validated*
internet connection. Otherwise Jarvis says so honestly instead of hanging or crashing.

### WhatsApp note
Android apps cannot silently send WhatsApp messages without the official Business API.
Jarvis opens WhatsApp (or `wa.me`) with the contact and message **already typed** — you
tap **Send** once. Contact names are resolved from the phone book; 10-digit numbers are
treated as India (`+91`) when building the `wa.me` link.

---

## 🏗️ Architecture

```
Voice (SpeechToText, soft-voice tuned)  OR  Text chat (ChatActivity → submitText)
        |
        v
AssistantForegroundService
        |
        +-> OfflineBrain.handle(speech)     <- FIRST, zero network
        |        |
        |        +- matched  -> CommandExecutor (apps, call, SMS, WhatsApp, …)
        |        +- no match -> null
        |
        +-> if null AND NetworkStatusManager.isOnline():
                 GeminiClient.getCommand(speech) -> CommandExecutor
             else:
                 honest offline message
        |
        v
TextToSpeechHelper.speak(reply)
        |
        v
MainActivity / ChatActivity (AssistantListener) updates HUD + bubbles
```

Optional cloud pieces:

- **Gemini** — conversational / unrecognized commands  
- **OpenWeather** — live weather on HUD + voice  
- **jarvis-backend** (Cloudflare Worker) — rate-limited `/command` proxy + `/updates` feed  

---

## 📂 Project Structure

```
JarvisAI/
├── app/src/main/java/com/jarvis/assistant/
│   ├── MainActivity.kt
│   ├── chat/ChatActivity.kt              # Text chat
│   ├── brain/OfflineBrain.kt             # Offline commands (incl. WhatsApp phrases)
│   ├── service/AssistantForegroundService.kt  # submitText + processSpeech
│   ├── ai/GeminiClient.kt
│   ├── executor/CommandExecutor.kt       # whatsappMessage → wa.me / WhatsApp intent
│   ├── voice/
│   │   ├── SpeechToText.kt               # Soft-voice silence + multi-result STT
│   │   └── TextToSpeechHelper.kt
│   ├── ui/                               # ArcReactorView, Waveform, HudOverlay, Typewriter
│   ├── settings/                         # SettingsActivity, StatsActivity
│   ├── vision/                           # CameraX + ML Kit
│   ├── security/                         # App lock + PIN
│   ├── network/PcBridgeServer.kt
│   └── accessibility/JarvisAccessibilityService.kt
├── jarvis-backend/                       # Cloudflare Worker (Gemini proxy + updates KV)
├── docs/                                 # Optional static landing page
├── .github/workflows/build.yml           # assembleDebug + public "latest" release APK
├── README.md
└── CHANGES.md
```

---

## 🚀 Setup & Build

### Prerequisites
- JDK 17 + Android SDK (or Android Studio)
- Free [Gemini API key](https://ai.google.dev/) (cloud fallback only)
- Optional [OpenWeatherMap](https://openweathermap.org/api) key for weather

### Clone & keys
```bash
git clone https://github.com/naveen-anon/JarvisAI.git
cd JarvisAI
export GEMINI_API_KEY="your_gemini_key_here"
export OPENWEATHER_API_KEY="your_openweather_key_here"   # optional
```

### Local debug APK
```bash
chmod +x gradlew
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions
On every push to `main`, `.github/workflows/build.yml`:

1. Runs `./gradlew assembleDebug` with repo secrets  
2. Uploads the APK as a workflow artifact  
3. Publishes a **public** prerelease tag `latest` with `app-debug.apk`  

Share this URL in groups/channels (no GitHub login required to download):

```text
https://github.com/naveen-anon/JarvisAI/releases/latest/download/app-debug.apk
```

---

## 🔑 Required Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Voice input |
| `CALL_PHONE` | "Call [contact]" |
| `SEND_SMS` | System SMS |
| `READ_CONTACTS` | Resolve names for call / SMS / WhatsApp |
| `ACCESS_FINE/COARSE_LOCATION` | Weather |
| `CAMERA` | Vision / OCR |
| `ACCESS_WIFI_STATE` / `ACCESS_NETWORK_STATE` | HUD signal + online check |
| `MODIFY_AUDIO_SETTINGS` | Volume commands |
| `POST_NOTIFICATIONS` | Foreground service + daily summary |
| `FOREGROUND_SERVICE` / `_MICROPHONE` | Background assistant |

Accessibility Service (app lock / screen features): enable manually under  
**Settings → Accessibility → Jarvis**.

---

## 🛡️ Safety & Reliability

- Network and permission failures are caught; the service speaks an error instead of crashing.
- Notes live in `SharedPreferences` and survive process death.
- WhatsApp is draft-only (user confirms Send).
- PC bridge is **same-LAN only** — do not port-forward it to the public internet.
- `jarvis-backend` rate-limits Gemini proxy usage per device per day via KV.

See **CHANGES.md** for the full history of bugs fixed and features added.

---

## 🧩 Tech Stack

- Kotlin, Android SDK 34, Coroutines  
- SpeechRecognizer + TextToSpeech  
- CameraX + ML Kit (text / object / face)  
- Gemini API, OpenWeatherMap (optional)  
- Cloudflare Workers + KV (`jarvis-backend`)  
- GitHub Actions CI + public release APK  

---

## 📄 License

MIT — fork, modify, and build on it.

---

## 🙋 Contributing

New offline phrases belong in `OfflineBrain.kt`. Device side-effects belong in
`CommandExecutor.kt`. Pull requests welcome.

---

## 👤 Author

**[naveen-anon](https://github.com/naveen-anon)**
