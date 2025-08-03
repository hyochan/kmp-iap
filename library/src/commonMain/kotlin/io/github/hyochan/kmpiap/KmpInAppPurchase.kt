package io.github.hyochan.kmpiap

interface KmpInAppPurchase {
    fun isSupported(): Boolean
    suspend fun initialize()
    suspend fun getProducts(productIds: List<String>): List<Product>
    suspend fun purchase(productId: String): PurchaseResult
    suspend fun restorePurchases(): List<Purchase>
}

data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: String
)

data class Purchase(
    val productId: String,
    val orderId: String,
    val purchaseTime: Long,
    val purchaseState: PurchaseState
)

enum class PurchaseState {
    PURCHASED,
    PENDING
}

sealed class PurchaseResult {
    data class Success(val purchase: Purchase) : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
    data object Cancelled : PurchaseResult()
}