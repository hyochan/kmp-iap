package io.github.hyochan.kmpiap.types

/**
 * Store types matching Flutter/Expo IAP
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
 * Purchase type enum
 */
enum class PurchaseType {
    INAPP,
    SUBS
}

/**
 * Transaction states
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
    PENDING,
    PURCHASED,
    UNSPECIFIED
}

/**
 * Android proration modes
 */
enum class AndroidProrationMode(val value: Int) {
    UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY(0),
    IMMEDIATE_WITH_TIME_PRORATION(1),
    IMMEDIATE_AND_CHARGE_PRORATED_PRICE(2),
    IMMEDIATE_WITHOUT_PRORATION(3),
    DEFERRED(4),
    IMMEDIATE_AND_CHARGE_FULL_PRICE(5)
}

/**
 * Get the current platform
 */
expect fun getCurrentPlatform(): IAPPlatform