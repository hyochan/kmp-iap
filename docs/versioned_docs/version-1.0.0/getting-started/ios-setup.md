---
title: iOS Setup
sidebar_label: iOS Setup
sidebar_position: 2
---

import GreatFrontEndBanner from '@site/src/uis/GreatFrontEndBanner';

# iOS Setup

<GreatFrontEndBanner />

For complete iOS setup instructions including App Store Connect configuration, Xcode setup, and testing guidelines, please visit:

👉 **[iOS Setup Guide - openiap.dev](https://openiap.dev/docs/ios-setup)**

The guide covers:
- App Store Connect configuration
- Xcode project setup
- Sandbox testing
- Common troubleshooting steps

## Code Implementation

### Basic Setup

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.*
import kotlinx.coroutines.*

class IAPManager {
    private val scope = CoroutineScope(Dispatchers.Main)

    fun initialize() {
        scope.launch {
            try {
                // Initialize connection
                kmpIapInstance.initConnection()
                println("StoreKit connected")

                // Set up purchase listeners
                setupPurchaseListeners()

                // Load products
                loadProducts()
            } catch (e: Exception) {
                println("Failed to initialize: ${e.message}")
            }
        }
    }
    
    private fun setupPurchaseListeners() {
        scope.launch {
            // Listen for purchase updates
            kmpIAP.purchaseUpdatedListener.collect { purchase ->
                handlePurchaseUpdate(purchase)
            }
        }
        
        scope.launch {
            // Listen for purchase errors
            kmpIAP.purchaseErrorListener.collect { error ->
                handlePurchaseError(error)
            }
        }
        
        scope.launch {
            // Listen for promoted products (iOS only)
            kmpIAP.promotedProductListener.collect { productId ->
                productId?.let {
                    // Handle promoted product
                    handlePromotedProduct(it)
                }
            }
        }
    }
    
    private suspend fun loadProducts() {
        try {
            val products = kmpIAP.fetchProducts {
                skus = listOf("premium_upgrade", "remove_ads")
                type = ProductQueryType.InApp
            }
            println("Found ${products.size} products")
        } catch (e: Exception) {
            println("Failed to load products: ${e.message}")
        }
    }
    
    private suspend fun handlePurchaseUpdate(purchase: Purchase) {
        // Verify purchase with your backend
        val isValid = verifyPurchaseWithBackend(purchase)
        
        if (isValid) {
            // Grant entitlement
            grantEntitlement(purchase.productId)
            
            // Finish transaction
            kmpIapInstance.finishTransaction(
                purchase.toPurchaseInput(),
                isConsumable = isConsumableProduct(purchase.productId)
            )
        }
    }
    
    private fun handlePurchaseError(error: PurchaseError) {
        when (error.code) {
            ErrorCode.UserCancelled -> {
                // User cancelled, no action needed
            }
            ErrorCode.AlreadyOwned -> {
                // Item already owned, restore it
                restorePurchases()
            }
            else -> {
                // Show error to user
                showError(error.message)
            }
        }
    }
    
    private suspend fun handlePromotedProduct(productId: String) {
        // Handle App Store promoted product
        println("Promoted product: $productId")
        // Optionally purchase the promoted product
        kmpIAP.buyPromotedProductIOS()
    }
    
    suspend fun purchaseProduct(productId: String) {
        kmpIAP.requestPurchase {
            ios {
                sku = productId
                quantity = 1
            }
            android {
                skus = listOf(productId)
            }
        }
    }
    
    suspend fun restorePurchases() {
        val purchases = kmpIAP.getAvailablePurchases()
        purchases.forEach { purchase ->
            grantEntitlement(purchase.productId)
        }
    }
    
    private suspend fun verifyPurchaseWithBackend(purchase: Purchase): Boolean {
        // TODO: Implement your backend verification
        return true
    }
    
    private fun grantEntitlement(productId: String) {
        // Grant the appropriate entitlement based on productId
        when (productId) {
            "remove_ads" -> UserSettings.adsRemoved = true
            "premium_upgrade" -> UserSettings.isPremium = true
            // Handle other products
        }
    }
    
    private fun isConsumableProduct(productId: String): Boolean {
        return when (productId) {
            "coins_100", "coins_500" -> true
            else -> false
        }
    }
    
    private fun showError(message: String) {
        // Show error message to user
    }
    
    fun cleanup() {
        scope.cancel()
        runBlocking {
            kmpIapInstance.endConnection()
        }
    }
}

// Simple user settings example
object UserSettings {
    var adsRemoved: Boolean = false
    var isPremium: Boolean = false
}
```

### iOS-specific Features

```kotlin
// Get current storefront (iOS only)
val storefront = kmpIAP.getStorefrontIOS()
println("Current storefront: $storefront") // e.g., "US", "GB", "JP"

// Present code redemption sheet (iOS only)
kmpIAP.presentCodeRedemptionSheetIOS()

// Handle promoted products
scope.launch {
    kmpIAP.promotedProductListener.collect { productId ->
        productId?.let {
            println("Promoted product: $it")
            // Purchase the promoted product
            kmpIAP.buyPromotedProductIOS()
        }
    }
}

// Clear pending transactions (iOS only)
kmpIAP.clearTransactionIOS()

// Clear products cache (iOS only)
kmpIAP.clearProductsIOS()

// Finish specific transaction by ID
kmpIAP.finishTransactionIOS(transactionId)
```

### StoreKit 2 Support

The library automatically uses StoreKit 2 on iOS 15.0+ with fallback to StoreKit 1:

```kotlin
// StoreKit 2 features are used automatically when available
// The same API works for both StoreKit 1 and 2

// Validate receipt (uses StoreKit 2 verification when available)
val validationResult = kmpIAP.validateReceipt(
    ValidationOptions(
        receiptData = purchase.transactionReceipt,
        isTest = false
    )
)

// Check if purchase is valid
val isValid = kmpIAP.isPurchaseValid(purchase)
```

### Subscription Offers

```kotlin
// Handle subscription with promotional offers
kmpIAP.requestPurchase(
    UnifiedPurchaseRequest(
        sku = "monthly_subscription",
        quantity = 1,
        promotionalOffer = PromotionalOffer(
            identifier = "offer_id",
            keyIdentifier = "key_id",
            nonce = "nonce_value",
            signature = "signature",
            timestamp = System.currentTimeMillis()
        )
    )
)
```

### Error Handling

```kotlin
scope.launch {
    kmpIAP.purchaseErrorListener.collect { error ->
        when (error.code) {
            ErrorCode.NetworkError -> {
                // Network error
                showDialog("Please check your internet connection")
            }
            ErrorCode.UserCancelled -> {
                // Payment cancelled by user
                println("Purchase cancelled")
            }
            ErrorCode.PaymentInvalid -> {
                // Invalid payment
                showDialog("Payment could not be processed")
            }
            ErrorCode.IapNotAvailable -> {
                // Permission denied
                showDialog("In-app purchases are not allowed")
            }
            else -> {
                showDialog("Purchase failed: ${error.message}")
            }
        }
    }
}
```

## Next Steps

- **[Android Setup](/docs/getting-started/android-setup)** - Configure for Android platform
- **[Basic Implementation](/docs/guides/purchases)** - Start implementing purchases
- **[Troubleshooting](/docs/guides/troubleshooting)** - Common issues and solutions

---

For detailed platform configuration, product setup, and testing instructions, visit the [iOS Setup Guide at openiap.dev](https://openiap.dev/docs/ios-setup).