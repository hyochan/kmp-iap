package io.github.hyochan.kmpiap.utils

import io.github.hyochan.kmpiap.types.BillingResponse

object ErrorMapping {
    fun mapErrorMessage(response: BillingResponse): String {
        return when (response) {
            BillingResponse.OK -> "Success"
            BillingResponse.USER_CANCELED -> "Purchase cancelled by user"
            BillingResponse.SERVICE_UNAVAILABLE -> "Billing service is currently unavailable"
            BillingResponse.BILLING_UNAVAILABLE -> "Billing is not available on this device"
            BillingResponse.ITEM_UNAVAILABLE -> "Requested item is not available for purchase"
            BillingResponse.DEVELOPER_ERROR -> "Developer error - invalid arguments provided"
            BillingResponse.ERROR -> "Fatal error during API action"
            BillingResponse.ITEM_ALREADY_OWNED -> "Item is already owned"
            BillingResponse.ITEM_NOT_OWNED -> "Item is not owned and cannot be consumed"
        }
    }

    fun isRecoverable(response: BillingResponse): Boolean {
        return when (response) {
            BillingResponse.SERVICE_UNAVAILABLE -> true
            BillingResponse.ERROR -> true
            else -> false
        }
    }
}