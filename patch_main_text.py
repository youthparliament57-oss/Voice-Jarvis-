import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

text_input_ui = """
            // 5. Text Input Row for Gemini
            var textQuery by remember { mutableStateOf("") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textQuery,
                    onValueChange = { textQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message to JARVIS...", color = SilverText.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        cursorColor = PureWhite
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (textQuery.isNotBlank() && isServiceRunning) {
                            val intent = android.content.Intent("com.example.ACTION_SEND_TEXT")
                            intent.setPackage(context.packageName)
                            intent.putExtra("TEXT_QUERY", textQuery)
                            context.startService(intent)
                            textQuery = ""
                        } else if (!isServiceRunning) {
                            Toast.makeText(context, "System Offline", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(if (textQuery.isNotBlank() && isServiceRunning) PureWhite else DarkCharcoal, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Send Text",
                        tint = if (textQuery.isNotBlank() && isServiceRunning) OledBlack else SilverText
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
"""

content = re.sub(r'            Spacer\(modifier = Modifier\.height\(28\.dp\)\)\n        \}\n    \}\n\}', text_input_ui + r'        }\n    }\n}', content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
