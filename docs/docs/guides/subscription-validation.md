---
sidebar_position: 6
title: Subscription Validation
---

import IapKitBanner from '@site/src/uis/IapKitBanner';
import IapKitLink from '@site/src/uis/IapKitLink';

# Subscription Validation

<IapKitBanner />

This guide covers subscription validation best practices, including renewal detection differences between iOS and Android.

## Subscription Renewal Detection

One of the most critical aspects of subscription management is properly detecting subscription renewals, especially when they occur while your app is not running.

### Platform Differences

| Aspect | iOS (StoreKit 2) | Android (Google Play Billing) |
|--------|------------------|-------------------------------|
| **Renewal during app use** | `purchaseUpdatedListener` fires | `purchaseUpdatedListener` fires |
| **Renewal while app closed** | Automatically detected on app launch | **Not detected** via listener |
| **Recommended approach** | Listener + periodic checks | `getAvailablePurchases()` + server verification |

### iOS Behavior

On iOS with StoreKit 2, subscription renewals are automatically detected:

```kotlin
// Renewals that occurred while app was closed are automatically
// delivered through the purchaseUpdatedListener on app launch
kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
    // This will fire for renewals even if app was closed
    handleSubscriptionUpdate(purchase)
}
```

### Android Behavior

On Android, the `purchaseUpdatedListener` does **not** fire for renewals that occurred while the app was closed. This is a fundamental limitation of Google Play Billing Library.

```kotlin
// WARNING: This will NOT fire for renewals that happened while app was closed
kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
    // Only fires for purchases/renewals while app is running
}
```

### Solution: Server-Side Verification with IAPKit

The recommended approach for both platforms is to verify subscription status on app launch using <IapKitLink>IAPKit</IapKitLink>:

```kotlin
suspend fun checkSubscriptionStatusOnAppLaunch() {
    try {
        val purchases = kmpIapInstance.getAvailablePurchases()

        purchases.forEach { purchase ->
            if (isSubscriptionProduct(purchase.productId)) {
                verifyAndUpdateSubscriptionStatus(purchase)
            }
        }
    } catch (e: Exception) {
        // Handle error - consider granting temporary access
        // to avoid penalizing users for network issues
    }
}

private suspend fun verifyAndUpdateSubscriptionStatus(purchase: Purchase) {
    val result = kmpIapInstance.verifyPurchaseWithProvider(
        VerifyPurchaseWithProviderProps(
            provider = PurchaseVerificationProvider.Iapkit,
            iapkit = RequestVerifyPurchaseWithIapkitProps(
                apiKey = AppConfig.iapkitApiKey,
                apple = RequestVerifyPurchaseWithIapkitAppleProps(
                    jws = purchase.purchaseToken
                ),
                google = RequestVerifyPurchaseWithIapkitGoogleProps(
                    purchaseToken = purchase.purchaseToken
                )
            )
        )
    )

    when (result.iapkit?.state) {
        IapkitPurchaseState.Entitled -> {
            // Subscription is active - grant/maintain access
            grantSubscriptionAccess(purchase.productId)
        }
        IapkitPurchaseState.Expired -> {
            // Subscription has expired - revoke access
            revokeSubscriptionAccess(purchase.productId)
        }
        IapkitPurchaseState.Canceled -> {
            // User canceled but may still have access until period ends
            // Check expiration date if available
            handleCanceledSubscription(purchase)
        }
        IapkitPurchaseState.Inauthentic -> {
            // Fraudulent purchase detected
            revokeSubscriptionAccess(purchase.productId)
            logSecurityEvent(purchase)
        }
        else -> {
            // Handle unknown states gracefully
        }
    }
}
```

## Android basePlanId Limitation {#android-baseplanid-limitation}

:::warning Critical Limitation
On Android, the `basePlanId` field may return **incorrect values** for subscription groups with multiple base plans.
:::

**Root Cause:** Google Play Billing API's `Purchase` object does NOT include `basePlanId` information. When a subscription group has multiple base plans (weekly, monthly, yearly), there is no way to determine which specific plan was purchased from the client-side `Purchase` object.

You may see this warning in logs:

```
Multiple offers (3) found for premium_subscription, using first basePlanId (may be inaccurate)
```

**What Works Correctly:**
- `productId` - Subscription group ID
- `purchaseToken` - Purchase token
- `isActive` - Subscription active status
- `transactionId` - Transaction ID

**What May Be Incorrect:**
- `basePlanIdAndroid` - May return first plan instead of purchased plan

### Solutions

#### 1. Client-side Tracking (Recommended for most apps)

