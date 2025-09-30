package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.openiap.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.Foundation.*
import cocoapods.openiap.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
internal class InAppPurchaseIOS : KmpInAppPurchase {
    // OpenIAP module instance - use constructor, not shared()
    private val openIapModule = OpenIapModule()

    // Event flows
    private val _purchaseUpdatedFlow = MutableSharedFlow<Purchase>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val purchaseUpdatedListener: Flow<Purchase> = _purchaseUpdatedFlow.asSharedFlow()

    private val _purchaseErrorFlow = MutableSharedFlow<PurchaseError>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val purchaseErrorListener: Flow<PurchaseError> = _purchaseErrorFlow.asSharedFlow()

    private val _promotedProductFlow = MutableSharedFlow<String?>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val promotedProductListener: Flow<String?> = _promotedProductFlow.asSharedFlow()

    private var isConnected = false
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Listener subscriptions
    private var purchaseSubscription: NSObject? = null
    private var errorSubscription: NSObject? = null
    private var promotedProductSubscription: NSObject? = null

    init {
        // Register listeners
        setupListeners()
    }

    private fun setupListeners() {
        // Purchase updated listener
        purchaseSubscription = openIapModule.addPurchaseUpdatedListener { dictionary ->
            println("[KMP-IAP iOS] Purchase updated received: $dictionary")
            val purchase = convertAnyToPurchase(dictionary)
            if (purchase != null) {
                coroutineScope.launch {
                    _purchaseUpdatedFlow.emit(purchase)
                }
            }
        }

        // Purchase error listener
        errorSubscription = openIapModule.addPurchaseErrorListener { dictionary ->
            println("[KMP-IAP iOS] Purchase error received: $dictionary")
            val map = (dictionary as? Map<*, *>)?.mapKeys { it.key.toString() } ?: return@addPurchaseErrorListener
            val codeString = map["code"] as? String ?: "unknown"
            val errorCode = ErrorCode.entries.find { it.rawValue == codeString } ?: ErrorCode.Unknown
            val error = PurchaseError(
                code = errorCode,
                message = map["message"] as? String ?: "",
                productId = map["productId"] as? String
            )
            coroutineScope.launch {
                _purchaseErrorFlow.emit(error)
            }
        }

        // Promoted product listener
        promotedProductSubscription = openIapModule.addPromotedProductListener { sku ->
            println("[KMP-IAP iOS] Promoted product received: $sku")
            coroutineScope.launch {
                _promotedProductFlow.emit(sku)
            }
        }
    }

    override fun getVersion(): String = "KMP-IAP v1.0.0-rc.2 (iOS)"

    override fun getStore(): Store = Store.APP_STORE

    override suspend fun canMakePayments(): Boolean {
        // OpenIAP will check this during initConnection
        return isConnected
    }

    // -------------------------------------------------------------------------
    // MutationResolver Implementation
    // -------------------------------------------------------------------------

