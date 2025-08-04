---
sidebar_position: 2
---

# Quick Start

Get up and running with KMP IAP in just a few minutes!

## Basic Implementation

Here's a complete example to get you started with in-app purchases:

```kotlin
import io.github.hyochan.kmpiap.*
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.*

class IAPManager {
    private val iap = createInAppPurchase()
    
    suspend fun initialize() {
        try {
            // Initialize connection
            iap.initConnection()
            
            // Listen to purchase updates
            iap.purchaseUpdatedFlow.collect { purchase ->
                handlePurchaseUpdate(purchase)
            }
        } catch (e: Exception) {
            println("Initialization failed: ${e.message}")
        }
    }
    
    suspend fun loadProducts() {
        try {
            // Load in-app products
            val products = iap.requestProducts(
                RequestProductsParams(
                    skus = listOf("remove_ads", "premium_upgrade"),
                    type = PurchaseType.INAPP
                )
            )
            
            products.forEach { product ->
                println("Product: ${product.productId} - ${product.localizedPrice}")
            }
        } catch (e: Exception) {
            println("Failed to load products: ${e.message}")
        }
    }
    
    suspend fun purchaseProduct(productId: String) {
        try {
            // Request purchase based on platform
            val request = when (iap.getStore()) {
                Store.PLAY_STORE -> RequestPurchaseAndroid(
                    skus = listOf(productId)
                )
                Store.APP_STORE -> RequestPurchaseIOS(
                    sku = productId
                )
                else -> RequestPurchaseGeneric(sku = productId)
            }
            
            iap.requestPurchase(
                request = request,
                type = PurchaseType.INAPP
            )
        } catch (e: PurchaseError) {
            when (e.code) {
                ErrorCode.E_USER_CANCELLED -> {
                    println("User cancelled the purchase")
                }
                ErrorCode.E_ITEM_UNAVAILABLE -> {
                    println("Item is not available")
                }
                else -> {
                    println("Purchase failed: ${e.message}")
                }
            }
        }
    }
    
    private suspend fun handlePurchaseUpdate(purchase: Purchase) {
        // Verify the purchase (implement your own verification)
        val isValid = verifyPurchase(purchase)
        
        if (isValid) {
            // Deliver the product
            deliverProduct(purchase.productId)
            
            // Finish the transaction
            iap.finishTransaction(purchase, isConsumable = true)
        }
    }
    
    private fun verifyPurchase(purchase: Purchase): Boolean {
        // Implement your purchase verification logic
        // This could involve server-side validation
        return true
    }
    
    private fun deliverProduct(productId: String) {
        // Implement your product delivery logic
        println("Product delivered: $productId")
    }
    
    fun disconnect() {
        iap.endConnection()
    }
}
```

## Using with ViewModels

Here's how to integrate KMP IAP with your ViewModels:

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class StoreViewModel : ViewModel() {
    private val iapManager = IAPManager()
    
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        viewModelScope.launch {
            iapManager.initialize()
            loadProducts()
        }
    }
    
    private suspend fun loadProducts() {
        _isLoading.value = true
        try {
            val loadedProducts = iapManager.loadProducts()
            _products.value = loadedProducts
        } finally {
            _isLoading.value = false
        }
    }
    
    fun purchaseProduct(productId: String) {
        viewModelScope.launch {
            iapManager.purchaseProduct(productId)
        }
    }
    
    override fun onCleared() {
        iapManager.disconnect()
        super.onCleared()
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