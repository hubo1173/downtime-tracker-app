package com.foodpack.downtime.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodpack.downtime.data.BreakEntry
import com.foodpack.downtime.data.DowntimeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TEAMS = listOf("Team A", "Team B", "Team C")
private val BREAK_TYPES = listOf("Morning", "Afternoon")

/** Log each team's morning / afternoon break start and end time. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreaksScreen(repository: DowntimeRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val entries by repository.observeBreaks().collectAsState(initial = emptyList())
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val clock = remember { SimpleDateFormat("HH:mm", Locale.US) }

    var team by remember { mutableStateOf(TEAMS.first()) }
    var breakType by remember { mutableStateOf(BREAK_TYPES.first()) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("☕ Breaks") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Team", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TEAMS.forEach { t ->
                        FilterChip(selected = team == t, onClick = { team = t }, label = { Text(t, fontSize = 16.sp) })
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Break", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BREAK_TYPES.forEach { b ->
                        FilterChip(selected = breakType == b, onClick = { breakType = b }, label = { Text("$b break", fontSize = 16.sp) })
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = start, onValueChange = { start = it },
                        label = { Text("Start (e.g. 09:30)") }, singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { start = clock.format(Date()) }) { Text("Now") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = end, onValueChange = { end = it },
                        label = { Text("End (e.g. 09:45)") }, singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { end = clock.format(Date()) }) { Text("Now") }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        scope.launch {
                            saving = true
                            val operator = repository.settings.operatorName.first()
                            repository.saveBreak(
                                BreakEntry(
                                    createdAt = System.currentTimeMillis(), date = today, operatorName = operator,
                                    team = team, breakType = breakType, startTime = start.trim(), endTime = end.trim()
                                )
                            )
                            start = ""; end = ""
                            saving = false
                        }
                    },
                    enabled = !saving && (start.isNotBlank() || end.isNotBlank()),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) { Text("Save break", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { HandoverShare.shareToWhatsApp(context, HandoverShare.breaksText(today, entries)) },
                    enabled = entries.any { it.date == today },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("💬 Send today's breaks on WhatsApp", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Logged breaks", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            if (entries.isEmpty()) {
                item { Text("None yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(entries, key = { it.id }) { b ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${b.team} · ${b.breakType} break", fontWeight = FontWeight.Bold)
                            Text("${b.startTime.ifBlank { "-" }} to ${b.endTime.ifBlank { "-" }}  ·  ${b.date}", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { HandoverShare.shareToWhatsApp(context, HandoverShare.breakText(b)) }) { Text("💬") }
                        IconButton(onClick = { scope.launch { repository.deleteBreak(b) } }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
