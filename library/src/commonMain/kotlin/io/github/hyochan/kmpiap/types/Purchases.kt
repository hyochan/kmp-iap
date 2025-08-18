package io.github.hyochan.kmpiap.types

import io.github.hyochan.kmpiap.utils.ErrorCode
import io.github.hyochan.kmpiap.utils.ErrorCodeUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Base purchase interface following OpenIAP spec
 */
interface PurchaseBase {
    val id: String  // Primary identifier (transaction ID)
    val transactionDate: Double
    val transactionReceipt: String
}

/**
 * iOS-specific purchase fields
 */
interface PurchaseIOS {
    val transactionId: String?
    val originalTransactionDateIOS: Double?
    val originalTransactionIdIOS: String?
    val transactionState: TransactionState?
    val verificationResult: VerificationResult?
    val jwsRepresentationIOS: String?  // iOS StoreKit 2 JWS representation
}

/**
 * Android-specific purchase fields
 */
interface PurchaseAndroid {
    val purchaseTokenAndroid: String?
    val purchaseStateAndroid: Int?
    val signatureAndroid: String?
    val autoRenewingAndroid: Boolean?
    val orderIdAndroid: String?
    val packageNameAndroid: String?
    val developerPayloadAndroid: String?
    val acknowledgedAndroid: Boolean?
}

/**
 * Unified Purchase class following OpenIAP spec
 */
@Serializable
data class Purchase(
    // PurchaseBase fields
    override val id: String,  // Primary identifier (transaction ID)
    override val transactionDate: Double,
    override val transactionReceipt: String,
    
    // Product identification
    val productId: String,  // Product ID for the purchased item
    val ids: List<String>? = null,  // Android: Product IDs array
    
    // Unified purchase token field
    val purchaseToken: String? = null,  // Android: purchase token, iOS: JWS representation
    
    // iOS-specific fields (optional)
    override val transactionId: String? = null,  // @deprecated - use id instead
    override val originalTransactionDateIOS: Double? = null,
    override val originalTransactionIdIOS: String? = null,
    override val transactionState: TransactionState? = null,
    @Transient override val verificationResult: VerificationResult? = null,
    @Deprecated("Use 'purchaseToken' instead", ReplaceWith("purchaseToken"))
    override val jwsRepresentationIOS: String? = null,  // @deprecated - use purchaseToken instead
    
    // Android-specific fields (optional)
    @Deprecated("Use 'purchaseToken' instead", ReplaceWith("purchaseToken"))
    override val purchaseTokenAndroid: String? = null,  // @deprecated - use purchaseToken instead
    override val purchaseStateAndroid: Int? = null,
    override val signatureAndroid: String? = null,
    override val autoRenewingAndroid: Boolean? = null,
    override val orderIdAndroid: String? = null,
    override val packageNameAndroid: String? = null,
    override val developerPayloadAndroid: String? = null,
    override val acknowledgedAndroid: Boolean? = null,
    
    // Platform indicator
    @Transient val platform: IAPPlatform = getCurrentPlatform(),
    @Transient val originalJson: Map<String, Any>? = null
) : PurchaseBase, PurchaseIOS, PurchaseAndroid {
    // Backward compatibility properties
    @Deprecated("Use 'acknowledgedAndroid' instead", ReplaceWith("acknowledgedAndroid"))
    val isAcknowledgedAndroid: Boolean get() = acknowledgedAndroid ?: false
}

/**
 * Product purchase with additional details
 */
data class ProductPurchase(
    val purchase: Purchase,
    val isConsumedAndroid: Boolean? = null,
    val isAcknowledgedAndroid: Boolean? = null,
    val isFinishedIOS: Boolean? = null,
    val purchaseState: PurchaseState? = null
)

/**
 * Purchase error class following documentation spec
 */
