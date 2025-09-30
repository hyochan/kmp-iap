---
title: API Reference
sidebar_position: 1
---

import AdFitTopFixed from '@site/src/uis/AdFitTopFixed';

# API Reference

<AdFitTopFixed />

Complete reference for kmp-iap - A unified Kotlin Multiplatform API for implementing in-app purchases across iOS and Android platforms.

## Available APIs

### 🏪 Core Methods
Essential methods for initializing connections, loading products, and processing purchases.

- **Connection Management**: `kmpIAP.initConnection()`, `kmpIAP.dispose()`
- **Product Loading**: `kmpIAP.requestProducts()`, `kmpIAP.requestSubscriptions()`
- **Purchase Processing**: `kmpIAP.requestPurchase()`, `kmpIAP.requestSubscription()`
- **Transaction Management**: `kmpIAP.finishTransaction()`, `kmpIAP.acknowledgePurchase()`

### 📱 Platform-Specific Methods
Access iOS and Android specific features and capabilities.

- **iOS Features**: Offer code redemption, subscription management, StoreKit 2 support
- **Android Features**: Billing client state, pending purchases, deep links

### 🎧 State Management
Real-time StateFlow for monitoring purchase events and connection states.

- **Purchase States**: Success, errors, and state changes via StateFlow
- **Connection States**: Store connection status updates
- **Product States**: Available products and subscriptions

### 🔧 Types & Enums
Comprehensive type definitions for type-safe development.

- **Data Classes**: Platform-specific purchase and product models
- **Response Models**: Products, purchases, and transaction data
- **Error Handling**: Detailed error codes and messages

## Quick Start

```kotlin
import io.github.hyochan.kmpiap.*
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class PurchaseManager {
    private val kmpIAP = KmpIAP()
    private val scope = CoroutineScope(Dispatchers.Main)
    
    suspend fun initializePurchases() {
        // Initialize connection
        kmpIAP.initConnection()
        
        // Set up state listeners
        scope.launch {
            kmpIAP.purchaseUpdatedListener.collect { purchase ->
                handlePurchaseSuccess(purchase)
            }
        }
        
        scope.launch {
            kmpIAP.purchaseErrorListener.collect { error ->
                handlePurchaseError(error)
            }
        }
    }
    
    suspend fun makePurchase(productId: String) {
        kmpIAP.requestPurchase(
            UnifiedPurchaseRequest(
                sku = productId,
                quantity = 1,
                obfuscatedAccountIdAndroid = "user_id" // Optional
            )
        )
    }
}
```

## Kotlin Multiplatform Support

kmp-iap provides full type safety with Kotlin's strong typing and coroutines support for all methods, parameters, and return values.

## Platform Compatibility

- **iOS**: 15.0+ with StoreKit 2
- **Android**: API level 24+ with Google Play Billing Client v8

## Need Help?

- Check our [Troubleshooting Guide](../guides/troubleshooting.md)
- Review Flutter to KMP migration tips in our [FAQ](../guides/faq.md)
- Browse [example implementations](https://github.com/hyochan/kmp-iap/tree/main/example)
- Join the [Kotlin Community](https://kotlinlang.org/community/) for support