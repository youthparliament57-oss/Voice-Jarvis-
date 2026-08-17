import re

with open("app/src/main/java/com/example/gemini/GeminiLiveClient.kt", "r") as f:
    content = f.read()

# Add TextToSpeech property
tts_prop = "    private var audioTrack: AudioTrack? = null\n    private var tts: android.speech.tts.TextToSpeech? = null"
content = content.replace("    private var audioTrack: AudioTrack? = null", tts_prop)

# Init TextToSpeech in connect() or init block
init_code = """
    init {
        tts = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.US
            }
        }
    }
"""
content = content.replace("    private val _stateFlow = MutableStateFlow<LiveState>(LiveState.IDLE)", init_code + "\n    private val _stateFlow = MutableStateFlow<LiveState>(LiveState.IDLE)")

# Handle Text Parts
handle_text_code = """
                            if (part.has("text")) {
                                val textContent = part.getString("text")
                                Log.d("GeminiLiveClient", "Model Text Response: $textContent")
                                updateState(LiveState.SPEAKING)
                                tts?.speak(textContent, android.speech.tts.TextToSpeech.QUEUE_ADD, null, null)
                            }
"""
content = content.replace("                            if (inlineData.getString(\"mimeType\").startsWith(\"audio/pcm\")) {", handle_text_code + "\n                            if (inlineData.getString(\"mimeType\").startsWith(\"audio/pcm\")) {")

# Cleanup TTS in disconnect
cleanup_code = "        tts?.stop()\n        tts?.shutdown()\n        tts = null\n        clientJob.cancel()"
content = content.replace("        clientJob.cancel()", cleanup_code)

with open("app/src/main/java/com/example/gemini/GeminiLiveClient.kt", "w") as f:
    f.write(content)
