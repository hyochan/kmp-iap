package io.github.hyochan.kmpiap.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Product types matching OpenIAP specification
 */

/**
 * Base product common fields matching OpenIAP spec
 */
interface ProductCommon {
    val id: String
    val title: String
    val description: String
    val type: ProductType
    val displayName: String?
    val displayPrice: String
    val currency: String
    val price: Double?
    val debugDescription: String?
    val platform: String?
}

/**
 * iOS Product matching OpenIAP spec
 */
@Serializable
data class ProductIOS(
    // ProductCommon fields
    override val id: String,
    override val title: String,
    override val description: String,
    override val type: ProductType,
    override val displayName: String? = null,
    override val displayPrice: String,
    override val currency: String,
    override val price: Double? = null,
    override val debugDescription: String? = null,
    override val platform: String = "ios",
    
    // iOS-specific fields
    val displayNameIOS: String,
    val isFamilyShareableIOS: Boolean,
    val jsonRepresentationIOS: String,
    val subscriptionInfoIOS: SubscriptionInfoIOS? = null,
    
    // Deprecated fields for backward compatibility
    @Deprecated("Use displayNameIOS instead", ReplaceWith("displayNameIOS"))
    val isFamilyShareable: Boolean? = null,
    @Deprecated("Use jsonRepresentationIOS instead", ReplaceWith("jsonRepresentationIOS"))
    val jsonRepresentation: String? = null,
    @Deprecated("Use subscriptionInfoIOS instead", ReplaceWith("subscriptionInfoIOS"))
    val subscription: SubscriptionInfoIOS? = null,
    val introductoryPriceNumberOfPeriodsIOS: String? = null,
    val introductoryPriceSubscriptionPeriodIOS: String? = null
) : ProductCommon

/**
 * iOS Product Subscription matching OpenIAP spec
 */
@Serializable
data class ProductSubscriptionIOS(
    // ProductCommon fields
    override val id: String,
    override val title: String,
    override val description: String,
    override val type: ProductType = ProductType.SUBS,
    override val displayName: String? = null,
    override val displayPrice: String,
    override val currency: String,
    override val price: Double? = null,
    override val debugDescription: String? = null,
    override val platform: String = "ios",
    
    // iOS-specific fields
    val displayNameIOS: String,
    val isFamilyShareableIOS: Boolean,
    val jsonRepresentationIOS: String,
    val subscriptionInfoIOS: SubscriptionInfoIOS? = null,
    
    // Subscription-specific iOS fields
    val discountsIOS: List<DiscountIOS>? = null,
    val introductoryPriceIOS: String? = null,
    val introductoryPriceAsAmountIOS: String? = null,
    val introductoryPricePaymentModeIOS: String? = null,
    val introductoryPriceNumberOfPeriodsIOS: String? = null,
    val introductoryPriceSubscriptionPeriodIOS: String? = null,
    val subscriptionPeriodNumberIOS: String? = null,
    val subscriptionPeriodUnitIOS: String? = null,
    
    // Deprecated fields
    @Deprecated("Use discountsIOS instead", ReplaceWith("discountsIOS"))
    val discounts: List<DiscountIOS>? = null,
    @Deprecated("Use introductoryPriceIOS instead", ReplaceWith("introductoryPriceIOS"))
    val introductoryPrice: String? = null
) : ProductCommon

/**
 * Android Product matching OpenIAP spec
 */
@Serializable
data class ProductAndroid(
    // ProductCommon fields
    override val id: String,
    override val title: String,
    override val description: String,
    override val type: ProductType,
    override val displayName: String? = null,
    override val displayPrice: String,
    override val currency: String,
    override val price: Double? = null,
    override val debugDescription: String? = null,
    override val platform: String = "android",
    
    // Android-specific fields
    val nameAndroid: String,
    val oneTimePurchaseOfferDetailsAndroid: ProductAndroidOneTimePurchaseOfferDetail? = null,
    val subscriptionOfferDetailsAndroid: List<ProductSubscriptionAndroidOfferDetail>? = null,
    
    // Deprecated fields
    @Deprecated("Use nameAndroid instead", ReplaceWith("nameAndroid"))
    val name: String? = null,
    @Deprecated("Use oneTimePurchaseOfferDetailsAndroid instead", ReplaceWith("oneTimePurchaseOfferDetailsAndroid"))
    val oneTimePurchaseOfferDetails: ProductAndroidOneTimePurchaseOfferDetail? = null,
    @Deprecated("Use subscriptionOfferDetailsAndroid instead", ReplaceWith("subscriptionOfferDetailsAndroid"))
    val subscriptionOfferDetails: List<ProductSubscriptionAndroidOfferDetail>? = null
) : ProductCommon

/**
 * Android Product Subscription matching OpenIAP spec
 */
@Serializable
data class ProductSubscriptionAndroid(
    // ProductCommon fields
    override val id: String,
    override val title: String,
    override val description: String,
    override val type: ProductType = ProductType.SUBS,
    override val displayName: String? = null,
    override val displayPrice: String,
    override val currency: String,
    override val price: Double? = null,
    override val debugDescription: String? = null,
    override val platform: String = "android",
    
    // Android-specific fields
    val nameAndroid: String,
    val oneTimePurchaseOfferDetailsAndroid: ProductAndroidOneTimePurchaseOfferDetail? = null,
    val subscriptionOfferDetailsAndroid: List<ProductSubscriptionAndroidOfferDetails>,
    
    // Deprecated fields
    @Deprecated("Use subscriptionOfferDetailsAndroid instead", ReplaceWith("subscriptionOfferDetailsAndroid"))
    val subscriptionOfferDetails: List<ProductSubscriptionAndroidOfferDetails>? = null
) : ProductCommon

/**
 * Type aliases for legacy naming - backward compatibility
 */
@Deprecated("Use ProductSubscriptionIOS instead", ReplaceWith("ProductSubscriptionIOS"))
typealias SubscriptionProductIOS = ProductSubscriptionIOS

@Deprecated("Use ProductSubscriptionAndroid instead", ReplaceWith("ProductSubscriptionAndroid"))
typealias SubscriptionProductAndroid = ProductSubscriptionAndroid

/**
 * Union type helpers for cross-platform usage
 */
typealias Product = ProductCommon
typealias SubscriptionProduct = ProductCommon