cat << 'INNER_EOF' > fix_vad.py
import sys

with open("app/src/main/java/com/example/services/JarvisService.kt", "r") as f:
    content = f.read()

vad_code = """
        serviceScope.launch {
            com.example.audio.AudioController.userSpeakingFlow.collect { isSpeaking ->
                if (isSpeaking && assistantState == AssistantState.SPEAKING) {
                    android.util.Log.d("JarvisService", "Local VAD Barge-in detected. Flushing playback.")
                    geminiLiveClient?.flushPlayback()
                    updateAssistantState(AssistantState.LISTENING)
                }
            }
        }
        
        geminiLiveClient?.connect()
"""

content = content.replace("geminiLiveClient?.connect()", vad_code)

with open("app/src/main/java/com/example/services/JarvisService.kt", "w") as f:
    f.write(content)
INNER_EOF
python3 fix_vad.py
