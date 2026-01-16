---
title: Types
sidebar_position: 2
---

import IapKitBanner from '@site/src/uis/IapKitBanner';

# Types

<IapKitBanner />

Comprehensive type definitions for kmp-iap v1.3.0 following [OpenIAP specification](https://openiap.dev). All types are fully documented with Kotlin data classes for complete type safety and cross-platform compatibility.

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
    val purchaseState: PurchaseState  // v1.3.0: Now a common field (pending, purchased, unknown)
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
    override val purchaseState: PurchaseState,  // v1.3.0: Common field

    // iOS-specific fields
    val jwsRepresentationIOS: String? = null,
    val originalTransactionDateIOS: Double? = null,
    val originalTransactionIdentifierIOS: String? = null,
    val transactionState: TransactionStateIOS? = null  // @deprecated in StoreKit 2
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
    override val purchaseState: PurchaseState,  // v1.3.0: Common field

    // Android-specific fields
    // Note: purchaseStateAndroid was removed in v1.3.0, use purchaseState instead
    val acknowledgedAndroid: Boolean? = null,
    val autoRenewingAndroid: Boolean? = null,
    val obfuscatedAccountIdAndroid: String? = null,
    val obfuscatedProfileIdAndroid: String? = null,
    val orderIdAndroid: String? = null,
    val packageNameAndroid: String? = null,
    val purchaseTimeAndroid: Double? = null,
    val purchaseTokenAndroid: String? = null,  // @deprecated
    val signatureAndroid: String? = null,
    // New in v1.2.0 (Google Play Billing 8.1.0+)
    val isSuspendedAndroid: Boolean? = null   // Subscription suspended due to payment issues
) : PurchaseCommon
```

:::tip Handling Suspended Subscriptions (v1.2.0)
When `isSuspendedAndroid` is `true`, the subscription is suspended due to payment issues. Direct users to fix their payment method:
```kotlin
if (purchase.isSuspendedAndroid == true) {
    // Show UI to direct user to fix payment method
    showPaymentIssueDialog()
}
```
:::

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
    val withOffer: PaymentDiscountIOS? = null,
    // New in v1.3.7 (openiap-apple)
    val advancedCommerceData: String? = null
)
```

#### advancedCommerceData (v1.3.7)

Support for StoreKit 2's `Product.PurchaseOption.custom` API to pass attribution data during purchases.

**Use Cases**:
- Campaign attribution tracking
- Affiliate marketing integration
- Promotional code tracking

**Example**:
```kotlin
kmpIapInstance.requestPurchase {
    apple {
        sku = "com.example.premium"
        advancedCommerceData = "campaign_summer_2025"
    }
}
```

The data is formatted as JSON internally: `{"signatureInfo": {"token": "<value>"}}`

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

:::info google field support (v1.3.15)
Starting from openiap-google v1.3.15, you can use the `google` field instead of `android` for purchase requests. The `android` field is still supported for backward compatibility but is deprecated.

```kotlin
// Recommended (new)
kmpIapInstance.requestPurchase {
    google {
        skus = listOf("sku_id")
    }
}

// Still supported (deprecated)
kmpIapInstance.requestPurchase {
    android {
        skus = listOf("sku_id")
    }
}
```
:::

### RequestSubscriptionProps

OpenIAP-compliant subscription request structure.

```kotlin
data class RequestSubscriptionProps(
    val ios: RequestSubscriptionIosProps? = null,
    val android: RequestSubscriptionAndroidProps? = null
)
```

### RequestSubscriptionIosProps

iOS-specific subscription request parameters. Similar to `RequestPurchaseIosProps` but for subscriptions.

```kotlin
data class RequestSubscriptionIosProps(
    val sku: String,
    val andDangerouslyFinishTransactionAutomatically: Boolean? = null,
    val appAccountToken: String? = null,
    val quantity: Int? = null,
    val withOffer: PaymentDiscountIOS? = null,
    // New in v1.3.7 (openiap-apple)
    val advancedCommerceData: String? = null
)
```

