---
sidebar_position: 3
title: Complete Implementation
---

import IapKitBanner from '@site/src/uis/IapKitBanner';

# Complete Implementation

<IapKitBanner />

:::tip
The complete working example can be found at [example/composeApp](https://github.com/hyochan/kmp-iap/tree/main/example/composeApp).
:::

## Flow Overview

```txt
Connect → Fetch Products → Request Purchase → Validate → Finish Transaction
```

## Example Screens

| Screen | Description | Source |
|--------|-------------|--------|
| Purchase Flow | One-time purchases | [PurchaseFlowScreen.kt](https://github.com/hyochan/kmp-iap/blob/main/example/composeApp/src/commonMain/kotlin/dev/hyo/martie/screens/PurchaseFlowScreen.kt) |
| Subscription Flow | Recurring purchases | [SubscriptionFlowScreen.kt](https://github.com/hyochan/kmp-iap/blob/main/example/composeApp/src/commonMain/kotlin/dev/hyo/martie/screens/SubscriptionFlowScreen.kt) |

## Key Patterns

### ViewModel with StateFlow

```kotlin
class StoreViewModel : ViewModel() {
    private val _state = MutableStateFlow(StoreState())
    val state: StateFlow<StoreState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            kmpIapInstance.initConnection()
            kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                handlePurchase(purchase)
            }
        }
    }
}
```

### Product Configuration

```kotlin
object ProductConfig {
    private val products = mapOf(
        "coins_100" to ProductType.CONSUMABLE,
        "remove_ads" to ProductType.NON_CONSUMABLE,
        "premium_monthly" to ProductType.SUBSCRIPTION
    )

    fun isConsumable(productId: String) =
        products[productId] == ProductType.CONSUMABLE
}
```

### Error Recovery

```kotlin
when (error.code) {
    ErrorCode.NetworkError -> retry(delay = 2000)
    ErrorCode.AlreadyOwned -> restorePurchases()
    else -> showError(error.message)
}
```

## Security Best Practices

1. **Server-side validation** - Always validate receipts on your backend
2. **Secure storage** - Use platform-specific secure storage for sensitive data
3. **HTTPS only** - All network requests must use TLS
4. **Code obfuscation** - Enable R8/ProGuard for release builds

## Resources

- [Example App Source](https://github.com/hyochan/kmp-iap/tree/main/example)
- [Purchase Flow](./purchase-flow.md) - One-time purchases
- [Subscription Flow](./subscription-flow.md) - Recurring payments
