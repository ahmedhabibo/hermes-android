# Hermes Android

Android companion app for [Hermes Agent](https://github.com/NousResearch/hermes-agent) — connects to any self-hosted `hermes-webui` server.

## Features

- **Onboarding** — Server URL + password setup with `/health` check
- **Session List** — Browse, create, and open chat sessions with pull-to-refresh
- **Chat with SSE Streaming** — Real-time token streaming, tool-call display, cancel support
- **Secure Storage** — EncryptedSharedPreferences for server credentials

## Architecture

```
app/src/main/kotlin/com/hermes/android/
├── HermesApp.kt              # Application class (OkHttp + CookieJar)
├── MainActivity.kt           # Single-activity, Compose Navigation
├── config/AppConfig.kt       # Constants
├── data/
│   ├── auth/AuthStore.kt     # Encrypted credential storage
│   ├── models/
│   │   ├── Models.kt         # API request/response models
│   │   └── SSEEvent.kt      # SSE event types + payloads
│   └── networking/
│       ├── Endpoints.kt     # All hermes-webui API endpoints
│       ├── ApiClient.kt      # REST client (OkHttp + kotlinx.serialization)
│       └── SSEClient.kt      # SSE streaming client (EventSource)
└── ui/
    ├── theme/Theme.kt        # Material3 theme
    ├── navigation/NavHost.kt # Compose Navigation graph
    ├── onboarding/           # Server connection screen
    ├── sessions/             # Session list screen
    └── chat/                 # Chat screen with SSE streaming
```

## Building

```bash
# Requires JDK 17, Android SDK 35, Gradle 8.9
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android/sdk
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## API Compatibility

Port of the Hermex iOS app's API contract — compatible with `hermes-webui` endpoints:
- Auth: `/health`, `/api/auth/status`, `/api/auth/login`, `/api/auth/logout`
- Sessions: CRUD, search, branch, pin, archive
- Chat: `/api/chat/start`, SSE `/api/chat/stream`, cancel, steer
- Panels: models, providers, profiles, crons, skills, memory
