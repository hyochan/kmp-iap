package io.github.hyochan.kmpiap

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.android.billingclient.api.*
import io.github.hyochan.kmpiap.types.*
import io.github.hyochan.kmpiap.utils.ErrorCode
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class InAppPurchaseAndroid : KmpInAppPurchase, Application.ActivityLifecycleCallbacks {
    private var billingClient: BillingClient? = null
    private var isConnected = false
    private var context: Context? = null
    private var currentActivity: Activity? = null
    private var productDetailsMap = mutableMapOf<String, ProductDetails>()
    
    // Activity lifecycle callbacks
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (currentActivity == null) {
            currentActivity = activity
            println("[KMP-IAP] Activity captured on create: ${activity::class.simpleName}")
        }
    }
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        println("[KMP-IAP] Activity resumed: ${activity::class.simpleName}")
    }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
            println("[KMP-IAP] Activity destroyed: ${activity::class.simpleName}")
        }
    }
    
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
    
    // Connection state for backward compatibility (not in interface)
    private val _connectionStateListener = MutableSharedFlow<ConnectionResult>(
        replay = 1,
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
    
    override fun getVersion(): String {
        return "KMP-IAP v1.0.0-alpha02 (Android)"
    }
    
    override suspend fun initConnection(): Boolean {
        println("[KMP-IAP] initConnection called")
        
        // Try to get context if not set
        if (context == null) {
            try {
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val currentActivityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
                val getApplication = activityThreadClass.getMethod("getApplication")
                val app = getApplication.invoke(currentActivityThread) as? Application
                
                if (app != null) {
                    context = app.applicationContext
                    app.registerActivityLifecycleCallbacks(this)
                    println("[KMP-IAP] Context and lifecycle callbacks registered")
                    
                    // Try to get current activity
                    try {
                        val activitiesField = activityThreadClass.getDeclaredField("mActivities")
                        activitiesField.isAccessible = true
                        val activities = activitiesField.get(currentActivityThread) as? Map<*, *>
                        if (!activities.isNullOrEmpty()) {
                            for ((_, activityRecord) in activities) {
                                val activityRecordClass = activityRecord?.javaClass
                                val activityField = activityRecordClass?.getDeclaredField("activity")
                                activityField?.isAccessible = true
                                val activity = activityField?.get(activityRecord) as? Activity
                                if (activity != null && !activity.isFinishing) {
                                    currentActivity = activity
                                    println("[KMP-IAP] Current activity found: ${activity::class.simpleName}")
                                    break
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("[KMP-IAP] Could not get current activity: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("[KMP-IAP] Failed to get context automatically: ${e.message}")
            }
        }
        
        context ?: throw PurchaseError(
            code = ErrorCode.E_SERVICE_ERROR.name,
            message = "Context not available"
        )
        
        println("[KMP-IAP] Starting BillingClient connection...")
        return suspendCancellableCoroutine { continuation ->
            val listener = object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    println("[KMP-IAP] BillingClient setup finished with code: ${billingResult.responseCode}")
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        isConnected = true
                        println("[KMP-IAP] Connected to Google Play successfully")
                        _connectionStateListener.tryEmit(ConnectionResult(connected = true, message = "Connected"))
                        if (continuation.isActive) {
                            continuation.resume(true)
                        }
                    } else {
                        val error = PurchaseError(
                            code = ErrorCode.E_SERVICE_ERROR.name,
                            message = billingResult.debugMessage ?: "Failed to connect",
                            responseCode = billingResult.responseCode
                        )
                        _purchaseErrorListener.tryEmit(error)
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                }
                
                override fun onBillingServiceDisconnected() {
                    println("[KMP-IAP] BillingClient disconnected")
                    isConnected = false
                    _connectionStateListener.tryEmit(ConnectionResult(connected = false, message = "Disconnected"))
                }
            }
            
            billingClient = BillingClient.newBuilder(context!!)
                .setListener { billingResult, purchases ->
                    handlePurchaseUpdate(billingResult, purchases)
                }
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build()
                )
                .build()
                
            billingClient?.startConnection(listener)
        }
    }
    
    override suspend fun endConnection() {
        billingClient?.endConnection()
        isConnected = false
        _connectionStateListener.tryEmit(ConnectionResult(connected = false, message = "Disconnected"))
        cleanupState()
    }
    
    override suspend fun requestProducts(params: ProductRequest): List<Product> {
        ensureConnection()
        println("[KMP-IAP] Requesting products: ${params.skus} of type ${params.type}")
        
        // Try multiple product types if not specified or if products not found
        val productTypes = when (params.type) {
            ProductType.INAPP -> listOf(BillingClient.ProductType.INAPP)
            ProductType.SUBS -> listOf(BillingClient.ProductType.SUBS)
            else -> listOf(BillingClient.ProductType.INAPP, BillingClient.ProductType.SUBS)
        }
        
        val allProducts = mutableListOf<Product>()
        
        for (productType in productTypes) {
            println("[KMP-IAP] Querying for product type: $productType")
            
            val productList = params.skus.map { sku ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(sku)
                    .setProductType(productType)
                    .build()
            }
            
            val queryParams = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()
                
            val products = suspendCancellableCoroutine<List<Product>> { continuation ->
                try {
                    println("[KMP-IAP] Starting product query for $productType...")
                    val client = billingClient
                    if (client == null) {
                        println("[KMP-IAP] BillingClient is null!")
                        if (continuation.isActive) {
                            continuation.resume(emptyList())
                        }
                        return@suspendCancellableCoroutine
                    }
                    
                    // Set up a timeout for the query
                    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
                    val timeoutJob = kotlinx.coroutines.GlobalScope.launch {
                        kotlinx.coroutines.delay(5000) // 5 second timeout
                        if (continuation.isActive) {
                            println("[KMP-IAP] Product query timed out for $productType")
                            continuation.resume(emptyList())
                        }
                    }
                    
                    client.queryProductDetailsAsync(queryParams) { billingResult, productDetailsList ->
                        timeoutJob.cancel() // Cancel timeout if we get a response
                        println("[KMP-IAP] Product query callback for $productType: ${billingResult.responseCode}, ${productDetailsList?.size ?: 0} products")
                        
                        try {
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                // Store product details for later use
                                productDetailsList.forEach { productDetails ->
                                    println("[KMP-IAP] Product found: ${productDetails.productId} - ${productDetails.title}")
                                    productDetailsMap[productDetails.productId] = productDetails
                                }
                                
                                val products = productDetailsList.map { productDetails ->
                                    productDetails.toProduct()
                                }
                                if (continuation.isActive) {
                                    continuation.resume(products)
                                }
                            } else {
                                println("[KMP-IAP] Product query failed for $productType: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                                if (continuation.isActive) {
                                    continuation.resume(emptyList())
                                }
                            }
                        } catch (e: Exception) {
                            println("[KMP-IAP] Error in product query callback: ${e.message}")
                            e.printStackTrace()
                            if (continuation.isActive) {
                                continuation.resume(emptyList())
                            }
                        }
                    }
                    
                    // Add cancellation handling
                    continuation.invokeOnCancellation {
                        timeoutJob.cancel()
                        println("[KMP-IAP] Product query cancelled for $productType")
                    }
                } catch (e: Exception) {
                    println("[KMP-IAP] Exception in requestProducts for $productType: ${e.message}")
                    e.printStackTrace()
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
            }
            
            allProducts.addAll(products)
            if (allProducts.isNotEmpty()) {
                // Found products, no need to try other types
                break
            }
        }
        
        println("[KMP-IAP] Total products found: ${allProducts.size}")
        return allProducts
    }
    
    override suspend fun requestPurchase(request: UnifiedPurchaseRequest): Purchase {
        ensureConnection()
        
        val sku = request.sku ?: request.skus?.firstOrNull() ?: throw PurchaseError(
            code = ErrorCode.E_DEVELOPER_ERROR.name,
            message = "No SKU provided"
        )
        
        println("[KMP-IAP] Requesting purchase for SKU: $sku")
        println("[KMP-IAP] Current activity: ${currentActivity?.javaClass?.simpleName}")
        
        // Try to get activity if not available
        if (currentActivity == null) {
            // Try to get the top activity from the application
            val app = context as? Application
            if (app != null) {
                try {
                    val activityThreadClass = Class.forName("android.app.ActivityThread")
                    val currentActivityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null)
                    val activitiesField = activityThreadClass.getDeclaredField("mActivities")
                    activitiesField.isAccessible = true
                    val activities = activitiesField.get(currentActivityThread) as? Map<*, *>
                    if (!activities.isNullOrEmpty()) {
                        for ((_, activityRecord) in activities) {
                            val activityRecordClass = activityRecord?.javaClass
                            val activityField = activityRecordClass?.getDeclaredField("activity")
                            activityField?.isAccessible = true
                            val activity = activityField?.get(activityRecord) as? Activity
                            if (activity != null && !activity.isFinishing) {
                                currentActivity = activity
                                println("[KMP-IAP] Found activity through reflection: ${activity::class.simpleName}")
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("[KMP-IAP] Failed to get activity through reflection: ${e.message}")
                }
            }
        }
        
        currentActivity ?: throw PurchaseError(
            code = ErrorCode.E_SERVICE_ERROR.name,
            message = "Activity not available for purchase. Please ensure your app has an active Activity."
        )
        
        // Get product details from cache or query
        var productDetails = productDetailsMap[sku]
        if (productDetails == null) {
            println("[KMP-IAP] Product details not in cache, querying...")
            // First try as INAPP, then as SUBS if not found
            var products = requestProducts(ProductRequest(listOf(sku), ProductType.INAPP))
            if (products.isEmpty()) {
                println("[KMP-IAP] Not found as INAPP, trying as SUBS...")
                products = requestProducts(ProductRequest(listOf(sku), ProductType.SUBS))
            }
            if (products.isEmpty()) {
                throw PurchaseError(
                    code = ErrorCode.E_ITEM_UNAVAILABLE.name,
                    message = "Product not found: $sku"
                )
            }
            productDetails = productDetailsMap[sku]
        }
        
        productDetails ?: throw PurchaseError(
            code = ErrorCode.E_ITEM_UNAVAILABLE.name,
            message = "Product details not available for: $sku"
        )
        
        // Build the purchase params
        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
        
        // For subscriptions, we need to add the offer token
        if (productDetails.subscriptionOfferDetails != null && productDetails.subscriptionOfferDetails!!.isNotEmpty()) {
            val offer = productDetails.subscriptionOfferDetails!![0]
            println("[KMP-IAP] Adding offer token for subscription: ${offer.offerToken}")
            productDetailsParamsBuilder.setOfferToken(offer.offerToken)
        }
        
        val productDetailsParamsList = listOf(productDetailsParamsBuilder.build())
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        // Launch the billing flow
        println("[KMP-IAP] Launching billing flow...")
        val billingResult = billingClient?.launchBillingFlow(currentActivity!!, billingFlowParams)
        
        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            throw PurchaseError(
                code = mapBillingResponseCode(billingResult?.responseCode ?: -1).name,
                message = billingResult?.debugMessage ?: "Failed to launch billing flow",
                responseCode = billingResult?.responseCode
            )
        }
        
        // Return a pending purchase - actual purchase will be handled by the listener
        return Purchase(
            productId = sku,
            transactionDate = Clock.System.now().epochSeconds.toDouble(),
            transactionReceipt = "",
            platform = IAPPlatform.ANDROID
        )
    }
    
    override suspend fun getAvailablePurchases(options: PurchaseOptions?): List<Purchase> {
        ensureConnection()
        
        val allPurchases = mutableListOf<Purchase>()
        
        // Query INAPP purchases
        val inappParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
            
        val inappResult = suspendCancellableCoroutine<List<Purchase>> { continuation ->
            billingClient?.queryPurchasesAsync(inappParams) { billingResult, purchasesList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val purchases = purchasesList.map { purchase ->
                        purchase.toPurchase()
                    }
                    continuation.resume(purchases)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }
        allPurchases.addAll(inappResult)
        
        // Query SUBS purchases
        val subsParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
            
        val subsResult = suspendCancellableCoroutine<List<Purchase>> { continuation ->
            billingClient?.queryPurchasesAsync(subsParams) { billingResult, purchasesList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val purchases = purchasesList.map { purchase ->
                        purchase.toPurchase()
                    }
                    continuation.resume(purchases)
                } else {
                    continuation.resume(emptyList())
                }
            }
        }
        allPurchases.addAll(subsResult)
        
        println("[KMP-IAP] Found ${allPurchases.size} available purchases (${inappResult.size} INAPP, ${subsResult.size} SUBS)")
        return allPurchases
    }
    
    override suspend fun getPurchaseHistories(options: PurchaseOptions?): List<ProductPurchase> {
        // Android doesn't provide purchase history in v6+
        return emptyList()
    }
    
    override suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean?): Boolean {
        return try {
            if (isConsumable == true) {
                purchase.purchaseTokenAndroid?.let { token ->
                    consumePurchaseAndroid(token)
                    true
                } ?: false
            } else {
                purchase.purchaseTokenAndroid?.let { token ->
                    acknowledgePurchaseAndroid(token)
                    true
                } ?: false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    // iOS-specific methods (no-op on Android)
    override suspend fun getStorefrontIOS(): String = ""
    override suspend fun presentCodeRedemptionSheetIOS() {}
    override suspend fun finishTransactionIOS(transactionId: String) {}
    override suspend fun clearTransactionIOS() {}
    override suspend fun clearProductsIOS() {}
    override suspend fun getPromotedProductIOS(): String? = null
    override suspend fun buyPromotedProductIOS() {}
    
    // Android-specific methods
    override suspend fun acknowledgePurchaseAndroid(purchaseToken: String) {
        ensureConnection()
        
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
            
        suspendCancellableCoroutine<Unit> { continuation ->
            billingClient?.acknowledgePurchase(params) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(Unit)
                } else {
                    _purchaseErrorListener.tryEmit(
                        PurchaseError(
                            code = ErrorCode.E_SERVICE_ERROR.name,
                            message = billingResult.debugMessage ?: "Failed to acknowledge",
                            responseCode = billingResult.responseCode
                        )
                    )
                    continuation.resume(Unit)
                }
            }
        }
    }
    
    override suspend fun consumePurchaseAndroid(purchaseToken: String) {
        ensureConnection()
        
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
            
        suspendCancellableCoroutine<Unit> { continuation ->
            billingClient?.consumeAsync(params) { billingResult, _ ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(Unit)
                } else {
                    _purchaseErrorListener.tryEmit(
                        PurchaseError(
                            code = ErrorCode.E_SERVICE_ERROR.name,
                            message = billingResult.debugMessage ?: "Failed to consume",
                            responseCode = billingResult.responseCode
                        )
                    )
                    continuation.resume(Unit)
                }
            }
        }
    }
    
    override suspend fun deepLinkToSubscriptions(options: DeepLinkOptions) {
        val sku = options.skuAndroid ?: return
        currentActivity?.let { activity ->
            val url = "https://play.google.com/store/account/subscriptions?sku=$sku&package=${activity.packageName}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(intent)
        }
    }
    
    // Validation methods
    override suspend fun validateReceipt(options: ValidationOptions): ValidationResult {
        return ValidationResult(isValid = false, status = -1)
    }
    
    override suspend fun isPurchaseValid(purchase: Purchase): Boolean {
        return purchase.purchaseTokenAndroid?.isNotEmpty() == true
    }
    
    
    override fun getStore(): Store = Store.PLAY_STORE
    
    override suspend fun canMakePayments(): Boolean = true
    
    // Private helper methods
    private fun cleanupState() {
        productDetailsMap.clear()
    }
    
    private fun ensureConnection() {
        if (!isConnected) {
            throw PurchaseError(
                code = ErrorCode.E_SERVICE_ERROR.name,
                message = "Not connected to billing service"
            )
        }
    }
    
    private fun handlePurchaseUpdate(billingResult: BillingResult, purchases: List<com.android.billingclient.api.Purchase>?) {
        println("[KMP-IAP] Purchase update: ${billingResult.responseCode}, ${purchases?.size} purchases")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach { purchase ->
                println("[KMP-IAP] Purchase: ${purchase.products}, state: ${purchase.purchaseState}")
                _purchaseUpdatedListener.tryEmit(purchase.toPurchase())
            }
        } else {
            val errorCode = mapBillingResponseCode(billingResult.responseCode)
            _purchaseErrorListener.tryEmit(
                PurchaseError(
                    code = errorCode.name,
                    message = billingResult.debugMessage ?: "Purchase failed",
                    responseCode = billingResult.responseCode
                )
            )
        }
    }
    
    private fun mapBillingResponseCode(responseCode: Int): ErrorCode {
        return when (responseCode) {
            BillingClient.BillingResponseCode.USER_CANCELED -> ErrorCode.E_USER_CANCELLED
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> ErrorCode.E_SERVICE_ERROR
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> ErrorCode.E_SERVICE_ERROR
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> ErrorCode.E_ITEM_UNAVAILABLE
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> ErrorCode.E_DEVELOPER_ERROR
            BillingClient.BillingResponseCode.ERROR -> ErrorCode.E_UNKNOWN
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> ErrorCode.E_ALREADY_OWNED
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> ErrorCode.E_PRODUCT_NOT_AVAILABLE
            else -> ErrorCode.E_UNKNOWN
        }
    }
}

// Extension functions
private fun com.android.billingclient.api.Purchase.toPurchase(): Purchase {
    return Purchase(
        productId = products.firstOrNull() ?: "",
        transactionDate = purchaseTime.toDouble() / 1000, // Convert millis to seconds
        transactionReceipt = originalJson,
        purchaseTokenAndroid = purchaseToken,
        purchaseStateAndroid = purchaseState,
        signatureAndroid = signature,
        acknowledgedAndroid = isAcknowledged,
        orderIdAndroid = orderId,
        packageNameAndroid = packageName,
        platform = IAPPlatform.ANDROID
    )
}

private fun ProductDetails.toProduct(): Product {
    val oneTimePurchaseOfferDetails = oneTimePurchaseOfferDetails
    val subscriptionOfferDetails = subscriptionOfferDetails
    
    return if (oneTimePurchaseOfferDetails != null) {
        Product(
            id = productId,
            title = title,
            description = description,
            price = oneTimePurchaseOfferDetails.formattedPrice,
            priceAmount = oneTimePurchaseOfferDetails.priceAmountMicros.toDouble() / 1000000,
            currency = oneTimePurchaseOfferDetails.priceCurrencyCode,
            platform = IAPPlatform.ANDROID
        )
    } else {
        val offer = subscriptionOfferDetails?.firstOrNull()
        val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
        Product(
            id = productId,
            title = title,
            description = description,
            price = phase?.formattedPrice ?: "",
            priceAmount = (phase?.priceAmountMicros?.toDouble() ?: 0.0) / 1000000,
            currency = phase?.priceCurrencyCode ?: "USD",
            subscriptionOfferDetails = subscriptionOfferDetails?.map { it.toOfferDetail() },
            platform = IAPPlatform.ANDROID
        )
    }
}

private fun ProductDetails.SubscriptionOfferDetails.toOfferDetail(): OfferDetail {
    return OfferDetail(
        offerId = offerId ?: "",
        basePlanId = basePlanId,
        offerToken = offerToken,
        pricingPhases = pricingPhases.pricingPhaseList.map { phase ->
            PricingPhase(
                billingPeriod = phase.billingPeriod,
                formattedPrice = phase.formattedPrice,
                priceAmountMicros = phase.priceAmountMicros.toString(),
                priceCurrencyCode = phase.priceCurrencyCode,
                billingCycleCount = phase.billingCycleCount,
                recurrenceMode = when (phase.recurrenceMode) {
                    1 -> RecurrenceMode.FINITE_RECURRING
                    2 -> RecurrenceMode.INFINITE_RECURRING
                    3 -> RecurrenceMode.NON_RECURRING
                    else -> null
                }
            )
        },
        offerTags = offerTags
    )
}