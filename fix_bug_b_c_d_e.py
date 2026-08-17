import re

# Fix BUG-B
with open("app/src/main/java/com/example/audio/AudioController.kt", "r") as f:
    content = f.read()
content = content.replace("val buffer = ShortArray(1024)", "val buffer = ShortArray(780)")
with open("app/src/main/java/com/example/audio/AudioController.kt", "w") as f:
    f.write(content)

# Fix BUG-E
with open("app/proguard-rules.pro", "a") as f:
    f.write("\n# MediaPipe Tasks Audio\n-keep class com.google.mediapipe.** { *; }\n")

# Fix BUG-D & BUG-C
with open("app/src/main/java/com/example/services/JarvisService.kt", "r") as f:
    service_content = f.read()

# BUG-D
service_content = service_content.replace("super.onDestroy()\n", "super.onDestroy()\n        com.example.audio.AudioController.destroy()\n")

# BUG-C
tone_gen_replacement = """        serviceScope.launch {
            var toneGen: android.media.ToneGenerator? = null
            try {
                toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 100)
                toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
                delay(200)
            } finally {
                toneGen?.release()
            }
        }"""
        
# Find the exact lines to replace
pattern_c = r"val toneGen = android\.media\.ToneGenerator\(android\.media\.AudioManager\.STREAM_MUSIC, 100\)\n\s+toneGen\.startTone\(android\.media\.ToneGenerator\.TONE_PROP_BEEP, 150\)"
service_content = re.sub(pattern_c, tone_gen_replacement, service_content)

with open("app/src/main/java/com/example/services/JarvisService.kt", "w") as f:
    f.write(service_content)

