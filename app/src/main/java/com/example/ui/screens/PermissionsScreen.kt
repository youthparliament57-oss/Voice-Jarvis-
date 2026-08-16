package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.utils.PermissionsHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(
    onAllPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Standard runtime permissions
    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS
        )
    )

    // Special permissions state
    var hasOverlay by remember { mutableStateOf(PermissionsHelper.hasOverlayPermission(context)) }
    var hasUsageStats by remember { mutableStateOf(PermissionsHelper.hasUsageStatsPermission(context)) }

    // Launchers for special permissions
    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlay = PermissionsHelper.hasOverlayPermission(context)
        if (hasOverlay && hasUsageStats && permissionState.allPermissionsGranted) {
            onAllPermissionsGranted()
        }
    }

    val usageStatsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasUsageStats = PermissionsHelper.hasUsageStatsPermission(context)
        if (hasOverlay && hasUsageStats && permissionState.allPermissionsGranted) {
            onAllPermissionsGranted()
        }
    }

    // Effect to check if everything is granted initially or after returning from settings
    LaunchedEffect(permissionState.allPermissionsGranted, hasOverlay, hasUsageStats) {
        if (permissionState.allPermissionsGranted && hasOverlay && hasUsageStats) {
            onAllPermissionsGranted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "JARVIS Initialization",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Standard Permissions
        PermissionItem(
            title = "Microphone & Contacts",
            description = "Required for voice commands and calling capabilities.",
            isGranted = permissionState.allPermissionsGranted,
            onRequest = { permissionState.launchMultiplePermissionRequest() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Overlay Permission
        PermissionItem(
            title = "Display Over Other Apps",
            description = "Required to show the JARVIS visualizer while you use other apps.",
            isGranted = hasOverlay,
            onRequest = { overlayLauncher.launch(PermissionsHelper.getOverlayPermissionIntent(context)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Usage Stats Permission
        PermissionItem(
            title = "Usage Access",
            description = "Required for JARVIS to understand your context and routines proactively.",
            isGranted = hasUsageStats,
            onRequest = { usageStatsLauncher.launch(PermissionsHelper.getUsageStatsPermissionIntent()) }
        )
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isGranted) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            if (!isGranted) {
                Button(
                    onClick = onRequest,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("Grant")
                }
            }
        }
    }
}
