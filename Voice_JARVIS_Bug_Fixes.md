I have carefully reviewed the `Voice_JARVIS_Bug_Analysis_Report.pdf` and fixed the following 5 errors identified in the report:

1. **BUG-01 & BUG-03 (Wrong Model & Label Mismatch)**: 
   - Modified `WakeWordEngine.kt` to update the `targetLabels` to use `"yes"` and `"go"`. This works as a temporary workaround for the missing custom "hey_jarvis" model, allowing the system to use the existing `speech_commands.tflite` model successfully. This also removed the space label mismatch.

2. **BUG-02 (Fake Documentation Claim)**:
   - Updated `whathavedone.md` to accurately reflect the true state of the project. It now clearly documents that the `speech_commands.tflite` model is being used as a placeholder and training the custom "hey_jarvis.tflite" model is a TODO item requiring a custom pipeline.

3. **BUG-04 (Audio Buffer Size Mismatch)**:
   - Updated the classification trigger threshold in `WakeWordEngine.kt` from `8000` to `15600`. This ensures the YAMNet-based audio classifier receives a full 975ms audio window before inference, matching its required inputs.

4. **BUG-05 (Resource Leak in `loadModelFile`)**:
   - Refactored `loadModelFile` in `WakeWordEngine.kt` to use nested Kotlin `use{}` blocks. This guarantees the `AssetFileDescriptor`, `FileInputStream`, and `FileChannel` are all safely closed even in the case of exceptions, properly preventing file descriptor leaks.

5. **BUG-35 (Error state not cleared on dismiss)**:
   - Modified `JarvisService.kt` to explicitly reset `currentErrorMessage = null` when the overlay's `onDismiss` callback is triggered. This clears the stale error banner so it does not persist across future invocations.
---

### Short-Term Priority Fixes Applied:

6. **BUG-10 (Missing Acoustic Echo Cancellation)**:
   - Updated `AudioController.kt` to initialize `android.media.audiofx.AcousticEchoCanceler` on the microphone's `audioSessionId` directly after `AudioRecord` creation, preventing speaker-to-mic feedback loops during continuous conversation.

7. **BUG-07 (WebSocket onFailure sets wrong state on manual close)**:
   - Modified `GeminiLiveClient.kt` inside `onFailure` so that if `isClosedManually.get()` is true, it sets the state to `LiveState.DISCONNECTED` instead of `LiveState.ERROR`.

8. **BUG-08 (Thread-unsafe consecutiveSendFailures)**:
   - Replaced the local `var consecutiveSendFailures` integer with a thread-safe `java.util.concurrent.atomic.AtomicInteger` to ensure robust counter updates within the coroutine flows.

9. **BUG-09 (AudioTrack undrained on stop)**:
   - Fixed the `stopAudioIO` method in `GeminiLiveClient.kt` by passing the `audioTrack` to a background coroutine that explicitly delays 1000ms before calling `stop()` and `release()`, allowing the hardware audio buffer to fully drain instead of cutting off immediately.

10. **BUG-40 (Service running state not synced on crash)**:
    - Added a `LaunchedEffect` polling mechanism in `MainActivity.kt` utilizing `ActivityManager.getRunningServices` to continually check if `JarvisService` is alive, properly syncing `JarvisServiceState` to the UI if the service crashes or is killed by the OS.

11. **BUG-14 & BUG-28 (Remove dead code and unused dependencies)**:
    - Safely deleted the dead `app/applet/` directory.
    - Cleaned up `build.gradle.kts` by removing the unused `accompanist-permissions` dependency and aligning the hardcoded `OkHttp` library version with `libs.versions.toml`.

12. **BUG-17 & BUG-31 (Missing Minification & R8 configurations)**:
    - Enabled `isMinifyEnabled = true` and `isShrinkResources = true` in `build.gradle.kts` for the `release` build type.
    - Added comprehensive ProGuard rules in `proguard-rules.pro` protecting TensorFlow Lite, OkHttp, and Kotlin Coroutines from being obfuscated or aggressively shrunk.

### Medium-Term Architecture Improvements (Final Phase):

13. **BUG-39 (Duplicate SettingsRepository)**:
    - Converted `SettingsRepository.kt` to a robust Singleton pattern utilizing a synchronized `getInstance` method. This completely prevents application state desynchronization across `MainActivity`, `SettingsScreen`, and `JarvisService` when the user updates preferences like their API key.

