---
sidebar_position: 7
title: Lifecycle
---

# Lifecycle

Understanding and managing the in-app purchase lifecycle is crucial for creating robust and reliable purchase experiences in Kotlin Multiplatform projects.

![Purchase Lifecycle](https://kmp-iap.hyo.dev/assets/images/lifecycle-882aa01ea00089e05a08f19581d9b349.svg)

The purchase lifecycle involves multiple interconnected states and transitions, from initial store connection through purchase completion and transaction finalization. Understanding this flow helps you build resilient purchase systems that handle edge cases gracefully.

While this diagram is from expo-iap, kmp-iap follows the exact same design patterns and flow, making this lifecycle representation identical for both libraries.

## Lifecycle Overview

The in-app purchase lifecycle consists of several key phases:

1. **Store Connection** - Establishing connection with platform stores
2. **Product Loading** - Fetching available products and pricing
3. **Purchase Initiation** - User-triggered purchase requests
4. **Transaction Processing** - Platform-handled payment flow
5. **Purchase Completion** - Successful transaction receipt
6. **Content Delivery** - Providing purchased content to user
7. **Transaction Finalization** - Consuming/acknowledging purchases

Each phase has its own requirements and potential failure modes that need proper handling.

## Connection Management with KmpIAP

### Automatic Connection

The kmp-iap library manages connections through the `KmpIAP` singleton:

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import io.github.hyochan.kmpiap.*
import io.github.hyochan.kmpiap.types.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

class IAPViewModel : ViewModel() {
    private val kmpIAP = KmpIAP()
    
    data class ConnectionState(
        val isConnected: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null
    )
    
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    init {
        // Automatically initialize connection when ViewModel is created
        initConnection()
        observePurchaseEvents()
    }
    
    private fun initConnection() {
        viewModelScope.launch {
            _connectionState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val connected = kmpIAP.initConnection()
                _connectionState.update { 
                    it.copy(
                        isConnected = connected,
                        isLoading = false
                    )
                }
            } catch (e: PurchaseError) {
                _connectionState.update { 
                    it.copy(
                        error = e.message,
                        isLoading = false,
                        isConnected = false
                    )
                }
            }
        }
    }
    
    private fun observePurchaseEvents() {
        viewModelScope.launch {
            kmpIAP.purchaseErrorListener.collect { error ->
                _connectionState.update { 
                    it.copy(error = error.message)
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        kmpIAP.dispose()
    }
```

### Connection States

Monitor connection states to provide appropriate user feedback:

```kotlin
enum class IAPConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class ConnectionManager(
    private val scope: CoroutineScope
) {
    // Initialize KmpIAP in init block
    
    private val _state = MutableStateFlow(IAPConnectionState.DISCONNECTED)
    val state: StateFlow<IAPConnectionState> = _state.asStateFlow()
    
    private var _errorMessage: String? = null
    val errorMessage: String? get() = _errorMessage
    
    suspend fun connect() {
        _state.value = IAPConnectionState.CONNECTING
        
        try {
            KmpIAP.initConnection()
            // Observe connection state
            scope.launch {
                val connected = KmpIAP.isConnected()
                    _state.value = if (connected) {
                        IAPConnectionState.CONNECTED
                    } else {
                        IAPConnectionState.DISCONNECTED
                    }
                }
            }
        } catch (e: PurchaseError) {
            _state.value = IAPConnectionState.ERROR
            _errorMessage = e.message
        }
    }
}
```

## Component Lifecycle Integration

### ViewModel Integration

Integrate IAP lifecycle with Android ViewModel:

```kotlin
import androidx.lifecycle.*
import kotlinx.coroutines.flow.*

class PurchaseViewModel : ViewModel() {
    // KmpIAP is a singleton, no need to create instance
    
    data class PurchaseUiState(
        val isProcessing: Boolean = false,
        val products: List<Product> = emptyList(),
        val currentPurchase: Purchase? = null,
        val error: PurchaseError? = null
    )
    
    private val kmpIAP = KmpIAP()
    private val _uiState = MutableStateFlow(PurchaseUiState())
    val uiState: StateFlow<PurchaseUiState> = _uiState.asStateFlow()
    
    init {
        setupPurchaseObservers()
        viewModelScope.launch {
            val connected = kmpIAP.initConnection()
            if (connected) {
                checkPendingPurchases()
            }
        }
    }
    
    private fun setupPurchaseObservers() {
        // Observe purchase success
        viewModelScope.launch {
            kmpIAP.purchaseUpdatedListener.collect { purchase ->
                purchase?.let {
                    _uiState.update { state ->
                        state.copy(
                            isProcessing = false,
                            currentPurchase = it
                        )
                    }
                    handlePurchaseSuccess(it)
                }
            }
        }
        
        // Observe errors
        viewModelScope.launch {
            kmpIAP.purchaseErrorListener.collect { error ->
                error?.let {
                    _uiState.update { state ->
                        state.copy(
                            isProcessing = false,
                            error = it
                        )
                    }
                    handlePurchaseError(it)
                }
            }
        }
    }
    
    private suspend fun checkPendingPurchases() {
        // Check for pending transactions on app resume
        val purchases = KmpIAP.getAvailablePurchases()
            purchases.forEach { purchase ->
                if (!isTransactionFinished(purchase)) {
                    finishPendingTransaction(purchase)
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        kmpIAP.dispose()
    }
}
```

### Compose Integration

Using IAP with Jetpack Compose and lifecycle awareness:

```kotlin
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun PurchaseScreen(viewModel: PurchaseViewModel = viewModel()) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // App resumed - check for pending purchases
                    viewModel.checkPendingPurchases()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // App paused - save any pending state
                    viewModel.savePendingState()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // UI content
    PurchaseContent(
        uiState = uiState,
        onPurchase = viewModel::purchaseProduct
    )
}
```

## Best Practices

### ✅ Do

- **Initialize connections early** in your app lifecycle
- **Set up state observers** before making any purchase requests
- **Handle app state changes** (background/foreground transitions)
- **Implement retry logic** for failed connections
- **Clean up resources** properly in onCleared/dispose methods
- **Check for pending purchases** when app resumes
- **Validate purchases server-side** for security
- **Provide user feedback** during purchase processing
- **Handle network interruptions** gracefully
- **Test on different devices** and OS versions

```kotlin
// Good: Comprehensive lifecycle management
class GoodPurchaseManager : ViewModel() {
    // KmpIAP is a singleton
    
    init {
        setupObservers()
        ensureConnection()
    }
    
    private fun ensureConnection() {
        viewModelScope.launch {
            val connected = KmpIAP.isConnected()
            if (!connected) {
                scheduleReconnection()
            }
        }
    }
    
    fun checkPendingTransactions() {
        viewModelScope.launch {
            val purchases = KmpIAP.getAvailablePurchases()
            purchases.forEach { finishIfNeeded(it) }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        kmpIAP.dispose()
    }
}
```

### ❌ Don't

- **Make purchases without observers** set up first
- **Ignore connection state** when making requests
- **Block UI indefinitely** during purchase processing
- **Store sensitive data** in local storage
- **Trust client-side validation** alone
- **Forget to handle edge cases** (network issues, app backgrounding)
- **Leave connections open** when not needed
- **Assume purchases complete immediately**
- **Skip testing** in sandbox environments
- **Ignore platform differences**

```kotlin
// Bad: No lifecycle management
class BadPurchaseManager {
    fun makePurchase(productId: String) {
        // Bad: No connection check
        // Bad: No observers set up
        // Bad: No error handling
        GlobalScope.launch {
            KmpIAP.requestPurchase(
                UnifiedPurchaseRequest(
                    sku = productId,
                    quantity = 1
                )
            )
        }
    }
}
```

## Purchase Flow Best Practices

### Receipt Validation and Security

Always validate purchases server-side:

```kotlin
class SecurePurchaseValidator {
    suspend fun validatePurchase(purchase: Purchase): Boolean {
        return try {
            when (getCurrentPlatform()) {
                IapPlatform.IOS -> {
                    // iOS receipt validation
                    val result = api.validateIOSReceipt(
                        receipt = purchase.transactionReceipt,
                        sharedSecret = "your-shared-secret",
                        isProduction = !BuildConfig.DEBUG
                    )
                    
                    result.status == 0
                }
                IapPlatform.ANDROID -> {
                    // Android purchase validation
                    val result = api.validateAndroidPurchase(
                        packageName = BuildConfig.APPLICATION_ID,
                        productId = purchase.productId,
                        purchaseToken = purchase.purchaseToken ?: "",
                        isSubscription = false
                    )
                    
                    result.isValid
                }
            }
        } catch (e: Exception) {
            println("Validation failed: $e")
            false
        }
    }
}
```

### Purchase State Management

Track purchase states throughout the lifecycle:

```kotlin
enum class PurchaseFlowState {
    IDLE,
    LOADING,
    PROCESSING,
    VALIDATING,
    DELIVERING,
    COMPLETED,
    ERROR
}

class PurchaseStateManager(
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(PurchaseFlowState.IDLE)
    val state: StateFlow<PurchaseFlowState> = _state.asStateFlow()
    
    private var _currentProductId: String? = null
    private var _errorMessage: String? = null
    
    val currentProductId: String? get() = _currentProductId
    val errorMessage: String? get() = _errorMessage
    
    suspend fun initiatePurchase(productId: String) {
        updateState(PurchaseFlowState.LOADING, productId)
        
        try {
            // Check connection
            if (!KmpIAP.isConnected()) {
                throw PurchaseError(
                    code = ErrorCode.SERVICE_DISCONNECTED,
                    message = "Store connection lost"
                )
            }
            
            updateState(PurchaseFlowState.PROCESSING, productId)
            
            KmpIAP.requestPurchase(
                UnifiedPurchaseRequest(
                    sku = productId,
                    quantity = 1,
                    obfuscatedAccountIdAndroid = getUserId()
                )
            )
            
        } catch (e: PurchaseError) {
            updateState(
                PurchaseFlowState.ERROR,
                productId,
                e.message
            )
        }
    }
    
    private fun updateState(
        newState: PurchaseFlowState,
        productId: String? = null,
        error: String? = null
    ) {
        _state.value = newState
        _currentProductId = productId
        _errorMessage = error
    }
}
```

## Error Handling and User Experience

### Comprehensive Error Handling

```kotlin
import androidx.compose.material3.*
import androidx.compose.runtime.*

class PurchaseErrorHandler {
    fun handlePurchaseError(
        error: PurchaseError,
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope
    ) {
        val (message, actionLabel) = when (error.code) {
            ErrorCode.USER_CANCELLED -> {
                // User cancelled - no message needed
                return
            }
            ErrorCode.NETWORK_ERROR -> {
                "Network error. Please check your connection." to "Retry"
            }
            ErrorCode.SERVICE_UNAVAILABLE -> {
                "Store service unavailable. Please try later." to null
            }
            ErrorCode.PRODUCT_NOT_AVAILABLE -> {
                "This item is currently unavailable" to null
            }
            ErrorCode.DEVELOPER_ERROR -> {
                "Configuration error. Please update the app." to null
            }
            ErrorCode.PRODUCT_ALREADY_OWNED -> {
                "You already own this item" to "Restore"
            }
            else -> {
                "Purchase failed: ${error.message}" to null
            }
        }
        
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Long
            )
            
            if (result == SnackbarResult.ActionPerformed) {
                when (error.code) {
                    ErrorCode.NETWORK_ERROR -> retryLastPurchase()
                    ErrorCode.PRODUCT_ALREADY_OWNED -> restorePurchases()
                    else -> {}
                }
            }
        }
    }
    
    private suspend fun retryLastPurchase() {
        // Implement retry logic
    }
    
    private suspend fun restorePurchases() {
        // Implement restore logic
    }
}
```

## Testing and Development

### Development Environment Setup

```kotlin
object DevelopmentHelpers {
    val isDebugMode = BuildConfig.DEBUG
    
    suspend fun setupTestEnvironment() {
        if (!isDebugMode) return
        
        // Clear any existing transactions in debug mode
        try {
            println("Setting up test environment...")
            // Test products will be loaded
        } catch (e: Exception) {
            println("Failed to setup test environment: $e")
        }
    }
    
    fun logPurchaseState(state: String, data: Map<String, Any?>? = null) {
        if (!isDebugMode) return
        
        println("Purchase State: $state")
        data?.forEach { (key, value) ->
            println("  $key: $value")
        }
    }
}
```

## Common Pitfalls and Solutions

### Transaction Management Issues

**Problem**: Purchases getting stuck in pending state
```kotlin
// Solution: Implement proper transaction cleanup
class TransactionCleanup(
    private val scope: CoroutineScope
) {
    suspend fun cleanupPendingTransactions() {
        try {
            // Get all available purchases
            KmpIAP.getAvailablePurchases().forEach { purchase ->
                finalizePurchase(purchase)
            }
        } catch (e: Exception) {
            println("Error cleaning up transactions: $e")
        }
    }
    
    private suspend fun finalizePurchase(purchase: Purchase) {
        // Validate and deliver content first
        val isValid = validatePurchase(purchase)
        if (!isValid) return
        
        deliverContent(purchase)
        
        // Then finalize the transaction
        val success = KmpIAP.finishTransaction(
            purchase = purchase,
            isConsumable = isConsumable(purchase.productId)
        )
        
        if (success) {
            println("Transaction finalized: ${purchase.productId}")
        }
    }
}
```

### Security Issues

**Problem**: Client-side only validation
```kotlin
// Solution: Always validate server-side
class SecurityBestPractices {
    suspend fun secureValidation(purchase: Purchase): Boolean {
        // 1. Client-side basic checks
        if (purchase.productId.isEmpty() || 
            purchase.transactionReceipt == null) {
            return false
        }
        
        // 2. Server-side validation (critical)
        val serverValid = validateWithServer(purchase)
        if (!serverValid) return false
        
        // 3. Business logic validation
        val businessValid = validateBusinessRules(purchase)
        
        return businessValid
    }
}
```

### Development and Testing Issues

**Problem**: Different behavior in sandbox vs production
```kotlin
// Solution: Environment-aware configuration
object EnvironmentConfig {
    val isProduction = !BuildConfig.DEBUG && isProductionBuild()
    val isSandbox = BuildConfig.DEBUG || isSandboxBuild()
    
    val validationEndpoint: String
        get() = if (isProduction) {
            "https://buy.itunes.apple.com/verifyReceipt"
        } else {
            "https://sandbox.itunes.apple.com/verifyReceipt"
        }
    
    private fun isProductionBuild(): Boolean {
        // Add your production detection logic
        return false
    }
    
    private fun isSandboxBuild(): Boolean {
        // Add your sandbox detection logic  
        return true
    }
}
```

### App Lifecycle Issues

**Problem**: Purchases interrupted by app backgrounding
```kotlin
// Solution: Implement proper app lifecycle handling
class LifecycleAwarePurchaseManager(
    private val scope: CoroutineScope
) {
    private val pendingPurchases = mutableMapOf<String, PurchaseFlowState>()
    // Initialize KmpIAP in init block
    
    fun onAppResumed() {
        resumePendingPurchases()
    }
    
    fun onAppPaused() {
        savePendingPurchases()
    }
    
    private fun resumePendingPurchases() {
        // Check for any purchases that completed while app was backgrounded
        scope.launch {
            val purchases = KmpIAP.getAvailablePurchases()
                purchases.forEach { purchase ->
                    if (pendingPurchases.containsKey(purchase.productId)) {
                        finalizePurchase(purchase)
                    }
                }
            }
        }
    }
    
    private fun savePendingPurchases() {
        // Persist pending purchase state
        // This helps recover from app kills during purchase
    }
}
```

### Connection Management Issues

**Problem**: Connection drops during purchase flow
```kotlin
// Solution: Implement connection resilience
class ResilientConnectionManager(
    private val scope: CoroutineScope
) {
    suspend fun ensureConnectionWithRetry(): Boolean {
        repeat(3) { attempt ->
            try {
                KmpIAP.initConnection()
                return true
            } catch (e: PurchaseError) {
                println("Connection attempt ${attempt + 1} failed: $e")
                
                if (attempt < 2) {
                    delay(2000L * (attempt + 1))
                }
            }
        }
        
        return false
    }
    
    init {
        // Check connection and retry if needed
        scope.launch {
            val connected = KmpIAP.isConnected()
            if (!connected) {
                ensureConnectionWithRetry()
            }
        }
    }
}
```

## Next Steps

After implementing proper lifecycle management:

1. **Test thoroughly** in both sandbox and production environments
2. **Monitor purchase analytics** to identify lifecycle issues
3. **Implement proper logging** for debugging purchase flows
4. **Set up alerts** for purchase failures and anomalies
5. **Review and optimize** purchase success rates
6. **Consider advanced features** like promotional offers and subscription management

For more detailed guidance on specific purchase flows, see:
- [Purchases Guide](./purchases.md) - Complete purchase implementation
- [Offer Code Redemption](./offer-code-redemption.md) - Promotional offers
- [Troubleshooting](./troubleshooting.md) - Common issues and solutions