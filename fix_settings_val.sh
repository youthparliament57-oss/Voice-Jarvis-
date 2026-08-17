sed -i '/private lateinit var wakeWordEngine: WakeWordEngine/a \    private val settingsRepository by lazy { SettingsRepository.getInstance(this) }' app/src/main/java/com/example/services/JarvisService.kt
sed -i 's/val settingsRepository = SettingsRepository.getInstance(this)//g' app/src/main/java/com/example/services/JarvisService.kt
