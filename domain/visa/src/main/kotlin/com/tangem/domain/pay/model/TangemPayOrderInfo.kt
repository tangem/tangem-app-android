package com.tangem.domain.pay.model

data class TangemPayOrderInfo(
    val orderId: String,
    val orderStatus: OrderStatus,
    val orderStep: OrderStep = OrderStep.UNKNOWN,
) {
    companion object {
        fun fromOrder(order: Order) = TangemPayOrderInfo(
            orderId = order.id,
            orderStatus = order.status,
            orderStep = order.step,
        )
    }
}