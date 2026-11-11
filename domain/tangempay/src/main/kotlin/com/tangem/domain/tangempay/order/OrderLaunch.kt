package com.tangem.domain.tangempay.order

import com.tangem.domain.tangempay.model.CustomerOrderRequest

sealed interface OrderLaunch {

    data class Data(
        val request: CustomerOrderRequest,
        val idempotencyKey: String,
    ) : OrderLaunch

    data class Id(val orderId: String) : OrderLaunch
}