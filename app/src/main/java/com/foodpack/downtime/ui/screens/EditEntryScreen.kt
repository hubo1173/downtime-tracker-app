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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foodpack.downtime.data.Constants
import com.foodpack.downtime.data.DowntimeRepository
import kotlinx.coroutines.launch

/**
 * Edits an already-logged entry in place. Pre-fills from the existing
 * row; Save overwrites it and re-syncs (updating the Sheet row rather
 * than creating a new one).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditEntryScreen(
    repository: DowntimeRepository,
    entryId: Long,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }

    var line by remember { mutableStateOf(Constants.LINES.first()) }
    var category by remember { mutableStateOf<String?>(null) }
    var equipment by remember { mutableStateOf<String?>(null) }
    var fault by remember { mutableStateOf<String?>(null) }
    var customFault by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        val existing = repository.getById(entryId) ?: run { onDone(); return@LaunchedEffect }
        line = existing.line
        category = existing.station
        equipment = existing.part
        val faultOptionsNow = if (existing.part != null) {
            Constants.faultsFor(existing.station, existing.part)
        } else {
            Constants.faultsFor(existing.station)
        }
        if (faultOptionsNow.contains(existing.reason)) {
            fault = existing.reason
        } else {
            fault = "Other"
            customFault = existing.reason
        }
        note = existing.note
        durationMinutes = existing.durationMinutes?.toString() ?: ""
        loaded = true
    }

    val needsEquipment = category?.let { Constants.hasEquipmentStep(it) } == true

    fun onCategoryChange(newCategory: String) {
        category = newCategory
        equipment = null
        fault = null
        customFault = ""
    }
    fun onEquipmentChange(newEquipment: String) {
        equipment = newEquipment
        fault = null
        customFault = ""
    }

    val faultOptions: List<String> = when {
        category == null -> emptyList()
        needsEquipment && equipment != null -> Constants.faultsFor(category!!, equipment!!)
        !needsEquipment -> Constants.faultsFor(category!!)
        else -> emptyList()
    }

    fun effectiveFault(): String? =
        if (fault == "Other") customFault.ifBlank { null } else fault

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("✏️ Edit entry") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (!loaded) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
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
            EditSectionLabel("Line")
            EditChipRow(options = Constants.LINES, selected = line, onSelect = { line = it })

            Spacer(modifier = Modifier.height(24.dp))
            EditSectionLabel("What caused the stop?")
            EditFlowRowChips(
                options = Constants.CATEGORIES,
                selected = category,
                onSelect = { onCategoryChange(it) },
                icons = Constants.CATEGORY_ICONS
            )

            if (needsEquipment) {
                Spacer(modifier = Modifier.height(24.dp))
                EditSectionLabel("Equipment")
                EditFlowRowChips(
                    options = Constants.equipmentFor(category!!),
                    selected = equipment,
                    onSelect = { onEquipmentChange(it) },
                    icons = Constants.EQUIPMENT_ICONS
                )
            }

            val showFaults = category != null && (!needsEquipment || equipment != null)
            if (showFaults) {
                Spacer(modifier = Modifier.height(24.dp))
                EditSectionLabel("Specifically")
                EditFlowRowChips(options = faultOptions, selected = fault, onSelect = { fault = it })

                if (fault == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customFault,
                        onValueChange = { customFault = it },
                        label = { Text("Describe it") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = durationMinutes,
                onValueChange = { durationMinutes = it.filter { c -> c.isDigit() } },
                label = { Text("Minutes lost (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = {
                    scope.launch {
                        val f = effectiveFault() ?: return@launch
                        val cat = category ?: return@launch
                        val existing = repository.getById(entryId) ?: return@launch
                        saving = true
                        repository.editEntry(
                            existing.copy(
                                line = line,
                                station = cat,
                                part = if (needsEquipment) equipment else null,
                                reason = f,
                                note = note,
                                durationMinutes = durationMinutes.toIntOrNull()
                            )
                        )
                        saving = false
                        onDone()
                    }
                },
                enabled = effectiveFault() != null && !saving,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (saving) "Saving…" else "Save changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun EditSectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun EditChipRow(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option, fontSize = 15.sp) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditFlowRowChips(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    icons: Map<String, String> = emptyMap()
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val icon = icons[option]
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                        if (icon != null) {
                            Text(icon, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(option, fontSize = 16.sp)
                    }
                }
            )
        }
    }
}
