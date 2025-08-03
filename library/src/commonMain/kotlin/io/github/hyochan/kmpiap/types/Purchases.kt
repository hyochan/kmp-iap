package io.github.hyochan.kmpiap.types

data class PurchaseDetails(
    val products: List<String>,
    val orderId: String,
    val purchaseTime: Long,
    val purchaseState: Int,
    val purchaseToken: String,
    val isAcknowledged: Boolean,
    val isAutoRenewing: Boolean?,
    val signature: String?,
    val originalJson: String?,
    val developerPayload: String?
)

data class PurchaseUpdateInfo(
    val purchases: List<PurchaseDetails>,
    val billingResult: BillingResult
)

data class BillingResult(
    val responseCode: BillingResponse,
    val debugMessage: String
)