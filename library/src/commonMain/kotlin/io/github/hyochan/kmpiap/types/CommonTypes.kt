package io.github.hyochan.kmpiap.types

/**
 * Connection result for IAP service connection
 */
data class ConnectionResult(
    val connected: Boolean,
    val message: String? = null
)

/**
 * App Store information (iOS only)
 */
data class AppStoreInfo(
    val countryCode: String? = null,
    val storefront: String? = null,
    val identifier: String? = null
)

/**
 * Legacy types for backward compatibility
 */
@Deprecated("Use Product instead", ReplaceWith("Product"))
typealias BaseProduct = Product

@Deprecated("Use PurchaseError instead", ReplaceWith("PurchaseError"))
data class PurchaseResult(
    val responseCode: Int? = null,
    val debugMessage: String? = null,
    val code: String? = null,
    val message: String? = null,
    val purchaseTokenAndroid: String? = null
)

/**
 * Request parameters for fetching products
 */
@Deprecated("Use ProductRequest instead", ReplaceWith("ProductRequest"))
data class RequestProductsParams(
    val skus: List<String>,
    val type: PurchaseType = PurchaseType.INAPP
)


/**
 * Purchase type enum
 */
enum class PurchaseType {
    INAPP,
    SUBS
}

/**
 * Android proration modes (backward compatibility)
 */
@Deprecated("Use ReplacementMode instead", ReplaceWith("ReplacementMode"))
enum class AndroidProrationMode(val value: Int) {
    UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY(0),
    IMMEDIATE_WITH_TIME_PRORATION(1),
    IMMEDIATE_AND_CHARGE_PRORATED_PRICE(2),
    IMMEDIATE_WITHOUT_PRORATION(3),
    DEFERRED(4),
    IMMEDIATE_AND_CHARGE_FULL_PRICE(5)
}

/**
 * Active subscription information
 * Contains platform-specific subscription details
 */
data class ActiveSubscription(
    val productId: String,
    val isActive: Boolean,
    val expirationDateIOS: Long? = null,        // iOS only - expiration timestamp
    val autoRenewingAndroid: Boolean? = null,   // Android only  
    val environmentIOS: String? = null,         // iOS only: "Sandbox" | "Production"
    val willExpireSoon: Boolean? = null,        // True if expiring within 7 days
    val daysUntilExpirationIOS: Int? = null     // iOS only
)