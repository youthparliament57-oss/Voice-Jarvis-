import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

content = content.replace("import androidx.compose.ui.text.input.VisualTransformation", "import androidx.compose.ui.text.input.VisualTransformation\nimport androidx.compose.foundation.text.KeyboardOptions\nimport androidx.compose.ui.text.input.KeyboardType")

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
