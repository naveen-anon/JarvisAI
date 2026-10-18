JARVIS Piper voice pack (no Google / no ElevenLabs)
====================================================

1) Download a male English Piper voice, e.g. from:
   https://huggingface.co/rhasspy/piper-voices/tree/main/en/en_GB/northern_english_male/medium

   Files needed in this folder (assets/piper/):
   - en_GB-northern_english_male-medium.onnx
   - en_GB-northern_english_male-medium.onnx.json

2) espeak-ng-data:
   From Piper releases, copy the espeak-ng-data directory into:
   assets/piper/espeak-ng-data/

3) Native library:
   Build or obtain libpiper_jni.so for arm64-v8a (and armeabi-v7a if needed):
   app/src/main/jniLibs/arm64-v8a/libpiper_jni.so

   JNI exports expected by PiperTtsEngine:
   - Java_com_jarvis_assistant_voice_PiperTtsEngine_nativeInit
   - Java_com_jarvis_assistant_voice_PiperTtsEngine_nativeSynthesize
   - Java_com_jarvis_assistant_voice_PiperTtsEngine_nativeStop
   - Java_com_jarvis_assistant_voice_PiperTtsEngine_nativeRelease

4) Rebuild the APK. Logcat tag "PiperTTS" should say: Piper ready.

Without these files, Jarvis will not speak (by design — no Google/ElevenLabs fallback).
