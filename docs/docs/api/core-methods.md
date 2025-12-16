---
title: Core Methods
sidebar_position: 3
---

import GreatFrontEndBanner from '@site/src/uis/GreatFrontEndBanner';

# Core Methods

<GreatFrontEndBanner />

Essential methods for implementing in-app purchases with kmp-iap, now **100% OpenIAP specification compliant**. All methods support both iOS and Android platforms with unified APIs using Kotlin coroutines.

## Version Information

:::info Version Differences
This documentation covers both:
- **v1.0.0-rc** (Current) - Simplified API without wrapper classes
- **v1.0.0-beta** - Previous API with wrapper classes
:::

⚠️ **Platform Differences**: While the API is unified, there are important differences between iOS and Android implementations. Each method documents platform-specific behavior.

## Connection Management

### initConnection()

Initializes the connection to the platform store.

```kotlin
suspend fun initConnection(): Boolean
```

**Returns**: `true` if connection successful, `false` otherwise

**Description**: Establishes connection with the App Store (iOS) or Google Play Store (Android). Must be called before any other IAP operations.

**Platform Differences**:
- **iOS**: Connects to StoreKit 2 (iOS 15+)
- **Android**: Connects to Google Play Billing Client v8+

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

val connected = kmpIapInstance.initConnection()
if (connected) {
    println("IAP connection initialized successfully")
} else {
    println("Failed to initialize IAP connection")
}
```

**Throws**: `PurchaseError` if connection fails

**See Also**: [endConnection()](#endconnection), [Connection Lifecycle](../guides/lifecycle.md)

---

### endConnection()

Ends the connection to the platform store and cleans up resources.

```kotlin
suspend fun endConnection()
```

**Returns**: Unit (void)

**Description**: Cleanly closes the store connection and frees resources. Should be called when IAP functionality is no longer needed.

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

// In your cleanup code
override fun onCleared() {
    scope.launch {
        kmpIapInstance.endConnection()
    }
}
```

## Product Loading

### requestProducts()

Loads product information from the store.

#### v1.0.0-rc (Current) - DSL API

```kotlin
suspend fun requestProducts(
    builder: ProductsRequestBuilder.() -> Unit
): List<Product>
```

**Parameters**:
- `builder` - DSL builder for configuring the request

**Returns**: List of products with pricing and metadata

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.ProductQueryType

// Load in-app products using DSL
val products = kmpIapInstance.fetchProducts {
    skus = listOf("coins_100", "coins_500", "remove_ads")
    type = ProductQueryType.InApp
}

// Load subscriptions using DSL
val subscriptions = kmpIapInstance.fetchProducts {
    skus = listOf("premium_monthly", "premium_yearly")
    type = ProductQueryType.Subscription
}
```

#### v1.0.0-beta

```kotlin
suspend fun requestProducts(params: ProductRequest): List<Product>
```

**Example**:
```kotlin
val products = kmpIapInstance.fetchProducts {
    skus = listOf("coins_100", "coins_500")
    type = ProductQueryType.InApp
}
```

**Platform Differences**:
- **iOS**: Uses StoreKit 2 API for product requests
- **Android**: Uses `queryProductDetailsAsync()` with automatic product type detection

## Purchase Processing

### requestPurchase()

Initiates a purchase using OpenIAP-compliant request structure.

#### v1.0.0-rc (Current) - DSL API

```kotlin
suspend fun requestPurchase(
    builder: PurchaseRequestBuilder.() -> Unit
): Purchase
```

**Parameters**:
- `builder` - DSL builder for configuring platform-specific purchase options

**Returns**: Purchase object implementing `PurchaseCommon` interface following OpenIAP specification

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.*

// Cross-platform purchase using DSL
kmpIapInstance.requestPurchase {
    ios {
        sku = "premium_upgrade"
        quantity = 1
    }
    android {
        skus = listOf("premium_upgrade")
    }
}

// iOS-only purchase
kmpIapInstance.requestPurchase {
    ios {
        sku = "coins_100"
        quantity = 5
        appAccountToken = "token_456"
    }
}

// Android-only purchase
kmpIapInstance.requestPurchase {
    android {
        skus = listOf("coins_100")
        obfuscatedAccountIdAndroid = "user_123"
        obfuscatedProfileIdAndroid = "profile_456"
    }
}
```

