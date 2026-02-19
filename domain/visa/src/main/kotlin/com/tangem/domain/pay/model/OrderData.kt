package com.tangem.domain.pay.model

data class OrderData(
    val status: OrderStatus,
    val withdrawTxHash: String?,
)