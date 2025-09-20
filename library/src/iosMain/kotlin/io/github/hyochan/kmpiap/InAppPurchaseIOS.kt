package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.openiap.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import platform.Foundation.*
import platform.StoreKit.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)

// Separate Objective-C observer class
private class PaymentObserver : NSObject(), SKPaymentTransactionObserverProtocol, SKProductsRequestDelegateProtocol {
    var purchaseHandler: ((SKPaymentTransaction) -> Unit)? = null
    var failureHandler: ((SKPaymentTransaction) -> Unit)? = null
    var restoreHandler: ((SKPaymentTransaction) -> Unit)? = null
    var deferredHandler: ((SKPaymentTransaction) -> Unit)? = null
    var restoreCompletionHandler: (() -> Unit)? = null
    var restoreFailureHandler: ((NSError) -> Unit)? = null
    var promotedProductHandler: ((String, SKProduct) -> Unit)? = null
    var productsResponseHandler: ((SKProductsRequest, List<SKProduct>, Set<*>) -> Unit)? = null
    var productRequestFailureHandler: ((SKProductsRequest, NSError) -> Unit)? = null
    
    // Track processed transactions to avoid duplicate logs
    private val processedTransactions = mutableSetOf<String>()
    
    // SKPaymentTransactionObserver methods
    override fun paymentQueue(queue: SKPaymentQueue, updatedTransactions: List<*>) {
        @Suppress("UNCHECKED_CAST")
        val transactions = updatedTransactions as List<SKPaymentTransaction>
        
        transactions.forEach { transaction ->
            val transactionKey = "${transaction.payment.productIdentifier}_${transaction.transactionState}_${transaction.transactionIdentifier ?: "notx"}"
            val shouldLog = !processedTransactions.contains(transactionKey)
            
            when (transaction.transactionState) {
                SKPaymentTransactionState.SKPaymentTransactionStatePurchasing -> {
                    // Transaction is being processed (Purchasing)
                    if (shouldLog) {
                        processedTransactions.add(transactionKey)
                        // Only log purchasing state for actual new purchases, not restorations
                    }
                }
                SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> {
                    // Purchased
                    purchaseHandler?.invoke(transaction)
                }
                SKPaymentTransactionState.SKPaymentTransactionStateFailed -> {
                    // Failed
                    failureHandler?.invoke(transaction)
                }
                SKPaymentTransactionState.SKPaymentTransactionStateRestored -> {
                    // Restored
                    restoreHandler?.invoke(transaction)
                }
                SKPaymentTransactionState.SKPaymentTransactionStateDeferred -> {
                    // Deferred
                    deferredHandler?.invoke(transaction)
                }
                else -> {
                    // Unknown state - log for debugging
                    println("[KMP-IAP] Unknown transaction state: ${transaction.transactionState}")
                }
            }
        }
    }
    
    override fun paymentQueueRestoreCompletedTransactionsFinished(queue: SKPaymentQueue) {
        restoreCompletionHandler?.invoke()
    }
    
    override fun paymentQueue(queue: SKPaymentQueue, restoreCompletedTransactionsFailedWithError: NSError) {
        restoreFailureHandler?.invoke(restoreCompletedTransactionsFailedWithError)
    }
    
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun paymentQueue(queue: SKPaymentQueue, shouldAddStorePayment: SKPayment, forProduct: SKProduct): Boolean {
        // Handle promoted purchases
        promotedProductHandler?.invoke(forProduct.productIdentifier, forProduct)
        return true
    }
    
    // SKProductsRequestDelegate methods
    override fun productsRequest(request: SKProductsRequest, didReceiveResponse: SKProductsResponse) {
        try {
            @Suppress("UNCHECKED_CAST")
            val products = didReceiveResponse.products as? List<SKProduct> ?: emptyList()
            val invalidIds = didReceiveResponse.invalidProductIdentifiers as? Set<*> ?: emptySet<Any>()
            productsResponseHandler?.invoke(request, products, invalidIds)
        } catch (e: Exception) {
            productsResponseHandler?.invoke(request, emptyList(), setOf<Any>())
        }
    }
    
