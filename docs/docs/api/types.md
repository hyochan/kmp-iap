---
title: Types
sidebar_position: 2
---

# Types

Comprehensive type definitions for kmp-iap v1.0.0. All types are fully documented with Kotlin data classes for complete type safety.

## Core Types

### Product

Represents a product available for purchase.

```kotlin
data class Product(
    val productId: String,
    val price: String? = null,
    val currency: String? = null,
    val title: String? = null,
    val description: String? = null,
    val priceAmountMicros: Long? = null,
    val originalPrice: String? = null,
    val originalPriceAmountMicros: Long? = null,
    
    // Subscription information
    val subscriptionPeriod: String? = null,
    val introductoryPrice: String? = null,
    val introductoryPriceAmountMicros: Long? = null,
    val introductoryPricePeriod: String? = null,
    val introductoryPriceCycles: Int? = null,
    
    // Platform specific
    val platform: IAPPlatform,
    val originalJson: String? = null
)
```

### Purchase

Represents a completed purchase.

```kotlin
data class Purchase(
    val productId: String,
    val transactionId: String?,
    val transactionDate: Long? = null,
    val purchaseToken: String? = null,
    val orderId: String? = null,
    val platform: IAPPlatform,
    
    // Purchase state
    val purchaseState: PurchaseState = PurchaseState.PURCHASED,
    val isAcknowledged: Boolean = false,
    val isAutoRenewing: Boolean = false,
    
    // Platform specific
    val originalJson: String? = null,
    val signature: String? = null // Android only
)
```

### SubscriptionOfferAndroid

Android subscription offer details.

```kotlin
data class SubscriptionOfferAndroid(
    val sku: String,
    val offerToken: String,
    val basePlanId: String? = null,
    val offerId: String? = null,
    val pricingPhases: List<PricingPhaseAndroid> = emptyList()
)
```

### PricingPhaseAndroid

Android subscription pricing phase.

```kotlin
data class PricingPhaseAndroid(
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val formattedPrice: String,
    val billingPeriod: String,
    val billingCycleCount: Int,
    val recurrenceMode: RecurrenceMode
)
```

### ProductIOS

iOS-specific product information.

```kotlin
data class ProductIOS(
    val product: Product,
    val subscriptionPeriodNumberIOS: String? = null,
    val subscriptionPeriodUnitIOS: SubscriptionPeriodUnit? = null,
    val introductoryPriceNumberOfPeriodsIOS: String? = null,
    val introductoryPricePaymentModeIOS: PaymentMode? = null,
    val discounts: List<DiscountIOS> = emptyList()
)
```

### DiscountIOS

iOS promotional discount information.

```kotlin
data class DiscountIOS(
    val identifier: String,
    val type: DiscountType,
    val price: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    val paymentMode: PaymentMode,
    val numberOfPeriods: Int,
    val subscriptionPeriod: String
)
```

### PurchaseError

Detailed error information for failed operations.

```kotlin
data class PurchaseError(
    val code: ErrorCode,
    val message: String,
    val details: String? = null,
    val productId: String? = null,
    val platform: IAPPlatform? = null,
    val underlyingError: Throwable? = null
) : Exception(message)
```

### UseIapOptions

Configuration options for UseIap initialization.

```kotlin
data class UseIapOptions(
    val autoFinishTransactions: Boolean = true,
    val enablePendingPurchases: Boolean = true,
    val connectionTimeout: Duration = 30.seconds
)
```

## Enums

### IAPPlatform

Platform enumeration.

```kotlin
enum class IAPPlatform {
    IOS,
    ANDROID
}
```

### Store

Store type enumeration.

```kotlin
enum class Store {
    APP_STORE,
    PLAY_STORE
}
```

### PurchaseState

Purchase state enumeration.

```kotlin
enum class PurchaseState {
    PENDING,
    PURCHASED,
    UNSPECIFIED
}
```

### ErrorCode

Standardized error codes across platforms.

```kotlin
enum class ErrorCode {
    UNKNOWN,
    USER_CANCELLED,
    ITEM_UNAVAILABLE,
    SERVICE_UNAVAILABLE,
    BILLING_UNAVAILABLE,
    NETWORK_ERROR,
    DEVELOPER_ERROR,
    ALREADY_OWNED,
    NOT_OWNED,
    REMOTE_ERROR,
    RECEIPT_FAILED,
    DEFERRED_PAYMENT,
    IAP_NOT_AVAILABLE,
    TRANSACTION_VALIDATION_FAILED,
    ACTIVITY_UNAVAILABLE,
    CONNECTION_CLOSED,
    FEATURE_NOT_SUPPORTED,
    SERVICE_DISCONNECTED,
    SERVICE_TIMEOUT,
    ILLEGAL_ARGUMENT
}
```

### SubscriptionPeriodUnit

