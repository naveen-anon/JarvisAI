# Jarvis AI — Android Voice Assistant

## Architecture

```
Wake word (Porcupine, offline) → SpeechRecognizer (STT)
        → ClaudeClient (LLM parses intent → structured JSON)
        → CommandExecutor (executes: open app / call / sms / settings)
        → TextToSpeechHelper (speaks result back)
```

Everything routes through `AssistantForegroundService`, which stays alive as a
foreground service (`START_STICKY`) so Doze/battery-optimization doesn't kill it
mid-conversation.

## Setup

1. Open in Android Studio (Koala+ recommended, AGP 8.5).
2. Get an Anthropic API key from console.anthropic.com. Do **not** hardcode it —
   replace `API_KEY_PLACEHOLDER` in `AssistantForegroundService.kt` with a call to
   `EncryptedSharedPreferences`, or better, proxy the call through your own backend
   so the key never ships inside the compiled APK. Anyone can `jadx`/`apktool` a
   release build and pull a hardcoded string in minutes.
3. Grant runtime permissions on first launch (mic, call, SMS, contacts).
4. For system-level control (reading screen, tapping UI elements), enable
   Settings → Accessibility → Jarvis manually — this can't be auto-granted.

## Wake word ("Hey Jarvis") — wiring Porcupine

The skeleton currently exposes a manual "Talk to Jarvis" button for testing so you
can validate the STT → Claude → Executor → TTS pipeline without needing wake-word
setup first. To make it hands-free:

1. Sign up free at console.picovoice.ai, train a custom wake word ("Jarvis"),
   download the `.ppn` model file into `app/src/main/assets/`.
2. In `AssistantForegroundService.onCreate()`, initialize `PorcupineManager` with
   your access key + `.ppn` path, and set its wake-word callback to call
   `startListeningCycle()` instead of the button's `onClick`.
3. Start the Porcupine manager in `onStartCommand()` and stop it in `onDestroy()`.

Don't run SpeechRecognizer continuously as a substitute for wake-word detection —
it burns battery fast and Android's mic-access indicator will stay on constantly,
which is also a bad look to end users.

## Known constraints / things that will bite you

- **Android 10+ blocks direct WiFi/Bluetooth toggling** by apps — `CommandExecutor`
  deep-links to the relevant Settings panel instead of flipping it silently. Flashlight
  is the one thing you can toggle directly (`CameraManager.setTorchMode`).
- **Accessibility Service scope**: `flagRetrieveInteractiveWindows` + full node-tree
  reading is exactly the kind of permission Play Store manual review flags. If you're
  publishing (vs. sideloading for personal/research use), you'll need an in-app
  disclosure screen and a narrowly scoped justification, or expect rejection.
- **SMS/Call permissions** are also "sensitive permissions" under Play policy —
  same story, expect a policy declaration form if publishing.
- **Foreground service type `microphone`** requires the manifest declaration shown
  here on API 34+, or the service silently fails to start in foreground mode.

## Extending

- Swap `ClaudeClient`'s single-turn call for a stateful conversation (keep a message
  history list) if you want multi-turn context ("turn it off" referring to the
  previous command).
- Add function-calling instead of the hand-rolled JSON schema if you want the model
  to chain multiple actions per utterance.
- For fully offline operation, replace `ClaudeClient` with an on-device quantized
  model via llama.cpp (JNI bindings) — trade-off is much weaker intent parsing on
  phone-class hardware.
