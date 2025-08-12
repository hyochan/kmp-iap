---
title: Core Methods
sidebar_position: 3
---

# Core Methods

Essential methods for implementing in-app purchases with kmp-iap v1.0.0-beta.2. All methods support both iOS and Android platforms with unified APIs using Kotlin coroutines.

⚠️ **Platform Differences**: While the API is unified, there are important differences between iOS and Android implementations. Each method documents platform-specific behavior.

## Connection Management

### initConnection()

Initializes the connection to the platform store.

```kotlin
suspend fun initConnection()
```

**Description**: Establishes connection with the App Store (iOS) or Google Play Store (Android). Must be called before any other IAP operations.

**Platform Differences**:
- **iOS**: Connects to StoreKit 2 (iOS 15+) or StoreKit 1 (fallback)
- **Android**: Connects to Google Play Billing Client v7

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

try {
    initConnection()
    println("IAP connection initialized successfully")
} catch (e: PurchaseError) {
    println("Failed to initialize IAP: $e")
}
```

**Throws**: `PurchaseError` if connection fails or already initialized

**See Also**: [dispose()](#dispose), [Connection Lifecycle](../guides/lifecycle.md)

---

### dispose()

Ends the connection to the platform store and cleans up resources.

```kotlin
fun dispose()
```

**Description**: Cleanly closes the store connection, cancels all active flows, and frees resources. Should be called when IAP functionality is no longer needed.

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

// In your cleanup code
override fun onCleared() {
    dispose()
    scope.cancel()
}
```

**Note**: After disposal, a new `UseIap` instance must be created to re-establish connection.

## Product Loading

### getProducts()

Loads in-app product information from the store.

```kotlin
suspend fun getProducts(skus: List<String>): List<Product>
```

**Parameters**:
- `skus` - List of product identifiers

**Returns**: List of products with pricing and metadata

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

try {
    val products = getProducts(
        listOf("coins_100", "coins_500", "remove_ads")
    )
    
    products.forEach { product ->
        println("Product: ${product.productId}")
        println("Price: ${product.price}")
        println("Title: ${product.title}")
    }
} catch (e: PurchaseError) {
    println("Failed to load products: $e")
}
```

**Platform Differences**:
- **iOS**: Uses `SKProductsRequest` (StoreKit)
- **Android**: Uses `queryProductDetails()` (Billing Client)

---

### getSubscriptions()

Loads subscription product information from the store.

```kotlin
suspend fun getSubscriptions(skus: List<String>): List<Product>
```

**Parameters**:
- `skus` - List of subscription identifiers

**Returns**: List of subscription products with subscription-specific metadata

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

try {
    val subscriptions = getSubscriptions(
        listOf("premium_monthly", "premium_yearly")
    )
    
    subscriptions.forEach { sub ->
        println("${sub.title}: ${sub.price}")
        println("Period: ${sub.subscriptionPeriod}")
    }
} catch (e: PurchaseError) {
    println("Failed to load subscriptions: $e")
}
```

## Purchase Processing

### requestPurchase()

Initiates a purchase for a single product.

```kotlin
suspend fun requestPurchase(
    sku: String,
    // iOS-specific parameters
    andDangerouslyFinishTransactionAutomaticallyIOS: Boolean? = null,
    appAccountTokenIOS: String? = null,
    quantityIOS: Int? = null,
    // Android-specific parameters
    obfuscatedAccountIdAndroid: String? = null,
    obfuscatedProfileIdAndroid: String? = null,
    isOfferPersonalizedAndroid: Boolean? = null
)
```

**Parameters**:
- `sku` - Product identifier
- Platform-specific optional parameters

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

