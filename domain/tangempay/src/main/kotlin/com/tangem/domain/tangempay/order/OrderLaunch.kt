package com.tangem.domain.tangempay.order

import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.model.OrderDetails

sealed interface OrderLaunch {

    data class Create(
        val request: CustomerOrderRequest,
        val idempotencyKey: String,
    ) : OrderLaunch

    data class Track(
        val orderId: String,
        val details: OrderDetails? = null,
    ) : OrderLaunch
}