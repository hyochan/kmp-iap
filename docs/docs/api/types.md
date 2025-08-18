---
title: Types
sidebar_position: 2
---

# Types

Comprehensive type definitions for kmp-iap v1.0.0. All types are fully documented with Kotlin data classes for complete type safety.

## Core Types

### Product

Represents a product available for purchase with full pricing and metadata.

```kotlin
data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val priceAmount: Double,
    val currency: String,
    
    // iOS-specific fields
    val displayName: String? = null,
    val isFamilyShareable: Boolean = false,
    val discounts: List<Discount>? = null,
    val subscription: SubscriptionInfo? = null,
    
    // Android-specific fields
    val nameAndroid: String? = null,              // Product display name (different from title)
    val typeAndroid: String? = null,              // Product type ("inapp" or "subs")
    val displayPriceAndroid: String? = null,      // Formatted display price
    val oneTimePurchaseOfferDetails: OneTimePurchaseOfferDetails? = null,  // One-time purchase pricing
    val subscriptionOfferDetails: List<OfferDetail>? = null,
    
    // Platform indicator
    val platform: IapPlatform
)
```

**Android Product Field Mapping**:
The Android-specific fields map directly from Google Play Billing's `ProductDetails`:

```kotlin
// Native Android mapping
mapOf(
    "id" to productDetails.productId,               // -> id
    "title" to productDetails.title,                // -> title  
    "description" to productDetails.description,    // -> description
    "type" to productDetails.productType,          // -> typeAndroid
    "displayName" to productDetails.name,           // -> nameAndroid
    "displayPrice" to displayPrice,                 // -> displayPriceAndroid
    "oneTimePurchaseOfferDetails" to offerDetails,  // -> oneTimePurchaseOfferDetails
    "subscriptionOfferDetails" to subscriptions     // -> subscriptionOfferDetails
)
```

**Field Descriptions**:
- `nameAndroid`: Product display name from `productDetails.name` (different from `title`)
- `typeAndroid`: Product type - `"inapp"` for one-time purchases, `"subs"` for subscriptions
- `displayPriceAndroid`: Formatted price string ready for display
- `oneTimePurchaseOfferDetails`: Pricing details for one-time purchases (in-app products)

### SubscriptionProduct

Specialized product type for subscriptions.

```kotlin
data class SubscriptionProduct(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val priceAmount: Double,
    val currency: String,
    val subscriptionPeriod: String,
    
    // Subscription pricing
    val introductoryPrice: String? = null,
    val introductoryPricePeriod: SubscriptionPeriodIOS? = null,
    val introductoryPriceCycles: Int? = null,
    val subscriptionGroupIdentifier: String? = null,
    
    val platform: IapPlatform
)
```

### Purchase

Represents a completed or pending purchase transaction.

```kotlin
data class Purchase(
    // Core fields (OpenIAP spec)
    val id: String,                    // Primary identifier (transaction ID)
    val productId: String,             // Product ID for the purchased item
    val ids: List<String>? = null,     // Android: Product IDs array
    val transactionDate: Double,
    val transactionReceipt: String,
    
    // iOS-specific fields
    val transactionId: String? = null,  // @deprecated - use id instead
    val originalTransactionDateIOS: Double? = null,
    val originalTransactionIdIOS: String? = null,
    val transactionState: TransactionState? = null,
    val verificationResult: VerificationResult? = null,
    
    // Android-specific fields
    val purchaseTokenAndroid: String? = null,
    val purchaseStateAndroid: Int? = null,
    val signatureAndroid: String? = null,
    val autoRenewingAndroid: Boolean? = null,
    val orderIdAndroid: String? = null,
    val packageNameAndroid: String? = null,
    val acknowledgedAndroid: Boolean? = null,
    val dataAndroid: String? = null,
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    
    // Platform indicator
    val platform: IapPlatform
)
```

**OpenIAP Specification Changes**:
- `id`: Primary identifier (iOS: transaction ID, Android: purchase token)
- `productId`: Product identifier for the purchased item
- `ids`: Array of product IDs (Android only, for multi-product purchases)
- `transactionId`: @deprecated - use `id` instead for consistency

**Important Android Fields**:
- `purchaseToken`: Unified purchase token (Android: purchase token, iOS: transaction ID/JWS)
- `acknowledgedAndroid`: Indicates if the purchase has been acknowledged. Subscriptions must be acknowledged within 3 days.
- `purchaseTokenAndroid`: ⚠️ **Deprecated** - Use `purchaseToken` instead
- `jwsRepresentationIOS`: ⚠️ **Deprecated** - Use `purchaseToken` instead
- `signatureAndroid`: Signature for verification

