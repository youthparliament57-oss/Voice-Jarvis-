package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.SettingsRepository
import com.example.services.JarvisService
import com.example.services.JarvisServiceState
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkCharcoal
import com.example.ui.theme.ErrorRedAlert
import com.example.ui.theme.JarvisTheme
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceBorderHighlight
import com.example.utils.PermissionsHelper

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
    val context = LocalContext.current

    val startDest = remember {
        if (!PermissionsHelper.hasAudioPermission(context) || !PermissionsHelper.hasOverlayPermission(context)) {
            "permissions"
        } else {
            "home"
        }
    }
    
    NavHost(navController = navController, startDestination = startDest) {
        composable("permissions") {
            PermissionsScreen(
                onAllPermissionsGranted = {
                    navController.navigate("home") {
                        popUpTo("permissions") { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate("home") {
                        popUpTo("permissions") { inclusive = true }
                    }
                }
            )
        }
        composable("manage_permissions") {
            PermissionsScreen(
                onAllPermissionsGranted = { navController.popBackStack() },
                onSkip = { navController.popBackStack() },
                isFromSettings = true,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("home") {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = OledBlack
            ) { innerPadding ->
                JarvisScreen(
                    modifier = Modifier.padding(innerPadding),
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToPermissions = { navController.navigate("manage_permissions") }
                )
            }
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPermissions = { navController.navigate("manage_permissions") }
            )
        }
    }
}

@Composable
fun JarvisScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val apiKey by settingsRepository.apiKeyFlow.collectAsStateWithLifecycle(initialValue = "")

    val isServiceRunning by JarvisServiceState.isRunning.collectAsStateWithLifecycle()
    val currentAssistantState by JarvisServiceState.assistantState.collectAsStateWithLifecycle()
    val serviceError by JarvisServiceState.lastError.collectAsStateWithLifecycle()

    val hasAudio = PermissionsHelper.hasAudioPermission(context)
    val hasOverlay = PermissionsHelper.hasOverlayPermission(context)
    val hasNotif = PermissionsHelper.hasNotificationPermission(context)
    val corePermissionsGranted = hasAudio && hasOverlay

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(
            isServiceRunning = isServiceRunning,
            onSettingsClick = onNavigateToSettings
        )

        // Error Alert Banner
        if (serviceError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ErrorRedAlert.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(
                    containerColor = ErrorRedAlert.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = ErrorRedAlert,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SYSTEM NOTICE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = ErrorRedAlert,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = serviceError ?: "System warning detected",
                            style = MaterialTheme.typography.bodySmall,
                            color = PureWhite
                        )
                    }
                    if (apiKey.isBlank()) {
                        Button(
                            onClick = onNavigateToSettings,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRedAlert),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("FIX KEY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        }
                    } else if (!corePermissionsGranted) {
                        Button(
                            onClick = onNavigateToPermissions,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRedAlert),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("GRANT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Futuristic Black & White Visualizer HUD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            contentAlignment = Alignment.Center
        ) {
            JarvisVisualizer(state = currentAssistantState, isRunning = isServiceRunning)
        }

        // Status Label Pill
        val stateLabel = when (currentAssistantState) {
            AssistantState.IDLE -> if (isServiceRunning) "STANDBY // LISTENING" else "SYSTEM INACTIVE"
            AssistantState.LISTENING -> "ACTIVE // LISTENING"
            AssistantState.UNDERSTANDING -> "PROCESSING REQUEST"
            AssistantState.SPEAKING -> "VOICE OUTPUT // SPEAKING"
            AssistantState.ERROR -> "SYSTEM ERROR"
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkCardSurface,
            border = BorderStroke(1.dp, if (currentAssistantState == AssistantState.ERROR) ErrorRedAlert else SurfaceBorder),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentAssistantState == AssistantState.ERROR) ErrorRedAlert
                            else if (isServiceRunning) PureWhite
                            else SilverText.copy(alpha = 0.4f)
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stateLabel,
                    color = PureWhite,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // 1. API Key Status Card
            Card(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SurfaceBorder),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (apiKey.isNotBlank()) PureWhite.copy(alpha = 0.1f) else ErrorRedAlert.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (apiKey.isNotBlank()) Icons.Default.Key else Icons.Default.Warning,
                            contentDescription = "API Key",
                            tint = if (apiKey.isNotBlank()) PureWhite else ErrorRedAlert,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (apiKey.isNotBlank()) "Gemini API Key Configured" else "Gemini API Key Missing",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = if (apiKey.isNotBlank()) "Tap to test or manage API key" else "Tap to enter your Google Gemini API key",
                            style = MaterialTheme.typography.bodySmall,
                            color = SilverText
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Edit",
                        tint = SilverText
                    )
                }
            }

            // 2. Service Control Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SurfaceBorder),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Foreground Service & Activated HUD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Runs local wake-word listener ('Hey Jarvis') in an Android Foreground Service. Floating alien HUD 👽 appears when spoken to.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SilverText
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (!hasAudio || !hasOverlay) {
                                Toast.makeText(context, "Microphone & Overlay permissions required!", Toast.LENGTH_LONG).show()
                                onNavigateToPermissions()
                                return@Button
                            }

                            if (apiKey.isBlank()) {
                                Toast.makeText(context, "Please set Gemini API Key in Settings first!", Toast.LENGTH_LONG).show()
                                onNavigateToSettings()
                                return@Button
                            }

                            val serviceIntent = Intent(context, JarvisService::class.java)
                            if (isServiceRunning) {
                                context.stopService(serviceIntent)
                                Toast.makeText(context, "JARVIS Background Service stopped.", Toast.LENGTH_SHORT).show()
                            } else {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                                Toast.makeText(context, "JARVIS Active! Listening for 'Hey Jarvis'...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceRunning) ErrorRedAlert else PureWhite,
                            contentColor = if (isServiceRunning) PureWhite else OledBlack
                        )
                    ) {
                        Icon(
                            imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Service"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isServiceRunning) "STOP JARVIS SERVICE" else "ACTIVATE JARVIS SERVICE",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // 3. System Permissions Card
            Card(
                onClick = onNavigateToPermissions,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SurfaceBorder),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (corePermissionsGranted) PureWhite.copy(alpha = 0.1f) else ErrorRedAlert.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (corePermissionsGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                            contentDescription = "Permissions",
                            tint = if (corePermissionsGranted) PureWhite else ErrorRedAlert,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "System Access Permissions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = if (corePermissionsGranted) "Core permissions granted (Microphone & Overlay)" else "Permissions missing! Tap to manage",
                            style = MaterialTheme.typography.bodySmall,
                            color = SilverText
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Manage",
                        tint = SilverText
                    )
                }
            }

            // 4. System Architecture Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SurfaceBorder),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "SYSTEM ARCHITECTURE STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = SilverText,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    ArchitectureFeatureRow(
                        title = "Local Wake-Word Listener ('Hey Jarvis')",
                        subtitle = "TFLite / Energy VAD engine",
                        isActive = hasAudio
                    )
                    ArchitectureFeatureRow(
                        title = "Gemini 2.5 Live Voice",
                        subtitle = "Direct WebSocket audio streaming",
                        isActive = apiKey.isNotBlank()
                    )
                    ArchitectureFeatureRow(
                        title = "Activated Alien HUD 👽 Overlay",
                        subtitle = "Appears only on wake-word trigger",
                        isActive = hasOverlay
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun ArchitectureFeatureRow(
    title: String,
    subtitle: String,
    isActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isActive) PureWhite else DarkCardSurface)
                .border(1.dp, if (isActive) PureWhite else SurfaceBorderHighlight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (isActive) OledBlack else SilverText,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = SilverText
            )
        }
    }
}

