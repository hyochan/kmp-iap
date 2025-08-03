package io.github.hyochan.kmpiap.useIap

import io.github.hyochan.kmpiap.KmpInAppPurchase
import io.github.hyochan.kmpiap.createInAppPurchase
import io.github.hyochan.kmpiap.types.AndroidProrationMode
import io.github.hyochan.kmpiap.types.AppStoreInfo
import io.github.hyochan.kmpiap.types.Product
import io.github.hyochan.kmpiap.types.Purchase
import io.github.hyochan.kmpiap.types.PurchaseError
import io.github.hyochan.kmpiap.types.PurchaseType
import io.github.hyochan.kmpiap.types.RequestProductsParams
import io.github.hyochan.kmpiap.types.RequestPurchase
import io.github.hyochan.kmpiap.types.RequestPurchaseAndroid
import io.github.hyochan.kmpiap.types.RequestPurchaseGeneric
import io.github.hyochan.kmpiap.types.RequestPurchaseIOS
import io.github.hyochan.kmpiap.types.RequestSubscriptionAndroid
import io.github.hyochan.kmpiap.types.Store
import io.github.hyochan.kmpiap.types.Subscription
import io.github.hyochan.kmpiap.types.SubscriptionOfferAndroid
import io.github.hyochan.kmpiap.types.IAPPlatform
import io.github.hyochan.kmpiap.types.getCurrentPlatform
import io.github.hyochan.kmpiap.utils.ErrorCode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Configuration options for UseIap
 */
data class UseIapOptions(
    val onPurchaseSuccess: ((purchase: Purchase) -> Unit)? = null,
    val onPurchaseError: ((error: PurchaseError) -> Unit)? = null,
    val onSyncError: ((error: Exception) -> Unit)? = null,
    val autoFinishTransactions: Boolean = true
)

/**
 * A helper class for managing In-App Purchase operations similar to Flutter/expo-iap's useIap hook.
 * This provides a convenient interface for IAP operations with state management.
 */
