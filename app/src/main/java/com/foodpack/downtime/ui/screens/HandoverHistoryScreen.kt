package com.foodpack.downtime.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foodpack.downtime.data.DowntimeRepository
import com.foodpack.downtime.data.HandoverEntry
import com.foodpack.downtime.data.HandoverType
import kotlinx.coroutines.launch

/** Every Morning/Evening handover sent so far, newest first, with edit/delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandoverHistoryScreen(
    repository: DowntimeRepository,
    onBack: () -> Unit,
    onEditMorning: (Long) -> Unit,
    onEditEvening: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val entries by repository.observeHandovers().collectAsState(initial = emptyList())
    var pendingDelete by remember { mutableStateOf<HandoverEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Handover History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No handovers sent yet.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                HandoverCard(
                    entry = entry,
                    onEdit = {
                        if (entry.type == HandoverType.MORNING) onEditMorning(entry.id) else onEditEvening(entry.id)
                    },
                    onDelete = { pendingDelete = entry }
                )
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this handover?") },
            text = {
                val label = if (entry.type == HandoverType.MORNING) "Morning Update" else "End of Shift Update"
                Text("$label — ${entry.date}. This also removes it from the Google Sheet.")
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deleteHandover(entry) }
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HandoverCard(
    entry: HandoverEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val label = if (entry.type == HandoverType.MORNING) "🌅 Start of Shift" else "🌇 End of Shift"
                Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
            Text(entry.date, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (entry.operatorName.isNotBlank()) Text(entry.operatorName)
            entry.lastSyncError?.let {
                Text(
                    "Sync issue: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
