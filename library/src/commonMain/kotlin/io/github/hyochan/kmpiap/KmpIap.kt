package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.types.*
import io.github.hyochan.kmpiap.dsl.*
import kotlinx.coroutines.flow.Flow

/**
 * KMP IAP Library
 * Main entry point that exports all public APIs
 * 
 * This library provides a unified API for in-app purchases across iOS and Android,
 * matching the API design of flutter_inapp_purchase and expo-iap
 */

// Re-export all types
typealias Product = io.github.hyochan.kmpiap.types.Product
typealias ProductAndroid = io.github.hyochan.kmpiap.types.ProductAndroid
typealias ProductIOS = io.github.hyochan.kmpiap.types.ProductIOS
typealias SubscriptionProduct = io.github.hyochan.kmpiap.types.SubscriptionProduct
typealias SubscriptionProductAndroid = io.github.hyochan.kmpiap.types.SubscriptionProductAndroid
typealias SubscriptionProductIOS = io.github.hyochan.kmpiap.types.SubscriptionProductIOS
typealias Purchase = io.github.hyochan.kmpiap.types.Purchase
typealias PurchaseAndroid = io.github.hyochan.kmpiap.types.PurchaseAndroid
typealias PurchaseIOS = io.github.hyochan.kmpiap.types.PurchaseIOS
typealias PurchaseError = io.github.hyochan.kmpiap.types.PurchaseError
typealias ConnectionResult = io.github.hyochan.kmpiap.types.ConnectionResult
typealias ActiveSubscription = io.github.hyochan.kmpiap.types.ActiveSubscription

// Re-export enums
typealias Store = io.github.hyochan.kmpiap.types.Store
typealias IapPlatform = io.github.hyochan.kmpiap.types.IapPlatform

// Re-export error codes
typealias ErrorCode = io.github.hyochan.kmpiap.utils.ErrorCode
typealias ErrorCodeUtils = io.github.hyochan.kmpiap.utils.ErrorCodeUtils

// Re-export Android types
typealias PurchaseAndroidState = io.github.hyochan.kmpiap.types.PurchaseAndroidState
typealias SubscriptionOfferAndroid = io.github.hyochan.kmpiap.types.SubscriptionOfferAndroid
typealias AndroidBillingResponseCode = io.github.hyochan.kmpiap.types.AndroidBillingResponseCode
typealias PricingPhaseAndroid = io.github.hyochan.kmpiap.types.PricingPhaseAndroid

// Re-export iOS types
typealias TransactionStateIOS = io.github.hyochan.kmpiap.types.TransactionStateIOS
typealias PaymentDiscountIOS = io.github.hyochan.kmpiap.types.PaymentDiscountIOS
typealias DiscountIOS = io.github.hyochan.kmpiap.types.DiscountIOS
typealias AppTransactionIOS = io.github.hyochan.kmpiap.types.AppTransactionIOS
typealias PaymentModeIOS = io.github.hyochan.kmpiap.types.PaymentModeIOS
typealias OfferTypeIOS = io.github.hyochan.kmpiap.types.OfferTypeIOS
typealias EnvironmentIOS = io.github.hyochan.kmpiap.types.EnvironmentIOS

/**
 * Re-export the main interface
 */
typealias InAppPurchase = io.github.hyochan.kmpiap.KmpInAppPurchase

// Re-export request types
typealias ProductType = io.github.hyochan.kmpiap.types.ProductType
typealias AppStoreInfo = io.github.hyochan.kmpiap.types.AppStoreInfo
typealias RequestPurchaseIosProps = io.github.hyochan.kmpiap.types.RequestPurchaseIosProps
typealias RequestPurchaseAndroidProps = io.github.hyochan.kmpiap.types.RequestPurchaseAndroidProps
typealias PurchaseOptions = io.github.hyochan.kmpiap.types.PurchaseOptions
typealias DeepLinkOptions = io.github.hyochan.kmpiap.types.DeepLinkOptions
typealias ValidationOptions = io.github.hyochan.kmpiap.types.ValidationOptions
typealias ValidationResult = io.github.hyochan.kmpiap.types.ValidationResult

// Re-export DSL builders
typealias ProductsRequestBuilder = io.github.hyochan.kmpiap.dsl.ProductsRequestBuilder
typealias PurchaseRequestBuilder = io.github.hyochan.kmpiap.dsl.PurchaseRequestBuilder
typealias SubscriptionRequestBuilder = io.github.hyochan.kmpiap.dsl.SubscriptionRequestBuilder

/**
 * Main interface for In-App Purchase operations across all platforms.
 * Designed to match Flutter InApp Purchase and expo-iap APIs.
 */
interface KmpInAppPurchase {
    /**
     * Returns the version of the KMP-IAP library.
     * Format: "KMP-IAP v{version} ({platform})"
     */
    fun getVersion(): String

    // ===== Event Listeners =====
    
