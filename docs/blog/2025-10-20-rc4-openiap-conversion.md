---
slug: 1.0.0-rc.4
title: 1.0.0-rc.4 - OpenIAP Monorepo Conversion & Alternative Billing
authors: [hyochan]
tags: [release, openiap, alternative-billing, monorepo, ios, android, storekit, kotlin-multiplatform]
date: 2025-10-20
---

# 1.0.0-rc.4 Release Notes

KMP-IAP 1.0.0-rc.4 marks a major milestone with the **OpenIAP monorepo conversion** and introduces **Alternative Billing** support for both iOS and Android platforms.

👉 [View the 1.0.0-rc.4 release](https://github.com/hyochan/kmp-iap/releases/tag/1.0.0-rc.4)

<!-- truncate -->

## 🎯 Major Changes

### OpenIAP Monorepo Conversion

KMP-IAP now fully integrates with the **[OpenIAP monorepo](https://github.com/hyodotdev/openiap)**, centralizing all in-app purchase implementations under one standardized specification. This conversion brings:

- **Unified dependency management** through `openiap-versions.json`
- **100% OpenIAP specification compliance** across all platforms
- **Centralized version control** for iOS, Android, and GraphQL types
- **Simplified maintenance** with single source of truth

**Current OpenIAP versions:**
- **openiap-apple**: 1.2.26 (iOS StoreKit wrapper with external purchase support)
- **openiap-google**: 1.3.2 (Android BillingClient wrapper with alternative billing)
- **openiap-gql**: 1.2.2 (GraphQL type definitions)

All native SDKs are now maintained in the OpenIAP monorepo, ensuring consistent APIs and behavior across platforms.

## 🚀 New Features

### iOS Alternative Billing (StoreKit External Purchase)

Three new APIs for managing external purchases on iOS:

- **[`canPresentExternalPurchaseNoticeIOS()`](/docs/api/core-methods#canpresentexternalpurchasenoticeios)** - Check if the notice sheet is available (iOS 18.2+)
- **[`presentExternalPurchaseNoticeSheetIOS()`](/docs/api/core-methods#presentexternalpurchasenoticesheetios)** - Present a notice before redirecting to external purchase (iOS 18.2+)
- **[`presentExternalPurchaseLinkIOS(url)`](/docs/api/core-methods#presentexternalpurchaselinkios)** - Open external purchase link in Safari (iOS 16.0+)

**iOS Configuration Required:**

**Info.plist:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- Countries where external purchases are supported -->
    <key>SKExternalPurchase</key>
    <array>
        <string>kr</string>
        <string>nl</string>
        <string>de</string>
    </array>
</dict>
</plist>
```

**Entitlements (iosApp.entitlements):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.developer.storekit.external-purchase</key>
    <true/>

    <key>com.apple.developer.storekit.external-purchase-link</key>
    <true/>
</dict>
</plist>
```

**Usage Example:**
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

// Redirect user to external purchase website
val result = kmpIapInstance.presentExternalPurchaseLinkIOS(
    url = "https://your-site.com/checkout"
)

if (result.success) {
    println("User redirected to external website")
} else {
    println("Error: ${result.error}")
}
```

### Android Alternative Billing

Three new APIs for Google Play Alternative Billing flow:

- **[`checkAlternativeBillingAvailabilityAndroid()`](/docs/api/core-methods#checkalternativebillingavailabilityandroid)** - Check if alternative billing is available for the user
- **[`showAlternativeBillingDialogAndroid()`](/docs/api/core-methods#showalternativebillingdialogandroid)** - Show Google's required information dialog
- **[`createAlternativeBillingTokenAndroid()`](/docs/api/core-methods#createalternativebillingtokenandroid)** - Generate reporting token after successful payment

**Configuration Support:**

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

**Two Billing Modes:**

- `UserChoice` - Users choose between Google Play billing (30% fee) or your payment system (lower fee)
- `AlternativeOnly` - Only your payment system is available (Google Play billing disabled)

**User Choice Mode Example:**
```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import kotlinx.coroutines.flow.collect

// Initialize with user-choice mode
val config = InitConnectionConfig(
    alternativeBillingModeAndroid = AlternativeBillingModeAndroid.UserChoice
)
kmpIapInstance.initConnection(config)

// Listen for when user selects alternative billing
scope.launch {
    kmpIapInstance.userChoiceBillingListener.collect { details ->
        println("User selected alternative billing")
        println("Products: ${details.products}")

        // Process payment in your system, then report token to Google
        processPaymentAndReportToken(details)
    }
}
```

**Alternative Only Mode Example:**
```kotlin
// Step 1: Check availability
val isAvailable = kmpIapInstance.checkAlternativeBillingAvailabilityAndroid()

// Step 2: Show Google's information dialog
val userAccepted = kmpIapInstance.showAlternativeBillingDialogAndroid()

if (userAccepted) {
    // Step 3: Process payment in your system, then create token
    val token = kmpIapInstance.createAlternativeBillingTokenAndroid()

    // Step 4: Report token to Google Play backend within 24 hours
    reportTokenToGooglePlay(token)
}
```

### 🎨 Alternative Billing Demo Screen

A complete alternative billing demo screen has been added to the example app:

- **Platform-specific flows** - Demonstrates iOS and Android alternative billing patterns
- **Billing mode toggle** (Android) - Switch between `AlternativeOnly` and `UserChoice` with auto-reconnect
- **External URL input** (iOS) - Configure and test external purchase links
- **Real-time results** - View purchase flow status and responses
- **Step-by-step guidance** - Visual flow diagrams for both platforms

Navigate to `example/composeApp/src/commonMain/kotlin/dev/hyo/martie/screens/AlternativeBillingScreen.kt` to explore the implementation.

## 🐛 Bug Fixes & Improvements

### Serialization Error Fix

Fixed a critical serialization issue in `AvailablePurchasesScreen` where `PurchaseIOS` and `PurchaseAndroid` objects couldn't be serialized directly. Now uses the `Purchase.toJson()` method for proper JSON serialization.

**Before:**
```kotlin
// This would throw SerializationException
val jsonString = json.encodeToString(purchase)
```

**After:**
```kotlin
// Uses the built-in toJson() method
val purchaseMap = purchase.toJson()
val jsonString = buildJsonString(purchaseMap)
```

### iOS Subscription Enhancements

Added `renewalInfoIOS` field to `activeSubscription` (openiap-apple 1.2.24+), providing access to subscription renewal information including:
- Auto-renew status
- Expiration reason
- Grace period status
- Offer information

## ⚠️ Platform Requirements

### iOS

- **Minimum Version**: iOS 16.0+ for external purchase links, iOS 18.2+ for notice sheet
- **App Store Connect**: Must request and receive approval for external purchase entitlements
- **Provisioning Profile**: Must include StoreKit external purchase entitlements
- See [StoreKit External Purchase documentation](https://developer.apple.com/documentation/storekit/external-purchase)

### Android

- **Google Play Console**: Must be approved for alternative billing program
- **Token Reporting**: Must report tokens to Google within 24 hours
- **Backend Integration**: Server-side validation and reporting required
- See [Google Play Alternative Billing documentation](https://developer.android.com/google/play/billing/alternative)

## 📦 Installation

### Gradle (Kotlin DSL)

```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.hyochan:kmp-iap:1.0.0-rc.4")
        }
    }
}
```

### Gradle (Groovy)

```groovy
// shared/build.gradle
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation 'io.github.hyochan:kmp-iap:1.0.0-rc.4'
            }
        }
    }
}
```

### Version Catalog (libs.versions.toml)

```toml
[versions]
kmp-iap = "1.0.0-rc.4"