#### v1.0.0-beta

```kotlin
suspend fun requestPurchase(request: RequestPurchaseProps): Purchase
```

**Example**:
```kotlin
kmpIapInstance.requestPurchase {
    ios {
        sku = "premium_upgrade"
        quantity = 1
    }
    android {
        skus = listOf("premium_upgrade")
    }
}
```

**Platform Differences**:
- **iOS**: Supports App Account Token for fraud prevention
- **Android**: Supports obfuscated user IDs and automatic Activity detection

## Transaction Management

### finishTransaction()

Completes a transaction after successful purchase processing.

```kotlin
suspend fun finishTransaction(
    purchase: Purchase,
    isConsumable: Boolean? = null
): Boolean
```

**Parameters**:
- `purchase` - The purchase to finish
- `isConsumable` - Whether the product is consumable (null for auto-detection)

**Returns**: `true` if transaction was successfully finished, `false` otherwise

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance


// For consumable products
val success = kmpIapInstance.finishTransaction(
    purchase = purchase.toPurchaseInput(),
    isConsumable = true
)
if (success) {
    println("Transaction finished successfully")
}

// For subscriptions (acknowledge only, don't consume)
val acknowledged = kmpIapInstance.finishTransaction(
    purchase = purchase.toPurchaseInput(),
    isConsumable = false
)
if (acknowledged) {
    println("Subscription acknowledged")
}
```

**Platform Behavior**:
- **iOS**: Calls `finish()` on the transaction
- **Android**: 
  - Consumables: Calls `consumeAsync`
  - Non-consumables/Subscriptions: Calls `acknowledgePurchase`

⚠️ **Important**: Subscriptions should NEVER be consumed. They should only be acknowledged once.

## Purchase History

### getAvailablePurchases()

Gets all available (unconsumed/active) purchases.

```kotlin
suspend fun getAvailablePurchases(options: PurchaseOptions? = null): List<Purchase>
```

**Returns**: List of available purchases

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

val purchases = kmpIapInstance.getAvailablePurchases()
purchases.forEach { purchase ->
    println("Product: \${purchase.productId}")
    println("Date: \${purchase.transactionDate}")
    
    // Check acknowledgment status (Android)
    if (purchase.acknowledgedAndroid == true) {
        println("Already acknowledged")
    }
}
```

**Platform Differences**:
- **iOS**: Returns active subscriptions and non-consumed purchases
- **Android**: Queries both INAPP and SUBS purchases separately

---

### getPurchaseHistories()

Gets purchase history including consumed items.

```kotlin
suspend fun getPurchaseHistories(options: PurchaseOptions? = null): List<ProductPurchase>
```

**Returns**: List of historical purchases

**Note**: Android Billing Client v6+ no longer provides purchase history. This method returns an empty list on Android.

## Event Flows

### Purchase Event Flows

All purchase events are exposed via Kotlin Flow for reactive programming:

```kotlin
// Purchase updates
val purchaseUpdatedListener: Flow<Purchase>

// Purchase errors
val purchaseErrorListener: Flow<PurchaseError>

// Promoted products (iOS only)
val promotedProductListener: Flow<String?>
```

**Example Usage**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import kotlinx.coroutines.flow.collectLatest

// Listen for purchase updates
scope.launch {
    kmpIapInstance.purchaseUpdatedListener.collectLatest { purchase ->
        println("Purchase completed: \${purchase.productId}")
        // Deliver content to user
        deliverContent(purchase.productId)
        // Finish transaction
        kmpIapInstance.finishTransaction(purchase, isConsumable = true)
    }
}

