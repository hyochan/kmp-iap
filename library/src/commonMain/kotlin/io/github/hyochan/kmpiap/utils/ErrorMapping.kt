package io.github.hyochan.kmpiap.utils

import io.github.hyochan.kmpiap.types.IapPlatform

/**
 * Error codes matching OpenIAP specification
 * https://openiap.dev/docs/errors
 * 
 * Complete list of 27 standardized error codes for in-app purchases
 */
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
    
    // Platform-Specific Errors
    E_PENDING,                        // Purchase is pending approval (Android)
    E_NOT_ENDED,                      // Transaction not finished (iOS)
    E_NOT_PREPARED,                   // Store connection not initialized
    E_ALREADY_PREPARED,               // Store connection already initialized
    E_BILLING_RESPONSE_JSON_PARSE_ERROR, // Failed to parse billing response (Android)
    E_PURCHASE_ERROR,                 // General purchase error
    E_ACTIVITY_UNAVAILABLE            // Activity context not available (Android)
}

/**
 * Error code mapping utilities
 */
object ErrorCodeUtils {
    /**
     * iOS error code mapping based on OpenIAP specification
     * Maps StoreKit error codes to OpenIAP error codes
     */
    private val iosErrorMapping = mapOf(
        // OpenIAP Core Error Codes
        ErrorCode.E_UNKNOWN to 0,
        ErrorCode.E_USER_CANCELLED to 1,
        ErrorCode.E_NETWORK_ERROR to 2,
        ErrorCode.E_ITEM_UNAVAILABLE to 3,
        ErrorCode.E_SERVICE_ERROR to 4,
        ErrorCode.E_RECEIPT_FAILED to 5,
        ErrorCode.E_ALREADY_OWNED to 6,
        ErrorCode.E_PRODUCT_NOT_AVAILABLE to 7,
        ErrorCode.E_PRODUCT_ALREADY_OWNED to 8,
        ErrorCode.E_RECEIPT_FINISHED to 9,
        ErrorCode.E_NOT_ENDED to 10,
        ErrorCode.E_DEVELOPER_ERROR to 11,
        ErrorCode.E_USER_ERROR to 12,
        ErrorCode.E_REMOTE_ERROR to 13,
        ErrorCode.E_PENDING to 14,
        ErrorCode.E_RECEIPT_FINISHED_FAILED to 15,
        ErrorCode.E_NOT_PREPARED to 16,
        ErrorCode.E_BILLING_RESPONSE_JSON_PARSE_ERROR to 17,
        ErrorCode.E_DEFERRED_PAYMENT to 18,
        ErrorCode.E_INTERRUPTED to 19,
        ErrorCode.E_IAP_NOT_AVAILABLE to 20,
        ErrorCode.E_PURCHASE_ERROR to 21,
        ErrorCode.E_SYNC_ERROR to 22,
        ErrorCode.E_TRANSACTION_VALIDATION_FAILED to 23,
        ErrorCode.E_ACTIVITY_UNAVAILABLE to 24,
        ErrorCode.E_ALREADY_PREPARED to 25,
        ErrorCode.E_CONNECTION_CLOSED to 26
    )


    /**
     * Convert platform-specific error code to OpenIAP error code
     * 
     * iOS: Maps StoreKit error codes (Int) to OpenIAP error codes
     * Android: Maps Google Play Billing response codes (String) to OpenIAP error codes
     */
    fun fromPlatformCode(platformCode: Any, platform: IapPlatform): ErrorCode {
        return when (platform) {
            IapPlatform.IOS -> {
                when (val code = platformCode as? Int) {
                    0 -> ErrorCode.E_UNKNOWN
                    1 -> ErrorCode.E_USER_CANCELLED
                    2 -> ErrorCode.E_NETWORK_ERROR
                    3 -> ErrorCode.E_ITEM_UNAVAILABLE
                    4 -> ErrorCode.E_SERVICE_ERROR
                    5 -> ErrorCode.E_RECEIPT_FAILED
                    6 -> ErrorCode.E_ALREADY_OWNED
                    else -> ErrorCode.E_UNKNOWN
                }
            }
            IapPlatform.ANDROID -> {
                val code = platformCode as? String ?: return ErrorCode.E_UNKNOWN
                try {
                    ErrorCode.valueOf(code)
                } catch (e: IllegalArgumentException) {
                    ErrorCode.E_UNKNOWN
                }
            }
        }
    }

    fun toPlatformCode(errorCode: ErrorCode, platform: IapPlatform): Any {
        return when (platform) {
            IapPlatform.IOS -> iosErrorMapping[errorCode] ?: 0
            IapPlatform.ANDROID -> errorCode.name
        }
    }

    /**
     * Check if an error code is valid for a specific platform
     */
    fun isValidForPlatform(errorCode: ErrorCode, platform: IapPlatform): Boolean {
        return when (platform) {
            IapPlatform.IOS -> iosErrorMapping.containsKey(errorCode)
            IapPlatform.ANDROID -> true // All error codes are valid for Android since we use the enum name directly
        }
    }
    
    /**
     * Get a user-friendly error message for an error code
     */
    fun getErrorMessage(errorCode: ErrorCode): String {
        return when (errorCode) {
            ErrorCode.E_UNKNOWN -> "Unknown error occurred"
            ErrorCode.E_USER_CANCELLED -> "Purchase cancelled by user"
            ErrorCode.E_USER_ERROR -> "User-related error during purchase"
            ErrorCode.E_ITEM_UNAVAILABLE -> "Product not available in store"
            ErrorCode.E_PRODUCT_NOT_AVAILABLE -> "Product SKU not found"
            ErrorCode.E_PRODUCT_ALREADY_OWNED -> "Non-consumable product already purchased"
            ErrorCode.E_ALREADY_OWNED -> "Item already owned by user"
            ErrorCode.E_NETWORK_ERROR -> "Network connection error"
            ErrorCode.E_SERVICE_ERROR -> "Store service error"
            ErrorCode.E_REMOTE_ERROR -> "Remote server error"
            ErrorCode.E_RECEIPT_FAILED -> "Receipt validation failed"
            ErrorCode.E_RECEIPT_FINISHED -> "Receipt already processed"
            ErrorCode.E_PENDING -> "Purchase is pending approval"
            ErrorCode.E_NOT_ENDED -> "Transaction not finished"
            ErrorCode.E_DEVELOPER_ERROR -> "Developer configuration error"
            ErrorCode.E_RECEIPT_FINISHED_FAILED -> "Failed to finish receipt processing"
            ErrorCode.E_NOT_PREPARED -> "Store connection not initialized"
            ErrorCode.E_BILLING_RESPONSE_JSON_PARSE_ERROR -> "Failed to parse billing response"
            ErrorCode.E_DEFERRED_PAYMENT -> "Payment was deferred"
            ErrorCode.E_INTERRUPTED -> "Purchase flow was interrupted"
            ErrorCode.E_IAP_NOT_AVAILABLE -> "In-app purchase service not available"
            ErrorCode.E_PURCHASE_ERROR -> "General purchase error"
            ErrorCode.E_SYNC_ERROR -> "Synchronization error with store"
            ErrorCode.E_TRANSACTION_VALIDATION_FAILED -> "Transaction validation failed"
            ErrorCode.E_ACTIVITY_UNAVAILABLE -> "Activity context not available"
            ErrorCode.E_ALREADY_PREPARED -> "Store connection already initialized"
            ErrorCode.E_CONNECTION_CLOSED -> "Connection to store service was closed"
        }
    }
}