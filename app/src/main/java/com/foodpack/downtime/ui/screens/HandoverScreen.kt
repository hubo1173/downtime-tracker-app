package com.foodpack.downtime.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shift handover: pick Start of Shift or End of Shift. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandoverScreen(
    onBack: () -> Unit,
    onOpenMorning: () -> Unit,
    onOpenEvening: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔄 Handover") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = "Handover History")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Sent to the shared Sheet, same as everything else.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Button(
                onClick = onOpenMorning,
                modifier = Modifier.fillMaxWidth().height(72.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌅 Start of Shift", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Morning Update", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onOpenEvening,
                modifier = Modifier.fillMaxWidth().height(72.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌇 End of Shift", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("End of Shift Update", fontSize = 13.sp)
                }
            }
        }
    }
}
