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

/**
 * How many sleeves of poke bowls to pull for an order: 50 bowls per sleeve,
 * so sleeves needed = order amount ÷ 50 (rounded up), with any leftover
 * packs shown separately. Standalone utility — doesn't save to the log.
 */
private const val PACKS_PER_SLEEVE = 50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleeveCalculatorScreen(onBack: () -> Unit) {
    var orderAmountText by remember { mutableStateOf("") }
    val orderAmount = orderAmountText.toIntOrNull()

    val fullSleeves = orderAmount?.let { it / PACKS_PER_SLEEVE } ?: 0
    val remainder = orderAmount?.let { it % PACKS_PER_SLEEVE } ?: 0
    val sleevesNeeded = orderAmount?.let { (it + PACKS_PER_SLEEVE - 1) / PACKS_PER_SLEEVE } ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📦 Poke Bowl Sleeves") },
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
                .padding(20.dp)
        ) {
            Text(
                "Enter the order amount — we'll work out how many sleeves to pull ($PACKS_PER_SLEEVE poke bowls per sleeve).",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = orderAmountText,
                onValueChange = { orderAmountText = it.filter { c -> c.isDigit() } },
                label = { Text("Order amount (poke bowls)") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (orderAmount != null && orderAmount > 0) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Sleeves to pull", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "$sleevesNeeded",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (remainder == 0) {
                            Text(
                                "$fullSleeves full sleeve${if (fullSleeves == 1) "" else "s"} — exact",
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                "$fullSleeves full sleeve${if (fullSleeves == 1) "" else "s"} + $remainder pack${if (remainder == 1) "" else "s"} from the next sleeve",
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
