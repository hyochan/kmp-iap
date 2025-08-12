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
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val PRODUCT_IDS = listOf("dev.hyo.martie.10bulbs", "dev.hyo.martie.30bulbs")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseFlowScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val json = remember { Json { prettyPrint = true; ignoreUnknownKeys = true } }
    
    var isConnecting by remember { mutableStateOf(true) }
    var isLoadingProducts by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var purchaseResult by remember { mutableStateOf<String?>(null) }
    var transactionResult by remember { mutableStateOf<String?>(null) }
    var initError by remember { mutableStateOf<String?>(null) }
    var purchaseToConsume by remember { mutableStateOf<Purchase?>(null) }
    
    val iap = KmpIAP
    var connected by remember { mutableStateOf(false) }
    var products by remember { mutableStateOf<List<BaseProduct>>(emptyList()) }
    var currentError by remember { mutableStateOf<PurchaseError?>(null) }
    var currentPurchase by remember { mutableStateOf<Purchase?>(null) }
    
    // Collect purchase events
    LaunchedEffect(Unit) {
        launch {
            iap.purchaseUpdatedFlow.collect { purchase ->
                currentPurchase = purchase
                purchaseResult = "✅ Purchase successful!\n\n${json.encodeToString(purchase)}"
                
                // TEST ONLY: Auto-consume for testing
                purchaseToConsume = purchase
            }
        }
        
        launch {
            iap.purchaseErrorFlow.collect { error ->
                currentError = error
                purchaseResult = when (error.code) {
                    ErrorCode.E_USER_CANCELLED -> "Purchase cancelled"
                    else -> "Error: ${error.message}\nCode: ${error.code}"
                }
            }
        }
        
        launch {
            iap.connectionStateFlow.collect { connectionResult ->
                connected = connectionResult.connected
                if (!connectionResult.connected) {
                    initError = connectionResult.message
                }
            }
        }
    }
    
    // TEST ONLY: Auto-consume purchases for testing
    LaunchedEffect(purchaseToConsume) {
        purchaseToConsume?.let { purchase ->
            try {
                val consumeResult = iap.finishTransaction(
                    purchase = purchase,
                    isConsumable = true
                )
                if (consumeResult) {
                    purchaseResult = "${purchaseResult}\n\n✅ Purchase consumed for testing"
                } else {
                    purchaseResult = "${purchaseResult}\n\n⚠️ Failed to consume purchase"
                }
            } catch (e: Exception) {
                purchaseResult = "${purchaseResult}\n\n❌ Error consuming: ${e.message}"
            }
            purchaseToConsume = null
        }
    }
    
    // Initialize connection
    LaunchedEffect(Unit) {
        isConnecting = true
        try {
            iap.initConnection()
        } catch (e: Exception) {
            purchaseResult = "Initialization error: ${e.message}"
        } finally {
            isConnecting = false
        }
    }
    
    // Load products when connected
    LaunchedEffect(connected) {
        if (connected) {
            kotlinx.coroutines.delay(500)
            isLoadingProducts = true
            try {
                // TEST ONLY: Clear existing purchases first
                try {
                    val existingPurchases = iap.getAvailablePurchases()
                    existingPurchases.forEach { purchase ->
                        if (PRODUCT_IDS.contains(purchase.productId)) {
                            try {
                                val consumed = iap.finishTransaction(purchase, isConsumable = true)
                                println("[TEST] Consumed existing purchase: ${purchase.productId} - Success: $consumed")
                            } catch (e: Exception) {
                                println("[TEST] Failed to consume existing purchase: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("[TEST] Could not check existing purchases: ${e.message}")
                }
                
                val result = iap.requestProducts(
                    RequestProductsParams(
                        type = PurchaseType.INAPP,
                        skus = PRODUCT_IDS
                    )
                )
                products = result
                if (products.isEmpty()) {
                    purchaseResult = "No products found for IDs: ${PRODUCT_IDS.joinToString()}"
                }
            } catch (e: Exception) {
                purchaseResult = "Failed to load products: ${e.message}"
            } finally {
                isLoadingProducts = false
            }
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
                            text = if (connected) "✓ Connected to ${iap.getStore()}" else "⚠ Not connected",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Products Section
            if (connected && !isLoadingProducts) {
                Text(
                    "Available Products",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AppColors.OnSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                if (products.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            "No products available",
                            color = Color.Gray,
                            modifier = Modifier.padding(20.dp),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    products.forEach { product ->
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
                                    product.title ?: product.productId,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = AppColors.OnSurface
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    product.description ?: "",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Text(
                                    "Price: ${product.localizedPrice}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.Primary,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                
                                Button(
                                    onClick = {
                                        if (!isProcessing) {
                                            scope.launch {
                                                isProcessing = true
                                                purchaseResult = "Processing purchase..."
                                                try {
                                                    val platform = getCurrentPlatform()
                                                    val request = when (platform) {
                                                        IAPPlatform.ANDROID -> {
                                                            RequestPurchaseAndroid(
                                                                sku = product.productId,
                                                                skus = listOf(product.productId),
                                                                obfuscatedAccountIdAndroid = null,
                                                                obfuscatedProfileIdAndroid = null
                                                            )
                                                        }
                                                        IAPPlatform.IOS -> {
                                                            RequestPurchaseIOS(
                                                                sku = product.productId,
                                                                quantity = 1
                                                            )
                                                        }
                                                        else -> throw IllegalStateException("Unsupported platform")
                                                    }
                                                    
                                                    iap.requestPurchase(request, PurchaseType.INAPP)
                                                } catch (e: Exception) {
                                                    purchaseResult = "Purchase failed: ${e.message}"
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
                                        Text("Purchase", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Get Available Purchases button
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = {
                        scope.launch {
                            transactionResult = "Loading purchases..."
                            try {
                                val purchases = iap.getAvailablePurchases()
                                transactionResult = if (purchases.isEmpty()) {
                                    "No active purchases found"
                                } else {
                                    "Active Purchases:\n" + purchases.joinToString("\n") { purchase ->
                                        "• ${purchase.productId} (${purchase.transactionDate})"
                                    }
                                }
                            } catch (e: Exception) {
                                transactionResult = "Error loading purchases: ${e.message}"
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Secondary)
                ) {
                    Text("Get Available Purchases", color = Color.White)
                }
            }
            
            // Results Section
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
                            "Purchase Result",
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
