package io.github.hyochan.kmpiap.types

/**
 * iOS-specific IAP types that will be mapped from StoreKit
 */
data class IosProduct(
    val productIdentifier: String,
    val localizedTitle: String,
    val localizedDescription: String,
    val price: String,
    val priceLocale: String,
    val isDownloadable: Boolean,
    val downloadContentLengths: List<Long>,
    val downloadContentVersion: String?
)

data class IosTransaction(
    val transactionIdentifier: String,
    val productIdentifier: String,
    val transactionDate: Long,
    val transactionState: IosTransactionState,
    val originalTransaction: IosTransaction?
)

enum class IosTransactionState {
    PURCHASING,
    PURCHASED,
    FAILED,
    RESTORED,
    DEFERRED
}