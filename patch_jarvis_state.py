import re

with open("app/src/main/java/com/example/services/JarvisService.kt", "r") as f:
    content = f.read()

# 1. Add TranscriptMessage data class before JarvisServiceState
transcript_class = """
data class TranscriptMessage(val isUser: Boolean, val text: String, val timestamp: Long = System.currentTimeMillis())

object JarvisServiceState {
    private val _transcript = MutableStateFlow<List<TranscriptMessage>>(emptyList())
    val transcript = _transcript.asStateFlow()

    fun addTranscript(message: TranscriptMessage) {
        val current = _transcript.value.toMutableList()
        current.add(message)
        if (current.size > 50) current.removeAt(0) // Keep last 50 messages
        _transcript.value = current
    }

    fun clearTranscript() {
        _transcript.value = emptyList()
    }
"""

content = content.replace("object JarvisServiceState {", transcript_class)

with open("app/src/main/java/com/example/services/JarvisService.kt", "w") as f:
    f.write(content)
