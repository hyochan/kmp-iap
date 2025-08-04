# kmp-iap

<p align="center">
  <img src="https://private-user-images.githubusercontent.com/27461460/473827950-93810638-a5f2-48f7-9ed7-863efc569b02.png?jwt=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3NTQyNjY1MzIsIm5iZiI6MTc1NDI2NjIzMiwicGF0aCI6Ii8yNzQ2MTQ2MC80NzM4Mjc5NTAtOTM4MTA2MzgtYTVmMi00OGY3LTllZDctODYzZWZjNTY5YjAyLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNTA4MDQlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjUwODA0VDAwMTAzMlomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPTBjYzc5ZTYxZWQyNTVlY2QzNGY3YTE2YjUyZDg5MTQyZjllYzM4MzU4MTdmYmUxNWE2ZWRmODgxNjc3MGU0NzAmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0In0.HA94pmSJaYLR6FU7XMBSzyMRGzlwfNVc4Sj0wU74lZc" width="200" alt="kmp-iap logo" />
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
    implementation("io.github.hyochan:kmp-iap:1.0.0-beta.2")
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