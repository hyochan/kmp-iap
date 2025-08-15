package io.github.hyochan.kmpiap.types

/**
 * Android-specific types matching expo-iap and flutter_inapp_purchase
 */

/**
 * Android purchase state enum
 */
enum class AndroidPurchaseState(val value: Int) {
    UNSPECIFIED_STATE(0),
    PURCHASED(1),
    PENDING(2)
}

/**
 * Android product type
 */
enum class AndroidProductType {
    INAPP,
    SUBS
}



/**
 * Android subscription offer
 */
data class SubscriptionOfferAndroid(
    val sku: String,
    val offerToken: String
)

/**
 * Android billing response codes
 */
enum class AndroidBillingResponseCode(val value: Int) {
    OK(0),
    USER_CANCELED(1),
    SERVICE_UNAVAILABLE(2),
    BILLING_UNAVAILABLE(3),
    ITEM_UNAVAILABLE(4),
    DEVELOPER_ERROR(5),
    ERROR(6),
    ITEM_ALREADY_OWNED(7),
    ITEM_NOT_OWNED(8),
    SERVICE_DISCONNECTED(-1),
    FEATURE_NOT_SUPPORTED(-2)
}