package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.openiap.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InAppPurchaseTest {

    @Test
    fun testErrorCodes() {
        val errorCode = ErrorCode.UserCancelled
        assertEquals(ErrorCode.UserCancelled, errorCode)
        assertEquals("user-cancelled", errorCode.toJson())
    }

    @Test
    fun testPlatformTypes() {
        val platform = IapPlatform.Android
        assertEquals(IapPlatform.Android, platform)
    }

    @Test
    fun testConnectionResult() {
        val result = ConnectionResult(connected = true, message = "Connected successfully")
        assertTrue(result.connected)
        assertEquals("Connected successfully", result.message)
    }

    @Test
    fun testPurchaseError() {
        val error = PurchaseError(code = ErrorCode.UserCancelled, message = "Test error")
        assertEquals("Test error", error.message)
        assertEquals(ErrorCode.UserCancelled, error.code)
    }

    @Test
    fun testProductTypes() {
        val product = ProductAndroid(
            currency = "USD",
            description = "A test product",
            displayPrice = "$0.99",
            id = "test_product",
            nameAndroid = "Test Product",
            platform = IapPlatform.Android,
            price = 0.99,
            title = "Test Product",
            type = ProductType.InApp
        )

        assertEquals("test_product", product.id)
        assertEquals("$0.99", product.displayPrice)
        assertEquals("USD", product.currency)
        assertEquals(0.99, product.price)
    }

    @Test
    fun testSubscriptionTypes() {
        val subscription = ProductSubscriptionIOS(
            currency = "USD",
            description = "A test subscription",
            displayPrice = "$9.99",
            id = "test_subscription",
            displayNameIOS = "Test Subscription",
            isFamilyShareableIOS = false,
            jsonRepresentationIOS = "{}",
            subscriptionPeriodNumberIOS = "1",
            subscriptionPeriodUnitIOS = SubscriptionPeriodIOS.Month,
            platform = IapPlatform.Ios,
            price = 9.99,
            title = "Test Subscription",
            type = ProductType.Subs,
            typeIOS = ProductTypeIOS.AutoRenewableSubscription,
            introductoryPricePaymentModeIOS = PaymentModeIOS.Empty
        )

        assertEquals("test_subscription", subscription.id)
        assertEquals("$9.99", subscription.displayPrice)
        assertEquals("1", subscription.subscriptionPeriodNumberIOS)
    }

    @Test
    fun testPurchaseTypes() {
        val purchase = PurchaseAndroid(
            autoRenewingAndroid = false,
            dataAndroid = "receipt_data",
            developerPayloadAndroid = null,
            id = "12345",
            ids = listOf("test_product"),
            isAcknowledgedAndroid = false,
            isAutoRenewing = false,
            obfuscatedAccountIdAndroid = null,
            obfuscatedProfileIdAndroid = null,
            packageNameAndroid = "com.example",
            platform = IapPlatform.Android,
            productId = "test_product",
            purchaseState = PurchaseState.Purchased,
            purchaseToken = "token",
            quantity = 1,
            signatureAndroid = "signature",
            store = IapStore.Google,
            transactionDate = 1234567890.0
        )

        assertEquals("12345", purchase.id)
        assertEquals("test_product", purchase.productId)
        assertEquals("token", purchase.purchaseToken)
        assertEquals(PurchaseState.Purchased, purchase.purchaseState)
    }

    @Test
    fun testProductRequest() {
        val request = ProductRequest(
            skus = listOf("product1", "product2"),
            type = ProductQueryType.InApp
        )

        assertEquals(2, request.skus.size)
        assertEquals(ProductQueryType.InApp, request.type)
    }

    @Test
    fun testActiveSubscription() {
        val activeSubscription = ActiveSubscription(
            autoRenewingAndroid = true,
            isActive = true,
            productId = "test_sub",
            purchaseToken = "token",
            transactionDate = 1234567890.0,
            transactionId = "txn"
        )

        assertEquals("test_sub", activeSubscription.productId)
        assertTrue(activeSubscription.isActive)
        assertEquals("txn", activeSubscription.transactionId)
    }

    // =========================================================================
    // advancedCommerceData Tests
    // =========================================================================

    @Test
    fun testRequestPurchaseIosPropsWithAdvancedCommerceData() {
        val props = RequestPurchaseIosProps(
            sku = "com.example.premium",
            advancedCommerceData = "campaign_summer_2025"
        )

        assertEquals("com.example.premium", props.sku)
        assertEquals("campaign_summer_2025", props.advancedCommerceData)
    }

    @Test
    fun testRequestPurchaseIosPropsWithoutAdvancedCommerceData() {
        val props = RequestPurchaseIosProps(
            sku = "com.example.premium"
        )

        assertEquals("com.example.premium", props.sku)
        assertNull(props.advancedCommerceData)
    }

    @Test
    fun testRequestSubscriptionIosPropsWithAdvancedCommerceData() {
        val props = RequestSubscriptionIosProps(
            sku = "com.example.subscription.monthly",
            advancedCommerceData = "campaign_q4_2025"
        )

        assertEquals("com.example.subscription.monthly", props.sku)
        assertEquals("campaign_q4_2025", props.advancedCommerceData)
    }

    // =========================================================================
    // google field support Tests
    // =========================================================================

    @Test
    fun testRequestPurchasePropsByPlatformsWithGoogleField() {
        val props = RequestPurchasePropsByPlatforms(
            google = RequestPurchaseAndroidProps(
                skus = listOf("sku_premium")
            )
        )

        assertNotNull(props.google)
        assertEquals(listOf("sku_premium"), props.google?.skus)
        assertNull(props.android)
    }

    @Test
    fun testRequestPurchasePropsByPlatformsWithBothIosAndGoogle() {
        val props = RequestPurchasePropsByPlatforms(
            ios = RequestPurchaseIosProps(
                sku = "ios_premium",
                advancedCommerceData = "campaign_123"
            ),
            google = RequestPurchaseAndroidProps(
                skus = listOf("android_premium"),
                obfuscatedAccountIdAndroid = "user_123"
            )
        )

        assertNotNull(props.ios)
        assertNotNull(props.google)
        assertEquals("ios_premium", props.ios?.sku)
        assertEquals("campaign_123", props.ios?.advancedCommerceData)
        assertEquals(listOf("android_premium"), props.google?.skus)
    }

    @Test
    fun testRequestSubscriptionPropsByPlatformsWithGoogleField() {
        val props = RequestSubscriptionPropsByPlatforms(
            google = RequestSubscriptionAndroidProps(
                skus = listOf("sub_monthly"),
                subscriptionOffers = listOf(
                    AndroidSubscriptionOfferInput(
                        sku = "sub_monthly",
                        offerToken = "offer_token_123"
                    )
                )
            )
        )

        assertNotNull(props.google)
        assertEquals(listOf("sub_monthly"), props.google?.skus)
        assertEquals(1, props.google?.subscriptionOffers?.size)
    }

    // =========================================================================
    // Billing Programs API types Tests
    // =========================================================================

    @Test
    fun testBillingProgramAndroidEnum() {
        assertEquals("unspecified", BillingProgramAndroid.Unspecified.rawValue)
        assertEquals("external-content-link", BillingProgramAndroid.ExternalContentLink.rawValue)
        assertEquals("external-offer", BillingProgramAndroid.ExternalOffer.rawValue)
        assertEquals("user-choice-billing", BillingProgramAndroid.UserChoiceBilling.rawValue)
    }

    @Test
    fun testPurchaseStateEnum() {
        assertEquals("pending", PurchaseState.Pending.rawValue)
        assertEquals("purchased", PurchaseState.Purchased.rawValue)
        assertEquals("unknown", PurchaseState.Unknown.rawValue)
    }

    @Test
    fun testExternalLinkLaunchModeAndroidEnum() {
        assertEquals("unspecified", ExternalLinkLaunchModeAndroid.Unspecified.rawValue)
        assertEquals("launch-in-external-browser-or-app", ExternalLinkLaunchModeAndroid.LaunchInExternalBrowserOrApp.rawValue)
        assertEquals("caller-will-launch-link", ExternalLinkLaunchModeAndroid.CallerWillLaunchLink.rawValue)
    }

    @Test
    fun testLaunchExternalLinkParamsAndroid() {
        val params = LaunchExternalLinkParamsAndroid(
            billingProgram = BillingProgramAndroid.ExternalOffer,
            launchMode = ExternalLinkLaunchModeAndroid.LaunchInExternalBrowserOrApp,
            linkType = ExternalLinkTypeAndroid.LinkToDigitalContentOffer,
            linkUri = "https://example.com/offer"
        )

        assertEquals(BillingProgramAndroid.ExternalOffer, params.billingProgram)
        assertEquals(ExternalLinkLaunchModeAndroid.LaunchInExternalBrowserOrApp, params.launchMode)
        assertEquals("https://example.com/offer", params.linkUri)
    }

    @Test
    fun testProductAndroidOneTimePurchaseOfferDetailWithRequiredFields() {
        val detail = ProductAndroidOneTimePurchaseOfferDetail(
            formattedPrice = "$0.99",
            priceAmountMicros = "990000",
            priceCurrencyCode = "USD",
            offerTags = listOf("sale", "featured"),
            offerToken = "offer_token_abc123"
        )

        assertEquals("$0.99", detail.formattedPrice)
        assertEquals("990000", detail.priceAmountMicros)
        assertEquals("USD", detail.priceCurrencyCode)
        assertEquals(listOf("sale", "featured"), detail.offerTags)
        assertEquals("offer_token_abc123", detail.offerToken)
    }

    // =========================================================================
    // External Payments API Tests (Billing Library 8.3.0+)
    // =========================================================================

    @Test
    fun testBillingProgramAndroidExternalPayments() {
        val program = BillingProgramAndroid.ExternalPayments
        assertEquals("external-payments", program.rawValue)
        assertEquals(BillingProgramAndroid.ExternalPayments, program)
    }

    @Test
    fun testDeveloperBillingLaunchModeAndroidEnum() {
        assertEquals("unspecified", DeveloperBillingLaunchModeAndroid.Unspecified.rawValue)
        assertEquals("launch-in-external-browser-or-app", DeveloperBillingLaunchModeAndroid.LaunchInExternalBrowserOrApp.rawValue)
        assertEquals("caller-will-launch-link", DeveloperBillingLaunchModeAndroid.CallerWillLaunchLink.rawValue)
    }

    @Test
    fun testDeveloperBillingOptionParamsAndroid() {
        val params = DeveloperBillingOptionParamsAndroid(
            billingProgram = BillingProgramAndroid.ExternalPayments,
            launchMode = DeveloperBillingLaunchModeAndroid.LaunchInExternalBrowserOrApp,
            linkUri = "https://example.com/checkout"
        )

        assertEquals(BillingProgramAndroid.ExternalPayments, params.billingProgram)
        assertEquals(DeveloperBillingLaunchModeAndroid.LaunchInExternalBrowserOrApp, params.launchMode)
        assertEquals("https://example.com/checkout", params.linkUri)
    }

    @Test
    fun testDeveloperBillingOptionParamsAndroidWithCallerWillLaunch() {
        val params = DeveloperBillingOptionParamsAndroid(
            billingProgram = BillingProgramAndroid.ExternalPayments,
            launchMode = DeveloperBillingLaunchModeAndroid.CallerWillLaunchLink,
            linkUri = "https://example.com/payment"
        )

        assertEquals(BillingProgramAndroid.ExternalPayments, params.billingProgram)
        assertEquals(DeveloperBillingLaunchModeAndroid.CallerWillLaunchLink, params.launchMode)
        assertEquals("https://example.com/payment", params.linkUri)
    }

    @Test
    fun testDeveloperProvidedBillingDetailsAndroid() {
        val details = DeveloperProvidedBillingDetailsAndroid(
            externalTransactionToken = "ext_txn_token_12345"
        )

        assertEquals("ext_txn_token_12345", details.externalTransactionToken)
    }

    @Test
    fun testRequestPurchaseAndroidPropsWithDeveloperBillingOption() {
        val developerBillingOption = DeveloperBillingOptionParamsAndroid(
            billingProgram = BillingProgramAndroid.ExternalPayments,
            launchMode = DeveloperBillingLaunchModeAndroid.LaunchInExternalBrowserOrApp,
            linkUri = "https://example.com/checkout"
        )

        val props = RequestPurchaseAndroidProps(
            skus = listOf("premium_product"),
            developerBillingOption = developerBillingOption
        )

        assertEquals(listOf("premium_product"), props.skus)
        assertNotNull(props.developerBillingOption)
        assertEquals(BillingProgramAndroid.ExternalPayments, props.developerBillingOption?.billingProgram)
        assertEquals("https://example.com/checkout", props.developerBillingOption?.linkUri)
    }

    @Test
    fun testRequestSubscriptionAndroidPropsWithDeveloperBillingOption() {
        val developerBillingOption = DeveloperBillingOptionParamsAndroid(
            billingProgram = BillingProgramAndroid.ExternalPayments,
            launchMode = DeveloperBillingLaunchModeAndroid.LaunchInExternalBrowserOrApp,
            linkUri = "https://example.com/subscribe"
        )

        val props = RequestSubscriptionAndroidProps(
            skus = listOf("monthly_subscription"),
            subscriptionOffers = listOf(
                AndroidSubscriptionOfferInput(
                    sku = "monthly_subscription",
                    offerToken = "offer_token_abc"
                )
            ),
            developerBillingOption = developerBillingOption
        )

        assertEquals(listOf("monthly_subscription"), props.skus)
        assertNotNull(props.developerBillingOption)
        assertEquals(BillingProgramAndroid.ExternalPayments, props.developerBillingOption?.billingProgram)
        assertEquals("https://example.com/subscribe", props.developerBillingOption?.linkUri)
    }

    @Test
    fun testRequestPurchasePropsByPlatformsWithExternalPayments() {
        val props = RequestPurchasePropsByPlatforms(
            google = RequestPurchaseAndroidProps(
                skus = listOf("premium_upgrade"),
                developerBillingOption = DeveloperBillingOptionParamsAndroid(
                    billingProgram = BillingProgramAndroid.ExternalPayments,
                    launchMode = DeveloperBillingLaunchModeAndroid.LaunchInExternalBrowserOrApp,
                    linkUri = "https://example.com/checkout"
                )
            )
        )

        assertNotNull(props.google)
        assertNotNull(props.google?.developerBillingOption)
        assertEquals(BillingProgramAndroid.ExternalPayments, props.google?.developerBillingOption?.billingProgram)
    }
}
