---
sidebar_position: 10
title: FAQ
---


import GreatFrontEndBanner from '@site/src/uis/GreatFrontEndBanner';

# Frequently Asked Questions

<GreatFrontEndBanner />

Common questions and answers about kmp-iap.

## General Questions

### Q: What platforms does kmp-iap support?

**A:** The library supports:

- **iOS**: 15.0+ with StoreKit 2
- **Android**: API 21+ (Android 5.0) with Google Play Billing Library v7+
- **Desktop & Web**: Planned for future releases

Windows, Linux, and Web support is on the roadmap as Kotlin Multiplatform expands to these platforms.

### Q: How is kmp-iap different from flutter_inapp_purchase?

**A:** Key differences:

- **Technology**: kmp-iap uses Kotlin Multiplatform vs Flutter/Dart
- **State Management**: StateFlow vs Streams
- **Integration**: Works with Compose Multiplatform, native Android/iOS
- **API Design**: Coroutines-based vs Future/async-await

### Q: Is this library free to use?

**A:** Yes, kmp-iap is open source and free to use under the MIT license. However, both Apple and Google charge fees for in-app purchases (typically 15-30%).

## Setup & Configuration

### Q: Do I need to configure anything in Xcode or Android Studio?

**A:** Yes, minimal setup is required:

**iOS:**

- Enable In-App Purchase capability in Xcode
- Configure products in App Store Connect

**Android:**

- Add `<uses-permission android:name="com.android.vending.BILLING" />` to AndroidManifest.xml
- Configure products in Google Play Console

See our [setup guides](../getting-started/ios-setup.md) for detailed instructions.

### Q: Can I test purchases without publishing my app?

**A:** Yes:

**iOS:** Use sandbox testing with sandbox Apple IDs
**Android:** Upload to Internal Testing track in Play Console

Both platforms require proper store setup but don't require public app release.

### Q: How long does it take for products to appear after configuration?

**A:** Product availability varies:

- **iOS:** Usually within a few hours, up to 24 hours
- **Android:** Can take 24-48 hours after app upload

Products must be properly configured and approved in the respective stores.

## Products & Subscriptions

### Q: What's the difference between consumable and non-consumable products?

**A:**

- **Consumable**: Can be purchased multiple times (coins, gems, power-ups)
- **Non-consumable**: Purchased once, owned forever (remove ads, premium features)
- **Subscriptions**: Recurring purchases with auto-renewal

### Q: How do I handle different product types?

**A:** Use the `isConsumable` parameter:

```kotlin
// For consumable products
val success = kmpIapInstance.finishTransaction(
    purchase = purchase,
    isConsumable = true
)

// For non-consumable products and subscriptions
val success = kmpIapInstance.finishTransaction(
    purchase = purchase,
    isConsumable = false
)
```

### Q: Can I offer subscription trials?

**A:** Yes, but setup varies by platform:

- **iOS:** Configure introductory offers in App Store Connect
- **Android:** Configure free trials in Play Console

The library will return trial information in the product data.

### Q: How do I handle subscription renewals?

**A:** Subscriptions auto-renew by default. To check status:

```kotlin
scope.launch {
    val purchases = kmpIapInstance.getAvailablePurchases()
        val activeSubscriptions = purchases.filter { purchase ->
            subscriptionIds.contains(purchase.productId) &&
            isActive(purchase)
        }
    }
}
```

Implement server-side receipt validation for accurate expiration checking.

## Purchases & Transactions

### Q: Why isn't my purchase completing?

**A:** Common causes:

1. **Not finishing transactions:** Always call `finishTransaction()`
2. **No state observers:** Set up StateFlow collectors before requesting purchases
3. **Network issues:** Ensure device has internet connectivity
4. **Account issues:** Verify store account is properly set up

### Q: How do I restore purchases?

**A:** Purchases are automatically available via StateFlow:

```kotlin
suspend fun restorePurchases() {
    try {
        // Get available purchases
        val purchases = kmpIapInstance.getAvailablePurchases()
        purchases.forEach { purchase ->
            // Re-deliver non-consumable products
            if (isNonConsumable(purchase.productId)) {
                deliverProduct(purchase)
            }
        }
    } catch (e: PurchaseError) {
        println("Restore failed: $e")
    }
}
```

### Q: Can users purchase the same product multiple times?

**A:** Depends on product type:

- **Consumable:** Yes, after finishing transaction as consumable
- **Non-consumable:** No, will get `PRODUCT_ALREADY_OWNED` error
- **Subscription:** Can upgrade/downgrade, but not duplicate

### Q: How do I handle pending purchases on Android?

**A:** Monitor purchase state:

```kotlin
private fun handlePurchase(purchase: Purchase) {
    when (purchase.purchaseState) {
        PurchaseState.PURCHASED -> {
            // Purchase completed
            deliverProduct(purchase)
        }
        PurchaseState.PENDING -> {
            // Purchase pending - show pending UI
            showPendingMessage()
        }
    }
}
```

## Security & Validation

### Q: Do I need to validate receipts?

**A:** Yes, for production apps you should always validate receipts server-side to prevent fraud. The library provides receipt data, but validation must be implemented separately.

### Q: How do I validate iOS receipts?

**A:** Send receipt to Apple's verification servers:

```kotlin
// Get receipt data
val receipt = purchase.transactionReceipt

// Send to your server for validation with Apple
val isValid = api.validateIOSReceipt(
    receipt = receipt,
    sharedSecret = "your-shared-secret"
)
```

### Q: How do I validate Android purchases?

**A:** Use the purchase token with Google Play Developer API:

```kotlin
// Get purchase token
val token = purchase.purchaseToken

// Validate on your server using Google Play Developer API
val isValid = api.validateAndroidPurchase(
    token = token,
    productId = purchase.productId
)
```

