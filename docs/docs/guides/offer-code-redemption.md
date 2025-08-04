---
sidebar_position: 8
title: Offer Code Redemption
---

# Offer Code Redemption

Guide to implementing promotional offer codes and subscription management with kmp-iap v1.0.0, covering iOS and Android platforms.

## Overview

This library provides native support for:

- **iOS**: Offer code redemption sheet and subscription management (iOS 14+)
- **Android**: Deep linking to subscription management
- **Cross-platform**: StateFlow-based state management for offers and subscriptions

## iOS Offer Code Redemption

### Present Code Redemption Sheet

```kotlin
import kotlinx.coroutines.*
import io.github.hyochan.kmpiap.useIap.*
import io.github.hyochan.kmpiap.IAPPlatform
import io.github.hyochan.kmpiap.getCurrentPlatform

class OfferCodeHandler(
    private val scope: CoroutineScope
) {
    private val iapHelper = UseIap(scope, UseIapOptions())
    
    /**
     * Present iOS system offer code redemption sheet (iOS 14+)
     */
    suspend fun presentOfferCodeRedemption() {
        if (getCurrentPlatform() != IAPPlatform.IOS) {
            println("Offer code redemption is only available on iOS")
            return
        }
        
        try {
            // Present the system offer code redemption sheet
            iapHelper.presentCodeRedemptionSheetIOS()
            println("Offer code redemption sheet presented")
            
            // Results will come through currentPurchase StateFlow
            listenForRedemptionResults()
            
        } catch (e: PurchaseError) {
            println("Failed to present offer code sheet: $e")
        }
    }
    
    private fun listenForRedemptionResults() {
        scope.launch {
            iapHelper.currentPurchase.collectLatest { purchase ->
                purchase?.let {
                    println("Offer code redeemed: ${it.productId}")
                    // Handle successful redemption
                    handleRedeemedPurchase(it)
                }
            }
        }
    }
    
    private suspend fun handleRedeemedPurchase(purchase: Purchase) {
        // Process the redeemed purchase
        // Verify receipt, deliver content, etc.
        val success = iapHelper.finishTransaction(
            purchase = purchase,
            isConsumable = false
        )
        
        if (success) {
            println("Redeemed purchase processed successfully")
        }
        
        // Clear the purchase state
        iapHelper.clearPurchase()
    }
}
```

### Storefront Information

```kotlin
class StorefrontHandler(
    private val iapHelper: UseIap
) {
    /**
     * Get App Store storefront information (iOS only)
     */
    suspend fun getStorefrontInfo(): Map<String, Any?>? {
        if (getCurrentPlatform() != IAPPlatform.IOS) return null
        
        return try {
            val storefront = iapHelper.getStorefrontIOS()
            println("Storefront info: $storefront")
            storefront
        } catch (e: PurchaseError) {
            println("Failed to get storefront info: $e")
            null
        }
    }
    
    /**
     * Get the current store type
     */
    fun getCurrentStore(): Store {
        return iapHelper.getStore()
    }
}
```

## Subscription Management

### iOS Subscription Management

```kotlin
class SubscriptionManager(
    private val scope: CoroutineScope
) {
    private val iapHelper = UseIap(scope, UseIapOptions())
    
    /**
     * Show iOS subscription management screen (iOS 15+)
     */
    suspend fun showManageSubscriptions() {
        if (getCurrentPlatform() != IAPPlatform.IOS) {
            println("Subscription management is only available on iOS")
            return
        }
        
        try {
            iapHelper.showManageSubscriptionsIOS()
            println("Subscription management screen presented")
        } catch (e: PurchaseError) {
            println("Failed to show subscription management: $e")
        }
    }
    
    /**
     * Monitor subscription state changes
     */
    fun observeSubscriptions() {
        scope.launch {
            iapHelper.subscriptions.collectLatest { subscriptions ->
                println("Active subscriptions: ${subscriptions.size}")
                subscriptions.forEach { sub ->
                    println("Subscription: ${sub.productId}")
                    println("Period: ${sub.subscriptionPeriod}")
                    println("Price: ${sub.price}")
                }
            }
        }
    }
}
```

## Android Subscription Management

### Deep Linking to Subscriptions

