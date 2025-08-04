---
title: State Management
sidebar_position: 4
---

# State Management

Real-time state management using Kotlin StateFlow for monitoring purchase transactions, connection states, and other IAP events in kmp-iap v1.0.0.

## Core StateFlow Properties

### Connection State

```kotlin
val isConnected: StateFlow<Boolean>
```

**Type**: `StateFlow<Boolean>`  
**Description**: Current connection status to the store  
**Initial Value**: `false`

**Example**:
```kotlin
import kotlinx.coroutines.flow.collectLatest

class IAPViewModel(private val iapHelper: UseIap) {
    
    init {
        // Observe connection state
        scope.launch {
            iapHelper.isConnected.collectLatest { connected ->
                if (connected) {
                    println("Store connected - ready for purchases")
                    loadProducts()
                } else {
                    println("Store disconnected")
                    disablePurchaseUI()
                }
            }
        }
    }
    
    // Get current state
    fun checkConnection(): Boolean {
        return iapHelper.isConnected.value
    }
}
```

---

### Product Lists

```kotlin
val products: StateFlow<List<Product>>
val subscriptions: StateFlow<List<Product>>
```

**Type**: `StateFlow<List<Product>>`  
**Description**: Available products and subscriptions  
**Initial Value**: Empty list

**Example**:
```kotlin
// Observe product updates
scope.launch {
    iapHelper.products.collectLatest { productList ->
        updateProductUI(productList)
    }
}

// Observe subscription updates
scope.launch {
    iapHelper.subscriptions.collectLatest { subList ->
        updateSubscriptionUI(subList)
    }
}

// Get current products
fun getCurrentProducts(): List<Product> {
    return iapHelper.products.value
}
```

---

### Purchase States

```kotlin
val currentPurchase: StateFlow<Purchase?>
val availablePurchases: StateFlow<List<Purchase>>
val purchaseHistories: StateFlow<List<Purchase>>
```

**Types**: 
- `StateFlow<Purchase?>` - Current active purchase
- `StateFlow<List<Purchase>>` - Available and historical purchases

**Example**:
```kotlin
// Observe current purchase
scope.launch {
    iapHelper.currentPurchase.collectLatest { purchase ->
        purchase?.let {
            handlePurchaseSuccess(it)
        }
    }
}

// Observe available purchases
scope.launch {
    iapHelper.availablePurchases.collectLatest { purchases ->
        updateOwnedProducts(purchases)
    }
}
```

---

### Error State

```kotlin
val currentError: StateFlow<PurchaseError?>
```

**Type**: `StateFlow<PurchaseError?>`  
**Description**: Current error state  
**Initial Value**: `null`

**Example**:
```kotlin
scope.launch {
    iapHelper.currentError.collectLatest { error ->
        error?.let {
            showErrorDialog(it)
            // Clear error after showing
            iapHelper.clearError()
        }
    }
}
```

## Complete State Management Example

