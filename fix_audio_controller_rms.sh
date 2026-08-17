cat << 'INNER_EOF' > app/src/main/java/com/example/audio/AudioController.kt
package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.AssistantState
import com.example.services.JarvisServiceState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlin.math.sqrt

@SuppressLint("StaticFieldLeak")
object AudioController {

    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private const val VAD_THRESHOLD = 1500.0 // RMS threshold for voice detection

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _wakeWordAudioFlow = MutableSharedFlow<ShortArray>(extraBufferCapacity = 64)
    val wakeWordAudioFlow: SharedFlow<ShortArray> = _wakeWordAudioFlow.asSharedFlow()

    private val _geminiAudioFlow = MutableSharedFlow<ShortArray>(extraBufferCapacity = 64)
    val geminiAudioFlow: SharedFlow<ShortArray> = _geminiAudioFlow.asSharedFlow()

    private val _userSpeakingFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val userSpeakingFlow: SharedFlow<Boolean> = _userSpeakingFlow.asSharedFlow()

    private var appContext: Context? = null
    private var stateObserverJob: Job? = null
    private var aec: AcousticEchoCanceler? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        
        stateObserverJob?.cancel()
        stateObserverJob = scope.launch {
            combine(
                JarvisServiceState.isRunning,
                JarvisServiceState.assistantState
            ) { isRunning, state ->
                Pair(isRunning, state)
            }.collect { (isRunning, state) ->
                if (isRunning && state != AssistantState.ERROR) {
                    startRecording()
                } else {
                    stopRecording()
                }
            }
        }
    }

    private fun calculateRMS(buffer: ShortArray): Double {
        var sum = 0.0
        for (sample in buffer) {
            sum += sample * sample
        }
        return sqrt(sum / buffer.size)
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        if (audioRecord != null && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }

        val context = appContext ?: return
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("AudioController", "Missing RECORD_AUDIO permission")
            return
        }

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioController", "AudioRecord failed to initialize")
                audioRecord = null
                return
            }

            if (AcousticEchoCanceler.isAvailable()) {
                try {
                    aec?.release()
                    val sessionId = audioRecord?.audioSessionId ?: -1
                    if (sessionId != -1) {
                        aec = AcousticEchoCanceler.create(sessionId)
                        aec?.enabled = true
                        Log.d("AudioController", "AEC enabled on session $sessionId")
                    }
                } catch (e: Exception) {
                    Log.e("AudioController", "Failed to enable AEC", e)
                }
            }

            audioRecord?.startRecording()
            Log.d("AudioController", "Audio capture started")

            recordingJob?.cancel()
            recordingJob = scope.launch {
                val buffer = ShortArray(1024)
                var consecutiveSpeechFrames = 0
                
                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val chunk = buffer.copyOfRange(0, read)
                        val isRunning = JarvisServiceState.isRunning.value
                        val state = JarvisServiceState.assistantState.value
                        
                        if (isRunning) {
                            if (state == AssistantState.IDLE) {
                                _wakeWordAudioFlow.tryEmit(chunk)
                            } else {
                                _geminiAudioFlow.tryEmit(chunk)
                                
                                // Local VAD for Barge-in logic
                                val rms = calculateRMS(chunk)
                                if (rms > VAD_THRESHOLD) {
                                    consecutiveSpeechFrames++
                                    if (consecutiveSpeechFrames == 3) {
                                        _userSpeakingFlow.tryEmit(true)
                                    }
                                } else {
                                    consecutiveSpeechFrames = 0
                                }
                            }
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
            aec?.release()
        } catch (_: Exception) {}
        aec = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e("AudioController", "Error stopping AudioRecord", e)
        }
        audioRecord = null
        Log.d("AudioController", "Audio capture stopped and released")
    }

    fun destroy() {
        stateObserverJob?.cancel()
        stateObserverJob = null
        stopRecording()
    }
}
INNER_EOF
