package io.github.hyochan.kmpiap

public actual fun createInAppPurchase(): InAppPurchase = JvmInAppPurchaseImpl()

public class JvmInAppPurchaseImpl : InAppPurchase {
    override fun getVersion(): String {
        return "KMP-IAP v0.0.0-alpha1 (JVM)"
    }
}