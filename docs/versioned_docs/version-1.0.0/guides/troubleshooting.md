---
sidebar_position: 9
title: Troubleshooting
---

import IapKitBanner from '@site/src/uis/IapKitBanner';

# Troubleshooting

<IapKitBanner />

Common issues and solutions when implementing in-app purchases with kmp-iap.

## Installation & Setup Issues

### Gradle Configuration Problems

**Problem:** Build fails with dependency conflicts

**Solution:**
```kotlin
// In your module's build.gradle.kts
dependencies {
    implementation("io.github.hyochan:kmp-iap:<version>")

    // Ensure correct Kotlin version
    implementation("org.jetbrains.kotlin:kotlin-stdlib:<version>")
}

// Clean and rebuild
./gradlew clean
./gradlew build
```

### iOS Build Issues

**Problem:** Build fails with StoreKit-related errors

**Solution:**
1. Ensure iOS deployment target is 15.0+:
   ```ruby
   # In your iOS project settings
   platform :ios, '15.0'
   ```

2. Clean and rebuild:
   ```bash
   cd iosApp
   rm -rf build
   pod deintegrate
   pod install
   cd ..
   ./gradlew clean
   ./gradlew build
   ```

### Android Build Issues

**Problem:** Build fails with billing library conflicts

**Solution:**
1. Check `android` block in build.gradle.kts:
   ```kotlin
   android {
       compileSdk = 34

       defaultConfig {
           minSdk = 24  // Required minimum
       }
   }
   ```

2. Add billing permission to AndroidManifest.xml:
   ```xml
   <uses-permission android:name="com.android.vending.BILLING" />
   ```

## Connection & Initialization Issues

### "Billing is unavailable" Error

**Problem:** IAP initialization returns billing unavailable error

**Possible Causes & Solutions:**

1. **Google Play Store not installed/updated:**
   - Ensure Google Play Store is installed and updated
   - Test on real device, not emulator

2. **App not uploaded to Play Console:**
   - Upload app to Internal Testing track minimum
   - Wait for processing (can take several hours)

3. **Package name mismatch:**
   ```kotlin
   // Ensure applicationId matches Play Console
   android {
       defaultConfig {
           applicationId = "com.yourcompany.yourapp"
       }
   }
   ```

4. **Developer account issues:**
   - Verify Google Play Developer account is active
   - Complete merchant agreement

### iOS Connection Issues

**Problem:** Connection fails on iOS

**Solutions:**

1. **Sandbox environment:**
   - Test on real device with sandbox account
   - Don't sign in to sandbox account in Settings
   - Only sign in when prompted during purchase

2. **App Store Connect setup:**
   - Verify products are "Ready to Submit"
   - Complete agreements in ASC
   - Wait up to 24 hours for product propagation

## Product Loading Issues

### Products Not Loading

**Problem:** `fetchProducts()` returns empty list

**Debugging Steps:**

1. **Verify product IDs:**
   ```kotlin
   // Enable debug logging
   // Check exact product ID matching
   try {
       val products = kmpIapInstance.fetchProducts {
           skus = listOf("exact.product.id.from.store")
           type = ProductQueryType.InApp
       }
       println("Loaded products: ${products.size}")
   } catch (e: PurchaseError) {
       println("Error loading products: $e")
   }
   ```

2. **Check store console:**
   - iOS: Products "Ready to Submit" in App Store Connect
   - Android: Products "Active" in Play Console

3. **Wait for propagation:**
   - New products can take 24+ hours to be available
   - Try with existing, known-working products first

### iOS Products Not Loading

**Specific Solutions:**

1. **Bundle ID verification:**
   - Check bundle ID in Xcode matches App Store Connect
   - Verify in Info.plist

2. **Agreements verification:**
   - Check App Store Connect > Agreements, Tax, and Banking
   - Ensure Paid Applications Agreement is active

3. **Product status:**
   - Products must be "Ready to Submit" or "Approved"
   - Check in App Store Connect > Features > In-App Purchases

### Android Products Not Loading

**Specific Solutions:**

1. **App upload requirement:**
   ```bash
   # Build and upload APK/AAB to Play Console
   ./gradlew bundleRelease
   ```

2. **Package name verification:**
   - Verify `applicationId` in build.gradle.kts
   - Must exactly match Play Console

3. **License testing:**
   - Add test accounts in Play Console > Setup > License testing
   - Use test accounts for testing

