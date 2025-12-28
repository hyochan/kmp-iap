package io.github.hyochan.kmpiap.dsl

import io.github.hyochan.kmpiap.openiap.AndroidSubscriptionOfferInput
import io.github.hyochan.kmpiap.openiap.ProductQueryType
import io.github.hyochan.kmpiap.openiap.ProductType
import io.github.hyochan.kmpiap.openiap.DeveloperBillingOptionParamsAndroid
import io.github.hyochan.kmpiap.openiap.RequestPurchaseAndroidProps
import io.github.hyochan.kmpiap.openiap.RequestPurchaseIosProps
import io.github.hyochan.kmpiap.openiap.RequestPurchaseProps
import io.github.hyochan.kmpiap.openiap.RequestPurchasePropsByPlatforms
import io.github.hyochan.kmpiap.openiap.RequestSubscriptionAndroidProps
import io.github.hyochan.kmpiap.openiap.RequestSubscriptionIosProps
import io.github.hyochan.kmpiap.openiap.RequestSubscriptionPropsByPlatforms
import io.github.hyochan.kmpiap.openiap.DiscountOfferInputIOS

/**
 * DSL marker for type-safe builders
 */
@DslMarker
annotation class IapDsl

/**
 * DSL builder for products request
 */
@IapDsl
class ProductsRequestBuilder {
    private var _skus: List<String> = emptyList()
    private var _type: ProductQueryType? = null
    
    var skus: List<String>
        get() = _skus
        set(value) { _skus = value }
    
    var type: ProductQueryType
        get() = _type ?: throw IllegalStateException("Product type must be specified")
        set(value) { _type = value }

    internal fun build(): Pair<List<String>, ProductQueryType> {
        if (_skus.isEmpty()) {
            throw IllegalArgumentException("SKUs list cannot be empty")
        }
        if (_type == null) {
            throw IllegalArgumentException("Product type must be specified")
        }
        return Pair(_skus, _type!!)
    }
}

/**
 * DSL builder for purchase request
 */
@IapDsl
class PurchaseRequestBuilder {
    var type: ProductType = ProductType.InApp

    private var iosOptions: (IosOptionsBuilder.() -> Unit)? = null
    private var androidOptions: (AndroidOptionsBuilder.() -> Unit)? = null

    fun ios(block: IosOptionsBuilder.() -> Unit) {
        iosOptions = block
    }

    /**
     * Alias for ios() - use apple {} as an alternative to ios {}
     */
    fun apple(block: IosOptionsBuilder.() -> Unit) {
        iosOptions = block
    }

    fun android(block: AndroidOptionsBuilder.() -> Unit) {
        androidOptions = block
    }

    /**
     * Alias for android() - use google {} as an alternative to android {}
     * Recommended over android {} as of openiap-google v1.3.15
     */
    fun google(block: AndroidOptionsBuilder.() -> Unit) {
        androidOptions = block
    }

    internal fun build(): RequestPurchaseProps {
        val iosBuilt = iosOptions?.let { IosOptionsBuilder().apply(it).build() }
        val androidBuilt = androidOptions?.let { AndroidOptionsBuilder().apply(it).build() }

        val queryType = when (type) {
            ProductType.InApp -> ProductQueryType.InApp
            ProductType.Subs -> ProductQueryType.Subs
        }

        val request = when (queryType) {
            ProductQueryType.InApp -> {
                val purchasePlatforms = RequestPurchasePropsByPlatforms(
                    android = androidBuilt?.purchase,
                    ios = iosBuilt?.purchase
                )

                if (purchasePlatforms.android == null && purchasePlatforms.ios == null) {
                    throw IllegalArgumentException("At least one platform must declare purchase options")
                }

                RequestPurchaseProps.Request.Purchase(purchasePlatforms)
            }
            ProductQueryType.Subs -> {
                val subscriptionPlatforms = RequestSubscriptionPropsByPlatforms(
                    android = androidBuilt?.subscription,
                    ios = iosBuilt?.subscription
                )

                if (subscriptionPlatforms.android == null && subscriptionPlatforms.ios == null) {
                    throw IllegalArgumentException("At least one platform must declare subscription options")
                }

                RequestPurchaseProps.Request.Subscription(subscriptionPlatforms)
            }
            ProductQueryType.All -> throw IllegalArgumentException("Product type ALL is not supported for purchases")
        }

        return RequestPurchaseProps(
            request = request,
            type = queryType
        )
    }
}