### ViewModel Implementation

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class PurchaseViewModel : ViewModel() {
    private val scope = viewModelScope
    private val iapHelper = UseIap(
        scope = scope,
        options = UseIapOptions()
    )
    
    // UI State
    data class IAPState(
        val isConnected: Boolean = false,
        val products: List<Product> = emptyList(),
        val subscriptions: List<Product> = emptyList(),
        val ownedProducts: List<Purchase> = emptyList(),
        val currentPurchase: Purchase? = null,
        val error: PurchaseError? = null,
        val isLoading: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(IAPState())
    val uiState: StateFlow<IAPState> = _uiState.asStateFlow()
    
    init {
        // Combine all states into UI state
        combine(
            iapHelper.isConnected,
            iapHelper.products,
            iapHelper.subscriptions,
            iapHelper.availablePurchases,
            iapHelper.currentPurchase,
            iapHelper.currentError
        ) { connected, products, subs, owned, purchase, error ->
            IAPState(
                isConnected = connected,
                products = products,
                subscriptions = subs,
                ownedProducts = owned,
                currentPurchase = purchase,
                error = error,
                isLoading = false
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(scope)
        
        // Initialize connection
        initializeIAP()
    }
    
    private fun initializeIAP() {
        scope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                iapHelper.initConnection()
            } catch (e: PurchaseError) {
                // Error will be handled by error state flow
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun loadProducts(skus: List<String>) {
        scope.launch {
            try {
                iapHelper.getProducts(skus)
                // Products will be updated via StateFlow
            } catch (e: PurchaseError) {
                // Error will be handled by error state flow
            }
        }
    }
    
    fun purchaseProduct(productId: String) {
        scope.launch {
            try {
                iapHelper.requestPurchase(
                    sku = productId,
                    obfuscatedAccountIdAndroid = getUserId()
                )
                // Purchase result will be emitted via currentPurchase
            } catch (e: PurchaseError) {
                // Error will be handled by error state flow
            }
        }
    }
    
    fun clearError() {
        iapHelper.clearError()
    }
    
    override fun onCleared() {
        super.onCleared()
        iapHelper.dispose()
    }
}
```

### Compose UI Integration

```kotlin
@Composable
fun PurchaseScreen(viewModel: PurchaseViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(uiState.currentPurchase) {
        uiState.currentPurchase?.let { purchase ->
            // Handle successful purchase
            showSuccessMessage("Purchase successful: ${purchase.productId}")
            viewModel.finishTransaction(purchase)
        }
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            // Show error dialog
            showErrorDialog(error.message)
            viewModel.clearError()
        }
    }
    
    Column {
        // Connection indicator
        ConnectionStatus(isConnected = uiState.isConnected)
        
        // Products list
        LazyColumn {
            items(uiState.products) { product ->
                ProductCard(
                    product = product,
                    onPurchase = { viewModel.purchaseProduct(product.productId) },
                    isOwned = uiState.ownedProducts.any { it.productId == product.productId }
                )
            }
        }
        
        // Loading overlay
        if (uiState.isLoading) {
            CircularProgressIndicator()
        }
    }
}
```

## Platform-Specific States

### iOS Promoted Products

```kotlin
val promotedProductsIOS: StateFlow<List<Product>?>
```

**Type**: `StateFlow<List<Product>?>`  
**Platform**: iOS only  
**Description**: Products promoted from App Store

**Example**:
```kotlin
if (getCurrentPlatform() == IAPPlatform.IOS) {
    scope.launch {
        iapHelper.promotedProductsIOS.collectLatest { promotedProducts ->
            promotedProducts?.let {
                showPromotedProducts(it)
            }
        }
    }
}
```

## State Flow Patterns

### Hot vs Cold Observables

StateFlow is hot - it maintains current state even without collectors:

```kotlin
// StateFlow retains latest value
val currentProducts = iapHelper.products.value // Get current value immediately

// Multiple collectors share the same state
scope.launch {
    iapHelper.products.collect { /* Collector 1 */ }
}
scope.launch {
    iapHelper.products.collect { /* Collector 2 - gets same values */ }
}
```

### Combining States

Combine multiple states for complex UI:

```kotlin
data class ProductWithOwnership(
    val product: Product,
    val isOwned: Boolean,
    val isPurchasing: Boolean
)

val productsWithOwnership = combine(
    iapHelper.products,
    iapHelper.availablePurchases,
    iapHelper.currentPurchase
) { products, purchases, currentPurchase ->
    products.map { product ->
        ProductWithOwnership(
            product = product,
            isOwned = purchases.any { it.productId == product.productId },
            isPurchasing = currentPurchase?.productId == product.productId
        )
    }
}.stateIn(
    scope = scope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
)
```

### Debouncing and Throttling

Handle rapid state changes:

```kotlin
// Debounce error messages
iapHelper.currentError
    .debounce(500) // Wait 500ms for stable state
    .collectLatest { error ->
        error?.let { showError(it) }
    }

// Throttle purchase requests
private val purchaseRequests = MutableSharedFlow<String>()

init {
    purchaseRequests
        .throttleLatest(1000) // Max one purchase per second
        .collectLatest { productId ->
            iapHelper.requestPurchase(productId)
        }
}
```

## Error Handling Best Practices

### Centralized Error Handling

```kotlin
class ErrorHandler(private val iapHelper: UseIap) {
    init {
        scope.launch {
            iapHelper.currentError.collectLatest { error ->
                error?.let {
                    when (it.code) {
                        ErrorCode.USER_CANCELLED -> {
                            // Don't show error for user cancellation
                            println("User cancelled purchase")
                        }
                        ErrorCode.NETWORK_ERROR -> {
                            showRetryableError("Network error. Please try again.")
                        }
                        ErrorCode.ALREADY_OWNED -> {
                            showInfo("You already own this item.")
                            refreshPurchases()
                        }
                        else -> {
                            showError(it.message)
                        }
                    }
                    
                    // Clear error after handling
                    iapHelper.clearError()
                }
            }
        }
    }
}
```

### State Recovery

Implement state recovery mechanisms:

```kotlin
class IAPStateManager(private val iapHelper: UseIap) {
    private var lastKnownProducts: List<Product> = emptyList()
    
    init {
        // Save last known good state
        scope.launch {
            iapHelper.products.collectLatest { products ->
                if (products.isNotEmpty()) {
                    lastKnownProducts = products
                }
            }
        }
        
        // Monitor connection and recover
        scope.launch {
            iapHelper.isConnected.collectLatest { connected ->
                if (!connected) {
                    scheduleReconnection()
                }
            }
        }
    }
    
    private fun scheduleReconnection() {
        scope.launch {
            delay(5000) // Wait 5 seconds
            try {
                iapHelper.initConnection()
                // Reload products with last known SKUs
                if (lastKnownProducts.isNotEmpty()) {
                    val skus = lastKnownProducts.map { it.productId }
                    iapHelper.getProducts(skus)
                }
            } catch (e: Exception) {
                // Retry again
                scheduleReconnection()
            }
        }
    }
}
```

## Performance Optimization

### Lazy Collection

Only collect when needed:

```kotlin
@Composable
fun ProductList(viewModel: PurchaseViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val job = lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.iapHelper.products.collectLatest { products ->
                    // Only collect when screen is visible
                    updateUI(products)
                }
            }
        }
        
        onDispose {
            job.cancel()
        }
    }
}
```

### State Caching

Cache expensive computations:

```kotlin
private val _sortedProducts = MutableStateFlow<List<Product>>(emptyList())
val sortedProducts: StateFlow<List<Product>> = _sortedProducts

