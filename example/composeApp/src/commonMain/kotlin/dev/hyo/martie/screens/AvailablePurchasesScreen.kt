package dev.hyo.martie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.openiap.*
import io.github.hyochan.kmpiap.toPurchaseInput
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailablePurchasesScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val json = remember { Json { prettyPrint = true; ignoreUnknownKeys = true } }
    
    // Create IAP instance
    val kmpIAP = remember { KmpIAP() }
    
    var isConnecting by remember { mutableStateOf(true) }
    var connected by remember { mutableStateOf(false) }
    var availablePurchases by remember { mutableStateOf<List<Purchase>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var consumeResult by remember { mutableStateOf<String?>(null) }
    var consumingPurchaseId by remember { mutableStateOf<String?>(null) }
    
    // Initialize connection and load available purchases
    LaunchedEffect(Unit) {
        scope.launch {
            isConnecting = true
            isLoading = true
            try {
                val connectionResult = kmpIAP.initConnection()
                connected = connectionResult
                
                if (!connectionResult) {
                    errorMessage = "Failed to connect to store"
                    return@launch
                }
                
                // Connection successful, immediately load available purchases
                isConnecting = false
                
                // Load purchases with timeout
                val purchasesResult = withTimeoutOrNull(10000) {
                    kmpIAP.getAvailablePurchases()
                }
                
                if (purchasesResult != null) {
                    availablePurchases = purchasesResult
                    if (purchasesResult.isEmpty()) {
                        errorMessage = "No active purchases found"
                    } else {
                        errorMessage = null
                    }
                } else {
                    errorMessage = "Loading purchases timed out"
                }
                
            } catch (e: Exception) {
                errorMessage = "Failed to initialize: ${e.message}"
                connected = false
            } finally {
                isConnecting = false
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (connected) AppColors.Success else AppColors.Surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = if (connected) Color.White else AppColors.Primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = when {
                            isConnecting -> "Connecting..."
                            connected -> "✓ Connected to Store"
                            else -> "Not connected"
                        },
                        fontWeight = FontWeight.Medium,
                        color = if (connected) Color.White else AppColors.OnSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Refresh Button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Purchases (${availablePurchases.size})",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    IconButton(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                try {
                                    val purchases = kmpIAP.getAvailablePurchases()
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
                        enabled = !isRefreshing && connected
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Error Message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = AppColors.Secondary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Loading State
            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (availablePurchases.isEmpty() && errorMessage == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No purchases found. Try restoring purchases.",
                        modifier = Modifier.padding(16.dp),
                        color = AppColors.Secondary
                    )
                }
            } else {
                // Purchase List
                availablePurchases.forEach { purchase ->
                    // Determine if this is a subscription based on product ID or other criteria
                    // Subscriptions typically have different product IDs or we can check the product type
                    val isSubscription = purchase.productId.contains("premium") || 
                                       purchase.productId.contains("subscription") ||
                                       purchase.productId.contains("sub_")
                    
                    // Check if already acknowledged (for Android) or restored (for iOS)
                    // Restored iOS transactions are already finished and cannot be finished again
                    val isAcknowledged = when (purchase) {
                        is PurchaseAndroid -> purchase.isAcknowledgedAndroid == true
                        is PurchaseIOS -> purchase.purchaseState == PurchaseState.Restored
                        else -> false
                    }
                    
                    PurchaseCard(
                        purchase = purchase,
                        isSubscription = isSubscription,
                        isAcknowledged = isAcknowledged,
                        onAction = {
                            // Only allow action if not already acknowledged (for subscriptions)
                            // or if it's a consumable (which can be consumed multiple times)
                            if (!isAcknowledged || !isSubscription) {
                                scope.launch {
                                    consumingPurchaseId = purchase.productId
                                    try {
                                        // For subscriptions, acknowledge only (don't consume)
                                        // For consumables, consume them
                                        kmpIAP.finishTransaction(purchase.toPurchaseInput(), isConsumable = !isSubscription)
                                        
                                        val action = if (isSubscription) "acknowledged" else "consumed"
                                        consumeResult = "✅ Purchase $action: ${purchase.productId}"
                                        
                                        // Only remove consumables from the list, keep subscriptions
                                        if (!isSubscription) {
                                            availablePurchases = availablePurchases.filter { it.productId != purchase.productId }
                                        } else {
                                            // Refresh the purchases list instead of trying to copy immutable types
                                            try {
                                                availablePurchases = kmpIAP.getAvailablePurchases()
                                            } catch (e: Exception) {
                                                println("Failed to refresh purchases: ${e.message}")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        val action = if (isSubscription) "acknowledge" else "consume"
                                        consumeResult = "❌ Failed to $action: ${e.message}"
                                    } finally {
                                        consumingPurchaseId = null
                                    }
                                }
                            } else {
                                consumeResult = "ℹ️ Subscription already acknowledged"
                            }
                        },
                        isProcessing = consumingPurchaseId == purchase.productId
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            // Consume Result
            consumeResult?.let { result ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.contains("✅")) 
                            AppColors.Success.copy(alpha = 0.1f) 
                        else 
                            AppColors.Error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(16.dp),
                        color = AppColors.OnSurface
                    )
                }
            }
        }
    }
}

@Composable
fun PurchaseCard(
    purchase: Purchase,
    isSubscription: Boolean,
    isAcknowledged: Boolean,
    onAction: () -> Unit,
    isProcessing: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Log purchase details to console in JSON format
                println("\n========== PURCHASE DETAILS (JSON) ==========")
                val json = Json { 
                    prettyPrint = true
                    encodeDefaults = true
                }
                
                // Serialize based on concrete type since Purchase is an interface
                val jsonString = when (purchase) {
                    is PurchaseAndroid -> json.encodeToString(purchase)
                    is PurchaseIOS -> json.encodeToString(purchase)
                    else -> {
                        val platform = purchase.platform.toJson()
                        """
                        {
                          "id": "${purchase.id}",
                          "productId": "${purchase.productId}",
                          "transactionDate": ${purchase.transactionDate},
                          "platform": "$platform"
                        }
                        """.trimIndent()
                    }
                }
                println(jsonString)
                println("Is Subscription: $isSubscription")
                println("Is Acknowledged: $isAcknowledged")
                println("====================================\n")
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Product ID: ${purchase.productId}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = AppColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Transaction ID: ${purchase.id}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = AppColors.Secondary
            )

            val instant = kotlinx.datetime.Instant.fromEpochSeconds(purchase.transactionDate.toLong())
            Text(
                text = "Date: $instant",
                fontSize = 12.sp,
                color = AppColors.Secondary
            )
            
            // Show transaction state for iOS purchases
            if (purchase is PurchaseIOS) {
                Text(
                    text = "State: ${purchase.purchaseState}",
                    fontSize = 12.sp,
                    color = AppColors.Secondary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Show product type and acknowledgment status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Type: ${if (isSubscription) "Subscription" else "Consumable"}",
                    fontSize = 12.sp,
                    color = if (isSubscription) AppColors.Primary else AppColors.Secondary,
                    fontWeight = FontWeight.Medium
                )
                
                if (isSubscription && isAcknowledged) {
                    val statusText = when (purchase) {
                        is PurchaseAndroid -> "✓ Acknowledged"
                        is PurchaseIOS -> "✓ Finished"
                        else -> "✓ Processed"
                    }
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = AppColors.Success,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Show button based on acknowledgment status
            if (isSubscription && isAcknowledged) {
                // Show disabled state for already acknowledged subscriptions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.Surface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val statusText = when (purchase) {
                            is PurchaseAndroid -> "Already Acknowledged"
                            is PurchaseIOS -> "Already Finished"
                            else -> "Already Processed"
                        }
                        Text(
                            text = statusText,
                            color = AppColors.Secondary
                        )
                    }
                }
            } else {
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubscription) AppColors.Secondary else AppColors.Primary
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        val buttonText = if (isSubscription) {
                            when (purchase) {
                                is PurchaseAndroid -> "Acknowledge Subscription"
                                is PurchaseIOS -> "Finish Transaction"
                                else -> "Process Subscription"
                            }
                        } else {
                            "Consume Purchase"
                        }
                        Text(buttonText)
                    }
                }
            }
        }
    }
}