    override suspend fun initConnection(): Boolean = suspendCoroutine { continuation ->
        openIapModule.initConnectionWithCompletion { success, error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error.localizedDescription))
            } else {
                isConnected = success
                continuation.resume(success)
            }
        }
    }

    override suspend fun endConnection(): Boolean = suspendCoroutine { continuation ->
        // Remove all listeners
        purchaseSubscription?.let { openIapModule.removeListener(it) }
        errorSubscription?.let { openIapModule.removeListener(it) }
        promotedProductSubscription?.let { openIapModule.removeListener(it) }

        openIapModule.endConnectionWithCompletion { success, error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error.localizedDescription))
            } else {
                isConnected = false
                continuation.resume(success)
            }
        }
    }

    override suspend fun requestPurchase(params: RequestPurchaseProps): RequestPurchaseResult? =
        suspendCoroutine { continuation ->
            when (val request = params.request) {
                is RequestPurchaseProps.Request.Purchase -> {
                    val sku = request.value.ios?.sku ?: run {
                        continuation.resumeWithException(Exception("SKU is required for iOS purchase"))
                        return@suspendCoroutine
                    }
                    val quantity = request.value.ios?.quantity ?: 1

                    openIapModule.requestPurchaseWithSku(
                        sku,
                        quantity = quantity.toLong(),
                        type = null
                    ) { result, error ->
                        if (error != null) {
                            continuation.resumeWithException(Exception(error.localizedDescription))
                        } else if (result != null) {
                            val purchase = convertAnyToPurchase(result)
                            if (purchase != null) {
                                continuation.resume(RequestPurchaseResultPurchase(purchase))
                            } else {
                                continuation.resume(null)
                            }
                        } else {
                            continuation.resume(null)
                        }
                    }
                }
                is RequestPurchaseProps.Request.Subscription -> {
                    val sku = request.value.ios?.sku ?: run {
                        continuation.resumeWithException(Exception("SKU is required for iOS subscription"))
                        return@suspendCoroutine
                    }
                    val offer = request.value.ios?.withOffer?.let { discountOffer ->
                        mapOf<Any?, Any?>(
                            "identifier" to discountOffer.identifier,
                            "keyIdentifier" to discountOffer.keyIdentifier,
                            "nonce" to discountOffer.nonce,
                            "signature" to discountOffer.signature,
                            "timestamp" to discountOffer.timestamp
                        )
                    }

                    openIapModule.requestSubscriptionWithSku(
                        sku,
                        offer = offer
                    ) { result, error ->
                        if (error != null) {
                            continuation.resumeWithException(Exception(error.localizedDescription))
                        } else if (result != null) {
                            val purchase = convertAnyToPurchase(result)
                            if (purchase != null) {
                                continuation.resume(RequestPurchaseResultPurchase(purchase))
                            } else {
                                continuation.resume(null)
                            }
                        } else {
                            continuation.resume(null)
                        }
                    }
                }
            }
        }

    override suspend fun requestPurchaseOnPromotedProductIOS(): Boolean {
        throw UnsupportedOperationException("requestPurchaseOnPromotedProductIOS not implemented in OpenIAP")
    }

    override suspend fun restorePurchases(): Unit = suspendCoroutine { continuation ->
        openIapModule.restorePurchasesWithCompletion { error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error.localizedDescription))
            } else {
                continuation.resume(Unit)
            }
        }
    }

    override suspend fun finishTransaction(purchase: PurchaseInput, isConsumable: Boolean?): Unit =
        suspendCoroutine { continuation ->
            val transactionId = purchase.id
            val productId = purchase.productId

            openIapModule.finishTransactionWithPurchaseId(
                transactionId,
                productId = productId,
                isConsumable = isConsumable ?: false
            ) { error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                } else {
                    continuation.resume(Unit)
                }
            }
        }

    override suspend fun deepLinkToSubscriptions(options: DeepLinkOptions?): Unit {
        throw UnsupportedOperationException("deepLinkToSubscriptions not implemented in OpenIAP")
    }

    override suspend fun presentCodeRedemptionSheetIOS(): Boolean =
        suspendCoroutine { continuation ->
            openIapModule.presentCodeRedemptionSheetIOSWithCompletion { success, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                } else {
                    continuation.resume(success)
                }
            }
        }

    override suspend fun beginRefundRequestIOS(sku: String): String? {
        throw UnsupportedOperationException("beginRefundRequestIOS not implemented in OpenIAP")
    }

    override suspend fun clearTransactionIOS(): Boolean = suspendCoroutine { continuation ->
        openIapModule.clearTransactionIOSWithCompletion { success, error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error.localizedDescription))
            } else {
                continuation.resume(success)
            }
        }
    }

    override suspend fun showManageSubscriptionsIOS(): List<PurchaseIOS> =
        suspendCoroutine { continuation ->
            openIapModule.showManageSubscriptionsIOSWithCompletion { result, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                } else if (result != null) {
                    val purchases = convertAnyListToPurchaseIOSList(result)
                    continuation.resume(purchases)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }

    override suspend fun syncIOS(): Boolean {
        throw UnsupportedOperationException("syncIOS not implemented in OpenIAP")
    }

    override suspend fun validateReceipt(options: ReceiptValidationProps): ReceiptValidationResult {
        // Call the iOS-specific version and return the result directly
        return validateReceiptIOS(options)
    }

    // -------------------------------------------------------------------------
    // QueryResolver Implementation
    // -------------------------------------------------------------------------

    override suspend fun fetchProducts(params: ProductRequest): FetchProductsResult =
        suspendCoroutine { continuation ->
            val skus = params.skus
            val type = params.type?.rawValue

            println("[KMP-IAP iOS] Fetching products with skus: $skus, type: $type")
            println("[KMP-IAP iOS] openIapModule: $openIapModule")

            openIapModule.fetchProductsWithSkus(skus, type = type) { result, error ->
                println("[KMP-IAP iOS] fetchProducts callback - result: $result, error: ${error?.localizedDescription}")

                if (error != null) {
                    println("[KMP-IAP iOS] Error fetching products: ${error.localizedDescription}")
                    continuation.resumeWithException(Exception(error.localizedDescription))
                } else if (result != null) {
                    println("[KMP-IAP iOS] Result type: ${result::class.simpleName}")
                    println("[KMP-IAP iOS] Result: $result")

                    // Convert [Any] to products or subscriptions based on type
                    when (params.type) {
                        ProductQueryType.Subs -> {
                            val subscriptions = convertAnyListToProductSubscriptions(result)
                            println("[KMP-IAP iOS] Converted to ${subscriptions.size} subscriptions")
                            continuation.resume(FetchProductsResultSubscriptions(subscriptions))
                        }
                        else -> {
                            val products = convertAnyListToProducts(result)
                            println("[KMP-IAP iOS] Converted to ${products.size} products")
                            continuation.resume(FetchProductsResultProducts(products))
                        }
                    }
                } else {
                    println("[KMP-IAP iOS] No result and no error, returning empty list")
                    continuation.resume(FetchProductsResultProducts(emptyList()))
                }
            }
        }

    override suspend fun getAvailablePurchases(options: PurchaseOptions?): List<Purchase> =
        suspendCoroutine { continuation ->
            openIapModule.getAvailablePurchasesWithCompletion { result, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                } else if (result != null) {
                    val purchases = convertAnyListToPurchases(result)
                    continuation.resume(purchases)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }

    override suspend fun getStorefront(): String {
        return getStorefrontIOS()
    }

    override suspend fun getStorefrontIOS(): String = suspendCoroutine { continuation ->
        openIapModule.getStorefrontIOSWithCompletion { result, error ->
            if (error != null) {
                continuation.resume("US") // Default fallback
            } else {
                continuation.resume(result ?: "US")
            }
        }
    }

    override suspend fun getPendingTransactionsIOS(): List<PurchaseIOS> =
        suspendCoroutine { continuation ->
            openIapModule.getPendingTransactionsIOSWithCompletion { result, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                } else if (result != null) {
                    val purchases = convertAnyListToPurchaseIOSList(result)
                    continuation.resume(purchases)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }

    override suspend fun getReceiptDataIOS(): String? = suspendCoroutine { continuation ->
        openIapModule.getReceiptDataIOSWithCompletion { result, error ->
            if (error != null) {
                continuation.resume(null)
            } else {
                continuation.resume(result)
            }
        }
    }

    override suspend fun getPromotedProductIOS(): ProductIOS? = suspendCoroutine { continuation ->
        openIapModule.getPromotedProductIOSWithCompletion { result, error ->
            if (error != null) {
                continuation.resumeWithException(Exception(error.localizedDescription))
            } else if (result != null) {
                val product = convertAnyToProductIOS(result)
                continuation.resume(product)
            } else {
                continuation.resume(null)
            }
        }
    }

    override suspend fun getActiveSubscriptions(subscriptionIds: List<String>?): List<ActiveSubscription> =
        suspendCoroutine { continuation ->
            openIapModule.getActiveSubscriptionsWithCompletion { result, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return@getActiveSubscriptionsWithCompletion
                }

                val subscriptions = (result as? List<*>)?.mapNotNull { item ->
                    val map = (item as? Map<*, *>)?.mapKeys { it.key.toString() } ?: return@mapNotNull null
                    try {
                        ActiveSubscription.fromJson(map)
                    } catch (e: Exception) {
                        println("[KMP-IAP iOS] Failed to parse ActiveSubscription: ${e.message}")
                        null
                    }
                } ?: emptyList()

                continuation.resume(subscriptions)
            }
        }

    override suspend fun getAppTransactionIOS(): AppTransaction? =
        suspendCoroutine { continuation ->
            // Note: getAppTransactionIOS requires iOS 16.0+
            // The @available annotation on the Swift side handles version checking
            openIapModule.getAppTransactionIOSWithCompletion { result, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return@getAppTransactionIOSWithCompletion
                }

                val map = (result as? Map<*, *>)?.mapKeys { it.key.toString() }
                val appTransaction = map?.let {
                    try {
                        AppTransaction.fromJson(it)
                    } catch (e: Exception) {
                        println("[KMP-IAP iOS] Failed to parse AppTransaction: ${e.message}")
                        null
                    }
                }

                continuation.resume(appTransaction)
            }
        }

    override suspend fun currentEntitlementIOS(sku: String): PurchaseIOS? =
        suspendCoroutine { continuation ->
            openIapModule.currentEntitlementIOSWithSku(sku) { result, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return@currentEntitlementIOSWithSku
                }

                val purchase = convertAnyToPurchaseIOS(result)
                continuation.resume(purchase)
            }
        }

    override suspend fun getTransactionJwsIOS(sku: String): String? =
        suspendCoroutine { continuation ->
            openIapModule.getTransactionJwsIOSWithSku(sku) { result, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return@getTransactionJwsIOSWithSku
                }

                continuation.resume(result as? String)
            }
        }

    override suspend fun hasActiveSubscriptions(subscriptionIds: List<String>?): Boolean =
        suspendCoroutine { continuation ->
            openIapModule.hasActiveSubscriptionsWithCompletion { hasActive, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return@hasActiveSubscriptionsWithCompletion
                }

                continuation.resume(hasActive)
            }
        }

    override suspend fun isEligibleForIntroOfferIOS(groupID: String): Boolean =
        suspendCoroutine { continuation ->
            openIapModule.isEligibleForIntroOfferIOSWithGroupID(groupID) { isEligible, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return@isEligibleForIntroOfferIOSWithGroupID
                }

                continuation.resume(isEligible)
            }
        }

    override suspend fun isTransactionVerifiedIOS(sku: String): Boolean =
        suspendCoroutine { continuation ->
            openIapModule.isTransactionVerifiedIOSWithSku(sku) { isVerified, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return@isTransactionVerifiedIOSWithSku
                }

                continuation.resume(isVerified)
            }
        }

    override suspend fun latestTransactionIOS(sku: String): PurchaseIOS? =
        suspendCoroutine { continuation ->
            openIapModule.latestTransactionIOSWithSku(sku) { result, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return@latestTransactionIOSWithSku
                }

                val purchase = convertAnyToPurchaseIOS(result)
                continuation.resume(purchase)
            }
        }

    override suspend fun subscriptionStatusIOS(sku: String): List<SubscriptionStatusIOS> =
        suspendCoroutine { continuation ->
            openIapModule.subscriptionStatusIOSWithSku(sku) { result, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return@subscriptionStatusIOSWithSku
                }

                val statuses = (result as? List<*>)?.mapNotNull { item ->
                    val map = (item as? Map<*, *>)?.mapKeys { it.key.toString() } ?: return@mapNotNull null
                    try {
                        SubscriptionStatusIOS.fromJson(map)
                    } catch (e: Exception) {
                        println("[KMP-IAP iOS] Failed to parse SubscriptionStatusIOS: ${e.message}")
                        null
                    }
                } ?: emptyList()

                continuation.resume(statuses)
            }
        }

    override suspend fun validateReceiptIOS(options: ReceiptValidationProps): ReceiptValidationResultIOS {
        // Get receipt data
        val receiptData = getReceiptDataIOS() ?: ""

        // For now, return a basic result. Full validation requires backend integration
        return ReceiptValidationResultIOS(
            isValid = false,
            jwsRepresentation = "",
            latestTransaction = null,
            receiptData = receiptData
        )
    }

    // -------------------------------------------------------------------------
    // SubscriptionResolver Implementation
    // -------------------------------------------------------------------------

    override suspend fun purchaseUpdated(): Purchase {
        throw UnsupportedOperationException("Use purchaseUpdatedListener Flow instead")
    }

    override suspend fun purchaseError(): PurchaseError {
        throw UnsupportedOperationException("Use purchaseErrorListener Flow instead")
    }

    override suspend fun promotedProductIOS(): String {
        throw UnsupportedOperationException("Use promotedProductListener Flow instead")
    }

    // -------------------------------------------------------------------------
    // Conversion Helpers
    // -------------------------------------------------------------------------

    @Suppress("UNCHECKED_CAST")
    private fun convertAnyToPurchase(data: Any?): Purchase? {
        return convertAnyToPurchaseIOS(data)
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertAnyToPurchaseIOS(data: Any?): PurchaseIOS? {
        if (data == null) return null

        return try {
            // OpenIAP returns NSDictionary which can be cast to Map
            val dict = (data as? Map<*, *>) ?: return null
            val map = dict.mapKeys { it.key.toString() }

            val platform = map["platform"] as? String
            if (platform == "ios" || platform == "iOS") {
                PurchaseIOS(
                    appAccountToken = map["appAccountToken"] as? String,
                    appBundleIdIOS = map["appBundleIdIOS"] as? String,
                    countryCodeIOS = map["countryCodeIOS"] as? String,
                    currencyCodeIOS = map["currencyCodeIOS"] as? String,
                    currencySymbolIOS = map["currencySymbolIOS"] as? String,
                    environmentIOS = map["environmentIOS"] as? String,
                    expirationDateIOS = (map["expirationDateIOS"] as? Number)?.toDouble(),
                    id = map["id"] as? String ?: "",
                    ids = (map["ids"] as? List<*>)?.mapNotNull { it as? String },
                    isAutoRenewing = map["isAutoRenewing"] as? Boolean ?: false,
                    isUpgradedIOS = map["isUpgradedIOS"] as? Boolean,
                    offerIOS = null, // Complex object, handle separately if needed
                    originalTransactionDateIOS = (map["originalTransactionDateIOS"] as? Number)?.toDouble(),
                    originalTransactionIdentifierIOS = map["originalTransactionIdentifierIOS"] as? String,
                    ownershipTypeIOS = map["ownershipTypeIOS"] as? String,
                    platform = IapPlatform.Ios,
                    productId = map["productId"] as? String ?: "",
                    purchaseState = (map["purchaseState"] as? String)?.let {
                        PurchaseState.fromJson(it)
                    } ?: PurchaseState.Unknown,
                    purchaseToken = map["purchaseToken"] as? String,
                    quantity = (map["quantity"] as? Number)?.toInt() ?: 1,
                    quantityIOS = (map["quantityIOS"] as? Number)?.toInt(),
                    reasonIOS = map["reasonIOS"] as? String,
                    reasonStringRepresentationIOS = map["reasonStringRepresentationIOS"] as? String,
                    revocationDateIOS = (map["revocationDateIOS"] as? Number)?.toDouble(),
                    revocationReasonIOS = map["revocationReasonIOS"] as? String,
                    storefrontCountryCodeIOS = map["storefrontCountryCodeIOS"] as? String,
                    subscriptionGroupIdIOS = map["subscriptionGroupIdIOS"] as? String,
                    transactionDate = (map["transactionDate"] as? Number)?.toDouble() ?: 0.0,
                    transactionId = map["transactionId"] as? String ?: "",
                    transactionReasonIOS = map["transactionReasonIOS"] as? String,
                    webOrderLineItemIdIOS = map["webOrderLineItemIdIOS"] as? String
                )
            } else {
                null
            }
        } catch (e: Exception) {
            println("[KMP-IAP] Error converting to Purchase: ${e.message}")
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertAnyListToPurchases(data: Any?): List<Purchase> {
        if (data == null) return emptyList()

        return try {
            val list = data as? List<*> ?: return emptyList()
            list.mapNotNull { convertAnyToPurchase(it) }
        } catch (e: Exception) {
            println("[KMP-IAP] Error converting to Purchase list: ${e.message}")
            emptyList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertAnyListToPurchaseIOSList(data: Any?): List<PurchaseIOS> {
        if (data == null) return emptyList()

        return try {
            val list = data as? List<*> ?: return emptyList()
            list.mapNotNull { item ->
                convertAnyToPurchaseIOS(item)
            }
        } catch (e: Exception) {
            println("[KMP-IAP] Error converting to PurchaseIOS list: ${e.message}")
            emptyList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertAnyListToProducts(data: Any?): List<Product> {
        if (data == null) {
            println("[KMP-IAP] convertAnyListToProducts: data is null")
            return emptyList()
        }

        println("[KMP-IAP] convertAnyListToProducts: data type = ${data::class.simpleName}")

        return try {
            val list = data as? List<*>
            if (list == null) {
                println("[KMP-IAP] convertAnyListToProducts: failed to cast to List, data = $data")
                return emptyList()
            }

            println("[KMP-IAP] convertAnyListToProducts: list size = ${list.size}")

            list.mapNotNull { item ->
                println("[KMP-IAP] convertAnyListToProducts: processing item type = ${item?.let { it::class.simpleName }}")

                val dict = (item as? Map<*, *>)
                if (dict == null) {
                    println("[KMP-IAP] convertAnyListToProducts: item is not a Map, it's $item")
                    return@mapNotNull null
                }

                val map = dict.mapKeys { it.key.toString() }
                println("[KMP-IAP] convertAnyListToProducts: map keys = ${map.keys}")

                ProductIOS(
                    currency = map["currency"] as? String ?: "",
                    debugDescription = map["debugDescription"] as? String,
                    description = map["description"] as? String ?: "",
                    displayName = map["displayName"] as? String,
                    displayNameIOS = map["displayNameIOS"] as? String ?: "",
                    displayPrice = map["displayPrice"] as? String ?: "",
                    id = map["id"] as? String ?: "",
                    isFamilyShareableIOS = map["isFamilyShareableIOS"] as? Boolean ?: false,
                    jsonRepresentationIOS = map["jsonRepresentationIOS"] as? String ?: "",
                    platform = IapPlatform.Ios,
                    price = (map["price"] as? Number)?.toDouble(),
                    subscriptionInfoIOS = null, // Complex object, handle separately if needed
                    title = map["title"] as? String ?: "",
                    type = (map["type"] as? String)?.let { ProductType.fromJson(it) }
                        ?: ProductType.InApp,
                    typeIOS = (map["typeIOS"] as? String)?.let {
                        ProductTypeIOS.fromJson(it)
                    } ?: ProductTypeIOS.Consumable
                )
            }
        } catch (e: Exception) {
            println("[KMP-IAP] Error converting to Product list: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertAnyListToProductSubscriptions(data: Any?): List<ProductSubscription> {
        if (data == null) return emptyList()

        return try {
            val list = data as? List<*> ?: return emptyList()
            list.mapNotNull { item ->
                val dict = (item as? Map<*, *>) ?: return@mapNotNull null
                val map = dict.mapKeys { it.key.toString() }

                ProductSubscriptionIOS(
                    currency = map["currency"] as? String ?: "",
                    debugDescription = map["debugDescription"] as? String,
                    description = map["description"] as? String ?: "",
                    discountsIOS = null, // Complex array
                    displayName = map["displayName"] as? String,
                    displayNameIOS = map["displayNameIOS"] as? String ?: "",
                    displayPrice = map["displayPrice"] as? String ?: "",
                    id = map["id"] as? String ?: "",
                    introductoryPriceAsAmountIOS = map["introductoryPriceAsAmountIOS"] as? String,
                    introductoryPriceIOS = map["introductoryPriceIOS"] as? String,
                    introductoryPriceNumberOfPeriodsIOS = map["introductoryPriceNumberOfPeriodsIOS"] as? String,
                    introductoryPricePaymentModeIOS = null, // Complex enum
                    introductoryPriceSubscriptionPeriodIOS = null, // Complex enum
                    isFamilyShareableIOS = map["isFamilyShareableIOS"] as? Boolean ?: false,
                    jsonRepresentationIOS = map["jsonRepresentationIOS"] as? String ?: "",
                    platform = IapPlatform.Ios,
                    price = (map["price"] as? Number)?.toDouble(),
                    subscriptionInfoIOS = null, // Complex object
                    subscriptionPeriodNumberIOS = map["subscriptionPeriodNumberIOS"] as? String,
                    subscriptionPeriodUnitIOS = null, // Complex enum
                    title = map["title"] as? String ?: "",
                    type = ProductType.Subs,
                    typeIOS = ProductTypeIOS.AutoRenewableSubscription
                )
            }
        } catch (e: Exception) {
            println("[KMP-IAP] Error converting to ProductSubscription list: ${e.message}")
            emptyList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertAnyToProductIOS(data: Any?): ProductIOS? {
        if (data == null) return null

        return try {
            val dict = (data as? Map<*, *>) ?: return null
            val map = dict.mapKeys { it.key.toString() }

            ProductIOS(
                currency = map["currency"] as? String ?: "",
                debugDescription = map["debugDescription"] as? String,
                description = map["description"] as? String ?: "",
                displayName = map["displayName"] as? String,
                displayNameIOS = map["displayNameIOS"] as? String ?: "",
                displayPrice = map["displayPrice"] as? String ?: "",
                id = map["id"] as? String ?: "",
                isFamilyShareableIOS = map["isFamilyShareableIOS"] as? Boolean ?: false,
                jsonRepresentationIOS = map["jsonRepresentationIOS"] as? String ?: "",
                platform = IapPlatform.Ios,
                price = (map["price"] as? Number)?.toDouble(),
                subscriptionInfoIOS = null, // Complex object
                title = map["title"] as? String ?: "",
                type = (map["type"] as? String)?.let { ProductType.fromJson(it) }
                    ?: ProductType.InApp,
                typeIOS = (map["typeIOS"] as? String)?.let {
                    ProductTypeIOS.fromJson(it)
                } ?: ProductTypeIOS.Consumable
            )
        } catch (e: Exception) {
            println("[KMP-IAP] Error converting to ProductIOS: ${e.message}")
            null
        }
    }

    // -------------------------------------------------------------------------
    // Android-specific stubs (not implemented on iOS)
    // -------------------------------------------------------------------------

    override suspend fun acknowledgePurchaseAndroid(purchaseToken: String): Boolean {
        throw UnsupportedOperationException("Android method not available on iOS")
    }

    override suspend fun consumePurchaseAndroid(purchaseToken: String): Boolean {
        throw UnsupportedOperationException("Android method not available on iOS")
    }
}