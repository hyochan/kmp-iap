package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.types.*
import io.github.hyochan.kmpiap.utils.ErrorCode
import io.github.hyochan.kmpiap.utils.ErrorCodeUtils
import kotlinx.datetime.Clock
import kotlin.test.*

class InAppPurchaseTest {
    @Test
    fun testCreateInAppPurchase() {
        val iap = createInAppPurchase()
        assertNotNull(iap)
    }
    
    @Test
    fun testGetVersion() {
        val iap = createInAppPurchase()
        val version = iap.getVersion()
        assertTrue(version.contains("KMP-IAP"))
        assertTrue(version.contains("alpha"))
    }
    
    @Test
    fun testGetStore() {
        val iap = createInAppPurchase()
        val store = iap.getStore()
        assertTrue(
            store == Store.PLAY_STORE || store == Store.APP_STORE,
            "Store should be either PLAY_STORE or APP_STORE"
        )
    }
    
    @Test
    fun testPurchaseErrorTypes() {
        val error = PurchaseError(
            message = "Test error",
            code = ErrorCode.E_USER_CANCELLED,
            productId = "test_product"
        )
        
        assertEquals("Test error", error.message)
        assertEquals(ErrorCode.E_USER_CANCELLED, error.code)
        assertEquals("test_product", error.productId)
        assertEquals("[kmp-iap]: PurchaseError", error.name)
    }
    
    @Test
    fun testProductTypes() {
        val product = Product(
            productId = "test_product",
            price = "0.99",
            currency = "USD",
            localizedPrice = "$0.99",
            title = "Test Product",
            description = "A test product",
            platform = IAPPlatform.ANDROID,
            type = ProductType.INAPP
        )
        
        assertEquals("test_product", product.productId)
        assertEquals("0.99", product.price)
        assertEquals("USD", product.currency)
        assertEquals("$0.99", product.localizedPrice)
        assertEquals(ProductType.INAPP, product.type)
    }
    
    @Test
    fun testSubscriptionTypes() {
        val subscription = Subscription(
            productId = "test_subscription",
            price = "9.99",
            currency = "USD",
            localizedPrice = "$9.99",
            title = "Test Subscription",
            description = "A test subscription",
            platform = IAPPlatform.IOS,
            type = ProductType.SUBS,
            subscriptionPeriodUnitIOS = "month",
            subscriptionPeriodNumberIOS = 1
        )
        
        assertEquals("test_subscription", subscription.productId)
        assertEquals("9.99", subscription.price)
        assertEquals(ProductType.SUBS, subscription.type)
        assertEquals("month", subscription.subscriptionPeriodUnitIOS)
        assertEquals(1, subscription.subscriptionPeriodNumberIOS)
    }
    
    @Test
    fun testPurchaseTypes() {
        val purchase = Purchase(
            productId = "test_product",
            transactionId = "12345",
            transactionReceipt = "receipt_data",
            purchaseToken = "token",
            transactionDate = Clock.System.now(),
            platform = IAPPlatform.ANDROID,
            isAcknowledgedAndroid = false,
            purchaseStateAndroid = "purchased"
        )
        
        assertEquals("test_product", purchase.productId)
        assertEquals("12345", purchase.transactionId)
        assertEquals("receipt_data", purchase.transactionReceipt)
        assertEquals("token", purchase.purchaseToken)
        assertEquals(IAPPlatform.ANDROID, purchase.platform)
        assertFalse(purchase.isAcknowledgedAndroid ?: true)
    }
    
    @Test
    fun testRequestProductsParams() {
        val params = RequestProductsParams(
            skus = listOf("product1", "product2"),
            type = PurchaseType.INAPP
        )
        
        assertEquals(2, params.skus.size)
        assertEquals("product1", params.skus[0])
        assertEquals("product2", params.skus[1])
        assertEquals(PurchaseType.INAPP, params.type)
    }
    
    @Test
    fun testAndroidRequestPurchase() {
        val request = RequestPurchaseAndroid(
            skus = listOf("product1"),
            obfuscatedAccountIdAndroid = "user123",
            obfuscatedProfileIdAndroid = "profile456"
        )
        
        assertEquals("product1", request.sku)
        assertEquals(listOf("product1"), request.skus)
        assertEquals("user123", request.obfuscatedAccountIdAndroid)
        assertEquals("profile456", request.obfuscatedProfileIdAndroid)
        assertEquals(IAPPlatform.ANDROID, request.platform)
    }
    
