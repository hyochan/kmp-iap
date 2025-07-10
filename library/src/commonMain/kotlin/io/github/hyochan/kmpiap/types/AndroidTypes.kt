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
 * Android-specific purchase request
 */
data class RequestPurchaseAndroid(
    override val sku: String,
    override val skus: List<String>,
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    val isOfferPersonalized: Boolean = false,
    override val platform: IAPPlatform? = IAPPlatform.ANDROID
) : RequestPurchase(), RequestPurchaseBase {
    constructor(
        skus: List<String>,
        obfuscatedAccountIdAndroid: String? = null,
        obfuscatedProfileIdAndroid: String? = null,
        isOfferPersonalized: Boolean = false
    ) : this(
        sku = skus.first(),
        skus = skus,
        obfuscatedAccountIdAndroid = obfuscatedAccountIdAndroid,
        obfuscatedProfileIdAndroid = obfuscatedProfileIdAndroid,
        isOfferPersonalized = isOfferPersonalized
    )
}

/**
 * Android-specific subscription request
 */
data class RequestSubscriptionAndroid(
    override val sku: String,
    override val skus: List<String>,
    val subscriptionOffers: List<SubscriptionOfferAndroid>? = null,
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    val purchaseTokenAndroid: String? = null,
    val replacementMode: AndroidProrationMode? = null,
    val isOfferPersonalized: Boolean = false,
    override val platform: IAPPlatform? = IAPPlatform.ANDROID
) : RequestPurchase(), RequestPurchaseBase {
    constructor(
        skus: List<String>,
        subscriptionOffers: List<SubscriptionOfferAndroid>? = null,
        obfuscatedAccountIdAndroid: String? = null,
        obfuscatedProfileIdAndroid: String? = null,
        purchaseTokenAndroid: String? = null,
        replacementMode: AndroidProrationMode? = null,
        isOfferPersonalized: Boolean = false
    ) : this(
        sku = skus.first(),
        skus = skus,
        subscriptionOffers = subscriptionOffers,
        obfuscatedAccountIdAndroid = obfuscatedAccountIdAndroid,
        obfuscatedProfileIdAndroid = obfuscatedProfileIdAndroid,
        purchaseTokenAndroid = purchaseTokenAndroid,
        replacementMode = replacementMode,
        isOfferPersonalized = isOfferPersonalized
    )
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