// Basic purchase
try {
    requestPurchase(
        sku = "premium_upgrade",
        quantityIOS = 1,  // iOS only
        obfuscatedAccountIdAndroid = "user_123"  // Android only
    )
    
    // Listen to purchase state flow for result
    currentPurchase.collectLatest { purchase ->
        purchase?.let {
            println("Purchase successful: ${it.productId}")
        }
    }
} catch (e: PurchaseError) {
    println("Purchase request failed: $e")
}
```

**Platform Differences**:
- **iOS**: Supports `quantity` and App Account Token
- **Android**: Supports obfuscated user IDs for fraud prevention

---

### requestSubscription()

Initiates a subscription purchase.

```kotlin
suspend fun requestSubscription(
    sku: String,
    // Android subscription offers
    subscriptionOffers: List<SubscriptionOfferAndroid>? = null,
    // Other parameters same as requestPurchase
    obfuscatedAccountIdAndroid: String? = null,
    obfuscatedProfileIdAndroid: String? = null,
    purchaseTokenAndroid: String? = null,
    prorationModeAndroid: Int? = null
)
```

**Parameters**:
- `sku` - Subscription product identifier
- `subscriptionOffers` - Android subscription offers (optional)
- Platform-specific optional parameters

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

try {
    // Simple subscription
    requestSubscription(sku = "premium_monthly")
    
    // Android with offers
    requestSubscription(
        sku = "premium_yearly",
        subscriptionOffers = listOf(
            SubscriptionOfferAndroid(
                sku = "premium_yearly",
                offerToken = "offer_token_here"
            )
        ),
        obfuscatedAccountIdAndroid = "user_123"
    )
} catch (e: PurchaseError) {
    println("Subscription request failed: $e")
}
```

## Transaction Management

### finishTransaction()

Completes a transaction after successful purchase processing.

```kotlin
suspend fun finishTransaction(
    purchase: Purchase,
    isConsumable: Boolean = false
): Boolean
```

**Parameters**:
- `purchase` - The purchase to finish
- `isConsumable` - Whether the product is consumable

**Returns**: `true` if successful, `false` otherwise

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

// In your purchase success handler
scope.launch {
    currentPurchase.collectLatest { purchase ->
        purchase?.let {
            try {
                // Deliver the product to user
                deliverProduct(it.productId)
                
                // Finish the transaction
                val success = finishTransaction(
                    purchase = it,
                    isConsumable = true  // For consumable products
                )
                
                if (success) {
                    println("Transaction completed successfully")
                }
            } catch (e: Exception) {
                println("Failed to finish transaction: $e")
            }
        }
    }
}
```

**Platform Behavior**:
- **iOS**: Calls `finish()` on the transaction
- **Android**: Calls `consumePurchase` (consumable) or `acknowledgePurchase` (non-consumable)

---

### consumePurchase()

Android-specific method to consume a purchase.

```kotlin
suspend fun consumePurchase(purchaseToken: String): Boolean
```

**Parameters**:
- `purchaseToken` - The purchase token to consume

**Returns**: `true` if successful, `false` otherwise

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

// Android-specific consumption
if (getCurrentPlatform() == IAPPlatform.ANDROID) {
    purchase.purchaseToken?.let { token ->
        val consumed = consumePurchase(token)
        if (consumed) {
            println("Purchase consumed successfully")
        }
    }
}
```

**Note**: Only available on Android. Use `finishTransaction()` for cross-platform compatibility.

## Purchase History

### getAvailablePurchases()

Gets all available (unconsumed) purchases.

```kotlin
suspend fun getAvailablePurchases(): List<Purchase>
```

**Returns**: List of available purchases via StateFlow

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

// Observe available purchases
scope.launch {
    availablePurchases.collectLatest { purchases ->
        println("Found ${purchases.size} available purchases")
        purchases.forEach { purchase ->
            println("Product: ${purchase.productId}")
            println("Date: ${purchase.transactionDate}")
        }
    }
}

