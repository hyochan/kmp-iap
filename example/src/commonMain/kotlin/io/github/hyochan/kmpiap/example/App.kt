package io.github.hyochan.kmpiap.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.hyochan.kmpiap.createInAppPurchase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val iap = remember { createInAppPurchase() }
    
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("KMP IAP Example") }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "KMP In-App Purchase",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    Card(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = iap.getVersion(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    Text(
                        text = "IAP functionality will be implemented",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Button(
                        onClick = { /* TODO: Initialize IAP */ }
                    ) {
                        Text("Initialize IAP")
                    }
                }
            }
        }
    }
}