package io.github.hyochan.kmpiap.types

import kotlinx.datetime.Instant

/**
 * iOS-specific types matching expo-iap and flutter_inapp_purchase
 */

/**
 * iOS transaction state
 */
enum class IosTransactionState(val value: Int) {
    PURCHASING(0),
    PURCHASED(1),
    FAILED(2),
    RESTORED(3),
    DEFERRED(4)
}

/**
 * iOS-specific purchase request
 */
data class RequestPurchaseIOS(
    override val sku: String,
    val andDangerouslyFinishTransactionAutomaticallyIOS: Boolean = false,
    val appAccountToken: String? = null,
    val quantity: Int? = null,
    val withOffer: PaymentDiscount? = null,
    override val platform: IAPPlatform? = IAPPlatform.IOS
) : RequestPurchase()

/**
 * iOS-specific subscription request
 */
data class RequestSubscriptionIOS(
    override val sku: String,
    val andDangerouslyFinishTransactionAutomaticallyIOS: Boolean = false,
    val appAccountToken: String? = null,
    val quantity: Int? = null,
    val withOffer: PaymentDiscount? = null,
    override val platform: IAPPlatform? = IAPPlatform.IOS
) : RequestPurchase()

/**
 * iOS payment discount
 */
data class PaymentDiscount(
    val identifier: String,
    val keyIdentifier: String,
    val nonce: String,
    val signature: String,
    val timestamp: Long
)

/**
 * iOS promotional offer
 */
data class PromotionalOffer(
    val productId: String,
    val username: String,
    val offerIdentifier: String? = null
)

/**
 * StoreKit 2 specific types
 */
data class AppTransaction(
    val appAppleId: String,
    val bundleId: String,
    val deviceVerification: String,
    val deviceVerificationNonce: String,
    val originalAppVersion: String,
    val originalPurchaseDate: Instant,
    val receiptCreationDate: Instant,
    val receiptType: String,
    val signedDate: Instant
)

/**
 * iOS subscription period unit
 */
enum class IosSubscriptionPeriodUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

/**
 * iOS discount payment mode
 */
enum class IosDiscountPaymentMode {
    PAY_AS_YOU_GO,
    PAY_UP_FRONT,
    FREE_TRIAL
}

/**
 * iOS discount type
 */
enum class IosDiscountType {
    INTRODUCTORY,
    SUBSCRIPTION
}