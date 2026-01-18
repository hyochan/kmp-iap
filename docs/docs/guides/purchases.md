---
sidebar_position: 3
title: Purchases
---

import IapKitBanner from '@site/src/uis/IapKitBanner';

# Purchases

<IapKitBanner />

Complete guide to implementing in-app purchases with kmp-iap, covering everything from basic setup to advanced purchase handling using Kotlin Multiplatform.

## Purchase Flow Overview

The in-app purchase flow follows this standardized pattern:

1. **Initialize Connection** - Establish connection with the store
2. **Setup State Observers** - Monitor purchase states via StateFlow  
3. **Load Products** - Fetch product information from the store
4. **Request Purchase** - Initiate purchase flow
5. **Handle Updates** - Process purchase results via StateFlow
6. **Deliver Content** - Provide purchased content to user
7. **Finish Transaction** - Complete the transaction with the store

## Key Concepts

### Purchase Types
- **Consumable**: Can be purchased multiple times (coins, gems, lives)
- **Non-Consumable**: Purchased once, owned forever (premium features, ad removal)  
- **Subscriptions**: Recurring purchases with auto-renewal

### Platform Differences
- **iOS**: Uses StoreKit 2 (iOS 15.0+)
- **Android**: Uses Google Play Billing Client v8
- Both platforms use the same API surface in kmp-iap

## Basic Purchase Flow

### 1. Setup Purchase Observers

Before making any purchases, set up StateFlow observers to handle purchase updates and errors:

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.data.*

class PurchaseHandler(
    private val scope: CoroutineScope
) {
    
    fun setupPurchaseObservers() {
        // Observe successful purchases
        scope.launch {
            kmpIapInstance.purchaseUpdatedListener.collectLatest { purchase ->
                println("Purchase update received: ${purchase.productId}")
                handlePurchaseUpdate(purchase)
            }
        }

        // Observe purchase errors
        scope.launch {
            kmpIapInstance.purchaseErrorListener.collectLatest { error ->
                println("Purchase failed: ${error.message}")
                handlePurchaseError(error)
            }
        }
    }
    
    fun dispose() {
        kmpIapInstance.endConnection()
    }
}
```

### 2. Using with ViewModel (Recommended)

For a more structured approach, use this purchase handler pattern:

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

class ProductsViewModel : ViewModel() {
    private val productIds = listOf(
        "dev.hyo.martie.10bulbs",
        "dev.hyo.martie.30bulbs"
    )
    
    
    data class PurchaseState(
        val isProcessing: Boolean = false,
        val purchaseResult: String? = null,
        val products: List<Product> = emptyList()
    )


    private val _state = MutableStateFlow(PurchaseState())
    val state: StateFlow<PurchaseState> = _state.asStateFlow()

    init {
        setupPurchaseObservers()

        // Initialize connection and load products
        viewModelScope.launch {
            kmpIapInstance.initConnection()
            loadProducts()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        kmpIAP.dispose()
    }
    
    // Purchase observer setup...
}
```

### 3. Request a Purchase

Use the unified API for initiating purchases:

```kotlin
suspend fun handlePurchase(productId: String) {
    try {
        _state.update { 
            it.copy(
                isProcessing = true,
                purchaseResult = "Processing purchase..."
            )
        }

        // Request purchase
        kmpIapInstance.requestPurchase {
            apple {
                sku = productId
                quantity = 1
                // Optional: Pass attribution data (v1.3.7+)
                // advancedCommerceData = "campaign_id"
            }
            google {  // Recommended (v1.3.15+), or use `android { }` for backward compatibility
                skus = listOf(productId)
                obfuscatedAccountIdAndroid = getUserId()
            }
        }
        
        // Result will be emitted via currentPurchase StateFlow
    } catch (error: PurchaseError) {
        _state.update {
            it.copy(
                isProcessing = false,
                purchaseResult = "❌ Purchase failed: ${error.message}"
            )
        }
    }
}
```

## Product Loading

### Loading Products

```kotlin
suspend fun loadProducts() {
    try {
        // Load products
        val products = kmpIapInstance.fetchProducts {
            skus = productIds
            type = ProductQueryType.InApp
        }

        println("Loaded ${products.size} products")
        products.forEach { product ->
            println("Product: ${product.productId}")
            println("Price: ${product.price}")
            println("Title: ${product.title}")
        }

        _state.update { it.copy(products = products) }
    } catch (e: PurchaseError) {
        println("Error loading products: $e")
    }
}
```