    @Test
    fun testIosRequestPurchase() {
        val request = RequestPurchaseIOS(
            sku = "product1",
            appAccountToken = "token123",
            quantity = 2
        )
        
        assertEquals("product1", request.sku)
        assertEquals("token123", request.appAccountToken)
        assertEquals(2, request.quantity)
        assertEquals(IAPPlatform.IOS, request.platform)
    }
    
    @Test
    fun testErrorCodeMapping() {
        // Test error code utilities
        val errorCode = ErrorCode.E_USER_CANCELLED
        
        // Platform-specific code mapping
        val androidCode = ErrorCodeUtils.toPlatformCode(
            errorCode, 
            IAPPlatform.ANDROID
        )
        assertEquals("E_USER_CANCELLED", androidCode)
        
        val iosCode = ErrorCodeUtils.toPlatformCode(
            errorCode, 
            IAPPlatform.IOS
        )
        assertEquals(2, iosCode)
        
        // Reverse mapping
        val androidMapped = ErrorCodeUtils.fromPlatformCode(
            "E_USER_CANCELLED", 
            IAPPlatform.ANDROID
        )
        assertEquals(ErrorCode.E_USER_CANCELLED, androidMapped)
        
        val iosMapped = ErrorCodeUtils.fromPlatformCode(
            2, 
            IAPPlatform.IOS
        )
        assertEquals(ErrorCode.E_USER_CANCELLED, iosMapped)
    }
    
    @Test
    fun testConnectionResult() {
        val connected = ConnectionResult(
            connected = true,
            message = "Connected successfully"
        )
        
        assertTrue(connected.connected)
        assertEquals("Connected successfully", connected.message)
        
        val disconnected = ConnectionResult(
            connected = false,
            message = "Connection failed"
        )
        
        assertFalse(disconnected.connected)
        assertEquals("Connection failed", disconnected.message)
    }
    
    @Test
    fun testAppStoreInfo() {
        val info = AppStoreInfo(
            countryCode = "US",
            storefront = "143441",
            identifier = "com.example.app"
        )
        
        assertEquals("US", info.countryCode)
        assertEquals("143441", info.storefront)
        assertEquals("com.example.app", info.identifier)
    }
    
    @Test
    fun testAndroidProrationModes() {
        assertEquals(1, AndroidProrationMode.IMMEDIATE_WITH_TIME_PRORATION.value)
        assertEquals(2, AndroidProrationMode.IMMEDIATE_AND_CHARGE_PRORATED_PRICE.value)
        assertEquals(3, AndroidProrationMode.IMMEDIATE_WITHOUT_PRORATION.value)
        assertEquals(4, AndroidProrationMode.DEFERRED.value)
        assertEquals(5, AndroidProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE.value)
    }
    
    @Test
    fun testPricingPhase() {
        val phase = PricingPhase(
            price = "4.99",
            formattedPrice = "$4.99",
            currency = "USD",
            billingCycleCount = 3,
            billingPeriod = "P1M",
            recurrenceMode = 1,
            priceAmountMicros = 4990000
        )
        
        assertEquals("4.99", phase.price)
        assertEquals("$4.99", phase.formattedPrice)
        assertEquals("USD", phase.currency)
        assertEquals(3, phase.billingCycleCount)
        assertEquals("P1M", phase.billingPeriod)
    }
    
    @Test
    fun testSubscriptionOffer() {
        val offer = SubscriptionOffer(
            offerId = "offer123",
            basePlanId = "base_plan",
            offerToken = "token123",
            pricingPhases = listOf(
                PricingPhase(
                    price = "0",
                    formattedPrice = "Free",
                    currency = "USD",
                    billingPeriod = "P7D"
                )
            )
        )
        
        assertEquals("offer123", offer.offerId)
        assertEquals("base_plan", offer.basePlanId)
        assertEquals("token123", offer.offerToken)
        assertEquals(1, offer.pricingPhases?.size)
        assertEquals("Free", offer.pricingPhases?.first()?.formattedPrice)
    }
}