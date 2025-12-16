---
sidebar_position: 8
title: Offer Code Redemption
---

import GreatFrontEndBanner from '@site/src/uis/GreatFrontEndBanner';

# Offer Code Redemption

<GreatFrontEndBanner />

Guide to implementing promotional offer codes and subscription management with kmp-iap, covering iOS and Android platforms.

## Overview

This library provides native support for:

- **iOS**: Offer code redemption sheet and subscription management (iOS 14+)
- **Android**: Deep linking to subscription management
- **Cross-platform**: StateFlow-based state management for offers and subscriptions

## iOS Offer Code Redemption

### Present Code Redemption Sheet

```kotlin
import kotlinx.coroutines.*
import io.github.hyochan.kmpiap.*
import io.github.hyochan.kmpiap.types.*
import io.github.hyochan.kmpiap.kmpIapInstance

class OfferCodeHandler(
    private val scope: CoroutineScope
) {
    
    /**
     * Present iOS system offer code redemption sheet (iOS 14+)
     */
    suspend fun presentOfferCodeRedemption() {
        if (getCurrentPlatform() != IapPlatform.IOS) {
            println("Offer code redemption is only available on iOS")
            return
        }
        
        try {
            // Present the system offer code redemption sheet
            kmpIapInstance.presentCodeRedemptionSheetIOS()
            println("Offer code redemption sheet presented")
            
            // Results will come through purchaseUpdatedListener
            listenForRedemptionResults()
            
        } catch (e: PurchaseError) {
            println("Failed to present offer code sheet: $e")
        }
    }
    
    private fun listenForRedemptionResults() {
        scope.launch {
            kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                println("Offer code redeemed: ${purchase.productId}")
                // Handle successful redemption
                handleRedeemedPurchase(purchase)
            }
        }
    }
    
    private suspend fun handleRedeemedPurchase(purchase: Purchase) {
        // Process the redeemed purchase
        // Verify receipt, deliver content, etc.
        val success = kmpIapInstance.finishTransaction(
            purchase = purchase,
            isConsumable = false
        )
        
        if (success) {
            println("Redeemed purchase processed successfully")
        }
    }
}
```

### Storefront Information

```kotlin
class StorefrontHandler() {
    /**
     * Get App Store storefront information (iOS only)
     * Returns country code string (e.g., "USA", "KOR")
     */
    suspend fun getStorefrontInfo(): String? {
        if (getCurrentPlatform() != IapPlatform.IOS) return null

        return try {
            val storefront = kmpIapInstance.getStorefrontIOS()
            println("Storefront country: $storefront")
            storefront
        } catch (e: PurchaseError) {
            println("Failed to get storefront info: $e")
            null
        }
    }
}
```

## Subscription Management

### iOS Subscription Management

```kotlin
class SubscriptionManager(
    private val scope: CoroutineScope
) {
    
    /**
     * Show iOS subscription management screen (iOS 15+)
     */
    suspend fun showManageSubscriptions() {
        if (getCurrentPlatform() != IapPlatform.IOS) {
            println("Subscription management is only available on iOS")
            return
        }
        
        try {
            kmpIapInstance.showManageSubscriptions()
            println("Subscription management screen presented")
        } catch (e: PurchaseError) {
            println("Failed to show subscription management: $e")
        }
    }
    
    /**
     * Monitor subscription state changes
     */
    suspend fun observeSubscriptions() {
        val subscriptions = kmpIapInstance.requestSubscriptions(
            ProductRequest(
                skus = listOf("monthly_sub", "yearly_sub"),
                type = ProductType.SUBS
            )
        )
        println("Active subscriptions: ${subscriptions.size}")
        subscriptions.forEach { sub ->
            println("Subscription: ${sub.id}")
            if (sub is SubscriptionProduct) {
                println("Period: ${sub.subscriptionPeriod}")
            }
            println("Price: ${sub.price}")
        }
    }
}
```

## Android Subscription Management

### Deep Linking to Subscriptions

```kotlin
class AndroidSubscriptionManager() {
    /**
     * Open Android subscription management (deep link to Play Store)
     */
    suspend fun openSubscriptionManagement(productId: String? = null) {
        if (getCurrentPlatform() != IapPlatform.ANDROID) {
            println("Android subscription management is only available on Android")
            return
        }
        
        try {
            // Deep link to subscription management in Play Store
            productId?.let {
                kmpIapInstance.deepLinkToSubscriptions(it)
            } ?: run {
                // Open general subscription management
                kmpIapInstance.deepLinkToSubscriptions("")
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
        if (getCurrentPlatform() != IapPlatform.ANDROID) return
        
        try {
            val history = kmpIapInstance.getAvailablePurchases()
            
            val subscriptions = history.filter { 
                it.productId.contains("subscription") || 
                it.productId.contains("monthly") ||
                it.productId.contains("yearly")
            }
            println("Found ${subscriptions.size} subscriptions in history")
        } catch (e: PurchaseError) {
            println("Failed to get subscription history: $e")
        }
    }
}
```

