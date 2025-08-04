---
title: Error Codes
sidebar_position: 6
---

# Error Codes

Comprehensive error handling reference for kmp-iap v1.0.0. Understanding error codes helps implement robust error recovery and provide better user experiences.

## Error Types

### PurchaseError

The main error class for all IAP-related exceptions.

```kotlin
data class PurchaseError(
    val code: ErrorCode,
    val message: String,
    val underlyingError: Throwable? = null
) : Exception(message)
```

**Properties**:
- `code` - Standardized error code enum
- `message` - Human-readable error description
- `underlyingError` - Platform-specific underlying error (optional)

## Error Code Reference

### ErrorCode Enum

```kotlin
enum class ErrorCode {
    // User Actions
    USER_CANCELLED,
    
    // Network & Service
    NETWORK_ERROR,
    SERVICE_UNAVAILABLE,
    SERVICE_TIMEOUT,
    SERVICE_DISCONNECTED,
    
    // Product & Purchase
    PRODUCT_NOT_AVAILABLE,
    PRODUCT_ALREADY_OWNED,
    PURCHASE_INVALID,
    PURCHASE_DEFERRED,
    
    // Configuration
    NOT_INITIALIZED,
    ALREADY_INITIALIZED,
    INVALID_CONFIGURATION,
    
    // Platform Specific
    BILLING_UNAVAILABLE,
    FEATURE_NOT_SUPPORTED,
    DEVELOPER_ERROR,
    
    // General
    UNKNOWN_ERROR
}
```

## Error Code Details

### User Action Errors

#### USER_CANCELLED
**Description**: User cancelled the purchase flow  
**Platforms**: iOS, Android  
**Recovery**: No action needed - expected user behavior

```kotlin
scope.launch {
    iapHelper.currentError.collectLatest { error ->
        error?.let {
            when (it.code) {
                ErrorCode.USER_CANCELLED -> {
                    // Don't show error - user intended to cancel
                    println("Purchase cancelled by user")
                }
                else -> showErrorDialog(it.message)
            }
            iapHelper.clearError()
        }
    }
}
```

### Network & Service Errors

#### NETWORK_ERROR
**Description**: Network connection failed  
**Platforms**: iOS, Android  
**Recovery**: Retry with exponential backoff

```kotlin
private fun handleNetworkError() {
    var retryCount = 0
    val maxRetries = 3
    
    scope.launch {
        while (retryCount < maxRetries) {
            delay(1000L * (2.0.pow(retryCount).toLong()))
            try {
                iapHelper.initConnection()
                break // Success
            } catch (e: PurchaseError) {
                if (e.code != ErrorCode.NETWORK_ERROR || ++retryCount >= maxRetries) {
                    showError("Network error. Please check your connection.")
                    break
                }
            }
        }
    }
}
```

#### SERVICE_UNAVAILABLE
**Description**: Store service is temporarily unavailable  
**Platforms**: iOS, Android  
**Recovery**: Retry later or check service status

```kotlin
when (error.code) {
    ErrorCode.SERVICE_UNAVAILABLE -> {
        showRetryableError(
            message = "Store service unavailable. Please try again later.",
            retryAction = { reconnectToStore() }
        )
    }
}
```

#### SERVICE_TIMEOUT
**Description**: Request to store service timed out  
**Platforms**: iOS, Android  
**Recovery**: Retry with longer timeout

```kotlin
// Configure longer timeout in options
val options = UseIapOptions(
    connectionTimeout = 60.seconds // Increase from default 30s
)
```

#### SERVICE_DISCONNECTED
**Description**: Lost connection to store service  
**Platforms**: iOS, Android  
**Recovery**: Re-initialize connection

```kotlin
scope.launch {
    iapHelper.isConnected.collectLatest { connected ->
        if (!connected) {
            // Attempt reconnection
            delay(2000)
            try {
                iapHelper.initConnection()
            } catch (e: PurchaseError) {
                scheduleReconnection()
            }
        }
    }
}
```

### Product & Purchase Errors

#### PRODUCT_NOT_AVAILABLE
**Description**: Requested product not found in store  
**Platforms**: iOS, Android  
**Common Causes**:
- Invalid product ID
- Product not approved in store
- Regional restrictions

```kotlin
try {
    val products = iapHelper.getProducts(listOf("invalid_sku"))
} catch (e: PurchaseError) {
    when (e.code) {
        ErrorCode.PRODUCT_NOT_AVAILABLE -> {
            println("Product not found. Check product IDs:")
            println("- Verify product ID matches store configuration")
            println("- Ensure product is approved and available")
            println("- Check regional availability")
        }
    }
}
```

