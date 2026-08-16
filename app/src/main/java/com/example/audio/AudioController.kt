package com.example.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.AppState
import com.example.AssistantStateManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AudioController {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _wakeWordAudioFlow = MutableSharedFlow<ShortArray>(extraBufferCapacity = 64)
    val wakeWordAudioFlow: SharedFlow<ShortArray> = _wakeWordAudioFlow.asSharedFlow()

    private val _geminiAudioFlow = MutableSharedFlow<ShortArray>(extraBufferCapacity = 64)
    val geminiAudioFlow: SharedFlow<ShortArray> = _geminiAudioFlow.asSharedFlow()

    private var appContext: Context? = null
    private var stateObserverJob: Job? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        
        stateObserverJob?.cancel()
        stateObserverJob = scope.launch {
            AssistantStateManager.appState.collect { state ->
                when (state) {
                    AppState.IDLE -> stopRecording()
                    AppState.WAKE_LISTENING, AppState.ACTIVE_CONVERSATION -> startRecording()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        val context = appContext
        if (context == null) {
            Log.e("AudioController", "AudioController not initialized with context.")
            return
        }

        if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("AudioController", "RECORD_AUDIO permission missing.")
            return
        }

        try {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

            if (audioRecord == null || audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioController", "AudioRecord failed to initialize")
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            Log.d("AudioController", "Audio capture started")

            recordingJob?.cancel()
            recordingJob = scope.launch {
                val buffer = ShortArray(1024)
                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val chunk = buffer.copyOfRange(0, read)
                        when (AssistantStateManager.appState.value) {
                            AppState.WAKE_LISTENING -> _wakeWordAudioFlow.tryEmit(chunk)
                            AppState.ACTIVE_CONVERSATION -> _geminiAudioFlow.tryEmit(chunk)
                            AppState.IDLE -> { /* Drop */ }
                        }
                    } else {
                        delay(5)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioController", "Failed to start audio capture", e)
        }
    }

    private fun stopRecording() {
        if (audioRecord == null) return
        
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e("AudioController", "Error stopping AudioRecord", e)
        }
        audioRecord = null
        Log.d("AudioController", "Audio capture stopped and released")
    }
}
