sed -i 's/import com.example.AssistantStateManager//g' app/src/main/java/com/example/services/JarvisService.kt
sed -i 's/AssistantStateManager.updateState(AppState.WAKE_LISTENING)//g' app/src/main/java/com/example/services/JarvisService.kt
sed -i 's/AssistantStateManager.updateState(AppState.ACTIVE_CONVERSATION)//g' app/src/main/java/com/example/services/JarvisService.kt
sed -i 's/AssistantStateManager.updateState(AppState.IDLE)//g' app/src/main/java/com/example/services/JarvisService.kt

sed -i 's/import com.example.AssistantStateManager//g' app/src/main/java/com/example/audio/AudioController.kt
sed -i 's/import com.example.AppState//g' app/src/main/java/com/example/audio/AudioController.kt
sed -i 's/AssistantStateManager.appState.collect { state ->/com.example.services.JarvisServiceState.assistantState.collect { state ->/g' app/src/main/java/com/example/audio/AudioController.kt
