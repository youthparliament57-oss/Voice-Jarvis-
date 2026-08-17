package com.example.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.AssistantState
import com.example.data.SettingsRepository
import com.example.gemini.GeminiLiveClient
import com.example.ui.screens.JarvisOverlay
import com.example.ui.theme.JarvisTheme
import com.example.utils.PermissionsHelper

import com.example.wakeword.WakeWordEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


data class TranscriptMessage(val isUser: Boolean, val text: String, val timestamp: Long = System.currentTimeMillis())

object JarvisServiceState {
    private val _transcript = MutableStateFlow<List<TranscriptMessage>>(emptyList())
    val transcript = _transcript.asStateFlow()

    fun addTranscript(message: TranscriptMessage) {
        val current = _transcript.value.toMutableList()
        current.add(message)
        if (current.size > 50) current.removeAt(0) // Keep last 50 messages
        _transcript.value = current
    }

    fun clearTranscript() {
        _transcript.value = emptyList()
    }

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState = _assistantState.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    fun updateState(state: AssistantState) {
        _assistantState.value = state
    }

    fun updateRunning(running: Boolean) {
        _isRunning.value = running
    }

    fun setError(error: String?) {
        _lastError.value = error
    }

    fun clearError() {
        _lastError.value = null
    }
}

class JarvisService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val CHANNEL_ID = "JarvisServiceChannel"
        private const val NOTIFICATION_ID = 101
        private const val SESSION_TIMEOUT_MS = 120_000L // 2 minutes
    }

    private lateinit var windowManager: WindowManager
    private var composeView: View? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    private lateinit var wakeWordEngine: WakeWordEngine
    private val settingsRepository by lazy { SettingsRepository.getInstance(this) }
    private var geminiLiveClient: GeminiLiveClient? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sessionTimeoutJob: Job? = null

    private var assistantState by mutableStateOf(AssistantState.IDLE)
    private var currentErrorMessage by mutableStateOf<String?>(null)
    private var apiKey: String = ""
    private val pingReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.ACTION_PING_SERVICE") {
                JarvisServiceState.updateRunning(true)
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        com.example.audio.AudioController.init(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        JarvisServiceState.updateRunning(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pingReceiver, android.content.IntentFilter("com.example.ACTION_PING_SERVICE"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(pingReceiver, android.content.IntentFilter("com.example.ACTION_PING_SERVICE"))
        }

        JarvisServiceState.clearError()



        if (PermissionsHelper.hasOverlayPermission(this)) {
            showOverlay()
        } else {
            Log.w("JarvisService", "Overlay permission missing. Cannot show floating overlay.")
            JarvisServiceState.setError("Overlay permission missing. Enable 'Display Over Other Apps' in Settings.")
        }

        initializeDependencies()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun updateAssistantState(newState: AssistantState, errorMessage: String? = null) {
        assistantState = newState
        currentErrorMessage = errorMessage
        JarvisServiceState.updateState(newState)
        if (errorMessage != null) {
            JarvisServiceState.setError(errorMessage)
        }
        updateNotification(newState)


        // CONTROL OVERLAY VISIBILITY BASED ON STATE
        // When IDLE: Hide floating overlay view (View.GONE)
        // When LISTENING / UNDERSTANDING / SPEAKING / ERROR: Show floating overlay view (View.VISIBLE)
        composeView?.post {
            if (newState == AssistantState.IDLE) {
                composeView?.visibility = View.GONE
            } else {
                composeView?.visibility = View.VISIBLE
            }
        }
    }

    private fun initializeDependencies() {
        
        serviceScope.launch {
            apiKey = settingsRepository.apiKeyFlow.first() ?: ""
            if (apiKey.isEmpty()) {
                Log.w("JarvisService", "Gemini API key is not set in Settings repository.")
                JarvisServiceState.setError("Gemini API key missing. Enter your key in Settings.")
            }
        }

        wakeWordEngine = WakeWordEngine(this, wakeThreshold = settingsRepository.getWakeThreshold())
        wakeWordEngine.initialize()

        if (PermissionsHelper.hasAudioPermission(this)) {
            startWakeWordMode()
        } else {
            val err = "Microphone permission missing. Cannot start Wake-Word engine."
            Log.e("JarvisService", err)
            JarvisServiceState.setError(err)
        }


        serviceScope.launch {
            wakeWordEngine.wakeWordDetected.collect {
                playWakeWordFeedback()


                if (assistantState == AssistantState.IDLE) {
                    if (apiKey.isNotEmpty()) {
                        Log.d("JarvisService", "Wake word heard! Stopping WakeWord listener and starting Gemini Live.")
                        wakeWordEngine.stopListening()
                        delay(200) // Give Android HAL time to release hardware mic
                        startGeminiLiveSession()
                    } else {
                        val keyErr = "API Key missing! Please set your Gemini API key in Settings."
                        Log.e("JarvisService", keyErr)
                        Toast.makeText(this@JarvisService, keyErr, Toast.LENGTH_LONG).show()
                        updateAssistantState(AssistantState.ERROR, "Gemini API Key Missing")

                        serviceScope.launch {
                            delay(3000)
                            startWakeWordMode()
                        }
                    }
                }
            }
        }
    }

    private fun playWakeWordFeedback() {
        try {
            val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
                    serviceScope.launch {
            var toneGen: android.media.ToneGenerator? = null
            try {
                toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
                delay(200)
            } finally {
                toneGen?.release()
            }
        }
        } catch (e: Exception) {
            Log.e("JarvisService", "Feedback error", e)
        }
    }

    private fun startWakeWordMode() {
        
        updateAssistantState(AssistantState.IDLE)
        geminiLiveClient?.disconnect()
        geminiLiveClient = null
        sessionTimeoutJob?.cancel()

        if (PermissionsHelper.hasAudioPermission(this)) {

            serviceScope.launch {
                delay(200)
                wakeWordEngine.startListening()
            }
        } else {
            JarvisServiceState.setError("Microphone permission missing. Please grant Microphone access.")
        }
    }

    private fun startGeminiLiveSession() {
        
        if (geminiLiveClient != null) return

        if (!PermissionsHelper.hasAudioPermission(this)) {
            val err = "Microphone permission required for voice conversation!"
            Toast.makeText(this, err, Toast.LENGTH_LONG).show()
            updateAssistantState(AssistantState.ERROR, err)
            return
        }

        geminiLiveClient = GeminiLiveClient(context = this, apiKey = apiKey, modelName = settingsRepository.getModelName(), systemInstruction = settingsRepository.getSystemPrompt()) // 




        serviceScope.launch {
            geminiLiveClient?.stateFlow?.collect { state ->
                resetSessionTimer()
                when (state) {
                    is GeminiLiveClient.LiveState.IDLE -> {
                        updateAssistantState(AssistantState.IDLE)
                    }
                    is GeminiLiveClient.LiveState.CONNECTING -> {
                        updateAssistantState(AssistantState.UNDERSTANDING)
                    }
                    is GeminiLiveClient.LiveState.CONNECTED,
                    is GeminiLiveClient.LiveState.LISTENING -> {
                        updateAssistantState(AssistantState.LISTENING)
                    }
                    is GeminiLiveClient.LiveState.SPEAKING -> {
                        updateAssistantState(AssistantState.SPEAKING)
                    }
                    is GeminiLiveClient.LiveState.RECONNECTING -> {
                        Log.d("JarvisService", "Reconnecting to Gemini Live attempt ${state.attempt}/${state.maxAttempts}")
                        updateAssistantState(AssistantState.UNDERSTANDING)
                    }
                    is GeminiLiveClient.LiveState.ERROR -> {
                        val errMsg = state.message.ifBlank { "Voice Connection Error (Check API Key / Network)" }
                        updateAssistantState(AssistantState.ERROR, errMsg)

                        serviceScope.launch {
                            delay(3500)
                            startWakeWordMode()
                        }
                    }
                    is GeminiLiveClient.LiveState.DISCONNECTED -> {
                        startWakeWordMode()
                    }
                }
            }
        }

        
        serviceScope.launch {
            com.example.audio.AudioController.userSpeakingFlow.collect { isSpeaking ->
                if (isSpeaking && assistantState == AssistantState.SPEAKING) {
                    android.util.Log.d("JarvisService", "Local VAD Barge-in detected. Flushing playback.")
                    geminiLiveClient?.flushPlayback()
                    updateAssistantState(AssistantState.LISTENING)
                }
            }
        }
        
        geminiLiveClient?.connect()

        resetSessionTimer()
    }

    private fun resetSessionTimer() {
        sessionTimeoutJob?.cancel()
        sessionTimeoutJob = serviceScope.launch {
            delay(settingsRepository.getSessionTimeout())
            Log.d("JarvisService", "Session timed out due to inactivity. Returning to Wake Word listening.")
            startWakeWordMode()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "com.example.ACTION_SEND_TEXT") {
            val text = intent.getStringExtra("TEXT_QUERY")
            if (!text.isNullOrBlank()) {
                sendTextQuery(text)
            }
        }
        return START_STICKY
    }


    private fun sendTextQuery(text: String) {
        JarvisServiceState.addTranscript(TranscriptMessage(isUser = true, text = text))
        if (assistantState == AssistantState.IDLE) {

            // Wake up JARVIS first if sleeping
            serviceScope.launch {
                val apiKey = settingsRepository.getApiKey()
                if (!apiKey.isNullOrBlank()) {
                    playWakeWordFeedback()
                    startGeminiLiveSession()
                    delay(500) // Brief delay to let WebSocket connect
                    geminiLiveClient?.sendText(text)
                } else {
                    JarvisServiceState.setError("API Key Missing")
                }
            }
        } else {
            // Already connected
            geminiLiveClient?.sendText(text)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("InflateParams")
    private fun showOverlay() {
        if (!PermissionsHelper.hasOverlayPermission(this)) return

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = 120
            }

            val contextThemeWrapper = android.view.ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_DayNight)
            composeView = ComposeView(contextThemeWrapper).apply {
                setViewTreeLifecycleOwner(this@JarvisService)
                setViewTreeViewModelStoreOwner(this@JarvisService)
                setViewTreeSavedStateRegistryOwner(this@JarvisService)
                setContent {
                    JarvisTheme {
                        JarvisOverlay(
                            state = assistantState,
                            errorMessage = currentErrorMessage,
                            onDismiss = {
                                currentErrorMessage = null
                                JarvisServiceState.setError(null)
                                startWakeWordMode()
                            }
                        )
                    }
                }
                // Initially GONE when IDLE
                visibility = View.GONE
            }
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            Log.e("JarvisService", "Failed to add overlay view", e)
            JarvisServiceState.setError("Overlay launch failed: ${e.localizedMessage}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.audio.AudioController.destroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()

        try { unregisterReceiver(pingReceiver) } catch (e: Exception) {}
        JarvisServiceState.updateRunning(false)
        
        JarvisServiceState.updateState(AssistantState.IDLE)

        wakeWordEngine.destroy()
        geminiLiveClient?.disconnect()
        sessionTimeoutJob?.cancel()

        if (composeView != null && ::windowManager.isInitialized) {
            try {
                windowManager.removeView(composeView)
            } catch (_: Exception) {}
            composeView = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    
    private fun updateNotification(state: AssistantState) {
        val text = when (state) {
            AssistantState.IDLE -> "Listening for wake word..."
            AssistantState.LISTENING -> "JARVIS is listening..."
            AssistantState.UNDERSTANDING -> "JARVIS is thinking..."
            AssistantState.SPEAKING -> "JARVIS is speaking..."
            AssistantState.ERROR -> "JARVIS encountered an error."
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS Voice Assistant")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS AI Agent Active")
            .setContentText("Listening for wake word ('yes'/'go')...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
}
