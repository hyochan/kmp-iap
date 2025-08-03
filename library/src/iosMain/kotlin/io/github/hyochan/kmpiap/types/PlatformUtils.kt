package io.github.hyochan.kmpiap.types

import platform.StoreKit.SKPaymentQueue
import platform.UIKit.UIDevice

object PlatformUtils {
    fun isStoreKitAvailable(): Boolean {
        return SKPaymentQueue.canMakePayments()
    }
    
    fun getDeviceInfo(): String {
        return "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}"
    }
}