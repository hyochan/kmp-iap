---
sidebar_position: 2
title: Subscription Store
---

import GreatFrontEndBanner from '@site/src/uis/GreatFrontEndBanner';

# Subscription Store Example

<GreatFrontEndBanner />

A complete subscription store implementation with monthly and yearly plans using kmp-iap.

## Features

- Multiple subscription tiers
- Subscription status display
- Automatic renewal handling
- Restore purchases
- Grace period support

## Complete Implementation

```kotlin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hyochan.kmpiap.KmpIAP
import io.github.hyochan.kmpiap.data.*
import io.github.hyochan.kmpiap.openiap.IapPlatform
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

class SubscriptionStoreViewModel : ViewModel() {
    
    // State management
    data class SubscriptionState(
        val isConnected: Boolean = false,
        val isLoading: Boolean = false,
        val subscriptions: List<Product> = emptyList(),
        val activeSubscriptions: List<ActiveSubscription> = emptyList(),
        val hasActiveSubscription: Boolean = false,
        val error: String? = null
    )
    
    private val _state = MutableStateFlow(SubscriptionState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()
    
    // Your subscription IDs
    private val subscriptionIds = listOf(
        "premium_monthly",
        "premium_yearly",
        "pro_monthly",
        "pro_yearly"
    )
    
    init {
        initializeStore()
        observeStates()
    }
    
    private fun initializeStore() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                kmpIapInstance.initConnection()
                loadSubscriptions()
            } catch (e: PurchaseError) {
                showError("Failed to initialize store: ${e.message}")
            }
        }
    }
    
    private fun observeStates() {
        // Observe connection
        viewModelScope.launch {
            KmpIAP.isConnected.collectLatest { connected ->
                _state.update { it.copy(isConnected = connected) }
            }
        }
        
        // Observe subscriptions
        viewModelScope.launch {
            KmpIAP.subscriptions.collectLatest { subs ->
                _state.update { it.copy(subscriptions = subs) }
            }
        }
        
        // Observe active subscriptions using new API
        viewModelScope.launch {
            // Check for active subscriptions periodically or when state changes
            loadActiveSubscriptions()
        }
        
        // Observe purchase updates
        viewModelScope.launch {
            KmpIAP.currentPurchase.collectLatest { purchase ->
                purchase?.let { handlePurchaseUpdate(it) }
            }
        }
        
        // Observe errors
        viewModelScope.launch {
            KmpIAP.currentError.collectLatest { error ->
                error?.let {
                    handlePurchaseError(it)
                    KmpIAP.clearError()
                }
            }
        }
    }
    
    private suspend fun loadSubscriptions() {
        _state.update { it.copy(isLoading = true, error = null) }

        try {
            val subscriptions = kmpIapInstance.fetchProducts {
                skus = subscriptionIds
                type = ProductQueryType.Subs
            }
            println("Loaded ${subscriptions.size} subscriptions")

            // Also load active subscriptions
            loadActiveSubscriptions()
        } catch (e: PurchaseError) {
            showError("Failed to load subscriptions: ${e.message}")
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }
    
    private suspend fun loadActiveSubscriptions() {
        try {
            // Use new APIs to get active subscription information
            val activeSubscriptions = kmpIapInstance.getActiveSubscriptions(subscriptionIds)
            val hasActiveSubscription = kmpIapInstance.hasActiveSubscriptions(subscriptionIds)
            
            _state.update { 
                it.copy(
                    activeSubscriptions = activeSubscriptions,
                    hasActiveSubscription = hasActiveSubscription
                ) 
            }
            
            println("Found ${activeSubscriptions.size} active subscriptions")
            activeSubscriptions.forEach { subscription ->
                println("Active subscription: ${subscription.productId}")
                
                // iOS-specific information
                subscription.expirationDateIOS?.let { expDate ->
                    val expirationDate = Instant.fromEpochMilliseconds(expDate)
                    println("  Expires: $expirationDate")
                }
                subscription.environmentIOS?.let { env ->
                    println("  Environment: $env")
                }
                subscription.daysUntilExpirationIOS?.let { days ->
                    println("  Days until expiration: $days")
                }
                
                // Android-specific information
                subscription.autoRenewingAndroid?.let { autoRenew ->
                    println("  Auto-renewing: $autoRenew")
                }
                
                // Cross-platform warnings
                if (subscription.willExpireSoon == true) {
                    println("  ⚠️ This subscription will expire soon!")
                }
            }
        } catch (e: Exception) {
            showError("Failed to load active subscriptions: ${e.message}")
        }
    }
    
    private suspend fun handlePurchaseUpdate(purchase: Purchase) {
        println("Purchase update: ${purchase.productId}")
        
        try {
            // Verify purchase on your server
            val isValid = verifyPurchase(purchase)
            
            if (isValid) {
                // Deliver subscription access
                deliverSubscription(purchase)
                
                // Complete transaction
                completeTransaction(purchase)
                
                // Reload active subscriptions to reflect changes
                loadActiveSubscriptions()
                
                // Clear current purchase
                KmpIAP.clearPurchase()
                
                showMessage("Subscription activated!")
            } else {
                showError("Purchase verification failed")
            }
        } catch (e: Exception) {
            showError("Failed to process purchase: ${e.message}")
        }
    }
    
    private fun handlePurchaseError(error: PurchaseError) {
        when (error.code) {
            ErrorCode.UserCancelled -> {
                // Silent - user cancelled
            }
            ErrorCode.AlreadyOwned -> {
                showMessage("You already have an active subscription")
            }
            else -> {
                showError("Purchase failed: ${error.message}")
            }
        }
    }
    
    fun requestSubscription(productId: String) {
        viewModelScope.launch {
            try {
                // For Android, you might want to handle subscription offers
                if (kmpIapInstance.getPlatform() == IapPlatform.Android) {
                    // Get available offers for the subscription
                    val product = _state.value.subscriptions.find { it.productId == productId }
                    val offers = product?.subscriptionOffers

                    if (!offers.isNullOrEmpty()) {
                        // Use the first offer (you might want to let user choose)
                        kmpIapInstance.requestPurchase {
                            ios { sku = productId }
                            android {
                                skus = listOf(productId)
                                subscriptionOffers = listOf(
                                    SubscriptionOfferAndroid(
                                        sku = productId,
                                        offerToken = offers.first().offerToken
                                    )
                                )
                            }
                        }
                    } else {
                        kmpIapInstance.requestPurchase {
                            ios { sku = productId }
                            android { skus = listOf(productId) }
                        }
                    }
                } else {
                    // iOS doesn't need offer tokens
                    kmpIapInstance.requestPurchase {
                        ios { sku = productId }
                        android { skus = listOf(productId) }
                    }
                }
            } catch (e: PurchaseError) {
                showError("Failed to request subscription: ${e.message}")
            }
        }
    }
    
    fun restorePurchases() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Get available purchases - this automatically refreshes
                val purchases = KmpIAP.availablePurchases.value
                val activeCount = purchases.count { 
                    subscriptionIds.contains(it.productId) && isSubscriptionActive(it)
                }
                
                showMessage("Restored $activeCount active subscriptions")
            } catch (e: PurchaseError) {
                showError("Failed to restore purchases: ${e.message}")
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
    
    private suspend fun verifyPurchase(purchase: Purchase): Boolean {
        // TODO: Implement server-side verification
        // This should verify the receipt with your backend
        return true
    }
    
    private fun deliverSubscription(purchase: Purchase) {
        // TODO: Grant subscription access to user
        println("Delivering subscription: ${purchase.productId}")
    }
    
    private suspend fun completeTransaction(purchase: Purchase) {
        val success = kmpIapInstance.finishTransaction(
            purchase.toPurchaseInput(),
            isConsumable = false // Subscriptions are non-consumable
        )

        if (success) {
            println("Transaction completed successfully")
        }
    }
    
    private fun isSubscriptionActive(purchase: Purchase): Boolean {
        // For a real implementation, check expiration date
        // This is a simplified version
        return when (purchase.purchaseState) {
            PurchaseState.PURCHASED -> true
            PurchaseState.PENDING -> true // Show as active during pending
            else -> false
        }
    }
    
    fun getSubscriptionTier(productId: String): String {
        return when {
            productId.contains("premium") -> "Premium"
            productId.contains("pro") -> "Pro"
            else -> "Basic"
        }
    }
    
    fun getSubscriptionPeriod(productId: String): String {
        return when {
            productId.contains("monthly") -> "Monthly"
            productId.contains("yearly") -> "Yearly"
            else -> ""
        }
    }
    
    fun getTierColor(tier: String): Color {
        return when (tier.lowercase()) {
            "premium" -> Color(0xFFFFA726) // Orange
            "pro" -> Color(0xFF9C27B0) // Purple
            else -> Color(0xFF2196F3) // Blue
        }
    }
    
    private fun showError(message: String) {
        _state.update { it.copy(error = message) }
    }
    
    private fun showMessage(message: String) {
        // In a real app, show a snackbar
        println("ℹ️ $message")
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    override fun onCleared() {
        super.onCleared()
        kmpIapInstance.endConnection()
    }
}

@Composable
fun SubscriptionStoreScreen(
    viewModel: SubscriptionStoreViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium Subscriptions") },
                actions = {
                    IconButton(
                        onClick = viewModel::restorePurchases,
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = "Restore Purchases"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading && state.subscriptions.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    SubscriptionPlans(
                        state = state,
                        viewModel = viewModel
                    )
                }
            }
            
            // Error banner
            state.error?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        IconButton(onClick = viewModel::clearError) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionPlans(
    state: SubscriptionStoreViewModel.SubscriptionState,
    viewModel: SubscriptionStoreViewModel
) {
    // Group subscriptions by tier
    val groupedSubs = state.subscriptions.groupBy { 
        viewModel.getSubscriptionTier(it.productId) 
    }
    
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current Status
        item {
            CurrentSubscriptionStatus(
                activeSubscriptions = state.activeSubscriptions,
                hasActiveSubscription = state.hasActiveSubscription,
                viewModel = viewModel
            )
        }
        
        // Subscription Plans Header
        item {
            Text(
                "Choose Your Plan",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Subscription Tiers
        groupedSubs.forEach { (tier, subscriptions) ->
            item {
                SubscriptionTierSection(
                    tier = tier,
                    subscriptions = subscriptions,
                    activeSubscriptions = state.activeSubscriptions,
                    hasActiveSubscription = state.hasActiveSubscription,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun CurrentSubscriptionStatus(
    activeSubscriptions: List<ActiveSubscription>,
    hasActiveSubscription: Boolean,
    viewModel: SubscriptionStoreViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (!hasActiveSubscription)
                MaterialTheme.colorScheme.surfaceVariant
            else
                Color(0xFF4CAF50).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (!hasActiveSubscription)
                    Icons.Default.Info
                else
                    Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (!hasActiveSubscription)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    Color(0xFF4CAF50)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (!hasActiveSubscription)
                        "No active subscriptions"
                    else
                        "Active Subscriptions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                activeSubscriptions.forEach { sub ->
                    Column {
                        Text(
                            text = "${viewModel.getSubscriptionTier(sub.productId)} ${viewModel.getSubscriptionPeriod(sub.productId)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4CAF50)
                        )
                        
                        // Show expiration information
                        sub.expirationDateIOS?.let { expDate ->
                            val expirationDate = Instant.fromEpochMilliseconds(expDate)
                            Text(
                                text = "Expires: ${expirationDate.toLocalDateTime(TimeZone.currentSystemDefault()).date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Show environment (iOS)
                        sub.environmentIOS?.let { env ->
                            if (env == "Sandbox") {
                                Text(
                                    text = "🧪 Sandbox",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                        
                        // Show expiration warning
                        if (sub.willExpireSoon == true) {
                            Text(
                                text = "⚠️ Expires soon",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF5722)
                            )
                        }
                        
                        // Show auto-renewal status (Android)
                        sub.autoRenewingAndroid?.let { autoRenew ->
                            Text(
                                text = if (autoRenew) "🔄 Auto-renewing" else "⏸️ Will not renew",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (autoRenew) Color(0xFF4CAF50) else Color(0xFFFF5722)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionTierSection(
    tier: String,
    subscriptions: List<Product>,
    activeSubscriptions: List<ActiveSubscription>,
    hasActiveSubscription: Boolean,
    viewModel: SubscriptionStoreViewModel
) {
    val tierColor = viewModel.getTierColor(tier)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column {
            // Tier Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = tierColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = tier,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = tierColor
                )
            }
            
            // Subscription Options
            subscriptions.forEach { subscription ->
                SubscriptionTile(
                    subscription = subscription,
                    isActive = activeSubscriptions.any { 
                        it.productId == subscription.productId 
                    },
                    tierColor = tierColor,
                    onSubscribe = { viewModel.requestSubscription(subscription.productId) }
                )
                
                if (subscription != subscriptions.last()) {
                    Divider()
                }
            }
        }
    }
}

@Composable
fun SubscriptionTile(
    subscription: Product,
    isActive: Boolean,
    tierColor: Color,
    onSubscribe: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(subscription.title)
        },
        supportingContent = {
            subscription.description?.let { Text(it) }
        },
        trailingContent = {
            if (isActive) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = Color(0xFF4CAF50),
                    contentColor = Color.White
                ) {
                    Text(
                        text = "ACTIVE",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onSubscribe,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tierColor
                    )
                ) {
                    Text(subscription.price)
                }
            }
        }
    )
}
```

