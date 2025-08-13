package io.github.hyochan.kmpiap.types

import kotlinx.datetime.Clock

/**
 * Event data wrapper for purchase events
 */
data class PurchaseUpdatedEvent(
    val purchase: Purchase,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Event data wrapper for error events
 */
data class PurchaseErrorEvent(
    val error: PurchaseError,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Event data wrapper for promoted product events
 */
data class PromotedProductEvent(
    val productId: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Event listener configuration
 */
data class EventListenerConfig(
    val autoRemoveOnError: Boolean = false,
    val logEvents: Boolean = false,
    val maxRetries: Int = 0
)

/**
 * Base event interface
 */
interface IapEventBase {
    val type: IapEvent
    val timestamp: Long
}

/**
 * Purchase updated event implementation
 */
data class IapPurchaseUpdatedEvent(
    override val type: IapEvent = IapEvent.PURCHASE_UPDATED,
    override val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val purchase: Purchase
) : IapEventBase

/**
 * Purchase error event implementation
 */
data class IapPurchaseErrorEvent(
    override val type: IapEvent = IapEvent.PURCHASE_ERROR,
    override val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val error: PurchaseError
) : IapEventBase

/**
 * Promoted product event implementation (iOS)
 */
data class IapPromotedProductEvent(
    override val type: IapEvent = IapEvent.PROMOTED_PRODUCT_IOS,
    override val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val productId: String
) : IapEventBase