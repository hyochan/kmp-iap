package io.github.hyochan.kmpiap

import kotlin.test.Test
import kotlin.test.assertNotNull

class InAppPurchaseTest {
    @Test
    fun testCreateInAppPurchase() {
        val iap = createInAppPurchase()
        assertNotNull(iap)
    }
}