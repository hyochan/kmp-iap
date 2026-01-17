---
sidebar_position: 2
title: Subscription Flow
---

import IapKitBanner from '@site/src/uis/IapKitBanner';
import IapKitLink from '@site/src/uis/IapKitLink';

# Subscription Flow

<IapKitBanner />

## Key Concepts

- Use `ProductQueryType.Subs` to fetch subscription products
- Android requires `offerToken` for subscription purchases
- Use `getActiveSubscriptions()` to check subscription status
- Always set `isConsumable = false` when finishing subscription transactions

## Fetch Subscriptions

```kotlin
val subscriptions = kmpIapInstance.fetchProducts {
    skus = listOf("premium_monthly", "premium_yearly")
    type = ProductQueryType.Subs
}
```

## Check Active Subscriptions

```kotlin
// Quick check
val hasActive = kmpIapInstance.hasActiveSubscriptions(subscriptionIds)

// Detailed info
val activeSubscriptions = kmpIapInstance.getActiveSubscriptions(subscriptionIds)
activeSubscriptions.forEach { sub ->
    println("Product: ${sub.productId}")
    println("Expires: ${sub.expirationDateIOS}")      // iOS
    println("Auto-renewing: ${sub.autoRenewingAndroid}") // Android
}
```

## Access Subscription Offers (v1.3.12+)

Starting from v1.3.12, subscription products include a cross-platform `subscriptionOffers` field that provides unified access to introductory and promotional offers:

```kotlin
val subscriptions = kmpIapInstance.fetchProducts {
    skus = listOf("premium_monthly", "premium_yearly")
    type = ProductQueryType.Subs
}

subscriptions.forEach { product ->
    when (product) {
        is ProductSubscriptionAndroid -> {
            // Android subscriptionOffers is a non-null list
            product.subscriptionOffers.forEach { offer ->
                println("Offer ID: ${offer.id}")
                println("Price: ${offer.displayPrice}")
                println("Type: ${offer.type}")  // Introductory, Promotional
                println("Payment Mode: ${offer.paymentMode}")  // FreeTrial, PayAsYouGo, PayUpFront

                // Android-specific fields
                println("Base Plan: ${offer.basePlanIdAndroid}")
                println("Token: ${offer.offerTokenAndroid}")
            }
        }
        is ProductSubscriptionIOS -> {
            // iOS subscriptionOffers is nullable
            product.subscriptionOffers?.forEach { offer ->
                println("Offer ID: ${offer.id}")
                println("Price: ${offer.displayPrice}")
                println("Type: ${offer.type}")
                println("Payment Mode: ${offer.paymentMode}")

                // iOS-specific fields
                offer.period?.let { period ->
                    println("Period: ${period.value} ${period.unit}")
                }
            }
        }
    }
}
```

### Offer Types

| Type | Description |
|------|-------------|
| `Introductory` | First-time subscriber discount (free trial, reduced price) |
| `Promotional` | Winback or promotional offer for existing/lapsed subscribers |
| `OneTime` | One-time purchase discount (Android only) |

### Payment Modes

| Mode | Description |
|------|-------------|
| `FreeTrial` | Free for a limited period |
| `PayAsYouGo` | Reduced price for each billing cycle |
| `PayUpFront` | Pay reduced price upfront for entire period |

## Request Subscription

```kotlin
// Get offer token for Android
val product = subscriptions.find { it.productId == "premium_monthly" }
val offerToken = when (product) {
    is ProductSubscriptionAndroid -> product.subscriptionOffers.firstOrNull()?.offerTokenAndroid
    else -> null
}

kmpIapInstance.requestPurchase {
    apple { sku = "premium_monthly" }
    google {  // Recommended (v1.3.15+)
        skus = listOf("premium_monthly")
        subscriptionOffers = offerToken?.let {
            listOf(SubscriptionOfferAndroid(sku = "premium_monthly", offerToken = it))
        }
    }
}
```

## Finish Transaction

```kotlin
kmpIapInstance.finishTransaction(
    purchase = purchase.toPurchaseInput(),
    isConsumable = false  // Subscriptions are never consumable
)
```

## Platform-Specific Info

| Feature | iOS | Android |
|---------|-----|---------|
| Expiration | `expirationDateIOS` | Server-side |
| Environment | `environmentIOS` | N/A |
| Auto-renew | N/A | `autoRenewingAndroid` |
| Manage subs | App Store | `deepLinkToSubscriptions()` |

## Android Subscription Management

```kotlin
if (kmpIapInstance.getPlatform() == IapPlatform.Android) {
    kmpIapInstance.deepLinkToSubscriptions("premium_monthly")
}
```

## IAPKit Server Verification

For subscription apps, server-side verification is critical for managing access control and handling renewals.

### Setup

