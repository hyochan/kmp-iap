---
title: Error Codes
sidebar_position: 6
---

import IapKitBanner from '@site/src/uis/IapKitBanner';

# Error Codes

<IapKitBanner />

Comprehensive error handling reference for kmp-iap. The library follows the OpenIAP specification for standardized error codes across platforms.

## Error Types

### PurchaseError

The main error class for all IAP-related exceptions.

```kotlin
data class PurchaseError(
    val code: String,  // Error code from ErrorCode enum
    val message: String,
    val productId: String? = null
) : Exception(message)
```

**Properties**:
- `code` - Standardized error code string (from ErrorCode enum)
- `message` - Human-readable error description
- `productId` - Related product ID (optional)

## OpenIAP Error Code Reference

kmp-iap implements all 30 standard OpenIAP error codes for consistent error handling across platforms.

### ErrorCode Enum

```kotlin
enum class ErrorCode {
    // General Errors
    E_UNKNOWN,                        // Unknown error occurred
    E_DEVELOPER_ERROR,                // Developer configuration error
    
    // User Action Errors  
    E_USER_CANCELLED,                 // User cancelled the purchase flow
    E_USER_ERROR,                     // User-related error during purchase
    E_DEFERRED_PAYMENT,               // Payment was deferred (pending family approval, etc.)
    E_INTERRUPTED,                    // Purchase flow was interrupted
    
    // Product Errors
    E_ITEM_UNAVAILABLE,               // Product not available in store
    E_PRODUCT_NOT_AVAILABLE,          // Product SKU not found
    E_PRODUCT_ALREADY_OWNED,          // Non-consumable product already purchased
    E_ALREADY_OWNED,                  // Item already owned by user
    
    // Network & Service Errors
    E_NETWORK_ERROR,                  // Network connection error
    E_SERVICE_ERROR,                  // Store service error
    E_REMOTE_ERROR,                   // Remote server error
    E_CONNECTION_CLOSED,              // Connection to store service was closed
    E_IAP_NOT_AVAILABLE,              // In-app purchase service not available
    E_SYNC_ERROR,                     // Synchronization error with store
    
    // Validation Errors
    E_RECEIPT_FAILED,                 // Receipt validation failed
    E_RECEIPT_FINISHED,               // Receipt already processed/finished
    E_RECEIPT_FINISHED_FAILED,        // Failed to finish receipt processing
    E_TRANSACTION_VALIDATION_FAILED,  // Transaction validation failed
    E_PURCHASE_VERIFICATION_FAILED,   // Purchase verification with provider failed
    E_PURCHASE_VERIFICATION_FINISHED, // Purchase verification completed
    E_PURCHASE_VERIFICATION_FINISH_FAILED, // Failed to complete verification
    
    // Platform-Specific Errors
    E_PENDING,                        // Purchase is pending approval (Android)
    E_NOT_ENDED,                      // Transaction not finished (iOS)
    E_NOT_PREPARED,                   // Store connection not initialized
    E_ALREADY_PREPARED,               // Store connection already initialized
    E_BILLING_RESPONSE_JSON_PARSE_ERROR, // Failed to parse billing response (Android)
    E_PURCHASE_ERROR,                 // General purchase error
    E_ACTIVITY_UNAVAILABLE            // Activity context not available (Android)
}
```

## Error Code Details

### General Errors

#### E_UNKNOWN
**Description**: Unknown error occurred  
**Platforms**: iOS, Android  
**Recovery**: Log and report error for debugging

```kotlin
when (error.code) {
    ErrorCode.Unknown.name -> {
        // Log full error details
        logger.error("Unknown IAP error", error)
        showError("An unexpected error occurred. Please try again.")
    }
}
```

#### E_DEVELOPER_ERROR
**Description**: Developer configuration error  
**Platforms**: iOS, Android  
**Common Causes**:
- Incorrect bundle ID
- Missing entitlements
- Invalid signing
- Product not configured in store

```kotlin
when (error.code) {
    ErrorCode.DeveloperError.name -> {
        if (BuildConfig.DEBUG) {
            println("Developer error - check configuration:")
            println("- App bundle ID matches store listing")
            println("- In-app purchase entitlements enabled")
            println("- Products configured in store console")
        }
    }
}
```

### User Action Errors

#### E_USER_CANCELLED
**Description**: User cancelled the purchase flow  
**Platforms**: iOS, Android  
**Recovery**: No action needed - expected user behavior