#### PRODUCT_ALREADY_OWNED
**Description**: User already owns this non-consumable product  
**Platforms**: iOS, Android  
**Recovery**: Restore purchases or check ownership

```kotlin
when (error.code) {
    ErrorCode.PRODUCT_ALREADY_OWNED -> {
        showInfo("You already own this product.")
        // Refresh purchase state
        scope.launch {
            iapHelper.getAvailablePurchases()
        }
    }
}
```

#### PURCHASE_INVALID
**Description**: Purchase validation failed  
**Platforms**: iOS, Android  
**Common Causes**:
- Invalid receipt
- Signature verification failed
- Purchase token expired

```kotlin
// Server-side validation example
suspend fun validatePurchase(purchase: Purchase): Boolean {
    return try {
        val response = api.validatePurchase(
            productId = purchase.productId,
            purchaseToken = purchase.purchaseToken,
            receipt = purchase.transactionReceipt
        )
        response.isValid
    } catch (e: Exception) {
        throw PurchaseError(
            code = ErrorCode.PURCHASE_INVALID,
            message = "Purchase validation failed",
            underlyingError = e
        )
    }
}
```

#### PURCHASE_DEFERRED
**Description**: Purchase requires additional approval (family sharing)  
**Platforms**: iOS (Ask to Buy)  
**Recovery**: Wait for approval

```kotlin
when (error.code) {
    ErrorCode.PURCHASE_DEFERRED -> {
        showInfo(
            "Purchase is pending approval. " +
            "You'll be notified when it's approved."
        )
        // Store pending purchase for later processing
        savePendingPurchase(purchase)
    }
}
```

### Configuration Errors

#### NOT_INITIALIZED
**Description**: IAP not initialized before use  
**Platforms**: iOS, Android  
**Recovery**: Call initConnection() first

```kotlin
class IAPManager {
    private var initialized = false
    
    suspend fun ensureInitialized() {
        if (!initialized) {
            try {
                iapHelper.initConnection()
                initialized = true
            } catch (e: PurchaseError) {
                throw PurchaseError(
                    code = ErrorCode.NOT_INITIALIZED,
                    message = "Failed to initialize IAP",
                    underlyingError = e
                )
            }
        }
    }
    
    suspend fun loadProducts(skus: List<String>) {
        ensureInitialized()
        return iapHelper.getProducts(skus)
    }
}
```

#### ALREADY_INITIALIZED
**Description**: Attempted to initialize already initialized connection  
**Platforms**: iOS, Android  
**Recovery**: Use existing connection

```kotlin
// Safe initialization
suspend fun safeInitialize() {
    if (!iapHelper.isConnected.value) {
        try {
            iapHelper.initConnection()
        } catch (e: PurchaseError) {
            if (e.code != ErrorCode.ALREADY_INITIALIZED) {
                throw e
            }
            // Already initialized - continue
        }
    }
}
```

#### INVALID_CONFIGURATION
**Description**: Invalid configuration parameters  
**Platforms**: iOS, Android  
**Common Causes**:
- Missing required parameters
- Invalid SKU format
- Incorrect platform configuration

```kotlin
// Validate configuration before use
fun validateProductIds(ids: List<String>) {
    ids.forEach { id ->
        require(id.isNotBlank()) { "Product ID cannot be blank" }
        require(!id.contains(" ")) { "Product ID cannot contain spaces" }
        require(id.length <= 255) { "Product ID too long" }
    }
}
```

### Platform-Specific Errors

#### BILLING_UNAVAILABLE
**Description**: Billing service not available on device  
**Platforms**: Android  
**Common Causes**:
- Google Play Store not installed
- Play Store account not configured
- Device doesn't support billing

```kotlin
when (error.code) {
    ErrorCode.BILLING_UNAVAILABLE -> {
        if (getCurrentPlatform() == IAPPlatform.ANDROID) {
            showError(
                "Google Play Store is not available. " +
                "Please install or update Google Play Store."
            )
        }
    }
}
```

#### FEATURE_NOT_SUPPORTED
**Description**: Requested feature not supported  
**Platforms**: iOS, Android  
**Examples**:
- Subscriptions on older iOS versions
- Promotional offers on Android

