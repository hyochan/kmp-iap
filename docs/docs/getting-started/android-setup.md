---
title: Android Setup
sidebar_label: Android Setup
sidebar_position: 3
---

# Android Setup Guide

Complete guide to configure kmp-iap for Android with Google Play Billing Client v7.

## Prerequisites

- **Android API 24+** (Android 7.0+)
- **Google Play Console Account** with billing enabled
- **Android Studio** with Android SDK
- **Physical device** for testing (emulators have limited support)

## Project Configuration

### Update build.gradle

Ensure your `androidApp/build.gradle.kts` has the correct configuration:

```kotlin title="androidApp/build.gradle.kts"
android {
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.yourapp.example"
        minSdk = 24  // Required minimum for kmp-iap
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    buildTypes {
        release {
            // Signing configuration
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### ProGuard Configuration

If using ProGuard/R8, add these rules to `androidApp/proguard-rules.pro`:

```proguard title="androidApp/proguard-rules.pro"
# KMP IAP
-keep class io.github.hyochan.kmpiap.** { *; }
-keep class com.android.vending.billing.**
-keep class com.google.android.gms.** { *; }

# Preserve billing client classes
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.vending.billing.**
-dontwarn com.google.android.gms.**

# Keep annotation classes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
```

### Permissions

The library automatically adds the required billing permission to your `AndroidManifest.xml`:

```xml title="androidApp/src/main/AndroidManifest.xml"
<!-- This is added automatically by the library -->
<uses-permission android:name="com.android.vending.BILLING" />
```

## Google Play Console Setup

### Create Your App

1. Sign in to [Google Play Console](https://play.google.com/console)
2. Create a new app or select existing app
3. Complete the app information and store listing
4. Set up your app's content rating and target audience

### Enable In-App Products

1. Go to **Monetize** → **Products** → **In-app products**
2. Click **Create product** to add new products
3. Configure your product details:

```
Product ID: premium_upgrade
Name: Premium Upgrade
Description: Unlock all premium features
Price: $9.99
```

### Product Types

Choose the appropriate product type:

- **Managed Products**: One-time purchases (non-consumable)
- **Consumable Products**: Can be purchased multiple times
- **Subscriptions**: Recurring billing (configured separately)

:::tip Product ID Best Practices
- Use descriptive, consistent naming: `premium_upgrade`, `remove_ads`, `coins_large_pack`
- Avoid special characters and spaces
- Keep IDs consistent across platforms (iOS/Android)
:::

## App Signing & Release

### Generate Signing Key

For production releases, you need a signed APK:

```bash
# Generate a new keystore (one-time setup)
keytool -genkey -v -keystore ~/upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload

# Or use Android Studio: Build → Generate Signed Bundle/APK
```

### Configure Signing

Create `androidApp/keystore.properties`:

```properties title="androidApp/keystore.properties"
storePassword=YOUR_STORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=upload
storeFile=../upload-keystore.jks
```

Update `androidApp/build.gradle.kts`:

```kotlin title="androidApp/build.gradle.kts"
import java.util.Properties
import java.io.FileInputStream