```kotlin
kmpIapInstance.purchaseErrorListener.collect { error ->
    when (error.code) {
        ErrorCode.UserCancelled.name -> {
            // Don't show error - user intended to cancel
            println("Purchase cancelled by user")
        }
        else -> showErrorDialog(error.message)
    }
}
```

#### E_USER_ERROR
**Description**: User-related error during purchase  
**Platforms**: iOS, Android  
**Common Causes**:
- User not signed in
- Parental controls active
- Payment method issues

#### E_DEFERRED_PAYMENT
**Description**: Payment was deferred (pending family approval, etc.)  
**Platforms**: iOS (Ask to Buy), Android (Pending purchases)  
**Recovery**: Wait for approval

```kotlin
when (error.code) {
    ErrorCode.DeferredPayment.name -> {
        showInfo("Purchase is pending approval. You'll be notified when approved.")
        // Store pending purchase for later processing
        savePendingPurchase(purchase)
    }
}
```

#### E_INTERRUPTED
**Description**: Purchase flow was interrupted  
**Platforms**: iOS, Android  
**Recovery**: Retry the purchase

### Product Errors

#### E_ITEM_UNAVAILABLE
**Description**: Product not available in store  
**Platforms**: iOS, Android  
**Common Causes**:
- Product not approved
- Regional restrictions
- Product removed from store

#### E_PRODUCT_NOT_AVAILABLE
**Description**: Product SKU not found  
**Platforms**: iOS, Android  
**Common Causes**:
- Invalid product ID
- Typo in SKU
- Product not yet published

```kotlin
try {
    val products = kmpIapInstance.requestProducts(listOf("invalid_sku"))
} catch (e: PurchaseError) {
    when (e.code) {
        ErrorCode.ItemUnavailable.name -> {
            println("Product not found. Check product ID configuration.")
        }
    }
}
```

#### E_PRODUCT_ALREADY_OWNED
**Description**: Non-consumable product already purchased  
**Platforms**: iOS, Android  
**Recovery**: Restore purchases

#### E_ALREADY_OWNED
**Description**: Item already owned by user  
**Platforms**: iOS, Android  
**Recovery**: Check purchase history or restore

```kotlin
when (error.code) {
    ErrorCode.AlreadyOwned.name,
    ErrorCode.AlreadyOwned.name -> {
        showInfo("You already own this product.")
        // Refresh purchase state
        kmpIapInstance.getAvailablePurchases()
    }
}
```

### Network & Service Errors

#### E_NETWORK_ERROR
**Description**: Network connection error  
**Platforms**: iOS, Android  
**Recovery**: Retry with exponential backoff

```kotlin
private suspend fun handleNetworkError() {
    var retryCount = 0
    val maxRetries = 3
    
    while (retryCount < maxRetries) {
        delay(1000L * (2.0.pow(retryCount).toLong()))
        try {
            kmpIapInstance.initConnection()
            break // Success
        } catch (e: PurchaseError) {
            if (e.code != ErrorCode.NetworkError.name || ++retryCount >= maxRetries) {
                showError("Network error. Please check your connection.")
                break
            }
        }
    }
}
```

#### E_SERVICE_ERROR
**Description**: Store service error  
**Platforms**: iOS, Android  
**Recovery**: Retry later or check service status

#### E_REMOTE_ERROR
**Description**: Remote server error  
**Platforms**: iOS, Android  
**Common Causes**:
- Backend validation server down
- API timeout
- Server configuration issues

#### E_CONNECTION_CLOSED
**Description**: Connection to store service was closed  
**Platforms**: iOS, Android  
**Recovery**: Re-initialize connection

#### E_IAP_NOT_AVAILABLE
**Description**: In-app purchase service not available  
**Platforms**: iOS, Android  
**Common Causes**:
- IAP disabled on device
- Restricted user account
- Store app not installed (Android)

#### E_SYNC_ERROR
**Description**: Synchronization error with store  
**Platforms**: iOS, Android  
**Recovery**: Retry synchronization

### Validation Errors

#### E_PURCHASE_VERIFICATION_FAILED
**Description**: Purchase verification with external provider failed
**Platforms**: iOS, Android
**Common Causes**:
- Invalid API key for verification provider
- Network error during verification
- Provider service unavailable

```kotlin
when (error.code) {
    ErrorCode.PurchaseVerificationFailed.name -> {
        println("Verification failed - check IAPKit API key and network")
        // Fall back to local validation or retry
    }
}
```

#### E_PURCHASE_VERIFICATION_FINISHED
**Description**: Purchase verification completed successfully
**Platforms**: iOS, Android
**Note**: This is an informational code, not an error