### Loading Subscriptions

```kotlin
suspend fun loadSubscriptions() {
    try {
        val subscriptionIds = listOf("premium_monthly", "premium_yearly")
        val subscriptions = kmpIapInstance.fetchProducts {
            skus = subscriptionIds
            type = ProductQueryType.Subscription
        }

        subscriptions.forEach { sub ->
            println("Subscription: ${sub.title}")
            println("Price: ${sub.price}")
            println("Period: ${sub.subscriptionPeriod}")
        }
    } catch (e: PurchaseError) {
        println("Error loading subscriptions: $e")
    }
}
```

## Subscription Purchases

### Subscription Purchase

```kotlin
suspend fun requestSubscription(productId: String) {
    try {
        kmpIapInstance.requestPurchase {
            apple {
                sku = productId
                quantity = 1
            }
            google {  // Recommended (v1.3.15+)
                skus = listOf(productId)
                obfuscatedAccountIdAndroid = getUserId()
            }
        }
        // Result via purchaseUpdatedListener Flow
    } catch (e: PurchaseError) {
        handleError(e)
    }
}

// With subscription offers (Android)
suspend fun requestSubscriptionWithOffer(
    productId: String,
    offerToken: String
) {
    kmpIapInstance.requestPurchase {
        apple {
            sku = productId
            quantity = 1
        }
        google {  // Recommended (v1.3.15+)
            skus = listOf(productId)
            subscriptionOffers = listOf(
                SubscriptionOfferAndroid(
                    sku = productId,
                    offerToken = offerToken
                )
            )
        }
    }
}
```

## Important Notes

### Purchase Flow Best Practices

1. **Always set up observers first** before making any purchase requests
2. **Handle both success and error cases** appropriately
3. **Show loading states** during purchase processing
4. **Validate purchases server-side** for security
5. **Finish transactions** after delivering content

### Handling Purchase Success

```kotlin
private suspend fun handlePurchaseUpdate(purchase: Purchase) {
    println("Purchase successful: ${purchase.productId}")
    
    // Deliver the product to the user
    deliverProduct(purchase.productId)
    
    // Finish the transaction
    try {
        val success = kmpIapInstance.finishTransaction(
            purchase = purchase.toPurchaseInput(),
            isConsumable = true // Set appropriately for your product type
        )

        if (success) {
            println("Transaction completed successfully")
        }
    } catch (e: Exception) {
        println("Error finishing transaction: $e")
    }
}
```

## Getting Product Information

### Retrieving Product Prices

```kotlin
class ProductInfo {
    suspend fun loadProductInformation(productIds: List<String>): List<Product> {
        return try {
            // Request products from store
            val products = kmpIapInstance.fetchProducts {
                skus = productIds
                type = ProductQueryType.InApp
            }

            products.forEach { product ->
                println("Product: ${product.productId}")
                println("Title: ${product.title}")
                println("Description: ${product.description}")
                println("Price: ${product.price}")
                println("Currency: ${product.currencyCode}")
                println("Price Micros: ${product.priceAmountMicros}")
            }

            products
        } catch (e: PurchaseError) {
            println("Error loading product information: $e")
            emptyList()
        }
    }
}
```

### Platform Support

```kotlin
import io.github.hyochan.kmpiap.openiap.IapPlatform

class PlatformSupport {
    suspend fun checkPurchaseSupport(): Boolean {
        return try {
            when (kmpIapInstance.getCurrentPlatform()) {
                IapPlatform.Ios -> {
                    // Check if device can make payments
                    kmpIapInstance.initConnection()
                    true
                }
                IapPlatform.Android -> {
                    // Check Play Store connection
                    kmpIapInstance.initConnection()
                    true
                }
            }
        } catch (e: PurchaseError) {
            println("Error checking purchase support: $e")
            false
        }
    }
}
```

### Checking Platform Compatibility

