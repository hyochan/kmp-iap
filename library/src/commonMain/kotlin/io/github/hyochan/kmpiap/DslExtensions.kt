package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.dsl.*
import io.github.hyochan.kmpiap.types.*

/**
 * Request products using DSL
 * 
 * Example:
 * ```kotlin
 * val products = kmpIapInstance.requestProducts {
 *     skus = listOf("product1", "product2")
 *     type = ProductType.INAPP
 * }
 * ```
 */
suspend fun KmpInAppPurchase.requestProducts(
    builder: ProductsRequestBuilder.() -> Unit
): List<Product> {
    val requestBuilder = ProductsRequestBuilder().apply(builder)
    val (skus, type) = requestBuilder.build()
    return requestProducts(ProductRequest(skus, type))
}

/**
 * Request purchase using DSL
 * 
 * Example:
 * ```kotlin
 * val purchase = kmpIapInstance.requestPurchase {
 *     ios {
 *         sku = "product_id"
 *         quantity = 1
 *     }
 *     android {
 *         skus = listOf("product_id")
 *     }
 * }
 * ```
 */
suspend fun KmpInAppPurchase.requestPurchase(
    builder: PurchaseRequestBuilder.() -> Unit
): Purchase {
    val requestBuilder = PurchaseRequestBuilder().apply(builder)
    val (primarySku, ios, android) = requestBuilder.build()
    return requestPurchase(primarySku, ios, android)
}

/**
 * Request subscription using DSL
 * 
 * Example:
 * ```kotlin
 * val subscription = kmpIapInstance.requestSubscription {
 *     ios {
 *         sku = "monthly_sub"
 *     }
 *     android {
 *         skus = listOf("monthly_sub")
 *         subscriptionOffers = listOf(...)
 *     }
 * }
 * ```
 */
suspend fun KmpInAppPurchase.requestSubscription(
    builder: SubscriptionRequestBuilder.() -> Unit
): Purchase {
    val requestBuilder = SubscriptionRequestBuilder().apply(builder)
    val (primarySku, ios, androidSub) = requestBuilder.build()
    
    // Convert subscription props to purchase props for Android
    val androidPurchase = androidSub?.let {
        RequestPurchaseAndroidProps(
            skus = it.skus,
            obfuscatedAccountIdAndroid = it.obfuscatedAccountIdAndroid,
            obfuscatedProfileIdAndroid = it.obfuscatedProfileIdAndroid,
            isOfferPersonalized = it.isOfferPersonalized
        )
    }
    
    return requestPurchase(primarySku, ios, androidPurchase)
}