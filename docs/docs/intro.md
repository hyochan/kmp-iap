---
title: kmp-iap
sidebar_label: Introduction
sidebar_position: 1
---

import IapKitBanner from '@site/src/uis/IapKitBanner';
import Logo from '@site/static/img/logo.png';

# 🛒 kmp-iap

<IapKitBanner />

:::info Version
This documentation is for **kmp-iap v1.3.0** which includes Google Play Billing 8.3.0 features and External Payments support. Native code is powered by [openiap-google](https://github.com/hyodotdev/openiap-google) and [openiap-apple](https://github.com/hyodotdev/openiap-apple) modules.
:::

A comprehensive Kotlin Multiplatform library for in-app purchases on iOS and Android that **conforms to the [Open IAP specification](https://openiap.dev)**.

<div style={{textAlign: 'center', margin: '2rem 0'}}>
  <img src={Logo} alt="kmp-iap Logo" style={{maxWidth: '200px', height: 'auto'}} />
</div>

## 🚀 What is kmp-iap?

This is an **In App Purchase** library for Kotlin Multiplatform. This project has been inspired by [flutter_inapp_purchase](https://github.com/hyochan/flutter_inapp_purchase) and [react-native-iap](https://github.com/hyochan/react-native-iap). We are trying to share same experience of **in-app-purchase** in **Kotlin Multiplatform** as in **Flutter** and **React Native**.

We will keep working on it as time goes by just like we did in **flutter_inapp_purchase** and **react-native-iap**.

## ✨ Key Features

- **Kotlin Multiplatform**: Share IAP logic across iOS and Android
- **StoreKit 2 Support**: Full StoreKit 2 support for iOS 15.0+ with automatic fallback
- **Billing Client v8**: Latest Android Billing Client features
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
| Billing Client v8        | N/A | ✅      |

## 🔄 Version Information

- **Current Version**: 1.3.0
- **Kotlin Compatibility**: Kotlin 2.1.10+
- **iOS Requirements**: iOS 15.0+ (via [openiap-apple](https://github.com/hyodotdev/openiap-apple))
- **Android Requirements**: API level 24+ (via [openiap-google](https://github.com/hyodotdev/openiap-google))

## ⚡ Quick Start

Get started with kmp-iap in minutes:

```kotlin
// In your build.gradle.kts
dependencies {
    implementation("io.github.hyochan:kmp-iap:1.3.3")
}
```

### Option 1: Using Global Instance (Simple)

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.*

// Initialize connection
kmpIapInstance.initConnection()

// Get products using DSL
val products = kmpIapInstance.fetchProducts {
    skus = listOf("product_id")
    type = ProductQueryType.InApp
}

// Request purchase using DSL
val purchase = kmpIapInstance.requestPurchase {
    apple {
        sku = "product_id"
        quantity = 1
    }
    android {
        skus = listOf("product_id")
    }
}

// Finish transaction after validation
kmpIapInstance.finishTransaction(
    purchase = purchase.toPurchaseInput(),
    isConsumable = true
)
```

### Option 2: Create Your Own Instance (Recommended for Testing)

```kotlin
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.*

// Create your own instance
val kmpIAP = KmpIAP()

// Initialize connection
kmpIAP.initConnection()

// Get products using DSL
val products = kmpIAP.fetchProducts {
    skus = listOf("product_id")
    type = ProductQueryType.InApp
}

// Request purchase using DSL
val purchase = kmpIAP.requestPurchase {
    apple {
        sku = "product_id"
        quantity = 1
    }
    android {
        skus = listOf("product_id")
    }
}

// Finish transaction after validation
kmpIAP.finishTransaction(
    purchase = purchase.toPurchaseInput(),
    isConsumable = true
)
```

## 🚀 Start Building Today

<div className="next-steps-grid-intro">
  <a href="getting-started/installation" className="next-step-card-intro gradient-purple">
    <div className="next-step-icon-intro">🚀</div>
    <h3 className="next-step-title-intro">Quick Start</h3>
    <p className="next-step-desc-intro">Get up and running with kmp-iap in minutes</p>
    <span className="next-step-arrow-intro">→</span>
  </a>

  <a href="guides/purchases" className="next-step-card-intro gradient-pink">
    <div className="next-step-icon-intro">📚</div>
    <h3 className="next-step-title-intro">Guides</h3>
    <p className="next-step-desc-intro">Step-by-step tutorials and best practices</p>
    <span className="next-step-arrow-intro">→</span>
  </a>

  <a href="api" className="next-step-card-intro gradient-blue">
    <div className="next-step-icon-intro">⚡</div>
    <h3 className="next-step-title-intro">API Reference</h3>
    <p className="next-step-desc-intro">Complete API documentation with examples</p>
    <span className="next-step-arrow-intro">→</span>
  </a>

  <a href="examples/purchase-flow" className="next-step-card-intro gradient-green">
    <div className="next-step-icon-intro">💻</div>
    <h3 className="next-step-title-intro">Examples</h3>
    <p className="next-step-desc-intro">Production-ready code samples</p>
    <span className="next-step-arrow-intro">→</span>
  </a>
</div>

<style>{`
  .next-steps-grid-intro {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1.5rem;
    margin: 2rem 0;
  }
  
  .next-step-card-intro {
    display: block;
    padding: 2rem;
    border-radius: 12px;
    color: white;
    text-decoration: none;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
    position: relative;
    overflow: hidden;
  }
  
  /* Light mode gradients */
  .gradient-purple {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  }
  
  .gradient-pink {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    box-shadow: 0 4px 12px rgba(240, 147, 251, 0.3);
  }
  
  .gradient-blue {
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
    box-shadow: 0 4px 12px rgba(79, 172, 254, 0.3);
  }
  
  .gradient-green {
    background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
    box-shadow: 0 4px 12px rgba(67, 233, 123, 0.3);
  }
  
  /* Dark mode - much darker backgrounds with subtle gradients */
  [data-theme='dark'] .gradient-purple {
    background: linear-gradient(135deg, #4a3d7a 0%, #5a4685 100%);
    box-shadow: 0 4px 12px rgba(74, 61, 122, 0.3);
    border: 1px solid rgba(139, 159, 232, 0.3);
  }
  
  [data-theme='dark'] .gradient-pink {
    background: linear-gradient(135deg, #7a3d6b 0%, #85465f 100%);
    box-shadow: 0 4px 12px rgba(122, 61, 107, 0.3);
    border: 1px solid rgba(244, 176, 250, 0.3);
  }
  
  [data-theme='dark'] .gradient-blue {
    background: linear-gradient(135deg, #3d5a7a 0%, #466685 100%);
    box-shadow: 0 4px 12px rgba(61, 90, 122, 0.3);
    border: 1px solid rgba(127, 195, 254, 0.3);
  }
  
  [data-theme='dark'] .gradient-green {
    background: linear-gradient(135deg, #3d7a5a 0%, #468566 100%);
    box-shadow: 0 4px 12px rgba(61, 122, 90, 0.3);
    border: 1px solid rgba(111, 238, 159, 0.3);
  }
  
  .next-step-card-intro:hover {
    transform: translateY(-8px);
    text-decoration: none;
  }
  
  .next-step-card-intro:hover {
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.2) !important;
  }
  
  [data-theme='dark'] .next-step-card-intro:hover {
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.6) !important;
    border: 1px solid rgba(255, 255, 255, 0.2) !important;
  }
  
  .next-step-icon-intro {
    font-size: 3rem;
    margin-bottom: 1rem;
    filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
  }
  
  .next-step-title-intro {
    font-size: 1.5rem;
    font-weight: 700;
    margin-bottom: 0.5rem;
    color: white !important;
  }
  
  /* Dark mode - high contrast white text */
  [data-theme='dark'] .next-step-title-intro {
    color: #ffffff !important;
    text-shadow: 0 2px 4px rgba(0,0,0,0.8);
    font-weight: 800;
  }
  
  .next-step-desc-intro {
    font-size: 1rem;
    opacity: 0.95;
    margin-bottom: 1rem;
    color: white !important;
    line-height: 1.5;
  }
  
  /* Dark mode - high contrast white text for description */
  [data-theme='dark'] .next-step-desc-intro {
    color: #f0f0f0 !important;
    opacity: 1;
    text-shadow: 0 1px 3px rgba(0,0,0,0.6);
    font-weight: 500;
  }
  
  .next-step-arrow-intro {
    font-size: 1.25rem;
    font-weight: 700;
    color: white !important;
  }
  
  /* Dark mode - high contrast white arrow */
  [data-theme='dark'] .next-step-arrow-intro {
    color: #ffffff !important;
    text-shadow: 0 2px 4px rgba(0,0,0,0.8);
    font-weight: 800;
  }
  
  /* Force white text on all child elements in dark mode */
  [data-theme='dark'] .next-step-card-intro * {
    color: white !important;
  }
  
  /* Ensure links don't change color on hover */
  .next-step-card-intro:hover .next-step-title-intro,
  .next-step-card-intro:hover .next-step-desc-intro,
  .next-step-card-intro:hover .next-step-arrow-intro {
    color: white !important;
  }
`}</style>

## 🤝 Community & Support

This project is maintained by [hyochan](https://github.com/hyochan).

- **GitHub Issues**: [Report bugs and feature requests](https://github.com/hyochan/kmp-iap/issues)
- **Discussions**: [Join community discussions](https://github.com/hyochan/kmp-iap/discussions)
- **Contributing**: [Contribute to the project](https://github.com/hyochan/kmp-iap/blob/main/CONTRIBUTING.md)

---

Ready to implement in-app purchases in your Kotlin Multiplatform app? Let's [get started](getting-started/installation)! 🚀
