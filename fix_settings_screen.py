import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# Make the settings fields state variables in SettingsScreen
# First, let's inject them at the beginning of the composable

header_injection = """
    var inputKey by remember { mutableStateOf(apiKey) }
    val systemPrompt by viewModel.systemPrompt.collectAsState()
    val modelName by viewModel.modelName.collectAsState()
    val wakeThreshold by viewModel.wakeThreshold.collectAsState()
    val sessionTimeout by viewModel.sessionTimeout.collectAsState()

    var inputSystemPrompt by remember(systemPrompt) { mutableStateOf(systemPrompt) }
    var inputModelName by remember(modelName) { mutableStateOf(modelName) }
    var inputWakeThreshold by remember(wakeThreshold) { mutableStateOf(wakeThreshold.toString()) }
    var inputSessionTimeout by remember(sessionTimeout) { mutableStateOf((sessionTimeout / 1000).toString()) }
"""
content = re.sub(r'    var inputKey by remember \{ mutableStateOf\(apiKey\) \}', header_injection, content)

# Now, add the new OutlinedTextFields and buttons below the "API Key" section.
# We'll inject them before "when (val state = validationState)"
new_ui_injection = """
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = inputSystemPrompt,
                onValueChange = { inputSystemPrompt = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("System Prompt") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PureWhite,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedLabelColor = PureWhite,
                    unfocusedLabelColor = SilverText,
                    cursorColor = PureWhite,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inputModelName,
                onValueChange = { inputModelName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Model Name (e.g. models/gemini-2.5-flash)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PureWhite,
                    unfocusedBorderColor = SurfaceBorder,
                    focusedLabelColor = PureWhite,
                    unfocusedLabelColor = SilverText,
                    cursorColor = PureWhite,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = inputWakeThreshold,
                    onValueChange = { inputWakeThreshold = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Wake Threshold (0.0-1.0)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedLabelColor = PureWhite,
                        unfocusedLabelColor = SilverText,
                        cursorColor = PureWhite,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    )
                )
                OutlinedTextField(
                    value = inputSessionTimeout,
                    onValueChange = { inputSessionTimeout = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Timeout (sec)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedLabelColor = PureWhite,
                        unfocusedLabelColor = SilverText,
                        cursorColor = PureWhite,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    viewModel.saveSystemPrompt(inputSystemPrompt)
                    viewModel.saveModelName(inputModelName)
                    inputWakeThreshold.toFloatOrNull()?.let { viewModel.saveWakeThreshold(it) }
                    inputSessionTimeout.toLongOrNull()?.let { viewModel.saveSessionTimeout(it * 1000) }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkCardSurface,
                    contentColor = PureWhite
                )
            ) {
                Text("SAVE ADDITIONAL SETTINGS", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
            }
            
            when (val state = validationState)"""
content = re.sub(r'\s+when \(val state = validationState\)', new_ui_injection, content)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
