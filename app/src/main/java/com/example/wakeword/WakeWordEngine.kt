package com.example.wakeword

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.components.containers.AudioData

class WakeWordEngine(private val context: Context) {

    private var classifier: AudioClassifier? = null
    private var audioData: AudioData? = null
    private var audioRecord: AudioRecord? = null

    @Volatile
    private var isListening = false
    private var listeningThread: Thread? = null

    private val _wakeWordDetected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val wakeWordDetected: SharedFlow<Unit> = _wakeWordDetected

    @SuppressLint("MissingPermission")
    fun initialize() {
        try {
            val modelPath = "hey_jarvis.tflite"
            classifier = AudioClassifier.createFromFile(context, modelPath)
            
            // The microWakeWord model expects 16kHz, mono
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat) * 2
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            audioData = AudioData.create(
                AudioData.AudioDataFormat.builder()
                    .setNumOfChannels(1)
                    .setSampleRate(16000f)
                    .build(),
                16000 // Buffer 1 second of audio
            )

            Log.d("WakeWord", "Engine initialized successfully")
        } catch (e: Exception) {
            Log.e("WakeWord", "Error initializing engine: ${e.message}", e)
        }
    }

    fun startListening() {
        if (isListening || audioRecord == null) return
        
        isListening = true
        audioRecord?.startRecording()
        
        listeningThread = Thread {
            Log.d("WakeWord", "Listening thread started")
            val buffer = ShortArray(16000 / 2) // Half a second chunks
            while (isListening) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    audioData?.load(buffer, 0, read)
                    val result = classifier?.classify(audioData)
                    
                    val classificationResults = result?.classificationResults()
                    if (!classificationResults.isNullOrEmpty()) {
                        val classifications = classificationResults[0].classifications()
                        if (classifications.isNotEmpty()) {
                            val categories = classifications[0].categories()
                            for (category in categories) {
                                if ((category.categoryName() == "hey_jarvis" || category.categoryName() == "1") && category.score() > 0.5f) {
                                    Log.d("WakeWord", "WAKE WORD DETECTED! Score: ${category.score()}")
                                    _wakeWordDetected.tryEmit(Unit)
                                    Thread.sleep(2000)
                                }
                            }
                        }
                    }
                }
            }
            Log.d("WakeWord", "Listening thread stopped")
        }
        listeningThread?.start()
    }

    fun stopListening() {
        isListening = false
        audioRecord?.stop()
        listeningThread?.join(1000)
    }

    fun destroy() {
        stopListening()
        audioRecord?.release()
        classifier?.close()
    }
}
