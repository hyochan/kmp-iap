package dev.hyo.martie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.hyo.martie.utils.swipeToBack
import dev.hyo.martie.theme.AppColors
import io.github.hyochan.kmpiap.KmpIAP.*
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailablePurchasesScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val json = remember { Json { prettyPrint = true; ignoreUnknownKeys = true } }
    
    // KmpIAP methods are now directly accessible via wildcard import
    var connected by remember { mutableStateOf(false) }
    var availablePurchases by remember { mutableStateOf<List<Purchase>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var consumeResult by remember { mutableStateOf<String?>(null) }
    var consumingPurchaseId by remember { mutableStateOf<String?>(null) }
    
    // Collect connection state
    LaunchedEffect(Unit) {
        launch {
            connectionStateFlow.collect { connectionResult ->
                connected = connectionResult.connected
            }
        }
    }
    
    // Initialize connection
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            initConnection()
        } catch (e: Exception) {
            errorMessage = "Failed to initialize: ${e.message}"
            isLoading = false
        }
    }
    
    // Load available purchases when connected
    LaunchedEffect(connected) {
        if (connected) {
            // Add a small delay to ensure connection is fully established
            kotlinx.coroutines.delay(500)
            try {
                val purchases = getAvailablePurchases()
                availablePurchases = purchases
                if (purchases.isEmpty()) {
                    errorMessage = "No active purchases found"
                } else {
                    errorMessage = null
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load purchases: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Purchases") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = AppColors.OnSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppColors.Background)
                .swipeToBack(navController)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Purchases",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.OnSurface
                )
                
                // Refresh Button
                IconButton(
                    onClick = {
                        scope.launch {
                            isRefreshing = true
                            errorMessage = null
                            try {
                                val purchases = getAvailablePurchases()
                                availablePurchases = purchases
                                if (purchases.isEmpty()) {
                                    errorMessage = "No active purchases found"
                                } else {
                                    errorMessage = null
                                }
                            } catch (e: Exception) {
                                errorMessage = "Failed to refresh: ${e.message}"
                            } finally {
                                isRefreshing = false
                            }
                        }
                    },
                    enabled = !isRefreshing && !isLoading
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AppColors.Primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AppColors.Primary
                        )
                    }
                }
            }
            
            Text(
                text = "Status: ${if (connected) "Connected" else "Not Connected"}",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Loading state
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Primary)
                }
            }
            
            // Error state
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFC62828)
                    )
                }
            }
            
            // Consume result
            consumeResult?.let { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            result.contains("✅") -> Color(0xFFE8F5E9)
                            result.contains("❌") -> Color(0xFFFFEBEE)
                            else -> Color(0xFFFFF8E1)
                        }
                    )
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(16.dp),
                        color = when {
                            result.contains("✅") -> Color(0xFF2E7D32)
                            result.contains("❌") -> Color(0xFFC62828)
                            else -> Color(0xFFF57F17)
                        }
                    )
                }
            }
            
            // Purchases list
            if (!isLoading && availablePurchases.isNotEmpty()) {
                availablePurchases.forEach { purchase ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = purchase.productId,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = AppColors.OnSurface
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            purchase.purchaseToken?.let { token ->
                                Text(
                                    text = "Token: ${token.take(20)}...",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            
                            Text(
                                text = "Date: ${purchase.transactionDate}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            
                            if (purchase.platform == IAPPlatform.ANDROID) {
                                Row(
                                    modifier = Modifier.padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "State: ${purchase.purchaseStateAndroid ?: "unknown"}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Acknowledged: ${purchase.isAcknowledgedAndroid ?: false}",
                                        fontSize = 12.sp,
                                        color = if (purchase.isAcknowledgedAndroid == true) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                    )
                                }
                            }
                            
                            // Consume button for testing (Android only)
                            if (purchase.platform == IAPPlatform.ANDROID && purchase.purchaseToken != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        consumingPurchaseId = purchase.productId
                                        scope.launch {
                                            try {
                                                val result = finishTransaction(
                                                    purchase = purchase,
                                                    isConsumable = true
                                                )
                                                consumeResult = if (result) {
                                                    "✅ Purchase consumed successfully"
                                                } else {
                                                    "⚠️ Failed to consume purchase"
                                                }
                                                // Refresh the list
                                                val purchases = getAvailablePurchases()
                                                availablePurchases = purchases
                                            } catch (e: Exception) {
                                                consumeResult = "❌ Error: ${e.message}"
                                            } finally {
                                                consumingPurchaseId = null
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = consumingPurchaseId != purchase.productId,
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                                ) {
                                    if (consumingPurchaseId == purchase.productId) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Consume (Test Only)")
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (!isLoading && connected && availablePurchases.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Active Purchases",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = AppColors.OnSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You don't have any active purchases yet",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpansionCard(
    title: String,
    content: String
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show", fontSize = 12.sp)
                }
            }
            
            if (expanded) {
                Text(
                    content,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                )
            }
        }
    }
}