Track `basePlanId` yourself during the purchase flow:

```kotlin
// Track basePlanId BEFORE calling requestPurchase
var purchasedBasePlanId: String? = null

suspend fun handlePurchase(basePlanId: String) {
    val product = subscriptions.find { it.productId == subscriptionGroupId }
    if (product !is ProductSubscriptionAndroid) return

    // Use cross-platform subscriptionOffers
    val offers = product.subscriptionOffers
    val offer = offers.find {
        it.basePlanIdAndroid == basePlanId && it.id == null
    }

    if (offer?.offerTokenAndroid == null) return

    // Store it before purchase
    purchasedBasePlanId = basePlanId

    kmpIapInstance.requestPurchase {
        apple { sku = subscriptionGroupId }
        google {
            skus = listOf(subscriptionGroupId)
            subscriptionOffers = listOf(
                SubscriptionOfferAndroid(
                    sku = subscriptionGroupId,
                    offerToken = offer.offerTokenAndroid!!
                )
            )
        }
    }
}

// Use YOUR tracked value in purchase listener
kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
    // DON'T rely on purchase data for basePlanId - it may be wrong!
    val actualBasePlanId = purchasedBasePlanId

    saveToBackend(
        purchaseToken = purchase.purchaseToken,
        basePlanId = actualBasePlanId,  // Use YOUR tracked value
        productId = purchase.productId
    )
}
```

#### 2. IAPKit Backend Validation (Recommended)

