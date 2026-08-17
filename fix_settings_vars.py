import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

header_injection = """
    var inputKey by remember { mutableStateOf("") }
    val systemPrompt by viewModel.systemPrompt.collectAsStateWithLifecycle()
    val modelName by viewModel.modelName.collectAsStateWithLifecycle()
    val wakeThreshold by viewModel.wakeThreshold.collectAsStateWithLifecycle()
    val sessionTimeout by viewModel.sessionTimeout.collectAsStateWithLifecycle()

    var inputSystemPrompt by remember(systemPrompt) { mutableStateOf(systemPrompt) }
    var inputModelName by remember(modelName) { mutableStateOf(modelName) }
    var inputWakeThreshold by remember(wakeThreshold) { mutableStateOf(wakeThreshold.toString()) }
    var inputSessionTimeout by remember(sessionTimeout) { mutableStateOf((sessionTimeout / 1000).toString()) }
"""

content = content.replace("    var inputKey by remember { mutableStateOf(\"\") }", header_injection)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
