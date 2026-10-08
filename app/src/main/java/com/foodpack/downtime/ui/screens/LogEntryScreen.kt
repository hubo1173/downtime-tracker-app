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
import androidx.compose.ui.unit.sp
import com.foodpack.downtime.data.Constants
import com.foodpack.downtime.data.DowntimeEntry
import com.foodpack.downtime.data.DowntimeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Line -> Category -> Equipment (Machinery only) -> Fault.
 * Each step only appears once the previous one is answered, so the
 * operator never sees more than one decision at a time and the whole
 * flow fits comfortably in a few taps.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogEntryScreen(
    repository: DowntimeRepository,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var line by remember { mutableStateOf(Constants.LINES.first()) }
    var category by remember { mutableStateOf<String?>(null) }
    var equipment by remember { mutableStateOf<String?>(null) }
    var fault by remember { mutableStateOf<String?>(null) }
    var customFault by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var manualMinutes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val needsEquipment = category?.let { Constants.hasEquipmentStep(it) } == true

    // Resetting a step clears everything that depends on it.
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

    fun buildEntry(now: Long): DowntimeEntry? {
        val f = effectiveFault() ?: return null
        val cat = category ?: return null
        return DowntimeEntry(
            createdAt = now,
            operatorName = "", // filled just before saving, from settings
            line = line,
            station = cat,
            part = if (needsEquipment) equipment else null,
            reason = f,
            note = note
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📝 Log entry") },
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

            Spacer(modifier = Modifier.height(24.dp))
            SectionLabel("What caused the stop?")
            FlowRowChips(
                options = Constants.CATEGORIES,
                selected = category,
                onSelect = { onCategoryChange(it) },
                big = true,
                icons = Constants.CATEGORY_ICONS
            )

            if (needsEquipment) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionLabel("Equipment")
                FlowRowChips(
                    options = Constants.equipmentFor(category!!),
                    selected = equipment,
                    onSelect = { onEquipmentChange(it) },
                    big = true,
                    icons = Constants.EQUIPMENT_ICONS
                )
            }

            val showFaults = category != null && (!needsEquipment || equipment != null)
            if (showFaults) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionLabel("Specifically")
                FlowRowChips(
                    options = faultOptions,
                    selected = fault,
                    onSelect = { fault = it },
                    big = true
                )

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
                enabled = effectiveFault() != null && !saving,
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Text("Start Downtime Timer", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                enabled = effectiveFault() != null && !saving,
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
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun ChipRow(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
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
private fun FlowRowChips(
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    big: Boolean = false,
    icons: Map<String, String> = emptyMap()
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val icon = icons[option]
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = {
                    Text(
                        if (icon != null) "$icon  $option" else option,
                        fontSize = if (big) 16.sp else 14.sp,
                        modifier = if (big) Modifier.padding(vertical = 6.dp) else Modifier
                    )
                }
            )
        }
    }
}
