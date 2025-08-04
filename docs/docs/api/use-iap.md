---
title: UseIap Class
sidebar_position: 5
---

# UseIap Class

The main class for managing in-app purchases in kmp-iap. Provides a comprehensive API with StateFlow-based state management for Kotlin Multiplatform projects.

## Class Overview

```kotlin
class UseIap(
    private val scope: CoroutineScope,
    private val options: UseIapOptions = UseIapOptions()
)
```

**Parameters**:
- `scope` - CoroutineScope for managing coroutines lifecycle
- `options` - Configuration options for the IAP helper

## Basic Usage

### Initialization

```kotlin
import io.github.hyochan.kmpiap.useIap.*
import kotlinx.coroutines.*

class PurchaseManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var iapHelper: UseIap
    
    fun initialize() {
        iapHelper = UseIap(
            scope = scope,
            options = UseIapOptions(
                autoFinishTransactions = true,
                enablePendingPurchases = true
            )
        )
        
        // Initialize connection
        scope.launch {
            try {
                iapHelper.initConnection()
                println("IAP initialized successfully")
            } catch (e: PurchaseError) {
                println("Failed to initialize: ${e.message}")
            }
        }
    }
}
```

### State Observation

```kotlin
class PurchaseViewModel(
    private val iapHelper: UseIap
) : ViewModel() {
    
    init {
        // Observe connection state
        viewModelScope.launch {
            iapHelper.isConnected.collectLatest { connected ->
                updateConnectionUI(connected)
            }
        }
        
        // Observe products
        viewModelScope.launch {
            iapHelper.products.collectLatest { products ->
                updateProductList(products)
            }
        }
        
        // Observe purchase events
        viewModelScope.launch {
            iapHelper.currentPurchase.collectLatest { purchase ->
                purchase?.let {
                    handlePurchaseSuccess(it)
                    iapHelper.clearPurchase()
                }
            }
        }
        
        // Observe errors
        viewModelScope.launch {
            iapHelper.currentError.collectLatest { error ->
                error?.let {
                    showError(it)
                    iapHelper.clearError()
                }
            }
        }
    }
}
```

## Constructor Options

### UseIapOptions

```kotlin
data class UseIapOptions(
    val autoFinishTransactions: Boolean = true,
    val enablePendingPurchases: Boolean = true,
    val connectionTimeout: Duration = 30.seconds
)
```

**Properties**:
- `autoFinishTransactions` - Automatically finish transactions after purchase
- `enablePendingPurchases` - Enable pending purchases on Android
- `connectionTimeout` - Timeout for connection establishment

## State Properties

### Connection State

```kotlin
val isConnected: StateFlow<Boolean>
```

Indicates whether the IAP service is connected and ready.

```kotlin
// Observe connection state
scope.launch {
    iapHelper.isConnected.collectLatest { connected ->
        if (connected) {
            loadProducts()
        } else {
            disablePurchaseButtons()
        }
    }
}
```

### Product Lists

```kotlin
val products: StateFlow<List<Product>>
val subscriptions: StateFlow<List<Product>>
```

Lists of available products and subscriptions.

```kotlin
// Display products
@Composable
fun ProductList(iapHelper: UseIap) {
    val products by iapHelper.products.collectAsState()
    
    LazyColumn {
        items(products) { product ->
            ProductCard(
                product = product,
                onPurchase = { 
                    scope.launch {
                        iapHelper.requestPurchase(product.productId)
                    }
                }
            )
        }
    }
}
```

### Purchase States

```kotlin
val availablePurchases: StateFlow<List<Purchase>>
val purchaseHistories: StateFlow<List<Purchase>>
val currentPurchase: StateFlow<Purchase?>
```

Purchase-related state flows.

```kotlin
// Check if user owns a product
fun isProductOwned(productId: String): Boolean {
    return iapHelper.availablePurchases.value.any { 
        it.productId == productId 
    }
}
```

### Error State

```kotlin
val currentError: StateFlow<PurchaseError?>
```

Current error state, if any.

