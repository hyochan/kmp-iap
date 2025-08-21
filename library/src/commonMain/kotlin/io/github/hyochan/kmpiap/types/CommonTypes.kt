package io.github.hyochan.kmpiap.types

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Common types matching OpenIAP specification
 */

/**
 * Product type enum matching OpenIAP spec
 */
@Serializable
enum class ProductType {
    @SerialName("inapp")
    INAPP,
    @SerialName("subs")
    SUBS
}

/**
 * Platform identifier
 */
enum class IapPlatform {
    IOS,
    ANDROID
}

/**
 * Get the current platform
 */
expect fun getCurrentPlatform(): IapPlatform

/**
 * Connection result for IAP service connection
 */
data class ConnectionResult(
    val connected: Boolean,
    val message: String? = null
)

/**
 * App Store information (iOS only)
 */
data class AppStoreInfo(
    val countryCode: String? = null,
    val storefront: String? = null,
    val identifier: String? = null
)

/**
 * Active subscription information
 * Contains platform-specific subscription details
 */
data class ActiveSubscription(
    val productId: String,
    val isActive: Boolean,
    val expirationDateIOS: Instant? = null,                    // iOS only - expiration date
    val autoRenewingAndroid: Boolean? = null,                 // Android only  
    val environmentIOS: String? = null,                        // iOS only: "Sandbox" | "Production"
    val willExpireSoon: Boolean? = null,                       // True if expiring within 7 days
    val daysUntilExpirationIOS: Number? = null                 // iOS only
)

/**
 * Product request parameters following OpenIAP spec
 */
data class ProductRequest(
    val skus: List<String>,
    val type: ProductType  // "inapp" or "subs"
)

/**
 * Purchase options for getAvailablePurchases and getPurchaseHistories
 */
data class PurchaseOptions(
    val alsoPublishToEventListener: Boolean? = null,
    val onlyIncludeActiveItems: Boolean? = null
)

/**
 * Deep link options for subscription management
 */
data class DeepLinkOptions(
    val skuAndroid: String? = null,
    val packageNameAndroid: String? = null
)

/**
 * iOS receipt body for validation
 */
data class IOSReceiptBody(
    val receiptData: String,
    val password: String? = null
)

/**
 * Validation options following OpenIAP spec
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
 * Validation result following OpenIAP spec
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