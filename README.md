# kmp-iap

<div align="center">
  <img src="https://github.com/hyochan/kmp-iap/blob/main/docs/static/img/logo.png" width="200" alt="kmp-iap logo" />
  
  <a href="https://central.sonatype.com/artifact/io.github.hyochan/kmp-iap"><img src="https://img.shields.io/maven-central/v/io.github.hyochan/kmp-iap.svg?style=flat-square" alt="Maven Central" /></a>
  <a href="https://github.com/hyochan/kmp-iap/actions/workflows/gradle.yml"><img src="https://github.com/hyochan/kmp-iap/actions/workflows/gradle.yml/badge.svg" alt="Java CI with Gradle" /></a>
  <a href="https://openiap.dev"><img src="https://img.shields.io/badge/OpenIAP-Compliant-green?style=flat-square" alt="OpenIAP Compliant" /></a>
  <a href="https://codecov.io/gh/hyochan/kmp-iap"><img src="https://codecov.io/gh/hyochan/kmp-iap/branch/main/graph/badge.svg?token=YOUR_TOKEN" alt="Coverage Status" /></a>
  <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License" />
  
  A comprehensive Kotlin Multiplatform library for in-app purchases on Android and iOS platforms that conforms to the <a href="https://openiap.dev">Open IAP specification</a>
  
  <a href="https://openiap.dev"><img src="https://github.com/hyodotdev/openiap/blob/main/logo.png" alt="Open IAP" height="40" /></a>
</div>

## 📚 Documentation

Visit the documentation site for installation guides, API reference, and examples:

### **[hyochan.github.io/kmp-iap](https://hyochan.github.io/kmp-iap)**

## Using with AI Assistants

kmp-iap provides AI-friendly documentation for Cursor, GitHub Copilot, Claude, and ChatGPT.

**[📖 AI Assistants Guide →](https://hyochan.github.io/kmp-iap/docs/guides/ai-assistants)**

Quick links:
- [llms.txt](https://hyochan.github.io/kmp-iap/llms.txt) - Quick reference (~300 lines)
- [llms-full.txt](https://hyochan.github.io/kmp-iap/llms-full.txt) - Full API reference (~1000 lines)

## 📦 Installation

```kotlin
dependencies {
    implementation("io.github.hyochan:kmp-iap:1.3.7")
}
```

## 🚀 Quick Start

### Option 1: Using Global Instance (Simple)

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.*

// Use the global singleton instance
kmpIapInstance.initConnection()

// Get products - DSL API in v1.0.0-rc.2
val products = kmpIapInstance.fetchProducts {
    skus = listOf("product_id")
    type = ProductQueryType.InApp
}

// Request purchase - DSL API with platform-specific options
val purchase = kmpIapInstance.requestPurchase {
    ios {
        sku = "product_id"
        quantity = 1
    }
    android {
        skus = listOf("product_id")
    }
}

// Or just for one platform
val iosPurchase = kmpIapInstance.requestPurchase {
    ios {
        sku = "product_id"
    }
}

// Finish transaction (after server-side validation)
kmpIapInstance.finishTransaction(
    purchase = purchase.toPurchaseInput(),
    isConsumable = true // true for consumables, false for subscriptions
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

// Get products - DSL API in v1.0.0-rc.2
val products = kmpIAP.fetchProducts {
    skus = listOf("product_id")
    type = ProductQueryType.InApp
}

// Request purchase - DSL API with platform-specific options
val purchase = kmpIAP.requestPurchase {
    ios {
        sku = "product_id"
        quantity = 1
    }
    android {
        skus = listOf("product_id")
    }
}

// Or just for one platform
val androidPurchase = kmpIAP.requestPurchase {
    android {
        skus = listOf("product_id")
    }
}

// Finish transaction (after server-side validation)
kmpIAP.finishTransaction(
    purchase = purchase.toPurchaseInput(),
    isConsumable = true // true for consumables, false for subscriptions
)
```

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.