Use [`verifyPurchaseWithProvider`](../apis#verifypurchasewithprovider) with <IapKitLink>IAPKit</IapKitLink> to get accurate `basePlanId` from Google Play Developer API:

```kotlin
val result = kmpIapInstance.verifyPurchaseWithProvider(
    VerifyPurchaseWithProviderProps(
        provider = PurchaseVerificationProvider.Iapkit,
        iapkit = RequestVerifyPurchaseWithIapkitProps(
            apiKey = AppConfig.iapkitApiKey,
            google = RequestVerifyPurchaseWithIapkitGoogleProps(
                purchaseToken = purchase.purchaseToken
            )
        )
    )
)

// Access basePlanId from the response
val providerResponse = result.providerResponse as? Map<*, *>
val lineItems = providerResponse?.get("lineItems") as? List<*>
val firstItem = lineItems?.firstOrNull() as? Map<*, *>
val offerDetails = firstItem?.get("offerDetails") as? Map<*, *>
val basePlanId = offerDetails?.get("basePlanId") as? String

println("Actual basePlanId: $basePlanId")
```

#### 3. Single Base Plan Per Subscription Group

If your subscription group has only one base plan, the `basePlanId` will always be accurate. This is the simplest solution if your product design allows it.

:::note
This is a fundamental limitation of Google Play Billing API, not a bug in this library. The `Purchase` object from Google simply does not include `basePlanId` information.
:::

**See also:**
- [SubscriptionOffer](../api/types#subscriptionoffer) - Each offer contains `id`, `displayPrice`, `paymentMode`, `period`, `basePlanIdAndroid`, `offerTokenAndroid`, and `pricingPhasesAndroid`.
- [Subscription Offers Guide](./subscription-offers.md) - Complete guide on working with subscription offers.
- [GitHub Issue #3096](https://github.com/hyochan/react-native-iap/issues/3096) - Original discussion about this limitation.

## IAPKit Purchase States

When verifying purchases with <IapKitLink>IAPKit</IapKitLink>, you'll receive one of these states:

| State | Description | Recommended Action |
|-------|-------------|-------------------|
| `entitled` | Active subscription with valid access | Grant/maintain premium features |
| `expired` | Subscription period has ended | Revoke access, show renewal prompt |
| `canceled` | User canceled but period not ended | Maintain access until expiration |
| `pending` | Purchase is being processed | Show pending state, await resolution |
| `pending-acknowledgment` | Android: needs acknowledgment | Call `finishTransaction()` |
| `ready-to-consume` | Consumable ready for consumption | Process and call `finishTransaction()` |
| `consumed` | Consumable has been used | Already processed |
| `inauthentic` | Failed verification (potential fraud) | Revoke access, log for review |

## useSubscriptionStatus Hook Pattern

For Compose Multiplatform apps, create a reusable hook to manage subscription status:

```kotlin
class SubscriptionStatusManager(
    private val subscriptionIds: List<String>
) {
    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Loading)
    val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()

    sealed class SubscriptionStatus {
        object Loading : SubscriptionStatus()
        data class Active(val productId: String, val expiresAt: Long?) : SubscriptionStatus()
        object Expired : SubscriptionStatus()
        data class Error(val message: String) : SubscriptionStatus()
    }

    suspend fun checkStatus() {
        _subscriptionStatus.value = SubscriptionStatus.Loading

        try {
            val purchases = kmpIapInstance.getAvailablePurchases()
            val subscriptionPurchase = purchases.find {
                subscriptionIds.contains(it.productId)
            }

            if (subscriptionPurchase == null) {
                _subscriptionStatus.value = SubscriptionStatus.Expired
                return
            }

            val result = kmpIapInstance.verifyPurchaseWithProvider(
                VerifyPurchaseWithProviderProps(
                    provider = PurchaseVerificationProvider.Iapkit,
                    iapkit = RequestVerifyPurchaseWithIapkitProps(
                        apiKey = AppConfig.iapkitApiKey,
                        apple = RequestVerifyPurchaseWithIapkitAppleProps(
                            jws = subscriptionPurchase.purchaseToken
                        ),
                        google = RequestVerifyPurchaseWithIapkitGoogleProps(
                            purchaseToken = subscriptionPurchase.purchaseToken
                        )
                    )
                )
            )

            _subscriptionStatus.value = when (result.iapkit?.state) {
                IapkitPurchaseState.Entitled -> SubscriptionStatus.Active(
                    productId = subscriptionPurchase.productId,
                    expiresAt = null // Parse from providerResponse if needed
                )
                else -> SubscriptionStatus.Expired
            }
        } catch (e: Exception) {
            _subscriptionStatus.value = SubscriptionStatus.Error(e.message ?: "Unknown error")
        }
    }
}
```

### Usage in Compose

```kotlin
@Composable
fun SubscriptionScreen(
    subscriptionManager: SubscriptionStatusManager
) {
    val status by subscriptionManager.subscriptionStatus.collectAsState()

    LaunchedEffect(Unit) {
        subscriptionManager.checkStatus()
    }

    when (val currentStatus = status) {
        is SubscriptionStatus.Loading -> {
            CircularProgressIndicator()
        }
        is SubscriptionStatus.Active -> {
            PremiumContent(productId = currentStatus.productId)
        }
        is SubscriptionStatus.Expired -> {
            SubscriptionOffer()
        }
        is SubscriptionStatus.Error -> {
            ErrorMessage(message = currentStatus.message)
        }
    }
}
```

## Best Practices

### 1. Check on App Launch

Always verify subscription status when the app launches:

```kotlin
class MainViewModel : ViewModel() {
    init {
        viewModelScope.launch {
            kmpIapInstance.initConnection()
            checkSubscriptionStatus()
        }
    }

    private suspend fun checkSubscriptionStatus() {
        // Verify with IAPKit to get authoritative status
    }
}
```

### 2. Periodic Validation

For long-running sessions, periodically revalidate:

```kotlin
class SubscriptionValidator {
    private val validationInterval = 24.hours

    suspend fun startPeriodicValidation() {
        while (true) {
            delay(validationInterval)
            validateActiveSubscriptions()
        }
    }
}
```

### 3. Handle Network Failures Gracefully

Don't revoke access immediately on network failures:

```kotlin
suspend fun validateWithGracePeriod(purchase: Purchase): Boolean {
    return try {
        val result = verifyWithIAPKit(purchase)
        result.iapkit?.state == IapkitPurchaseState.Entitled
    } catch (e: NetworkException) {
        // Grant temporary access on network failure
        // to avoid penalizing users for connectivity issues
        true
    }
}
```

### 4. Cache Subscription Status Locally

Cache the last known status for offline support:

```kotlin
class SubscriptionCache(private val prefs: SharedPreferences) {
    fun cacheStatus(productId: String, isActive: Boolean, validUntil: Long) {
        prefs.edit {
            putBoolean("sub_active_$productId", isActive)
            putLong("sub_valid_until_$productId", validUntil)
        }
    }

    fun getCachedStatus(productId: String): CachedStatus? {
        val isActive = prefs.getBoolean("sub_active_$productId", false)
        val validUntil = prefs.getLong("sub_valid_until_$productId", 0)

        if (validUntil == 0L) return null

        return CachedStatus(
            isActive = isActive && System.currentTimeMillis() < validUntil,
            validUntil = validUntil
        )
    }
}
```

## Next Steps

- [Subscription Flow](../examples/subscription-flow.md) - Complete subscription implementation
- [Subscription Offers](./subscription-offers.md) - Working with subscription offers
- [Purchase Flow](../examples/purchase-flow.md) - One-time purchase handling
- [Error Codes](../api/error-codes.md) - Handle verification errors
