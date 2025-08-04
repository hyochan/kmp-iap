---
title: iOS Setup
sidebar_label: iOS Setup
sidebar_position: 2
---

# iOS Setup Guide

Complete guide to configure kmp-iap for iOS with StoreKit 2 support.

## Prerequisites

- **iOS 11.0+** (StoreKit 2 requires iOS 15.0+)
- **Xcode 14+** for StoreKit 2 development
- **Apple Developer Account** with valid agreements
- **Physical device** for production testing

## Xcode Configuration

### Enable In-App Purchase Capability

1. Open your project in Xcode
2. Select your project in the navigator
3. Select your target under **TARGETS**
4. Go to **Signing & Capabilities** tab
5. Click **+ Capability** and add **In-App Purchase**

### Configure Info.plist

Add the following to your `iosApp/Info.plist` for iOS 14+ compatibility:

```xml title="iosApp/Info.plist"
<key>LSApplicationQueriesSchemes</key>
<array>
    <string>itms-apps</string>
</array>

<!-- Optional: For subscription management -->
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsLocalNetworking</key>
    <true/>
    <key>NSExceptionDomains</key>
    <dict>
        <key>apps.apple.com</key>
        <dict>
            <key>NSExceptionRequiresForwardSecrecy</key>
            <false/>
            <key>NSIncludesSubdomains</key>
            <true/>
        </dict>
    </dict>
</dict>
```

### Configure StoreKit Testing

For local testing with StoreKit Configuration File:

1. In Xcode, go to **File** → **New** → **File**
2. Choose **StoreKit Configuration File**
3. Name it (e.g., `Products.storekit`)
4. Add your products for testing

## App Store Connect Configuration

### Create Your App

