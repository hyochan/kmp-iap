package io.github.hyochan.kmpiap

/**
 * KMP IAP Library
 * Main entry point that exports all public APIs
 * 
 * This library provides a unified API for in-app purchases across iOS and Android,
 * matching the API design of flutter_inapp_purchase and expo-iap
 */

// Re-export all types
typealias BaseProduct = io.github.hyochan.kmpiap.types.BaseProduct
typealias Product = io.github.hyochan.kmpiap.types.Product
typealias Subscription = io.github.hyochan.kmpiap.types.Subscription
typealias Purchase = io.github.hyochan.kmpiap.types.Purchase
typealias PurchaseError = io.github.hyochan.kmpiap.types.PurchaseError
typealias PurchaseResult = io.github.hyochan.kmpiap.types.PurchaseResult
typealias ConnectionResult = io.github.hyochan.kmpiap.types.ConnectionResult
typealias DiscountIOS = io.github.hyochan.kmpiap.types.DiscountIOS
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
 *     RequestProductsParams(
 *         type = PurchaseType.INAPP,
 *         skus = listOf("product1", "product2")
 *     )
 * )
 * 
 * // Listen to purchase updates
 * KmpIAP.purchaseUpdatedFlow.collect { purchase ->
 *     // Handle purchase
 * }
 * ```
 */
expect object KmpIAP