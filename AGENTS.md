# JARVIS Project - AI Agent Strict Instructions

This file contains persistent, non-negotiable rules for the AI Agent working on the JARVIS project. The system automatically injects these rules into the AI's context to prevent hallucinations, mistakes, and architectural deviations.

## 1. Architectural Constraints
- **Wake Word**: STRICTLY use TensorFlow Lite (`.tflite`) or `openWakeWord` for local wake word detection. DO NOT use Picovoice Porcupine or any engine requiring an API key or internet connection for wake word detection.
- **Continuous Conversation**: DO NOT use standard one-shot Speech-to-Text. The system MUST maintain a continuous session using WebSockets, WebRTC VAD (Voice Activity Detection), and AEC (Acoustic Echo Cancellation) to allow barge-in (interruptions).
- **Background Execution**: Core listening and processing MUST run in an Android `ForegroundService` (type="microphone") to prevent the OS from killing it.
- **Overlay UI**: Visual feedback must use `SYSTEM_ALERT_WINDOW` to draw a floating overlay over other apps, allowing multitasking.

## 2. Proactive Intelligence Constraints
- **Data Privacy**: All behavioral data (app usage, events) MUST be stored locally using Android `Room` Database (SQLite). DO NOT send usage logs to the cloud.
- **Data Source**: Rely on Android `UsageStatsManager` and `ActivityRecognitionClient` for tracking user context.
- **Scoring Engine**: Proactive suggestions must pass through a local mathematical scoring function (Confidence + Relevance - Annoyance) before pinging the LLM for a generated response.

## 3. Engineering & Code Quality
- **Tech Stack**: 100% Kotlin, Jetpack Compose for UI, Coroutines & Flow for asynchronous tasks.
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture principles.
- **No Placeholders**: Write fully functional, production-ready code. DO NOT leave `// TODO: implement this` or placeholder functions for core logic.
- **Permissions**: Implement a progressive permission request flow (Record Audio, Usage Access, Display Over Other Apps) gracefully.

## 4. API Keys & LLM Models (CRITICAL)
- **API Key UI**: API keys MUST NOT be hardcoded in the backend. The app MUST include a User Interface (Settings screen) where the user can manually input and save their Gemini API key. Store this key securely on the device (e.g., using `EncryptedSharedPreferences` or `DataStore`).
- **Gemini Models**: STRICTLY use `gemini-2.5-flash` or higher models for all LLM reasoning, continuous conversation, and proactive intelligence tasks. Do not use older models.