The `advancedCommerceData` field works the same way as in `RequestPurchaseIosProps`.

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

Android one-time purchase offer details from Google Play Billing. **Updated in v1.2.0** with discount support.

:::warning Breaking Change (v1.2.0)
`oneTimePurchaseOfferDetailsAndroid` is now a **List** instead of a single object to support multiple offers with discounts.

```kotlin
// Before (v1.0.0)
val price = product.oneTimePurchaseOfferDetailsAndroid?.formattedPrice

// After (v1.2.0)
val price = product.oneTimePurchaseOfferDetailsAndroid?.firstOrNull()?.formattedPrice
```
:::

```kotlin
data class ProductAndroidOneTimePurchaseOfferDetail(
    val formattedPrice: String,
    val priceAmountMicros: String,
    val priceCurrencyCode: String,
    // New fields in v1.2.0 (Google Play Billing 7.0+)
    val offerId: String? = null,
    val offerTags: List<String>? = null,
    val offerToken: String? = null,
    val discountDisplayInfo: DiscountDisplayInfoAndroid? = null,
    val limitedQuantityInfo: LimitedQuantityInfoAndroid? = null,
    val validTimeWindow: ValidTimeWindowAndroid? = null,
    val preorderDetails: PreorderDetailsAndroid? = null,
    val rentalDetails: RentalDetailsAndroid? = null
)
```

### DiscountDisplayInfoAndroid

Display information for one-time product discounts (Google Play Billing 7.0+).

```kotlin
data class DiscountDisplayInfoAndroid(
    val percentageDiscount: Int? = null,
    val discountAmount: DiscountAmountAndroid? = null
)
```

### DiscountAmountAndroid

Discount amount details for Android.

```kotlin
data class DiscountAmountAndroid(
    val discountAmountMicros: String,
    val formattedDiscountAmount: String
)
```

### LimitedQuantityInfoAndroid

Limited quantity information for offers.

```kotlin
data class LimitedQuantityInfoAndroid(
    val maximumQuantity: Int,
    val remainingQuantity: Int
)
```

### ValidTimeWindowAndroid

Validity period for offers.

```kotlin
data class ValidTimeWindowAndroid(
    val startTimeMillis: String,
    val endTimeMillis: String
)
```

### PreorderDetailsAndroid

Pre-order details for products (Google Play Billing 8.1.0+).

```kotlin
data class PreorderDetailsAndroid(
    val preorderReleaseTimeMillis: String,
    val preorderPresaleEndTimeMillis: String
)
```

### RentalDetailsAndroid

Rental details for products.

```kotlin
data class RentalDetailsAndroid(
    val rentalPeriod: String,
    val rentalExpirationPeriod: String? = null
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

### PurchaseState

Unified purchase state for cross-platform use.

:::warning Breaking Change (v1.3.0)
`PurchaseState` enum has been simplified. The following states were removed:
- `Failed` - Both platforms return errors instead of Purchase objects on failure
- `Restored` - Restored purchases return as `Purchased` state
- `Deferred` - iOS StoreKit 2 has no transaction state; Android uses `Pending`

**Migration Guide:**
```kotlin
// Before (v1.2.x)
when (purchase.purchaseState) {
    PurchaseState.Purchased, PurchaseState.Restored -> handleSuccess()
    PurchaseState.Pending, PurchaseState.Deferred -> handlePending()
    PurchaseState.Failed -> handleFailure()
    else -> {}
}

