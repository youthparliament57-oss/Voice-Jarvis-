package com.example.wakeword

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.components.containers.AudioData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import com.example.audio.AudioController
import kotlinx.coroutines.*

class WakeWordEngine(
    private val context: Context,
    private val wakeThreshold: Float = 0.50f,
    private val cooldownMs: Long = 2000L,
    private val targetLabels: Set<String> = setOf("hey_jarvis", "jarvis", "hey jarvis")
) {

    sealed class EngineState {
        object UNINITIALIZED : EngineState()
        object INITIALIZING : EngineState()
        object READY : EngineState()
        object LISTENING : EngineState()
        object DETECTED : EngineState()
        object COOLDOWN : EngineState()
        object MODEL_UNAVAILABLE : EngineState()
        object MIC_UNAVAILABLE : EngineState()
        object STOPPED : EngineState()
        data class ERROR(val message: String) : EngineState()
    }

    private var classifier: AudioClassifier? = null
    private var audioData: AudioData? = null

    @Volatile
    private var isListening = false
    private var listeningJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var lastDetectionTime: Long = 0L

    private val _engineState = MutableStateFlow<EngineState>(EngineState.UNINITIALIZED)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val _wakeWordDetected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val wakeWordDetected: SharedFlow<Unit> = _wakeWordDetected

    fun initialize() {
        if (_engineState.value == EngineState.INITIALIZING) return
        _engineState.value = EngineState.INITIALIZING

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _engineState.value = EngineState.MIC_UNAVAILABLE
            return
        }

        try {
            val modelBuffer = findAndLoadModelAsset(context)

            if (modelBuffer != null) {
                val options = AudioClassifier.AudioClassifierOptions.builder()
                    .setBaseOptions(
                        com.google.mediapipe.tasks.core.BaseOptions.builder()
                            .setModelAssetBuffer(modelBuffer)
                            .build()
                    )
                    .build()
                classifier = AudioClassifier.createFromOptions(context, options)
                Log.d("WakeWordEngine", "TFLite AudioClassifier created successfully")
            } else {
                Log.w("WakeWordEngine", "No valid TFLite wake-word model found in assets. Engine set to MODEL_UNAVAILABLE.")
                _engineState.value = EngineState.MODEL_UNAVAILABLE
                return
            }

            // Using 15600 samples buffer (975ms) which matches YAMNet-based models (speech_commands)
            audioData = AudioData.create(
                AudioData.AudioDataFormat.builder()
                    .setNumOfChannels(1)
                    .setSampleRate(16000f)
                    .build(),
                15600
            )

            _engineState.value = EngineState.READY
            Log.d("WakeWordEngine", "Engine initialized & READY")
        } catch (e: Exception) {
            Log.e("WakeWordEngine", "Error initializing WakeWordEngine", e)
            _engineState.value = EngineState.ERROR(e.message ?: "Initialization failed")
        }
    }

    fun startListening() {
        if (isListening) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("WakeWordEngine", "Cannot start listening: RECORD_AUDIO permission missing.")
            _engineState.value = EngineState.MIC_UNAVAILABLE
            return
        }

        if (classifier == null || audioData == null) {
            initialize()
        }

        if (classifier == null) {
            Log.w("WakeWordEngine", "Cannot start listening: Model is unavailable.")
            _engineState.value = EngineState.MODEL_UNAVAILABLE
            return
        }

        isListening = true
        _engineState.value = EngineState.LISTENING
        

        listeningJob?.cancel()
        listeningJob = engineScope.launch {
            Log.d("WakeWordEngine", "Wake-word listening job started via AudioController")
            
            val windowBuffer = ShortArray(15600)
            var bufferPos = 0

            AudioController.wakeWordAudioFlow.collect { chunk ->
                if (!isListening) return@collect
                
                val currentTime = System.currentTimeMillis()
                val isInCooldown = (currentTime - lastDetectionTime) < cooldownMs
                
                if (isInCooldown) {
                    if (_engineState.value != EngineState.COOLDOWN) {
                        _engineState.value = EngineState.COOLDOWN
                    }
                    return@collect
                }

                if (_engineState.value != EngineState.LISTENING) {
                    _engineState.value = EngineState.LISTENING
                }

                val copyLength = minOf(chunk.size, windowBuffer.size - bufferPos)
                System.arraycopy(chunk, 0, windowBuffer, bufferPos, copyLength)
                bufferPos += copyLength

                if (bufferPos >= 8000) {
                    try {
                        audioData?.load(windowBuffer, 0, bufferPos)
                        bufferPos = 0
                        
                        val result = classifier?.classify(audioData)
                        val classificationResults = result?.classificationResults()

                        if (!classificationResults.isNullOrEmpty()) {
                            val classifications = classificationResults[0].classifications()
                            if (classifications.isNotEmpty()) {
                                val categories = classifications[0].categories()
                                for (category in categories) {
                                    val label = category.categoryName().lowercase().trim()
                                    val score = category.score()

                                    val matchesLabel = targetLabels.any { target ->
                                        label == target || label.contains(target)
                                    }

                                    if (matchesLabel && score >= wakeThreshold) {
                                        Log.d("WakeWordEngine", "WAKE WORD DETECTED! Keyword: '$label', Score: $score")
                                        lastDetectionTime = System.currentTimeMillis()
                                        _engineState.value = EngineState.DETECTED
                                        _wakeWordDetected.tryEmit(Unit)
                                        return@collect
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("WakeWordEngine", "Classification processing error: ${e.message}")
                    }
                }
            }
        }
    }

    fun stopListening() {
        isListening = false
        _engineState.value = EngineState.STOPPED
        
        listeningJob?.cancel()
        listeningJob = null
        
        
    }

    private fun findAndLoadModelAsset(context: Context): MappedByteBuffer? {
        val priorityModels = listOf("hey_jarvis.tflite", "speech_commands.tflite")
        val searchFolders = listOf("", "models", "tflite")

        for (folder in searchFolders) {
            for (modelName in priorityModels) {
                val fullPath = if (folder.isEmpty()) modelName else "$folder/$modelName"
                val buffer = loadModelFile(context, fullPath)
                if (buffer != null) {
                    Log.d("WakeWordEngine", "Found and mapped TFLite model at: '$fullPath'")
                    return buffer
                }
            }
        }
        return null
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer? {
        return try {
            val fileDescriptor = context.assets.openFd(modelPath)
            val inputStream = java.io.FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            fileDescriptor.close()
            inputStream.close()
            buffer
        } catch (e: Exception) {
            null
        }
    }

    fun destroy() {
        stopListening()
        engineScope.cancel()
        try {
            classifier?.close()
        } catch (_: Exception) {}
        classifier = null
        audioData = null
        _engineState.value = EngineState.UNINITIALIZED
    }
}
