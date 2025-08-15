# kmp-iap

<p align="center">
  <img src="https://github.com/hyochan/kmp-iap/blob/main/docs/static/img/logo.png" width="200" alt="kmp-iap logo" />
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/io.github.hyochan/kmp-iap"><img src="https://img.shields.io/maven-central/v/io.github.hyochan/kmp-iap.svg?style=flat-square" alt="Maven Central" /></a>
  <a href="https://github.com/hyochan/kmp-iap/actions/workflows/gradle.yml"><img src="https://github.com/hyochan/kmp-iap/actions/workflows/gradle.yml/badge.svg" alt="Java CI with Gradle" /></a>
  <a href="https://codecov.io/gh/hyochan/kmp-iap"><img src="https://codecov.io/gh/hyochan/kmp-iap/branch/main/graph/badge.svg?token=YOUR_TOKEN" alt="Coverage Status" /></a>
  <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License" />
</p>

<p align="center">
  A comprehensive Kotlin Multiplatform library for in-app purchases on Android and iOS platforms.
</p>

<p align="center">
  Implementing the <a href="https://openiap.dev"><strong>Open IAP Specification</strong></a> for consistent cross-platform in-app purchase handling.
</p>

## 📚 Documentation

Visit the documentation site for installation guides, API reference, and examples:

### **[kmp-iap.hyo.dev](https://kmp-iap.hyo.dev)**

## 📦 Installation

```kotlin
dependencies {
    implementation("io.github.hyochan:kmp-iap:1.0.0-beta.9")
}
```

## 🚀 Quick Start

### Option 1: Using Global Instance (Simple)

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.types.*

// Use the global singleton instance
kmpIapInstance.initConnection()

// Get products
val products = kmpIapInstance.requestProducts(
    ProductRequest(
        skus = listOf("product_id"),
        type = ProductType.INAPP
    )
)

// Request purchase
val purchase = kmpIapInstance.requestPurchase(
    UnifiedPurchaseRequest(
        sku = "product_id",
        quantity = 1
    )
)

// Finish transaction (after server-side validation)
kmpIapInstance.finishTransaction(
    purchase = purchase,
    isConsumable = true // true for consumables, false for subscriptions
)
```

### Option 2: Create Your Own Instance (Recommended for Testing)

```kotlin
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.types.*

// Create your own instance
val kmpIAP = KmpIAP()

// Initialize connection
kmpIAP.initConnection()

// Get products
val products = kmpIAP.requestProducts(
    ProductRequest(
        skus = listOf("product_id"),
        type = ProductType.INAPP
    )
)

// Request purchase
val purchase = kmpIAP.requestPurchase(
    UnifiedPurchaseRequest(
        sku = "product_id",
        quantity = 1
    )
)

// Finish transaction (after server-side validation)
kmpIAP.finishTransaction(
    purchase = purchase,
    isConsumable = true // true for consumables, false for subscriptions
)
```

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.
