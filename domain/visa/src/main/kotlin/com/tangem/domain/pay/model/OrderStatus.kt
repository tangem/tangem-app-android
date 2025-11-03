package com.tangem.domain.pay.model

enum class OrderStatus(val apiName: String) {
    UNKNOWN(""),
    NEW("NEW"),
    PROCESSING("PROCESSING"),
    COMPLETED("COMPLETED"),
    CANCELED("CANCELED"),
}