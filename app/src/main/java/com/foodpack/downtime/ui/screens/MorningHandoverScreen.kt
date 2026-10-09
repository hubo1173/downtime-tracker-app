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
fun MorningHandoverScreen(repository: DowntimeRepository, entryId: Long? = null, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var existing by remember { mutableStateOf<HandoverEntry?>(null) }
    var loaded by remember { mutableStateOf(entryId == null) }

    var sheIncident by remember { mutableStateOf(false) }
    var sheIncidentDetail by remember { mutableStateOf("") }
    var technicalIssue by remember { mutableStateOf(false) }
    var technicalDetail by remember { mutableStateOf("") }
    var qualityIssue by remember { mutableStateOf(false) }
    var qualityDetail by remember { mutableStateOf("") }
    var downtimeIssue by remember { mutableStateOf(false) }
    var downtimeDetail by remember { mutableStateOf("") }

    var todaysVolume by remember { mutableStateOf("") }
    var tomorrowsVolume by remember { mutableStateOf("") }

    var teamAPlanned by remember { mutableStateOf("") }
    var teamAActual by remember { mutableStateOf("") }
    var teamAFinishPlanned by remember { mutableStateOf("") }
    var teamAFinishActual by remember { mutableStateOf("") }
    var teamBPlanned by remember { mutableStateOf("") }
    var teamBActual by remember { mutableStateOf("") }
    var teamBFinishPlanned by remember { mutableStateOf("") }
    var teamBFinishActual by remember { mutableStateOf("") }
    var teamCPlanned by remember { mutableStateOf("") }
    var teamCActual by remember { mutableStateOf("") }
    var teamCFinishPlanned by remember { mutableStateOf("") }
    var teamCFinishActual by remember { mutableStateOf("") }

    var packingPpl by remember { mutableStateOf("") }
    var packingTrainees by remember { mutableStateOf("") }

    var absence by remember { mutableStateOf(false) }
    var absenceNames by remember { mutableStateOf("") }
    var rtw by remember { mutableStateOf(false) }
    var rtwNames by remember { mutableStateOf("") }
    var notOnRota by remember { mutableStateOf(false) }
    var notOnRotaNames by remember { mutableStateOf("") }

    var riskToService by remember { mutableStateOf(false) }
    var riskToServiceDetail by remember { mutableStateOf("") }

    var agencyCollation by remember { mutableStateOf("") }
    var agencyPrep by remember { mutableStateOf("") }
    var agencyHygiene by remember { mutableStateOf("") }
    var agencyFridge by remember { mutableStateOf("") }
    var agencyPackagingStore by remember { mutableStateOf("") }

    var saving by remember { mutableStateOf(false) }

    // Edit mode: load the existing entry once and pre-fill every field.
    LaunchedEffect(entryId) {
        if (entryId != null) {
            val entry = repository.getHandoverById(entryId)
            existing = entry
            if (entry != null) {
                sheIncident = entry.sheIncident; sheIncidentDetail = entry.sheIncidentDetail
                technicalIssue = entry.technicalIssue; technicalDetail = entry.technicalDetail
                qualityIssue = entry.qualityIssue; qualityDetail = entry.qualityDetail
                downtimeIssue = entry.downtimeIssue; downtimeDetail = entry.downtimeDetail
                todaysVolume = entry.todaysVolume?.toString() ?: ""
                tomorrowsVolume = entry.tomorrowsVolume?.toString() ?: ""
                teamAPlanned = entry.teamAPlanned?.toString() ?: ""
                teamAActual = entry.teamAActual?.toString() ?: ""
                teamAFinishPlanned = entry.teamAFinishPlanned
                teamAFinishActual = entry.teamAFinishActual
                teamBPlanned = entry.teamBPlanned?.toString() ?: ""
                teamBActual = entry.teamBActual?.toString() ?: ""
                teamBFinishPlanned = entry.teamBFinishPlanned
                teamBFinishActual = entry.teamBFinishActual
                teamCPlanned = entry.teamCPlanned?.toString() ?: ""
                teamCActual = entry.teamCActual?.toString() ?: ""
                teamCFinishPlanned = entry.teamCFinishPlanned
                teamCFinishActual = entry.teamCFinishActual
                packingPpl = entry.packingPpl?.toString() ?: ""
                packingTrainees = entry.packingTrainees?.toString() ?: ""
                absence = entry.absence; absenceNames = entry.absenceNames
                rtw = entry.rtw; rtwNames = entry.rtwNames
                notOnRota = entry.notOnRota; notOnRotaNames = entry.notOnRotaNames
                riskToService = entry.riskToService; riskToServiceDetail = entry.riskToServiceDetail
                agencyCollation = entry.agencyCollation?.toString() ?: ""
                agencyPrep = entry.agencyPrep?.toString() ?: ""
                agencyHygiene = entry.agencyHygiene?.toString() ?: ""
                agencyFridge = entry.agencyFridge?.toString() ?: ""
                agencyPackagingStore = entry.agencyPackagingStore?.toString() ?: ""
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
                type = HandoverType.MORNING,
                date = existing?.date ?: today,
                operatorName = operator,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                sheIncident = sheIncident, sheIncidentDetail = sheIncidentDetail,
                technicalIssue = technicalIssue, technicalDetail = technicalDetail,
                qualityIssue = qualityIssue, qualityDetail = qualityDetail,
                downtimeIssue = downtimeIssue, downtimeDetail = downtimeDetail,
                todaysVolume = todaysVolume.toIntOrNull(),
                tomorrowsVolume = tomorrowsVolume.toIntOrNull(),
                teamAPlanned = teamAPlanned.toIntOrNull(), teamAActual = teamAActual.toIntOrNull(),
                teamAFinishPlanned = teamAFinishPlanned, teamAFinishActual = teamAFinishActual,
                teamBPlanned = teamBPlanned.toIntOrNull(), teamBActual = teamBActual.toIntOrNull(),
                teamBFinishPlanned = teamBFinishPlanned, teamBFinishActual = teamBFinishActual,
                teamCPlanned = teamCPlanned.toIntOrNull(), teamCActual = teamCActual.toIntOrNull(),
                teamCFinishPlanned = teamCFinishPlanned, teamCFinishActual = teamCFinishActual,
                packingPpl = packingPpl.toIntOrNull(), packingTrainees = packingTrainees.toIntOrNull(),
                absence = absence, absenceNames = absenceNames,
                rtw = rtw, rtwNames = rtwNames,
                notOnRota = notOnRota, notOnRotaNames = notOnRotaNames,
                riskToService = riskToService, riskToServiceDetail = riskToServiceDetail,
                agencyCollation = agencyCollation.toIntOrNull(),
                agencyPrep = agencyPrep.toIntOrNull(),
                agencyHygiene = agencyHygiene.toIntOrNull(),
                agencyFridge = agencyFridge.toIntOrNull(),
                agencyPackagingStore = agencyPackagingStore.toIntOrNull(),
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
                title = { Text(if (entryId != null) "🌅 Edit Morning Update" else "🌅 Morning Update") },
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

            HandoverSectionHeader("In the last 24hrs")
            YesNoDetailField("SHE incidents and accidents", sheIncident, { sheIncident = it }, sheIncidentDetail, { sheIncidentDetail = it })
            YesNoDetailField("Technical", technicalIssue, { technicalIssue = it }, technicalDetail, { technicalDetail = it })
            YesNoDetailField("Quality", qualityIssue, { qualityIssue = it }, qualityDetail, { qualityDetail = it })
            YesNoDetailField("Downtime", downtimeIssue, { downtimeIssue = it }, downtimeDetail, { downtimeDetail = it })

            HandoverSectionHeader("Volumes")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Today's volume", todaysVolume, { todaysVolume = it }, Modifier.weight(1f))
                NumberField("Tomorrow's volume", tomorrowsVolume, { tomorrowsVolume = it }, Modifier.weight(1f))
            }

            HandoverSectionHeader("Teams")
            TeamBlock("Team A", teamAPlanned, { teamAPlanned = it }, teamAActual, { teamAActual = it }, teamAFinishPlanned, { teamAFinishPlanned = it }, teamAFinishActual, { teamAFinishActual = it })
            TeamBlock("Team B", teamBPlanned, { teamBPlanned = it }, teamBActual, { teamBActual = it }, teamBFinishPlanned, { teamBFinishPlanned = it }, teamBFinishActual, { teamBFinishActual = it })
            TeamBlock("Team C", teamCPlanned, { teamCPlanned = it }, teamCActual, { teamCActual = it }, teamCFinishPlanned, { teamCFinishPlanned = it }, teamCFinishActual, { teamCFinishActual = it })

            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Packing ppl", packingPpl, { packingPpl = it }, Modifier.weight(1f))
                NumberField("Of which on training", packingTrainees, { packingTrainees = it }, Modifier.weight(1f))
            }

            HandoverSectionHeader("People")
            YesNoDetailField("Absence", absence, { absence = it }, absenceNames, { absenceNames = it }, detailLabel = "Names")
            YesNoDetailField("RTW (return to work)", rtw, { rtw = it }, rtwNames, { rtwNames = it }, detailLabel = "Names")
            YesNoDetailField("Not on rota", notOnRota, { notOnRota = it }, notOnRotaNames, { notOnRotaNames = it }, detailLabel = "Names")

            HandoverSectionHeader("Risk to Service")
            YesNoDetailField("Risk to service", riskToService, { riskToService = it }, riskToServiceDetail, { riskToServiceDetail = it }, detailLabel = "What")

            HandoverSectionHeader("Agency")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Collation", agencyCollation, { agencyCollation = it }, Modifier.weight(1f))
                NumberField("Prep", agencyPrep, { agencyPrep = it }, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Hygiene", agencyHygiene, { agencyHygiene = it }, Modifier.weight(1f))
                NumberField("Fridge", agencyFridge, { agencyFridge = it }, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            NumberField("Packaging store", agencyPackagingStore, { agencyPackagingStore = it }, Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = { save() },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    if (saving) "Saving…" else if (existing != null) "Save Changes" else "Send Morning Update",
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
