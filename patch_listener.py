import re

with open("app/src/main/java/com/example/gemini/GeminiLiveClient.kt", "r") as f:
    content = f.read()

content = content.replace(
    "fun onModelSpoke(pcmData: ByteArray)\n}",
    "fun onModelSpoke(pcmData: ByteArray)\n    fun onModelText(text: String)\n}"
)

content = content.replace(
    """                            val textContent = part.getString("text")
                            Log.d("GeminiLiveClient", "Model Text Response: $textContent")""",
    """                            val textContent = part.getString("text")
                            Log.d("GeminiLiveClient", "Model Text Response: $textContent")
                            listener?.onModelText(textContent)"""
)

with open("app/src/main/java/com/example/gemini/GeminiLiveClient.kt", "w") as f:
    f.write(content)

