import re

with open("app/src/main/java/com/example/ui/screens/PermissionsScreen.kt", "r") as f:
    content = f.read()

content = content.replace("'Hey Jarvis' wake word", "'yes'/'go' wake word")

with open("app/src/main/java/com/example/ui/screens/PermissionsScreen.kt", "w") as f:
    f.write(content)
