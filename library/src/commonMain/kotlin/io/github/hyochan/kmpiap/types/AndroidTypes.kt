package io.github.hyochan.kmpiap.types

/**
 * Android-specific IAP types that will be mapped from BillingClient
 */
data class AndroidPurchase(
    val orderId: String,
    val packageName: String,
    val productId: String,
    val purchaseTime: Long,
    val purchaseState: Int,
    val purchaseToken: String,
    val quantity: Int,
    val isAcknowledged: Boolean,
    val isAutoRenewing: Boolean,
    val signature: String,
    val originalJson: String
)

data class AndroidProductDetails(
    val sku: String,
    val type: String,
    val price: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val title: String,
    val description: String
)