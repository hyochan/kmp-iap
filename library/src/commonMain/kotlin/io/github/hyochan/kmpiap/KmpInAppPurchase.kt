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

    /**
     * Purchase update event flow
     */
    val purchaseUpdatedFlow: Flow<Purchase>

    /**
     * Purchase error event flow
     */
    val purchaseErrorFlow: Flow<PurchaseError>

    /**
     * Connection state event flow
     */
    val connectionStateFlow: Flow<ConnectionResult>

    /**
     * iOS-only: Promoted product event flow
     */
    val promotedProductFlow: Flow<String?>

    /**
     * Initialize IAP connection
     * Must be called before any other IAP operations
     */
    suspend fun initConnection()

    /**
     * End IAP connection
     * Should be called when IAP is no longer needed
     */
    suspend fun endConnection()

    /**
     * Request products with unified API
     * @param params Product request configuration
     * @return List of products or subscriptions
     */
    suspend fun requestProducts(params: RequestProductsParams): List<BaseProduct>

    /**
     * Request a purchase
     * @param request Purchase request configuration
     * @param type Type of purchase: INAPP or SUBS
     */
    suspend fun requestPurchase(request: RequestPurchase, type: PurchaseType = PurchaseType.INAPP)

    /**
     * Get available purchases (previously purchased items)
     * @return List of available purchases
     */
    suspend fun getAvailablePurchases(): List<Purchase>

    /**
     * Get purchase history
     * @return List of purchase history items
     */
    suspend fun getPurchaseHistories(): List<Purchase>

    /**
     * Finish a transaction
     * @param purchase The purchase to finish
     * @param isConsumable Whether the purchase is consumable
     * @return Success status
     */
    suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean = false): Boolean

    /**
     * iOS-only: Get storefront information
     * @return App Store info or null on Android
     */
    suspend fun getStorefrontIOS(): AppStoreInfo?

    /**
     * iOS-only: Present code redemption sheet
     */
    suspend fun presentCodeRedemptionSheetIOS()

    /**
     * iOS-only: Show manage subscriptions
     */
    suspend fun showManageSubscriptionsIOS()

    /**
     * Android-only: Deep link to subscriptions
     * @param sku Optional SKU to highlight
     */
    suspend fun deepLinkToSubscriptionsAndroid(sku: String? = null)

    /**
     * Android-only: Acknowledge purchase
     * @param purchaseToken Purchase token to acknowledge
     * @return Success status
     */
    suspend fun acknowledgePurchaseAndroid(purchaseToken: String): Boolean

    /**
     * Android-only: Consume purchase
     * @param purchaseToken Purchase token to consume
     * @return Success status
     */
    suspend fun consumePurchaseAndroid(purchaseToken: String): Boolean

    /**
     * Validate receipt on iOS
     * @param receiptBody Receipt data to validate
     * @param isTest Whether to use sandbox environment
     * @return Validation response
     */
    suspend fun validateReceiptIos(
        receiptBody: Map<String, String>,
        isTest: Boolean = true
    ): Map<String, Any>?

    /**
     * Validate receipt on Android
     * @param packageName Package name
     * @param productId Product ID
     * @param productToken Product token
     * @param accessToken Access token
     * @param isSub Whether it's a subscription
     * @return Validation response
     */
    suspend fun validateReceiptAndroid(
        packageName: String,
        productId: String,
        productToken: String,
        accessToken: String,
        isSub: Boolean
    ): Map<String, Any>?

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

