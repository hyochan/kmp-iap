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


// PaymentDiscount and PromotionalOffer are now defined in Products.kt

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