## Purchase Issues

### Purchase Flow Not Working

**Problem:** Purchase request doesn't trigger system dialog

**Debugging:**

1. **Check initialization:**
   ```kotlin
   // Ensure IAP is initialized before purchase
   val isConnected = kmpIapInstance.isConnected()
   if (!isConnected) {
       println("IAP not initialized")
       return
   }
   ```

2. **Verify product exists:**
   ```kotlin
   // Check if products are loaded
   val products = loadedProducts
   if (products.none { it.productId == productId }) {
       println("Product not found: $productId")
       return
   }
   ```

3. **Check state observers:**
   ```kotlin
   // Ensure observers are set up before purchase
   scope.launch {
       kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
           purchase?.let {
               println("Purchase updated: ${it.productId}")
           }
       }
   }
   
   scope.launch {
       kmpIapInstance.purchaseErrorListener.collect { error ->
           error?.let {
               println("Purchase error: ${it.message}")
           }
       }
   }
   ```

### Transaction Not Completing

**Problem:** Purchase succeeds but transaction doesn't complete

**Solution:**
```kotlin
private suspend fun handlePurchaseUpdate(purchase: Purchase) {
    // Assuming kmpIap is available as class property or use kmpIapInstance
    try {
        // IMPORTANT: Always complete transactions
        val success = kmpIapInstance.finishTransaction(
            purchase = purchase,
            isConsumable = isConsumable(purchase.productId)
        )
        
        if (success) {
            println("Transaction completed successfully")
        } else {
            println("Failed to complete transaction")
        }
        
        // Clear purchase state
        // No need to clear purchase with new API
    } catch (e: Exception) {
        println("Failed to complete transaction: $e")
    }
}
```

### "Already Owned" Error

**Problem:** Getting "already owned" error on Android

**Solutions:**

1. **Check for existing purchases:**
   ```kotlin
   // For consumable products
   scope.launch {
       val purchases = kmpIapInstance.getAvailablePurchases()
       purchases.filter { isConsumable(it.productId) }
           .forEach { purchase ->
               // Consume the purchase
               kmpIapInstance.finishTransaction(
                   purchase = purchase,
                   isConsumable = true
               )
           }
   }
   ```

2. **Clear test purchases:**
   - In Google Play Store app: Menu > Account > Purchase history
   - Cancel test purchases

## Testing Issues

### Sandbox Testing Problems (iOS)

**Problem:** Sandbox purchases not working

**Solutions:**

1. **Account management:**
   - Create fresh sandbox accounts in App Store Connect
   - Don't sign in to sandbox account in Settings
   - Only sign in when prompted during purchase

2. **Purchase history:**
   - Clear purchase history in Settings > App Store > Sandbox Account
   - Use different sandbox accounts for different test scenarios

3. **Network issues:**
   - Test on real device with cellular/different WiFi
   - Sandbox can be unstable, try multiple times

### Test Purchases Not Working (Android)

**Problem:** Test purchases failing on Android

**Solutions:**

1. **License testers:**
   ```text
   Play Console > Setup > License testing > License Testers
   Add Gmail accounts for testing
   ```

2. **Test tracks:**
   - Upload app to Internal Testing minimum
   - Join testing program with test account
   - Install from Play Store (not sideload)

3. **Account verification:**
   - Use Gmail account added as license tester
   - Clear Play Store cache/data if needed

## Runtime Errors

### StateFlow Collection Errors

**Problem:** Multiple collectors or flow errors

**Solution:**
```kotlin
class PurchaseManager : ViewModel() {
    private val kmpIap = KmpIAP()
    
    init {
        initializeIAP()
        setupObservers()
    }
    
    private fun initializeIAP() {
        viewModelScope.launch {
            kmpIap.initConnection()
        }
    }
    
    private fun setupObservers() {
        // Use viewModelScope for automatic cancellation
        viewModelScope.launch {
            kmpIap.purchaseUpdatedListener.collect { purchase ->
                handlePurchase(purchase)
            }
        }
        
        viewModelScope.launch {
            kmpIap.purchaseErrorListener.collect { error ->
                handleError(error)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Dispose KmpIAP resources
        kmpIap.dispose()
    }
}
```

### Memory Leaks

**Problem:** App crashes or memory issues

