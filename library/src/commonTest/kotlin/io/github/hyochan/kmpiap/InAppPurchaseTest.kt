package io.github.hyochan.kmpiap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import io.github.hyochan.kmpiap.types.*
import io.github.hyochan.kmpiap.utils.ErrorCode

class InAppPurchaseTest {
    
    @Test
    fun testErrorCodes() {
        val errorCode = ErrorCode.E_USER_CANCELLED
        assertEquals("E_USER_CANCELLED", errorCode.name)
        assertNotNull(errorCode)
    }
    
    @Test
    fun testPlatformTypes() {
        val android = IapPlatform.ANDROID
        val ios = IapPlatform.IOS
        
        assertEquals("ANDROID", android.name)
        assertEquals("IOS", ios.name)
    }
    
    @Test
    fun testPurchaseErrorTypes() {
        val error = PurchaseError(
            message = "Test error",
            code = ErrorCode.E_USER_CANCELLED.name
        )
        
        assertEquals("Test error", error.message)
        assertEquals(ErrorCode.E_USER_CANCELLED.name, error.code)
    }
    
    @Test
    fun testProductTypes() {
        val product = Product(
            id = "test_product",
            price = "$0.99",
            priceAmount = 0.99,
            currency = "USD",
            title = "Test Product",
            description = "A test product",
            platform = IapPlatform.ANDROID
        )
        
        assertEquals("test_product", product.id)
        assertEquals("$0.99", product.price)
        assertEquals("USD", product.currency)
        assertEquals(0.99, product.priceAmount)
    }
    
    @Test
    fun testSubscriptionTypes() {
        val subscription = SubscriptionProduct(
            id = "test_subscription",
            price = "$9.99",
            priceAmount = 9.99,
            currency = "USD",
            title = "Test Subscription",
            description = "A test subscription",
            platform = IapPlatform.IOS,
            subscriptionPeriod = "P1M",
            introductoryPrice = null,
            subscriptionGroupIdentifier = null
        )
        
        assertEquals("test_subscription", subscription.id)
        assertEquals("$9.99", subscription.price)
        assertEquals("P1M", subscription.subscriptionPeriod)
    }
    
    @Test
    fun testPurchaseTypes() {
        val purchase = Purchase(
            id = "12345",  // Primary identifier
            productId = "test_product",
            purchaseToken = "token",  // Unified purchase token
            transactionId = "12345",  // @deprecated - use id instead
            transactionReceipt = "receipt_data",
            transactionDate = 1234567890.0,
            purchaseTokenAndroid = "token",  // @deprecated - use purchaseToken instead
            platform = IapPlatform.ANDROID,
            acknowledgedAndroid = false,
            purchaseStateAndroid = 1
        )
        
        assertEquals("12345", purchase.id)
        assertEquals("test_product", purchase.productId)
        assertEquals("token", purchase.purchaseToken)  // Test unified field
        assertEquals("12345", purchase.transactionId)
        assertEquals("receipt_data", purchase.transactionReceipt)
        assertEquals("token", purchase.purchaseTokenAndroid)  // Test deprecated field
        assertEquals(IapPlatform.ANDROID, purchase.platform)
        assertFalse(purchase.acknowledgedAndroid ?: true)
    }
    
    @Test
    fun testProductRequest() {
        val request = ProductRequest(
            skus = listOf("product1", "product2"),
            type = ProductType.INAPP
        )
        
        assertEquals(2, request.skus.size)
        assertEquals("product1", request.skus[0])
        assertEquals("product2", request.skus[1])
        assertEquals(ProductType.INAPP, request.type)
    }
    
    @Test
    fun testUnifiedPurchaseRequest() {
        val request = UnifiedPurchaseRequest(
            sku = "product1",
            quantity = 1,
            obfuscatedAccountIdAndroid = "user123",
            obfuscatedProfileIdAndroid = "profile456"
        )
        
        assertEquals("product1", request.sku)
        assertEquals(1, request.quantity)
        assertEquals("user123", request.obfuscatedAccountIdAndroid)
        assertEquals("profile456", request.obfuscatedProfileIdAndroid)
    }
    
    @Test
    fun testReplacementMode() {
        val mode = ReplacementMode.IMMEDIATE_WITHOUT_PRORATION
        assertEquals("IMMEDIATE_WITHOUT_PRORATION", mode.name)
    }
    
    @Test
    fun testPurchaseState() {
        val purchased = PurchaseState.PURCHASED
        val pending = PurchaseState.PENDING
        
        assertEquals("PURCHASED", purchased.name)
        assertEquals("PENDING", pending.name)
    }
    
    @Test
    fun testRecurrenceMode() {
        val finite = RecurrenceMode.FINITE_RECURRING
        val infinite = RecurrenceMode.INFINITE_RECURRING
        
        assertEquals("FINITE_RECURRING", finite.name)
        assertEquals("INFINITE_RECURRING", infinite.name)
    }
    
    @Test
    fun testTransactionState() {
        val purchasing = TransactionState.PURCHASING
        val purchased = TransactionState.PURCHASED
        
        assertEquals("PURCHASING", purchasing.name)
        assertEquals("PURCHASED", purchased.name)
    }
    
    @Test
    fun testPricingPhase() {
        val phase = PricingPhase(
            formattedPrice = "$4.99",
            priceCurrencyCode = "USD",
            billingCycleCount = 3,
            billingPeriod = "P1M",
            recurrenceMode = RecurrenceMode.FINITE_RECURRING,
            priceAmountMicros = "4990000"
        )
        
        assertEquals("$4.99", phase.formattedPrice)
        assertEquals("USD", phase.priceCurrencyCode)
        assertEquals(3, phase.billingCycleCount)
        assertEquals("P1M", phase.billingPeriod)
    }
    
    @Test
    fun testSubscriptionOffer() {
        val offer = SubscriptionOffer(
            sku = "test_subscription",
            offerToken = "token123"
        )
        
        assertEquals("test_subscription", offer.sku)
        assertEquals("token123", offer.offerToken)
    }
}