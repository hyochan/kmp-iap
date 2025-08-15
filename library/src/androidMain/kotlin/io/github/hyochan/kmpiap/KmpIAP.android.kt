package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.flow.Flow

/**
 * Android-specific implementation of KmpIAP class
 */
actual class KmpIAP actual constructor() : KmpInAppPurchase {
    private val delegate = AndroidInAppPurchase()
    
    actual companion object {
        actual val instance: KmpIAP by lazy { KmpIAP() }
    }
    
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
    
    override suspend fun endConnection(): Boolean = delegate.endConnection()
    
    // Product Management
    override suspend fun requestProducts(params: ProductRequest): List<Product> = 
        delegate.requestProducts(params)
    
    // Purchase Operations
    override suspend fun requestPurchase(request: UnifiedPurchaseRequest): Purchase = 
        delegate.requestPurchase(request)
    
    override suspend fun getAvailablePurchases(options: PurchaseOptions?): List<Purchase> = 
        delegate.getAvailablePurchases(options)
    
    override suspend fun getPurchaseHistories(options: PurchaseOptions?): List<ProductPurchase> = 
        delegate.getPurchaseHistories(options)
    
    override suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean?) = 
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
    
    // Utility
    override fun getStore(): Store = delegate.getStore()
    
    override suspend fun canMakePayments(): Boolean = delegate.canMakePayments()
}
