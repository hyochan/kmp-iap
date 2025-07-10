package io.github.hyochan.kmpiap.utils

import io.github.hyochan.kmpiap.types.IAPPlatform

/**
 * Error codes matching Flutter/Expo IAP
 */
enum class ErrorCode {
    E_UNKNOWN,
    E_USER_CANCELLED,
    E_USER_ERROR,
    E_ITEM_UNAVAILABLE,
    E_REMOTE_ERROR,
    E_NETWORK_ERROR,
    E_SERVICE_ERROR,
    E_RECEIPT_FAILED,
    E_RECEIPT_FINISHED_FAILED,
    E_NOT_PREPARED,
    E_NOT_ENDED,
    E_ALREADY_OWNED,
    E_DEVELOPER_ERROR,
    E_BILLING_RESPONSE_JSON_PARSE_ERROR,
    E_DEFERRED_PAYMENT,
    E_INTERRUPTED,
    E_IAP_NOT_AVAILABLE,
    E_PURCHASE_ERROR,
    E_SYNC_ERROR,
    E_TRANSACTION_VALIDATION_FAILED,
    E_ACTIVITY_UNAVAILABLE,
    E_ALREADY_PREPARED,
    E_PENDING,
    E_CONNECTION_CLOSED,
    E_BILLING_UNAVAILABLE,
    E_PRODUCT_ALREADY_OWNED,
    E_PURCHASE_NOT_ALLOWED,
    E_QUOTA_EXCEEDED,
    E_FEATURE_NOT_SUPPORTED,
    E_NOT_INITIALIZED,
    E_ALREADY_INITIALIZED,
    E_CLIENT_INVALID,
    E_PAYMENT_INVALID,
    E_PAYMENT_NOT_ALLOWED,
    E_STOREKIT_ORIGINAL_TRANSACTION_ID_NOT_FOUND,
    E_NOT_SUPPORTED,
    E_TRANSACTION_FAILED,
    E_TRANSACTION_INVALID,
    E_PRODUCT_NOT_FOUND,
    E_PURCHASE_FAILED,
    E_TRANSACTION_NOT_FOUND,
    E_RESTORE_FAILED,
    E_REDEEM_FAILED,
    E_NO_WINDOW_SCENE,
    E_SHOW_SUBSCRIPTIONS_FAILED,
    E_PRODUCT_LOAD_FAILED,
    E_RECEIPT_REQUEST_FAILED
}

/**
 * Error code mapping utilities
 */
object ErrorCodeUtils {
    private val iosErrorMapping = mapOf(
        ErrorCode.E_UNKNOWN to 0,
        ErrorCode.E_SERVICE_ERROR to 1,
        ErrorCode.E_USER_CANCELLED to 2,
        ErrorCode.E_USER_ERROR to 3,
        ErrorCode.E_ITEM_UNAVAILABLE to 4,
        ErrorCode.E_REMOTE_ERROR to 5,
        ErrorCode.E_NETWORK_ERROR to 6,
        ErrorCode.E_RECEIPT_FAILED to 7,
        ErrorCode.E_RECEIPT_FINISHED_FAILED to 8,
        ErrorCode.E_DEVELOPER_ERROR to 9,
        ErrorCode.E_PURCHASE_ERROR to 10,
        ErrorCode.E_SYNC_ERROR to 11,
        ErrorCode.E_DEFERRED_PAYMENT to 12,
        ErrorCode.E_TRANSACTION_VALIDATION_FAILED to 13,
        ErrorCode.E_NOT_PREPARED to 14,
        ErrorCode.E_NOT_ENDED to 15,
        ErrorCode.E_ALREADY_OWNED to 16,
        ErrorCode.E_BILLING_RESPONSE_JSON_PARSE_ERROR to 17,
        ErrorCode.E_INTERRUPTED to 18,
        ErrorCode.E_IAP_NOT_AVAILABLE to 19,
        ErrorCode.E_ACTIVITY_UNAVAILABLE to 20,
        ErrorCode.E_ALREADY_PREPARED to 21,
        ErrorCode.E_PENDING to 22,
        ErrorCode.E_CONNECTION_CLOSED to 23
    )

