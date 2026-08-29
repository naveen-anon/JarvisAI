# 🛠️ CHANGES.md — Bugs Fixed + Features Added

This document lists what was broken, what was fixed, and what was added across passes.

---

## 🔄 Documentation sync pass

README and the website were updated to catch up with features that had landed in code but
weren't documented anywhere yet:

- **Telegram messaging** — same pre-filled-chat pattern as WhatsApp (`telegram_message` action,
  Hindi + English word order both supported).
- **Voice authentication** — optional on-device voiceprint enrollment/check (Settings screen),
  gates command execution once per app session rather than per command.
- **Home screen widget** — one-tap listening trigger without opening the app.
- **Lock-screen quick action** — a "🎙 Listen" button on the persistent notification, tappable
  without unlocking the device.
- **Feedback screen** — sends star rating + message straight to the `jarvis-site` Cloudflare
  Worker's feedback store, visible in that project's own `/admin` dashboard.
- **Smart App Navigator** — handles compound commands like "open Flipkart and search watches
  under 500" instead of just plain app-launch commands.
- **Dual cloud brain** — Groq (fast, free-tier Llama-based) added alongside Gemini as a second
  online fallback option.
- **Liquid glass UI** — translucent frosted-panel styling (semi-transparent fill + top-edge
  specular highlight) applied across the HUD, Settings, Stats, Feedback, and Chat screens.
- Fixed: arc reactor accent color chosen in Settings wasn't reflected on the HUD until a full
  app restart — `MainActivity.onResume()` now re-applies it every time the screen becomes
  visible again, not just once in `onCreate()`.
- Fixed: a Kotlin compile error in `LockScreenActivity` (`setStroke(3, 0x8000E5FF)`) — the hex
  literal exceeds `Int.MAX_VALUE` and defaults to `Long`, which doesn't match either `setStroke`
  overload. Fixed with an explicit `.toInt()`.

---

## 🔄 Pass 6 — Soft-voice STT + WhatsApp command fix

### Soft / quiet voice listening (`voice/SpeechToText.kt`)
Default recognizer settings cut off soft speakers too early and only kept a single
hypothesis. Tuned for dimmer speech:

- `EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS` = **2000** (wait longer before ending)
- `EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS` = **1000**
- `EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS` = **400** (still accept short quiet commands)
- `EXTRA_MAX_RESULTS` = **5**, pick the **longest** non-blank transcript
- Extra wake variant: `jarviz`

Hardware mic gain still cannot be raised from app code — keep the phone close and prefer
`hi-IN` / `en-IN` in system speech settings for best results.

### WhatsApp message phrases (`brain/OfflineBrain.kt`, `executor/CommandExecutor.kt`)
Chat phrase like **"WhatsApp pe XploitNinja ko sms kro hlo"** was matching the plain SMS
path with a null body → `"Missing recipient or message."`

Fixed with flexible offline patterns (no forced `ki` / `saying`):

- `whatsapp pe NAME ko sms/message kro MESSAGE`
- `NAME ko whatsapp pe sms kro MESSAGE`
- English: `whatsapp NAME saying …`, `message NAME on whatsapp saying …`

`CommandExecutor.whatsappMessage()` opens WhatsApp / `wa.me` with text pre-filled.
**Silent auto-send is intentionally impossible** on consumer WhatsApp; user taps Send once.
10-digit numbers default to India (`91…`) for `wa.me`.

---

## 🔄 Pass 5 — Build fix, HUD polish, text chat

### Kotlin compile fix (`settings/StatsActivity.kt`)
CI failed on `:app:compileDebugKotlin` with illegal string escapes (`\(` / `\)`) and
broken template tokens on the "Using Jarvis for N days" line. Replaced with proper
Kotlin string templates: `${stats.firstUsedDaysAgo}` and
`${if (stats.firstUsedDaysAgo != 1) "s" else ""}`.

### HUD chrome (`themes.xml`, `AndroidManifest.xml`, `HudOverlayView.kt`)
- Default `Theme.AppCompat.DayNight` showed a grey ActionBar that clashed with the cyan HUD.
  Added `Theme.Jarvis` (`Theme.AppCompat.NoActionBar`) with status/navigation bar and
  window background `#03080E`.
