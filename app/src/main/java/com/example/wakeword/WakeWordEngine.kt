package com.example.wakeword

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.components.containers.AudioData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.sqrt

class WakeWordEngine(private val context: Context) {

    private var classifier: AudioClassifier? = null
    private var audioData: AudioData? = null
    private var audioRecord: AudioRecord? = null

    @Volatile
    private var isListening = false
    private var listeningThread: Thread? = null

    private val _wakeWordDetected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val wakeWordDetected: SharedFlow<Unit> = _wakeWordDetected

    private var isFallbackEnergyDetector = false

    fun initialize() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("WakeWordEngine", "RECORD_AUDIO permission not granted. Cannot initialize audio recorder.")
            return
        }

        try {
            // Try model names
            val modelNames = listOf("hey_jarvis.tflite", "speech_commands.tflite")
            var modelLoaded = false

            for (model in modelNames) {
                try {
                    classifier = AudioClassifier.createFromFile(context, model)
                    modelLoaded = true
                    Log.d("WakeWordEngine", "Successfully loaded model: $model")
                    break
                } catch (e: Exception) {
                    Log.w("WakeWordEngine", "Model $model not found or failed to load: ${e.message}")
                }
            }

            if (!modelLoaded) {
                Log.w("WakeWordEngine", "No TFLite classifier loaded. Using RMS Voice Energy fallback detector.")
                isFallbackEnergyDetector = true
            }

            // 16kHz, mono, PCM 16-bit
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT

            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

            @SuppressLint("MissingPermission")
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("WakeWordEngine", "AudioRecord failed to initialize")
                audioRecord = null
                return
            }

            audioData = AudioData.create(
                AudioData.AudioDataFormat.builder()
                    .setNumOfChannels(1)
                    .setSampleRate(16000f)
                    .build(),
                16000 // 1 second buffer
            )

            Log.d("WakeWordEngine", "Engine initialized successfully")
        } catch (e: Exception) {
            Log.e("WakeWordEngine", "Error initializing WakeWordEngine", e)
            isFallbackEnergyDetector = true
        }
    }

    fun startListening() {
        if (isListening) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("WakeWordEngine", "Cannot start listening: RECORD_AUDIO permission missing.")
            return
        }

        if (audioRecord == null) {
            initialize()
        }

        val record = audioRecord ?: run {
            Log.e("WakeWordEngine", "AudioRecord is null. Cannot start listening.")
            return
        }

        try {
            record.startRecording()
            isListening = true

            listeningThread = Thread {
                Log.d("WakeWordEngine", "Wake-word listening thread started")
                val buffer = ShortArray(8000) // 0.5 sec buffer

                var consecutiveHighVolumeFrames = 0

                while (isListening) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        if (!isFallbackEnergyDetector && classifier != null) {
                            try {
                                audioData?.load(buffer, 0, read)
                                val result = classifier?.classify(audioData)
                                val classificationResults = result?.classificationResults()
                                if (!classificationResults.isNullOrEmpty()) {
                                    val classifications = classificationResults[0].classifications()
                                    if (classifications.isNotEmpty()) {
                                        val categories = classifications[0].categories()
                                        for (category in categories) {
                                            val name = category.categoryName().lowercase()
                                            val score = category.score()
                                            if ((name.contains("jarvis") || name.contains("hey") || name == "1") && score > 0.4f) {
                                                Log.d("WakeWordEngine", "WAKE WORD DETECTED! Keyword: $name, Score: $score")
                                                _wakeWordDetected.tryEmit(Unit)
                                                Thread.sleep(1500)
                                                break
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("WakeWordEngine", "Classification error: ${e.message}")
                            }
                        } else {
                            // Voice Energy VAD Fallback
                            var sumSq = 0.0
                            for (i in 0 until read) {
                                sumSq += buffer[i] * buffer[i]
                            }
                            val rms = sqrt(sumSq / read)
                            if (rms > 2500) { // Speech energy threshold
                                consecutiveHighVolumeFrames++
                                if (consecutiveHighVolumeFrames >= 2) {
                                    Log.d("WakeWordEngine", "Voice Activity / Wake Word triggered by energy (RMS: $rms)")
                                    _wakeWordDetected.tryEmit(Unit)
                                    consecutiveHighVolumeFrames = 0
                                    Thread.sleep(2000)
                                }
                            } else {
                                consecutiveHighVolumeFrames = 0
                            }
                        }
                    } else {
                        Thread.sleep(10)
                    }
                }
                Log.d("WakeWordEngine", "Wake-word listening thread exited")
            }
            listeningThread?.start()
        } catch (e: Exception) {
            Log.e("WakeWordEngine", "Failed to start recording thread", e)
            isListening = false
        }
    }

    fun stopListening() {
        isListening = false
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.w("WakeWordEngine", "Error stopping audio record: ${e.message}")
        }
        try {
            listeningThread?.join(500)
        } catch (_: Exception) {}
        listeningThread = null
    }

    fun destroy() {
        stopListening()
        try {
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        try {
            classifier?.close()
        } catch (_: Exception) {}
        classifier = null
    }
}
