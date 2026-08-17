import re

with open("app/src/main/java/com/example/gemini/GeminiLiveClient.kt", "r") as f:
    content = f.read()

content = content.replace("""        try {
            webSocket?.close(1000, "User requested disconnect")
        }
 catch (_: Exception) {}
        webSocket = null

        updateState(LiveState.DISCONNECTED)
    }""", """        try {
            webSocket?.close(1000, "User requested disconnect")
        } catch (_: Exception) {}
        webSocket = null

        updateState(LiveState.DISCONNECTED)
    }""")

with open("app/src/main/java/com/example/gemini/GeminiLiveClient.kt", "w") as f:
    f.write(content)
