package io.github.hyochan.kmpiap

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import io.github.hyochan.kmpiap.types.*
import io.github.hyochan.kmpiap.utils.ErrorCode
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


internal class AndroidInAppPurchase : KmpInAppPurchase, Application.ActivityLifecycleCallbacks {
    private var billingClient: BillingClient? = null
    private var isConnected = false
    private var context: Context? = null
    private var currentActivity: Activity? = null
    
    
    // Activity lifecycle callbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Automatically capture the first activity created
        if (currentActivity == null) {
            currentActivity = activity
            println("[KMP-IAP] Activity auto-captured on create: $activity")
        }
    }
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        println("[KMP-IAP] Activity resumed: $activity")
    }
    override fun onActivityPaused(activity: Activity) {
        // Don't clear the activity on pause, only on destroy
        // This prevents losing the activity reference during purchase flow
    }
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
            println("[KMP-IAP] Activity destroyed and cleared: $activity")
        }
    }
    
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
        replay = 1,
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
    
    
    override fun getVersion(): String {
        return "KMP-IAP v0.0.0-alpha1 (Android)"
    }
    
    override suspend fun initConnection() {
        println("[KMP-IAP] initConnection called")
        
        // Try to get context and register for activity lifecycle if not already done
        if (context == null) {
            try {
                // Try to get application context through reflection
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val currentActivityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
                val getApplication = activityThreadClass.getMethod("getApplication")
                val application = getApplication.invoke(currentActivityThread) as? Application
                
                if (application != null) {
                    context = application.applicationContext
                    // Register activity lifecycle callbacks to track current activity
                    application.registerActivityLifecycleCallbacks(this)
                    println("[KMP-IAP] Context obtained automatically and lifecycle callbacks registered")
                    
                    // Try to get current activity through reflection
                    try {
                        val activitiesField = activityThreadClass.getDeclaredField("mActivities")
                        activitiesField.isAccessible = true
                        val activities = activitiesField.get(currentActivityThread) as? Map<*, *>
                        if (!activities.isNullOrEmpty()) {
                            val activityRecord = activities.values.firstOrNull()
                            if (activityRecord != null) {
                                val activityField = activityRecord.javaClass.getDeclaredField("activity")
                                activityField.isAccessible = true
                                currentActivity = activityField.get(activityRecord) as? Activity
                                if (currentActivity != null) {
                                    println("[KMP-IAP] Current activity found through reflection: $currentActivity")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("[KMP-IAP] Could not get current activity through reflection: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("[KMP-IAP] Failed to get context automatically: ${e.message}")
            }
        }
        
        context ?: throw PurchaseError(
            message = "Context not available. Please ensure your app is properly initialized.",
            code = ErrorCode.E_NOT_INITIALIZED
        )
        
        // Clean up any existing state from hot reload
        cleanupState()
        
        println("[KMP-IAP] Starting BillingClient connection...")
        return suspendCancellableCoroutine { continuation ->
            val listener = object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    println("[KMP-IAP] onBillingSetupFinished - responseCode: ${billingResult.responseCode}")
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        isConnected = true
                        println("[KMP-IAP] Connected to Google Play successfully")
                        _connectionStateFlow.tryEmit(ConnectionResult(connected = true, message = "Connected to Google Play"))
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    } else {
                        val error = PurchaseError(
                            message = billingResult.debugMessage ?: "Failed to connect to Google Play",
                            responseCode = billingResult.responseCode,
                            code = ErrorCode.E_SERVICE_ERROR
                        )
                        _purchaseErrorFlow.tryEmit(error)
                        _connectionStateFlow.tryEmit(ConnectionResult(connected = false, message = error.message))
                        if (continuation.isActive) {
                            continuation.resumeWithException(error)
                        }
                    }
                }
                
                override fun onBillingServiceDisconnected() {
                    isConnected = false
                    _connectionStateFlow.tryEmit(ConnectionResult(connected = false, message = "Disconnected from Google Play"))
                }
            }
            
            val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    purchases.forEach { purchase ->
                        _purchaseUpdatedFlow.tryEmit(purchase.toKmpPurchase())
                    }
                } else {
                    _purchaseErrorFlow.tryEmit(
                        PurchaseError(
                            message = billingResult.debugMessage ?: "Purchase failed",
                            responseCode = billingResult.responseCode,
                            code = mapBillingResponseCode(billingResult.responseCode)
                        )
                    )
                }
            }
            
            billingClient = BillingClient.newBuilder(context!!)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build()
                
            billingClient?.startConnection(listener)
        }
    }
    
    override suspend fun endConnection() {
        billingClient?.endConnection()
        billingClient = null
        isConnected = false
        cleanupState()
        _connectionStateFlow.emit(ConnectionResult(connected = false, message = "Disconnected from Google Play"))
        
        // Unregister activity callbacks if we registered them
        try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentActivityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
            val getApplication = activityThreadClass.getMethod("getApplication")
            val application = getApplication.invoke(currentActivityThread) as? Application
            application?.unregisterActivityLifecycleCallbacks(this)
        } catch (e: Exception) {
            // Ignore errors during cleanup
        }
    }
    
    override suspend fun requestProducts(params: RequestProductsParams): List<BaseProduct> {
        println("[KMP-IAP] requestProducts called - type: ${params.type}, skus: ${params.skus}")
        ensureConnection()
        
        return suspendCancellableCoroutine { continuation ->
            val productList = params.skus.map { sku ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(sku)
                    .setProductType(
                        when (params.type) {
                            PurchaseType.INAPP -> BillingClient.ProductType.INAPP
                            PurchaseType.SUBS -> BillingClient.ProductType.SUBS
                        }
                    )
                    .build()
            }
            
            val queryParams = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
                
            billingClient?.queryProductDetailsAsync(queryParams) { billingResult, productDetailsList ->
                println("[KMP-IAP] queryProductDetailsAsync result: ${billingResult.responseCode}, products: ${productDetailsList.size}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val products = productDetailsList.map { productDetails ->
                        println("[KMP-IAP] Product: ${productDetails.productId}, type: ${params.type}")
                        when (params.type) {
                            PurchaseType.INAPP -> productDetails.toProduct()
                            PurchaseType.SUBS -> productDetails.toSubscription()
                        }
                    }
                    println("[KMP-IAP] Returning ${products.size} products")
                    if (continuation.isActive) {
                        continuation.resume(products)
                    }
                } else {
                    println("[KMP-IAP] queryProductDetailsAsync failed - responseCode: ${billingResult.responseCode}, debugMessage: ${billingResult.debugMessage}")
                    val error = PurchaseError(
                        message = "Failed to fetch products: ${billingResult.debugMessage}",
                        responseCode = billingResult.responseCode,
                        code = ErrorCode.E_PRODUCT_LOAD_FAILED
                    )
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
            }
        }
    }
    
    override suspend fun requestPurchase(request: RequestPurchase, type: PurchaseType) {
        ensureConnection()
        
        println("[KMP-IAP] requestPurchase - currentActivity: $currentActivity")
        
        val purchaseActivity = currentActivity ?: throw PurchaseError(
            message = "Activity not available for purchase. Make sure to call AndroidInAppPurchaseImpl.initializeAndroid() from your MainActivity",
            code = ErrorCode.E_ACTIVITY_UNAVAILABLE
        )
        
        val androidRequest = when (request) {
            is RequestPurchaseAndroid -> request
            is RequestSubscriptionAndroid -> request
            is RequestPurchaseIOS, is RequestSubscriptionIOS -> throw PurchaseError(
                message = "iOS request on Android platform",
                code = ErrorCode.E_DEVELOPER_ERROR
            )
            else -> throw PurchaseError(
                message = "Invalid request type: ${request::class.simpleName}",
                code = ErrorCode.E_DEVELOPER_ERROR
            )
        }
        
        // First, query product details
        val productId = when (androidRequest) {
            is RequestPurchaseAndroid -> androidRequest.sku
            is RequestSubscriptionAndroid -> androidRequest.sku
            else -> throw PurchaseError(
                message = "Unexpected request type",
                code = ErrorCode.E_DEVELOPER_ERROR
            )
        }
        
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(
                    when (type) {
                        PurchaseType.INAPP -> BillingClient.ProductType.INAPP
                        PurchaseType.SUBS -> BillingClient.ProductType.SUBS
                    }
                )
                .build()
        )
        
        val queryParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
            
        billingClient?.queryProductDetailsAsync(queryParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList.first()
                
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .apply {
                            if (type == PurchaseType.SUBS && request is RequestSubscriptionAndroid) {
                                request.subscriptionOffers?.firstOrNull()?.let { offer ->
                                    setOfferToken(offer.offerToken)
                                }
                            }
                        }
                        .build()
                )
                
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .apply {
                        when (androidRequest) {
                            is RequestPurchaseAndroid -> {
                                androidRequest.obfuscatedAccountIdAndroid?.let {
                                    setObfuscatedAccountId(it)
                                }
                                androidRequest.obfuscatedProfileIdAndroid?.let {
                                    setObfuscatedProfileId(it)
                                }
                            }
                            is RequestSubscriptionAndroid -> {
                                androidRequest.obfuscatedAccountIdAndroid?.let {
                                    setObfuscatedAccountId(it)
                                }
                                androidRequest.obfuscatedProfileIdAndroid?.let {
                                    setObfuscatedProfileId(it)
                                }
                            }
                            else -> {
                                // Other request types don't have Android-specific fields
                            }
                        }
                    }
                    .build()
                    
                billingClient?.launchBillingFlow(purchaseActivity, billingFlowParams)
            } else {
                _purchaseErrorFlow.tryEmit(
                    PurchaseError(
                        message = "Product not found: $productId",
                        code = ErrorCode.E_ITEM_UNAVAILABLE
                    )
                )
            }
        }
    }
    
    override suspend fun getAvailablePurchases(): List<Purchase> {
        println("[KMP-IAP] getAvailablePurchases called")
        ensureConnection()
        billingClient ?: throw PurchaseError(
            message = "Billing client not initialized",
            code = ErrorCode.E_NOT_INITIALIZED
        )
        
        return suspendCancellableCoroutine { continuation ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
                
            billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
                println("[KMP-IAP] INAPP query result: ${billingResult.responseCode}, purchases: ${purchases.size}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val inappPurchases = purchases.map { it.toKmpPurchase() }
                    
                    // Also query subscriptions
                    val subsParams = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                        
                    billingClient?.queryPurchasesAsync(subsParams) { subsResult, subsPurchases ->
                        println("[KMP-IAP] SUBS query result: ${subsResult.responseCode}, purchases: ${subsPurchases.size}")
                        if (subsResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            val allPurchases = inappPurchases + subsPurchases.map { it.toKmpPurchase() }
                            println("[KMP-IAP] Total available purchases: ${allPurchases.size}")
                            if (continuation.isActive) {
                                continuation.resume(allPurchases)
                            }
                        } else {
                            if (continuation.isActive) {
                                continuation.resume(inappPurchases)
                            }
                        }
                    }
                } else {
                    val error = PurchaseError(
                        message = "Failed to query purchases: ${billingResult.debugMessage}",
                        responseCode = billingResult.responseCode,
                        code = ErrorCode.E_SERVICE_ERROR
                    )
                    if (continuation.isActive) {
                        continuation.resumeWithException(error)
                    }
                }
            }
        }
    }
    
    override suspend fun getPurchaseHistories(): List<Purchase> {
        ensureConnection()
        // Note: Google Play Billing Library v8 doesn't support purchase history
        return emptyList()
    }
    
    override suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean): Boolean {
        ensureConnection()
        val purchaseToken = purchase.purchaseToken ?: return false
        
        return if (isConsumable) {
            consumePurchaseAndroid(purchaseToken)
        } else {
            acknowledgePurchaseAndroid(purchaseToken)
        }
    }
    
    override suspend fun getStorefrontIOS(): AppStoreInfo? = null
    
    override suspend fun presentCodeRedemptionSheetIOS() {
        // No-op on Android
    }
    
    override suspend fun showManageSubscriptionsIOS() {
        // No-op on Android
    }
    
    override suspend fun deepLinkToSubscriptionsAndroid(sku: String?) {
        val launchActivity = currentActivity ?: context ?: return
        
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = if (sku != null) {
                android.net.Uri.parse("https://play.google.com/store/account/subscriptions?sku=$sku&package=${context?.packageName}")
            } else {
                android.net.Uri.parse("https://play.google.com/store/account/subscriptions")
            }
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        
        if (launchActivity is Activity) {
            launchActivity.startActivity(intent)
        } else if (launchActivity is Context) {
            launchActivity.startActivity(intent)
        }
    }
    
    override suspend fun acknowledgePurchaseAndroid(purchaseToken: String): Boolean {
        ensureConnection()
        billingClient ?: throw PurchaseError(
            message = "Billing client not initialized",
            code = ErrorCode.E_NOT_INITIALIZED
        )
        
        return suspendCancellableCoroutine { continuation ->
            val params = com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
                
            billingClient?.acknowledgePurchase(params) { billingResult ->
                if (continuation.isActive) {
                    continuation.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                }
            }
        }
    }
    
    override suspend fun consumePurchaseAndroid(purchaseToken: String): Boolean {
        ensureConnection()
        billingClient ?: throw PurchaseError(
            message = "Billing client not initialized",
            code = ErrorCode.E_NOT_INITIALIZED
        )
        
        return suspendCancellableCoroutine { continuation ->
            val params = com.android.billingclient.api.ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build()
                
            billingClient?.consumeAsync(params) { billingResult, _ ->
                if (continuation.isActive) {
                    continuation.resume(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                }
            }
        }
    }
    
    override suspend fun validateReceiptIos(
        receiptBody: Map<String, String>,
        isTest: Boolean
    ): Map<String, Any>? = null
    
    override suspend fun validateReceiptAndroid(
        packageName: String,
        productId: String,
        productToken: String,
        accessToken: String,
        isSub: Boolean
    ): Map<String, Any>? {
        // TODO: Implement server-side validation
        return null
    }
    
    override fun getStore(): Store = Store.PLAY_STORE
    
    override suspend fun canMakePayments(): Boolean {
        return billingClient?.isReady ?: false
    }
    
    private fun mapBillingResponseCode(responseCode: Int): ErrorCode {
        return when (responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> ErrorCode.E_USER_CANCELLED
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> ErrorCode.E_SERVICE_ERROR
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> ErrorCode.E_BILLING_UNAVAILABLE
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> ErrorCode.E_ITEM_UNAVAILABLE
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> ErrorCode.E_DEVELOPER_ERROR
            BillingClient.BillingResponseCode.ERROR -> ErrorCode.E_UNKNOWN
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> ErrorCode.E_ALREADY_OWNED
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> ErrorCode.E_PURCHASE_NOT_ALLOWED
            else -> ErrorCode.E_UNKNOWN
        }
    }
    
    // Private helper methods
    private fun cleanupState() {
        // Clear any pending states to avoid race conditions
        // Android billing client handles most cleanup internally
    }

    private suspend fun ensureConnection() {
        if (!isConnected || billingClient == null) {
            throw PurchaseError(
                message = "IAP connection not initialized. Call initConnection() first.",
                code = ErrorCode.E_NOT_INITIALIZED
            )
        }
    }
    
}

