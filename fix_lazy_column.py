import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace(".heightIn(max = 200.dp)", ".height(200.dp)")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
