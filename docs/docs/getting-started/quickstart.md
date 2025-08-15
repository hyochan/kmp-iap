---
sidebar_position: 2
---

# Quick Start

Get up and running with KMP IAP in just a few minutes!

## Choose Your Approach

KMP IAP supports two usage patterns:

### Option 1: Singleton Pattern (Recommended)
Use `KmpIAP.instance` for a global singleton instance that's shared across your app.

### Option 2: Instance Creation
Create your own `KmpIAP()` instances for more control, testing, or dependency injection.

## Basic Implementation with Singleton

Here's a complete example using the singleton pattern:

```kotlin
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.*

class IAPManager {
    
    suspend fun initialize() {
        try {
            // Initialize connection using singleton
            KmpIAP.instance.initConnection()
            
            // Listen to purchase updates
            launch {
                KmpIAP.instance.purchaseUpdatedListener.collect { purchase ->
                    handlePurchaseUpdate(purchase)
                }
            }
            
            // Listen to errors
            launch {
                KmpIAP.instance.purchaseErrorListener.collect { error ->
                    handlePurchaseError(error)
                }
            }
        } catch (e: Exception) {
            println("Initialization failed: ${e.message}")
        }
    }
    
    suspend fun loadProducts() {
        try {
            // Load in-app products
            val products = KmpIAP.instance.requestProducts(
                ProductRequest(
                    skus = listOf("remove_ads", "premium_upgrade"),
                    type = ProductType.INAPP
                )
            )
            
            products.forEach { product ->
                println("Product: ${product.id} - ${product.price}")
            }
        } catch (e: Exception) {
            println("Failed to load products: ${e.message}")
        }
    }
    
    suspend fun purchaseProduct(productId: String) {
        try {
            // Request purchase with unified API
            val purchase = KmpIAP.instance.requestPurchase(
                UnifiedPurchaseRequest(
                    sku = productId,
                    quantity = 1
                )
            )
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
            KmpIAP.instance.finishTransaction(
                purchase = purchase,
                isConsumable = true // true for consumables, false for subscriptions
            )
        }
    }
    
    private fun handlePurchaseError(error: PurchaseError) {
        when (error.code) {
            ErrorCode.E_USER_CANCELLED.name -> {
                println("User cancelled the purchase")
            }
            ErrorCode.E_ITEM_UNAVAILABLE.name -> {
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
    
    suspend fun disconnect() {
        KmpIAP.instance.endConnection()
    }
}
```

## Using with Instance Creation

For cases where you need more control (like testing or dependency injection):

```kotlin
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.types.*

class IAPService(private val kmpIAP: KmpIAP = KmpIAP()) {
    
    suspend fun initialize() {
        // Initialize with your own instance
        kmpIAP.initConnection()
        
        // Set up listeners
        launch {
            kmpIAP.purchaseUpdatedListener.collect { purchase ->
                // Handle purchase
            }
        }
    }
    
    suspend fun purchaseItem(productId: String) {
        val purchase = kmpIAP.requestPurchase(
            UnifiedPurchaseRequest(
                sku = productId,
                quantity = 1
            )
        )
        
        // Validate and finish transaction
        kmpIAP.finishTransaction(purchase, isConsumable = true)
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
    // Create instance for this screen (or use singleton)
    val kmpIAP = remember { KmpIAP() }
    // Or use singleton: val kmpIAP = KmpIAP.instance
    
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Initialize connection
        kmpIAP.initConnection()
        
        // Load products
        isLoading = true
        try {
            products = kmpIAP.requestProducts(
                ProductRequest(
                    skus = listOf("product_1", "product_2"),
                    type = ProductType.INAPP
                )
            )
        } finally {
            isLoading = false
        }
        
        // Listen for purchases
        launch {
            kmpIAP.purchaseUpdatedListener.collect { purchase ->
                // Handle successful purchase
                kmpIAP.finishTransaction(purchase, isConsumable = true)
            }
        }
    }
    
    Column {
        products.forEach { product ->
            Card(
                onClick = {
                    // Purchase product
                    scope.launch {
                        kmpIAP.requestPurchase(
                            UnifiedPurchaseRequest(
                                sku = product.id,
                                quantity = 1
                            )
                        )
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