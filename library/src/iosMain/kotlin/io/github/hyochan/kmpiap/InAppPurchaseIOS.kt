package io.github.hyochan.kmpiap

import cocoapods.OpenIAP.*
import io.github.hyochan.kmpiap.types.*
import io.github.hyochan.kmpiap.utils.ErrorCode
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Instant
import platform.Foundation.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
internal class InAppPurchaseIOS : KmpInAppPurchase {
    // Event flows
    private val _purchaseUpdatedListener = MutableSharedFlow<Purchase>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val purchaseUpdatedListener: Flow<Purchase> = _purchaseUpdatedListener.asSharedFlow()

    private val _purchaseErrorListener = MutableSharedFlow<PurchaseError>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val purchaseErrorListener: Flow<PurchaseError> = _purchaseErrorListener.asSharedFlow()

    private val _connectionStateListener = MutableSharedFlow<ConnectionResult>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val connectionStateListener: Flow<ConnectionResult> = _connectionStateListener.asSharedFlow()

    private val _promotedProductListener = MutableSharedFlow<String?>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val promotedProductListener: Flow<String?> = _promotedProductListener.asSharedFlow()

    // OpenIAP module and subscriptions
    private val module = OpenIapModule.shared()
    private val eventSubscriptions = mutableListOf<Subscription>()
    private var isConnected = false

    override fun getVersion(): String = "KMP-IAP v1.0.0-alpha02 (iOS)"

    // ===== Connection Management =====
    override suspend fun initConnection(): Boolean {
        // Register listeners once
        if (eventSubscriptions.isEmpty()) {
            val sub1 = module.purchaseUpdatedListener { p: OpenIapPurchase ->
                _purchaseUpdatedListener.tryEmit(p.toKmpPurchase())
            }
            val sub2 = module.purchaseErrorListener { err: cocoapods.OpenIAP.PurchaseError ->
                _purchaseErrorListener.tryEmit(err.toKmpError())
            }
            val sub3 = module.promotedProductListenerIOS { productId: String ->
                _promotedProductListener.tryEmit(productId)
            }
            eventSubscriptions += listOf(sub1, sub2, sub3)
        }

        return suspendCancellableCoroutine { cont ->
            module.initConnectionWithCompletionHandler { ok: Boolean?, _: NSError? ->
                isConnected = ok == true
                _connectionStateListener.tryEmit(ConnectionResult(isConnected, if (isConnected) "Connected to App Store" else "Failed to connect"))
                cont.resume(isConnected)
            }
        }
    }

    override suspend fun endConnection() {
        suspendCancellableCoroutine { cont ->
            module.endConnectionWithCompletionHandler { _: Boolean?, _: NSError? ->
                isConnected = false
                _connectionStateListener.tryEmit(ConnectionResult(connected = false, message = "Disconnected from App Store"))
                cont.resume(Unit)
            }
        }
    }

    // ===== Product Management =====
    override suspend fun requestProducts(params: ProductRequest): List<Product> {
        ensureConnection()
        val type = when (params.type) {
            ProductType.INAPP -> "inapp"
            ProductType.SUBS -> "subs"
        }
        val req = OpenIapProductRequest(skus = params.skus, type = type)
        return suspendCancellableCoroutine { cont ->
            module.fetchProductsWithParams(req) { list: List<OpenIapProduct>?, error: NSError? ->
                if (error != null) cont.resumeWithException(Exception(error.localizedDescription ?: "Failed to fetch products"))
                else cont.resume((list ?: emptyList()).map { it.toKmpProduct() })
            }
        }
    }

    // ===== Purchase Operations =====
    override suspend fun requestPurchase(
        sku: String,
        ios: RequestPurchaseIosProps?,
        android: RequestPurchaseAndroidProps?
    ): Purchase {
        ensureConnection()
        val props = OpenIapRequestPurchaseProps(
            sku = sku,
            andDangerouslyFinishTransactionAutomatically = ios?.andDangerouslyFinishTransactionAutomatically,
            appAccountToken = ios?.appAccountToken,
            quantity = ios?.quantity,
            withOffer = ios?.withOffer?.let {
                OpenIapDiscountOffer(
                    identifier = it.identifier,
                    keyIdentifier = it.keyIdentifier,
                    nonce = it.nonce,
                    signature = it.signature,
                    timestamp = it.timestamp.toLong().toString()
                )
            }
        )
        return suspendCancellableCoroutine { cont ->
            module.requestPurchaseWithProps(props) { p: OpenIapPurchase?, error: NSError? ->
                if (error != null) cont.resumeWithException(io.github.hyochan.kmpiap.types.PurchaseError(ErrorCode.E_PURCHASE_ERROR.name, error.localizedDescription ?: "Purchase failed"))
                else cont.resume((p ?: error("Missing purchase")).toKmpPurchase())
            }
        }
    }