#### E_PURCHASE_VERIFICATION_FINISH_FAILED
**Description**: Failed to complete purchase verification process
**Platforms**: iOS, Android
**Recovery**: Retry verification

#### E_RECEIPT_FAILED (Deprecated)
**Description**: Receipt validation failed
**Platforms**: iOS, Android
**Status**: ⚠️ **Deprecated** - Use `E_PURCHASE_VERIFICATION_FAILED` instead

#### E_RECEIPT_FINISHED (Deprecated)
**Description**: Receipt already processed/finished
**Platforms**: iOS, Android
**Status**: ⚠️ **Deprecated** - Use `E_PURCHASE_VERIFICATION_FINISHED` instead

#### E_RECEIPT_FINISHED_FAILED (Deprecated)
**Description**: Failed to finish receipt processing
**Platforms**: iOS, Android
**Status**: ⚠️ **Deprecated** - Use `E_PURCHASE_VERIFICATION_FINISH_FAILED` instead

:::info Migration Note
The `E_RECEIPT_*` error codes are deprecated and replaced by `E_PURCHASE_VERIFICATION_*` codes:

| Deprecated | Replacement |
|------------|-------------|
| `E_RECEIPT_FAILED` | `E_PURCHASE_VERIFICATION_FAILED` |
| `E_RECEIPT_FINISHED` | `E_PURCHASE_VERIFICATION_FINISHED` |
| `E_RECEIPT_FINISHED_FAILED` | `E_PURCHASE_VERIFICATION_FINISH_FAILED` |

The new codes better reflect the provider-based verification approach used by IAPKit and other verification services.
:::

#### E_TRANSACTION_VALIDATION_FAILED
**Description**: Transaction validation failed  
**Platforms**: iOS, Android  
**Common Causes**:
- Transaction data corrupted
- Validation server error
- Invalid transaction state

### Platform-Specific Errors

#### E_PENDING (Android)
**Description**: Purchase is pending approval  
**Platform**: Android  
**Recovery**: Wait for purchase to be approved

```kotlin
when (error.code) {
    ErrorCode.Pending.name -> {
        if (getCurrentPlatform() == Platform.ANDROID) {
            showInfo("Purchase is pending. Check back later.")
        }
    }
}
```

#### E_NOT_ENDED (iOS)
**Description**: Transaction not finished  
**Platform**: iOS  
**Recovery**: Call finishTransaction()

```kotlin
when (error.code) {
    ErrorCode.NotEnded.name -> {
        // Finish the pending transaction
        kmpIapInstance.finishTransaction(purchase)
    }
}
```

#### E_NOT_PREPARED
**Description**: Store connection not initialized  
**Platforms**: iOS, Android  
**Recovery**: Call initConnection() first

```kotlin
class IAPManager {
    suspend fun ensureInitialized() {
        if (!kmpIapInstance.isInitialized()) {
            try {
                kmpIapInstance.initConnection()
            } catch (e: PurchaseError) {
                if (e.code == ErrorCode.NotPrepared.name) {
                    throw PurchaseError(
                        code = ErrorCode.NotPrepared.name,
                        message = "Failed to initialize IAP connection"
                    )
                }
            }
        }
    }
}
```

#### E_ALREADY_PREPARED
**Description**: Store connection already initialized  
**Platforms**: iOS, Android  
**Recovery**: Use existing connection

#### E_BILLING_RESPONSE_JSON_PARSE_ERROR (Android)
**Description**: Failed to parse billing response  
**Platform**: Android  
**Common Causes**:
- Corrupted response from Google Play
- Library version mismatch

#### E_PURCHASE_ERROR
**Description**: General purchase error  
**Platforms**: iOS, Android  
**Recovery**: Check specific error details

#### E_ACTIVITY_UNAVAILABLE (Android)
**Description**: Activity context not available  
**Platform**: Android  
**Common Causes**:
- App in background
- Activity destroyed
- No active activity

```kotlin
when (error.code) {
    ErrorCode.ActivityUnavailable.name -> {
        showError("Please ensure the app is in foreground and try again.")
    }
}
```

## Error Handling Best Practices

### 1. Centralized Error Handler

```kotlin
class IAPErrorHandler(
    private val kmpIap: KmpIAP
) {
    init {
        scope.launch {
            kmpIap.purchaseErrorListener.collect { error ->
                handleError(error)
            }
        }
    }
    
    private fun handleError(error: PurchaseError) {
        // Log error
        logger.error("IAP Error: ${error.code}", error)
        
        // Handle by type
        when (error.code) {
            ErrorCode.UserCancelled.name -> {
                // Silent - user action
            }
            ErrorCode.NetworkError.name,
            ErrorCode.ServiceError.name -> {
                showRetryableError(error)
            }
            ErrorCode.AlreadyOwned.name,
            ErrorCode.AlreadyOwned.name -> {
                handleAlreadyOwned()
            }
            else -> {
                showGenericError(error)
            }
        }
    }
}
```

