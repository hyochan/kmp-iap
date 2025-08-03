package dev.hyo.martie.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hyo.martie.navigation.Screen
import io.github.hyochan.kmpiap.KmpIap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (Screen) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KMP IAP Example") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Library Version",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = KmpIap.getVersion(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Button(
                onClick = { onNavigate(Screen.AvailablePurchases) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Available Purchases")
            }

            Button(
                onClick = { onNavigate(Screen.PurchaseFlow) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Purchase Flow")
            }

            Button(
                onClick = { onNavigate(Screen.SubscriptionFlow) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Subscription Flow")
            }

            Button(
                onClick = { onNavigate(Screen.OfferCode) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Offer Code")
            }
        }
    }
}