### Q: Can I trust client-side purchase data?

**A:** No, never trust client-side data for security-critical operations. Always validate receipts server-side before granting paid content.

## Error Handling

### Q: What does "Billing is unavailable" mean?

**A:** This indicates the billing system isn't ready. Common causes:

- Google Play Store not installed/updated (Android)
- App not uploaded to store (Android)
- Network connectivity issues
- Store service temporarily unavailable

### Q: Why do I get "Product not found" errors?

**A:** Product ID mismatches are common:

- Verify exact product ID spelling
- Check product is active in store console
- Wait for product propagation (up to 24 hours)
- Ensure app bundle ID matches store configuration

### Q: How do I handle purchase cancellations?

**A:** Monitor error state:

```kotlin
scope.launch {
    kmpIapInstance.purchaseErrorListener.collect { error ->
        when (error.code) {
            ErrorCode.E_USER_CANCELLED.name -> {
                // User cancelled - no error message needed
            }
            else -> {
                // Show error message
                showErrorDialog(error.message)
            }
        }
        // No need to clear error with new API
    }
}
```

## Development & Testing

### Q: Can I test purchases on simulators/emulators?

**A:**

- **iOS Simulator:** Limited support, use StoreKit testing
- **Android Emulator:** Not recommended, use real devices
- **Best practice:** Always test on real devices with test accounts

### Q: How do I test subscriptions?

**A:** Both platforms offer accelerated testing:

- **iOS:** Subscriptions renew every few minutes in sandbox
- **Android:** Test subscriptions renew quickly in test environment

### Q: Do test purchases cost real money?

**A:** No:

- **iOS:** Sandbox purchases are free
- **Android:** License tester purchases are free and auto-refund

### Q: How do I clear test purchase history?

**A:**

- **iOS:** Settings > App Store > Sandbox Account > Reset
- **Android:** Google Play Store > Account > Purchase history (cancel test purchases)

## Performance & Best Practices

### Q: When should I initialize the IAP connection?

**A:** Initialize as early as possible, typically in your ViewModel initialization. Don't initialize on every screen.

### Q: How do I handle app lifecycle events?

**A:** Clean up properly:

```kotlin
class PurchaseViewModel : ViewModel() {
    private val kmpIAP = KmpIAP()

    init {
        viewModelScope.launch {
            kmpIAP.initConnection()
        }
    }

    override fun onCleared() {
        super.onCleared()
        kmpIAP.dispose()
    }
}
```

## Kotlin Multiplatform Specific

### Q: Can I use kmp-iap with SwiftUI?

**A:** Yes, the library works with SwiftUI through Kotlin/Native interop:

```swift
// In your SwiftUI view
let iapManager = IAPManager() // Your KMP wrapper

Button("Purchase") {
    iapManager.purchaseProduct(productId: "premium")
}
```

### Q: Does it work with Compose Multiplatform?

**A:** Yes, kmp-iap is designed to work seamlessly with Compose Multiplatform:

```kotlin
@Composable
fun PurchaseButton(productId: String) {
    val scope = rememberCoroutineScope()

    Button(onClick = {
        scope.launch {
            kmpIapInstance.requestPurchase(
                UnifiedPurchaseRequest(
                    sku = productId,
                    quantity = 1
                )
            )
        }
    }) {
        Text("Purchase")
    }
}
```

### Q: How do I share IAP logic between platforms?

**A:** Create a common interface:

```kotlin
// In commonMain
expect class IAPManager {
    suspend fun purchaseProduct(productId: String)
    fun observePurchases(): Flow<Purchase?>
}

// Platform-specific implementations
actual class IAPManager {
    // iOS/Android specific implementation
}
```

## Troubleshooting

### Q: My app was rejected for IAP issues. What should I check?

**A:** Common rejection reasons:

1. **Missing restore functionality:** Always provide restore purchases option
2. **Incorrect product types:** Ensure consumable/non-consumable types match usage
3. **Price display:** Show localized prices from store data
4. **Terms compliance:** Follow platform guidelines for IAP UI

### Q: Why are my products not loading in production but work in testing?

**A:** Check:

1. **App review status:** App must be approved and live
2. **Product review status:** Products must be approved
3. **Regional availability:** Products might not be available in all regions
4. **Time delay:** Products can take 24+ hours to propagate globally

### Q: How do I debug IAP issues?

**A:** Enable debug logging:

```kotlin
// Add logging to your IAP operations
scope.launch {
    kmpIapInstance.purchaseErrorListener.collect { error ->
        println("[IAP Debug] Error: ${error.code} - ${error.message}")
    }
}
```

Check console output for detailed error information.

## Support & Community

### Q: Where can I get help?

**A:** Multiple support channels:

- [GitHub Issues](https://github.com/hyochan/kmp-iap/issues) for bugs
- [GitHub Discussions](https://github.com/hyochan/kmp-iap/discussions) for questions
- [Discord Community](https://discord.gg/hyo) for real-time chat

### Q: How do I report bugs?

**A:** Create detailed GitHub issues with:

- Platform and version information
- Steps to reproduce
- Expected vs actual behavior
- Relevant code snippets
- Console logs with debug mode enabled

### Q: Can I contribute to the project?

**A:** Yes! Contributions are welcome:

- Report bugs and issues
- Submit pull requests for fixes
- Improve documentation
- Help answer community questions

See the [Contributing Guide](https://github.com/hyochan/kmp-iap/blob/main/CONTRIBUTING.md) for details.

---

**Still have questions?** Check our [Troubleshooting Guide](./troubleshooting.md) or [open a discussion](https://github.com/hyochan/kmp-iap/discussions) on GitHub.
