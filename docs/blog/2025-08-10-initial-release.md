---
slug: initial-release
title: Initial Release - StoreKit 2 and Google Play Billing Library Support
authors: [hyochan]
tags: [release, StoreKit2, android, billing]
---

## kmp-iap Initial Release 🎉

We're excited to announce the first release of **kmp-iap**, a unified in-app purchase library for Kotlin Multiplatform!

## Key Features

### 🍎 iOS - Full StoreKit 2 Support

- Complete support for Apple's latest StoreKit 2 framework
- Enhanced purchase verification and transaction management
- Improved subscription status tracking
- Leverages modern Swift async/await patterns

### 🤖 Android - Google Play Billing Library 7.1.1 Support

Supporting Google Play Billing Library 7.1.1 with comprehensive features:

- **Product Details API**: Modern API for querying product information
- **Subscription offers**: Support for multiple subscription offers and pricing phases
- **Pending purchases**: Full support for pending transactions
- **Error handling**: Detailed error codes and debugging information
- **Purchase verification**: Secure purchase token verification

### 🎯 Unified API

- Same API for in-app purchases on both iOS and Android
- Reactive programming with Kotlin Coroutines and Flow
- Type-safe Kotlin native implementation

## Getting Started

```kotlin
// Initialize
KmpIAP.initConnection()

// Load products
val products = KmpIAP.requestProducts(
    ProductRequest(
        skus = listOf("product_id"),
        type = ProductType.INAPP
    )
)

// Request purchase
KmpIAP.requestPurchase(
    UnifiedPurchaseRequest(
        sku = "product_id",
        quantity = 1
    )
)

// Observe purchase state
KmpIAP.purchaseUpdatedListener.collect { purchase ->
    // Handle purchase
}
```

## Next Steps

We'll continue to improve the library and add new features. Your feedback and contributions are welcome!

GitHub: [https://github.com/hyochan/kmp-iap](https://github.com/hyochan/kmp-iap)
