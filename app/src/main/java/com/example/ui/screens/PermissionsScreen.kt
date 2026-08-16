package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkCharcoal
import com.example.ui.theme.ErrorRedAlert
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceBorderHighlight
import com.example.utils.PermissionsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onAllPermissionsGranted: () -> Unit,
    onSkip: () -> Unit = {},
    isFromSettings: Boolean = false,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasMicPermission by remember { mutableStateOf(PermissionsHelper.hasAudioPermission(context)) }
    var hasOverlayPermission by remember { mutableStateOf(PermissionsHelper.hasOverlayPermission(context)) }
    var hasNotificationPermission by remember { mutableStateOf(PermissionsHelper.hasNotificationPermission(context)) }
    var hasContactsPermission by remember { mutableStateOf(PermissionsHelper.hasContactsPermission(context)) }

    fun refreshPermissions() {
        val newMic = PermissionsHelper.hasAudioPermission(context)
        val newOverlay = PermissionsHelper.hasOverlayPermission(context)
        val newNotif = PermissionsHelper.hasNotificationPermission(context)
        val newContacts = PermissionsHelper.hasContactsPermission(context)

        if (!hasMicPermission && newMic) {
            Toast.makeText(context, "Microphone Permission Granted!", Toast.LENGTH_SHORT).show()
        }
        if (!hasOverlayPermission && newOverlay) {
            Toast.makeText(context, "Display Over Other Apps Granted!", Toast.LENGTH_SHORT).show()
        }

        hasMicPermission = newMic
        hasOverlayPermission = newOverlay
        hasNotificationPermission = newNotif
        hasContactsPermission = newContacts
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Microphone Permission Granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone Access Required for Voice Commands", Toast.LENGTH_LONG).show()
        }
        refreshPermissions()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        refreshPermissions()
    }

    val contactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPermission = isGranted
        refreshPermissions()
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPermissions()
    }

    val allCoreGranted = hasMicPermission && hasOverlayPermission

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isFromSettings) {
            TopAppBar(
                title = {
                    Text(
                        text = "SYSTEM PERMISSIONS",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = PureWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OledBlack,
                    titleContentColor = PureWhite
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isFromSettings) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "PERMISSIONS SETUP",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Text(
                        text = "Configure system access required for local wake-word detection and floating alien assistant 👽 HUD overlay.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverText,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Microphone Access
                    item {
                        PermissionCard(
                            icon = Icons.Default.Mic,
                            title = "Microphone Access",
                            description = "Required for continuous 'Hey Jarvis' wake word audio sampling.",
                            isGranted = hasMicPermission,
                            isRequired = true,
                            onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                        )
                    }

                    // 2. Display Over Other Apps (Overlay)
                    item {
                        PermissionCard(
                            icon = Icons.Default.Window,
                            title = "Display Over Other Apps (Overlay)",
                            description = "Required to draw the floating alien assistant 👽 HUD on top of other apps when activated.",
                            isGranted = hasOverlayPermission,
                            isRequired = true,
                            onRequest = {
                                overlayLauncher.launch(PermissionsHelper.getOverlayPermissionIntent(context))
                            }
                        )
                    }

                    // 3. Notifications
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        item {
                            PermissionCard(
                                icon = Icons.Default.Notifications,
                                title = "Notifications",
                                description = "Enables foreground service status notification bar controls.",
                                isGranted = hasNotificationPermission,
                                isRequired = false,
                                onRequest = {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            )
                        }
                    }

                    // 4. Contacts Access
                    item {
                        PermissionCard(
                            icon = Icons.Default.People,
                            title = "Contacts Access",
                            description = "Optional. Enables voice calling and contact lookup commands.",
                            isGranted = hasContactsPermission,
                            isRequired = false,
                            onRequest = {
                                contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (allCoreGranted) {
                            onAllPermissionsGranted()
                        } else {
                            Toast.makeText(
                                context,
                                "Microphone & Overlay permissions are recommended for full background operation.",
                                Toast.LENGTH_LONG
                            ).show()
                            onAllPermissionsGranted()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = OledBlack
                    )
                ) {
                    Text(
                        text = if (allCoreGranted) "ACTIVATE JARVIS NOW" else "CONTINUE TO DASHBOARD",
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SurfaceBorderHighlight)
                ) {
                    Text(
                        text = "SKIP FOR NOW",
                        fontWeight = FontWeight.Bold,
                        color = SilverText,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isRequired: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    .background(if (isGranted) PureWhite.copy(alpha = 0.1f) else DarkCardSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) PureWhite else SilverText
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                    if (isRequired) {
                        Text(
                            text = " *",
                            color = ErrorRedAlert,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SilverText,
                    modifier = Modifier.padding(top = 4.dp)
                )

                AnimatedVisibility(visible = isGranted) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Granted",
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GRANTED & ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!isGranted) {
                Button(
                    onClick = onRequest,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = OledBlack
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Grant", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
