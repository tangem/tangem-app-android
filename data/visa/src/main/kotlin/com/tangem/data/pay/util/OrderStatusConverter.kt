package com.tangem.data.pay.util

import com.tangem.spend.datasource.pay.models.response.OrderResponse
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger

internal object OrderStatusConverter : Converter<OrderResponse.Result.Status, OrderStatus> {
    override fun convert(value: OrderResponse.Result.Status): OrderStatus {
        return when (value) {
            OrderResponse.Result.Status.PROCESSING -> OrderStatus.PROCESSING
            OrderResponse.Result.Status.COMPLETED -> OrderStatus.COMPLETED
            OrderResponse.Result.Status.NEW -> OrderStatus.NEW
            OrderResponse.Result.Status.CANCELED -> OrderStatus.CANCELED
            // An unknown status is most likely a new intermediate one, so the order keeps being polled
            // instead of being reported as finished — the same rule a missing status already follows.
            OrderResponse.Result.Status.UNKNOWN -> {
                TangemLogger.e("Unknown order status received, treated as ${OrderStatus.PROCESSING}")
                OrderStatus.PROCESSING
            }
        }
    }
}