- Removed the four cyan corner brackets from `HudOverlayView.drawCornerBrackets()`.

### Text chat (`chat/ChatActivity.kt`, `activity_chat.xml`)
- **💬 CHAT** chip on the HUD opens `ChatActivity`.
- Bubble UI + EditText + SEND; same offline-first pipeline via
  `AssistantForegroundService.submitText()` → `handleUserSpeech()`.
- Replies tagged `[OFFLINE]` when from `OfflineBrain`; TTS still speaks.

### Public release download (`.github/workflows/build.yml`)
On push to `main`, CI publishes prerelease tag `latest` with `app-debug.apk` so the APK
is downloadable without logging into GitHub Actions artifacts.

---

## 🔄 Pass 4 — Crash fixes, on-device diagnostics, Settings + Stats screens

### Real crash found and fixed
`NetworkStatusManager.getSignalLabel()` called `WifiManager.getConnectionInfo()` without
`ACCESS_WIFI_STATE` — `SecurityException` every few seconds on the HUD. Added the
permission and a `try/catch` fallback label.

### On-device crash reporter (`diagnostics/CrashHandler.kt`, `JarvisApplication.kt`)
Global `UncaughtExceptionHandler` writes stack traces to a local file; `MainActivity`
shows a copyable dialog on next launch (no adb/root).

### Hindi word-order call/SMS (`OfflineBrain.kt`)
Natural order **"Ashu ko call karo"** matched incorrectly before; Hindi-order regex runs
first. Same for SMS. "wifi" also matches "wi-fi" / "wi fi".

### TTS voice settings (`TextToSpeechHelper.kt`)
Re-reads `SettingsManager` pitch/rate before every utterance so "change voice" /
"speak faster" actually apply.

### Settings + Usage Stats screens
Visual controls for voice, name, notes; dashboard for streak, totals, top apps/contacts.
`recordInteraction()` wired from the service.

---

## 🔄 Pass 3 — Vision + Phase 5 features

- **Vision:** CameraX + ML Kit OCR / objects / faces (on-device only).
- **App lock:** PIN hash + Accessibility overlay `LockScreenActivity`.
- **PC bridge:** TCP port 8765, same pipeline as voice.
- **Web search / weather by voice / daily summary notification.**
- `processSpeech()` shared by voice + PC bridge.

---

## 🔄 Pass 2 — Auto Learn + volume/music/alarm/timer

- Wired dead `AutoLearnEngine` into `OfflineBrain`.
- Volume, media keys, alarm, timer offline commands.
- Whole-word `mute`; Hindi keywords; recognizer locale = device default.

---

## 🐛 Pass 1 — Core reliability (summary)

1. Offline brain existed but was never called — service went straight to Gemini.  
2. No-internet Gemini calls crashed the process — offline-first + try/catch.  
3. Real `isOnline()` (INTERNET + VALIDATED).  
4. TTS language set before engine ready — moved into `onInit` + queue.  
5. UI never bound to service — `AssistantListener` wired.  
6. Notes in memory map — replaced with `PersistentMemory` (SharedPreferences).  
7. Hardcoded weather API key — moved to BuildConfig / CI secrets.  

---

## ☁️ jarvis-backend (Cloudflare Worker)

Separate Worker under `jarvis-backend/`:

| Route | Purpose |
|---|---|
| `POST /command` | Gemini proxy with per-device daily rate limit (KV) |
| `GET /updates` | Public product updates list |
| `POST /updates` | Admin create (header `x-admin-token`) |
| `DELETE /updates` | Admin delete by index |

Bindings: `RATE_LIMIT_KV`, `UPDATES_KV`. Secrets: `GEMINI_API_KEY`, `ADMIN_TOKEN`.

---

## ⚠️ Build verification

Prefer GitHub Actions (`assembleDebug` on `main`) for a real compile. Locally:

```bash
./gradlew clean assembleDebug
```
