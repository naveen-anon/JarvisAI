# Security Policy

## API Keys
Never commit `local.properties` or any file containing `GEMINI_API_KEY`, `GROQ_API_KEY`, or `OPENWEATHER_API_KEY`.

Keys are injected at build time via `BuildConfig` from environment variables / GitHub Secrets.

## Permissions
The app requests sensitive permissions (microphone, contacts, SMS, call, camera, storage).
All offline commands that use them degrade gracefully with a spoken error if the permission is missing.

## Accessibility Service
Used only for optional auto-tap of the Send button in WhatsApp / Telegram and for the app-lock overlay.
It does not read passwords or send data off-device.

## PC Bridge
Listens only on the local Wi-Fi network (port 8765). Do not expose it to the public internet.

## Reporting
Open a GitHub issue or contact the maintainer for security concerns.
