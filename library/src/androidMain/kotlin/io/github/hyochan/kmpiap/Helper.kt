package io.github.hyochan.kmpiap

import android.app.Activity
import android.app.Application
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsResult
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import io.github.hyochan.kmpiap.openiap.ErrorCode
import io.github.hyochan.kmpiap.openiap.FetchProductsResult
import io.github.hyochan.kmpiap.openiap.IapPlatform
import io.github.hyochan.kmpiap.openiap.Product
import io.github.hyochan.kmpiap.openiap.ProductAndroid
import io.github.hyochan.kmpiap.openiap.ProductAndroidOneTimePurchaseOfferDetail
import io.github.hyochan.kmpiap.openiap.ProductRequest
import io.github.hyochan.kmpiap.openiap.ProductSubscriptionAndroid
import io.github.hyochan.kmpiap.openiap.ProductSubscriptionAndroidOfferDetails
import io.github.hyochan.kmpiap.openiap.ProductType
import io.github.hyochan.kmpiap.openiap.PricingPhaseAndroid
import io.github.hyochan.kmpiap.openiap.PricingPhasesAndroid
import io.github.hyochan.kmpiap.openiap.Purchase
import io.github.hyochan.kmpiap.openiap.PurchaseAndroid
import io.github.hyochan.kmpiap.openiap.PurchaseError
import io.github.hyochan.kmpiap.openiap.PurchaseState
import kotlinx.coroutines.flow.MutableSharedFlow

internal const val ANDROID_VERSION = "KMP-IAP v1.0.0-alpha02 (Android)"

internal fun emitFailureAndThrow(
    errorFlow: MutableSharedFlow<PurchaseError>,
    error: PurchaseError
): Nothing {
    errorFlow.tryEmit(error)
    throw PurchaseException(error)
}

internal fun mapFetchResultToProductsHelper(
    params: ProductRequest,
    @Suppress("UNUSED_PARAMETER")
    result: FetchProductsResult,
    cache: Map<String, ProductDetails>
): List<Product> = params.skus.flatMap { sku ->
    cache[sku]?.let { listOf(it.toProduct()) } ?: emptyList()
}

internal fun clearProductCache(cache: MutableMap<String, ProductDetails>) {
    cache.clear()
}

internal fun ensureConnectedOrFail(
    isConnected: Boolean,
    fail: (PurchaseError) -> Nothing
) {
    if (!isConnected) {
        fail(
            PurchaseError(
                code = ErrorCode.ServiceError,
                message = "Not connected to billing service"
            )
        )
    }
}

internal fun isPurchaseTokenValid(purchase: Purchase): Boolean =
    purchase.purchaseToken?.isNotEmpty() == true

internal fun mapBillingResponseCode(responseCode: Int): ErrorCode = when (responseCode) {
    BillingClient.BillingResponseCode.USER_CANCELED -> ErrorCode.UserCancelled
    BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> ErrorCode.ServiceError
    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> ErrorCode.BillingUnavailable
    BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> ErrorCode.ItemUnavailable
    BillingClient.BillingResponseCode.DEVELOPER_ERROR -> ErrorCode.DeveloperError
    BillingClient.BillingResponseCode.ERROR -> ErrorCode.Unknown
    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> ErrorCode.AlreadyOwned
    BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> ErrorCode.ItemNotOwned
    BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> ErrorCode.ServiceDisconnected
    BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> ErrorCode.FeatureNotSupported
    else -> ErrorCode.Unknown
}