class PurchaseError(
    val code: String,
    override val message: String,
    val productId: String? = null,
    val responseCode: Int? = null,
    val debugMessage: String? = null,
    val platform: IAPPlatform? = null
) : Exception(message) {
    
    companion object {
        fun fromPlatformError(
            errorData: Map<String, Any?>,
            platform: IAPPlatform
        ): PurchaseError {
            val errorCode = errorData["code"]?.let { code ->
                ErrorCodeUtils.fromPlatformCode(code, platform)
            } ?: ErrorCode.E_UNKNOWN
            
            return PurchaseError(
                code = errorCode.name,
                message = errorData["message"]?.toString() ?: "Unknown error occurred",
                productId = errorData["productId"]?.toString(),
                responseCode = errorData["responseCode"] as? Int,
                debugMessage = errorData["debugMessage"]?.toString(),
                platform = platform
            )
        }
    }
}

/**
 * Unified purchase request following documentation spec
 */
data class UnifiedPurchaseRequest(
    // Single SKU (convenience)
    val sku: String? = null,
    
    // Multiple SKUs
    val skus: List<String>? = null,
    
    // iOS options
    val andDangerouslyFinishTransactionAutomaticallyIOS: Boolean? = null,
    val appAccountToken: String? = null,
    val quantity: Int? = null,
    val withOffer: PaymentDiscount? = null,
    
    // Android options
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    val isOfferPersonalized: Boolean? = null,
    val subscriptionOffers: List<SubscriptionOffer>? = null,
    val purchaseTokenAndroid: String? = null,
    val replacementModeAndroid: Int? = null
)

/**
 * Platform-specific purchase request
 */
data class PlatformPurchaseRequest(
    val ios: IOSPurchaseOptions? = null,
    val android: AndroidPurchaseOptions? = null
)

/**
 * iOS purchase options
 */
data class IOSPurchaseOptions(
    val sku: String,
    val andDangerouslyFinishTransactionAutomaticallyIOS: Boolean? = null,
    val appAccountToken: String? = null,
    val quantity: Int? = null,
    val withOffer: PaymentDiscount? = null
)

/**
 * Android purchase options
 */
data class AndroidPurchaseOptions(
    val skus: List<String>,
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    val isOfferPersonalized: Boolean? = null,
    val subscriptionOffers: List<SubscriptionOffer>? = null,
    val purchaseTokenAndroid: String? = null,
    val replacementModeAndroid: Int? = null
)

/**
 * Purchase options for getAvailablePurchases and getPurchaseHistories
 */
data class PurchaseOptions(
    val alsoPublishToEventListener: Boolean? = null,
    val onlyIncludeActiveItems: Boolean? = null
)

/**
 * Validation options following documentation spec
 */
sealed class ValidationOptions {
    data class IOSValidation(
        val receiptBody: IOSReceiptBody
    ) : ValidationOptions()
    
    data class AndroidValidation(
        val packageName: String,
        val productToken: String,
        val accessToken: String,
        val isSub: Boolean
    ) : ValidationOptions()
}

/**
 * iOS receipt body for validation
 */
data class IOSReceiptBody(
    val receiptData: String,
    val password: String? = null
)

/**
 * Validation result following documentation spec
 */
data class ValidationResult(
    val isValid: Boolean,
    val status: Int,
    
    // iOS response fields
    val receipt: Map<String, Any>? = null,
    val latestReceipt: String? = null,
    val latestReceiptInfo: List<Map<String, Any>>? = null,
    val pendingRenewalInfo: List<Map<String, Any>>? = null,
    
    // Android response fields
    val purchaseState: Int? = null,
    val consumptionState: Int? = null,
    val acknowledgementState: Int? = null
)

/**
 * Verification result for iOS StoreKit 2
 */
data class VerificationResult(
    val isValid: Boolean,
    val environment: String? = null,
    val verificationError: String? = null
)

/**
 * Deep link options for subscription management
 */
data class DeepLinkOptions(
    val skuAndroid: String? = null,
    val packageNameAndroid: String? = null
)

/**
 * Event subscription for cleanup
 */
interface Subscription {
    fun remove()
}

/**
 * Implementation of event subscription
 */
class EventSubscription(
    private val onRemove: () -> Unit
) : Subscription {
    override fun remove() {
        onRemove()
    }
}