## Key Features Explained

### 1. Subscription Grouping

The store groups subscriptions by tier (Premium, Pro) for better organization:

```kotlin
val groupedSubs = state.subscriptions.groupBy { 
    viewModel.getSubscriptionTier(it.productId) 
}
```

### 2. Status Display

Shows current subscription status prominently:

```kotlin
@Composable
fun CurrentSubscriptionStatus(
    activeSubscriptions: List<Purchase>,
    viewModel: SubscriptionStoreViewModel
) {
    // Visual indication of active subscriptions
    // Different colors and icons for active vs inactive
}
```

### 3. Visual Hierarchy

Different colors and styling for different subscription tiers:

```kotlin
fun getTierColor(tier: String): Color {
    return when (tier.lowercase()) {
        "premium" -> Color(0xFFFFA726) // Orange
        "pro" -> Color(0xFF9C27B0) // Purple
        else -> Color(0xFF2196F3) // Blue
    }
}
```

### 4. Android Subscription Offers

Handles Android subscription offers properly:

```kotlin
if (kmpIapInstance.getPlatform() == IapPlatform.Android) {
    val offers = product?.subscriptionOffers
    if (!offers.isNullOrEmpty()) {
        kmpIapInstance.requestPurchase {
            ios { sku = productId }
            android {
                skus = listOf(productId)
                subscriptionOffers = listOf(
                    SubscriptionOfferAndroid(
                        sku = productId,
                        offerToken = offers.first().offerToken
                    )
                )
            }
        }
    }
}
```

