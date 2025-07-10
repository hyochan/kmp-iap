package io.github.hyochan.kmpiap.types

import kotlinx.datetime.Instant

/**
 * Product type enum
 */
enum class ProductType {
    INAPP,
    SUBS
}

/**
 * Base product interface
 */
interface BaseProduct {
    val productId: String
    val price: String
    val currency: String?
    val localizedPrice: String?
    val title: String?
    val description: String?
    val platform: IAPPlatform
}

/**
 * Product class for non-subscription items
 */
data class Product(
    override val productId: String,
    override val price: String,
    override val currency: String? = null,
    override val localizedPrice: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val platform: IAPPlatform = getCurrentPlatform(),
    val type: ProductType = ProductType.INAPP,
    val priceAmountMicros: Long = 0,
    val isFamilyShareable: Boolean? = null,
    // Android-specific fields
    val iconUrl: String? = null,
    val originalJson: Map<String, Any>? = null,
    val originalPrice: String? = null,
    val priceMicros: Long? = null,
    val discountPrice: String? = null,
    // iOS-specific fields
    val discountsIOS: List<DiscountIOS>? = null
) : BaseProduct

/**
 * Subscription class
 */
data class Subscription(
    override val productId: String,
    override val price: String,
    override val currency: String? = null,
    override val localizedPrice: String? = null,
    override val title: String? = null,
    override val description: String? = null,
    override val platform: IAPPlatform = getCurrentPlatform(),
    val type: ProductType = ProductType.SUBS,
    val priceAmountMicros: Long = 0,
    val subscriptionOfferAndroid: List<SubscriptionOffer>? = null,
    val subscriptionOfferDetails: List<SubscriptionOffer>? = null,
    val subscriptionPeriodAndroid: String? = null,
    val subscriptionPeriodUnitIOS: String? = null,
    val subscriptionPeriodNumberIOS: Int? = null,
    val isFamilyShareable: Boolean? = null,
    val subscriptionGroupId: String? = null,
    val introductoryPrice: String? = null,
    val introductoryPriceNumberOfPeriodsIOS: Int? = null,
    val introductoryPriceSubscriptionPeriod: String? = null,
    val originalJson: Map<String, Any>? = null,
    val originalPrice: String? = null,
    val iconUrl: String? = null
) : BaseProduct

/**
 * iOS discount information
 */
data class DiscountIOS(
    val identifier: String?,
    val type: String?,
    val numberOfPeriods: String?,
    val price: Double?,
    val localizedPrice: String?,
    val paymentMode: String?,
    val subscriptionPeriod: String?
)

/**
 * Subscription offer details
 */
data class SubscriptionOffer(
    val offerId: String?,
    val basePlanId: String?,
    val offerToken: String?,
    val pricingPhases: List<PricingPhase>?
)

/**
 * Pricing phase
 */
data class PricingPhase(
    val price: String? = null,
    val formattedPrice: String? = null,
    val currency: String? = null,
    val currencyCode: String? = null,
    val billingCycleCount: Int? = null,
    val billingPeriod: String? = null,
    val recurrenceMode: Int? = null,
    val priceAmountMicros: Long? = null
)