```kotlin
// Error handling
LaunchedEffect(Unit) {
    iapHelper.currentError.collectLatest { error ->
        error?.let {
            when (it.code) {
                ErrorCode.USER_CANCELLED -> {
                    // User cancelled, no action needed
                }
                ErrorCode.NETWORK_ERROR -> {
                    showRetryDialog("Network error. Please try again.")
                }
                else -> {
                    showErrorDialog(it.message)
                }
            }
            iapHelper.clearError()
        }
    }
}
```

### Platform-Specific States

```kotlin
val promotedProductsIOS: StateFlow<List<Product>?>
```

iOS-specific promoted products from App Store.

## Core Methods

### Connection Management

```kotlin
suspend fun initConnection()
fun dispose()
```

**Example**:
```kotlin
class IAPService {
    private val iapHelper = UseIap(scope, options)
    
    suspend fun connect() {
        try {
            iapHelper.initConnection()
        } catch (e: PurchaseError) {
            handleError(e)
        }
    }
    
    fun cleanup() {
        iapHelper.dispose()
    }
}
```

### Product Management

```kotlin
suspend fun getProducts(skus: List<String>): List<Product>
suspend fun getSubscriptions(skus: List<String>): List<Product>
```

**Example**:
```kotlin
suspend fun loadStore() {
    try {
        // Load products
        val productSkus = listOf("coins_100", "coins_500", "remove_ads")
        iapHelper.getProducts(productSkus)
        
        // Load subscriptions
        val subscriptionSkus = listOf("premium_monthly", "premium_yearly")
        iapHelper.getSubscriptions(subscriptionSkus)
        
        // Products will be available via StateFlow
    } catch (e: PurchaseError) {
        println("Failed to load products: ${e.message}")
    }
}
```

### Purchase Processing

```kotlin
suspend fun requestPurchase(
    sku: String,
    andDangerouslyFinishTransactionAutomaticallyIOS: Boolean? = null,
    appAccountTokenIOS: String? = null,
    quantityIOS: Int? = null,
    obfuscatedAccountIdAndroid: String? = null,
    obfuscatedProfileIdAndroid: String? = null,
    isOfferPersonalizedAndroid: Boolean? = null
)

suspend fun requestSubscription(
    sku: String,
    subscriptionOffers: List<SubscriptionOfferAndroid>? = null,
    // ... same parameters as requestPurchase
)
```

**Example**:
```kotlin
// Simple purchase
suspend fun purchaseItem(productId: String) {
    try {
        iapHelper.requestPurchase(
            sku = productId,
            obfuscatedAccountIdAndroid = getUserId()
        )
        // Result will be emitted via currentPurchase StateFlow
    } catch (e: PurchaseError) {
        // Error will be emitted via currentError StateFlow
    }
}

// Subscription with offer
suspend fun subscribeWithOffer(sku: String, offerToken: String) {
    iapHelper.requestSubscription(
        sku = sku,
        subscriptionOffers = listOf(
            SubscriptionOfferAndroid(
                sku = sku,
                offerToken = offerToken
            )
        )
    )
}
```

### Transaction Management

```kotlin
suspend fun finishTransaction(
    purchase: Purchase,
    isConsumable: Boolean = false
): Boolean

suspend fun consumePurchase(purchaseToken: String): Boolean
```

**Example**:
```kotlin
// Handle successful purchase
scope.launch {
    iapHelper.currentPurchase.collectLatest { purchase ->
        purchase?.let {
            // Deliver content
            deliverPurchasedContent(it.productId)
            
            // Finish transaction
            val success = iapHelper.finishTransaction(
                purchase = it,
                isConsumable = true
            )
            
            if (success) {
                println("Transaction completed")
            }
        }
    }
}
```

### State Management

```kotlin
fun clearPurchase()
fun clearError()
```

**Example**:
```kotlin
// Clear states after handling
fun handlePurchaseComplete(purchase: Purchase) {
    // Process purchase
    processPurchase(purchase)
    
    // Clear the current purchase state
    iapHelper.clearPurchase()
}

fun handleError(error: PurchaseError) {
    // Show error to user
    showErrorDialog(error.message)
    
    // Clear error state
    iapHelper.clearError()
}
```

