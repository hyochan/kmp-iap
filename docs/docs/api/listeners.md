---
title: Listeners
sidebar_position: 4
---

import IapKitBanner from '@site/src/uis/IapKitBanner';

# Listeners

<IapKitBanner />

Event listeners and flow collectors for monitoring purchase updates, errors, and connection states in kmp-iap.

## Purchase Update Listener

### purchaseUpdatedListener

```kotlin
val purchaseUpdatedListener: Flow<Purchase>
```

**Type**: `Flow<Purchase>`  
**Description**: Emits purchase updates when transactions occur  
**Emission**: Triggered on successful purchase completion

**Example**:
```kotlin
import kotlinx.coroutines.flow.collectLatest

class PurchaseManager {
    private val kmpIAP = KmpIAP()
    private val scope = CoroutineScope(Dispatchers.Main)
    
    init {
        scope.launch {
            kmpIAP.purchaseUpdatedListener.collectLatest { purchase ->
                println("Purchase updated: \${purchase.productId}")
                println("Transaction ID: \${purchase.transactionId}")
                println("State: \${purchase.purchaseState}")
                
                // Handle purchase based on state
                when (purchase.purchaseState) {
                    PurchaseState.PURCHASED -> {
                        handleSuccessfulPurchase(purchase)
                    }
                    PurchaseState.PENDING -> {
                        handlePendingPurchase(purchase)
                    }
                    else -> {
                        println("Unexpected purchase state")
                    }
                }
            }
        }
    }
    
    private suspend fun handleSuccessfulPurchase(purchase: Purchase) {
        // Validate purchase
        validatePurchase(purchase)
        
        // Grant entitlement
        grantEntitlement(purchase.productId)
        
        // Finish transaction
        kmpIAP.finishTransaction(purchase, isConsumable = true)
    }
}
```

---

## Error Listener

### purchaseErrorListener

```kotlin
val purchaseErrorListener: Flow<PurchaseError>
```

**Type**: `Flow<PurchaseError>`  
**Description**: Emits errors that occur during purchase operations  
**Emission**: Triggered on any purchase-related error

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

scope.launch {
    kmpIapInstance.purchaseErrorListener.collectLatest { error ->
        println("Purchase error: \${error.message}")
        println("Error code: \${error.code}")
        
        when (error.code) {
            ErrorCode.USER_CANCELLED -> {
                // User cancelled, no action needed
                println("User cancelled the purchase")
            }
            ErrorCode.NETWORK_ERROR -> {
                showRetryDialog("Network error. Please check your connection.")
            }
            ErrorCode.ITEM_UNAVAILABLE -> {
                showError("This item is not available in your region.")
            }
            ErrorCode.ALREADY_OWNED -> {
                showInfo("You already own this item.")
                refreshOwnedPurchases()
            }
            else -> {
                showError("Purchase failed: \${error.message}")
            }
        }
    }
}
```

---

## Connection State Listener

### Connection State

```kotlin
suspend fun isConnected(): Boolean
```

**Type**: `suspend fun`  
**Description**: Check connection state to the store service  
**Returns**: `Boolean` - true if connected

**Example**:
```kotlin
// Check connection state
scope.launch {
    val connected = kmpIapInstance.isConnected()
    if (connected) {
        enablePurchaseButtons()
        loadProducts()
    } else {
        disablePurchaseButtons()
        showConnectionError()
    }
}

// Connection state with retry
class ConnectionManager(private val iap: InAppPurchase) {
    private var retryCount = 0
    private val maxRetries = 3
    
    init {
        monitorConnection()
    }
    
