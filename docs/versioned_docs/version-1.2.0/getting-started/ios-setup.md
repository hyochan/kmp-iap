---
title: iOS Setup
sidebar_label: iOS Setup
sidebar_position: 2
---

import IapKitBanner from '@site/src/uis/IapKitBanner';

# iOS Setup

<IapKitBanner />

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
import io.github.hyochan.kmpiap.fetchProducts
import io.github.hyochan.kmpiap.requestPurchase
import io.github.hyochan.kmpiap.openiap.*
import kotlinx.coroutines.*

val productIds = listOf(
    "com.yourapp.premium",
    "com.yourapp.coins_100",
    "com.yourapp.subscription_monthly"
)

class IAPManager {
    private val scope = CoroutineScope(Dispatchers.Main)

    fun initialize() {
        scope.launch {
            // Initialize connection
            val connected = kmpIapInstance.initConnection()

            if (connected) {
                // Fetch products
                val products = kmpIapInstance.fetchProducts {
                    skus = productIds
                    type = ProductQueryType.InApp
                }

                // Display products
                products.forEach { product ->
                    println("${product.title} - ${product.displayPrice}")
                }
            }
        }

        // Listen for purchase updates
        scope.launch {
            kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                println("Purchase successful: ${purchase.productId}")
                // For production apps, verify purchases server-side.
                // See verifyPurchaseWithProvider: /blog/release-1.0.0
            }
        }

        // Listen for errors
        scope.launch {
            kmpIapInstance.purchaseErrorListener.collect { error ->
                println("Purchase failed: ${error.message}")
            }
        }
    }

    suspend fun purchase(productId: String) {
        kmpIapInstance.requestPurchase {
            ios {
                sku = productId
            }
        }
    }
}
```

:::tip Cross-Platform Note
This example shows iOS-specific usage with `sku`. For cross-platform compatibility, include both `ios` and `android` blocks in your request. See the [Core Methods](/docs/api/core-methods) documentation for details.
:::

:::info Full Examples
For complete implementation examples including purchase flow, subscription handling, and verification, see the example app:
- [PurchaseFlowScreen.kt](https://github.com/hyochan/kmp-iap/blob/main/example/composeApp/src/commonMain/kotlin/dev/hyo/martie/screens/PurchaseFlowScreen.kt)
- [SubscriptionFlowScreen.kt](https://github.com/hyochan/kmp-iap/blob/main/example/composeApp/src/commonMain/kotlin/dev/hyo/martie/screens/SubscriptionFlowScreen.kt)
:::

## Common Issues

### Product IDs Not Found

**Problem:** Products return empty or undefined

**Solutions:**

1. **Check Prerequisites (Most common cause):**
   - Verify ALL agreements are signed in App Store Connect > Business
   - Ensure ALL banking, legal, and tax information is completed AND approved by Apple
   - These are the most commonly overlooked requirements

2. **Verify Product Configuration:**
   - Product IDs match exactly between code and App Store Connect
   - Products are in "Ready to Submit" or "Approved" state
   - Bundle identifier matches

3. **Use Proper Sandbox Testing:**
   - Sign in via Settings > Developer > Sandbox Apple Account
   - NOT through the App Store app

### Sandbox Testing Issues

**Problem:** "Cannot connect to iTunes Store" error

**Solution:**
- Use a dedicated sandbox test user
- Sign out of regular App Store account
- Verify internet connection
- Try on a real device (simulator may have issues)

### Purchase Verification Failures

**Problem:** Purchase verification returns invalid

**Solution:**
- Check if app is properly signed
- Verify receipt data is not corrupted
- Ensure proper error handling for network issues

## Best Practices

- Always verify purchases server-side for production apps
- Handle all error cases gracefully
- Test thoroughly with sandbox users
- Provide restore functionality for non-consumable products

## Next Steps

- **[Android Setup](/docs/getting-started/android-setup)** - Configure for Android platform
- **[Basic Implementation](/docs/guides/purchases)** - Start implementing purchases
- **[Troubleshooting](/docs/guides/troubleshooting)** - Common issues and solutions

---

For detailed platform configuration, product setup, and testing instructions, visit the [iOS Setup Guide at openiap.dev](https://openiap.dev/docs/ios-setup).