// After (v1.3.0)
when (purchase.purchaseState) {
    PurchaseState.Purchased -> handleSuccess()
    PurchaseState.Pending -> handlePending()
    PurchaseState.Unknown -> {}
}
// Handle failures via purchaseErrorListener instead
```
:::

```kotlin
enum class PurchaseState(val rawValue: String) {
    Pending("pending"),     // Purchase is being processed
    Purchased("purchased"), // Purchase completed successfully (includes restored)
    Unknown("unknown")      // Unknown state
}
```

### TransactionStateIOS

iOS transaction states from StoreKit.

:::info Deprecated in StoreKit 2
Apple's StoreKit 1 `SKPaymentTransactionState` is fully deprecated. StoreKit 2 uses `Product.PurchaseResult` instead, which only provides a `Transaction` on success.
:::

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

## Cross-Platform Offer Types (v1.3.12+)

These types provide a unified way to work with subscription and discount offers across both iOS and Android platforms.

### SubscriptionOffer

Cross-platform subscription offer type with platform-specific fields. Available on both `ProductSubscriptionIOS` and `ProductSubscriptionAndroid`.

```kotlin
data class SubscriptionOffer(
    val id: String,                           // Offer identifier
    val displayPrice: String,                 // Formatted price (e.g., "$9.99/month")
    val price: Double,                        // Numeric price value
    val currency: String?,                    // ISO 4217 currency code (e.g., "USD")
    val type: DiscountOfferType,              // Introductory, Promotional, or OneTime
    val paymentMode: PaymentMode?,            // FreeTrial, PayAsYouGo, or PayUpFront
    val period: SubscriptionPeriod?,          // Subscription period
    val periodCount: Int?,                    // Number of periods

    // iOS-specific fields
    val keyIdentifierIOS: String?,            // Key identifier for signature validation
    val nonceIOS: String?,                    // UUID nonce for signature validation
    val signatureIOS: String?,                // Server-generated signature for promotional offers
    val timestampIOS: Double?,                // Timestamp when signature was generated
    val localizedPriceIOS: String?,           // Localized price string
    val numberOfPeriodsIOS: Int?,             // Number of billing periods

    // Android-specific fields
    val basePlanIdAndroid: String?,           // Base plan identifier
    val offerTokenAndroid: String?,           // Offer token required for purchase
    val offerTagsAndroid: List<String>?,      // Tags associated with this offer
    val pricingPhasesAndroid: PricingPhasesAndroid?  // Pricing phases for this offer
)
```

**Usage Example:**

```kotlin
// Access subscription offers from a product
val product = kmpIapInstance.fetchProducts { skus = listOf("premium_monthly") }.first()

