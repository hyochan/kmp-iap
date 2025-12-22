---
title: Alternative Billing Example
sidebar_label: Alternative Billing
sidebar_position: 4
---

import IapKitBanner from '@site/src/uis/IapKitBanner';

# Alternative Billing

<IapKitBanner />

Use alternative billing to redirect users to external payment systems or offer payment choices alongside platform billing.

View the full example source:

- GitHub: [AlternativeBillingScreen.kt](https://github.com/hyochan/kmp-iap/blob/main/example/composeApp/src/commonMain/kotlin/dev/hyo/martie/screens/AlternativeBillingScreen.kt)

## iOS - External Purchase URL

Redirect users to an external website for payment (iOS 16.0+):

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.ProductCommon
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun IOSAlternativeBilling(product: ProductCommon) {
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch {
                try {
                    val result = kmpIapInstance.presentExternalPurchaseLinkIOS(
                        url = "https://your-site.com/checkout?product=${product.id}"
                    )

                    if (result.success) {
                        // User was redirected to external website
                        println("Redirected to external checkout")
                    } else {
                        println("Error: ${result.error}")
                    }
                } catch (e: Exception) {
                    println("Failed to present external link: ${e.message}")
                }
            }
        }
    ) {
        Text("Buy (External URL)")
    }
}
```

### Important Notes

- **iOS 16.0+ Required**: External URLs only work on iOS 16.0 and later
- **Configuration Required**: External URLs must be configured in Info.plist (see [Alternative Billing Guide](../guides/alternative-billing.md))
- **No Callback**: `purchaseUpdatedListener` will NOT fire when using external URLs
- **Deep Linking**: Implement deep linking to return users to your app

## Android - Alternative Billing Only

Manual 3-step flow for alternative billing only:

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.ProductCommon
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AndroidAlternativeBillingOnly(product: ProductCommon) {
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch {
                try {
                    // Step 1: Check availability
                    val isAvailable = kmpIapInstance.checkAlternativeBillingAvailabilityAndroid()
                    if (!isAvailable) {
                        println("Alternative billing not available")
                        return@launch
                    }

                    // Step 2: Show information dialog
                    val userAccepted = kmpIapInstance.showAlternativeBillingDialogAndroid()
                    if (!userAccepted) {
                        println("User declined")
                        return@launch
                    }

                    // Step 2.5: Process payment with your payment system
                    // ... your payment processing logic here ...
                    println("Processing payment...")

                    // Step 3: Create reporting token (after successful payment)
                    val token = kmpIapInstance.createAlternativeBillingTokenAndroid()
                    println("Token created: $token")

                    // Step 4: Report token to Google Play backend within 24 hours
                    // reportToGoogleBackend(token)

                    println("Alternative billing completed")
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                }
            }
        }
    ) {
        Text("Buy (Alternative Only)")
    }
}
```

### Flow Steps

1. **Check availability** - Verify alternative billing is enabled
2. **Show info dialog** - Display Google's information dialog
3. **Process payment** - Handle payment with your system
4. **Create token** - Generate reporting token
5. **Report to Google** - Send token to Google within 24 hours

## Android - User Choice Billing