## Complete Implementation Example

### Android Offer Handler

> **Note**: This example uses Android-specific dependencies (`ViewModel`, `viewModelScope`). For iOS, use a similar pattern with your preferred state management approach.

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*

class OfferViewModel : ViewModel() {
    
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
        initializeIAP()
        observeStates()
        checkPlatformCapabilities()
    }
    
    private fun initializeIAP() {
        viewModelScope.launch {
            kmpIapInstance.initConnection()
        }
    }
    
    private fun observeStates() {
        // Load subscriptions
        viewModelScope.launch {
            try {
                val subs = kmpIapInstance.requestSubscriptions(
                    ProductRequest(
                        skus = listOf("monthly_sub", "yearly_sub"),
                        type = ProductType.SUBS
                    )
                )
                _state.update { it.copy(activeSubscriptions = subs) }
            } catch (e: PurchaseError) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }
    
    private fun checkPlatformCapabilities() {
        val canRedeem = getCurrentPlatform() == IapPlatform.IOS
        _state.update { it.copy(canRedeemCode = canRedeem) }
    }
    
    /**
     * Present offer code redemption (iOS) or subscription management (Android)
     */
    suspend fun handleOfferRedemption() {
        _state.update { it.copy(isLoading = true, error = null) }
        
        try {
            when (getCurrentPlatform()) {
                IapPlatform.IOS -> {
                    // iOS: Present code redemption sheet
                    kmpIapInstance.presentCodeRedemptionSheetIOS()
                    println("iOS offer code redemption sheet presented")
                    listenForPurchases()
                }
                IapPlatform.ANDROID -> {
                    // Android: Open subscription management
                    kmpIapInstance.deepLinkToSubscriptions("")
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
                IapPlatform.IOS -> {
                    kmpIapInstance.showManageSubscriptions()
                }
                IapPlatform.ANDROID -> {
                    // For Android, deep link to the first active subscription
                    val firstSub = _state.value.activeSubscriptions.firstOrNull()
                    firstSub?.let {
                        kmpIapInstance.deepLinkToSubscriptions(it.id)
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
            kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
                println("Purchase received: ${purchase.productId}")
                handlePurchaseSuccess(purchase)
            }
        }
    }
    
    private suspend fun handlePurchaseSuccess(purchase: Purchase) {
        // Deliver content
        deliverContent(purchase.productId)
        
        // Finish transaction
        kmpIapInstance.finishTransaction(
            purchase = purchase,
            isConsumable = false
        )
        
        _state.update { it.copy(isLoading = false) }
    }
    
    override fun onCleared() {
        super.onCleared()
        kmpIapInstance.dispose()
    }
}
```

## Additional Features

### Platform-Specific Helpers

```kotlin
class PlatformSpecificFeatures(
    private val scope: CoroutineScope
) {
    /**
     * iOS: Get promoted products from App Store
     */
    suspend fun getPromotedProducts(): List<Product> {
        if (getCurrentPlatform() != IapPlatform.IOS) return emptyList()
        
        return try {
            // Load promoted products
            val products = kmpIapInstance.requestProducts(
                ProductRequest(
                    skus = listOf("promoted_product_1", "promoted_product_2"),
                    type = ProductType.INAPP
                )
            )
            products.forEach { product ->
                println("Promoted product: ${product.id}")
                println("Price: ${product.price}")
            }
            products
        } catch (e: PurchaseError) {
            println("Failed to get promoted products: $e")
            emptyList()
        }
    }
    
    /**
     * Android: Handle subscription with specific offer
     */
    suspend fun purchaseSubscriptionWithOffer(
        productId: String,
        offerToken: String
    ) {
        if (getCurrentPlatform() != IapPlatform.ANDROID) return
        
        try {
            kmpIapInstance.requestSubscription(
                SubscriptionRequest(
                    sku = productId,
                    offerToken = offerToken
                )
            )
        } catch (e: PurchaseError) {
            println("Failed to purchase with offer: $e")
        }
    }
}
```

## Usage Examples

### In a Compose UI (Android)

> **Note**: This example uses Android Compose with `viewModel()`. For Compose Multiplatform, adjust the ViewModel injection to match your DI approach.

```kotlin
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*

@Composable
fun OfferRedemptionScreen(
    viewModel: OfferViewModel = viewModel()
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
                            IapPlatform.IOS -> {
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
                            
                            IapPlatform.ANDROID -> {
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