[libraries]
kmp-iap = { module = "io.github.hyochan:kmp-iap", version.ref = "kmp-iap" }
```

Then sync your Gradle project.

## 🚨 Important Notes

### For iOS Developers

Alternative billing on iOS requires explicit approval from Apple. During development:

1. Configure entitlements and Info.plist as shown above
2. Test regular IAP flows without external purchase features
3. When ready for production, follow Apple's approval process for external purchase entitlements

### For Android Developers

Alternative billing on Android requires:

1. Approval from Google Play Console for the alternative billing program
2. Backend integration to report tokens within 24 hours
3. Proper error handling for users not eligible for alternative billing

**No Breaking Changes**: All changes are additive. Existing apps will continue to work without modifications.

## 📖 Documentation Updates

This release includes comprehensive documentation:

- **[Alternative Billing Guide](/docs/guides/alternative-billing)** - Complete guide for implementing alternative billing
- **[Alternative Billing Example](/docs/examples/alternative-billing)** - Full code examples for both platforms
- **[Core Methods - Alternative Billing APIs](/docs/api/core-methods#ios-specific-alternative-billing)** - API reference
- **[OpenIAP Specification](https://openiap.dev)** - Official specification and terminology

## 🔗 References

- [OpenIAP Monorepo](https://github.com/hyodotdev/openiap)
- [OpenIAP Documentation](https://openiap.dev)
- [StoreKit External Purchase](https://developer.apple.com/documentation/storekit/external-purchase)
- [Google Play Alternative Billing](https://developer.android.com/google/play/billing/alternative)
- [Example Implementation](https://github.com/hyochan/kmp-iap/blob/main/example/composeApp/src/commonMain/kotlin/dev/hyo/martie/screens/AlternativeBillingScreen.kt)

## 🙏 Acknowledgments

Special thanks to the [OpenIAP](https://openiap.dev) project for providing the standardized specification that makes cross-platform IAP implementations possible.

Questions or issues? Let us know via [GitHub issues](https://github.com/hyochan/kmp-iap/issues).
