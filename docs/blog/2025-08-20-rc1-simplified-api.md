---
title: v1.0.0-rc.1 - Simplified API Design
authors: [hyochan]
tags: [release, api, rc1]
---

# kmp-iap v1.0.0-rc.1 Released - Simplified API Design

We're excited to announce the release of **kmp-iap v1.0.0-rc.1**, which brings significant API improvements that make in-app purchases even easier to implement in your Kotlin Multiplatform projects.

## 🎯 Key Changes

### Simplified API Design

We've removed unnecessary wrapper classes to make the API more intuitive and reduce boilerplate code. The core methods now accept parameters directly instead of requiring wrapper objects.

<!-- truncate -->

## 📝 API Changes

### 1. requestProducts() - Direct Parameters

#### Before (v1.0.0-beta.14)

```kotlin
val products = kmpIapInstance.requestProducts(
    ProductRequest(
        skus = listOf("product_id"),
        type = ProductType.INAPP
    )
)
```

#### After (v1.0.0-rc.1)

```kotlin
val products = kmpIapInstance.requestProducts(
    skus = listOf("product_id"),
    type = ProductType.INAPP
)
```

### 2. requestPurchase() - Streamlined Parameters

#### Before (v1.0.0-beta.14)

```kotlin
val purchase = kmpIapInstance.requestPurchase(
    RequestPurchaseProps(
        ios = RequestPurchaseIosProps(
            sku = "product_id",
            quantity = 1
        ),
        android = RequestPurchaseAndroidProps(
            skus = listOf("product_id")
        )
    )
)
```

#### After (v1.0.0-rc.1)

```kotlin
// Simple usage - just the SKU
val purchase = kmpIapInstance.requestPurchase(sku = "product_id")

// With platform-specific options
val purchase = kmpIapInstance.requestPurchase(
    sku = "product_id",
    ios = RequestPurchaseIosProps(
        sku = "product_id",
        quantity = 1
    ),
    android = RequestPurchaseAndroidProps(
        skus = listOf("product_id")
    )
)
```

## 🚀 Migration Guide

### Update Your Gradle Dependencies

```kotlin
dependencies {
    implementation("io.github.hyochan:kmp-iap:1.0.0-rc.1")
}
```

### Code Migration

The migration is straightforward - simply remove the wrapper classes:

1. **For `requestProducts()`**: Remove `ProductRequest` wrapper
2. **For `requestPurchase()`**: Remove `RequestPurchaseProps` wrapper and pass parameters directly

### Complete Example

Here's a complete example showing the new simplified API:

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.*

class StoreViewModel {
    suspend fun loadProducts() {
        // Initialize connection
        val connected = kmpIapInstance.initConnection()
        if (!connected) return

        // Load products - no wrapper needed
        val products = kmpIapInstance.requestProducts(
            skus = listOf("premium", "coins_100", "coins_500"),
            type = ProductType.INAPP
        )

        // Load subscriptions
        val subscriptions = kmpIapInstance.requestProducts(
            skus = listOf("monthly_sub", "yearly_sub"),
            type = ProductType.SUBS
        )
    }

    suspend fun purchaseProduct(productId: String) {
        // Simple purchase - just pass the SKU
        val purchase = kmpIapInstance.requestPurchase(sku = productId)

        // Handle the purchase
        kmpIapInstance.finishTransaction(
            purchase = purchase,
            isConsumable = true
        )
    }

    suspend fun purchaseWithOptions(productId: String) {
        // Purchase with platform-specific options
        val purchase = kmpIapInstance.requestPurchase(
            sku = productId,
            ios = RequestPurchaseIosProps(
                sku = productId,
                quantity = 1,
                appAccountToken = "user_token"
            ),
            android = RequestPurchaseAndroidProps(
                skus = listOf(productId),
                obfuscatedAccountIdAndroid = "user_123"
            )
        )
    }
}
```

## ✨ Benefits

1. **Cleaner Code**: Less boilerplate, more readable code
2. **Easier to Use**: Direct parameter passing is more intuitive
3. **Better IDE Support**: Better auto-completion and parameter hints
4. **Backward Compatible**: Platform-specific options still available when needed

## 🔄 What's Next

This RC1 release brings us closer to the stable 1.0.0 release. We're focusing on:

- Final testing and bug fixes
- Documentation improvements
- Community feedback integration

## 📚 Resources

- [API Documentation](/docs/api/core-methods)
- [Getting Started Guide](/docs/getting-started/quickstart)
- [GitHub Repository](https://github.com/hyochan/kmp-iap)
- [OpenIAP Specification](https://www.openiap.dev)

## 🙏 Feedback

Please try out the new API and let us know your feedback! Report any issues on our [GitHub Issues](https://github.com/hyochan/kmp-iap/issues) page.

Happy coding! 🎉