**Solution:** Always clean up resources:
```kotlin
class IAPService : ViewModel() {
    private val kmpIap = KmpIAP()
    
    init {
        viewModelScope.launch {
            kmpIap.initConnection()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Dispose IAP resources
        kmpIap.dispose()
    }
}
```

## Receipt Validation Issues

### iOS Receipt Validation

**Problem:** Receipt validation fails

**Solutions:**

1. **Receipt data retrieval:**
   ```kotlin
   private suspend fun validateIOSPurchase(purchase: Purchase) {
       val receipt = purchase.transactionReceipt
       if (receipt == null) {
           println("No receipt data available")
           return
       }
       
       // Send to your server for validation
       val isValid = api.validateIOSReceipt(
           receipt = receipt,
           isProduction = !BuildConfig.DEBUG
       )
   }
   ```

2. **Server validation:**
   - Use production URL for live app: `https://buy.itunes.apple.com/verifyReceipt`
   - Use sandbox URL for testing: `https://sandbox.itunes.apple.com/verifyReceipt`

### Android Receipt Validation

**Problem:** Purchase token validation fails

**Solution:**
```kotlin
// Use Google Play Developer API for server-side validation
private suspend fun validateAndroidPurchase(purchase: Purchase) {
    val purchaseToken = purchase.purchaseToken
    if (purchaseToken == null) {
        println("No purchase token available")
        return
    }
    
    // Send to your server
    val isValid = api.validateAndroidPurchase(
        token = purchaseToken,
        productId = purchase.productId,
        packageName = BuildConfig.APPLICATION_ID
    )
}
```

## Debug Tools

### Enable Debug Logging

```kotlin
// Add logging to track IAP flow
class DebugIAPHelper(scope: CoroutineScope) {
    
    private val kmpIap = KmpIAP()
    
    init {
        scope.launch {
            val connected = kmpIap.initConnection()
            println("[IAP] Connection state: $connected")
        }
        
        scope.launch {
            kmpIap.purchaseErrorListener.collect { error ->
                println("[IAP] Error: ${error.code} - ${error.message}")
            }
        }
    }
}
```

### Check Connection Status

```kotlin
suspend fun debugConnection() {
    try {
        kmpIapInstance.initConnection()
        println("Connection initialized")

        // Test with known product
        val testProductId = when (getCurrentPlatform()) {
            IapPlatform.Android -> "android.test.purchased"
            IapPlatform.Ios -> "your.test.product"
        }

        val products = kmpIapInstance.fetchProducts {
            skus = listOf(testProductId)
            type = ProductQueryType.InApp
        }
        println("Test products loaded: ${products.size}")
    } catch (e: PurchaseError) {
        println("Debug error: $e")
    }
}
```

## Common Error Codes

### Understanding Error Codes

```kotlin
fun handleError(error: PurchaseError) {
    when (error.code) {
        ErrorCode.UserCancelled -> {
            // User cancelled - no action needed
        }
        ErrorCode.NetworkError -> {
            showRetryDialog("Network error. Please try again.")
        }
        ErrorCode.AlreadyOwned -> {
            // Refresh purchases
            scope.launch {
                kmpIapInstance.syncPurchases()
            }
        }
        ErrorCode.ServiceDisconnected -> {
            // Reconnect
            scope.launch {
                kmpIapInstance.initConnection()
            }
        }
        else -> {
            showError("Purchase failed: ${error.message}")
        }
    }
}
```

## Getting Help

If you're still experiencing issues:

1. **Check logs:** Enable debug logging and check console output
2. **Minimal reproduction:** Create minimal example that reproduces issue
3. **Platform testing:** Test on both iOS and Android
4. **Version check:** Ensure you're using latest library version
5. **GitHub Issues:** [Report bugs](https://github.com/hyochan/kmp-iap/issues) with detailed information

### Issue Report Template

When reporting issues, include:

```text
**Platform:** iOS/Android/Both
**Library Version:** kmp-iap x.x.x
**Kotlin Version:** x.x.x
**Device:** Real device/Simulator/Emulator

**Issue Description:**
Clear description of the problem

**Steps to Reproduce:**
1. Step one
2. Step two
3. Step three

**Expected Behavior:**
What should happen

**Actual Behavior:**
What actually happens

**Logs:**
```text
Relevant console output
```

**Sample Code:**
```kotlin
// Minimal code that reproduces the issue
```

This comprehensive troubleshooting guide should help developers resolve most common issues with kmp-iap.