// Extension functions
private fun com.android.billingclient.api.Purchase.toKmpPurchase(): Purchase {
    return Purchase(
        productId = products.firstOrNull() ?: "",
        transactionId = orderId,
        purchaseToken = purchaseToken,
        transactionDate = kotlinx.datetime.Instant.fromEpochMilliseconds(purchaseTime),
        platform = IAPPlatform.ANDROID,
        isAcknowledgedAndroid = isAcknowledged,
        purchaseStateAndroid = when (purchaseState) {
            com.android.billingclient.api.Purchase.PurchaseState.PURCHASED -> "purchased"
            com.android.billingclient.api.Purchase.PurchaseState.PENDING -> "pending"
            else -> "unspecified"
        },
        originalJson = mapOf<String, Any>(
            "orderId" to (orderId ?: ""),
            "packageName" to packageName,
            "productIds" to products,
            "purchaseTime" to purchaseTime,
            "purchaseState" to purchaseState,
            "purchaseToken" to purchaseToken,
            "isAcknowledged" to isAcknowledged,
            "isAutoRenewing" to isAutoRenewing,
            "originalJson" to originalJson,
            "signature" to signature
        )
    )
}

private fun com.android.billingclient.api.ProductDetails.toProduct(): Product {
    return Product(
        productId = productId,
        price = oneTimePurchaseOfferDetails?.formattedPrice ?: "",
        currency = oneTimePurchaseOfferDetails?.priceCurrencyCode ?: "",
        localizedPrice = oneTimePurchaseOfferDetails?.formattedPrice ?: "",
        title = title,
        description = description,
        priceAmountMicros = oneTimePurchaseOfferDetails?.priceAmountMicros ?: 0,
        iconUrl = null,
        originalJson = null,
        type = ProductType.INAPP,
        originalPrice = null
    )
}

