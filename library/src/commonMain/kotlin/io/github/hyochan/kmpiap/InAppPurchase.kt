package io.github.hyochan.kmpiap

/**
 * Main interface for In-App Purchase operations across all platforms.
 */
public interface InAppPurchase {
    /**
     * Returns the version of the KMP-IAP library.
     * Format: "KMP-IAP v{version} ({platform})"
     */
    fun getVersion(): String
}

/**
 * Factory function to create platform-specific InAppPurchase implementation.
 */
public expect fun createInAppPurchase(): InAppPurchase