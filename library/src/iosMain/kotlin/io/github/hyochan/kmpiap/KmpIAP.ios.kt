package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.flow.Flow

/**
 * iOS-specific implementation of KmpIAP singleton
 */
actual object KmpIAP : KmpInAppPurchase {
    private val delegate = IosInAppPurchase()
    
    override fun getVersion(): String = delegate.getVersion()
    
    // Expose Flow properties from interface
    override val purchaseUpdatedListener: Flow<Purchase>
        get() = delegate.purchaseUpdatedListener
    
    override val purchaseErrorListener: Flow<PurchaseError>
        get() = delegate.purchaseErrorListener
    
    override val promotedProductListener: Flow<String?>
        get() = delegate.promotedProductListener
    
    // Backward compatibility - not in interface
    val connectionStateListener: Flow<ConnectionResult>
        get() = delegate.connectionStateListener
    
    override suspend fun initConnection(): Boolean = delegate.initConnection()
    
    override suspend fun endConnection(): Boolean = delegate.endConnection()
    
    override suspend fun requestProducts(params: ProductRequest): List<Product> = 
        delegate.requestProducts(params)
    
    override suspend fun requestPurchase(request: UnifiedPurchaseRequest): Purchase = 
        delegate.requestPurchase(request)
    
    override suspend fun getAvailablePurchases(options: PurchaseOptions?): List<Purchase> = 
        delegate.getAvailablePurchases(options)
    
    override suspend fun getPurchaseHistories(options: PurchaseOptions?): List<ProductPurchase> = 
        delegate.getPurchaseHistories(options)
    
    override suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean?) = 
        delegate.finishTransaction(purchase, isConsumable)
    
    override suspend fun validateReceipt(options: ValidationOptions): ValidationResult = 
        delegate.validateReceipt(options)
    
    override suspend fun isPurchaseValid(purchase: Purchase): Boolean = 
        delegate.isPurchaseValid(purchase)
    
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
    
    override suspend fun acknowledgePurchaseAndroid(purchaseToken: String) = 
        delegate.acknowledgePurchaseAndroid(purchaseToken)
    
    override suspend fun consumePurchaseAndroid(purchaseToken: String) = 
        delegate.consumePurchaseAndroid(purchaseToken)
    
    override suspend fun deepLinkToSubscriptions(options: DeepLinkOptions) = 
        delegate.deepLinkToSubscriptions(options)
    
    override fun getStore(): Store = delegate.getStore()
    
    override suspend fun canMakePayments(): Boolean = delegate.canMakePayments()
}