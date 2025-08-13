---
title: KmpIAP Usage Guide
sidebar_position: 5
---

# KmpIAP Usage Guide

Complete guide for using KmpIAP with proper state management and error handling using Flow-based APIs.

## Basic Setup

### Initialization

```kotlin
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class PurchaseManager(
    private val scope: CoroutineScope
) {
    fun initialize() {
        scope.launch {
            try {
                val connected = KmpIAP.initConnection()
                if (connected) {
                    println("Successfully connected to store")
                    setupEventListeners()
                } else {
                    println("Failed to connect to store")
                }
            } catch (e: Exception) {
                println("Connection error: ${e.message}")
            }
        }
    }
    
    private fun setupEventListeners() {
        // Listen for purchase updates
        scope.launch {
            KmpIAP.purchaseUpdatedListener.collect { purchase ->
                handlePurchaseSuccess(purchase)
            }
        }
        
        // Listen for purchase errors
        scope.launch {
            KmpIAP.purchaseErrorListener.collect { error ->
                handlePurchaseError(error)
            }
        }
    }
}
```

## Error Handling

### Comprehensive Error Handling

```kotlin
import io.github.hyochan.kmpiap.ErrorCode

class PurchaseViewModel : ViewModel() {
    
    init {
        // Setup error listener with proper handling
        viewModelScope.launch {
            KmpIAP.purchaseErrorListener.collect { error ->
                when (error.code) {
                    ErrorCode.E_USER_CANCELLED.name -> {
                        // User cancelled - no error message needed
                        println("User cancelled the purchase")
                    }
                    ErrorCode.E_NETWORK_ERROR.name -> {
                        showErrorDialog("Network error. Please check your connection.")
                    }
                    ErrorCode.E_ITEM_UNAVAILABLE.name -> {
                        showErrorDialog("This item is not available.")
                    }
                    ErrorCode.E_ITEM_ALREADY_OWNED.name -> {
                        showInfoDialog("You already own this item.")
                        refreshPurchases()
                    }
                    ErrorCode.E_SERVICE_DISCONNECTED.name -> {
                        // Try to reconnect
                        reconnectToStore()
                    }
                    ErrorCode.E_BILLING_UNAVAILABLE.name -> {
                        showErrorDialog("Billing is not available on this device.")
                    }
                    else -> {
                        // Generic error handling
                        showErrorDialog("Purchase failed: ${error.message}")
                    }
                }
            }
        }
    }
    
    private fun showErrorDialog(message: String) {
        // Show error to user
        _errorMessage.value = message
    }
    
    private fun showInfoDialog(message: String) {
        // Show info to user
        _infoMessage.value = message
    }
}
```

## Purchase Flow Management

### Complete Purchase Flow with Error Handling

```kotlin
class PurchaseFlowManager(
    private val scope: CoroutineScope
) {
    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()
    
    sealed class PurchaseState {
        object Idle : PurchaseState()
        object Loading : PurchaseState()
        data class Success(val purchase: Purchase) : PurchaseState()
        data class Error(val error: PurchaseError) : PurchaseState()
    }
    
    init {
        // Setup purchase listeners
        scope.launch {
            KmpIAP.purchaseUpdatedListener.collect { purchase ->
                _purchaseState.value = PurchaseState.Success(purchase)
                processPurchase(purchase)
            }
        }
        
        scope.launch {
            KmpIAP.purchaseErrorListener.collect { error ->
                _purchaseState.value = PurchaseState.Error(error)
            }
        }
    }
    
    suspend fun purchaseProduct(productId: String) {
        _purchaseState.value = PurchaseState.Loading
        
        try {
            KmpIAP.requestPurchase(
                UnifiedPurchaseRequest(
                    sku = productId,
                    quantity = 1
                )
            )
        } catch (e: PurchaseError) {
            _purchaseState.value = PurchaseState.Error(e)
        }
    }
    
    private suspend fun processPurchase(purchase: Purchase) {
        try {
            // Verify purchase with your backend
            val isValid = verifyPurchase(purchase)
            
            if (isValid) {
                // Deliver content
                deliverContent(purchase.productId)
                
                // Finish transaction
                KmpIAP.finishTransaction(
                    purchase = purchase,
                    isConsumable = true
                )
            }
        } catch (e: Exception) {
            println("Failed to process purchase: ${e.message}")
        }
    }
}
```

