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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodpack.downtime.data.DowntimeEntry
import com.foodpack.downtime.data.DowntimeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: DowntimeRepository,
    onLogNew: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLineSpeed: () -> Unit,
    onOpenSleeveCalc: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenHandover: () -> Unit,
    onOpenBreaks: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val openTimers by repository.observeOpenTimers().collectAsState(initial = emptyList())
    val allEntries by repository.observeAll().collectAsState(initial = emptyList())
    val unsyncedCount by repository.observeUnsyncedCount().collectAsState(initial = 0)

    // Recompute today's totals every 30s so a running stop keeps nudging the total up.
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }
    val todayStats = remember(allEntries, openTimers, tick) { computeTodayStats(allEntries, openTimers) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📡 Unit K Pulse") },
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onLogNew,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🚨", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Issue / Downtime", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
                Button(
                    onClick = onOpenLineSpeed,
                    modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⏱️", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Line Speed", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TodayStatsCard(stats = todayStats)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onOpenSleeveCalc,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("📦", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Poke Bowl Sleeve Calculator", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenNotes,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("📝", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Notes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (unsyncedCount > 0) {
                AssistChip(
                    onClick = { scope.launch { repository.retrySync() } },
                    label = { Text("$unsyncedCount entr${if (unsyncedCount == 1) "y" else "ies"} waiting to sync — tap to retry") }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (openTimers.isNotEmpty()) {
                    Text(
                        "🔴 Running now",
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
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("No downtime running right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Pinned to the bottom of the screen: Breaks above Handover.
            OutlinedButton(
                onClick = onOpenBreaks,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("☕", fontSize = 26.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Breaks", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onOpenHandover,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("🔄", fontSize = 26.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Handover", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
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

private data class TodayStats(
    val totalMinutes: Int,
    val topReason: String?,
    val topReasonMinutes: Int
)

/**
 * Sums today's downtime (closed entries by their recorded duration, open
 * timers by their live elapsed time) and finds which fault cost the most
 * minutes today.
 */
private fun computeTodayStats(all: List<DowntimeEntry>, open: List<DowntimeEntry>): TodayStats {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val startOfDay = cal.timeInMillis
    val now = System.currentTimeMillis()

    val openIds = open.map { it.id }.toSet()
    val minutesByReason = mutableMapOf<String, Int>()
    var total = 0

    for (entry in all) {
        if (entry.createdAt < startOfDay) continue
        if (entry.id in openIds) continue // counted below from live open list
        val minutes = entry.durationMinutes ?: 0
        total += minutes
        minutesByReason[entry.reason] = (minutesByReason[entry.reason] ?: 0) + minutes
    }
    for (entry in open) {
        if (entry.createdAt < startOfDay) continue
        val minutes = ((now - (entry.startTime ?: entry.createdAt)) / 60_000L).toInt().coerceAtLeast(0)
        total += minutes
        minutesByReason[entry.reason] = (minutesByReason[entry.reason] ?: 0) + minutes
    }

    val top = minutesByReason.maxByOrNull { it.value }
    return TodayStats(total, top?.key, top?.value ?: 0)
}

@Composable
private fun TodayStatsCard(stats: TodayStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📊 Today's Downtime", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${stats.totalMinutes} min",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            if (stats.topReason != null && stats.topReasonMinutes > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Top Issue Today", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Text(
                    "${stats.topReason} — ${stats.topReasonMinutes} min",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