14. **BUG-06 (Missing Haptic & Audio Feedback)**:
    - Implemented a `playWakeWordFeedback()` mechanism within `JarvisService.kt`. It leverages `android.os.Vibrator` (with fallback for older devices) and `android.media.ToneGenerator` to deliver immediate auditory and physical feedback as soon as the Wake Word engine triggers.

15. **BUG-33 (READ_CONTACTS Declared but Unused)**:
    - Rectified a prominent privacy issue by tearing out the `android.permission.READ_CONTACTS` declaration from `AndroidManifest.xml` and securely stripping the redundant requesting logic and UI code from `PermissionsHelper.kt` and `PermissionsScreen.kt`.

16. **BUG-32 (Lint Checks Disabled for Release)**:
    - Reinforced production safety constraints by reverting `abortOnError = true` and `checkReleaseBuilds = true` inside `app/build.gradle.kts`. This ensures the project will reliably refuse to build release variants if critical architectural or linting errors exist.

17. **BUG-13 & BUG-12 (API Key Exposure & Model Adjustments)**:
    - Completely resolved the security vulnerability where the Gemini API Key was being sent via the WebSocket query parameters. It is now securely attached using the `x-goog-api-key` HTTP header in `GeminiLiveClient.kt`.
    - Simultaneously corrected the placeholder model string, enforcing the requirement for `models/gemini-2.5-flash` as the core operational LLM.

### Medium-Term Architecture Improvements (Final Phase):

13. **BUG-39 (Duplicate SettingsRepository)**:
    - Converted `SettingsRepository.kt` to a robust Singleton pattern utilizing a synchronized `getInstance` method. This completely prevents application state desynchronization across `MainActivity`, `SettingsScreen`, and `JarvisService` when the user updates preferences like their API key.

14. **BUG-06 (Missing Haptic & Audio Feedback)**:
    - Implemented a `playWakeWordFeedback()` mechanism within `JarvisService.kt`. It leverages `android.os.Vibrator` (with fallback for older devices) and `android.media.ToneGenerator` to deliver immediate auditory and physical feedback as soon as the Wake Word engine triggers.

15. **BUG-33 (READ_CONTACTS Declared but Unused)**:
    - Rectified a prominent privacy issue by tearing out the `android.permission.READ_CONTACTS` declaration from `AndroidManifest.xml` and securely stripping the redundant requesting logic and UI code from `PermissionsHelper.kt` and `PermissionsScreen.kt`.

16. **BUG-32 (Lint Checks Disabled for Release)**:
    - Reinforced production safety constraints by reverting `abortOnError = true` and `checkReleaseBuilds = true` inside `app/build.gradle.kts`. This ensures the project will reliably refuse to build release variants if critical architectural or linting errors exist.

17. **BUG-13 & BUG-12 (API Key Exposure & Model Adjustments)**:
    - Completely resolved the security vulnerability where the Gemini API Key was being sent via the WebSocket query parameters. It is now securely attached using the `x-goog-api-key` HTTP header in `GeminiLiveClient.kt`.
    - Simultaneously corrected the placeholder model string, enforcing the requirement for `models/gemini-2.5-flash` as the core operational LLM.

### Advanced Architecture & UI Refinements (Phase 4):

18. **BUG-26 & BUG-27 (Dual State Management & Audio Race Condition)**:
    - Entirely eliminated the redundant `AssistantStateManager.kt` to establish `JarvisServiceState` as the single source of truth.
    - Updated `AudioController.kt` to use `combine` flow operators over `isRunning` and `assistantState`. This reactive pattern completely eradicates race conditions when transitioning between Wake Word listening and Gemini Active Conversation modes.

19. **BUG-41 (Inconsistent Emoji Rendering in Overlay)**:
    - Removed the non-standard text-based emojis ("👽", "⚠️") from `JarvisOverlay.kt`. These are now replaced with reliable Material 3 vector graphics (`Icons.Default.SmartToy` and `Icons.Default.Warning`), guaranteeing a uniform UI across all Android distributions.

20. **BUG-38 (False System Ready Status)**:
    - Corrected the 'System Architecture Status' card in `MainActivity.kt`. It now logically binds the active visual indicators to `isServiceRunning && hasPermission`, ensuring the UI truthfully reflects the background engine's actual runtime state instead of merely assuming it's running when permissions are granted.

