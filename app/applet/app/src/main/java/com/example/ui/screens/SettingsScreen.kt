package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodels.SettingsViewModel
import androidx.navigation.NavController
import com.example.ui.viewmodels.SettingsViewModelFactory
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val apiKey by viewModel.apiKey.collectAsState(initial = "")
    var currentInput by remember { mutableStateOf(apiKey ?: "") }
    var hasSaved by remember { mutableStateOf(false) }

    LaunchedEffect(apiKey) {
        if (apiKey != null) {
            currentInput = apiKey!!
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "API Configuration",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = currentInput,
                onValueChange = { currentInput = it; hasSaved = false },
                label = { Text("Gemini API Key") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = { 
                    viewModel.saveApiKey(currentInput)
                    hasSaved = true
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (hasSaved) "Saved!" else "Save Key")
            }
            
            Text(
                "Your API key is stored securely on this device using EncryptedSharedPreferences. It is only used to communicate with Google's Gemini API.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
