package io.github.hyochan.kmpiap.types

/**
 * Product type enum matching documentation
 */
enum class ProductType {
    INAPP,
    SUBS
}

/**
 * Base product interface following documentation spec
 */
interface ProductBase {
    val id: String  // Changed from productId to id
    val title: String
    val description: String
    val price: String
    val priceAmount: Double  // Changed from Long to Double
    val currency: String
}

/**
 * iOS-specific product fields
 */
interface ProductIOS {
    val displayName: String
    val isFamilyShareable: Boolean
    val jsonRepresentation: String?
    val discounts: List<Discount>?
    val subscription: SubscriptionInfo?
    val introductoryPriceNumberOfPeriodsIOS: String?
    val introductoryPriceSubscriptionPeriodIOS: SubscriptionPeriodIOS?
}

/**
 * Android-specific product fields
 */
interface ProductAndroid {
    val originalPrice: String?
    val originalPriceAmount: Double?
    val freeTrialPeriod: String?
    val iconUrl: String?
    val subscriptionOfferDetails: List<OfferDetail>?
    val oneTimePurchaseOfferDetails: OneTimePurchaseOfferDetails?
    val typeAndroid: String?
    val nameAndroid: String?
    val displayPriceAndroid: String?
}

/**
 * Unified Product class combining base and platform-specific fields
 */
data class Product(
    // ProductBase fields
    override val id: String,
    override val title: String,
    override val description: String,
    override val price: String,
    override val priceAmount: Double,
    override val currency: String,
    
    // iOS-specific fields (optional)
    val displayName: String? = null,
    val isFamilyShareable: Boolean = false,
    val jsonRepresentation: String? = null,
    val discounts: List<Discount>? = null,
    val subscription: SubscriptionInfo? = null,
    val introductoryPriceNumberOfPeriodsIOS: String? = null,
    val introductoryPriceSubscriptionPeriodIOS: SubscriptionPeriodIOS? = null,
    
    // Android-specific fields (optional)
    val originalPrice: String? = null,
    val originalPriceAmount: Double? = null,
    val freeTrialPeriod: String? = null,
    val iconUrl: String? = null,
    val subscriptionOfferDetails: List<OfferDetail>? = null,
    val oneTimePurchaseOfferDetails: OneTimePurchaseOfferDetails? = null,
    val typeAndroid: String? = null,
    val nameAndroid: String? = null,
    val displayPriceAndroid: String? = null,
    
    // Platform indicator
    val platform: IapPlatform = getCurrentPlatform()
) : ProductBase

/**
 * Subscription-specific product extensions
 */
data class SubscriptionProduct(
    // ProductBase fields
    override val id: String,
    override val title: String,
    override val description: String,
    override val price: String,
    override val priceAmount: Double,
    override val currency: String,
    
    // Subscription-specific fields
    val subscriptionPeriod: String,
    val introductoryPrice: String? = null,
    val introductoryPricePaymentMode: String? = null,
    val introductoryPriceNumberOfPeriods: Int? = null,
    val introductoryPriceSubscriptionPeriod: String? = null,
    
    // Platform-specific subscription fields
    val subscriptionGroupIdentifier: String? = null,  // iOS
    val promotionalOffers: List<PromotionalOffer>? = null,  // iOS
    val offerDetails: List<OfferDetail>? = null,  // Android
    val subscriptionOfferAndroid: List<SubscriptionOffer>? = null,  // Android backward compat
    
    val platform: IapPlatform = getCurrentPlatform()
) : ProductBase

/**
 * iOS Discount information
 */
data class Discount(
    val identifier: String,
    val type: String,
    val numberOfPeriods: Int,
    val price: String,
    val priceAmount: Double,
    val paymentMode: String,
    val subscriptionPeriod: String
)

/**
 * iOS Subscription info
 */
data class SubscriptionInfo(
    val subscriptionGroupIdentifier: String,
    val subscriptionPeriod: SubscriptionPeriodIOS,
    val introductoryPrice: IntroductoryPrice? = null,
    val promotionalOffers: List<PromotionalOffer>? = null
)

/**
 * iOS Introductory price
 */
data class IntroductoryPrice(
    val price: String,
    val priceAmount: Double,
    val paymentMode: String,
    val numberOfPeriods: Int,
    val subscriptionPeriod: SubscriptionPeriodIOS
)

/**
 * iOS Promotional offer
 */
data class PromotionalOffer(
    val identifier: String,
    val price: String,
    val priceAmount: Double,
    val paymentMode: String,
    val numberOfPeriods: Int,
    val subscriptionPeriod: SubscriptionPeriodIOS
)

/**
 * iOS Payment discount for offers
 */
data class PaymentDiscount(
    val identifier: String,
    val keyIdentifier: String,
    val nonce: String,
    val signature: String,
    val timestamp: Double
)

/**
 * Android One-time purchase offer details
 */
data class OneTimePurchaseOfferDetails(
    val priceCurrencyCode: String,
    val formattedPrice: String,
    val priceAmountMicros: String
)

/**
 * Android Offer detail
 */
data class OfferDetail(
    val offerId: String,
    val basePlanId: String,
    val offerToken: String,
    val pricingPhases: List<PricingPhase>,
    val offerTags: List<String>? = null
)

/**
 * Android Pricing phase
 */
data class PricingPhase(
    val billingPeriod: String,
    val formattedPrice: String,
    val priceAmountMicros: String,
    val priceCurrencyCode: String,
    val billingCycleCount: Int? = null,
    val recurrenceMode: RecurrenceMode? = null
)

/**
 * Android Subscription offer for purchase
 */
data class SubscriptionOffer(
    val sku: String,
    val offerToken: String
)

/**
 * Product request parameters
 */
data class ProductRequest(
    val skus: List<String>,
    val type: ProductType  // "inapp" or "subs"
)