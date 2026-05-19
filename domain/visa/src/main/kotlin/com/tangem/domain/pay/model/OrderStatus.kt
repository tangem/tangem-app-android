package com.tangem.domain.pay.model

enum class OrderStatus {
    NEW,
    PROCESSING,
    COMPLETED,
    CANCELED,
}

val OrderStatus.isFinalStatus
    get() = this == OrderStatus.COMPLETED || this == OrderStatus.CANCELED