package com.example.gemini

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import com.example.audio.AudioController

interface GeminiLiveListener {
    fun onStateChanged(state: GeminiLiveClient.LiveState)
    fun onError(message: String)
    fun onModelSpoke(pcmData: ByteArray)
}

class GeminiLiveClient(
    private val context: Context,
    private val apiKey: String,
    private val modelName: String = "models/gemini-3.1-flash-live-preview",
    private val listener: GeminiLiveListener? = null
) {
    sealed class LiveState {
        object IDLE : LiveState()
        object CONNECTING : LiveState()
        object CONNECTED : LiveState()
        object LISTENING : LiveState()
        object SPEAKING : LiveState()
        data class RECONNECTING(val attempt: Int, val maxAttempts: Int) : LiveState()
        data class ERROR(val message: String) : LiveState()
        object DISCONNECTED : LiveState()
    }

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(12, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    
    private var clientJob = SupervisorJob()
    private var clientScope = CoroutineScope(Dispatchers.IO + clientJob)

    private var audioPlaybackJob: Job? = null
    private var audioRecordJob: Job? = null

    private var audioTrack: AudioTrack? = null

    @Volatile
    private var isRecording = false

    private val isConnecting = AtomicBoolean(false)
    private val isClosedManually = AtomicBoolean(false)
    private val reconnectAttempts = AtomicInteger(0)
    private val MAX_RECONNECT_ATTEMPTS = 5

    // Bounded channel to prevent unbounded memory growth (max 64 chunks ~ 1.5s audio)
    private val audioPlaybackChannel = Channel<ByteArray>(
        capacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val _stateFlow = MutableStateFlow<LiveState>(LiveState.IDLE)
    val stateFlow: StateFlow<LiveState> = _stateFlow.asStateFlow()

    private fun updateState(newState: LiveState) {
        _stateFlow.value = newState
        listener?.onStateChanged(newState)
    }

    fun connect() {
        if (apiKey.isEmpty()) {
            Log.e("GeminiLiveClient", "API Key is empty. Cannot connect to Gemini Live.")
            updateState(LiveState.ERROR("Gemini API Key is empty"))
            return
        }

        if (!isConnecting.compareAndSet(false, true)) {
            Log.d("GeminiLiveClient", "Already connecting or connection attempt in progress")
            return
        }

        isClosedManually.set(false)
        updateState(LiveState.CONNECTING)

        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("GeminiLiveClient", "WebSocket Opened successfully")
                isConnecting.set(false)
                reconnectAttempts.set(0)
                sendSetupMessage()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("GeminiLiveClient", "WebSocket Closed ($code): $reason")
                isConnecting.set(false)
                stopAudioIO()
                this@GeminiLiveClient.webSocket = null
                
                if (!isClosedManually.get()) {
                    triggerReconnect("Connection closed unexpectedly")
                } else {
                    updateState(LiveState.DISCONNECTED)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("GeminiLiveClient", "WebSocket Failure", t)
                isConnecting.set(false)
                stopAudioIO()
                this@GeminiLiveClient.webSocket = null

                if (!isClosedManually.get()) {
                    triggerReconnect(t.message ?: "Network error")
                } else {
                    updateState(LiveState.ERROR(t.message ?: "Connection failed"))
                }
            }
        })
    }

    private fun triggerReconnect(reason: String) {
        val attempts = reconnectAttempts.incrementAndGet()
        if (attempts <= MAX_RECONNECT_ATTEMPTS) {
            val backoffMs = (1000L * (1 shl (attempts - 1))).coerceAtMost(16000L)
            Log.w("GeminiLiveClient", "Reconnecting attempt $attempts/$MAX_RECONNECT_ATTEMPTS in ${backoffMs}ms ($reason)")
            updateState(LiveState.RECONNECTING(attempts, MAX_RECONNECT_ATTEMPTS))

            clientScope.launch {
                delay(backoffMs)
                if (!isClosedManually.get()) {
                    connect()
                }
            }
        } else {
            Log.e("GeminiLiveClient", "Max reconnect attempts reached ($MAX_RECONNECT_ATTEMPTS). Giving up.")
            updateState(LiveState.ERROR("Network connection lost. Max reconnection attempts reached."))
            listener?.onError("Max reconnection attempts reached")
        }
    }

    private fun sendSetupMessage() {
        try {
            val setupMsg = JSONObject().apply {
                put("setup", JSONObject().apply {
                    put("model", modelName)
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "You are JARVIS, a highly advanced, witty AI assistant. Keep responses brief and conversational.")
                            })
                        })
                    })
                })
            }
            val sent = webSocket?.send(setupMsg.toString()) ?: false
            if (!sent) {
                Log.e("GeminiLiveClient", "Failed to send setup message over WebSocket")
                updateState(LiveState.ERROR("WebSocket send failed"))
            }
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "Error sending setup message", e)
            updateState(LiveState.ERROR("Setup message formatting error: ${e.message}"))
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)

            if (json.has("setupComplete")) {
                Log.d("GeminiLiveClient", "Gemini Live Setup Complete!")
                updateState(LiveState.CONNECTED)
                startAudioIO()
            } else if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    Log.d("GeminiLiveClient", "User interrupted model speech. Clearing audio buffer...")
                    var cleared = 0
                    while (audioPlaybackChannel.tryReceive().isSuccess) { cleared++ }
                    
                    try {
                        audioTrack?.pause()
                        audioTrack?.flush()
                        audioTrack?.play()
                    } catch (e: Exception) {
                        Log.w("GeminiLiveClient", "Error flushing audioTrack on interrupt: ${e.message}")
                    }
                    updateState(LiveState.LISTENING)
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.getJSONArray("parts")
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        if (part.has("inlineData")) {
                            val inlineData = part.getJSONObject("inlineData")
                            if (inlineData.getString("mimeType").startsWith("audio/pcm")) {
                                val dataBase64 = inlineData.getString("data")
                                val pcmData = Base64.decode(dataBase64, Base64.NO_WRAP)
                                audioPlaybackChannel.trySend(pcmData)
                                listener?.onModelSpoke(pcmData)
                                updateState(LiveState.SPEAKING)
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    Log.d("GeminiLiveClient", "Turn Complete")
                    updateState(LiveState.LISTENING)
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "Error handling incoming WebSocket message", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudioIO() {
        if (isRecording) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("GeminiLiveClient", "Cannot start audio input: RECORD_AUDIO permission missing")
            updateState(LiveState.ERROR("RECORD_AUDIO permission missing"))
            return
        }

        isRecording = true

        // 1. Setup AudioTrack Playback: 24kHz Mono 16-bit PCM
        val outSampleRate = 24000
        val outBufferSize = AudioTrack.getMinBufferSize(
            outSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        ) * 4

        try {
            audioTrack = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(outSampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(outBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    outSampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    outBufferSize,
                    AudioTrack.MODE_STREAM
                )
            }

            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                Log.e("GeminiLiveClient", "AudioTrack state uninitialized! Cannot play audio.")
                updateState(LiveState.ERROR("AudioTrack initialization failed"))
                stopAudioIO()
                return
            }

            audioTrack?.play()

            // Playback coroutine
            audioPlaybackJob?.cancel()
            audioPlaybackJob = clientScope.launch {
                for (pcmData in audioPlaybackChannel) {
                    if (!isActive || !isRecording) break
                    val track = audioTrack
                    if (track != null && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.write(pcmData, 0, pcmData.size)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "AudioTrack setup error", e)
            updateState(LiveState.ERROR("AudioTrack setup error: ${e.message}"))
            stopAudioIO()
            return
        }

        // 2. Start Recording Coroutine using AudioController
        
        Log.d("GeminiLiveClient", "Started recording via AudioController")

        var consecutiveSendFailures = 0

        audioRecordJob?.cancel()
        audioRecordJob = clientScope.launch {
            AudioController.geminiAudioFlow.collect { shortChunk ->
                if (!isActive || !isRecording) return@collect
                
                // Convert ShortArray to ByteArray (Little Endian for standard PCM 16-bit)
                val byteBuffer = ByteBuffer.allocate(shortChunk.size * 2)
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                byteBuffer.asShortBuffer().put(shortChunk)
                val byteArray = byteBuffer.array()

                val encoded = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                val json = JSONObject().apply {
                    put("realtimeInput", JSONObject().apply {
                        put("audio", JSONObject().apply {
                            put("mimeType", "audio/pcm;rate=16000")
                            put("data", encoded)
                        })
                    })
                }

                val sent = webSocket?.send(json.toString()) ?: false
                if (!sent) {
                    consecutiveSendFailures++
                    if (consecutiveSendFailures > 5) {
                        Log.e("GeminiLiveClient", "WebSocket send failed repeatedly. Triggering reconnect.")
                        triggerReconnect("WebSocket streaming failed")
                        return@collect
                    }
                } else {
                    consecutiveSendFailures = 0
                }
            }
        }

        updateState(LiveState.LISTENING)
    }

    private fun stopAudioIO() {
        isRecording = false

        audioPlaybackJob?.cancel()
        audioPlaybackJob = null

        audioRecordJob?.cancel()
        audioRecordJob = null
        
        

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun disconnect() {
        isClosedManually.set(true)
        isConnecting.set(false)
        stopAudioIO()

        try {
            webSocket?.close(1000, "User requested disconnect")
        } catch (_: Exception) {}
        webSocket = null

        updateState(LiveState.DISCONNECTED)
    }

    fun destroy() {
        disconnect()
        clientJob.cancel()
    }
}
