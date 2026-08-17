import re

with open("app/src/main/java/com/example/services/JarvisService.kt", "r") as f:
    content = f.read()

update_notification_code = """
    private fun updateNotification(state: AssistantState) {
        val text = when (state) {
            AssistantState.IDLE -> "Listening for wake word..."
            AssistantState.LISTENING -> "JARVIS is listening..."
            AssistantState.THINKING -> "JARVIS is thinking..."
            AssistantState.SPEAKING -> "JARVIS is speaking..."
            AssistantState.ERROR -> "JARVIS encountered an error."
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS Voice Assistant")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }
"""

content = content.replace("private fun createNotification(): Notification {", update_notification_code + "\n    private fun createNotification(): Notification {")

update_state_replacement = """    private fun updateAssistantState(newState: AssistantState, errorMessage: String? = null) {
        assistantState = newState
        currentErrorMessage = errorMessage
        JarvisServiceState.updateState(newState)
        if (errorMessage != null) {
            JarvisServiceState.setError(errorMessage)
        }
        updateNotification(newState)
"""
content = content.replace("    private fun updateAssistantState(newState: AssistantState, errorMessage: String? = null) {\n        assistantState = newState\n        currentErrorMessage = errorMessage\n        JarvisServiceState.updateState(newState)\n        if (errorMessage != null) {\n            JarvisServiceState.setError(errorMessage)\n        }", update_state_replacement)

with open("app/src/main/java/com/example/services/JarvisService.kt", "w") as f:
    f.write(content)