    private fun monitorConnection() {
        scope.launch {
            kmpIapInstance.isConnected.collectLatest { connected ->
                if (!connected && retryCount < maxRetries) {
                    delay(2000 * (retryCount + 1)) // Exponential backoff
                    retryCount++
                    try {
                        kmpIapInstance.initConnection()
                    } catch (e: Exception) {
                        println("Retry failed: \${e.message}")
                    }
                } else if (connected) {
                    retryCount = 0
                }
            }
        }
    }
}
```

---

## Platform-Specific Listeners

### iOS Promoted Product Listener

```kotlin
val promotedProductListener: Flow<String?>
```

**Type**: `Flow<String?>`
**Platform**: iOS only
**Description**: Product ID of promoted product from App Store that triggered app launch

**Example**:
```kotlin
if (getCurrentPlatform() == IapPlatform.IOS) {
    scope.launch {
        kmpIapInstance.promotedProductListener.collect { productId ->
            productId?.let {
                // Fetch product details using the product ID
                val products = kmpIapInstance.getProducts(
                    GetProductsProps(skus = listOf(it))
                )
                products.firstOrNull()?.let { product ->
                    showProductDetail(product)
                }

                // Optionally auto-purchase
                if (userSettings.autoPromotedPurchase) {
                    kmpIapInstance.buyPromotedProductIOS()
                }
            }
        }
    }
}
```

### Android Billing Client State

While not exposed as a direct flow, Android billing client state changes can be monitored:

```kotlin
// Monitor through connection state
scope.launch {
    kmpIapInstance.isConnected.collectLatest { connected ->
        if (connected) {
            // BillingClient is ready
            println("Google Play Billing connected")
        } else {
            // BillingClient disconnected
            println("Google Play Billing disconnected")
        }
    }
}
```

---

## Advanced Listener Patterns

### Combined Listeners

Monitor multiple events simultaneously:

```kotlin
class PurchaseFlowManager(private val iap: InAppPurchase) {
    
    private val kmpIAP = KmpIAP()
    
    init {
        // Combine purchase and error flows
        scope.launch {
            merge(
                kmpIAP.purchaseUpdatedListener.map { PurchaseEvent.Success(it) },
                kmpIAP.purchaseErrorListener.map { PurchaseEvent.Error(it) }
            ).collectLatest { event ->
                when (event) {
                    is PurchaseEvent.Success -> handleSuccess(event.purchase)
                    is PurchaseEvent.Error -> handleError(event.error)
                }
            }
        }
    }
    
    sealed class PurchaseEvent {
        data class Success(val purchase: Purchase) : PurchaseEvent()
        data class Error(val error: PurchaseError) : PurchaseEvent()
    }
}
```

### Filtered Listeners

Listen for specific events:

```kotlin
// Only listen for subscription purchases
kmpIapInstance.purchaseUpdatedListener
    .filter { purchase ->
        purchase.products.any { it.type == PurchaseType.SUBS }
    }
    .collectLatest { subscriptionPurchase ->
        handleSubscriptionPurchase(subscriptionPurchase)
    }

// Only listen for specific error types
kmpIAP.purchaseErrorListener
    .filter { error ->
        error.code in listOf(
            ErrorCode.NETWORK_ERROR,
            ErrorCode.SERVICE_UNAVAILABLE
        )
    }
    .collectLatest { networkError ->
        scheduleRetry()
    }
```

### Debounced Listeners

Prevent rapid successive events:

```kotlin
// Debounce purchase updates
kmpIapInstance.purchaseUpdatedListener
    .debounce(500) // Wait 500ms for stable state
    .collectLatest { purchase ->
        updatePurchaseUI(purchase)
    }

// Throttle error messages
kmpIAP.purchaseErrorListener
    .throttleLatest(2000) // Max one error dialog per 2 seconds
    .collectLatest { error ->
        showErrorDialog(error)
    }
