package dev.hyo.martie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import io.github.hyochan.kmpiap.openiap.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val PRODUCT_IDS = listOf("dev.hyo.martie.10bulbs", "dev.hyo.martie.30bulbs")

/**
 * Alternative Billing Example
 *
 * Demonstrates alternative billing flows for iOS and Android:
 *
 * iOS (Alternative Billing):
 * - Redirects users to external website
 * - No onPurchaseUpdated callback when using external URL
 * - User completes purchase on external website
 * - Must implement deep link to return to app
 *
 * Android (Alternative Billing Only):
 * - Step 1: Check availability with checkAlternativeBillingAvailabilityAndroid()
 * - Step 2: Show information dialog with showAlternativeBillingDialogAndroid()
 * - Step 3: Process payment in your payment system
 * - Step 4: Create token with createAlternativeBillingTokenAndroid()
 * - Must report token to Google Play backend within 24 hours
 * - No onPurchaseUpdated callback
 *
 * Android (User Choice Billing):
 * - Call requestPurchase() normally
 * - Google shows selection dialog automatically
 * - If user selects Google Play: onPurchaseUpdated callback
 * - If user selects alternative: No callback (manual flow required)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlternativeBillingScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var externalUrl by remember { mutableStateOf("https://openiap.dev") }
    var selectedProduct by remember { mutableStateOf<ProductCommon?>(null) }
    var billingMode by remember { mutableStateOf(AlternativeBillingModeAndroid.AlternativeOnly) }
    var showModeSelector by remember { mutableStateOf(false) }
    var purchaseResult by remember { mutableStateOf("") }
    var lastPurchase by remember { mutableStateOf<Purchase?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isReconnecting by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    var products by remember { mutableStateOf<List<ProductCommon>>(emptyList()) }
    var currentPlatform by remember { mutableStateOf("") }

    // Detect platform
    LaunchedEffect(Unit) {
        currentPlatform = getPlatformName()
    }

    // Initialize connection
    LaunchedEffect(Unit) {
        try {
            val config = if (currentPlatform == "Android") {
                InitConnectionConfig(alternativeBillingModeAndroid = billingMode)
            } else null

            connected = kmpIapInstance.initConnection(config)

            if (connected) {
                // Fetch products
                val result = kmpIapInstance.fetchProducts(
                    ProductRequest(
                        skus = PRODUCT_IDS,
                        type = ProductQueryType.InApp
                    )
                )
                products = when (result) {
                    is FetchProductsResultProducts -> result.value
                    is FetchProductsResultSubscriptions -> result.value
                } ?: emptyList()
            }
        } catch (e: Exception) {
            purchaseResult = "❌ Connection failed: ${e.message}"
        }
    }

    // Listen for purchase updates
    LaunchedEffect(Unit) {
        launch {
            kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                lastPurchase = purchase
                isProcessing = false

                val dateText = purchase.transactionDate?.let {
                    Instant.fromEpochSeconds(it.toLong())
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                } ?: "N/A"

                purchaseResult = """
                    ✅ Purchase successful
                    Product: ${purchase.productId}
                    Transaction ID: ${purchase.id}
                    Date: $dateText
                """.trimIndent()

                // Finish transaction
                scope.launch {
                    try {
                        kmpIapInstance.finishTransaction(
                            purchase = purchase,
                            isConsumable = true
                        )
                    } catch (e: Exception) {
                        println("Failed to finish transaction: ${e.message}")
                    }
                }
            }
        }
    }

    // Listen for purchase errors
    LaunchedEffect(Unit) {
        launch {
            kmpIapInstance.purchaseErrorListener.collect { error ->
                isProcessing = false
                purchaseResult = "❌ Purchase failed: ${error.message}"
            }
        }
    }

    // Reconnect with new billing mode
    fun reconnectWithMode(newMode: AlternativeBillingModeAndroid) {
        scope.launch {
            try {
                isReconnecting = true
                purchaseResult = "Reconnecting with new billing mode..."

                // End current connection
                kmpIapInstance.endConnection()

                // Wait for cleanup
                kotlinx.coroutines.delay(500)

                // Reinitialize with new mode
                val config = if (currentPlatform == "Android") {
                    InitConnectionConfig(alternativeBillingModeAndroid = newMode)
                } else null

                connected = kmpIapInstance.initConnection(config)

                purchaseResult = "✅ Reconnected with ${
                    when (newMode) {
                        AlternativeBillingModeAndroid.AlternativeOnly -> "Alternative Only"
                        AlternativeBillingModeAndroid.UserChoice -> "User Choice"
                        else -> "None"
                    }
                } mode"

                // Reload products
                if (connected) {
                    val result = kmpIapInstance.fetchProducts(
                        ProductRequest(
                            skus = PRODUCT_IDS,
                            type = ProductQueryType.InApp
                        )
                    )
                    products = when (result) {
                    is FetchProductsResultProducts -> result.value
                    is FetchProductsResultSubscriptions -> result.value
                } ?: emptyList()
                }
            } catch (e: Exception) {
                purchaseResult = "❌ Reconnection failed: ${e.message}"
            } finally {
                isReconnecting = false
            }
        }
    }

    // Handle iOS alternative billing purchase
    fun handleIOSAlternativeBillingPurchase(product: ProductCommon) {
        scope.launch {
            if (externalUrl.trim().isEmpty()) {
                purchaseResult = "❌ Please enter a valid external purchase URL"
                return@launch
            }

            isProcessing = true
            purchaseResult = "🌐 Opening external purchase link..."

            try {
                val result = kmpIapInstance.presentExternalPurchaseLinkIOS(externalUrl)

                if (result.error != null) {
                    purchaseResult = "❌ Error: ${result.error}"
                } else if (result.success) {
                    purchaseResult = """
                        ✅ External purchase link opened successfully

                        Product: ${product.id}
                        URL: $externalUrl

                        User was redirected to external website.

                        Note: Complete purchase on your website and implement server-side validation.
                    """.trimIndent()
                }
            } catch (e: Exception) {
                purchaseResult = "❌ Error: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    // Handle Android Alternative Billing Only (3-step flow)
    fun handleAndroidAlternativeBillingOnly(product: ProductCommon) {
        scope.launch {
            isProcessing = true
            purchaseResult = "Checking alternative billing availability..."

            try {
                // Step 1: Check availability
                val isAvailable = kmpIapInstance.checkAlternativeBillingAvailabilityAndroid()

                if (!isAvailable) {
                    purchaseResult = "❌ Alternative billing not available"
                    isProcessing = false
                    return@launch
                }

                purchaseResult = "Showing information dialog..."

                // Step 2: Show information dialog
                val userAccepted = kmpIapInstance.showAlternativeBillingDialogAndroid()

                if (!userAccepted) {
                    purchaseResult = "ℹ️ User cancelled"
                    isProcessing = false
                    return@launch
                }

                purchaseResult = "Creating token..."

                // Step 2.5: In production, process payment here with your payment system

                // Step 3: Create token (after successful payment)
                val token = kmpIapInstance.createAlternativeBillingTokenAndroid()

                if (token != null) {
                    purchaseResult = """
                        ✅ Alternative billing completed (DEMO)

                        Product: ${product.id}
                        Token: ${token.take(20)}...

                        ⚠️ Important:
                        1. Process payment with your payment system
                        2. Report token to Google Play backend within 24 hours
                        3. No onPurchaseUpdated callback
                    """.trimIndent()
                } else {
                    purchaseResult = "❌ Failed to create reporting token"
                }
            } catch (e: Exception) {
                purchaseResult = "❌ Error: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    // Handle Android User Choice Billing
    fun handleAndroidUserChoiceBilling(product: ProductCommon) {
        scope.launch {
            isProcessing = true
            purchaseResult = "Showing user choice dialog..."

            try {
                kmpIapInstance.requestPurchase(
                    RequestPurchaseProps(
                        request = RequestPurchaseProps.Request.Purchase(
                            RequestPurchasePropsByPlatforms(
                                android = RequestPurchaseAndroidProps(
                                    skus = listOf(product.id)
                                )
                            )
                        ),
                        type = ProductQueryType.InApp,
                        useAlternativeBilling = true
                    )
                )

                purchaseResult = """
                    🔄 User choice dialog shown

                    Product: ${product.id}

                    If user selects:
                    - Google Play: onPurchaseUpdated callback
                    - Alternative: Manual flow required
                """.trimIndent()
            } catch (e: Exception) {
                purchaseResult = "❌ Error: ${e.message}"
                isProcessing = false
            }
        }
    }

    // Handle purchase based on platform and mode
    fun handlePurchase(product: ProductCommon) {
        if (currentPlatform == "iOS") {
            handleIOSAlternativeBillingPurchase(product)
        } else if (currentPlatform == "Android") {
            when (billingMode) {
                AlternativeBillingModeAndroid.AlternativeOnly -> handleAndroidAlternativeBillingOnly(product)
                AlternativeBillingModeAndroid.UserChoice -> handleAndroidUserChoiceBilling(product)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alternative Billing") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFF9800),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (!connected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Connecting to Store...")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .swipeToBack(navController)
                    .padding(16.dp)
            ) {
                // Platform info
                Text(
                    text = if (currentPlatform == "iOS")
                        "External purchase links (iOS 16.0+)"
                    else
                        "Google Play alternative billing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.background(
                        Color(0xFFFF9800),
                        RoundedCornerShape(8.dp)
                    ).padding(8.dp).fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Info Card
                InfoCard(currentPlatform, billingMode)

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Selector (Android only)
                if (currentPlatform == "Android") {
                    ModeSelectorCard(
                        billingMode = billingMode,
                        onModeClick = { showModeSelector = true }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // External URL Input (iOS only)
                if (currentPlatform == "iOS") {
                    ExternalUrlCard(
                        url = externalUrl,
                        onUrlChange = { externalUrl = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Reconnecting Status
                if (isReconnecting) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3CD)
                        )
                    ) {
                        Text(
                            text = "🔄 Reconnecting with new billing mode...",
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Connection Status
                ConnectionStatusCard(connected, currentPlatform, billingMode)

                Spacer(modifier = Modifier.height(16.dp))

                // Products
                Text(
                    text = "Select Product",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (products.isEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading products...")
                        }
                    }
                } else {
                    products.forEach { product ->
                        ProductCard(
                            product = product,
                            isSelected = selectedProduct?.id == product.id,
                            onClick = { selectedProduct = product }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Product Details & Action
                selectedProduct?.let { product ->
                    ProductDetailsCard(product)
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { handlePurchase(product) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !isProcessing && connected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Text(
                            text = when {
                                isProcessing -> "Processing..."
                                currentPlatform == "iOS" -> "🛒 Buy (External URL)"
                                billingMode == AlternativeBillingModeAndroid.AlternativeOnly -> "🛒 Buy (Alternative Only)"
                                else -> "🛒 Buy (User Choice)"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Purchase Result
                if (purchaseResult.isNotEmpty()) {
                    PurchaseResultCard(
                        result = purchaseResult,
                        onDismiss = {
                            purchaseResult = ""
                            lastPurchase = null
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Last Purchase
                lastPurchase?.let { purchase ->
                    LastPurchaseCard(purchase)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Instructions
                InstructionsCard()
            }
        }
    }

    // Mode Selector Modal (Android)
    if (showModeSelector) {
        AlertDialog(
            onDismissRequest = { showModeSelector = false },
            title = { Text("Select Billing Mode") },
            text = {
                Column {
                    ModeSelectorOption(
                        title = "Alternative Billing Only",
                        description = "Only your payment system is available. Users cannot use Google Play.",
                        isSelected = billingMode == AlternativeBillingModeAndroid.AlternativeOnly,
                        onClick = {
                            billingMode = AlternativeBillingModeAndroid.AlternativeOnly
                            showModeSelector = false
                            reconnectWithMode(AlternativeBillingModeAndroid.AlternativeOnly)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ModeSelectorOption(
                        title = "User Choice Billing",
                        description = "Users can choose between Google Play and your payment system.",
                        isSelected = billingMode == AlternativeBillingModeAndroid.UserChoice,
                        onClick = {
                            billingMode = AlternativeBillingModeAndroid.UserChoice
                            showModeSelector = false
                            reconnectWithMode(AlternativeBillingModeAndroid.UserChoice)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showModeSelector = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InfoCard(platform: String, billingMode: AlternativeBillingModeAndroid) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ℹ️ How It Works",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFFE65100)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (platform == "iOS") {
                Text(
                    text = """
                        • Enter your external purchase URL
                        • Tap Purchase on any product
                        • User will be redirected to the external URL
                        • Complete purchase on your website
                        • No onPurchaseUpdated callback
                        • Implement deep link to return to app
                    """.trimIndent(),
                    fontSize = 13.sp,
                    color = Color(0xFF5D4037),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """
                        ⚠️ iOS 16.0+ required
                        ⚠️ Valid external URL needed
                        ⚠️ useAlternativeBilling: true is set
                    """.trimIndent(),
                    fontSize = 12.sp,
                    color = Color(0xFFD84315),
                    lineHeight = 18.sp
                )
            } else {
                val infoText = if (billingMode == AlternativeBillingModeAndroid.AlternativeOnly) {
                    """
                        • Alternative Billing Only Mode
                        • Users CANNOT use Google Play billing
                        • Only your payment system available
                        • 3-step manual flow required
                        • No onPurchaseUpdated callback
                        • Must report to Google within 24h
                    """.trimIndent()
                } else {
                    """
                        • User Choice Billing Mode
                        • Users choose between:
                          - Google Play (30% fee)
                          - Your payment system (lower fee)
                        • Google shows selection dialog
                        • If Google Play: onPurchaseUpdated
                        • If alternative: Manual flow
                    """.trimIndent()
                }

                Text(
                    text = infoText,
                    fontSize = 13.sp,
                    color = Color(0xFF5D4037),
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """
                        ⚠️ Requires approval from Google
                        ⚠️ Must report tokens within 24 hours
                        ⚠️ Backend integration required
                    """.trimIndent(),
                    fontSize = 12.sp,
                    color = Color(0xFFD84315),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ModeSelectorCard(
    billingMode: AlternativeBillingModeAndroid,
    onModeClick: () -> Unit
) {
    Column {
        Text(
            text = "Billing Mode",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onModeClick() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (billingMode == AlternativeBillingModeAndroid.AlternativeOnly)
                        "Alternative Billing Only"
                    else
                        "User Choice Billing",
                    fontSize = 14.sp
                )
                Text("▼", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun ExternalUrlCard(url: String, onUrlChange: (String) -> Unit) {
    Column {
        Text(
            text = "External Purchase URL",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://your-payment-site.com/checkout") },
            singleLine = true
        )
        Text(
            text = "This URL will be opened when a user taps Purchase",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ConnectionStatusCard(
    connected: Boolean,
    platform: String,
    billingMode: AlternativeBillingModeAndroid
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Store Connection:",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (connected) "✅ Connected" else "❌ Disconnected",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (connected) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            if (platform == "Android") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Current mode: ${
                        when (billingMode) {
                            AlternativeBillingModeAndroid.AlternativeOnly -> "ALTERNATIVE_ONLY"
                            AlternativeBillingModeAndroid.UserChoice -> "USER_CHOICE"
                            else -> "NONE"
                        }
                    }",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun ProductCard(product: ProductCommon, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = if (isSelected) Color(0xFFFF9800) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFF3E0) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = product.displayPrice,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFFF9800)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = product.description,
                fontSize = 14.sp,
                color = Color.Gray
            )
            if (isSelected) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFF9800),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "✓ Selected",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductDetailsCard(product: ProductCommon) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Product Details",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            DetailRow("ID:", product.id)
            DetailRow("Title:", product.title)
            DetailRow("Price:", product.displayPrice)
            DetailRow("Type:", product.type.rawValue)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PurchaseResultCard(result: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Purchase Result",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = Color(0xFFFF9800), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = result,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun LastPurchaseCard(purchase: Purchase) {
    Column {
        Text(
            text = "Last Purchase",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Product: ${purchase.productId}", fontSize = 14.sp)
                Text("Transaction: ${purchase.id}", fontSize = 14.sp)
                val dateText = purchase.transactionDate?.let {
                    Instant.fromEpochSeconds(it.toLong())
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .toString()
                } ?: "N/A"
                Text("Date: $dateText", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """
                        ℹ️ Transaction auto-finished for testing.
                        PRODUCTION: Validate on backend first!
                    """.trimIndent(),
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun InstructionsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Testing Instructions:",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF1565C0)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = """
                    1. Select a product from the list
                    2. Tap the purchase button
                    3. Follow the platform-specific flow
                    4. Check the purchase result
                    5. Verify token/URL behavior
                """.trimIndent(),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ModeSelectorOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 2.dp,
                color = if (isSelected) Color(0xFFFF9800) else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFF3E0) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}

// Platform detection helper
expect fun getPlatformName(): String
