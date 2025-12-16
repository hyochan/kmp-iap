---
title: Android Setup
sidebar_label: Android Setup
sidebar_position: 3
---

import GreatFrontEndBanner from '@site/src/uis/GreatFrontEndBanner';

# Android Setup

<GreatFrontEndBanner />

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
            android {
                skus = listOf(productId)
            }
        }
    }
}
```

:::tip Cross-Platform Note
This example shows Android-specific usage with `skus`. For cross-platform compatibility, include both `ios` and `android` blocks in your request. See the [Core Methods](/docs/api/core-methods) documentation for details.
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

1. **Check App Signing:**
   - Ensure your app is signed with the same key uploaded to Play Console
   - Debug builds must use the same signing key for testing

2. **Verify Product Configuration:**
   - Product IDs match exactly between code and Play Console
   - Products are in "Active" state
   - App must be published (at least to internal testing)

3. **Wait for Propagation:**
   - New products may take 2-3 hours to become available
   - App must be uploaded to at least internal testing track

### Billing Unavailable

**Problem:** "Billing unavailable" or "Service unavailable" errors

**Solution:**
- Test on a real device (not emulator)
- Ensure Google Play Store is installed and up-to-date
- Check that the Google account is added to license testers in Play Console

### Purchase Not Acknowledged

**Problem:** Purchases are refunded after 3 days

**Solution:**
- Always call `finishTransaction()` after successful purchase
- For non-consumables, ensure acknowledgment is completed
- For consumables, ensure consumption is completed

## Best Practices

- Always verify purchases server-side for production apps
- Handle all error cases gracefully
- Test with license testers in Play Console
- Provide restore functionality for non-consumable products

## Next Steps

- **[iOS Setup](/docs/getting-started/ios-setup)** - Configure for iOS platform
- **[Basic Implementation](/docs/guides/purchases)** - Start implementing purchases
- **[Troubleshooting](/docs/guides/troubleshooting)** - Common issues and solutions

---

For detailed platform configuration, product setup, and testing instructions, visit the [Android Setup Guide at openiap.dev](https://openiap.dev/docs/android-setup).