internal fun enablePendingPurchasesCompat(builder: BillingClient.Builder): BillingClient.Builder {
    return try {
        val paramsClass = Class.forName("com.android.billingclient.api.PendingPurchasesParams")
        val newBuilder = paramsClass.getMethod("newBuilder").invoke(null)
        val enableOneTime = newBuilder.javaClass.getMethod("enableOneTimeProducts").invoke(newBuilder) ?: newBuilder
        val enableSubscriptions = runCatching {
            enableOneTime.javaClass.getMethod("enableSubscriptionProducts").invoke(enableOneTime)
        }.getOrNull() ?: enableOneTime
        val params = enableSubscriptions.javaClass.getMethod("build").invoke(enableSubscriptions)
        builder.javaClass.getMethod("enablePendingPurchases", paramsClass).invoke(builder, params)
        builder
    } catch (throwable: Throwable) {
        runCatching { builder.javaClass.getMethod("enablePendingPurchases").invoke(builder) }
        println("[KMP-IAP] Pending purchase support unavailable: ${throwable.message ?: "unknown"}")
        builder
    }
}

internal fun tryCaptureApplication(
    callback: Application.ActivityLifecycleCallbacks,
    onContextAvailable: (Context?) -> Unit,
    onActivityFound: (Activity?) -> Unit
) {
    runCatching {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val currentActivityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
        val getApplication = activityThreadClass.getMethod("getApplication")
        val app = getApplication.invoke(currentActivityThread) as? Application
        onContextAvailable(app?.applicationContext)
        app?.registerActivityLifecycleCallbacks(callback)

        val activitiesField = activityThreadClass.getDeclaredField("mActivities")
        activitiesField.isAccessible = true
        val activities = activitiesField.get(currentActivityThread) as? Map<*, *>
        activities?.values?.forEach { value ->
            val recordClass = value?.javaClass
            val activityField = recordClass?.getDeclaredField("activity")
            activityField?.isAccessible = true
            val activity = activityField?.get(value) as? Activity
            if (activity != null && !activity.isFinishing) {
                onActivityFound(activity)
                return@forEach
            }
        }
    }
}

internal suspend fun loadProductDetails(
    client: BillingClient,
    productType: String,
    skus: List<String>,
    cache: MutableMap<String, ProductDetails>,
    errorFlow: MutableSharedFlow<PurchaseError>
): Map<String, ProductDetails>? {
    val details = mutableMapOf<String, ProductDetails>()
    skus.forEach { sku ->
        cache[sku]?.takeIf { it.productType == productType }?.let { details[sku] = it }
    }

    val missing = skus.filterNot(details::containsKey)
    if (missing.isNotEmpty()) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                missing.map { sku ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(sku)
                        .setProductType(productType)
                        .build()
                }
            )
            .build()

        val success = suspendCancellableCoroutine<Boolean> { continuation ->
            client.queryProductDetailsAsync(params) { billingResult: BillingResult, queryResult: QueryProductDetailsResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryResult.productDetailsList?.forEach { detail -> cache[detail.productId] = detail }
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            }
        }

        if (!success) {
            errorFlow.tryEmit(
                PurchaseError(code = ErrorCode.QueryProduct, message = "Failed to query product details")
            )
            return null
        }

        skus.forEach { sku ->
            cache[sku]?.takeIf { it.productType == productType }?.let { details[sku] = it }
        }
    }

    if (details.size != skus.size) {
        val missingSku = skus.firstOrNull { !details.containsKey(it) }.orEmpty()
        errorFlow.tryEmit(
            PurchaseError(code = ErrorCode.SkuNotFound, message = "Product not found: $missingSku")
        )
        return null
    }

    return details
}

