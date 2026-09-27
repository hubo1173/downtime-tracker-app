package com.foodpack.downtime.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foodpack.downtime.data.DowntimeEntry
import com.foodpack.downtime.data.DowntimeRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    repository: DowntimeRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val entries by repository.observeAll().collectAsState(initial = emptyList())
    val unsyncedCount by repository.observeUnsyncedCount().collectAsState(initial = 0)
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (unsyncedCount > 0) {
                        IconButton(onClick = { scope.launch { repository.retrySync() } }) {
                            Icon(Icons.Filled.Sync, contentDescription = "Sync now")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No entries yet.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries) { entry ->
                EntryCard(entry, dateFormat)
            }
        }
    }
}

@Composable
private fun EntryCard(entry: DowntimeEntry, dateFormat: SimpleDateFormat) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${entry.line} · ${entry.station}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (entry.synced) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Synced",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        Icons.Filled.Sync,
                        contentDescription = "Waiting to sync",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (!entry.part.isNullOrBlank()) Text(entry.part)
            if (!entry.subIssue.isNullOrBlank()) Text(entry.subIssue)
            Text(entry.reason, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (entry.note.isNotBlank()) Text("\"${entry.note}\"")

            Spacer(modifier = Modifier.height(6.dp))
            Row {
                Text(dateFormat.format(Date(entry.createdAt)), style = MaterialTheme.typography.bodySmall)
                entry.durationMinutes?.let {
                    Text(
                        "  ·  $it min",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (entry.isOpen) {
                    Text(
                        "  ·  running",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            entry.lastSyncError?.let {
                Text(
                    "Sync issue: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
