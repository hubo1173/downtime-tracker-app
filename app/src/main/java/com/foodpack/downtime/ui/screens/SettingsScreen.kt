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
import com.foodpack.downtime.reminders.ReminderScheduler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
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
    val context = LocalContext.current
    var remindersOn by remember { mutableStateOf(true) }
    var handoverTime by remember { mutableStateOf("06:45") }
    var breaksTime by remember { mutableStateOf("06:15") }

    LaunchedEffect(Unit) {
        operatorName = repository.settings.operatorName.first()
        webhookUrl = repository.settings.webhookUrl.first()
        remindersOn = repository.settings.remindersEnabled.first()
        handoverTime = repository.settings.handoverReminderTime.first()
        breaksTime = repository.settings.breaksReminderTime.first()
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Daily reminders", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Switch(checked = remindersOn, onCheckedChange = { remindersOn = it; saved = false })
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Notifies you before the 6:30am break-times and 7am morning-handover deadlines.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (remindersOn) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = breaksTime, onValueChange = { breaksTime = it; saved = false },
                    label = { Text("Break times reminder (HH:mm)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = handoverTime, onValueChange = { handoverTime = it; saved = false },
                    label = { Text("Morning handover reminder (HH:mm)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        repository.settings.setOperatorName(operatorName.trim())
                        repository.settings.setWebhookUrl(webhookUrl.trim())
                        val (bh, bm) = ReminderScheduler.parse(breaksTime, 6 to 15)
                        val (hh, hm) = ReminderScheduler.parse(handoverTime, 6 to 45)
                        breaksTime = "%02d:%02d".format(bh, bm)
                        handoverTime = "%02d:%02d".format(hh, hm)
                        repository.settings.setReminders(remindersOn, handoverTime, breaksTime)
                        ReminderScheduler.scheduleAll(context.applicationContext)
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
