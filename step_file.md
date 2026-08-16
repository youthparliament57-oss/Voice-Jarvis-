# JARVIS Project - Step-by-Step Execution Plan

This document breaks down the development of the JARVIS Assistant into small, manageable steps. 
**RULE:** We will execute ONLY ONE step at a time, verify it builds correctly (`compile_applet`), and then move to the next. Do not jump ahead.

## Step 1: Base Project Setup & Security
- [ ] Setup `build.gradle.kts` with all required dependencies (Compose, Room, TFLite, Coroutines, DataStore/EncryptedSharedPreferences).
- [ ] Define the MVVM folder structure (`ui`, `data`, `domain`, `services`, `utils`).
- [ ] Create the `DataStore` or `EncryptedSharedPreferences` logic to securely store the user's Gemini API Key.

## Step 2: UI & Permissions Layer
- [ ] Build the Settings Screen UI for the user to input and save the Gemini API Key.
- [ ] Build the Progressive Permission Manager (Microphone, Overlay, Usage Stats, Contacts).
- [ ] Refine the Main Screen UI (The pulsating JARVIS visualizer).

## Step 3: Foreground Service & Overlay System
- [ ] Create the `JarvisForegroundService` (type="microphone") to keep the app alive in the background.
- [ ] Implement `SYSTEM_ALERT_WINDOW` to create the floating JARVIS UI that appears over other apps.
- [ ] Connect the floating UI to the service lifecycle (Start/Stop).

## Step 4: The Wake Word Engine (TFLite)
- [ ] Integrate TensorFlow Lite for the wake word detection (`openWakeWord` or custom `.tflite`).
- [ ] Set up `AudioRecord` to capture audio chunks continuously and feed them to the TFLite model.
- [ ] Trigger an `AWAKE` state in the `JarvisForegroundService` when "JARVIS" is detected.

## Step 5: Continuous Conversation & Gemini 2.5 Flash
- [ ] Implement the WebSocket streaming client to connect with the Gemini API (using `gemini-2.5-flash`).
- [ ] Integrate WebRTC VAD (Voice Activity Detection) and AEC (Acoustic Echo Cancellation) to handle "Barge-in" (interruptions).
- [ ] Setup the 3-minute continuous session loop (The Idle/Awake/Listening state machine).
- [ ] Implement Neural TTS (Text-to-Speech) for JARVIS's voice output.

## Step 6: Intent & Action Layer (Android Integration)
- [ ] Build the Action Router.
- [ ] Implement native Android tools (e.g., `OpenAppTool`, `CallContactTool`).
- [ ] Connect the LLM's structured output to these native tools so JARVIS can actually *do* things.

## Step 7: Proactive Intelligence - Data Collection
- [ ] Set up the Android `Room Database` for local memory (`AppUsage`, `Patterns`).
- [ ] Implement `UsageStatsManager` to track what apps the user opens.
- [ ] Create a background `WorkManager` that quietly logs this data into the local Room DB every 30 minutes.

## Step 8: Proactive Intelligence - Analysis & Suggestion
- [ ] Write the local statistical pattern detector (e.g., finding routines).
- [ ] Implement the "Proactive Scorer" (Confidence + Relevance - Annoyance).
- [ ] Create the trigger mechanism: When a routine matches the current time and score is > 80, wake up the Overlay UI and speak to the user using the Gemini API.

## Final Step: Polish & Testing
- [ ] Extensive logging and debugging.
- [ ] UI/UX polishing (animations, haptics, sounds).
- [ ] Final end-to-end testing of the Listen → Understand → Act → Observe → Learn → Suggest loop.
