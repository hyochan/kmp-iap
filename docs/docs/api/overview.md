---
sidebar_position: 1
---

# API Overview

KMP IAP provides a comprehensive API for handling in-app purchases across platforms. This overview covers the main components and their usage.

## Core Components

### InAppPurchase Interface

The main interface for all IAP operations:

```kotlin
interface InAppPurchase {
    // Connection management
    suspend fun initConnection()
    fun endConnection()
    fun getStore(): Store?
    
    // Product management
    suspend fun requestProducts(params: RequestProductsParams): List<BaseProduct>
    suspend fun getAvailablePurchases(): List<Purchase>
    suspend fun getPurchaseHistories(type: PurchaseType? = null): List<Purchase>
    
    // Purchase operations
    suspend fun requestPurchase(request: RequestPurchase, type: PurchaseType)
    suspend fun requestSubscription(request: RequestPurchase, type: PurchaseType)
    suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean = true): FinishTransactionResult
    
    // Platform-specific
    suspend fun flushFailedPurchasesCachedAsPendingAndroid(): Boolean
    suspend fun buyPromotedProductIOS(): Boolean
    suspend fun validateReceiptIos(
        receiptBody: Map<String, Any>,
        isTest: Boolean = true
    ): Map<String, Any?>
    
    // Event flows
    val purchaseUpdatedFlow: SharedFlow<Purchase>
    val purchaseErrorFlow: SharedFlow<PurchaseError>
    val promotedProductFlow: SharedFlow<String?>
}
```

### Factory Function

Create an instance of InAppPurchase:

```kotlin
fun createInAppPurchase(): InAppPurchase
```

## Data Types

### Purchase Types

```kotlin
enum class PurchaseType {
    INAPP,  // One-time purchases
    SUBS    // Subscriptions
}
```

### Store Types

```kotlin
enum class Store {
    PLAY_STORE,    // Google Play Store
    APP_STORE,     // Apple App Store
    STRIPE,        // Stripe (future)
    NONE           // No store available
}
```

### Error Codes

```kotlin
enum class ErrorCode {
    E_OK,
    E_USER_CANCELLED,
    E_NETWORK_ERROR,
    E_SERVICE_ERROR,
    E_ITEM_UNAVAILABLE,
    E_ITEM_ALREADY_OWNED,
    E_REMOTE_ERROR,
    E_DEVELOPER_ERROR,
    E_BILLING_RESPONSE_JSON_PARSE_ERROR,
    E_UNKNOWN,
    E_PENDING,
    E_NOT_PREPARED,
    E_ALREADY_PREPARED
}
```

## Request/Response Models

### RequestProductsParams

```kotlin
data class RequestProductsParams(
    val skus: List<String>,
    val type: PurchaseType
)
```

### RequestPurchase

Platform-specific purchase requests:

```kotlin
// Android
data class RequestPurchaseAndroid(
    val skus: List<String>,
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    val replacementMode: ReplacementMode? = null
) : RequestPurchase

// iOS
data class RequestPurchaseIOS(
    val sku: String,
    val appAccountToken: String? = null,
    val quantity: Int? = null,
    val discount: RequestedDiscount? = null,
    val requestId: String? = null
) : RequestPurchase
```

### Product Models

```kotlin
// Base product interface
interface BaseProduct {
    val productId: String
    val price: String
    val currency: String
    val localizedPrice: String
    val title: String
    val description: String
}

// One-time purchase product
data class Product(
    override val productId: String,
    override val price: String,
    override val currency: String,
    override val localizedPrice: String,
    override val title: String,
    override val description: String,
    val priceAmountMicros: Long,
    val oneTimePurchaseOfferDetails: OneTimePurchaseOfferDetails?
) : BaseProduct

// Subscription product
data class Subscription(
    override val productId: String,
    override val price: String,
    override val currency: String,
    override val localizedPrice: String,
    override val title: String,
    override val description: String,
    val subscriptionOfferDetails: List<SubscriptionOfferDetails>?
) : BaseProduct
```

## Event Handling

### Purchase Updates

```kotlin
iap.purchaseUpdatedFlow.collect { purchase ->
    when (purchase.purchaseState) {
        PurchaseState.PURCHASED -> {
            // Handle successful purchase
        }
        PurchaseState.PENDING -> {
            // Handle pending purchase
        }
        PurchaseState.UNSPECIFIED -> {
            // Handle unspecified state
        }
    }
}
```

### Error Handling

```kotlin
iap.purchaseErrorFlow.collect { error ->
    when (error.code) {
        ErrorCode.E_USER_CANCELLED -> {
            // User cancelled the purchase
        }
        ErrorCode.E_ITEM_UNAVAILABLE -> {
            // Item not available
        }
        else -> {
            // Handle other errors
        }
    }
}
```

## Platform Considerations

### Android
- Uses Google Play Billing Library v8
- Supports pending transactions
- Handles obfuscated account IDs

### iOS
- Uses StoreKit 1 and 2
- Supports promotional offers
- Handles receipt validation

## Best Practices

1. Always initialize connection before any operations
2. Handle all error cases appropriately
3. Finish transactions promptly
4. Validate receipts on your server
5. Test thoroughly with sandbox/test accounts