### ActiveSubscription

Represents an active subscription with platform-specific details.

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

## Request Types

### ProductRequest

Request parameters for loading products.

```kotlin
data class ProductRequest(
    val skus: List<String>,
    val type: ProductType
)
```

### UnifiedPurchaseRequest

Cross-platform purchase request.

```kotlin
data class UnifiedPurchaseRequest(
    val sku: String? = null,
    val skus: List<String>? = null,
    val quantity: Int? = null,
    
    // iOS options
    val appAccountTokenIOS: String? = null,
    val promotionalOfferIOS: PromotionalOffer? = null,
    
    // Android options
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    val subscriptionUpdateParamsAndroid: SubscriptionUpdateParams? = null
)
```

### RequestPurchaseIOS

iOS-specific purchase request.

```kotlin
data class RequestPurchaseIOS(
    val sku: String,
    val andDangerouslyFinishTransactionAutomaticallyIOS: Boolean = false,
    val appAccountToken: String? = null,
    val quantity: Int? = null,
    val withOffer: PaymentDiscount? = null
)
```

### RequestPurchaseAndroid

Android-specific purchase request.

```kotlin
data class RequestPurchaseAndroid(
    val skus: List<String>,
    val obfuscatedAccountId: String? = null,
    val obfuscatedProfileId: String? = null,
    val isOfferPersonalized: Boolean = false
)
```

## Subscription Types

### SubscriptionInfo

General subscription information.

```kotlin
data class SubscriptionInfo(
    val subscriptionGroupIdentifier: String? = null,
    val subscriptionPeriod: SubscriptionPeriod? = null,
    val introductoryPrice: IntroductoryPrice? = null,
    val promotionalOffers: List<PromotionalOffer>? = null
)
```

### SubscriptionPeriod

Subscription duration information.

```kotlin
data class SubscriptionPeriod(
    val numberOfUnits: Int,
    val unit: SubscriptionIosPeriod
)
```

### OfferDetail

Android subscription offer details.

```kotlin
data class OfferDetail(
    val offerId: String,
    val basePlanId: String,
    val offerToken: String,
    val pricingPhases: List<PricingPhase>,
    val offerTags: List<String>? = null
)
```

### PricingPhase

Android subscription pricing phase.

```kotlin
data class PricingPhase(
    val billingPeriod: String,
    val formattedPrice: String,
    val priceAmountMicros: String,
    val priceCurrencyCode: String,
    val billingCycleCount: Int,
    val recurrenceMode: RecurrenceMode? = null
)
```

## Enums

### IapPlatform

Platform identifier.

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

### TransactionState

iOS transaction states.

```kotlin
enum class TransactionState {
    PURCHASING,
    PURCHASED,
    FAILED,
    RESTORED,
    DEFERRED
}
```

### PurchaseState

Android purchase states.

```kotlin
enum class PurchaseState {
    UNSPECIFIED,  // 0 - Unspecified state
    PURCHASED,    // 1 - Purchase completed
    PENDING       // 2 - Purchase pending
}
```

### SubscriptionPeriodIOS

iOS subscription period units.

```kotlin
enum class SubscriptionPeriodIOS {
    P1W,  // 1 week
    P1M,  // 1 month
    P2M,  // 2 months
    P3M,  // 3 months
    P6M,  // 6 months
    P1Y   // 1 year
}
```

### RecurrenceMode

Android subscription recurrence modes.

```kotlin
enum class RecurrenceMode {
    INFINITE_RECURRING,       // Charges recur forever
    FINITE_RECURRING,         // Charges recur for a fixed number of cycles
    NON_RECURRING            // Charges occur once
}
```

### ReplacementMode

Android subscription replacement modes (proration).

```kotlin
enum class ReplacementMode {
    UNKNOWN_REPLACEMENT_MODE,
    IMMEDIATE_WITH_TIME_PRORATION,
    IMMEDIATE_AND_CHARGE_PRORATED_PRICE,
    IMMEDIATE_WITHOUT_PRORATION,
    DEFERRED,
    IMMEDIATE_AND_CHARGE_FULL_PRICE
}
```

## Error Types

### PurchaseError

Exception thrown for IAP errors.

```kotlin
data class PurchaseError(
    val code: String,
    override val message: String,
    val productId: String? = null,
    val responseCode: Int? = null,
    val debugMessage: String? = null,
    val platform: IapPlatform? = null,
    val subResponseCode: Int? = null,  // Android billing v8.0.0+
    val subResponseMessage: String? = null
) : Exception(message)
```

### ErrorCode

