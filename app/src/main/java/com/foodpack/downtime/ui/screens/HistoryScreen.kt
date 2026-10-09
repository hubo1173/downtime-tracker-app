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
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
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
    onBack: () -> Unit,
    onEditEntry: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val entries by repository.observeAll().collectAsState(initial = emptyList())
    val unsyncedCount by repository.observeUnsyncedCount().collectAsState(initial = 0)
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.US) }
    var pendingDelete by remember { mutableStateOf<DowntimeEntry?>(null) }

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
            items(entries, key = { it.id }) { entry ->
                EntryCard(
                    entry = entry,
                    dateFormat = dateFormat,
                    onEdit = { onEditEntry(entry.id) },
                    onDelete = { pendingDelete = entry }
                )
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this entry?") },
            text = { Text("${entry.line} · ${entry.station} — ${entry.reason}. This also removes it from the Google Sheet.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository.deleteEntry(entry) }
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
private fun EntryCard(
    entry: DowntimeEntry,
    dateFormat: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
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
                entry.peopleAffected?.let {
                    Text(
                        "  ·  $it ppl affected",
                        style = MaterialTheme.typography.bodySmall
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
            if (entry.photoUrl != null) {
                TextButton(onClick = {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.photoUrl))) } catch (_: Exception) {}
                }) { Text("📷 View photo") }
            } else if (entry.photoPath != null) {
                Text("📷 Photo waiting to upload", style = MaterialTheme.typography.bodySmall)
            }
            entry.lastSyncError?.let {
                Text(
                    "Sync issue: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (!entry.isOpen) {
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
}