```kotlin
class AndroidSubscriptionManager(
    private val iapHelper: UseIap
) {
    /**
     * Open Android subscription management (deep link to Play Store)
     */
    suspend fun openSubscriptionManagement(productId: String? = null) {
        if (getCurrentPlatform() != IAPPlatform.ANDROID) {
            println("Android subscription management is only available on Android")
            return
        }
        
        try {
            // Deep link to subscription management in Play Store
            productId?.let {
                iapHelper.deepLinkToSubscriptionsAndroid(it)
            } ?: run {
                // Open general subscription management
                iapHelper.deepLinkToSubscriptionsAndroid("")
            }
            
            println("Opened Android subscription management")
        } catch (e: PurchaseError) {
            println("Failed to open subscription management: $e")
        }
    }
    
    /**
     * Get purchase history including subscriptions
     */
    suspend fun getSubscriptionHistory() {
        if (getCurrentPlatform() != IAPPlatform.ANDROID) return
        
        try {
            iapHelper.requestPurchaseHistoryAndroid()
            
            // Results available via StateFlow
            iapHelper.purchaseHistories.collectLatest { history ->
                val subscriptions = history.filter { 
                    it.productId.contains("subscription") || 
                    it.productId.contains("monthly") ||
                    it.productId.contains("yearly")
                }
                println("Found ${subscriptions.size} subscriptions in history")
            }
        } catch (e: PurchaseError) {
            println("Failed to get subscription history: $e")
        }
    }
}
```

## Complete Implementation Example

### Cross-Platform Offer Handler

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*

class CrossPlatformOfferViewModel : ViewModel() {
    private val iapHelper = UseIap(
        scope = viewModelScope,
        options = UseIapOptions()
    )
    
    data class OfferState(
        val isLoading: Boolean = false,
        val canRedeemCode: Boolean = false,
        val activeSubscriptions: List<Product> = emptyList(),
        val promotedProducts: List<Product>? = null,
        val error: String? = null
    )
    
    private val _state = MutableStateFlow(OfferState())
    val state: StateFlow<OfferState> = _state.asStateFlow()
    
    init {
        observeStates()
        checkPlatformCapabilities()
    }
    
    private fun observeStates() {
        // Observe subscriptions
        viewModelScope.launch {
            iapHelper.subscriptions.collectLatest { subs ->
                _state.update { it.copy(activeSubscriptions = subs) }
            }
        }
        
        // Observe promoted products (iOS)
        viewModelScope.launch {
            iapHelper.promotedProductsIOS.collectLatest { promoted ->
                _state.update { it.copy(promotedProducts = promoted) }
            }
        }
    }
    
    private fun checkPlatformCapabilities() {
        val canRedeem = getCurrentPlatform() == IAPPlatform.IOS
        _state.update { it.copy(canRedeemCode = canRedeem) }
    }
    
