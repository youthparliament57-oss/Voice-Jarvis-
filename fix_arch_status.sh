sed -i 's/isActive = hasAudio/isActive = hasAudio \&\& isServiceRunning/g' app/src/main/java/com/example/MainActivity.kt
sed -i 's/isActive = apiKey.isNotBlank()/isActive = apiKey.isNotBlank() \&\& isServiceRunning/g' app/src/main/java/com/example/MainActivity.kt
sed -i 's/isActive = hasOverlay/isActive = hasOverlay \&\& isServiceRunning/g' app/src/main/java/com/example/MainActivity.kt