## Subscription Management

### Subscription Flow with Proper Error Handling

```kotlin
class SubscriptionManager(
    private val scope: CoroutineScope
) {
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.NotSubscribed)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()
    
    sealed class SubscriptionState {
        object NotSubscribed : SubscriptionState()
        object Loading : SubscriptionState()
        data class Active(val purchase: Purchase) : SubscriptionState()
        data class Error(val message: String) : SubscriptionState()
    }
    
    init {
        setupListeners()
        checkActiveSubscriptions()
    }
    
    private fun setupListeners() {
        // Listen for successful subscription purchases
        scope.launch {
            KmpIAP.purchaseUpdatedListener
                .filter { purchase ->
                    // Filter for subscription purchases
                    purchase.productId.startsWith("subscription_")
                }
                .collect { purchase ->
                    handleSubscriptionPurchase(purchase)
                }
        }
        
        // Listen for subscription errors
        scope.launch {
            KmpIAP.purchaseErrorListener.collect { error ->
                when (error.code) {
                    ErrorCode.E_USER_CANCELLED.name -> {
                        _subscriptionState.value = SubscriptionState.NotSubscribed
                    }
                    ErrorCode.E_ITEM_ALREADY_OWNED.name -> {
                        // User already has an active subscription
                        checkActiveSubscriptions()
                    }
                    else -> {
                        _subscriptionState.value = SubscriptionState.Error(
                            error.message ?: "Subscription failed"
                        )
                    }
                }
            }
        }
    }
    
    private suspend fun handleSubscriptionPurchase(purchase: Purchase) {
        // For subscriptions, only acknowledge (don't consume)
        KmpIAP.finishTransaction(
            purchase = purchase,
            isConsumable = false // Important: subscriptions are not consumable
        )
        
        _subscriptionState.value = SubscriptionState.Active(purchase)
    }
    
    private fun checkActiveSubscriptions() {
        scope.launch {
            try {
                val purchases = KmpIAP.getAvailablePurchases()
                val activeSubscription = purchases.firstOrNull { purchase ->
                    purchase.productId.startsWith("subscription_")
                }
                
                if (activeSubscription != null) {
                    _subscriptionState.value = SubscriptionState.Active(activeSubscription)
                } else {
                    _subscriptionState.value = SubscriptionState.NotSubscribed
                }
            } catch (e: Exception) {
                _subscriptionState.value = SubscriptionState.Error(e.message ?: "Failed to check subscriptions")
            }
        }
    }
}
```

## Compose Integration

### Using KmpIAP in Compose UI

```kotlin
@Composable
fun PurchaseScreen() {
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var purchaseResult by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Setup listeners
    LaunchedEffect(Unit) {
        // Load products
        launch {
            try {
                products = KmpIAP.requestProducts(
                    ProductRequest(
                        skus = listOf("product_1", "product_2"),
                        type = ProductType.INAPP
                    )
                )
            } catch (e: Exception) {
                errorMessage = "Failed to load products: ${e.message}"
            }
        }
        
        // Listen for purchase updates
        launch {
            KmpIAP.purchaseUpdatedListener.collect { purchase ->
                purchaseResult = "Purchase successful: ${purchase.productId}"
                
                // Finish the transaction
                KmpIAP.finishTransaction(purchase, isConsumable = true)
            }
        }
        
        // Listen for errors
        launch {
            KmpIAP.purchaseErrorListener.collect { error ->
                errorMessage = when (error.code) {
                    ErrorCode.E_USER_CANCELLED.name -> null // Don't show error for cancellation
                    ErrorCode.E_NETWORK_ERROR.name -> "Network error. Please try again."
                    ErrorCode.E_ITEM_ALREADY_OWNED.name -> "You already own this item."
                    else -> error.message
                }
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Show products
        products.forEach { product ->
            ProductCard(
                product = product,
                onPurchase = {
                    purchaseProduct(product.id)
                }
            )
        }
        
        // Show purchase result
        purchaseResult?.let {
            SuccessMessage(message = it)
        }
        
        // Show error message
        errorMessage?.let {
            ErrorMessage(message = it)
        }
    }
}

private suspend fun purchaseProduct(productId: String) {
    try {
        KmpIAP.requestPurchase(
            UnifiedPurchaseRequest(
                sku = productId,
                quantity = 1
            )
        )
    } catch (e: Exception) {
        // Error will be handled by purchaseErrorListener
    }
}
```