iOS subscription period units.

```kotlin
enum class SubscriptionPeriodUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR
}
```

### PaymentMode

iOS payment mode for discounts.

```kotlin
enum class PaymentMode {
    PAY_AS_YOU_GO,
    PAY_UP_FRONT,
    FREE_TRIAL
}
```

### DiscountType

iOS discount types.

```kotlin
enum class DiscountType {
    INTRODUCTORY,
    SUBSCRIPTION
}
```

### RecurrenceMode

Android subscription recurrence mode.

```kotlin
enum class RecurrenceMode {
    INFINITE_RECURRING,
    FINITE_RECURRING,
    NON_RECURRING
}
```

## StateFlow Types

### Connection State

```kotlin
// In UseIap class
val isConnected: StateFlow<Boolean>
val products: StateFlow<List<Product>>
val subscriptions: StateFlow<List<Product>>
val availablePurchases: StateFlow<List<Purchase>>
val purchaseHistories: StateFlow<List<Purchase>>
val currentPurchase: StateFlow<Purchase?>
val currentError: StateFlow<PurchaseError?>
```

## Platform-Specific Types

### iOS Specific

```kotlin
// StoreKit 2 Transaction
data class TransactionIOS(
    val id: String,
    val originalID: String,
    val productID: String,
    val purchaseDate: Long,
    val originalPurchaseDate: Long?,
    val expirationDate: Long?,
    val revocationDate: Long?,
    val revocationReason: RevocationReason?,
    val isUpgraded: Boolean,
    val offerType: OfferType?,
    val offerID: String?,
    val environment: Environment
)

enum class RevocationReason {
    DEVELOPER_ISSUE,
    OTHER
}

enum class OfferType {
    INTRODUCTORY,
    PROMOTIONAL,
    SUBSCRIPTION_OFFER_CODE
}

enum class Environment {
    PRODUCTION,
    SANDBOX
}
```

### Android Specific

```kotlin
// Billing Client Response
data class BillingResult(
    val responseCode: Int,
    val debugMessage: String
)

// Purchase History Record
data class PurchaseHistoryRecord(
    val purchases: List<Purchase>,
    val billingResult: BillingResult
)

// Billing Connection State
enum class BillingConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    CLOSED
}
```

## Extension Functions

### Product Extensions

```kotlin
// Get localized price string
fun Product.getLocalizedPrice(): String {
    return when (platform) {
        IAPPlatform.IOS -> price ?: ""
        IAPPlatform.ANDROID -> price ?: ""
    }
}

// Check if product is subscription
fun Product.isSubscription(): Boolean {
    return subscriptionPeriod != null
}
```

### Purchase Extensions

```kotlin
// Check if purchase needs acknowledgment (Android)
fun Purchase.needsAcknowledgment(): Boolean {
    return platform == IAPPlatform.ANDROID && !isAcknowledged
}

// Get human-readable purchase date
fun Purchase.getPurchaseDate(): Instant? {
    return transactionDate?.let { Instant.fromEpochMilliseconds(it) }
}
```

## Type Guards

Utility functions for type checking.

```kotlin
// Check current platform
fun getCurrentPlatform(): IAPPlatform {
    return when (Platform.current) {
        Platform.IOS -> IAPPlatform.IOS
        Platform.ANDROID -> IAPPlatform.ANDROID
        else -> throw IllegalStateException("Unsupported platform")
    }
}

// Safe cast to platform-specific types
inline fun <reified T> Any.safeCast(): T? {
    return this as? T
}
```

## Platform Differences

### Key Type Differences

| Feature | iOS | Android |
|---------|-----|---------|
| Product ID | Single product ID | SKU with optional offers |
| Transaction ID | Transaction ID & Original ID | Order ID & Purchase Token |
| Receipt | App Store receipt | Purchase token & signature |
| Subscription Info | Period unit & number | Period string format |
| Error Codes | StoreKit errors | Billing Client response codes |

### Platform-Specific Fields

**iOS Only:**
- `subscriptionPeriodUnitIOS`
- `subscriptionPeriodNumberIOS`
- `introductoryPricePaymentModeIOS`
- `discounts`

**Android Only:**
- `purchaseToken`
- `signature`
- `isAcknowledged`
- `subscriptionOffers`

## Migration Notes

⚠️ **Differences from Flutter plugin:**

1. **Kotlin Types**: Uses data classes instead of Dart classes
2. **Null Safety**: Kotlin's built-in null safety
3. **StateFlow**: Replaces Dart Streams
4. **Coroutines**: Async operations use suspend functions
5. **Sealed Classes**: For better exhaustive when expressions

## See Also

- [Core Methods](./core-methods.md) - Using these types in method calls
- [Error Codes](./error-codes.md) - Detailed error handling
- [Migration Guide](../migration/from-flutter.md) - Migrating from Flutter