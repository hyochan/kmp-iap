package io.github.hyochan.kmpiap.types

import kotlinx.serialization.Serializable

/**
 * Enums matching OpenIAP specification
 */

/**
 * Store types
 */
@Serializable
enum class Store {
    NONE,
    PLAY_STORE,
    AMAZON,
    APP_STORE
}

/**
 * IAP Event types
 */
@Serializable
enum class IapEvent {
    PURCHASE_UPDATED,
    PURCHASE_ERROR,
    PROMOTED_PRODUCT_IOS
}

/**
 * Error codes matching OpenIAP spec
 */
@Serializable
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
    E_CONNECTION_CLOSED
}