/**
 * iOS options builder for purchase request
 */
@IapDsl
class IosOptionsBuilder {
    var sku: String? = null
    var quantity: Int? = null
    var appAccountToken: String? = null
    var andDangerouslyFinishTransactionAutomatically: Boolean? = null
    var withOffer: DiscountOfferInputIOS? = null
    /**
     * Attribution tracking data for StoreKit 2's Product.PurchaseOption.custom API.
     * Added in openiap-apple v1.3.7
     */
    var advancedCommerceData: String? = null

    internal fun build(): BuiltIosOptions? {
        val skuValue = sku ?: return null
        val purchase = RequestPurchaseIosProps(
            sku = skuValue,
            quantity = quantity,
            appAccountToken = appAccountToken,
            andDangerouslyFinishTransactionAutomatically = andDangerouslyFinishTransactionAutomatically,
            withOffer = withOffer,
            advancedCommerceData = advancedCommerceData
        )
        val subscription = RequestSubscriptionIosProps(
            sku = skuValue,
            quantity = quantity,
            appAccountToken = appAccountToken,
            andDangerouslyFinishTransactionAutomatically = andDangerouslyFinishTransactionAutomatically,
            withOffer = withOffer,
            advancedCommerceData = advancedCommerceData
        )
        return BuiltIosOptions(purchase = purchase, subscription = subscription)
    }
}

/**
 * Android options builder for purchase request
 */
@IapDsl
class AndroidOptionsBuilder {
    var skus: List<String> = emptyList()
    var obfuscatedAccountIdAndroid: String? = null
    var obfuscatedProfileIdAndroid: String? = null
    var isOfferPersonalized: Boolean? = null
    var purchaseTokenAndroid: String? = null
    var replacementModeAndroid: Int? = null
    var subscriptionOffers: List<AndroidSubscriptionOfferInput> = emptyList()
    /**
     * Developer billing option for External Payments (Billing Library 8.3.0+, Japan only).
     * When set, the purchase dialog shows side-by-side choice between Google Play
     * and developer's external payment option.
     */
    var developerBillingOption: DeveloperBillingOptionParamsAndroid? = null

    internal fun build(): BuiltAndroidOptions? {
        if (skus.isEmpty()) return null

        val purchase = RequestPurchaseAndroidProps(
            skus = skus,
            obfuscatedAccountIdAndroid = obfuscatedAccountIdAndroid,
            obfuscatedProfileIdAndroid = obfuscatedProfileIdAndroid,
            isOfferPersonalized = isOfferPersonalized,
            developerBillingOption = developerBillingOption
        )

        val subscription = RequestSubscriptionAndroidProps(
            skus = skus,
            obfuscatedAccountIdAndroid = obfuscatedAccountIdAndroid,
            obfuscatedProfileIdAndroid = obfuscatedProfileIdAndroid,
            isOfferPersonalized = isOfferPersonalized,
            purchaseTokenAndroid = purchaseTokenAndroid,
            replacementModeAndroid = replacementModeAndroid,
            subscriptionOffers = if (subscriptionOffers.isNotEmpty()) subscriptionOffers else null,
            developerBillingOption = developerBillingOption
        )

        return BuiltAndroidOptions(purchase = purchase, subscription = subscription)
    }
}

internal data class BuiltIosOptions(
    val purchase: RequestPurchaseIosProps?,
    val subscription: RequestSubscriptionIosProps?
)

internal data class BuiltAndroidOptions(
    val purchase: RequestPurchaseAndroidProps?,
    val subscription: RequestSubscriptionAndroidProps?
)
