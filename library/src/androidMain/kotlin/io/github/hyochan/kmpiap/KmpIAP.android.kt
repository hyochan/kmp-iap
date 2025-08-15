package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.flow.Flow

/**
 * Android-specific implementation of KmpIAP class
 */
@OptIn(kotlin.ExperimentalMultiplatform::class)
actual class KmpIAP actual constructor() {
    private val delegate = InAppPurchaseAndroid()
    
    actual fun getVersion(): String = delegate.getVersion()
    
    // Event Listeners
    actual val purchaseUpdatedListener: Flow<Purchase>
        get() = delegate.purchaseUpdatedListener
    
    actual val purchaseErrorListener: Flow<PurchaseError>
        get() = delegate.purchaseErrorListener
    
    actual val promotedProductListener: Flow<String?>
        get() = delegate.promotedProductListener
    
    // Connection Management
    actual suspend fun initConnection(): Boolean = delegate.initConnection()
    
    actual suspend fun endConnection() = delegate.endConnection()
    
    // Product Management
    actual suspend fun requestProducts(params: ProductRequest): List<Product> = 
        delegate.requestProducts(params)
    
    // Purchase Operations
    actual suspend fun requestPurchase(request: UnifiedPurchaseRequest): Purchase = 
        delegate.requestPurchase(request)
    
    actual suspend fun getAvailablePurchases(options: PurchaseOptions?): List<Purchase> = 
        delegate.getAvailablePurchases(options)
    
    actual suspend fun getPurchaseHistories(options: PurchaseOptions?): List<ProductPurchase> = 
        delegate.getPurchaseHistories(options)
    
    actual suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean?): Boolean = 
        delegate.finishTransaction(purchase, isConsumable)
    
    // Validation
    actual suspend fun validateReceipt(options: ValidationOptions): ValidationResult = 
        delegate.validateReceipt(options)
    
    actual suspend fun isPurchaseValid(purchase: Purchase): Boolean = 
        delegate.isPurchaseValid(purchase)
    
    // iOS-specific APIs
    actual suspend fun finishTransactionIOS(transactionId: String) = 
        delegate.finishTransactionIOS(transactionId)
    
    actual suspend fun clearTransactionIOS() = 
        delegate.clearTransactionIOS()
    
    actual suspend fun clearProductsIOS() = 
        delegate.clearProductsIOS()
    
    actual suspend fun getStorefrontIOS(): String = 
        delegate.getStorefrontIOS()
    
    actual suspend fun presentCodeRedemptionSheetIOS() = 
        delegate.presentCodeRedemptionSheetIOS()
    
    actual suspend fun getPromotedProductIOS(): String? = 
        delegate.getPromotedProductIOS()
    
    actual suspend fun buyPromotedProductIOS() = 
        delegate.buyPromotedProductIOS()
    
    // Android-specific APIs
    actual suspend fun acknowledgePurchaseAndroid(purchaseToken: String) = 
        delegate.acknowledgePurchaseAndroid(purchaseToken)
    
    actual suspend fun consumePurchaseAndroid(purchaseToken: String) = 
        delegate.consumePurchaseAndroid(purchaseToken)
    
    // Subscription Management
    actual suspend fun deepLinkToSubscriptions(options: DeepLinkOptions) = 
        delegate.deepLinkToSubscriptions(options)
    
    // Utility
    actual fun getStore(): Store = delegate.getStore()
    
    actual suspend fun canMakePayments(): Boolean = delegate.canMakePayments()
}
