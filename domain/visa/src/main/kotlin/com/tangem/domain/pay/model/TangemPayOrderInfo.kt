package com.tangem.domain.pay.model

data class TangemPayOrderInfo(
    val orderId: String,
    val orderStatus: OrderStatus,
)