package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.types.*
import io.github.hyochan.kmpiap.utils.ErrorCode
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    
    // Connection state - exposed for backward compatibility
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
    
    // Private properties
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
            _purchaseErrorListener.tryEmit(
                PurchaseError(
                    code = ErrorCode.E_PENDING.name,
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
                    code = ErrorCode.E_UNKNOWN.name,
                    message = "Restore failed: ${error.localizedDescription}"
                )
            )
            restoreCompletion?.invoke()
        }
        
        paymentObserver.promotedProductHandler = { productId, product ->
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
                    code = ErrorCode.E_SERVICE_ERROR.name,
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
    
    override suspend fun endConnection() {
        SKPaymentQueue.defaultQueue().removeTransactionObserver(paymentObserver)
        isConnected = false
        cleanupState()
        _connectionStateListener.emit(ConnectionResult(connected = false, message = "Disconnected from App Store"))
    }
    
    override suspend fun requestProducts(params: ProductRequest): List<Product> {
        ensureConnection()
        return suspendCancellableCoroutine { continuation ->
            try {
                val request = SKProductsRequest(productIdentifiers = params.skus.toSet())
                
                productRequests[request] = { products, invalidIds ->
                    productRequests.remove(request)
                    
                    // Note: invalidIds contains products not found in the store
                    
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
    
    override suspend fun requestPurchase(
        sku: String,
        ios: RequestPurchaseIosProps?,
        android: RequestPurchaseAndroidProps?
    ): Purchase {
        ensureConnection()
        
        val quantity = ios?.quantity ?: 1
        val appAccountToken = ios?.appAccountToken
        
        // Fetch product details
        val products = requestProducts(ProductRequest(listOf(sku), ProductType.INAPP))
        if (products.isEmpty()) {
            val error = PurchaseError(
                code = ErrorCode.E_ITEM_UNAVAILABLE.name,
                message = "Product not found: $sku",
            )
            _purchaseErrorListener.tryEmit(error)
            throw error
        }
        
        // Get SKProduct from fetched products
        val productList = suspendCancellableCoroutine<List<SKProduct>> { continuation ->
            val request = SKProductsRequest(productIdentifiers = setOf(sku))
            productRequests[request] = { skProducts, _ ->
                if (continuation.isActive) {
                    continuation.resume(skProducts)
                }
            }
            request.delegate = paymentObserver
            request.start()
        }
        
        val skProduct = productList.firstOrNull()
        if (skProduct == null) {
            val error = PurchaseError(
                code = ErrorCode.E_ITEM_UNAVAILABLE.name,
                message = "Failed to fetch product: $sku",
            )
            _purchaseErrorListener.tryEmit(error)
            throw error
        }
        
        try {
            val payment = SKMutablePayment.paymentWithProduct(skProduct)
            
            payment.setQuantity(quantity.toLong())
            
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
            return PurchaseIOS(
                id = sku,  // Use SKU as temporary ID for immediate return
                productId = sku,
                transactionDate = Clock.System.now().epochSeconds.toDouble(),
                transactionReceipt = "",
                platform = "ios"
            )
        } catch (e: Exception) {
            val error = PurchaseError(
                code = ErrorCode.E_UNKNOWN.name,
                message = "Failed to create payment: ${e.message}",
            )
            _purchaseErrorListener.tryEmit(error)
            throw error
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
    
    override suspend fun getPurchaseHistories(options: PurchaseOptions?): List<Purchase> {
        // iOS doesn't have a separate purchase history API
        // Return available purchases directly
        return getAvailablePurchases(options)
    }
    
    override suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean?): Boolean {
        ensureConnection()
        val transactionId = purchase.id ?: return false
        
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
            return true
        }
        
        queue.finishTransaction(transaction)
        println("[KMP-IAP] Transaction finished: $transactionId")
        return true
    }
    
    override suspend fun getStorefrontIOS(): String {
        if (NSClassFromString("SKStorefront") == null) {
            return "US"
        }
        
        val storefront = SKPaymentQueue.defaultQueue().storefront ?: return "US"
        return storefront.countryCode
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
    
    // iOS subscription management - not in interface
    private suspend fun showManageSubscriptionsIOS() {
        ensureConnection()
        val url = NSURL(string = "https://apps.apple.com/account/subscriptions")
        if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>()) { _ -> }
        }
    }
    
    // New methods from updated interface
    override suspend fun validateReceipt(options: ValidationOptions): ValidationResult {
        return when (options) {
            is ValidationOptions.IOSValidation -> {
                val receiptMap = mapOf(
                    "receipt-data" to options.receiptBody.receiptData,
                    "password" to (options.receiptBody.password ?: "")
                )
                val result = validateReceiptIos(receiptMap, true)
                ValidationResult(
                    isValid = result != null,
                    status = (result?.get("status") as? Int) ?: -1,
                    receipt = result
                )
            }
            is ValidationOptions.AndroidValidation -> {
                ValidationResult(
                    isValid = false,
                    status = -1
                )
            }
        }
    }
    
    override suspend fun isPurchaseValid(purchase: Purchase): Boolean {
        return purchase.transactionReceipt.isNotEmpty()
    }
    
    override suspend fun finishTransactionIOS(transactionId: String) {
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
    
    override suspend fun clearTransactionIOS() {
        ensureConnection()
        val queue = SKPaymentQueue.defaultQueue()
        queue.transactions?.forEach { transaction ->
            queue.finishTransaction(transaction as SKPaymentTransaction)
        }
        // No cache to clear anymore
    }
    
    override suspend fun clearProductsIOS() {
        // No cache to clear anymore
    }
    
    override suspend fun getPromotedProductIOS(): String? {
        // Return last promoted product from flow
        return null // Would need to track this separately
    }
    
    override suspend fun buyPromotedProductIOS() {
        // Would need to track promoted product and buy it
    }
    
    override suspend fun acknowledgePurchaseAndroid(purchaseToken: String) {
        // No-op on iOS
    }
    
    override suspend fun consumePurchaseAndroid(purchaseToken: String) {
        // No-op on iOS
    }
    
    override suspend fun deepLinkToSubscriptions(options: DeepLinkOptions) {
        // No-op on iOS
    }
    
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun getActiveSubscriptions(subscriptionIds: List<String>?): List<ActiveSubscription> {
        return suspendCancellableCoroutine { continuation ->
            dispatch_async(dispatch_get_main_queue()) {
                val transactions = SKPaymentQueue.defaultQueue().transactions()
                val now = NSDate().timeIntervalSince1970
                
                // Use a map to keep only the most recent transaction per product ID
                val latestTransactionsByProduct = mutableMapOf<String, SKPaymentTransaction>()
                
                for (transaction in transactions) {
                    val skTransaction = transaction as SKPaymentTransaction
                    
                    // Get product ID from the payment
                    val productId = skTransaction.payment?.productIdentifier ?: continue
                    
                    // Filter by subscriptionIds if provided
                    if (subscriptionIds != null && !subscriptionIds.contains(productId)) {
                        continue
                    }
                    
                    // Only include purchased transactions
                    if (skTransaction.transactionState != SKPaymentTransactionState.SKPaymentTransactionStatePurchased) {
                        continue
                    }
                    
                    // Keep only the most recent transaction for each product ID
                    val existingTransaction = latestTransactionsByProduct[productId]
                    if (existingTransaction == null || 
                        (skTransaction.transactionDate?.timeIntervalSince1970 ?: 0.0) > 
                        (existingTransaction.transactionDate?.timeIntervalSince1970 ?: 0.0)) {
                        latestTransactionsByProduct[productId] = skTransaction
                    }
                }
                
                val activeSubscriptions = mutableListOf<ActiveSubscription>()
                
                // Process only the latest transaction for each unique product
                for ((productId, skTransaction) in latestTransactionsByProduct) {
                    // Product details not available without fetching
                    val product: SKProduct? = null
                    
                    // Calculate expiration info (simplified - actual implementation would need receipt validation)
                    val expirationDate = skTransaction.transactionDate?.let { purchaseDate ->
                        // For subscriptions, we'd need to parse the receipt to get actual expiration
                        // This is a simplified implementation
                        val purchaseTime = purchaseDate.timeIntervalSince1970
                        
                        // Estimate based on subscription period if available
                        val period = product?.subscriptionPeriod
                        if (period != null) {
                            val daysToAdd = when (period.unit) {
                                SKProductPeriodUnit.SKProductPeriodUnitDay -> period.numberOfUnits.toLong()
                                SKProductPeriodUnit.SKProductPeriodUnitWeek -> period.numberOfUnits.toLong() * 7
                                SKProductPeriodUnit.SKProductPeriodUnitMonth -> period.numberOfUnits.toLong() * 30
                                SKProductPeriodUnit.SKProductPeriodUnitYear -> period.numberOfUnits.toLong() * 365
                                else -> 0
                            }
                            val expirationSeconds = purchaseTime + (daysToAdd * 24 * 60 * 60)
                            Instant.fromEpochSeconds(expirationSeconds.toLong())
                        } else {
                            null
                        }
                    }
                    
val daysUntilExpiration: Int? = expirationDate?.let { exp ->
    val nowSeconds = now.toLong()
    ((exp.epochSeconds - nowSeconds) / (24 * 60 * 60)).toInt()
}
@@ Lines 596-599 in InAppPurchaseIOS.kt
- val willExpireSoon = daysUntilExpiration?.let { it in 0..7 }
- 
 val willExpireSoon = daysUntilExpiration?.let { it in 0..7 }
                    // Determine environment
                    val receiptURL = NSBundle.mainBundle.appStoreReceiptURL
                    val environment = if (receiptURL?.absoluteString?.contains("sandboxReceipt") == true) {
                        "Sandbox"
                    } else {
                        "Production"
                    }
                    
                    activeSubscriptions.add(
                        ActiveSubscription(
                            productId = productId,
                            isActive = true,
                            expirationDateIOS = expirationDate,
                            autoRenewingAndroid = null, // Android only
                            environmentIOS = environment,
                            willExpireSoon = willExpireSoon,
                            daysUntilExpirationIOS = daysUntilExpiration
                        )
                    )
                }
                
                continuation.resume(activeSubscriptions)
            }
        }
    }
    
    override suspend fun hasActiveSubscriptions(subscriptionIds: List<String>?): Boolean {
        val activeSubscriptions = getActiveSubscriptions(subscriptionIds)
        return activeSubscriptions.isNotEmpty()
    }
    
    
    // Internal iOS receipt validation - not in interface
    private suspend fun validateReceiptIos(
        receiptBody: Map<String, String>,
        isTest: Boolean
    ): Map<String, Any>? {
        return suspendCancellableCoroutine { continuation ->
            val receiptData = receiptBody["receipt-data"] ?: run {
                continuation.resumeWithException(
                    PurchaseError(
                        code = ErrorCode.E_RECEIPT_FAILED.name,
                        message = "Missing receipt-data"
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
                        code = ErrorCode.E_RECEIPT_FAILED.name,
                        message = "Failed to serialize receipt data"
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
                            code = ErrorCode.E_RECEIPT_FAILED.name,
                            message = error.localizedDescription
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
                                code = ErrorCode.E_RECEIPT_FAILED.name,
                                message = "Failed to parse validation response"
                            )
                        )
                    }
                } ?: continuation.resume(null)
            }
            
            task.resume()
        }
    }
    
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
    }
    
    private suspend fun ensureConnection() {
        if (!isConnected) {
            throw PurchaseError(
                code = ErrorCode.E_SERVICE_ERROR.name,
                message = "IAP connection not initialized. Call initConnection() first."
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
        val error = transaction.error
        val errorCode = when ((error as? NSError)?.code) {
            2L -> ErrorCode.E_USER_CANCELLED
            0L -> ErrorCode.E_DEVELOPER_ERROR  // Payment invalid
            1L -> ErrorCode.E_USER_ERROR       // Payment not allowed
            5L -> ErrorCode.E_ITEM_UNAVAILABLE
            else -> ErrorCode.E_UNKNOWN
        }
        
        _purchaseErrorListener.tryEmit(
            PurchaseError(
                code = errorCode.name,
                message = error?.localizedDescription ?: "Purchase failed",
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
        id = productIdentifier,
        title = try { localizedTitle } catch (e: Exception) { productIdentifier },
        description = try { localizedDescription } catch (e: Exception) { "" },
        type = ProductType.INAPP,
        displayPrice = try { localizedPrice() } catch (e: Exception) { price.toString() },
        price = price.doubleValue,
        currency = try { priceLocale.currencyCode } catch (e: Exception) { null } ?: "USD",
        displayNameIOS = try { localizedTitle } catch (e: Exception) { productIdentifier },
        isFamilyShareableIOS = try { isFamilyShareable } catch (e: Exception) { false },
        jsonRepresentationIOS = "", // Would need actual JSON representation
        subscriptionInfoIOS = try { 
            subscriptionPeriod?.let { 
                SubscriptionInfoIOS(
                    subscriptionGroupId = subscriptionGroupIdentifier ?: "",
                    subscriptionPeriod = SubscriptionOfferPeriod(
                        unit = it.unit.toUnitString().uppercase(),
                        value = it.numberOfUnits.toInt()
                    )
                )
            }
        } catch (e: Exception) { null },
        platform = "ios"
    )
}

private fun SKProduct.toSubscription(): Product {
    return ProductSubscriptionIOS(
        id = productIdentifier,
        title = try { localizedTitle } catch (e: Exception) { productIdentifier },
        description = try { localizedDescription } catch (e: Exception) { "" },
        type = ProductType.SUBS,
        displayPrice = try { localizedPrice() } catch (e: Exception) { price.toString() },
        price = price.doubleValue,
        currency = try { priceLocale.currencyCode } catch (e: Exception) { null } ?: "USD",
        displayNameIOS = try { localizedTitle } catch (e: Exception) { productIdentifier },
        isFamilyShareableIOS = try { isFamilyShareable } catch (e: Exception) { false },
        jsonRepresentationIOS = "", // Would need actual JSON representation
        subscriptionInfoIOS = try { 
            subscriptionPeriod?.let { 
                SubscriptionInfoIOS(
                    subscriptionGroupId = subscriptionGroupIdentifier ?: "",
                    subscriptionPeriod = SubscriptionOfferPeriod(
                        unit = it.unit.toUnitString().uppercase(),
                        value = it.numberOfUnits.toInt()
                    )
                )
            }
        } catch (e: Exception) { null },
        introductoryPriceIOS = try { introductoryPrice?.localizedPrice() } catch (e: Exception) { null },
        introductoryPriceNumberOfPeriodsIOS = try { introductoryPrice?.numberOfPeriods?.toString() } catch (e: Exception) { null },
        subscriptionPeriodUnitIOS = try { subscriptionPeriod?.unit?.toUnitString()?.uppercase() } catch (e: Exception) { null },
        subscriptionPeriodNumberIOS = try { subscriptionPeriod?.numberOfUnits?.toString() } catch (e: Exception) { null },
        platform = "ios"
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

private fun SKProductDiscount.toDiscount(): DiscountIOS {
    return DiscountIOS(
        identifier = identifier ?: "",
        type = when ((type as NSNumber).longValue) {
            0L -> "introductory"
            1L -> "subscription"
            else -> "unknown"
        },
        numberOfPeriods = numberOfPeriods.toString(),
        price = localizedPrice(),
        localizedPrice = localizedPrice(),
        paymentMode = when ((paymentMode as NSNumber).longValue) {
            0L -> "PAYASYOUGO"
            1L -> "PAYUPFRONT"
            2L -> "FREETRIAL"
            else -> ""
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
    return PurchaseIOS(
        id = transactionIdentifier ?: payment.productIdentifier,  // Primary identifier
        purchaseToken = transactionIdentifier,  // Unified purchase token (iOS uses transaction ID for StoreKit 1)
        transactionDate = transactionDate?.timeIntervalSince1970 ?: 0.0,
        transactionReceipt = NSBundle.mainBundle.appStoreReceiptURL?.path?.let { path ->
            NSData.dataWithContentsOfFile(path)?.base64EncodedStringWithOptions(0u)
        } ?: "",
        transactionId = transactionIdentifier,  // @deprecated - use id instead
        jwsRepresentationIOS = null,  // StoreKit 1 doesn't provide JWS, only available in StoreKit 2
        originalTransactionDateIOS = originalTransaction?.transactionDate?.timeIntervalSince1970,
        originalTransactionIdentifierIOS = originalTransaction?.transactionIdentifier,
        transactionState = when (transactionState) {
            SKPaymentTransactionState.SKPaymentTransactionStatePurchasing -> TransactionStateIOS.PURCHASING
            SKPaymentTransactionState.SKPaymentTransactionStatePurchased -> TransactionStateIOS.PURCHASED
            SKPaymentTransactionState.SKPaymentTransactionStateFailed -> TransactionStateIOS.FAILED
            SKPaymentTransactionState.SKPaymentTransactionStateRestored -> TransactionStateIOS.RESTORED
            SKPaymentTransactionState.SKPaymentTransactionStateDeferred -> TransactionStateIOS.DEFERRED
            else -> null
        },
        productId = payment?.productIdentifier ?: "",
        platform = "ios"
    )
}