    /**
     * Listener for observing purchase updates
     * Collect this Flow to receive purchase completion events
     */
    val purchaseUpdatedListener: Flow<Purchase>

    /**
     * Listener for observing purchase errors
     * Collect this Flow to receive error events
     */
    val purchaseErrorListener: Flow<PurchaseError>

    /**
     * Listener for observing promoted products (iOS only)
     * Collect this Flow to receive promoted product events
     */
    val promotedProductListener: Flow<String?>


    // ===== Connection Management =====
    
    /**
     * Initialize connection to the store service
     * @return True if successful
     */
    suspend fun initConnection(): Boolean

    /**
     * End connection to the store service
     */
    suspend fun endConnection()

    // ===== Product Management =====
    
    /**
     * Retrieve products or subscriptions from the store
     * @param skus List of product SKUs to retrieve
     * @param type Product type (INAPP or SUBS)
     * @return List of products matching the provided SKUs
     */
    suspend fun requestProducts(skus: List<String>, type: ProductType): List<Product>

    // ===== Purchase Operations =====
    
    /**
     * Request a purchase (one-time or subscription)
     * @param sku Product SKU to purchase
     * @param ios iOS-specific purchase options (optional)
     * @param android Android-specific purchase options (optional)
     * @return The successful purchase
     */
    suspend fun requestPurchase(
        sku: String,
        ios: RequestPurchaseIosProps? = null,
        android: RequestPurchaseAndroidProps? = null
    ): Purchase

    /**
     * Get all available purchases for the current user
     * @param options Options for fetching purchases
     * @return List of non-consumed purchases
     */
    suspend fun getAvailablePurchases(options: PurchaseOptions? = null): List<Purchase>

    /**
     * Get purchase history (iOS only, returns empty on Android v8+)
     * @param options Options for fetching purchase history
     * @return List of purchase history items
     */
    suspend fun getPurchaseHistories(options: PurchaseOptions? = null): List<Purchase>

    /**
     * Complete a purchase transaction
     * @param purchase The purchase to finish
     * @param isConsumable Whether the purchase is consumable
     * @return True if transaction was successfully finished
     */
    suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean? = null): Boolean

    // ===== Validation =====
    
    /**
     * Validate a receipt with your server or platform servers
     * @param options Validation options
     * @return Validation result
     */
    suspend fun validateReceipt(options: ValidationOptions): ValidationResult

    /**
     * Quick check if a purchase is valid
     * @param purchase The purchase to validate
     * @return True if the purchase is valid
     */
    suspend fun isPurchaseValid(purchase: Purchase): Boolean

    // ===== iOS-specific APIs =====
    
    /**
     * iOS-specific transaction completion
     * @param transactionId Transaction ID to finish
     */
    suspend fun finishTransactionIOS(transactionId: String)

    /**
     * Clear pending transactions (iOS)
     */
    suspend fun clearTransactionIOS()

    /**
     * Clear the products cache (iOS)
     */
    suspend fun clearProductsIOS()

    /**
     * Get the current App Store storefront country code
     * @return Storefront country code (e.g., "US", "GB", "JP")
     */
    suspend fun getStorefrontIOS(): String

    /**
     * Present code redemption sheet (iOS)
     */
    suspend fun presentCodeRedemptionSheetIOS()

    /**
     * Get promoted product (iOS)
     * @return Product SKU if available
     */
    suspend fun getPromotedProductIOS(): String?

    /**
     * Buy promoted product (iOS)
     */
    suspend fun buyPromotedProductIOS()

    // ===== Android-specific APIs =====
    
    /**
     * Acknowledge a non-consumable purchase or subscription
     * @param purchaseToken Purchase token to acknowledge
     */
    suspend fun acknowledgePurchaseAndroid(purchaseToken: String)

    /**
     * Consume a purchase (for consumable products only)
     * @param purchaseToken Purchase token to consume
     */
    suspend fun consumePurchaseAndroid(purchaseToken: String)

    // ===== Subscription Management =====
    
    /**
     * Open native subscription management interface
     * @param options Deep link options
     */
    suspend fun deepLinkToSubscriptions(options: DeepLinkOptions)

    /**
     * Get all active subscriptions with detailed information
     * @param subscriptionIds Optional list of subscription IDs to check. If null, returns all active subscriptions
     * @return List of active subscriptions
     */
    suspend fun getActiveSubscriptions(subscriptionIds: List<String>? = null): List<ActiveSubscription>

    /**
     * Check if the user has any active subscriptions
     * @param subscriptionIds Optional list of subscription IDs to check. If null, checks all subscriptions
     * @return True if the user has at least one active subscription
     */
    suspend fun hasActiveSubscriptions(subscriptionIds: List<String>? = null): Boolean

    /**
     * Get the current store type
     * @return Store type
     */
    fun getStore(): Store

    /**
     * Check if the device can make payments
     * @return True if payments are available
     */
    suspend fun canMakePayments(): Boolean
}

