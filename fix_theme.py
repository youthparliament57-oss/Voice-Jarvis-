import re

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.material3.lightColorScheme\n", "")
content = content.replace("    darkTheme: Boolean = true,\n    dynamicColor: Boolean = false,\n", "")

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "w") as f:
    f.write(content)
