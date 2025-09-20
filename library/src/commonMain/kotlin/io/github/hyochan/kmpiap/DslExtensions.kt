package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.dsl.*
import io.github.hyochan.kmpiap.openiap.*

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
    val request = requestBuilder.build()
    return requestPurchase(request)
}