    override fun requestDidFinish(request: SKRequest) {
        // Handled by productsResponseHandler
    }
    
    override fun request(request: SKRequest, didFailWithError: NSError) {
        if (request is SKProductsRequest) {
            productRequestFailureHandler?.invoke(request, didFailWithError)
        }
    }
}

internal class InAppPurchaseIOS : KmpInAppPurchase {
    // Event flows
    private val _purchaseUpdatedListener = MutableSharedFlow<Purchase>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val purchaseUpdatedListener: Flow<Purchase> = _purchaseUpdatedListener.asSharedFlow()
    
    private val _purchaseErrorListener = MutableSharedFlow<PurchaseError>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val purchaseErrorListener: Flow<PurchaseError> = _purchaseErrorListener.asSharedFlow()
    
    // Connection state - exposed for backward compatibility
    private val _connectionStateListener = MutableSharedFlow<ConnectionResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val connectionStateListener: Flow<ConnectionResult> = _connectionStateListener.asSharedFlow()
    
    private val _promotedProductListener = MutableSharedFlow<String?>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val promotedProductListener: Flow<String?> = _promotedProductListener.asSharedFlow()
    
    // Private properties
    private val productRequests = mutableMapOf<SKProductsRequest, (List<SKProduct>, Set<*>) -> Unit>()
    private val restoredPurchases = mutableListOf<Purchase>()
    private var restoreCompletion: (() -> Unit)? = null
    private var isConnected = false
    private var isRestoring = false
    private var hasRestoredOnce = false
    private var promotedProduct: SKProduct? = null
    
    // The Objective-C observer
    private val paymentObserver = PaymentObserver()
    
    init {
        // Setup observer callbacks
        paymentObserver.purchaseHandler = { transaction ->
            handlePurchasedTransaction(transaction)
        }
        
        paymentObserver.failureHandler = { transaction ->
            handleFailedTransaction(transaction)
        }
        
        paymentObserver.restoreHandler = { transaction ->
            handleRestoredTransaction(transaction)
        }
        
        paymentObserver.deferredHandler = { transaction ->
            _purchaseErrorListener.tryEmit(
                PurchaseError(
                    code = ErrorCode.Pending,
                    message = "Purchase is deferred and waiting for approval",
                )
            )
        }
        
        paymentObserver.restoreCompletionHandler = {
            isRestoring = false
            restoreCompletion?.invoke()
        }
        
        paymentObserver.restoreFailureHandler = { error ->
            isRestoring = false
            _purchaseErrorListener.tryEmit(
                PurchaseError(
                    code = ErrorCode.Unknown,
                    message = "Restore failed: ${error.localizedDescription}"
                )
            )
            restoreCompletion?.invoke()
        }
        
        paymentObserver.promotedProductHandler = { productId, product ->
            promotedProduct = product
            _promotedProductListener.tryEmit(productId)
        }
        
        paymentObserver.productsResponseHandler = { request, products, invalidIds ->
            productRequests[request]?.invoke(products, invalidIds)
            productRequests.remove(request)
        }
        
        paymentObserver.productRequestFailureHandler = { request, error ->
            productRequests.remove(request)
            _purchaseErrorListener.tryEmit(
                PurchaseError(
                    code = ErrorCode.ServiceError,
                    message = "Failed to load products: ${error.localizedDescription}"
                )
            )
        }
    }
    
    override fun getVersion(): String {
        return "KMP-IAP v1.0.0-alpha02 (iOS)"
    }
    
    override suspend fun initConnection(): Boolean {
        // Clean up any existing state from hot reload
        cleanupState()
        
        // Add transaction observer
        SKPaymentQueue.defaultQueue().addTransactionObserver(paymentObserver)
        isConnected = true
        hasRestoredOnce = false
        _connectionStateListener.emit(ConnectionResult(connected = true, message = "Connected to App Store"))
        return true
    }
    
