package io.github.hyochan.kmpiap

public actual fun createInAppPurchase(): InAppPurchase = LinuxInAppPurchaseImpl()

public class LinuxInAppPurchaseImpl : InAppPurchase {
    override fun getVersion(): String {
        return "KMP-IAP v0.0.0-alpha1 (Linux)"
    }
}