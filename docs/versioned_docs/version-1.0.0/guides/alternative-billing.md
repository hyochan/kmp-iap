---
sidebar_position: 7
title: Alternative Billing
---

import GreatFrontEndBanner from '@site/src/uis/GreatFrontEndBanner';

# Alternative Billing

<GreatFrontEndBanner />

This guide explains how to implement alternative billing functionality in your app using kmp-iap, allowing you to use external payment systems alongside or instead of the App Store/Google Play billing.

## Official Documentation

### Apple (iOS)

- [StoreKit External Purchase Documentation](https://developer.apple.com/documentation/storekit/external-purchase) - Official StoreKit external purchase API reference
- [External Purchase Link Entitlement](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.storekit.external-purchase-link) - Entitlement configuration
- [ExternalPurchaseCustomLink API](https://developer.apple.com/documentation/storekit/externalpurchasecustomlink) - Custom link API documentation
- [OpenIAP External Purchase](https://www.openiap.dev/docs/external-purchase) - OpenIAP external purchase specification

### Google Play (Android)

- [Alternative Billing APIs](https://developer.android.com/google/play/billing/alternative) - Official Android alternative billing API guide
- [User Choice Billing Overview](https://support.google.com/googleplay/android-developer/answer/13821247) - Understanding user choice billing
- [User Choice Billing Pilot](https://support.google.com/googleplay/android-developer/answer/12570971) - Enrollment and setup
- [Payments Policy](https://support.google.com/googleplay/android-developer/answer/10281818) - Google Play's payment policy
- [UX Guidelines (User Choice)](https://developer.android.com/google/play/billing/alternative/interim-ux/user-choice) - User choice billing UX guidelines
- [UX Guidelines (Alternative Billing)](https://developer.android.com/google/play/billing/alternative/interim-ux/billing-choice) - Alternative billing UX guidelines
- [EEA Alternative Billing](https://support.google.com/googleplay/android-developer/answer/12348241) - European Economic Area specific guidance

### Platform Updates (2024)

#### iOS

- US apps can use StoreKit External Purchase Link Entitlement
- System disclosure sheet shown each time external link is accessed
- Commission: 27% (reduced from 30%) for first year, 12% for subsequent years
- EU apps have additional flexibility for external purchases

#### Android

- As of March 13, 2024: Alternative billing APIs must be used (manual reporting deprecated)
- Service fee reduced by 4% when using alternative billing (e.g., 15% → 11%)
- Available in South Korea, India, and EEA
- Gaming and non-gaming apps eligible (varies by region)

## Overview

Alternative billing enables developers to offer payment options outside of the platform's standard billing systems:

- **iOS**: Redirect users to external websites for payment (iOS 16.0+)
- **Android**: Use Google Play's alternative billing options (requires approval)

:::warning Platform Approval Required

Both platforms require special approval to use alternative billing:

- **iOS**: Must be approved for external purchase entitlement
- **Android**: Must be approved for alternative billing in Google Play Console

:::

## iOS Alternative Billing (External Purchase)

On iOS, alternative billing works by redirecting users to an external website where they complete the purchase.

### Configuration

Configure iOS external purchase entitlements in your iOS project:

**Entitlements (iosApp.entitlements):**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- Required: External purchase entitlement -->
    <key>com.apple.developer.storekit.external-purchase</key>
    <true/>

    <!-- Optional: External purchase link entitlement -->
    <key>com.apple.developer.storekit.external-purchase-link</key>
    <true/>
</dict>
</plist>
```

**Info.plist:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- Countries where external purchases are supported (ISO 3166-1 alpha-2 uppercase) -->
    <key>SKExternalPurchase</key>
    <dict>
        <key>AllowedCountries</key>
        <array>
            <string>KR</string>
            <string>NL</string>
            <string>DE</string>
            <string>FR</string>
        </array>
    </dict>
</dict>
</plist>
```

:::warning Requirements

- **Approval Required**: You must obtain approval from Apple to use external purchase features
- **URL Format**: URLs must use HTTPS and be valid
- **Supported Regions**: Different features support different regions (EU, US, etc.)

See [External Purchase Link Entitlement](https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.storekit.external-purchase-link) for details.

:::

### Basic Usage

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.presentExternalPurchaseLinkIOS

// Present external purchase link
val result = kmpIapInstance.presentExternalPurchaseLinkIOS(
    url = "https://your-site.com/checkout"
)

if (result.success) {
    println("User was redirected to external URL")
} else {
    println("Error: ${result.error}")
}
```

### Important Notes

- **iOS 16.0+ Required**: External purchase links only work on iOS 16.0 and later
- **No Purchase Callback**: The `purchaseUpdatedListener` will NOT fire when using external URLs
- **Deep Link Required**: Implement deep linking to return users to your app after purchase
- **Manual Validation**: You must validate purchases on your backend server

### Complete iOS Example

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.presentExternalPurchaseLinkIOS
import kotlinx.coroutines.launch

fun handleExternalPurchase(productId: String) {
    scope.launch {
        try {
            val result = kmpIapInstance.presentExternalPurchaseLinkIOS(
                url = "https://your-site.com/checkout?product=$productId"
            )

            if (result.success) {
                // User was redirected to external site
                // Implement deep linking to handle return to app
                println("Redirected to external checkout")
            } else {
                println("Error: ${result.error}")
            }
        } catch (e: Exception) {
            println("Failed to present external link: ${e.message}")
        }
    }
}
```

## Android Alternative Billing

Android supports two alternative billing modes:

1. **Alternative Billing Only**: Users can ONLY use your payment system
2. **User Choice Billing**: Users choose between Google Play or your payment system

### Configuration

Set the billing mode when initializing the connection:

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.AlternativeBillingModeAndroid
import io.github.hyochan.kmpiap.openiap.InitConnectionConfig

// Initialize with alternative billing mode
val config = InitConnectionConfig(
    alternativeBillingModeAndroid = AlternativeBillingModeAndroid.AlternativeOnly
    // Or: AlternativeBillingModeAndroid.UserChoice
    // Or: AlternativeBillingModeAndroid.None (default)
)

val connected = kmpIapInstance.initConnection(config)
```

### Mode 1: Alternative Billing Only

This mode requires a manual 3-step flow:

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

suspend fun handleAlternativeBillingOnly(productId: String) {
    try {
        // Step 1: Check availability
        val isAvailable = kmpIapInstance.checkAlternativeBillingAvailabilityAndroid()
        if (!isAvailable) {
            println("Alternative billing not available")
            return
        }

        // Step 2: Show information dialog
        val userAccepted = kmpIapInstance.showAlternativeBillingDialogAndroid()
        if (!userAccepted) {
            println("User declined alternative billing")
            return
        }

        // Step 2.5: Process payment with your payment system
        // ... your payment processing logic here ...

        // Step 3: Create reporting token (after successful payment)
        val token = kmpIapInstance.createAlternativeBillingTokenAndroid()

        if (token != null) {
            // Step 4: Report token to Google Play backend within 24 hours
            reportToGoogleBackend(token)
            println("Alternative billing completed")
        } else {
            println("Failed to create token")
        }
    } catch (e: Exception) {
        println("Alternative billing error: ${e.message}")
    }
}
```

### Mode 2: User Choice Billing

With user choice, Google automatically shows a selection dialog:

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.requestPurchase
import io.github.hyochan.kmpiap.openiap.AlternativeBillingModeAndroid
import io.github.hyochan.kmpiap.openiap.InitConnectionConfig
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// Initialize with user choice mode
val config = InitConnectionConfig(
    alternativeBillingModeAndroid = AlternativeBillingModeAndroid.UserChoice
)
kmpIapInstance.initConnection(config)

// Listen for user choice events
scope.launch {
    kmpIapInstance.userChoiceBillingListener.collect { details ->
        println("User selected alternative billing")
        println("Products: ${details.products}")
        println("Token: ${details.externalTransactionToken}")

        // Process payment with your system
        // ... your payment processing logic ...

        // Report token to Google (token is provided in details)
        reportToGoogleBackend(details.externalTransactionToken)
    }
}

suspend fun handleUserChoicePurchase(productId: String) {
    try {
        // Request purchase - Google will show selection dialog
        kmpIapInstance.requestPurchase {
            android {
                skus = listOf(productId)
            }
        }

        // If user selects Google Play: purchaseUpdatedListener fires
        // If user selects alternative: userChoiceBillingListener fires
        println("Purchase requested")
    } catch (e: Exception) {
        println("Purchase error: ${e.message}")
    }
}
```

### Listening for User Choice Events

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

// Listen for user choice billing events
scope.launch {
    kmpIapInstance.userChoiceBillingListener.collect { details ->
        println("User chose alternative billing")
        println("Products: ${details.products}")
        println("Token: ${details.externalTransactionToken}")

        // Process payment with your system
        processAlternativePayment(details.products)

        // Report token to Google (token is already provided in details)
        // No need to call createAlternativeBillingTokenAndroid() for UserChoice mode
        reportToGoogleBackend(details.externalTransactionToken)
    }
}
```

## Complete Cross-Platform Example

See the [AlternativeBillingScreen.kt](https://github.com/hyochan/kmp-iap/blob/main/example/composeApp/src/commonMain/kotlin/dev/hyo/martie/screens/AlternativeBillingScreen.kt) in the example app for a complete implementation:

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.*
import kotlinx.coroutines.launch

@Composable
fun AlternativeBillingScreen() {
    var selectedProduct by remember { mutableStateOf<ProductCommon?>(null) }
    var billingMode by remember {
        mutableStateOf(AlternativeBillingModeAndroid.AlternativeOnly)
    }
    var externalUrl by remember { mutableStateOf("https://your-site.com") }

    LaunchedEffect(Unit) {
        // Initialize connection with alternative billing
        val config = if (getPlatformName() == "Android") {
            InitConnectionConfig(alternativeBillingModeAndroid = billingMode)
        } else null

        kmpIapInstance.initConnection(config)
    }

    // Platform-specific purchase handling
    fun handlePurchase(product: ProductCommon) {
        scope.launch {
            if (getPlatformName() == "iOS") {
                // iOS: Use external URL
                val result = kmpIapInstance.presentExternalPurchaseLinkIOS(
                    url = "$externalUrl?product=${product.id}"
                )
                if (result.success) {
                    println("Redirected to external checkout")
                } else {
                    println("Error: ${result.error}")
                }
            } else {
                // Android: Handle based on billing mode
                when (billingMode) {
                    AlternativeBillingModeAndroid.AlternativeOnly -> {
                        handleAndroidAlternativeBillingOnly(product)
                    }
                    AlternativeBillingModeAndroid.UserChoice -> {
                        handleAndroidUserChoice(product)
                    }
                    else -> {
                        println("Alternative billing not configured")
                    }
                }
            }
        }
    }

    // UI implementation...
}

suspend fun handleAndroidAlternativeBillingOnly(product: ProductCommon) {
    val isAvailable = kmpIapInstance.checkAlternativeBillingAvailabilityAndroid()
    if (!isAvailable) {
        println("Alternative billing not available")
        return
    }

    val userAccepted = kmpIapInstance.showAlternativeBillingDialogAndroid()
    if (!userAccepted) return

    // Process payment with your system...

    val token = kmpIapInstance.createAlternativeBillingTokenAndroid()
    if (token != null) {
        reportToGoogleBackend(token)
    }
}

suspend fun handleAndroidUserChoice(product: ProductCommon) {
    try {
        val purchase = kmpIapInstance.requestPurchase {
            android {
                skus = listOf(product.id)
            }
        }
        println("Purchase completed via Google Play")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
```

## Best Practices

### General

1. **Backend Validation**: Always validate purchases on your backend server
2. **Clear Communication**: Inform users they're leaving the app for external payment
3. **Deep Linking**: Implement deep links to return users to your app (iOS)
4. **Error Handling**: Handle all error cases gracefully

### iOS Specific

1. **iOS Version Check**: Verify iOS 16.0+ before enabling alternative billing
2. **URL Validation**: Ensure external URLs are valid and secure (HTTPS)
3. **No Purchase Events**: Don't rely on `purchaseUpdatedListener` when using external URLs
4. **Deep Link Implementation**: Crucial for returning users to your app

### Android Specific

1. **24-Hour Reporting**: Report tokens to Google within 24 hours
2. **Mode Selection**: Choose the appropriate mode for your use case
3. **User Experience**: User Choice mode provides better UX but shares revenue with Google
4. **Backend Integration**: Implement proper token reporting to Google Play

## Testing

### iOS Testing

1. Test on real devices running iOS 16.0+
2. Verify external URL opens correctly in Safari
3. Test deep link return flow
4. Ensure StoreKit is configured for alternative billing

### Android Testing

1. Configure alternative billing in Google Play Console
2. Test both billing modes separately
3. Verify token generation and reporting
4. Test user choice dialog behavior

## Troubleshooting

### iOS Issues

#### "Feature not supported"

- Ensure iOS 16.0 or later
- Verify external purchase entitlement is approved

#### "External URL not opening"

- Check URL format (must be valid HTTPS)
- Verify entitlements are properly configured

#### "User stuck on external site"

- Implement deep linking to return to app
- Test deep link handling

### Android Issues

#### "Alternative billing not available"

- Verify Google Play approval
- Check device and Play Store version
- Ensure billing mode is configured

#### "Token creation failed"

- Verify billing mode configuration
- Ensure user completed info dialog
- Check Google Play Console settings

#### "User choice dialog not showing"

- Verify `AlternativeBillingModeAndroid.UserChoice` is set
- Ensure Google Play configuration is correct
- Check device compatibility

## Platform Requirements

- **iOS**: iOS 16.0+ for external purchase URLs
- **Android**: Google Play Billing Library 8.0+ with alternative billing enabled
- **Approval**: Both platforms require approval for alternative billing features

## API Reference

### iOS APIs

- `presentExternalPurchaseLinkIOS(url: String): ExternalPurchaseLinkResultIOS`
- `canPresentExternalPurchaseNoticeIOS(): Boolean`
- `presentExternalPurchaseNoticeSheetIOS(): ExternalPurchaseNoticeResultIOS`

### Android APIs

- `checkAlternativeBillingAvailabilityAndroid(): Boolean`
- `showAlternativeBillingDialogAndroid(): Boolean`
- `createAlternativeBillingTokenAndroid(): String?`
- `userChoiceBillingListener: Flow<UserChoiceBillingDetails>`

## See Also

- [OpenIAP Alternative Billing Specification](https://www.openiap.dev/docs/apis#alternative-billing)
- [Alternative Billing Example App](https://github.com/hyochan/kmp-iap/blob/main/example/composeApp/src/commonMain/kotlin/dev/hyo/martie/screens/AlternativeBillingScreen.kt)
- [Basic Setup Guide](./basic-setup.md)
- [Purchases Guide](./purchases.md)
