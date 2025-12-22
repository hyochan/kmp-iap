---
title: Types
sidebar_position: 2
---


import IapKitBanner from '@site/src/uis/IapKitBanner';

# Types

<IapKitBanner />

Comprehensive type definitions for kmp-iap v1.0.0-rc following [OpenIAP specification](https://openiap.dev). All types are fully documented with Kotlin data classes for complete type safety and cross-platform compatibility.

## OpenIAP Base Interfaces

### ProductCommon (OpenIAP Base)

Base interface following OpenIAP specification for all product types.

```kotlin
interface ProductCommon {
    val id: String              // Unified product identifier
    val title: String           // Product title
    val description: String     // Product description
    val type: ProductType       // "inapp" or "subs"
    val displayName: String?    // Optional display name
    val displayPrice: String    // Formatted price for display
    val currency: String        // ISO currency code
    val price: Double?          // Numeric price value
    val debugDescription: String?
    val platform: String?      // Platform identifier
}
```

### PurchaseCommon (OpenIAP Base)

Base interface following OpenIAP specification for all purchase types.

```kotlin
interface PurchaseCommon {
    val id: String              // Transaction identifier
    val productId: String       // Product that was purchased
    val ids: List<String>?      // Multiple product IDs (Android)
    val transactionId: String?  // @deprecated - use id instead
    val transactionDate: Double // Unix timestamp
    val transactionReceipt: String
    val purchaseToken: String?  // Unified token field
    val platform: String?
}
```

## Platform-Specific Implementations

### ProductIOS

iOS product implementation with platform-specific fields.

```kotlin
data class ProductIOS(
    // ProductCommon fields
    override val id: String,
    override val title: String,
    override val description: String,
    override val type: ProductType,
    override val displayPrice: String,
    override val currency: String,
    override val price: Double?,
    override val debugDescription: String?,
    override val displayName: String?,
    override val platform: String = "ios",

    // iOS-specific fields
    val displayNameIOS: String,
    val isFamilyShareableIOS: Boolean,
    val jsonRepresentationIOS: String,
    val subscriptionInfoIOS: SubscriptionInfoIOS? = null
) : ProductCommon
```

### ProductAndroid

Android product implementation with Google Play Billing fields.

```kotlin
data class ProductAndroid(
    // ProductCommon fields
    override val id: String,
    override val title: String,
    override val description: String,
    override val type: ProductType,
    override val displayPrice: String,
    override val currency: String,
    override val price: Double?,
    override val debugDescription: String?,
    override val displayName: String?,
    override val platform: String = "android",

    // Android-specific fields
    val nameAndroid: String,
    val oneTimePurchaseOfferDetailsAndroid: ProductAndroidOneTimePurchaseOfferDetail? = null,
    val subscriptionOfferDetailsAndroid: List<ProductSubscriptionAndroidOfferDetail>? = null
) : ProductCommon
```

### PurchaseIOS

iOS purchase implementation with StoreKit fields.

```kotlin
data class PurchaseIOS(
    // PurchaseCommon fields
    override val id: String,
    override val productId: String,
    override val ids: List<String>? = null,
    override val transactionId: String? = null,  // @deprecated
    override val transactionDate: Double,
    override val transactionReceipt: String,
    override val purchaseToken: String? = null,
    override val platform: String = "ios",

    // iOS-specific fields
    val jwsRepresentationIOS: String? = null,
    val originalTransactionDateIOS: Double? = null,
    val originalTransactionIdentifierIOS: String? = null,
    val transactionState: TransactionStateIOS? = null
) : PurchaseCommon
```

### PurchaseAndroid

Android purchase implementation with Google Play Billing fields.

```kotlin
data class PurchaseAndroid(
    // PurchaseCommon fields
    override val id: String,
    override val productId: String,
    override val ids: List<String>? = null,
    override val transactionId: String? = null,  // @deprecated
    override val transactionDate: Double,
    override val transactionReceipt: String,
    override val purchaseToken: String? = null,
    override val platform: String = "android",

    // Android-specific fields
    val purchaseStateAndroid: AndroidPurchaseState? = null,
    val acknowledgedAndroid: Boolean? = null,
    val autoRenewingAndroid: Boolean? = null,
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    val orderIdAndroid: String? = null,
    val packageNameAndroid: String? = null,
    val purchaseTimeAndroid: Double? = null,
    val purchaseTokenAndroid: String? = null,  // @deprecated
    val signatureAndroid: String? = null
) : PurchaseCommon
```

## Type Aliases for Convenience

OpenIAP-compliant type aliases for backward compatibility and ease of use.

```kotlin
// Union types
typealias Product = ProductCommon
typealias Purchase = PurchaseCommon
typealias SubscriptionProduct = ProductCommon
typealias ProductPurchase = PurchaseCommon
typealias SubscriptionPurchase = PurchaseCommon
```

## Request Types (OpenIAP-Compliant)

### RequestPurchaseProps

OpenIAP-compliant purchase request structure with platform-specific options.

```kotlin
data class RequestPurchaseProps(
    val ios: RequestPurchaseIosProps? = null,
    val android: RequestPurchaseAndroidProps? = null
)
```

### RequestPurchaseIosProps

iOS-specific purchase request parameters.

```kotlin
data class RequestPurchaseIosProps(
    val sku: String,
    val andDangerouslyFinishTransactionAutomatically: Boolean? = null,
    val appAccountToken: String? = null,
    val quantity: Int? = null,
    val withOffer: PaymentDiscountIOS? = null
)
```

### RequestPurchaseAndroidProps

Android-specific purchase request parameters.

```kotlin
data class RequestPurchaseAndroidProps(
    val skus: List<String>,
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    val isOfferPersonalized: Boolean? = null
)
```

### RequestSubscriptionProps

OpenIAP-compliant subscription request structure.

```kotlin
data class RequestSubscriptionProps(
    val ios: RequestPurchaseIosProps? = null,
    val android: RequestSubscriptionAndroidProps? = null
)
```

### ActiveSubscription

Represents an active subscription with platform-specific details following OpenIAP specification.

```kotlin
data class ActiveSubscription(
    val productId: String,                    // Product identifier
    val isActive: Boolean,                    // Always true for active subscriptions

    // iOS-specific fields
    val expirationDateIOS: Long? = null,      // Expiration timestamp (iOS only)
    val environmentIOS: String? = null,       // "Sandbox" | "Production" (iOS only)
    val daysUntilExpirationIOS: Int? = null,  // Days remaining until expiration (iOS only)

    // Android-specific fields
    val autoRenewingAndroid: Boolean? = null, // Auto-renewal status (Android only)

    // Cross-platform fields
    val willExpireSoon: Boolean? = null       // True if expiring within 7 days
)
```

**Platform-Specific Behavior**:

- **iOS**: Provides exact expiration dates, environment info, and calculated days until expiration
- **Android**: Provides auto-renewal status. When `false`, the subscription will not renew
- **Cross-platform**: `willExpireSoon` warns if subscription expires within 7 days

**Use Cases**:

- Feature gating based on subscription status
- Showing expiration warnings
- Managing subscription renewals
- Environment-specific testing (iOS Sandbox vs Production)

### ProductRequest

Request parameters for loading products.

```kotlin
data class ProductRequest(
    val skus: List<String>,
    val type: ProductType
)
```

## iOS-Specific Types

### SubscriptionInfoIOS

iOS subscription information from StoreKit.

```kotlin
data class SubscriptionInfoIOS(
    val subscriptionGroupId: String,
    val subscriptionPeriod: SubscriptionOfferPeriod
)
```

### SubscriptionOfferPeriod

iOS subscription period definition.

```kotlin
data class SubscriptionOfferPeriod(
    val unit: String,        // "DAY", "WEEK", "MONTH", "YEAR"
    val value: Int           // Number of units
)
```

### PaymentDiscountIOS

iOS promotional offer payment discount.

```kotlin
data class PaymentDiscountIOS(
    val identifier: String,
    val keyIdentifier: String,
    val nonce: String,
    val signature: String,
    val timestamp: Double
)
```

### DiscountIOS

iOS product discount information.

```kotlin
data class DiscountIOS(
    val identifier: String,
    val type: String,
    val numberOfPeriods: String,
    val price: String,
    val localizedPrice: String,
    val paymentMode: String,
    val subscriptionPeriod: String
)
```

## Android-Specific Types

### ProductAndroidOneTimePurchaseOfferDetail

Android one-time purchase offer details from Google Play Billing.

```kotlin
data class ProductAndroidOneTimePurchaseOfferDetail(
    val formattedPrice: String,
    val priceAmountMicros: String,
    val priceCurrencyCode: String
)
```

### ProductSubscriptionAndroidOfferDetail

Android subscription offer details.

```kotlin
data class ProductSubscriptionAndroidOfferDetail(
    val basePlanId: String,
    val offerId: String?,
    val offerToken: String,
    val pricingPhases: List<PricingPhaseAndroid>,
    val offerTags: List<String>
)
```

### PricingPhaseAndroid

Android subscription pricing phase.

```kotlin
data class PricingPhaseAndroid(
    val formattedPrice: String,
    val priceCurrencyCode: String,
    val billingPeriod: String,
    val billingCycleCount: Int,
    val priceAmountMicros: String,
    val recurrenceMode: Int
)
```

### SubscriptionOfferAndroid

Android subscription offer for purchase requests.

```kotlin
data class SubscriptionOfferAndroid(
    val basePlanId: String,
    val offerId: String?,
    val offerToken: String
)
```

### RequestSubscriptionAndroidProps

Android-specific subscription request parameters.

```kotlin
data class RequestSubscriptionAndroidProps(
    val skus: List<String>,
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    val isOfferPersonalized: Boolean? = null,
    val purchaseTokenAndroid: String? = null,
    val replacementModeAndroid: Int? = null,
    val subscriptionOffers: List<SubscriptionOfferAndroid>
)
```

## Enums

### IapPlatform

Platform identifier for cross-platform compatibility.

```kotlin
enum class IapPlatform {
    IOS,
    ANDROID
}
```

### Store

Store type identifier.

```kotlin
enum class Store {
    NONE,
    PLAY_STORE,
    AMAZON,
    APP_STORE
}
```

### ProductType

Product type for queries.

```kotlin
enum class ProductType {
    INAPP,  // One-time purchases
    SUBS    // Subscriptions
}
```

### TransactionStateIOS

iOS transaction states from StoreKit.

```kotlin
enum class TransactionStateIOS {
    PURCHASING,
    PURCHASED,
    FAILED,
    RESTORED,
    DEFERRED
}
```

### AndroidPurchaseState

Android purchase states from Google Play Billing.

```kotlin
enum class AndroidPurchaseState {
    UNSPECIFIED_STATE,  // 0 - Unspecified state
    PURCHASED,          // 1 - Purchase completed
    PENDING             // 2 - Purchase pending
}
```

### SubscriptionPeriodUnitIOS

iOS subscription period units from StoreKit.

```kotlin
enum class SubscriptionPeriodUnitIOS {
    DAY,
    WEEK,
    MONTH,
    YEAR
}
```

### DiscountPaymentModeIOS

iOS discount payment modes.

```kotlin
enum class DiscountPaymentModeIOS {
    PAYASYOUGO,
    PAYUPFRONT,
    FREETRIAL
}
```

### DiscountTypeIOS

iOS discount types.

```kotlin
enum class DiscountTypeIOS {
    INTRODUCTORY,
    SUBSCRIPTION
}
```

## Error Types (OpenIAP-Compliant)

### PurchaseError

Exception thrown for IAP errors following OpenIAP specification.

```kotlin
class PurchaseError(
    val code: String,           // ErrorCode enum value
    override val message: String,
    val responseCode: Int? = null,
    val debugMessage: String? = null,
    val purchaseToken: String? = null
) : Exception(message)
```

### ErrorCode

OpenIAP-compliant error code enumeration.

```kotlin
enum class ErrorCode {
    E_UNKNOWN,
    E_USER_CANCELLED,
    E_USER_ERROR,
    E_ITEM_UNAVAILABLE,
    E_REMOTE_ERROR,
    E_NETWORK_ERROR,
    E_SERVICE_ERROR,
    E_RECEIPT_FAILED,
    E_RECEIPT_FINISHED_FAILED,
    E_NOT_PREPARED,
    E_NOT_ENDED,
    E_ALREADY_OWNED,
    E_DEVELOPER_ERROR,
    E_BILLING_RESPONSE_JSON_PARSE_ERROR,
    E_DEFERRED_PAYMENT,
    E_INTERRUPTED,
    E_IAP_NOT_AVAILABLE,
    E_PURCHASE_ERROR,
    E_SYNC_ERROR,
    E_TRANSACTION_VALIDATION_FAILED,
    E_ACTIVITY_UNAVAILABLE,
    E_ALREADY_PREPARED,
    E_PENDING,
    E_CONNECTION_CLOSED,
    E_PRODUCT_NOT_AVAILABLE
}
```

## Validation Types

### ValidationOptions

Union type for platform-specific validation options.

```kotlin
sealed class ValidationOptions {
    data class IOSValidation(
        val receiptBody: IOSReceiptBody
    ) : ValidationOptions()

    data class AndroidValidation(
        val packageName: String,
        val productId: String,
        val productToken: String,
        val accessToken: String,
        val isSub: Boolean = false
    ) : ValidationOptions()
}
```

### IOSReceiptBody

iOS receipt validation parameters.

```kotlin
data class IOSReceiptBody(
    val receiptData: String,
    val password: String? = null
)
```

### ValidationResult

Receipt validation result.

```kotlin
data class ValidationResult(
    val isValid: Boolean,
    val status: Int,
    val receipt: Map<String, Any>? = null
)
```

## Utility Types

### DeepLinkOptions

Options for deep linking to subscription management.

```kotlin
data class DeepLinkOptions(
    val skuAndroid: String? = null,
    val skuIOS: String? = null
)
```

### PurchaseOptions

Options for querying purchases.

```kotlin
data class PurchaseOptions(
    val productType: ProductType? = null,
    val activeOnly: Boolean = false
)
```

### ConnectionResult

Connection state result.

```kotlin
data class ConnectionResult(
    val connected: Boolean,
    val message: String
)
```

### Subscription

Interface for event listeners.

```kotlin
interface Subscription {
    fun remove()
}
```

## Usage Examples

### Cross-Platform Product Handling

```kotlin
// OpenIAP-compliant product handling
fun displayProduct(product: ProductCommon) {
    // Use standardized fields
    println("Product: ${product.title}")
    println("Price: ${product.displayPrice}")
    println("Type: ${product.type}")

    // Platform-specific enhancements
    when (product.platform) {
        "ios" -> {
            val iosProduct = product as ProductIOS
            if (iosProduct.isFamilyShareableIOS) {
                println("Available for family sharing")
            }
        }
        "android" -> {
            val androidProduct = product as ProductAndroid
            androidProduct.oneTimePurchaseOfferDetailsAndroid?.let { offer ->
                println("Detailed pricing: ${offer.formattedPrice}")
            }
        }
    }
}
```

### Standardized Purchase Processing

```kotlin
// OpenIAP-compliant purchase flow
kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
    // Use unified fields
    val receiptData = PurchaseReceiptData(
        transactionId = purchase.id,
        productId = purchase.productId,
        purchaseToken = purchase.purchaseToken,
        transactionDate = purchase.transactionDate,
        platform = purchase.platform
    )

    // Validate with your backend
    val isValid = validateReceiptOnServer(receiptData)

    if (isValid) {
        // Grant entitlement
        grantEntitlement(purchase.productId)

        // Finish transaction
        kmpIapInstance.finishTransaction(
            purchase = purchase,
            isConsumable = determineIfConsumable(purchase.productId)
        )
    }
}
```

### Making Purchases with OpenIAP Request Structure

```kotlin
// OpenIAP-compliant purchase request
val purchase = kmpIapInstance.requestPurchase(
    RequestPurchaseProps(
        ios = RequestPurchaseIosProps(
            sku = "premium",
            quantity = 1
        ),
        android = RequestPurchaseAndroidProps(
            skus = listOf("premium")
        )
    )
)
```

## Best Practices

1. **Use OpenIAP interfaces**: Always work with `ProductCommon` and `PurchaseCommon` for maximum compatibility
2. **Platform-specific casting**: Cast to platform-specific types only when needed for specific features
3. **Unified fields first**: Use unified fields like `purchaseToken` instead of platform-specific deprecated fields
4. **Error handling**: Always catch `PurchaseError` and check OpenIAP-compliant error codes
5. **Backward compatibility**: Existing deprecated fields are still available for migration

## Migration from Legacy Types

### Type Aliases for Compatibility

```kotlin
// These work automatically with existing code
val products: List<Product> = kmpIapInstance.requestProducts(...)
val purchase: Purchase = kmpIapInstance.requestPurchase(...)

// But you can now access OpenIAP-compliant fields
products.forEach { product ->
    val displayPrice = product.displayPrice  // ✅ New unified field
    val productType = product.type           // ✅ New unified field
}
```

### Deprecated Field Migration

```kotlin
// ❌ OLD: Platform-specific deprecated fields
val tokenOld = (purchase as? PurchaseAndroid)?.purchaseTokenAndroid
    ?: (purchase as? PurchaseIOS)?.jwsRepresentationIOS

// ✅ NEW: Unified purchaseToken field
val tokenNew = purchase.purchaseToken

// ❌ OLD: UnifiedPurchaseRequest (deprecated)
val oldRequest = UnifiedPurchaseRequest(sku = "premium")

// ✅ NEW: OpenIAP-compliant RequestPurchaseProps
val newRequest = RequestPurchaseProps(
    ios = RequestPurchaseIosProps(sku = "premium"),
    android = RequestPurchaseAndroidProps(skus = listOf("premium"))
)
```

## See Also

- [Core Methods](./core-methods.md) - How to use these OpenIAP-compliant types
- [Error Codes](./error-codes.md) - Complete OpenIAP error code reference
- [OpenIAP Specification](https://openiap.dev) - Official OpenIAP standard