```kotlin
// Check feature availability
suspend fun checkFeatureSupport() {
    when (getCurrentPlatform()) {
        IAPPlatform.IOS -> {
            if (Build.VERSION.SDK_INT < 14) {
                throw PurchaseError(
                    code = ErrorCode.FEATURE_NOT_SUPPORTED,
                    message = "Code redemption requires iOS 14+"
                )
            }
        }
        IAPPlatform.ANDROID -> {
            // Check Android feature support
        }
    }
}
```

#### DEVELOPER_ERROR
**Description**: Developer configuration error  
**Platforms**: iOS, Android  
**Common Causes**:
- Incorrect bundle ID
- Missing entitlements
- Invalid signing

```kotlin
when (error.code) {
    ErrorCode.DEVELOPER_ERROR -> {
        if (BuildConfig.DEBUG) {
            println("Developer error details:")
            println("- Check app bundle ID matches store listing")
            println("- Verify in-app purchase entitlements")
            println("- Ensure proper app signing")
            error.underlyingError?.printStackTrace()
        }
    }
}
```

### General Errors

#### UNKNOWN_ERROR
**Description**: Unexpected error occurred  
**Platforms**: iOS, Android  
**Recovery**: Log and report error

```kotlin
when (error.code) {
    ErrorCode.UNKNOWN_ERROR -> {
        // Log full error details
        logger.error("Unknown IAP error", error)
        
        // Report to crash analytics
        crashlytics.recordException(error)
        
        // Show generic error to user
        showError("An unexpected error occurred. Please try again.")
    }
}
```

## Error Handling Best Practices

### 1. Centralized Error Handler

```kotlin
class IAPErrorHandler(
    private val iapHelper: UseIap,
    private val analytics: Analytics,
    private val logger: Logger
) {
    init {
        scope.launch {
            iapHelper.currentError.collectLatest { error ->
                error?.let { handleError(it) }
            }
        }
    }
    
    private fun handleError(error: PurchaseError) {
        // Log error
        logger.error("IAP Error: ${error.code}", error)
        
        // Track in analytics
        analytics.track("iap_error", mapOf(
            "error_code" to error.code.name,
            "message" to error.message
        ))
        
        // Handle by type
        when (error.code) {
            ErrorCode.USER_CANCELLED -> {
                // Silent - user action
            }
            ErrorCode.NETWORK_ERROR,
            ErrorCode.SERVICE_UNAVAILABLE -> {
                showRetryableError(error)
            }
            ErrorCode.PRODUCT_ALREADY_OWNED -> {
                handleAlreadyOwned()
            }
            else -> {
                showGenericError(error)
            }
        }
        
        // Clear error
        iapHelper.clearError()
    }
}
```

### 2. User-Friendly Messages

```kotlin
fun getErrorMessage(error: PurchaseError): String {
    return when (error.code) {
        ErrorCode.USER_CANCELLED -> "Purchase cancelled"
        ErrorCode.NETWORK_ERROR -> "Network connection failed. Please check your internet connection."
        ErrorCode.SERVICE_UNAVAILABLE -> "Store service is temporarily unavailable. Please try again later."
        ErrorCode.PRODUCT_NOT_AVAILABLE -> "This product is not available in your region."
        ErrorCode.PRODUCT_ALREADY_OWNED -> "You already own this item."
        ErrorCode.PURCHASE_INVALID -> "Purchase verification failed. Please contact support."
        ErrorCode.BILLING_UNAVAILABLE -> "Billing service not available on this device."
        ErrorCode.DEVELOPER_ERROR -> "Configuration error. Please update the app."
        else -> "Something went wrong. Please try again."
    }
}
```

### 3. Error Recovery Strategies

```kotlin
class ErrorRecoveryManager(private val iapHelper: UseIap) {
    
    suspend fun recoverFromError(error: PurchaseError) {
        when (error.code) {
            ErrorCode.SERVICE_DISCONNECTED,
            ErrorCode.NETWORK_ERROR -> {
                attemptReconnection()
            }
            ErrorCode.PRODUCT_ALREADY_OWNED -> {
                refreshPurchases()
            }
            ErrorCode.PURCHASE_INVALID -> {
                validateAllPurchases()
            }
            else -> {
                // No automatic recovery
            }
        }
    }
    
    private suspend fun attemptReconnection() {
        repeat(3) { attempt ->
            delay(2000L * (attempt + 1))
            try {
                iapHelper.initConnection()
                return
            } catch (e: PurchaseError) {
                if (attempt == 2) throw e
            }
        }
    }
    
    private suspend fun refreshPurchases() {
        iapHelper.getAvailablePurchases()
    }
}
```

