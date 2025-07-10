package io.github.hyochan.kmpiap

public actual fun createInAppPurchase(): InAppPurchase = IosInAppPurchaseImpl()

public class IosInAppPurchaseImpl : InAppPurchase {
    override fun getVersion(): String {
        return "KMP-IAP v0.0.0-alpha1 (iOS)"
    }
}