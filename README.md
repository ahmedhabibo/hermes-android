# Hermes Android

A native Android companion app for [Hermes Agent](https://hermes-agent.nousresearch.com) self-hosted WebUI servers.

## Features

- **Session management** — Create, rename, pin, archive, and delete chat sessions
- **Real-time chat** — SSE streaming with token, reasoning, and tool event display
- **Model picker** — Fetches available models from `/api/models`, selectable per chat
- **File attachments** — Upload images/files via `/api/upload`, rendered inline with Coil
- **Search & filter** — Search sessions by title, pinned sessions sorted first
- **Memory viewer** — View Hermes memory entries from the session list
- **Auto-login** — Skips onboarding if credentials are already stored
- **Material 3 UI** — Jetpack Compose with Material 3 theming

## Screens

| Screen | Description |
|--------|-------------|
| Onboarding | Enter server URL + password to connect |
| Sessions | List of chat sessions with search, refresh, overflow menu |
| Chat | Streaming chat with model picker, file attach, image rendering |
| Memory | View Hermes agent memory entries |

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Networking**: OkHttp 4.x (REST + SSE)
- **Serialization**: kotlinx.serialization
- **Image Loading**: Coil 2.5.0
- **Navigation**: Compose Navigation
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35

## Build

### Prerequisites

- JDK 17
- Android SDK (commandline-tools)
- Gradle (or use the included wrapper)

### Debug Build

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android/sdk
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

```bash
./gradlew assembleRelease
```

## Architecture

```
app/src/main/kotlin/com/hermes/android/
├── HermesApp.kt              # Application class, OkHttpClient singleton
├── MainActivity.kt            # Single-activity entry point
├── data/
│   ├── auth/AuthStore.kt      # Credential storage (encrypted)
│   ├── models/                # Data classes (sessions, chat, SSE events)
│   └── networking/
│       ├── ApiClient.kt       # REST API client (all endpoints)
│       ├── SSEClient.kt       # Server-Sent Events streaming client
│       └── Endpoints.kt       # API endpoint definitions
└── ui/
    ├── chat/                  # Chat screen + ViewModel
    ├── sessions/              # Session list + ViewModel
    ├── onboarding/            # Server login screen
    ├── memory/                # Memory viewer screen
    ├── navigation/NavHost.kt  # Compose Navigation routes
    └── theme/                 # Material 3 theme
```

## Connecting to a Hermes Server

1. Install and run Hermes WebUI on your server
2. Open the app and enter the server URL (e.g., `https://my-server.com:3000`)
3. Enter the WebUI password
4. The app will authenticate and show your sessions

## CI

GitHub Actions workflow (`.github/workflows/build.yml`) runs `assembleDebug` on every push.

## License

MIT