    /**
     * Present offer code redemption (iOS) or subscription management (Android)
     */
    suspend fun handleOfferRedemption() {
        _state.update { it.copy(isLoading = true, error = null) }
        
        try {
            when (getCurrentPlatform()) {
                IAPPlatform.IOS -> {
                    // iOS: Present code redemption sheet
                    iapHelper.presentCodeRedemptionSheetIOS()
                    println("iOS offer code redemption sheet presented")
                    listenForPurchases()
                }
                IAPPlatform.ANDROID -> {
                    // Android: Open subscription management
                    iapHelper.deepLinkToSubscriptionsAndroid("")
                    println("Android subscription management opened")
                }
            }
        } catch (e: PurchaseError) {
            _state.update { 
                it.copy(
                    isLoading = false,
                    error = "Failed to handle offer redemption: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Show subscription management UI
     */
    suspend fun showSubscriptionManagement() {
        try {
            when (getCurrentPlatform()) {
                IAPPlatform.IOS -> {
                    iapHelper.showManageSubscriptionsIOS()
                }
                IAPPlatform.ANDROID -> {
                    // For Android, deep link to the first active subscription
                    val firstSub = _state.value.activeSubscriptions.firstOrNull()
                    firstSub?.let {
                        iapHelper.deepLinkToSubscriptionsAndroid(it.productId)
                    }
                }
            }
        } catch (e: PurchaseError) {
            _state.update { 
                it.copy(error = "Failed to open subscription management: ${e.message}")
            }
        }
    }
    
    private fun listenForPurchases() {
        viewModelScope.launch {
            iapHelper.currentPurchase.collectLatest { purchase ->
                purchase?.let {
                    println("Purchase received: ${it.productId}")
                    handlePurchaseSuccess(it)
                }
            }
        }
    }
    
    private suspend fun handlePurchaseSuccess(purchase: Purchase) {
        // Deliver content
        deliverContent(purchase.productId)
        
        // Finish transaction
        iapHelper.finishTransaction(
            purchase = purchase,
            isConsumable = false
        )
        
        // Clear purchase state
        iapHelper.clearPurchase()
        
        _state.update { it.copy(isLoading = false) }
    }
    
    override fun onCleared() {
        super.onCleared()
        iapHelper.dispose()
    }
}
```

## Additional Features

### Platform-Specific Helpers

```kotlin
class PlatformSpecificFeatures(
    private val iapHelper: UseIap,
    private val scope: CoroutineScope
) {
    /**
     * iOS: Get promoted products from App Store
     */
    fun observePromotedProducts() {
        if (getCurrentPlatform() != IAPPlatform.IOS) return
        
        scope.launch {
            iapHelper.promotedProductsIOS.collectLatest { products ->
                products?.forEach { product ->
                    println("Promoted product: ${product.productId}")
                    println("Price: ${product.price}")
                }
            }
        }
    }
    
    /**
     * Android: Handle subscription with specific offer
     */
    suspend fun purchaseSubscriptionWithOffer(
        productId: String,
        offerToken: String
    ) {
        if (getCurrentPlatform() != IAPPlatform.ANDROID) return
        
        try {
            iapHelper.requestSubscription(
                sku = productId,
                subscriptionOffers = listOf(
                    SubscriptionOfferAndroid(
                        sku = productId,
                        offerToken = offerToken
                    )
                )
            )
        } catch (e: PurchaseError) {
            println("Failed to purchase with offer: $e")
        }
    }
}
```

## Usage Examples

### In a Compose UI

```kotlin
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*

@Composable
fun OfferRedemptionScreen(
    viewModel: CrossPlatformOfferViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redeem Offers") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator()
                }
                
                state.error != null -> {
                    ErrorMessage(
                        message = state.error,
                        onRetry = {
                            scope.launch {
                                viewModel.handleOfferRedemption()
                            }
                        }
                    )
                }
                
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (getCurrentPlatform()) {
                            IAPPlatform.IOS -> {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            viewModel.handleOfferRedemption()
                                        }
                                    }
                                ) {
                                    Text("Redeem Offer Code")
                                }
                                
                                if (state.promotedProducts?.isNotEmpty() == true) {
                                    Text(
                                        "Promoted products available!",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            
                            IAPPlatform.ANDROID -> {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            viewModel.handleOfferRedemption()
                                        }
                                    }
                                ) {
                                    Text("Manage Subscriptions")
                                }
                            }
                        }
                        
                        if (state.activeSubscriptions.isNotEmpty()) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.showSubscriptionManagement()
                                    }
                                }
                            ) {
                                Text("View Active Subscriptions")
                            }
                            
                            Text(
                                "Active: ${state.activeSubscriptions.size} subscriptions",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
```

## Important Notes

### Platform Differences

- **iOS**: Full support for offer code redemption through system sheet (iOS 14+)
- **Android**: No direct promo code API - users must redeem through Play Store
- **Subscription Management**: Both platforms support opening native subscription management

### Requirements

- **iOS**: Minimum iOS 14.0 for offer code redemption
- **iOS**: Minimum iOS 15.0 for subscription management  
- **Android**: Requires Google Play Billing Library 7.x+

### Best Practices

1. Always check platform before calling platform-specific methods
2. Handle errors gracefully as native dialogs may fail
3. Monitor purchase StateFlow when presenting offer code redemption
4. Use subscription management for user convenience
5. Validate redeemed purchases server-side
6. Clear purchase state after processing

### Error Handling

```kotlin
private fun handleOfferError(error: PurchaseError) {
    when (error.code) {
        ErrorCode.FEATURE_NOT_SUPPORTED -> {
            // Feature not available on this OS version
            showMessage("This feature requires a newer OS version")
        }
        ErrorCode.SERVICE_DISCONNECTED -> {
            // Store connection lost
            showMessage("Please check your connection and try again")
        }
        else -> {
            // Generic error
            showMessage("Failed to process offer: ${error.message}")
        }
    }
}
```

## Testing

### iOS Testing
- Use sandbox tester accounts
- Configure offer codes in App Store Connect
- Test on iOS 14+ devices

### Android Testing
- Use test subscriptions in Google Play Console
- Configure subscription offers
- Test deep linking functionality