## Advanced Patterns

### Retry Logic with Exponential Backoff

```kotlin
class ResilientPurchaseManager(
    private val scope: CoroutineScope
) {
    private var retryCount = 0
    private val maxRetries = 3
    
    init {
        scope.launch {
            KmpIAP.purchaseErrorListener.collect { error ->
                when (error.code) {
                    ErrorCode.E_NETWORK_ERROR.name,
                    ErrorCode.E_SERVICE_DISCONNECTED.name -> {
                        retryWithBackoff()
                    }
                    else -> {
                        // Reset retry count for other errors
                        retryCount = 0
                    }
                }
            }
        }
    }
    
    private suspend fun retryWithBackoff() {
        if (retryCount < maxRetries) {
            val delayMillis = (1000 * Math.pow(2.0, retryCount.toDouble())).toLong()
            delay(delayMillis)
            retryCount++
            
            try {
                KmpIAP.initConnection()
                retryCount = 0 // Reset on success
            } catch (e: Exception) {
                // Will trigger error listener again
            }
        }
    }
}
```

### Combined State Management

```kotlin
class PurchaseStateManager(
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(PurchaseUiState())
    val state: StateFlow<PurchaseUiState> = _state.asStateFlow()
    
    data class PurchaseUiState(
        val isLoading: Boolean = false,
        val products: List<Product> = emptyList(),
        val lastPurchase: Purchase? = null,
        val error: String? = null
    )
    
    init {
        // Combine multiple flows into single state
        scope.launch {
            combine(
                KmpIAP.purchaseUpdatedListener,
                KmpIAP.purchaseErrorListener
            ) { purchase, error ->
                _state.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        lastPurchase = purchase,
                        error = error?.message
                    )
                }
            }.collect()
        }
    }
}
```

## Best Practices

1. **Always handle all error codes** - Provide appropriate user feedback for each error type
2. **Use structured concurrency** - Launch collectors in appropriate scopes
3. **Don't show error for user cancellation** - This is expected behavior
4. **Verify purchases server-side** - Always validate purchases with your backend
5. **Handle reconnection** - Implement retry logic for network errors
6. **Clean up resources** - Cancel collection jobs when no longer needed
7. **Test error scenarios** - Use test cards/accounts to verify error handling

## Migration from UseIap

If you were using the old UseIap pattern, here's how to migrate:

### Old Pattern
```kotlin
// Old UseIap pattern
iapHelper.currentError.collectLatest { error ->
    error?.let {
        showError(it)
        iapHelper.clearError()
    }
}
```

### New Pattern
```kotlin
// New KmpIAP pattern
KmpIAP.purchaseErrorListener.collect { error ->
    // No need to check for null or clear
    showError(error)
}
```

The new API is simpler:
- No need to clear errors manually
- No nullable types in flows
- Direct access via KmpIAP singleton
- Cleaner event-driven architecture

## See Also

- [Core Methods](./core-methods.md) - Complete API reference
- [Error Codes](./error-codes.md) - All error codes explained
- [Listeners](./listeners.md) - Event listener documentation
- [Examples](../examples/complete-implementation.md) - Full implementation examples