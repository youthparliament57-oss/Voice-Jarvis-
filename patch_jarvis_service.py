import re

with open("app/src/main/java/com/example/services/JarvisService.kt", "r") as f:
    content = f.read()

new_onStartCommand = """
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "com.example.ACTION_SEND_TEXT") {
            val text = intent.getStringExtra("TEXT_QUERY")
            if (!text.isNullOrBlank()) {
                sendTextQuery(text)
            }
        }
        return START_STICKY
    }

    private fun sendTextQuery(text: String) {
        if (assistantState == AssistantState.IDLE) {
            // Wake up JARVIS first if sleeping
            serviceScope.launch {
                val apiKey = settingsRepository.getApiKey()
                if (!apiKey.isNullOrBlank()) {
                    playWakeWordFeedback()
                    startGeminiSession(apiKey)
                    delay(500) // Brief delay to let WebSocket connect
                    geminiLiveClient?.sendText(text)
                } else {
                    JarvisServiceState.setError("API Key Missing")
                }
            }
        } else {
            // Already connected
            geminiLiveClient?.sendText(text)
        }
    }
"""

content = re.sub(r'    override fun onStartCommand\(intent: Intent\?, flags: Int, startId: Int\): Int \{\n        return START_STICKY\n    \}', new_onStartCommand, content)

with open("app/src/main/java/com/example/services/JarvisService.kt", "w") as f:
    f.write(content)

