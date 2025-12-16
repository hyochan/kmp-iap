---
sidebar_position: 1
---

import GreatFrontEndBanner from '@site/src/uis/GreatFrontEndBanner';

# Basic Setup

<GreatFrontEndBanner />

This guide walks you through the complete setup process for KMP IAP in your Kotlin Multiplatform project.

## Project Configuration

### 1. Add Dependencies

In your shared module's `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.github.hyochan:kmp-iap:<version>")
            }
        }
    }
}
```

### 2. Platform Configuration

#### Android Configuration

In your Android app module's `build.gradle.kts`:

```kotlin
android {
    defaultConfig {
        // Ensure minimum SDK is 24 or higher
        minSdk = 24
    }
}
```

Add to your `AndroidManifest.xml`:

```xml
<uses-permission android:name="com.android.vending.BILLING" />
```

#### iOS Configuration

1. In Xcode, add the In-App Purchase capability:
   - Select your app target
   - Go to "Signing & Capabilities"
   - Click "+" and add "In-App Purchase"

2. Configure your products in App Store Connect

## Implementation

### 1. Create IAP Manager

```kotlin
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.types.*

```kotlin
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

object IAPManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // State management
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<Product>>(emptyList())
    val subscriptions: StateFlow<List<Product>> = _subscriptions.asStateFlow()

    fun initialize() {
        scope.launch {
            try {
                kmpIapInstance.initConnection()
                _isConnected.value = true

                // Set up purchase listeners
                setupPurchaseListeners()

                // Load products
                loadProducts()
            } catch (e: Exception) {
                println("IAP initialization failed: ${e.message}")
                _isConnected.value = false
            }
        }
    }
    
    private fun setupPurchaseListeners() {
        scope.launch {
            // Listen for purchase updates
            kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                handlePurchaseUpdate(purchase)
            }
        }

        scope.launch {
            // Listen for purchase errors
            kmpIapInstance.purchaseErrorListener.collect { error ->
                handlePurchaseError(error)
            }
        }
    }
    
    private suspend fun loadProducts() {
        try {
            // Load in-app products
            val productList = kmpIapInstance.fetchProducts {
                skus = listOf("remove_ads", "premium_features")
                type = ProductQueryType.InApp
            }
            _products.value = productList

            // Load subscriptions
            val subsList = kmpIapInstance.fetchProducts {
                skus = listOf("monthly_sub", "yearly_sub")
                type = ProductQueryType.Subscription
            }
            _subscriptions.value = subsList
        } catch (e: Exception) {
            println("Failed to load products: ${e.message}")
        }
    }
    
    suspend fun purchaseProduct(productId: String) {
        kmpIapInstance.requestPurchase {
            ios {
                sku = productId
                quantity = 1
            }
            android {
                skus = listOf(productId)
            }
        }
    }

    suspend fun purchaseSubscription(productId: String) {
        kmpIapInstance.requestPurchase {
            ios {
                sku = productId
                quantity = 1
            }
            android {
                skus = listOf(productId)
            }
        }
    }
    
    private suspend fun handlePurchaseUpdate(purchase: Purchase) {
        // Purchase is valid if returned from purchaseUpdatedListener flow
        // Process the purchase
        // Verify purchase with your backend
        val isValid = verifyPurchaseWithBackend(purchase)
        
        if (isValid) {
            // Grant entitlement
            grantEntitlement(purchase.productId)

            // Finish transaction
            kmpIapInstance.finishTransaction(
                purchase.toPurchaseInput(),
                isConsumable = isConsumableProduct(purchase.productId)
            )
        }
    }
    
    private fun handlePurchaseError(error: PurchaseError) {
        when (error.code) {
            ErrorCode.UserCancelled -> {
                // User cancelled, no action needed
            }
            ErrorCode.AlreadyOwned -> {
                // Item already owned, restore it
                restorePurchases()
            }
            else -> {
                // Show error to user
                showError(error.message)
            }
        }
    }
    
    suspend fun restorePurchases() {
        val purchases = kmpIapInstance.getAvailablePurchases()
        purchases.forEach { purchase ->
            grantEntitlement(purchase.productId)
        }
    }
    
    private suspend fun verifyPurchaseWithBackend(purchase: Purchase): Boolean {
        // Implement your backend verification
        // This is a simplified example
        return true
    }
    
    private fun grantEntitlement(productId: String) {
        // Grant the appropriate entitlement based on productId
        when (productId) {
            "remove_ads" -> UserSettings.adsRemoved = true
            "premium_features" -> UserSettings.isPremium = true
            // Handle other products
        }
    }
    
    private fun isConsumableProduct(productId: String): Boolean {
        // Define which products are consumable
        return when (productId) {
            "coins_pack_100", "coins_pack_500" -> true
            else -> false
        }
    }
    
    private fun notifyUserOfPendingPurchase() {
        // Notify user that purchase is pending
    }
    
    private fun showError(message: String) {
        // Show error message to user
    }
    
    fun cleanup() {
        scope.launch {
            kmpIapInstance.endConnection()
        }.invokeOnCompletion {
            scope.cancel()
        }
    }

    // Alternative: suspend function for use in lifecycle-aware contexts
    suspend fun cleanupAsync() {
        kmpIapInstance.endConnection()
        scope.cancel()
    }
}

// Simple user settings example
object UserSettings {
    var adsRemoved: Boolean = false
    var isPremium: Boolean = false
}
```

### 2. Initialize in Your App

#### Android

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize IAP
        IAPManager.initialize()
        
        setContent {
            MyApp()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        IAPManager.cleanup()
    }
}
```

#### iOS

```swift
import UIKit
import shared

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        // Initialize IAP
        IAPManager.shared.initialize()
        return true
    }
}
```

### 3. Use in Your UI

```kotlin
@Composable
fun StoreScreen() {
    val products by IAPManager.products.collectAsState()
    val subscriptions by IAPManager.subscriptions.collectAsState()
    val scope = rememberCoroutineScope()

    LazyColumn {
        items(products) { product ->
            ProductCard(
                product = product,
                onPurchase = {
                    scope.launch {
                        IAPManager.purchaseProduct(product.productId)
                    }
                }
            )
        }

        items(subscriptions) { subscription ->
            SubscriptionCard(
                subscription = subscription,
                onPurchase = {
                    scope.launch {
                        IAPManager.purchaseSubscription(subscription.productId)
                    }
                }
            )
        }
    }
}

@Composable
fun ProductCard(product: Product, onPurchase: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.h6
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.body2
                )
            }
            Button(onClick = onPurchase) {
                Text(product.localizedPrice)
            }
        }
    }
}
```

## Testing

### Android Testing

1. Upload your app to Google Play Console (at least Internal Testing)
2. Add test accounts in Google Play Console
3. Test with a signed APK

### iOS Testing

1. Create sandbox test accounts in App Store Connect
2. Use StoreKit Configuration file for local testing
3. Test on a real device for best results

## Common Issues

### Connection Issues
- Ensure you're calling `initConnection()` before any other operations
- Check network connectivity
- Verify store configuration

### Product Not Found
- Verify product IDs match exactly with store configuration
- Ensure products are active in store console
- Wait for products to propagate (can take up to 24 hours)

### Purchase Failures
- Check if user is signed in to store account
- Verify payment methods are set up
- Ensure app is properly signed for release

## Next Steps

- Learn about [Making Purchases](./purchases.md)
- Understand [Purchase Lifecycle](./lifecycle.md)
- Check [Troubleshooting](./troubleshooting.md)