    private val androidErrorMapping = mapOf(
        ErrorCode.E_UNKNOWN to "E_UNKNOWN",
        ErrorCode.E_USER_CANCELLED to "E_USER_CANCELLED",
        ErrorCode.E_USER_ERROR to "E_USER_ERROR",
        ErrorCode.E_ITEM_UNAVAILABLE to "E_ITEM_UNAVAILABLE",
        ErrorCode.E_REMOTE_ERROR to "E_REMOTE_ERROR",
        ErrorCode.E_NETWORK_ERROR to "E_NETWORK_ERROR",
        ErrorCode.E_SERVICE_ERROR to "E_SERVICE_ERROR",
        ErrorCode.E_RECEIPT_FAILED to "E_RECEIPT_FAILED",
        ErrorCode.E_RECEIPT_FINISHED_FAILED to "E_RECEIPT_FINISHED_FAILED",
        ErrorCode.E_NOT_PREPARED to "E_NOT_PREPARED",
        ErrorCode.E_NOT_ENDED to "E_NOT_ENDED",
        ErrorCode.E_ALREADY_OWNED to "E_ALREADY_OWNED",
        ErrorCode.E_DEVELOPER_ERROR to "E_DEVELOPER_ERROR",
        ErrorCode.E_BILLING_RESPONSE_JSON_PARSE_ERROR to "E_BILLING_RESPONSE_JSON_PARSE_ERROR",
        ErrorCode.E_DEFERRED_PAYMENT to "E_DEFERRED_PAYMENT",
        ErrorCode.E_INTERRUPTED to "E_INTERRUPTED",
        ErrorCode.E_IAP_NOT_AVAILABLE to "E_IAP_NOT_AVAILABLE",
        ErrorCode.E_PURCHASE_ERROR to "E_PURCHASE_ERROR",
        ErrorCode.E_SYNC_ERROR to "E_SYNC_ERROR",
        ErrorCode.E_TRANSACTION_VALIDATION_FAILED to "E_TRANSACTION_VALIDATION_FAILED",
        ErrorCode.E_ACTIVITY_UNAVAILABLE to "E_ACTIVITY_UNAVAILABLE",
        ErrorCode.E_ALREADY_PREPARED to "E_ALREADY_PREPARED",
        ErrorCode.E_PENDING to "E_PENDING",
        ErrorCode.E_CONNECTION_CLOSED to "E_CONNECTION_CLOSED"
    )

    fun fromPlatformCode(platformCode: Any, platform: IAPPlatform): ErrorCode {
        return when (platform) {
            IAPPlatform.IOS -> {
                val code = platformCode as? Int ?: return ErrorCode.E_UNKNOWN
                iosErrorMapping.entries.firstOrNull { it.value == code }?.key ?: ErrorCode.E_UNKNOWN
            }
            IAPPlatform.ANDROID -> {
                val code = platformCode as? String ?: return ErrorCode.E_UNKNOWN
                androidErrorMapping.entries.firstOrNull { it.value == code }?.key ?: ErrorCode.E_UNKNOWN
            }
        }
    }

    fun toPlatformCode(errorCode: ErrorCode, platform: IAPPlatform): Any {
        return when (platform) {
            IAPPlatform.IOS -> iosErrorMapping[errorCode] ?: 0
            IAPPlatform.ANDROID -> androidErrorMapping[errorCode] ?: "E_UNKNOWN"
        }
    }

    fun isValidForPlatform(errorCode: ErrorCode, platform: IAPPlatform): Boolean {
        return when (platform) {
            IAPPlatform.IOS -> iosErrorMapping.containsKey(errorCode)
            IAPPlatform.ANDROID -> androidErrorMapping.containsKey(errorCode)
        }
    }
}