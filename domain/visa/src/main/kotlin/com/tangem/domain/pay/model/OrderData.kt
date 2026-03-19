package com.tangem.domain.pay.model

data class OrderData(
    val customerId: String,
    val status: OrderStatus,
    val withdrawTxHash: String?,
)