@Composable
fun TopBar(
    isServiceRunning: Boolean,
    onSettingsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "J.A.R.V.I.S",
                color = PureWhite,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isServiceRunning) PureWhite else SilverText.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isServiceRunning) "SYSTEM ONLINE" else "SYSTEM OFFLINE",
                    color = SilverText,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp
                )
            }
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(DarkCharcoal)
                .border(1.dp, SurfaceBorder, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = PureWhite,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun JarvisVisualizer(state: AssistantState, isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_anim")
    
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (state == AssistantState.IDLE) 1.1f else 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == AssistantState.SPEAKING) 300 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == AssistantState.UNDERSTANDING) 1200 else 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val mainAccent = if (state == AssistantState.ERROR) ErrorRedAlert else PureWhite

    Box(
        modifier = Modifier
            .size(190.dp)
            .drawBehind {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.width / 2
                
                // Monochromatic radial halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(mainAccent.copy(alpha = 0.25f), Color.Transparent),
                        center = center,
                        radius = maxRadius * 1.4f * pulseAnim
                    )
                )

                // Concentric outer rotating orbit arc
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(mainAccent, SilverText, mainAccent),
                        center = center
                    ),
                    startAngle = rotationAnim,
                    sweepAngle = 280f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    size = this.size.copy(width = maxRadius * 1.5f, height = maxRadius * 1.5f),
                    topLeft = Offset(maxRadius * 0.25f, maxRadius * 0.25f)
                )

                // Secondary counter-rotating thin ring
                drawArc(
                    color = mainAccent.copy(alpha = 0.4f),
                    startAngle = -rotationAnim * 1.5f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                    size = this.size.copy(width = maxRadius * 1.7f, height = maxRadius * 1.7f),
                    topLeft = Offset(maxRadius * 0.15f, maxRadius * 0.15f)
                )

                // Inner sphere
                drawCircle(
                    color = if (state == AssistantState.LISTENING || isRunning) mainAccent else DarkCharcoal,
                    radius = maxRadius * 0.5f * pulseAnim
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (state == AssistantState.LISTENING) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Listening",
                tint = OledBlack,
                modifier = Modifier.size(42.dp)
            )
        } else if (state == AssistantState.ERROR) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = PureWhite,
                modifier = Modifier.size(42.dp)
            )
        } else {
            Text(
                text = "J",
                color = if (isRunning) OledBlack else PureWhite,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black
            )
        }
    }
}
