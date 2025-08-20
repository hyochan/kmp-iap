package io.github.hyochan.kmpiap.types

import kotlinx.serialization.Serializable

/**
 * iOS-specific types matching OpenIAP specification
 */

/**
 * iOS transaction state
 */
@Serializable
enum class TransactionStateIOS(val value: Int) {
    PURCHASING(0),
    PURCHASED(1),
    FAILED(2),
    RESTORED(3),
    DEFERRED(4)
}

/**
 * iOS payment mode matching OpenIAP spec
 */
@Serializable
enum class PaymentModeIOS {
    FREE_TRIAL,
    PAY_AS_YOU_GO,
    PAY_UP_FRONT,
    UNKNOWN
}

/**
 * iOS subscription offer matching OpenIAP spec
 */
@Serializable
data class SubscriptionOfferIOS(
    val displayPrice: String,
    val id: String,
    val paymentMode: String,  // 'FREETRIAL' | 'PAYASYOUGO' | 'PAYUPFRONT' | ''
    val period: SubscriptionOfferPeriod,
    val periodCount: Int,
    val price: Double,
    val type: String  // 'introductory' | 'promotional'
)

@Serializable
data class SubscriptionOfferPeriod(
    val unit: String,  // 'DAY' | 'WEEK' | 'MONTH' | 'YEAR' | ''
    val value: Int
)

/**
 * iOS subscription info matching OpenIAP spec
 */
@Serializable
data class SubscriptionInfoIOS(
    val introductoryOffer: SubscriptionOfferIOS? = null,
    val promotionalOffers: List<SubscriptionOfferIOS>? = null,
    val subscriptionGroupId: String,
    val subscriptionPeriod: SubscriptionOfferPeriod
)

/**
 * iOS discount matching OpenIAP spec
 */
@Serializable
data class DiscountIOS(
    val identifier: String,
    val type: String,
    val numberOfPeriods: String,
    val price: String,
    val localizedPrice: String,
    val paymentMode: String,  // PaymentMode
    val subscriptionPeriod: String
)

/**
 * iOS payment discount for offers matching OpenIAP spec
 */
@Serializable
data class PaymentDiscountIOS(
    val identifier: String,
    val keyIdentifier: String,
    val nonce: String,
    val signature: String,
    val timestamp: Double
)

/**
 * StoreKit 2 specific types - AppTransaction matching OpenIAP spec
 */
@Serializable
data class AppTransactionIOS(
    val appTransactionId: String? = null,
    val originalPlatform: String? = null,
    val bundleId: String,
    val appVersion: String,
    val originalAppVersion: String,
    val originalPurchaseDate: Double,
    val deviceVerification: String,
    val deviceVerificationNonce: String,
    val environment: String,
    val signedDate: Double,
    val appId: Long? = null,
    val appVersionId: Long? = null,
    val preorderDate: Double? = null
)

/**
 * iOS transaction reason matching OpenIAP spec
 */
@Serializable
enum class TransactionReasonIOS {
    PURCHASE,
    RENEWAL
}

/**
 * iOS ownership type
 */
@Serializable
enum class OwnershipTypeIOS {
    PURCHASED,
    FAMILY_SHARED
}

/**
 * iOS offer type
 */
@Serializable
enum class OfferTypeIOS {
    INTRODUCTORY,
    PROMOTIONAL,
    SUBSCRIPTION_OFFER_CODE
}

/**
 * iOS environment
 */
@Serializable
enum class EnvironmentIOS {
    SANDBOX,
    PRODUCTION
}

/**
 * iOS product type
 */
@Serializable
enum class ProductTypeIOS {
    CONSUMABLE,
    NON_CONSUMABLE,
    NON_RENEWING_SUBSCRIPTION,
    AUTO_RENEWABLE_SUBSCRIPTION
}