package io.github.hyochan.kmpiap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import io.github.hyochan.kmpiap.types.*
import io.github.hyochan.kmpiap.utils.ErrorCode
import kotlinx.datetime.Instant

class InAppPurchaseTest {
    
    @Test
    fun testErrorCodes() {
        val errorCode = ErrorCode.E_USER_CANCELLED
        assertEquals("E_USER_CANCELLED", errorCode.name)
        assertNotNull(errorCode)
    }
    
    @Test
    fun testPlatformTypes() {
        val platform = IapPlatform.ANDROID
        assertEquals(IapPlatform.ANDROID, platform)
    }
    
    @Test
    fun testConnectionResult() {
        val result = ConnectionResult(connected = true, message = "Connected successfully")
        assertEquals(true, result.connected)
        assertEquals("Connected successfully", result.message)
    }
    
    @Test
    fun testPurchaseError() {
        val error = PurchaseError(code = ErrorCode.E_USER_CANCELLED.name, message = "Test error")
        assertEquals("Test error", error.message)
        assertEquals(ErrorCode.E_USER_CANCELLED.name, error.code)
    }
    
    @Test
    fun testProductTypes() {
        val product = ProductAndroid(
            id = "test_product",
            displayPrice = "$0.99",
            price = 0.99,
            currency = "USD",
            title = "Test Product",
            description = "A test product",
            type = ProductType.INAPP,
            nameAndroid = "Test Product",
            platform = "android"
        )
        
        assertEquals("test_product", product.id)
        assertEquals("$0.99", product.displayPrice)
        assertEquals("USD", product.currency)
        assertEquals(0.99, product.price)
    }
    
    @Test
    fun testSubscriptionTypes() {
        val subscription = ProductSubscriptionIOS(
            id = "test_subscription",
            displayPrice = "$9.99",
            price = 9.99,
            currency = "USD",
            title = "Test Subscription",
            description = "A test subscription",
            type = ProductType.SUBS,
            displayNameIOS = "Test Subscription",
            isFamilyShareableIOS = false,
            jsonRepresentationIOS = "{}",
            subscriptionPeriodNumberIOS = "1",
            subscriptionPeriodUnitIOS = "MONTH",
            platform = "ios"
        )
        
        assertEquals("test_subscription", subscription.id)
        assertEquals("$9.99", subscription.displayPrice)
        assertEquals("1", subscription.subscriptionPeriodNumberIOS)
    }
    
    @Test
    fun testPurchaseTypes() {
        val purchase = PurchaseAndroid(
            id = "12345",  // Primary identifier
            productId = "test_product",
            purchaseToken = "token",  // Unified purchase token
            transactionId = "12345",  // @deprecated - use id instead
            transactionReceipt = "receipt_data",
            transactionDate = 1234567890.0,
            isAcknowledgedAndroid = false,
            purchaseStateAndroid = PurchaseAndroidState.PURCHASED,
            platform = "android"
        )
        
        assertEquals("12345", purchase.id)
        assertEquals("test_product", purchase.productId)
        assertEquals("token", purchase.purchaseToken)  // Test unified field
        assertEquals("12345", purchase.transactionId)
        assertEquals("receipt_data", purchase.transactionReceipt)
        assertEquals("android", purchase.platform)
        assertFalse(purchase.isAcknowledgedAndroid ?: true)
    }
    
    @Test
    fun testProductRequest() {
        val request = ProductRequest(
            skus = listOf("product1", "product2"),
            type = ProductType.INAPP
        )
        
        assertEquals(2, request.skus.size)
        assertEquals(ProductType.INAPP, request.type)
    }
    
    @Test
    fun testActiveSubscription() {
        val expirationDate = Instant.fromEpochSeconds(1234567890)
        val activeSubscription = ActiveSubscription(
            productId = "test_sub",
            isActive = true,
            expirationDateIOS = expirationDate,
            autoRenewingAndroid = null,
            environmentIOS = "Production",
            willExpireSoon = false,
            daysUntilExpirationIOS = 30
        )
        
        assertEquals("test_sub", activeSubscription.productId)
        assertEquals(true, activeSubscription.isActive)
        assertEquals(expirationDate, activeSubscription.expirationDateIOS)
        assertEquals("Production", activeSubscription.environmentIOS)
    }
}