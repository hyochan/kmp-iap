---
title: Purchase Flow
sidebar_label: Purchase Flow
sidebar_position: 1
---

import IapKitBanner from '@site/src/uis/IapKitBanner';
import IapKitLink from '@site/src/uis/IapKitLink';

# Purchase Flow

<IapKitBanner />

## Key Concepts

- Always call `initConnection()` before any IAP operations
- Listen to `purchaseUpdatedListener` and `purchaseErrorListener` for purchase results
- Always call `finishTransaction()` after processing a purchase
- Verify purchases server-side in production

## Initialize

```kotlin
kmpIapInstance.initConnection()
```

## Fetch Products

```kotlin
val products = kmpIapInstance.fetchProducts {
    skus = listOf("coins_100", "premium_upgrade")
    type = ProductQueryType.InApp
}
```

## Listen for Purchases

```kotlin
// Success
kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
    // 1. Verify on server
    // 2. Deliver content
    // 3. Finish transaction
    kmpIapInstance.finishTransaction(
        purchase = purchase.toPurchaseInput(),
        isConsumable = true
    )
}

// Errors
kmpIapInstance.purchaseErrorListener.collect { error ->
    when (error.code) {
        ErrorCode.UserCancelled -> { /* Silent */ }
        ErrorCode.AlreadyOwned -> { /* Suggest restore */ }
        else -> { /* Show error */ }
    }
}
```

## Request Purchase

```kotlin
kmpIapInstance.requestPurchase {
    ios { sku = "coins_100" }
    android { skus = listOf("coins_100") }
}
```

## Restore Purchases

```kotlin
val purchases = kmpIapInstance.getAvailablePurchases()
purchases.forEach { purchase ->
    // Re-deliver non-consumables
}
```

## Cleanup

```kotlin
kmpIapInstance.endConnection()
```

## IAPKit Server Verification

For production apps, always verify purchases server-side. <IapKitLink>IAPKit</IapKitLink> provides a simple unified API for both iOS and Android verification.

### Setup

1. Get your API key from <IapKitLink>iapkit.com</IapKitLink>
2. Configure environment variables:

**Android** (`.env` or `local.properties`):
```properties
IAPKIT_API_KEY=your_api_key_here
```

**iOS** (`Secrets.xcconfig`):
```properties
IAPKIT_API_KEY = your_api_key_here
```

### How It Works

```kotlin
// In your purchase listener
kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
    val purchaseToken = purchase.purchaseToken
    if (purchaseToken.isNullOrEmpty()) {
        showError("No purchase token available")
        return@collect
    }

    try {
        val result = kmpIapInstance.verifyPurchaseWithProvider(
            VerifyPurchaseWithProviderProps(
                provider = PurchaseVerificationProvider.Iapkit,
                iapkit = RequestVerifyPurchaseWithIapkitProps(
                    apiKey = AppConfig.iapkitApiKey,
                    apple = RequestVerifyPurchaseWithIapkitAppleProps(jws = purchaseToken),
                    google = RequestVerifyPurchaseWithIapkitGoogleProps(purchaseToken = purchaseToken)
                )
            )
        )

        if (result.iapkit?.isValid == true) {
            // Grant entitlement to user
            deliverProduct(purchase.productId)

            // Finish the transaction
            kmpIapInstance.finishTransaction(
                purchase = purchase.toPurchaseInput(),
                isConsumable = isConsumable(purchase.productId)
            )
        } else {
            showError("Purchase verification failed: ${result.iapkit?.state}")
        }
    } catch (e: Exception) {
        showError("Verification error: ${e.message}")
    }
}
```

### Verification Response

```json
{
  "isValid": true,
  "state": "ENTITLED",
  "store": "APPLE"
}
```

```kotlin
// Access in code
val isValid = result.iapkit?.isValid     // true
val state = result.iapkit?.state         // IapkitPurchaseState.Entitled
val store = result.iapkit?.store         // IapStore.Apple or IapStore.Google
```

### Verification Methods

| Method | Use Case | Security |
|--------|----------|----------|
| **None** | Development only | None |
| **Local** | Basic validation | Low |
| **IAPKit** | Production apps | High |

### Benefits of IAPKit

- Unified API for iOS and Android
- Real-time fraud detection
- Subscription status tracking
- Sandbox/Production environment detection
- No backend infrastructure needed

For more details, visit the <IapKitLink href="https://iapkit.com/docs">IAPKit Documentation</IapKitLink>.

## IAPKit Purchase States

| State | Description |
|-------|-------------|
| `Entitled` | Valid access |
| `Expired` | Subscription expired |
| `Canceled` | Purchase canceled |
| `Consumed` | Consumable used |
| `Inauthentic` | Fraudulent |

## Next Steps

- [Subscription Flow](./subscription-flow.md) - Recurring payments
- [Error Codes](../api/error-codes.md) - Handle all error types