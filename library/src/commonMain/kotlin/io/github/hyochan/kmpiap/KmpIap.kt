package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.openiap.*
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
typealias Product = io.github.hyochan.kmpiap.openiap.Product
typealias ProductAndroid = io.github.hyochan.kmpiap.openiap.ProductAndroid
typealias ProductIOS = io.github.hyochan.kmpiap.openiap.ProductIOS
typealias SubscriptionProduct = io.github.hyochan.kmpiap.openiap.ProductSubscription
typealias SubscriptionProductAndroid = io.github.hyochan.kmpiap.openiap.ProductSubscriptionAndroid
typealias SubscriptionProductIOS = io.github.hyochan.kmpiap.openiap.ProductSubscriptionIOS
typealias Purchase = io.github.hyochan.kmpiap.openiap.Purchase
typealias PurchaseAndroid = io.github.hyochan.kmpiap.openiap.PurchaseAndroid
typealias PurchaseIOS = io.github.hyochan.kmpiap.openiap.PurchaseIOS
typealias PurchaseError = io.github.hyochan.kmpiap.openiap.PurchaseError
typealias ActiveSubscription = io.github.hyochan.kmpiap.openiap.ActiveSubscription

// Re-export enums
typealias IapPlatform = io.github.hyochan.kmpiap.openiap.IapPlatform

// Re-export error codes
typealias ErrorCode = io.github.hyochan.kmpiap.openiap.ErrorCode

// Re-export Android types
typealias PricingPhaseAndroid = io.github.hyochan.kmpiap.openiap.PricingPhaseAndroid
typealias PricingPhasesAndroid = io.github.hyochan.kmpiap.openiap.PricingPhasesAndroid
typealias AndroidSubscriptionOfferInput = io.github.hyochan.kmpiap.openiap.AndroidSubscriptionOfferInput

// Re-export iOS types
typealias DiscountOfferInputIOS = io.github.hyochan.kmpiap.openiap.DiscountOfferInputIOS
typealias PaymentModeIOS = io.github.hyochan.kmpiap.openiap.PaymentModeIOS
typealias SubscriptionOfferIOS = io.github.hyochan.kmpiap.openiap.SubscriptionOfferIOS
typealias SubscriptionStatusIOS = io.github.hyochan.kmpiap.openiap.SubscriptionStatusIOS
typealias SubscriptionPeriodValueIOS = io.github.hyochan.kmpiap.openiap.SubscriptionPeriodValueIOS
typealias SubscriptionOfferTypeIOS = io.github.hyochan.kmpiap.openiap.SubscriptionOfferTypeIOS
typealias AppTransaction = io.github.hyochan.kmpiap.openiap.AppTransaction

/**
 * Re-export the main interface
 */
typealias InAppPurchase = io.github.hyochan.kmpiap.KmpInAppPurchase

// Re-export request types
typealias ProductType = io.github.hyochan.kmpiap.openiap.ProductType
typealias ProductQueryType = io.github.hyochan.kmpiap.openiap.ProductQueryType
typealias ProductRequest = io.github.hyochan.kmpiap.openiap.ProductRequest
typealias RequestPurchaseIosProps = io.github.hyochan.kmpiap.openiap.RequestPurchaseIosProps
typealias RequestPurchaseAndroidProps = io.github.hyochan.kmpiap.openiap.RequestPurchaseAndroidProps
typealias RequestSubscriptionAndroidProps = io.github.hyochan.kmpiap.openiap.RequestSubscriptionAndroidProps
typealias PurchaseOptions = io.github.hyochan.kmpiap.openiap.PurchaseOptions
typealias DeepLinkOptions = io.github.hyochan.kmpiap.openiap.DeepLinkOptions
typealias ValidationOptions = io.github.hyochan.kmpiap.openiap.ReceiptValidationProps
typealias ValidationResult = io.github.hyochan.kmpiap.openiap.ReceiptValidationResult
typealias ReceiptValidationResultAndroid = io.github.hyochan.kmpiap.openiap.ReceiptValidationResultAndroid
typealias ReceiptValidationResultIOS = io.github.hyochan.kmpiap.openiap.ReceiptValidationResultIOS

// Re-export DSL builders
typealias ProductsRequestBuilder = io.github.hyochan.kmpiap.dsl.ProductsRequestBuilder
typealias PurchaseRequestBuilder = io.github.hyochan.kmpiap.dsl.PurchaseRequestBuilder

/**
 * Main interface for In-App Purchase operations across all platforms.
 * Designed to match Flutter InApp Purchase and expo-iap APIs.
 */
interface KmpInAppPurchase : MutationResolver, QueryResolver, SubscriptionResolver {
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
    fun getStore(): Store

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
 * KmpIAP.instance.fetchProducts(...)
 * 
 * // Option 2: Create your own instance
 * val kmpIAP = KmpIAP()
 * kmpIAP.initConnection()
 * kmpIAP.fetchProducts(...)
 * ```
 */
class KmpIAP(
    private val delegate: KmpInAppPurchase = createPlatformInAppPurchase()
) : KmpInAppPurchase by delegate

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
 * kmpIapInstance.fetchProducts(...)
 * ```
 * 
 * Note: For better testability and dependency injection,
 * consider creating your own instance with `KmpIAP()`.
 */
val kmpIapInstance: KmpIAP by lazy { KmpIAP() }