## Best Practices Implemented

1. **Error Handling**: Comprehensive error handling with user-friendly messages
2. **Loading States**: Shows loading indicators during async operations
3. **Purchase Verification**: Placeholder for server-side verification
4. **Transaction Completion**: Proper handling of iOS and Android differences
5. **Restore Functionality**: Easy way for users to restore purchases
6. **Status Display**: Clear indication of active subscriptions with detailed information
7. **StateFlow Usage**: Reactive state management with Kotlin StateFlow
8. **Enhanced Subscription Management**: Uses new `getActiveSubscriptions()` and `hasActiveSubscriptions()` APIs
9. **Platform-Specific Details**: Shows expiration dates (iOS), auto-renewal status (Android), and environment info

## Enhanced Subscription Management Features

### Using New ActiveSubscription APIs

The example now uses the enhanced subscription APIs for more detailed information:

```kotlin
// Check if user has any active subscriptions (quick check)
val hasActiveSubscription = kmpIapInstance.hasActiveSubscriptions(subscriptionIds)

// Get detailed subscription information
val activeSubscriptions = kmpIapInstance.getActiveSubscriptions(subscriptionIds)

activeSubscriptions.forEach { subscription ->
    println("Active subscription: ${subscription.productId}")
    
    // iOS-specific information
    subscription.expirationDateIOS?.let { expDate ->
        val expirationDate = Instant.fromEpochMilliseconds(expDate)
        println("  Expires: $expirationDate")
    }
    subscription.environmentIOS?.let { env ->
        println("  Environment: $env") // "Sandbox" or "Production"
    }
    subscription.daysUntilExpirationIOS?.let { days ->
        println("  Days until expiration: $days")
    }
    
    // Android-specific information
    subscription.autoRenewingAndroid?.let { autoRenew ->
        println("  Auto-renewing: $autoRenew")
    }
    
    // Cross-platform warnings
    if (subscription.willExpireSoon == true) {
        println("  ⚠️ This subscription will expire soon!")
    }
}
```

