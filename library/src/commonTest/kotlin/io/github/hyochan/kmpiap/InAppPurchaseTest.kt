package io.github.hyochan.kmpiap

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InAppPurchaseTest {
    @Test
    fun testKmpIapVersion() {
        val version = KmpIap.getVersion()
        assertNotNull(version)
        assertTrue(version.contains("KMP-IAP"))
    }
}