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
import io.github.hyochan.kmpiap.types.*
import io.github.hyochan.kmpiap.useIap.UseIapOptions
import io.github.hyochan.kmpiap.useIap.useIap
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val SUBSCRIPTION_IDS = listOf("dev.hyo.martie.premium")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionFlowScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val json = remember { Json { prettyPrint = true; ignoreUnknownKeys = true } }
    
    var isConnecting by remember { mutableStateOf(true) }  // Start with true
    var isLoadingSubscriptions by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var purchaseResult by remember { mutableStateOf<String?>(null) }
    
    var refreshPurchases by remember { mutableStateOf({}) }
    
    val iapHelper = remember {
        useIap(
            scope = scope,
            options = UseIapOptions(
                onPurchaseSuccess = { purchase ->
                    purchaseResult = json.encodeToString(purchase)
                },
                onPurchaseError = { error ->
                    purchaseResult = when (error.code) {
                        io.github.hyochan.kmpiap.utils.ErrorCode.E_USER_CANCELLED -> "Subscription cancelled"
                        else -> "Error: ${error.message}\nCode: ${error.code}"
                    }
                }
            )
        )
    }
    
    refreshPurchases = { scope.launch { iapHelper.refreshPurchases() } }
    
    val connected by iapHelper.connected.collectAsState()
    val subscriptions by iapHelper.subscriptions.collectAsState()
    val activeSubscriptions by iapHelper.activeSubscriptions.collectAsState()
    
    LaunchedEffect(Unit) {
        isConnecting = true
        try {
            iapHelper.initConnection()
        } catch (e: Exception) {
            purchaseResult = "Connection error: ${e.message}"
        } finally {
            isConnecting = false
        }
    }
    
    // Update connection state and load subscriptions when connected
    LaunchedEffect(connected) {
        if (connected) {
            // Add small delay to ensure connection is fully established
            kotlinx.coroutines.delay(500)
            isLoadingSubscriptions = true
            try {
                println("[SubscriptionFlowScreen] Loading subscriptions: $SUBSCRIPTION_IDS")
                val loadedSubscriptions = iapHelper.getSubscriptions(SUBSCRIPTION_IDS)
                println("[SubscriptionFlowScreen] Subscriptions loaded: ${loadedSubscriptions.size}")
                // Check cached purchases first
                val cachedPurchases = iapHelper.getCachedAvailablePurchases()
                if (cachedPurchases.isEmpty()) {
                    // Only refresh if no cached purchases
                    iapHelper.refreshPurchases()
                }
            } catch (e: Exception) {
                purchaseResult = "Failed to load subscriptions: ${e.message}"
                println("[SubscriptionFlowScreen] Error loading subscriptions: ${e.message}")
            } finally {
                isLoadingSubscriptions = false
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            iapHelper.dispose()
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
                    containerColor = if (connected) AppColors.Success else AppColors.Secondary
                ),
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
                        text = if (connected) "Connected" else if (isConnecting) "Connecting..." else "Disconnected",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Active Subscription Status (if any)
            if (activeSubscriptions.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Success),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Premium Active",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your premium subscription is active",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // Subscriptions Section
            if (!connected && isConnecting) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            color = AppColors.Primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connecting to store...",
                            fontSize = 16.sp,
                            color = AppColors.Secondary
                        )
                    }
                }
            } else if (connected && isLoadingSubscriptions) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        CircularProgressIndicator(
                            color = AppColors.Primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading subscriptions...",
                            fontSize = 16.sp,
                            color = AppColors.Secondary
                        )
                    }
                }
            } else if (connected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Subscriptions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                    if (isLoadingSubscriptions) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AppColors.Primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            if (subscriptions.isEmpty() && !isLoadingSubscriptions) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
                ) {
                    Text(
                        text = "No subscriptions available",
                        modifier = Modifier.padding(16.dp),
                        color = AppColors.Secondary
                    )
                }
            }
            
            subscriptions.forEach { subscription ->
                SubscriptionCard(
                    subscription = subscription,
                    onSubscribe = {
                        scope.launch {
                            isProcessing = true
                            try {
                                iapHelper.requestSubscription(
                                    sku = subscription.productId,
                                    subscriptionOffers = subscription.subscriptionOfferAndroid?.mapNotNull { offer ->
                                        offer.offerToken?.let { token ->
                                            SubscriptionOfferAndroid(
                                                sku = subscription.productId,
                                                offerToken = token
                                            )
                                        }
                                    }
                                )
                            } catch (e: Exception) {
                                purchaseResult = "Subscription error: ${e.message}"
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    isLoading = isProcessing,
                    activeSubscriptions = activeSubscriptions
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Results Section
            purchaseResult?.let { result ->
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Purchase Result",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.OnSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(16.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = AppColors.OnSurface
                    )
                }
            }
            
            // Info Section
            Spacer(modifier = Modifier.height(20.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ℹ️ Subscription Testing",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = AppColors.OnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• iOS: Subscriptions renew every 5 minutes in sandbox\n" +
                              "• Android: Use test accounts configured in Play Console\n" +
                              "• Check subscription status in store settings",
                        fontSize = 14.sp,
                        color = AppColors.Secondary
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionCard(
    subscription: Subscription,
    onSubscribe: () -> Unit,
    isLoading: Boolean,
    activeSubscriptions: List<Purchase> = emptyList()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subscription.title ?: subscription.productId,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = AppColors.OnSurface
                    )
                    subscription.description?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            color = AppColors.Secondary
                        )
                    }
                    
                    // Subscription period
                    val period = when {
                        subscription.subscriptionPeriodUnitIOS != null -> {
                            "${subscription.subscriptionPeriodNumberIOS ?: 1} ${subscription.subscriptionPeriodUnitIOS}"
                        }
                        subscription.subscriptionPeriodAndroid != null -> {
                            subscription.subscriptionPeriodAndroid
                        }
                        else -> null
                    }
                    
                    period?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Billed $it",
                            fontSize = 14.sp,
                            color = AppColors.Secondary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = subscription.localizedPrice ?: subscription.price,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    
                    // Introductory price
                    subscription.introductoryPrice?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Intro: $it",
                            fontSize = 14.sp,
                            color = AppColors.Success
                        )
                    }
                }
                
                val isSubscribed = activeSubscriptions.any { it.productId == subscription.productId }
                
                Button(
                    onClick = if (isSubscribed) {
                        { /* Already subscribed, do nothing */ }
                    } else {
                        onSubscribe
                    },
                    enabled = !isSubscribed && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubscribed) AppColors.Success else AppColors.Primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (isSubscribed) "Subscribed" else "Subscribe")
                    }
                }
            }
        }
    }
}