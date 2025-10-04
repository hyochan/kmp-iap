---
slug: 1.0.0-rc.4
title: 1.0.0-rc.4 - Alternative Billing Support
authors: [hyochan]
tags: [release, alternative-billing, ios, android, storekit, kotlin-multiplatform]
date: 2025-10-05
---

# 1.0.0-rc.4 Release Notes

KMP-IAP 1.0.0-rc.4 introduces **Alternative Billing** support for both iOS and Android platforms, enabling developers to offer external payment options in compliance with App Store and Google Play requirements.

This release integrates StoreKit External Purchase APIs (iOS 16.0+) and Google Play Alternative Billing APIs, providing a unified Kotlin Multiplatform interface for alternative payment flows across platforms.

👉 [View the 1.0.0-rc.4 release](https://github.com/hyochan/kmp-iap/releases/tag/1.0.0-rc.4)

<!-- truncate -->

## 🚀 Highlights

### iOS Alternative Billing (StoreKit External Purchase)

Three new APIs for managing external purchases on iOS:

- **[`canPresentExternalPurchaseNoticeIOS()`](/docs/api/core-methods#canpresentexternalpurchasenoticeios)** - Check if the notice sheet is available (iOS 18.2+)
- **[`presentExternalPurchaseNoticeSheetIOS()`](/docs/api/core-methods#presentexternalpurchasenoticesheetios)** - Present a notice before redirecting to external purchase (iOS 18.2+)
- **[`presentExternalPurchaseLinkIOS(url)`](/docs/api/core-methods#presentexternalpurchaselinkios)** - Open external purchase link in Safari (iOS 16.0+)

**Manual Configuration Required**: Configure your iOS project's Info.plist and entitlements:

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

### Android Alternative Billing

Three new APIs for Google Play Alternative Billing flow:

- **[`checkAlternativeBillingAvailabilityAndroid()`](/docs/api/core-methods#checkalternativebillingavailabilityandroid)** - Check if alternative billing is available for the user
- **[`showAlternativeBillingDialogAndroid()`](/docs/api/core-methods#showalternativebillingdialogandroid)** - Show Google's required information dialog
- **[`createAlternativeBillingTokenAndroid()`](/docs/api/core-methods#createalternativebillingtokenandroid)** - Generate reporting token after successful payment

**Configuration Support**: `initConnection()` now accepts an optional config parameter:

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

**Two Billing Modes**:

- `UserChoice` - Users choose between Google Play billing (30% fee) or your payment system (lower fee)
- `AlternativeOnly` - Only your payment system is available (Google Play billing disabled)

## 📚 Usage Examples

### iOS External Purchase

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

**User Choice Mode** - When using `UserChoice` mode, listen for user selection with **`userChoiceBillingListener`**:

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.AlternativeBillingModeAndroid
import io.github.hyochan.kmpiap.openiap.InitConnectionConfig
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

**Alternative Only Mode** - Manual 3-step flow:

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance

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

## 🎨 Example App

A complete alternative billing demo screen has been added to the example app:

- **Platform-specific flows** - Demonstrates iOS and Android alternative billing patterns
- **Billing mode toggle** (Android) - Switch between `AlternativeOnly` and `UserChoice` with auto-reconnect
- **External URL input** (iOS) - Configure and test external purchase links
- **Real-time results** - View purchase flow status and responses
- **Step-by-step guidance** - Visual flow diagrams for both platforms

Navigate to `example/composeApp/src/commonMain/kotlin/dev/hyo/martie/screens/AlternativeBillingScreen.kt` to explore the implementation.

## 🔧 OpenIAP Upgrades

This release upgrades to the latest OpenIAP specification versions:

- **openiap-apple** upgraded to **1.2.10** with StoreKit external purchase support
- **openiap-google** upgraded to **1.2.12** with alternative billing APIs
- **openiap-gql** upgraded to **1.0.12** with updated type definitions

All implementations are now **100% OpenIAP specification compliant**.

## 🐛 Bug Fixes

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

## 🔗 References

- [OpenIAP Documentation](https://openiap.dev)
- [StoreKit External Purchase](https://developer.apple.com/documentation/storekit/external-purchase)
- [Google Play Alternative Billing](https://developer.android.com/google/play/billing/alternative)
- [Example Implementation](https://github.com/hyochan/kmp-iap/blob/main/example/composeApp/src/commonMain/kotlin/dev/hyo/martie/screens/AlternativeBillingScreen.kt)

## 🙏 Acknowledgments

Special thanks to the [OpenIAP](https://openiap.dev) project for providing the standardized specification that makes cross-platform IAP implementations possible.

Questions or issues? Let us know via [GitHub issues](https://github.com/hyochan/kmp-iap/issues).
