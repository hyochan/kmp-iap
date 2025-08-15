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
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val PRODUCT_IDS = listOf("dev.hyo.martie.10bulbs", "dev.hyo.martie.30bulbs")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseFlowScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val json = remember { Json { prettyPrint = true; ignoreUnknownKeys = true } }
    
    // Use global IAP instance for this example
    // This demonstrates using the pre-created singleton instance
    
    var isConnecting by remember { mutableStateOf(true) }
    var isLoadingProducts by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var purchaseResult by remember { mutableStateOf<String?>(null) }
    var transactionResult by remember { mutableStateOf<String?>(null) }
    var initError by remember { mutableStateOf<String?>(null) }
    
    var connected by remember { mutableStateOf(false) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var currentError by remember { mutableStateOf<PurchaseError?>(null) }
    var currentPurchase by remember { mutableStateOf<Purchase?>(null) }
    
    // Register purchase event listeners
    LaunchedEffect(Unit) {
        launch {
            kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                currentPurchase = purchase
                
                // Handle successful purchase
                purchaseResult = """
                    ✅ Purchase successful (${purchase.platform})
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
                // For consumable products (like bulb packs), set isConsumable to true
                scope.launch {
                    try {
                        kmpIapInstance.finishTransaction(
                            purchase = purchase,
                            isConsumable = true // Set to true for consumable products
                        )
                        purchaseResult = "$purchaseResult\n\n✅ Transaction finished successfully"
                    } catch (e: Exception) {
                        purchaseResult = "$purchaseResult\n\n❌ Failed to finish transaction: ${e.message}"
                    }
                }
            }
        }
        
        launch {
            kmpIapInstance.purchaseErrorListener.collect { error ->
                currentError = error
                purchaseResult = when (error.code) {
                    ErrorCode.E_USER_CANCELLED.name -> "⚠️ Purchase cancelled by user"
                    else -> "❌ Error: ${error.message}\nCode: ${error.code}"
                }
            }
        }
    }
    // Initialize connection and load products
    LaunchedEffect(Unit) {
        scope.launch {
            // Step 1: Initialize connection
            isConnecting = true
            try {
                val connectionResult = kmpIapInstance.initConnection()
                connected = connectionResult
                
                if (!connectionResult) {
                    initError = "Failed to connect to store"
                    return@launch
                }
                
                // Step 2: Connection successful, load products immediately
                isConnecting = false
                isLoadingProducts = true
                purchaseResult = "Loading products from store..."
                
                // Load products with timeout
                val loadJob = async {
                    try {
                        println("[KMP-IAP Example] Requesting products: ${PRODUCT_IDS.joinToString()}")
                        val result = kmpIapInstance.requestProducts(
                            ProductRequest(
                                skus = PRODUCT_IDS,
                                type = ProductType.INAPP
                            )
                        )
                        println("[KMP-IAP Example] Products loaded: ${result.size} products")
                        result
                    } catch (e: Exception) {
                        println("[KMP-IAP Example] Error loading products: ${e.message}")
                        e.printStackTrace()
                        throw e
                    }
                }
                
                // Wait for products or timeout after 10 seconds
                val productsResult = withTimeoutOrNull(10000) {
                    loadJob.await()
                }
                
                if (productsResult != null) {
                    products = productsResult
                    if (products.isEmpty()) {
                        purchaseResult = """
                            ⚠️ No products found in store!
                            
                            Requested IDs: ${PRODUCT_IDS.joinToString()}
                            
                            Make sure these product IDs exist in Google Play Console
                            and are published/active.
                            
                            For testing, "android.test.purchased" should always work.
                        """.trimIndent()
                    } else {
                        purchaseResult = null // Clear message when products load
                        println("[KMP-IAP Example] Product details: ${products.map { "${it.id}: ${it.price}" }}")
                    }
                } else {
                    purchaseResult = """
                        ⏱️ Product loading timed out
                        
                        The store took too long to respond.
                        Please check your internet connection and try again.
                    """.trimIndent()
                    println("[KMP-IAP Example] Product loading timed out after 10 seconds")
                }
                
            } catch (e: Exception) {
                purchaseResult = """
                    ❌ Failed to initialize: ${e.message}
                    
                    Check logcat for more details.
                """.trimIndent()
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
            
            // Products Section
            Text(
                text = "Available Products",
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
            } else if (products.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (connected) "No products available" else "Connect to load products",
                        modifier = Modifier.padding(16.dp),
                        color = AppColors.Secondary
                    )
                }
            } else {
                products.forEach { product ->
                    ProductCard(
                        product = product,
                        onPurchase = {
                            scope.launch {
                                isProcessing = true
                                purchaseResult = null
                                try {
                                    val purchase = kmpIapInstance.requestPurchase(
                                        UnifiedPurchaseRequest(
                                            sku = product.id,
                                            quantity = 1
                                        )
                                    )
                                    // Purchase updates will be received through the Flow
                                } catch (e: Exception) {
                                    purchaseResult = "Purchase failed: ${e.message}"
                                } finally {
                                    isProcessing = false
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
                            text = "Purchase Result",
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
fun ProductCard(
    product: Product,
    onPurchase: () -> Unit,
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
                        text = product.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = AppColors.OnSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = product.description,
                        fontSize = 14.sp,
                        color = AppColors.Secondary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "ID: ${product.id}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AppColors.Secondary
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = product.price,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = AppColors.Primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onPurchase,
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
                    Text("Purchase")
                }
            }
        }
    }
}