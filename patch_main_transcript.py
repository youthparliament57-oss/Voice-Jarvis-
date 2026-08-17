import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Make sure we have the necessary imports for LazyColumn, etc. (They might already be there, but let's add them to be safe)
imports = """
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.services.TranscriptMessage
"""
content = content.replace("import com.example.AssistantState", "import com.example.AssistantState" + imports)

# Also observe transcript state
val_transcript = """
    val hasNotif = PermissionsHelper.hasNotificationPermission(context)
    val corePermissionsGranted = hasAudio && hasOverlay
    val transcriptList by JarvisServiceState.transcript.collectAsStateWithLifecycle()
"""
content = content.replace("""    val hasNotif = PermissionsHelper.hasNotificationPermission(context)
    val corePermissionsGranted = hasAudio && hasOverlay""", val_transcript)

# Now inject the transcript UI after "Status Label Pill"
transcript_ui = """
        // Transcript History View
        if (transcriptList.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .heightIn(max = 200.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceBorder),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    reverseLayout = true
                ) {
                    items(transcriptList.reversed()) { msg ->
                        val timeString = remember(msg.timestamp) {
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!msg.isUser) {
                                    Text(
                                        text = "JARVIS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PureWhite,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = timeString,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SilverText,
                                    fontSize = 9.sp
                                )
                                if (msg.isUser) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "USER",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SilverText,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (msg.isUser) DarkCardSurface else PureWhite.copy(alpha = 0.1f),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = PureWhite,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
"""
content = re.sub(r'        Column\(\n            modifier = Modifier\n                \.fillMaxWidth\(\)\n                \.padding\(horizontal = 20\.dp, vertical = 8\.dp\)\n        \) \{', transcript_ui + '\n        Column(\n            modifier = Modifier\n                .fillMaxWidth()\n                .padding(horizontal = 20.dp, vertical = 8.dp)\n        ) {', content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
