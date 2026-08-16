package com.example.gemini

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLiveClient(
    private val context: Context,
    private val apiKey: String
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var isRecording = false

    private val audioPlaybackChannel = Channel<ByteArray>(Channel.UNLIMITED)
    private val _stateFlow = MutableSharedFlow<LiveState>(extraBufferCapacity = 1)
    val stateFlow: SharedFlow<LiveState> = _stateFlow

    enum class LiveState {
        CONNECTING, CONNECTED, SPEAKING, LISTENING, ERROR, DISCONNECTED
    }

    fun connect() {
        if (apiKey.isBlank()) {
            Log.e("GeminiLiveClient", "API Key is empty. Cannot connect to Gemini Live.")
            _stateFlow.tryEmit(LiveState.ERROR)
            return
        }

        if (webSocket != null) return // Already connecting/connected

        _stateFlow.tryEmit(LiveState.CONNECTING)

        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("GeminiLiveClient", "WebSocket Opened successfully")
                sendSetupMessage()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("GeminiLiveClient", "WebSocket Closed: $reason")
                _stateFlow.tryEmit(LiveState.DISCONNECTED)
                stopAudioIO()
                this@GeminiLiveClient.webSocket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("GeminiLiveClient", "WebSocket Failure: ${t.message}", t)
                _stateFlow.tryEmit(LiveState.ERROR)
                stopAudioIO()
                this@GeminiLiveClient.webSocket = null
            }
        })
    }

    private fun sendSetupMessage() {
        try {
            val setupMsg = JSONObject().apply {
                put("setup", JSONObject().apply {
                    put("model", "models/gemini-2.5-flash")
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "You are an advanced, proactive AI assistant named JARVIS. Speak naturally, concisely, and conversationally. Do not use markdown, emojis, or lists in your spoken response. Speak in a confident, clear tone. You can be interrupted at any time.")
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().apply { put("AUDIO") })
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", "Aoede")
                                })
                            })
                        })
                    })
                })
            }
            webSocket?.send(setupMsg.toString())
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "Error sending setup message", e)
            _stateFlow.tryEmit(LiveState.ERROR)
        }
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)

            if (json.has("setupComplete")) {
                Log.d("GeminiLiveClient", "Setup Complete!")
                _stateFlow.tryEmit(LiveState.CONNECTED)
                startAudioIO()
            } else if (json.has("serverContent")) {
                val serverContent = json.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    Log.d("GeminiLiveClient", "User interrupted model speaking")
                    var cleared = 0
                    while (audioPlaybackChannel.tryReceive().isSuccess) { cleared++ }
                    Log.d("GeminiLiveClient", "Cleared $cleared pending audio chunks")

                    audioTrack?.pause()
                    audioTrack?.flush()
                    audioTrack?.play()
                    _stateFlow.tryEmit(LiveState.LISTENING)
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val dataBase64 = inlineData.getString("data")
                                val pcmData = Base64.decode(dataBase64, Base64.NO_WRAP)
                                audioPlaybackChannel.trySend(pcmData)
                                _stateFlow.tryEmit(LiveState.SPEAKING)
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    Log.d("GeminiLiveClient", "Turn Complete")
                    _stateFlow.tryEmit(LiveState.LISTENING)
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "Error handling message", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudioIO() {
        if (isRecording) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("GeminiLiveClient", "Cannot start audio input: RECORD_AUDIO permission missing")
            _stateFlow.tryEmit(LiveState.ERROR)
            return
        }

        isRecording = true

        // Playback: 24kHz Mono 16-bit PCM
        val outSampleRate = 24000
        val outBufferSize = AudioTrack.getMinBufferSize(
            outSampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        ) * 4

        try {
            audioTrack = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
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
            audioTrack?.play()

            // Playback coroutine
            coroutineScope.launch {
                for (pcmData in audioPlaybackChannel) {
                    if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        audioTrack?.write(pcmData, 0, pcmData.size)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "AudioTrack setup error", e)
        }

        // Recording: 16kHz Mono 16-bit PCM
        val inSampleRate = 16000
        val inBufferSize = AudioRecord.getMinBufferSize(
            inSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                inSampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                inBufferSize
            )

            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()

                coroutineScope.launch {
                    val buffer = ByteArray(2048)
                    while (isActive && isRecording) {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            val encoded = Base64.encodeToString(buffer, 0, read, Base64.NO_WRAP)
                            val json = JSONObject().apply {
                                put("realtimeInput", JSONObject().apply {
                                    put("mediaChunks", JSONArray().apply {
                                        put(JSONObject().apply {
                                            put("mimeType", "audio/pcm;rate=16000")
                                            put("data", encoded)
                                        })
                                    })
                                })
                            }
                            webSocket?.send(json.toString())
                        }
                    }
                }
            } else {
                Log.e("GeminiLiveClient", "AudioRecord initialization failed")
            }
        } catch (e: Exception) {
            Log.e("GeminiLiveClient", "AudioRecord setup error", e)
        }

        _stateFlow.tryEmit(LiveState.LISTENING)
    }

    private fun stopAudioIO() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun disconnect() {
        stopAudioIO()
        try {
            webSocket?.close(1000, "User requested disconnect")
        } catch (_: Exception) {}
        webSocket = null
        _stateFlow.tryEmit(LiveState.DISCONNECTED)
    }
}
