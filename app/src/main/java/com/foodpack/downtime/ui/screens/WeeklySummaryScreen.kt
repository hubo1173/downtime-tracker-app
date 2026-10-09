package com.foodpack.downtime.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodpack.downtime.data.DowntimeRepository
import com.foodpack.downtime.data.DowntimeSummary

/** Minutes lost, people affected and top causes for the last 7 / 30 days, shareable to WhatsApp. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklySummaryScreen(repository: DowntimeRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val entries by repository.observeAll().collectAsState(initial = emptyList())
    var days by remember { mutableStateOf(7) }

    val since = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
    val summary = remember(entries, days) { DowntimeSummary.since(entries, since) }
    val title = if (days == 7) "Weekly downtime summary (last 7 days)" else "Downtime summary (last 30 days)"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Summary") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = days == 7, onClick = { days = 7 }, label = { Text("Last 7 days", fontSize = 15.sp) })
                FilterChip(selected = days == 30, onClick = { days = 30 }, label = { Text("Last 30 days", fontSize = 15.sp) })
            }
            Spacer(Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("${summary.totalMinutes} min lost", fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("${summary.stops} stops  ·  ${summary.peopleAffected} people affected", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("By category", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            if (summary.byCategory.isEmpty()) Text("Nothing logged in this period.")
            summary.byCategory.forEach {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(it.label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("${it.minutes} min · ${it.people} ppl · ${it.stops}x")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Top causes", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            summary.topCauses.take(5).forEachIndexed { i, g ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("${i + 1}. ${g.label}", modifier = Modifier.weight(1f))
                    Text("${g.minutes} min · ${g.stops}x")
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { HandoverShare.shareToWhatsApp(context, DowntimeSummary.reportText(title, summary)) },
                enabled = summary.stops > 0,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) { Text("💬  Share to WhatsApp", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(32.dp))
        }
    }
}
