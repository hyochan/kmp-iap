package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.types.*
import io.github.hyochan.kmpiap.utils.ErrorCode
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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

internal class IosInAppPurchase : KmpInAppPurchase {
    // Event flows
    private val _purchaseUpdatedFlow = MutableSharedFlow<Purchase>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val purchaseUpdatedFlow: Flow<Purchase> = _purchaseUpdatedFlow.asSharedFlow()
    
    private val _purchaseErrorFlow = MutableSharedFlow<PurchaseError>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val purchaseErrorFlow: Flow<PurchaseError> = _purchaseErrorFlow.asSharedFlow()
    
    private val _connectionStateFlow = MutableSharedFlow<ConnectionResult>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val connectionStateFlow: Flow<ConnectionResult> = _connectionStateFlow.asSharedFlow()
    
    private val _promotedProductFlow = MutableSharedFlow<String?>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val promotedProductFlow: Flow<String?> = _promotedProductFlow.asSharedFlow()
    
    // Private properties
    private val productCache = mutableMapOf<String, SKProduct>()
    private val transactionCache = mutableMapOf<String, SKPaymentTransaction>()
    private val productRequests = mutableMapOf<SKProductsRequest, (List<SKProduct>, Set<*>) -> Unit>()
    private val restoredPurchases = mutableListOf<Purchase>()
    private var restoreCompletion: (() -> Unit)? = null
    private var isConnected = false
    private var isRestoring = false
    private var hasRestoredOnce = false
    
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
            _purchaseErrorFlow.tryEmit(
                PurchaseError(
                    message = "Purchase is deferred and waiting for approval",
                    code = ErrorCode.E_DEFERRED_PAYMENT,
                    productId = transaction.payment.productIdentifier
                )
            )
        }
        
        paymentObserver.restoreCompletionHandler = {
            isRestoring = false
            restoreCompletion?.invoke()
        }
        
        paymentObserver.restoreFailureHandler = { error ->
            isRestoring = false
            _purchaseErrorFlow.tryEmit(
                PurchaseError(
                    message = error.localizedDescription,
                    code = ErrorCode.E_RESTORE_FAILED
                )
            )
            restoreCompletion?.invoke()
        }
        
        paymentObserver.promotedProductHandler = { productId, product ->
            _promotedProductFlow.tryEmit(productId)
            productCache[productId] = product
        }
        
        paymentObserver.productsResponseHandler = { request, products, invalidIds ->
            // Cache products
            products.forEach { product ->
                productCache[product.productIdentifier] = product
            }
            
            productRequests[request]?.invoke(products, invalidIds)
            productRequests.remove(request)
        }
        
        paymentObserver.productRequestFailureHandler = { request, error ->
            productRequests.remove(request)
            _purchaseErrorFlow.tryEmit(
                PurchaseError(
                    message = error.localizedDescription,
                    code = ErrorCode.E_PRODUCT_LOAD_FAILED
                )
            )
        }
    }
    
    override fun getVersion(): String {
        return "KMP-IAP v1.0.0-alpha02 (iOS)"
    }
    
    override suspend fun initConnection() {
        // Clean up any existing state from hot reload
        cleanupState()
        
        // Add transaction observer
        SKPaymentQueue.defaultQueue().addTransactionObserver(paymentObserver)
        isConnected = true
        hasRestoredOnce = false
        _connectionStateFlow.emit(ConnectionResult(connected = true, message = "Connected to App Store"))
    }
    
    override suspend fun endConnection() {
        SKPaymentQueue.defaultQueue().removeTransactionObserver(paymentObserver)
        isConnected = false
        cleanupState()
        _connectionStateFlow.emit(ConnectionResult(connected = false, message = "Disconnected from App Store"))
    }
    
    override suspend fun requestProducts(params: RequestProductsParams): List<BaseProduct> {
        ensureConnection()
        return suspendCancellableCoroutine { continuation ->
            try {
                val productIds = params.skus.toSet()
                val request = SKProductsRequest(productIdentifiers = productIds)
                
                productRequests[request] = { products, invalidIds ->
                    productRequests.remove(request)
                    
                    // Note: invalidIds contains products not found in the store
                    
                    try {
                        val productList = products.map { skProduct ->
                            when (params.type) {
                                PurchaseType.INAPP -> skProduct.toProduct()
                                PurchaseType.SUBS -> skProduct.toSubscription()
                            }
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
    
    override suspend fun requestPurchase(request: RequestPurchase, type: PurchaseType) {
        ensureConnection()
        val sku = when (request) {
            is RequestPurchaseIOS -> request.sku
            is RequestPurchaseAndroid -> throw PurchaseError(
                message = "Android request on iOS platform",
                code = ErrorCode.E_DEVELOPER_ERROR
            )
            is RequestPurchaseGeneric -> request.sku
            else -> throw PurchaseError(
                message = "Invalid request type: ${request::class.simpleName}",
                code = ErrorCode.E_DEVELOPER_ERROR
            )
        }
        
        val quantity = when (request) {
            is RequestPurchaseIOS -> request.quantity
            is RequestPurchaseGeneric -> 1
            else -> 1
        }
        
        val appAccountToken = when (request) {
            is RequestPurchaseIOS -> request.appAccountToken
            else -> null
        }
        
        // Check if product is already in cache first
        var skProduct = productCache[sku]
        
        if (skProduct == null) {
            // Only fetch if not in cache
            val products = requestProducts(RequestProductsParams(listOf(sku), type))
            if (products.isEmpty()) {
                throw PurchaseError(
                    message = "Product not found: $sku",
                    code = ErrorCode.E_ITEM_UNAVAILABLE
                )
            }
            
            skProduct = productCache[sku] ?: throw PurchaseError(
                message = "Product not in cache after fetch: $sku",
                code = ErrorCode.E_PRODUCT_NOT_FOUND
            )
        }
        
        try {
            val payment = SKMutablePayment.paymentWithProduct(skProduct)
            
            payment.setQuantity(quantity?.toLong() ?: 1)
            
            appAccountToken?.let { token ->
                payment.setApplicationUsername(token)
            }
            
            // Check if default queue exists
            val queue = SKPaymentQueue.defaultQueue()
            
            // Ensure we're on main thread for StoreKit operations
            dispatch_async(dispatch_get_main_queue()) {
                try {
                    queue.addPayment(payment)
                } catch (e: Exception) {
                    // Silent fail - will be handled by transaction observer
                }
            }
        } catch (e: Exception) {
            throw PurchaseError(
                message = "Failed to create payment: ${e.message}",
                code = ErrorCode.E_PURCHASE_FAILED
            )
        }
    }
    
    override suspend fun getAvailablePurchases(): List<Purchase> {
        ensureConnection()
        return suspendCancellableCoroutine { continuation ->
            isRestoring = true
            SKPaymentQueue.defaultQueue().restoreCompletedTransactions()
            
            restoredPurchases.clear()
            restoreCompletion = {
                isRestoring = false
                if (continuation.isActive) {
                    continuation.resume(restoredPurchases.toList())
                }
                restoreCompletion = null
            }
        }
    }
    
    override suspend fun getPurchaseHistories(): List<Purchase> {
        // iOS doesn't have a separate purchase history API
        // Return available purchases instead
        return getAvailablePurchases()
    }
    
    override suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean): Boolean {
        ensureConnection()
        val transactionId = purchase.transactionId ?: return false
        val transaction = transactionCache[transactionId] ?: return false
        
        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
        transactionCache.remove(transactionId)
        return true
    }
    
    override suspend fun getStorefrontIOS(): AppStoreInfo? {
        if (NSClassFromString("SKStorefront") == null) {
            return null
        }
        
        val storefront = SKPaymentQueue.defaultQueue().storefront ?: return null
        return AppStoreInfo(
            countryCode = storefront.countryCode,
            identifier = storefront.identifier
        )
    }
    
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun presentCodeRedemptionSheetIOS() {
        ensureConnection()
        if (NSClassFromString("SKPaymentQueue") != null) {
            val queue = SKPaymentQueue.defaultQueue()
            if (queue.respondsToSelector(NSSelectorFromString("presentCodeRedemptionSheet"))) {
                queue.performSelector(NSSelectorFromString("presentCodeRedemptionSheet"))
            }
        }
    }
    
    override suspend fun showManageSubscriptionsIOS() {
        ensureConnection()
        val url = NSURL(string = "https://apps.apple.com/account/subscriptions")
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>()) { _ -> }
        }
    }
    
    override suspend fun deepLinkToSubscriptionsAndroid(sku: String?) {
        // No-op on iOS
    }
    
    override suspend fun acknowledgePurchaseAndroid(purchaseToken: String): Boolean = false
    
    override suspend fun consumePurchaseAndroid(purchaseToken: String): Boolean = false
    
    override suspend fun validateReceiptIos(
        receiptBody: Map<String, String>,
        isTest: Boolean
    ): Map<String, Any>? {
        return suspendCancellableCoroutine { continuation ->
            val receiptData = receiptBody["receipt-data"] ?: run {
                continuation.resumeWithException(
                    PurchaseError(
                        message = "Missing receipt-data",
                        code = ErrorCode.E_RECEIPT_FAILED
                    )
                )
                return@suspendCancellableCoroutine
            }
            
            val url = if (isTest) {
                "https://sandbox.itunes.apple.com/verifyReceipt"
            } else {
                "https://buy.itunes.apple.com/verifyReceipt"
            }
            
            val request = NSMutableURLRequest(uRL = NSURL(string = url)!!)
            request.HTTPMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField = "Content-Type")
            
            val jsonData = try {
                @OptIn(ExperimentalForeignApi::class)
                NSJSONSerialization.dataWithJSONObject(receiptBody, 0u, null)
            } catch (e: Exception) {
                continuation.resumeWithException(
                    PurchaseError(
                        message = "Failed to serialize receipt data",
                        code = ErrorCode.E_RECEIPT_FAILED
                    )
                )
                return@suspendCancellableCoroutine
            }
            
            request.HTTPBody = jsonData
            
            val session = NSURLSession.sharedSession
            val task = session.dataTaskWithRequest(request) { data, response, error ->
                if (error != null) {
                    continuation.resumeWithException(
                        PurchaseError(
                            message = error.localizedDescription,
                            code = ErrorCode.E_RECEIPT_FAILED
                        )
                    )
                    return@dataTaskWithRequest
                }
                
                data?.let { responseData ->
                    try {
                        @OptIn(ExperimentalForeignApi::class)
                        val json = NSJSONSerialization.JSONObjectWithData(responseData, 0u, null)
                        @Suppress("UNCHECKED_CAST")
                        val result = json as? Map<String, Any>
                        continuation.resume(result)
                    } catch (e: Exception) {
                        continuation.resumeWithException(
                            PurchaseError(
                                message = "Failed to parse validation response",
                                code = ErrorCode.E_RECEIPT_FAILED
                            )
                        )
                    }
                } ?: continuation.resume(null)
            }
            
            task.resume()
        }
    }
    
    override suspend fun validateReceiptAndroid(
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
        transactionCache.clear()
        restoredPurchases.clear()
        restoreCompletion = null
        hasRestoredOnce = false
    }
    
    private suspend fun ensureConnection() {
        if (!isConnected) {
            throw PurchaseError(
                message = "IAP connection not initialized. Call initConnection() first.",
                code = ErrorCode.E_NOT_INITIALIZED
            )
        }
    }
    
    private fun handlePurchasedTransaction(transaction: SKPaymentTransaction) {
        if (!isRestoring) {
            val purchase = transaction.toPurchase()
            transactionCache[transaction.transactionIdentifier ?: ""] = transaction
            _purchaseUpdatedFlow.tryEmit(purchase)
        }
    }
    
    private fun handleFailedTransaction(transaction: SKPaymentTransaction) {
        val error = transaction.error
        val errorCode = when ((error as? NSError)?.code) {
            2L -> ErrorCode.E_USER_CANCELLED
            0L -> ErrorCode.E_PAYMENT_INVALID
            1L -> ErrorCode.E_PAYMENT_NOT_ALLOWED
            5L -> ErrorCode.E_ITEM_UNAVAILABLE
            else -> ErrorCode.E_PURCHASE_FAILED
        }
        
        _purchaseErrorFlow.tryEmit(
            PurchaseError(
                message = error?.localizedDescription ?: "Purchase failed",
                code = errorCode,
                productId = transaction.payment.productIdentifier
            )
        )
        
        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
    }
    
    private fun handleRestoredTransaction(transaction: SKPaymentTransaction) {
        val purchase = transaction.toPurchase()
        restoredPurchases.add(purchase)
        transactionCache[transaction.transactionIdentifier ?: ""] = transaction
        
        // Also finish restored transactions to prevent them from being reported again
        SKPaymentQueue.defaultQueue().finishTransaction(transaction)
    }
}

// Extension functions
private fun SKProduct.toProduct(): Product {
    return Product(
        productId = productIdentifier,
        price = price.doubleValue.toString(),
        currency = try { priceLocale.currencyCode } catch (e: Exception) { null } ?: "USD",
        localizedPrice = try { localizedPrice() } catch (e: Exception) { price.toString() },
        title = try { localizedTitle } catch (e: Exception) { productIdentifier },
        description = try { localizedDescription } catch (e: Exception) { "" },
        platform = IAPPlatform.IOS,
        type = ProductType.INAPP,
        priceAmountMicros = (price.doubleValue * 1_000_000).toLong(),
        isFamilyShareable = try { isFamilyShareable } catch (e: Exception) { null },
        discountsIOS = try { 
            discounts?.map { discount -> (discount as SKProductDiscount).toDiscountIOS() }
        } catch (e: Exception) { null }
    )
}

private fun SKProduct.toSubscription(): Subscription {
    return Subscription(
        productId = productIdentifier,
        price = price.doubleValue.toString(),
        currency = try { priceLocale.currencyCode } catch (e: Exception) { null } ?: "USD",
        localizedPrice = try { localizedPrice() } catch (e: Exception) { price.toString() },
        title = try { localizedTitle } catch (e: Exception) { productIdentifier },
        description = try { localizedDescription } catch (e: Exception) { "" },
        platform = IAPPlatform.IOS,
        type = ProductType.SUBS,
        priceAmountMicros = (price.doubleValue * 1_000_000).toLong(),
        subscriptionPeriodUnitIOS = try { subscriptionPeriod?.unit?.toUnitString() } catch (e: Exception) { null },
        subscriptionPeriodNumberIOS = try { subscriptionPeriod?.numberOfUnits?.toInt() } catch (e: Exception) { null },
        isFamilyShareable = try { isFamilyShareable } catch (e: Exception) { null },
        subscriptionGroupId = try { subscriptionGroupIdentifier } catch (e: Exception) { null },
        introductoryPrice = try { introductoryPrice?.localizedPrice() } catch (e: Exception) { null },
        introductoryPriceNumberOfPeriodsIOS = try { introductoryPrice?.numberOfPeriods?.toInt() } catch (e: Exception) { null },
        introductoryPriceSubscriptionPeriod = try { introductoryPrice?.subscriptionPeriod?.toReadableString() } catch (e: Exception) { null }
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

private fun SKProductDiscount.toDiscountIOS(): DiscountIOS {
    return DiscountIOS(
        identifier = identifier,
        type = when ((type as NSNumber).longValue) {
            0L -> "introductory"
            1L -> "subscription"
            else -> "unknown"
        },
        numberOfPeriods = numberOfPeriods.toString(),
        price = price.doubleValue,
        localizedPrice = localizedPrice(),
        paymentMode = when ((paymentMode as NSNumber).longValue) {
            0L -> "payAsYouGo"
            1L -> "payUpFront"
            2L -> "freeTrial"
            else -> "unknown"
        },
        subscriptionPeriod = subscriptionPeriod.toReadableString()
    )
}

private fun SKProductDiscount.localizedPrice(): String {
    return try {
        val formatter = NSNumberFormatter()
        formatter.numberStyle = NSNumberFormatterCurrencyStyle
        formatter.locale = priceLocale
        formatter.stringFromNumber(price) ?: price.toString()
    } catch (e: Exception) {
        price.toString()
    }
}

private fun SKProductSubscriptionPeriod.toReadableString(): String {
    val unitStr = unit.toUnitString()
    return "$numberOfUnits $unitStr"
}

private fun SKProductPeriodUnit.toUnitString(): String {
    return when ((this as NSNumber).longValue) {
        0L -> "day"
        1L -> "week"
        2L -> "month"
        3L -> "year"
        else -> "unknown"
    }
}

private fun SKPaymentTransaction.toPurchase(): Purchase {
    return Purchase(
        productId = payment.productIdentifier,
        transactionId = transactionIdentifier,
        transactionReceipt = NSBundle.mainBundle.appStoreReceiptURL?.path?.let { path ->
            NSData.dataWithContentsOfFile(path)?.base64EncodedStringWithOptions(0u)
        },
        transactionDate = transactionDate?.let {
            kotlinx.datetime.Instant.fromEpochSeconds(it.timeIntervalSince1970.toLong())
        },
        platform = IAPPlatform.IOS,
        originalTransactionIdentifierIOS = originalTransaction?.transactionIdentifier,
        transactionState = when (transactionState) {
            SKPaymentTransactionState.SKPaymentTransactionStatePurchasing -> "purchasing"
            SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> "purchased"
            SKPaymentTransactionState.SKPaymentTransactionStateFailed -> "failed"
            SKPaymentTransactionState.SKPaymentTransactionStateRestored -> "restored"
            SKPaymentTransactionState.SKPaymentTransactionStateDeferred -> "deferred"
            else -> "unknown"
        }
    )
}