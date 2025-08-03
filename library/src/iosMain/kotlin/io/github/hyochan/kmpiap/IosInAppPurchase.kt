package io.github.hyochan.kmpiap

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.StoreKit.*
import platform.Foundation.*

actual class KmpIap {
    actual companion object {
        actual fun getVersion(): String = "KMP-IAP v0.0.0-alpha1 (iOS)"
    }
}

class IosInAppPurchase : KmpInAppPurchase {
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    override fun isSupported(): Boolean {
        // Check if StoreKit is available
        return SKPaymentQueue.canMakePayments()
    }

    override suspend fun initialize() {
        // TODO: Set up payment queue observer
        _isInitialized.value = true
    }

    override suspend fun getProducts(productIds: List<String>): List<Product> {
        // TODO: Fetch products from App Store
        return emptyList()
    }

    override suspend fun purchase(productId: String): PurchaseResult {
        // TODO: Initiate purchase through StoreKit
        return PurchaseResult.Error("Not implemented")
    }

    override suspend fun restorePurchases(): List<Purchase> {
        // TODO: Restore purchases through StoreKit
        return emptyList()
    }
}