```kotlin
fun checkPlatformFeatures() {
    when (kmpIapInstance.getCurrentPlatform()) {
        IapPlatform.Ios -> {
            // iOS-specific features
            println("iOS platform detected")
            // Can use iOS-specific methods like:
            // - kmpIapInstance.presentCodeRedemptionSheetIOS()
            // - kmpIapInstance.showManageSubscriptionsIOS()
            // - kmpIapInstance.getStorefrontIOS()
        }
        IapPlatform.Android -> {
            // Android-specific features
            println("Android platform detected")
            // Can use Android-specific methods like:
            // - kmpIapInstance.consumePurchase()
            // - kmpIapInstance.deepLinkToSubscriptionsAndroid()
            // - kmpIapInstance.requestPurchaseHistoryAndroid()
        }
    }
}
```

## Product Types

### Consumable Products

Products that can be purchased multiple times:

```kotlin
suspend fun handleConsumableProduct(purchase: Purchase) {
    // Deliver the consumable content (coins, lives, etc.)
    deliverConsumableProduct(purchase.productId)

    // Finish transaction as consumable
    val success = kmpIapInstance.finishTransaction(
        purchase = purchase.toPurchaseInput(),
        isConsumable = true
    )

    if (success) {
        println("Consumable product delivered and consumed")
    }
}
```

### Non-Consumable Products

Products purchased once and owned permanently:

```kotlin
suspend fun handleNonConsumableProduct(purchase: Purchase) {
    // Deliver the permanent content (premium features, ad removal)
    deliverPermanentProduct(purchase.productId)

    // Finish transaction as non-consumable
    val success = kmpIapInstance.finishTransaction(
        purchase = purchase.toPurchaseInput(),
        isConsumable = false
    )

    if (success) {
        println("Non-consumable product delivered")
    }
}
```

### Subscriptions

Recurring purchases with auto-renewal:

```kotlin
suspend fun handleSubscriptionProduct(purchase: Purchase) {
    // Activate subscription for user
    activateSubscription(purchase.productId)

    // Finish transaction as non-consumable
    val success = kmpIapInstance.finishTransaction(
        purchase = purchase.toPurchaseInput(),
        isConsumable = false
    )

    if (success) {
        println("Subscription activated")
    }
}
```

## Advanced Purchase Handling

### Purchase Restoration

Restore previously purchased items:

```kotlin
suspend fun restorePurchases() {
    try {
        // Get available purchases
        val purchases = kmpIapInstance.getAvailablePurchases()
        println("Found ${purchases.size} available purchases")

        // Process each restored purchase
        purchases.forEach { purchase ->
            deliverProduct(purchase.productId)
        }
    } catch (e: PurchaseError) {
        println("Error restoring purchases: $e")
    }
}
```

### Handling Already Owned Error

Handle cases where user already owns the item:

```kotlin
private fun handlePurchaseError(error: PurchaseError) {
    println("Purchase failed: ${error.message}")

    when (error.code) {
        ErrorCode.AlreadyOwned -> {
            println("User already owns this item")
            scope.launch {
                // Refresh available purchases
                kmpIapInstance.getAvailablePurchases()
            }
        }
        ErrorCode.UserCancelled -> {
            // User cancelled, no action needed
            println("Purchase cancelled by user")
        }
        else -> {
            // Handle other errors
            showErrorDialog(error.message)
        }
    }
}
```

### Subscription Management

Open native subscription management:

```kotlin
suspend fun openSubscriptionManagement() {
    try {
        when (getCurrentPlatform()) {
            IapPlatform.Ios -> {
                kmpIapInstance.showManageSubscriptionsIOS()
            }
            IapPlatform.Android -> {
                kmpIapInstance.deepLinkToSubscriptions(
                    DeepLinkOptions(skuAndroid = "premium_monthly")
                )
            }
        }
    } catch (e: PurchaseError) {
        println("Failed to open subscription management: $e")
    }
}
```

### Receipt Validation

Validate purchases server-side for security:

```kotlin
suspend fun validatePurchaseReceipt(purchase: Purchase): Boolean {
    return try {
        // Send to your server for validation
        val response = api.validatePurchase(
            productId = purchase.productId,
            purchaseToken = purchase.purchaseToken,
            receipt = purchase.transactionReceipt,
            platform = getCurrentPlatform().name
        )

        response.isValid
    } catch (e: Exception) {
        println("Receipt validation failed: $e")
        false
    }
}
```

## Error Handling

### Common Purchase Errors

