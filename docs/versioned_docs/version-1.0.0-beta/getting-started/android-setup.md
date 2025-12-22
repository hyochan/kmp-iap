---
title: Android Setup
sidebar_label: Android Setup
sidebar_position: 3
---

import IapKitBanner from '@site/src/uis/IapKitBanner';

# Android Setup

<IapKitBanner />

For complete Android setup instructions including Google Play Console configuration, app setup, and testing guidelines, please visit:

👉 **[Android Setup Guide - openiap.dev](https://openiap.dev/docs/android-setup)**

The guide covers:
- Google Play Console configuration
- App bundle setup and signing
- Testing with internal testing tracks
- Common troubleshooting steps

## Code Implementation

### Basic Setup

```kotlin
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.*

class IAPManager {
    private val kmpIAP = KmpIAP()
    private val scope = CoroutineScope(Dispatchers.Main)
    
    fun initialize() {
        scope.launch {
            try {
                // Initialize connection
                kmpIAP.initConnection()
                println("Billing client connected")
                
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
    }
    
    private suspend fun loadProducts() {
        try {
            val products = kmpIAP.requestProducts(
                ProductRequest(
                    skus = listOf("premium_upgrade", "remove_ads"),
                    type = ProductType.INAPP
                )
            )
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
            kmpIAP.finishTransaction(
                purchase = purchase,
                isConsumable = isConsumableProduct(purchase.productId)
            )
        }
    }
    
    private fun handlePurchaseError(error: PurchaseError) {
        when (error.code) {
            ErrorCode.USER_CANCELLED -> {
                // User cancelled, no action needed
            }
            ErrorCode.PRODUCT_ALREADY_OWNED -> {
                // Item already owned, restore it
                restorePurchases()
            }
            else -> {
                // Show error to user
                showError(error.message)
            }
        }
    }
    
    suspend fun purchaseProduct(productId: String) {
        kmpIAP.requestPurchase(
            UnifiedPurchaseRequest(
                sku = productId,
                quantity = 1
            )
        )
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
            kmpIAP.endConnection()
        }
    }
}

// Simple user settings example
object UserSettings {
    var adsRemoved: Boolean = false
    var isPremium: Boolean = false
}
```

### Android-specific Features

```kotlin
// Use obfuscated account IDs for enhanced security
kmpIAP.requestPurchase(
    UnifiedPurchaseRequest(
        sku = "premium_upgrade",
        quantity = 1,
        obfuscatedAccountIdAndroid = "user_account_123",
        obfuscatedProfileIdAndroid = "profile_456"
    )
)

// Acknowledge a purchase (for non-consumables)
kmpIAP.acknowledgePurchaseAndroid(purchase.purchaseToken)

// Consume a purchase (for consumables)
kmpIAP.consumePurchaseAndroid(purchase.purchaseToken)

// Deep link to subscription management
kmpIAP.deepLinkToSubscriptions(
    DeepLinkOptions(sku = "subscription_id")
)
```

### Error Handling

```kotlin
scope.launch {
    kmpIAP.purchaseErrorListener.collect { error ->
        when (error.code) {
            ErrorCode.E_SERVICE_UNAVAILABLE.name -> {
                // Google Play services unavailable
                showDialog("Please update Google Play services")
            }
            ErrorCode.E_BILLING_UNAVAILABLE.name -> {
                // Billing API version not supported
                showDialog("In-app purchases not supported on this device")
            }
            ErrorCode.E_ITEM_UNAVAILABLE.name -> {
                // Product not found or not available for purchase
                showDialog("This item is currently unavailable")
            }
            ErrorCode.E_DEVELOPER_ERROR.name -> {
                // Invalid arguments provided to the API
                println("Developer error: Check product configuration")
            }
            else -> {
                showDialog("Purchase failed: ${error.message}")
            }
        }
    }
}
```

## Next Steps

- **[iOS Setup](/docs/getting-started/ios-setup)** - Configure for iOS platform
- **[Basic Implementation](/docs/guides/purchases)** - Start implementing purchases
- **[Troubleshooting](/docs/guides/troubleshooting)** - Common issues and solutions

---

For detailed platform configuration, product setup, and testing instructions, visit the [Android Setup Guide at openiap.dev](https://openiap.dev/docs/android-setup).