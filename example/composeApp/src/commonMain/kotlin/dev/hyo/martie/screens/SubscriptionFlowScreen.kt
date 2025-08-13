package dev.hyo.martie.screens

import androidx.compose.foundation.background
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
    
    var isConnecting by remember { mutableStateOf(true) }
    var isLoadingProducts by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var purchaseResult by remember { mutableStateOf<String?>(null) }
    var transactionResult by remember { mutableStateOf<String?>(null) }
    var initError by remember { mutableStateOf<String?>(null) }
    
    var connected by remember { mutableStateOf(false) }
    var subscriptions by remember { mutableStateOf<List<SubscriptionProduct>>(emptyList()) }
    var activeSubscriptions by remember { mutableStateOf<List<Purchase>>(emptyList()) }
    var currentError by remember { mutableStateOf<PurchaseError?>(null) }
    var currentPurchase by remember { mutableStateOf<Purchase?>(null) }
    
    // Register purchase event listeners
    LaunchedEffect(Unit) {
        launch {
            KmpIAP.purchaseUpdatedListener.collect { purchase ->
                currentPurchase = purchase
                purchaseResult = "✅ Subscription successful!\n\n${json.encodeToString(purchase)}"
            }
        }
        
        launch {
            KmpIAP.purchaseErrorListener.collect { error ->
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
                val connectionResult = KmpIAP.initConnection()
                connected = connectionResult
                
                if (!connectionResult) {
                    initError = "Failed to connect to store"
                    return@launch
                }
                
                // Step 2: Connection successful, load subscriptions immediately
                isConnecting = false
                isLoadingProducts = true
                
                // Load active purchases and subscription products in parallel
                val activePurchasesDeferred = async {
                    try {
                        KmpIAP.getAvailablePurchases()
                    } catch (e: Exception) {
                        println("Failed to get active purchases: ${e.message}")
                        emptyList()
                    }
                }
                
                val subscriptionProductsDeferred = async {
                    try {
                        KmpIAP.requestProducts(
                            ProductRequest(
                                skus = SUBSCRIPTION_IDS,
                                type = ProductType.SUBS
                            )
                        )
                    } catch (e: Exception) {
                        println("Failed to load subscription products: ${e.message}")
                        throw e
                    }
                }
                
                // Wait for both results with timeout
                val (activePurchases, subscriptionProducts) = withTimeoutOrNull(10000) {
                    Pair(
                        activePurchasesDeferred.await(),
                        subscriptionProductsDeferred.await()
                    )
                } ?: Pair(emptyList(), emptyList())
                
                // Process active subscriptions
                activeSubscriptions = activePurchases.filter { purchase ->
                    SUBSCRIPTION_IDS.contains(purchase.productId)
                }
                
                // Process subscription products
                subscriptions = subscriptionProducts.filterIsInstance<SubscriptionProduct>()
                if (subscriptions.isEmpty() && subscriptionProducts.isNotEmpty()) {
                    // If we got products but they're not SubscriptionProduct type,
                    // convert them to subscription format
                    subscriptions = subscriptionProducts.map { product ->
                        SubscriptionProduct(
                            id = product.id,
                            title = product.title,
                            description = product.description,
                            price = product.price,
                            priceAmount = product.priceAmount,
                            currency = product.currency,
                            subscriptionPeriod = product.subscription?.subscriptionPeriod?.toReadableString() ?: "",
                            introductoryPrice = product.subscription?.introductoryPrice?.price,
                            subscriptionGroupIdentifier = product.subscription?.subscriptionGroupIdentifier,
                            platform = product.platform
                        )
                    }
                }
                
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
                    
                    SubscriptionCard(
                        subscription = subscription,
                        isSubscribed = isSubscribed,
                        onSubscribe = {
                            if (!isSubscribed) {
                                scope.launch {
                                    isProcessing = true
                                    purchaseResult = null
                                    try {
                                        val platform = getCurrentPlatform()
                                        val purchase = KmpIAP.requestPurchase(
                                            UnifiedPurchaseRequest(
                                                sku = subscription.id,
                                                quantity = 1
                                            )
                                        )
                                        // Purchase updates will be received through the Flow
                                        // Refresh active subscriptions after purchase
                                        val updatedPurchases = KmpIAP.getAvailablePurchases()
                                        activeSubscriptions = updatedPurchases.filter { p ->
                                            SUBSCRIPTION_IDS.contains(p.productId)
                                        }
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
    subscription: SubscriptionProduct,
    isSubscribed: Boolean,
    onSubscribe: () -> Unit,
    isProcessing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    
                    if (subscription.subscriptionPeriod.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Period: ${subscription.subscriptionPeriod}",
                            fontSize = 12.sp,
                            color = AppColors.Secondary
                        )
                    }
                    
                    subscription.introductoryPrice?.let { introPrice ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Introductory price: $introPrice",
                            fontSize = 12.sp,
                            color = AppColors.Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
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
                        text = subscription.price,
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

// Extension function for iOS subscription period
private fun SubscriptionIosPeriod.toReadableString(): String {
    return when (this) {
        SubscriptionIosPeriod.P1W -> "1 week"
        SubscriptionIosPeriod.P1M -> "1 month"
        SubscriptionIosPeriod.P2M -> "2 months"
        SubscriptionIosPeriod.P3M -> "3 months"
        SubscriptionIosPeriod.P6M -> "6 months"
        SubscriptionIosPeriod.P1Y -> "1 year"
    }
}