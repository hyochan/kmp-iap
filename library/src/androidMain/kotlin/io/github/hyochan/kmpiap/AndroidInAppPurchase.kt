package io.github.hyochan.kmpiap

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class KmpIap {
    actual companion object {
        actual fun getVersion(): String = "KMP-IAP v0.0.0-alpha1 (Android)"
    }
}

class AndroidInAppPurchase(private val context: Context) : KmpInAppPurchase {
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    override fun isSupported(): Boolean {
        // TODO: Check if Google Play Billing is available
        return true
    }

    override suspend fun initialize() {
        // TODO: Initialize BillingClient
        _isInitialized.value = true
    }

    override suspend fun getProducts(productIds: List<String>): List<Product> {
        // TODO: Query product details from Google Play
        return emptyList()
    }

    override suspend fun purchase(productId: String): PurchaseResult {
        // TODO: Initiate purchase flow
        return PurchaseResult.Error("Not implemented")
    }

    override suspend fun restorePurchases(): List<Purchase> {
        // TODO: Query purchases
        return emptyList()
    }
}