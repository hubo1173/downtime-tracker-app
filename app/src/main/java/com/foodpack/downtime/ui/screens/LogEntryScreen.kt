package com.foodpack.downtime.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foodpack.downtime.data.Constants
import com.foodpack.downtime.data.DowntimeEntry
import com.foodpack.downtime.data.DowntimeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogEntryScreen(
    repository: DowntimeRepository,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var line by remember { mutableStateOf(Constants.LINES.first()) }
    var station by remember { mutableStateOf(Constants.STATIONS.first()) }
    var part by remember { mutableStateOf<String?>(null) }
    var subIssue by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf<String?>(null) }
    var customReason by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var manualMinutes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    // Reset dependent fields when station changes
    LaunchedEffect(station) {
        part = null
        subIssue = null
        reason = null
    }

    val quickReasons = Constants.quickReasonsFor(station)
    val isLabeler = station == "Labeler"
    val isMaterial = station == "Material Supply (Prep)"

    fun effectiveReason(): String? =
        if (reason == "Other") customReason.ifBlank { null } else reason

    fun buildEntry(now: Long): DowntimeEntry? {
        val r = effectiveReason() ?: return null
        return DowntimeEntry(
            createdAt = now,
            operatorName = "", // filled in just before saving, from settings
            line = line,
            station = station,
            part = if (isLabeler) part else null,
            subIssue = if (isMaterial) subIssue else null,
            reason = r,
            note = note
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log entry") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
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
            SectionLabel("Line")
            ChipRow(options = Constants.LINES, selected = line, onSelect = { line = it })

            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel("Station")
            FlowRowChips(options = Constants.STATIONS, selected = station, onSelect = { station = it })

            if (isLabeler) {
                Spacer(modifier = Modifier.height(20.dp))
                SectionLabel("Labeler part")
                FlowRowChips(options = Constants.LABELER_PARTS, selected = part, onSelect = { part = it })
            }

            if (isMaterial) {
                Spacer(modifier = Modifier.height(20.dp))
                SectionLabel("What happened")
                FlowRowChips(options = Constants.MATERIAL_SUB_ISSUES, selected = subIssue, onSelect = { subIssue = it })
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel("Reason")
            FlowRowChips(options = quickReasons + "Other", selected = reason, onSelect = { reason = it })

            if (reason == "Other") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customReason,
                    onValueChange = { customReason = it },
                    label = { Text("Describe the reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(28.dp))
            Text("If the line is stopped right now, start a timer.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        val now = System.currentTimeMillis()
                        val entry = buildEntry(now) ?: return@launch
                        val operator = repository.settings.operatorName.first()
                        saving = true
                        repository.startTimer(entry.copy(operatorName = operator))
                        saving = false
                        onDone()
                    }
                },
                enabled = effectiveReason() != null && !saving,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Start Downtime Timer")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Or log it directly (already resolved / no timer needed):", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = manualMinutes,
                onValueChange = { manualMinutes = it.filter { c -> c.isDigit() } },
                label = { Text("Minutes lost (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val now = System.currentTimeMillis()
                        val entry = buildEntry(now) ?: return@launch
                        val operator = repository.settings.operatorName.first()
                        saving = true
                        repository.logInstant(
                            entry.copy(
                                operatorName = operator,
                                durationMinutes = manualMinutes.toIntOrNull()
                            )
                        )
                        saving = false
                        onDone()
                    }
                },
                enabled = effectiveReason() != null && !saving,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save Entry (no timer)")
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun ChipRow(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowChips(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option) }
            )
        }
    }
}
