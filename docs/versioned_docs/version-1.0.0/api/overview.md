---
sidebar_position: 1
---

import IapKitBanner from '@site/src/uis/IapKitBanner';

# API Overview

<IapKitBanner />

KMP IAP provides a comprehensive API for handling in-app purchases across platforms. This overview covers the main components and their usage.

## Core Components

### KmpIAP Class

The main class for all IAP operations. You can use it in two ways:

#### 1. Instance Creation (Recommended)

```kotlin
import io.github.hyochan.kmpiap.KmpIAP

// Create your own instance
val kmpIap = KmpIAP()
kmpIap.initConnection()
kmpIap.requestProducts(...)
```

#### 2. Singleton Pattern (Deprecated)

```kotlin
import io.github.hyochan.kmpiap.KmpIAP

// Use the global singleton instance (deprecated)
KmpIAP.instance.initConnection()
KmpIAP.instance.requestProducts(...)
```

### KmpInAppPurchase Interface

The main interface implemented by KmpIAP:

```kotlin
interface KmpInAppPurchase {
    // Connection management
    suspend fun initConnection(): Boolean
    suspend fun endConnection(): Boolean
    fun getStore(): Store
    suspend fun canMakePayments(): Boolean
    
    // Product management
    suspend fun requestProducts(params: ProductRequest): List<Product>
    suspend fun getAvailablePurchases(options: PurchaseOptions? = null): List<Purchase>
    suspend fun getPurchaseHistories(options: PurchaseOptions? = null): List<ProductPurchase>
    
    // Purchase operations
    suspend fun requestPurchase(request: UnifiedPurchaseRequest): Purchase
    suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean? = null)
    
    // Validation
    suspend fun validateReceipt(options: ValidationOptions): ValidationResult
    suspend fun isPurchaseValid(purchase: Purchase): Boolean
    
    // Platform-specific (iOS)
    suspend fun finishTransactionIOS(transactionId: String)
    suspend fun clearTransactionIOS()
    suspend fun clearProductsIOS()
    suspend fun getStorefrontIOS(): String
    suspend fun presentCodeRedemptionSheetIOS()
    suspend fun getPromotedProductIOS(): String?
    suspend fun buyPromotedProductIOS()
    
    // Platform-specific (Android)
    suspend fun acknowledgePurchaseAndroid(purchaseToken: String)
    suspend fun consumePurchaseAndroid(purchaseToken: String)
    
    // Subscription management
    suspend fun deepLinkToSubscriptions(options: DeepLinkOptions)
    
    // Event listeners
    val purchaseUpdatedListener: Flow<Purchase>
    val purchaseErrorListener: Flow<PurchaseError>
    val promotedProductListener: Flow<String?>
}
```

## Data Types

### Product Types

```kotlin
enum class ProductType {
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

### ProductRequest

```kotlin
data class ProductRequest(
    val skus: List<String>,
    val type: ProductType
)
```

### UnifiedPurchaseRequest

Unified purchase request for all platforms:

```kotlin
data class UnifiedPurchaseRequest(
    val sku: String,
    val quantity: Int = 1,
    // Android-specific
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    val replacementMode: AndroidProrationMode? = null,
    // iOS-specific
    val appAccountToken: String? = null,
    val discount: RequestedDiscount? = null,
    val requestId: String? = null
)
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
val kmpIap = KmpIAP()
kmpIap.purchaseUpdatedListener.collect { purchase ->
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
val kmpIap = KmpIAP()
kmpIap.purchaseErrorListener.collect { error ->
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
- Uses StoreKit 2 (iOS 15+)
- Supports promotional offers
- Handles receipt validation

## Best Practices

1. Always initialize connection before any operations
2. Handle all error cases appropriately
3. Finish transactions promptly
4. Validate receipts on your server
5. Test thoroughly with sandbox/test accounts