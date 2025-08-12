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
import io.github.hyochan.kmpiap.RequestSubscriptionIOS
import kotlinx.coroutines.launch
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
    var subscriptions by remember { mutableStateOf<List<Subscription>>(emptyList()) }
    var currentError by remember { mutableStateOf<PurchaseError?>(null) }
    var currentPurchase by remember { mutableStateOf<Purchase?>(null) }
    
    // Collect purchase events
    LaunchedEffect(Unit) {
        launch {
            KmpIAP.purchaseUpdatedFlow.collect { purchase ->
                currentPurchase = purchase
                purchaseResult = "✅ Subscription successful!\n\n${json.encodeToString(purchase)}"
            }
        }
        
        launch {
            KmpIAP.purchaseErrorFlow.collect { error ->
                currentError = error
                purchaseResult = when (error.code) {
                    ErrorCode.E_USER_CANCELLED -> "Subscription cancelled"
                    else -> "❌ Subscription error: ${error.message}\nCode: ${error.code}"
                }
            }
        }
        
        launch {
            KmpIAP.connectionStateFlow.collect { connectionResult ->
                connected = connectionResult.connected
                if (!connectionResult.connected) {
                    initError = connectionResult.message
                }
            }
        }
    }
    
    // Initialize connection
    LaunchedEffect(Unit) {
        isConnecting = true
        try {
            KmpIAP.initConnection()
        } catch (e: Exception) {
            purchaseResult = "Initialization error: ${e.message}"
        } finally {
            isConnecting = false
        }
    }
    
    // Load subscriptions when connected
    LaunchedEffect(connected) {
        if (connected) {
            kotlinx.coroutines.delay(500)
            isLoadingProducts = true
            try {
                val result = KmpIAP.requestProducts(
                    RequestProductsParams(
                        type = PurchaseType.SUBS,
                        skus = SUBSCRIPTION_IDS
                    )
                )
                subscriptions = result.filterIsInstance<Subscription>()
                if (subscriptions.isEmpty()) {
                    purchaseResult = "No subscriptions found for IDs: ${SUBSCRIPTION_IDS.joinToString()}"
                }
            } catch (e: Exception) {
                purchaseResult = "Failed to load subscriptions: ${e.message}"
            } finally {
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
                    containerColor = if (connected) AppColors.Primary else AppColors.Secondary
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Text(
                            text = if (connected) "✓ Connected to ${KmpIAP.getStore()}" else "⚠ Not connected",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
            
            if (initError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Error: $initError",
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFC62828)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Available subscriptions
            if (connected && !isLoadingProducts) {
                Text(
                    "Available Subscriptions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AppColors.OnSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                if (subscriptions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            "No subscriptions available",
                            color = Color.Gray,
                            modifier = Modifier.padding(20.dp),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    subscriptions.forEach { subscription ->
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
                                    subscription.title ?: subscription.productId,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = AppColors.OnSurface
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    subscription.description ?: "",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                // Show subscription offers for Android
                                subscription.subscriptionOfferAndroid?.forEach { offer ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF5F5F5)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                "Plan: ${offer.basePlanId}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = AppColors.OnSurface
                                            )
                                            offer.pricingPhases?.forEach { phase ->
                                                Text(
                                                    "${phase.price} / ${phase.billingPeriod}",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Text(
                                    "Price: ${subscription.localizedPrice}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.Primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                
                                Button(
                                    onClick = {
                                        if (!isProcessing) {
                                            scope.launch {
                                                isProcessing = true
                                                purchaseResult = "Processing subscription..."
                                                try {
                                                    val platform = getCurrentPlatform()
                                                    val request = when (platform) {
                                                        IAPPlatform.ANDROID -> {
                                                            val offers = subscription.subscriptionOfferAndroid?.mapNotNull { offer ->
                                                                offer.offerToken?.let { token ->
                                                                    SubscriptionOfferAndroid(
                                                                        sku = subscription.productId,
                                                                        offerToken = token
                                                                    )
                                                                }
                                                            }
                                                            RequestSubscriptionAndroid(
                                                                sku = subscription.productId,
                                                                skus = listOf(subscription.productId),
                                                                subscriptionOffers = offers,
                                                                obfuscatedAccountIdAndroid = null,
                                                                obfuscatedProfileIdAndroid = null
                                                            )
                                                        }
                                                        IAPPlatform.IOS -> {
                                                            RequestSubscriptionIOS(
                                                                sku = subscription.productId,
                                                                quantity = 1
                                                            )
                                                        }
                                                        else -> throw IllegalStateException("Unsupported platform")
                                                    }
                                                    
                                                    KmpIAP.requestPurchase(request, PurchaseType.SUBS)
                                                } catch (e: Exception) {
                                                    purchaseResult = "Subscription failed: ${e.message}"
                                                } finally {
                                                    isProcessing = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isProcessing,
                                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Subscribe for ${subscription.localizedPrice}", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Manage subscriptions button
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                KmpIAP.deepLinkToSubscriptionsAndroid(null)
                            } catch (e: Exception) {
                                transactionResult = "Error opening subscriptions: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Secondary)
                ) {
                    Text("Manage Subscriptions", color = Color.White)
                }
            }
            
            // Results display
            if (purchaseResult != null) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            purchaseResult?.contains("✅") == true -> Color(0xFFE8F5E9)
                            purchaseResult?.contains("❌") == true -> Color(0xFFFFEBEE)
                            else -> Color(0xFFF5F5F5)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Subscription Result",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppColors.OnSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            purchaseResult ?: "",
                            fontSize = 12.sp,
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
            
            if (transactionResult != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Transaction Result",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppColors.OnSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            transactionResult ?: "",
                            fontSize = 12.sp,
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
    }
}