/**
 * KMP In-App Purchase class.
 * 
 * Usage:
 * ```kotlin
 * import io.github.hyochan.kmpiap.KmpIAP
 * 
 * // Option 1: Use global instance
 * KmpIAP.instance.initConnection()
 * KmpIAP.instance.requestProducts(...)
 * 
 * // Option 2: Create your own instance
 * val kmpIAP = KmpIAP()
 * kmpIAP.initConnection()
 * kmpIAP.requestProducts(...)
 * ```
 */
class KmpIAP : KmpInAppPurchase {
    private val delegate: KmpInAppPurchase = createPlatformInAppPurchase()
    
    override fun getVersion(): String = delegate.getVersion()
    
    // Event Listeners
    override val purchaseUpdatedListener: Flow<Purchase>
        get() = delegate.purchaseUpdatedListener
    
    override val purchaseErrorListener: Flow<PurchaseError>
        get() = delegate.purchaseErrorListener
    
    override val promotedProductListener: Flow<String?>
        get() = delegate.promotedProductListener
    
    // Connection Management
    override suspend fun initConnection(): Boolean = delegate.initConnection()
    
    override suspend fun endConnection() = delegate.endConnection()
    
    // Product Management
    override suspend fun requestProducts(skus: List<String>, type: ProductType): List<Product> = 
        delegate.requestProducts(skus, type)
    
    // Purchase Operations
    override suspend fun requestPurchase(
        sku: String,
        ios: RequestPurchaseIosProps?,
        android: RequestPurchaseAndroidProps?
    ): Purchase = delegate.requestPurchase(sku, ios, android)
    
    override suspend fun getAvailablePurchases(options: PurchaseOptions?): List<Purchase> = 
        delegate.getAvailablePurchases(options)
    
    override suspend fun getPurchaseHistories(options: PurchaseOptions?): List<Purchase> = 
        delegate.getPurchaseHistories(options)
    
    override suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean?): Boolean = 
        delegate.finishTransaction(purchase, isConsumable)
    
    // Validation
    override suspend fun validateReceipt(options: ValidationOptions): ValidationResult = 
        delegate.validateReceipt(options)
    
    override suspend fun isPurchaseValid(purchase: Purchase): Boolean = 
        delegate.isPurchaseValid(purchase)
    
    // iOS-specific APIs
    override suspend fun finishTransactionIOS(transactionId: String) = 
        delegate.finishTransactionIOS(transactionId)
    
    override suspend fun clearTransactionIOS() = 
        delegate.clearTransactionIOS()
    
    override suspend fun clearProductsIOS() = 
        delegate.clearProductsIOS()
    
    override suspend fun getStorefrontIOS(): String = 
        delegate.getStorefrontIOS()
    
    override suspend fun presentCodeRedemptionSheetIOS() = 
        delegate.presentCodeRedemptionSheetIOS()
    
    override suspend fun getPromotedProductIOS(): String? = 
        delegate.getPromotedProductIOS()
    
    override suspend fun buyPromotedProductIOS() = 
        delegate.buyPromotedProductIOS()
    
    // Android-specific APIs
    override suspend fun acknowledgePurchaseAndroid(purchaseToken: String) = 
        delegate.acknowledgePurchaseAndroid(purchaseToken)
    
    override suspend fun consumePurchaseAndroid(purchaseToken: String) = 
        delegate.consumePurchaseAndroid(purchaseToken)
    
    // Subscription Management
    override suspend fun deepLinkToSubscriptions(options: DeepLinkOptions) = 
        delegate.deepLinkToSubscriptions(options)
    
    override suspend fun getActiveSubscriptions(subscriptionIds: List<String>?): List<ActiveSubscription> = 
        delegate.getActiveSubscriptions(subscriptionIds)
    
    override suspend fun hasActiveSubscriptions(subscriptionIds: List<String>?): Boolean = 
        delegate.hasActiveSubscriptions(subscriptionIds)
    
    // Utility
    override fun getStore(): Store = delegate.getStore()
    
    override suspend fun canMakePayments(): Boolean = delegate.canMakePayments()
}

/**
 * Creates platform-specific InAppPurchase implementation
 */
expect fun createPlatformInAppPurchase(): KmpInAppPurchase

/**
 * Global singleton instance of KmpIAP for convenience.
 * 
 * This provides a pre-created instance for developers who prefer using a singleton pattern.
 * 
 * Usage:
 * ```kotlin
 * import io.github.hyochan.kmpiap.kmpIapInstance
 * 
 * // Use the global instance
 * kmpIapInstance.initConnection()
 * kmpIapInstance.requestProducts(...)
 * ```
 * 
 * Note: For better testability and dependency injection,
 * consider creating your own instance with `KmpIAP()`.
 */
val kmpIapInstance: KmpIAP by lazy { KmpIAP() }