### Platform-Specific Methods

```kotlin
// iOS Methods
suspend fun presentCodeRedemptionSheetIOS()
suspend fun showManageSubscriptionsIOS()
suspend fun getStorefrontIOS(): Map<String, Any?>?
fun getStore(): Store

// Android Methods
suspend fun deepLinkToSubscriptionsAndroid(sku: String)
suspend fun requestPurchaseHistoryAndroid()
```

**Example**:
```kotlin
// Platform-specific features
when (getCurrentPlatform()) {
    IAPPlatform.IOS -> {
        // iOS specific
        Button(onClick = {
            scope.launch {
                iapHelper.presentCodeRedemptionSheetIOS()
            }
        }) {
            Text("Redeem Code")
        }
    }
    IAPPlatform.ANDROID -> {
        // Android specific
        Button(onClick = {
            scope.launch {
                iapHelper.deepLinkToSubscriptionsAndroid("premium_monthly")
            }
        }) {
            Text("Manage Subscription")
        }
    }
}
```

## Complete Implementation Example

### View Model

```kotlin
class StoreViewModel : ViewModel() {
    private val iapHelper = UseIap(
        scope = viewModelScope,
        options = UseIapOptions(
            autoFinishTransactions = false // Manual control
        )
    )
    
    data class StoreState(
        val isConnected: Boolean = false,
        val products: List<Product> = emptyList(),
        val subscriptions: List<Product> = emptyList(),
        val ownedProducts: Set<String> = emptySet(),
        val isLoading: Boolean = false,
        val error: String? = null
    )
    
    private val _state = MutableStateFlow(StoreState())
    val state: StateFlow<StoreState> = _state.asStateFlow()
    
    init {
        observeIAPStates()
        initializeStore()
    }
    
    private fun observeIAPStates() {
        // Combine all states
        combine(
            iapHelper.isConnected,
            iapHelper.products,
            iapHelper.subscriptions,
            iapHelper.availablePurchases
        ) { connected, products, subs, purchases ->
            StoreState(
                isConnected = connected,
                products = products,
                subscriptions = subs,
                ownedProducts = purchases.map { it.productId }.toSet()
            )
        }.onEach { newState ->
            _state.update { current ->
                current.copy(
                    isConnected = newState.isConnected,
                    products = newState.products,
                    subscriptions = newState.subscriptions,
                    ownedProducts = newState.ownedProducts
                )
            }
        }.launchIn(viewModelScope)
        
        // Observe purchase success
        viewModelScope.launch {
            iapHelper.currentPurchase.collectLatest { purchase ->
                purchase?.let {
                    handlePurchaseSuccess(it)
                }
            }
        }
        
        // Observe errors
        viewModelScope.launch {
            iapHelper.currentError.collectLatest { error ->
                error?.let {
                    _state.update { current ->
                        current.copy(error = it.message)
                    }
                    iapHelper.clearError()
                }
            }
        }
    }
    
    private fun initializeStore() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                // Initialize connection
                iapHelper.initConnection()
                
                // Load products
                iapHelper.getProducts(PRODUCT_SKUS)
                iapHelper.getSubscriptions(SUBSCRIPTION_SKUS)
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(error = "Failed to initialize: ${e.message}")
                }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun purchaseProduct(productId: String) {
        viewModelScope.launch {
            try {
                iapHelper.requestPurchase(
                    sku = productId,
                    obfuscatedAccountIdAndroid = getUserId()
                )
            } catch (e: Exception) {
                // Handled by error flow
            }
        }
    }
    
    private suspend fun handlePurchaseSuccess(purchase: Purchase) {
        try {
            // Verify purchase on server
            val isValid = verifyPurchaseOnServer(purchase)
            
            if (isValid) {
                // Deliver content
                deliverContent(purchase.productId)
                
                // Finish transaction
                iapHelper.finishTransaction(
                    purchase = purchase,
                    isConsumable = isConsumableProduct(purchase.productId)
                )
                
                // Clear purchase state
                iapHelper.clearPurchase()
                
                // Show success
                _state.update { 
                    it.copy(error = null)
                }
            } else {
                _state.update { 
                    it.copy(error = "Purchase verification failed")
                }
            }
        } catch (e: Exception) {
            _state.update { 
                it.copy(error = "Failed to process purchase: ${e.message}")
            }
        }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        iapHelper.dispose()
    }
    
    companion object {
        private val PRODUCT_SKUS = listOf(
            "coins_100",
            "coins_500",
            "remove_ads"
        )
        
        private val SUBSCRIPTION_SKUS = listOf(
            "premium_monthly",
            "premium_yearly"
        )
    }
}
```

