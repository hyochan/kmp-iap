package io.github.hyochan.kmpiap.types

import kotlinx.serialization.Serializable

/**
 * Android-specific types matching OpenIAP specification
 */

/**
 * Android purchase state enum matching OpenIAP spec
 */
@Serializable
enum class PurchaseAndroidState(val value: Int) {
    UNSPECIFIED_STATE(0),
    PURCHASED(1),
    PENDING(2)
}

/**
 * Android one-time purchase offer detail matching OpenIAP spec
 */
@Serializable
data class ProductAndroidOneTimePurchaseOfferDetail(
    val priceCurrencyCode: String,
    val formattedPrice: String,
    val priceAmountMicros: String
)

/**
 * Android pricing phase matching OpenIAP spec
 */
@Serializable
data class PricingPhaseAndroid(
    val formattedPrice: String,
    val priceCurrencyCode: String,
    val billingPeriod: String,  // P1W, P1M, P1Y
    val billingCycleCount: Int,
    val priceAmountMicros: String,
    val recurrenceMode: Int
)

/**
 * Android pricing phases matching OpenIAP spec
 */
@Serializable
data class PricingPhasesAndroid(
    val pricingPhaseList: List<PricingPhaseAndroid>
)

/**
 * Android subscription offer detail matching OpenIAP spec
 */
@Serializable
data class ProductSubscriptionAndroidOfferDetail(
    val basePlanId: String,
    val offerId: String,
    val offerToken: String,
    val offerTags: List<String>,
    val pricingPhases: PricingPhasesAndroid
)

/**
 * Android subscription offer details matching OpenIAP spec
 */
@Serializable
data class ProductSubscriptionAndroidOfferDetails(
    val basePlanId: String,
    val offerId: String?,
    val offerToken: String,
    val pricingPhases: PricingPhasesAndroid,
    val offerTags: List<String>
)

/**
 * Android subscription offer for purchase
 */
@Serializable
data class SubscriptionOfferAndroid(
    val sku: String,
    val offerToken: String
)

/**
 * Android billing response codes
 */
@Serializable
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

/**
 * Android replacement modes (proration modes)
 */
@Serializable
enum class ReplacementModeAndroid(val value: Int) {
    UNKNOWN_REPLACEMENT_MODE(0),
    IMMEDIATE_WITH_TIME_PRORATION(1),
    IMMEDIATE_AND_CHARGE_PRORATED_PRICE(2),
    IMMEDIATE_WITHOUT_PRORATION(3),
    DEFERRED(4),
    IMMEDIATE_AND_CHARGE_FULL_PRICE(5)
}