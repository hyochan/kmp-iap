package io.github.hyochan.kmpiap.types

/**
 * Store types matching documentation spec
 */
enum class Store {
    NONE,
    PLAY_STORE,
    AMAZON,
    APP_STORE
}

/**
 * Platform detection enum
 */
enum class IAPPlatform {
    IOS,
    ANDROID
}

/**
 * IAP Event types matching documentation spec
 */
enum class IapEvent {
    PURCHASE_UPDATED,
    PURCHASE_ERROR,
    PROMOTED_PRODUCT_IOS
}

/**
 * Transaction states (iOS)
 */
enum class TransactionState {
    PURCHASING,
    PURCHASED,
    FAILED,
    RESTORED,
    DEFERRED
}

/**
 * Purchase states (Android)
 */
enum class PurchaseState {
    UNSPECIFIED,  // 0 - Unspecified state
    PURCHASED,    // 1 - Purchase completed
    PENDING       // 2 - Purchase pending
}

/**
 * iOS Subscription periods matching documentation spec
 */
enum class SubscriptionIosPeriod {
    P1W,  // 1 week
    P1M,  // 1 month
    P2M,  // 2 months
    P3M,  // 3 months
    P6M,  // 6 months
    P1Y   // 1 year
}

/**
 * Android Recurrence modes
 */
enum class RecurrenceMode {
    INFINITE_RECURRING,       // Charges recur forever
    FINITE_RECURRING,         // Charges recur for a fixed number of cycles
    NON_RECURRING            // Charges occur once
}

/**
 * Android Replacement modes (proration modes)
 */
enum class ReplacementMode {
    UNKNOWN_REPLACEMENT_MODE,
    IMMEDIATE_WITH_TIME_PRORATION,
    IMMEDIATE_AND_CHARGE_PRORATED_PRICE,
    IMMEDIATE_WITHOUT_PRORATION,
    DEFERRED,
    IMMEDIATE_AND_CHARGE_FULL_PRICE
}

/**
 * Get the current platform
 */
expect fun getCurrentPlatform(): IAPPlatform