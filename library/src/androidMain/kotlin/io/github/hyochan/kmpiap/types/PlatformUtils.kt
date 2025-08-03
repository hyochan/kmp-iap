package io.github.hyochan.kmpiap.types

import android.content.Context

object PlatformUtils {
    fun isGooglePlayAvailable(context: Context): Boolean {
        // TODO: Check if Google Play Services are available
        return try {
            context.packageManager.getPackageInfo("com.android.vending", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}