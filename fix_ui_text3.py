import re

with open("app/src/main/java/com/example/services/JarvisService.kt", "r") as f:
    content = f.read()

content = content.replace("Listening for wake word 'Hey Jarvis'...", "Listening for wake word ('yes'/'go')...")

with open("app/src/main/java/com/example/services/JarvisService.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("'Hey Jarvis'", "'yes'/'go'")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

