package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.flow.Flow

/**
 * Android-specific implementation of KmpIAP singleton
 */
actual object KmpIAP : KmpInAppPurchase {
    private val delegate = AndroidInAppPurchase()
    
    override fun getVersion(): String = delegate.getVersion()
    
    override val purchaseUpdatedFlow: Flow<Purchase> 
        get() = delegate.purchaseUpdatedFlow
    
    override val purchaseErrorFlow: Flow<PurchaseError>
        get() = delegate.purchaseErrorFlow
    
    override val connectionStateFlow: Flow<ConnectionResult>
        get() = delegate.connectionStateFlow
    
    override val promotedProductFlow: Flow<String?>
        get() = delegate.promotedProductFlow
    
    override suspend fun initConnection() = delegate.initConnection()
    
    override suspend fun endConnection() = delegate.endConnection()
    
    override suspend fun requestProducts(params: RequestProductsParams): List<BaseProduct> = 
        delegate.requestProducts(params)
    
    override suspend fun requestPurchase(request: RequestPurchase, type: PurchaseType) = 
        delegate.requestPurchase(request, type)
    
    override suspend fun getAvailablePurchases(): List<Purchase> = 
        delegate.getAvailablePurchases()
    
    override suspend fun getPurchaseHistories(): List<Purchase> = 
        delegate.getPurchaseHistories()
    
    override suspend fun finishTransaction(purchase: Purchase, isConsumable: Boolean): Boolean = 
        delegate.finishTransaction(purchase, isConsumable)
    
    override suspend fun getStorefrontIOS(): AppStoreInfo? = 
        delegate.getStorefrontIOS()
    
    override suspend fun presentCodeRedemptionSheetIOS() = 
        delegate.presentCodeRedemptionSheetIOS()
    
    override suspend fun showManageSubscriptionsIOS() = 
        delegate.showManageSubscriptionsIOS()
    
    override suspend fun deepLinkToSubscriptionsAndroid(sku: String?) = 
        delegate.deepLinkToSubscriptionsAndroid(sku)
    
    override suspend fun acknowledgePurchaseAndroid(purchaseToken: String): Boolean = 
        delegate.acknowledgePurchaseAndroid(purchaseToken)
    
    override suspend fun consumePurchaseAndroid(purchaseToken: String): Boolean = 
        delegate.consumePurchaseAndroid(purchaseToken)
    
    override suspend fun validateReceiptIos(
        receiptBody: Map<String, String>,
        isTest: Boolean
    ): Map<String, Any>? = delegate.validateReceiptIos(receiptBody, isTest)
    
    override suspend fun validateReceiptAndroid(
        packageName: String,
        productId: String,
        productToken: String,
        accessToken: String,
        isSub: Boolean
    ): Map<String, Any>? = delegate.validateReceiptAndroid(
        packageName, productId, productToken, accessToken, isSub
    )
    
    override fun getStore(): Store = delegate.getStore()
    
    override suspend fun canMakePayments(): Boolean = delegate.canMakePayments()
}