package com.foodpack.downtime.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodpack.downtime.data.DowntimeEntry
import com.foodpack.downtime.data.DowntimeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: DowntimeRepository,
    onLogNew: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val openTimers by repository.observeOpenTimers().collectAsState(initial = emptyList())
    val unsyncedCount by repository.observeUnsyncedCount().collectAsState(initial = 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downtime Log") },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = "History")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onLogNew,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
            ) {
                Text("+ Log Issue / Downtime", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (unsyncedCount > 0) {
                AssistChip(
                    onClick = { scope.launch { repository.retrySync() } },
                    label = { Text("$unsyncedCount entr${if (unsyncedCount == 1) "y" else "ies"} waiting to sync — tap to retry") }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (openTimers.isNotEmpty()) {
                Text(
                    "Running now",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(openTimers) { entry ->
                        OpenTimerCard(entry = entry) {
                            scope.launch { repository.stopTimer(entry.id, System.currentTimeMillis()) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(40.dp))
                Text("No downtime running right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OpenTimerCard(entry: DowntimeEntry, onStop: () -> Unit) {
    var elapsedMs by remember { mutableStateOf(System.currentTimeMillis() - (entry.startTime ?: entry.createdAt)) }

    LaunchedEffect(entry.id) {
        while (true) {
            elapsedMs = System.currentTimeMillis() - (entry.startTime ?: entry.createdAt)
            delay(1000)
        }
    }

    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMs) % 60

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${entry.line} · ${entry.station}", fontWeight = FontWeight.Bold)
                if (!entry.part.isNullOrBlank()) Text(entry.part)
                Text(entry.reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    String.format("%02d:%02d elapsed", minutes, seconds),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Button(onClick = onStop) {
                Text("Stop")
            }
        }
    }
}