### Platform-Specific Subscription Details

#### iOS Features
- **Expiration Date**: Exact timestamp when subscription expires
- **Environment Detection**: Automatically detects Sandbox vs Production
- **Days Until Expiration**: Calculated remaining days
- **Expiration Warnings**: `willExpireSoon` flag for subscriptions expiring within 7 days

#### Android Features
- **Auto-Renewal Status**: Shows if subscription will automatically renew
- **Grace Period Support**: Can be extended with server-side validation

### Visual Subscription Status

The UI now shows rich subscription information:

```kotlin
// Show expiration information
sub.expirationDateIOS?.let { expDate ->
    val expirationDate = Instant.fromEpochMilliseconds(expDate)
    Text(
        text = "Expires: ${expirationDate.toLocalDateTime(TimeZone.currentSystemDefault()).date}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// Show environment (iOS)
sub.environmentIOS?.let { env ->
    if (env == "Sandbox") {
        Text(
            text = "🧪 Sandbox",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFF9800)
        )
    }
}

// Show expiration warning
if (sub.willExpireSoon == true) {
    Text(
        text = "⚠️ Expires soon",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFFF5722)
    )
}

// Show auto-renewal status (Android)
sub.autoRenewingAndroid?.let { autoRenew ->
    Text(
        text = if (autoRenew) "🔄 Auto-renewing" else "⏸️ Will not renew",
        style = MaterialTheme.typography.bodySmall,
        color = if (autoRenew) Color(0xFF4CAF50) else Color(0xFFFF5722)
    )
}
```

