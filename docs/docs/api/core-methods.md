---
title: Core Methods
sidebar_position: 3
---

# Core Methods

Essential methods for implementing in-app purchases with kmp-iap v1.0.0. All methods support both iOS and Android platforms with unified APIs using Kotlin coroutines.

⚠️ **Platform Differences**: While the API is unified, there are important differences between iOS and Android implementations. Each method documents platform-specific behavior.

## Connection Management

### initConnection()

Initializes the connection to the platform store.

```kotlin
suspend fun initConnection(): Boolean
```

**Returns**: `true` if connection successful, `false` otherwise

**Description**: Establishes connection with the App Store (iOS) or Google Play Store (Android). Must be called before any other IAP operations.

**Platform Differences**:
- **iOS**: Connects to StoreKit 2 (iOS 15+)
- **Android**: Connects to Google Play Billing Client v8+

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP

val connected = KmpIAP.initConnection()
if (connected) {
    println("IAP connection initialized successfully")
} else {
    println("Failed to initialize IAP connection")
}
```

**Throws**: `PurchaseError` if connection fails

**See Also**: [endConnection()](#endconnection), [Connection Lifecycle](../guides/lifecycle.md)

---

### endConnection()

Ends the connection to the platform store and cleans up resources.

```kotlin
suspend fun endConnection(): Boolean
```

**Returns**: `true` if disconnection successful

**Description**: Cleanly closes the store connection and frees resources. Should be called when IAP functionality is no longer needed.

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP

// In your cleanup code
override fun onCleared() {
    scope.launch {
        KmpIAP.endConnection()
    }
}
```

## Product Loading

### requestProducts()

Loads product information from the store.

```kotlin
suspend fun requestProducts(params: ProductRequest): List<Product>
```

**Parameters**:
- `params` - Product request parameters containing SKUs and product type

**Returns**: List of products with pricing and metadata

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.types.*

// Load in-app products
val products = KmpIAP.requestProducts(
    ProductRequest(
        skus = listOf("coins_100", "coins_500", "remove_ads"),
        type = ProductType.INAPP
    )
)

// Load subscriptions
val subscriptions = KmpIAP.requestProducts(
    ProductRequest(
        skus = listOf("premium_monthly", "premium_yearly"),
        type = ProductType.SUBS
    )
)

products.forEach { product ->
    println("Product: ${product.id}")
    println("Price: ${product.price}")
    println("Title: ${product.title}")
}
```

**Platform Differences**:
- **iOS**: Uses StoreKit 2 API for product requests
- **Android**: Uses `queryProductDetailsAsync()` with automatic product type detection

## Purchase Processing

### requestPurchase()

Initiates a purchase using a unified request format.

```kotlin
suspend fun requestPurchase(request: UnifiedPurchaseRequest): Purchase
```

**Parameters**:
- `request` - Unified purchase request with cross-platform support

**Returns**: Purchase object representing the transaction

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.types.*

// Simple purchase
val purchase = KmpIAP.requestPurchase(
    UnifiedPurchaseRequest(
        sku = "premium_upgrade",
        quantity = 1
    )
)

// With platform-specific options
val purchase = KmpIAP.requestPurchase(
    UnifiedPurchaseRequest(
        sku = "coins_100",
        quantity = 5,
        obfuscatedAccountIdAndroid = "user_123",  // Android
        appAccountTokenIOS = "token_456"          // iOS
    )
)
```

**Platform Differences**:
- **iOS**: Supports App Account Token for fraud prevention
- **Android**: Supports obfuscated user IDs and automatic Activity detection

## Transaction Management

### finishTransaction()

Completes a transaction after successful purchase processing.

```kotlin
suspend fun finishTransaction(
    purchase: Purchase,
    isConsumable: Boolean? = null
)
```

**Parameters**:
- `purchase` - The purchase to finish
- `isConsumable` - Whether the product is consumable (null for auto-detection)

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP

// For consumable products
KmpIAP.finishTransaction(purchase, isConsumable = true)

// For subscriptions (acknowledge only, don't consume)
KmpIAP.finishTransaction(purchase, isConsumable = false)
```

**Platform Behavior**:
- **iOS**: Calls `finish()` on the transaction
- **Android**: 
  - Consumables: Calls `consumeAsync`
  - Non-consumables/Subscriptions: Calls `acknowledgePurchase`

⚠️ **Important**: Subscriptions should NEVER be consumed. They should only be acknowledged once.

## Purchase History

### getAvailablePurchases()

Gets all available (unconsumed/active) purchases.

```kotlin
suspend fun getAvailablePurchases(options: PurchaseOptions? = null): List<Purchase>
```

**Returns**: List of available purchases

**Example**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP

val purchases = KmpIAP.getAvailablePurchases()
purchases.forEach { purchase ->
    println("Product: ${purchase.productId}")
    println("Date: ${purchase.transactionDate}")
    
    // Check acknowledgment status (Android)
    if (purchase.acknowledgedAndroid == true) {
        println("Already acknowledged")
    }
}
```

**Platform Differences**:
- **iOS**: Returns active subscriptions and non-consumed purchases
- **Android**: Queries both INAPP and SUBS purchases separately

---

### getPurchaseHistories()

Gets purchase history including consumed items.

```kotlin
suspend fun getPurchaseHistories(options: PurchaseOptions? = null): List<ProductPurchase>
```

**Returns**: List of historical purchases