class UseIap(
    private val iap: KmpInAppPurchase = createInAppPurchase(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    private val options: UseIapOptions = UseIapOptions()
) {
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()
    
    private val _subscriptions = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptions: StateFlow<List<Subscription>> = _subscriptions.asStateFlow()
    
    private val _availablePurchases = MutableStateFlow<List<Purchase>>(emptyList())
    val availablePurchases: StateFlow<List<Purchase>> = _availablePurchases.asStateFlow()
    
    private val _purchaseHistories = MutableStateFlow<List<Purchase>>(emptyList())
    val purchaseHistories: StateFlow<List<Purchase>> = _purchaseHistories.asStateFlow()
    
    private val _currentPurchase = MutableStateFlow<Purchase?>(null)
    val currentPurchase: StateFlow<Purchase?> = _currentPurchase.asStateFlow()
    
    private val _currentError = MutableStateFlow<PurchaseError?>(null)
    val currentError: StateFlow<PurchaseError?> = _currentError.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    val connected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _promotedProductsIOS = MutableStateFlow<String?>(null)
    val promotedProductsIOS: StateFlow<String?> = _promotedProductsIOS.asStateFlow()
    val promotedProductIOS: StateFlow<Product?> = _products.map { products ->
        _promotedProductsIOS.value?.let { productId ->
            products.find { it.productId == productId }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(), null)
    
    private val _activeSubscriptions = MutableStateFlow<List<Purchase>>(emptyList())
    val activeSubscriptions: StateFlow<List<Purchase>> = _activeSubscriptions.asStateFlow()
    
    // Event listeners
    val purchaseUpdatedListener = iap.purchaseUpdatedFlow
    val purchaseErrorListener = iap.purchaseErrorFlow
    val connectionStateListener = iap.connectionStateFlow
    val promotedProductListener = iap.promotedProductFlow
    
    init {
        // Setup event listeners
        scope.launch {
            iap.purchaseUpdatedFlow.collect { purchase ->
                _currentPurchase.value = purchase
                
                // Call success callback if provided
                options.onPurchaseSuccess?.invoke(purchase)
                
                // Auto finish transaction if enabled
                if (options.autoFinishTransactions) {
                    try {
                        iap.finishTransaction(purchase, isConsumable = false)
                    } catch (e: Exception) {
                        options.onSyncError?.invoke(e)
                    }
                }
            }
        }
        
        scope.launch {
            iap.purchaseErrorFlow.collect { error ->
                _currentError.value = error
                // Call error callback if provided
                options.onPurchaseError?.invoke(error)
            }
        }
        
        scope.launch {
            iap.connectionStateFlow.collect { result ->
                println("[UseIap] Connection state received: ${result.connected}, message: ${result.message}")
                _isConnected.value = result.connected
            }
        }
        
        scope.launch {
            iap.promotedProductFlow.collect { productId ->
                _promotedProductsIOS.value = productId
            }
        }
    }
    
    /**
     * Initialize IAP connection
     */
    suspend fun initConnection() {
        try {
            iap.initConnection()
        } catch (e: Exception) {
            val error = when (e) {
                is PurchaseError -> e
                else -> PurchaseError(
                    message = e.message ?: "Failed to initialize connection",
                    code = ErrorCode.E_NOT_INITIALIZED
                )
            }
            _currentError.value = error
            options.onSyncError?.invoke(e)
        }
    }
    
    /**
     * End IAP connection
     */
    suspend fun endConnection() {
        try {
            iap.endConnection()
        } catch (e: Exception) {
            val error = when (e) {
                is PurchaseError -> e
                else -> PurchaseError(
                    message = e.message ?: "Failed to end connection",
                    code = ErrorCode.E_SERVICE_ERROR
                )
            }
            _currentError.value = error
            options.onSyncError?.invoke(e)
        }
    }
    
    /**
     * Request products with unified API (matches expo-iap)
     * @param params Product request configuration
     * @return List of products directly (no casting needed)
     */
    suspend fun requestProducts(params: RequestProductsParams): List<Product> {
        return try {
            val result = iap.requestProducts(params)
            val products = result.filterIsInstance<Product>()
            
            // Update state based on type
            when (params.type) {
                PurchaseType.INAPP -> _products.value = products
                PurchaseType.SUBS -> {
                    // For subscriptions, convert to Product for consistency with expo-iap
                    _products.value = result.filterIsInstance<Subscription>().map { sub ->
                        Product(
                            productId = sub.productId,
                            title = sub.title,
                            description = sub.description,
                            price = sub.price,
                            localizedPrice = sub.localizedPrice,
                            currency = sub.currency,
                            platform = sub.platform,
                            priceAmountMicros = sub.priceAmountMicros,
                            iconUrl = sub.iconUrl,
                            originalJson = sub.originalJson,
                            originalPrice = sub.originalPrice,
                            discountPrice = null
                        )
                    }
                }
            }
            
            products
        } catch (e: Exception) {
            val error = when (e) {
                is PurchaseError -> e
                else -> PurchaseError(
                    message = e.message ?: "Failed to request products",
                    code = ErrorCode.E_PRODUCT_LOAD_FAILED
                )
            }
            _currentError.value = error
            options.onSyncError?.invoke(e)
            emptyList()
        }
    }
    
    /**
     * Get products from store (deprecated - use requestProducts)
     * @deprecated Use requestProducts instead
     */
    suspend fun getProducts(skus: List<String>): List<Product> {
        return requestProducts(RequestProductsParams(skus, PurchaseType.INAPP))
    }
    
    /**
     * Get subscriptions from store (deprecated - use requestProducts)
     * @deprecated Use requestProducts instead
     */
    suspend fun getSubscriptions(skus: List<String>): List<Subscription> {
        println("[UseIap] getSubscriptions called with skus: $skus")
        return try {
            val result = iap.requestProducts(RequestProductsParams(skus, PurchaseType.SUBS))
            println("[UseIap] requestProducts returned ${result.size} items")
            val subscriptions = result.filterIsInstance<Subscription>()
            println("[UseIap] Filtered ${subscriptions.size} subscriptions")
            _subscriptions.value = subscriptions
            subscriptions
        } catch (e: Exception) {
            println("[UseIap] getSubscriptions error: ${e.message}")
            val error = when (e) {
                is PurchaseError -> e
                else -> PurchaseError(
                    message = e.message ?: "Failed to get subscriptions",
                    code = ErrorCode.E_PRODUCT_LOAD_FAILED
                )
            }
            _currentError.value = error
            options.onSyncError?.invoke(e)
            emptyList()
        }
    }
    
    /**
     * Request a purchase
     */
    suspend fun requestPurchase(
        sku: String,
        appAccountToken: String? = null,
        obfuscatedAccountIdAndroid: String? = null,
        obfuscatedProfileIdAndroid: String? = null,
        quantity: Int? = null
    ) {
        try {
            val request = when (iap.getStore()) {
                Store.APP_STORE -> RequestPurchaseIOS(
                    sku = sku,
                    appAccountToken = appAccountToken,
                    quantity = quantity
                )
                Store.PLAY_STORE -> RequestPurchaseAndroid(
                    skus = listOf(sku),
                    obfuscatedAccountIdAndroid = obfuscatedAccountIdAndroid,
                    obfuscatedProfileIdAndroid = obfuscatedProfileIdAndroid
                )
                else -> RequestPurchaseGeneric(sku = sku)
            }
            iap.requestPurchase(request, PurchaseType.INAPP)
        } catch (e: Exception) {
            _currentError.value = when (e) {
                is PurchaseError -> e
                else -> PurchaseError(
                    message = e.message ?: "Purchase failed",
                    code = ErrorCode.E_PURCHASE_FAILED
                )
            }
        }
    }
    
    /**
     * Request a subscription
     */
    suspend fun requestSubscription(
        sku: String,
        appAccountToken: String? = null,
        obfuscatedAccountIdAndroid: String? = null,
        obfuscatedProfileIdAndroid: String? = null,
        subscriptionOffers: List<SubscriptionOfferAndroid>? = null
    ) {
        try {
            val request = when (iap.getStore()) {
                Store.APP_STORE -> RequestPurchaseIOS(
                    sku = sku,
                    appAccountToken = appAccountToken
                )
                Store.PLAY_STORE -> RequestSubscriptionAndroid(
                    skus = listOf(sku),
                    obfuscatedAccountIdAndroid = obfuscatedAccountIdAndroid,
                    obfuscatedProfileIdAndroid = obfuscatedProfileIdAndroid,
                    subscriptionOffers = subscriptionOffers
                )
                else -> RequestPurchaseGeneric(sku = sku)
            }
            iap.requestPurchase(request, PurchaseType.SUBS)
        } catch (e: Exception) {
            _currentError.value = when (e) {
                is PurchaseError -> e
                else -> PurchaseError(
                    message = e.message ?: "Subscription purchase failed",
                    code = ErrorCode.E_PURCHASE_FAILED
                )
            }
        }
    }
    
    /**
     * Get available purchases
     */
    suspend fun getAvailablePurchases() {
        try {
            val purchases = iap.getAvailablePurchases()
            _availablePurchases.value = purchases
            updateActiveSubscriptions(purchases)
        } catch (e: Exception) {
            _currentError.value = when (e) {
                is PurchaseError -> e
                else -> PurchaseError(
                    message = e.message ?: "Failed to get available purchases",
                    code = ErrorCode.E_SERVICE_ERROR
                )
            }
        }
    }
    
    /**
     * Get purchase history
     */
    suspend fun getPurchaseHistories() {
        try {
            val history = iap.getPurchaseHistories()
            _purchaseHistories.value = history
        } catch (e: Exception) {
            _currentError.value = when (e) {
                is PurchaseError -> e
                else -> PurchaseError(
                    message = e.message ?: "Failed to get purchase history",
                    code = ErrorCode.E_SERVICE_ERROR
                )
            }
        }
    }
    
    /**
     * Finish a transaction
     */
    suspend fun finishTransaction(
        purchase: Purchase,
        isConsumable: Boolean = false
    ): Boolean {
        return try {
            iap.finishTransaction(purchase, isConsumable)
        } catch (e: Exception) {
            _currentError.value = when (e) {
                is PurchaseError -> e
                else -> PurchaseError(
                    message = e.message ?: "Failed to finish transaction",
                    code = ErrorCode.E_RECEIPT_FINISHED_FAILED
                )
            }
            false
        }
    }
    
    /**
     * Clear current error
     */
    fun clearError() {
        _currentError.value = null
    }
    
    /**
     * Clear current purchase
     */
    fun clearPurchase() {
        _currentPurchase.value = null
    }
    
    /**
     * Get store type
     */
    fun getStore(): Store = iap.getStore()
    
    /**
     * Check if device can make payments
     */
    suspend fun canMakePayments(): Boolean = iap.canMakePayments()
    
    /**
     * iOS-only: Get storefront
     */
    suspend fun getStorefrontIOS(): AppStoreInfo? = iap.getStorefrontIOS()
    
    /**
     * iOS-only: Present code redemption sheet
     */
    suspend fun presentCodeRedemptionSheetIOS() = iap.presentCodeRedemptionSheetIOS()
    
    /**
     * iOS-only: Show manage subscriptions
     */
    suspend fun showManageSubscriptionsIOS() = iap.showManageSubscriptionsIOS()
    
    /**
     * Android-only: Deep link to subscriptions
     */
    suspend fun deepLinkToSubscriptionsAndroid(sku: String? = null) = 
        iap.deepLinkToSubscriptionsAndroid(sku)
    
    /**
     * Validate receipt (unified method for both platforms)
     * @param productId Product ID to validate
     * @param params Platform-specific validation parameters
     * @return Validation result
     */
    suspend fun validateReceipt(
        productId: String,
        params: Map<String, Any>? = null
    ): Map<String, Any>? {
        return try {
            when (iap.getStore()) {
                Store.APP_STORE -> {
                    val receipt = _currentPurchase.value?.transactionReceipt
                        ?: throw PurchaseError(
                            message = "No receipt found for validation",
                            code = ErrorCode.E_RECEIPT_REQUEST_FAILED
                        )
                    val isTest = params?.get("isTest") as? Boolean ?: true
                    iap.validateReceiptIos(
                        receiptBody = mapOf("receipt-data" to receipt),
                        isTest = isTest
                    )
                }
                Store.PLAY_STORE -> {
                    val packageName = params?.get("packageName") as? String
                        ?: throw PurchaseError(
                            message = "packageName is required for Android validation",
                            code = ErrorCode.E_RECEIPT_REQUEST_FAILED
                        )
                    val productToken = params?.get("productToken") as? String
                        ?: _currentPurchase.value?.purchaseToken
                        ?: throw PurchaseError(
                            message = "productToken is required for Android validation",
                            code = ErrorCode.E_RECEIPT_REQUEST_FAILED
                        )
                    val accessToken = params?.get("accessToken") as? String
                        ?: throw PurchaseError(
                            message = "accessToken is required for Android validation",
                            code = ErrorCode.E_RECEIPT_REQUEST_FAILED
                        )
                    val isSub = params?.get("isSub") as? Boolean ?: false
                    
                    iap.validateReceiptAndroid(
                        packageName = packageName,
                        productId = productId,
                        productToken = productToken,
                        accessToken = accessToken,
                        isSub = isSub
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            _currentError.value = when (e) {
                is PurchaseError -> e
                else -> PurchaseError(
                    message = e.message ?: "Receipt validation failed",
                    code = ErrorCode.E_RECEIPT_REQUEST_FAILED
                )
            }
            null
        }
    }
    
    /**
     * iOS-only: Get promoted product
     * @return The promoted product if available
     */
    suspend fun getPromotedProductIOS(): Product? {
        return _promotedProductsIOS.value?.let { productId ->
            _products.value.find { it.productId == productId }
        }
    }
    
    /**
     * iOS-only: Buy promoted product
     */
    suspend fun buyPromotedProductIOS() {
        try {
            val promotedProductId = _promotedProductsIOS.value
                ?: throw PurchaseError(
                    message = "No promoted product available",
                    code = ErrorCode.E_ITEM_UNAVAILABLE
                )
            
            // Find the promoted product in our products list
            val product = _products.value.find { it.productId == promotedProductId }
                ?: _subscriptions.value.find { it.productId == promotedProductId }
                ?: throw PurchaseError(
                    message = "Promoted product not found in loaded products",
                    code = ErrorCode.E_ITEM_UNAVAILABLE
                )
            
            // Request purchase for the promoted product
            if (product is Subscription) {
                requestSubscription(promotedProductId)
            } else {
                requestPurchase(promotedProductId)
            }
        } catch (e: Exception) {
            _currentError.value = when (e) {
                is PurchaseError -> e
                else -> PurchaseError(
                    message = e.message ?: "Failed to buy promoted product",
                    code = ErrorCode.E_PURCHASE_FAILED
                )
            }
        }
    }
    
    /**
     * Cleanup resources
     */
    fun dispose() {
        scope.cancel()
    }
    
    /**
     * Update active subscriptions based on available purchases
     */
    private fun updateActiveSubscriptions(purchases: List<Purchase>) {
        val subscriptionIds = _subscriptions.value.map { it.productId }
        val activeSubscriptions = purchases.filter { purchase ->
            subscriptionIds.contains(purchase.productId) && isSubscriptionActive(purchase)
        }
        _activeSubscriptions.value = activeSubscriptions
    }
    
    /**
     * Check if a subscription is active based on platform-specific criteria
     */
    private fun isSubscriptionActive(purchase: Purchase): Boolean {
        return when (getCurrentPlatform()) {
            IAPPlatform.IOS -> {
                // For iOS, we should check expiration date but it's not in current Purchase class
                // For now, we consider it active if it exists in available purchases
                true
            }
            IAPPlatform.ANDROID -> {
                // For Android, check purchase state
                // State "1" means purchased, which is active for subscriptions
                purchase.purchaseStateAndroid == "1"
            }
        }
    }
    
    /**
     * Check if a specific subscription is active
     */
    fun isSubscriptionActive(subscriptionId: String): Boolean {
        return _activeSubscriptions.value.any { it.productId == subscriptionId }
    }
    
    /**
     * Refresh purchases and update subscription status
     */
    suspend fun refreshPurchases() {
        try {
            val purchases = iap.getAvailablePurchases()
            _availablePurchases.value = purchases
            updateActiveSubscriptions(purchases)
        } catch (e: Exception) {
            // Silently handle errors during refresh
        }
    }
    
    /**
     * Check available purchases without triggering restore
     */
    fun getCachedAvailablePurchases(): List<Purchase> {
        return _availablePurchases.value
    }
    
    /**
     * Force refresh purchases (always triggers restore)
     */
    suspend fun forceRefreshPurchases() {
        val purchases = iap.getAvailablePurchases()
        _availablePurchases.value = purchases
        updateActiveSubscriptions(purchases)
    }
}

/**
 * Extension function to simplify creating UseIap with a custom scope
 */
fun useIap(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
    options: UseIapOptions = UseIapOptions()
): UseIap = UseIap(scope = scope, options = options)