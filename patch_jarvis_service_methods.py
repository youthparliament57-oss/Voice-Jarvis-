import re

with open("app/src/main/java/com/example/services/JarvisService.kt", "r") as f:
    content = f.read()

# Implement onModelText in JarvisService which implements GeminiLiveListener
# Since we don't know exactly how it is implemented, we'll replace "override fun onModelSpoke"
on_model_spoke_impl = """
    override fun onModelSpoke(pcmData: ByteArray) {
        // Handled internally by GeminiLiveClient, but we reset timeout
        resetSessionTimeout()
    }

    override fun onModelText(text: String) {
        JarvisServiceState.addTranscript(TranscriptMessage(isUser = false, text = text))
        resetSessionTimeout()
    }
"""
content = re.sub(r'\s+override fun onModelSpoke\(pcmData: ByteArray\) \{.*?\n\s+\}', on_model_spoke_impl, content, flags=re.DOTALL)

# Add logging to sendTextQuery
send_text_code = """
    private fun sendTextQuery(text: String) {
        JarvisServiceState.addTranscript(TranscriptMessage(isUser = true, text = text))
        if (assistantState == AssistantState.IDLE) {
"""
content = content.replace("    private fun sendTextQuery(text: String) {\n        if (assistantState == AssistantState.IDLE) {", send_text_code)

with open("app/src/main/java/com/example/services/JarvisService.kt", "w") as f:
    f.write(content)

