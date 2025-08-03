package io.github.hyochan.kmpiap.types

enum class ProductType {
    CONSUMABLE,
    NON_CONSUMABLE,
    SUBSCRIPTION
}

enum class BillingResponse {
    OK,
    USER_CANCELED,
    SERVICE_UNAVAILABLE,
    BILLING_UNAVAILABLE,
    ITEM_UNAVAILABLE,
    DEVELOPER_ERROR,
    ERROR,
    ITEM_ALREADY_OWNED,
    ITEM_NOT_OWNED
}