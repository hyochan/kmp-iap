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

private val PRODUCT_IDS = listOf("dev.hyo.martie.10bulbs", "dev.hyo.martie.30bulbs")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseFlowScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val json = remember { Json { prettyPrint = true; ignoreUnknownKeys = true } }
    
    var isConnecting by remember { mutableStateOf(true) }  // Start with true
    var isLoadingProducts by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var purchaseResult by remember { mutableStateOf<String?>(null) }
    var transactionResult by remember { mutableStateOf<String?>(null) }
    var initError by remember { mutableStateOf<String?>(null) }
    var purchaseToConsume by remember { mutableStateOf<Purchase?>(null) }
    
    val iapHelper = remember {
        try {
            useIap(
                scope = scope,
                options = UseIapOptions(
                    onPurchaseSuccess = { purchase ->
                        purchaseResult = json.encodeToString(purchase)
                        
                        // TEST ONLY: Store the purchase to be consumed
                        // This is for testing purposes only - in production, consume only after
                        // delivering the product to the user
                        purchaseToConsume = purchase
                    },
                    onPurchaseError = { error ->
                        purchaseResult = when (error.code) {
                            io.github.hyochan.kmpiap.utils.ErrorCode.E_USER_CANCELLED -> "Purchase cancelled"
                            else -> "Error: ${error.message}\nCode: ${error.code}"
                        }
                    }
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    val connected by iapHelper?.connected?.collectAsState() ?: remember { mutableStateOf(false) }
    val products by iapHelper?.products?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val currentError by iapHelper?.currentError?.collectAsState() ?: remember { mutableStateOf(null) }
    
    // TEST ONLY: Automatically consume purchases after they're successful
    // This allows repeated testing of the same products
    LaunchedEffect(purchaseToConsume) {
        purchaseToConsume?.let { purchase ->
            iapHelper?.let { helper ->
                try {
                    val consumeResult = helper.finishTransaction(
                        purchase = purchase,
                        isConsumable = true // Mark as consumable for testing
                    )
                    if (consumeResult) {
                        purchaseResult = "${purchaseResult}\n\n✅ Purchase consumed for testing"
                    } else {
                        purchaseResult = "${purchaseResult}\n\n⚠️ Failed to consume purchase"
                    }
                } catch (e: Exception) {
                    purchaseResult = "${purchaseResult}\n\n❌ Error consuming: ${e.message}"
                }
                purchaseToConsume = null // Reset after consuming
            }
        }
    }
    
    LaunchedEffect(Unit) {
        if (iapHelper != null) {
            isConnecting = true
            try {
                iapHelper.initConnection()
            } catch (e: Exception) {
                purchaseResult = "Initialization error: ${e.message}"
            } finally {
                isConnecting = false
            }
        } else {
            purchaseResult = "Failed to initialize IAP helper"
        }
    }
    
    // Load products when connected
    LaunchedEffect(connected) {
        if (connected && iapHelper != null) {
            // Add small delay to ensure connection is fully established
            kotlinx.coroutines.delay(500)
            isLoadingProducts = true
            try {
                // TEST ONLY: First, get and consume any existing purchases to reset state
                // This ensures the test products can be purchased again
                try {
                    iapHelper.getAvailablePurchases()
                    kotlinx.coroutines.delay(200) // Give time for state to update
                    val existingPurchases = iapHelper.availablePurchases.value
                    existingPurchases.forEach { purchase ->
                        if (PRODUCT_IDS.contains(purchase.productId)) {
                            try {
                                val consumed = iapHelper.finishTransaction(purchase, isConsumable = true)
                                println("[TEST] Consumed existing purchase: ${purchase.productId} - Success: $consumed")
                            } catch (e: Exception) {
                                println("[TEST] Failed to consume existing purchase: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("[TEST] Could not check existing purchases: ${e.message}")
                }
                
                iapHelper.requestProducts(RequestProductsParams(PRODUCT_IDS, PurchaseType.INAPP))
            } catch (e: Exception) {
                purchaseResult = "Failed to load products: ${e.message}"
            } finally {
                isLoadingProducts = false
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            iapHelper?.dispose()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("In-App Purchase Flow") },
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
            // Init Error
            initError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
            
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
            
            // Products Section
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
            } else if (connected && isLoadingProducts) {
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
                            text = "Loading products...",
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
                        text = "Available Products",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.OnSurface
                    )
                    if (isLoadingProducts) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AppColors.Primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (products.isEmpty() && !isLoadingProducts) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
                ) {
                    Text(
                        text = "No products available",
                        modifier = Modifier.padding(16.dp),
                        color = AppColors.Secondary
                    )
                }
            }
            
            products.forEach { product ->
                ProductCard(
                    product = product,
                    onPurchase = {
                        scope.launch {
                            isProcessing = true
                            try {
                                iapHelper?.requestPurchase(
                                    sku = product.productId,
                                    quantity = 1
                                ) ?: run {
                                    purchaseResult = "IAP not initialized"
                                }
                            } catch (e: Exception) {
                                purchaseResult = "Purchase error: ${e.message}\n${e.stackTraceToString()}"
                            } finally {
                                isProcessing = false
                            }
                        }
                    },
                    isLoading = isProcessing
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
            
            // iOS Transaction Test
            if (getCurrentPlatform() == IAPPlatform.IOS) {
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            // TODO: Implement getAppTransactionIOS when available
                            transactionResult = "getAppTransactionIOS not yet implemented"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Purple)
                ) {
                    Text("Test getAppTransactionIOS()")
                }
                
                transactionResult?.let { result ->
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Text(
                            text = result,
                            modifier = Modifier.padding(16.dp),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
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
                        text = "ℹ️ Testing Tips",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = AppColors.OnSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Use sandbox account on iOS\n" +
                              "• Use test card on Android\n" +
                              "• Products may take time to propagate\n" +
                              "• 🔄 Purchases are auto-consumed for testing",
                        fontSize = 14.sp,
                        color = AppColors.Secondary
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    onPurchase: () -> Unit,
    isLoading: Boolean
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
                        text = product.title ?: product.productId,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = AppColors.OnSurface
                    )
                    product.description?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            fontSize = 14.sp,
                            color = AppColors.Secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = product.localizedPrice ?: product.price,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                }
                
                Button(
                    onClick = onPurchase,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Buy")
                    }
                }
            }
        }
    }
}