import re

with open("app/src/main/java/com/example/gemini/GeminiLiveClient.kt", "r") as f:
    content = f.read()

# Revert previous bad injection
bad_code = """
                            if (part.has("text")) {
                                val textContent = part.getString("text")
                                Log.d("GeminiLiveClient", "Model Text Response: $textContent")
                                updateState(LiveState.SPEAKING)
                                tts?.speak(textContent, android.speech.tts.TextToSpeech.QUEUE_ADD, null, null)
                            }
"""
content = content.replace(bad_code, "")

# Inject properly
correct_code = """
                        if (part.has("text")) {
                            val textContent = part.getString("text")
                            Log.d("GeminiLiveClient", "Model Text Response: $textContent")
                            updateState(LiveState.SPEAKING)
                            tts?.speak(textContent, android.speech.tts.TextToSpeech.QUEUE_ADD, null, null)
                        }
"""
content = content.replace("                        if (part.has(\"inlineData\")) {", correct_code + "\n                        if (part.has(\"inlineData\")) {")

with open("app/src/main/java/com/example/gemini/GeminiLiveClient.kt", "w") as f:
    f.write(content)
