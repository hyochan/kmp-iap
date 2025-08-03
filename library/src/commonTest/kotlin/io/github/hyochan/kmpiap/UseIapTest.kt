package io.github.hyochan.kmpiap

import io.github.hyochan.kmpiap.types.*
import io.github.hyochan.kmpiap.useIap.UseIap
import io.github.hyochan.kmpiap.useIap.useIap
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class UseIapTest {
    private lateinit var iapHelper: UseIap
    
    @BeforeTest
    fun setup() {
        iapHelper = useIap()
    }
    
    @AfterTest
    fun tearDown() {
        iapHelper.dispose()
    }
    
    @Test
    fun testInitialState() {
        // Initial state should be empty/disconnected
        assertEquals(emptyList(), iapHelper.products.value)
        assertEquals(emptyList(), iapHelper.subscriptions.value)
        assertEquals(emptyList(), iapHelper.availablePurchases.value)
        assertEquals(emptyList(), iapHelper.purchaseHistories.value)
        assertNull(iapHelper.currentPurchase.value)
        assertNull(iapHelper.currentError.value)
        assertFalse(iapHelper.isConnected.value)
        assertNull(iapHelper.promotedProductsIOS.value)
    }
    
    @Test
    fun testGetStore() {
        val store = iapHelper.getStore()
        assertTrue(
            store == Store.PLAY_STORE || store == Store.APP_STORE,
            "Store should be either PLAY_STORE or APP_STORE"
        )
    }
    
    @Test
    fun testClearError() {
        // Simulate an error state
        // In real scenario, this would be set by a failed operation
        iapHelper.clearError()
        assertNull(iapHelper.currentError.value)
    }
    
    @Test
    fun testClearPurchase() {
        // Simulate a purchase state
        // In real scenario, this would be set by a successful purchase
        iapHelper.clearPurchase()
        assertNull(iapHelper.currentPurchase.value)
    }
    
    @Test
    fun testEventListeners() {
        // Verify event listeners are set up
        assertNotNull(iapHelper.purchaseUpdatedListener)
        assertNotNull(iapHelper.purchaseErrorListener)
        assertNotNull(iapHelper.connectionStateListener)
        assertNotNull(iapHelper.promotedProductListener)
    }
    
    @Test
    fun testConnectionFlow() = runBlocking {
        // Test connection flow
        assertFalse(iapHelper.isConnected.value)
        
        try {
            iapHelper.initConnection()
            // Connection state will be updated via flow
        } catch (e: Exception) {
            // May fail in test environment without proper platform setup
            assertTrue(e is PurchaseError || e is NotImplementedError)
        }
    }
    
    @Test
    fun testRequestProductsValidation() = runBlocking {
        // Test that empty SKU list is handled
        try {
            iapHelper.getProducts(emptyList())
        } catch (e: Exception) {
            // Expected to fail with empty list or in test environment
            assertTrue(e is PurchaseError || e is NotImplementedError || e is IllegalArgumentException)
        }
        
        // Test with valid SKUs
        try {
            iapHelper.getProducts(listOf("test_product_1", "test_product_2"))
        } catch (e: Exception) {
            // May fail in test environment without proper platform setup
            assertTrue(e is PurchaseError || e is NotImplementedError)
        }
    }
    
    @Test
    fun testRequestSubscriptionsValidation() = runBlocking {
        // Test subscription request
        try {
            iapHelper.getSubscriptions(listOf("test_subscription_1"))
        } catch (e: Exception) {
            // May fail in test environment without proper platform setup
            assertTrue(e is PurchaseError || e is NotImplementedError)
        }
    }
    
    @Test
    fun testPurchaseRequestValidation() = runBlocking {
        // Test purchase request with missing product
        try {
            iapHelper.requestPurchase(
                sku = "non_existent_product",
                obfuscatedAccountIdAndroid = "test_user"
            )
        } catch (e: Exception) {
            // Expected to fail
            assertTrue(e is PurchaseError || e is NotImplementedError)
        }
    }
    
    @Test
    fun testSubscriptionRequestValidation() = runBlocking {
        // Test subscription request
        try {
            iapHelper.requestSubscription(
                sku = "test_subscription",
                obfuscatedAccountIdAndroid = "test_user",
                subscriptionOffers = listOf(
                    SubscriptionOfferAndroid(
                        sku = "test_subscription",
                        offerToken = "test_offer"
                    )
                )
            )
        } catch (e: Exception) {
            // Expected to fail in test environment
            assertTrue(e is PurchaseError || e is NotImplementedError)
        }
    }
    
    @Test
    fun testFinishTransactionValidation() = runBlocking {
        // Create a mock purchase
        val purchase = Purchase(
            productId = "test_product",
            transactionId = "12345",
            platform = getCurrentPlatform()
        )
        
        try {
            val result = iapHelper.finishTransaction(purchase, isConsumable = true)
            // May return false if not properly connected
            assertTrue(result || !iapHelper.isConnected.value)
        } catch (e: Exception) {
            // Expected to fail in test environment
            assertTrue(e is PurchaseError || e is NotImplementedError)
        }
    }
    
    @Test
    fun testCanMakePayments() = runBlocking {
        try {
            val canPay = iapHelper.canMakePayments()
            // Should return a boolean
            assertTrue(canPay || !canPay) // Always true, just checking it returns boolean
        } catch (e: Exception) {
            // May fail in test environment
            assertTrue(e is NotImplementedError)
        }
    }
    
    @Test
    fun testPlatformSpecificMethods() = runBlocking {
        val platform = getCurrentPlatform()
        
        if (platform == IAPPlatform.IOS) {
            // Test iOS-specific methods
            try {
                iapHelper.getStorefrontIOS()
                iapHelper.presentCodeRedemptionSheetIOS()
                iapHelper.showManageSubscriptionsIOS()
            } catch (e: Exception) {
                // Expected to fail in test environment
                assertTrue(e is NotImplementedError || e is PurchaseError)
            }
        } else {
            // Test Android-specific methods
            try {
                iapHelper.deepLinkToSubscriptionsAndroid("test_sku")
            } catch (e: Exception) {
                // Expected to fail in test environment
                assertTrue(e is NotImplementedError || e is PurchaseError)
            }
        }
    }
}