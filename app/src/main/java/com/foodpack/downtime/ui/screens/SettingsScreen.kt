package com.foodpack.downtime.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foodpack.downtime.data.DowntimeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: DowntimeRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var operatorName by remember { mutableStateOf("") }
    var webhookUrl by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        operatorName = repository.settings.operatorName.first()
        webhookUrl = repository.settings.webhookUrl.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Your name", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = operatorName,
                onValueChange = { operatorName = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Maria") }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Google Sheet link (Apps Script Web App URL)", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = webhookUrl,
                onValueChange = { webhookUrl = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://script.google.com/macros/s/.../exec") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Set up once by a supervisor: deploy the included Apps Script to your team's " +
                    "Google Sheet, then paste the Web App URL it gives you here. Every phone " +
                    "on the team uses the same URL.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        repository.settings.setOperatorName(operatorName.trim())
                        repository.settings.setWebhookUrl(webhookUrl.trim())
                        repository.retrySync()
                        saved = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            if (saved) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Saved.", color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
