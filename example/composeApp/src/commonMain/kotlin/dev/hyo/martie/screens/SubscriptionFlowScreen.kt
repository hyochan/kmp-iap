package dev.hyo.martie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import dev.hyo.martie.theme.AppColors
import dev.hyo.martie.utils.swipeToBack
import io.github.hyochan.kmpiap.ErrorCode
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val SUBSCRIPTION_IDS = listOf("dev.hyo.martie.premium")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionFlowScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val json = remember { Json { prettyPrint = true; ignoreUnknownKeys = true } }
    
    // Create IAP instance
    val kmpIAP = remember { KmpIAP() }
    
    var isConnecting by remember { mutableStateOf(true) }
    var isLoadingProducts by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var purchaseResult by remember { mutableStateOf<String?>(null) }
    var transactionResult by remember { mutableStateOf<String?>(null) }
    var initError by remember { mutableStateOf<String?>(null) }
    
    var connected by remember { mutableStateOf(false) }
    var subscriptions by remember { mutableStateOf<List<Product>>(emptyList()) }
    var activeSubscriptions by remember { mutableStateOf<List<ActiveSubscription>>(emptyList()) }
    var hasActiveSubscription by remember { mutableStateOf(false) }
    var currentError by remember { mutableStateOf<PurchaseError?>(null) }
    var currentPurchase by remember { mutableStateOf<Purchase?>(null) }
    
    // Register purchase event listeners
    LaunchedEffect(Unit) {
        launch {
            kmpIAP.purchaseUpdatedListener.collect { purchase ->
                currentPurchase = purchase
                
                // Handle successful purchase
                purchaseResult = """
                    ✅ Subscription successful (${purchase.platform})
                    Product: ${purchase.productId}
                    Transaction ID: ${purchase.transactionId ?: "N/A"}
                    Date: ${purchase.transactionDate?.let { kotlinx.datetime.Instant.fromEpochSeconds(it.toLong()) } ?: "N/A"}
                    Receipt: ${purchase.transactionReceipt?.take(50)}...
                """.trimIndent()
                
                // IMPORTANT: Server-side receipt validation should be performed here
                // Send the receipt to your backend server for validation
                // Example:
                // val isValid = validateReceiptOnServer(purchase.transactionReceipt)
                // if (!isValid) {
                //     purchaseResult = "❌ Receipt validation failed"
                //     return@collect
                // }
                
                // After successful server validation, finish the transaction
                // For subscriptions, set isConsumable to false
                scope.launch {
                    try {
                        kmpIAP.finishTransaction(
                            purchase = purchase,
                            isConsumable = false // Set to false for subscription products
                        )
                        purchaseResult = "$purchaseResult\n\n✅ Transaction finished successfully"
                        
                        // Refresh active subscriptions after successful purchase
                        activeSubscriptions = kmpIAP.getActiveSubscriptions(SUBSCRIPTION_IDS)
                        hasActiveSubscription = kmpIAP.hasActiveSubscriptions(SUBSCRIPTION_IDS)
                    } catch (e: Exception) {
                        purchaseResult = "$purchaseResult\n\n❌ Failed to finish transaction: ${e.message}"
                    }
                }
            }
        }
        
        launch {
            kmpIAP.purchaseErrorListener.collect { error ->
                currentError = error
                purchaseResult = when (error.code) {
                    ErrorCode.E_USER_CANCELLED.name -> "⚠️ Subscription cancelled by user"
                    else -> "❌ Error: ${error.message}\nCode: ${error.code}"
                }
            }
        }
    }
    
    // Initialize connection and load subscriptions
    LaunchedEffect(Unit) {
        scope.launch {
            // Step 1: Initialize connection
            isConnecting = true
            try {
                val connectionResult = kmpIAP.initConnection()
                connected = connectionResult
                
                if (!connectionResult) {
                    initError = "Failed to connect to store"
                    return@launch
                }
                
                // Step 2: Connection successful, load subscriptions immediately
                isConnecting = false
                isLoadingProducts = true
                
                // Load active subscriptions and subscription products in parallel
                val activeSubscriptionsDeferred = async {
                    try {
                        kmpIAP.getActiveSubscriptions(SUBSCRIPTION_IDS)
                    } catch (e: Exception) {
                        println("Failed to get active subscriptions: ${e.message}")
                        emptyList()
                    }
                }
                
                val hasActiveSubDeferred = async {
                    try {
                        kmpIAP.hasActiveSubscriptions(SUBSCRIPTION_IDS)
                    } catch (e: Exception) {
                        println("Failed to check active subscriptions: ${e.message}")
                        false
                    }
                }
                
                val subscriptionProductsDeferred = async {
                    try {
                        kmpIAP.requestProducts(
                            skus = SUBSCRIPTION_IDS,
                            type = ProductType.SUBS
                        )
                    } catch (e: Exception) {
                        println("Failed to load subscription products: ${e.message}")
                        throw e
                    }
                }
                
                // Wait for all results with timeout
                val results = withTimeoutOrNull(10000) {
                    Triple(
                        activeSubscriptionsDeferred.await(),
                        hasActiveSubDeferred.await(),
                        subscriptionProductsDeferred.await()
                    )
                } ?: Triple(emptyList(), false, emptyList())
                
                // Process results
                activeSubscriptions = results.first
                hasActiveSubscription = results.second
                val subscriptionProducts = results.third
                
                // Process subscription products - they are already of type Product
                subscriptions = subscriptionProducts
                
                if (subscriptions.isEmpty()) {
                    purchaseResult = "No subscriptions found for IDs: ${SUBSCRIPTION_IDS.joinToString()}"
                }
                
            } catch (e: Exception) {
                purchaseResult = "Initialization error: ${e.message}"
                connected = false
            } finally {
                isConnecting = false
                isLoadingProducts = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscription Flow") },
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
            
            // Active Subscription Status
            if (hasActiveSubscription) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.Success.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Active Subscriptions (${activeSubscriptions.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppColors.OnSurface
                        )
                        
                        activeSubscriptions.forEach { activeSub ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Column {
                                Text(
                                    text = "• ${activeSub.productId}",
                                    fontSize = 14.sp,
                                    color = AppColors.OnSurface
                                )
                                
                                // Show iOS-specific info
                                activeSub.expirationDateIOS?.let { expDate ->
                                    Text(
                                        text = "  Expires: ${kotlinx.datetime.Instant.fromEpochMilliseconds(expDate)}",
                                        fontSize = 12.sp,
                                        color = AppColors.Secondary
                                    )
                                }
                                
                                activeSub.environmentIOS?.let { env ->
                                    Text(
                                        text = "  Environment: $env",
                                        fontSize = 12.sp,
                                        color = AppColors.Secondary
                                    )
                                }
                                
                                activeSub.daysUntilExpirationIOS?.let { days ->
                                    Text(
                                        text = "  Days until expiration: $days",
                                        fontSize = 12.sp,
                                        color = if (days <= 7) AppColors.Error else AppColors.Secondary
                                    )
                                }
                                
                                // Show Android-specific info
                                activeSub.autoRenewingAndroid?.let { autoRenew ->
                                    Text(
                                        text = "  Auto-renewing: ${if (autoRenew) "Yes" else "No"}",
                                        fontSize = 12.sp,
                                        color = AppColors.Secondary
                                    )
                                }
                                
                                if (activeSub.willExpireSoon == true) {
                                    Text(
                                        text = "  ⚠️ Expiring soon!",
                                        fontSize = 12.sp,
                                        color = AppColors.Warning,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // Subscriptions Section
            Text(
                text = "Available Subscriptions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.OnSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isLoadingProducts) {
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
            } else if (subscriptions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (connected) "No subscriptions available" else "Connect to load subscriptions",
                        modifier = Modifier.padding(16.dp),
                        color = AppColors.Secondary
                    )
                }
            } else {
                subscriptions.forEach { subscription ->
                    val isSubscribed = activeSubscriptions.any { it.productId == subscription.id }
                    val activeSubscription = activeSubscriptions.find { it.productId == subscription.id }
                    
                    SubscriptionCard(
                        subscription = subscription,
                        isSubscribed = isSubscribed,
                        activeSubscription = activeSubscription,
                        onSubscribe = {
                            if (!isSubscribed) {
                                scope.launch {
                                    isProcessing = true
                                    purchaseResult = null
                                    try {
                                        kmpIAP.requestPurchase(
                                            sku = subscription.id,
                                            ios = RequestPurchaseIosProps(
                                                sku = subscription.id,
                                                quantity = 1
                                            ),
                                            android = RequestPurchaseAndroidProps(
                                                skus = listOf(subscription.id)
                                            )
                                        )
                                        // Purchase updates will be received through the purchaseUpdatedListener
                                        // The UI will be updated automatically when the listener triggers
                                    } catch (e: Exception) {
                                        purchaseResult = "Subscription failed: ${e.message}"
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        isProcessing = isProcessing
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            // Purchase Result
            purchaseResult?.let { result ->
                Spacer(modifier = Modifier.height(20.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            result.contains("✅") -> AppColors.Success.copy(alpha = 0.1f)
                            result.contains("❌") || result.contains("Error", ignoreCase = true) -> AppColors.Error.copy(alpha = 0.1f)
                            result.contains("⚠️") || result.contains("cancelled", ignoreCase = true) -> AppColors.Warning
                            else -> Color.White
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Subscription Result",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppColors.OnSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = result,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AppColors.OnSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionCard(
    subscription: Product,
    isSubscribed: Boolean,
    activeSubscription: ActiveSubscription? = null,
    onSubscribe: () -> Unit,
    isProcessing: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Log subscription product details to console in JSON format
                println("\n========== SUBSCRIPTION PRODUCT (JSON) ==========")
                val json = Json { 
                    prettyPrint = true
                    encodeDefaults = true
                }
                
                // Serialize based on concrete type since Product is an interface
                val jsonString = when (subscription) {
                    is ProductSubscriptionAndroid -> json.encodeToString(subscription)
                    is ProductSubscriptionIOS -> json.encodeToString(subscription)
                    is ProductAndroid -> json.encodeToString(subscription)
                    is ProductIOS -> json.encodeToString(subscription)
                    else -> {
                        // Fallback to manual JSON if unknown type
                        """
                        {
                          "id": "${subscription.id}",
                          "title": "${subscription.title}",
                          "description": "${subscription.description}",
                          "displayPrice": "${subscription.displayPrice}",
                          "type": "${subscription.type}",
                          "platform": "${subscription.platform}"
                        }
                        """.trimIndent()
                    }
                }
                println(jsonString)
                println("Is Subscribed: $isSubscribed")
                println("====================================\n")
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = subscription.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = AppColors.OnSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = subscription.description,
                        fontSize = 14.sp,
                        color = AppColors.Secondary
                    )
                    
                    // Show subscription period if available (would need to be extracted from platform-specific data)
                    // For now, just show that it's a subscription
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Type: Subscription",
                        fontSize = 12.sp,
                        color = AppColors.Primary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "ID: ${subscription.id}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AppColors.Secondary
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = subscription.displayPrice,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = AppColors.Primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Show subscribed status badge if active
            if (isSubscribed) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.Success.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✓ Subscribed",
                            color = AppColors.Success,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Button(
                    onClick = onSubscribe,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Subscribe")
                    }
                }
            }
        }
    }
}

