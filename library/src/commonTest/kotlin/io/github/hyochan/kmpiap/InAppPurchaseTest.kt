package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.openiap.*
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