// Listen for errors
scope.launch {
    kmpIapInstance.purchaseErrorListener.collectLatest { error ->
        when (error.code) {
            ErrorCode.E_USER_CANCELLED.name -> {
                println("User cancelled purchase")
            }
            else -> {
                println("Purchase error: \${error.message}")
            }
        }
    }
}
```

## Platform-Specific Methods

### iOS-Specific Methods

#### clearTransactionIOS()

Clears pending transactions (iOS only).

```kotlin
suspend fun clearTransactionIOS()
```

#### clearProductsIOS()

Clears cached products (iOS only).

```kotlin
suspend fun clearProductsIOS()
```

#### getPromotedProductIOS()

Gets the currently promoted product (iOS only).

```kotlin
suspend fun getPromotedProductIOS(): String?
```

#### presentCodeRedemptionSheetIOS()

Presents the App Store code redemption sheet.

```kotlin
suspend fun presentCodeRedemptionSheetIOS()
```

#### getStorefrontIOS()

Gets the App Store storefront information.

```kotlin
suspend fun getStorefrontIOS(): String
```

### iOS-Specific Alternative Billing

#### canPresentExternalPurchaseNoticeIOS()

Check if the device can present an external purchase notice sheet. Requires iOS 18.2+.

```kotlin
suspend fun canPresentExternalPurchaseNoticeIOS(): Boolean
```

**Returns**: `true` if the device supports external purchase notice sheets

**Platform**: iOS 18.2+

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

val canPresent = kmpIapInstance.canPresentExternalPurchaseNoticeIOS()
if (canPresent) {
    println("External purchase notice sheet is available")
}
```

**Note**: This notice sheet must be presented before redirecting users to external purchase links on iOS 18.2+.

---

#### presentExternalPurchaseNoticeSheetIOS()

Present an external purchase notice sheet to inform users about external purchases. This must be called before opening an external purchase link on iOS 18.2+.

```kotlin
suspend fun presentExternalPurchaseNoticeSheetIOS(): ExternalPurchaseNoticeResultIOS
```

**Returns**: `ExternalPurchaseNoticeResultIOS` with result status

```kotlin
data class ExternalPurchaseNoticeResultIOS(
    val error: String?,
    val result: String? // "continue" or "dismissed"
)
```

**Platform**: iOS 18.2+

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

val result = kmpIapInstance.presentExternalPurchaseNoticeSheetIOS()

if (result.error != null) {
    println("Failed to present notice: ${result.error}")
} else if (result.result == "continue") {
    // User chose to continue to external purchase
    println("User accepted external purchase notice")
} else if (result.result == "dismissed") {
    // User dismissed the sheet
    println("User dismissed notice")
}
```

**See also**: [StoreKit External Purchase documentation](https://developer.apple.com/documentation/storekit/external-purchase)

---

#### presentExternalPurchaseLinkIOS()

Open an external purchase link in Safari to redirect users to your website for purchase. Requires iOS 16.0+.

```kotlin
suspend fun presentExternalPurchaseLinkIOS(url: String): ExternalPurchaseLinkResultIOS
```

**Parameters**:
- `url` - The external purchase URL to open

**Returns**: `ExternalPurchaseLinkResultIOS` with success status

```kotlin
data class ExternalPurchaseLinkResultIOS(
    val error: String?,
    val success: Boolean
)
```

**Platform**: iOS 16.0+

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

val result = kmpIapInstance.presentExternalPurchaseLinkIOS(
    url = "https://your-site.com/checkout"
)

if (result.error != null) {
    println("Failed to open link: ${result.error}")
} else if (result.success) {
    println("User redirected to external purchase website")
}
```

**Requirements**:
- Must configure entitlements in your iOS project
- Requires Apple approval and proper provisioning profile with external purchase entitlements
- URLs must be configured in Info.plist
- iOS 16.0 or later

**Configuration Example** (Info.plist):
```xml
<plist>
<dict>
    <!-- Countries where external purchases are supported -->
    <key>SKExternalPurchase</key>
    <array>
        <string>kr</string>
        <string>nl</string>
    </array>
</dict>
</plist>
```