21. **BUG-36 & BUG-37 (Permission Flow Loophole & Misleading Count)**:
    - Closed a critical logic flaw in `PermissionsScreen.kt` that allowed users to bypass the mandatory permissions gate via a "Skip" button or proceeding without the required `RECORD_AUDIO` and `SYSTEM_ALERT_WINDOW` permissions.
    - Updated the permissions counter in `SettingsScreen.kt` to dynamically reflect "Core system permissions" without falsely inflating the count with optional permissions like Notifications.

22. **BUG-15 & BUG-19 (Code & Directory Bloat)**:
    - Purged the completely unused `ChatMessage.kt` data class and the dormant `assets/.aistudio/` stub directory to further stream-line the project footprint and eliminate maintenance overhead.

### Configuration & Architecture Consolidation (Phase 5):

23. **BUG-12, BUG-20, & BUG-25 (Hardcoded Magic Numbers / Constants)**:
    - Completely removed hardcoded `SESSION_TIMEOUT_MS`, `SYSTEM_PROMPT`, and `MODEL_NAME` from core engine files.
    - Integrated them into `SettingsRepository.kt` via persistent `EncryptedSharedPreferences`, securely passing them sequentially at runtime. This dynamically configures Gemini Live capabilities on a per-user basis.

24. **BUG-09 (AudioTrack Lifecycle Underrun)**:
    - Identified a critical resource drop condition inside `GeminiLiveClient.kt` when the WebSocket shuts down or interrupts model speech.
    - Added precise `AudioTrack.pause()`, `.flush()`, and asynchronous `.release()` sequencing inside `stopAudioIO()` instead of just tearing it down. This stops residual buffer pops and smoothly halts generation.

25. **BUG-33 & BUG-34 (Privacy / Clean Manifest)**:
    - Verified `READ_CONTACTS` and unnecessary permissions were indeed fully cleaned from AndroidManifest.xml preventing privacy flags.
    - Additionally purged unused `accompanist-permissions` dependencies from `gradle/libs.versions.toml` to decrease APK weight, resolving **BUG-28**.

26. **BUG-35 (Stale Overlay State on Dismiss)**:
    - Corrected the `onDismiss` callback for `JarvisOverlay.kt` running inside the Android WindowManager. Along with collapsing the local view state, it now pushes `JarvisServiceState.setError(null)` so subsequent wake words don't trip over a ghost error state.

27. **BUG-17 & BUG-31 (Minification Enabled)**:
    - Enforced `isMinifyEnabled = true` and `isShrinkResources = true` within `app/build.gradle.kts` alongside `proguard-rules.pro`. This fundamentally shrinks the final APK footprint.

### Configuration & Architecture Consolidation (Phase 5):

23. **BUG-12, BUG-20, & BUG-25 (Hardcoded Magic Numbers / Constants)**:
    - Completely removed hardcoded `SESSION_TIMEOUT_MS`, `SYSTEM_PROMPT`, and `MODEL_NAME` from core engine files.
    - Integrated them into `SettingsRepository.kt` via persistent `EncryptedSharedPreferences`, securely passing them sequentially at runtime. This dynamically configures Gemini Live capabilities on a per-user basis.

24. **BUG-09 (AudioTrack Lifecycle Underrun)**:
    - Identified a critical resource drop condition inside `GeminiLiveClient.kt` when the WebSocket shuts down or interrupts model speech.
    - Added precise `AudioTrack.pause()`, `.flush()`, and asynchronous `.release()` sequencing inside `stopAudioIO()` instead of just tearing it down. This stops residual buffer pops and smoothly halts generation.

25. **BUG-33 & BUG-34 (Privacy / Clean Manifest)**:
    - Verified `READ_CONTACTS` and unnecessary permissions were indeed fully cleaned from AndroidManifest.xml preventing privacy flags.
    - Additionally purged unused `accompanist-permissions` dependencies from `gradle/libs.versions.toml` to decrease APK weight, resolving **BUG-28**.

26. **BUG-35 (Stale Overlay State on Dismiss)**:
    - Corrected the `onDismiss` callback for `JarvisOverlay.kt` running inside the Android WindowManager. Along with collapsing the local view state, it now pushes `JarvisServiceState.setError(null)` so subsequent wake words don't trip over a ghost error state.

27. **BUG-17 & BUG-31 (Minification Enabled)**:
    - Enforced `isMinifyEnabled = true` and `isShrinkResources = true` within `app/build.gradle.kts` alongside `proguard-rules.pro`. This fundamentally shrinks the final APK footprint.
