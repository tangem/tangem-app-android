package com.tangem.data.pay.util

import com.tangem.datasource.api.pay.models.response.OrderResponse
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.utils.converter.Converter

internal object OrderStatusConverter : Converter<OrderResponse.Result.Status, OrderStatus> {
    override fun convert(value: OrderResponse.Result.Status): OrderStatus {
        return when (value) {
            OrderResponse.Result.Status.PROCESSING -> OrderStatus.PROCESSING
            OrderResponse.Result.Status.COMPLETED -> OrderStatus.COMPLETED
            OrderResponse.Result.Status.NEW -> OrderStatus.NEW
            OrderResponse.Result.Status.CANCELED -> OrderStatus.CANCELED
        }
    }
}