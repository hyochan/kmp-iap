package dev.hyo.martie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.fetchProducts
import io.github.hyochan.kmpiap.requestPurchase
import io.github.hyochan.kmpiap.toPurchaseInput
import io.github.hyochan.kmpiap.openiap.Product
import io.github.hyochan.kmpiap.openiap.Purchase
import io.github.hyochan.kmpiap.openiap.PurchaseError
import io.github.hyochan.kmpiap.openiap.PurchaseState
import io.github.hyochan.kmpiap.openiap.ProductQueryType
import io.github.hyochan.kmpiap.openiap.ProductType
import io.github.hyochan.kmpiap.openiap.ErrorCode
import io.github.hyochan.kmpiap.openiap.PurchaseAndroid
import io.github.hyochan.kmpiap.openiap.PurchaseIOS
import io.github.hyochan.kmpiap.openiap.ProductAndroid
import io.github.hyochan.kmpiap.openiap.ProductIOS
import io.github.hyochan.kmpiap.openiap.ActiveSubscription
import kotlinx.coroutines.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val SUBSCRIPTION_IDS = listOf(
    "dev.hyo.martie.premium",
    "dev.hyo.martie.premium_year"
)

/**
 * Helper function to format epoch milliseconds to LocalDateTime string
 */
private fun Long.toFormattedDate(): String {
    return Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionFlowScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

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

                when (purchase.purchaseState) {
                    PurchaseState.Purchased, PurchaseState.Restored -> {
                        isProcessing = false

                        val dateText = purchase.transactionDate?.let {
                            Instant.fromEpochSeconds(it.toLong()).toLocalDateTime(TimeZone.currentSystemDefault())
                        } ?: "N/A"
                        purchaseResult = """
                    ✅ Subscription successful (${purchase.platform})
                    Product: ${purchase.productId}
                    Transaction ID: ${purchase.id.ifEmpty { "N/A" }}
                    Date: $dateText
                    Receipt: ${purchase.purchaseToken?.take(50) ?: "N/A"}
                """.trimIndent()

                        scope.launch {
                            try {
                                kmpIAP.finishTransaction(
                                    purchase = purchase.toPurchaseInput(),
                                    isConsumable = false
                                )
                                purchaseResult = "$purchaseResult\n\n✅ Transaction finished successfully"

                                activeSubscriptions = kmpIAP.getActiveSubscriptions(SUBSCRIPTION_IDS)
                                hasActiveSubscription = kmpIAP.hasActiveSubscriptions(SUBSCRIPTION_IDS)
                            } catch (e: Exception) {
                                purchaseResult = "$purchaseResult\n\n❌ Failed to finish transaction: ${e.message}"
                            }
                        }
                    }
                    PurchaseState.Pending, PurchaseState.Deferred -> {
                        isProcessing = true
                        purchaseResult = "⏳ Subscription is pending user confirmation..."
                    }
                    PurchaseState.Failed -> {
                        isProcessing = false
                        purchaseResult = "❌ Subscription failed"
                    }
                    else -> {
                        isProcessing = false
                        purchaseResult = null
                    }
                }
            }
        }
        
        launch {
            kmpIAP.purchaseErrorListener.collect { error ->
                isProcessing = false
                currentError = error
                purchaseResult = when (error.code) {
                    ErrorCode.UserCancelled -> "⚠️ Subscription cancelled by user"
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
                        kmpIAP.fetchProducts {
                            skus = SUBSCRIPTION_IDS
                            type = ProductQueryType.Subs
                        }
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
                                    val expiration = expDate.toLong().toFormattedDate()
                                    Text(
                                        text = "  Expires: $expiration",
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

                                // Show renewalInfoIOS details
                                activeSub.renewalInfoIOS?.let { renewalInfo ->
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = AppColors.Background
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Text(
                                                text = "Renewal Info (iOS)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AppColors.InfoPurple
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Auto-Renew Status
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (renewalInfo.willAutoRenew) "✅ Auto-Renew" else "⚠️ Won't Auto-Renew",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (renewalInfo.willAutoRenew) AppColors.Success else AppColors.Orange
                                                )
                                            }

                                            // Pending Upgrade Detection
                                            renewalInfo.pendingUpgradeProductId?.let { upgradeId ->
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "🔵 Upgrade Pending → $upgradeId",
                                                    fontSize = 11.sp,
                                                    color = AppColors.InfoBlue,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            // Next Renewal Date
                                            renewalInfo.renewalDate?.let { renewalDate ->
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val date = renewalDate.toLong().toFormattedDate()
                                                Text(
                                                    text = "Next Renewal: $date",
                                                    fontSize = 10.sp,
                                                    color = AppColors.Secondary
                                                )
                                            }

                                            // Expiration Reason (if cancelled)
                                            renewalInfo.expirationReason?.let { reason ->
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Expiration Reason: $reason",
                                                    fontSize = 10.sp,
                                                    color = AppColors.Error
                                                )
                                            }

                                            // Billing Retry Status
                                            if (renewalInfo.isInBillingRetry == true) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Column {
                                                    Text(
                                                        text = "🟣 Billing Retry in Progress",
                                                        fontSize = 11.sp,
                                                        color = AppColors.BillingRetryPurple,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    renewalInfo.gracePeriodExpirationDate?.let { graceDate ->
                                                        val grace = graceDate.toLong().toFormattedDate()
                                                        Text(
                                                            text = "Grace Period Ends: $grace",
                                                            fontSize = 10.sp,
                                                            color = AppColors.Secondary
                                                        )
                                                    }
                                                }
                                            }

                                            // Price Increase Status
                                            renewalInfo.priceIncreaseStatus?.let { status ->
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Price Increase: $status",
                                                    fontSize = 10.sp,
                                                    color = AppColors.Secondary
                                                )
                                            }

                                            // Auto-Renew Preference (if different from current product)
                                            renewalInfo.autoRenewPreference?.let { preference ->
                                                if (preference != activeSub.productId) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Will renew as: $preference",
                                                        fontSize = 10.sp,
                                                        color = AppColors.Secondary
                                                    )
                                                }
                                            }
                                        }
                                    }
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

            // Upgrade Detection Section
            activeSubscriptions.firstOrNull { it.renewalInfoIOS?.pendingUpgradeProductId != null }?.let { upgrading ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.UpgradeBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🔵 Subscription Upgrade Detected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppColors.InfoBlue
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Current Plan: ${upgrading.productId}",
                            fontSize = 14.sp,
                            color = AppColors.OnSurface
                        )

                        upgrading.renewalInfoIOS?.pendingUpgradeProductId?.let { upgradeId ->
                            Text(
                                text = "Upgrading To: $upgradeId",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.InfoBlue
                            )
                        }

                        upgrading.renewalInfoIOS?.renewalDate?.let { renewalDate ->
                            val date = renewalDate.toLong().toFormattedDate()
                            Text(
                                text = "Effective Date: $date",
                                fontSize = 12.sp,
                                color = AppColors.Secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Cancellation Detection Section
            activeSubscriptions.firstOrNull {
                val info = it.renewalInfoIOS
                info?.willAutoRenew == false && info.pendingUpgradeProductId == null
            }?.let { cancelled ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AppColors.CancellationBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🟠 Subscription Cancelled",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppColors.Orange
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Product: ${cancelled.productId}",
                            fontSize = 14.sp,
                            color = AppColors.OnSurface
                        )

                        Text(
                            text = "Status: Active but won't renew",
                            fontSize = 14.sp,
                            color = AppColors.Secondary
                        )

                        cancelled.expirationDateIOS?.let { expDate ->
                            val expiration = expDate.toLong().toFormattedDate()
                            Text(
                                text = "Expires: $expiration",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppColors.Orange
                            )
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
                                        val purchase = kmpIAP.requestPurchase {
                                            type = ProductType.Subs
                                            ios {
                                                sku = subscription.id
                                                quantity = 1
                                            }
                                            android {
                                                skus = listOf(subscription.id)
                                            }
                                        }
                                        // Purchase updates will be received through the purchaseUpdatedListener
                                        // The UI will be updated automatically when the listener triggers
                                    } catch (e: Exception) {
                                        purchaseResult = "Subscription failed: ${e.message}"
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
                // Log subscription product details to console
                println("\n========== SUBSCRIPTION PRODUCT ==========")
                println("ID: ${subscription.id}")
                println("Title: ${subscription.title}")
                println("Description: ${subscription.description}")
                println("Display Price: ${subscription.displayPrice}")
                println("Currency: ${subscription.currency}")
                println("Price: ${subscription.price}")
                println("Type: ${subscription.type}")
                println("Platform: ${subscription.platform}")

                // Platform-specific details
                when (subscription) {
                    is ProductAndroid -> {
                        println("--- Android Specific ---")
                        println("One Time Purchase Offer: ${subscription.oneTimePurchaseOfferDetailsAndroid}")
                        println("Subscription Offers: ${subscription.subscriptionOfferDetailsAndroid?.size ?: 0} offers")
                    }
                    is ProductIOS -> {
                        println("--- iOS Specific ---")
                        println("Subscription Info: ${subscription.subscriptionInfoIOS}")
                    }
                }

                println("Is Subscribed: $isSubscribed")

                // Log renewal info if available
                activeSubscription?.renewalInfoIOS?.let { renewalInfo ->
                    println("\n--- Renewal Info (iOS) ---")
                    println("willAutoRenew: ${renewalInfo.willAutoRenew}")
                    renewalInfo.pendingUpgradeProductId?.let { println("pendingUpgradeProductId: $it") }
                    renewalInfo.autoRenewPreference?.let { println("autoRenewPreference: $it") }
                    renewalInfo.renewalDate?.let {
                        val date = it.toLong().toFormattedDate()
                        println("renewalDate: $date")
                    }
                    renewalInfo.expirationReason?.let { println("expirationReason: $it") }
                    renewalInfo.gracePeriodExpirationDate?.let {
                        val date = it.toLong().toFormattedDate()
                        println("gracePeriodExpirationDate: $date")
                    }
                    renewalInfo.isInBillingRetry?.let { println("isInBillingRetry: $it") }
                    renewalInfo.priceIncreaseStatus?.let { println("priceIncreaseStatus: $it") }
                    renewalInfo.renewalOfferId?.let { println("renewalOfferId: $it") }
                    renewalInfo.renewalOfferType?.let { println("renewalOfferType: $it") }
                    println("------------------------")
                }
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
