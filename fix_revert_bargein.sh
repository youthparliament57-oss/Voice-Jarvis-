sed -i '/AudioController.userSpeakingFlow.collect/d' app/src/main/java/com/example/services/JarvisService.kt
sed -i '/if (isSpeaking && assistantState == AssistantState.SPEAKING) {/d' app/src/main/java/com/example/services/JarvisService.kt
sed -i '/Log.d("JarvisService", "Local VAD Barge-in detected. Flushing playback.")/d' app/src/main/java/com/example/services/JarvisService.kt
sed -i '/geminiLiveClient?.flushPlayback()/d' app/src/main/java/com/example/services/JarvisService.kt
sed -i '/updateAssistantState(AssistantState.LISTENING)/d' app/src/main/java/com/example/services/JarvisService.kt