internal fun com.android.billingclient.api.Purchase.toPurchase(): Purchase {
    val purchaseStateEnum = when (purchaseState) {
        com.android.billingclient.api.Purchase.PurchaseState.PURCHASED -> PurchaseState.Purchased
        com.android.billingclient.api.Purchase.PurchaseState.PENDING -> PurchaseState.Pending
        com.android.billingclient.api.Purchase.PurchaseState.UNSPECIFIED_STATE -> PurchaseState.Unknown
        else -> PurchaseState.Unknown
    }

    val accountIdentifiers = accountIdentifiers

    return PurchaseAndroid(
        autoRenewingAndroid = isAutoRenewing,
        dataAndroid = originalJson,
        developerPayloadAndroid = null,
        id = orderId ?: purchaseToken,
        ids = products,
        isAcknowledgedAndroid = isAcknowledged,
        isAutoRenewing = isAutoRenewing,
        obfuscatedAccountIdAndroid = accountIdentifiers?.obfuscatedAccountId,
        obfuscatedProfileIdAndroid = accountIdentifiers?.obfuscatedProfileId,
        packageNameAndroid = packageName,
        platform = IapPlatform.Android,
        productId = products.firstOrNull() ?: "",
        purchaseState = purchaseStateEnum,
        purchaseToken = purchaseToken,
        quantity = quantity,
        signatureAndroid = signature,
        transactionDate = purchaseTime.toDouble() / 1000
    )
}

internal fun ProductDetails.toProduct(): Product {
    val oneTime = oneTimePurchaseOfferDetails
    val offers = subscriptionOfferDetails

    val pricingPhase = offers?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()

    val productType = if (!offers.isNullOrEmpty()) ProductType.Subs else ProductType.InApp
    val displayPrice = when {
        oneTime != null -> oneTime.formattedPrice
        pricingPhase != null -> pricingPhase.formattedPrice
        else -> ""
    }
    val priceValue = when {
        oneTime != null -> oneTime.priceAmountMicros.toDouble() / 1_000_000
        pricingPhase != null -> pricingPhase.priceAmountMicros.toDouble() / 1_000_000
        else -> null
    }
    val currencyCode = when {
        oneTime != null -> oneTime.priceCurrencyCode
        pricingPhase != null -> pricingPhase.priceCurrencyCode
        else -> "USD"
    }

    return ProductAndroid(
        currency = currencyCode,
        description = description,
        displayPrice = displayPrice,
        id = productId,
        nameAndroid = name,
        oneTimePurchaseOfferDetailsAndroid = oneTime?.let {
            ProductAndroidOneTimePurchaseOfferDetail(
                formattedPrice = it.formattedPrice,
                priceAmountMicros = it.priceAmountMicros.toString(),
                priceCurrencyCode = it.priceCurrencyCode
            )
        },
        platform = IapPlatform.Android,
        price = priceValue,
        subscriptionOfferDetailsAndroid = offers?.map { it.toOfferDetail() },
        title = title,
        type = productType
    )
}

internal fun ProductDetails.toSubscriptionProduct(): ProductSubscriptionAndroid? {
    val product = toProduct() as? ProductAndroid ?: return null
    val offers = product.subscriptionOfferDetailsAndroid ?: return null
    return ProductSubscriptionAndroid(
        currency = product.currency,
        debugDescription = product.debugDescription,
        description = product.description,
        displayName = product.displayName,
        displayPrice = product.displayPrice,
        id = product.id,
        nameAndroid = product.nameAndroid,
        oneTimePurchaseOfferDetailsAndroid = product.oneTimePurchaseOfferDetailsAndroid,
        platform = product.platform,
        price = product.price,
        subscriptionOfferDetailsAndroid = offers,
        title = product.title,
        type = product.type
    )
}

internal fun ProductDetails.SubscriptionOfferDetails.toOfferDetail(): ProductSubscriptionAndroidOfferDetails {
    return ProductSubscriptionAndroidOfferDetails(
        basePlanId = basePlanId,
        offerId = offerId,
        offerTags = offerTags,
        offerToken = offerToken,
        pricingPhases = PricingPhasesAndroid(
            pricingPhaseList = pricingPhases.pricingPhaseList.map { phase ->
                PricingPhaseAndroid(
                    billingCycleCount = phase.billingCycleCount,
                    billingPeriod = phase.billingPeriod,
                    formattedPrice = phase.formattedPrice,
                    priceAmountMicros = phase.priceAmountMicros.toString(),
                    priceCurrencyCode = phase.priceCurrencyCode,
                    recurrenceMode = phase.recurrenceMode
                )
            }
        )
    )
}