when (product) {
    is ProductSubscriptionAndroid -> {
        product.subscriptionOffers.forEach { offer ->
            println("Offer: ${offer.id}")
            println("Price: ${offer.displayPrice}")
            println("Type: ${offer.type}")  // Introductory, Promotional
            println("Payment Mode: ${offer.paymentMode}")  // FreeTrial, PayAsYouGo, PayUpFront

            // Use offer token for purchase
            offer.offerTokenAndroid?.let { token ->
                kmpIapInstance.requestSubscription {
                    android {
                        skus = listOf("premium_monthly")
                        subscriptionOffers = listOf(
                            AndroidSubscriptionOfferInput(
                                sku = "premium_monthly",
                                offerToken = token
                            )
                        )
                    }
                }
            }
        }
    }
    is ProductSubscriptionIOS -> {
        product.subscriptionOffers?.forEach { offer ->
            println("Offer: ${offer.id}")
            println("Price: ${offer.displayPrice}")
            println("Type: ${offer.type}")
            println("Period: ${offer.period?.value} ${offer.period?.unit}")
        }
    }
}
```

### DiscountOffer

Cross-platform discount offer type for one-time purchases (primarily Android).

```kotlin
data class DiscountOffer(
    val currency: String,                     // ISO 4217 currency code
    val displayPrice: String,                 // Formatted price string
    val price: Double,                        // Numeric price value
    val type: DiscountOfferType,              // Type of discount
    val id: String?,                          // Offer identifier

    // Android-specific fields
    val discountAmountMicrosAndroid: String?,      // Fixed discount in micro-units
    val formattedDiscountAmountAndroid: String?,   // Formatted discount (e.g., "$5.00 OFF")
    val fullPriceMicrosAndroid: String?,           // Original price in micro-units
    val offerTagsAndroid: List<String>?,           // Tags for this offer
    val offerTokenAndroid: String?,                // Token required for purchase
    val percentageDiscountAndroid: Int?,           // Percentage discount (e.g., 33 for 33% off)
    val validTimeWindowAndroid: ValidTimeWindowAndroid?  // Offer validity window
)
```

### DiscountOfferType

Type of discount/subscription offer.

```kotlin
enum class DiscountOfferType(val rawValue: String) {
    Introductory("introductory"),   // First-time subscriber discount
    Promotional("promotional"),     // Promotional/winback offer
    OneTime("one-time")             // One-time purchase discount (Android)
}
```

### PaymentMode

Payment mode during offer period.

```kotlin
enum class PaymentMode(val rawValue: String) {
    FreeTrial("free-trial"),        // Free trial period
    PayAsYouGo("pay-as-you-go"),    // Pay each billing cycle
    PayUpFront("pay-up-front")      // Pay full amount upfront
}
```

### SubscriptionPeriod

Subscription period definition.

```kotlin
data class SubscriptionPeriod(
    val value: Int,                  // Number of units
    val unit: SubscriptionPeriodUnit // Period unit
)
```

### SubscriptionPeriodUnit

Unit for subscription period.

```kotlin
enum class SubscriptionPeriodUnit(val rawValue: String) {
    Day("day"),
    Week("week"),
    Month("month"),
    Year("year")
}
```

## Billing Programs API Types (v1.2.0+)

New types for Google Play Billing Programs API (Android 8.2.0+).

### BillingProgramAndroid

Billing program types for external billing.

:::warning Breaking Change (v1.3.0)
`AlternativeBillingModeAndroid` is deprecated. Use `BillingProgramAndroid` with `enableBillingProgramAndroid` in `InitConnectionConfig` instead.

**Migration Guide:**
```kotlin
// Before (deprecated)
val config = InitConnectionConfig(
    alternativeBillingModeAndroid = AlternativeBillingModeAndroid.UserChoice
)

