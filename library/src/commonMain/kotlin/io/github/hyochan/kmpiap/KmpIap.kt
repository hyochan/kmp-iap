package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.flow.Flow

/**
 * KMP IAP Library
 * Main entry point that exports all public APIs
 * 
 * This library provides a unified API for in-app purchases across iOS and Android,
 * matching the API design of flutter_inapp_purchase and expo-iap
 */

// Re-export all types
typealias BaseProduct = io.github.hyochan.kmpiap.types.Product
typealias Product = io.github.hyochan.kmpiap.types.Product
typealias SubscriptionProduct = io.github.hyochan.kmpiap.types.SubscriptionProduct
typealias Purchase = io.github.hyochan.kmpiap.types.Purchase
typealias PurchaseError = io.github.hyochan.kmpiap.types.PurchaseError
typealias PurchaseResult = io.github.hyochan.kmpiap.types.PurchaseResult
typealias ConnectionResult = io.github.hyochan.kmpiap.types.ConnectionResult
typealias Discount = io.github.hyochan.kmpiap.types.Discount
typealias SubscriptionOffer = io.github.hyochan.kmpiap.types.SubscriptionOffer
typealias PricingPhase = io.github.hyochan.kmpiap.types.PricingPhase

// Re-export enums
typealias Store = io.github.hyochan.kmpiap.types.Store
typealias IAPPlatform = io.github.hyochan.kmpiap.types.IAPPlatform
typealias PurchaseType = io.github.hyochan.kmpiap.types.PurchaseType
typealias TransactionState = io.github.hyochan.kmpiap.types.TransactionState
typealias PurchaseState = io.github.hyochan.kmpiap.types.PurchaseState
typealias AndroidProrationMode = io.github.hyochan.kmpiap.types.AndroidProrationMode

// Re-export error codes
typealias ErrorCode = io.github.hyochan.kmpiap.utils.ErrorCode
typealias ErrorCodeUtils = io.github.hyochan.kmpiap.utils.ErrorCodeUtils

// Re-export Android types
typealias AndroidPurchaseState = io.github.hyochan.kmpiap.types.AndroidPurchaseState
typealias AndroidProductType = io.github.hyochan.kmpiap.types.AndroidProductType
typealias RequestPurchaseAndroid = io.github.hyochan.kmpiap.types.RequestPurchaseAndroid
typealias RequestSubscriptionAndroid = io.github.hyochan.kmpiap.types.RequestSubscriptionAndroid
typealias SubscriptionOfferAndroid = io.github.hyochan.kmpiap.types.SubscriptionOfferAndroid
typealias AndroidBillingResponseCode = io.github.hyochan.kmpiap.types.AndroidBillingResponseCode

// Re-export iOS types
typealias IosTransactionState = io.github.hyochan.kmpiap.types.IosTransactionState
typealias RequestPurchaseIOS = io.github.hyochan.kmpiap.types.RequestPurchaseIOS
typealias RequestSubscriptionIOS = io.github.hyochan.kmpiap.types.RequestSubscriptionIOS
typealias PaymentDiscount = io.github.hyochan.kmpiap.types.PaymentDiscount
typealias PromotionalOffer = io.github.hyochan.kmpiap.types.PromotionalOffer
typealias AppTransaction = io.github.hyochan.kmpiap.types.AppTransaction
typealias IosSubscriptionPeriodUnit = io.github.hyochan.kmpiap.types.IosSubscriptionPeriodUnit
typealias IosDiscountPaymentMode = io.github.hyochan.kmpiap.types.IosDiscountPaymentMode
typealias IosDiscountType = io.github.hyochan.kmpiap.types.IosDiscountType

/**
 * Re-export the main interface
 */
typealias InAppPurchase = io.github.hyochan.kmpiap.KmpInAppPurchase

// Re-export request types
typealias RequestProductsParams = io.github.hyochan.kmpiap.types.RequestProductsParams
typealias RequestPurchase = io.github.hyochan.kmpiap.types.RequestPurchase
typealias ProductType = io.github.hyochan.kmpiap.types.ProductType
typealias AppStoreInfo = io.github.hyochan.kmpiap.types.AppStoreInfo

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
     * @return True if successful
     */
    suspend fun endConnection(): Boolean

    // ===== Product Management =====
    
    /**
     * Retrieve products or subscriptions from the store
     * @param params Product request with SKUs and type
     * @return List of products matching the provided SKUs
     */
    suspend fun requestProducts(params: ProductRequest): List<Product>

    // ===== Purchase Operations =====
    
    /**
     * Request a purchase (one-time or subscription)
     * @param request Unified purchase request configuration
     * @return The successful purchase
     */
    suspend fun requestPurchase(request: UnifiedPurchaseRequest): Purchase

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
    suspend fun getPurchaseHistories(options: PurchaseOptions? = null): List<ProductPurchase>

    /**
     * Complete a purchase transaction
     * @param purchase The purchase to finish
     * @param isConsumable Whether the purchase is consumable
     */
    suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean? = null)

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
 * Global singleton instance for In-App Purchase operations.
 * 
 * Usage:
 * ```kotlin
 * import io.github.hyochan.kmpiap.KmpIAP
 * 
 * // Initialize connection
 * KmpIAP.initConnection()
 * 
 * // Request products
 * val products = KmpIAP.requestProducts(
 *     ProductRequest(
 *         skus = listOf("product1", "product2"),
 *         type = ProductType.SUBS
 *     )
 * )
 * 
 * // Listen to purchase updates
 * KmpIAP.purchaseUpdatedListener.collect { purchase ->
 *     // Handle purchase
 * }
 * ```
 */
expect object KmpIAP