```kotlin
fun handlePurchaseError(error: PurchaseError) {
    when (error.code) {
        ErrorCode.UserCancelled -> {
            println("User cancelled the purchase")
        }
        ErrorCode.NetworkError -> {
            println("Network error occurred")
            showRetryDialog()
        }
        ErrorCode.ServiceUnavailable -> {
            println("Billing service unavailable")
        }
        ErrorCode.ProductNotAvailable -> {
            println("Requested item is unavailable")
        }
        ErrorCode.DeveloperError -> {
            println("Invalid arguments provided to the API")
        }
        ErrorCode.AlreadyOwned -> {
            println("User already owns this item")
            handleAlreadyOwned()
        }
        ErrorCode.Unknown -> {
            println("Unknown error: ${error.message}")
        }
    }
}
```

## Testing Purchases

### iOS Testing

Set up iOS testing environment:

```kotlin
// For iOS testing in sandbox environment
fun setupIOSTesting() {
    println("Testing on iOS Sandbox")
    
    // Use test Apple ID for sandbox testing
    // Products must be configured in App Store Connect
    // Test with different sandbox user accounts
}
```

### Android Testing

Set up Android testing environment:

```kotlin
// For Android testing with test purchases
fun setupAndroidTesting() {
    println("Testing on Android")
    
    // Use test product IDs like:
    // - android.test.purchased
    // - android.test.canceled  
    // - android.test.refunded
    // - android.test.item_unavailable
    
    val testProductIds = listOf(
        "android.test.purchased", // Always succeeds
        "android.test.canceled"   // Always cancelled
    )
}
```

## Complete Example

Here's a complete working example:

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hyochan.kmpiap.useIap.*
import io.github.hyochan.kmpiap.data.*

class PurchaseService : ViewModel() {
    init {
        // Initialize IAP connection
        viewModelScope.launch {
            kmpIapInstance.initConnection()
        }
    
    init {
        setupPurchaseObservers()
    }
    
    private fun setupPurchaseObservers() {
        // Observe purchase success
        viewModelScope.launch {
            kmpIapInstance.purchaseUpdatedListener.collectLatest { purchase ->
                handlePurchaseSuccess(purchase)
            }
        }

        // Observe errors
        viewModelScope.launch {
            kmpIapInstance.purchaseErrorListener.collectLatest { error ->
                handlePurchaseError(error)
            }
        }
    }
    
    private suspend fun handlePurchaseSuccess(purchase: Purchase) {
        // 1. Deliver product
        deliverProduct(purchase.productId)

        // 2. Finish transaction
        val success = kmpIapInstance.finishTransaction(
            purchase = purchase.toPurchaseInput(),
            isConsumable = true
        )

        if (success) {
            println("Transaction completed")
        }
    }
    
    private fun handlePurchaseError(error: PurchaseError) {
        if (error.code == ErrorCode.AlreadyOwned) {
            // Handle "already owned" error
            viewModelScope.launch {
                kmpIapInstance.getAvailablePurchases()
            }
        }
    }
    
    suspend fun purchaseProduct(productId: String) {
        kmpIapInstance.requestPurchase {
            apple {
                sku = productId
                quantity = 1
            }
            google {  // Recommended (v1.3.15+)
                skus = listOf(productId)
                obfuscatedAccountIdAndroid = getUserId()
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        kmpIapInstance.endConnection()
    }

    private fun deliverProduct(productId: String) {
        // Implement your product delivery logic
    }
    
    private fun getUserId(): String {
        // Return user ID for fraud prevention
        return "user_123"
    }
}
```

## Compose UI Integration

```kotlin
@Composable
fun PurchaseScreen(viewModel: PurchaseService = viewModel()) {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isConnected by remember { mutableStateOf(false) }
    
    Column {
        // Connection indicator
        if (!isConnected) {
            Card(
                backgroundColor = Color.Red,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Store not connected", color = Color.White)
            }
        }
        
        // Products list
        LazyColumn {
            items(products) { product ->
                ProductCard(
                    product = product,
                    onPurchase = {
                        scope.launch {
                            viewModel.purchaseProduct(product.productId)
                        }
                    }
                )
            }
        }
    }
}
```

This guide covers the complete purchase flow using the kmp-iap API, with examples demonstrating the Kotlin Multiplatform approach to in-app purchases.
