package com.foodpack.downtime.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Quick line-speed check: tap once per pack as it comes off the line for
 * 30 seconds, then see the count and the extrapolated packs-per-minute.
 * This is a standalone utility — it doesn't save anything to the log.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineSpeedScreen(onBack: () -> Unit) {
    val countStartSeconds = 30
    var secondsLeft by remember { mutableStateOf(countStartSeconds) }
    var count by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft -= 1
            }
            isRunning = false
            finished = true
        }
    }

    fun start() {
        count = 0
        secondsLeft = countStartSeconds
        finished = false
        isRunning = true
    }

    fun reset() {
        count = 0
        secondsLeft = countStartSeconds
        finished = false
        isRunning = false
    }

    val perMinute = count * (60 / countStartSeconds)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⏱️ Line Speed Check") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
            Text(
                "Tap the button once for every pack that comes off the line. We'll count for 30 seconds and work out packs per minute.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isRunning) "${secondsLeft}s left" else if (finished) "Done" else "Ready",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$count",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("packs counted", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                isRunning -> {
                    Button(
                        onClick = { count += 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Text("TAP — Pack", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }
                finished -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📊 Result", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$count packs / 30s",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "≈ $perMinute packs / min",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { start() },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Run again")
                    }
                }
                else -> {
                    Button(
                        onClick = { start() },
                        modifier = Modifier.fillMaxWidth().height(64.dp)
                    ) {
                        Text("Start 30s Count", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isRunning || finished) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { reset() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Reset")
                }
            }
        }
    }
}
