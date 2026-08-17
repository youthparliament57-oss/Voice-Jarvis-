import re

with open("app/src/main/java/com/example/audio/AudioController.kt", "r") as f:
    content = f.read()

pattern = r'    private var retryJob: Job\? = null.*?    private fun stopRecording\(\) \{'

replacement = """    private var retryJob: Job? = null

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        retryJob?.cancel()
        retryJob = scope.launch {
            var attempt = 0
            val maxAttempts = 5
            while (attempt < maxAttempts) {
                if (audioRecord != null && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    return@launch
                }
                val context = appContext ?: return@launch
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    Log.e("AudioController", "Missing RECORD_AUDIO permission")
                    return@launch
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
                        Log.e("AudioController", "AudioRecord failed to initialize, attempt ${attempt + 1}")
                        audioRecord?.release()
                        audioRecord = null
                        attempt++
                        kotlinx.coroutines.delay(1000L * attempt)
                        continue
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
                        val buffer = ShortArray(780)
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
                                            if (consecutiveSpeechFrames >= 3) {
                                                _userSpeakingFlow.tryEmit(false)
                                            }
                                            consecutiveSpeechFrames = 0
                                        }
                                    }
                                }
                            } else {
                                kotlinx.coroutines.delay(5)
                            }
                        }
                    }
                    break // Successfully started recording, exit the retry loop
                } catch (e: Exception) {
                    Log.e("AudioController", "Failed to start audio capture", e)
                    attempt++
                    kotlinx.coroutines.delay(1000L * attempt)
                }
            }
            
            if (attempt >= maxAttempts) {
                Log.e("AudioController", "Exhausted all attempts to initialize AudioRecord")
                JarvisServiceState.setError("Microphone unavailable after multiple attempts.")
            }
        }
    }

    private fun stopRecording() {"""

content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/audio/AudioController.kt", "w") as f:
    f.write(content)
