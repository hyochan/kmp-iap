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
        implementation("io.github.hyochan:kmp-iap:1.0.0-rc.6")
    }
}
```

Or if using version catalogs:

```toml
# gradle/libs.versions.toml
[versions]
kmp-iap = "1.0.0-rc.6"

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

kmp-iap uses the [OpenIAP framework](https://github.com/hyodotdev/openiap) on iOS. You need to add it to your iOS app using **either CocoaPods or Swift Package Manager**.

:::tip Quick Decision Guide
- **Use CocoaPods** if you want automatic dependency management through Gradle
- **Use SPM** if you prefer modern iOS tooling and want to avoid CocoaPods
:::

#### Step 1: Choose Your Dependency Manager

<details>
<summary><strong>Option A: CocoaPods (Recommended)</strong></summary>

CocoaPods is automatically managed by the Kotlin CocoaPods plugin.

1. **Ensure your shared module has the CocoaPods plugin:**

```kotlin
// shared/build.gradle.kts or composeApp/build.gradle.kts
plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
}

kotlin {
    cocoapods {
        version = "1.0"
        ios.deploymentTarget = "15.0"
        framework {
            baseName = "ComposeApp" // or "shared"
            isStatic = true
        }
    }
}
```

2. **Run pod install:**

```bash
cd iosApp
pod install
```

3. **Important:** Always open `.xcworkspace`, not `.xcodeproj`:

```bash
open iosApp.xcworkspace
```

The OpenIAP dependency will be automatically included.

</details>

<details>
<summary><strong>Option B: Swift Package Manager</strong></summary>

If you prefer SPM or don't want to use CocoaPods:

1. In Xcode, go to **File** → **Add Package Dependencies**
2. Enter repository URL: `https://github.com/hyodotdev/openiap.git`
3. Select version **1.2.26** (or "Up to Next Major" from 1.2.26)
4. Add to your iOS app target
5. Verify in **Build Phases** → **Link Binary with Libraries**

**Note:** With SPM, don't use the CocoaPods plugin. You'll manually update OpenIAP when kmp-iap updates.

</details>

#### Step 2: Enable In-App Purchase Capability

1. Open your project in Xcode
2. Select your project in the navigator
3. Select your target
4. Go to **Signing & Capabilities** tab
5. Click **+ Capability** and add **In-App Purchase**

#### Step 3: Configure Info.plist (iOS 14+)

Add the following to your `iosApp/Info.plist`:

```xml
<key>LSApplicationQueriesSchemes</key>
<array>
    <string>itms-apps</string>
</array>
```

#### Step 4: StoreKit Configuration (Optional)

For testing with StoreKit 2, create a `.storekit` configuration file:

1. In Xcode, go to **File** → **New** → **File**
2. Choose **StoreKit Configuration File**
3. Add your products for testing

#### Troubleshooting iOS Setup

<details>
<summary><strong>Error: Undefined symbol '_OBJC_CLASS_$__TtC7OpenIAP13OpenIapModule'</strong></summary>

This error means the OpenIAP framework isn't linked. Fix it by:

**If using CocoaPods:**
1. Run `cd iosApp && pod install`
2. Open `.xcworkspace` (NOT `.xcodeproj`)
3. Clean build folder: **Product** → **Clean Build Folder**
4. Rebuild

**If using SPM:**
1. Verify OpenIAP appears in **Build Phases** → **Link Binary with Libraries**
2. Clean and rebuild

See [Issue #21](https://github.com/hyochan/kmp-iap/issues/21) for more details.

</details>

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
        kmpIapInstance.initConnection()
        println("Connection initialized")

        // Connection successful, test product fetching
        val products = kmpIapInstance.fetchProducts {
            skus = listOf("test_product_id")
            type = ProductQueryType.InApp
        }
        println("Found ${products.size} products")

    } catch (e: PurchaseError) {
        println("Connection test failed: $e")
    } finally {
        kmpIapInstance.endConnection()
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

#### Linking Error: Undefined symbol OpenIapModule

**Symptom:** Build fails with `Undefined symbols for architecture arm64: "_OBJC_CLASS_$__TtC7OpenIAP13OpenIapModule"`

**Solution:**
- **CocoaPods:** Run `pod install` and open `.xcworkspace` (not `.xcodeproj`)
- **SPM:** Verify OpenIAP is in Build Phases → Link Binary with Libraries
- Clean build folder and rebuild
- See [Issue #21](https://github.com/hyochan/kmp-iap/issues/21) for details

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
