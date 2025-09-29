package com.tangem.domain.pay.model

enum class OrderStatus(val apiName: String) {
    NOT_ISSUED(""),
    NEW("NEW"),
    PROCESSING("PROCESSING"),
    COMPLETED("COMPLETED"),
    CANCELED("CANCELED"),
}