1. Get your API key from <IapKitLink>iapkit.com</IapKitLink>
2. Configure environment variables (see [Purchase Flow](./purchase-flow.md#setup) for details)

### Subscription Verification

```kotlin
kmpIapInstance.purchaseUpdatedListener.collect { purchase ->
    val purchaseToken = purchase.purchaseToken
    if (purchaseToken.isNullOrEmpty()) {
        showError("No purchase token available")
        return@collect
    }

    try {
        val result = kmpIapInstance.verifyPurchaseWithProvider(
            VerifyPurchaseWithProviderProps(
                provider = PurchaseVerificationProvider.Iapkit,
                iapkit = RequestVerifyPurchaseWithIapkitProps(
                    apiKey = AppConfig.iapkitApiKey,
                    apple = RequestVerifyPurchaseWithIapkitAppleProps(jws = purchaseToken),
                    google = RequestVerifyPurchaseWithIapkitGoogleProps(purchaseToken = purchaseToken)
                )
            )
        )

        when (result.iapkit?.state) {
            IapkitPurchaseState.Entitled -> {
                // Active subscription - grant access
                grantSubscriptionAccess(purchase.productId)
                kmpIapInstance.finishTransaction(
                    purchase = purchase.toPurchaseInput(),
                    isConsumable = false
                )
            }
            IapkitPurchaseState.Expired -> {
                // Subscription ended - revoke access
                revokeSubscriptionAccess(purchase.productId)
            }
            IapkitPurchaseState.Canceled -> {
                // User canceled but may still have access until period ends
                showInfo("Subscription will end at period end")
            }
            IapkitPurchaseState.Inauthentic -> {
                // Fraudulent purchase detected
                revokeSubscriptionAccess(purchase.productId)
                showError("Invalid purchase detected")
            }
            else -> {
                showError("Unknown state: ${result.iapkit?.state}")
            }
        }
    } catch (e: Exception) {
        showError("Verification error: ${e.message}")
    }
}
```

### Verification Response

```json
{
  "isValid": true,
  "state": "ENTITLED",
  "store": "APPLE"
}
```

```kotlin
// Access in code
val isValid = result.iapkit?.isValid     // true
val state = result.iapkit?.state         // IapkitPurchaseState.Entitled
val store = result.iapkit?.store         // IapStore.Apple or IapStore.Google
```

### Periodic Validation

For production apps, validate subscriptions periodically:

```kotlin
// On app launch or every 24 hours
suspend fun validateActiveSubscriptions() {
    val activeSubscriptions = kmpIapInstance.getActiveSubscriptions(subscriptionIds)

    activeSubscriptions.forEach { subscription ->
        val result = verifyWithIapkit(subscription)

        when (result.iapkit?.state) {
            IapkitPurchaseState.Entitled -> { /* Still valid */ }
            IapkitPurchaseState.Expired,
            IapkitPurchaseState.Canceled -> {
                revokeSubscriptionAccess(subscription.productId)
            }
            else -> { /* Handle error */ }
        }
    }
}
```

## IAPKit Subscription States

| State | Description | Action |
|-------|-------------|--------|
| `entitled` | User has an active, valid subscription | Grant access |
| `expired` | Subscription has expired and not renewed | Remove access |
| `canceled` | User cancelled but may still have access until period ends | Check expiration date |
| `pending` | Payment is pending (e.g., awaiting parental approval) | Show pending UI |
| `pending-acknowledgment` | Purchase needs acknowledgment (Android) | Call `finishTransaction()` |
| `inauthentic` | Purchase could not be verified / fraudulent | Deny access |

## Checking Subscription Status on App Launch

:::warning Android Renewal Detection
On Android, `purchaseUpdatedListener` does **not** fire for renewals that occurred while the app was closed. Always verify subscription status on app launch.
:::

```kotlin
class SubscriptionViewModel : ViewModel() {
    private val subscriptionIds = listOf("premium_monthly", "premium_yearly")

    init {
        viewModelScope.launch {
            kmpIapInstance.initConnection()
            checkSubscriptionStatusOnLaunch()
        }
    }

    private suspend fun checkSubscriptionStatusOnLaunch() {
        try {
            val purchases = kmpIapInstance.getAvailablePurchases()

            val subscriptionPurchase = purchases.find {
                subscriptionIds.contains(it.productId)
            }

            if (subscriptionPurchase == null) {
                // No subscription found
                updateUIForExpiredSubscription()
                return
            }

            // Verify with IAPKit for authoritative status
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

            when (result.iapkit?.state) {
                IapkitPurchaseState.Entitled -> {
                    grantSubscriptionAccess(subscriptionPurchase.productId)
                }
                IapkitPurchaseState.Expired -> {
                    revokeSubscriptionAccess(subscriptionPurchase.productId)
                    showRenewalPrompt()
                }
                IapkitPurchaseState.Canceled -> {
                    // Still has access until period ends
                    showCancellationNotice()
                }
                else -> {
                    handleUnknownState(result.iapkit?.state)
                }
            }
        } catch (e: Exception) {
            // Grant temporary access on error to avoid penalizing users
            grantTemporaryAccess()
        }
    }
}
```

## Complete Verification Flow

Here's a complete example of handling subscription verification within `purchaseUpdatedListener`:

```kotlin
private suspend fun onPurchaseSuccess(purchase: Purchase) {
    val purchaseToken = purchase.purchaseToken
    if (purchaseToken.isNullOrEmpty()) {
        showError("No purchase token available")
        return
    }

    try {
        val result = kmpIapInstance.verifyPurchaseWithProvider(
            VerifyPurchaseWithProviderProps(
                provider = PurchaseVerificationProvider.Iapkit,
                iapkit = RequestVerifyPurchaseWithIapkitProps(
                    apiKey = AppConfig.iapkitApiKey,
                    apple = RequestVerifyPurchaseWithIapkitAppleProps(jws = purchaseToken),
                    google = RequestVerifyPurchaseWithIapkitGoogleProps(purchaseToken = purchaseToken)
                )
            )
        )

        when (result.iapkit?.state) {
            IapkitPurchaseState.Entitled -> {
                // 1. Grant access
                grantSubscriptionAccess(purchase.productId)

                // 2. Finish the transaction
                kmpIapInstance.finishTransaction(
                    purchase = purchase.toPurchaseInput(),
                    isConsumable = false
                )

                // 3. Show success message
                showSuccess("Subscription activated!")
            }

            IapkitPurchaseState.PendingAcknowledgment -> {
                // Android: Transaction needs acknowledgment
                kmpIapInstance.finishTransaction(
                    purchase = purchase.toPurchaseInput(),
                    isConsumable = false
                )
                grantSubscriptionAccess(purchase.productId)
            }

            IapkitPurchaseState.Pending -> {
                // Purchase still processing (e.g., pending payment)
                showPendingUI(purchase.productId)
            }

            IapkitPurchaseState.Expired,
            IapkitPurchaseState.Canceled -> {
                revokeSubscriptionAccess(purchase.productId)
            }

            IapkitPurchaseState.Inauthentic -> {
                revokeSubscriptionAccess(purchase.productId)
                logSecurityEvent("Inauthentic purchase detected", purchase)
            }

            else -> {
                showError("Unexpected state: ${result.iapkit?.state}")
            }
        }
    } catch (e: Exception) {
        // Handle verification errors gracefully
        // Consider granting temporary access rather than blocking the user
        showError("Verification error: ${e.message}")
    }
}
```

> For more details on subscription validation patterns, see [Subscription Validation](../guides/subscription-validation.md).

## Android basePlanId Limitation {#baseplanid-limitation}

### Client-Side Limitation

The `basePlanId` is available when fetching products, but **not** when retrieving purchases via `getAvailablePurchases()`. This is a limitation of Google Play Billing Library - the purchase token alone doesn't reveal which base plan was purchased.

> See [GitHub Issue #3096](https://github.com/hyochan/react-native-iap/issues/3096) for more details.

**Why this matters:**

- If you have multiple base plans (e.g., monthly, yearly, premium), you cannot determine which plan the user is subscribed to using client-side APIs alone
- The `basePlanId` is only available from `subscriptionOfferDetailsAndroid` at the time of purchase, not from restored purchases

### Solution: Server-Side Verification with IAPKit

Use the `verifyPurchaseWithProvider` function to get complete subscription details including `basePlanId`:

```kotlin
val verifyAndroidSubscription = suspend fun(purchase: Purchase) {
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

    // Response includes offerDetails.basePlanId in lineItems
    val basePlanId = result.providerResponse
        ?.get("lineItems")
        ?.let { (it as? List<*>)?.firstOrNull() as? Map<*, *> }
        ?.get("offerDetails")
        ?.let { (it as? Map<*, *>)?.get("basePlanId") as? String }

    println("Subscribed to base plan: $basePlanId")
}
```

> See [verifyPurchaseWithProvider](https://www.openiap.dev/docs/apis#verifypurchasewithprovider)

The server response includes `offerDetails.basePlanId` in the `lineItems` array, allowing you to identify exactly which subscription plan the user purchased.

:::tip Subscription Offers
When fetching products, each subscription offer includes: `basePlanId`, `offerId?`, `offerTags`, `offerToken`, and `pricingPhases`. See [ProductSubscriptionAndroidOfferDetails](https://www.openiap.dev/docs/types#productsubscriptionandroidofferdetails) for more details.
:::

> See <IapKitLink>IAPKit documentation</IapKitLink> for setup instructions and API details.

## Next Steps

- [Purchase Flow](./purchase-flow.md) - One-time purchases
- [Error Codes](../api/error-codes.md) - Handle all error types