```

---

## Lifecycle-Aware Listeners

### Compose Integration

```kotlin
@Composable
fun PurchaseScreen(iap: InAppPurchase) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Purchase updates
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            kmpIapInstance.purchaseUpdatedListener.collectLatest { purchase ->
                // Only collect when screen is visible
                showPurchaseSuccess(purchase)
            }
        }
    }
    
    // Error handling
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            kmpIAP.purchaseErrorListener.collectLatest { error ->
                showErrorSnackbar(error.message)
            }
        }
    }
}
```

### Activity/Fragment Integration

```kotlin
class PurchaseActivity : AppCompatActivity() {
    private lateinit var iap: InAppPurchase
    private val purchaseJobs = mutableListOf<Job>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        iap = createInAppPurchase()
        setupListeners()
    }
    
    private fun setupListeners() {
        // Lifecycle-aware collection
        purchaseJobs += lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                kmpIapInstance.purchaseUpdatedListener.collectLatest { purchase ->
                    handlePurchaseUpdate(purchase)
                }
            }
        }
        
        purchaseJobs += lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                kmpIapInstance.purchaseErrorListener.collectLatest { error ->
                    handlePurchaseError(error)
                }
            }
        }
    }
    
    override fun onDestroy() {
        // Clean up listeners
        purchaseJobs.forEach { it.cancel() }
        purchaseJobs.clear()
        super.onDestroy()
    }
}
```

---

## Error Recovery Strategies

### Automatic Retry with Listeners

```kotlin
class ResilientPurchaseManager(private val iap: InAppPurchase) {
    private val retryDelays = listOf(1000L, 2000L, 4000L, 8000L)
    private val retryAttempts = mutableMapOf<String, Int>()
    
    init {
        scope.launch {
            kmpIapInstance.purchaseErrorListener.collectLatest { error ->
                when (error.code) {
                    ErrorCode.NETWORK_ERROR,
                    ErrorCode.SERVICE_UNAVAILABLE -> {
                        scheduleRetry(error)
                    }
                    else -> {
                        // Non-retryable error
                        resetRetryCount(error.message)
                    }
                }
            }
        }
        
        scope.launch {
            kmpIAP.purchaseUpdatedListener.collectLatest { purchase ->
                // Success - reset retry count
                resetRetryCount(purchase.productId)
            }
        }
    }
    
    private suspend fun scheduleRetry(error: PurchaseError) {
        val attempt = retryAttempts.getOrDefault(error.message, 0)
        if (attempt < retryDelays.size) {
            delay(retryDelays[attempt])
            retryAttempts[error.message] = attempt + 1
            // Retry the operation
            retryLastOperation()
        }
    }
    
    private fun resetRetryCount(key: String) {
        retryAttempts.remove(key)
    }
}
```

---

## Testing Listeners

### Mock Flow Testing

```kotlin
class MockKmpIAP : KmpIAP {
    private val _purchaseUpdated = MutableSharedFlow<Purchase>()
    override val purchaseUpdatedListener: Flow<Purchase> = _purchaseUpdated

    private val _purchaseError = MutableSharedFlow<PurchaseError>()
    override val purchaseErrorListener: Flow<PurchaseError> = _purchaseError
    
    // Test helpers
    suspend fun emitPurchase(purchase: Purchase) {
        _purchaseUpdated.emit(purchase)
    }
    
    suspend fun emitError(error: PurchaseError) {
        _purchaseError.emit(error)
    }
}

// In tests
@Test
fun testPurchaseListener() = runTest {
    val mockIap = MockKmpIAP()
    var receivedPurchase: Purchase? = null
    
    val job = launch {
        mockIap.purchaseUpdatedListener.collect { purchase ->
            receivedPurchase = purchase
        }
    }
    
    // Emit test purchase
    val testPurchase = Purchase(
        productId = "test_product",
        transactionId = "12345"
    )
    mockIap.emitPurchase(testPurchase)
    
    // Verify
    advanceUntilIdle()
    assertEquals("test_product", receivedPurchase?.productId)
    
    job.cancel()
}
```

---

## Best Practices

1. **Always collect in coroutine scope**: Prevent memory leaks
2. **Use lifecycle-aware collection**: Stop listening when UI is not visible
3. **Handle all error types**: Provide appropriate user feedback
4. **Debounce/throttle when needed**: Prevent UI flooding
5. **Clean up listeners**: Cancel jobs when no longer needed
6. **Test listener behavior**: Mock flows for unit testing
7. **Combine related flows**: Simplify complex event handling

## See Also

- [Core Methods](./core-methods.md) - Methods that trigger events
- [Error Codes](./error-codes.md) - Complete error code reference
- [Examples](../examples/complete-implementation.md) - Full listener implementation