    override suspend fun getAvailablePurchases(options: PurchaseOptions?): List<Purchase> {
        ensureConnection()
        val opts = OpenIapGetAvailablePurchasesProps(
            alsoPublishToEventListenerIOS = options?.alsoPublishToEventListener,
            onlyIncludeActiveItemsIOS = options?.onlyIncludeActiveItems
        )
        return suspendCancellableCoroutine { cont ->
            module.getAvailablePurchasesWithOptions(opts) { list: List<OpenIapPurchase>?, error: NSError? ->
                if (error != null) cont.resumeWithException(Exception(error.localizedDescription ?: "Failed to get purchases"))
                else cont.resume((list ?: emptyList()).map { it.toKmpPurchase() })
            }
        }
    }

    override suspend fun getPurchaseHistories(options: PurchaseOptions?): List<Purchase> = getAvailablePurchases(options)

    override suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean?): Boolean {
        finishTransactionIOS(purchase.id)
        return true
    }

    // ===== Validation =====
    override suspend fun validateReceipt(options: ValidationOptions): ValidationResult {
        return when (options) {
            is ValidationOptions.IOSValidation -> {
                val map = hashMapOf<String, String>().apply {
                    put("receipt-data", options.receiptBody.receiptData)
                    options.receiptBody.password?.let { put("password", it) }
                }
                val result = validateReceiptIos(map, isTest = false)
                ValidationResult(
                    isValid = result?.get("status") == 0,
                    status = (result?.get("status") as? Int) ?: -1,
                    receipt = result as? Map<String, Any>,
                    latestReceipt = result?.get("latest_receipt") as? String,
                    latestReceiptInfo = result?.get("latest_receipt_info") as? List<Map<String, Any>>,
                    pendingRenewalInfo = result?.get("pending_renewal_info") as? List<Map<String, Any>>
                )
            }
            is ValidationOptions.AndroidValidation -> ValidationResult(false, -1)
        }
    }

    override suspend fun isPurchaseValid(purchase: Purchase): Boolean {
        ensureConnection()
        return suspendCancellableCoroutine { cont ->
            module.isTransactionVerifiedIOSWithSku(purchase.productId) { ok: Boolean -> cont.resume(ok) }
        }
    }

    // ===== iOS-specific APIs =====
    override suspend fun finishTransactionIOS(transactionId: String) {
        ensureConnection()
        suspendCancellableCoroutine<Unit> { cont ->
            module.finishTransactionWithTransactionIdentifier(transactionId) { _: Boolean?, _: NSError? -> cont.resume(Unit) }
        }
    }

    override suspend fun clearTransactionIOS() {
        ensureConnection()
        suspendCancellableCoroutine<Unit> { cont ->
            module.clearTransactionIOSWithCompletionHandler { _: Unit?, _: NSError? -> cont.resume(Unit) }
        }
    }

    override suspend fun clearProductsIOS() { /* no-op */ }

    override suspend fun getStorefrontIOS(): String {
        ensureConnection()
        return suspendCancellableCoroutine { cont ->
            module.getStorefrontIOSWithCompletionHandler { code: String?, _: NSError? -> cont.resume(code ?: "") }
        }
    }

    override suspend fun presentCodeRedemptionSheetIOS() {
        ensureConnection()
        suspendCancellableCoroutine<Unit> { cont ->
            module.presentCodeRedemptionSheetIOSWithCompletionHandler { _: Boolean?, _: NSError? -> cont.resume(Unit) }
        }
    }

    override suspend fun getPromotedProductIOS(): String? = null

    override suspend fun buyPromotedProductIOS() { /* not supported */ }

    // ===== Android-specific stubs =====
    override suspend fun acknowledgePurchaseAndroid(purchaseToken: String) { /* no-op */ }
    override suspend fun consumePurchaseAndroid(purchaseToken: String) { /* no-op */ }

    // ===== Subscription Management =====
    override suspend fun deepLinkToSubscriptions(options: DeepLinkOptions) {
        // On iOS, present native subscription management UI
        suspendCancellableCoroutine<Unit> { cont ->
            module.showManageSubscriptionsIOSWithCompletionHandler { _: List<Map<Any?, *>>?, _: NSError? -> cont.resume(Unit) }
        }
    }

    override suspend fun getActiveSubscriptions(subscriptionIds: List<String>?): List<ActiveSubscription> {
        ensureConnection()
        return suspendCancellableCoroutine { cont ->
            module.getActiveSubscriptionsWithSubscriptionIds(subscriptionIds) { list: List<OpenIapActiveSubscription>?, _: NSError? ->
                cont.resume((list ?: emptyList()).map { it.toKmpActiveSubscription() })
            }
        }
    }

    override suspend fun hasActiveSubscriptions(subscriptionIds: List<String>?): Boolean {
        ensureConnection()
        return suspendCancellableCoroutine { cont ->
            module.hasActiveSubscriptionsWithSubscriptionIds(subscriptionIds) { ok: Boolean?, _: NSError? -> cont.resume(ok == true) }
        }
    }

    override fun getStore(): Store = Store.APP_STORE

    override suspend fun canMakePayments(): Boolean {
        return try {
            val ok = suspendCancellableCoroutine<Boolean> { cont ->
                module.initConnectionWithCompletionHandler { res: Boolean?, _: NSError? -> cont.resume(res == true) }
            }
            suspendCancellableCoroutine { cont -> module.endConnectionWithCompletionHandler { _: Boolean?, _: NSError? -> cont.resume(Unit) } }
            ok
        } catch (e: Throwable) { false }
    }

    // ===== Helpers =====
    private suspend fun ensureConnection() {
        if (!isConnected) {
            throw PurchaseError(
                code = ErrorCode.E_SERVICE_ERROR.name,
                message = "IAP connection not initialized. Call initConnection() first."
            )
        }
    }

    private suspend fun validateReceiptIos(receiptBody: Map<String, String>, isTest: Boolean): Map<String, Any>? {
        return suspendCancellableCoroutine { continuation ->
            val url = if (isTest) "https://sandbox.itunes.apple.com/verifyReceipt" else "https://buy.itunes.apple.com/verifyReceipt"
            val request = NSMutableURLRequest(uRL = NSURL(string = url)!!)
            request.HTTPMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField = "Content-Type")
            val jsonData = try { NSJSONSerialization.dataWithJSONObject(receiptBody, 0u, null) } catch (e: Exception) {
                continuation.resumeWithException(PurchaseError(ErrorCode.E_RECEIPT_FAILED.name, "Failed to serialize receipt data")); return@suspendCancellableCoroutine
            }
            request.HTTPBody = jsonData
            val session = NSURLSession.sharedSession
            val task = session.dataTaskWithRequest(request) { data, _, error ->
                if (error != null) { continuation.resumeWithException(PurchaseError(ErrorCode.E_RECEIPT_FAILED.name, error.localizedDescription)); return@dataTaskWithRequest }
                data?.let { responseData ->
                    try {
                        val json = NSJSONSerialization.JSONObjectWithData(responseData, 0u, null)
                        @Suppress("UNCHECKED_CAST") val result = json as? Map<String, Any>
                        continuation.resume(result)
                    } catch (e: Exception) {
                        continuation.resumeWithException(PurchaseError(ErrorCode.E_RECEIPT_FAILED.name, "Failed to parse validation response"))
                    }
                } ?: continuation.resume(null)
            }
            task.resume()
        }
    }

    private fun OpenIapProduct.toKmpProduct(): Product {
        val commonType = if (this.type == "subs") ProductType.SUBS else ProductType.INAPP
        val subInfo = this.subscriptionInfoIOS?.let { info ->
            SubscriptionInfoIOS(
                introductoryOffer = info.introductoryOffer?.let { offer ->
                    SubscriptionOfferIOS(
                        displayPrice = offer.displayPrice,
                        id = offer.id,
                        paymentMode = offer.paymentMode.rawValue,
                        period = SubscriptionOfferPeriod(offer.period.unit.rawValue, offer.period.value),
                        periodCount = offer.periodCount,
                        price = offer.price,
                        type = offer.type.rawValue
                    )
                },
                promotionalOffers = info.promotionalOffers?.map { offer ->
                    SubscriptionOfferIOS(
                        displayPrice = offer.displayPrice,
                        id = offer.id,
                        paymentMode = offer.paymentMode.rawValue,
                        period = SubscriptionOfferPeriod(offer.period.unit.rawValue, offer.period.value),
                        periodCount = offer.periodCount,
                        price = offer.price,
                        type = offer.type.rawValue
                    )
                },
                subscriptionGroupId = info.subscriptionGroupId,
                subscriptionPeriod = SubscriptionOfferPeriod(info.subscriptionPeriod.unit.rawValue, info.subscriptionPeriod.value)
            )
        }
        return if (commonType == ProductType.SUBS) {
            ProductSubscriptionIOS(
                id = id,
                title = title,
                description = description,
                displayName = displayName,
                displayPrice = displayPrice,
                currency = currency,
                price = price,
                debugDescription = debugDescription,
                displayNameIOS = displayNameIOS,
                isFamilyShareableIOS = isFamilyShareableIOS,
                jsonRepresentationIOS = jsonRepresentationIOS,
                subscriptionInfoIOS = subInfo,
                discountsIOS = discountsIOS?.map { d ->
                    DiscountIOS(
                        identifier = d.identifier,
                        type = d.type,
                        numberOfPeriods = d.numberOfPeriods.toString(),
                        price = d.price,
                        localizedPrice = d.price,
                        paymentMode = d.paymentMode,
                        subscriptionPeriod = d.subscriptionPeriod
                    )
                },
                introductoryPriceIOS = introductoryPriceIOS,
                introductoryPriceAsAmountIOS = introductoryPriceAsAmountIOS,
                introductoryPricePaymentModeIOS = introductoryPricePaymentModeIOS,
                introductoryPriceNumberOfPeriodsIOS = introductoryPriceNumberOfPeriodsIOS,
                introductoryPriceSubscriptionPeriodIOS = introductoryPriceSubscriptionPeriodIOS,
                subscriptionPeriodNumberIOS = subscriptionPeriodNumberIOS,
                subscriptionPeriodUnitIOS = subscriptionPeriodUnitIOS
            )
        } else {
            ProductIOS(
                id = id,
                title = title,
                description = description,
                type = ProductType.INAPP,
                displayName = displayName,
                displayPrice = displayPrice,
                currency = currency,
                price = price,
                debugDescription = debugDescription,
                displayNameIOS = displayNameIOS,
                isFamilyShareableIOS = isFamilyShareableIOS,
                jsonRepresentationIOS = jsonRepresentationIOS,
                subscriptionInfoIOS = subInfo
            )
        }
    }

    private fun OpenIapPurchase.toKmpPurchase(): PurchaseIOS {
        val offer = this.offerIOS?.let { o -> io.github.hyochan.kmpiap.types.OfferIOS(o.id, o.type, o.paymentMode) }
        return PurchaseIOS(
            id = id,
            productId = productId,
            ids = ids,
            transactionId = id,
            transactionDate = transactionDate,
            transactionReceipt = transactionReceipt,
            purchaseToken = purchaseToken,
            quantityIOS = quantityIOS?.toInt(),
            originalTransactionDateIOS = originalTransactionDateIOS,
            originalTransactionIdentifierIOS = originalTransactionIdentifierIOS,
            appAccountToken = appAccountToken,
            expirationDateIOS = expirationDateIOS,
            webOrderLineItemIdIOS = webOrderLineItemIdIOS?.toLong(),
            environmentIOS = environmentIOS,
            storefrontCountryCodeIOS = storefrontCountryCodeIOS,
            appBundleIdIOS = appBundleIdIOS,
            productTypeIOS = productTypeIOS,
            subscriptionGroupIdIOS = subscriptionGroupIdIOS,
            isUpgradedIOS = isUpgradedIOS,
            ownershipTypeIOS = ownershipTypeIOS,
            reasonIOS = reasonIOS,
            reasonStringRepresentationIOS = reasonStringRepresentationIOS,
            transactionReasonIOS = transactionReasonIOS,
            revocationDateIOS = revocationDateIOS,
            revocationReasonIOS = revocationReasonIOS,
            offerIOS = offer,
            currencyCodeIOS = currencyCodeIOS,
            currencySymbolIOS = currencySymbolIOS,
            countryCodeIOS = countryCodeIOS
        )
    }

    private fun OpenIapActiveSubscription.toKmpActiveSubscription(): ActiveSubscription {
        return ActiveSubscription(
            productId = productId,
            isActive = isActive,
            expirationDateIOS = expirationDateIOS?.let { Instant.fromEpochMilliseconds((it.timeIntervalSince1970 * 1000.0).toLong()) },
            autoRenewingAndroid = autoRenewingAndroid,
            environmentIOS = environmentIOS,
            willExpireSoon = willExpireSoon,
            daysUntilExpirationIOS = daysUntilExpirationIOS?.toInt()
        )
    }

    private fun cocoapods.OpenIAP.PurchaseError.toKmpError(): io.github.hyochan.kmpiap.types.PurchaseError {
        return io.github.hyochan.kmpiap.types.PurchaseError(
            code = this.code,
            message = this.message,
        )
    }
}