val keystorePropertiesFile = rootProject.file("androidApp/keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### Upload to Play Console

1. Build a signed app bundle:
   ```bash
   ./gradlew :androidApp:bundleRelease
   ```

2. Upload `androidApp/build/outputs/bundle/release/app-release.aab` to Play Console

3. Create an **Internal Testing** track for testing in-app purchases

## Testing Setup

### Internal Testing

1. In Play Console, go to **Testing** → **Internal testing**
2. Create a new release and upload your signed app bundle
3. Add test users by email addresses
4. Send the testing link to your testers

### License Testing

1. Go to **Setup** → **License testing**
2. Add Gmail accounts that should have testing access
3. These accounts can test purchases without being charged

### Test Accounts Configuration

```kotlin title="Testing Example"
import io.github.hyochan.kmpiap.useIap.*
import kotlinx.coroutines.*

suspend fun testAndroidPurchase(iapHelper: UseIap) {
    try {
        // Initialize connection
        iapHelper.initConnection()
        println("Billing client connected")
        
        // Check connection state
        val isConnected = iapHelper.isConnected.value
        println("Connection state: $isConnected")
        
        // Get products
        val products = iapHelper.getProducts(listOf(
            "premium_upgrade",
            "remove_ads"
        ))
        println("Found ${products.size} products")
        
        // Test purchase
        if (products.isNotEmpty()) {
            iapHelper.requestPurchase(
                sku = products.first().productId,
                obfuscatedAccountIdAndroid = "test_user_123"
            )
        }
    } catch (e: Exception) {
        println("Android test failed: $e")
    }
}
```

## Billing Client v7 Features

### Enhanced Error Handling

```kotlin
import kotlinx.coroutines.flow.collectLatest

scope.launch {
    iapHelper.currentError.collectLatest { error ->
        error?.let {
            when (it.code) {
                ErrorCode.SERVICE_UNAVAILABLE -> {
                    // Google Play services unavailable
                    showDialog("Please update Google Play services")
                }
                ErrorCode.BILLING_UNAVAILABLE -> {
                    // Billing API version not supported
                    showDialog("In-app purchases not supported on this device")
                }
                ErrorCode.ITEM_UNAVAILABLE -> {
                    // Product not found or not available for purchase
                    showDialog("This item is currently unavailable")
                }
                ErrorCode.DEVELOPER_ERROR -> {
                    // Invalid arguments provided to the API
                    println("Developer error: Check product configuration")
                }
                else -> {
                    showDialog("Purchase failed: ${it.message}")
                }
            }
        }
    }
}
```

### Connection State Monitoring

```kotlin
// Monitor connection state
suspend fun checkConnectionHealth(iapHelper: UseIap) {
    val isConnected = iapHelper.isConnected.value
    
    if (!isConnected) {
        // Reconnect if needed
        try {
            iapHelper.initConnection()
            println("Billing client reconnected")
        } catch (e: Exception) {
            println("Failed to reconnect: $e")
        }
    } else {
        println("Billing client ready")
    }
}
```

## Common Issues & Solutions

### Issue: "Billing service unavailable"

**Solutions:**
- Test on a real device with Google Play services
- Ensure you're signed in to a Google account
- Check that Google Play is up to date
- Verify your app is properly signed and uploaded

### Issue: Products not loading

**Solutions:**
- Ensure products are active in Play Console
- Verify product IDs match exactly
- Check that your app is published (at least to Internal Testing)
- Wait for product propagation (can take a few hours)

### Issue: "Item unavailable" error

**Solutions:**
- Verify the product exists and is active in Play Console
- Check that your app version includes the product
- Ensure you're testing with the correct account
- Confirm the product is available in your test region

### Issue: Testing with wrong account

**Solutions:**
- Use an account added to License Testing
- Install the app from the Internal Testing link
- Don't use the developer account for testing purchases
- Clear Google Play Store cache if needed

## Advanced Configuration

### Obfuscated Account IDs

For enhanced security, use obfuscated account IDs:

```kotlin
iapHelper.requestPurchase(
    sku = "premium_upgrade",
    obfuscatedAccountIdAndroid = "user_account_123",
    obfuscatedProfileIdAndroid = "profile_456"
)
```

### Purchase Token Validation

Always validate purchases on your server:

```kotlin
scope.launch {
    iapHelper.currentPurchase.collectLatest { purchase ->
        purchase?.let {
            // Send to your server for validation
            val isValid = validatePurchaseOnServer(it)
            
            if (isValid) {
                // Grant the purchased content
                grantPurchase(it)
                
                // Acknowledge the purchase
                iapHelper.finishTransaction(
                    purchase = it,
                    isConsumable = false
                )
            }
        }
    }
}
```

### Deep Linking to Subscriptions

Direct users to subscription management:

```kotlin
suspend fun openSubscriptionManagement(iapHelper: UseIap) {
    val platform = getCurrentPlatform()
    if (platform == IAPPlatform.ANDROID) {
        iapHelper.deepLinkToSubscriptionsAndroid("your_subscription_sku")
    }
}
```

## Next Steps

Once Android setup is complete:

1. **[Basic Implementation](/docs/guides/purchases)** - Start implementing purchases
2. **[Troubleshooting](/docs/guides/troubleshooting)** - Common issues and solutions

---

Need help? Check our [troubleshooting guide](/docs/guides/troubleshooting) or [open an issue](https://github.com/hyochan/kmp-iap/issues) on GitHub.