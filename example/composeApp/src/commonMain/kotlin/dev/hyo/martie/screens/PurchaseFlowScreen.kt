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
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.requestProducts
import io.github.hyochan.kmpiap.requestPurchase
import io.github.hyochan.kmpiap.toPurchaseInput
import io.github.hyochan.kmpiap.openiap.Product
import io.github.hyochan.kmpiap.openiap.Purchase
import io.github.hyochan.kmpiap.openiap.PurchaseError
import io.github.hyochan.kmpiap.openiap.ProductQueryType
import io.github.hyochan.kmpiap.openiap.ErrorCode
import kotlinx.coroutines.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

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
                
                // Log purchase data as JSON
                println("\n========== PURCHASE SUCCESS (JSON) ==========")
                val json = Json { 
                    prettyPrint = true
                    encodeDefaults = true
                    ignoreUnknownKeys = true
                }
                
                val jsonString = purchase.toPrettyJson(json)
                println(jsonString)
                println("=============================================\n")
                
                // Handle successful purchase
                val dateText = purchase.transactionDate?.let {
                    Instant.fromEpochSeconds(it.toLong()).toLocalDateTime(TimeZone.currentSystemDefault())
                } ?: "N/A"
                purchaseResult = """
                    ✅ Purchase successful (${purchase.platform})
                    Product: ${purchase.productId}
                    Transaction ID: ${purchase.id.ifEmpty { "N/A" }}
                    Date: $dateText
                    Receipt: ${purchase.purchaseToken?.take(50) ?: "N/A"}
                """.trimIndent()
                
                // IMPORTANT: Server-side receipt validation should be performed here
                // Send the receipt to your backend server for validation
                // Example:
                // val isValid = validateReceiptOnServer(purchase.purchaseToken)
                // if (!isValid) {
                //     purchaseResult = "❌ Receipt validation failed"
                //     return@collect
                // }
                
                // After successful server validation, finish the transaction
                // For consumable products (like bulb packs), set isConsumable to true
                scope.launch {
                    try {
                        kmpIapInstance.finishTransaction(
                            purchase = purchase.toPurchaseInput(),
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
                    ErrorCode.UserCancelled -> "⚠️ Purchase cancelled by user"
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
                        val result = kmpIapInstance.requestProducts {
                            skus = PRODUCT_IDS
                            type = ProductQueryType.InApp
                        }
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
                                    val purchase = kmpIapInstance.requestPurchase {
                                        ios {
                                            sku = product.id
                                            quantity = 1
                                        }
                                        android {
                                            skus = listOf(product.id)
                                        }
                                    }
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // Log product details to console in JSON format
                println("\n========== PRODUCT DETAILS (JSON) ==========")
                val json = Json { 
                    prettyPrint = true
                    encodeDefaults = true
                }

                val jsonString = product.toPrettyJson(json)
                println(jsonString)
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
                        text = product.displayPrice,
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

private fun Purchase.toPrettyJson(json: Json): String = toJson().toPrettyJson(json)

private fun Product.toPrettyJson(json: Json): String = toJson().toPrettyJson(json)

private fun Map<String, Any?>.toPrettyJson(json: Json): String {
    return runCatching {
        val element = toJsonElement()
        json.encodeToString(JsonElement.serializer(), element)
    }.getOrElse { error ->
        println("[KMP-IAP Example] Failed to encode map to JSON: ${error.message}")
        buildString {
            appendLine("{")
            val entries = this@toPrettyJson.entries.toList()
            entries.forEachIndexed { index, (key, value) ->
                append("  \"")
                append(key)
                append("\": \"")
                append(value?.toString() ?: "null")
                append("\"")
                if (index < entries.lastIndex) {
                    append(',')
                }
                appendLine()
            }
            append('}')
        }
    }
}

private fun Map<String, Any?>.toJsonElement(): JsonElement = buildJsonObject {
    this@toJsonElement.forEach { (key, value) ->
        put(key, value.toJsonElement())
    }
}

private fun Iterable<*>.toJsonArray(): JsonArray = buildJsonArray {
    this@toJsonArray.forEach { item ->
        add(item.toJsonElement())
    }
}

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is String -> JsonPrimitive(this)
    is Map<*, *> -> this.toStringKeyMapOrNull()?.toJsonElement() ?: JsonPrimitive(toString())
    is Iterable<*> -> this.toJsonArray()
    is Array<*> -> this.asList().toJsonArray()
    else -> JsonPrimitive(this.toString())
}

private fun Map<*, *>.toStringKeyMapOrNull(): Map<String, Any?>? {
    if (isEmpty()) return emptyMap<String, Any?>()
    val result = mutableMapOf<String, Any?>()
    for ((key, value) in this) {
        val stringKey = key as? String ?: return null
        result[stringKey] = value
    }
    return result
}
