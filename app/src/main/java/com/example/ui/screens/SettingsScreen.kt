package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SettingsRepository
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkCharcoal
import com.example.ui.theme.ErrorRedAlert
import com.example.ui.theme.OledBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SilverText
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceBorderHighlight
import com.example.ui.viewmodel.SaveStatus
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.ValidationState
import com.example.utils.PermissionsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository.getInstance(context) }
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(repository))
    
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val validationState by viewModel.validationState.collectAsStateWithLifecycle()
    val saveStatus by viewModel.saveStatus.collectAsStateWithLifecycle()


    var inputKey by remember { mutableStateOf("") }
    val systemPrompt by viewModel.systemPrompt.collectAsStateWithLifecycle()
    val modelName by viewModel.modelName.collectAsStateWithLifecycle()
    val wakeThreshold by viewModel.wakeThreshold.collectAsStateWithLifecycle()
    val sessionTimeout by viewModel.sessionTimeout.collectAsStateWithLifecycle()

    var inputSystemPrompt by remember(systemPrompt) { mutableStateOf(systemPrompt) }
    var inputModelName by remember(modelName) { mutableStateOf(modelName) }
    var inputWakeThreshold by remember(wakeThreshold) { mutableStateOf(wakeThreshold.toString()) }
    var inputSessionTimeout by remember(sessionTimeout) { mutableStateOf((sessionTimeout / 1000).toString()) }

    var passwordVisible by remember { mutableStateOf(false) }

    val hasMic = PermissionsHelper.hasAudioPermission(context)
    val hasOverlay = PermissionsHelper.hasOverlayPermission(context)
    val hasNotif = PermissionsHelper.hasNotificationPermission(context)
    val coreGranted = hasMic && hasOverlay

    LaunchedEffect(apiKey) {
        inputKey = apiKey
    }

    LaunchedEffect(saveStatus) {
        if (saveStatus is SaveStatus.Success) {
            val msg = (saveStatus as SaveStatus.Success).message
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "JARVIS CONFIGURATION",
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Key Saved Indicator Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, SurfaceBorder),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
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
                            imageVector = if (apiKey.isNotBlank()) Icons.Default.Lock else Icons.Default.ErrorOutline,
                            contentDescription = "Status Icon",
                            tint = if (apiKey.isNotBlank()) PureWhite else ErrorRedAlert,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (apiKey.isNotBlank()) "API Key Stored On Device" else "No API Key Saved",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = if (apiKey.isNotBlank()) {
                                val masked = if (apiKey.length > 8) "••••••••" + apiKey.takeLast(4) else "••••••••"
                                "Saved: $masked"
                            } else {
                                "Enter your Google Gemini API key below to activate JARVIS."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SilverText
                        )
                    }
                }
            }

            Text(
                text = "GEMINI API KEY SETUP",
                style = MaterialTheme.typography.labelSmall,
                color = SilverText,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = inputKey,
                onValueChange = { 
                    inputKey = it 
                    viewModel.clearValidationState()
                },
                label = { Text("Gemini API Key (AI Studio)", color = SilverText) },
                placeholder = { Text("AIzaSy...", color = SilverText.copy(alpha = 0.5f)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) "Hide password" else "Show password"
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description, tint = SilverText)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PureWhite,
                    unfocusedBorderColor = SurfaceBorderHighlight,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedContainerColor = DarkCharcoal,
                    unfocusedContainerColor = DarkCharcoal
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.testApiKey(inputKey) },
                    enabled = inputKey.isNotBlank() && validationState !is ValidationState.Testing,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SurfaceBorderHighlight),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite)
                ) {
                    if (validationState is ValidationState.Testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PureWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.NetworkCheck, 
                            contentDescription = "Test", 
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TEST KEY", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                }

                Button(
                    onClick = { viewModel.saveApiKey(inputKey) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = OledBlack
                    )
                ) {
                    Icon(
                        Icons.Default.Save, 
                        contentDescription = "Save", 
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SAVE KEY", fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                }
            }
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
            
            when (val state = validationState) {
                is ValidationState.Success -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F291E))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle, 
                                contentDescription = "Valid", 
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.message,
                                color = PureWhite,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                is ValidationState.Error -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ErrorRedAlert.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = ErrorRedAlert.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline, 
                                contentDescription = "Invalid", 
                                tint = ErrorRedAlert,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = state.error,
                                color = PureWhite,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                else -> {}
            }

            if (saveStatus is SaveStatus.Success) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardSurface)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = (saveStatus as SaveStatus.Success).message,
                        style = MaterialTheme.typography.labelMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Dedicated Permissions Card Section
            Text(
                text = "SYSTEM PERMISSIONS & ACCESS",
                style = MaterialTheme.typography.labelSmall,
                color = SilverText,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                onClick = onNavigateToPermissions,
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
                            .background(PureWhite.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Permissions",
                            tint = PureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Manage System Permissions",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = if (coreGranted) "Core system permissions granted" else "Missing required core permissions",
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

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceBorder),
                colors = CardDefaults.cardColors(containerColor = DarkCharcoal)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔒 HARDWARE ENCRYPTION & SECURITY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = SilverText,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your API Key is encrypted locally via Android KeyStore (EncryptedSharedPreferences). It is strictly transmitted directly to Google AI Studio endpoints over TLS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SilverText
                    )
                }
            }
        }
    }
}
