package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisTheme
import kotlinx.coroutines.delay
import android.content.Intent
import com.example.services.JarvisService

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.utils.PermissionsHelper
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      JarvisTheme {
        JarvisApp()
      }
    }
  }
}

@Composable
fun JarvisApp() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = "permissions") {
        composable("permissions") {
            PermissionsScreen(
                onAllPermissionsGranted = {
                    navController.navigate("home") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                JarvisScreen(
                    modifier = Modifier.padding(innerPadding),
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// Conversation state enum
enum class AssistantState {
  IDLE,
  LISTENING,
  UNDERSTANDING,
  SPEAKING
}

// Data class for chat history
data class ChatMessage(
  val id: Int,
  val text: String,
  val isUser: Boolean,
  val isSystemMessage: Boolean = false
)

@Composable
fun JarvisScreen(
  modifier: Modifier = Modifier,
  onNavigateToSettings: () -> Unit = {}
) {
  var currentState by remember { mutableStateOf(AssistantState.IDLE) }
  
  // Dummy conversation for UI preview
  val chatHistory = remember {
    listOf(
      ChatMessage(1, "System: Wake word active", false, true),
      ChatMessage(2, "JARVIS, kal 5 baje mummy ko call karta hoon.", true),
      ChatMessage(3, "Noted sir, I will remind you.", false),
      ChatMessage(4, "System: Proactive pattern detected (YouTube at 17:40)", false, true)
    )
  }

  // Simulate state changes for the preview
  LaunchedEffect(Unit) {
    while (true) {
      delay(3000)
      currentState = AssistantState.LISTENING
      delay(2000)
      currentState = AssistantState.UNDERSTANDING
      delay(1500)
      currentState = AssistantState.SPEAKING
      delay(3000)
      currentState = AssistantState.IDLE
    }
  }

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    TopBar(onSettingsClick = onNavigateToSettings)
    
    // Core visualizer
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      contentAlignment = Alignment.Center
    ) {
      JarvisVisualizer(state = currentState)
    }

    // Status Text
    Text(
      text = currentState.name,
      color = MaterialTheme.colorScheme.primary,
      style = MaterialTheme.typography.titleMedium,
      letterSpacing = 4.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(vertical = 16.dp)
    )

    // Chat history and proactive suggestions
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1.2f)
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        .padding(16.dp)
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        reverseLayout = true,
        contentPadding = PaddingValues(vertical = 8.dp)
      ) {
        items(chatHistory.reversed()) { message ->
          MessageBubble(message = message)
          Spacer(modifier = Modifier.height(12.dp))
        }
      }
    }

    val context = LocalContext.current
    Button(
      onClick = {
        val serviceIntent = Intent(context, JarvisService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
      },
      modifier = Modifier.padding(vertical = 8.dp)
    ) {
      Text("Start Background Service & Overlay")
    }

    // Proactive Suggestion Card
    ProactiveSuggestionCard()
  }
}

@Composable
fun TopBar(onSettingsClick: () -> Unit = {}) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "J.A.R.V.I.S",
      color = MaterialTheme.colorScheme.onBackground,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Black,
      letterSpacing = 2.sp
    )
    IconButton(onClick = onSettingsClick) {
      Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Settings",
        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
      )
    }
  }
}

@Composable
fun JarvisVisualizer(state: AssistantState) {
  val infiniteTransition = rememberInfiniteTransition(label = "visualizer_anim")
  
  // Base pulsing animation
  val pulseAnim by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = if (state == AssistantState.IDLE) 1.0f else 1.3f,
    animationSpec = infiniteRepeatable(
      animation = tween(if (state == AssistantState.SPEAKING) 300 else 1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

  val rotationAnim by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(if (state == AssistantState.UNDERSTANDING) 1000 else 8000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  val primaryColor = MaterialTheme.colorScheme.primary
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val surfaceColor = MaterialTheme.colorScheme.surface

  Box(
    modifier = Modifier
      .size(200.dp)
      .drawBehind {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.width / 2
        
        // Outer glow
        drawCircle(
          brush = Brush.radialGradient(
            colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent),
            center = center,
            radius = maxRadius * 1.5f * pulseAnim
          )
        )

        // Middle ring
        drawArc(
          brush = Brush.sweepGradient(
            colors = listOf(primaryColor, secondaryColor, primaryColor),
            center = center
          ),
          startAngle = rotationAnim,
          sweepAngle = 270f,
          useCenter = false,
          style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
          size = this.size.copy(width = maxRadius * 1.5f, height = maxRadius * 1.5f),
          topLeft = Offset(maxRadius * 0.25f, maxRadius * 0.25f)
        )

        // Inner solid circle
        drawCircle(
          color = if (state == AssistantState.LISTENING) primaryColor else surfaceColor,
          radius = maxRadius * 0.6f * pulseAnim
        )
      },
    contentAlignment = Alignment.Center
  ) {
    if (state == AssistantState.LISTENING) {
      Icon(
        imageVector = Icons.Default.Mic,
        contentDescription = "Listening",
        tint = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.size(48.dp)
      )
    } else {
      Text(
        text = "J",
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.displayLarge,
        fontWeight = FontWeight.Black
      )
    }
  }
}

@Composable
fun MessageBubble(message: ChatMessage) {
  val align = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
  val bgColor = when {
    message.isSystemMessage -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    message.isUser -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
  }
  val textColor = when {
    message.isSystemMessage -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
    message.isUser -> MaterialTheme.colorScheme.onBackground
    else -> MaterialTheme.colorScheme.primary
  }

  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = align) {
    Text(
      text = message.text,
      modifier = Modifier
        .clip(RoundedCornerShape(
          topStart = 16.dp,
          topEnd = 16.dp,
          bottomStart = if (message.isUser || message.isSystemMessage) 16.dp else 4.dp,
          bottomEnd = if (!message.isUser || message.isSystemMessage) 16.dp else 4.dp
        ))
        .background(bgColor)
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .widthIn(max = 280.dp),
      color = textColor,
      style = if (message.isSystemMessage) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
      textAlign = if (message.isSystemMessage) TextAlign.Center else TextAlign.Start
    )
  }
}

@Composable
fun ProactiveSuggestionCard() {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 24.dp, end = 24.dp, bottom = 32.dp, top = 8.dp),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.NotificationsActive,
          contentDescription = "Proactive Suggestion",
          tint = MaterialTheme.colorScheme.secondary
        )
      }
      Spacer(modifier = Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Routine Detected",
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Open YouTube? (Usually opened at 17:40)",
          color = MaterialTheme.colorScheme.onBackground,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(top = 4.dp)
        )
      }
      TextButton(onClick = { /* Execute Tool */ }) {
        Text("YES", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun JarvisPreview() {
  JarvisTheme {
    Scaffold(modifier = Modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background) {
      JarvisScreen(modifier = Modifier.padding(it))
    }
  }
}
