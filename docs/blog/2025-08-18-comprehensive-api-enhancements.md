---
title: Comprehensive API Enhancements & OpenIAP Compliance in KMP-IAP v1.0.0-beta.13
description: Complete platform parity with enhanced Android/iOS fields, OpenIAP specification compliance, unified API structure, and improved type safety
slug: comprehensive-api-enhancements
authors:
  - name: KMP-IAP Team
    title: Maintainer
    url: https://github.com/hyochan/kmp-iap
    image_url: https://github.com/hyochan.png
tags:
  [
    android,
    ios,
    api-enhancement,
    naming-conventions,
    purchase-types,
    storekit,
    google-play-billing,
    openiap,
    specification,
    api-standardization,
    type-safety,
    cross-platform,
    standards-compliance,
  ]
hide_table_of_contents: false
date: 2025-08-18
---

# Comprehensive API Enhancements & OpenIAP Compliance in KMP-IAP v1.0.0-beta.13

We're thrilled to announce the most comprehensive update to KMP-IAP yet! Version 1.0.0-beta.13 brings complete platform parity, **100% compliance with the OpenIAP specification**, improved naming conventions, and extensive field additions for both Android and iOS platforms.

<!--truncate-->

## 📢 OpenIAP Specification: The New Standard

KMP-IAP v1.0.0-beta.13 achieves **100% compliance** with the [OpenIAP specification](https://openiap.dev), providing a standardized approach to in-app purchases across different platforms and libraries. By adopting this specification, KMP-IAP now offers:

- **Standardized Types**: Consistent data structures across platforms
- **Unified API**: Common interface patterns for all IAP operations
- **Cross-Platform Compatibility**: Seamless integration with other OpenIAP-compliant libraries
- **Future-Proof Architecture**: Ready for emerging platforms and specifications

Visit [openiap.dev](https://openiap.dev) to learn more about the specification.

## 🚀 What's New

### Enhanced Android Product Support

Complete Google Play Billing `ProductDetails` API parity with new fields:

#### Product Type Fields

- **`typeAndroid`**: `String?` - Product type ("inapp" or "subs") from `productDetails.productType`
- **`nameAndroid`**: `String?` - Product display name from `productDetails.name` (different from title)
- **`displayPriceAndroid`**: `String?` - Formatted display price ready for UI

#### One-Time Purchase Details

- **`oneTimePurchaseOfferDetails`**: `OneTimePurchaseOfferDetails?` - Complete pricing information:
  ```kotlin
  data class OneTimePurchaseOfferDetails(
      val priceCurrencyCode: String,     // ISO 4217 currency code (e.g., "USD")
      val formattedPrice: String,        // Human-readable price (e.g., "$0.99")
      val priceAmountMicros: String      // Price in micros (e.g., "990000")
  )
  ```

### Enhanced iOS Transaction Support

Following StoreKit 2 Transaction API with comprehensive field mapping for iOS 15.0+:

#### Core iOS Fields (Already Supported)

- `quantityIOS`: Purchase quantity
- `originalTransactionDateIOS`: Original purchase date
- `originalTransactionIdIOS`: Original transaction identifier
- `appBundleIdIOS`: App bundle identifier
- `productTypeIOS`: Product type (consumable, non-consumable, etc.)
- `subscriptionGroupIdIOS`: Subscription group identifier

#### Enhanced iOS Fields for Future StoreKit 2 Support

When StoreKit 2 support is added, these fields will be available:

- `signedDateIOS`: Transaction signing date
- `deviceVerificationIOS`: Device verification data
- `deviceVerificationNonceIOS`: Device verification nonce
- `offerIdIOS`: Promotional offer identifier
- `offerTypeIOS`: Offer type (introductory, promotional, code)
- `subscriptionPeriodIOS`: Subscription period unit
- `environmentIOS`: Store environment (Sandbox/Production) - iOS 16.0+
- `storefrontCountryCodeIOS`: Storefront country code - iOS 17.0+
- `reasonIOS`: Transaction reason - iOS 17.0+

### Enhanced Purchase Type Support

Comprehensive purchase data capture with new Android fields:

#### New Android Purchase Fields

- **`dataAndroid`**: `String?` - Original JSON data from purchase
- **`obfuscatedAccountIdAndroid`**: `String?` - Account identifier for purchase attribution
- **`obfuscatedProfileIdAndroid`**: `String?` - Profile identifier for user segmentation

#### Enhanced Error Handling

- **`subResponseCode`**: `Int?` - Android billing v8.0.0+ sub-response code for detailed error info
- **`subResponseMessage`**: `String?` - Human-readable message for sub-response codes

```kotlin
// Example: Enhanced error handling
try {
    val purchase = kmpIapInstance.requestPurchase(request)
} catch (error: PurchaseError) {
    when (error.subResponseCode) {
        1 -> println("Payment declined: ${error.subResponseMessage}")
        // Handle specific billing errors
        else -> println("General error: ${error.message}")
    }
}
```

## 🎯 Complete Type System Overhaul

### OpenIAP-Compliant Base Types

Following the OpenIAP specification, all types now implement standardized interfaces:

```kotlin
// ProductCommon interface - OpenIAP base specification
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
    val platform: String?       // Platform identifier
}

// PurchaseCommon interface - OpenIAP base specification
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

### Platform-Specific Implementation Types

Following OpenIAP naming conventions with proper platform suffixes:

```kotlin
// iOS Product (ProductIOS)
data class ProductIOS(
    // ProductCommon fields
    override val id: String,
    override val title: String,
    // ... other base fields

    // iOS-specific fields with IOS suffix
    val displayNameIOS: String,
    val isFamilyShareableIOS: Boolean,
    val jsonRepresentationIOS: String,
    val subscriptionInfoIOS: SubscriptionInfoIOS? = null,

    // Backward compatibility (deprecated)
    @Deprecated("Use displayNameIOS") val displayName: String? = null,
    override val platform: String = "ios"
) : ProductCommon

// Android Product (ProductAndroid)
data class ProductAndroid(
    // ProductCommon fields
    override val id: String,
    override val title: String,
    // ... other base fields

    // Android-specific fields with Android suffix
    val nameAndroid: String,
    val oneTimePurchaseOfferDetailsAndroid: ProductAndroidOneTimePurchaseOfferDetail? = null,
    val subscriptionOfferDetailsAndroid: List<ProductSubscriptionAndroidOfferDetail>? = null,

    // Backward compatibility (deprecated)
    @Deprecated("Use nameAndroid") val name: String? = null,
    override val platform: String = "android"
) : ProductCommon
```

## 📐 Improved Naming Conventions

### Consistent Platform Suffixes

Following OpenIAP and our internal CLAUDE.md guidelines:

```kotlin
// ✅ CORRECT: Platform suffix at the end
val quantityIOS: Int
val environmentIOS: String
val appBundleIdIOS: String
val purchaseTokenAndroid: String
val packageNameAndroid: String

// ❌ INCORRECT: Platform prefix
val iosQuantity: Int
val androidPurchaseToken: String
```

### Type Name Changes

| Old Name                    | New Name                    |
| --------------------------- | --------------------------- |
| `IosTransactionState`       | `TransactionStateIOS`       |
| `IosSubscriptionPeriodUnit` | `SubscriptionPeriodUnitIOS` |
| `IosDiscountPaymentMode`    | `DiscountPaymentModeIOS`    |
| `IosDiscountType`           | `DiscountTypeIOS`           |
| `SubscriptionIosPeriod`     | `SubscriptionPeriodIOS`     |
| `IapPlatform`               | `IapPlatform`               |

### ID Naming Consistency

```kotlin
// ✅ CORRECT: Use "Id" not "ID"
val productId: String
val transactionId: String
val subscriptionGroupId: String
val orderIdAndroid: String
val originalTransactionIdIOS: String

// ❌ INCORRECT: Using "ID"
val productID: String
val transactionID: String
```

### IAP Acronym Usage

```kotlin
// ✅ CORRECT: IAP as final word
class KmpIAP
val kmpIAP = KmpIAP()

// ✅ CORRECT: Iap when followed by other words
val kmpIapInstance: KmpIAP
enum class IapPlatform { IOS, ANDROID }

// ❌ INCORRECT: Inconsistent usage
val kmpIAPInstance: KmpIAP
```

## 🛠 Platform API Mapping

### Android Product Mapping

```kotlin
// Native Android ProductDetails mapping
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

### Android Purchase Mapping

```kotlin
// Native Android Purchase mapping
val purchaseData = mapOf(
    "id" to purchase.orderId,                       // -> id
    "productId" to purchase.products.first(),       // -> productId
    "purchaseToken" to purchase.purchaseToken,      // -> purchaseToken
    "dataAndroid" to purchase.originalJson,         // -> dataAndroid ✨ NEW
    "signatureAndroid" to purchase.signature,       // -> signatureAndroid
    "obfuscatedAccountId" to accountId,             // -> obfuscatedAccountIdAndroid ✨ NEW
    "obfuscatedProfileId" to profileId              // -> obfuscatedProfileIdAndroid ✨ NEW
)
```

### iOS Transaction Support (Current + Future)

```swift
// Current StoreKit 1 support + Future StoreKit 2 fields
let transactionData = [
    "id": transaction.id,                           // -> id
    "productId": transaction.productID,             // -> productId
    "quantityIOS": transaction.purchasedQuantity,   // -> quantityIOS

    // Future StoreKit 2 fields
    "signedDateIOS": transaction.signedDate,        // -> signedDateIOS
    "deviceVerificationIOS": deviceVerification,    // -> deviceVerificationIOS
    "offerIdIOS": transaction.offerID,             // -> offerIdIOS
    "environmentIOS": transaction.environment       // -> environmentIOS (iOS 16.0+)
]
```

## 📱 Usage Examples

### Enhanced Android Product Usage

```kotlin
val products = kmpIapInstance.requestProducts(
    ProductRequest(listOf("premium_upgrade"), ProductType.INAPP)
)

products.forEach { product ->
    // Use enhanced Android fields
    val displayName = product.nameAndroid ?: product.title
    val displayPrice = product.displayPriceAndroid ?: product.price

    // Check product type
    when (product.typeAndroid) {
        "inapp" -> println("One-time purchase: $displayName")
        "subs" -> println("Subscription: $displayName")
    }

    // Access detailed pricing for one-time purchases
    product.oneTimePurchaseOfferDetails?.let { offer ->
        println("Price: ${offer.formattedPrice}")
        println("Currency: ${offer.priceCurrencyCode}")

        // Convert micros to decimal
        val actualPrice = offer.priceAmountMicros.toLong() / 1_000_000.0
        println("Decimal price: $actualPrice")
    }
}
```

### Enhanced Purchase Handling

```kotlin
// Listen for purchase updates with enhanced data
kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
    // Validate receipt with your backend
    val receiptData = PurchaseReceiptData(
        purchaseToken = purchase.purchaseToken,
        originalJson = purchase.dataAndroid,  // ✨ NEW: Full purchase data
        signature = purchase.signatureAndroid,
        accountId = purchase.obfuscatedAccountIdAndroid,  // ✨ NEW: User attribution
        profileId = purchase.obfuscatedProfileIdAndroid   // ✨ NEW: Profile tracking
    )

    val isValid = validateReceiptOnServer(receiptData)

    if (isValid) {
        grantEntitlement(purchase.productId)
        kmpIapInstance.finishTransaction(purchase, isConsumable = true)
    }
}
```

### Enhanced Error Handling

```kotlin
kmpIapInstance.purchaseErrorListener.collect { error ->
    when (error.code) {
        "E_USER_CANCELLED" -> showUserCancelledMessage()
        "E_PAYMENT_DECLINED" -> {
            // Enhanced Android error handling
            when (error.subResponseCode) {
                1 -> showInsufficientFundsMessage(error.subResponseMessage)
                else -> showGenericPaymentError(error.message)
            }
        }
        else -> showGenericError(error.message)
    }
}
```

## 🔄 Migration Guide

### Unified Purchase Request Structure

Replace old request structures with OpenIAP-compliant ones:

```kotlin
// ❌ OLD: UnifiedPurchaseRequest (deprecated)
val purchase = kmpIapInstance.requestPurchase(
    UnifiedPurchaseRequest(
        sku = "premium",
        quantity = 1
    )
)

// ✅ NEW: OpenIAP-compliant RequestPurchaseProps
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

### Type Names (Automatic via Type Aliases)

The library provides type aliases for renamed types, so existing code continues to work:

```kotlin
// These imports automatically use the new names
import io.github.hyochan.kmpiap.*

// Your existing code works unchanged
val state: TransactionStateIOS = TransactionStateIOS.PURCHASED
```

### New Optional Fields

All new fields are optional (`nullable`), so no migration is required:

```kotlin
// Existing code works unchanged
val purchase = kmpIapInstance.requestPurchase(request)

// New fields available when needed
val accountId = purchase.obfuscatedAccountIdAndroid  // null if not available
val originalData = purchase.dataAndroid              // null if not available
```

### Unified Purchase Token Access

```kotlin
// ✅ NEW: Unified purchaseToken field
val token = purchase.purchaseToken

// ✅ FALLBACK: Platform-specific deprecated fields still work
val tokenFallback = (purchase as? PurchaseAndroid)?.purchaseTokenAndroid
    ?: (purchase as? PurchaseIOS)?.jwsRepresentationIOS
```

## 🎯 Why These Changes Matter

### OpenIAP Standards Compliance

- **Industry Standard**: Follows the OpenIAP specification for cross-library compatibility
- **Interoperability**: Libraries can work together seamlessly
- **Community Standards**: Shared best practices across the ecosystem
- **Innovation**: Focus on features, not API design

### Complete Platform Parity

- **Android**: Full Google Play Billing API coverage
- **iOS**: Comprehensive StoreKit field mapping (current + future StoreKit 2)
- **Consistent**: Unified API across platforms

### Better Developer Experience

- **Type Safety**: All fields properly typed with clear nullability
- **Consistent Naming**: Platform suffixes make code more readable
- **Enhanced Documentation**: Every field documented with usage examples
- **Easier Migration**: Move between OpenIAP-compliant libraries seamlessly

### Improved App Quality

- **Better User Experience**: Access to localized names and formatted prices
- **Enhanced Analytics**: User attribution with account/profile IDs
- **Robust Error Handling**: Detailed error codes for better UX
- **Reduced Bugs**: Standardized error codes and handling

### Future-Proof

- **StoreKit 2 Ready**: Field structure prepared for StoreKit 2 migration
- **Extensible**: Easy to add new platform-specific fields
- **Backward Compatible**: Existing code continues to work
- **Ready for New Platforms**: Architecture supports emerging platforms

## 📚 Complete API Reference

### Product Type Fields

```kotlin
data class Product(
    // Core fields
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

    // Android-specific fields ✨
    val nameAndroid: String? = null,              // Product display name
    val typeAndroid: String? = null,              // "inapp" or "subs"
    val displayPriceAndroid: String? = null,      // Formatted price
    val oneTimePurchaseOfferDetails: OneTimePurchaseOfferDetails? = null,
    val subscriptionOfferDetails: List<OfferDetail>? = null,

    val platform: IapPlatform
)
```

### Purchase Type Fields

```kotlin
data class Purchase(
    // Core fields
    val id: String,
    val productId: String,
    val transactionDate: Double,
    val transactionReceipt: String,
    val purchaseToken: String? = null,

    // iOS-specific fields
    val quantityIOS: Int? = null,
    val originalTransactionDateIOS: Double? = null,
    val originalTransactionIdIOS: String? = null,
    // ... other iOS fields

    // Android-specific fields ✨
    val purchaseStateAndroid: Int? = null,
    val signatureAndroid: String? = null,
    val autoRenewingAndroid: Boolean? = null,
    val acknowledgedAndroid: Boolean? = null,
    val dataAndroid: String? = null,                    // ✨ NEW
    val obfuscatedAccountIdAndroid: String? = null,     // ✨ NEW
    val obfuscatedProfileIdAndroid: String? = null,     // ✨ NEW

    val platform: IapPlatform
)
```

### Error Type Fields

```kotlin
class PurchaseError(
    val code: String,
    override val message: String,
    val productId: String? = null,
    val responseCode: Int? = null,
    val debugMessage: String? = null,
    val platform: IapPlatform? = null,
    val subResponseCode: Int? = null,        // ✨ NEW: Android v8.0.0+
    val subResponseMessage: String? = null   // ✨ NEW: Detailed error message
) : Exception(message)
```

## 🚀 Get Started

Update to KMP-IAP v1.0.0-beta.13 today:

```kotlin
implementation("io.github.hyochan:kmp-iap:1.0.0-beta.13")
```

### Documentation Links

- **[OpenIAP Specification](https://openiap.dev)** - Learn about the standard
- **[Complete API Documentation](https://kmp-iap.hyo.dev)** - Complete API reference
- **[Migration Guide](../MIGRATION.md)** - Detailed migration instructions
- **[Naming Conventions](../CLAUDE.md)** - Our coding standards

## 🎉 What's Next

### OpenIAP Ecosystem Integration

- Cross-library compatibility testing
- Shared validation utilities
- Common testing frameworks

### Enhanced Standards Compliance

- Receipt validation standardization
- Promotional offers specification
- Subscription management patterns

### Platform Extensions

- **StoreKit 2 Implementation**: Complete iOS StoreKit 2 support with all enhanced fields
- **Advanced Subscription Management**: Enhanced subscription lifecycle APIs
- **Promotional Offers**: Comprehensive promotional offer handling for both platforms
- **Web IAP Integration**: Support for web platforms
- **Desktop Platform Support**: Native desktop IAP integration

---

Have questions or feedback? Join the discussion on [GitHub](https://github.com/hyochan/kmp-iap) or contribute to the project!

For new feature proposals, discuss at [OpenIAP Discussions](https://github.com/hyochan/openiap.dev/discussions) to ensure alignment with standards.

_This update represents months of work to provide the most comprehensive and standards-compliant cross-platform IAP solution available. Thank you to all contributors and users who made this possible!_