### Compose UI

```kotlin
@Composable
fun StoreScreen(viewModel: StoreViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Store") },
                actions = {
                    // Connection indicator
                    Icon(
                        imageVector = if (state.isConnected) 
                            Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = "Connection status",
                        tint = if (state.isConnected) Color.Green else Color.Red
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                state.error != null -> {
                    ErrorView(
                        error = state.error,
                        onDismiss = viewModel::clearError,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                else -> {
                    LazyColumn {
                        // Products section
                        item {
                            Text(
                                "Products",
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        
                        items(state.products) { product ->
                            ProductItem(
                                product = product,
                                isOwned = product.productId in state.ownedProducts,
                                onPurchase = {
                                    viewModel.purchaseProduct(product.productId)
                                }
                            )
                        }
                        
                        // Subscriptions section
                        if (state.subscriptions.isNotEmpty()) {
                            item {
                                Text(
                                    "Subscriptions",
                                    style = MaterialTheme.typography.h6,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            
                            items(state.subscriptions) { subscription ->
                                SubscriptionItem(
                                    subscription = subscription,
                                    isActive = subscription.productId in state.ownedProducts,
                                    onSubscribe = {
                                        viewModel.purchaseProduct(subscription.productId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

## Best Practices

1. **Scope Management**: Use appropriate CoroutineScope (viewModelScope, lifecycleScope)
2. **Error Handling**: Always handle PurchaseError exceptions
3. **State Collection**: Use `collectLatest` to cancel previous collections
4. **Clear States**: Clear purchase and error states after handling
5. **Platform Checks**: Check platform before using platform-specific methods
6. **Transaction Verification**: Always verify purchases on your server

## Testing

### Mock Implementation

```kotlin
class MockUseIap : UseIap {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    override val products: StateFlow<List<Product>> = _products
    
    private val _currentPurchase = MutableStateFlow<Purchase?>(null)
    override val currentPurchase: StateFlow<Purchase?> = _currentPurchase
    
    // Test helpers
    fun emitProducts(products: List<Product>) {
        _products.value = products
    }
    
    fun emitPurchase(purchase: Purchase) {
        _currentPurchase.value = purchase
    }
}

// In tests
@Test
fun testPurchaseFlow() = runTest {
    val mockIap = MockUseIap()
    val viewModel = StoreViewModel(mockIap)
    
    // Emit test products
    mockIap.emitProducts(
        listOf(
            Product(productId = "test_product", price = "$0.99")
        )
    )
    
    // Verify state update
    assertEquals(1, viewModel.state.value.products.size)
}
```

## Migration from Flutter

Key differences when migrating from flutter_inapp_purchase:

1. **StateFlow vs Streams**: Use Kotlin StateFlow instead of Dart Streams
2. **Coroutines vs Futures**: Use suspend functions with coroutines
3. **Data Classes**: Kotlin data classes instead of Dart classes
4. **Null Safety**: Built-in Kotlin null safety
5. **Platform Detection**: Use `getCurrentPlatform()` instead of `Platform.isIOS`

## See Also

- [Core Methods](./core-methods.md) - Detailed method documentation
- [State Management](./listeners.md) - StateFlow usage patterns
- [Types](./types.md) - Data class definitions
- [Error Codes](./error-codes.md) - Error handling guide
- [Examples](../examples/basic-store.md) - Complete implementation examples