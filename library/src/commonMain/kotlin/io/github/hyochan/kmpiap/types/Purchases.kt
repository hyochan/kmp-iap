package io.github.hyochan.kmpiap.types

import io.github.hyochan.kmpiap.utils.ErrorCode
import io.github.hyochan.kmpiap.utils.ErrorCodeUtils
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Purchase class
 */
@Serializable
data class Purchase(
    val productId: String,
    val transactionId: String? = null,
    val transactionReceipt: String? = null,
    val purchaseToken: String? = null,
    val transactionDate: Instant? = null,
    val platform: IAPPlatform,
    val isAcknowledgedAndroid: Boolean? = null,
    val purchaseStateAndroid: String? = null,
    val originalTransactionIdentifierIOS: String? = null,
    @Transient val originalJson: Map<String, Any>? = null,
    // StoreKit 2 specific fields
    val transactionState: String? = null,
    val isUpgraded: Boolean? = null,
    val expirationDate: Instant? = null,
    val revocationDate: Instant? = null,
    val revocationReason: Int? = null
)

/**
 * Purchase error class
 */
class PurchaseError(
    message: String,
    val responseCode: Int? = null,
    val debugMessage: String? = null,
    val code: ErrorCode? = null,
    val productId: String? = null,
    val platform: IAPPlatform? = null
) : Exception(message) {
    val name: String = "[kmp-iap]: PurchaseError"

    companion object {
        fun fromPlatformError(
            errorData: Map<String, Any?>,
            platform: IAPPlatform
        ): PurchaseError {
            val errorCode = errorData["code"]?.let { code ->
                ErrorCodeUtils.fromPlatformCode(code, platform)
            } ?: ErrorCode.E_UNKNOWN

            return PurchaseError(
                message = errorData["message"]?.toString() ?: "Unknown error occurred",
                responseCode = errorData["responseCode"] as? Int,
                debugMessage = errorData["debugMessage"]?.toString(),
                code = errorCode,
                productId = errorData["productId"]?.toString(),
                platform = platform
            )
        }
    }

    fun getPlatformCode(): Any? {
        return code?.let { platform?.let { p -> ErrorCodeUtils.toPlatformCode(it, p) } }
    }
}

/**
 * Purchase result (legacy)
 */
data class PurchaseResult(
    val responseCode: Int? = null,
    val debugMessage: String? = null,
    val code: String? = null,
    val message: String? = null,
    val purchaseTokenAndroid: String? = null
)

/**
 * Connection result
 */
data class ConnectionResult(
    val connected: Boolean,
    val message: String? = null
)

/**
 * Request parameters for fetching products
 */
data class RequestProductsParams(
    val skus: List<String>,
    val type: PurchaseType = PurchaseType.INAPP
)

/**
 * Base class for purchase requests
 */
sealed class RequestPurchase {
    abstract val sku: String
    abstract val platform: IAPPlatform?
}

/**
 * Cross-platform purchase request
 */
data class RequestPurchaseGeneric(
    override val sku: String,
    override val platform: IAPPlatform? = null
) : RequestPurchase()

/**
 * App Store information (iOS only)
 */
data class AppStoreInfo(
    val countryCode: String? = null,
    val storefront: String? = null,
    val identifier: String? = null
)

/**
 * Base interface for purchase request parameters
 */
interface RequestPurchaseBase {
    val sku: String
    val skus: List<String>
}