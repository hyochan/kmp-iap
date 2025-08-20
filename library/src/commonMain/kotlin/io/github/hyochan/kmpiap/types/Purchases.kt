package io.github.hyochan.kmpiap.types

import io.github.hyochan.kmpiap.utils.ErrorCode
import io.github.hyochan.kmpiap.utils.ErrorCodeUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Purchase types matching OpenIAP specification
 */

/**
 * Base purchase common fields matching OpenIAP spec
 */
interface PurchaseCommon {
    val id: String  // Transaction identifier - used by finishTransaction
    val productId: String  // Product identifier - which product was purchased
    val ids: List<String>?  // Product identifiers for purchases that include multiple products
    val transactionId: String?  // @deprecated - use id instead
    val transactionDate: Double
    val transactionReceipt: String
    val purchaseToken: String?  // Unified purchase token (jwsRepresentation for iOS, purchaseToken for Android)
    val platform: String?
}

/**
 * iOS Purchase matching OpenIAP spec (ProductPurchaseIOS)
 */
@Serializable
data class PurchaseIOS(
    // PurchaseCommon fields
    override val id: String,
    override val productId: String,
    override val ids: List<String>? = null,
    @Deprecated("Use 'id' instead", ReplaceWith("id"))
    override val transactionId: String? = null,
    override val transactionDate: Double,
    override val transactionReceipt: String,
    override val purchaseToken: String? = null,
    override val platform: String = "ios",
    
    // iOS-specific fields matching OpenIAP spec
    val quantityIOS: Int? = null,
    val originalTransactionDateIOS: Double? = null,
    val originalTransactionIdentifierIOS: String? = null,
    val appAccountToken: String? = null,
    
    // iOS StoreKit 2 additional fields
    val expirationDateIOS: Double? = null,
    val webOrderLineItemIdIOS: Long? = null,
    val environmentIOS: String? = null,
    val storefrontCountryCodeIOS: String? = null,
    val appBundleIdIOS: String? = null,
    val productTypeIOS: String? = null,
    val subscriptionGroupIdIOS: String? = null,
    val isUpgradedIOS: Boolean? = null,
    val ownershipTypeIOS: String? = null,
    val reasonIOS: String? = null,
    val reasonStringRepresentationIOS: String? = null,
    val transactionReasonIOS: String? = null,  // 'PURCHASE' | 'RENEWAL' | string
    val revocationDateIOS: Double? = null,
    val revocationReasonIOS: String? = null,
    val offerIOS: OfferIOS? = null,
    
    // Price locale fields
    val currencyCodeIOS: String? = null,
    val currencySymbolIOS: String? = null,
    val countryCodeIOS: String? = null,
    
    // Deprecated field
    @Deprecated("Use 'purchaseToken' instead", ReplaceWith("purchaseToken"))
    val jwsRepresentationIOS: String? = null,
    
    // Internal fields
    @Transient val transactionState: TransactionStateIOS? = null,
    @Transient val verificationResult: VerificationResult? = null
) : PurchaseCommon

/**
 * iOS offer information in purchase
 */
@Serializable
data class OfferIOS(
    val id: String,
    val type: String,
    val paymentMode: String
)

/**
 * Android Purchase matching OpenIAP spec (ProductPurchaseAndroid)
 */
@Serializable
data class PurchaseAndroid(
    // PurchaseCommon fields
    override val id: String,
    override val productId: String,
    override val ids: List<String>? = null,
    @Deprecated("Use 'id' instead", ReplaceWith("id"))
    override val transactionId: String? = null,
    override val transactionDate: Double,
    override val transactionReceipt: String,
    override val purchaseToken: String? = null,
    override val platform: String = "android",
    
    // Android-specific fields matching OpenIAP spec
    @Deprecated("Use 'purchaseToken' instead", ReplaceWith("purchaseToken"))
    val purchaseTokenAndroid: String? = null,
    val dataAndroid: String? = null,
    val signatureAndroid: String? = null,
    val autoRenewingAndroid: Boolean? = null,
    val purchaseStateAndroid: PurchaseAndroidState? = null,
    val isAcknowledgedAndroid: Boolean? = null,
    val packageNameAndroid: String? = null,
    val developerPayloadAndroid: String? = null,
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null
) : PurchaseCommon