    override suspend fun endConnection(): Boolean {
        SKPaymentQueue.defaultQueue().removeTransactionObserver(paymentObserver)
        isConnected = false
        cleanupState()
        _connectionStateListener.emit(ConnectionResult(connected = false, message = "Disconnected from App Store"))
        return true
    }
    
    override suspend fun fetchProducts(params: ProductRequest): FetchProductsResult {
        val products = loadProducts(params)
        return FetchProductsResultProducts(products)
    }

    private suspend fun loadProducts(params: ProductRequest): List<Product> {
        ensureConnection()
        return suspendCancellableCoroutine { continuation ->
            try {
                val request = SKProductsRequest(productIdentifiers = params.skus.toSet())

                productRequests[request] = { products, _ ->
                    productRequests.remove(request)

                    try {
                        val productList = products.map { skProduct ->
                            skProduct.toProduct()
                        }

                        if (continuation.isActive) {
                            continuation.resume(productList)
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(e)
                        }
                    }
                }

                request.delegate = paymentObserver
                request.start()

                continuation.invokeOnCancellation {
                    request.cancel()
                    productRequests.remove(request)
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }
    
    override suspend fun requestPurchase(request: RequestPurchaseProps): RequestPurchaseResult? {
        ensureConnection()

        val purchase = when (val req = request.request) {
            is RequestPurchaseProps.Request.Purchase -> handlePurchaseRequest(req.value)
            is RequestPurchaseProps.Request.Subscription -> handleSubscriptionRequest(req.value)
        }
        return RequestPurchaseResultPurchase(purchase)
    }

    private suspend fun handlePurchaseRequest(props: RequestPurchasePropsByPlatforms): Purchase {
        val sku = props.ios?.sku ?: props.android?.skus?.firstOrNull()
            ?: failWithPurchase(ErrorCode.EmptySkuList, "SKU list is empty")

        val quantity = props.ios?.quantity ?: 1
        val appAccountToken = props.ios?.appAccountToken

        val products = loadProducts(ProductRequest(listOf(sku), ProductQueryType.InApp))
        if (products.isEmpty()) failWithPurchase(ErrorCode.ItemUnavailable, "Product not found: $sku")

        val skProduct = fetchSkProduct(sku)
            ?: failWithPurchase(ErrorCode.ItemUnavailable, "Failed to fetch product: $sku")

        return launchStoreKitPayment(skProduct, quantity, appAccountToken, isAutoRenewing = false)
    }

    private suspend fun handleSubscriptionRequest(props: RequestSubscriptionPropsByPlatforms): Purchase {
        val sku = props.ios?.sku ?: props.android?.skus?.firstOrNull()
            ?: failWithPurchase(ErrorCode.EmptySkuList, "SKU list is empty")

        val quantity = props.ios?.quantity ?: 1
        val appAccountToken = props.ios?.appAccountToken

        val products = loadProducts(ProductRequest(listOf(sku), ProductQueryType.Subs))
        if (products.isEmpty()) failWithPurchase(ErrorCode.ItemUnavailable, "Subscription not found: $sku")

        val skProduct = fetchSkProduct(sku)
            ?: failWithPurchase(ErrorCode.ItemUnavailable, "Failed to fetch subscription: $sku")

        return launchStoreKitPayment(skProduct, quantity, appAccountToken, isAutoRenewing = true)
    }

    private fun failWithPurchase(code: ErrorCode, message: String): Nothing {
        val error = PurchaseError(code = code, message = message)
        _purchaseErrorListener.tryEmit(error)
        throw PurchaseException(error)
    }

    private suspend fun fetchSkProduct(sku: String): SKProduct? = suspendCancellableCoroutine { continuation ->
        val request = SKProductsRequest(productIdentifiers = setOf(sku))
        productRequests[request] = { skProducts, _ ->
            if (continuation.isActive) {
                continuation.resume(skProducts.firstOrNull())
            }
        }
        request.delegate = paymentObserver
        request.start()
    }

    private fun launchStoreKitPayment(
        product: SKProduct,
        quantity: Int,
        appAccountToken: String?,
        isAutoRenewing: Boolean
    ): PurchaseIOS {
        return try {
            val payment = SKMutablePayment.paymentWithProduct(product)
            payment.setQuantity(quantity.toLong())
            appAccountToken?.let(payment::setApplicationUsername)

            val queue = SKPaymentQueue.defaultQueue()
            dispatch_async(dispatch_get_main_queue()) {
                runCatching { queue.addPayment(payment) }
            }

            val instant = Clock.System.now()
            val epochSeconds = instant.epochSeconds
            PurchaseIOS(
                id = product.productIdentifier,
                ids = listOf(product.productIdentifier),
                isAutoRenewing = isAutoRenewing,
                platform = IapPlatform.Ios,
                productId = product.productIdentifier,
                purchaseState = PurchaseState.Pending,
                purchaseToken = null,
                quantity = quantity,
                transactionDate = epochSeconds.toDouble(),
                transactionId = "pending-${epochSeconds}-${product.productIdentifier}"
            )
        } catch (e: Exception) {
            val error = PurchaseError(
                code = ErrorCode.Unknown,
                message = "Failed to create payment: ${e.message}",
            )
            _purchaseErrorListener.tryEmit(error)
            throw PurchaseException(error)
        }
    }
    
    override suspend fun getAvailablePurchases(options: PurchaseOptions?): List<Purchase> {
        ensureConnection()
        return suspendCancellableCoroutine { continuation ->
            isRestoring = true
            SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
            
            restoredPurchases.clear()
            restoreCompletion = {
                isRestoring = false
                if (continuation.isActive) {
                    // Deduplicate purchases by product ID, keeping only the most recent one
                    val deduplicatedPurchases = restoredPurchases
                        .groupBy { it.productId }
                        .mapValues { (_, purchases) ->
                            // Keep the purchase with the latest transaction date
                            purchases.maxByOrNull { it.transactionDate ?: 0.0 } ?: purchases.first()
                        }
                        .values
                        .toList()
                    
                    continuation.resume(deduplicatedPurchases)
                }
                restoreCompletion = null
            }
        }
    }

    override suspend fun restorePurchases() {
        getAvailablePurchases(null)
    }
    
    suspend fun getPurchaseHistories(options: PurchaseOptions?): List<Purchase> {
        // iOS doesn't have a separate purchase history API
        // Return available purchases directly
        return getAvailablePurchases(options)
    }
    
    override suspend fun finishTransaction(purchase: PurchaseInput, isConsumable: Boolean?) {
        ensureConnection()
        val transactionId = purchase.id

        // Find the transaction from the queue instead of using cache
        val queue = SKPaymentQueue.defaultQueue()
        val transactions = queue.transactions() ?: emptyList<Any>()

        val transaction = transactions.firstOrNull { trans ->
            val skTransaction = trans as? SKPaymentTransaction
            skTransaction?.transactionIdentifier == transactionId
        } as? SKPaymentTransaction

        if (transaction == null) {
            // Transaction not found in queue - it's likely already finished
            // For restored purchases, this is normal and should be considered success
            println("[KMP-IAP] Transaction not found in queue (likely already finished): $transactionId")
            return
        }

        queue.finishTransaction(transaction)
        println("[KMP-IAP] Transaction finished: $transactionId")
    }
    
    override suspend fun getStorefrontIOS(): String {
        if (NSClassFromString("SKStorefront") == null) {
            return "US"
        }
        
        val storefront = SKPaymentQueue.defaultQueue().storefront ?: return "US"
        return storefront.countryCode
    }
    
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun presentCodeRedemptionSheetIOS(): Boolean {
        ensureConnection()
        if (NSClassFromString("SKPaymentQueue") != null) {
            val queue = SKPaymentQueue.defaultQueue()
            if (queue.respondsToSelector(NSSelectorFromString("presentCodeRedemptionSheet"))) {
                queue.performSelector(NSSelectorFromString("presentCodeRedemptionSheet"))
                return true
            }
        }
        return false
    }
    
    override suspend fun showManageSubscriptionsIOS(): List<PurchaseIOS> {
        ensureConnection()
        val url = NSURL(string = "https://apps.apple.com/account/subscriptions")
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>()) { _ -> }
        }
        return emptyList()
    }
    
    // New methods from updated interface
    override suspend fun validateReceipt(options: ValidationOptions): ValidationResult {
        ensureConnection()
        val receiptData = readReceiptData()
            ?: failWithPurchase(ErrorCode.ReceiptFailed, "App Store receipt not found")

        val purchases = getAvailablePurchases(null)
        val matchingPurchase = purchases.firstOrNull { it.productId == options.sku }
        val isValid = matchingPurchase != null

        if (!isValid) {
            _purchaseErrorListener.tryEmit(
                PurchaseError(
                    code = ErrorCode.ReceiptFailed,
                    message = "No matching purchase for SKU ${options.sku}"
                )
            )
        }

        return ReceiptValidationResultIOS(
            isValid = isValid,
            jwsRepresentation = "",
            latestTransaction = matchingPurchase,
            receiptData = receiptData
        )
    }
    
    suspend fun finishTransactionIOS(transactionId: String) {
        ensureConnection()

        // Find the transaction from the queue instead of using cache
        val queue = SKPaymentQueue.defaultQueue()
        val transactions = queue.transactions() ?: return
        
        val transaction = transactions.firstOrNull { trans ->
            val skTransaction = trans as? SKPaymentTransaction
            skTransaction?.transactionIdentifier == transactionId
        } as? SKPaymentTransaction ?: return
        
        queue.finishTransaction(transaction)
    }
    
    override suspend fun clearTransactionIOS(): Boolean {
        ensureConnection()
        val queue = SKPaymentQueue.defaultQueue()
        val transactions = queue.transactions() ?: return false
        transactions.forEach { transaction ->
            queue.finishTransaction(transaction as SKPaymentTransaction)
        }
        return true
    }
    
    suspend fun clearProductsIOS() {
        // No cache to clear anymore
    }
    
    override suspend fun getPromotedProductIOS(): ProductIOS? {
        ensureConnection()
        return promotedProduct?.toProduct() as? ProductIOS
    }

    override suspend fun requestPurchaseOnPromotedProductIOS(): Boolean {
        ensureConnection()
        val product = promotedProduct ?: return false
        val queue = SKPaymentQueue.defaultQueue()
        return runCatching {
            val payment = SKMutablePayment.paymentWithProduct(product)
            dispatch_async(dispatch_get_main_queue()) {
                queue.addPayment(payment)
            }
            true
        }.getOrDefault(false)
    }

    override suspend fun syncIOS(): Boolean {
        ensureConnection()
        return false
    }

    override suspend fun beginRefundRequestIOS(sku: String): String? {
        ensureConnection()
        return null
    }
    
    override suspend fun acknowledgePurchaseAndroid(purchaseToken: String): Boolean {
        // No-op on iOS
        return false
    }

    override suspend fun consumePurchaseAndroid(purchaseToken: String): Boolean {
        // No-op on iOS
        return false
    }
    
    override suspend fun deepLinkToSubscriptions(options: DeepLinkOptions?) {
        showManageSubscriptionsIOS()
    }
    
    override suspend fun currentEntitlementIOS(sku: String): PurchaseIOS? = latestTransactionIOS(sku)

    override suspend fun getActiveSubscriptions(subscriptionIds: List<String>?): List<ActiveSubscription> = emptyList()

    override suspend fun getAppTransactionIOS(): AppTransaction? = null

    override suspend fun getPendingTransactionsIOS(): List<PurchaseIOS> {
        ensureConnection()
        val queue = SKPaymentQueue.defaultQueue()
        val transactions = queue.transactions() ?: emptyList<Any>()
        return transactions.mapNotNull { (it as? SKPaymentTransaction)?.toPurchase() as? PurchaseIOS }
    }

    override suspend fun getReceiptDataIOS(): String? = readReceiptData()

    override suspend fun getTransactionJwsIOS(sku: String): String? = null

    override suspend fun hasActiveSubscriptions(subscriptionIds: List<String>?): Boolean =
        getActiveSubscriptions(subscriptionIds).isNotEmpty()

    override suspend fun isEligibleForIntroOfferIOS(groupID: String): Boolean = false

    override suspend fun isTransactionVerifiedIOS(sku: String): Boolean = false

    override suspend fun latestTransactionIOS(sku: String): PurchaseIOS? {
        ensureConnection()
        return getAvailablePurchases(null)
            .filterIsInstance<PurchaseIOS>()
            .firstOrNull { it.productId == sku }
    }

    override suspend fun subscriptionStatusIOS(sku: String): List<SubscriptionStatusIOS> = emptyList()

    override suspend fun validateReceiptIOS(options: ReceiptValidationProps): ReceiptValidationResultIOS {
        val result = validateReceipt(options)
        return result as? ReceiptValidationResultIOS
            ?: ReceiptValidationResultIOS(
                isValid = false,
                jwsRepresentation = "",
                receiptData = readReceiptData() ?: ""
            )
    }

    override suspend fun promotedProductIOS(): String = promotedProductListener.first() ?: ""

    override suspend fun purchaseError(): PurchaseError = purchaseErrorListener.first()

    override suspend fun purchaseUpdated(): Purchase = purchaseUpdatedListener.first()

    suspend fun isPurchaseValid(purchase: Purchase): Boolean =
        purchase.purchaseToken?.isNotEmpty() == true || purchase.id.isNotEmpty()
    
    
    // Internal iOS receipt validation - not in interface
    private suspend fun validateReceiptIos(
        receiptBody: Map<String, String>,
        isTest: Boolean
    ): Map<String, Any>? = null
    
    // Not applicable for iOS - not in interface
    private suspend fun validateReceiptAndroid(
        packageName: String,
        productId: String,
        productToken: String,
        accessToken: String,
        isSub: Boolean
    ): Map<String, Any>? = null
    
    override fun getStore(): Store = Store.APP_STORE
    
    override suspend fun canMakePayments(): Boolean {
        return SKPaymentQueue.canMakePayments()
    }
    
    // Private helper methods
    private fun cleanupState() {
        // Clear all pending requests and caches to avoid race conditions
        productRequests.forEach { (request, _) ->
            request.cancel()
        }
        productRequests.clear()
        // No transaction cache to clear anymore
        restoredPurchases.clear()
        restoreCompletion = null
        hasRestoredOnce = false
        promotedProduct = null
    }

    private fun readReceiptData(): String? {
        val receiptUrl = NSBundle.mainBundle.appStoreReceiptURL ?: return null
        val data = NSData.dataWithContentsOfURL(receiptUrl) ?: return null
        return data.base64EncodedStringWithOptions(0u)
    }
    
    private suspend fun ensureConnection() {
        if (!isConnected) {
            throw PurchaseException(
                PurchaseError(
                    code = ErrorCode.ServiceError,
                    message = "IAP connection not initialized. Call initConnection() first."
                )
            )
        }
    }

    private fun handlePurchasedTransaction(transaction: SKPaymentTransaction) {
        if (!isRestoring) {
            val purchase = transaction.toPurchase()
            // Don't cache transactions - they should be handled immediately
            _purchaseUpdatedListener.tryEmit(purchase)
        }
    }
    
    private fun handleFailedTransaction(transaction: SKPaymentTransaction) {
        val nsError = transaction.error as? NSError
        val errorCode = if (nsError?.domain == SKErrorDomain) {
            when (nsError.code.toLong()) {
                0L -> ErrorCode.Unknown          // SKErrorUnknown
                1L -> ErrorCode.DeveloperError   // SKErrorClientInvalid
                2L -> ErrorCode.UserCancelled    // SKErrorPaymentCancelled
                3L -> ErrorCode.DeveloperError   // SKErrorPaymentInvalid
                4L -> ErrorCode.UserError        // SKErrorPaymentNotAllowed
                5L -> ErrorCode.ItemUnavailable  // SKErrorStoreProductNotAvailable
                else -> ErrorCode.Unknown
            }
        } else {
            ErrorCode.Unknown
        }

        _purchaseErrorListener.tryEmit(
            PurchaseError(
                code = errorCode,
                message = nsError?.localizedDescription ?: "Purchase failed",
            )
        )

        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
    }
    
    private fun handleRestoredTransaction(transaction: SKPaymentTransaction) {
        val purchase = transaction.toPurchase()
        restoredPurchases.add(purchase)
        // Don't cache transactions - finish them immediately
        
        // Also finish restored transactions to prevent them from being reported again
        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
    }
}

// Extension functions
private fun SKProduct.toProduct(): Product {
    return ProductIOS(
        currency = runCatching { priceLocale.currencyCode }.getOrNull() ?: "USD",
        description = runCatching { localizedDescription }.getOrNull() ?: "",
        displayNameIOS = runCatching { localizedTitle }.getOrNull() ?: productIdentifier,
        displayPrice = runCatching { localizedPrice() }.getOrNull() ?: price.toString(),
        id = productIdentifier,
        isFamilyShareableIOS = runCatching { isFamilyShareable }.getOrNull() ?: false,
        jsonRepresentationIOS = "{}",
        platform = IapPlatform.Ios,
        price = price.doubleValue,
        subscriptionInfoIOS = null,
        title = runCatching { localizedTitle }.getOrNull() ?: productIdentifier,
        type = ProductType.InApp,
        typeIOS = ProductTypeIOS.NonConsumable
    )
}

private fun SKProduct.localizedPrice(): String {
    return try {
        val formatter = NSNumberFormatter()
        formatter.numberStyle = NSNumberFormatterCurrencyStyle
        formatter.locale = priceLocale
        formatter.stringFromNumber(price) ?: price.toString()
    } catch (e: Exception) {
        price.toString()
    }
}
private fun SKProductPeriodUnit?.toUnitEnum(): SubscriptionPeriodIOS? {
    return when ((this as? NSNumber)?.longValue) {
        0L -> SubscriptionPeriodIOS.Day
        1L -> SubscriptionPeriodIOS.Week
        2L -> SubscriptionPeriodIOS.Month
        3L -> SubscriptionPeriodIOS.Year
        else -> null
    }
}

private fun SKPaymentTransaction.toPurchase(): Purchase {
    val state = when (transactionState) {
        SKPaymentTransactionState.SKPaymentTransactionStatePurchasing -> PurchaseState.Pending
        SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> PurchaseState.Purchased
        SKPaymentTransactionState.SKPaymentTransactionStateFailed -> PurchaseState.Failed
        SKPaymentTransactionState.SKPaymentTransactionStateRestored -> PurchaseState.Restored
        SKPaymentTransactionState.SKPaymentTransactionStateDeferred -> PurchaseState.Deferred
        else -> PurchaseState.Unknown
    }

    val ids = payment?.productIdentifier?.let { listOf(it) }

    return PurchaseIOS(
        id = transactionIdentifier ?: payment?.productIdentifier ?: "unknown",
        ids = ids,
        isAutoRenewing = false,
        originalTransactionDateIOS = originalTransaction?.transactionDate?.timeIntervalSince1970,
        originalTransactionIdentifierIOS = originalTransaction?.transactionIdentifier,
        platform = IapPlatform.Ios,
        productId = payment?.productIdentifier ?: "",
        purchaseState = state,
        purchaseToken = transactionIdentifier,
        quantity = payment?.quantity?.toInt() ?: 1,
        transactionDate = transactionDate?.timeIntervalSince1970 ?: 0.0,
        transactionId = transactionIdentifier ?: payment?.productIdentifier ?: "unknown",
        transactionReasonIOS = when (transactionState) {
            SKPaymentTransactionState.SKPaymentTransactionStateRestored -> "RESTORED"
            SKPaymentTransactionState.SKPaymentTransactionStateDeferred -> "DEFERRED"
            else -> "PURCHASE"
        }
    )
}
