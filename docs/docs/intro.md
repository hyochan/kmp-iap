---
title: kmp-iap
sidebar_label: Introduction
sidebar_position: 1
---

import AdFitTopFixed from '@site/src/uis/AdFitTopFixed';

# 🛒 kmp-iap

<AdFitTopFixed />

A comprehensive Kotlin Multiplatform library for implementing in-app purchases on iOS and Android platforms.

<div style={{textAlign: 'center', margin: '2rem 0'}}>
  <img src="/img/logo.png" alt="kmp-iap Logo" style={{maxWidth: '100%', height: 'auto'}} />
</div>

## 🚀 What is kmp-iap?

This is an **In App Purchase** library for Kotlin Multiplatform. This project has been inspired by [flutter_inapp_purchase](https://github.com/hyochan/flutter_inapp_purchase) and [react-native-iap](https://github.com/hyochan/react-native-iap). We are trying to share same experience of **in-app-purchase** in **Kotlin Multiplatform** as in **Flutter** and **React Native**.

We will keep working on it as time goes by just like we did in **flutter_inapp_purchase** and **react-native-iap**.

## ✨ Key Features

- **Kotlin Multiplatform**: Share IAP logic across iOS and Android
- **StoreKit 2 Support**: Full StoreKit 2 support for iOS 15.0+ with automatic fallback
- **Billing Client v7**: Latest Android Billing Client features
- **Type-safe**: Complete type safety with Kotlin's strong typing
- **Coroutines Support**: Modern async/await pattern with Kotlin Coroutines
- **StateFlow Integration**: Reactive state management with StateFlow
- **Receipt Validation**: Built-in receipt validation for both platforms

## 🎯 What this library does

- **Product Management**: Fetch and manage consumable and non-consumable products
- **Purchase Flow**: Handle complete purchase workflows with proper error handling
- **Subscription Support**: Full subscription lifecycle management
- **Receipt Validation**: Validate purchases on both platforms
- **Store Communication**: Direct communication with App Store and Google Play
- **Error Recovery**: Comprehensive error handling and recovery mechanisms

## 🛠️ Platform Support

| Feature                  | iOS | Android |
| ------------------------ | --- | ------- |
| Products & Subscriptions | ✅  | ✅      |
| Purchase Flow            | ✅  | ✅      |
| Receipt Validation       | ✅  | ✅      |
| Subscription Management  | ✅  | ✅      |
| Promotional Offers       | ✅  | N/A     |
| StoreKit 2               | ✅  | N/A     |
| Billing Client v7        | N/A | ✅      |

## 🔄 Version Information

- **Current Version**: 1.0.0-beta.2
- **Kotlin Compatibility**: Kotlin 2.1.10+
- **iOS Requirements**: iOS 11.0+
- **Android Requirements**: API level 24+

## ⚡ Quick Start

Get started with kmp-iap in minutes:

```kotlin
// In your build.gradle.kts
dependencies {
    implementation("io.github.hyochan:kmp-iap:1.0.0-beta.2")
}
```

```kotlin
import io.github.hyochan.kmpiap.useIap.*

// Initialize
val iapHelper = UseIap(
    scope = CoroutineScope(Dispatchers.Main),
    options = UseIapOptions()
)

// Initialize connection
iapHelper.initConnection()

// Get products
iapHelper.getProducts(listOf("product_id"))

// Request purchase
iapHelper.requestPurchase(
    sku = "product_id",
    obfuscatedAccountIdAndroid = "user_id" // Optional
)
```

## 📚 What's Next?

<div className="grid grid-cols-1 md:grid-cols-2 gap-4 my-8">
  <div className="card">
    <div className="card-body">
      <h3>🏁 Getting Started</h3>
      <p>Learn how to install and configure kmp-iap in your project.</p>
      <a href="/docs/getting-started/installation" className="button button--primary">Get Started →</a>
    </div>
  </div>
  
  <div className="card">
    <div className="card-body">
      <h3>📖 Guides</h3>
      <p>Follow step-by-step guides for implementing purchases and subscriptions.</p>
      <a href="/docs/guides/purchases" className="button button--secondary">View Guides →</a>
    </div>
  </div>
  
  <div className="card">
    <div className="card-body">
      <h3>🔧 API Reference</h3>
      <p>Comprehensive API documentation with examples and type definitions.</p>
      <a href="/docs/api/" className="button button--secondary">API Docs →</a>
    </div>
  </div>
  
  <div className="card">
    <div className="card-body">
      <h3>💡 Examples</h3>
      <p>Real-world examples and implementation patterns.</p>
      <a href="/docs/examples/basic-store" className="button button--secondary">See Examples →</a>
    </div>
  </div>
</div>

## 🤝 Community & Support

This project is maintained by [hyochan](https://github.com/hyochan).

- **GitHub Issues**: [Report bugs and feature requests](https://github.com/hyochan/kmp-iap/issues)
- **Discussions**: [Join community discussions](https://github.com/hyochan/kmp-iap/discussions)
- **Contributing**: [Contribute to the project](https://github.com/hyochan/kmp-iap/blob/main/CONTRIBUTING.md)

---

Ready to implement in-app purchases in your Kotlin Multiplatform app? Let's [get started](/docs/getting-started/installation)! 🚀