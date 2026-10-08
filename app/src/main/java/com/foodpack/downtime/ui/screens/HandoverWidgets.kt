package com.foodpack.downtime.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared building blocks for the Morning / Evening handover forms. */

@Composable
fun HandoverSectionHeader(text: String) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun YesNoDetailField(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    detail: String,
    onDetailChange: (String) -> Unit,
    detailLabel: String = "Add detail"
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            FilterChip(selected = !checked, onClick = { onCheckedChange(false) }, label = { Text("No") })
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(selected = checked, onClick = { onCheckedChange(true) }, label = { Text("Yes") })
        }
        if (checked) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = detail,
                onValueChange = onDetailChange,
                label = { Text(detailLabel) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
    }
}

@Composable
fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter { c -> c.isDigit() }) },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true
    )
}

@Composable
fun TimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("e.g. 13:14") },
        modifier = modifier,
        singleLine = true
    )
}

@Composable
fun TextDetailField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true
    )
}

@Composable
fun TeamBlock(
    teamName: String,
    plannedPpl: String,
    onPlannedPplChange: (String) -> Unit,
    actualPpl: String,
    onActualPplChange: (String) -> Unit,
    plannedFinish: String,
    onPlannedFinishChange: (String) -> Unit,
    actualFinish: String,
    onActualFinishChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(teamName, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Planned ppl", plannedPpl, onPlannedPplChange, Modifier.weight(1f))
                NumberField("Actual ppl", actualPpl, onActualPplChange, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeField("Finish (planned)", plannedFinish, onPlannedFinishChange, Modifier.weight(1f))
                TimeField("Finish (actual)", actualFinish, onActualFinishChange, Modifier.weight(1f))
            }
        }
    }
}