Error code enumeration.

```kotlin
enum class ErrorCode {
    E_UNKNOWN,
    E_USER_CANCELLED,
    E_NOT_INITIALIZED,
    E_STORE_NOT_AVAILABLE,
    E_PRODUCTS_NOT_FETCHED,
    E_PURCHASE_NOT_ALLOWED,
    E_ITEM_UNAVAILABLE,
    E_DEVELOPER_ERROR,
    E_PRODUCT_ALREADY_PURCHASED,
    E_PRODUCT_NOT_PURCHASED,
    E_PRODUCT_LOAD_FAILED,
    E_UNABLE_TO_BUY,
    E_PENDING,
    E_UNFINISHED_TRANSACTION,
    E_RECEIPT_REQUEST_FAILED,
    E_RECEIPT_LOAD_FAILED,
    E_BILLING_UNAVAILABLE,
    E_SERVICE_ERROR,
    E_SERVICE_TIMEOUT,
    E_SERVICE_DISCONNECTED,
    E_ALREADY_OWNED,
    E_INTERRUPTED,
    E_NETWORK_ERROR,
    E_ACTIVITY_UNAVAILABLE
}
```

## Validation Types

### ValidationOptions

Options for receipt validation.

```kotlin
data class ValidationOptions(
    val receiptIOS: String? = null,
    val signatureAndroid: String? = null,
    val purchaseTokenAndroid: String? = null
)
```

### ValidationResult

Receipt validation result.

```kotlin
data class ValidationResult(
    val isValid: Boolean,
    val status: Int,
    val latestReceiptInfo: List<Map<String, Any>>? = null,
    val pendingRenewalInfo: List<Map<String, Any>>? = null
)
```

## Platform-Specific Types

### iOS Types

#### PaymentDiscount

iOS promotional offer information.

```kotlin
data class PaymentDiscount(
    val identifier: String,
    val keyIdentifier: String,
    val nonce: String,
    val signature: String,
    val timestamp: Long
)
```

#### PromotionalOffer

iOS promotional offer details.

```kotlin
data class PromotionalOffer(
    val offerId: String,
    val price: String,
    val priceAmount: Double,
    val period: SubscriptionPeriod,
    val numberOfPeriods: Int,
    val type: DiscountTypeIOS
)
```

#### VerificationResult

iOS transaction verification result.

```kotlin
enum class VerificationResult {
    VERIFIED,
    UNVERIFIED
}
```

### Android Types

#### OneTimePurchaseOfferDetails

Android one-time purchase pricing details from Google Play Billing's `ProductDetails.OneTimePurchaseOfferDetails`.

```kotlin
data class OneTimePurchaseOfferDetails(
    val priceCurrencyCode: String,     // ISO 4217 currency code (e.g., "USD")
    val formattedPrice: String,        // Formatted price string (e.g., "$0.99")
    val priceAmountMicros: String      // Price in micros (e.g., "990000" for $0.99)
)
```

**Field Descriptions**:
- `priceCurrencyCode`: ISO 4217 currency code for the price
- `formattedPrice`: Human-readable price string formatted for the user's locale
- `priceAmountMicros`: Price in micros (divide by 1,000,000 to get actual price)

#### SubscriptionUpdateParams

Android subscription upgrade/downgrade parameters.

```kotlin
data class SubscriptionUpdateParams(
    val oldPurchaseToken: String,
    val replacementMode: ReplacementMode
)
```

#### DeepLinkOptions

Options for deep linking to subscription management.

```kotlin
data class DeepLinkOptions(
    val skuAndroid: String? = null,
    val skuIOS: String? = null
)
```

## Utility Types

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

## Type Conversion

### Extension Functions

```kotlin
// Convert Android Purchase to unified Purchase
fun com.android.billingclient.api.Purchase.toPurchase(): Purchase

// Convert ProductDetails to unified Product
fun ProductDetails.toProduct(): Product

// Convert iOS Transaction to unified Purchase
fun Transaction.toPurchase(): Purchase
```

## Best Practices

1. **Always check nullable fields**: Many fields are platform-specific and may be null
2. **Use platform checks**: Check `getCurrentPlatform()` before accessing platform-specific fields
3. **Handle acknowledgment**: Check `acknowledgedAndroid` before re-acknowledging
4. **Validate types**: Use proper `ProductType` when querying products
5. **Error handling**: Always catch `PurchaseError` and check error codes

## See Also

- [Core Methods](./core-methods.md) - How to use these types
- [Error Codes](./error-codes.md) - Complete error code reference
- [Platform Differences](../guides/platform-differences.md) - Platform-specific considerations