// Or get current value
val currentPurchases = availablePurchases.value
```

---

### getPurchaseHistories()

Gets purchase history including consumed items.

```kotlin
suspend fun getPurchaseHistories(): List<Purchase>
```

**Returns**: List of historical purchases via StateFlow

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

// Observe purchase history
scope.launch {
    purchaseHistories.collectLatest { history ->
        println("Purchase history: ${history.size} items")
        history.forEach { purchase ->
            println("${purchase.productId} - ${purchase.transactionDate}")
        }
    }
}
```

---

### requestPurchaseHistoryAndroid()

Queries purchase history from Google Play (Android only).

```kotlin
suspend fun requestPurchaseHistoryAndroid()
```

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

if (getCurrentPlatform() == IAPPlatform.ANDROID) {
    requestPurchaseHistoryAndroid()
    // Results will be available in purchaseHistories StateFlow
}
```

## Platform-Specific Methods

### iOS-Specific Methods

#### presentCodeRedemptionSheetIOS()

Presents the App Store code redemption sheet.

```kotlin
suspend fun presentCodeRedemptionSheetIOS()
```

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

if (getCurrentPlatform() == IAPPlatform.IOS) {
    try {
        presentCodeRedemptionSheetIOS()
    } catch (e: PurchaseError) {
        println("Failed to present redemption sheet: $e")
    }
}
```

**Requirements**: iOS 14.0+

---

#### showManageSubscriptionsIOS()

Shows the subscription management interface.

```kotlin
suspend fun showManageSubscriptionsIOS()
```

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

if (getCurrentPlatform() == IAPPlatform.IOS) {
    try {
        showManageSubscriptionsIOS()
    } catch (e: PurchaseError) {
        println("Failed to show subscription management: $e")
    }
}
```

---

#### getStorefrontIOS()

Gets the App Store storefront information.

```kotlin
suspend fun getStorefrontIOS(): Map<String, Any?>?
```

**Returns**: Storefront information or null

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

if (getCurrentPlatform() == IAPPlatform.IOS) {
    val storefront = getStorefrontIOS()
    storefront?.let {
        println("Storefront: $it")
    }
}
```

### Android-Specific Methods

#### deepLinkToSubscriptionsAndroid()

Opens the Google Play subscription management page.

```kotlin
suspend fun deepLinkToSubscriptionsAndroid(sku: String)
```

**Parameters**:
- `sku` - Subscription SKU to manage

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

if (getCurrentPlatform() == IAPPlatform.ANDROID) {
    try {
        deepLinkToSubscriptionsAndroid("premium_monthly")
    } catch (e: PurchaseError) {
        println("Failed to open subscription management: $e")
    }
}
```

## State Management

### StateFlow Properties

All state is exposed via StateFlow for reactive programming:

```kotlin
// Connection state
val isConnected: StateFlow<Boolean>

// Product lists
val products: StateFlow<List<Product>>
val subscriptions: StateFlow<List<Product>>

// Purchase state
val availablePurchases: StateFlow<List<Purchase>>
val purchaseHistories: StateFlow<List<Purchase>>
val currentPurchase: StateFlow<Purchase?>
val currentError: StateFlow<PurchaseError?>

// iOS specific
val promotedProductsIOS: StateFlow<List<Product>?>
```

**Example Usage**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP.*

// Observe connection state
scope.launch {
    isConnected.collectLatest { connected ->
        updateUI(connected)
    }
}

// Observe errors
scope.launch {
    currentError.collectLatest { error ->
        error?.let {
            showErrorDialog(it.message)
            clearError()
        }
    }
}
```

## Best Practices

1. **Always handle errors**: Every method can throw `PurchaseError`
2. **Finish transactions**: Always call `finishTransaction()` after delivering content
3. **Monitor connection**: Check `isConnected` before operations
4. **Use StateFlow**: Prefer reactive state observation over polling
5. **Clear errors**: Call `clearError()` after handling errors

## See Also

- [Types](./types.md) - Type definitions used in methods
- [Error Codes](./error-codes.md) - Error handling details
- [State Management](./listeners.md) - Using StateFlow effectively
- [Examples](../examples/basic-store.md) - Complete implementation examples