### Legacy Subscription Status Check

For backward compatibility, you can still use the basic check:

```kotlin
private fun isSubscriptionActive(purchase: Purchase): Boolean {
    // In production, check actual expiration date
    return when (purchase.purchaseState) {
        PurchaseState.PURCHASED -> true
        PurchaseState.PENDING -> true // Show as active during pending
        else -> false
    }
}
```

### Handling Grace Periods

For production apps, implement grace period handling:

```kotlin
private fun checkSubscriptionWithGracePeriod(purchase: Purchase): SubscriptionStatus {
    val expirationDate = purchase.expirationDate ?: return SubscriptionStatus.EXPIRED
    val now = Clock.System.now()
    
    return when {
        now < expirationDate -> SubscriptionStatus.ACTIVE
        now < expirationDate.plus(3.days) -> SubscriptionStatus.IN_GRACE_PERIOD
        else -> SubscriptionStatus.EXPIRED
    }
}
```

## Testing Considerations

- Test with different subscription tiers
- Test restoration on device reinstall
- Test subscription expiration handling
- Test grace period scenarios
- Test with different payment methods
- Test subscription upgrades/downgrades

## Platform-Specific Features

### iOS Promotional Offers

```kotlin
// iOS specific - check for promotional offers
if (kmpIapInstance.getPlatform() == IapPlatform.Ios) {
    viewModelScope.launch {
        kmpIapInstance.promotedProductIOS.collectLatest { promotedProduct ->
            // Handle App Store promoted product
            promotedProduct?.let {
                // Show promoted product UI
            }
        }
    }
}
```

### Android Subscription Management

```kotlin
// Android specific - deep link to subscription management
suspend fun openSubscriptionManagement(productId: String) {
    if (kmpIapInstance.getPlatform() == IapPlatform.Android) {
        kmpIapInstance.deepLinkToSubscriptions(productId)
    }
}
```

This example provides a solid foundation for a subscription-based app with multiple tiers and billing periods using Kotlin Multiplatform.
