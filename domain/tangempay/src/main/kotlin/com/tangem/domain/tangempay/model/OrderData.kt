package com.tangem.domain.tangempay.model

data class OrderData(
    val customerId: String,
    val status: OrderStatus,
    val withdrawTxHash: String?,
)