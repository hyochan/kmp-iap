---
title: Error Codes
sidebar_position: 6
---

# Error Codes

Comprehensive error handling reference for kmp-iap v1.0.0-beta.2. Understanding error codes helps implement robust error recovery and provide better user experiences.

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
    KmpIAP.purchaseErrorListener.collect { error ->
        when (error.code) {
            ErrorCode.E_USER_CANCELLED.name -> {
                    // Don't show error - user intended to cancel
                    println("Purchase cancelled by user")
                }
            else -> showErrorDialog(error.message)
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
                KmpIAP.initConnection()
                break // Success
            } catch (e: PurchaseError) {
                if (e.code != ErrorCode.E_NETWORK_ERROR.name || ++retryCount >= maxRetries) {
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
    ErrorCode.E_SERVICE_UNAVAILABLE.name -> {
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
// Handle timeout errors
when (error.code) {
    ErrorCode.E_SERVICE_TIMEOUT.name -> {
        // Retry with exponential backoff
        retryWithDelay()
    }
}
```

#### SERVICE_DISCONNECTED
**Description**: Lost connection to store service  
**Platforms**: iOS, Android  
**Recovery**: Re-initialize connection

```kotlin
scope.launch {
    KmpIAP.isConnected.collectLatest { connected ->
        if (!connected) {
            // Attempt reconnection
            delay(2000)
            try {
                KmpIAP.initConnection()
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
    val products = KmpIAP.getProducts(listOf("invalid_sku"))
} catch (e: PurchaseError) {
    when (e.code) {
        ErrorCode.E_ITEM_UNAVAILABLE.name -> {
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
    ErrorCode.E_ITEM_ALREADY_OWNED.name -> {
        showInfo("You already own this product.")
        // Refresh purchase state
        scope.launch {
            kmpIAP.getAvailablePurchases()
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
            code = ErrorCode.E_PURCHASE_INVALID.name,
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
    ErrorCode.E_DEFERRED_PAYMENT.name -> {
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
                KmpIAP.initConnection()
                initialized = true
            } catch (e: PurchaseError) {
                throw PurchaseError(
                    code = ErrorCode.E_NOT_INITIALIZED.name,
                    message = "Failed to initialize IAP",
                    underlyingError = e
                )
            }
        }
    }
    
    suspend fun loadProducts(skus: List<String>) {
        ensureInitialized()
        return KmpIAP.getProducts(skus)
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
    if (!KmpIAP.isConnected.value) {
        try {
            KmpIAP.initConnection()
        } catch (e: PurchaseError) {
            if (e.code != ErrorCode.E_NOT_INITIALIZED.name) {
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
        if (getCurrentPlatform() == IapPlatform.ANDROID) {
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
        IapPlatform.IOS -> {
            if (Build.VERSION.SDK_INT < 14) {
                throw PurchaseError(
                    code = ErrorCode.FEATURE_NOT_SUPPORTED,
                    message = "Code redemption requires iOS 14+"
                )
            }
        }
        IapPlatform.ANDROID -> {
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
            KmpIAP.currentError.collectLatest { error ->
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
            ErrorCode.E_NETWORK_ERROR.name,
            ErrorCode.E_SERVICE_UNAVAILABLE.name -> {
                showRetryableError(error)
            }
            ErrorCode.E_ITEM_ALREADY_OWNED.name -> {
                handleAlreadyOwned()
            }
            else -> {
                showGenericError(error)
            }
        }
        
        // Clear error
        kmpIAP.clearError()
    }
}
```

### 2. User-Friendly Messages

```kotlin
fun getErrorMessage(error: PurchaseError): String {
    return when (error.code) {
        ErrorCode.USER_CANCELLED -> "Purchase cancelled"
        ErrorCode.E_NETWORK_ERROR.name -> "Network connection failed. Please check your internet connection."
        ErrorCode.E_SERVICE_UNAVAILABLE.name -> "Store service is temporarily unavailable. Please try again later."
        ErrorCode.E_ITEM_UNAVAILABLE.name -> "This product is not available in your region."
        ErrorCode.E_ITEM_ALREADY_OWNED.name -> "You already own this item."
        ErrorCode.E_PURCHASE_INVALID.name -> "Purchase verification failed. Please contact support."
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
            ErrorCode.E_NETWORK_ERROR.name -> {
                attemptReconnection()
            }
            ErrorCode.E_ITEM_ALREADY_OWNED.name -> {
                refreshPurchases()
            }
            ErrorCode.E_PURCHASE_INVALID.name -> {
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
                KmpIAP.initConnection()
                return
            } catch (e: PurchaseError) {
                if (attempt == 2) throw e
            }
        }
    }
    
    private suspend fun refreshPurchases() {
        KmpIAP.getAvailablePurchases()
    }
}
```

### 4. Debug Logging

```kotlin
// Debug error details in development
if (BuildConfig.DEBUG) {
    scope.launch {
        KmpIAP.currentError.collectLatest { error ->
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
        1 -> ErrorCode.E_SERVICE_UNAVAILABLE.name
        2 -> ErrorCode.USER_CANCELLED
        3 -> ErrorCode.E_PURCHASE_INVALID.name
        4 -> ErrorCode.E_ITEM_UNAVAILABLE.name
        5 -> ErrorCode.E_NOT_INITIALIZED.name
        6 -> ErrorCode.E_NETWORK_ERROR.name
        7 -> ErrorCode.E_DEFERRED_PAYMENT.name
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
        2 -> ErrorCode.E_SERVICE_UNAVAILABLE.name
        3 -> ErrorCode.BILLING_UNAVAILABLE
        4 -> ErrorCode.E_ITEM_UNAVAILABLE.name
        5 -> ErrorCode.DEVELOPER_ERROR
        6 -> ErrorCode.UNKNOWN_ERROR
        7 -> ErrorCode.E_ITEM_ALREADY_OWNED.name
        8 -> ErrorCode.E_ITEM_UNAVAILABLE.name
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
            code = ErrorCode.E_NETWORK_ERROR.name,
            message = "Network unavailable"
        )
    )
    
    // Verify error handled
    assertTrue(errorHandler.lastError?.code == ErrorCode.E_NETWORK_ERROR.name)
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
    assertEquals(ErrorCode.E_NETWORK_ERROR.name, error?.code)
}
```

## Migration from flutter_inapp_purchase

Key differences in error handling:

1. **Structured Errors**: KMP uses `PurchaseError` class vs numeric codes
2. **Flow**: Errors emitted via `purchaseErrorListener` Flow vs callbacks
3. **Coroutines**: Error handling with try-catch vs Future error callbacks
4. **Platform Abstraction**: Unified `ErrorCode` enum vs platform-specific codes

## See Also

- [Core Methods](./core-methods.md) - Methods that may throw errors
- [State Management](./listeners.md) - Error state observation
- [Troubleshooting](../guides/troubleshooting.md) - Common error solutions
- [Examples](../examples/complete-implementation.md) - Complete error handling examples