// After (recommended)
val config = InitConnectionConfig(
    enableBillingProgramAndroid = BillingProgramAndroid.UserChoiceBilling
)
```

| Before (Deprecated) | After (Recommended) |
|---------------------|---------------------|
| `alternativeBillingModeAndroid: USER_CHOICE` | `enableBillingProgramAndroid: USER_CHOICE_BILLING` |
| `alternativeBillingModeAndroid: ALTERNATIVE_ONLY` | `enableBillingProgramAndroid: EXTERNAL_OFFER` |
:::

```kotlin
enum class BillingProgramAndroid(val rawValue: String) {
    Unspecified("unspecified"),
    ExternalContentLink("external-content-link"),
    ExternalOffer("external-offer"),
    ExternalPayments("external-payments"),      // 8.3.0+ (Japan only)
    UserChoiceBilling("user-choice-billing")    // 7.0+ (New in v1.3.0)
}
```

### AlternativeBillingModeAndroid (Deprecated)

:::caution Deprecated
Use `BillingProgramAndroid` with `enableBillingProgramAndroid` instead.
:::

```kotlin
@Deprecated("Use BillingProgramAndroid with enableBillingProgramAndroid instead")
enum class AlternativeBillingModeAndroid(val rawValue: String) {
    None("none"),
    UserChoice("user-choice"),
    AlternativeOnly("alternative-only")
}
```

### ExternalLinkLaunchMode

Launch mode for external links.

```kotlin
enum class ExternalLinkLaunchMode {
    Unspecified,
    LaunchInExternalBrowserOrApp,  // Open link in browser or app
    CallerWillLaunchLink           // Caller handles opening the link
}
```

### ExternalLinkType

Type of external link.

```kotlin
enum class ExternalLinkType {
    Unspecified,
    LinkToDigitalContentOffer,  // Link to digital content offer
    LinkToAppDownload           // Link to app download
}
```

### LaunchExternalLinkParams

Parameters for launching external links.

```kotlin
data class LaunchExternalLinkParams(
    val billingProgram: BillingProgram,
    val launchMode: ExternalLinkLaunchMode,
    val linkType: ExternalLinkType,
    val linkUri: String
)
```

### BillingProgramAvailabilityResult

Result of checking billing program availability.

```kotlin
data class BillingProgramAvailabilityResult(
    val billingProgram: BillingProgram,
    val isAvailable: Boolean
)
```

### BillingProgramReportingDetails

Reporting details for external transactions.

```kotlin
data class BillingProgramReportingDetails(
    val billingProgram: BillingProgram,
    val externalTransactionToken: String
)
```

:::info Note
The Billing Programs API requires Google Play Billing Library 8.2.0+. These methods currently return `FeatureNotSupported` error as the underlying Google Play Billing Library APIs are not yet available in the current billing-ktx dependency.
:::

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

## Purchase Verification Types

### VerifyPurchaseProps

Platform-specific options for purchase verification.

```kotlin
data class VerifyPurchaseProps(
    val apple: VerifyPurchaseAppleOptions? = null,
    val google: VerifyPurchaseGoogleOptions? = null,
    val horizon: VerifyPurchaseHorizonOptions? = null
)
```

### VerifyPurchaseAppleOptions

iOS-specific verification options.

```kotlin
data class VerifyPurchaseAppleOptions(
    val sku: String
)
```

### VerifyPurchaseGoogleOptions

Android-specific verification options.

```kotlin
data class VerifyPurchaseGoogleOptions(
    val sku: String,
    val accessToken: String,      // Obtain from your backend
    val packageName: String,
    val purchaseToken: String,
    val isSub: Boolean? = null
)
```

:::warning Security Note
The `accessToken` must be obtained from your secure backend. Never hardcode or store Google API credentials in your app.
:::

### VerifyPurchaseHorizonOptions

Meta Quest (Horizon) verification options.

```kotlin
data class VerifyPurchaseHorizonOptions(
    val sku: String,
    val userId: String,
    val accessToken: String       // Obtain from your backend
)
```

### PurchaseVerificationProvider

Supported third-party verification providers.

```kotlin
enum class PurchaseVerificationProvider(val rawValue: String) {
    Iapkit("iapkit")
}
```

### VerifyPurchaseWithProviderProps

Configuration for provider-based verification (e.g., IAPKit).

```kotlin
data class VerifyPurchaseWithProviderProps(
    val provider: PurchaseVerificationProvider,
    val iapkit: RequestVerifyPurchaseWithIapkitProps?
)

data class RequestVerifyPurchaseWithIapkitProps(
    val apiKey: String?,
    val apple: RequestVerifyPurchaseWithIapkitAppleProps?,
    val google: RequestVerifyPurchaseWithIapkitGoogleProps?
)
```

### IapkitPurchaseState

Purchase states returned by IAPKit verification.

```kotlin
enum class IapkitPurchaseState(val rawValue: String) {
    Unknown("unknown"),
    Entitled("entitled"),
    PendingAcknowledgment("pending_acknowledgment"),
    Pending("pending"),
    Canceled("canceled"),
    Expired("expired"),
    ReadyToConsume("ready_to_consume"),
    Consumed("consumed"),
    Inauthentic("inauthentic")
}
```

| State | Description |
|-------|-------------|
| `Entitled` | Purchase is valid and user has access |
| `PendingAcknowledgment` | Purchase needs acknowledgment (Android) |
| `Pending` | Purchase is being processed |
| `Canceled` | Purchase was canceled |
| `Expired` | Subscription has expired |
| `ReadyToConsume` | Consumable ready to be consumed |
| `Consumed` | Consumable has been consumed |
| `Inauthentic` | Purchase failed verification (potential fraud) |
| `Unknown` | Unknown state |

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