1. Sign in to [App Store Connect](https://appstoreconnect.apple.com)
2. Go to **My Apps** → **+** → **New App**
3. Fill in the required information:
   - Platform: iOS
   - Name: Your app name
   - Primary Language
   - Bundle ID: Must match your Xcode project
   - SKU: Unique identifier for your app

### Configure In-App Purchases

1. Select your app in App Store Connect
2. Go to **Monetization** → **In-App Purchases**
3. Click **+** to create a new in-app purchase
4. Choose the product type:
   - **Consumable**: Can be purchased multiple times
   - **Non-Consumable**: One-time purchase
   - **Auto-Renewable Subscription**: Recurring subscription
   - **Non-Renewing Subscription**: Fixed-term subscription

### Product Configuration

Fill in the required fields for each product:

```
Reference Name: Premium Upgrade (internal use)
Product ID: com.yourapp.premium_upgrade
Price: Choose from price tiers
Display Name: Premium Features
Description: Unlock all premium features
```

:::tip Product ID Best Practices
- Use reverse domain notation: `com.yourapp.productname`
- Keep IDs consistent across platforms
- Avoid special characters and spaces
- Use descriptive names: `premium_monthly`, `coins_pack_large`
:::

## StoreKit 2 Implementation

### Enable StoreKit 2

kmp-iap automatically uses StoreKit 2 on iOS 15+. To ensure compatibility:

```kotlin
// Check iOS version for StoreKit 2 availability
suspend fun checkStoreKit2Support(iapHelper: UseIap) {
    val platform = getCurrentPlatform()
    if (platform == IAPPlatform.IOS) {
        // StoreKit 2 is available on iOS 15+
        println("Platform: iOS")
        // kmp-iap will automatically use StoreKit 2 when available
    }
}
```

### StoreKit Configuration File

Create a `.storekit` file for testing:

```json title="Products.storekit"
{
  "identifier": "YOUR_BUNDLE_ID",
  "nonRenewingSubscriptions": [],
  "products": [
    {
      "displayPrice": "0.99",
      "familyShareable": false,
      "internalID": "XXXXXX",
      "localizations": [
        {
          "description": "Remove all advertisements",
          "displayName": "Remove Ads",
          "locale": "en_US"
        }
      ],
      "productID": "com.yourapp.remove_ads",
      "referenceName": "Remove Ads",
      "type": "NonConsumable"
    }
  ],
  "settings": {},
  "subscriptionGroups": []
}
```

## Testing Setup

### Sandbox Testing

1. Create sandbox tester accounts in App Store Connect:
   - Go to **Users and Access** → **Sandbox Testers**
   - Click **+** to add a new tester
   - Use a unique email (not associated with an Apple ID)

2. Configure your device for sandbox testing:
   - Sign out of your Apple ID in Settings
   - When prompted during purchase, sign in with sandbox account

### StoreKit Testing in Xcode

1. In Xcode, go to **Product** → **Scheme** → **Edit Scheme**
2. Select **Run** → **Options**
3. Under **StoreKit Configuration**, select your `.storekit` file
4. Run your app - purchases will use the local configuration

### Test Purchase Flow

```kotlin title="iOS Testing Example"
import io.github.hyochan.kmpiap.useIap.*
import kotlinx.coroutines.*

suspend fun testIOSPurchase(iapHelper: UseIap) {
    try {
        // Initialize connection
        iapHelper.initConnection()
        println("StoreKit connected")
        
        // Get products
        val products = iapHelper.getProducts(listOf(
            "com.yourapp.premium_upgrade",
            "com.yourapp.remove_ads"
        ))
        println("Found ${products.size} products")
        
        // Display products
        products.forEach { product ->
            println("Product: ${product.productId}")
            println("Price: ${product.price}")
            println("Title: ${product.title}")
        }
        
        // Test purchase
        if (products.isNotEmpty()) {
            iapHelper.requestPurchase(
                sku = products.first().productId,
                // iOS specific parameters can be added here
            )
        }
    } catch (e: Exception) {
        println("iOS test failed: $e")
    }
}
```

## StoreKit 2 Features

### Transaction Management

```kotlin
// Listen for transaction updates
import kotlinx.coroutines.flow.collectLatest

scope.launch {
    iapHelper.currentPurchase.collectLatest { purchase ->
        purchase?.let {
            println("Transaction updated: ${it.transactionId}")
            
            // Verify the purchase
            val isValid = verifyPurchase(it)
            
            if (isValid) {
                // Deliver content
                deliverContent(it)
                
                // Finish transaction
                iapHelper.finishTransaction(
                    purchase = it,
                    isConsumable = false
                )
            }
        }
    }
}
```

### Subscription Management

```kotlin
// Show subscription management page
suspend fun showSubscriptionManagement(iapHelper: UseIap) {
    val platform = getCurrentPlatform()
    if (platform == IAPPlatform.IOS) {
        iapHelper.showManageSubscriptionsIOS()
    }
}

// Present code redemption sheet
suspend fun redeemCode(iapHelper: UseIap) {
    val platform = getCurrentPlatform()
    if (platform == IAPPlatform.IOS) {
        iapHelper.presentCodeRedemptionSheetIOS()
    }
}
```

## Common Issues & Solutions

### Issue: Products not loading

**Solutions:**
- Ensure your Apple Developer agreements are active
- Verify product IDs match exactly
- Products must be submitted for review at least once
- Wait 24 hours after creating products
- Check that Bundle ID matches App Store Connect

### Issue: "Cannot connect to iTunes Store"

**Solutions:**
- Test on a real device (not simulator)
- Ensure you're using a sandbox account
- Check network connectivity
- Verify In-App Purchase capability is enabled

### Issue: Sandbox purchases not working

**Solutions:**
- Sign out of production Apple ID first
- Use a valid sandbox tester account
- Clear App Store cache: Settings → App Store → Sign Out/In
- Ensure device region matches product availability

### Issue: StoreKit 2 compatibility

**Solutions:**
- Update to Xcode 14 or later
- Ensure minimum iOS deployment target is correct
- Check device/simulator iOS version (15.0+ for StoreKit 2)

## Advanced Configuration

### Receipt Validation

Always validate receipts on your server:

```kotlin
scope.launch {
    iapHelper.currentPurchase.collectLatest { purchase ->
        purchase?.let {
            // Send receipt to your server
            val validationResult = validateReceiptOnServer(
                receipt = it.purchaseToken ?: "",
                productId = it.productId
            )
            
            if (validationResult.isValid) {
                // Grant access to content
                grantPurchase(it)
            }
        }
    }
}
```

### Family Sharing

Enable family sharing for non-consumable purchases:

1. In App Store Connect, edit your in-app purchase
2. Enable **Family Sharing**
3. Submit for review

### Promotional Offers

Configure promotional offers for subscriptions:

```kotlin
// Request purchase with promotional offer
suspend fun purchaseWithOffer(iapHelper: UseIap) {
    iapHelper.requestSubscription(
        sku = "monthly_subscription",
        // Add promotional offer parameters if needed
    )
}
```

## Next Steps

Once iOS setup is complete:

1. **[Basic Implementation](/docs/guides/purchases)** - Start implementing purchases
2. **[Troubleshooting](/docs/guides/troubleshooting)** - Common issues and solutions

---

Need help? Check our [troubleshooting guide](/docs/guides/troubleshooting) or [open an issue](https://github.com/hyochan/kmp-iap/issues) on GitHub.