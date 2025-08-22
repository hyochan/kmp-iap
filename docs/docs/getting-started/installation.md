---
title: Installation & Setup
sidebar_label: Installation
sidebar_position: 1
---

import AdFitTopFixed from '@site/src/uis/AdFitTopFixed';

# Installation & Setup

<AdFitTopFixed />

Learn how to install and configure kmp-iap in your Kotlin Multiplatform project.

## Prerequisites

Before installing kmp-iap, ensure you have:

- Kotlin 2.1.10 or higher
- Gradle 8.0 or higher
- Active Apple Developer account (for iOS)
- Active Google Play Developer account (for Android)
- Physical device for testing (simulators/emulators have limited support)

## Package Installation

Add kmp-iap to your project's dependencies:

### In your shared module's `build.gradle.kts`

```kotlin
val commonMain by getting {
    dependencies {
        implementation("io.github.hyochan:kmp-iap:1.0.0-rc")
    }
}
```

Or if using version catalogs:

```toml
# gradle/libs.versions.toml
[versions]
kmp-iap = "1.0.0-rc"

[libraries]
kmp-iap = { module = "io.github.hyochan:kmp-iap", version.ref = "kmp-iap" }
```

```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.kmp.iap)
}
```

## Platform Configuration

### iOS Configuration

#### Enable In-App Purchase Capability

1. Open your project in Xcode
2. Select your project in the navigator
3. Select your target
4. Go to **Signing & Capabilities** tab
5. Click **+ Capability** and add **In-App Purchase**

#### Configure Info.plist (iOS 14+)

Add the following to your `iosApp/Info.plist`:

```xml
<key>LSApplicationQueriesSchemes</key>
<array>
    <string>itms-apps</string>
</array>
```

#### StoreKit Configuration (Optional)

For testing with StoreKit 2, create a `.storekit` configuration file:

1. In Xcode, go to **File** → **New** → **File**
2. Choose **StoreKit Configuration File**
3. Add your products for testing

### Android Configuration

#### Update build.gradle

Ensure your `androidApp/build.gradle.kts` has the minimum SDK version:

```kotlin
android {
    compileSdk = 34

    defaultConfig {
        minSdk = 24  // Required minimum
        targetSdk = 34
    }
}
```

#### Add Billing Client dependency

The library includes the billing client, but ensure ProGuard rules are configured if using ProGuard:

```proguard
# In-App Purchase
-keep class com.android.billingclient.** { *; }
-keep class io.github.hyochan.kmpiap.** { *; }
-keepattributes *Annotation*
```

#### Permissions

The library automatically adds the required billing permission to your manifest.

## Configuration

### App Store Connect (iOS)

1. Sign in to [App Store Connect](https://appstoreconnect.apple.com)
2. Select your app
3. Navigate to **Monetization** → **In-App Purchases**
4. Create your products:

   - **Consumable**: Can be purchased multiple times
   - **Non-Consumable**: One-time purchase
   - **Auto-Renewable Subscription**: Recurring payments
   - **Non-Renewing Subscription**: Fixed duration

5. Fill in required fields:

   - Reference Name (internal use)
   - Product ID (used in code)
   - Pricing
   - Localizations

6. Submit for review with your app

### Google Play Console (Android)

1. Sign in to [Google Play Console](https://play.google.com/console)
2. Select your app
3. Navigate to **Monetization** → **In-app products**
4. Create products:

   - **One-time products**: Consumable or non-consumable
   - **Subscriptions**: Recurring payments

5. Configure product details:

   - Product ID (used in code)
   - Name and description
   - Price
   - Status (Active)

6. Save and activate products

## Verification

### Initialize the Plugin

```kotlin
import io.github.hyochan.kmpiap.*
import io.github.hyochan.kmpiap.types.*
import kotlinx.coroutines.*

class IAPManager {
    private val kmpIAP = KmpIAP()
    private val scope = CoroutineScope(Dispatchers.Main)

    fun initialize() {
        scope.launch {
            try {
                kmpIAP.initConnection()
                println("IAP connection initialized successfully")
            } catch (e: PurchaseError) {
                println("Failed to initialize IAP connection: $e")
            }
        }
    }

    fun dispose() {
        scope.launch {
            kmpIAP.endConnection()
        }
        scope.cancel()
    }
}
```

### Test Connection

Test your setup with this verification code:

```kotlin
suspend fun testConnection() {
    import io.github.hyochan.kmpiap.kmpIapInstance

    try {
        // Initialize connection
        val connected = kmpIapInstance.initConnection()
        println("Connection status: $connected")

        if (!connected) {
            println("Failed to connect to store")
            return
        }

        // Connection successful, test product fetching
        val products = kmpIAP.requestProducts(
            ProductRequest(
                skus = listOf("test_product_id"),
                type = ProductType.INAPP
            )
        )
        println("Found ${products.size} products")

    } catch (e: PurchaseError) {
        println("Connection test failed: $e")
    } finally {
        kmpIAP.endConnection()
    }
}
```

## Next Steps

Now that you have kmp-iap installed and configured:

- [**iOS Setup Guide**](/docs/getting-started/ios-setup) - iOS specific configuration
- [**Android Setup Guide**](/docs/getting-started/android-setup) - Android specific configuration
- [**Basic Implementation**](/docs/guides/purchases) - Learn the fundamentals

## Troubleshooting

### iOS Common Issues

#### Permission Denied

- Ensure In-App Purchase capability is enabled
- Verify your Apple Developer account has active agreements
- Check that products are configured in App Store Connect

#### Products Not Loading

- Products must be submitted for review (at least once)
- Wait 24 hours after creating products
- Verify product IDs match exactly

### Android Common Issues

#### Billing Unavailable

- Test on a real device (not emulator)
- Ensure Google Play is installed and up-to-date
- Verify app is signed with the same key as uploaded to Play Console

#### Products Not Found

- Products must be active in Play Console
- App must be published (at least to internal testing)
- Wait 2-3 hours after creating products

---

Need help? Check our [troubleshooting guide](/docs/guides/troubleshooting) or [open an issue](https://github.com/hyochan/kmp-iap/issues) on GitHub.
