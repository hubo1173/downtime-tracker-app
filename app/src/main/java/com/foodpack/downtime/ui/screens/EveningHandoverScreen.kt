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
import com.foodpack.downtime.data.HandoverEntry
import com.foodpack.downtime.data.HandoverType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EveningHandoverScreen(repository: DowntimeRepository, entryId: Long? = null, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var existing by remember { mutableStateOf<HandoverEntry?>(null) }
    var loaded by remember { mutableStateOf(entryId == null) }

    var accidentIncident by remember { mutableStateOf(false) }
    var accidentIncidentDetail by remember { mutableStateOf("") }
    var qualityIssue by remember { mutableStateOf(false) }
    var qualityDetail by remember { mutableStateOf("") }
    var downtimeIssue by remember { mutableStateOf(false) }
    var downtimeDetail by remember { mutableStateOf("") }

    var serviceScore by remember { mutableStateOf("") }
    var efficiency by remember { mutableStateOf("") }

    var teamAFinishedTime by remember { mutableStateOf("") }
    var teamBFinishedTime by remember { mutableStateOf("") }
    var teamCFinishedTime by remember { mutableStateOf("") }
    var collationFinished by remember { mutableStateOf("") }
    var trayUpSentBy by remember { mutableStateOf("") }

    var shorts by remember { mutableStateOf(false) }
    var shortsDetail by remember { mutableStateOf("") }
    var reportUpdated by remember { mutableStateOf(false) }
    var reportUpdatedDetail by remember { mutableStateOf("") }
    var aob by remember { mutableStateOf(false) }
    var aobDetail by remember { mutableStateOf("") }

    var saving by remember { mutableStateOf(false) }

    // Edit mode: load the existing entry once and pre-fill every field.
    LaunchedEffect(entryId) {
        if (entryId != null) {
            val entry = repository.getHandoverById(entryId)
            existing = entry
            if (entry != null) {
                accidentIncident = entry.accidentIncident; accidentIncidentDetail = entry.accidentIncidentDetail
                qualityIssue = entry.qualityIssue; qualityDetail = entry.qualityDetail
                downtimeIssue = entry.downtimeIssue; downtimeDetail = entry.downtimeDetail
                serviceScore = entry.serviceScore?.toString() ?: ""
                efficiency = entry.efficiency?.toString() ?: ""
                teamAFinishedTime = entry.teamAFinishedTime
                teamBFinishedTime = entry.teamBFinishedTime
                teamCFinishedTime = entry.teamCFinishedTime
                collationFinished = entry.collationFinished
                trayUpSentBy = entry.trayUpSentBy
                shorts = entry.shorts; shortsDetail = entry.shortsDetail
                reportUpdated = entry.reportUpdated; reportUpdatedDetail = entry.reportUpdatedDetail
                aob = entry.aob; aobDetail = entry.aobDetail
            }
            loaded = true
        }
    }

    fun save(shareToWhatsApp: Boolean = false) {
        scope.launch {
            saving = true
            val operator = existing?.operatorName ?: repository.settings.operatorName.first()
            val entry = HandoverEntry(
                id = existing?.id ?: 0,
                type = HandoverType.EVENING,
                date = existing?.date ?: today,
                operatorName = operator,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                accidentIncident = accidentIncident, accidentIncidentDetail = accidentIncidentDetail,
                qualityIssue = qualityIssue, qualityDetail = qualityDetail,
                downtimeIssue = downtimeIssue, downtimeDetail = downtimeDetail,
                serviceScore = serviceScore.toIntOrNull(),
                efficiency = efficiency.toIntOrNull(),
                teamAFinishedTime = teamAFinishedTime,
                teamBFinishedTime = teamBFinishedTime,
                teamCFinishedTime = teamCFinishedTime,
                collationFinished = collationFinished,
                trayUpSentBy = trayUpSentBy,
                shorts = shorts, shortsDetail = shortsDetail,
                reportUpdated = reportUpdated, reportUpdatedDetail = reportUpdatedDetail,
                aob = aob, aobDetail = aobDetail,
                synced = existing?.synced ?: false,
                sheetRowCreated = existing?.sheetRowCreated ?: false
            )
            if (existing != null) repository.editHandover(entry) else repository.saveHandover(entry)
            if (shareToWhatsApp) HandoverShare.shareToWhatsApp(context, HandoverShare.text(entry))
            saving = false
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId != null) "🌇 Edit End of Shift Update" else "🌇 End of Shift Update") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!loaded) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(existing?.date ?: today, color = MaterialTheme.colorScheme.onSurfaceVariant)

            HandoverSectionHeader("Summary")
            YesNoDetailField("Accident / incident", accidentIncident, { accidentIncident = it }, accidentIncidentDetail, { accidentIncidentDetail = it })
            YesNoDetailField("Quality", qualityIssue, { qualityIssue = it }, qualityDetail, { qualityDetail = it })
            YesNoDetailField("Downtime", downtimeIssue, { downtimeIssue = it }, downtimeDetail, { downtimeDetail = it })

            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Service score %", serviceScore, { serviceScore = it }, Modifier.weight(1f))
                NumberField("Efficiency %", efficiency, { efficiency = it }, Modifier.weight(1f))
            }

            HandoverSectionHeader("Finish times")
            TimeField("Team A finished", teamAFinishedTime, { teamAFinishedTime = it }, Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            TimeField("Team B finished", teamBFinishedTime, { teamBFinishedTime = it }, Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            TimeField("Team C finished", teamCFinishedTime, { teamCFinishedTime = it }, Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            TimeField("Collation finished", collationFinished, { collationFinished = it }, Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            TimeField("Tray up sent by", trayUpSentBy, { trayUpSentBy = it }, Modifier.fillMaxWidth())

            HandoverSectionHeader("Wrap-up")
            YesNoDetailField("Shorts", shorts, { shorts = it }, shortsDetail, { shortsDetail = it })
            YesNoDetailField("Report updated", reportUpdated, { reportUpdated = it }, reportUpdatedDetail, { reportUpdatedDetail = it })
            YesNoDetailField("AOB", aob, { aob = it }, aobDetail, { aobDetail = it })

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = { save() },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    if (saving) "Saving…" else if (existing != null) "Save Changes" else "Send End of Shift Update",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { save(shareToWhatsApp = true) },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("💬  Save & share to WhatsApp", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
