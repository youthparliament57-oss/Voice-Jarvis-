# JARVIS Implementation Plan - The 3 Pillars

This document outlines the engineering and implementation plan to build the JARVIS Assistant, focusing on combining three advanced systems into a single, seamless Android application.

## Pillar 1: The Wake Word System (Always-On, Low Power)
**Goal:** A reliable, zero-touch activation mechanism that runs locally without draining the battery or compromising privacy.

### Core Technologies
*   **Engine:** `openWakeWord` (an open-source TFLite-based wake word engine) or a custom TensorFlow Lite (`.tflite`) audio classification model. Completely free, no API keys required.
*   **Android API:** `AudioRecord` / `MediaRecorder` running inside an Android `ForegroundService`.

### Implementation Steps
1.  **Audio Pipeline Setup:** Initialize a continuous audio buffer that records in short chunks (e.g., 512 frames) and immediately discards them if no wake word is detected.
2.  **Engine Integration:** Feed the audio chunks into the Wake Word engine specifically trained for the word "JARVIS".
3.  **State Management:** When confidence > 85%, trigger the transition from `IDLE` to `AWAKE`.
4.  **Haptic & Audio Feedback:** Play a subtle futuristic sound and trigger a haptic pulse to notify the user that JARVIS is listening.

---

## Pillar 2: Continuous Talk System (Gemini Live Style)
**Goal:** A stateful, full-duplex conversational interface that allows the user to talk naturally, interrupt the assistant, and multitask across the OS.

### Core Technologies
*   **Communication:** Stateful WebSockets (WSS) connecting to Gemini Live API / Multimodal LLM.
*   **Audio Processing:** WebRTC Voice Activity Detection (VAD) & Acoustic Echo Cancellation (AEC).
*   **UI/UX:** `SYSTEM_ALERT_WINDOW` (Floating Overlay) and Android `ForegroundService` (Microphone type).

### Implementation Steps
1.  **Foreground Overlay:** When the wake word triggers, launch a sleek, floating UI (the JARVIS pulsating ring) over the current screen using the Android Window Manager.
2.  **Continuous Session (The 3-Minute Loop):** 
    *   Open a WebSocket connection to the LLM. 
    *   Start a 3-minute inactivity timer. Every time the user speaks or JARVIS executes a task, reset the timer.
3.  **Full-Duplex & Barge-in:**
    *   Implement AEC so the microphone ignores JARVIS's own TTS output.
    *   Run local VAD. If JARVIS is speaking and the user starts talking, send a `flush`/`stop` command to the TTS engine immediately and capture the new user intent.
4.  **Neural TTS:** Ensure the generated response uses high-quality neural voice APIs with streaming capability (playing audio as chunks arrive, rather than waiting for the whole sentence).

---

## Pillar 3: Proactive Intelligence System (The "Brain")
**Goal:** An on-device background engine that tracks habits, detects patterns, and initiates conversations when relevant without being annoying.

### Core Technologies
*   **Data Collection:** Android `UsageStatsManager`, `ActivityRecognitionClient`.
*   **Background Processing:** Android `WorkManager`.
*   **Storage & Logic:** `Room Database` (SQLite) and local statistical algorithms.

### Implementation Steps
1.  **The Heartbeat Worker:** Schedule a `WorkManager` job to run periodically (e.g., every 30 mins) to fetch device usage stats (what apps were opened, for how long).
2.  **Local Memory Store (Room DB):**
    *   `AppUsageTable`: Timestamp, App Package, Duration.
    *   `EventTable`: Location changes, headphone plugs, time of day.
3.  **Pattern Detection Algorithm:** Run a local statistical script (like FP-Growth) overnight to find correlations (e.g., "7:30 AM + Headphones = Spotify").
4.  **The Proactive Scorer:** Before speaking, evaluate the context.
    *   *Score = (Confidence of Pattern) + (Time Relevance) - (Annoyance Penalty).*
5.  **LLM Prompt Injection:** If the score is > 80, silently ping the LLM: *"Context: User's morning routine detected. Prompt them warmly if they want to start their day."*
6.  **Proactive Wake:** JARVIS automatically enters the `AWAKE` state, plays a chime, and speaks the suggestion.

---

## System Synergy: The Combined Flow

Here is how the 3 pillars work together flawlessly:
1.  **Background (Pillar 3):** JARVIS is quietly learning your routine via `UsageStatsManager`.
2.  **Trigger (Pillar 1):** You say *"JARVIS"*. The local engine detects it.
3.  **Session Start (Pillar 2):** The floating UI appears. A WebSocket connects. You say, *"Mummy ko call lagao."*
4.  **Execution (Pillar 2):** JARVIS routes the intent to the Android Dialer API, makes the call, and says *"Calling Mummy."*
5.  **Continuous Loop (Pillar 2):** JARVIS doesn't die. He waits. 1 minute later, you say *"Aur YouTube par arijit singh chalao."* He does it instantly.
6.  **Proactive Interjection (Pillars 3 & 2):** The next day, you plug in your earphones at your exact usual time. Pillar 3 calculates a high score, triggers Pillar 2, and JARVIS proactively asks, *"Good morning sir, YouTube par Arijit Singh start karun?"*
