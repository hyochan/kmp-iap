package io.github.hyochan.kmpiap.dsl

import io.github.hyochan.kmpiap.types.*

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
    private var _type: ProductType? = null
    
    var skus: List<String>
        get() = _skus
        set(value) { _skus = value }
    
    var type: ProductType
        get() = _type ?: throw IllegalStateException("Product type must be specified")
        set(value) { _type = value }
    
    internal fun build(): Pair<List<String>, ProductType> {
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
    private var iosOptions: (IosOptionsBuilder.() -> Unit)? = null
    private var androidOptions: (AndroidOptionsBuilder.() -> Unit)? = null
    
    fun ios(block: IosOptionsBuilder.() -> Unit) {
        iosOptions = block
    }
    
    fun android(block: AndroidOptionsBuilder.() -> Unit) {
        androidOptions = block
    }
    
    internal fun build(): Triple<String, RequestPurchaseIosProps?, RequestPurchaseAndroidProps?> {
        val iosBuilder = iosOptions?.let { IosOptionsBuilder().apply(it) }
        val androidBuilder = androidOptions?.let { AndroidOptionsBuilder().apply(it) }
        
        val iosProps = iosBuilder?.build()
        val androidProps = androidBuilder?.build()
        
        // Validate that at least one platform has SKU
        if (iosProps?.sku.isNullOrEmpty() && androidProps?.skus.isNullOrEmpty()) {
            throw IllegalArgumentException("At least one platform must have SKU(s) defined")
        }
        
        // Determine primary SKU for backward compatibility
        val primarySku = iosProps?.sku ?: androidProps?.skus?.firstOrNull() 
            ?: throw IllegalArgumentException("No SKU provided")
        
        return Triple(primarySku, iosProps, androidProps)
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
    var withOffer: PaymentDiscountIOS? = null
    
    internal fun build(): RequestPurchaseIosProps? {
        val skuValue = sku ?: return null
        return RequestPurchaseIosProps(
            sku = skuValue,
            quantity = quantity,
            appAccountToken = appAccountToken,
            andDangerouslyFinishTransactionAutomatically = andDangerouslyFinishTransactionAutomatically,
            withOffer = withOffer
        )
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
    
    internal fun build(): RequestPurchaseAndroidProps? {
        if (skus.isEmpty()) return null
        return RequestPurchaseAndroidProps(
            skus = skus,
            obfuscatedAccountIdAndroid = obfuscatedAccountIdAndroid,
            obfuscatedProfileIdAndroid = obfuscatedProfileIdAndroid,
            isOfferPersonalized = isOfferPersonalized
        )
    }
}

/**
 * DSL builder for subscription request
 */
@IapDsl
class SubscriptionRequestBuilder {
    private var iosOptions: (IosOptionsBuilder.() -> Unit)? = null
    private var androidOptions: (AndroidSubscriptionOptionsBuilder.() -> Unit)? = null
    
    fun ios(block: IosOptionsBuilder.() -> Unit) {
        iosOptions = block
    }
    
    fun android(block: AndroidSubscriptionOptionsBuilder.() -> Unit) {
        androidOptions = block
    }
    
    internal fun build(): Triple<String, RequestPurchaseIosProps?, RequestSubscriptionAndroidProps?> {
        val iosBuilder = iosOptions?.let { IosOptionsBuilder().apply(it) }
        val androidBuilder = androidOptions?.let { AndroidSubscriptionOptionsBuilder().apply(it) }
        
        val iosProps = iosBuilder?.build()
        val androidProps = androidBuilder?.build()
        
        // Validate that at least one platform has SKU
        if (iosProps?.sku.isNullOrEmpty() && androidProps?.skus.isNullOrEmpty()) {
            throw IllegalArgumentException("At least one platform must have SKU(s) defined")
        }
        
        // Determine primary SKU
        val primarySku = iosProps?.sku ?: androidProps?.skus?.firstOrNull() 
            ?: throw IllegalArgumentException("No SKU provided")
        
        return Triple(primarySku, iosProps, androidProps)
    }
}

/**
 * Android subscription options builder
 */
@IapDsl
class AndroidSubscriptionOptionsBuilder {
    var skus: List<String> = emptyList()
    var obfuscatedAccountIdAndroid: String? = null
    var obfuscatedProfileIdAndroid: String? = null
    var isOfferPersonalized: Boolean? = null
    var purchaseTokenAndroid: String? = null
    var replacementModeAndroid: Int? = null
    var subscriptionOffers: List<SubscriptionOfferAndroid> = emptyList()
    
    internal fun build(): RequestSubscriptionAndroidProps? {
        if (skus.isEmpty()) return null
        return RequestSubscriptionAndroidProps(
            skus = skus,
            obfuscatedAccountIdAndroid = obfuscatedAccountIdAndroid,
            obfuscatedProfileIdAndroid = obfuscatedProfileIdAndroid,
            isOfferPersonalized = isOfferPersonalized,
            purchaseTokenAndroid = purchaseTokenAndroid,
            replacementModeAndroid = replacementModeAndroid,
            subscriptionOffers = subscriptionOffers
        )
    }
}