/**
 * Type aliases matching OpenIAP spec
 */
typealias ProductPurchaseIOS = PurchaseIOS
typealias ProductPurchaseAndroid = PurchaseAndroid

// Legacy naming for backward compatibility
@Deprecated("Use PurchaseIOS instead", ReplaceWith("PurchaseIOS"))
typealias SubscriptionPurchaseIOS = PurchaseIOS

@Deprecated("Use PurchaseAndroid instead", ReplaceWith("PurchaseAndroid"))
typealias SubscriptionPurchaseAndroid = PurchaseAndroid

/**
 * Union type helpers
 */
typealias ProductPurchase = PurchaseCommon
typealias SubscriptionPurchase = PurchaseCommon
typealias Purchase = PurchaseCommon

/**
 * Request types matching OpenIAP spec
 */

/**
 * iOS-specific purchase request parameters
 */
data class RequestPurchaseIosProps(
    val sku: String,
    val andDangerouslyFinishTransactionAutomatically: Boolean? = null,
    val appAccountToken: String? = null,
    val quantity: Int? = null,
    val withOffer: PaymentDiscountIOS? = null
)

/**
 * Android-specific purchase request parameters
 */
data class RequestPurchaseAndroidProps(
    val skus: List<String>,
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    val isOfferPersonalized: Boolean? = null
)

/**
 * Android-specific subscription request parameters
 */
data class RequestSubscriptionAndroidProps(
    val skus: List<String>,
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    val isOfferPersonalized: Boolean? = null,
    val purchaseTokenAndroid: String? = null,
    val replacementModeAndroid: Int? = null,
    val subscriptionOffers: List<SubscriptionOfferAndroid>
)

/**
 * Platform-specific request structures (internal use only)
 */
internal data class RequestPurchasePropsByPlatforms(
    val ios: RequestPurchaseIosProps? = null,
    val android: RequestPurchaseAndroidProps? = null
)

internal data class RequestSubscriptionPropsByPlatforms(
    val ios: RequestPurchaseIosProps? = null,
    val android: RequestSubscriptionAndroidProps? = null
)

/**
 * Internal type aliases for legacy compatibility
 */
internal typealias RequestPurchaseProps = RequestPurchasePropsByPlatforms
internal typealias RequestSubscriptionProps = RequestSubscriptionPropsByPlatforms

/**
 * Purchase error class following OpenIAP spec
 */
class PurchaseError(
    val code: String,
    override val message: String,
    val responseCode: Int? = null,
    val debugMessage: String? = null,
    val purchaseToken: String? = null,
    @Deprecated("Use 'purchaseToken' instead", ReplaceWith("purchaseToken"))
    val purchaseTokenAndroid: String? = null
) : Exception(message) {
    
    companion object {
        fun fromPlatformError(
            errorData: Map<String, Any?>,
            platform: IapPlatform
        ): PurchaseError {
            val errorCode = errorData["code"]?.let { code ->
                ErrorCodeUtils.fromPlatformCode(code, platform)
            } ?: ErrorCode.E_UNKNOWN
            
            return PurchaseError(
                code = errorCode.name,
                message = errorData["message"]?.toString() ?: "Unknown error occurred",
                responseCode = errorData["responseCode"] as? Int,
                debugMessage = errorData["debugMessage"]?.toString(),
                purchaseToken = errorData["purchaseToken"]?.toString(),
                purchaseTokenAndroid = errorData["purchaseTokenAndroid"]?.toString()
            )
        }
    }
}

/**
 * Purchase result type
 */
data class PurchaseResult(
    val responseCode: Int? = null,
    val debugMessage: String? = null,
    val code: String? = null,
    val message: String? = null,
    val purchaseToken: String? = null,
    @Deprecated("Use 'purchaseToken' instead", ReplaceWith("purchaseToken"))
    val purchaseTokenAndroid: String? = null
)