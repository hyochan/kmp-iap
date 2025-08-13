package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.flow.Flow

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

// Backward compatibility extension functions
@Deprecated("Use 'deepLinkToSubscriptions' instead", ReplaceWith("deepLinkToSubscriptions(DeepLinkOptions(skuAndroid = sku))"))
suspend fun KmpInAppPurchase.deepLinkToSubscriptionsAndroid(sku: String) {
    deepLinkToSubscriptions(DeepLinkOptions(skuAndroid = sku))
}

