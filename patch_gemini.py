import re

with open("app/src/main/java/com/example/gemini/GeminiLiveClient.kt", "r") as f:
    content = f.read()

send_text_code = """
    fun sendText(text: String) {
        clientScope.launch {
            if (webSocket == null) {
                Log.w("GeminiLiveClient", "Cannot send text, webSocket is null")
                return@launch
            }
            try {
                val json = org.json.JSONObject().apply {
                    put("clientContent", org.json.JSONObject().apply {
                        put("turns", org.json.JSONArray().apply {
                            put(org.json.JSONObject().apply {
                                put("role", "user")
                                put("parts", org.json.JSONArray().apply {
                                    put(org.json.JSONObject().apply {
                                        put("text", text)
                                    })
                                })
                            })
                        })
                        put("turnComplete", true)
                    })
                }
                val sent = webSocket?.send(json.toString()) ?: false
                if (sent) {
                    Log.d("GeminiLiveClient", "Sent text query successfully: $text")
                    updateState(LiveState.SPEAKING) // Transition state if needed
                } else {
                    Log.w("GeminiLiveClient", "Failed to send text via WebSocket")
                }
            } catch (e: Exception) {
                Log.e("GeminiLiveClient", "Error sending text", e)
            }
        }
    }
"""

# Insert sendText after disconnect()
pattern = r'(    fun disconnect\(\) \{[\s\S]*?    \})'
content = re.sub(pattern, r'\1\n' + send_text_code, content)

with open("app/src/main/java/com/example/gemini/GeminiLiveClient.kt", "w") as f:
    f.write(content)