**See also**:
- [StoreKit External Purchase documentation](https://developer.apple.com/documentation/storekit/external-purchase)
- [Alternative Billing Guide](../guides/alternative-billing.md)

---

### Android-Specific Methods

#### acknowledgePurchaseAndroid()

Acknowledges a purchase (Android only).

```kotlin
suspend fun acknowledgePurchaseAndroid(purchaseToken: String)
```

**Important**: Subscriptions must be acknowledged within 3 days or they will be refunded.

#### consumePurchaseAndroid()

Consumes a purchase (Android only).

```kotlin
suspend fun consumePurchaseAndroid(purchaseToken: String)
```

**Warning**: Only use for consumable products. Never consume subscriptions.

#### deepLinkToSubscriptions()

Opens the Google Play subscription management page.

```kotlin
suspend fun deepLinkToSubscriptions(options: DeepLinkOptions)
```

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

kmpIapInstance.deepLinkToSubscriptions(
    DeepLinkOptions(skuAndroid = "premium_monthly")
)
```

---

### Android-Specific Alternative Billing

#### checkAlternativeBillingAvailabilityAndroid()

Check if alternative billing is available for the current user. This must be called before showing the alternative billing dialog.

```kotlin
suspend fun checkAlternativeBillingAvailabilityAndroid(): Boolean
```

**Returns**: `true` if alternative billing is available, `false` otherwise

**Platform**: Android

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

val isAvailable = kmpIapInstance.checkAlternativeBillingAvailabilityAndroid()
if (isAvailable) {
    println("Alternative billing is available")
} else {
    println("Alternative billing not available for this user")
}
```

**Requirements**:
- Must initialize connection with alternative billing mode
- User must be eligible for alternative billing (determined by Google)

**See also**: [Google Play Alternative Billing documentation](https://developer.android.com/google/play/billing/alternative)

---

#### showAlternativeBillingDialogAndroid()

Show Google's required information dialog to inform users about alternative billing. This must be called after checking availability and before processing payment.

```kotlin
suspend fun showAlternativeBillingDialogAndroid(): Boolean
```

**Returns**: `true` if user accepted, `false` if user declined

**Platform**: Android

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

val userAccepted = kmpIapInstance.showAlternativeBillingDialogAndroid()
if (userAccepted) {
    println("User accepted alternative billing")
    // Proceed with your payment flow
} else {
    println("User declined alternative billing")
}
```

**Note**: This dialog is required by Google Play's alternative billing policy. You must show this before redirecting users to your payment system.

---

#### createAlternativeBillingTokenAndroid()

Generate a reporting token after successfully processing payment through your payment system. This token must be reported to Google Play within 24 hours.

```kotlin
suspend fun createAlternativeBillingTokenAndroid(): String?
```

**Returns**: Token string if successful, `null` if failed

**Platform**: Android

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

// After successfully processing payment in your system
val token = kmpIapInstance.createAlternativeBillingTokenAndroid()

if (token != null) {
    println("Token created: $token")
    // Send this token to your backend to report to Google
    reportTokenToGooglePlay(token)
} else {
    println("Failed to create token")
}
```

**Important**:
- Token must be reported to Google Play backend within 24 hours
- Requires server-side integration with Google Play Developer API
- Failure to report will result in refund and possible account suspension

---

#### Alternative Billing Configuration

Configure alternative billing mode when initializing the connection:

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.AlternativeBillingModeAndroid
import io.github.hyochan.kmpiap.openiap.InitConnectionConfig

// Initialize with alternative billing mode
val config = InitConnectionConfig(
    alternativeBillingModeAndroid = AlternativeBillingModeAndroid.UserChoice
    // Or: AlternativeBillingModeAndroid.AlternativeOnly
)

val connected = kmpIapInstance.initConnection(config)
```

**Billing Modes**:
- `UserChoice` - Users choose between Google Play billing or your payment system
- `AlternativeOnly` - Only your payment system is available
- `None` - Default, no alternative billing

---

#### Complete Alternative Billing Flow Example

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

suspend fun purchaseWithAlternativeBilling(productId: String) {
    // Step 1: Check availability
    val isAvailable = kmpIapInstance.checkAlternativeBillingAvailabilityAndroid()
    if (!isAvailable) {
        throw Exception("Alternative billing not available")
    }

    // Step 2: Show required dialog
    val userAccepted = kmpIapInstance.showAlternativeBillingDialogAndroid()
    if (!userAccepted) {
        throw Exception("User declined alternative billing")
    }

    // Step 3: Process payment in your system
    val paymentResult = processPaymentInYourSystem(productId)
    if (!paymentResult.success) {
        throw Exception("Payment failed")
    }

    // Step 4: Create reporting token
    val token = kmpIapInstance.createAlternativeBillingTokenAndroid()
    if (token == null) {
        throw Exception("Failed to create token")
    }

    // Step 5: Report to Google (must be done within 24 hours)
    reportToGooglePlayBackend(token, productId, paymentResult)

    println("Alternative billing purchase completed")
}
```

**See also**: [Alternative Billing Guide](../guides/alternative-billing.md)

---

### Billing Programs API (v1.2.0)

New in v1.2.0, the Billing Programs API provides a unified way to handle external billing programs (Google Play Billing 8.2.0+).

:::info Availability Note
The Billing Programs API methods currently return `FeatureNotSupported` error as the underlying Google Play Billing Library 8.2.0 APIs are not yet available in the billing-ktx dependency. This documentation is provided for future compatibility.
:::

#### isBillingProgramAvailable()

Check if a specific billing program is available for the current user.

```kotlin
suspend fun isBillingProgramAvailable(program: BillingProgram): BillingProgramAvailabilityResult
```

**Parameters**:
- `program` - The billing program to check (`ExternalOffer` or `ExternalContentLink`)

**Returns**: `BillingProgramAvailabilityResult` with availability status

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.BillingProgram

try {
    val result = kmpIapInstance.isBillingProgramAvailable(BillingProgram.ExternalOffer)
    if (result.isAvailable) {
        println("External offer program is available")
    } else {
        println("External offer program not available for this user")
    }
} catch (e: PurchaseException) {
    println("Error: ${e.error.message}")
}
```

**Platform**: Android 8.2.0+

---

#### createBillingProgramReportingDetails()

Create reporting details for external transactions. Call this after completing an external purchase.

```kotlin
suspend fun createBillingProgramReportingDetails(program: BillingProgram): BillingProgramReportingDetails
```

**Parameters**:
- `program` - The billing program type

**Returns**: `BillingProgramReportingDetails` with external transaction token

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.BillingProgram

try {
    val details = kmpIapInstance.createBillingProgramReportingDetails(BillingProgram.ExternalOffer)
    val token = details.externalTransactionToken

    // Report this token to Google Play backend within 24 hours
    reportToGoogleBackend(token)
} catch (e: PurchaseException) {
    println("Error: ${e.error.message}")
}
```

**Platform**: Android 8.2.0+

---

#### launchExternalLink()

Launch an external link for the specified billing program.

```kotlin
suspend fun launchExternalLink(params: LaunchExternalLinkParams)
```

**Parameters**:
- `params` - `LaunchExternalLinkParams` with billing program, launch mode, link type, and URI

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.*

try {
    kmpIapInstance.launchExternalLink(
        LaunchExternalLinkParams(
            billingProgram = BillingProgram.ExternalOffer,
            launchMode = ExternalLinkLaunchMode.LaunchInExternalBrowserOrApp,
            linkType = ExternalLinkType.LinkToDigitalContentOffer,
            linkUri = "https://your-payment-site.com/offer"
        )
    )
    println("External link launched")
} catch (e: PurchaseException) {
    println("Error: ${e.error.message}")
}
```

**Platform**: Android 8.2.0+

---

#### Complete Billing Programs Flow Example

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.*

suspend fun purchaseWithBillingPrograms(productId: String) {
    // Step 1: Check availability
    val availabilityResult = kmpIapInstance.isBillingProgramAvailable(BillingProgram.ExternalOffer)
    if (!availabilityResult.isAvailable) {
        throw Exception("External offer program not available")
    }

    // Step 2: Launch external link
    kmpIapInstance.launchExternalLink(
        LaunchExternalLinkParams(
            billingProgram = BillingProgram.ExternalOffer,
            launchMode = ExternalLinkLaunchMode.LaunchInExternalBrowserOrApp,
            linkType = ExternalLinkType.LinkToDigitalContentOffer,
            linkUri = "https://your-payment-site.com/checkout?product=$productId"
        )
    )

    // Step 3: After user completes external purchase, get reporting token
    val reportingDetails = kmpIapInstance.createBillingProgramReportingDetails(BillingProgram.ExternalOffer)

    // Step 4: Report to Google Play backend within 24 hours
    reportToGoogleBackend(reportingDetails.externalTransactionToken, productId)

    println("Billing Programs purchase completed")
}
```

**See also**:
- [Alternative Billing Guide](../guides/alternative-billing.md)
- [Billing Programs API Types](./types.md#billing-programs-api-types-v110)

## Subscription Management

### getActiveSubscriptions()

Gets all active subscriptions with detailed information.

```kotlin
suspend fun getActiveSubscriptions(subscriptionIds: List<String>? = null): List<ActiveSubscription>
```

**Parameters**:
- `subscriptionIds` - Optional list of subscription IDs to check. If null, returns all active subscriptions

**Returns**: List of active subscriptions with platform-specific details

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

// Get all active subscriptions
val allActiveSubscriptions = kmpIapInstance.getActiveSubscriptions()

// Get specific subscriptions
val premiumSubscriptions = kmpIapInstance.getActiveSubscriptions(
    listOf("premium_monthly", "premium_yearly")
)

premiumSubscriptions.forEach { subscription ->
    println("Product: \${subscription.productId}")
    println("Active: \${subscription.isActive}")
    
    // iOS-specific information
    subscription.expirationDateIOS?.let { expDate ->
        println("Expires: \${Instant.fromEpochMilliseconds(expDate)}")
    }
    subscription.environmentIOS?.let { env ->
        println("Environment: $env") // "Sandbox" or "Production"
    }
    subscription.daysUntilExpirationIOS?.let { days ->
        println("Days until expiration: $days")
    }
    
    // Android-specific information
    subscription.autoRenewingAndroid?.let { autoRenew ->
        println("Auto-renewing: $autoRenew")
    }
    
    // Cross-platform
    if (subscription.willExpireSoon == true) {
        println("⚠️ Subscription expires soon!")
    }
}
```

**Platform Differences**:
- **iOS**: Provides `expirationDateIOS`, `environmentIOS`, `daysUntilExpirationIOS`
- **Android**: Provides `autoRenewingAndroid` status
- **Cross-platform**: `willExpireSoon` (true if expiring within 7 days)

---

### hasActiveSubscriptions()

Checks if the user has any active subscriptions.

```kotlin
suspend fun hasActiveSubscriptions(subscriptionIds: List<String>? = null): Boolean
```

**Parameters**:
- `subscriptionIds` - Optional list of subscription IDs to check. If null, checks all subscriptions

**Returns**: `true` if the user has at least one active subscription, `false` otherwise

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

// Check if user has any active subscriptions
val hasAnySubscription = kmpIapInstance.hasActiveSubscriptions()
if (hasAnySubscription) {
    println("User has active subscriptions")
    showPremiumFeatures()
} else {
    showSubscriptionOffer()
}

// Check specific subscription types
val hasPremium = kmpIapInstance.hasActiveSubscriptions(
    listOf("premium_monthly", "premium_yearly")
)
if (hasPremium) {
    enablePremiumFeatures()
}
```

**Use Cases**:
- Feature gating based on subscription status
- Showing/hiding subscription offers
- Quick subscription status checks without detailed information

## Purchase Validation

### verifyPurchase()

Verifies a purchase. For server-side verification, use [`verifyPurchaseWithProvider()`](#verifypurchasewithprovider).

```kotlin
suspend fun verifyPurchase(options: VerifyPurchaseProps): VerifyPurchaseResult
```

**Parameters**:

```kotlin
data class VerifyPurchaseProps(
    val apple: VerifyPurchaseAppleOptions? = null,
    val google: VerifyPurchaseGoogleOptions? = null,
    val horizon: VerifyPurchaseHorizonOptions? = null
)

data class VerifyPurchaseAppleOptions(
    val sku: String
)

data class VerifyPurchaseGoogleOptions(
    val sku: String,
    val accessToken: String,      // Obtain from your backend, NOT stored in app
    val packageName: String,
    val purchaseToken: String,
    val isSub: Boolean? = null
)

data class VerifyPurchaseHorizonOptions(
    val sku: String,
    val userId: String,
    val accessToken: String       // Obtain from your backend
)
```

**Returns**: `VerifyPurchaseResult` with verification status

**Platform Notes**:
- **iOS**: Uses StoreKit's native verification. Only `apple` field is used.
- **Android**: Requires `google` field with Google Play API credentials.
- **Horizon (Meta Quest)**: Requires `horizon` field with user credentials.

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.*

// iOS verification
val iosResult = kmpIapInstance.verifyPurchase(
    VerifyPurchaseProps(
        apple = VerifyPurchaseAppleOptions(sku = "premium_upgrade")
    )
)

// Android verification
val androidResult = kmpIapInstance.verifyPurchase(
    VerifyPurchaseProps(
        google = VerifyPurchaseGoogleOptions(
            sku = "premium_upgrade",
            accessToken = backendProvidedToken, // Get from your secure backend
            packageName = "com.yourapp.id",
            purchaseToken = purchase.purchaseToken ?: "",
            isSub = false
        )
    )
)
```

:::warning Security Note
The `accessToken` for Google and Horizon verification must be obtained from your secure backend. Never hardcode or store API credentials in your app.
:::

:::tip Recommendation
For production apps, use [`verifyPurchaseWithProvider()`](#verifypurchasewithprovider) with [IAPKit](https://iapkit.com) for secure server-side verification without managing your own backend.
:::

---

### verifyPurchaseWithProvider()

Verifies purchases using external verification services like IAPKit. This provides server-side validation without maintaining your own validation infrastructure.

```kotlin
suspend fun verifyPurchaseWithProvider(
    options: VerifyPurchaseWithProviderProps
): VerifyPurchaseWithProviderResult
```

**Parameters**:
- `options` - Verification configuration including provider and credentials

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

**Returns**: `VerifyPurchaseWithProviderResult` with verification results

```kotlin
data class VerifyPurchaseWithProviderResult(
    val provider: PurchaseVerificationProvider,
    val iapkit: RequestVerifyPurchaseWithIapkitResult? = null
)

data class RequestVerifyPurchaseWithIapkitResult(
    val isValid: Boolean,
    val state: IapkitPurchaseState,
    val store: IapStore
)
```

For detailed information about `IapkitPurchaseState` values (Entitled, Pending, Expired, Canceled, etc.), see the [IAPKit Purchase States documentation](https://www.openiap.dev/docs/apis#iapkit-purchase-states).

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.*

// Verify iOS purchase with IAPKit
val result = kmpIapInstance.verifyPurchaseWithProvider(
    VerifyPurchaseWithProviderProps(
        provider = PurchaseVerificationProvider.Iapkit,
        iapkit = RequestVerifyPurchaseWithIapkitProps(
            apiKey = "your-iapkit-api-key",
            apple = RequestVerifyPurchaseWithIapkitAppleProps(
                jws = purchase.jwsRepresentationIOS ?: ""
            ),
            google = null
        )
    )
)

// Check verification results
result.iapkit?.let { iapkit ->
    println("Is Valid: ${iapkit.isValid}")
    println("State: ${iapkit.state}") // Entitled, Expired, Canceled, etc.
    println("Store: ${iapkit.store}") // Apple or Google
}

// Verify Android purchase
val androidResult = kmpIapInstance.verifyPurchaseWithProvider(
    VerifyPurchaseWithProviderProps(
        provider = PurchaseVerificationProvider.Iapkit,
        iapkit = RequestVerifyPurchaseWithIapkitProps(
            apiKey = "your-iapkit-api-key",
            apple = null,
            google = RequestVerifyPurchaseWithIapkitGoogleProps(
                purchaseToken = purchase.purchaseToken ?: ""
            )
        )
    )
)
```

**Platform Behavior**:
- **iOS**: Sends the JWS token to IAPKit for server-side verification
- **Android**: Sends the purchase token for verification

**Use Cases**:
- Server-side receipt validation without maintaining your own infrastructure
- Cross-platform purchase verification with a unified API
- Enhanced security through external verification services

:::tip Environment Variables
Store your IAPKit API key securely using environment variables:
```bash
# .env file
IAPKIT_API_KEY=your_iapkit_api_key_here
```

Never hardcode API keys in your source code.
:::

**Note**: You need an IAPKit API key to use this feature. Visit [iapkit.com](https://iapkit.com) to get started.

:::warning Error Handling Best Practice
**Verification error ≠ Invalid purchase**. When verification fails due to network issues or server errors, don't penalize the customer. Use a "fail-open" approach.

See the [Verification Error Handling guide](https://www.openiap.dev/docs/apis#verification-error-handling) for detailed implementation patterns and best practices.
:::

---

### validateReceipt() (Deprecated)

:::warning Deprecated
Use [`verifyPurchase()`](#verifypurchase) or [`verifyPurchaseWithProvider()`](#verifypurchasewithprovider) instead.
:::

### isPurchaseValid() (Deprecated)

:::warning Deprecated
Use [`verifyPurchase()`](#verifypurchase) or [`verifyPurchaseWithProvider()`](#verifypurchasewithprovider) instead.
:::

**Example**:
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

val isValid = kmpIapInstance.isPurchaseValid(purchase)
if (isValid) {
    // Proceed with server validation
    validateOnServer(purchase)
}
```

## Utility Methods

### getStore()

Gets the current store type.

```kotlin
fun getStore(): Store
```

**Returns**: Store enum (APP_STORE, PLAY_STORE, AMAZON, or NONE)

### canMakePayments()

Checks if the device can make payments.

```kotlin
suspend fun canMakePayments(): Boolean
```

**Returns**: `true` if payments are available

### getVersion()

Gets the library version.

```kotlin
fun getVersion(): String
```

**Returns**: Version string (e.g., "KMP-IAP v1.0.0 (Android)")

## Best Practices

1. **Always handle errors**: Use try-catch blocks or collect error flows
2. **Finish transactions**: Always call `finishTransaction()` after delivering content
3. **Check acknowledgment**: For Android, check if subscriptions are already acknowledged before re-acknowledging
4. **Monitor connection**: Check connection status before operations
5. **Use proper product types**: Specify ProductType.INAPP or ProductType.SUBS correctly
6. **Never consume subscriptions**: Only acknowledge subscriptions, never consume them
7. **Validate on server**: Always perform receipt validation on your server

## Error Handling

All methods can throw `PurchaseError` with specific error codes:

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

try {
    val purchase = kmpIapInstance.requestPurchase(request)
} catch (e: PurchaseError) {
    when (e.code) {
        ErrorCode.E_USER_CANCELLED.name -> handleCancellation()
        ErrorCode.E_ITEM_UNAVAILABLE.name -> handleUnavailable()
        ErrorCode.E_NETWORK_ERROR.name -> handleNetworkError()
        else -> handleGenericError(e)
    }
}
```

## See Also

- [Types](./types.md) - Type definitions used in methods
- [Error Codes](./error-codes.md) - Complete error code reference
- [Events and Listeners](./listeners.md) - Using Flow for events
- [Examples](../examples/purchase-flow.md) - Complete implementation examples