**Note**: Android Billing Client v6+ no longer provides purchase history. This method returns an empty list on Android.

## Event Flows

### Purchase Event Flows

All purchase events are exposed via Kotlin Flow for reactive programming:

```kotlin
// Purchase updates
val purchaseUpdatedListener: Flow<Purchase>

// Purchase errors
val purchaseErrorListener: Flow<PurchaseError>

// Promoted products (iOS only)
val promotedProductListener: Flow<String?>
```

**Example Usage**:
```kotlin
import io.github.hyochan.kmpiap.KmpIAP
import kotlinx.coroutines.flow.collectLatest

// Listen for purchase updates
scope.launch {
    KmpIAP.purchaseUpdatedListener.collectLatest { purchase ->
        println("Purchase completed: ${purchase.productId}")
        // Deliver content to user
        deliverContent(purchase.productId)
        // Finish transaction
        KmpIAP.finishTransaction(purchase, isConsumable = true)
    }
}

// Listen for errors
scope.launch {
    KmpIAP.purchaseErrorListener.collectLatest { error ->
        when (error.code) {
            ErrorCode.E_USER_CANCELLED.name -> {
                println("User cancelled purchase")
            }
            else -> {
                println("Purchase error: ${error.message}")
            }
        }
    }
}
```

## Platform-Specific Methods

### iOS-Specific Methods

#### clearTransactionIOS()

Clears pending transactions (iOS only).

```kotlin
suspend fun clearTransactionIOS()
```

#### clearProductsIOS()

Clears cached products (iOS only).

```kotlin
suspend fun clearProductsIOS()
```

#### getPromotedProductIOS()

Gets the currently promoted product (iOS only).

```kotlin
suspend fun getPromotedProductIOS(): String?
```

#### presentCodeRedemptionSheetIOS()

Presents the App Store code redemption sheet.

```kotlin
suspend fun presentCodeRedemptionSheetIOS()
```

#### getStorefrontIOS()

Gets the App Store storefront information.

```kotlin
suspend fun getStorefrontIOS(): String
```

### Android-Specific Methods

#### acknowledgePurchaseAndroid()

Acknowledges a purchase (Android only).

```kotlin
suspend fun acknowledgePurchaseAndroid(purchaseToken: String)
```

**Important**: Subscriptions must be acknowledged within 3 days or they will be refunded.

#### consumePurchaseAndroid()

Consumes a purchase (Android only).

```kotlin
suspend fun consumePurchaseAndroid(purchaseToken: String)
```

**Warning**: Only use for consumable products. Never consume subscriptions.

#### deepLinkToSubscriptions()

Opens the Google Play subscription management page.

```kotlin
suspend fun deepLinkToSubscriptions(options: DeepLinkOptions)
```

**Example**:
```kotlin
KmpIAP.deepLinkToSubscriptions(
    DeepLinkOptions(skuAndroid = "premium_monthly")
)
```

## Validation

### validateReceipt()

Validates a purchase receipt (server-side validation recommended).

```kotlin
suspend fun validateReceipt(options: ValidationOptions): ValidationResult
```

**Parameters**:
- `options` - Validation options including receipt data

**Returns**: Validation result with status

**Note**: Client-side validation is not secure. Always validate receipts on your server.

### isPurchaseValid()

Quick client-side purchase validation.

```kotlin
suspend fun isPurchaseValid(purchase: Purchase): Boolean
```

**Returns**: `true` if purchase appears valid

**Example**:
```kotlin
val isValid = KmpIAP.isPurchaseValid(purchase)
if (isValid) {
    // Proceed with server validation
    validateOnServer(purchase)
}
```

## Utility Methods

### getStore()

Gets the current store type.

```kotlin
fun getStore(): Store
```

**Returns**: Store enum (APP_STORE, PLAY_STORE, AMAZON, or NONE)

### canMakePayments()

Checks if the device can make payments.

```kotlin
suspend fun canMakePayments(): Boolean
```

**Returns**: `true` if payments are available

### getVersion()

Gets the library version.

```kotlin
fun getVersion(): String
```

**Returns**: Version string (e.g., "KMP-IAP v1.0.0 (Android)")

## Best Practices

1. **Always handle errors**: Use try-catch blocks or collect error flows
2. **Finish transactions**: Always call `finishTransaction()` after delivering content
3. **Check acknowledgment**: For Android, check if subscriptions are already acknowledged before re-acknowledging
4. **Monitor connection**: Check connection status before operations
5. **Use proper product types**: Specify ProductType.INAPP or ProductType.SUBS correctly
6. **Never consume subscriptions**: Only acknowledge subscriptions, never consume them
7. **Validate on server**: Always perform receipt validation on your server

## Error Handling

All methods can throw `PurchaseError` with specific error codes:

```kotlin
try {
    val purchase = KmpIAP.requestPurchase(request)
} catch (e: PurchaseError) {
    when (e.code) {
        ErrorCode.E_USER_CANCELLED.name -> handleCancellation()
        ErrorCode.E_ITEM_UNAVAILABLE.name -> handleUnavailable()
        ErrorCode.E_NETWORK_ERROR.name -> handleNetworkError()
        else -> handleGenericError(e)
    }
}
```

## See Also

- [Types](./types.md) - Type definitions used in methods
- [Error Codes](./error-codes.md) - Complete error code reference
- [Events and Listeners](./listeners.md) - Using Flow for events
- [Examples](../examples/basic-store.md) - Complete implementation examples