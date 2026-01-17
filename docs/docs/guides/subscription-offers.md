---
sidebar_position: 7
title: Subscription Offers
---

import IapKitBanner from '@site/src/uis/IapKitBanner';

# Subscription Offers

<IapKitBanner />

This guide explains how to handle subscription offers (pricing plans) when purchasing subscriptions on iOS and Android platforms.

For a complete implementation example, see the [Subscription Flow Example](../examples/subscription-flow.md).

## Overview

Subscription offers represent different pricing plans for the same subscription product:

- **Base Plan**: The standard pricing for a subscription
- **Introductory Offers**: Special pricing for new subscribers (free trial, discounted period)
- **Promotional Offers**: Limited-time discounts configured in the app stores

## Platform Differences

At a glance:

- Android: subscription offers are required when purchasing subscriptions. You must pass `subscriptionOffers` with one or more offer tokens from `fetchProducts()`.
- iOS: base plan is used by default. Promotional discounts are optional via `withOffer`.

:::tip
Always fetch products first; offers only exist after `fetchProducts { type = ProductQueryType.Subs }`.
:::

### Android Subscription Offers

Android requires explicit specification of subscription offers when purchasing. Each offer is identified by an `offerToken` obtained from `fetchProducts()`.

#### Required for Android Subscriptions

Unlike iOS, Android subscriptions **must** include `subscriptionOffers` in the purchase request. Without it, the purchase will fail with:

```text
The number of skus (1) must match: the number of offerTokens (0)
```

#### Getting Offer Tokens

```kotlin
// 1) Fetch subscription products
val subscriptions = kmpIapInstance.fetchProducts {
    skus = listOf("premium_monthly")
    type = ProductQueryType.Subs
}

// 2) Access offer details from fetched subscriptions
val subscription = subscriptions.find { it.productId == "premium_monthly" }

if (subscription is ProductSubscriptionAndroid) {
    println("Available offers: ${subscription.subscriptionOffers}")
    // Each offer contains: id, displayPrice, paymentMode, period, offerTokenAndroid, pricingPhasesAndroid
}
```

#### Purchase with Offers

```kotlin
suspend fun purchaseSubscription(subscriptionId: String) {
    val subscription = subscriptions.find { it.productId == subscriptionId }
    if (subscription == null) return

    // Build subscriptionOffers from fetched data using cross-platform subscriptionOffers
    val offers = when (subscription) {
        is ProductSubscriptionAndroid -> {
            subscription.subscriptionOffers
                .filter { it.offerTokenAndroid != null }
                .map { offer ->
                    SubscriptionOfferAndroid(
                        sku = subscriptionId,
                        offerToken = offer.offerTokenAndroid!!
                    )
                }
        }
        else -> emptyList()
    }

    // Only proceed if offers are available
    if (offers.isEmpty()) {
        println("No subscription offers available")
        return
    }

    kmpIapInstance.requestPurchase {
        apple { sku = subscriptionId }
        google {
            skus = listOf(subscriptionId)
            subscriptionOffers = offers
        }
    }
}
```

#### Understanding Offer Details

Each `subscriptionOffers` item contains (cross-platform `SubscriptionOffer` type):

```kotlin
data class SubscriptionOffer(
    val id: String?,                           // Unique identifier for the offer
    val displayPrice: String?,                 // Formatted price string (e.g., "$9.99/month")
    val price: Double?,                        // Numeric price value
    val currency: String?,                     // Currency code (ISO 4217)
    val type: DiscountOfferType?,              // Introductory, Promotional, OneTime
    val paymentMode: PaymentMode?,             // FreeTrial, PayAsYouGo, PayUpFront
    val period: SubscriptionPeriod?,           // Subscription period (unit + value)
    val periodCount: Int?,                     // Number of periods the offer applies

    // Android-specific fields
    val basePlanIdAndroid: String?,            // Base plan identifier
    val offerTokenAndroid: String?,            // Token required for purchase
    val offerTagsAndroid: List<String>?,       // Tags associated with the offer
    val pricingPhasesAndroid: PricingPhasesAndroid?, // Detailed pricing phases

    // iOS-specific fields
    val keyIdentifierIOS: String?,             // Key identifier for signature validation
    val nonceIOS: String?,                     // Cryptographic nonce (UUID)
    val signatureIOS: String?,                 // Server-generated signature
    val timestampIOS: Long?                    // Timestamp when signature was generated
)
```

