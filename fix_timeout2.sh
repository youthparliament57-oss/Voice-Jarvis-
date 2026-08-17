sed -i 's/delay(SESSION_TIMEOUT_MS)/delay(settingsRepository.getSessionTimeout())/g' app/src/main/java/com/example/services/JarvisService.kt