### 4. Debug Logging

```kotlin
// Debug error details in development
if (BuildConfig.DEBUG) {
    scope.launch {
        iapHelper.currentError.collectLatest { error ->
            error?.let {
                println("=====================================")
                println("IAP ERROR DETAILS")
                println("=====================================")
                println("Code: ${it.code}")
                println("Message: ${it.message}")
                println("Platform: ${getCurrentPlatform()}")
                println("Timestamp: ${System.currentTimeMillis()}")
                it.underlyingError?.let { underlying ->
                    println("Underlying error: ${underlying.message}")
                    underlying.printStackTrace()
                }
                println("=====================================")
            }
        }
    }
}
```

## Platform-Specific Error Mapping

### iOS Error Mapping

```kotlin
// StoreKit error codes to ErrorCode mapping
fun mapIOSError(code: Int): ErrorCode {
    return when (code) {
        0 -> ErrorCode.UNKNOWN_ERROR
        1 -> ErrorCode.SERVICE_UNAVAILABLE
        2 -> ErrorCode.USER_CANCELLED
        3 -> ErrorCode.PURCHASE_INVALID
        4 -> ErrorCode.PRODUCT_NOT_AVAILABLE
        5 -> ErrorCode.NOT_INITIALIZED
        6 -> ErrorCode.NETWORK_ERROR
        7 -> ErrorCode.PURCHASE_DEFERRED
        8 -> ErrorCode.FEATURE_NOT_SUPPORTED
        else -> ErrorCode.UNKNOWN_ERROR
    }
}
```

### Android Error Mapping

```kotlin
// BillingClient response codes to ErrorCode mapping
fun mapAndroidError(responseCode: Int): ErrorCode {
    return when (responseCode) {
        1 -> ErrorCode.USER_CANCELLED
        2 -> ErrorCode.SERVICE_UNAVAILABLE
        3 -> ErrorCode.BILLING_UNAVAILABLE
        4 -> ErrorCode.PRODUCT_NOT_AVAILABLE
        5 -> ErrorCode.DEVELOPER_ERROR
        6 -> ErrorCode.UNKNOWN_ERROR
        7 -> ErrorCode.PRODUCT_ALREADY_OWNED
        8 -> ErrorCode.PRODUCT_NOT_AVAILABLE
        -1 -> ErrorCode.SERVICE_DISCONNECTED
        -2 -> ErrorCode.FEATURE_NOT_SUPPORTED
        else -> ErrorCode.UNKNOWN_ERROR
    }
}
```

## Testing Error Scenarios

### Unit Testing

```kotlin
@Test
fun testErrorHandling() = runTest {
    val mockIap = MockUseIap()
    val errorHandler = IAPErrorHandler(mockIap)
    
    // Simulate network error
    mockIap.emitError(
        PurchaseError(
            code = ErrorCode.NETWORK_ERROR,
            message = "Network unavailable"
        )
    )
    
    // Verify error handled
    assertTrue(errorHandler.lastError?.code == ErrorCode.NETWORK_ERROR)
    assertTrue(errorHandler.retriedCount > 0)
}
```

### Integration Testing

```kotlin
@Test
fun testPurchaseErrorRecovery() = runTest {
    // Test purchase with network failure
    coEvery { api.purchase(any()) } throws NetworkException()
    
    val result = iapManager.purchaseProduct("test_product")
    
    // Verify error propagated correctly
    assertTrue(result.isFailure)
    val error = result.exceptionOrNull() as? PurchaseError
    assertEquals(ErrorCode.NETWORK_ERROR, error?.code)
}
```

## Migration from flutter_inapp_purchase

Key differences in error handling:

1. **Structured Errors**: KMP uses `PurchaseError` class vs numeric codes
2. **StateFlow**: Errors emitted via `currentError` StateFlow vs callbacks
3. **Coroutines**: Error handling with try-catch vs Future error callbacks
4. **Platform Abstraction**: Unified `ErrorCode` enum vs platform-specific codes

## See Also

- [Core Methods](./core-methods.md) - Methods that may throw errors
- [State Management](./listeners.md) - Error state observation
- [Troubleshooting](../troubleshooting.md) - Common error solutions
- [Examples](../examples/error-handling.md) - Complete error handling examples