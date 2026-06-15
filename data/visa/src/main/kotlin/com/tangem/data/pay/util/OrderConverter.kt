package com.tangem.data.pay.util

import com.tangem.datasource.api.pay.models.response.OrderResponse
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderType

/** Maps a wire `OrderResponse.Result` into the domain [Order] model. */
internal object OrderConverter {

    fun convert(value: OrderResponse.Result): Order {
        val status = OrderStatusConverter.convert(value.status)
        val type = OrderType.fromString(value.type ?: value.data.type)
        return Order(
            id = value.id,
            customerId = value.customerId,
            type = type,
            status = status,
            step = value.step,
            stepChangeCode = value.stepChangeCode,
            productInstanceId = value.data.productInstanceId,
            paymentAccountId = value.data.paymentAccountId,
            cardId = null, // Card id not in v1 response shape; resolved via productInstanceId.
            withdrawTxHash = value.data.transactionHash?.ifEmpty { null },
            createdAt = value.createdAt,
            updatedAt = value.updatedAt,
        )
    }
}