init {
    iapHelper.products
        .map { products ->
            // Expensive sorting operation
            products.sortedBy { it.priceAmountMicros }
        }
        .distinctUntilChanged()
        .onEach { sorted ->
            _sortedProducts.value = sorted
        }
        .launchIn(scope)
}
```

## Testing

### Mock StateFlow for Testing

```kotlin
class MockUseIap : UseIap {
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected
    
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    override val products: StateFlow<List<Product>> = _products
    
    // Test helpers
    fun setConnected(connected: Boolean) {
        _isConnected.value = connected
    }
    
    fun setProducts(products: List<Product>) {
        _products.value = products
    }
}

// In tests
@Test
fun testPurchaseFlow() = runTest {
    val mockIap = MockUseIap()
    val viewModel = PurchaseViewModel(mockIap)
    
    // Simulate connection
    mockIap.setConnected(true)
    
    // Verify UI state
    assertEquals(true, viewModel.uiState.value.isConnected)
}
```

## Best Practices

1. **Use collectLatest**: Cancels previous collection on new emission
2. **Handle all states**: Don't assume non-null values
3. **Clear errors**: Always clear error state after handling
4. **Lifecycle awareness**: Cancel collections when not needed
5. **Combine states**: Use `combine` for derived states
6. **Test states**: Mock StateFlow for unit testing

## See Also

- [Core Methods](./core-methods.md) - Methods that update state
- [Types](./types.md) - State data structures
- [Error Codes](./error-codes.md) - Error state handling
- [Examples](../examples/basic-store.md) - Complete state management examples