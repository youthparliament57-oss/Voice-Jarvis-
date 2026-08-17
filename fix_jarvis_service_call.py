import re

with open("app/src/main/java/com/example/services/JarvisService.kt", "r") as f:
    content = f.read()

content = content.replace("startGeminiSession(apiKey)", "startGeminiLiveSession()")

with open("app/src/main/java/com/example/services/JarvisService.kt", "w") as f:
    f.write(content)