### 2. User-Friendly Messages

```kotlin
fun getErrorMessage(error: PurchaseError): String {
    return ErrorCodeUtils.getErrorMessage(
        ErrorCode.valueOf(error.code)
    )
}
```

### 3. Error Recovery Strategies

```kotlin
class ErrorRecoveryManager(private val kmpIap: KmpIAP) {
    
    suspend fun recoverFromError(error: PurchaseError) {
        when (error.code) {
            ErrorCode.ConnectionClosed.name,
            ErrorCode.NetworkError.name -> {
                attemptReconnection()
            }
            ErrorCode.AlreadyOwned.name,
            ErrorCode.AlreadyOwned.name -> {
                refreshPurchases()
            }
            ErrorCode.PurchaseVerificationFailed.name,
            ErrorCode.TransactionValidationFailed.name -> {
                revalidatePurchases()
            }
            ErrorCode.NotPrepared.name -> {
                kmpIap.initConnection()
            }
        }
    }
    
    private suspend fun attemptReconnection() {
        repeat(3) { attempt ->
            delay(2000L * (attempt + 1))
            try {
                kmpIap.initConnection()
                return
            } catch (e: PurchaseError) {
                if (attempt == 2) throw e
            }
        }
    }
}
```

### 4. Debug Logging

```kotlin
if (BuildConfig.DEBUG) {
    scope.launch {
        kmpIapInstance.purchaseErrorListener.collect { error ->
            println("=====================================")
            println("IAP ERROR DETAILS")
            println("=====================================")
            println("Code: ${error.code}")
            println("Message: ${error.message}")
            println("Product ID: ${error.productId}")
            println("Platform: ${getCurrentPlatform()}")
            println("Timestamp: ${System.currentTimeMillis()}")
            println("=====================================")
        }
    }
}
```

## Platform Error Code Mapping

The library automatically maps platform-specific error codes to OpenIAP standard codes:

### iOS (StoreKit) Mapping
- Uses integer codes from StoreKit
- Mapped via `ErrorCodeUtils.fromPlatformCode()`

### Android (Google Play Billing) Mapping  
- Uses string error codes directly (matching enum names)
- Direct conversion via `ErrorCode.valueOf()`

## Testing Error Scenarios

### Unit Testing

```kotlin
@Test
fun testErrorHandling() = runTest {
    val error = PurchaseError(
        code = ErrorCode.NetworkError.name,
        message = "Network unavailable"
    )
    
    // Verify error code
    assertEquals(ErrorCode.NetworkError.name, error.code)
    
    // Verify error message utility
    val message = ErrorCodeUtils.getErrorMessage(ErrorCode.NetworkError)
    assertEquals("Network connection error", message)
}
```

### Integration Testing

```kotlin
@Test
fun testPurchaseErrorRecovery() = runTest {
    // Simulate network failure
    val error = PurchaseError(
        code = ErrorCode.NetworkError.name,
        message = "Connection failed"
    )
    
    val recoveryManager = ErrorRecoveryManager(kmpIap)
    recoveryManager.recoverFromError(error)
    
    // Verify reconnection attempted
    assertTrue(kmpIap.isInitialized())
}
```

## OpenIAP Specification Compliance

kmp-iap fully implements the [OpenIAP error specification](https://openiap.dev/docs/errors), ensuring:

- **Consistent error codes** across all platforms
- **Standardized error messages** for better UX
- **Compatible with expo-iap** and other OpenIAP implementations
- **30 standard error codes** covering all IAP scenarios

## Migration Notes

### From Previous Versions
If migrating from older versions of kmp-iap, note that error codes now follow the OpenIAP standard. Update your error handling code to use the new `ErrorCode` enum values.

### From Other Libraries
- **expo-iap**: Error codes are identical (both follow OpenIAP)
- **flutter_inapp_purchase**: Similar error codes with minor naming differences
- **Native SDKs**: Platform codes are automatically mapped to OpenIAP codes

## See Also

- [OpenIAP Error Specification](https://openiap.dev/docs/errors)
- [Core Methods](./core-methods.md) - Methods that may throw errors
- [Event Listeners](./listeners.md) - Error event handling
- [Troubleshooting](../guides/troubleshooting.md) - Common error solutions