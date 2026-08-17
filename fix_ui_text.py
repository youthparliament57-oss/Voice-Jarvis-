import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace('title = "Local Wake-Word Listener (\'Hey Jarvis\')",', 'title = "Local Wake-Word Listener (\'yes\'/\'go\')",')

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
