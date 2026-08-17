sed -i 's/geminiLiveClient = GeminiLiveClient(/geminiLiveClient = GeminiLiveClient(context = this, apiKey = apiKey, systemInstruction = settingsRepository.getSystemPrompt()) \/\/ /g' app/src/main/java/com/example/services/JarvisService.kt
sed -i 's/            systemInstruction = settingsRepository.getSystemPrompt(),//g' app/src/main/java/com/example/services/JarvisService.kt
sed -i 's/this, apiKey)//g' app/src/main/java/com/example/services/JarvisService.kt
