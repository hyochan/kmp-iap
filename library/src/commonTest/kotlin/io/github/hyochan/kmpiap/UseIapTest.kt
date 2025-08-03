package io.github.hyochan.kmpiap

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UseIapTest {
    @Test
    fun testIapStateInitialization() {
        val iapState = useIap()
        
        assertFalse(iapState.isInitialized.value)
        assertTrue(iapState.products.value.isEmpty())
        assertTrue(iapState.purchases.value.isEmpty())
    }
    
    @Test
    fun testIapStateUpdates() {
        val iapState = useIap()
        
        iapState.setInitialized(true)
        assertTrue(iapState.isInitialized.value)
        
        val testProducts = listOf(
            Product("test_id", "Test Product", "Test Description", "$0.99")
        )
        iapState.setProducts(testProducts)
        assertTrue(iapState.products.value.isNotEmpty())
    }
}