Let users choose between Google Play and alternative billing:

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.requestPurchase
import io.github.hyochan.kmpiap.openiap.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@Composable
fun AndroidUserChoiceBilling(product: ProductCommon) {
    val scope = rememberCoroutineScope()
    // Initialize with user choice mode
    LaunchedEffect(Unit) {
        val config = InitConnectionConfig(
            alternativeBillingModeAndroid = AlternativeBillingModeAndroid.UserChoice
        )
        kmpIapInstance.initConnection(config)

        // Listen for user choice events
        kmpIapInstance.userChoiceBillingListener.collect { details ->
            println("User selected alternative billing")
            println("Products: ${details.products}")
            // Handle alternative billing flow
        }
    }

    // Listen for Google Play purchases
    LaunchedEffect(Unit) {
        kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
            // Fires if user selects Google Play
            println("Google Play purchase: ${purchase.productId}")
        }
    }

    Button(
        onClick = {
            scope.launch {
                try {
                    // Google will show selection dialog automatically
                    kmpIapInstance.requestPurchase {
                        android {
                            skus = listOf(product.id)
                        }
                    }
                    // If user selects Google Play: purchaseUpdatedListener fires
                    // If user selects alternative: userChoiceBillingListener fires
                } catch (e: Exception) {
                    println("Purchase error: ${e.message}")
                }
            }
        }
    ) {
        Text("Buy (User Choice)")
    }
}
```

### Selection Dialog

- Google shows automatic selection dialog
- User chooses: Google Play (30% fee) or Alternative (lower fee)
- Different callbacks based on user choice

## Complete Cross-Platform Example

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.requestPurchase
import io.github.hyochan.kmpiap.fetchProducts
import io.github.hyochan.kmpiap.openiap.*
import kotlinx.coroutines.launch

@Composable
fun AlternativeBillingScreen() {
    val scope = rememberCoroutineScope()
    var billingMode by remember {
        mutableStateOf(AlternativeBillingModeAndroid.AlternativeOnly)
    }
    var products by remember { mutableStateOf<List<ProductCommon>>(emptyList()) }
    var connected by remember { mutableStateOf(false) }
    var externalUrl by remember { mutableStateOf("https://your-site.com") }

    // Initialize connection
    LaunchedEffect(billingMode) {
        val config = if (getPlatformName() == "Android") {
            InitConnectionConfig(alternativeBillingModeAndroid = billingMode)
        } else null

        connected = kmpIapInstance.initConnection(config)

        if (connected) {
            products = kmpIapInstance.fetchProducts {
                skus = listOf("premium_upgrade", "coins_100")
                type = ProductQueryType.InApp
            }
        }
    }

    // Listen for purchase events
    LaunchedEffect(Unit) {
        launch {
            kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                println("Purchase successful: ${purchase.productId}")
            }
        }

        launch {
            kmpIapInstance.purchaseErrorListener.collect { error ->
                println("Purchase error: ${error.message}")
            }
        }

        // Android user choice listener
        if (getPlatformName() == "Android") {
            launch {
                kmpIapInstance.userChoiceBillingListener.collect { details ->
                    println("User chose alternative billing: ${details.products}")
                }
            }
        }
    }

    // Platform-specific purchase handlers
    fun handleIOSPurchase(product: ProductCommon) {
        scope.launch {
            val result = kmpIapInstance.presentExternalPurchaseLinkIOS(
                url = "$externalUrl?product=${product.id}"
            )
            if (result.success) {
                println("Redirected to external checkout")
            } else {
                println("Error: ${result.error}")
            }
        }
    }

    fun handleAndroidAlternativeOnly(product: ProductCommon) {
        scope.launch {
            val isAvailable = kmpIapInstance.checkAlternativeBillingAvailabilityAndroid()
            if (!isAvailable) {
                println("Alternative billing not available")
                return@launch
            }

            val userAccepted = kmpIapInstance.showAlternativeBillingDialogAndroid()
            if (!userAccepted) return@launch

            // Process payment...
            val token = kmpIapInstance.createAlternativeBillingTokenAndroid()
            println("Token created: ${token?.substring(0, 20)}...")
        }
    }

    fun handleAndroidUserChoice(product: ProductCommon) {
        scope.launch {
            try {
                kmpIapInstance.requestPurchase {
                    android {
                        skus = listOf(product.id)
                    }
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    fun handlePurchase(product: ProductCommon) {
        when (getPlatformName()) {
            "iOS" -> handleIOSPurchase(product)
            "Android" -> {
                when (billingMode) {
                    AlternativeBillingModeAndroid.AlternativeOnly -> {
                        handleAndroidAlternativeOnly(product)
                    }
                    AlternativeBillingModeAndroid.UserChoice -> {
                        handleAndroidUserChoice(product)
                    }
                    else -> println("Alternative billing not configured")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Alternative Billing Demo", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // Platform indicator
        Text("Platform: ${getPlatformName()}")

        Spacer(modifier = Modifier.height(16.dp))

        // Android: Mode selector
        if (getPlatformName() == "Android") {
            Text("Billing Mode:", style = MaterialTheme.typography.titleMedium)
            Row {
                Button(
                    onClick = {
                        billingMode = AlternativeBillingModeAndroid.AlternativeOnly
                    }
                ) {
                    Text("Alternative Only")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        billingMode = AlternativeBillingModeAndroid.UserChoice
                    }
                ) {
                    Text("User Choice")
                }
            }
            Text("Current: $billingMode", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(16.dp))
        }

        // iOS: External URL input
        if (getPlatformName() == "iOS") {
            OutlinedTextField(
                value = externalUrl,
                onValueChange = { externalUrl = it },
                label = { Text("External URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Connection status
        Text(
            text = if (connected) "✓ Connected" else "Not connected",
            color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Products list
        Text("Products:", style = MaterialTheme.typography.titleMedium)

        products.forEach { product ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(product.title, style = MaterialTheme.typography.titleSmall)
                    Text(product.description, style = MaterialTheme.typography.bodySmall)
                    Text(product.displayPrice, style = MaterialTheme.typography.bodyLarge)

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { handlePurchase(product) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Purchase")
                    }
                }
            }
        }
    }
}

// Platform detection helper
expect fun getPlatformName(): String
```

**Platform-specific implementations:**

```kotlin
// androidMain/AlternativeBillingScreen.android.kt
actual fun getPlatformName(): String = "Android"
```

```kotlin
// iosMain/AlternativeBillingScreen.ios.kt
actual fun getPlatformName(): String = "iOS"
```

## Configuration

### Initialize with Alternative Billing Mode

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.AlternativeBillingModeAndroid
import io.github.hyochan.kmpiap.openiap.InitConnectionConfig

