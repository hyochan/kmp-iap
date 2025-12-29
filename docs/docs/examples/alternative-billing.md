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

## Android - Billing Programs

Select a billing program and handle purchases accordingly (v1.3.0+):

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.requestPurchase
import io.github.hyochan.kmpiap.openiap.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@Composable
fun AndroidBillingPrograms(product: ProductCommon) {
    val scope = rememberCoroutineScope()
    var billingProgram by remember { mutableStateOf(BillingProgramAndroid.ExternalOffer) }
    var showProgramSelector by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }

    // Initialize with selected billing program
    LaunchedEffect(billingProgram) {
        val config = InitConnectionConfig(
            enableBillingProgramAndroid = billingProgram
        )
        connected = kmpIapInstance.initConnection(config)
    }

    // Listen for purchase events
    LaunchedEffect(Unit) {
        // Google Play purchases (UserChoiceBilling, ExternalPayments)
        launch {
            kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                println("Google Play purchase: ${purchase.productId}")
            }
        }

        // User choice events (UserChoiceBilling)
        launch {
            kmpIapInstance.userChoiceBillingListener.collect { details ->
                println("User selected alternative billing: ${details.products}")
            }
        }

        // Developer provided billing (ExternalPayments - Japan only)
        launch {
            kmpIapInstance.developerProvidedBillingListener.collect { details ->
                println("Developer billing selected: ${details.externalTransactionToken}")
            }
        }
    }

    Column {
        // Billing Program Selector (Dropdown)
        Text("Billing Program:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showProgramSelector = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when (billingProgram) {
                        BillingProgramAndroid.ExternalOffer -> "External Offer (8.2.0+)"
                        BillingProgramAndroid.UserChoiceBilling -> "User Choice Billing (7.0+)"
                        BillingProgramAndroid.ExternalPayments -> "External Payments (8.3.0+, Japan)"
                        else -> "Select Program"
                    }
                )
                Text("▼", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Program Selector Dialog
        if (showProgramSelector) {
            AlertDialog(
                onDismissRequest = { showProgramSelector = false },
                title = { Text("Select Billing Program") },
                text = {
                    Column {
                        // Option 1: External Offer
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    billingProgram = BillingProgramAndroid.ExternalOffer
                                    showProgramSelector = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "External Offer (8.2.0+)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Only your payment system. Manual 3-step flow required.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option 2: User Choice Billing
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    billingProgram = BillingProgramAndroid.UserChoiceBilling
                                    showProgramSelector = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "User Choice Billing (7.0+)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Users choose between Google Play or your payment system.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Option 3: External Payments
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    billingProgram = BillingProgramAndroid.ExternalPayments
                                    showProgramSelector = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "External Payments (8.3.0+, Japan)",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "Side-by-side choice in purchase dialog. Japan users only.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProgramSelector = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Purchase button
        Button(
            onClick = {
                scope.launch {
                    when (billingProgram) {
                        BillingProgramAndroid.ExternalOffer -> {
                            // Manual 3-step flow
                            val isAvailable = kmpIapInstance.checkAlternativeBillingAvailabilityAndroid()
                            if (!isAvailable) return@launch

                            val userAccepted = kmpIapInstance.showAlternativeBillingDialogAndroid()
                            if (!userAccepted) return@launch

                            // Process payment with your system...
                            val token = kmpIapInstance.createAlternativeBillingTokenAndroid()
                            println("Token: $token")
                        }
                        BillingProgramAndroid.UserChoiceBilling -> {
                            // Google shows selection dialog
                            kmpIapInstance.requestPurchase {
                                google { skus = listOf(product.id) }
                            }
                        }
                        BillingProgramAndroid.ExternalPayments -> {
                            // Japan: Side-by-side choice in dialog
                            kmpIapInstance.requestPurchase {
                                google {
                                    skus = listOf(product.id)
                                    developerBillingOption = DeveloperBillingOptionParamsAndroid(
                                        billingProgram = BillingProgramAndroid.ExternalPayments,
                                        launchMode = DeveloperBillingLaunchModeAndroid.LaunchInExternalBrowserOrApp,
                                        linkUri = "https://your-site.com/checkout"
                                    )
                                }
                            }
                        }
                        else -> println("Select a billing program")
                    }
                }
            },
            enabled = connected
        ) {
            Text(
                when (billingProgram) {
                    BillingProgramAndroid.ExternalOffer -> "Buy (External Offer)"
                    BillingProgramAndroid.UserChoiceBilling -> "Buy (User Choice)"
                    BillingProgramAndroid.ExternalPayments -> "Buy (External Payments)"
                    else -> "Buy"
                }
            )
        }
    }
}
```

### Billing Program Options

| Program | Min Version | Description |
|---------|-------------|-------------|
| `ExternalOffer` | 8.2.0+ | Only your payment system. Manual 3-step flow required. |
| `UserChoiceBilling` | 7.0+ | Users choose between Google Play or your payment system via dialog. |
| `ExternalPayments` | 8.3.0+ | Side-by-side choice in purchase dialog. Japan users only. |

### Event Listeners by Program

| Program | `purchaseUpdatedListener` | `userChoiceBillingListener` | `developerProvidedBillingListener` |
|---------|---------------------------|-----------------------------|------------------------------------|
| `ExternalOffer` | ❌ | ❌ | ❌ |
| `UserChoiceBilling` | ✅ (Google Play) | ✅ (Alternative) | ❌ |
| `ExternalPayments` | ✅ (Google Play) | ❌ | ✅ (Developer billing) |

## Complete Cross-Platform Example

```kotlin
import androidx.compose.foundation.clickable
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
    var billingProgram by remember { mutableStateOf(BillingProgramAndroid.ExternalOffer) }
    var showProgramSelector by remember { mutableStateOf(false) }
    var products by remember { mutableStateOf<List<ProductCommon>>(emptyList()) }
    var connected by remember { mutableStateOf(false) }
    var externalUrl by remember { mutableStateOf("https://your-site.com") }

    // Initialize connection (v1.3.0+)
    LaunchedEffect(billingProgram) {
        val config = if (getPlatformName() == "Android") {
            InitConnectionConfig(enableBillingProgramAndroid = billingProgram)
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

        // Android listeners
        if (getPlatformName() == "Android") {
            launch {
                kmpIapInstance.userChoiceBillingListener.collect { details ->
                    println("User chose alternative billing: ${details.products}")
                }
            }
            launch {
                kmpIapInstance.developerProvidedBillingListener.collect { details ->
                    println("Developer billing: ${details.externalTransactionToken}")
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

    fun handleAndroidPurchase(product: ProductCommon) {
        scope.launch {
            when (billingProgram) {
                BillingProgramAndroid.ExternalOffer -> {
                    val isAvailable = kmpIapInstance.checkAlternativeBillingAvailabilityAndroid()
                    if (!isAvailable) return@launch

                    val userAccepted = kmpIapInstance.showAlternativeBillingDialogAndroid()
                    if (!userAccepted) return@launch

                    val token = kmpIapInstance.createAlternativeBillingTokenAndroid()
                    println("Token: $token")
                }
                BillingProgramAndroid.UserChoiceBilling -> {
                    kmpIapInstance.requestPurchase {
                        google { skus = listOf(product.id) }
                    }
                }
                BillingProgramAndroid.ExternalPayments -> {
                    kmpIapInstance.requestPurchase {
                        google {
                            skus = listOf(product.id)
                            developerBillingOption = DeveloperBillingOptionParamsAndroid(
                                billingProgram = BillingProgramAndroid.ExternalPayments,
                                launchMode = DeveloperBillingLaunchModeAndroid.LaunchInExternalBrowserOrApp,
                                linkUri = externalUrl
                            )
                        }
                    }
                }
                else -> println("Select a billing program")
            }
        }
    }

    fun handlePurchase(product: ProductCommon) {
        when (getPlatformName()) {
            "iOS" -> handleIOSPurchase(product)
            "Android" -> handleAndroidPurchase(product)
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

        // Android: Billing Program selector dropdown (v1.3.0+)
        if (getPlatformName() == "Android") {
            Text("Billing Program:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProgramSelector = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        when (billingProgram) {
                            BillingProgramAndroid.ExternalOffer -> "External Offer (8.2.0+)"
                            BillingProgramAndroid.UserChoiceBilling -> "User Choice Billing (7.0+)"
                            BillingProgramAndroid.ExternalPayments -> "External Payments (8.3.0+, Japan)"
                            else -> "Select Program"
                        }
                    )
                    Text("▼", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Program Selector Dialog
            if (showProgramSelector) {
                AlertDialog(
                    onDismissRequest = { showProgramSelector = false },
                    title = { Text("Select Billing Program") },
                    text = {
                        Column {
                            listOf(
                                Triple(BillingProgramAndroid.ExternalOffer, "External Offer (8.2.0+)", "Only your payment system. Manual 3-step flow."),
                                Triple(BillingProgramAndroid.UserChoiceBilling, "User Choice Billing (7.0+)", "Users choose Google Play or your system."),
                                Triple(BillingProgramAndroid.ExternalPayments, "External Payments (8.3.0+, Japan)", "Side-by-side choice. Japan only.")
                            ).forEach { (program, title, desc) ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            billingProgram = program
                                            showProgramSelector = false
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(title, style = MaterialTheme.typography.titleSmall)
                                        Text(desc, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showProgramSelector = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // External URL input (iOS and Android ExternalPayments)
        val needsExternalUrl = getPlatformName() == "iOS" ||
            (getPlatformName() == "Android" && billingProgram == BillingProgramAndroid.ExternalPayments)

        if (needsExternalUrl) {
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
                        Text(
                            when {
                                getPlatformName() == "iOS" -> "Buy (External URL)"
                                billingProgram == BillingProgramAndroid.ExternalOffer -> "Buy (External Offer)"
                                billingProgram == BillingProgramAndroid.UserChoiceBilling -> "Buy (User Choice)"
                                billingProgram == BillingProgramAndroid.ExternalPayments -> "Buy (External Payments)"
                                else -> "Purchase"
                            }
                        )
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

### Initialize with Billing Program (v1.3.0+)

```kotlin
import io.github.hyochan.kmpiap.kmpIapInstance
import io.github.hyochan.kmpiap.openiap.BillingProgramAndroid
import io.github.hyochan.kmpiap.openiap.InitConnectionConfig

// Android with billing program
val config = InitConnectionConfig(
    enableBillingProgramAndroid = BillingProgramAndroid.ExternalOffer
    // Or: BillingProgramAndroid.UserChoiceBilling (7.0+)
    // Or: BillingProgramAndroid.ExternalPayments (8.3.0+, Japan only)
    // Or: BillingProgramAndroid.Unspecified (default)
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

- Configure billing program in Google Play Console
- Test each billing program separately (ExternalOffer, UserChoiceBilling, ExternalPayments)
- Verify token generation and reporting
- Test dialog behavior for each program type

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
