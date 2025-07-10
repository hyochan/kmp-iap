# KMP IAP (Kotlin Multiplatform In-App Purchase)

A Kotlin Multiplatform library for handling in-app purchases across Android and iOS platforms, designed to match the APIs of [Flutter InApp Purchase](https://github.com/dooboolab/flutter_inapp_purchase) and [expo-iap](https://github.com/dooboolab/expo-iap) for consistency across platforms.

> ⚠️ **Work in Progress**: Core API design is complete, but native implementations are still in development.

## Status

This project provides a complete API structure matching Flutter/expo-iap with:
- ✅ Project setup with Gradle configuration
- ✅ Multi-platform source sets (Android, iOS)
- ✅ Publishing configuration for Maven Central
- ✅ CI/CD setup with GitHub Actions
- ✅ Full API interface design matching Flutter/expo-iap
- ✅ Type definitions for all platforms
- ✅ Event flow architecture using Kotlin Coroutines
- ✅ Android implementation (Google Play Billing Library v8)
- ✅ useIap Hook for easy state management
- 🚧 iOS implementation (StoreKit)
- ❌ Receipt validation implementation
- ❌ Complete test coverage
- ❌ Example app implementation

## Supported Platforms

- Android (API 24+) - Google Play Billing Library v8
- iOS (iOS 13.0+) - StoreKit 1 & 2

## Project Structure

```
kmp-iap/
├── library/
│   └── src/
│       ├── commonMain/         # Shared code
│       ├── androidMain/        # Android-specific
│       ├── iosMain/           # iOS-specific
│       ├── jvmMain/           # Desktop JVM
│       ├── wasmJsMain/        # Web WASM
│       └── linuxX64Main/      # Linux native
├── example/                    # Example Compose Multiplatform app
│   └── src/
│       ├── commonMain/
│       ├── androidMain/
│       ├── iosMain/
│       └── jvmMain/
├── gradle/
│   └── libs.versions.toml     # Version catalog
├── build.gradle.kts
└── settings.gradle.kts
```

## Installation

```kotlin
// In your shared module's build.gradle.kts
dependencies {
    implementation("io.github.hyochan:kmp-iap:1.0.0-alpha02")
}
```

## Usage

### Basic Setup

```kotlin
import io.github.hyochan.kmpiap.*
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.launch

// Create IAP instance
val iap = createInAppPurchase()

// Initialize connection
lifecycleScope.launch {
    try {
        iap.initConnection()
        
        // Listen to purchase updates
        iap.purchaseUpdatedFlow.collect { purchase ->
            // Handle purchase update
            println("Purchase updated: ${purchase.productId}")
        }
    } catch (e: PurchaseError) {
        println("Error: ${e.message}")
    }
}
```

### Using the UseIap Helper

```kotlin
import io.github.hyochan.kmpiap.useIap.*

class YourViewModel : ViewModel() {
    // Create with options (100% compatible with expo-iap)
    private val iapHelper = useIap(
        scope = viewModelScope,
        options = UseIapOptions(
            onPurchaseSuccess = { purchase ->
                // Handle successful purchase
                println("Purchase successful: ${purchase.productId}")
            },
            onPurchaseError = { error ->
                // Handle purchase error
                println("Purchase error: ${error.message}")
            },
            autoFinishTransactions = true // Auto-finish transactions
        )
    )
    
    // State flows (matches expo-iap)
    val products = iapHelper.products
    val subscriptions = iapHelper.subscriptions
    val availablePurchases = iapHelper.availablePurchases
    val purchaseHistories = iapHelper.purchaseHistories // Note: plural form
    val currentPurchase = iapHelper.currentPurchase
    val currentError = iapHelper.currentError
    val connected = iapHelper.connected // Also available as isConnected
    val promotedProductIOS = iapHelper.promotedProductIOS
    
    init {
        viewModelScope.launch {
            // Initialize connection
            iapHelper.initConnection()
            
            // Get products
            iapHelper.getProducts(listOf("product_id_1", "product_id_2"))
            
            // Get subscriptions
            iapHelper.getSubscriptions(listOf("sub_id_1", "sub_id_2"))
            
            // Get available purchases and history
            iapHelper.getAvailablePurchases()
            iapHelper.getPurchaseHistories()
        }
    }
    
    fun purchaseProduct(productId: String) {
        viewModelScope.launch {
            iapHelper.requestPurchase(
                sku = productId,
                obfuscatedAccountIdAndroid = "user_id" // Optional
            )
        }
    }
    
    fun purchaseSubscription(subId: String) {
        viewModelScope.launch {
            iapHelper.requestSubscription(
                sku = subId,
                obfuscatedAccountIdAndroid = "user_id" // Optional
            )
        }
    }
    
    fun validatePurchase(productId: String) {
        viewModelScope.launch {
            // Unified receipt validation (matches expo-iap)
            val validationResult = iapHelper.validateReceipt(
                productId = productId,
                params = when (iapHelper.getStore()) {
                    Store.APP_STORE -> mapOf("isTest" to true)
                    Store.PLAY_STORE -> mapOf(
                        "packageName" to "com.example.app",
                        "accessToken" to "your-access-token"
                    )
                    else -> null
                }
            )
        }
    }
    
    fun buyPromotedProduct() {
        viewModelScope.launch {
            // iOS only: Buy promoted product
            iapHelper.buyPromotedProductIOS()
        }
    }
    
    fun finishPurchase(purchase: Purchase) {
        viewModelScope.launch {
            // Manual finish (only needed if autoFinishTransactions = false)
            val success = iapHelper.finishTransaction(
                purchase = purchase,
                isConsumable = true // For consumable products
            )
        }
    }
    
    override fun onCleared() {
        iapHelper.dispose()
        super.onCleared()
    }
}
```

### Android-specific Setup

```kotlin
// In your Android Application or Activity
val androidIap = iap as? AndroidInAppPurchaseImpl
androidIap?.initialize(context = this, activity = this)
```

### Request Products

```kotlin
// Get products
val products = iap.requestProducts(
    RequestProductsParams(
        skus = listOf("product_1", "product_2"),
        type = PurchaseType.INAPP
    )
)

// Get subscriptions  
val subscriptions = iap.requestProducts(
    RequestProductsParams(
        skus = listOf("subscription_1", "subscription_2"),
        type = PurchaseType.SUBS
    )
)

// Products will be returned as List<BaseProduct>
// Cast to Product or Subscription as needed
products.forEach { product ->
    when (product) {
        is Product -> println("Product: ${product.productId} - ${product.localizedPrice}")
        is Subscription -> println("Subscription: ${product.productId} - ${product.localizedPrice}")
    }
}
```

### Make a Purchase

```kotlin
// Platform-specific purchase request
val request = when (iap.getStore()) {
    Store.PLAY_STORE -> RequestPurchaseAndroid(
        skus = listOf("product_id"),
        obfuscatedAccountIdAndroid = "user_id",
        obfuscatedProfileIdAndroid = "profile_id" // Optional
    )
    Store.APP_STORE -> RequestPurchaseIOS(
        sku = "product_id",
        appAccountToken = "user_token",
        quantity = 1 // Optional
    )
    else -> RequestPurchaseGeneric(sku = "product_id")
}

// Request purchase
iap.requestPurchase(
    request = request,
    type = PurchaseType.INAPP
)

// For subscriptions with offers (Android)
val subsRequest = RequestSubscriptionAndroid(
    skus = listOf("subscription_id"),
    subscriptionOffers = listOf(
        SubscriptionOfferAndroid(
            sku = "subscription_id",
            offerToken = "offer_token_from_product_details"
        )
    )
)
```

### Handle Purchase Updates

```kotlin
// Listen to purchase updates
iap.purchaseUpdatedFlow.collect { purchase ->
    // Verify purchase
    if (purchase.platform == IAPPlatform.IOS) {
        // iOS validation
        val result = iap.validateReceiptIos(
            receiptBody = mapOf("receipt-data" to purchase.transactionReceipt!!),
            isTest = true
        )
    }
    
    // Finish transaction
    val success = iap.finishTransaction(purchase, isConsumable = true)
}

// Listen to errors
iap.purchaseErrorFlow.collect { error ->
    when (error.code) {
        ErrorCode.E_USER_CANCELLED -> println("User cancelled")
        ErrorCode.E_ITEM_UNAVAILABLE -> println("Item unavailable")
        else -> println("Error: ${error.message}")
    }
}
```

## Getting Started

See [docs/SETUP.md](docs/SETUP.md) for detailed setup instructions.

### Quick Start

```bash
# Automated setup (recommended)
./setup.sh

# Manual setup
cp local.properties.template local.properties
# Edit local.properties with your values

# Build library
./gradlew :library:build

# Run example app
./gradlew :example:run  # Desktop
./gradlew :example:installDebug  # Android
```

### VS Code Integration

The project includes VS Code launch configurations for common tasks:
- Build/test library
- Run example applications
- Publish to Maven repositories
- Generate documentation
- Code formatting

Open the project in VS Code and use **Run and Debug** panel.

## Example App

The project includes a Compose Multiplatform example app demonstrating library usage:
- Android, iOS, Desktop, and Web support
- Basic UI showcasing IAP integration (to be implemented)
- Located in the `example/` directory

## Documentation

- [Setup Guide](docs/SETUP.md) - Development environment setup
- [Release Guide](docs/RELEASE.md) - Publishing to Maven Central
- [GPG Configuration](gpg-key-spec.md) - GPG key setup for signing

## Contributing

Contributions are welcome! Feel free to implement the IAP functionality or improve the project structure.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Follow the setup guide in [docs/SETUP.md](docs/SETUP.md)
4. Implement your changes
5. Add tests if applicable
6. Run `./gradlew spotlessApply` to format code
7. Commit your changes (`git commit -m 'Add amazing feature'`)
8. Push to the branch (`git push origin feature/amazing-feature`)
9. Open a Pull Request

## License

MIT License