// Android with alternative billing
val config = InitConnectionConfig(
    alternativeBillingModeAndroid = AlternativeBillingModeAndroid.AlternativeOnly
    // Or: AlternativeBillingModeAndroid.UserChoice
    // Or: AlternativeBillingModeAndroid.None (default)
)

val connected = kmpIapInstance.initConnection(config)
```

### iOS Configuration (Info.plist)

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

### iOS Entitlements

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

## Event Listeners

### Purchase Updates (Google Play)

```kotlin
LaunchedEffect(Unit) {
    kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
        println("Purchase successful: ${purchase.productId}")

        // Deliver content
        deliverContent(purchase.productId)

        // Finish transaction
        kmpIapInstance.finishTransaction(
            purchase = purchase.toPurchaseInput(),
            isConsumable = true
        )
    }
}
```

### User Choice Billing (Android)

```kotlin
LaunchedEffect(Unit) {
    kmpIapInstance.userChoiceBillingListener.collect { details ->
        println("User selected alternative billing")
        println("Products: ${details.products}")

        // Process payment with your system
        processAlternativePayment(details.products)

        // Create and report token
        val token = kmpIapInstance.createAlternativeBillingTokenAndroid()
        if (token != null) {
            reportToGoogleBackend(token)
        }
    }
}
```

### Purchase Errors

```kotlin
LaunchedEffect(Unit) {
    kmpIapInstance.purchaseErrorListener.collect { error ->
        when (error.code) {
            "user_cancelled" -> println("User cancelled")
            else -> println("Error: ${error.message}")
        }
    }
}
```

## Testing

### iOS

- Test on iOS 16.0+ devices
- Verify external URL opens in Safari
- Test deep link return flow
- Check entitlements are properly configured

### Android

- Configure alternative billing in Google Play Console
- Test both modes separately (Alternative Only & User Choice)
- Verify token generation
- Test user choice dialog behavior

## Best Practices

1. **Backend Validation** - Always validate purchases on your server
2. **Clear UI** - Show users they're leaving the app (iOS)
3. **Error Handling** - Handle all error cases gracefully
4. **Token Reporting** - Report within 24 hours (Android)
5. **Deep Linking** - Essential for iOS return flow
6. **Mode Selection** - Choose appropriate mode for your use case (Android)

## Common Patterns

### Check if Alternative Billing is Available

```kotlin
suspend fun isAlternativeBillingSupported(): Boolean {
    return when (getPlatformName()) {
        "iOS" -> {
            // NOTE: This is a placeholder. A real implementation should check the OS version (iOS 16.0+).
            // You can use Platform.osVersion or similar APIs to check the iOS version.
            true
        }
        "Android" -> {
            kmpIapInstance.checkAlternativeBillingAvailabilityAndroid()
        }
        else -> false
    }
}
```

### Handle Alternative Billing Purchase Result

```kotlin
data class AlternativeBillingResult(
    val success: Boolean,
    val token: String?,
    val error: String?
)

suspend fun handleAlternativeBillingPurchase(
    product: ProductCommon
): AlternativeBillingResult {
    return when (getPlatformName()) {
        "iOS" -> {
            val result = kmpIapInstance.presentExternalPurchaseLinkIOS(
                url = "https://your-site.com/checkout?product=${product.id}"
            )
            AlternativeBillingResult(
                success = result.success,
                token = null,
                error = result.error
            )
        }
        "Android" -> {
            val isAvailable = kmpIapInstance.checkAlternativeBillingAvailabilityAndroid()
            if (!isAvailable) {
                return AlternativeBillingResult(
                    success = false,
                    token = null,
                    error = "Alternative billing not available"
                )
            }

            val userAccepted = kmpIapInstance.showAlternativeBillingDialogAndroid()
            if (!userAccepted) {
                return AlternativeBillingResult(
                    success = false,
                    token = null,
                    error = "User declined"
                )
            }

            // Process payment...
            val token = kmpIapInstance.createAlternativeBillingTokenAndroid()
            AlternativeBillingResult(
                success = token != null,
                token = token,
                error = if (token == null) "Failed to create token" else null
            )
        }
        else -> AlternativeBillingResult(
            success = false,
            token = null,
            error = "Platform not supported"
        )
    }
}
```

## Troubleshooting

### iOS: External URL not opening

- Verify iOS 16.0 or later
- Check entitlements are approved
- Ensure URLs are configured in Info.plist
- Verify URL format (must be valid HTTPS)

### Android: Alternative billing not available

- Verify Google Play approval
- Check device and Play Store version
- Ensure billing mode is configured
- Check Google Play Console settings

### Android: Token creation failed

- Verify billing mode configuration
- Ensure user completed info dialog
- Check Google Play Console settings

## See Also

- [Alternative Billing Guide](../guides/alternative-billing.md)
- [Core Methods - Alternative Billing APIs](../api/core-methods.md#ios-specific-alternative-billing)
- [Purchase Flow Example](./purchase-flow.md)
- [Error Handling](../guides/troubleshooting.md)
