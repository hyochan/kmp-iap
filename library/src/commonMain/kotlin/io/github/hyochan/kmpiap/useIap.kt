package io.github.hyochan.kmpiap

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IapState {
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())
    val purchases: StateFlow<List<Purchase>> = _purchases.asStateFlow()

    fun setInitialized(value: Boolean) {
        _isInitialized.value = value
    }

    fun setProducts(products: List<Product>) {
        _products.value = products
    }

    fun setPurchases(purchases: List<Purchase>) {
        _purchases.value = purchases
    }
}

fun useIap(): IapState {
    return IapState()
}