private fun com.android.billingclient.api.ProductDetails.toSubscription(): Subscription {
    val defaultOffer = subscriptionOfferDetails?.firstOrNull()
    val pricingPhase = defaultOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()
    
    return Subscription(
        productId = productId,
        price = pricingPhase?.formattedPrice ?: "",
        currency = pricingPhase?.priceCurrencyCode ?: "",
        localizedPrice = pricingPhase?.formattedPrice ?: "",
        title = title,
        description = description,
        priceAmountMicros = pricingPhase?.priceAmountMicros ?: 0,
        iconUrl = null,
        originalJson = null,
        type = ProductType.SUBS,
        originalPrice = null,
        subscriptionOfferAndroid = subscriptionOfferDetails?.map { offer ->
            SubscriptionOffer(
                offerId = offer.offerId ?: "",
                basePlanId = offer.basePlanId,
                offerToken = offer.offerToken,
                pricingPhases = offer.pricingPhases.pricingPhaseList.map { phase ->
                    PricingPhase(
                        price = phase.formattedPrice,
                        currency = phase.priceCurrencyCode,
                        billingPeriod = phase.billingPeriod,
                        billingCycleCount = phase.billingCycleCount,
                        recurrenceMode = phase.recurrenceMode,
                        priceAmountMicros = phase.priceAmountMicros
                    )
                }
            )
        }
    )
}


