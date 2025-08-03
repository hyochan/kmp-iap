package io.github.hyochan.kmpiap.types

data class ProductDetails(
    val productId: String,
    val productType: ProductType,
    val title: String,
    val name: String,
    val description: String,
    val price: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val subscriptionOfferDetails: List<SubscriptionOfferDetails>? = null
)

data class SubscriptionOfferDetails(
    val basePlanId: String,
    val offerId: String?,
    val offerTags: List<String>,
    val pricingPhases: List<PricingPhase>
)

data class PricingPhase(
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val formattedPrice: String,
    val billingPeriod: String,
    val billingCycleCount: Int,
    val recurrenceMode: RecurrenceMode
)

enum class RecurrenceMode {
    INFINITE_RECURRING,
    FINITE_RECURRING,
    NON_RECURRING
}