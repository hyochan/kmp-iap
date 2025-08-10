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

## 📚 Documentation

Visit the documentation site for installation guides, API reference, and examples:

### **[kmp-iap.hyo.dev](https://kmp-iap.hyo.dev)**

## 📦 Installation

```kotlin
dependencies {
    implementation("io.github.hyochan:kmp-iap:1.0.0-beta.3")
}
```

## 🚀 Quick Start

```kotlin
import io.github.hyochan.kmpiap.*

// Initialize connection
val iap = createInAppPurchase()
iap.initConnection()

// Get products
val products = iap.requestProducts(
    RequestProductsParams(
        skus = listOf("product_id"),
        type = PurchaseType.INAPP
    )
)

// Request purchase
iap.requestPurchase(
    request = RequestPurchaseAndroid(skus = listOf("product_id")),
    type = PurchaseType.INAPP
)
```

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.
