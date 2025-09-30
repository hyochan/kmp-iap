---
sidebar_position: 2
---

# Quick Start

Get up and running with KMP IAP in just a few minutes!

:::info Version
This guide covers **v1.0.0-rc** with simplified API. For v1.0.0-rc, see the [migration guide](/blog/2025/08/20/rc1-simplified-api).
:::

## Choose Your Approach

KMP IAP supports two usage patterns:

### Option 1: Global Instance (Simple)

Use the pre-created `kmpIapInstance` for convenience and simplicity.

### Option 2: Create Your Own Instance (Recommended for Testing)

Create your own `KmpIAP()` instances for better control, testing, or dependency injection.

## Basic Implementation

### Using Global Instance

Here's a complete example using the global instance:

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.*

class IAPManager {
    private val scope = CoroutineScope(Dispatchers.Main)

    suspend fun initialize() {
        try {
            // Initialize connection using global instance
            kmpIapInstance.initConnection()

            // Listen to purchase updates
            scope.launch {
                kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                    handlePurchaseUpdate(purchase)
                }
            }

            // Listen to errors
            scope.launch {
                kmpIapInstance.purchaseErrorListener.collect { error ->
                    handlePurchaseError(error)
                }
            }
        } catch (e: Exception) {
            println("Initialization failed: ${e.message}")
        }
    }

    suspend fun loadProducts() {
        try {
            // v1.0.0-rc - DSL API
            val products = kmpIapInstance.fetchProducts {
                skus = listOf("product_1", "product_2")
                type = ProductQueryType.InApp
            }
            println("Loaded ${products.size} products")
        } catch (e: Exception) {
            println("Failed to load products: ${e.message}")
        }
    }

    suspend fun purchaseProduct(productId: String) {
        try {
            // v1.0.0-rc - DSL API
            kmpIapInstance.requestPurchase {
                ios {
                    sku = productId
                    quantity = 1
                }
                android {
                    skus = listOf(productId)
                }
            }
            // Purchase will be handled via purchaseUpdatedListener
        } catch (e: Exception) {
            println("Purchase failed: ${e.message}")
        }
    }

    private suspend fun handlePurchaseUpdate(purchase: Purchase) {
        // Verify purchase with your backend
        val isValid = verifyPurchaseWithBackend(purchase)

        if (isValid) {
            // Grant the purchased content
            grantPurchase(purchase)

            // Finish the transaction
            kmpIapInstance.finishTransaction(
                purchase = purchase.toPurchaseInput(),
                isConsumable = isConsumableProduct(purchase.productId)
            )
        }
    }
}
```

### Using Your Own Instance

Here's the same example creating your own instance:

```kotlin
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.*

class IAPManager {
    private val scope = CoroutineScope(Dispatchers.Main)

    suspend fun initialize() {
        try {
            // Initialize connection using instance
            kmpIapInstance.initConnection()

            // Listen to purchase updates
            scope.launch {
                kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                    handlePurchaseUpdate(purchase)
                }
            }

            // Listen to errors
            scope.launch {
                kmpIapInstance.purchaseErrorListener.collect { error ->
                    handlePurchaseError(error)
                }
            }
        } catch (e: Exception) {
            println("Initialization failed: ${e.message}")
        }
    }

    suspend fun loadProducts() {
        try {
            // Load in-app products - v1.0.0-rc DSL API
            val products = kmpIapInstance.fetchProducts {
                skus = listOf("remove_ads", "premium_upgrade")
                type = ProductQueryType.InApp
            }

            products.forEach { product ->
                println("Product: ${product.id} - ${product.price}")
            }
        } catch (e: Exception) {
            println("Failed to load products: ${e.message}")
        }
    }

    suspend fun purchaseProduct(productId: String) {
        try {
            // Request purchase - v1.0.0-rc DSL API
            kmpIapInstance.requestPurchase {
                ios {
                    sku = productId
                    quantity = 1
                }
                android {
                    skus = listOf(productId)
                }
            }
            // Purchase will be handled in purchaseUpdatedListener
        } catch (e: Exception) {
            println("Purchase request failed: ${e.message}")
        }
    }

    private suspend fun handlePurchaseUpdate(purchase: Purchase) {
        // IMPORTANT: Server-side receipt validation should be performed here
        // val isValid = validateReceiptOnServer(purchase.transactionReceipt)

        // For this example, we'll assume validation passed
        val isValid = true

        if (isValid) {
            // Deliver the product
            deliverProduct(purchase.productId)

            // Finish the transaction
            kmpIapInstance.finishTransaction(
                purchase = purchase.toPurchaseInput(),
                isConsumable = true // true for consumables, false for subscriptions
            )
        }
    }

    private fun handlePurchaseError(error: PurchaseError) {
        when (error.code) {
            ErrorCode.UserCancelled -> {
                println("User cancelled the purchase")
            }
            ErrorCode.ProductNotAvailable -> {
                println("Item is not available")
            }
            else -> {
                println("Purchase failed: ${error.message}")
            }
        }
    }

    private fun deliverProduct(productId: String) {
        // Implement your product delivery logic
        println("Product delivered: $productId")
    }

    fun disconnect() {
        kmpIapInstance.endConnection()
        scope.cancel()
    }
}
```

## Using with Instance Creation

For cases where you need more control (like testing or dependency injection):

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.types.*

class IAPService {

    suspend fun initialize() {
        // Initialize connection
        kmpIapInstance.initConnection()

        // Set up listeners
        launch {
            kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                // Handle purchase
                kmpIapInstance.finishTransaction(purchase.toPurchaseInput(), isConsumable = true)
            }
        }
    }

    suspend fun purchaseItem(productId: String) {
        // v1.0.0-rc - DSL API
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
}
```

## Using with Compose Multiplatform

Here's how to use KMP IAP in a Compose Multiplatform app:

```kotlin
import androidx.compose.runtime.*
import androidx.compose.material3.*
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.types.*

@Composable
fun StoreScreen() {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Initialize connection
        kmpIapInstance.initConnection()

        // Load products
        isLoading = true
        try {
            // v1.0.0-rc - DSL API
            products = kmpIapInstance.fetchProducts {
                skus = listOf("product_1", "product_2")
                type = ProductQueryType.InApp
            }
        } finally {
            isLoading = false
        }

        // Listen for purchases
        launch {
            kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                // Handle successful purchase
                kmpIapInstance.finishTransaction(purchase.toPurchaseInput(), isConsumable = true)
            }
        }
    }

    Column {
        products.forEach { product ->
            Card(
                onClick = {
                    // Purchase product
                    scope.launch {
                        // v1.0.0-rc - DSL API
                        kmpIapInstance.requestPurchase {
                            ios {
                                sku = product.id
                                quantity = 1
                            }
                            android {
                                skus = listOf(product.id)
                            }
                        }
                    }
                }
            ) {
                Text("${product.title} - ${product.price}")
            }
        }
    }
}
```

## Platform-Specific Notes

### Android

- Make sure your app is signed with the release key when testing
- Upload your app to Google Play Console (at least to internal testing)
- Add test accounts in Google Play Console

### iOS

- Test with sandbox accounts during development
- Use StoreKit configuration files for local testing
- Remember to handle promotional offers if needed

## Next Steps

- Learn about [Basic Setup](../guides/basic-setup.md)
- Understand [Purchase Flow](../guides/purchases.md)
- Master [Purchase Lifecycle](../guides/lifecycle.md)
- Check [Troubleshooting](../guides/troubleshooting.md)