:::warning Android basePlanId Limitation
The `basePlanId` is available when fetching products, but not when retrieving purchases via `getAvailablePurchases()`. This is a limitation of Google Play Billing Library - the purchase token alone doesn't reveal which base plan was purchased.

See [GitHub Issue #3096](https://github.com/hyochan/react-native-iap/issues/3096) for more details. See the [basePlanId Limitation](./subscription-validation.md#android-baseplanid-limitation) section for details and workarounds.
:::

### iOS Subscription Offers

iOS handles subscription offers differently - the base plan is used by default, and promotional offers are optional.

#### Base Plan (Default)

For standard subscription purchases, no special offer specification is needed:

```kotlin
kmpIapInstance.requestPurchase {
    apple { sku = "premium_monthly" }
    google {
        skus = listOf("premium_monthly")
        // include subscriptionOffers only if available
    }
}
```

#### Introductory Offers

iOS automatically applies introductory prices (free trials, intro pricing) configured in App Store Connect. No additional code is needed - users will see the introductory offer when eligible.

To check if a subscription has an introductory offer:

```kotlin
val subscription = subscriptions.find { it.productId == "premium_monthly" }

if (subscription is ProductSubscriptionIOS) {
    subscription.introductoryDiscountIOS?.let { offer ->
        when (offer.paymentMode) {
            PaymentMode.FreeTrial -> {
                println("${offer.periodCount} ${offer.period?.unit} free trial")
            }
            PaymentMode.PayAsYouGo -> {
                println("${offer.displayPrice} for ${offer.periodCount} ${offer.period?.unit}")
            }
            PaymentMode.PayUpFront -> {
                println("${offer.displayPrice} for first ${offer.periodCount} ${offer.period?.unit}")
            }
            else -> {}
        }
    }
}
```

#### Promotional Offers (Optional)

iOS supports promotional offers through the `withOffer` parameter. These are server-to-server offers that require signature generation from your backend.

##### Getting Available Promotional Offers

```kotlin
val subscription = subscriptions.find { it.productId == "premium_monthly" }

if (subscription is ProductSubscriptionIOS) {
    subscription.discountsIOS?.forEach { discount ->
        println("- Identifier: ${discount.identifier}")
        println("  Price: ${discount.localizedPrice}")
        println("  Payment mode: ${discount.paymentMode}")
        println("  Period: ${discount.subscriptionPeriod}")
        println("  Number of periods: ${discount.numberOfPeriods}")
    }
}
```

##### Applying Promotional Offers

To apply a promotional offer, you need to generate a signature on your backend server. See [Apple's documentation](https://developer.apple.com/documentation/storekit/in-app_purchase/original_api_for_in-app_purchase/subscriptions_and_offers/generating_a_signature_for_promotional_offers) for signature generation.

```kotlin
suspend fun purchaseWithPromotionalOffer(
    subscriptionId: String,
    offerId: String
) {
    // 1. Generate signature on your backend
    val nonce = UUID.randomUUID().toString()
    val timestamp = System.currentTimeMillis()

    val signatureResponse = fetchSignatureFromBackend(
        productId = subscriptionId,
        offerId = offerId,
        nonce = nonce,
        timestamp = timestamp
    )

    // 2. Purchase with the promotional offer
    kmpIapInstance.requestPurchase {
        apple {
            sku = subscriptionId
            withOffer = DiscountOfferInputIOS(
                identifier = offerId,
                keyIdentifier = signatureResponse.keyIdentifier,
                nonce = nonce,
                signature = signatureResponse.signature,
                timestamp = timestamp
            )
        }
        google {
            skus = listOf(subscriptionId)
            subscriptionOffers = listOf(/* ... */)
        }
    }
}
```

## Common Patterns

### Selecting Specific Offers

```kotlin
fun selectOffer(
    subscription: Product,
    offerType: OfferType
): SubscriptionOffer? {
    return when (subscription) {
        is ProductSubscriptionIOS -> {
            // iOS: Check for introductory offer
            if (offerType == OfferType.INTRODUCTORY) {
                // Access introductory offer from subscriptionOffers
                subscription.subscriptionOffers?.find {
                    it.type == DiscountOfferType.Introductory
                }
            } else {
                // Base plan is default, no selection needed
                null
            }
        }
        is ProductSubscriptionAndroid -> {
            // Android: Select offer based on type using cross-platform subscriptionOffers
            val offers = subscription.subscriptionOffers

            when (offerType) {
                OfferType.BASE -> {
                    // Find base plan (offer without introductory/promotional type)
                    offers.find { it.type == null || it.type == DiscountOfferType.OneTime }
                }
                OfferType.INTRODUCTORY -> {
                    // Find introductory offer
                    offers.find { it.type == DiscountOfferType.Introductory }
                }
            }
        }
        else -> null
    }
}

enum class OfferType {
    BASE,
    INTRODUCTORY
}

suspend fun purchaseWithSelectedOffer(
    subscriptionId: String,
    offerType: OfferType = OfferType.BASE
) {
    val subscription = subscriptions.find { it.productId == subscriptionId }
    if (subscription == null) return

    val selectedOffer = selectOffer(subscription, offerType)

    when (subscription) {
        is ProductSubscriptionAndroid -> {
            val subscriptionOffers = selectedOffer?.offerTokenAndroid?.let { token ->
                listOf(SubscriptionOfferAndroid(sku = subscriptionId, offerToken = token))
            } ?: emptyList()

            if (subscriptionOffers.isEmpty()) {
                println("No suitable offer found")
                return
            }

            kmpIapInstance.requestPurchase {
                apple { sku = subscriptionId }
                google {
                    skus = listOf(subscriptionId)
                    subscriptionOffers = subscriptionOffers
                }
            }
        }
        is ProductSubscriptionIOS -> {
            // iOS: Introductory offers are automatically applied
            // selectedOffer contains intro price info for display purposes
            if (offerType == OfferType.INTRODUCTORY && selectedOffer != null) {
                println("Offer: ${selectedOffer.displayPrice}")
                println("Payment mode: ${selectedOffer.paymentMode}")
            }

            kmpIapInstance.requestPurchase {
                apple { sku = subscriptionId }
                google {
                    skus = listOf(subscriptionId)
                    // include subscriptionOffers only if available
                }
            }
        }
    }
}
```

## Error Handling

### Android Errors

```kotlin
kmpIapInstance.purchaseErrorListener.collect { error ->
    when (error.code) {
        IapErrorCode.PURCHASE_ERROR -> {
            println("Purchase failed - check subscription offers")
            // Ensure subscriptionOffers is included and valid
        }
        else -> {
            println("Error: ${error.message}")
        }
    }
}
```

### iOS Errors

```kotlin
kmpIapInstance.purchaseErrorListener.collect { error ->
    when (error.code) {
        IapErrorCode.UNKNOWN -> {
            println("Invalid promotional offer for iOS")
            // Check offerIdentifier, signature, etc.
        }
        else -> {
            println("Error: ${error.message}")
        }
    }
}
```

## Best Practices

1. **Always fetch products first**: Subscription offers are only available after `fetchProducts()`.

2. **Handle platform differences**: Android requires offers, iOS makes them optional.

3. **Validate offers**: Check that offers exist before attempting purchase.

4. **User selection**: Allow users to choose between different pricing plans when multiple offers are available.

5. **Error recovery**: Provide fallback to base plan if selected offer fails.

## See Also

- [Subscription Flow](../examples/subscription-flow.md) - Complete implementation
- [Subscription Validation](./subscription-validation.md) - Validate subscription status
- [Error Codes](../api/error-codes.md) - Purchase error handling
