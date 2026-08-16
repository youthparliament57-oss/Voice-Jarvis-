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
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner

import com.example.ui.screens.JarvisOverlay
import com.example.ui.theme.JarvisTheme
import com.example.wakeword.WakeWordEngine
import com.example.gemini.GeminiLiveClient
import com.example.data.SettingsRepository
import com.example.AssistantState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

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
    private var geminiLiveClient: GeminiLiveClient? = null
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sessionTimeoutJob: Job? = null
    
    private var assistantState by mutableStateOf(AssistantState.IDLE)
    private var apiKey: String = ""

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        showOverlay()
        initializeDependencies()
        
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun initializeDependencies() {
        val settingsRepository = SettingsRepository(this)
        serviceScope.launch {
            apiKey = settingsRepository.apiKeyFlow.first() ?: ""
        }

        // Initialize Wake Word Engine
        wakeWordEngine = WakeWordEngine(this)
        wakeWordEngine.initialize()
        startWakeWordMode()
        
        // Listen for Wake Word -> Switch to Gemini Live
        serviceScope.launch {
            wakeWordEngine.wakeWordDetected.collect {
                if (assistantState == AssistantState.IDLE && apiKey.isNotEmpty()) {
                    Log.d("JarvisService", "Wake word heard. Triggering Gemini Live.")
                    wakeWordEngine.stopListening() // Pause wake word
                    startGeminiLiveSession()
                } else if (apiKey.isEmpty()) {
                    Log.e("JarvisService", "API Key not found. Cannot start Gemini.")
                }
            }
        }
    }

    private fun startWakeWordMode() {
        assistantState = AssistantState.IDLE
        geminiLiveClient?.disconnect()
        geminiLiveClient = null
        sessionTimeoutJob?.cancel()
        wakeWordEngine.startListening()
    }

    private fun startGeminiLiveSession() {
        if (geminiLiveClient != null) return // Already running
        
        geminiLiveClient = GeminiLiveClient(apiKey)
        
        serviceScope.launch {
            geminiLiveClient?.stateFlow?.collect { state ->
                resetSessionTimer()
                when (state) {
                    GeminiLiveClient.LiveState.CONNECTING -> {
                        assistantState = AssistantState.UNDERSTANDING // Show spinner
                    }
                    GeminiLiveClient.LiveState.CONNECTED,
                    GeminiLiveClient.LiveState.LISTENING -> {
                        assistantState = AssistantState.LISTENING // Show pulsating ring
                    }
                    GeminiLiveClient.LiveState.SPEAKING -> {
                        assistantState = AssistantState.SPEAKING // Show speaking animation
                    }
                    GeminiLiveClient.LiveState.ERROR,
                    GeminiLiveClient.LiveState.DISCONNECTED -> {
                        startWakeWordMode()
                    }
                }
            }
        }
        
        geminiLiveClient?.connect()
        resetSessionTimer()
    }

    private fun resetSessionTimer() {
        sessionTimeoutJob?.cancel()
        sessionTimeoutJob = serviceScope.launch {
            delay(SESSION_TIMEOUT_MS)
            Log.d("JarvisService", "Session timed out due to inactivity.")
            startWakeWordMode()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("InflateParams")
    private fun showOverlay() {
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
            y = 100
        }

        val contextThemeWrapper = android.view.ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_DayNight)
        composeView = ComposeView(contextThemeWrapper).apply {
            setViewTreeLifecycleOwner(this@JarvisService)
            setViewTreeViewModelStoreOwner(this@JarvisService)
            setViewTreeSavedStateRegistryOwner(this@JarvisService)
            setContent {
                JarvisTheme {
                    JarvisOverlay(state = assistantState)
                }
            }
        }

        windowManager.addView(composeView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        
        wakeWordEngine.destroy()
        geminiLiveClient?.disconnect()
        sessionTimeoutJob?.cancel()
        
        if (composeView != null) {
            windowManager.removeView(composeView)
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

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS is awake")
            .setContentText("Listening for wake word...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore
        get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
}
